package cn.cug.sxy.ai.domain.armory.service.factory;

import cn.cug.sxy.ai.domain.armory.service.node.RootNode;
import cn.cug.sxy.design.framework.tree.handler.StrategyHandler;
import cn.cug.sxy.ai.domain.armory.model.entity.ArmoryCommandEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/8/19 14:36
 * @Description 默认装配工厂
 * @Author jerryhotton
 */

@Component
public class DefaultArmoryStrategyFactory {

    private final RootNode armoryRootNode;

    public DefaultArmoryStrategyFactory(RootNode armoryRootNode) {
        this.armoryRootNode = armoryRootNode;
    }

    public StrategyHandler<ArmoryCommandEntity, DynamicContext, String> armoryStrategyHandler(){
        return armoryRootNode;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {

        private Map<String, Object> dataObjects = new HashMap<>();

        public <T> void setValue(String key, T value) {
            dataObjects.put(key, value);
        }

        public <T> T getValue(String key) {
            return (T) dataObjects.get(key);
        }

    }

}
