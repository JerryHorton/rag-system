package cn.cug.sxy.ai.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @version 1.0
 * @Date 2025/11/25 17:45
 * @Description 系统提示词配置表
 * @Author jerryhotton
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiClientSystemPromptPO {

    /**
     * 主键ID
     */
    private Long id;
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
    /**
     * 状态(0:禁用,1:启用)
     */
    private Integer status;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
