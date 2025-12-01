package cn.cug.sxy.ai.domain.rag.service.parsing;

import cn.cug.sxy.ai.domain.rag.model.parsing.StructuredDocument;

import java.util.List;

/**
 * OCR服务接口（上层）。
 * 
 * 职责：
 * - 提供统一的OCR能力给业务层
 * - 管理多个OCR Provider
 * - 实现降级、重试等策略
 * 
 * 设计说明：
 * - 这是面向业务层的接口
 * - 具体实现（OcrServiceImpl）负责调度底层的 OcrProvider
 * - 业务代码只依赖这个接口，不感知具体的 Provider
 * 
 * @author jerryhotton
 */
public interface OcrService {
    
    /**
     * 对图像进行OCR识别。
     * 
     * @param imageBytes 图像字节数组
     * @return 结构化文档
     * @throws OcrException OCR处理异常
     */
    StructuredDocument recognize(byte[] imageBytes) throws OcrException;
    
    /**
     * 批量OCR识别。
     * 
     * @param imageBytesList 图像字节数组列表
     * @return 结构化文档列表
     * @throws OcrException OCR处理异常
     */
    default List<StructuredDocument> recognizeBatch(List<byte[]> imageBytesList) throws OcrException {
        return imageBytesList.stream()
                .map(imageBytes -> {
                    try {
                        return recognize(imageBytes);
                    } catch (OcrException e) {
                        throw new RuntimeException("批量OCR处理失败", e);
                    }
                })
                .toList();
    }
    
    /**
     * 检查服务是否可用。
     * 
     * @return 是否可用
     */
    boolean isAvailable();
    
    /**
     * 获取服务名称（用于日志和监控）。
     * 
     * @return 服务名称
     */
    String getServiceName();
    
    /**
     * 获取当前活跃的Provider数量。
     * 
     * @return Provider数量
     */
    int getActiveProviderCount();
    
    /**
     * OCR服务异常
     */
    class OcrException extends Exception {
        private final boolean retryable;
        
        public OcrException(String message) {
            super(message);
            this.retryable = false;
        }
        
        public OcrException(String message, Throwable cause) {
            super(message, cause);
            this.retryable = false;
        }
        
        public OcrException(String message, boolean retryable) {
            super(message);
            this.retryable = retryable;
        }
        
        public OcrException(String message, Throwable cause, boolean retryable) {
            super(message, cause);
            this.retryable = retryable;
        }
        
        public boolean isRetryable() {
            return retryable;
        }
    }
}
