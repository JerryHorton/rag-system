package cn.cug.sxy.ai.domain.document.service.indexing;

import cn.cug.sxy.ai.domain.document.model.entity.Document;
import cn.cug.sxy.ai.domain.document.model.entity.DocumentChunk;
import cn.cug.sxy.ai.domain.document.repository.IDocumentChunkRepository;
import cn.cug.sxy.ai.domain.document.repository.IDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Optional;

/**
 * @version 1.0
 * @Date 2025/9/9 09:54
 * @Description 文档后置处理器
 * @Author jerryhotton
 */

@Slf4j
@Component
public class DocumentPostProcessor {

    private final IDocumentRepository documentRepository;
    private final IDocumentChunkRepository documentChunkRepository;
    private final DocumentProcessingService documentProcessingService;
    private final DocumentVectorizationService documentVectorizationService;

    public DocumentPostProcessor(IDocumentRepository documentRepository,
                                 IDocumentChunkRepository documentChunkRepository,
                                 DocumentProcessingService documentProcessingService,
                                 DocumentVectorizationService documentVectorizationService) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.documentProcessingService = documentProcessingService;
        this.documentVectorizationService = documentVectorizationService;
    }

    @Async("documentProcessingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(retryFor = {RuntimeException.class}, maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2.0, maxDelay = 5000, random = true))
    public void onDocumentCreated(DocumentProcessingService.DocumentCreatedEvent event) {
        Long documentId = event.getDocumentId();
        try {
            Optional<Document> opt = documentRepository.findById(documentId);
            if (!opt.isPresent()) {
                log.warn("Document id={} not found when processing event.", documentId);
                return;
            }
            Document document = opt.get();
            // 幂等保护：如果已是INDEXED或FAILED则不重复处理
            if ("INDEXED".equalsIgnoreCase(document.getStatus()) || "FAILED".equalsIgnoreCase(document.getStatus())) {
                log.info("Document id={} already in terminal status: {}", documentId, document.getStatus());
                return;
            }
            // 状态推进到PROCESSING（如果当前不是）
            if (!"PROCESSING".equalsIgnoreCase(document.getStatus())) {
                documentRepository.updateStatus(documentId, "PROCESSING", null);
            }
            // 分块
            List<DocumentChunk> chunks = documentProcessingService.splitDocument(document);
            if (!chunks.isEmpty()) {
                // 幂等保护：若已有该documentId的块，避免重复保存
                List<DocumentChunk> existing = documentChunkRepository.findByDocumentId(documentId);
                if (existing == null || existing.isEmpty()) {
                    documentChunkRepository.saveAll(chunks);
                } else {
                    log.info("Document id={} chunks already exist ({}). Skip saving.", documentId, existing.size());
                    chunks = existing;
                }
                // 异步向量化（向量化内部再做幂等）
                documentVectorizationService.vectorizeDocumentChunksAsync(chunks);
            } else {
                // 无分块也标记为已索引（幂等）
                if (!"INDEXED".equalsIgnoreCase(document.getStatus())) {
                    documentRepository.updateStatus(documentId, "INDEXED", null);
                }
            }
        } catch (Exception e) {
            log.error("Post process document id={} failed: {}", documentId, e.getMessage(), e);
            documentRepository.updateStatus(documentId, "FAILED", e.getMessage());
            throw new RuntimeException("Document post processing failed", e);
        }
    }

    @Recover
    public void recoverOnDocumentCreated(RuntimeException e, DocumentProcessingService.DocumentCreatedEvent event) {
        Long documentId = event.getDocumentId();
        log.error("Document post processing permanently failed after retries. documentId={} error={}", documentId, e.getMessage(), e);
        documentRepository.updateStatus(documentId, "FAILED", e.getMessage());
    }

}
