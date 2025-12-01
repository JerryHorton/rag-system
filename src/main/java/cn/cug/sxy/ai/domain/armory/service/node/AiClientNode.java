package cn.cug.sxy.ai.domain.armory.service.node;

import cn.cug.sxy.ai.domain.armory.model.entity.ArmoryCommandEntity;
import cn.cug.sxy.ai.domain.armory.model.valobj.AiClientSystemPromptVO;
import cn.cug.sxy.ai.domain.armory.model.valobj.AiClientVO;
import cn.cug.sxy.ai.domain.armory.model.valobj.AiDataTypeVO;
import cn.cug.sxy.ai.domain.armory.service.data.AbstractArmorySupport;
import cn.cug.sxy.ai.domain.armory.service.factory.DefaultArmoryStrategyFactory;
import cn.cug.sxy.design.framework.tree.handler.StrategyHandler;
import com.alibaba.fastjson2.JSON;
import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/8/21 17:11
 * @Description AI 客户端配置节点
 * @Author jerryhotton
 */

@Slf4j
@Service
public class AiClientNode extends AbstractArmorySupport {

    public AiClientNode(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    @Override
    protected String doApply(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 构建，客户端配置节点 {}", JSON.toJSONString(requestParameter));
        List<AiClientVO> aiClientList = dynamicContext.getValue(dataName());
        if (CollectionUtils.isEmpty(aiClientList)) {
            log.warn("没有需要被初始化的 ai client");
            return router(requestParameter, dynamicContext);
        }
        Map<String, AiClientSystemPromptVO> systemPromptMap = dynamicContext.getValue(AiDataTypeVO.AI_CLIENT_SYSTEM_PROMPT.getDataName());
        for (AiClientVO aiClientVO : aiClientList) {
            // 1. 预设话术
            StringBuilder defaultSystem = new StringBuilder("Ai 智能体 \r\n");
            List<String> promptIdList = aiClientVO.getPromptIdList();
            for (String promptId : promptIdList) {
                AiClientSystemPromptVO aiClientSystemPromptVO = systemPromptMap.get(promptId);
                defaultSystem.append(aiClientSystemPromptVO.getPromptContent());
            }
            // 2. 对话模型
            OpenAiChatModel chatModel = getBean(getModelBeanName(aiClientVO.getModelId()));
            // 3. Mcp服务
            List<McpSyncClient> mcpSyncClientList = new ArrayList<>();
            getMcpBeanNameList(aiClientVO.getMcpIdList()).forEach(mcpId -> mcpSyncClientList.add(getBean(mcpId)));
            // 4. 构建对话客户端
            ChatClient chatClient = ChatClient.builder(chatModel)
                    .defaultSystem(defaultSystem.toString())
                    .defaultToolCallbacks(new SyncMcpToolCallbackProvider(mcpSyncClientList.toArray(new McpSyncClient[]{})))
                    .build();
            registerBean(beanName(aiClientVO.getClientId()), ChatClient.class, chatClient);
        }

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext, String> get(ArmoryCommandEntity armoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return defaultStrategyHandler;
    }

    @Override
    protected String beanName(String beanId) {
        return AiDataTypeVO.AI_CLIENT.getBeanName(beanId);
    }

    @Override
    protected String dataName() {
        return AiDataTypeVO.AI_CLIENT.getDataName();
    }

    /**
     * 获取模型Bean名称
     *
     * @param modelId 模型 ID
     * @return 模型Bean名称
     */
    private String getModelBeanName(String modelId) {
        return AiDataTypeVO.AI_CLIENT_MODEL.getBeanName(modelId);
    }

    /**
     * 获取McpBean名称列表
     *
     * @param mcpIdList Mcp ID 列表
     * @return McpBean名称列表
     */
    private List<String> getMcpBeanNameList(List<String> mcpIdList) {
        List<String> mcpBeanNameList = new ArrayList<>();
        for (String mcpId : mcpIdList) {
            mcpBeanNameList.add(AiDataTypeVO.AI_CLIENT_TOOL_MCP.getBeanName(mcpId));
        }
        return mcpBeanNameList;
    }

}
