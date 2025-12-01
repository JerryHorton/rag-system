package cn.cug.sxy.ai.infrastructure.dao.mysql;

import cn.cug.sxy.ai.infrastructure.dao.po.AiClientApiPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @version 1.0
 * @Date 2025/8/18 09:10
 * @Description AI客户端API配置表数据访问对象
 * @Author jerryhotton
 */

@Mapper
public interface IAiClientApiDao {

    /**
     * 插入AI客户端API配置
     *
     * @param aiClientApi AI客户端API配置对象
     * @return 影响行数
     */
    int insert(AiClientApiPO aiClientApi);

    /**
     * 根据ID更新AI客户端API配置
     *
     * @param aiClientApi AI客户端API配置对象
     * @return 影响行数
     */
    int updateById(AiClientApiPO aiClientApi);

    /**
     * 根据API ID更新AI客户端API配置
     *
     * @param aiClientApi AI客户端API配置对象
     * @return 影响行数
     */
    int updateByApiId(AiClientApiPO aiClientApi);

    /**
     * 根据ID删除AI客户端API配置
     *
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteById(Long id);

    /**
     * 根据API ID删除AI客户端API配置
     *
     * @param apiId API ID
     * @return 影响行数
     */
    int deleteByApiId(String apiId);

    /**
     * 根据ID查询AI客户端API配置
     *
     * @param id 主键ID
     * @return AI客户端API配置对象
     */
    AiClientApiPO queryById(Long id);

    /**
     * 根据API ID查询AI客户端API配置
     *
     * @param apiId API ID
     * @return AI客户端API配置对象
     */
    AiClientApiPO queryByApiId(String apiId);

    /**
     * 查询所有启用的AI客户端API配置
     *
     * @return AI客户端API配置列表
     */
    List<AiClientApiPO> queryEnabledApis();

    /**
     * 查询所有AI客户端API配置
     *
     * @return AI客户端API配置列表
     */
    List<AiClientApiPO> queryAll();

}
