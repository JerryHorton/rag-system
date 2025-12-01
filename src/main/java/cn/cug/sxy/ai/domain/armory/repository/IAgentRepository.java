package cn.cug.sxy.ai.domain.armory.repository;

import cn.cug.sxy.ai.domain.armory.model.valobj.*;

import java.util.List;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/8/19 14:42
 * @Description Agent仓储接口
 * @Author jerryhotton
 */

public interface IAgentRepository {

    /**
     * 根据客户端ID查询客户端API配置
     *
     * @param clientIdList 客户端ID列表
     * @return 客户端API配置列表
     */
    List<AiClientApiVO> queryAiClientApiVOListByClientIds(List<String> clientIdList);

    /**
     * 根据客户端ID查询客户端模型配置
     *
     * @param clientIdList 客户端ID列表
     * @return 客户端模型配置列表
     */
    List<AiClientModelVO> queryAiClientModelVOListByClientIds(List<String> clientIdList);

    /**
     * 根据客户端ID查询客户端工具配置
     *
     * @param clientIdList 客户端ID列表
     * @return 客户端工具配置列表
     */
    List<AiClientToolMcpVO> queryAiClientToolMcpVOListByClientIds(List<String> clientIdList);

    /**
     * 根据客户端ID查询客户端系统提示词配置
     *
     * @param clientIdList 客户端ID列表
     * @return 客户端系统提示词配置列表
     */
    List<AiClientSystemPromptVO> queryAiClientSystemPromptVOListByClientIds(List<String> clientIdList);

    /**
     * 根据客户端ID查询客户端系统提示词配置
     *
     * @param clientIdList 客户端ID列表
     * @return 客户端系统提示词配置列表
     */
    Map<String, AiClientSystemPromptVO> queryAiClientSystemPromptVOMapByClientIds(List<String> clientIdList);

    /**
     * 根据客户端ID查询客户端配置
     *
     * @param clientIdList 客户端ID列表
     * @return 客户端配置列表
     */
    List<AiClientVO> queryAiClientVOListByClientIds(List<String> clientIdList);

    /**
     * 根据模型ID查询API配置
     *
     * @param modelIdList 模型ID列表
     * @return API配置列表
     */
    List<AiClientApiVO> queryAiClientApiVOListByModelIds(List<String> modelIdList);

    /**
     * 根据模型ID查询模型配置
     *
     * @param modelIdList 模型ID列表
     * @return 模型配置列表
     */
    List<AiClientModelVO> queryAiClientModelVOListByModelIds(List<String> modelIdList);

     /**
     * 查询所有启用的客户端ID
     *
     * @return 所有启用的客户端ID列表
     */
    List<String> queryAllEnabledClientIds();

}
