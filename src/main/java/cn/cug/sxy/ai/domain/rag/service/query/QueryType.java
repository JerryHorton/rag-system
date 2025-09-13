package cn.cug.sxy.ai.domain.rag.service.query;

/**
 * @version 1.0
 * @Date 2025/9/10 16:42
 * @Description 查询类型枚举
 * @Author jerryhotton
 */

public enum QueryType {

    /**
     * 基本RAG查询
     * 简单的检索增强生成流程：查询向量化、检索相关文档、生成回答
     */
    BASIC,
    /**
     * 多查询生成
     * 从单个用户查询生成多个查询变体，分别检索，然后合并结果
     */
    MULTI_QUERY,
    /**
     * 假设性文档嵌入 (Hypothetical Document Embeddings)
     * 先生成假设性文档作为中间步骤，然后进行检索
     */
    HYDE,
    /**
     * 查询分解
     * 将复杂查询分解为多个简单子查询，分别处理后合并结果
     */
    DECOMPOSITION,
    /**
     * 后退一步查询
     * 先生成更抽象/高层次的查询，获取背景知识后再处理原始查询
     */
    STEP_BACK,
    /**
     * 自我纠正RAG (Self-RAG)
     * 在生成过程中进行自我反思和验证，确保回答的准确性
     */
    SELF_RAG,
    /**
     * RAG-Fusion
     * 结合多种检索策略和重排序技术的综合查询处理
     */
    RAG_FUSION,
    /**
     * 检索感知生成
     * 模型在生成过程中可感知检索到的文档质量，动态调整生成策略
     */
    RETRIEVAL_AWARE

}
