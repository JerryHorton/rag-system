package cn.cug.sxy.ai.domain.rag.service.generation;

/**
 * @version 1.0
 * @Date 2025/9/11 09:42
 * @Description 生成类型枚举
 * @Author jerryhotton
 */

public enum GenerationType {

    /**
     * 基本生成
     * 简单的提示模板和上下文注入生成
     */
    BASIC,
    /**
     * 自我纠正生成
     * 在生成过程中进行自我反思和验证
     */
    SELF_CORRECTION,
    /**
     * 检索感知生成
     * 生成过程中感知检索质量，动态调整生成策略
     */
    RETRIEVAL_AWARE,
    /**
     * 思维链生成
     * 使用思维链（Chain-of-Thought）提示技术引导模型逐步推理
     */
    CHAIN_OF_THOUGHT,
    /**
     * 检索验证生成
     * 生成后进行事实验证，确保与检索内容一致
     */
    RETRIEVAL_VERIFICATION,
    /**
     * 多模型集成生成
     * 结合多个模型的输出生成最终回答
     */
    ENSEMBLE,
    /**
     * 可控制生成
     * 具有可控制的生成参数，如长度、风格、深度等
     */
    CONTROLLABLE,
    /**
     * 渐进式生成
     * 先生成草稿，再逐步优化和细化
     */
    PROGRESSIVE,
    /**
     * 多角度生成
     * 从不同视角或立场生成多个回答
     */
    MULTI_PERSPECTIVE,
    /**
     * 树状生成
     * 生成具有树状结构的层次化回答
     */
    TREE_BASED

}
