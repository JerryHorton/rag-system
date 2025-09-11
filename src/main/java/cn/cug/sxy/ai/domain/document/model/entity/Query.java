package cn.cug.sxy.ai.domain.document.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/8 17:20
 * @Description 查询实体类
 * @Author jerryhotton
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Query {

    /**
     * 查询ID
     */
    private Long id;
    /**
     * 用户ID，关联发起查询的用户
     */
    private String userId;
    /**
     * 会话ID，关联同一次会话中的多个查询
     */
    private String sessionId;
    /**
     * 原始查询文本，用户输入的查询内容
     */
    private String originalText;
    /**
     * 处理后的查询文本，经过预处理（如拼写纠错、同义词替换等）后的查询文本
     */
    private String processedText;
    /**
     * 查询状态：PENDING（待处理）、PROCESSING（处理中）、
     * COMPLETED（已完成）、FAILED（处理失败）
     */
    private String status;
    /**
     * 查询类型：RAG_BASIC（基础RAG）、RAG_ADVANCED（高级RAG）、
     * HYDE（假设文档嵌入）、QUERY_DECOMPOSITION（查询分解）等
     */
    private String queryType;
    /**
     * 查询的路由目标，指向应处理此查询的数据源或索引
     */
    private String routeTarget;
    /**
     * 查询创建时间
     */
    private LocalDateTime createTime;
    /**
     * 查询完成时间
     */
    private LocalDateTime completeTime;
    /**
     * 处理延迟（毫秒）
     */
    private Long latencyMs;
    /**
     * 查询向量表示
     */
    private float[] vector;
    /**
     * 查询元数据，可包含上下文信息、用户偏好等
     */
    private Map<String, Object> metadata;
    /**
     * 查询变体列表，当启用多查询策略时使用
     * 存储为序列化的JSON字符串
     */
    private String queryVariants;
    /**
     * 查询分解结果，当启用查询分解策略时使用
     * 存储为序列化的JSON字符串
     */
    private String decomposedQueries;
    /**
     * 关联的响应ID列表，一个查询可能产生多个响应
     * 存储为序列化的JSON字符串
     */
    private String responseIds;
    /**
     * 系统生成的查询嵌入维度
     */
    private Integer dimensions;
    /**
     * 错误信息，当status为FAILED时有值
     */
    private String errorMessage;
    /**
     * 检索参数，如topK、相似度阈值等
     * 存储为序列化的JSON字符串
     */
    private String retrievalParams;

}
