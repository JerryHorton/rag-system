package cn.cug.sxy.ai.domain.document.service.indexing;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/8 15:20
 * @Description PDF文档加载器实现
 * @Author jerryhotton
 */

@Component
public class PdfDocumentLoader implements IDocumentLoader {

    /**
     * 从本地文件路径加载PDF文档
     *
     * @param filePath 文件路径
     * @return 文档内容和元数据的Map，包含text和metadata两个键
     * @throws IOException 如果文件读取或解析失败
     */
    @Override
    public Map<String, Object> loadFromFile(String filePath) throws IOException {
        File file = new File(filePath);
        try (PDDocument document = Loader.loadPDF(file)) {
            return extractContentAndMetadata(document, filePath);
        }
    }

    /**
     * 从URL加载PDF文档
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
            RandomAccessReadBuffer buffer = new RandomAccessReadBuffer(is);
            try (PDDocument document = Loader.loadPDF(buffer)) {
                return extractContentAndMetadata(document, url);
            }
        }
    }

    /**
     * 从PDDocument对象中提取内容和元数据
     *
     * @param document PDFBox文档对象
     * @param source 文档来源（文件路径或URL）
     * @return 包含文本内容和元数据的Map
     * @throws IOException 如果文本提取失败
     */
    private Map<String, Object> extractContentAndMetadata(PDDocument document, String source) throws IOException {
        Map<String, Object> result = new HashMap<>();
        // 提取文本内容
        PDFTextStripper textStripper = new PDFTextStripper();
        textStripper.setSortByPosition(true);
        String text = textStripper.getText(document);
        result.put("text", text);
        // 提取元数据
        Map<String, Object> metadata = new HashMap<>();
        PDDocumentInformation info = document.getDocumentInformation();
        metadata.put("title", info.getTitle());
        metadata.put("author", info.getAuthor());
        metadata.put("subject", info.getSubject());
        metadata.put("keywords", info.getKeywords());
        metadata.put("creator", info.getCreator());
        metadata.put("producer", info.getProducer());
        Calendar creationDate = info.getCreationDate();
        if (creationDate != null) {
            metadata.put("creationDate", creationDate.getTime());
        }
        Calendar modificationDate = info.getModificationDate();
        if (modificationDate != null) {
            metadata.put("modificationDate", modificationDate.getTime());
        }
        // 文档属性
        metadata.put("pageCount", document.getNumberOfPages());
        metadata.put("isEncrypted", document.isEncrypted());
        metadata.put("version", document.getVersion());
        // 如果有来源信息，添加到元数据
        if (source != null) {
            String fileName = source;
            if (source.contains("/")) {
                fileName = source.substring(source.lastIndexOf('/') + 1);
            }
            metadata.put("source", source);
            metadata.put("fileName", fileName);
        }

        result.put("metadata", metadata);
        return result;
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
