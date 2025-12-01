package cn.cug.sxy.ai.domain.rag.service.intent;

import cn.cug.sxy.ai.domain.rag.model.intent.QueryIntent;
import cn.cug.sxy.ai.domain.rag.model.intent.TaskType;
import cn.cug.sxy.ai.domain.rag.model.plan.TaskPlan;
import cn.cug.sxy.ai.domain.rag.model.strategy.EvaluationStrategy;
import cn.cug.sxy.ai.domain.rag.model.strategy.GenerationStrategy;
import cn.cug.sxy.ai.domain.rag.model.strategy.QueryStrategy;
import cn.cug.sxy.ai.domain.rag.model.strategy.RetrievalStrategy;
import cn.cug.sxy.ai.domain.rag.model.valobj.QueryParams;
import cn.cug.sxy.ai.domain.rag.service.query.QueryType;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 将意图映射为执行策略。
 */
@Component
public class StrategyMapper {

    @Value("${rag.retrieval.default-top-k:5}")
    private int defaultTopK;

    @Value("${rag.retrieval.default-min-score:0.7}")
    private double defaultMinScore;

    @Value("${rag.generation.default-model:gpt-3.5-turbo}")
    private String defaultModel;

    public QueryStrategy map(QueryIntent intent, QueryParams params) {
        QueryType processor = decideProcessor(intent);
        RetrievalStrategy retrieval = buildRetrievalStrategy(intent, params);
        GenerationStrategy generation = buildGenerationStrategy(intent, params);
        EvaluationStrategy evaluation = buildEvaluationStrategy(intent);

        return QueryStrategy.builder()
                .processorType(processor)
                .retrieval(retrieval)
                .generation(generation)
                .evaluation(evaluation)
                .clarificationRequired(intent.isRequiresClarification())
                .taskPlan(intent.getTaskPlan())
                .build()
                .ensureDefaults();
    }

    private QueryType decideProcessor(QueryIntent intent) {
        TaskPlan plan = intent.getTaskPlan();
        if (plan != null && plan.hasTasks()) {
            return QueryType.RAG_FUSION;
        }
        QueryType suggested = intent.getRecommendedProcessor();
        if (suggested != null && intent.isLockProcessor()) {
            return suggested;
        }
        QueryType fallback = switch (intent.getTaskType()) {
            case COMPARISON -> QueryType.MULTI_QUERY;
            case ANALYSIS, DECISION_SUPPORT -> QueryType.STEP_BACK;
            case TROUBLESHOOT -> QueryType.DECOMPOSITION;
            case FAQ, FACT_LOOKUP -> QueryType.BASIC;
            case SUMMARIZATION -> QueryType.BASIC;
            default -> QueryType.BASIC;
        };
        if (suggested != null) {
            return suggested;
        }
        return fallback;
    }

    private RetrievalStrategy buildRetrievalStrategy(QueryIntent intent, QueryParams params) {
        int topK = params != null && params.getTopK() != null ? params.getTopK() : defaultTopK;
        double minScore = params != null && params.getMinScore() != null ? params.getMinScore() : defaultMinScore;

        boolean multiQuery = intent.getTaskType() == TaskType.COMPARISON
                || BooleanUtils.isTrue(params != null ? params.getMultiQueryEnabled() : null);
        boolean hyde = BooleanUtils.isTrue(params != null ? params.getHydeEnabled() : null);
        boolean stepBack = intent.getTaskType() == TaskType.ANALYSIS
                || BooleanUtils.isTrue(params != null ? params.getStepBackEnabled() : null);
        return RetrievalStrategy.builder()
                .topK(topK)
                .limit(params != null ? params.getLimit() : null)
                .minScore(minScore)
                .similarityThreshold(params != null ? params.getSimilarityThreshold() : null)
                .hybridEnabled(shouldEnableHybrid(intent))
                .rerankerEnabled(BooleanUtils.isTrue(params != null ? params.getRerankerEnabled() : null))
                .multiQueryEnabled(multiQuery)
                .hydeEnabled(hyde)
                .stepBackEnabled(stepBack)
                .selfRagEnabled(BooleanUtils.isTrue(params != null ? params.getSelfRagEnabled() : null))
                .indexName(params != null ? params.getIndexName() : null)
                .router(params != null ? params.getRouter() : null)
                .build();
    }

    private boolean shouldEnableHybrid(QueryIntent intent) {
        return switch (intent.getTaskType()) {
            case COMPARISON, DECISION_SUPPORT -> true;
            case FAQ, FACT_LOOKUP -> false;
            default -> intent.isMultiStep();
        };
    }

    private GenerationStrategy buildGenerationStrategy(QueryIntent intent, QueryParams params) {
        return GenerationStrategy.builder()
                .model(params != null && params.getModel() != null ? params.getModel() : defaultModel)
                .temperature(intent.getTaskType() == TaskType.DECISION_SUPPORT ? 0.2 : 0.1)
                .maxTokens(1024)
                .promptTemplate(null)
                .citationsRequired(intent.getTaskType() != TaskType.CHAT)
                .build();
    }

    private EvaluationStrategy buildEvaluationStrategy(QueryIntent intent) {
        boolean strict = intent.getTaskType() == TaskType.ANALYSIS
                || intent.getTaskType() == TaskType.DECISION_SUPPORT
                || intent.getTaskType() == TaskType.ORDER_LOOKUP;
        return EvaluationStrategy.builder()
                .enabled(strict)
                .minFaithfulness(strict ? 0.85 : 0.6)
                .minRelevance(0.6)
                .allowRetry(strict)
                .maxRetry(strict ? 1 : 0)
                .build();
    }
}

