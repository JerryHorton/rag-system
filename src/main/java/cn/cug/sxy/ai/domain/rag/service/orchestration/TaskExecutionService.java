package cn.cug.sxy.ai.domain.rag.service.orchestration;

import cn.cug.sxy.ai.domain.rag.model.plan.TaskExecutionContext;
import cn.cug.sxy.ai.domain.rag.model.plan.TaskNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 使用 ChatClient 自动执行任务节点，让 Spring AI 根据任务描述自动选择和使用工具。
 * 支持 MCP 工具和所有已注册到 Spring AI 的工具。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskExecutionService {

    private final ChatClient chatClient;

    /**
     * 执行任务节点，使用 ChatClient 自动选择合适的工具。
     * ChatClient 会自动调用已注册的工具（包括 MCP 工具）来完成任务。
     *
     * @param node    任务节点（包含任务描述）
     * @param context 执行上下文（包含依赖任务的结果）
     * @return 执行结果
     */
    public Object executeTaskNode(TaskNode node, TaskExecutionContext context) {
        log.info("使用 ChatClient 自动执行任务节点: step={}, description={}", node.getStep(), node.getDescription());
        
        // 构建系统提示，指导 LLM 如何使用工具
        String systemPrompt = buildSystemPrompt(context);
        
        // 构建用户提示，包含任务描述和依赖结果
        String userPrompt = buildUserPrompt(node, context);
        
        // 使用 ChatClient 执行，它会自动选择合适的工具
        // Spring AI 会自动处理工具调用，包括 MCP 工具
        List<Message> messages = List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt)
        );
        
        try {
            // 使用 ChatClient 执行任务，Spring AI 会自动调用已注册的工具（包括 MCP 工具）
            // ChatClient 会根据任务描述自动选择合适的工具并调用
            String result = chatClient.prompt(new Prompt(messages))
                    .call()
                    .content();
            
            log.info("任务节点执行完成: step={}, resultLength={}", node.getStep(), 
                    result != null ? result.length() : 0);
            
            // 将结果保存到上下文，供后续步骤使用
            String resultKey = "STEP_" + node.getStep();
            context.recordResult(resultKey, result);
            
            // 同时也保存到通用的结果键中
            if (node.getStep() == 1) {
                context.put("firstResult", result);
            }
            
            return result;
        } catch (Exception e) {
            log.error("任务节点执行失败: step={}, error={}", node.getStep(), e.getMessage(), e);
            throw new RuntimeException("任务执行失败: " + node.getDescription(), e);
        }
    }

    /**
     * 构建系统提示，说明可用的工具和如何执行任务。
     */
    private String buildSystemPrompt(TaskExecutionContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个智能任务执行助手。系统已为你配置了多种工具，尤其包含本系统内置的 RAG 检索能力，用于读取企业知识库；同时也连接了 MCP 等外部工具，可在需要时调用外部服务。\n\n");
        sb.append("执行指南：\n");
        sb.append("1. 仔细阅读任务描述，理解需要完成的目标\n");
        sb.append("2. 优先使用本系统内置的 RAG 检索工具处理与内部知识相关的任务\n");
        sb.append("3. 当任务需要外部数据（如实时信息、天气、网页内容）时，再选择合适的外部工具\n");
        sb.append("3. 如果需要多个工具协作，按顺序调用相关工具\n");
        sb.append("4. 如果任务依赖于其他步骤的结果，请使用上下文中提供的信息\n");
        sb.append("5. 完成任务后，提供清晰、准确的答案\n\n");
        sb.append("注意：系统会自动处理工具调用，你只需要在需要时使用工具即可。");
        return sb.toString();
    }

    /**
     * 构建用户提示，包含任务描述和依赖结果。
     */
    private String buildUserPrompt(TaskNode node, TaskExecutionContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("任务描述: ").append(node.getDescription()).append("\n\n");
        
        // 添加依赖步骤的结果
        if (!node.safeDependencies().isEmpty()) {
            sb.append("依赖步骤的结果：\n");
            for (Integer depStep : node.safeDependencies()) {
                String depKey = "STEP_" + depStep;
                Object depResult = context.get(depKey);
                if (depResult != null) {
                    sb.append("步骤 ").append(depStep).append(": ").append(depResult.toString()).append("\n");
                }
            }
            sb.append("\n");
        }
        
        // 添加查询信息
        if (context.getQuery() != null) {
            sb.append("原始查询: ").append(context.getQuery().getOriginalText()).append("\n");
        }
        
        sb.append("\n请执行上述任务，并使用合适的工具获取所需信息。");
        
        return sb.toString();
    }
}

