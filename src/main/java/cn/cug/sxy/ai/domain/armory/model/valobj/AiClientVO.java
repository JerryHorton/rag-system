package cn.cug.sxy.ai.domain.armory.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @version 1.0
 * @Date 2025/11/25 15:05
 * @Description AI客户端配置 值对象
 * @Author jerryhotton
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiClientVO {

    /**
     * 客户端ID
     */
    private String clientId;
    /**
     * 客户端名称
     */
    private String clientName;
    /**
     * 描述
     */
    private String description;
    /**
     * 全局唯一模型ID
     */
    private String modelId;
    /**
     * Prompt ID List
     */
    private List<String> promptIdList;
    /**
     * MCP ID 列表
     */
    private List<String> mcpIdList;
    /**
     * 顾问ID 列表
     */
    private List<String> advisorIdList;

}
