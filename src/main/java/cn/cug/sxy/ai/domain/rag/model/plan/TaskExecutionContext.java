package cn.cug.sxy.ai.domain.rag.model.plan;

import cn.cug.sxy.ai.domain.rag.model.entity.Query;
import cn.cug.sxy.ai.domain.rag.model.entity.Response;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 任务执行上下文，用于在任务之间传递状态。
 */
@Data
public class TaskExecutionContext {

    /**
     * 原始查询。
     */
    private final Query query;

    /**
     * 当前最新的响应（如有）。
     */
    private Response lastResponse;

    /**
     * 全局共享数据。
     */
    private final Map<String, Object> state = new HashMap<>();

    private final Map<String, Integer> toolAttempts = new HashMap<>();

    public TaskExecutionContext(Query query) {
        this.query = query;
    }

    public void put(String key, Object value) {
        state.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) state.get(key);
    }

    public void incrementAttempt(String toolName) {
        toolAttempts.compute(toolName, (k, v) -> v == null ? 1 : v + 1);
    }

    public int getAttempts(String toolName) {
        return toolAttempts.getOrDefault(toolName, 0);
    }

    public void recordResult(String key, Object value) {
        state.put(key, value);
    }
}


