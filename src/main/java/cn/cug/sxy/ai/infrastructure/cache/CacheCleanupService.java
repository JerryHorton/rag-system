package cn.cug.sxy.ai.infrastructure.cache;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 缓存清理服务。
 * 用于清理旧格式的缓存数据，解决序列化兼容性问题。
 * 
 * @author jerryhotton
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheCleanupService {

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${rag.cache.cleanup-on-startup:false}")
    private boolean cleanupOnStartup;

    /**
     * 清理embeddings缓存中的旧格式数据。
     * 当序列化器变更时，需要清理旧格式数据以避免反序列化错误。
     */
    public void cleanupEmbeddingsCache() {
        log.info("开始清理embeddings缓存的旧格式数据...");
        try {
            Cache cache = cacheManager.getCache("embeddings");
            if (cache == null) {
                log.warn("embeddings缓存不存在");
                return;
            }

            // 如果使用Redis，直接清理所有embeddings相关的键
            if (redisTemplate != null) {
                String pattern = "rag:cache:embeddings::*";
                Set<String> keys = redisTemplate.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    log.info("找到 {} 个embeddings缓存键，开始清理...", keys.size());
                    redisTemplate.delete(keys);
                    log.info("已清理 {} 个embeddings缓存键", keys.size());
                } else {
                    log.info("未找到需要清理的embeddings缓存键");
                }
            } else {
                log.info("未使用Redis，跳过清理");
            }
        } catch (Exception e) {
            log.error("清理embeddings缓存失败", e);
        }
    }

    /**
     * 清理所有缓存。
     */
    public void cleanupAllCaches() {
        log.info("开始清理所有缓存...");
        try {
            if (redisTemplate != null) {
                String pattern = "rag:cache:*";
                Set<String> keys = redisTemplate.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    log.info("找到 {} 个缓存键，开始清理...", keys.size());
                    redisTemplate.delete(keys);
                    log.info("已清理 {} 个缓存键", keys.size());
                }
            }
        } catch (Exception e) {
            log.error("清理所有缓存失败", e);
        }
    }

    /**
     * 启动时自动清理旧格式的embeddings缓存（如果配置启用）。
     */
    //@PostConstruct
    public void init() {
        if (cleanupOnStartup) {
            log.info("启动时自动清理旧格式缓存（cleanup-on-startup=true）");
            cleanupEmbeddingsCache();
        } else {
            log.info("启动时跳过缓存清理（cleanup-on-startup=false）。如需清理旧格式缓存，请手动调用cleanupEmbeddingsCache()");
        }
    }
}

