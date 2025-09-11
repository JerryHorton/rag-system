package cn.cug.sxy.ai.domain.document.service.query;

import cn.cug.sxy.ai.domain.document.model.entity.Query;
import cn.cug.sxy.ai.domain.document.model.entity.Response;
import cn.cug.sxy.ai.domain.document.model.valobj.RetrievalParams;
import cn.cug.sxy.ai.domain.document.repository.IQueryRepository;
import cn.cug.sxy.ai.domain.document.repository.IResponseRepository;
import cn.cug.sxy.ai.domain.document.service.retrieval.IRetriever;
import cn.cug.sxy.ai.infrastructure.embedding.IEmbeddingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @version 1.0
 * @Date 2025/9/10 16:55
 * @Description 基础查询处理器实现
 * @Author jerryhotton
 */

@Slf4j
public class BasicQueryProcessor implements IQueryProcessor {

    private final IEmbeddingService embeddingService;
    private final IRetriever retriever;
    private final Generator generator;
    private final Evaluator evaluator;
    private final IQueryRepository queryRepository;
    private final IResponseRepository responseRepository;
    private final ObjectMapper objectMapper;

    public BasicQueryProcessor(
            IEmbeddingService embeddingService,
            IRetriever retriever,
            Generator generator,
            Evaluator evaluator,
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
        log.info("开始处理查询: {}", query.getId());
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
            log.debug("为查询生成向量嵌入: {}", query.getId());
            float[] queryEmbedding = embeddingService.generateEmbedding(query.getProcessedText());
            query.setVector(queryEmbedding);
            // 2. 检索相关上下文
            log.debug("检索相关上下文: {}", query.getId());
            RetrievalParams retrievalParams = new RetrievalParams();
            retrievalParams.setTopK(5);
            retrievalParams.setMinScore(0.7);
            List<Map<String, Object>> retrievedContexts = retriever.retrieve(query, retrievalParams);
            // 提取上下文文本和来源
            List<String> contextTexts = new ArrayList<>();
            List<String> contextSources = new ArrayList<>();
            for (Map<String, Object> context : retrievedContexts) {
                contextTexts.add((String) context.get("content"));
                contextSources.add((String) context.get("source"));
            }
            // 3. 生成回答
            log.debug("生成回答: {}", query.getId());
            Map<String, Object> generationParams = new HashMap<>();
            generationParams.put("model", "gpt-3.5-turbo");
            generationParams.put("temperature", 0.7);
            generationParams.put("maxTokens", 1024);

            String answer = generator.generate(query.getProcessedText(), contextTexts, generationParams);
            // 4. 更新响应
            response.setAnswerText(answer);
            response.setRetrievedContext(String.join("\n\n", contextTexts));
            try {
                response.setContextSources(objectMapper.writeValueAsString(contextSources));
            } catch (JsonProcessingException e) {
                response.setContextSources("[]");
            }
            response.setModelName((String) generationParams.get("model"));
            try {
                response.setGenerationParams(objectMapper.writeValueAsString(generationParams));
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
            response.setFaithfulnessScore((Double) evaluationResult.get("faithfulness"));
            response.setRelevanceScore((Double) evaluationResult.get("relevance"));
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
            log.error("处理查询时出错: " + query.getId(), e);
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

    /**
     * 异步处理查询请求
     *
     * @param query 查询实体对象
     * @return 包含响应的CompletableFuture
     */
    @Override
    public CompletableFuture<Response> processQueryAsync(Query query) {
        return CompletableFuture.supplyAsync(() -> processQuery(query));
    }

    /**
     * 处理器描述
     *
     * @return 处理器的描述信息
     */
    @Override
    public String getDescription() {
        return "基础RAG查询处理器，提供简单的检索增强生成功能";
    }

}
