package cn.cug.sxy.ai.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @version 1.0
 * @Date 2025/11/25 17:45
 * @Description MCP客户端配置表 PO 对象
 * @Author jerryhotton
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiClientToolMcpPO {

    /**
     * 主键ID
     */
    private Long id;
    /**
     * MCP ID
     */
    private String mcpId;
    /**
     * MCP名称
     */
    private String mcpName;
    /**
     * 传输类型(sse/stdio)
     */
    private String transportType;
    /**
     * 传输配置(sse/stdio)
     */
    private String transportConfig;
    /**
     * 请求超时时间(分钟)
     */
    private Integer requestTimeout;
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
