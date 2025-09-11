package cn.cug.sxy.ai.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @version 1.0
 * @Date 2025/9/8 16:41
 * @Description 文档持久化对象
 * @Author jerryhotton
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentPO {

    /**
     * 文档ID
     */
    private Long id;
    /**
     * 文档标题
     */
    private String title;
    /**
     * 文档内容
     */
    private String content;
    /**
     * 文档来源URL或文件路径
     */
    private String source;
    /**
     * 文档类型
     */
    private String documentType;
    /**
     * 文档状态
     */
    private String status;
    /**
     * 文档处理批次ID
     */
    private String batchId;
    /**
     * 父文档ID
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
     * 文档元数据JSON
     */
    private String metadata;
    /**
     * 文档的相对位置
     */
    private Integer position;
    /**
     * 文档内容的向量表示是否已生成
     */
    private Boolean vectorized;
    /**
     * 处理错误信息
     */
    private String errorMessage;

}
