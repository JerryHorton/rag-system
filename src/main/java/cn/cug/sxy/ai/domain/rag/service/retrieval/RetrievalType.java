package cn.cug.sxy.ai.domain.rag.service.retrieval;

/**
 * @version 1.0
 * @Date 2025/9/10 17:29
 * @Description 检索类型枚举
 * @Author jerryhotton
 */

public enum RetrievalType {

    /**
     * 向量相似度检索
     * 基于查询向量和文档向量的相似度进行检索
     */
    VECTOR_SIMILARITY,
    /**
     * 关键词匹配检索
     * 基于关键词匹配技术进行检索
     */
    KEYWORD,
    /**
     * 混合检索
     * 结合向量相似度和关键词匹配的混合检索策略
     */
    HYBRID,
    /**
     * 多索引检索
     * 在多个索引或向量空间中进行检索
     */
    MULTI_INDEX,
    /**
     * 查询融合
     * 将多个查询的检索结果进行融合
     */
    QUERY_FUSION,
    /**
     * 结果重排序
     * 对初步检索结果进行重新排序，提高相关性
     */
    RERANKING,
    /**
     * 稀疏-密集混合检索
     * 结合稀疏向量和密集向量的混合检索策略
     */
    SPARSE_DENSE_HYBRID,
    /**
     * 层次化检索
     * 先进行粗粒度检索，再进行细粒度检索
     */
    HIERARCHICAL,
    /**
     * 图检索
     * 基于图结构的检索方法，考虑节点间关系
     */
    GRAPH_BASED,
    /**
     * 元学习检索
     * 基于元学习技术自适应优化检索策略
     */
    META_LEARNING

}
