package cn.cug.sxy.ai.domain.armory.service;

import java.util.List;

/**
 * @version 1.0
 * @Date 2025/11/25 14:32
 * @Description AI代理客户端服务接口
 * @Author jerryhotton
 */

public interface IAgentClientService {

    List<String> getAllEnabledClientIds();

}
