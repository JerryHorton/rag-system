package cn.cug.sxy.ai.domain.rag.service.intent.cache;

import cn.cug.sxy.ai.domain.rag.model.plan.TaskPlan;
import cn.cug.sxy.ai.infrastructure.embedding.IEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 任务规划缓存服务。
 * 通过向量相似度匹配，对语义相似的查询复用已规划的任务计划，避免重复的LLM调用。
 * 
 * @author jerryhotton
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskPlanCacheService {

    private final IEmbeddingService embeddingService;
    private final CacheManager cacheManager;
    
    // 内存缓存：查询向量 -> CachedTaskPlan
    private final ConcurrentMap<String, List<CachedTaskPlan>> vectorCache = new ConcurrentHashMap<>();
    
    @Value("${rag.intent.plan-cache.similarity-threshold:0.95}")
    private double similarityThreshold;
    
    @Value("${rag.intent.plan-cache.max-cache-size:1000}")
    private int maxCacheSize;
    
    @Value("${rag.intent.plan-cache.ttl-hours:24}")
    private int ttlHours;

    /**
     * 查找缓存的任务计划。
     * 通过计算查询向量与缓存中向量的相似度，找到最相似的已规划任务。
     * 
     * @param queryText 用户查询文本
     * @return 缓存的TaskPlan（如果找到相似度足够高的）
     */
    public Optional<TaskPlan> getCachedPlan(String queryText) {
        if (queryText == null || queryText.isBlank()) {
            return Optional.empty();
        }
        
        try {
            // 生成查询向量
            float[] queryVector = embeddingService.generateEmbedding(queryText);
            String vectorKey = getVectorKey(queryVector);
            
            // 从缓存中查找相似的计划
            List<CachedTaskPlan> cachedPlans = vectorCache.get(vectorKey);
            if (cachedPlans == null || cachedPlans.isEmpty()) {
                // 尝试从Spring Cache查找（持久化缓存）
                Cache cache = cacheManager.getCache("taskPlanCache");
                if (cache != null) {
                    CachedTaskPlan cached = cache.get(queryText.hashCode(), CachedTaskPlan.class);
                    if (cached != null && isSimilar(queryVector, cached.vector)) {
                        log.debug("从Spring Cache找到缓存的TaskPlan: query={}", queryText.substring(0, Math.min(50, queryText.length())));
                        return Optional.of(cached.plan);
                    }
                }
                return Optional.empty();
            }
            
            // 在内存缓存中查找最相似的
            CachedTaskPlan bestMatch = null;
            double bestSimilarity = -1.0;
            
            for (CachedTaskPlan cached : cachedPlans) {
                double similarity = calculateCosineSimilarity(queryVector, cached.vector);
                if (similarity >= similarityThreshold && similarity > bestSimilarity) {
                    bestSimilarity = similarity;
                    bestMatch = cached;
                }
            }
            
            if (bestMatch != null) {
                log.info("从缓存找到TaskPlan: similarity={:.3f}, query={}", 
                        bestSimilarity, queryText.substring(0, Math.min(50, queryText.length())));
                return Optional.of(bestMatch.plan);
            }
            
            return Optional.empty();
        } catch (Exception e) {
            log.warn("查找缓存TaskPlan失败: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 缓存任务计划。
     * 
     * @param queryText 用户查询文本
     * @param plan 任务计划
     */
    public void cachePlan(String queryText, TaskPlan plan) {
        if (queryText == null || queryText.isBlank() || plan == null) {
            return;
        }
        
        try {
            // 生成查询向量
            float[] queryVector = embeddingService.generateEmbedding(queryText);
            String vectorKey = getVectorKey(queryVector);
            
            // 创建缓存条目
            CachedTaskPlan cached = new CachedTaskPlan(
                    queryText,
                    queryVector,
                    plan,
                    System.currentTimeMillis()
            );
            
            // 存入内存缓存
            vectorCache.compute(vectorKey, (k, v) -> {
                if (v == null) {
                    v = new ArrayList<>();
                }
                // 限制缓存大小，移除最旧的
                if (v.size() >= 10) {
                    v.remove(0);
                }
                v.add(cached);
                return v;
            });
            
            // 同时存入Spring Cache（持久化）
            Cache cache = cacheManager.getCache("taskPlanCache");
            if (cache != null) {
                cache.put(queryText.hashCode(), cached);
            }
            
            // 清理过期缓存
            cleanupExpiredCache();
            
            log.debug("已缓存TaskPlan: query={}", queryText.substring(0, Math.min(50, queryText.length())));
        } catch (Exception e) {
            log.warn("缓存TaskPlan失败: {}", e.getMessage());
        }
    }

    /**
     * 清理过期缓存。
     */
    private void cleanupExpiredCache() {
        long expireTime = System.currentTimeMillis() - (ttlHours * 3600 * 1000L);
        
        vectorCache.values().forEach(plans -> {
            plans.removeIf(cached -> cached.timestamp < expireTime);
        });
        
        // 清理空列表
        vectorCache.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        // 限制总缓存大小
        if (vectorCache.size() > maxCacheSize) {
            // 移除最旧的条目
            vectorCache.entrySet().stream()
                    .sorted((a, b) -> {
                        long timeA = a.getValue().stream()
                                .mapToLong(c -> c.timestamp)
                                .min()
                                .orElse(0);
                        long timeB = b.getValue().stream()
                                .mapToLong(c -> c.timestamp)
                                .min()
                                .orElse(0);
                        return Long.compare(timeA, timeB);
                    })
                    .limit(vectorCache.size() - maxCacheSize)
                    .forEach(entry -> vectorCache.remove(entry.getKey()));
        }
    }

    /**
     * 计算两个向量的余弦相似度。
     */
    private double calculateCosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 检查两个向量是否相似（快速检查，用于Spring Cache）。
     */
    private boolean isSimilar(float[] a, float[] b) {
        return calculateCosineSimilarity(a, b) >= similarityThreshold;
    }

    /**
     * 获取向量的键（用于分组，减少比较次数）。
     */
    private String getVectorKey(float[] vector) {
        // 使用向量的前几个维度作为键，实现粗略分组
        if (vector == null || vector.length == 0) {
            return "default";
        }
        int keyLength = Math.min(5, vector.length);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keyLength; i++) {
            sb.append(String.format("%.2f", vector[i]));
        }
        return sb.toString();
    }

    /**
     * 缓存的任务计划条目。
     */
    private record CachedTaskPlan(
            String queryText,
            float[] vector,
            TaskPlan plan,
            long timestamp
    ) {}
}

