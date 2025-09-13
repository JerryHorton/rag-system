package cn.cug.sxy.ai.domain.rag.service.evaluate;

/**
 * @version 1.0
 * @Date 2025/9/11 10:41
 * @Description 评估类型枚举
 * @Author jerryhotton
 */

public enum EvaluationType {

    /**
     * 基于LLM的评估
     * 使用大语言模型进行评估
     */
    LLM_BASED,
    /**
     * 基于规则的评估
     * 使用预定义规则和指标进行评估
     */
    RULE_BASED,
    /**
     * 基于参考答案的评估
     * 将生成的回答与参考答案进行对比
     */
    REFERENCE_BASED,
    /**
     * 人机协作评估
     * 结合人类评估和自动评估
     */
    HUMAN_IN_LOOP,
    /**
     * 多维度评估
     * 从多个维度综合评估生成质量
     */
    MULTI_DIMENSIONAL,
    /**
     * 行为评估
     * 基于系统行为和用户交互进行评估
     */
    BEHAVIORAL,
    /**
     * 对比评估
     * 将多个系统或版本的回答进行对比评估
     */
    COMPARATIVE,
    /**
     * 适应性评估
     * 根据不同类型的查询动态调整评估标准
     */
    ADAPTIVE

}
