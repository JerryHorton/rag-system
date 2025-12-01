package cn.cug.sxy.ai.domain.rag.service.orchestration;

import java.util.Map;

/**
 * 工具注册中心，后续可以改成基于数据库/配置的动态加载。
 */
public interface ToolRegistry {

    RagTool getTool(String name);

    Map<String, RagTool> getAllTools();
}


