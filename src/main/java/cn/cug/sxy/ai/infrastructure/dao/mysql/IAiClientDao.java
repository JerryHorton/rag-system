package cn.cug.sxy.ai.infrastructure.dao.mysql;

import cn.cug.sxy.ai.infrastructure.dao.po.AiClientPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @version 1.0
 * @Date 2025/8/18 09:27
 * @Description IAiClientDao
 * @Author jerryhotton
 */

@Mapper
public interface IAiClientDao {

    /**
     * 插入AI客户端配置
     *
     * @param aiClient AI客户端配置对象
     * @return 影响行数
     */
    int insert(AiClientPO aiClient);

    /**
     * 根据ID更新AI客户端配置
     *
     * @param aiClient AI客户端配置对象
     * @return 影响行数
     */
    int updateById(AiClientPO aiClient);

    /**
     * 根据客户端ID更新AI客户端配置
     *
     * @param aiClient AI客户端配置对象
     * @return 影响行数
     */
    int updateByClientId(AiClientPO aiClient);

    /**
     * 根据ID删除AI客户端配置
     *
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteById(Long id);

    /**
     * 根据客户端ID删除AI客户端配置
     *
     * @param clientId 客户端ID
     * @return 影响行数
     */
    int deleteByClientId(String clientId);

    /**
     * 根据ID查询AI客户端配置
     *
     * @param id 主键ID
     * @return AI客户端配置对象
     */
    AiClientPO queryById(Long id);

    /**
     * 根据客户端ID查询AI客户端配置
     *
     * @param clientId 客户端ID
     * @return AI客户端配置对象
     */
    AiClientPO queryByClientId(String clientId);

    /**
     * 查询所有启用的AI客户端配置
     *
     * @return AI客户端配置列表
     */
    List<AiClientPO> queryEnabledClients();

    /**
     * 根据客户端名称查询AI客户端配置
     *
     * @param clientName 客户端名称
     * @return AI客户端配置列表
     */
    List<AiClientPO> queryByClientName(String clientName);

    /**
     * 查询所有AI客户端配置
     *
     * @return AI客户端配置列表
     */
    List<AiClientPO> queryAll();

    /**
     * 查询所有启用的客户端ID列表
     *
     * @return 启用的客户端ID列表
     */
    List<String> queryEnabledClientIds();

}
