package cn.cug.sxy.ai.domain.armory.service.strategy.impl;

import cn.cug.sxy.ai.domain.armory.model.valobj.AiClientToolMcpVO;
import cn.cug.sxy.ai.domain.armory.service.strategy.ICreateMcpStrategy;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * @version 1.0
 * @Date 2025/8/20 15:19
 * @Description 创建sse类型MCP客户端策略
 * @Author jerryhotton
 */

@Slf4j
@Service
public class SseCreateMcpStrategy implements ICreateMcpStrategy {

    @Override
    public McpSyncClient createMcpSyncClient(AiClientToolMcpVO aiClientToolMcpVO) {
        AiClientToolMcpVO.TransportConfigSse sse = aiClientToolMcpVO.getTransportConfigSse();
        HttpClientSseClientTransport sseClientTransport = HttpClientSseClientTransport
                .builder(sse.getBaseUri())
                .sseEndpoint(sse.getSseEndpoint())
                .build();
        McpSyncClient mcpSyncClient = McpClient.sync(sseClientTransport).requestTimeout(Duration.ofMinutes(aiClientToolMcpVO.getRequestTimeout())).build();
        McpSchema.InitializeResult init_sse = mcpSyncClient.initialize();
        log.info("Tool SSE MCP Initialized {}", init_sse);

        return mcpSyncClient;
    }

}
