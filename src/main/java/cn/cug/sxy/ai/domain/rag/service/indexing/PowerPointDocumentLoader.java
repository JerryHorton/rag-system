package cn.cug.sxy.ai.domain.rag.service.indexing;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/8 15:40
 * @Description PowerPoint文档加载器实现
 * @Author jerryhotton
 */

@Component
public class PowerPointDocumentLoader implements IDocumentLoader {

    /**
     * 从本地文件路径加载PowerPoint文档
     *
     * @param filePath 文件路径
     * @return 文档内容和元数据的Map，包含text和metadata两个键
     * @throws IOException 如果文件读取或解析失败
     */
    @Override
    public Map<String, Object> loadFromFile(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            return extractContentAndMetadata(fis, filePath);
        }
    }

    /**
     * 从URL加载PowerPoint文档
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
     * 从输入流中提取PowerPoint文档的文本内容和元数据
     *
     * @param inputStream 文档输入流
     * @param source      文档来源（文件路径或URL）
     * @return 包含文本内容和元数据的Map
     * @throws IOException 如果解析失败
     */
    private Map<String, Object> extractContentAndMetadata(InputStream inputStream, String source) throws IOException {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> metadata = new HashMap<>();
        StringBuilder contentBuilder = new StringBuilder();
        // 根据文件扩展名判断PowerPoint文档类型
        boolean isPptx = source.toLowerCase().endsWith(".pptx");
        String format = isPptx ? "PPTX" : "PPT";
        try (XMLSlideShow ppt = new XMLSlideShow(inputStream)) {
            metadata.put("format", format);
            metadata.put("slideCount", ppt.getSlides().size());
            metadata.put("pageSize", Map.of(
                    "width", ppt.getPageSize().getWidth(),
                    "height", ppt.getPageSize().getHeight()
            ));
            // 提取幻灯片内容
            for (int i = 0; i < ppt.getSlides().size(); i++) {
                XSLFSlide slide = ppt.getSlides().get(i);
                contentBuilder.append("===== 幻灯片 ").append(i + 1).append(" =====\n");
                // 提取所有形状中的文本
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape) {
                        XSLFTextShape textShape = (XSLFTextShape) shape;
                        String text = textShape.getText();
                        if (text != null && !text.trim().isEmpty()) {
                            contentBuilder.append(text).append("\n");
                        }
                    }
                }
                contentBuilder.append("\n");
            }
            // 提取文档属性
            if (ppt.getProperties() != null && ppt.getProperties().getCoreProperties() != null) {
                metadata.put("title", ppt.getProperties().getCoreProperties().getTitle());
                metadata.put("creator", ppt.getProperties().getCoreProperties().getCreator());
                metadata.put("description", ppt.getProperties().getCoreProperties().getDescription());
                metadata.put("subject", ppt.getProperties().getCoreProperties().getSubject());
                metadata.put("keywords", ppt.getProperties().getCoreProperties().getKeywords());
                metadata.put("category", ppt.getProperties().getCoreProperties().getCategory());
                metadata.put("created", ppt.getProperties().getCoreProperties().getCreated());
                metadata.put("modified", ppt.getProperties().getCoreProperties().getModified());
            }
        }
        // 如果有来源信息，添加到元数据
        String fileName = source;
        if (source.contains("/")) {
            fileName = source.substring(source.lastIndexOf('/') + 1);
        }
        metadata.put("source", source);
        metadata.put("fileName", fileName);
        result.put("text", contentBuilder.toString());
        result.put("metadata", metadata);

        return result;
    }

    /**
     * 判断当前加载器是否支持指定的文件类型
     *
     * @param fileExtension 文件扩展名
     * @return 如果是PowerPoint文件(.ppt, .pptx)则返回true
     */
    @Override
    public boolean supports(String fileExtension) {
        return fileExtension.equalsIgnoreCase("ppt") ||
                fileExtension.equalsIgnoreCase("pptx");
    }

}
