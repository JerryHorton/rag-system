package cn.cug.sxy.ai.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @version 1.0
 * @Date 2025/9/8 17:22
 * @Description 查询持久化对象
 * @Author jerryhotton
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QueryPO {

    /**
     * 查询ID
     */
    private Long id;
    /**
     * 用户ID
     */
    private String userId;
    /**
     * 会话ID
     */
    private String sessionId;
    /**
     * 原始查询文本
     */
    private String originalText;
    /**
     * 处理后的查询文本
     */
    private String processedText;
    /**
     * 查询状态
     */
    private String status;
    /**
     * 查询类型
     */
    private String queryType;
    /**
     * 路由目标
     */
    private String routeTarget;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 完成时间
     */
    private LocalDateTime completeTime;
    /**
     * 处理延迟（毫秒）
     */
    private Long latencyMs;
    /**
     * 查询元数据JSON
     */
    private String metadata;
    /**
     * 查询变体JSON
     */
    private String queryVariants;
    /**
     * 分解查询JSON
     */
    private String decomposedQueries;
    /**
     * 响应IDs JSON
     */
    private String responseIds;
    /**
     * 错误信息
     */
    private String errorMessage;
    /**
     * 检索参数JSON
     */
    private String retrievalParams;

}
