package cn.cug.sxy.ai.domain.armory.service.node;

import cn.cug.sxy.ai.domain.armory.service.strategy.ILoadDataStrategy;
import cn.cug.sxy.design.framework.tree.handler.StrategyHandler;
import cn.cug.sxy.ai.domain.armory.model.entity.ArmoryCommandEntity;
import cn.cug.sxy.ai.domain.armory.service.data.AbstractArmorySupport;
import cn.cug.sxy.ai.domain.armory.service.factory.DefaultArmoryStrategyFactory;
import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @version 1.0
 * @Date 2025/8/19 17:00
 * @Description 根节点，数据加载
 * @Author jerryhotton
 */

@Slf4j
@Service(value = "armoryRootNode")
public class RootNode extends AbstractArmorySupport {

    private final Map<String, ILoadDataStrategy> loadDataStrategyMap;

    @Resource
    private AiClientApiNode aiClientApiNode;

    public RootNode(
            Map<String, ILoadDataStrategy> loadDataStrategyMap,
            ApplicationContext applicationContext) {
        super(applicationContext);
        this.loadDataStrategyMap = loadDataStrategyMap;
    }

    @Override
    protected void multiThread(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {
        String commandType = requestParameter.getCommandType();
        ILoadDataStrategy loadDataStrategy = loadDataStrategyMap.get(commandType);
        if (loadDataStrategy == null) {
            log.error("未找到数据加载策略，commandType={}", commandType);
            return;
        }
        loadDataStrategy.loadData(requestParameter, dynamicContext);
    }

    @Override
    protected String doApply(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai 智能体 构建，数据加载节点 {}", JSON.toJSONString(requestParameter));
        multiThread(requestParameter, dynamicContext);

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext, String> get(ArmoryCommandEntity armoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return aiClientApiNode;
    }

}
