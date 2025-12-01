package cn.cug.sxy.ai.domain.armory.service.strategy;

import cn.cug.sxy.ai.domain.armory.model.entity.ArmoryCommandEntity;
import cn.cug.sxy.ai.domain.armory.service.factory.DefaultArmoryStrategyFactory;

/**
 * @version 1.0
 * @Date 2025/8/19 17:47
 * @Description 数据加载策略
 * @Author jerryhotton
 */

public interface ILoadDataStrategy {

    void loadData(ArmoryCommandEntity armoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext dynamicContext);

}
