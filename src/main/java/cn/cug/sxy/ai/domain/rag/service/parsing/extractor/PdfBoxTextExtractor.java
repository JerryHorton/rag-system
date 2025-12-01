package cn.cug.sxy.ai.domain.rag.service.parsing.extractor;

import cn.cug.sxy.ai.domain.rag.service.parsing.TextExtractor;
import lombok.extern.slf4j.Slf4j;
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
 * PDFBox文本提取器。
 * 用于直接从PDF提取文本（预检阶段）。
 * 
 * @author jerryhotton
 */
@Slf4j
@Component
public class PdfBoxTextExtractor implements TextExtractor {
    
    @Override
    public ExtractionResult extractFromFile(String filePath) throws IOException {
        File file = new File(filePath);
        try (PDDocument document = Loader.loadPDF(file)) {
            return extractContentAndMetadata(document, filePath);
        }
    }
    
    @Override
    public ExtractionResult extractFromUrl(String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        try (InputStream is = connection.getInputStream()) {
            RandomAccessReadBuffer buffer = new RandomAccessReadBuffer(is);
            try (PDDocument document = Loader.loadPDF(buffer)) {
                return extractContentAndMetadata(document, url);
            }
        }
    }
    
    private ExtractionResult extractContentAndMetadata(PDDocument document, String source) throws IOException {
        // 提取文本内容
        PDFTextStripper textStripper = new PDFTextStripper();
        textStripper.setSortByPosition(true);
        String text = textStripper.getText(document);
        
        // 提取元数据
        Map<String, Object> metadata = new HashMap<>();
        PDDocumentInformation info = document.getDocumentInformation();
        if (info != null) {
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
        }
        
        // 文档属性
        metadata.put("pageCount", document.getNumberOfPages());
        metadata.put("isEncrypted", document.isEncrypted());
        metadata.put("version", document.getVersion());
        
        // 来源信息
        if (source != null) {
            String fileName = source;
            if (source.contains("/")) {
                fileName = source.substring(source.lastIndexOf('/') + 1);
            }
            metadata.put("source", source);
            metadata.put("fileName", fileName);
        }
        
        // 判断文本质量：如果文本长度合理且不为空，认为是高质量
        boolean highQuality = text != null && text.length() > 50;
        
        return new ExtractionResult(text, metadata, highQuality);
    }
    
    @Override
    public boolean supports(String fileExtension) {
        return "pdf".equalsIgnoreCase(fileExtension);
    }
}

