package cn.cug.sxy.ai.domain.document.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/8 16:44
 * @Description 文档实体类
 * @Author jerryhotton
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Document {

    /**
     * 文档ID，系统自动生成的唯一标识
     */
    private Long id;
    /**
     * 文档标题
     */
    private String title;
    /**
     * 文档内容，可能是全文或片段
     */
    private String content;
    /**
     * 文档来源URL或文件路径
     */
    private String source;
    /**
     * 文档类型，如PDF、HTML、TXT等
     */
    private String documentType;
    /**
     * 文档状态：PENDING（待处理）、PROCESSING（处理中）、
     * INDEXED（已索引）、FAILED（处理失败）
     */
    private String status;
    /**
     * 文档处理批次ID，用于批量处理追踪
     */
    private String batchId;
    /**
     * 父文档ID，当前文档如果是某个文档的子片段，则指向父文档
     */
    private Long parentId;
    /**
     * 文档创建时间
     */
    private LocalDateTime createTime;
    /**
     * 文档更新时间
     */
    private LocalDateTime updateTime;
    /**
     * 文档元数据，存储为JSON格式，可以包含作者、发布日期、关键词等信息
     */
    private Map<String, Object> metadata;
    /**
     * 文档的相对位置（在父文档中的位置信息），用于文档片段
     */
    private Integer position;
    /**
     * 文档内容的向量表示是否已生成
     */
    private Boolean vectorized;
    /**
     * 处理错误信息，当status为FAILED时有值
     */
    private String errorMessage;

}
