package cn.cug.sxy.ai.domain.rag.service.orchestration;

import cn.cug.sxy.ai.domain.rag.model.entity.Query;
import cn.cug.sxy.ai.domain.rag.model.entity.Response;
import cn.cug.sxy.ai.domain.rag.model.plan.TaskExecutionContext;
import cn.cug.sxy.ai.domain.rag.model.plan.TaskNode;
import cn.cug.sxy.ai.domain.rag.model.strategy.QueryStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 内部 RAG 检索工具，供 TaskPlan 调用。
 * 封装了系统内部的知识检索功能，从内部知识库中检索并生成答案。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagQueryTool implements RagTool {

    public static final String TOOL_NAME = "RAG_QUERY";

    private final InternalRagService internalRagService;

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public Object execute(TaskExecutionContext context, TaskNode node) {
        Query query = context.getQuery();
        QueryStrategy strategy = context.get("strategy");
        
        log.info("RagQueryTool 执行内部 RAG 检索: queryId={}, step={}", 
                query.getId(), node.getStep());
        
        // 使用统一的内部 RAG 服务执行检索
        Response response = internalRagService.executeRagQuery(query, strategy);
        
        // 保存结果到上下文，供后续步骤使用
        context.setLastResponse(response);
        context.put("RAG_RESULT", response);
        context.recordResult("RAG_RESULT", response);
        
        log.debug("RagQueryTool 检索完成: queryId={}, responseStatus={}", 
                query.getId(), response != null ? response.getStatus() : "null");
        
        return response;
    }

}


