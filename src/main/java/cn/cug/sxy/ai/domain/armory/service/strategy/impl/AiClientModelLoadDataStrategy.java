package cn.cug.sxy.ai.domain.armory.service.strategy.impl;

import cn.cug.sxy.ai.domain.armory.repository.IAgentRepository;
import cn.cug.sxy.ai.domain.armory.model.entity.ArmoryCommandEntity;
import cn.cug.sxy.ai.domain.armory.model.valobj.AiClientApiVO;
import cn.cug.sxy.ai.domain.armory.model.valobj.AiClientModelVO;
import cn.cug.sxy.ai.domain.armory.model.valobj.AiDataTypeVO;
import cn.cug.sxy.ai.domain.armory.service.factory.DefaultArmoryStrategyFactory;
import cn.cug.sxy.ai.domain.armory.service.strategy.ILoadDataStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @version 1.0
 * @Date 2025/8/19 17:49
 * @Description 客户端模型-数据加载策略
 * @Author jerryhotton
 */

@Slf4j
@Service
public class AiClientModelLoadDataStrategy implements ILoadDataStrategy {

    private final IAgentRepository repository;
    private final ThreadPoolExecutor threadPoolExecutor;

    public AiClientModelLoadDataStrategy(
            IAgentRepository repository,
            ThreadPoolExecutor threadPoolExecutor) {
        this.repository = repository;
        this.threadPoolExecutor = threadPoolExecutor;
    }

    @Override
    public void loadData(ArmoryCommandEntity armoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) {
        List<String> modelIdList = armoryCommandEntity.getCommandIdList();
        log.info("加载客户端模型数据 modelIdList={}", modelIdList);

        CompletableFuture<List<AiClientApiVO>> aiClientApiListFuture = CompletableFuture.supplyAsync(() -> {
            log.info("查询配置数据(ai_client_api) {}", modelIdList);
            return repository.queryAiClientApiVOListByModelIds(modelIdList);
        }, threadPoolExecutor);

        CompletableFuture<List<AiClientModelVO>> aiClientModelListFuture = CompletableFuture.supplyAsync(() -> {
            log.info("查询配置数据(ai_client_model) {}", modelIdList);
            return repository.queryAiClientModelVOListByModelIds(modelIdList);
        }, threadPoolExecutor);

        CompletableFuture.allOf(aiClientApiListFuture, aiClientModelListFuture).thenRun(() -> {
            dynamicContext.setValue(AiDataTypeVO.AI_CLIENT_API.getDataName(), aiClientApiListFuture.join());
            dynamicContext.setValue(AiDataTypeVO.AI_CLIENT_MODEL.getDataName(), aiClientModelListFuture.join());
        }).join();

        log.info("加载客户端模型数据完成");
    }

}
