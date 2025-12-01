package cn.cug.sxy.ai.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @version 1.0
 * @Date 2025/11/20 15:00
 * @Description 意图规则数据传输对象
 * @Author jerryhotton
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IntentRuleDTO {

    private Long id;

    @NotBlank(message = "规则类型不能为空")
    private String ruleType;

    @NotBlank(message = "规则内容不能为空")
    private String ruleContent;

    /**
     * 匹配模式 (EXACT/CONTAINS/PREFIX/SUFFIX/REGEX)，关键词类型可配置
     */
    private String matchMode;

    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    @NotBlank(message = "主题领域不能为空")
    private String topicDomain;

    @NotBlank(message = "目标处理器不能为空")
    private String targetProcessor;

    @NotNull(message = "置信度不能为空")
    @Min(value = 0, message = "置信度最小值为0")
    @Max(value = 1, message = "置信度最大值为1")
    private Double confidence;

    @NotNull(message = "优先级不能为空")
    private Integer priority;

    private Boolean isActive;

    /**
     * 是否允许后续检测器继续判定
     */
    private Boolean allowCascade;

    /**
     * 是否锁定 Processor
     */
    private Boolean lockProcessor;

    /**
     * 语义路由分组键
     */
    private String routeKey;

    /**
     * 个性化语义阈值
     */
    private Double semanticThreshold;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
