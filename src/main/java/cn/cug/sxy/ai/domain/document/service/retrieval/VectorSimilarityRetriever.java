package cn.cug.sxy.ai.domain.document.service.retrieval;

import cn.cug.sxy.ai.domain.document.model.entity.DocumentChunk;
import cn.cug.sxy.ai.domain.document.model.entity.Query;
import cn.cug.sxy.ai.domain.document.model.valobj.RetrievalParams;
import cn.cug.sxy.ai.infrastructure.embedding.IEmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/10 17:46
 * @Description 基于向量相似度的检索器实现
 * @Author jerryhotton
 */

@Slf4j
@Service("vectorSimilarityRetriever")
public class VectorSimilarityRetriever implements IRetriever {

    private final IEmbeddingService embeddingService;

    public VectorSimilarityRetriever(IEmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @Value("${rag.retrieval.default-limit:5}")
    private int defaultLimit;

    @Value("${rag.retrieval.default-min-score:0.7}")
    private double defaultMinScore;

    @Override
    public List<Map<String, Object>> retrieve(Query query, RetrievalParams params) {
        log.info("执行向量相似度检索（Map结果），查询ID: {}", query.getId());
        int topK = params.getTopK() != null ? params.getTopK() : defaultLimit;
        double minScore = params.getMinScore() != null ? params.getMinScore() : defaultMinScore;
        // 获取或生成查询向量
        float[] vector = query.getVector();
        if (vector == null || vector.length == 0) {
            vector = embeddingService.generateEmbedding(query.getProcessedText());
            query.setVectorData(queryVector);
        }
        return List.of();
    }

    @Override
    public double getSimilarityScore(Query query, DocumentChunk chunk) {
        return 0;
    }

}
