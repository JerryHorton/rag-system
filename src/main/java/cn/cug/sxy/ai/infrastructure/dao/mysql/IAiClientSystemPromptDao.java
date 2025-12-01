package cn.cug.sxy.ai.infrastructure.dao.mysql;

import cn.cug.sxy.ai.infrastructure.dao.po.AiClientSystemPromptPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @version 1.0
 * @Date 2025/8/18 09:30
 * @Description 系统提示词配置表数据访问对象
 * @Author jerryhotton
 */

@Mapper
public interface IAiClientSystemPromptDao {

    /**
     * 插入系统提示词配置
     *
     * @param aiClientSystemPrompt 系统提示词配置
     */
    void insert(AiClientSystemPromptPO aiClientSystemPrompt);

    /**
     * 根据ID更新系统提示词配置
     *
     * @param aiClientSystemPrompt 系统提示词配置
     * @return 影响行数
     */
    int updateById(AiClientSystemPromptPO aiClientSystemPrompt);

    /**
     * 根据提示词ID更新系统提示词配置
     *
     * @param aiClientSystemPrompt 系统提示词配置
     * @return 影响行数
     */
    int updateByPromptId(AiClientSystemPromptPO aiClientSystemPrompt);

    /**
     * 根据ID删除系统提示词配置
     *
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteById(Long id);

    /**
     * 根据提示词ID删除系统提示词配置
     *
     * @param promptId 提示词ID
     * @return 影响行数
     */
    int deleteByPromptId(String promptId);

    /**
     * 根据ID查询系统提示词配置
     *
     * @param id 主键ID
     * @return 系统提示词配置
     */
    AiClientSystemPromptPO queryById(Long id);

    /**
     * 根据提示词ID查询系统提示词配置
     *
     * @param promptId 提示词ID
     * @return 系统提示词配置
     */
    AiClientSystemPromptPO queryByPromptId(String promptId);

    /**
     * 查询启用的系统提示词配置
     *
     * @return 系统提示词配置列表
     */
    List<AiClientSystemPromptPO> queryEnabledPrompts();

    /**
     * 根据提示词名称查询系统提示词配置
     *
     * @param promptName 提示词名称
     * @return 系统提示词配置列表
     */
    List<AiClientSystemPromptPO> queryByPromptName(String promptName);

    /**
     * 查询所有系统提示词配置
     *
     * @return 系统提示词配置列表
     */
    List<AiClientSystemPromptPO> queryAll();

}
