package cn.cug.sxy.ai.domain.armory.service.strategy;

import cn.cug.sxy.ai.domain.armory.model.valobj.AiClientToolMcpVO;
import io.modelcontextprotocol.client.McpSyncClient;

/**
 * @version 1.0
 * @Date 2025/8/20 15:04
 * @Description MCP 创建策略
 * @Author jerryhotton
 */

public interface ICreateMcpStrategy {

    McpSyncClient createMcpSyncClient(AiClientToolMcpVO aiClientToolMcpVO);

}
