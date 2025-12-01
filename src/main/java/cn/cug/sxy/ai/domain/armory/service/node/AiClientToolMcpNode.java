package cn.cug.sxy.ai.domain.armory.service.node;

import cn.cug.sxy.ai.domain.armory.service.data.AbstractArmorySupport;
import cn.cug.sxy.ai.domain.armory.service.factory.DefaultArmoryStrategyFactory;
import cn.cug.sxy.ai.domain.armory.service.strategy.ICreateMcpStrategy;
import cn.cug.sxy.design.framework.tree.handler.StrategyHandler;
import cn.cug.sxy.ai.domain.armory.model.entity.ArmoryCommandEntity;
import cn.cug.sxy.ai.domain.armory.model.valobj.AiClientToolMcpVO;
import cn.cug.sxy.ai.domain.armory.model.valobj.AiDataTypeVO;
import com.alibaba.fastjson2.JSON;
import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/8/20 11:47
 * @Description 客户端工具MCP配置节点
 * @Author jerryhotton
 */

@Slf4j
@Service
public class AiClientToolMcpNode extends AbstractArmorySupport {

    private final AiClientModelNode aiClientModelNode;

    private final Map<String, ICreateMcpStrategy> createMcpStrategyMap;

    public AiClientToolMcpNode(
            ApplicationContext applicationContext,
            Map<String, ICreateMcpStrategy> createMcpStrategyMap,
            AiClientModelNode aiClientModelNode) {
        super(applicationContext);
        this.createMcpStrategyMap = createMcpStrategyMap;
        this.aiClientModelNode = aiClientModelNode;
    }

    @Override
    protected String doApply(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 构建，客户端工具MCP配置节点 {}", JSON.toJSONString(requestParameter));

        List<AiClientToolMcpVO> aiClientToolMcpList = dynamicContext.getValue(AiDataTypeVO.AI_CLIENT_TOOL_MCP.getDataName());
        if (CollectionUtils.isEmpty(aiClientToolMcpList)) {
            log.warn("没有需要被初始化的 ai client tool mcp");
            return router(requestParameter, dynamicContext);
        }
        for (AiClientToolMcpVO aiClientToolMcpVO : aiClientToolMcpList) {
            String transportType = aiClientToolMcpVO.getTransportType().getCreateStrategy();
            ICreateMcpStrategy createMcpStrategy = createMcpStrategyMap.get(transportType);
            if (createMcpStrategy == null) {
                log.warn("没有找到对应的 ai client tool mcp 初始化策略，transportType: {}", transportType);
                continue;
            }
            // 创建 MCP 服务
            McpSyncClient mcpSyncClient = createMcpStrategy.createMcpSyncClient(aiClientToolMcpVO);
            // 注册 MCP 服务
            registerBean(AiDataTypeVO.AI_CLIENT_TOOL_MCP.getBeanName(aiClientToolMcpVO.getMcpId()), McpSyncClient.class, mcpSyncClient);
        }

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext, String> get(ArmoryCommandEntity armoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return aiClientModelNode;
    }

}
