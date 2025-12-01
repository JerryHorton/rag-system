package cn.cug.sxy.ai.domain.armory.service.strategy.impl;

import cn.cug.sxy.ai.domain.armory.repository.IAgentRepository;
import cn.cug.sxy.ai.domain.armory.model.entity.ArmoryCommandEntity;
import cn.cug.sxy.ai.domain.armory.model.valobj.*;
import cn.cug.sxy.ai.domain.armory.service.factory.DefaultArmoryStrategyFactory;
import cn.cug.sxy.ai.domain.armory.service.strategy.ILoadDataStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @version 1.0
 * @Date 2025/8/19 18:51
 * @Description 客户端-数据加载策略
 * @Author jerryhotton
 */

@Slf4j
@Service
public class AiClientLoadDataStrategy implements ILoadDataStrategy {

    private final IAgentRepository repository;
    private final ThreadPoolExecutor threadPoolExecutor;

    public AiClientLoadDataStrategy(
            IAgentRepository repository,
            ThreadPoolExecutor threadPoolExecutor) {
        this.repository = repository;
        this.threadPoolExecutor = threadPoolExecutor;
    }

    @Override
    public void loadData(ArmoryCommandEntity armoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) {
        List<String> clientIdList = armoryCommandEntity.getCommandIdList();
        log.info("加载客户端数据 clientIdList:{}", clientIdList);

        CompletableFuture<List<AiClientApiVO>> aiClientApiListFuture = CompletableFuture.supplyAsync(() -> {
            log.info("查询配置数据(ai_client_api) {}", clientIdList);
            return repository.queryAiClientApiVOListByClientIds(clientIdList);
        }, threadPoolExecutor);

        CompletableFuture<List<AiClientModelVO>> aiClientModelListFuture = CompletableFuture.supplyAsync(() -> {
            log.info("查询配置数据(ai_client_model) {}", clientIdList);
            return repository.queryAiClientModelVOListByClientIds(clientIdList);
        }, threadPoolExecutor);

        CompletableFuture<List<AiClientToolMcpVO>> aiClientToolMcpListFuture = CompletableFuture.supplyAsync(() -> {
            log.info("查询配置数据(ai_client_tool_mcp) {}", clientIdList);
            return repository.queryAiClientToolMcpVOListByClientIds(clientIdList);
        }, threadPoolExecutor);

        CompletableFuture<Map<String, AiClientSystemPromptVO>> aiClientSystemPromptListFuture = CompletableFuture.supplyAsync(() -> {
            log.info("查询配置数据(ai_client_system_prompt) {}", clientIdList);
            return repository.queryAiClientSystemPromptVOMapByClientIds(clientIdList);
        }, threadPoolExecutor);

        CompletableFuture<List<AiClientVO>> aiClientListFuture = CompletableFuture.supplyAsync(() -> {
            log.info("查询配置数据(ai_client) {}", clientIdList);
            return repository.queryAiClientVOListByClientIds(clientIdList);
        }, threadPoolExecutor);

        CompletableFuture.allOf(aiClientApiListFuture, aiClientModelListFuture, aiClientToolMcpListFuture, aiClientSystemPromptListFuture, aiClientListFuture).thenRun(() -> {
            dynamicContext.setValue(AiDataTypeVO.AI_CLIENT_API.getDataName(), aiClientApiListFuture.join());
            dynamicContext.setValue(AiDataTypeVO.AI_CLIENT_MODEL.getDataName(), aiClientModelListFuture.join());
            dynamicContext.setValue(AiDataTypeVO.AI_CLIENT_TOOL_MCP.getDataName(), aiClientToolMcpListFuture.join());
            dynamicContext.setValue(AiDataTypeVO.AI_CLIENT_SYSTEM_PROMPT.getDataName(), aiClientSystemPromptListFuture.join());
            dynamicContext.setValue(AiDataTypeVO.AI_CLIENT.getDataName(), aiClientListFuture.join());
        }).join();

        log.info("加载客户端数据完成");
    }

}
