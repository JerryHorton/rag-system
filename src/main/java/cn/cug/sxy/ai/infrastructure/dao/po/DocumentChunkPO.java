package cn.cug.sxy.ai.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @version 1.0
 * @Date 2025/9/8 16:42
 * @Description 文档块持久化对象
 * @Author jerryhotton
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentChunkPO {

    /**
     * 文档块ID
     */
    private Long id;
    /**
     * 关联的文档ID
     */
    private Long documentId;
    /**
     * 文档块内容
     */
    private String content;
    /**
     * 起始位置
     */
    private Integer startPosition;
    /**
     * 结束位置
     */
    private Integer endPosition;
    /**
     * 文档块序号
     */
    private Integer chunkIndex;
    /**
     * 向量ID
     */
    private Long vectorId;
    /**
     * 向量是否已生成
     */
    private Boolean vectorized;
    /**
     * 元数据JSON
     */
    private String metadata;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    /**
     * 重叠长度
     */
    private Integer overlapLength;
    /**
     * 质量分数
     */
    private Double qualityScore;

}
