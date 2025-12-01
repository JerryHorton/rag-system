package cn.cug.sxy.ai.domain.rag.service.intent.detector;

import cn.cug.sxy.ai.domain.rag.model.intent.ComplexityLevel;
import cn.cug.sxy.ai.domain.rag.model.intent.IntentSource;
import cn.cug.sxy.ai.domain.rag.model.intent.QueryIntent;
import cn.cug.sxy.ai.domain.rag.model.intent.TaskType;
import cn.cug.sxy.ai.domain.rag.model.intent.TopicDomain;
import cn.cug.sxy.ai.domain.rag.model.plan.TaskNode;
import cn.cug.sxy.ai.domain.rag.model.plan.TaskPlan;
import cn.cug.sxy.ai.domain.rag.service.intent.IntentDetector;
import cn.cug.sxy.ai.domain.rag.service.intent.cache.TaskPlanCacheService;
import cn.cug.sxy.ai.domain.rag.service.query.QueryType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 使用 LLM 生成任务规划。
 * 集成规划缓存，避免重复的LLM调用。
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class LlmPlannerIntentDetector implements IntentDetector {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TaskPlanCacheService taskPlanCacheService;

    @Value("${rag.intent.llm-planner-enabled:true}")
    private boolean enabled;

    @Value("${rag.intent.llm-planner-min-length:10}")
    private int shortQueryThreshold;

    private static final String PROMPT_TEMPLATE = """
            你是企业级AI系统的任务规划师。请对用户请求进行分类并生成高层次的任务计划。
            系统会自动选择合适的工具（包括RAG检索、网络搜索、MCP工具等）来执行每个任务步骤。
            
            输出必须是JSON，格式：
            {
              "taskType": "COMPARISON|FAQ|ANALYSIS|SUMMARIZATION|TROUBLESHOOT|DECISION_SUPPORT|UNKNOWN",
              "domain": "GENERAL|ORDER|PRODUCT|TECH_SUPPORT|LEGAL|FINANCE|WEATHER|UNKNOWN",
              "multiStep": true|false,
              "summary": "任务的高层次描述",
              "tasks": [
                {"step":1,"description":"任务步骤的自然语言描述，例如'搜索相关信息'、'查询订单状态'、'综合分析结果'等","dependencies":[]}
              ]
            }
            
            重要提示：
            - description应该用自然语言描述任务目标，而不是指定具体工具名称
            - 系统会根据描述自动选择最合适的工具（RAG查询、网络搜索、MCP工具等）
            - 任务必须保证依赖为有效的步骤编号
            - 请勿添加额外文本，严格输出JSON
            """;

    @Override
    public DetectionResult detect(IntentRequest request) {
        String queryText = request.queryText();
        if (!enabled || StringUtils.isBlank(queryText)) {
            return null;
        }
        boolean shortQuery = queryText.trim().length() < shortQueryThreshold;
        
        try {
            // 1. 先查缓存
            Optional<TaskPlan> cachedPlan = taskPlanCacheService.getCachedPlan(queryText);
            if (cachedPlan.isPresent()) {
                log.info("使用缓存的TaskPlan: query={}", queryText.substring(0, Math.min(50, queryText.length())));
                TaskPlan taskPlan = cachedPlan.get();
                boolean multiStep = taskPlan.hasTasks();
                boolean requiresClarification = shortQuery || !multiStep;
                QueryIntent intent = QueryIntent.builder()
                        .taskType(TaskType.UNKNOWN) // 缓存中可能没有taskType信息
                        .domain(TopicDomain.UNKNOWN)
                        .complexity(multiStep ? ComplexityLevel.HIGH : ComplexityLevel.MEDIUM)
                        .multiStep(multiStep)
                        .requiresClarification(requiresClarification)
                        .summary("从缓存加载的任务计划")
                        .recommendedProcessor(multiStep ? QueryType.RAG_FUSION : QueryType.BASIC)
                        .taskPlan(taskPlan)
                        .source(IntentSource.LLM_PLANNER)
                        .confidence(0.85) // 缓存命中，置信度稍低
                        .attributes(Map.of(
                                "plannerModel", "cached",
                                "shortQuery", shortQuery,
                                "requiresClarification", requiresClarification))
                        .build();
                return new DetectionResult(true, intent.getConfidence(), intent, "cached-llm-plan");
            }
            
            // 2. 调用LLM规划
            List<Message> messages = List.of(
                    new SystemMessage(shortQuery
                            ? "你是任务规划师。用户输入较短，请尽量推测潜在任务；若无法确定，请保持tasks为空并在summary中注明需要澄清。严格输出JSON。"
                            : "你是任务规划师。请严格输出JSON。"),
                    new UserMessage(buildUserPrompt(queryText, shortQuery)));
            Prompt prompt = new Prompt(messages);
            String content = chatClient.prompt(prompt).call().content();
            if (StringUtils.isBlank(content)) {
                return null;
            }
            Map<String, Object> payload = objectMapper.readValue(content, new TypeReference<>() {
            });
            TaskPlan taskPlan = parseTaskPlan(payload);
            
            // 3. 缓存结果
            taskPlanCacheService.cachePlan(queryText, taskPlan);
            
            boolean multiStep = taskPlan.hasTasks();
            boolean requiresClarification = shortQuery || !multiStep;
            QueryIntent intent = QueryIntent.builder()
                    .taskType(parseTaskType(payload.get("taskType")))
                    .domain(parseDomain(payload.get("domain")))
                    .complexity(multiStep ? ComplexityLevel.HIGH : ComplexityLevel.MEDIUM)
                    .multiStep(multiStep)
                    .requiresClarification(requiresClarification)
                    .summary(String.valueOf(payload.getOrDefault("summary", "LLM规划")))
                    .recommendedProcessor(multiStep ? QueryType.RAG_FUSION : QueryType.BASIC)
                    .taskPlan(taskPlan)
                    .source(IntentSource.LLM_PLANNER)
                    .confidence(shortQuery ? 0.72 : 0.88)
                    .attributes(Map.of(
                            "plannerModel", "llm",
                            "shortQuery", shortQuery,
                            "requiresClarification", requiresClarification))
                    .build();
            return new DetectionResult(true, intent.getConfidence(), intent, "llm-plan");
        } catch (Exception ex) {
            log.warn("LLM Planner 解析失败: {}", ex.getMessage());
            return null;
        }
    }

    private String buildUserPrompt(String queryText, boolean shortQuery) {
        StringBuilder builder = new StringBuilder(PROMPT_TEMPLATE);
        if (shortQuery) {
            builder.append("\n用户输入较短，如无法判断任务，请返回空tasks并说明需要澄清。");
        }
        builder.append("\n用户请求：").append(queryText);
        return builder.toString();
    }

    private TaskPlan parseTaskPlan(Map<String, Object> payload) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tasks = (List<Map<String, Object>>) payload.getOrDefault("tasks", Collections.emptyList());
            List<TaskNode> nodes = tasks.stream().map(t -> TaskNode.builder()
                    .step(((Number) t.getOrDefault("step", 0)).intValue())
                    .toolName("AUTO") // 标记为自动选择工具，由 ChatClient 根据描述选择合适的工具
                    .description(String.valueOf(t.getOrDefault("description", "")))
                    .dependencies(((List<?>) t.getOrDefault("dependencies", Collections.emptyList())).stream()
                            .filter(Number.class::isInstance)
                            .map(dep -> ((Number) dep).intValue())
                            .toList())
                    .build()).toList();
            return TaskPlan.builder()
                    .summary(String.valueOf(payload.getOrDefault("summary", "")))
                    .tasks(nodes)
                    .requiresTools(nodes.size() > 1)
                    .build();
        } catch (Exception ex) {
            log.warn("TaskPlan 解析失败: {}", ex.getMessage());
            return TaskPlan.builder().tasks(Collections.emptyList()).summary("planner-error").build();
        }
    }

    private TaskType parseTaskType(Object value) {
        if (value == null) return TaskType.UNKNOWN;
        try {
            return TaskType.valueOf(String.valueOf(value).toUpperCase());
        } catch (Exception ignored) {
            return TaskType.UNKNOWN;
        }
    }

    private TopicDomain parseDomain(Object value) {
        if (value == null) return TopicDomain.UNKNOWN;
        try {
            return TopicDomain.valueOf(String.valueOf(value).toUpperCase());
        } catch (Exception ignored) {
            return TopicDomain.UNKNOWN;
        }
    }

}

