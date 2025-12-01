package cn.cug.sxy.ai.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 意图规则返回对象（VO）
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IntentRuleVO {

    private Long id;
    private String ruleType;
    private String ruleContent;
    private String matchMode;
    private String taskType;
    private String topicDomain;
    private String targetProcessor;
    private Double confidence;
    private Integer priority;
    private Boolean isActive;
    private Boolean allowCascade;
    private Boolean lockProcessor;
    private String routeKey;
    private Double semanticThreshold;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

