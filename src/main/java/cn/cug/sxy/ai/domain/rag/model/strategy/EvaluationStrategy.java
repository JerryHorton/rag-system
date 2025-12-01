package cn.cug.sxy.ai.domain.rag.model.strategy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评估策略。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationStrategy {

    private boolean enabled;
    private Double minFaithfulness;
    private Double minRelevance;
    private boolean allowRetry;
    private int maxRetry;
}

