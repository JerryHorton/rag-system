package cn.cug.sxy.ai.domain.rag.service.routing;

import cn.cug.sxy.ai.domain.rag.model.entity.Query;
import cn.cug.sxy.ai.domain.rag.service.query.IQueryProcessor;
import cn.cug.sxy.ai.domain.rag.service.query.QueryType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @Date 2025/9/12 10:00
 * @Description 基于规则的查询路由器实现
 * @Author jerryhotton
 */

@Slf4j
@Service("ruleBasedQueryRouter")
public class RuleBasedQueryRouter implements IQueryRouter {

    private final Map<QueryType, IQueryProcessor> processorMap;

    @Value("${rag.routing.default-query-type:BASIC}")
    private String defaultQueryType;

    // 多查询关键词
    private static final Set<String> MULTI_QUERY_KEYWORDS = Set.of(
            "比较", "对比", "不同", "区别", "多个", "列举", "枚举", "所有", "哪些", "列出"
    );

    // 分解查询关键词
    private static final Set<String> DECOMPOSITION_KEYWORDS = Set.of(
            "详细解释", "深入分析", "分步", "逐步", "从多方面", "全面", "综合", "详细描述"
    );

    // 自纠正关键词
    private static final Set<String> SELF_RAG_KEYWORDS = Set.of(
            "准确", "精确", "权威", "可靠", "专业", "事实", "科学", "验证"
    );

    // 假设性文档嵌入关键词
    private static final Pattern HYDE_PATTERN = Pattern.compile("假设|假如|如果|设想|想象|理论上");

    /**
     * 构造函数
     *
     * @param queryProcessors 查询处理器列表
     */
    public RuleBasedQueryRouter(List<IQueryProcessor> queryProcessors) {
        // 将处理器按类型进行分类
        this.processorMap = queryProcessors.stream()
                .collect(Collectors.toMap(IQueryProcessor::getType, processor -> processor));
        log.info("已注册{}个查询处理器: {}", queryProcessors.size(),
                queryProcessors.stream()
                        .map(p -> p.getType().name())
                        .collect(Collectors.joining(", ")));
    }

    /**
     * 根据查询内容和规则路由到适当的处理器
     *
     * @param query 查询实体
     * @return 选择的查询处理器
     */
    @Override
    public IQueryProcessor route(Query query) {
        QueryType targetType = determineQueryType(query);
        query.setQueryType(targetType.name());
        query.setRouteTarget(targetType.name());
        log.debug("查询[{}]被路由至[{}]处理器", query.getId(), targetType);

        return processorMap.getOrDefault(targetType, processorMap.get(QueryType.BASIC));
    }

    @Override
    public IQueryProcessor route(Query query, Map<String, Object> params) {
        // 显式指定优先
        QueryType explicit = parseExplicitType(params);
        if (explicit != null && processorMap.containsKey(explicit)) {
            query.setQueryType(explicit.name());
            query.setRouteTarget(explicit.name());
            log.debug("查询[{}]显式路由至[{}]", query.getId(), explicit);
            return processorMap.get(explicit);
        }
        // 基于规则+参数偏好打分
        RoutingDecision decision = scoreAndSelect(query, params);
        QueryType selected = decision.type;
        if (!processorMap.containsKey(selected)) {
            selected = QueryType.BASIC;
        }
        query.setQueryType(selected.name());
        query.setRouteTarget(selected.name());
        log.debug("查询[{}]路由至[{}] (置信度: {})", query.getId(), selected, String.format("%.2f", decision.confidence));

        return processorMap.getOrDefault(selected, processorMap.get(QueryType.BASIC));
    }

    private QueryType parseExplicitType(Map<String, Object> params) {
        if (params == null) return null;
        Object t = params.get("queryType");
        if (t == null) t = params.get("forceType");
        if (t == null) return null;
        try {
            return QueryType.valueOf(String.valueOf(t).trim().toUpperCase());
        } catch (Exception ignore) {
            return null;
        }
    }

    private RoutingDecision scoreAndSelect(Query query, Map<String, Object> params) {
        Map<QueryType, Double> scores = new java.util.EnumMap<>(QueryType.class);
        for (QueryType t : QueryType.values()) scores.put(t, 0.0);
        String text = (query.getOriginalText() != null ? query.getOriginalText() : "").toLowerCase();
        int len = text.length();
        // 关键词/模式规则
        if (HYDE_PATTERN.matcher(text).find()) bump(scores, QueryType.HYDE, 0.35);
        if (containsAny(text, MULTI_QUERY_KEYWORDS)) bump(scores, QueryType.MULTI_QUERY, 0.30);
        if (containsAny(text, DECOMPOSITION_KEYWORDS)) bump(scores, QueryType.DECOMPOSITION, 0.25);
        if (containsAny(text, SELF_RAG_KEYWORDS)) bump(scores, QueryType.SELF_RAG, 0.20);
        if (len > 120) bump(scores, QueryType.STEP_BACK, 0.20);
        // 参数偏好（布尔开关）
        if (isTrue(params, "multiQueryEnabled")) bump(scores, QueryType.MULTI_QUERY, 0.35);
        if (isTrue(params, "hydeEnabled")) bump(scores, QueryType.HYDE, 0.35);
        if (isTrue(params, "stepBackEnabled")) bump(scores, QueryType.STEP_BACK, 0.30);
        if (isTrue(params, "selfRagEnabled")) bump(scores, QueryType.SELF_RAG, 0.30);
        // 基础类型作为兜底
        bump(scores, QueryType.BASIC, 0.10);
        // 仅在可用处理器中选择最高分
        QueryType best = QueryType.BASIC;
        double bestScore = -1;
        for (Map.Entry<QueryType, Double> e : scores.entrySet()) {
            if (!processorMap.containsKey(e.getKey())) continue;
            if (e.getValue() > bestScore) {
                bestScore = e.getValue();
                best = e.getKey();
            }
        }

        return new RoutingDecision(best, clamp(bestScore));
    }

    private void bump(Map<QueryType, Double> scores, QueryType type, double inc) {
        scores.put(type, scores.getOrDefault(type, 0.0) + inc);
    }

    private boolean containsAny(String text, Set<String> words) {
        for (String w : words) {
            if (text.contains(w.toLowerCase())) return true;
        }
        return false;
    }

    private boolean isTrue(Map<String, Object> params, String key) {
        if (params == null) return false;
        Object v = params.get(key);
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) return Boolean.parseBoolean((String) v);
        return false;
    }

    private double clamp(double v) {
        if (v < 0) return 0;
        if (v > 1) return 1;
        return v;
    }

    private static class RoutingDecision {
        final QueryType type;
        final double confidence;

        RoutingDecision(QueryType t, double c) {
            this.type = t;
            this.confidence = c;
        }
    }

    /**
     * 确定查询类型
     *
     * @param query 查询实体
     * @return 确定的查询类型
     */
    private QueryType determineQueryType(Query query) {
        String queryText = query.getOriginalText().toLowerCase();
        // 检查是否是HyDE查询（假设性文档嵌入）
        if (HYDE_PATTERN.matcher(queryText).find()) {
            return getProcessorTypeIfAvailable(QueryType.HYDE);
        }
        // 检查是否是多查询
        for (String keyword : MULTI_QUERY_KEYWORDS) {
            if (queryText.contains(keyword)) {
                return getProcessorTypeIfAvailable(QueryType.MULTI_QUERY);
            }
        }
        // 检查是否是分解查询
        for (String keyword : DECOMPOSITION_KEYWORDS) {
            if (queryText.contains(keyword)) {
                return getProcessorTypeIfAvailable(QueryType.DECOMPOSITION);
            }
        }
        // 检查是否是Self-RAG查询
        for (String keyword : SELF_RAG_KEYWORDS) {
            if (queryText.contains(keyword)) {
                return getProcessorTypeIfAvailable(QueryType.SELF_RAG);
            }
        }
        // 检查查询长度，超过一定长度可能需要Step-Back处理
        if (queryText.length() > 100) {
            return getProcessorTypeIfAvailable(QueryType.STEP_BACK);
        }
        // 默认为基本查询类型
        return QueryType.BASIC;
    }

    /**
     * 获取处理器类型，如果不可用则返回基本类型
     *
     * @param preferredType 首选类型
     * @return 确定的查询类型
     */
    private QueryType getProcessorTypeIfAvailable(QueryType preferredType) {
        return processorMap.containsKey(preferredType) ? preferredType : QueryType.BASIC;
    }

    @Override
    public RouterType getType() {
        return RouterType.RULE_BASED;
    }

}
