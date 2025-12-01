package cn.cug.sxy.ai.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @version 1.0
 * @Date 2025/11/25 17:38
 * @Description AI客户端API配置表 PO 对象
 * @Author jerryhotton
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiClientApiPO {

    /**
     * 主键ID
     */
    private Long id;
    /**
     * API ID
     */
    private String apiId;
    /**
     * 基础URL
     */
    private String baseUrl;
    /**
     * API密钥
     */
    private String apiKey;
    /**
     * 对话补全路径
     */
    private String completionsPath;
    /**
     * 嵌入向量路径
     */
    private String embeddingsPath;
    /**
     * 状态(0:禁用,1:启用)
     */
    private Integer status;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
