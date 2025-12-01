package cn.cug.sxy.ai.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @version 1.0
 * @Description 意图识别规则持久化对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IntentRulePO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 规则类型: KEYWORD, REGEX, SEMANTIC_EXAMPLE
     */
    private String ruleType;

    /**
     * 规则内容
     */
    private String ruleContent;

    /**
     * 匹配模式: EXACT, CONTAINS, PREFIX, SUFFIX, REGEX
     */
    private String matchMode;

    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 领域
     */
    private String topicDomain;

    /**
     * 推荐处理器
     */
    private String targetProcessor;

    /**
     * 置信度
     */
    private Double confidence;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 是否启用
     */
    private Boolean isActive;

    /**
     * 是否允许级联
     */
    private Boolean allowCascade;

    /**
     * 是否锁定处理器
     */
    private Boolean lockProcessor;

    /**
     * 路由分组键
     */
    private String routeKey;

    /**
     * 个性化语义阈值
     */
    private Double semanticThreshold;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
