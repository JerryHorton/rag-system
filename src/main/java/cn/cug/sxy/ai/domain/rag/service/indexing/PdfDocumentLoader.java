package cn.cug.sxy.ai.domain.rag.service.indexing;

import cn.cug.sxy.ai.domain.rag.service.parsing.HybridDocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * PDF文档加载器实现（增强版）。
 * 集成Hybrid Parser，支持智能路由：直接文本提取 → OCR降级。
 * 
 * @version 2.0
 * @Date 2025/9/8 15:20
 * @Description PDF文档加载器实现（支持OCR）
 * @Author jerryhotton
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PdfDocumentLoader implements IDocumentLoader {
    
    private final HybridDocumentParser hybridParser;

    /**
     * 从本地文件路径加载PDF文档（使用Hybrid Parser）。
     *
     * @param filePath 文件路径
     * @return 文档内容和元数据的Map，包含text和metadata两个键
     * @throws IOException 如果文件读取或解析失败
     */
    @Override
    public Map<String, Object> loadFromFile(String filePath) throws IOException {
        try {
            log.debug("使用Hybrid Parser解析PDF文件: {}", filePath);
            
            // 使用Hybrid Parser解析
            HybridDocumentParser.ParsingResult result = hybridParser.parseFromFile(filePath, "pdf");
            
            // 构建返回结果
            Map<String, Object> loadResult = new HashMap<>();
            loadResult.put("text", result.getFinalText());
            loadResult.put("metadata", result.getMetadata() != null ? result.getMetadata() : new HashMap<>());
            
            // 添加解析方法信息
            if (result.getMetadata() == null) {
                result.setMetadata(new HashMap<>());
            }
            result.getMetadata().put("parsingMethod", result.getParsingMethod());
            result.getMetadata().put("qualityScore", result.getQualityScore());
            
            // 如果有结构化文档，也保存
            if (result.getStructuredDocument() != null) {
                loadResult.put("structuredDocument", result.getStructuredDocument());
            }
            
            log.info("PDF解析完成，方法: {}, 质量分数: {}", 
                    result.getParsingMethod(), result.getQualityScore());
            
            return loadResult;
        } catch (Exception e) {
            log.error("PDF解析失败，文件: {}", filePath, e);
            throw new IOException("PDF解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从URL加载PDF文档（使用Hybrid Parser）。
     *
     * @param url 文档URL
     * @return 文档内容和元数据的Map，包含text和metadata两个键
     * @throws IOException 如果URL连接或解析失败
     */
    @Override
    public Map<String, Object> loadFromUrl(String url) throws IOException {
        try {
            log.debug("使用Hybrid Parser解析PDF URL: {}", url);
            
            HybridDocumentParser.ParsingResult result = hybridParser.parseFromUrl(url, "pdf");
            
            Map<String, Object> loadResult = new HashMap<>();
            loadResult.put("text", result.getFinalText());
            loadResult.put("metadata", result.getMetadata() != null ? result.getMetadata() : new HashMap<>());
            
            if (result.getMetadata() == null) {
                result.setMetadata(new HashMap<>());
            }
            result.getMetadata().put("parsingMethod", result.getParsingMethod());
            result.getMetadata().put("qualityScore", result.getQualityScore());
            
            if (result.getStructuredDocument() != null) {
                loadResult.put("structuredDocument", result.getStructuredDocument());
            }
            
            return loadResult;
        } catch (Exception e) {
            log.error("PDF解析失败，URL: {}", url, e);
            throw new IOException("PDF解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 判断当前加载器是否支持指定的文件类型
     *
     * @param fileExtension 文件扩展名
     * @return 如果是PDF文件(.pdf)则返回true
     */
    @Override
    public boolean supports(String fileExtension) {
        return fileExtension.equalsIgnoreCase("pdf");
    }

}
