package cn.cug.sxy.ai.domain.rag.service.orchestration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简单的内存工具注册表，先手动配置几个工具，后续可改为动态配置/MCP。
 */
@Slf4j
@Component
public class InMemoryToolRegistry implements ToolRegistry {

    private final Map<String, RagTool> registry = new HashMap<>();

    public InMemoryToolRegistry(List<RagTool> ragQueryTool) {
        ragQueryTool.forEach(this::register);
        log.info("工具注册表初始化完成，已注册工具: {}", registry.keySet());
    }

    private void register(RagTool tool) {
        registry.put(tool.getName(), tool);
    }

    @Override
    public RagTool getTool(String name) {
        return registry.get(name);
    }

    @Override
    public Map<String, RagTool> getAllTools() {
        return Collections.unmodifiableMap(registry);
    }
}


