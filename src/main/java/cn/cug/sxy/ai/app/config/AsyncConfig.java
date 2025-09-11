package cn.cug.sxy.ai.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * @version 1.0
 * @Date 2025/9/9 09:47
 * @Description 异步配置
 * @Author jerryhotton
 */

@Configuration
public class AsyncConfig {

    @Bean(name = "documentProcessingExecutor")
    public Executor documentProcessingExecutor(
            @Value("${rag.async.document.core-pool-size:4}") int corePoolSize,
            @Value("${rag.async.document.max-pool-size:16}") int maxPoolSize,
            @Value("${rag.async.document.queue-capacity:200}") int queueCapacity,
            @Value("${rag.async.document.keep-alive-seconds:60}") int keepAliveSeconds) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("doc-post-processor-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Bean(name = "vectorizationExecutor")
    public Executor vectorizationExecutor(
            @Value("${rag.async.vector.core-pool-size:8}") int corePoolSize,
            @Value("${rag.async.vector.max-pool-size:32}") int maxPoolSize,
            @Value("${rag.async.vector.queue-capacity:500}") int queueCapacity,
            @Value("${rag.async.vector.keep-alive-seconds:120}") int keepAliveSeconds) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("vectorizer-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

}
