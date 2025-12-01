package cn.cug.sxy.ai.domain.rag.model.intent;

import cn.cug.sxy.ai.domain.rag.model.plan.TaskPlan;
import cn.cug.sxy.ai.domain.rag.service.query.QueryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 描述一次查询的意图结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryIntent {

    /**
     * 意图检测来源。
     */
    private IntentSource source;

    /**
     * 任务类型。
     */
    private TaskType taskType;

    /**
     * 业务域。
     */
    private TopicDomain domain;

    /**
     * 复杂度评级。
     */
    private ComplexityLevel complexity;

    /**
     * 该查询是否需要多个步骤或工具协同。
     */
    private boolean multiStep;

    /**
     * 是否需要在执行前向用户澄清。
     */
    private boolean requiresClarification;

    /**
     * 是否锁定处理器（若true则必须采用recommendedProcessor）。
     */
    private boolean lockProcessor;

    /**
     * 是否允许继续级联到后续检测器（true=允许）。
     */
    private boolean allowCascade;

    /**
     * 推荐的处理器类型。
     */
    private QueryType recommendedProcessor;

    /**
     * 其他推荐的处理器候选。
     */
    @Builder.Default
    private Set<QueryType> secondaryProcessors = EnumSet.noneOf(QueryType.class);

    /**
     * 置信度（0-1）。
     */
    private double confidence;

    /**
     * 人类可读摘要。
     */
    private String summary;

    /**
     * 任务规划结果。
     */
    private TaskPlan taskPlan;

    /**
     * 附加属性。
     */
    @Builder.Default
    private Map<String, Object> attributes = Collections.emptyMap();

    public boolean hasTaskPlan() {
        return taskPlan != null && taskPlan.hasTasks();
    }

    public void addSecondaryProcessor(QueryType queryType) {
        if (secondaryProcessors == null) {
            secondaryProcessors = EnumSet.noneOf(QueryType.class);
        }
        secondaryProcessors.add(queryType);
    }
}

