package cn.cug.sxy.ai.domain.document.service.indexing;

import cn.cug.sxy.ai.domain.document.model.entity.DocumentChunk;
import cn.cug.sxy.ai.domain.document.model.entity.Vector;
import cn.cug.sxy.ai.domain.document.model.valobj.SpaceType;
import cn.cug.sxy.ai.domain.document.model.valobj.VectorCategory;
import cn.cug.sxy.ai.domain.document.model.valobj.VectorType;
import cn.cug.sxy.ai.domain.document.repository.IDocumentChunkRepository;
import cn.cug.sxy.ai.domain.document.repository.IDocumentRepository;
import cn.cug.sxy.ai.domain.document.repository.IVectorRepository;
import cn.cug.sxy.ai.infrastructure.embedding.IEmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @Date 2025/9/8 17:43
 * @Description 文档向量化服务
 * @Author jerryhotton
 */

@Slf4j
@Service
public class DocumentVectorizationService {

    private final IDocumentRepository documentRepository;
    private final IDocumentChunkRepository documentChunkRepository;
    private final IVectorRepository vectorRepository;
    private final IEmbeddingService embeddingService;
    private final DocumentVectorizationService self;

    /**
     * 构造函数，注入依赖
     */
    public DocumentVectorizationService(IDocumentRepository documentRepository,
                                        IDocumentChunkRepository documentChunkRepository,
                                        IVectorRepository vectorRepository,
                                        IEmbeddingService embeddingService,
                                        @Lazy DocumentVectorizationService self) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.vectorRepository = vectorRepository;
        this.embeddingService = embeddingService;
        this.self = self;
    }

    @Value("${spring.ai.embedding.options.dimensions:1536}")
    private int embeddingDimensions;

    @Value("${spring.ai.embedding.options.model:text-embedding-ada-002}")
    private String embeddingModel;

    @Value("${rag.vectorization.batch-size:10}")
    private int batchSize;

    @Value("${rag.vectorization.index-name:default}")
    private String defaultIndexName;

    /**
     * 异步向量化一组文档片段
     *
     * @param chunks 待向量化的文档片段列表
     * @return 异步任务
     */
    @Async("vectorizationExecutor")
    @Retryable(retryFor = {RuntimeException.class}, maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2.0, maxDelay = 8000, random = true))
    public CompletableFuture<Void> vectorizeDocumentChunksAsync(List<DocumentChunk> chunks) {
        try {
            self.vectorizeDocumentChunks(chunks);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Error vectorizing document chunks: {}", e.getMessage(), e);
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(new RuntimeException("Failed to vectorize document chunks", e));
            return failed;
        }
    }

    /**
     * 向量化一组文档片段
     *
     * @param chunks 待向量化的文档片段列表
     */
    @Transactional
    public void vectorizeDocumentChunks(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            log.warn("No chunks to vectorize");
            return;
        }
        log.info("Vectorizing {} document chunks", chunks.size());
        // 分批处理
        for (int i = 0; i < chunks.size(); i += batchSize) {
            int end = Math.min(i + batchSize, chunks.size());
            List<DocumentChunk> batch = chunks.subList(i, end);
            try {
                // 提取文本内容
                List<String> texts = batch.stream()
                        .map(DocumentChunk::getContent)
                        .collect(Collectors.toList());
                // 生成向量嵌入
                List<float[]> embeddings = embeddingService.generateEmbeddings(texts);
                // 更新文档片段和创建向量记录
                for (int j = 0; j < batch.size(); j++) {
                    DocumentChunk chunk = batch.get(j);
                    float[] embedding = embeddings.get(j);
                    // 幂等保护：已向量化的块跳过
                    if (Boolean.TRUE.equals(chunk.getVectorized())) {
                        continue;
                    }
                    // 计算向量范式（欧几里得范数）
                    double norm = calculateVectorNorm(embedding);
                    // 创建向量实体
                    Vector vector = createVector(chunk, embedding, norm);
                    // 幂等保护：外部ID唯一避免重复
                    Vector existing = null;
                    try {
                        existing = vectorRepository.findByExternalId(vector.getExternalId()).orElse(null);
                    } catch (Exception ignore) {
                    }
                    if (existing != null) {
                        updateChunkVectorStatus(chunk, existing.getId());
                        continue;
                    }
                    vectorRepository.save(vector);
                    // 更新文档片段状态
                    updateChunkVectorStatus(chunk, vector.getId());
                }
                log.debug("Vectorized batch of {} chunks", batch.size());
            } catch (Exception e) {
                log.error("Error vectorizing chunk batch: {}", e.getMessage(), e);
                // 标记失败的文档片段
                for (DocumentChunk chunk : batch) {
                    if (!Boolean.TRUE.equals(chunk.getVectorized())) {
                        chunk.setVectorized(false);
                        documentChunkRepository.save(chunk);
                    }
                }
            }
        }
        if (documentChunkRepository.countNonVectorizedByDocumentId(chunks.get(0).getDocumentId()) == 0) {
            // 标记文档为已向量化
            documentRepository.updateVectorized(chunks.get(0).getDocumentId(), true);
            // 状态推进到INDEXED
            documentRepository.updateStatus(chunks.get(0).getDocumentId(), "INDEXED", null);
        }

        log.info("Completed vectorization of {} document chunks", chunks.size());
    }

    /**
     * 异步处理未向量化的文档片段
     *
     * @param limit 处理的最大数量
     * @return 处理的片段数量
     */
    @Async("vectorizationExecutor")
    @Transactional
    @Retryable(retryFor = {RuntimeException.class}, maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2.0, maxDelay = 8000, random = true))
    public CompletableFuture<Integer> processNonVectorizedChunksAsync(int limit) {
        try {
            // 查询未向量化的片段
            List<DocumentChunk> chunks = documentChunkRepository.findNonVectorizedChunks(limit);
            if (chunks.isEmpty()) {
                return CompletableFuture.completedFuture(0);
            }
            // 向量化片段
            vectorizeDocumentChunks(chunks);
            return CompletableFuture.completedFuture(chunks.size());
        } catch (Exception e) {
            log.error("Error processing non-vectorized chunks: {}", e.getMessage(), e);
            CompletableFuture<Integer> failed = new CompletableFuture<>();
            failed.completeExceptionally(new RuntimeException("Failed to process non-vectorized chunks", e));
            return failed;
        }
    }

    @Recover
    public CompletableFuture<Void> recoverVectorize(RuntimeException e, List<DocumentChunk> chunks) {
        log.error("Vectorization permanently failed after retries. chunks={} error={}",
                chunks != null ? chunks.size() : 0, e.getMessage(), e);
        return CompletableFuture.failedFuture(e);
    }

    @Recover
    public CompletableFuture<Integer> recoverProcessNonVectorized(RuntimeException e, int limit) {
        log.error("Process non-vectorized permanently failed after retries. limit={} error={}", limit, e.getMessage(), e);
        return CompletableFuture.completedFuture(0);
    }

    /**
     * 创建向量实体
     *
     * @param chunk     文档片段
     * @param embedding 向量嵌入
     * @param norm      向量范式
     * @return 向量实体
     */
    private Vector createVector(DocumentChunk chunk, float[] embedding, double norm) {
        Vector vector = new Vector();
        vector.setExternalId("chunk-" + chunk.getId());
        vector.setVectorType(VectorType.CHUNK);
        vector.setEmbedding(embedding);
        vector.setDimensions(embedding.length);
        vector.setIndexName(defaultIndexName);
        vector.setCreateTime(LocalDateTime.now());
        vector.setUpdateTime(LocalDateTime.now());
        // 元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("documentId", chunk.getDocumentId());
        metadata.put("chunkId", chunk.getId());
        metadata.put("chunkIndex", chunk.getChunkIndex());
        if (chunk.getMetadata() != null) {
            metadata.putAll(chunk.getMetadata());
        }
        vector.setMetadata(metadata);
        // 嵌入信息
        vector.setEmbeddingModel(embeddingModel);
        vector.setVectorNorm(norm);
        vector.setVectorCategory(VectorCategory.TEXT);
        // 摘要
        String summary = chunk.getContent().length() > 100
                ? chunk.getContent().substring(0, 100) + "..."
                : chunk.getContent();
        vector.setContentSummary(summary);
        // 空间类型
        vector.setSpaceType(SpaceType.COSINE);
        vector.setIsPrimary(true);

        return vector;
    }

    /**
     * 更新文档片段的向量化状态
     *
     * @param chunk    文档片段
     * @param vectorId 向量ID
     */
    private void updateChunkVectorStatus(DocumentChunk chunk, Long vectorId) {
        chunk.setVectorized(true);
        chunk.setVectorId(vectorId); // 注意：这里使用统一的vectorId字段名
        chunk.setUpdateTime(LocalDateTime.now());
        documentChunkRepository.updateVectorized(
                chunk.getId(),
                true,
                vectorId);
        // 保存更新后的文档块
        documentChunkRepository.save(chunk);
    }

    /**
     * 计算向量的欧几里得范数
     *
     * @param vector 向量
     * @return 范数
     */
    private double calculateVectorNorm(float[] vector) {
        double sum = 0.0;
        for (float v : vector) {
            sum += v * v;
        }
        return Math.sqrt(sum);
    }

    /**
     * 将float数组转换为byte数组
     *
     * @param floatArray float数组
     * @return byte数组
     */
    private byte[] convertFloatArrayToByteArray(float[] floatArray) {
        byte[] byteArray = new byte[floatArray.length * 4];
        for (int i = 0; i < floatArray.length; i++) {
            int floatBits = Float.floatToIntBits(floatArray[i]);
            byteArray[i * 4] = (byte) (floatBits >> 24);
            byteArray[i * 4 + 1] = (byte) (floatBits >> 16);
            byteArray[i * 4 + 2] = (byte) (floatBits >> 8);
            byteArray[i * 4 + 3] = (byte) floatBits;
        }
        return byteArray;
    }

}
