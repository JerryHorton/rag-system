package cn.cug.sxy.ai.infrastructure.dao.mysql;

import cn.cug.sxy.ai.infrastructure.dao.po.AiClientToolMcpPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @version 1.0
 * @Date 2025/8/18 09:32
 * @Description MCP客户端配置表数据访问对象
 * @Author jerryhotton
 */

@Mapper
public interface IAiClientToolMcpDao {

    /**
     * 插入MCP客户端配置
     *
     * @param aiClientToolMcp MCP客户端配置对象
     * @return 影响行数
     */
    int insert(AiClientToolMcpPO aiClientToolMcp);

    /**
     * 根据ID更新MCP客户端配置
     *
     * @param aiClientToolMcp MCP客户端配置对象
     * @return 影响行数
     */
    int updateById(AiClientToolMcpPO aiClientToolMcp);

    /**
     * 根据MCP ID更新MCP客户端配置
     *
     * @param aiClientToolMcp MCP客户端配置对象
     * @return 影响行数
     */
    int updateByMcpId(AiClientToolMcpPO aiClientToolMcp);

    /**
     * 根据ID删除MCP客户端配置
     *
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteById(Long id);

    /**
     * 根据MCP ID删除MCP客户端配置
     *
     * @param mcpId MCP ID
     * @return 影响行数
     */
    int deleteByMcpId(String mcpId);

    /**
     * 根据ID查询MCP客户端配置
     *
     * @param id 主键ID
     * @return MCP客户端配置对象
     */
    AiClientToolMcpPO queryById(Long id);

    /**
     * 根据MCP ID查询MCP客户端配置
     *
     * @param mcpId MCP ID
     * @return MCP客户端配置对象
     */
    AiClientToolMcpPO queryByMcpId(String mcpId);

    /**
     * 查询所有MCP客户端配置
     *
     * @return MCP客户端配置列表
     */
    List<AiClientToolMcpPO> queryAll();

    /**
     * 根据状态查询MCP客户端配置
     *
     * @param status 状态
     * @return MCP客户端配置列表
     */
    List<AiClientToolMcpPO> queryByStatus(Integer status);

    /**
     * 根据传输类型查询MCP客户端配置
     *
     * @param transportType 传输类型
     * @return MCP客户端配置列表
     */
    List<AiClientToolMcpPO> queryByTransportType(String transportType);

    /**
     * 查询启用的MCP客户端配置
     *
     * @return MCP客户端配置列表
     */
    List<AiClientToolMcpPO> queryEnabledMcps();

}
