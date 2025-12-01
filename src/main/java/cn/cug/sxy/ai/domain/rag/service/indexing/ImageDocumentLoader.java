package cn.cug.sxy.ai.domain.rag.service.indexing;

import cn.cug.sxy.ai.domain.rag.model.parsing.StructuredDocument;
import cn.cug.sxy.ai.domain.rag.service.parsing.OcrService;
import cn.cug.sxy.ai.domain.rag.service.parsing.OcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 图片文档加载器。
 * 支持直接对图片文件（PNG、JPEG、TIFF等）进行OCR识别。
 * 
 * 技术指南要求：
 * "企业80%最有价值的知识，都沉睡在这些图文混排、格式复杂的PDF、扫描件中。"
 * 
 * 支持的图片格式：
 * - PNG
 * - JPEG/JPG
 * - TIFF/TIF
 * - BMP
 * - WebP
 * - GIF
 * 
 * @author jerryhotton
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageDocumentLoader implements IDocumentLoader {
    
    private final OcrService ocrService;
    
    // 支持的图片扩展名
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "tiff", "tif", "bmp", "webp", "gif"
    );
    
    @Override
    public Map<String, Object> loadFromFile(String filePath) throws IOException {
        log.info("开始加载图片文件进行OCR识别: {}", filePath);
        
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("图片文件不存在: " + filePath);
        }
        
        // 读取图片文件
        byte[] imageBytes = Files.readAllBytes(path);
        log.debug("图片文件读取成功，大小: {} bytes", imageBytes.length);
        
        return processImage(imageBytes, filePath);
    }
    
    @Override
    public Map<String, Object> loadFromUrl(String url) throws IOException {
        log.info("开始从URL加载图片进行OCR识别: {}", url);
        
        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(60000);
        
        try (InputStream is = connection.getInputStream()) {
            byte[] imageBytes = is.readAllBytes();
            log.debug("图片URL读取成功，大小: {} bytes", imageBytes.length);
            return processImage(imageBytes, url);
        }
    }
    
    @Override
    public boolean supports(String fileExtension) {
        return fileExtension != null && 
               SUPPORTED_EXTENSIONS.contains(fileExtension.toLowerCase());
    }
    
    /**
     * 处理图片并进行OCR识别。
     */
    private Map<String, Object> processImage(byte[] imageBytes, String source) throws IOException {
        try {
            long startTime = System.currentTimeMillis();
            
            // 调用OCR服务进行识别
            StructuredDocument document = ocrService.recognize(imageBytes);
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            // 构建结果
            Map<String, Object> result = new HashMap<>();
            
            // 获取文本内容
            String text = document != null ? document.toMarkdown() : "";
            result.put("text", text);
            
            // 构建元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", source);
            metadata.put("parsingMethod", "ocr");
            metadata.put("processingTimeMs", processingTime);
            metadata.put("imageSizeBytes", imageBytes.length);
            
            if (document != null) {
                StructuredDocument.DocumentStats stats = document.getStats();
                metadata.put("pageCount", stats.getPageCount());
                metadata.put("elementCount", stats.getTotalElements());
                metadata.put("tableCount", stats.getTableCount());
                metadata.put("averageConfidence", stats.getAverageConfidence());
                metadata.put("estimatedTokens", stats.getEstimatedTokens());
                
                // 保存结构化文档
                result.put("structuredDocument", document);
            }
            
            result.put("metadata", metadata);
            
            log.info("图片OCR识别完成，来源: {}, 处理时间: {}ms, 文本长度: {}", 
                    source, processingTime, text.length());
            
            return result;
            
        } catch (OcrService.OcrException e) {
            log.error("图片OCR识别失败: {}", source, e);
            throw new IOException("图片OCR识别失败: " + e.getMessage(), e);
        }
    }
}

