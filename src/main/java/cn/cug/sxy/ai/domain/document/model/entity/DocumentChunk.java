package cn.cug.sxy.ai.domain.document.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/8 16:45
 * @Description 文档片段实体类
 * @Author jerryhotton
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentChunk {

    /**
     * 片段ID，系统自动生成的唯一标识
     */
    private Long id;
    /**
     * 原始文档ID，指向所属的原始文档
     */
    private Long documentId;
    /**
     * 片段内容
     */
    private String content;
    /**
     * 片段在原文档中的起始位置（字符偏移量）
     */
    private Integer startPosition;
    /**
     * 片段在原文档中的结束位置（字符偏移量）
     */
    private Integer endPosition;
    /**
     * 片段索引号，表示在同一文档的所有片段中的顺序
     */
    private Integer chunkIndex;
    /**
     * 片段的元数据，可能包含片段级别的特定信息
     */
    private Map<String, Object> metadata;
    /**
     * 片段创建时间
     */
    private LocalDateTime createTime;
    /**
     * 片段更新时间
     */
    private LocalDateTime updateTime;
    /**
     * 片段是否已被向量化
     */
    private Boolean vectorized;
    /**
     * 外部向量存储系统中的向量ID
     */
    private Long vectorId;
    /**
     * 片段的重叠部分长度（与前一个片段的重叠字符数）
     */
    private Integer overlapLength;
    /**
     * 片段质量分数，用于评估片段的质量（可基于内容丰富度、长度等计算）
     */
    private Double qualityScore;

}
