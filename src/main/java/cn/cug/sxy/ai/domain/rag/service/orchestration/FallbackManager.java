package cn.cug.sxy.ai.domain.rag.service.orchestration;

import cn.cug.sxy.ai.domain.rag.model.entity.Query;
import cn.cug.sxy.ai.domain.rag.model.entity.Response;
import cn.cug.sxy.ai.domain.rag.model.intent.QueryIntent;
import cn.cug.sxy.ai.domain.rag.model.plan.TaskExecutionContext;
import cn.cug.sxy.ai.domain.rag.model.strategy.QueryStrategy;
import cn.cug.sxy.ai.domain.rag.model.strategy.EvaluationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 负责出现错误或低质量结果时的重试与降级策略。
 * 实现三级降级机制：
 * 第一级：工具级重试/切换
 * 第二级：任务级澄清
 * 第三级：系统级致歉
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FallbackManager {

    private final ToolRegistry toolRegistry;

    @Value("${rag.fallback.max-tool-retries:2}")
    private int maxToolRetries;

    @Value("${rag.fallback.max-task-retries:1}")
    private int maxTaskRetries;

    @Value("${rag.fallback.enable-clarification:true}")
    private boolean enableClarification;
    
    @Value("${rag.fallback.prefer-rag-on-failure:true}")
    private boolean preferRagOnFailure;

    /**
     * 根据响应质量调整策略（第一级降级：策略调整）。
     */
    public QueryStrategy maybeAdjustStrategy(Query query, Response response, QueryStrategy original) {
        EvaluationStrategy eval = original.getEvaluation();
        if (eval == null || !eval.isEnabled()) {
            return original;
        }
        Integer faith = response.getFaithfulnessScore();
        Integer rel = response.getRelevanceScore();
        if (faith == null || rel == null) {
            return original;
        }
        double f = faith / 10.0;
        double r = rel / 10.0;
        if ((f < eval.getMinFaithfulness() || r < eval.getMinRelevance()) && eval.isAllowRetry()) {
            log.warn("查询 {} 评估不达标 (faith={}, rel={})，将降级策略重试一次", query.getId(), f, r);
            QueryStrategy downgraded = QueryStrategy.builder()
                    .processorType(cn.cug.sxy.ai.domain.rag.service.query.QueryType.BASIC)
                    .retrieval(original.getRetrieval())
                    .generation(original.getGeneration())
                    .evaluation(EvaluationStrategy.builder()
                            .enabled(false)
                            .allowRetry(false)
                            .build())
                    .clarificationRequired(false)
                    .taskPlan(original.getTaskPlan())
                    .build()
                    .ensureDefaults();
            return downgraded;
        }
        return original;
    }

    /**
     * 处理工具执行失败（第一级降级：工具级重试/切换）。
     */
    public FallbackDecision handleToolFailure(String toolName, Exception ex, TaskExecutionContext context) {
        log.warn("工具 {} 执行失败: {}", toolName, ex.getMessage());
        int attempts = context.getAttempts(toolName);
        
        // 第一级：工具级重试
        if (attempts < maxToolRetries) {
            log.info("工具 {} 第 {} 次重试", toolName, attempts + 1);
            return FallbackDecision.retry("重试工具 " + toolName);
        }

        // 第一级：工具级切换（尝试备用工具）
        String fallbackTool = getFallbackTool(toolName);
        if (fallbackTool != null) {
            log.info("工具 {} 失败，切换到备用工具: {}", toolName, fallbackTool);
            return FallbackDecision.switchTo(fallbackTool, "切换到备用工具: " + fallbackTool);
        }

        // 第二级：任务级澄清（如果启用且意图模糊）
        if (enableClarification && shouldRequestClarification(context)) {
            log.info("工具 {} 多次失败，请求用户澄清", toolName);
            return FallbackDecision.clarify("工具执行失败，需要澄清用户意图。请提供更多细节或明确您的需求。");
        }

        // 第三级：系统级致歉
        return FallbackDecision.abort("工具 " + toolName + " 多次失败: " + ex.getMessage() + 
                "。系统暂时无法处理此请求，建议联系人工客服。");
    }

    /**
     * 处理工具返回空结果。
     */
    public FallbackDecision handleEmptyResult(String toolName, TaskExecutionContext context) {
        log.warn("工具 {} 未产生有效结果", toolName);
        
        // 第一级：尝试切换工具
        String fallbackTool = getFallbackTool(toolName);
        if (fallbackTool != null) {
            return FallbackDecision.switchTo(fallbackTool, "工具未找到结果，切换到备用工具");
        }

        // 第二级：请求澄清
        if (enableClarification && shouldRequestClarification(context)) {
            return FallbackDecision.clarify("未找到相关信息，请提供更多细节或重新表述您的问题。");
        }

        // 第三级：致歉
        return FallbackDecision.abort("工具 " + toolName + " 未产生有效结果，系统暂时无法处理此请求。");
    }

    /**
     * 判断是否应该请求用户澄清。
     */
    private boolean shouldRequestClarification(TaskExecutionContext context) {
        Query query = context.getQuery();
        if (query == null) {
            return false;
        }
        
        // 检查查询是否模糊（短查询或意图不明确）
        String queryText = query.getOriginalText();
        if (queryText == null || queryText.trim().length() < 10) {
            return true;
        }
        
        // 检查是否有意图信息且需要澄清
        QueryIntent intent = context.get("intent");
        if (intent != null && intent.isRequiresClarification()) {
            return true;
        }
        
        return false;
    }

    /**
     * 获取备用工具。
     * 动态从ToolRegistry中查找可用的备用工具，而不是硬编码。
     * 
     * 策略：
     * 1. 如果当前工具是RAG_QUERY（系统内部知识检索），优先尝试其他已注册的工具（MCP工具）
     * 2. 如果当前工具是外部MCP工具，优先尝试RAG_QUERY（系统内部知识检索）
     * 3. 如果都没有，尝试其他任意可用工具
     * 
     * @param toolName 当前失败的工具名称
     * @return 备用工具名称，如果没有则返回null
     */
    private String getFallbackTool(String toolName) {
        Map<String, RagTool> allTools = toolRegistry.getAllTools();
        if (allTools == null || allTools.isEmpty()) {
            log.debug("工具注册表中没有可用工具");
            return null;
        }
        
        // 获取所有可用工具名称（排除当前失败的工具）
        List<String> availableTools = allTools.keySet().stream()
                .filter(name -> !name.equals(toolName))
                .filter(name -> !"AUTO".equals(name)) // 排除AUTO模式
                .collect(Collectors.toList());
        
        if (availableTools.isEmpty()) {
            log.debug("没有可用的备用工具");
            return null;
        }
        
        // 策略1：如果当前工具是RAG_QUERY，优先使用其他工具（MCP工具）
        if (RagQueryTool.TOOL_NAME.equals(toolName)) {
            // 排除RAG_QUERY，选择其他工具
            String fallback = availableTools.stream()
                    .filter(name -> !RagQueryTool.TOOL_NAME.equals(name))
                    .findFirst()
                    .orElse(null);
            if (fallback != null) {
                log.debug("RAG_QUERY失败，选择备用工具: {}", fallback);
                return fallback;
            }
        }
        
        // 策略2：如果当前工具是外部MCP工具，优先尝试RAG_QUERY（系统内部知识检索）
        if (!RagQueryTool.TOOL_NAME.equals(toolName) && preferRagOnFailure) {
            if (allTools.containsKey(RagQueryTool.TOOL_NAME)) {
                log.debug("外部工具 {} 失败，优先使用系统内部RAG检索", toolName);
                return RagQueryTool.TOOL_NAME;
            }
        }
        
        // 策略3：选择任意其他可用工具
        String fallback = availableTools.get(0);
        log.debug("工具 {} 失败，选择备用工具: {}", toolName, fallback);
        return fallback;
    }
}
