package cn.cug.sxy.ai.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @version 1.0
 * @Date 2025/9/9 14:53
 * @Description
 * @Author jerryhotton
 */

@SpringBootTest
@ExtendWith({SpringExtension.class})
public class ApiTest {

    @Test
    public void test() {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey("sk-375014f0a89f4e3ebb9cc22ef791cd90")
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode")
                .completionsPath("/v1/chat/completions")
                .embeddingsPath("/v1/embeddings")
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder().openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("qwen-plus-2025-07-14")
                        .build())
                .build();
        String response = chatModel.call("你是什么模型");
        System.out.println(response);
    }

}
