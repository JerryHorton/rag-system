package cn.cug.sxy.ai.domain.armory.model.valobj;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/11/25 14:57
 * @Description MCP客户端配置 值对象
 * @Author jerryhotton
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiClientToolMcpVO {

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
    private TransportType transportType;
    /**
     * 传输配置(sse/stdio)
     */
    private String transportConfig;
    /**
     * 请求超时时间(分钟)
     */
    private Integer requestTimeout;
    /**
     * 传输配置 - sse
     */
    private TransportConfigSse transportConfigSse;
    /**
     * 传输配置 - stdio
     */
    private TransportConfigStdio transportConfigStdio;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TransportConfigSse {

        private String baseUri;
        private String sseEndpoint;

    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TransportConfigStdio {

        private String command;
        private List<String> args;
        private Map<String, String> env;

    }

    @Getter
    @AllArgsConstructor
    public enum TransportType {

        SSE("sse", "sseCreateMcpStrategy"),
        STDIO("stdio", "stdioCreateMcpStrategy");

        private final String code;
        private final String createStrategy;

        public static TransportType fromCode(String code) {
            for (TransportType type : values()) {
                if (type.getCode().equals(code)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid transport type code: " + code);
        }
    }

}
