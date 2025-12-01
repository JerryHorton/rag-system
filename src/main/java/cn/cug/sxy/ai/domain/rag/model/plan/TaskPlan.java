package cn.cug.sxy.ai.domain.rag.model.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 任务规划结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskPlan {

    private String summary;
    @Builder.Default
    private List<TaskNode> tasks = Collections.emptyList();
    private boolean requiresTools;

    public boolean hasTasks() {
        return tasks != null && !tasks.isEmpty();
    }
}

