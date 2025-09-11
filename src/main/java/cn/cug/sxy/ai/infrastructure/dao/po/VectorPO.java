package cn.cug.sxy.ai.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @version 1.0
 * @Date 2025/9/8 17:35
 * @Description 向量持久化对象
 * @Author jerryhotton
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VectorPO {

    /**
     * 向量ID
     */
    private Long id;
    /**
     * 外部ID
     */
    private String externalId;
    /**
     * 向量类型
     */
    private String vectorType;
    /**
     * 向量
     */
    private float[] embedding;
    /**
     * 向量维度
     */
    private Integer dimensions;
    /**
     * 索引名称
     */
    private String indexName;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    /**
     * 元数据JSON
     */
    private String metadata;
    /**
     * 嵌入模型名称
     */
    private String embeddingModel;
    /**
     * 向量范式
     */
    private Double vectorNorm;
    /**
     * 向量类别
     */
    private String vectorCategory;
    /**
     * 内容摘要
     */
    private String contentSummary;
    /**
     * 空间类型
     */
    private String spaceType;
    /**
     * 是否为主向量
     */
    private Boolean isPrimary;

}
