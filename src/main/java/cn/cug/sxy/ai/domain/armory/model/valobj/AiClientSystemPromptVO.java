package cn.cug.sxy.ai.domain.armory.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @version 1.0
 * @Date 2025/11/25 14:56
 * @Description AI 提示词 值对象
 * @Author jerryhotton
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiClientSystemPromptVO {

    /**
     * 提示词ID
     */
    private String promptId;
    /**
     * 提示词名称
     */
    private String promptName;
    /**
     * 提示词内容
     */
    private String promptContent;
    /**
     * 描述
     */
    private String description;

}
