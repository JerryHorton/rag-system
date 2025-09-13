package cn.cug.sxy.ai.domain.rag.service.evaluate;

import cn.cug.sxy.ai.domain.rag.model.entity.Query;
import cn.cug.sxy.ai.domain.rag.model.entity.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/11 10:44
 * @Description 基本评估器实现
 * @Author jerryhotton
 */

@Slf4j
@Service("basicEvaluator")
public class BasicEvaluator implements IEvaluator {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public BasicEvaluator(ChatClient chatClient, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    private static final String EVALUATION_PROMPT_TEMPLATE =
            """
                    你是一个专业的RAG系统评估专家，负责评估检索增强生成系统的回答质量。
                    
                    请根据以下信息评估回答质量：
                    
                    原始问题：
                    {{question}}
                    
                    系统回答：
                    {{answer}}
                    
                    检索的上下文：
                    {{context}}
                    
                    评估以下几个维度，并给出1-10的分数，其中1最低，10最高：
                    
                    1. 忠实度（Faithfulness）：回答是否基于提供的上下文，不包含虚构内容
                    2. 相关性（Relevance）：回答是否与问题相关，并直接回应了问题
                    3. 上下文相关性（Context Relevance）：检索的上下文是否包含回答问题所需的关键信息
                    4. 事实一致性（Factual Consistency）：回答中的事实是否与上下文一致
                    5. 回答完整性（Completeness）：回答是否全面地覆盖了问题的各个方面
                    6. 回答简洁性（Conciseness）：回答是否简洁，没有不必要的冗余
                    
                    请以JSON格式返回评估结果，格式如下：
                    {
                      "faithfulness": <分数>,
                      "relevance": <分数>,
                      "contextRelevance": <分数>,
                      "factualConsistency": <分数>,
                      "completeness": <分数>,
                      "conciseness": <分数>,
                      "totalScore": <总分>,
                      "reasoning": "<评分理由简述>"
                    }
                    
                    只返回JSON格式结果，不要包含任何其他说明或前言。
                    """;

    @Override
    public Map<String, Object> evaluate(Query query, Response response) {
        log.debug("评估查询回答质量，查询ID: {}, 响应ID: {}", query.getId(), response.getId());
        try {
            String question = query.getOriginalText();
            String answer = response.getAnswerText();
            String context = response.getRetrievedContext();
            // 准备评估提示
            Map<String, Object> variables = new HashMap<>();
            variables.put("question", question);
            variables.put("answer", answer);
            variables.put("context", context);
            String promptText = EVALUATION_PROMPT_TEMPLATE;
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                promptText = promptText.replace("{{" + entry.getKey() + "}}", entry.getValue().toString());
            }
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage("你是一个专业的RAG系统评估专家。"));
            messages.add(new UserMessage(promptText));
            Prompt prompt = new Prompt(messages);
            // 使用ChatClient评估
            String evaluationResult = chatClient.prompt(prompt).call().content();
            // 解析JSON结果
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = objectMapper.readValue(evaluationResult, Map.class);
            log.info("完成评估，总分: {}", resultMap.get("totalScore"));

            return resultMap;
        } catch (Exception e) {
            log.error("评估过程中发生错误", e);
            // 返回默认评估结果
            return fallbackResult(e);
        }
    }

    private static Map<String, Object> fallbackResult(Exception e) {
        Map<String, Object> defaultResult = new HashMap<>();
        defaultResult.put("faithfulness", 5);
        defaultResult.put("relevance", 5);
        defaultResult.put("contextRelevance", 5);
        defaultResult.put("factualConsistency", 5);
        defaultResult.put("completeness", 5);
        defaultResult.put("conciseness", 5);
        defaultResult.put("totalScore", 5);
        defaultResult.put("reasoning", "评估过程中发生错误: " + e.getMessage());
        defaultResult.put("error", e.getMessage());
        return defaultResult;
    }

}
