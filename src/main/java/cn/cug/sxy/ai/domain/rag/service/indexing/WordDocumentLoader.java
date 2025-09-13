package cn.cug.sxy.ai.domain.rag.service.indexing;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ooxml.util.PackageHelper;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.filesystem.FileMagic;
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
     */
    private Map<String, Object> extractContentAndMetadata(InputStream inputStream, String source) throws IOException {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> metadata = new HashMap<>();
        String text;

        // 使用 FileMagic 嗅探文件类型，避免以扩展名判断
        try (InputStream magicIn = FileMagic.prepareToCheckMagic(inputStream)) {
            FileMagic fm = FileMagic.valueOf(magicIn);
            if (fm == FileMagic.OOXML) {
                // .docx → XWPF
                try (XWPFDocument document = new XWPFDocument(magicIn);
                     XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                    text = extractor.getText();
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
                    metadata.put("format", "DOCX");
                } catch (Exception e) {
                    throw new IOException("解析DOCX失败: " + e.getMessage(), e);
                }
            } else if (fm == FileMagic.OLE2) {
                // .doc → HWPF
                try (HWPFDocument doc = new HWPFDocument(magicIn);
                     WordExtractor extractor = new WordExtractor(doc)) {
                    text = extractor.getText();
                    metadata.put("format", "DOC");
                } catch (Exception e) {
                    throw new IOException("解析DOC失败: " + e.getMessage(), e);
                }
            } else {
                throw new IOException("不支持的Word文件格式");
            }
        }
        // 通用元数据
        if (source != null) {
            String fileName = source;
            if (source.contains("/")) {
                fileName = source.substring(source.lastIndexOf('/') + 1);
            }
            metadata.put("source", source);
            metadata.put("fileName", fileName);
        }
        result.put("text", text);
        result.put("metadata", metadata);

        return result;
    }

    @Override
    public boolean supports(String fileExtension) {
        return fileExtension.equalsIgnoreCase("doc") ||
                fileExtension.equalsIgnoreCase("docx");
    }

}
