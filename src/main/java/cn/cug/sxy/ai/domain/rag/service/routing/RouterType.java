package cn.cug.sxy.ai.domain.rag.service.routing;

/**
 * @version 1.0
 * @Date 2025/9/12 09:57
 * @Description 路由器类型枚举
 * @Author jerryhotton
 */

public enum RouterType {

    /**
     * 基于规则的路由
     * 根据预定义规则和关键词进行路由
     */
    RULE_BASED,
    /**
     * 基于语义的路由
     * 使用语义相似度进行路由决策
     */
    SEMANTIC,
    /**
     * 混合路由
     * 结合规则和语义的混合路由策略
     */
    HYBRID,
    /**
     * 基于模型的路由
     * 使用机器学习模型进行路由决策
     */
    MODEL_BASED,
    /**
     * 自适应路由
     * 根据历史表现动态调整路由策略
     */
    ADAPTIVE,
    /**
     * 基于负载的路由
     * 考虑系统负载进行路由决策
     */
    LOAD_BASED,
    /**
     * 多阶段路由
     * 按多个阶段进行路由决策
     */
    MULTI_STAGE

}
