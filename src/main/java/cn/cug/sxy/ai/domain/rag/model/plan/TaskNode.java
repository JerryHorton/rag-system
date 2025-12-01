package cn.cug.sxy.ai.domain.rag.model.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 任务规划中的节点。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskNode {

    private int step;
    private String toolName;
    private String description;
    @Builder.Default
    private List<Integer> dependencies = new ArrayList<>();

    public List<Integer> safeDependencies() {
        return dependencies == null ? Collections.emptyList() : dependencies;
    }
}

