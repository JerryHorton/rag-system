package cn.cug.sxy.ai.domain.rag.service;

import cn.cug.sxy.ai.domain.rag.model.entity.Query;
import cn.cug.sxy.ai.domain.rag.model.entity.Response;
import cn.cug.sxy.ai.domain.rag.model.intent.QueryIntent;
import cn.cug.sxy.ai.domain.rag.model.strategy.QueryStrategy;
import cn.cug.sxy.ai.domain.rag.model.valobj.QueryParams;
import cn.cug.sxy.ai.domain.rag.repository.IQueryRepository;
import cn.cug.sxy.ai.domain.rag.service.intent.ClarificationService;
import cn.cug.sxy.ai.domain.rag.service.intent.IntentDetectionService;
import cn.cug.sxy.ai.domain.rag.service.intent.IntentDetector.IntentRequest;
import cn.cug.sxy.ai.domain.rag.service.intent.StrategyMapper;
import cn.cug.sxy.ai.domain.rag.service.intent.QueryRewriteService;
import cn.cug.sxy.ai.domain.rag.service.orchestration.FallbackManager;
import cn.cug.sxy.ai.domain.rag.service.orchestration.InternalRagService;
import cn.cug.sxy.ai.domain.rag.service.orchestration.TaskOrchestrator;
import cn.cug.sxy.ai.domain.rag.service.query.IQueryProcessor;
import cn.cug.sxy.ai.domain.rag.service.query.QueryType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @Date 2025/9/12 09:54
 * @Description RAG业务流程编排服务实现
 * @Author jerryhotton
 */

@Slf4j
@Service
public class RagOrchestrationService implements IRagOrchestrationService {

    private final Map<QueryType, IQueryProcessor> processorMap;
    private final IQueryRepository queryRepository;
    private final IntentDetectionService intentDetectionService;
    private final StrategyMapper strategyMapper;
    private final QueryRewriteService queryRewriteService;
    private final ClarificationService clarificationService;
    private final TaskOrchestrator taskOrchestrator;
    private final FallbackManager fallbackManager;
    private final InternalRagService internalRagService;

    public RagOrchestrationService(
            List<IQueryProcessor> queryProcessors,
            IQueryRepository queryRepository,
            IntentDetectionService intentDetectionService,
            StrategyMapper strategyMapper,
            QueryRewriteService queryRewriteService,
            ClarificationService clarificationService,
            TaskOrchestrator taskOrchestrator,
            FallbackManager fallbackManager,
            InternalRagService internalRagService) {
        this.processorMap = queryProcessors.stream()
                .collect(Collectors.toMap(IQueryProcessor::getType, processor -> processor));
        log.info("已注册{}个查询处理器: {}", processorMap.size(),
                processorMap.keySet().stream()
                        .map(QueryType::name)
                        .collect(Collectors.joining(", ")));
        this.queryRepository = queryRepository;
        this.intentDetectionService = intentDetectionService;
        this.strategyMapper = strategyMapper;
        this.queryRewriteService = queryRewriteService;
        this.clarificationService = clarificationService;
        this.taskOrchestrator = taskOrchestrator;
        this.fallbackManager = fallbackManager;
        this.internalRagService = internalRagService;
    }

    @Override
    public Response processQuery(String queryText, String userId, String sessionId) {
        return processQuery(queryText, userId, sessionId, null);
    }

    @Override
    public Response processQuery(String queryText, String userId, String sessionId, QueryParams params) {
        log.info("处理查询请求: {}, 参数: {}", queryText, params);
        // 1. 创建查询实体
        Query query = createQuery(queryText, userId, sessionId);
        QueryParams safeParams = Optional.ofNullable(params).orElseGet(QueryParams::new);
        // 2. 意图检测
        QueryIntent intent = intentDetectionService.detect(new IntentRequest(queryText, userId, sessionId, safeParams));
        if (intent.isRequiresClarification()) {
            String clarification = clarificationService.buildClarification(query, intent);
            return buildClarificationResponse(query, clarification);
        }
        // 3. 策略映射
        QueryStrategy strategy = strategyMapper.map(intent, safeParams);
        // 4. 查询重构（Step-back / Multi-query / HyDE 等）
        queryRewriteService.rewrite(query, intent, strategy);
        query.setQueryType(strategy.getProcessorType().name());
        query.setRouteTarget(strategy.getRetrieval().getRouter());
        query.setMetadata(safeParams);
        log.info("查询[{}] intent={}, processor={}", query.getId(), intent.getTaskType(), strategy.getProcessorType());
        // 5. 如果存在 TaskPlan，则走 TaskOrchestrator 多工具执行流程
        //    TaskPlan 中的任务可以调用 RAG_QUERY 工具（内部检索）或其他工具（MCP等）
        if (intent.getTaskPlan() != null && intent.getTaskPlan().hasTasks()) {
            Object result = taskOrchestrator.executePlan(intent.getTaskPlan(), query, strategy);
            if (result instanceof Response response) {
                return response;
            }
            // 如果 TaskPlan 执行后没有返回 Response，尝试降级处理
            log.warn("TaskPlan 执行完成但未返回 Response，降级到单处理器流程");
        }
        
        // 6. 默认单处理器流程（简单查询，无需多工具协作）
        //    使用统一的 InternalRagService 执行内部检索，与 TaskPlan 中的 RAG_QUERY 工具逻辑一致
        log.info("执行单处理器流程（内部 RAG 检索）: queryId={}", query.getId());
        Response response = internalRagService.executeRagQuery(query, strategy);
        
        // 7. Fallback 降级：如果结果不理想，尝试调整策略重试
        QueryStrategy adjusted = fallbackManager.maybeAdjustStrategy(query, response, strategy);
        if (adjusted != strategy) {
            log.info("策略降级调整: {} -> {}", strategy.getProcessorType(), adjusted.getProcessorType());
            response = internalRagService.executeRagQuery(query, adjusted);
        }
        
        return response;
    }

    /**
     * 创建并保存查询实体
     *
     * @param queryText 原始查询文本
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @return 查询实体
     */
    private Query createQuery(String queryText, String userId, String sessionId) {
        // 创建新的查询实体
        Query query = new Query();
        query.setOriginalText(queryText);
        query.setProcessedText(queryText); // 初始时与原始文本相同，后续可进行预处理
        query.setUserId(userId);
        query.setSessionId(sessionId);
        query.setCreateTime(LocalDateTime.now());
        query.setStatus("CREATED");
        // 保存查询
        queryRepository.save(query);
        log.debug("创建查询 queryId={}", query.getId());

        return query;
    }

    private Response buildClarificationResponse(Query query, String clarification) {
        Response response = new Response();
        response.setQueryId(query.getId());
        response.setSessionId(query.getSessionId());
        response.setStatus("CLARIFY");
        response.setCreateTime(LocalDateTime.now());
        response.setCompleteTime(LocalDateTime.now());
        response.setAnswerText(clarification);
        return response;
    }
}
