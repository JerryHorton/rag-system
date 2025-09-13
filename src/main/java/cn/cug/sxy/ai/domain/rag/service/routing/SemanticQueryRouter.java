package cn.cug.sxy.ai.domain.rag.service.routing;

import cn.cug.sxy.ai.domain.rag.model.entity.Query;
import cn.cug.sxy.ai.domain.rag.service.query.IQueryProcessor;
import cn.cug.sxy.ai.domain.rag.service.query.QueryType;
import cn.cug.sxy.ai.infrastructure.embedding.IEmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @Date 2025/9/12 10:38
 * @Description 基于语义的查询路由器实现
 * @Author jerryhotton
 */

@Slf4j
@Service("semanticQueryRouter")
public class SemanticQueryRouter implements IQueryRouter {

    private final Map<QueryType, IQueryProcessor> processorMap;
    private final IEmbeddingService embeddingService;

    // 查询类型的示例问题，用于计算语义相似度
    private final Map<QueryType, List<String>> queryTypeExamples;

    // 查询类型的嵌入向量缓存
    private final Map<QueryType, float[][]> queryTypeEmbeddings;

    // 相似度阈值
    @Value("${rag.routing.semantic.threshold:0.75}")
    private double similarityThreshold;

    // 最小相似度差异，用于确保明确的路由决策
    @Value("${rag.routing.semantic.min-difference:0.1}")
    private double minSimilarityDifference;

    /**
     * 构造函数
     *
     * @param queryProcessors  查询处理器列表
     * @param embeddingService 嵌入服务
     */
    public SemanticQueryRouter(
            List<IQueryProcessor> queryProcessors,
            IEmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
        // 将处理器按类型进行分类
        this.processorMap = queryProcessors.stream()
                .collect(Collectors.toMap(IQueryProcessor::getType, processor -> processor));
        // 初始化示例问题
        this.queryTypeExamples = initializeQueryTypeExamples();
        // 初始化嵌入向量缓存
        this.queryTypeEmbeddings = new EnumMap<>(QueryType.class);
        // 预计算所有示例问题的嵌入向量
        precomputeEmbeddings();
        log.info("已注册{}个查询处理器, 已初始化语义路由模型", queryProcessors.size());
    }

    /**
     * 根据查询内容的语义相似度路由到适当的处理器
     *
     * @param query 查询实体
     * @return 选择的查询处理器
     */
    @Override
    public IQueryProcessor route(Query query) {
        QueryType targetType = determineQueryType(query);
        query.setQueryType(targetType.name());
        query.setRouteTarget(targetType.name());
        log.debug("查询[{}]被语义路由至[{}]处理器", query.getId(), targetType);

        return processorMap.getOrDefault(targetType, processorMap.get(QueryType.BASIC));
    }

    @Override
    public IQueryProcessor route(Query query, Map<String, Object> params) {
        try {
            // 显式指定优先（queryType / forceType）
            if (params != null) {
                Object t = params.get("queryType");
                if (t == null) t = params.get("forceType");
                if (t != null) {
                    try {
                        QueryType explicit = QueryType.valueOf(String.valueOf(t).trim().toUpperCase());
                        if (processorMap.containsKey(explicit)) {
                            query.setQueryType(explicit.name());
                            query.setRouteTarget(explicit.name());
                            log.info("查询[{}]显式语义路由至[{}]", query.getId(), explicit);

                            return processorMap.get(explicit);
                        }
                    } catch (Exception ignore) { /* 无效类型忽略 */ }
                }
            }
            // 阈值/差异参数覆盖（缺省使用配置）
            double thr = this.similarityThreshold;
            double minDiff = this.minSimilarityDifference;
            if (params != null) {
                Object v1 = params.get("similarityThreshold");
                if (v1 instanceof Number) {
                    thr = ((Number) v1).doubleValue();
                } else if (v1 instanceof String) try {
                    thr = Double.parseDouble((String) v1);
                } catch (Exception ignore) {
                }
                Object v2 = params.get("minDifference");
                if (v2 instanceof Number) {
                    minDiff = ((Number) v2).doubleValue();
                } else if (v2 instanceof String) try {
                    minDiff = Double.parseDouble((String) v2);
                } catch (Exception ignore) {
                }
            }
            // 计算语义相似度（一次嵌入，多次对比）
            String queryText = query.getOriginalText();
            float[] queryVector = embeddingService.generateEmbedding(queryText);
            Map<QueryType, Double> similarities = new EnumMap<>(QueryType.class);
            for (Map.Entry<QueryType, float[][]> entry : queryTypeEmbeddings.entrySet()) {
                QueryType qType = entry.getKey();
                float[][] examples = entry.getValue();
                double maxSim = 0.0;
                for (float[] ex : examples) {
                    double sim = calculateCosineSimilarity(queryVector, ex);
                    if (sim > maxSim) {
                        maxSim = sim;
                    }
                }
                similarities.put(qType, maxSim);
            }
            // 选择最高相似度并应用阈值/差异规则
            Map.Entry<QueryType, Double> highest = similarities.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(new AbstractMap.SimpleEntry<>(QueryType.BASIC, 0.0));
            if (highest.getValue() < thr) {
                query.setQueryType(QueryType.BASIC.name());
                query.setRouteTarget(QueryType.BASIC.name());
                log.info("查询[{}]相似度({})低于阈值({})，采用BASIC", query.getId(), String.format("%.3f", highest.getValue()), String.format("%.3f", thr));

                return processorMap.getOrDefault(QueryType.BASIC, processorMap.values().stream().findFirst().orElse(null));
            }
            Optional<Double> second = similarities.entrySet().stream()
                    .filter(e -> e.getKey() != highest.getKey())
                    .map(Map.Entry::getValue)
                    .max(Double::compare);
            if (second.isPresent() && (highest.getValue() - second.get()) < minDiff) {
                query.setQueryType(QueryType.BASIC.name());
                query.setRouteTarget(QueryType.BASIC.name());
                log.info("查询[{}]相似度差异({}) < 最小差异({})，采用BASIC", query.getId(),
                        String.format("%.3f", (highest.getValue() - second.get())), String.format("%.3f", minDiff));

                return processorMap.getOrDefault(QueryType.BASIC, processorMap.values().stream().findFirst().orElse(null));
            }
            QueryType targetType = highest.getKey();
            query.setQueryType(targetType.name());
            query.setRouteTarget(targetType.name());
            log.info("查询[{}]语义路由至[{}] (sim={})", query.getId(), targetType, String.format("%.3f", highest.getValue()));

            return processorMap.getOrDefault(targetType, processorMap.get(QueryType.BASIC));
        } catch (Exception e) {
            log.error("语义路由异常，降级至BASIC", e);
            query.setQueryType(QueryType.BASIC.name());
            query.setRouteTarget(QueryType.BASIC.name());

            return processorMap.getOrDefault(QueryType.BASIC, processorMap.values().stream().findFirst().orElse(null));
        }
    }

    /**
     * 基于语义相似度确定查询类型
     *
     * @param query 查询实体
     * @return 确定的查询类型
     */
    private QueryType determineQueryType(Query query) {
        String queryText = query.getOriginalText();

        try {
            // 生成查询文本的嵌入向量
            float[] queryVector = embeddingService.generateEmbedding(queryText);
            // 计算查询文本与各个类型示例之间的语义相似度
            Map<QueryType, Double> similarities = new EnumMap<>(QueryType.class);
            for (Map.Entry<QueryType, float[][]> entry : queryTypeEmbeddings.entrySet()) {
                QueryType queryType = entry.getKey();
                float[][] embeddings = entry.getValue();
                // 计算与当前类型所有示例的最大相似度
                double maxSimilarity = 0.0;
                for (float[] embedding : embeddings) {
                    double similarity = calculateCosineSimilarity(queryVector, embedding);
                    maxSimilarity = Math.max(maxSimilarity, similarity);
                }
                similarities.put(queryType, maxSimilarity);
            }
            // 找出相似度最高的类型
            Map.Entry<QueryType, Double> highestSimilarity = similarities.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(new AbstractMap.SimpleEntry<>(QueryType.BASIC, 0.0));
            // 检查相似度是否超过阈值
            if (highestSimilarity.getValue() < similarityThreshold) {
                log.debug("查询相似度低于阈值({})，使用基础处理器", similarityThreshold);
                return QueryType.BASIC;
            }
            // 检查与第二高相似度的差异是否足够明显
            Optional<Double> secondHighest = similarities.entrySet().stream()
                    .filter(e -> e.getKey() != highestSimilarity.getKey())
                    .map(Map.Entry::getValue)
                    .max(Double::compare);
            if (secondHighest.isPresent() &&
                    (highestSimilarity.getValue() - secondHighest.get() < minSimilarityDifference)) {
                log.debug("相似度差异不够明显({})，使用基础处理器",
                        highestSimilarity.getValue() - secondHighest.get());
                return QueryType.BASIC;
            }
            log.debug("查询被路由到类型: {}, 相似度: {}",
                    highestSimilarity.getKey(), highestSimilarity.getValue());

            return highestSimilarity.getKey();
        } catch (Exception e) {
            log.error("语义路由过程中发生错误，降级至基础处理器", e);
            return QueryType.BASIC;
        }
    }

    /**
     * 计算两个向量之间的余弦相似度
     *
     * @param vec1 向量1
     * @param vec2 向量2
     * @return 余弦相似度值，范围[-1,1]
     */
    private double calculateCosineSimilarity(float[] vec1, float[] vec2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }
        // 避免除以0
        if (norm1 <= 0 || norm2 <= 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 为每种查询类型初始化示例问题
     *
     * @return 查询类型与示例问题的映射
     */
    private Map<QueryType, List<String>> initializeQueryTypeExamples() {
        Map<QueryType, List<String>> examples = new EnumMap<>(QueryType.class);
        // 基础查询示例
        examples.put(QueryType.BASIC, Arrays.asList(
                "什么是机器学习？",
                "谁是阿尔伯特·爱因斯坦？",
                "Java如何处理异常？",
                "区块链技术的基本原理是什么？",
                "人工智能的发展历史"
        ));
        // 多查询示例
        examples.put(QueryType.MULTI_QUERY, Arrays.asList(
                "比较传统数据库和NoSQL数据库的区别",
                "对比React和Vue的优缺点",
                "列举常见的设计模式及其应用场景",
                "MySQL和PostgreSQL有什么不同？",
                "枚举人工智能的主要分支领域",
                "谈谈函数式编程和面向对象编程的区别"
        ));
        // 假设性文档嵌入示例
        examples.put(QueryType.HYDE, Arrays.asList(
                "假设我是一个初学者，如何学习Python？",
                "如果我想转行到人工智能领域，需要掌握哪些知识？",
                "假如我需要设计一个高并发系统，应该注意什么？",
                "设想未来十年人工智能的发展趋势",
                "如果要实现一个推荐系统，应该如何设计？"
        ));
        // 查询分解示例
        examples.put(QueryType.DECOMPOSITION, Arrays.asList(
                "详细解释神经网络的工作原理",
                "深入分析微服务架构的优缺点",
                "请全面介绍敏捷开发方法论",
                "逐步说明如何搭建一个完整的CI/CD流程",
                "从多方面分析区块链技术的应用前景",
                "综合解释量子计算的基本概念和应用"
        ));
        // 后退一步查询示例
        examples.put(QueryType.STEP_BACK, Arrays.asList(
                "这段代码为什么会产生内存泄漏？",
                "为什么我的机器学习模型准确率这么低？",
                "这个系统架构有什么问题？",
                "我的Kubernetes集群经常崩溃的原因是什么？",
                "为什么我的网站加载速度很慢？"
        ));
        // 自我纠正RAG示例
        examples.put(QueryType.SELF_RAG, Arrays.asList(
                "请提供准确的关于量子计算的最新进展",
                "需要权威的关于气候变化的科学数据",
                "给我可靠的人工智能伦理问题研究",
                "我需要精确的关于新冠病毒的医学信息",
                "提供专业的机器学习算法比较",
                "请给出关于区块链技术的科学评估"
        ));
        // RAG-Fusion示例
        examples.put(QueryType.RAG_FUSION, Arrays.asList(
                "综合分析全球经济趋势和技术发展的关系",
                "从多个角度解析人工智能对就业市场的影响",
                "结合多种数据源分析气候变化的证据和影响",
                "整合不同观点评估区块链技术的未来发展",
                "融合多领域知识解释量子计算的应用前景"
        ));
        // 检索感知生成示例
        examples.put(QueryType.RETRIEVAL_AWARE, Arrays.asList(
                "根据最新研究成果，人工智能会在哪些领域取得突破？",
                "基于公开数据，评估自动驾驶技术的当前状态",
                "参考学术文献，解释深度学习中的注意力机制",
                "根据市场报告，分析云计算服务的发展趋势",
                "依据临床研究，说明基因编辑技术的医疗应用"
        ));

        return examples;
    }

    /**
     * 预计算所有示例问题的嵌入向量
     */
    private void precomputeEmbeddings() {
        for (Map.Entry<QueryType, List<String>> entry : queryTypeExamples.entrySet()) {
            QueryType queryType = entry.getKey();
            List<String> examples = entry.getValue();
            float[][] embeddings = new float[examples.size()][];
            for (int i = 0; i < examples.size(); i++) {
                try {
                    embeddings[i] = embeddingService.generateEmbedding(examples.get(i));
                } catch (Exception e) {
                    log.error("为示例问题生成嵌入向量时出错: {}", examples.get(i), e);
                    // 使用空向量代替
                    embeddings[i] = new float[embeddingService.getDimensions()];
                }
            }

            queryTypeEmbeddings.put(queryType, embeddings);
        }
    }

    @Override
    public RouterType getType() {
        return RouterType.SEMANTIC;
    }

}
