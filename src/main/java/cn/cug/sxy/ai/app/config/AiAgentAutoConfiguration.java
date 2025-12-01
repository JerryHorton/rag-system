package cn.cug.sxy.ai.app.config;

import cn.cug.sxy.ai.domain.armory.model.entity.ArmoryCommandEntity;
import cn.cug.sxy.ai.domain.armory.model.valobj.AiDataTypeVO;
import cn.cug.sxy.ai.domain.armory.service.IAgentClientService;
import cn.cug.sxy.ai.domain.armory.service.factory.DefaultArmoryStrategyFactory;
import cn.cug.sxy.design.framework.tree.handler.StrategyHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @version 1.0
 * @Date 2025/8/26 14:32
 * @Description AI Agent 自动装配配置类
 * @Author jerryhotton
 */

@Slf4j
//@Configuration
public class AiAgentAutoConfiguration implements ApplicationListener<ApplicationReadyEvent> {

    @Resource
    private DefaultArmoryStrategyFactory defaultArmoryStrategyFactory;

    @Resource
    private IAgentClientService agentClientService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            log.info("AI Agent 自动装配开始");
            List<String> clientIds = agentClientService.getAllEnabledClientIds();
            if (CollectionUtils.isEmpty(clientIds)) {
                log.warn("AI Agent 自动装配配置的客户端ID列表为空");
                return;
            }
            log.info("开始自动装配AI客户端，客户端ID列表: {}", clientIds);
            // 执行自动装配
            StrategyHandler<ArmoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext, String> armoryStrategyHandler =
                    defaultArmoryStrategyFactory.armoryStrategyHandler();
            String result = armoryStrategyHandler.apply(
                    ArmoryCommandEntity.builder()
                            .commandType(AiDataTypeVO.AI_CLIENT.getLoadDataStrategy())
                            .commandIdList(clientIds)
                            .build(),
                    new DefaultArmoryStrategyFactory.DynamicContext());
            log.info("AI Agent 自动装配完成，结果: {}", result);
        } catch (Exception e) {
            log.error("AI Agent 自动装配失败", e);
        }
    }

}
