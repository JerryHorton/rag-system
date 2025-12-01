package cn.cug.sxy.ai.domain.armory.service.strategy.impl;

import cn.cug.sxy.ai.domain.armory.model.valobj.AiClientToolMcpVO;
import cn.cug.sxy.ai.domain.armory.service.strategy.ICreateMcpStrategy;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * @version 1.0
 * @Date 2025/8/20 18:36
 * @Description 创建stdio类型MCP客户端策略
 * @Author jerryhotton
 */

@Slf4j
@Service
public class StdioCreateMcpStrategy implements ICreateMcpStrategy {

    @Override
    public McpSyncClient createMcpSyncClient(AiClientToolMcpVO aiClientToolMcpVO) {
        AiClientToolMcpVO.TransportConfigStdio stdio = aiClientToolMcpVO.getTransportConfigStdio();
        ServerParameters parameters = ServerParameters.builder(stdio.getCommand())
                .args(stdio.getArgs())
                .env(stdio.getEnv())
                .build();
        McpSyncClient mcpSyncClient = McpClient.sync(new StdioClientTransport(parameters)).requestTimeout(Duration.ofMinutes(aiClientToolMcpVO.getRequestTimeout())).build();
        McpSchema.InitializeResult init_stdio = mcpSyncClient.initialize();
        log.info("Tool Stdio MCP Initialized {}", init_stdio);

        return mcpSyncClient;
    }

}
