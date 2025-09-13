package cn.cug.sxy.ai.domain.rag.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/10 16:43
 * @Description 响应实体类
 * @Author jerryhotton
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Response {

    /**
     * 响应ID，系统自动生成的唯一标识
     */
    private Long id;
    /**
     * 关联的查询ID
     */
    private Long queryId;
    /**
     * 会话ID，关联同一次会话中的多个查询-响应对
     */
    private String sessionId;
    /**
     * 生成的回答文本
     */
    private String answerText;
    /**
     * 检索到的上下文，用于生成回答的参考文档内容
     * 存储为序列化的JSON字符串
     */
    private String retrievedContext;
    /**
     * 上下文来源，记录文档ID列表
     * 存储为序列化的JSON字符串
     */
    private String contextSources;
    /**
     * 响应状态：PENDING（待处理）、PROCESSING（处理中）、
     * COMPLETED（已完成）、FAILED（处理失败）
     */
    private String status;
    /**
     * 响应创建时间
     */
    private LocalDateTime createTime;
    /**
     * 响应完成时间
     */
    private LocalDateTime completeTime;
    /**
     * 处理延迟（毫秒）
     */
    private Long latencyMs;
    /**
     * 生成模型名称，如gpt-3.5-turbo、claude-3-opus等
     */
    private String modelName;
    /**
     * 生成参数，如temperature、top_p等
     * 存储为序列化的JSON字符串
     */
    private String generationParams;
    /**
     * 评估指标，如忠实度（faithfulness）、相关性（relevance）等
     * 存储为序列化的JSON字符串
     */
    private String evaluationMetrics;
    /**
     * 忠实度评分，表示回答对检索上下文的忠实程度
     * 范围0-1，越高表示越忠实于上下文
     */
    private Integer faithfulnessScore;
    /**
     * 相关性评分，表示回答对原始查询的相关程度
     * 范围0-1，越高表示越相关
     */
    private Integer relevanceScore;
    /**
     * 错误信息，当status为FAILED时有值
     */
    private String errorMessage;
    /**
     * 系统认为响应是否需要人工审核
     * true表示系统认为该响应可能存在问题，需要人工审核
     */
    private Boolean needReview;
    /**
     * 响应元数据，包含其他相关信息
     */
    private Map<String, Object> metadata;
    /**
     * 经过自我纠正的回答文本，如果启用了自我纠正机制
     */
    private String correctedAnswerText;
    /**
     * 响应类型：RAG_BASIC（基础RAG）、RAG_ADVANCED（高级RAG）、
     * HYDE（假设文档嵌入）、QUERY_DECOMPOSITION（查询分解）等
     */
    private String responseType;

}
