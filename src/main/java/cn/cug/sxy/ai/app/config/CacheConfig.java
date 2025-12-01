package cn.cug.sxy.ai.app.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import cn.cug.sxy.ai.infrastructure.cache.FloatArrayRedisSerializer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 缓存配置。
 * 支持内存缓存和Redis分布式缓存，可通过配置切换。
 * 
 * 推荐使用Redis分布式缓存，优势：
 * 1. 多实例共享缓存，避免重复计算
 * 2. 缓存一致性，避免数据不一致
 * 3. 更好的扩展性，支持集群
 * 4. 持久化能力，重启不丢失
 * 
 * @author jerryhotton
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${spring.cache.type:redis}")
    private String cacheType;

    @Value("${spring.cache.redis.time-to-live:86400}")
    private long defaultTtl; // 默认24小时

    /**
     * 配置缓存管理器。
     * 根据配置选择内存缓存或Redis缓存。
     */
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        if ("redis".equalsIgnoreCase(cacheType) && redisConnectionFactory != null) {
            return redisCacheManager(redisConnectionFactory);
        } else {
            // 降级为内存缓存（开发环境或Redis不可用时）
            return memoryCacheManager();
        }
    }

    /**
     * Redis分布式缓存管理器。
     * 推荐用于生产环境。
     */
    private CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        // 配置Jackson序列化器（支持复杂对象）
        // 创建并配置ObjectMapper
        ObjectMapper objectMapper = createObjectMapper();
        
        // 使用GenericJackson2JsonRedisSerializer，支持在构造函数中传入ObjectMapper
        // 避免使用已弃用的setObjectMapper方法
        GenericJackson2JsonRedisSerializer serializer = createJacksonSerializer(objectMapper);

        // 默认缓存配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(defaultTtl))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues(); // 不缓存null值

        // 为不同缓存区域配置不同的TTL和序列化器
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // embeddings缓存：向量嵌入，使用专门的float[]序列化器
        // 因为float[]数组使用Jackson序列化会有类型信息问题，使用字节数组序列化更可靠
        RedisCacheConfiguration embeddingsConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofDays(7))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new FloatArrayRedisSerializer()))
                .disableCachingNullValues();
        cacheConfigurations.put("embeddings", embeddingsConfig);
        
        // intentCache缓存：意图检测结果，TTL中等（24小时），使用Jackson序列化
        cacheConfigurations.put("intentCache", defaultConfig.entryTtl(Duration.ofHours(24)));
        
        // taskPlanCache缓存：任务规划，TTL较长（24小时），使用Jackson序列化
        cacheConfigurations.put("taskPlanCache", defaultConfig.entryTtl(Duration.ofHours(24)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware() // 支持事务
                .build();
    }

    /**
     * 内存缓存管理器。
     * 用于开发环境或Redis不可用时。
     */
    private CacheManager memoryCacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        // 定义所有缓存区域
        cacheManager.setCacheNames(java.util.Arrays.asList(
                "embeddings",        // 向量嵌入缓存
                "intentCache",       // 意图检测缓存
                "taskPlanCache"      // 任务规划缓存
        ));
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }

    /**
     * 创建并配置ObjectMapper。
     * 统一配置，避免重复代码。
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return objectMapper;
    }

    /**
     * 创建Jackson序列化器。
     * 使用GenericJackson2JsonRedisSerializer，它支持在构造函数中传入ObjectMapper，
     * 避免使用已弃用的setObjectMapper方法。
     * 
     * GenericJackson2JsonRedisSerializer 是 Jackson2JsonRedisSerializer 的替代方案，
     * 提供了更好的类型支持和更灵活的配置。
     */
    private GenericJackson2JsonRedisSerializer createJacksonSerializer(ObjectMapper objectMapper) {
        // GenericJackson2JsonRedisSerializer 支持在构造函数中传入 ObjectMapper
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    /**
     * RedisTemplate配置。
     * 用于直接操作Redis（非Spring Cache场景）。
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用GenericJackson2JsonRedisSerializer序列化值
        // 使用统一的创建方法，支持在构造函数中传入ObjectMapper
        ObjectMapper objectMapper = createObjectMapper();
        GenericJackson2JsonRedisSerializer serializer = createJacksonSerializer(objectMapper);

        // 使用StringRedisSerializer序列化键
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();

        return template;
    }
}

