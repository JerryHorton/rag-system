package cn.cug.sxy.ai.domain.rag.service.generation;

import cn.cug.sxy.ai.domain.rag.model.valobj.GenerateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.DefaultChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/11 09:53
 * @Description 基本生成器实现
 * @Author jerryhotton
 */

@Slf4j
@Service("basicGenerator")
public class BasicGenerator implements IGenerator {

    private final ChatClient chatClient;

    public BasicGenerator(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    private static final String SYSTEM_PROMPT_TEMPLATE =
            """
                    你是一个专业的AI助手，负责基于提供的上下文信息回答用户问题。
                    
                    请遵循以下规则：
                    1. 仅使用提供的上下文信息回答问题，不要编造内容
                    2. 如果上下文不包含回答问题所需的信息，请直接说："抱歉，我在提供的资料中找不到相关信息。"
                    3. 回答要简洁明了，直击要点，提供用户真正需要的信息
                    4. 回答应准确反映上下文的内容，不添加个人观点
                    5. 如果用户询问与上下文无关的问题，请礼貌地引导回相关领域
                    
                    上下文信息：
                    %s
                    """;

    @Override
    //@Cacheable(value = "generationCache", key = "{#query, #contexts.hashCode()}", unless = "#result == null")
    public String generate(String query, List<String> contexts, GenerateParams params) {
        log.info("生成查询回答, 查询长度: {}, 上下文数量: {}", query.length(), contexts.size());
        try {
            // 合并上下文
            String mergedContext = String.join("\n\n", contexts);
            // 准备系统提示和用户消息
            List<Message> messages = getMessages(query, mergedContext);
            // 创建提示
            Prompt prompt = new Prompt(messages, extractChatOptions(params));
            // 发送到模型生成回答
            return chatClient.prompt(prompt).call().content();
        } catch (Exception e) {
            log.error("生成回答过程中发生错误", e);
            return "抱歉，生成回答时出现技术故障。" + e.getMessage();
        }
    }

    private static List<Message> getMessages(String query, String mergedContext) {
        Map<String, Object> systemVariables = new HashMap<>();
        systemVariables.put("context", mergedContext);
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(String.format(SYSTEM_PROMPT_TEMPLATE, mergedContext));
        Message systemMessage = systemPromptTemplate.createMessage(systemVariables);
        UserMessage userMessage = new UserMessage(query);
        List<Message> messages = new ArrayList<>();
        messages.add(systemMessage);
        messages.add(userMessage);
        return messages;
    }

    /**
     * 从参数Map中提取聊天选项
     *
     * @param params 生成参数
     * @return 提取的聊天选项Map
     */
    private ChatOptions extractChatOptions(GenerateParams params) {
        DefaultChatOptions options = new DefaultChatOptions();
        if (params.getTemperature() != null) {
            options.setTemperature(params.getTemperature());
        }
        if (params.getMaxTokens() != null) {
            options.setMaxTokens(params.getMaxTokens());
        }
        if (params.getTopP() != null) {
            options.setTopP(params.getTopP());
        }
        if (params.getTopK() != null) {
            options.setTopK(params.getTopK());
        }

        return options;
    }

}
