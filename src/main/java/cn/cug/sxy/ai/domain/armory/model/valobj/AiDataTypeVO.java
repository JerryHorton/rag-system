package cn.cug.sxy.ai.domain.armory.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @version 1.0
 * @Date 2025/8/19 14:49
 * @Description Ai数据类型 枚举
 * @Author jerryhotton
 */

@Getter
@AllArgsConstructor
public enum AiDataTypeVO {

    AI_CLIENT("客户端", "client", "ai_client_", "ai_client_data_list", "aiClientLoadDataStrategy"),
    AI_CLIENT_MODEL("对话模型", "model", "ai_client_model_", "ai_client_model_data_list", "aiClientModelLoadDataStrategy"),
    AI_CLIENT_TOOL_MCP("工具配置", "tool_mcp", "ai_client_tool_mcp_", "ai_client_tool_mcp_data_list", "aiClientToolMcpLoadDataStrategy"),
    AI_CLIENT_SYSTEM_PROMPT("系统提示词", "system_prompt", "ai_client_system_prompt_", "ai_client_system_prompt_data_map", "aiClientSystemPromptLoadDataStrategy"),
    AI_CLIENT_API("客户端API", "api", "ai_client_api_", "ai_client_api_data_list", "aiClientApiLoadDataStrategy");

    /**
     * 名称
     */
    private final String name;
    /**
     * 代码
     */
    private final String code;
    /**
     * Bean 对象名称标签
     */
    private final String beanNameTag;
    /**
     * 数据名称
     */
    private String dataName;
    /**
     * 装配数据策略
     */
    private final String loadDataStrategy;

    /**
     * 获取Bean名称
     *
     * @param id 传入的参数
     * @return beanNameTag + id 拼接的Bean名称
     */
    public String getBeanName(String id) {
        return this.beanNameTag + id;
    }

}
