package cn.cug.sxy.ai.domain.rag.service;

import cn.cug.sxy.ai.domain.rag.model.entity.Query;
import cn.cug.sxy.ai.domain.rag.model.entity.Response;
import cn.cug.sxy.ai.domain.rag.model.valobj.QueryParams;
import cn.cug.sxy.ai.domain.rag.repository.IQueryRepository;
import cn.cug.sxy.ai.domain.rag.service.query.IQueryProcessor;
import cn.cug.sxy.ai.domain.rag.service.routing.IQueryRouter;
import cn.cug.sxy.ai.domain.rag.service.routing.RouterType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

    private final Map<RouterType, IQueryRouter> queryRouterMap;
    private final IQueryRepository queryRepository;

    @Value("${rag.routing.default-router-type:RULE_BASED}")
    private String defaultRouterKey;

    public RagOrchestrationService(
            List<IQueryRouter> queryRouters,
            IQueryRepository queryRepository) {
        this.queryRouterMap = queryRouters.stream()
                .collect(Collectors.toMap(IQueryRouter::getType, router -> router));
        log.info("已注册{}个查询路由: {}", queryRouterMap.size(),
                queryRouterMap.keySet().stream()
                        .map(RouterType::name)
                        .collect(Collectors.joining(", ")));
        this.queryRepository = queryRepository;
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
        RouterType routerType = params != null ? RouterType.valueOf(params.getRouter()) : RouterType.valueOf(defaultRouterKey);
        // 2. 路由到合适的处理器
        IQueryProcessor processor = queryRouterMap.get(routerType).route(query);
        // 3. 设置额外参数
        query.setMetadata(params);
        log.info("查询[{}]被路由到处理器: {}", query.getId(), processor.getType());
        // 4. 处理查询并返回响应
        return processor.processQuery(query);
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

}
