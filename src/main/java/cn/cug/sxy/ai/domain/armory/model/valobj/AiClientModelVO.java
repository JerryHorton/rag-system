package cn.cug.sxy.ai.domain.armory.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @version 1.0
 * @Date 2025/11/25 14:55
 * @Description 聊天模型配置 值对象
 * @Author jerryhotton
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiClientModelVO {

    /**
     * 模型ID
     */
    private String modelId;
    /**
     * 关联的API配置ID（只能关联一个）
     */
    private String apiId;
    /**
     * 关联的Tool MCP配置ID（可以关联多个）
     */
    private List<String> toolMcpIds;
    /**
     * 模型名称
     */
    private String modelName;
    /**
     * 模型类型：openai、deepseek、claude
     */
    private String modelType;

}
