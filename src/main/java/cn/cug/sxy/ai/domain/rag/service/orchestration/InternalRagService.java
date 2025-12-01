package cn.cug.sxy.ai.domain.rag.service.orchestration;

import cn.cug.sxy.ai.domain.rag.model.entity.Query;
import cn.cug.sxy.ai.domain.rag.model.entity.Response;
import cn.cug.sxy.ai.domain.rag.model.strategy.QueryStrategy;
import cn.cug.sxy.ai.domain.rag.service.query.IQueryProcessor;
import cn.cug.sxy.ai.domain.rag.service.query.QueryType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 内部 RAG 检索服务，统一封装系统内部的知识检索逻辑。
 * 供 RagQueryTool 和单处理器流程复用，确保逻辑统一。
 */
@Slf4j
@Service
public class InternalRagService {

    private final Map<QueryType, IQueryProcessor> processorMap;
    private final String defaultProcessor;

    public InternalRagService(List<IQueryProcessor> processors,
                              @Value("${rag.intent.default-processor:BASIC}") String defaultProcessor) {
        this.processorMap = processors.stream()
                .collect(Collectors.toMap(IQueryProcessor::getType, p -> p));
        this.defaultProcessor = defaultProcessor;
        log.info("InternalRagService 初始化完成，已注册处理器: {}", processorMap.keySet());
    }

    /**
     * 执行内部 RAG 检索。
     * 这是系统核心的知识检索功能，从内部知识库中检索并生成答案。
     *
     * @param query    查询对象
     * @param strategy 查询策略（如果为 null，使用默认策略）
     * @return 响应对象
     */
    public Response executeRagQuery(Query query, QueryStrategy strategy) {
        if (query == null) {
            throw new IllegalArgumentException("Query 不能为 null");
        }
        
        // 如果没有提供策略，使用默认处理器
        QueryType processorType = strategy != null 
                ? strategy.getProcessorType() 
                : QueryType.valueOf(defaultProcessor);
        
        IQueryProcessor processor = processorMap.getOrDefault(
                processorType,
                processorMap.getOrDefault(
                        QueryType.valueOf(defaultProcessor),
                        processorMap.values().stream().findFirst()
                                .orElseThrow(() -> new IllegalStateException("没有可用的 QueryProcessor"))
                )
        );
        
        log.info("InternalRagService 执行检索: queryId={}, processorType={}", 
                query.getId(), processorType);
        
        // 执行查询
        Response response = strategy != null 
                ? processor.processQuery(query, strategy)
                : processor.processQuery(query);
        
        log.debug("InternalRagService 检索完成: queryId={}, responseStatus={}", 
                query.getId(), response != null ? response.getStatus() : "null");
        
        return response;
    }
}

