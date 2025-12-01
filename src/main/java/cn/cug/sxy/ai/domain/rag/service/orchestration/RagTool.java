package cn.cug.sxy.ai.domain.rag.service.orchestration;

import cn.cug.sxy.ai.domain.rag.model.plan.TaskExecutionContext;
import cn.cug.sxy.ai.domain.rag.model.plan.TaskNode;

/**
 * RAG 系统中的“工具”，可对应 MCP 工具、内部 RAG 调用、外部 API 等。
 */
public interface RagTool {

    /**
     * 工具名称（应与 TaskNode.toolName 对齐）。
     */
    String getName();

    /**
     * 执行工具逻辑。
     *
     * @param context 全局执行上下文
     * @param node    当前任务节点
     * @return 任意结果对象，通常会写入 context.state 中
     */
    Object execute(TaskExecutionContext context, TaskNode node) throws Exception;

}


