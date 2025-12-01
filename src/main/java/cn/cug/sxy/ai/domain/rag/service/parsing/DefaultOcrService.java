package cn.cug.sxy.ai.domain.rag.service.parsing;

import cn.cug.sxy.ai.domain.rag.model.parsing.StructuredDocument;
import cn.cug.sxy.ai.domain.rag.service.parsing.ocr.OcrProvider;
import cn.cug.sxy.ai.domain.rag.service.parsing.ocr.OcrProvider.OcrProviderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OCR服务实现（门面模式）。
 * 
 * 职责：
 * - 管理多个 OcrProvider
 * - 实现降级策略：优先级高的Provider失败后，自动切换到下一个
 * - 实现重试策略：可重试的异常会进行重试
 * - 提供统一的日志和监控
 * 
 * 设计模式：
 * - 门面模式：对外提供简单接口，隐藏内部复杂性
 * - 策略模式：多个Provider可以随时切换
 * - 责任链模式：按优先级依次尝试各Provider
 *
 * @author jerryhotton
 */
@Slf4j
@Service
public class DefaultOcrService implements OcrService {
    
    private static final String SERVICE_NAME = "OcrService";
    
    private final List<OcrProvider> providers;
    
    @Value("${rag.ocr.max-retries:2}")
    private int maxRetries;
    
    @Value("${rag.ocr.retry-delay-ms:1000}")
    private long retryDelayMs;

    /**
     * 构造函数。
     * Spring自动注入所有OcrProvider实现，按优先级排序。
     */
    public DefaultOcrService(List<OcrProvider> providers) {
        // 按优先级排序（数字越小优先级越高）
        this.providers = providers.stream()
                .sorted(Comparator.comparingInt(OcrProvider::getPriority))
                .collect(Collectors.toList());
        
        log.info("OCR服务初始化完成，已注册 {} 个Provider:", this.providers.size());
        for (OcrProvider provider : this.providers) {
            log.info("  - {} (优先级: {}, 可用: {})", 
                    provider.getProviderName(), 
                    provider.getPriority(),
                    provider.isAvailable());
        }
        
        if (this.providers.isEmpty() || this.providers.stream().noneMatch(OcrProvider::isAvailable)) {
            log.warn("警告：没有可用的OCR Provider，OCR功能将不可用");
        }
    }

    @Override
    public StructuredDocument recognize(byte[] imageBytes) throws OcrException {
        if (providers.isEmpty()) {
            throw new OcrException("没有配置任何OCR Provider");
        }
        
        List<OcrProvider> availableProviders = providers.stream()
                .filter(OcrProvider::isAvailable)
                .toList();
        
        if (availableProviders.isEmpty()) {
            throw new OcrException("没有可用的OCR Provider，请检查配置");
        }
        
        OcrProviderException lastException = null;
        
        // 按优先级尝试各个Provider
        for (OcrProvider provider : availableProviders) {
            try {
                log.debug("尝试使用Provider: {}", provider.getProviderName());
                
                // 带重试的调用
                StructuredDocument result = recognizeWithRetry(provider, imageBytes);
                
                log.info("OCR识别成功，使用Provider: {}", provider.getProviderName());
                return result;
                
            } catch (OcrProviderException e) {
                log.warn("Provider {} 失败: {}", provider.getProviderName(), e.getMessage());
                lastException = e;
                
                // 如果是不可重试的错误（如配置错误），直接跳到下一个Provider
                // 如果是可重试的错误，已经在recognizeWithRetry中处理过了
            }
        }
        
        // 所有Provider都失败
        String errorMsg = "所有OCR Provider均失败";
        if (lastException != null) {
            errorMsg += "，最后一个错误: " + lastException.getMessage();
        }
        throw new OcrException(errorMsg, lastException);
    }
    
    /**
     * 带重试的识别调用
     */
    private StructuredDocument recognizeWithRetry(OcrProvider provider, byte[] imageBytes) 
            throws OcrProviderException {
        
        OcrProviderException lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return provider.recognize(imageBytes);
            } catch (OcrProviderException e) {
                lastException = e;
                
                // 如果不可重试，直接抛出
                if (!e.isRetryable()) {
                    throw e;
                }
                
                // 如果还有重试机会
                if (attempt < maxRetries) {
                    log.warn("Provider {} 第 {} 次尝试失败，{}ms后重试: {}", 
                            provider.getProviderName(), attempt, retryDelayMs, e.getMessage());
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new OcrProviderException(provider.getProviderName(), "重试被中断", ie);
                    }
                }
            }
        }
        
        // 重试次数用尽
        throw lastException != null ? lastException : 
                new OcrProviderException(provider.getProviderName(), "未知错误");
    }

    @Override
    public boolean isAvailable() {
        return providers.stream().anyMatch(OcrProvider::isAvailable);
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public int getActiveProviderCount() {
        return (int) providers.stream().filter(OcrProvider::isAvailable).count();
    }
}

