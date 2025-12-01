package cn.cug.sxy.ai.domain.rag.service.intent.detector;

import cn.cug.sxy.ai.domain.rag.model.entity.IntentRule;
import cn.cug.sxy.ai.domain.rag.model.intent.ComplexityLevel;
import cn.cug.sxy.ai.domain.rag.model.intent.IntentSource;
import cn.cug.sxy.ai.domain.rag.model.intent.QueryIntent;
import cn.cug.sxy.ai.domain.rag.model.intent.TaskType;
import cn.cug.sxy.ai.domain.rag.model.intent.TopicDomain;
import cn.cug.sxy.ai.domain.rag.service.intent.IntentDetector;
import cn.cug.sxy.ai.domain.rag.service.intent.IntentRuleService;
import cn.cug.sxy.ai.domain.rag.service.query.QueryType;
import cn.cug.sxy.ai.infrastructure.embedding.IEmbeddingService;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量语义路由 (数据库配置化).
 */
@Slf4j
@Component
@Order(-50)
@RequiredArgsConstructor
public class SemanticIntentDetector implements IntentDetector {

    private final IEmbeddingService embeddingService;
    private final IntentRuleService intentRuleService;

    // 缓存路由：routeKey -> SemanticRoute
    private final Map<String, SemanticRoute> routes = new ConcurrentHashMap<>();

    @Value("${rag.intent.semantic.threshold:0.78}")
    private double similarityThreshold;

    //@PostConstruct
    public void init() {
        refreshRoutes();
    }

    /**
     * <p>
     * 流程：
     * 1. 从 IntentRuleService 获取所有语义示例规则
     * 2. 批量生成 Embedding 向量 (优化：一次API调用)
     * 3. 将向量按 TaskType 分组构建新的路由表
     * 4. 原子性替换旧的路由表
     * </p>
     * 注意：这里依赖 IntentRuleService 的缓存，实际应用中可以监听事件或简单轮询
     */
    public void refreshRoutes() {
        log.info("开始刷新语义路由规则...");
        List<IntentRule> rules = intentRuleService.getRulesByType("SEMANTIC_EXAMPLE");
        if (rules.isEmpty()) {
            log.info("没有找到语义示例规则");
            return;
        }

        // 使用局部变量构建新路由，避免影响正在进行的查询
        Map<String, SemanticRoute> newRoutes = new ConcurrentHashMap<>();

        // 批量提取文本内容
        List<String> texts = rules.stream()
                .map(IntentRule::getRuleContent)
                .filter(StringUtils::isNotBlank)
                .collect(java.util.stream.Collectors.toList());

        if (texts.isEmpty()) {
            log.warn("没有有效的规则内容");
            return;
        }

        // 批量生成向量（一次API调用，大幅提升性能）
        List<float[]> vectors;
        try {
            log.info("批量生成 {} 个语义路由向量...", texts.size());
            vectors = embeddingService.generateEmbeddings(texts);
            if (vectors.size() != texts.size()) {
                log.warn("向量数量与文本数量不匹配: {} != {}", vectors.size(), texts.size());
                return;
            }
        } catch (Exception e) {
            log.error("批量生成向量失败，降级为逐个生成", e);
            // 降级：逐个生成
            vectors = new ArrayList<>();
            for (String text : texts) {
                try {
                    vectors.add(embeddingService.generateEmbedding(text));
                } catch (Exception ex) {
                    log.warn("生成向量失败: text={}", text, ex);
                    vectors.add(null);
                }
            }
        }

        // 构建路由
        for (int i = 0; i < rules.size(); i++) {
            IntentRule rule = rules.get(i);
            float[] vector = i < vectors.size() ? vectors.get(i) : null;

            if (vector == null) {
                log.warn("跳过无效向量: ruleId={}", rule.getId());
                continue;
            }

            try {
                TaskType taskType = TaskType.valueOf(rule.getTaskType());
                QueryType processor = QueryType.valueOf(rule.getTargetProcessor());
                TopicDomain domain = TopicDomain.valueOf(rule.getTopicDomain());
                String routeKey = StringUtils.isNotBlank(rule.getRouteKey()) ? rule.getRouteKey() : taskType.name();

                SemanticRoute route = newRoutes.computeIfAbsent(routeKey,
                        k -> new SemanticRoute(routeKey, taskType, processor, domain,
                                Boolean.TRUE.equals(rule.getLockProcessor()),
                                rule.getSemanticThreshold(), new ArrayList<>()));

                route.getVectors().add(vector);
            } catch (Exception e) {
                log.warn("处理语义规则失败: id={}, content={}", rule.getId(), rule.getRuleContent(), e);
            }
        }

        // 原子性替换路由表
        routes.clear();
        routes.putAll(newRoutes);
        log.info("语义路由刷新完成，加载了 {} 个路由，共 {} 个向量",
                routes.size(),
                routes.values().stream().mapToInt(r -> r.getVectors().size()).sum());
    }

    @Override
    public DetectionResult detect(IntentRequest request) {
        String query = request.queryText();
        if (query == null || query.isBlank()) {
            return null;
        }

        try {
            float[] queryEmbedding = embeddingService.generateEmbedding(query);
            double bestScore = -1;
            SemanticRoute bestRoute = null;

            // 遍历所有路由，计算查询向量与每个路由中示例向量的最大相似度
            for (SemanticRoute route : routes.values()) {
                double score = route.maxSimilarity(queryEmbedding);
                if (score > bestScore) {
                    bestScore = score;
                    bestRoute = route;
                }
            }

            double threshold = bestRoute != null && bestRoute.threshold != null ? bestRoute.threshold : similarityThreshold;
            if (bestRoute == null || bestScore < threshold) {
                return null;
            }

            QueryIntent intent = QueryIntent.builder()
                    .taskType(bestRoute.taskType)
                    .domain(bestRoute.domain)
                    .recommendedProcessor(bestRoute.processor)
                    .lockProcessor(bestRoute.lockProcessor)
                    .complexity(ComplexityLevel.MEDIUM)
                    .multiStep(bestRoute.processor != QueryType.BASIC)
                    .source(IntentSource.SEMANTIC_ROUTER)
                    .confidence(bestScore)
                    .summary("semantic-match:" + bestRoute.routeKey)
                    .build();

            return new DetectionResult(true, bestScore, intent, "semantic-match");
        } catch (Exception e) {
            log.error("语义意图检测失败", e);
            return null;
        }
    }

    @Data
    @AllArgsConstructor
    private static class SemanticRoute {
        private String routeKey;
        private TaskType taskType;
        private QueryType processor;
        private TopicDomain domain;
        private boolean lockProcessor;
        private Double threshold;
        private List<float[]> vectors;

        double maxSimilarity(float[] candidate) {
            double max = 0;
            for (float[] vector : vectors) {
                max = Math.max(max, cosine(candidate, vector));
            }
            return max;
        }

        private double cosine(float[] a, float[] b) {
            double dot = 0, normA = 0, normB = 0;
            for (int i = 0; i < a.length; i++) {
                dot += a[i] * b[i];
                normA += a[i] * a[i];
                normB += b[i] * b[i];
            }
            if (normA == 0 || normB == 0) {
                return 0;
            }
            return dot / (Math.sqrt(normA) * Math.sqrt(normB));
        }
    }
}

