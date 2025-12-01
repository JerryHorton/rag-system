package cn.cug.sxy.ai.domain.rag.model.strategy;

import cn.cug.sxy.ai.domain.rag.model.plan.TaskPlan;
import cn.cug.sxy.ai.domain.rag.service.query.QueryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * L1 意图层输出的执行策略。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryStrategy {

    private QueryType processorType;
    private RetrievalStrategy retrieval;
    private GenerationStrategy generation;
    private EvaluationStrategy evaluation;
    private boolean clarificationRequired;
    private TaskPlan taskPlan;

    public QueryStrategy ensureDefaults() {
        if (retrieval == null) {
            retrieval = RetrievalStrategy.builder().build();
        }
        if (generation == null) {
            generation = GenerationStrategy.builder().build();
        }
        if (evaluation == null) {
            evaluation = EvaluationStrategy.builder().build();
        }
        return this;
    }
}

