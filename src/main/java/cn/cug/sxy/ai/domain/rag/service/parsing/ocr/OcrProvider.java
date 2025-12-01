package cn.cug.sxy.ai.domain.rag.service.parsing.ocr;

import cn.cug.sxy.ai.domain.rag.model.parsing.StructuredDocument;

/**
 * OCR提供者接口（底层）。
 * 
 * 职责：定义具体OCR引擎的能力抽象
 * 实现者：AliOcrProvider, TencentOcrProvider, GoogleOcrProvider 等
 * 
 * 设计说明：
 * - 这是底层接口，不直接暴露给业务层
 * - 具体实现只负责调用OCR API，不处理降级、重试等策略
 * - 由上层的 OcrService 统一管理和调度
 * 
 * @author jerryhotton
 */
public interface OcrProvider {
    
    /**
     * 对图像进行OCR识别。
     * 
     * @param imageBytes 图像字节数组
     * @return 结构化文档
     * @throws OcrProviderException OCR处理异常
     */
    StructuredDocument recognize(byte[] imageBytes) throws OcrProviderException;
    
    /**
     * 获取提供者名称。
     * 
     * @return 提供者名称（如 "阿里云DashScope", "腾讯云OCR" 等）
     */
    String getProviderName();
    
    /**
     * 获取提供者优先级（数字越小优先级越高）。
     * 
     * @return 优先级
     */
    default int getPriority() {
        return 100;
    }
    
    /**
     * 检查提供者是否可用。
     * 
     * @return 是否可用
     */
    boolean isAvailable();
    
    /**
     * 获取提供者类型。
     * 
     * @return 提供者类型
     */
    default ProviderType getProviderType() {
        return ProviderType.CLOUD_API;
    }
    
    /**
     * 提供者类型枚举
     */
    enum ProviderType {
        CLOUD_API,      // 云API（如阿里云、腾讯云）
        LOCAL_MODEL,    // 本地模型（如PaddleOCR）
        HYBRID          // 混合
    }
    
    /**
     * OCR提供者异常
     */
    class OcrProviderException extends Exception {
        private final String providerName;
        private final boolean retryable;
        
        public OcrProviderException(String providerName, String message) {
            super(message);
            this.providerName = providerName;
            this.retryable = false;
        }
        
        public OcrProviderException(String providerName, String message, Throwable cause) {
            super(message, cause);
            this.providerName = providerName;
            this.retryable = false;
        }
        
        public OcrProviderException(String providerName, String message, boolean retryable) {
            super(message);
            this.providerName = providerName;
            this.retryable = retryable;
        }
        
        public OcrProviderException(String providerName, String message, Throwable cause, boolean retryable) {
            super(message, cause);
            this.providerName = providerName;
            this.retryable = retryable;
        }
        
        public String getProviderName() {
            return providerName;
        }
        
        public boolean isRetryable() {
            return retryable;
        }
    }
}
