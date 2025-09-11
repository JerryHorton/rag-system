package cn.cug.sxy.ai.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/9 10:25
 * @Description 文档详情VO
 * @Author jerryhotton
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentDetailVO {

    /**
     * 文档ID
     */
    private Long id;
    /**
     * 文档标题
     */
    private String title;
    /**
     * 文档来源
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
     * 文档是否已向量化
     */
    private Boolean vectorized;
    /**
     * 文档向量ID
     */
    private String vectorId;
    /**
     * 文档创建时间
     */
    private LocalDateTime createTime;
    /**
     * 文档更新时间
     */
    private LocalDateTime updateTime;
    /**
     * 文档元数据
     */
    private Map<String, Object> metadata;
    /**
     * 文档错误信息
     */
    private String errorMessage;
    /**
     * 文档内容预览
     */
    private String contentPreview;

}
