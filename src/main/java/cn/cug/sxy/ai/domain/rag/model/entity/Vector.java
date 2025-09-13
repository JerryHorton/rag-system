package cn.cug.sxy.ai.domain.rag.model.entity;

import cn.cug.sxy.ai.domain.rag.model.valobj.SpaceType;
import cn.cug.sxy.ai.domain.rag.model.valobj.VectorCategory;
import cn.cug.sxy.ai.domain.rag.model.valobj.VectorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/8 17:34
 * @Description 向量实体类
 * @Author jerryhotton
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Vector {

    /**
     * 向量ID
     */
    private Long id;
    /**
     * 外部ID，根据向量类型可能关联到文档ID、查询ID等
     */
    private String externalId;
    /**
     * 向量类型：DOCUMENT（文档向量）、CHUNK（文档片段向量）、
     * QUERY（查询向量）、HYBRID（混合向量）
     */
    private VectorType vectorType;
    /**
     * 向量
     */
    private float[] embedding;
    /**
     * 向量维度
     */
    private Integer dimensions;
    /**
     * 向量所属的索引名称
     */
    private String indexName;
    /**
     * 向量创建时间
     */
    private LocalDateTime createTime;
    /**
     * 向量更新时间
     */
    private LocalDateTime updateTime;
    /**
     * 向量元数据，包含向量相关的额外信息
     */
    private Map<String, Object> metadata;
    /**
     * 所使用的嵌入模型名称，如text-embedding-ada-002等
     */
    private String embeddingModel;
    /**
     * 向量范式（向量长度）
     */
    private Double vectorNorm;
    /**
     * 向量类别，例如对于混合向量，可以标识是文本、图像或多模态向量
     */
    private VectorCategory vectorCategory;
    /**
     * 原始内容的摘要或描述，帮助调试和追踪
     */
    private String contentSummary;
    /**
     * 向量空间类型：COSINE（余弦空间）、EUCLIDEAN（欧几里得空间）、
     * DOT_PRODUCT（点积空间）、MANHATTAN（曼哈顿空间）
     */
    private SpaceType spaceType;
    /**
     * 是否为主向量（当使用多表示索引时，一个实体可能有多个向量表示）
     */
    private Boolean isPrimary;

}
