package cn.cug.sxy.ai.domain.rag.service.query;

import cn.cug.sxy.ai.domain.rag.model.entity.Query;
import cn.cug.sxy.ai.domain.rag.model.entity.Response;
import cn.cug.sxy.ai.domain.rag.model.valobj.GenerateParams;
import cn.cug.sxy.ai.domain.rag.model.valobj.RetrievalParams;
import cn.cug.sxy.ai.domain.rag.repository.IQueryRepository;
import cn.cug.sxy.ai.domain.rag.repository.IResponseRepository;
import cn.cug.sxy.ai.domain.rag.service.evaluate.IEvaluator;
import cn.cug.sxy.ai.domain.rag.service.generation.IGenerator;
import cn.cug.sxy.ai.domain.rag.service.retrieval.IRetriever;
import cn.cug.sxy.ai.infrastructure.embedding.IEmbeddingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/10 16:55
 * @Description 基础查询处理器实现
 * @Author jerryhotton
 */

@Slf4j
@Service("basicQueryProcessor")
public class BasicQueryProcessor implements IQueryProcessor {

    @Value("${rag.retrieval.default-top-k:5}")
    private Integer defaultTopK;

    @Value("${rag.retrieval.default-min-score:0.5}")
    private Double defaultMinScore;

    @Value("${rag.retrieval.default-index:default}")
    private String defaultIndexName;

    @Value("${rag.retrieval.default-limit:5}")
    private Integer defaultLimit;

    @Value("${rag.retrieval.default-max-contexts:6}")
    private Integer defaultMaxContexts;

    @Value("${rag.retrieval.default-per-doc-max-chunks:2}")
    private Integer defaultPerDocMaxChunks;

    @Value("${rag.retrieval.default-neighbor-window:1}")
    private Integer defaultNeighborWindow;

    @Value("${rag.retrieval.default-candidate-multiplier:4}")
    private Integer defaultCandidateMultiplier;

    @Value("${rag.retrieval.default-doc-agg:MEAN_TOP2}")
    private String defaultDocAgg;


    private final IEmbeddingService embeddingService;
    private final IRetriever retriever;
    private final IGenerator generator;
    private final IEvaluator evaluator;
    private final IQueryRepository queryRepository;
    private final IResponseRepository responseRepository;
    private final ObjectMapper objectMapper;

    public BasicQueryProcessor(
            IEmbeddingService embeddingService,
            IRetriever retriever,
            IGenerator generator,
            IEvaluator evaluator,
            IQueryRepository queryRepository,
            IResponseRepository responseRepository,
            ObjectMapper objectMapper) {
        this.embeddingService = embeddingService;
        this.retriever = retriever;
        this.generator = generator;
        this.evaluator = evaluator;
        this.queryRepository = queryRepository;
        this.responseRepository = responseRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 同步处理查询请求
     *
     * @param query 查询实体对象
     * @return 响应实体对象
     */
    @Override
    @Transactional
    public Response processQuery(Query query) {
        log.info("开始处理查询 queryId={}", query.getId());
        Instant start = Instant.now();
        try {
            // 更新查询状态为处理中
            query.setStatus("PROCESSING");
            queryRepository.updateStatus(query.getId(), "PROCESSING", null, null, null);
            // 创建响应对象
            Response response = new Response();
            response.setQueryId(query.getId());
            response.setSessionId(query.getSessionId());
            response.setStatus("PROCESSING");
            response.setCreateTime(LocalDateTime.now());
            responseRepository.save(response);
            // 1. 为查询生成向量嵌入
            log.info("为查询生成向量嵌入 queryId={}", query.getId());
            float[] queryEmbedding = embeddingService.generateEmbedding(query.getProcessedText());
            query.setVector(queryEmbedding);
            // 2.1. 检索相关上下文 设置检索参数
            log.info("检索相关上下文 queryId={}", query.getId());
            RetrievalParams retrievalParams = RetrievalParams.builder()
                    .topK(query.getMetadata().getTopK() != null ? query.getMetadata().getTopK() : defaultTopK)
                    .minScore(query.getMetadata().getMinScore() != null ? query.getMetadata().getMinScore() : defaultMinScore)
                    .indexName(query.getMetadata().getIndexName() != null ? query.getMetadata().getIndexName() : defaultIndexName)
                    .limit(defaultLimit)
                    .candidateMultiplier(defaultCandidateMultiplier)
                    .docAgg(defaultDocAgg)
                    .neighborWindow(defaultNeighborWindow)
                    .perDocMaxChunks(defaultPerDocMaxChunks)
                    .maxContexts(defaultMaxContexts)
                    .build();
            List<Map<String, Object>> retrievedContexts = retriever.retrieve(query, retrievalParams);
            // 2.2. 提取上下文文本和来源
            List<String> contextTexts = new ArrayList<>();
            List<Map<String, Object>> structuredSources = new ArrayList<>();
            for (Map<String, Object> ctx : retrievedContexts) {
                String contentPiece = (String) ctx.get("content");
                if (contentPiece != null) {
                    contextTexts.add(contentPiece);
                }
                Map<String, Object> src = new HashMap<>();
                Object docId = ctx.get("documentId");
                if (docId instanceof Number) {
                    src.put("documentId", ((Number) docId).longValue());
                } else if (docId != null) {
                    try {
                        src.put("documentId", Long.parseLong(String.valueOf(docId)));
                    } catch (Exception ignore) {
                    }
                }
                src.put("title", ctx.get("title"));
                src.put("source", ctx.get("source"));
                src.put("content", contentPiece);
                src.put("startPosition", ctx.get("startPosition"));
                src.put("endPosition", ctx.get("endPosition"));
                src.put("score", ctx.get("score"));
                structuredSources.add(src);
            }
            // 3. 生成回答
            log.info("生成回答 queryId={}", query.getId());
            GenerateParams generateParams = new GenerateParams();
            generateParams.setTemperature(0.7);
            generateParams.setMaxTokens(1024);
            String answer = generator.generate(query.getProcessedText(), contextTexts, generateParams);
            // 4. 更新响应
            response.setAnswerText(answer);
            response.setRetrievedContext(String.join("\n\n", contextTexts));
            try {
                response.setContextSources(objectMapper.writeValueAsString(structuredSources));
            } catch (JsonProcessingException e) {
                response.setContextSources("[]");
            }
            response.setModelName(generateParams.getModel());
            try {
                response.setGenerationParams(objectMapper.writeValueAsString(generateParams));
            } catch (JsonProcessingException e) {
                response.setGenerationParams("{}");
            }
            response.setCompleteTime(LocalDateTime.now());
            // 计算延迟
            long latencyMs = Duration.between(start, Instant.now()).toMillis();
            response.setLatencyMs(latencyMs);
            query.setLatencyMs(latencyMs);
            // 5. 评估响应质量
            Map<String, Object> evaluationResult = evaluator.evaluate(query, response);
            response.setFaithfulnessScore((Integer) evaluationResult.get("faithfulness"));
            response.setRelevanceScore((Integer) evaluationResult.get("relevance"));
            try {
                response.setEvaluationMetrics(objectMapper.writeValueAsString(evaluationResult));
            } catch (JsonProcessingException e) {
                response.setEvaluationMetrics("{}");
            }
            // 6. 更新状态
            response.setStatus("COMPLETED");
            query.setStatus("COMPLETED");
            // 7. 保存更新
            responseRepository.save(response);
            queryRepository.updateStatus(query.getId(), "COMPLETED", response.getCompleteTime(), latencyMs, null);
            log.info("查询处理完成: {}, 延迟: {}ms", query.getId(), latencyMs);

            return response;
        } catch (Exception e) {
            log.error("处理查询时出错 queryId: {}", query.getId(), e);
            // 更新查询和响应状态为失败
            query.setStatus("FAILED");
            query.setErrorMessage(e.getMessage());
            queryRepository.updateStatus(query.getId(), "FAILED", LocalDateTime.now(), null, e.getMessage());
            Response errorResponse = new Response();
            errorResponse.setQueryId(query.getId());
            errorResponse.setSessionId(query.getSessionId());
            errorResponse.setStatus("FAILED");
            errorResponse.setErrorMessage(e.getMessage());
            errorResponse.setCreateTime(LocalDateTime.now());
            errorResponse.setCompleteTime(LocalDateTime.now());
            responseRepository.save(errorResponse);

            return errorResponse;
        }
    }

    @Override
    public QueryType getType() {
        return QueryType.BASIC;
    }

}
