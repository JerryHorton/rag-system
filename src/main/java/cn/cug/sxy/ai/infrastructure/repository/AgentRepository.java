package cn.cug.sxy.ai.infrastructure.repository;

import cn.cug.sxy.ai.domain.armory.repository.IAgentRepository;
import cn.cug.sxy.ai.domain.armory.model.valobj.*;
import cn.cug.sxy.ai.infrastructure.dao.mysql.*;
import cn.cug.sxy.ai.infrastructure.dao.po.*;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @Date 2025/8/19 15:12
 * @Description AiAgent 仓储实现
 * @Author jerryhotton
 */

@Slf4j
@Repository
public class AgentRepository implements IAgentRepository {

    private final IAiClientDao aiClientDao;
    private final IAiClientApiDao aiClientApiDao;
    private final IAiClientConfigDao aiClientConfigDao;
    private final IAiClientModelDao aiClientModelDao;
    private final IAiClientSystemPromptDao aiClientSystemPromptDao;
    private final IAiClientToolMcpDao aiClientToolMcpDao;

    public AgentRepository(
            IAiClientDao aiClientDao,
            IAiClientApiDao aiClientApiDao,
            IAiClientConfigDao aiClientConfigDao,
            IAiClientModelDao aiClientModelDao,
            IAiClientSystemPromptDao aiClientSystemPromptDao,
            IAiClientToolMcpDao aiClientToolMcpDao) {
        this.aiClientDao = aiClientDao;
        this.aiClientApiDao = aiClientApiDao;
        this.aiClientConfigDao = aiClientConfigDao;
        this.aiClientModelDao = aiClientModelDao;
        this.aiClientSystemPromptDao = aiClientSystemPromptDao;
        this.aiClientToolMcpDao = aiClientToolMcpDao;
    }

    @Override
    public List<AiClientApiVO> queryAiClientApiVOListByClientIds(List<String> clientIdList) {
        if (CollectionUtils.isEmpty(clientIdList)) {
            return List.of();
        }
        List<AiClientApiVO> result = new ArrayList<>();
        Set<String> processedClientApiIds = new HashSet<>();
        for (String clientId : clientIdList) {
            // 1. 通过clientId查询关联的modelId
            List<AiClientConfigPO> configs = aiClientConfigDao.queryBySourceTypeAndId(AiDataTypeVO.AI_CLIENT.getCode(), clientId);
            for (AiClientConfigPO config : configs) {
                if (AiDataTypeVO.AI_CLIENT_MODEL.getCode().equals(config.getTargetType()) && config.getStatus() == 1) {
                    String modelId = config.getTargetId();
                    // 2. 通过modelId查询模型配置，获取apiId
                    AiClientModelPO model = aiClientModelDao.queryByModelId(modelId);
                    if (model != null && model.getStatus() == 1) {
                        String apiId = model.getApiId();
                        // 避免重复添加相同的API配置
                        if (processedClientApiIds.contains(apiId)) {
                            continue;
                        }
                        processedClientApiIds.add(apiId);
                        // 3. 通过apiId查询API配置信息
                        AiClientApiPO apiConfig = aiClientApiDao.queryByApiId(apiId);
                        if (apiConfig != null && apiConfig.getStatus() == 1) {
                            // 4. 转换为VO对象
                            AiClientApiVO apiVO = AiClientApiVO.builder()
                                    .apiId(apiConfig.getApiId())
                                    .baseUrl(apiConfig.getBaseUrl())
                                    .apiKey(apiConfig.getApiKey())
                                    .completionsPath(apiConfig.getCompletionsPath())
                                    .embeddingsPath(apiConfig.getEmbeddingsPath())
                                    .build();
                            result.add(apiVO);
                        }
                    }
                }
            }
        }

        return result;
    }

    @Override
    public List<AiClientModelVO> queryAiClientModelVOListByClientIds(List<String> clientIdList) {
        if (clientIdList == null || clientIdList.isEmpty()) {
            return List.of();
        }
        List<AiClientModelVO> result = new ArrayList<>();
        Set<String> processedModelIds = new HashSet<>();
        for (String clientId : clientIdList) {
            // 1. 通过clientId查询关联的modelId
            List<AiClientConfigPO> modelConfigs = aiClientConfigDao.queryBySourceTypeAndId(AiDataTypeVO.AI_CLIENT.getCode(), clientId);
            for (AiClientConfigPO modelConfig : modelConfigs) {
                if (AiDataTypeVO.AI_CLIENT_MODEL.getCode().equals(modelConfig.getTargetType()) && modelConfig.getStatus() == 1) {
                    String modelId = modelConfig.getTargetId();
                    if (processedModelIds.contains(modelId)) {
                        continue;
                    }
                    processedModelIds.add(modelId);
                    // 2. 通过modelId查询模型配置
                    AiClientModelPO model = aiClientModelDao.queryByModelId(modelId);
                    if (model != null && model.getStatus() == 1) {
                        // 3. 查询当前模型关联的 Tool MCP 配置
                        List<AiClientConfigPO> toolMcpConfigs = aiClientConfigDao.queryBySourceTypeAndId(AiDataTypeVO.AI_CLIENT_MODEL.getCode(), modelId);
                        Set<String> toolMcpIds = toolMcpConfigs.stream()
                                .filter(item -> AiDataTypeVO.AI_CLIENT_TOOL_MCP.getCode().equals(item.getTargetType()))
                                .map(AiClientConfigPO::getTargetId)
                                .collect(Collectors.toSet());
                        // 4. 转换为VO对象
                        AiClientModelVO modelVO = AiClientModelVO.builder()
                                .modelId(model.getModelId())
                                .apiId(model.getApiId())
                                .toolMcpIds(new ArrayList<>(toolMcpIds))
                                .modelName(model.getModelName())
                                .modelType(model.getModelType())
                                .build();
                        // 避免重复添加相同的模型配置
                        result.add(modelVO);
                    }
                }
            }
        }

        return result;
    }

    @Override
    public List<AiClientToolMcpVO> queryAiClientToolMcpVOListByClientIds(List<String> clientIdList) {
        if (clientIdList == null || clientIdList.isEmpty()) {
            return List.of();
        }
        List<AiClientToolMcpVO> result = new ArrayList<>();
        Set<String> processedMcpIds = new HashSet<>();
        for (String clientId : clientIdList) {
            // 1. 通过clientId查询关联的model配置
            List<AiClientConfigPO> clientConfigs = aiClientConfigDao.queryBySourceTypeAndId(AiDataTypeVO.AI_CLIENT.getCode(), clientId);
            for (AiClientConfigPO clientConfig : clientConfigs) {
                if (AiDataTypeVO.AI_CLIENT_MODEL.getCode().equals(clientConfig.getTargetType()) && clientConfig.getStatus() == 1) {
                    String modelId = clientConfig.getTargetId();
                    List<AiClientConfigPO> modelConfigs = aiClientConfigDao.queryBySourceTypeAndId(AiDataTypeVO.AI_CLIENT_MODEL.getCode(), modelId);
                    // 2. 通过modelId查询关联的tool_mcp配置
                    for (AiClientConfigPO modelConfig : modelConfigs) {
                        if (AiDataTypeVO.AI_CLIENT_TOOL_MCP.getCode().equals(modelConfig.getTargetType()) && modelConfig.getStatus() == 1) {
                            String mcpId = modelConfig.getTargetId();
                            if (processedMcpIds.contains(mcpId)) {
                                continue;
                            }
                            processedMcpIds.add(mcpId);
                            // 3. 通过mcpId查询ai_client_tool_mcp表获取MCP工具配置
                            AiClientToolMcpPO toolMcp = aiClientToolMcpDao.queryByMcpId(mcpId);
                            if (toolMcp != null && toolMcp.getStatus() == 1) {
                                // 4. 解析传输配置
                                AiClientToolMcpVO.TransportConfigSse transportConfigSse = null;
                                AiClientToolMcpVO.TransportConfigStdio transportConfigStdio = null;
                                String transportConfig = toolMcp.getTransportConfig();
                                if (StringUtils.isNotBlank(transportConfig)) {
                                    try {
                                        if (AiClientToolMcpVO.TransportType.SSE.getCode().equals(toolMcp.getTransportType())) {
                                            transportConfigSse = JSON.parseObject(transportConfig, new TypeReference<>() {
                                            });
                                        } else if (AiClientToolMcpVO.TransportType.STDIO.getCode().equals(toolMcp.getTransportType())) {
                                            transportConfigStdio = JSON.parseObject(transportConfig, new TypeReference<>() {
                                            });
                                        }
                                    } catch (Exception e) {
                                        // 解析失败时忽略，使用默认值nul
                                        log.error("解析MCP工具配置失败，McpId={}", toolMcp.getMcpId(), e);
                                    }
                                }
                                // 4. 转换为VO对象
                                AiClientToolMcpVO mcpVO = AiClientToolMcpVO.builder()
                                        .mcpId(toolMcp.getMcpId())
                                        .mcpName(toolMcp.getMcpName())
                                        .transportType(AiClientToolMcpVO.TransportType.fromCode(toolMcp.getTransportType()))
                                        .transportConfig(toolMcp.getTransportConfig())
                                        .requestTimeout(toolMcp.getRequestTimeout())
                                        .transportConfigSse(transportConfigSse)
                                        .transportConfigStdio(transportConfigStdio)
                                        .build();
                                result.add(mcpVO);
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    @Override
    public List<AiClientSystemPromptVO> queryAiClientSystemPromptVOListByClientIds(List<String> clientIdList) {
        if (clientIdList == null || clientIdList.isEmpty()) {
            return List.of();
        }
        List<AiClientSystemPromptVO> result = new ArrayList<>();
        Set<String> processedPromptIds = new HashSet<>();
        for (String clientId : clientIdList) {
            // 1. 通过clientId查询关联的prompt配置
            List<AiClientConfigPO> configs = aiClientConfigDao.queryBySourceTypeAndId(AiDataTypeVO.AI_CLIENT.getCode(), clientId);
            for (AiClientConfigPO config : configs) {
                if (AiDataTypeVO.AI_CLIENT_SYSTEM_PROMPT.getCode().equals(config.getTargetType()) && config.getStatus() == 1) {
                    String promptId = config.getTargetId();
                    // 避免重复添加相同的系统提示词配置
                    if (processedPromptIds.contains(promptId)) {
                        continue;
                    }
                    processedPromptIds.add(promptId);
                    // 2. 通过promptId查询ai_client_system_prompt表获取系统提示词配置
                    AiClientSystemPromptPO systemPrompt = aiClientSystemPromptDao.queryByPromptId(promptId);
                    if (systemPrompt != null && systemPrompt.getStatus() == 1) {
                        // 3. 转换为VO对象
                        AiClientSystemPromptVO promptVO = AiClientSystemPromptVO.builder()
                                .promptId(systemPrompt.getPromptId())
                                .promptName(systemPrompt.getPromptName())
                                .promptContent(systemPrompt.getPromptContent())
                                .description(systemPrompt.getDescription())
                                .build();
                        result.add(promptVO);
                    }
                }
            }
        }

        return result;
    }

    @Override
    public Map<String, AiClientSystemPromptVO> queryAiClientSystemPromptVOMapByClientIds(List<String> clientIdList) {
        List<AiClientSystemPromptVO> aiClientSystemPromptVOS = queryAiClientSystemPromptVOListByClientIds(clientIdList);
        if (CollectionUtils.isEmpty(aiClientSystemPromptVOS)) {
            return Map.of();
        }
        return aiClientSystemPromptVOS.stream()
                .collect(Collectors.toMap(
                        AiClientSystemPromptVO::getPromptId,
                        promptVO -> promptVO,
                        (oldValue, newValue) -> oldValue
                ));
    }

    @Override
    public List<AiClientVO> queryAiClientVOListByClientIds(List<String> clientIdList) {
        if (clientIdList == null || clientIdList.isEmpty()) {
            return List.of();
        }
        List<AiClientVO> result = new ArrayList<>();
        Set<String> processedClientIds = new HashSet<>();
        for (String clientId : clientIdList) {
            // 避免重复添加相同的客户端配置
            if (processedClientIds.contains(clientId)) {
                continue;
            }
            processedClientIds.add(clientId);
            // 1. 查询客户端基本信息
            AiClientPO aiClient = aiClientDao.queryByClientId(clientId);
            if (aiClient != null && aiClient.getStatus() == 1) {
                // 2. 查询客户端相关配置
                List<AiClientConfigPO> configs = aiClientConfigDao.queryBySourceTypeAndId(AiDataTypeVO.AI_CLIENT.getCode(), clientId);
                String modelId = null;
                List<String> promptIdList = new ArrayList<>();
                List<String> mcpIdList = new ArrayList<>();
                List<String> advisorIdList = new ArrayList<>();
                for (AiClientConfigPO config : configs) {
                    if (config.getStatus() != 1) {
                        continue;
                    }
                    switch (config.getTargetType()) {
                        case "model":
                            modelId = config.getTargetId();
                            break;
                        case "system_prompt":
                            promptIdList.add(config.getTargetId());
                            break;
                        case "tool_mcp":
                            mcpIdList.add(config.getTargetId());
                            break;
                        case "advisor":
                            break;
                        default:
                            log.warn("未处理的客户端配置类型 targetType={}", config.getTargetType());
                            break;
                    }
                }
                // 3. 构建AiClientVO对象
                AiClientVO aiClientVO = AiClientVO.builder()
                        .clientId(aiClient.getClientId())
                        .clientName(aiClient.getClientName())
                        .description(aiClient.getDescription())
                        .modelId(modelId)
                        .promptIdList(promptIdList)
                        .mcpIdList(mcpIdList)
                        .advisorIdList(advisorIdList)
                        .build();

                result.add(aiClientVO);
            }
        }

        return result;
    }

    @Override
    public List<AiClientApiVO> queryAiClientApiVOListByModelIds(List<String> modelIdList) {
        if (modelIdList == null || modelIdList.isEmpty()) {
            return List.of();
        }
        List<AiClientApiVO> result = new ArrayList<>();
        Set<String> processedClientIds = new HashSet<>();
        for (String modelId : modelIdList) {
            // 避免重复添加相同的客户端配置
            if (processedClientIds.contains(modelId)) {
                continue;
            }
            processedClientIds.add(modelId);
            // 1. 通过modelId查询模型配置，获取apiId
            AiClientModelPO model = aiClientModelDao.queryByModelId(modelId);
            if (model != null && model.getStatus() == 1) {
                String apiId = model.getApiId();
                // 2. 通过apiId查询API配置信息
                AiClientApiPO apiConfig = aiClientApiDao.queryByApiId(apiId);
                if (apiConfig != null && apiConfig.getStatus() == 1) {
                    // 3. 转换为VO对象
                    AiClientApiVO apiVO = AiClientApiVO.builder()
                            .apiId(apiConfig.getApiId())
                            .baseUrl(apiConfig.getBaseUrl())
                            .apiKey(apiConfig.getApiKey())
                            .completionsPath(apiConfig.getCompletionsPath())
                            .embeddingsPath(apiConfig.getEmbeddingsPath())
                            .build();
                    result.add(apiVO);
                }
            }
        }

        return result;
    }

    @Override
    public List<AiClientModelVO> queryAiClientModelVOListByModelIds(List<String> modelIdList) {
        if (modelIdList == null || modelIdList.isEmpty()) {
            return List.of();
        }
        List<AiClientModelVO> result = new ArrayList<>();
        Set<String> processedModelIds = new HashSet<>();
        for (String modelId : modelIdList) {
            // 避免重复添加相同的模型配置
            if (processedModelIds.contains(modelId)) {
                continue;
            }
            processedModelIds.add(modelId);
            // 通过modelId查询模型配置
            AiClientModelPO model = aiClientModelDao.queryByModelId(modelId);
            List<AiClientConfigPO> configs = aiClientConfigDao.queryBySourceTypeAndId(AiDataTypeVO.AI_CLIENT_MODEL.getCode(), modelId);
            Set<String> toolMcpIds = configs.stream()
                    .filter(config -> config.getTargetType().equals(AiDataTypeVO.AI_CLIENT_TOOL_MCP.getCode()))
                    .map(AiClientConfigPO::getTargetId)
                    .collect(Collectors.toSet());
            if (model != null && model.getStatus() == 1) {
                // 转换为VO对象
                AiClientModelVO modelVO = AiClientModelVO.builder()
                        .modelId(model.getModelId())
                        .apiId(model.getApiId())
                        .toolMcpIds(new ArrayList<>(toolMcpIds))
                        .modelName(model.getModelName())
                        .modelType(model.getModelType())
                        .build();
                result.add(modelVO);
            }
        }

        return result;
    }

    @Override
    public List<String> queryAllEnabledClientIds() {
        return aiClientDao.queryEnabledClientIds();
    }

}
