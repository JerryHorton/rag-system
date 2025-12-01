package cn.cug.sxy.ai.domain.armory.service;

import cn.cug.sxy.ai.domain.armory.repository.IAgentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @version 1.0
 * @Date 2025/11/25 14:33
 * @Description AI代理客户端服务实现类
 * @Author jerryhotton
 */

@Service
public class AgentClientService  implements IAgentClientService {

    private final IAgentRepository agentRepository;

    public AgentClientService(IAgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    @Override
    public List<String> getAllEnabledClientIds() {
        return agentRepository.queryAllEnabledClientIds();
    }


}
