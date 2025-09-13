package cn.cug.sxy.ai.domain.rag.service.indexing;

import cn.cug.sxy.ai.domain.rag.model.entity.Document;
import cn.cug.sxy.ai.domain.rag.model.entity.DocumentChunk;
import cn.cug.sxy.ai.domain.rag.repository.IDocumentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @version 1.0
 * @Date 2025/9/8 16:32
 * @Description 文档处理服务
 * @Author jerryhotton
 */

@Slf4j
@Service
public class DocumentProcessingService {

    private final IDocumentRepository documentRepository;
    private final DocumentLoaderFactory documentLoaderFactory;
    private final TextSplitterService textSplitterService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${rag.document.chunk-size:5000}")
    private int defaultChunkSize;

    @Value("${rag.document.chunk-overlap:400}")
    private int defaultChunkOverlap;

    /**
     * 构造函数，注入依赖
     */
    public DocumentProcessingService(IDocumentRepository documentRepository,
                                     DocumentLoaderFactory documentLoaderFactory,
                                     TextSplitterService textSplitterService,
                                     ApplicationEventPublisher eventPublisher) {
        this.documentRepository = documentRepository;
        this.documentLoaderFactory = documentLoaderFactory;
        this.textSplitterService = textSplitterService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Long processDocumentFromFile(MultipartFile file, Map<String, Object> metadata) throws IOException {
        Path tempFile = Files.createTempFile("upload-", file.getOriginalFilename());
        file.transferTo(tempFile.toFile());
        try {
            String fileName = file.getOriginalFilename();
            String fileExtension = getFileExtension(fileName);
            IDocumentLoader loader = documentLoaderFactory.getLoader(fileExtension);
            Map<String, Object> loadResult = loader.loadFromFile(tempFile.toString());
            String content = (String) loadResult.get("text");
            if (loadResult.containsKey("metadata") && loadResult.get("metadata") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> docMetadata = (Map<String, Object>) loadResult.get("metadata");
                if (metadata == null) {
                    metadata = new HashMap<>();
                }
                metadata.putAll(docMetadata);
            }
            Document document = createDocument(fileName, content, metadata);
            document.setDocumentType(fileExtension.toUpperCase());
            document.setSource(fileName);
            document.setStatus("PENDING");
            document = documentRepository.save(document);
            // 在事务提交后发布事件
            publishDocumentCreatedEventAfterCommit(document.getId());
            return document.getId();
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Transactional
    public Long processDocumentFromUrl(String url, Map<String, Object> metadata) throws IOException {
        String fileExtension = getFileExtensionFromUrl(url);
        IDocumentLoader loader = documentLoaderFactory.getLoader(fileExtension);
        Map<String, Object> loadResult = loader.loadFromUrl(url);
        String content = (String) loadResult.get("text");
        if (loadResult.containsKey("metadata") && loadResult.get("metadata") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> docMetadata = (Map<String, Object>) loadResult.get("metadata");
            if (metadata == null) {
                metadata = new HashMap<>();
            }
            metadata.putAll(docMetadata);
        }
        Document document = createDocument(url, content, metadata);
        document.setDocumentType(fileExtension.toUpperCase());
        document.setSource(url);
        document.setStatus("PENDING");
        document = documentRepository.save(document);
        publishDocumentCreatedEventAfterCommit(document.getId());
        return document.getId();
    }

    @Transactional
    public Long processDocumentFromText(String title, String content, Map<String, Object> metadata) {
        Document document = createDocument(title, content, metadata);
        document.setDocumentType("TEXT");
        document.setSource("DIRECT_INPUT");
        document.setStatus("PENDING");
        document = documentRepository.save(document);
        publishDocumentCreatedEventAfterCommit(document.getId());
        return document.getId();
    }

    @Transactional
    public List<Long> batchProcessDocuments(List<Document> documents) {
        String batchId = UUID.randomUUID().toString();
        List<Long> documentIds = new ArrayList<>();
        for (Document document : documents) {
            document.setBatchId(batchId);
            document.setStatus("PENDING");
            document.setCreateTime(LocalDateTime.now());
            document.setUpdateTime(LocalDateTime.now());
        }
        documentRepository.saveAll(documents);
        for (Document document : documents) {
            documentIds.add(document.getId());
            publishDocumentCreatedEventAfterCommit(document.getId());
        }
        return documentIds;
    }

    private void publishDocumentCreatedEventAfterCommit(Long documentId) {
        eventPublisher.publishEvent(new DocumentProcessingService.DocumentCreatedEvent(this, documentId));
    }

    /**
     * 分割文档为文档块（供事件处理器调用）
     */
    List<DocumentChunk> splitDocument(Document document) {
        String content = document.getContent();
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> textChunks = textSplitterService.splitText(content, defaultChunkSize, defaultChunkOverlap);
        List<DocumentChunk> documentChunks = new ArrayList<>();
        int chunkIndex = 0;
        int startPosition = 0;
        for (String chunk : textChunks) {
            int endPosition = startPosition + chunk.length();
            DocumentChunk documentChunk = new DocumentChunk();
            documentChunk.setDocumentId(document.getId());
            documentChunk.setContent(chunk);
            documentChunk.setStartPosition(startPosition);
            documentChunk.setEndPosition(endPosition);
            documentChunk.setChunkIndex(chunkIndex);
            documentChunk.setCreateTime(LocalDateTime.now());
            documentChunk.setUpdateTime(LocalDateTime.now());
            documentChunk.setVectorized(false);
            documentChunk.setOverlapLength(chunkIndex > 0 ? defaultChunkOverlap : 0);
            double qualityScore = calculateQualityScore(chunk);
            documentChunk.setQualityScore(qualityScore);
            if (document.getMetadata() != null) {
                documentChunk.setMetadata(document.getMetadata());
            }
            documentChunks.add(documentChunk);
            startPosition = endPosition - defaultChunkOverlap;
            chunkIndex++;
        }
        return documentChunks;
    }

    private Document createDocument(String title, String content, Map<String, Object> metadata) {
        Document document = new Document();
        document.setTitle(title);
        document.setContent(content);
        document.setCreateTime(LocalDateTime.now());
        document.setUpdateTime(LocalDateTime.now());
        document.setVectorized(false);
        if (metadata != null) {
            document.setMetadata(metadata);
        } else {
            document.setMetadata(new HashMap<>());
        }
        return document;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty() || !fileName.contains(".")) {
            return "txt";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private String getFileExtensionFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "html";
        }
        String cleanUrl = url;
        if (url.contains("?")) {
            cleanUrl = url.substring(0, url.indexOf("?"));
        }
        if (cleanUrl.contains(".")) {
            String ext = cleanUrl.substring(cleanUrl.lastIndexOf(".") + 1).toLowerCase();
            if (ext.contains("/")) {
                return "html";
            }
            return ext;
        }
        return "html";
    }

    private double calculateQualityScore(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }
        double lengthScore = Math.min(1.0, text.length() / (double) defaultChunkSize);
        double specialCharsRatio = text.chars()
                .filter(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c))
                .count() / (double) text.length();
        double keywordDensity = 0.5;
        return lengthScore * 0.5 + (1.0 - specialCharsRatio) * 0.3 + keywordDensity * 0.2;
    }

    @Getter
    public static class DocumentCreatedEvent extends java.util.EventObject {

        private final Long documentId;

        public DocumentCreatedEvent(Object source, Long documentId) {
            super(source);
            this.documentId = documentId;
        }

    }

    /**
     * 处理已保存到本地的临时文件（控制器已完成保存），metadata为JSON字符串
     */
    @Transactional
    public Document processFile(String absolutePath, String originalFilename, String metadataJson) throws IOException {
        Map<String, Object> metadata = parseMetadataJson(metadataJson);
        String fileExtension = getFileExtension(originalFilename);
        IDocumentLoader loader = documentLoaderFactory.getLoader(fileExtension);
        Map<String, Object> loadResult = loader.loadFromFile(absolutePath);
        String content = (String) loadResult.get("text");
        if (loadResult.containsKey("metadata") && loadResult.get("metadata") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> docMetadata = (Map<String, Object>) loadResult.get("metadata");
            if (metadata == null) {
                metadata = new HashMap<>();
            }
            metadata.putAll(docMetadata);
        }
        Document document = createDocument(originalFilename, content, metadata);
        document.setDocumentType(fileExtension.toUpperCase());
        document.setSource(originalFilename);
        document.setStatus("PENDING");
        document = documentRepository.save(document);
        publishDocumentCreatedEventAfterCommit(document.getId());

        return document;
    }

    /**
     * 处理URL（返回Document以匹配控制器）
     */
    @Transactional
    public Document processUrl(String url, Map<String, Object> metadata) throws IOException {
        String fileExtension = getFileExtensionFromUrl(url);
        IDocumentLoader loader = documentLoaderFactory.getLoader(fileExtension);
        Map<String, Object> loadResult = loader.loadFromUrl(url);
        String content = (String) loadResult.get("text");
        if (loadResult.containsKey("metadata") && loadResult.get("metadata") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> docMetadata = (Map<String, Object>) loadResult.get("metadata");
            if (metadata == null) {
                metadata = new HashMap<>();
            }
            metadata.putAll(docMetadata);
        }
        Document document = createDocument(url, content, metadata);
        document.setDocumentType(fileExtension.toUpperCase());
        document.setSource(url);
        document.setStatus("PENDING");
        document = documentRepository.save(document);
        publishDocumentCreatedEventAfterCommit(document.getId());
        return document;
    }

    /**
     * 处理文本（返回Document以匹配控制器；参数顺序与控制器一致：content, title）
     */
    @Transactional
    public Document processText(String content, String title, Map<String, Object> metadata) {
        Document document = createDocument(title, content, metadata);
        document.setDocumentType("TEXT");
        document.setSource("DIRECT_INPUT");
        document.setStatus("PENDING");
        document = documentRepository.save(document);
        publishDocumentCreatedEventAfterCommit(document.getId());
        return document;
    }

    private Map<String, Object> parseMetadataJson(String metadataJson) {
        if (metadataJson == null || metadataJson.isEmpty()) {
            return new HashMap<>();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(metadataJson, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("metadata解析失败，使用空对象，json={}", metadataJson, e);
            return new HashMap<>();
        }
    }

}
