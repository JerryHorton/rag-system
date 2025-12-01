package cn.cug.sxy.ai.domain.rag.service.parsing.ocr;

import cn.cug.sxy.ai.domain.rag.model.parsing.StructuredDocument;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 阿里云DashScope视觉模型OCR提供者。
 * <p>
 * 职责：
 * - 封装阿里云DashScope API调用细节
 * - 提供OCR识别能力
 * - 不处理降级、重试等策略（由上层OcrService负责）
 * <p>
 * 使用的模型：qwen3-vl-plus（通义千问视觉理解模型）
 *
 * @author jerryhotton
 */
@Slf4j
@Component
public class AliOcrProvider implements OcrProvider {

    private static final String PROVIDER_NAME = "阿里云DashScope";

    // 最大Token数：qwen-vl-max 支持更大输出，这里设置较高值
    private static final int MAX_TOKENS = 16384;

    // JSON不完整时的最大重试次数
    private static final int MAX_JSON_RETRIES = 2;

    private final ObjectMapper objectMapper;
    private final String model;
    private final boolean enabled;
    private final String apiKey;

    public AliOcrProvider(ObjectMapper objectMapper,
                          @Value("${rag.ocr.ali.api-key:}") String apiKey,
                          @Value("${rag.ocr.ali.model:qwen3-vl-plus}") String model,
                          @Value("${rag.ocr.ali.enabled:true}") boolean enabled) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.enabled = enabled;

        if (enabled && apiKey != null && !apiKey.isEmpty()) {
            log.info("阿里云OCR提供者初始化成功，模型: {}, 最大Token: {}", model, MAX_TOKENS);
        } else {
            log.warn("阿里云OCR提供者未启用或API Key未配置");
        }
    }

    @Override
    public StructuredDocument recognize(byte[] imageBytes) throws OcrProviderException {
        if (!isAvailable()) {
            throw new OcrProviderException(PROVIDER_NAME, "阿里云OCR服务未启用或未正确配置");
        }

        Path tempImageFile = null;
        try {
            log.debug("调用阿里云OCR API，图像大小: {} bytes", imageBytes.length);

            // 检测图像格式
            String imageFormat = detectImageFormat(imageBytes);

            // 将图像保存为临时文件（文件路径上传方式，稳定性更高）
            tempImageFile = Files.createTempFile("ocr_image_", "." + imageFormat);
            Files.write(tempImageFile, imageBytes);
            log.debug("已创建临时图像文件: {}", tempImageFile);

            // 尝试使用完整模式，如果JSON被截断则降级到简化模式
            return recognizeWithFallback(tempImageFile);

        } catch (OcrProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("阿里云OCR调用失败", e);
            throw new OcrProviderException(PROVIDER_NAME, "OCR调用失败: " + e.getMessage(), e);
        } finally {
            // 清理临时文件
            if (tempImageFile != null) {
                try {
                    Files.deleteIfExists(tempImageFile);
                    log.debug("已删除临时图像文件: {}", tempImageFile);
                } catch (IOException e) {
                    log.warn("删除临时图像文件失败: {}", tempImageFile, e);
                }
            }
        }
    }

    /**
     * 带降级策略的OCR识别。
     * <p>
     * 策略：
     * 1. 首先尝试完整模式（详细结构信息）
     * 2. 如果JSON被截断（不完整），使用简化模式重试
     * 3. 简化模式只返回核心信息，减少Token消耗
     */
    private StructuredDocument recognizeWithFallback(Path tempImageFile) throws OcrProviderException {
        // 第一次尝试：完整模式
        try {
            String content = callOcrApi(tempImageFile, buildOcrPrompt(), MAX_TOKENS);

            // 检查JSON是否完整
            JsonValidationResult validation = validateJsonCompleteness(content);

            if (validation.isComplete()) {
                StructuredDocument document = parseResponseContent(content);
                document.setModelInfo(model);
                log.debug("阿里云OCR识别成功（完整模式），页面数: {}",
                        document.getPages() != null ? document.getPages().size() : 0);
                return document;
            } else {
                log.warn("JSON响应被截断（完整模式），原因: {}，尝试简化模式", validation.getReason());
            }
        } catch (Exception e) {
            log.warn("完整模式识别失败: {}，尝试简化模式", e.getMessage());
        }

        // 第二次尝试：简化模式
        try {
            String content = callOcrApi(tempImageFile, buildSimplifiedPrompt(), MAX_TOKENS);

            JsonValidationResult validation = validateJsonCompleteness(content);

            if (validation.isComplete()) {
                StructuredDocument document = parseResponseContent(content);
                document.setModelInfo(model + " (simplified)");
                log.info("阿里云OCR识别成功（简化模式），页面数: {}",
                        document.getPages() != null ? document.getPages().size() : 0);
                return document;
            } else {
                log.error("JSON响应仍然被截断（简化模式），原因: {}", validation.getReason());
                throw new OcrProviderException(PROVIDER_NAME,
                        "OCR响应JSON不完整，内容可能过多。原因: " + validation.getReason(), true);
            }
        } catch (OcrProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("简化模式识别也失败", e);
            throw new OcrProviderException(PROVIDER_NAME, "OCR识别失败: " + e.getMessage(), e, true);
        }
    }

    /**
     * 调用OCR API。
     */
    private String callOcrApi(Path imageFile, String prompt, int maxTokens)
            throws ApiException, NoApiKeyException, UploadFileException, OcrProviderException {

        MultiModalMessage userMessage = buildMultimodalMessage(imageFile, prompt);

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .apiKey(apiKey)
                .model(model)
                .messages(List.of(userMessage))
                .temperature(0.1f)
                .maxTokens(maxTokens)
                .build();

        MultiModalConversation conversation = new MultiModalConversation();
        MultiModalConversationResult result = conversation.call(param);

        if (result == null) {
            throw new RuntimeException("API响应为空");
        }

        String content = extractContentFromResult(result);
        if (content == null || content.isEmpty()) {
            throw new RuntimeException("API返回的内容为空");
        }

        // 记录使用统计
        if (result.getUsage() != null) {
            log.debug("API调用统计 - 输入tokens: {}, 输出tokens: {}, 总tokens: {}, 最大: {}",
                    result.getUsage().getInputTokens(),
                    result.getUsage().getOutputTokens(),
                    result.getUsage().getTotalTokens(),
                    maxTokens);

            // 检查是否接近Token上限
            int outputTokens = result.getUsage().getOutputTokens();
            if (outputTokens >= maxTokens - 100) {
                log.warn("输出Token数({})接近上限({})，可能被截断", outputTokens, maxTokens);
            }
        }

        return content;
    }

    /**
     * 验证JSON是否完整。
     */
    private JsonValidationResult validateJsonCompleteness(String content) {
        if (content == null || content.isEmpty()) {
            return new JsonValidationResult(false, "内容为空");
        }

        // 提取JSON
        String json = extractJsonFromContent(content);
        if (json == null || json.isEmpty()) {
            return new JsonValidationResult(false, "无法提取JSON");
        }

        // 检查基本结构
        String trimmed = json.trim();

        // 检查是否以正确的括号开始和结束
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return new JsonValidationResult(false, "JSON结构不完整，缺少开头或结尾大括号");
        }

        // 检查括号是否平衡
        int braceCount = 0;
        int bracketCount = 0;
        boolean inString = false;
        char prevChar = 0;

        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);

            // 处理字符串
            if (c == '"' && prevChar != '\\') {
                inString = !inString;
            }

            if (!inString) {
                if (c == '{') braceCount++;
                else if (c == '}') braceCount--;
                else if (c == '[') bracketCount++;
                else if (c == ']') bracketCount--;
            }

            prevChar = c;
        }

        if (braceCount != 0) {
            return new JsonValidationResult(false, "大括号不平衡，差值: " + braceCount);
        }
        if (bracketCount != 0) {
            return new JsonValidationResult(false, "方括号不平衡，差值: " + bracketCount);
        }

        // 尝试解析JSON
        try {
            objectMapper.readTree(json);
            return new JsonValidationResult(true, null);
        } catch (Exception e) {
            return new JsonValidationResult(false, "JSON解析失败: " + e.getMessage());
        }
    }

    /**
     * JSON验证结果。
     */
    private static class JsonValidationResult {
        private final boolean complete;
        private final String reason;

        JsonValidationResult(boolean complete, String reason) {
            this.complete = complete;
            this.reason = reason;
        }

        boolean isComplete() {
            return complete;
        }

        String getReason() {
            return reason;
        }
    }

    /**
     * 构建简化版OCR提示词。
     * 用于内容过多导致JSON被截断时的降级方案。
     */
    private String buildSimplifiedPrompt() {
        return """
                请识别图片中的文本内容，以简化的JSON格式返回。
                
                【重要】由于内容可能较多，请只返回核心信息：
                1. 只返回type为"title"或"text"的元素
                2. 表格直接转换为Markdown格式文本（type="text"）
                3. 省略bbox、table_info等详细信息
                4. 确保JSON完整，不要被截断
                
                返回格式：
                ```json
                {
                  "pages": [
                    {
                      "page_no": 1,
                      "layout": [
                        {
                          "element_id": "e1",
                          "type": "title或text",
                          "heading_level": 1或null,
                          "text": "原始文本",
                          "md_text": "Markdown格式文本",
                          "confidence": 0.95
                        }
                      ]
                    }
                  ]
                }
                ```
                
                注意：
                - 确保JSON格式正确且完整
                - 如果内容很多，可以合并相邻的text元素
                - 表格转换为Markdown表格语法放在md_text中
                """;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME + " (" + model + ")";
    }

    @Override
    public int getPriority() {
        return 10; // 高优先级
    }

    @Override
    public boolean isAvailable() {
        return enabled && apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.CLOUD_API;
    }

    // ==================== 私有方法 ====================

    /**
     * 构建多模态消息
     */
    private MultiModalMessage buildMultimodalMessage(Path imageFile, String text) {
        String fileUrl = "file://" + imageFile.toAbsolutePath();

        List<Map<String, Object>> contentList = new ArrayList<>();

        Map<String, Object> imageContent = new HashMap<>();
        imageContent.put("image", fileUrl);
        contentList.add(imageContent);

        Map<String, Object> textContent = new HashMap<>();
        textContent.put("text", text);
        contentList.add(textContent);

        return MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(contentList)
                .build();
    }

    /**
     * 从结果中提取内容
     */
    @SuppressWarnings("unchecked")
    private String extractContentFromResult(MultiModalConversationResult result) throws OcrProviderException {
        if (result.getOutput() == null || result.getOutput().getChoices() == null
                || result.getOutput().getChoices().isEmpty()) {
            throw new OcrProviderException(PROVIDER_NAME, "API响应中没有choices");
        }

        var firstChoice = result.getOutput().getChoices().get(0);
        if (firstChoice.getMessage() == null) {
            throw new OcrProviderException(PROVIDER_NAME, "API响应中message为空");
        }

        var message = firstChoice.getMessage();
        List<Map<String, Object>> messageContentList = (List<Map<String, Object>>) message.getContent();

        if (messageContentList == null || messageContentList.isEmpty()) {
            throw new OcrProviderException(PROVIDER_NAME, "API返回的内容为空");
        }

        for (Map<String, Object> contentItem : messageContentList) {
            if (contentItem.containsKey("text")) {
                Object textObj = contentItem.get("text");
                if (textObj != null) {
                    return textObj.toString();
                }
            }
        }

        throw new OcrProviderException(PROVIDER_NAME, "API返回的content中没有text字段");
    }

    /**
     * 构建OCR提示词（精简版）。
     * 大幅减少token消耗，保留核心结构定义。
     */
    private String buildOcrPrompt() {
        return """
                识别图片中的文档内容，返回JSON格式。
                
                【JSON结构】
                {"pages":[{"page_no":1,"image_size":[宽,高],"layout":[元素数组]}]}
                
                【元素字段】
                - element_id: 唯一ID如"e1_1"
                - type: title/text/table/list/formula/code
                - heading_level: 标题层级(1-6)，非标题为null
                - text: 原始文本
                - md_text: Markdown格式（表格用|语法，公式用$LaTeX$）
                - confidence: 置信度0-1
                - table_info: 表格时必填{headers:[],rows:[[]],row_count,column_count}
                
                【重要规则】
                1. 表格必须用type="table"并提供完整table_info
                2. md_text中表格格式：|列1|列2|\\n|---|---|\\n|值1|值2|
                3. 按阅读顺序排列元素
                4. 直接返回JSON，不加```标记，确保完整可解析
                
                【表格示例】
                {"element_id":"e1_1","type":"table","text":"姓名 年龄\\n张三 25","md_text":"| 姓名 | 年龄 |\\n|---|---|\\n| 张三 | 25 |","confidence":0.95,"table_info":{"headers":["姓名","年龄"],"rows":[["张三","25"]],"row_count":1,"column_count":2}}
                """;
    }

    /**
     * 解析响应内容
     */
    private StructuredDocument parseResponseContent(String content) throws OcrProviderException {
        try {
            String jsonContent = extractJsonFromContent(content);

            StructuredDocument document;
            try {
                document = objectMapper.readValue(jsonContent, StructuredDocument.class);
            } catch (Exception e) {
                log.warn("JSON解析失败，尝试修复。原始内容片段: {}",
                        content.length() > 300 ? content.substring(0, 300) + "..." : content);
                jsonContent = fixJsonFormat(jsonContent);
                document = objectMapper.readValue(jsonContent, StructuredDocument.class);
            }

            // 验证并补充默认值
            if (document.getPages() == null || document.getPages().isEmpty()) {
                log.warn("解析的文档没有页面信息，创建默认页面");
                StructuredDocument.LayoutElement textElement = StructuredDocument.LayoutElement.builder()
                        .type("text")
                        .text(content)
                        .mdText(content)
                        .confidence(0.8)
                        .build();
                StructuredDocument.Page page = StructuredDocument.Page.builder()
                        .pageNo(1)
                        .imageSize(List.of(0, 0))
                        .layout(List.of(textElement))
                        .build();
                document = StructuredDocument.builder()
                        .pages(List.of(page))
                        .build();
            }

            return document;
        } catch (Exception e) {
            log.error("解析响应内容失败", e);
            throw new OcrProviderException(PROVIDER_NAME, "解析响应内容失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从内容中提取JSON
     */
    private String extractJsonFromContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        content = content.trim();

        // 移除markdown代码块标记
        if (content.contains("```json")) {
            int start = content.indexOf("```json") + 7;
            int end = content.indexOf("```", start);
            if (end > start) {
                content = content.substring(start, end).trim();
            }
        } else if (content.contains("```")) {
            int start = content.indexOf("```") + 3;
            int end = content.lastIndexOf("```");
            if (end > start) {
                content = content.substring(start, end).trim();
            }
        }

        // 提取JSON对象
        int firstBrace = content.indexOf('{');
        int lastBrace = content.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return content.substring(firstBrace, lastBrace + 1);
        }

        return content;
    }

    /**
     * 修复JSON格式问题
     */
    private String fixJsonFormat(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }

        // 移除控制字符
        json = json.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");

        // 移除注释
        json = json.replaceAll("//.*?(\n|$)", "\n");
        json = json.replaceAll("/\\*[\\s\\S]*?\\*/", "");

        // 修复尾随逗号
        json = json.replaceAll(",\\s*}", "}");
        json = json.replaceAll(",\\s*]", "]");

        // 修复单引号
        json = json.replaceAll("'([^']*)':", "\"$1\":");
        json = json.replaceAll(":\\s*'([^']*)'", ": \"$1\"");

        // 修复缺失引号的键名
        json = json.replaceAll("([{,])\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*:", "$1\"$2\":");

        // 修复布尔值和null
        json = json.replaceAll("\\bTrue\\b", "true");
        json = json.replaceAll("\\bFalse\\b", "false");
        json = json.replaceAll("\\bNull\\b", "null");
        json = json.replaceAll("\\bNULL\\b", "null");

        // 移除BOM
        if (json.startsWith("\uFEFF")) {
            json = json.substring(1);
        }

        return json.trim();
    }

    /**
     * 检测图像格式
     */
    private String detectImageFormat(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length < 4) {
            return "png";
        }

        // PNG: 89 50 4E 47
        if (imageBytes[0] == (byte) 0x89 && imageBytes[1] == 0x50 &&
                imageBytes[2] == 0x4E && imageBytes[3] == 0x47) {
            return "png";
        }

        // JPEG: FF D8 FF
        if (imageBytes[0] == (byte) 0xFF && imageBytes[1] == (byte) 0xD8 &&
                imageBytes[2] == (byte) 0xFF) {
            return "jpeg";
        }

        // GIF: 47 49 46 38
        if (imageBytes[0] == 0x47 && imageBytes[1] == 0x49 &&
                imageBytes[2] == 0x46 && imageBytes[3] == 0x38) {
            return "gif";
        }

        // WebP
        if (imageBytes.length >= 12 &&
                imageBytes[0] == 0x52 && imageBytes[1] == 0x49 &&
                imageBytes[2] == 0x46 && imageBytes[3] == 0x46 &&
                imageBytes[8] == 0x57 && imageBytes[9] == 0x45 &&
                imageBytes[10] == 0x42 && imageBytes[11] == 0x50) {
            return "webp";
        }

        return "png";
    }
}

