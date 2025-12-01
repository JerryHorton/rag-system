package cn.cug.sxy.ai.domain.rag.service.intent;

import cn.cug.sxy.ai.domain.rag.model.entity.Query;
import cn.cug.sxy.ai.domain.rag.model.intent.QueryIntent;
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
 * 当意图无法确定时，生成澄清式提问。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClarificationService {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            你是一个礼貌且专业的AI助手，需要针对用户模糊的问题给出澄清式提问。
            你的问题应该简洁、可供用户选择或回答，并能够帮助系统明确下一步的动作。
            """;

    public String buildClarification(Query query, QueryIntent intent) {
        try {
            String instructions = """
                    用户原始问题：%s
                    当前已识别信息：%s
                    请给出一句澄清式提问，例如“您是想...还是...？”或“请具体说明...”
                    """.formatted(query.getOriginalText(), intent.getSummary());
            List<Message> messages = List.of(
                    new SystemMessage(SYSTEM_PROMPT),
                    new UserMessage(instructions)
            );
            return chatClient.prompt(new Prompt(messages)).call().content();
        } catch (Exception e) {
            log.warn("澄清问题生成失败: {}", e.getMessage());
            return "为了更好地帮助您，请提供更多细节或明确您的需求。";
        }
    }
}

