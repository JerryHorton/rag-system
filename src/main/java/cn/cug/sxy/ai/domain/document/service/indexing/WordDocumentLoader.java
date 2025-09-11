package cn.cug.sxy.ai.domain.document.service.indexing;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/8 16:12
 * @Description Word文档加载器实现
 * @Author jerryhotton
 */

@Component
public class WordDocumentLoader implements IDocumentLoader {

    /**
     * 从本地文件路径加载Word文档
     *
     * @param filePath 文件路径
     * @return 文档内容和元数据的Map，包含text和metadata两个键
     * @throws IOException 如果文件读取或解析失败
     */
    @Override
    public Map<String, Object> loadFromFile(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(new File(filePath))) {
            return extractContentAndMetadata(fis, filePath);
        }
    }

    /**
     * 从URL加载Word文档
     *
     * @param url 文档URL
     * @return 文档内容和元数据的Map，包含text和metadata两个键
     * @throws IOException 如果URL连接或解析失败
     */
    @Override
    public Map<String, Object> loadFromUrl(String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        try (InputStream is = connection.getInputStream()) {
            return extractContentAndMetadata(is, url);
        }
    }

    /**
     * 从输入流中提取Word文档的文本内容和元数据
     *
     * @param inputStream 文档输入流
     * @param source      文档来源（文件路径或URL）
     * @return 包含文本内容和元数据的Map
     * @throws IOException 如果解析失败
     */
    private Map<String, Object> extractContentAndMetadata(InputStream inputStream, String source) throws IOException {
        Map<String, Object> result = new HashMap<>();
        String text;
        Map<String, Object> metadata = new HashMap<>();
        // 根据文件扩展名判断Word文档类型
        boolean isDocx = source.toLowerCase().endsWith(".docx");
        // 在POI 5.4.0中，XWPFDocument可以处理.doc和.docx格式
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            text = extractor.getText();
            // 提取元数据
            if (document.getProperties() != null && document.getProperties().getCoreProperties() != null) {
                metadata.put("title", document.getProperties().getCoreProperties().getTitle());
                metadata.put("creator", document.getProperties().getCoreProperties().getCreator());
                metadata.put("description", document.getProperties().getCoreProperties().getDescription());
                metadata.put("subject", document.getProperties().getCoreProperties().getSubject());
                metadata.put("keywords", document.getProperties().getCoreProperties().getKeywords());
                metadata.put("category", document.getProperties().getCoreProperties().getCategory());
                metadata.put("created", document.getProperties().getCoreProperties().getCreated());
                metadata.put("modified", document.getProperties().getCoreProperties().getModified());
            }
            // 文档格式
            metadata.put("format", isDocx ? "DOCX" : "DOC");
        }
        // 添加元数据
        String fileName = source;
        if (source.contains("/")) {
            fileName = source.substring(source.lastIndexOf('/') + 1);
        }
        metadata.put("source", source);
        metadata.put("fileName", fileName);

        result.put("text", text);
        result.put("metadata", metadata);
        return result;
    }

    /**
     * 判断当前加载器是否支持指定的文件类型
     *
     * @param fileExtension 文件扩展名
     * @return 如果是Word文件(.doc, .docx)则返回true
     */
    @Override
    public boolean supports(String fileExtension) {
        return fileExtension.equalsIgnoreCase("doc") ||
                fileExtension.equalsIgnoreCase("docx");
    }

}
