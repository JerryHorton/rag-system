package cn.cug.sxy.ai.domain.armory.service.node;

import cn.cug.sxy.ai.domain.armory.model.entity.ArmoryCommandEntity;
import cn.cug.sxy.ai.domain.armory.model.valobj.AiClientModelVO;
import cn.cug.sxy.ai.domain.armory.model.valobj.AiDataTypeVO;
import cn.cug.sxy.ai.domain.armory.service.data.AbstractArmorySupport;
import cn.cug.sxy.ai.domain.armory.service.factory.DefaultArmoryStrategyFactory;
import cn.cug.sxy.design.framework.tree.handler.StrategyHandler;
import com.alibaba.fastjson2.JSON;
import io.modelcontextprotocol.client.McpSyncClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @version 1.0
 * @Date 2025/8/20 18:44
 * @Description 客户端对话模型配置节点
 * @Author jerryhotton
 */

@Slf4j
@Service
public class AiClientModelNode extends AbstractArmorySupport {

    @Resource
    private AiClientNode aiClientNode;

    public AiClientModelNode(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    @Override
    protected String doApply(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 构建节点，Mode 对话模型{}", JSON.toJSONString(requestParameter));

        List<AiClientModelVO> aiClientModelList = dynamicContext.getValue(dataName());
        if (CollectionUtils.isEmpty(aiClientModelList)) {
            log.warn("没有需要被初始化的 ai client model");
            return router(requestParameter, dynamicContext);
        }
        for (AiClientModelVO aiClientModelVO : aiClientModelList) {
            // 获取当前模型关联的 API Bean 对象
            OpenAiApi openAiApi = getBean(AiDataTypeVO.AI_CLIENT_API.getBeanName(aiClientModelVO.getApiId()));
            if (openAiApi == null) {
                throw new RuntimeException("未找到对应的 OpenAiApi Bean 对象");
            }
            // 获取当前模型关联的 Tool MCP Bean 对象
            List<McpSyncClient> mcpSyncClientList = new ArrayList<>();
            if (CollectionUtils.isEmpty(aiClientModelVO.getToolMcpIds())) {
                continue;
            }
            for (String toolMcpId : aiClientModelVO.getToolMcpIds()) {
                McpSyncClient mcpSyncClient = getBean(AiDataTypeVO.AI_CLIENT_TOOL_MCP.getBeanName(toolMcpId));
                if (mcpSyncClient != null) {
                    mcpSyncClientList.add(mcpSyncClient);
                }
            }
            // 实例化对话模型
            OpenAiChatModel chatModel = OpenAiChatModel.builder()
                    .openAiApi(openAiApi)
                    .defaultOptions(
                            OpenAiChatOptions.builder()
                                    .model(aiClientModelVO.getModelName())
                                    .toolCallbacks(new SyncMcpToolCallbackProvider(mcpSyncClientList).getToolCallbacks())
                                    .build())
                    .build();
            // 注册 Bean 对象
            registerBean(beanName(aiClientModelVO.getModelId()), OpenAiChatModel.class, chatModel);
        }

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext, String> get(ArmoryCommandEntity armoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return aiClientNode;
    }

    @Override
    protected String beanName(String beanId) {
        return AiDataTypeVO.AI_CLIENT_MODEL.getBeanName(beanId);
    }

    @Override
    protected String dataName() {
        return AiDataTypeVO.AI_CLIENT_MODEL.getDataName();
    }

}
