package cn.cug.sxy.ai.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @version 1.0
 * @Date 2025/9/11 11:31
 * @Description 响应持久化对象
 * @Author jerryhotton
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponsePO {

    /**
     * 响应ID
     */
    private Long id;
    /**
     * 关联的查询ID
     */
    private Long queryId;
    /**
     * 会话ID
     */
    private String sessionId;
    /**
     * 生成的回答文本
     */
    private String answerText;
    /**
     * 检索到的上下文JSON
     */
    private String retrievedContext;
    /**
     * 上下文来源JSON
     */
    private String contextSources;
    /**
     * 响应状态
     */
    private String status;
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
     * 生成模型名称
     */
    private String modelName;
    /**
     * 生成参数JSON
     */
    private String generationParams;
    /**
     * 评估指标JSON
     */
    private String evaluationMetrics;
    /**
     * 忠实度评分
     */
    private Integer faithfulnessScore;
    /**
     * 相关性评分
     */
    private Integer relevanceScore;
    /**
     * 错误信息
     */
    private String errorMessage;
    /**
     * 是否需要审核
     */
    private Boolean needReview;
    /**
     * 元数据JSON
     */
    private String metadata;
    /**
     * 纠正后的回答文本
     */
    private String correctedAnswerText;
    /**
     * 响应类型
     */
    private String responseType;

}
