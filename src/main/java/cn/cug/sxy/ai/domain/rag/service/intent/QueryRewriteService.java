package cn.cug.sxy.ai.domain.rag.service.intent;

import cn.cug.sxy.ai.domain.rag.model.entity.Query;
import cn.cug.sxy.ai.domain.rag.model.intent.QueryIntent;
import cn.cug.sxy.ai.domain.rag.model.intent.TaskType;
import cn.cug.sxy.ai.domain.rag.model.strategy.QueryStrategy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 查询重构服务：Step-Back、多查询、HyDE 等。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryRewriteService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void rewrite(Query query, QueryIntent intent, QueryStrategy strategy) {
        try {
            boolean didRewrite = false;
            if (strategy.getRetrieval().getMultiQueryEnabled() == Boolean.TRUE) {
                generateMultiQueries(query);
                didRewrite = true;
            }
            if (strategy.getRetrieval().getHydeEnabled() == Boolean.TRUE) {
                generateHydeAnswer(query);
                didRewrite = true;
            }
            if (strategy.getRetrieval().getStepBackEnabled() == Boolean.TRUE
                    || intent.getTaskType() == TaskType.ANALYSIS) {
                generateStepBackQuery(query);
                didRewrite = true;
            }
            if (didRewrite) {
                log.info("查询 {} 已进行重写/扩展", query.getId());
            }
        } catch (Exception ex) {
            log.warn("查询重构失败，将使用原始查询。原因: {}", ex.getMessage());
        }
    }

    private void generateMultiQueries(Query query) throws JsonProcessingException {
        String prompt = """
                你是查询重写助手。请基于下面的用户问题，生成3个不同角度的改写版本，用于检索知识库。
                要求：
                1. 每个版本是一个独立的问题；
                2. 覆盖不同表达方式或侧重点。
                只返回JSON数组，例如：["问题1","问题2","问题3"]。

                用户问题：%s
                """.formatted(query.getOriginalText());
        String content = callModel(prompt);
        List<String> variants = objectMapper.readValue(content, List.class);
        query.setQueryVariants(objectMapper.writeValueAsString(variants));
    }

    private void generateHydeAnswer(Query query) throws JsonProcessingException {
        String prompt = """
                假设你已经查询到了完美的资料，请为下面的问题生成一个“理想答案”（可以合理推断，但不要显式说明是假设），用于作为检索向量。
                只返回答案文本。

                问题：%s
                """.formatted(query.getOriginalText());
        String answer = callModel(prompt);
        query.setDecomposedQueries(answer);
    }

    private void generateStepBackQuery(Query query) throws JsonProcessingException {
        String prompt = """
                下面是一个具体问题，请为它生成一个更抽象、更上位的版本，便于先检索高层次背景知识。
                只返回一个问题句子。

                具体问题：%s
                """.formatted(query.getOriginalText());
        String generalized = callModel(prompt);
        query.setProcessedText(generalized);
    }

    private String callModel(String userPrompt) {
        List<Message> messages = List.of(
                new SystemMessage("你是一个严格遵守输出格式的助手。"),
                new UserMessage(userPrompt)
        );
        return chatClient.prompt(new Prompt(messages)).call().content();
    }
}


