package cn.cug.sxy.ai.domain.rag.service.parsing;

import java.io.IOException;
import java.util.Map;

/**
 * 文本提取器接口。
 * 用于直接从文档中提取文本（无需OCR）。
 * 
 * @author jerryhotton
 */
public interface TextExtractor {
    
    /**
     * 从文件提取文本。
     * 
     * @param filePath 文件路径
     * @return 提取结果，包含text和metadata
     * @throws IOException 提取失败
     */
    ExtractionResult extractFromFile(String filePath) throws IOException;
    
    /**
     * 从URL提取文本。
     * 
     * @param url 文档URL
     * @return 提取结果
     * @throws IOException 提取失败
     */
    ExtractionResult extractFromUrl(String url) throws IOException;
    
    /**
     * 检查是否支持该文件类型。
     * 
     * @param fileExtension 文件扩展名
     * @return 是否支持
     */
    boolean supports(String fileExtension);
    
    /**
     * 提取结果
     */
    class ExtractionResult {
        private final String text;
        private final Map<String, Object> metadata;
        private final boolean highQuality;
        
        public ExtractionResult(String text, Map<String, Object> metadata, boolean highQuality) {
            this.text = text;
            this.metadata = metadata;
            this.highQuality = highQuality;
        }
        
        public String getText() {
            return text;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        public boolean isHighQuality() {
            return highQuality;
        }
    }
}

