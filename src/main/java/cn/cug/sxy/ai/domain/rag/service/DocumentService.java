package cn.cug.sxy.ai.domain.rag.service;

import cn.cug.sxy.ai.domain.rag.model.entity.Document;
import cn.cug.sxy.ai.domain.rag.service.indexing.DocumentProcessingService;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/9 10:18
 * @Description 文档服务
 * @Author jerryhotton
 */

@Service
public class DocumentService implements IDocumentService {

    private final DocumentProcessingService documentProcessingService;

    public DocumentService(DocumentProcessingService documentProcessingService) {
        this.documentProcessingService = documentProcessingService;
    }

    @Override
    public Document processFile(String absolutePath, String originalFilename, String metadataJson) throws Exception {
        return documentProcessingService.processFile(absolutePath, originalFilename, metadataJson);
    }

    @Override
    public Document processUrl(String url, Map<String, Object> metadata) throws Exception {
        return documentProcessingService.processUrl(url, metadata);
    }

    @Override
    public Document processText(String content, String title, Map<String, Object> metadata) throws Exception {
        return documentProcessingService.processText(content, title, metadata);
    }

}
