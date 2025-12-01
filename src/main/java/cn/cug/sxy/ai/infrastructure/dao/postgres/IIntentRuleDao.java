package cn.cug.sxy.ai.infrastructure.dao.postgres;

import cn.cug.sxy.ai.infrastructure.dao.po.IntentRulePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @version 1.0
 * @Description 意图规则数据访问接口
 */
@Mapper
public interface IIntentRuleDao {

    /**
     * 获取所有启用的规则
     */
    List<IntentRulePO> selectAllActive();

    /**
     * 获取所有规则
     */
    List<IntentRulePO> selectAll();

    /**
     * 根据ID查询规则
     */
    IntentRulePO selectById(Long id);

    /**
     * 根据类型查询规则
     */
    List<IntentRulePO> selectByType(@Param("ruleType") String ruleType);

    /**
     * 插入规则
     */
    int insert(IntentRulePO rulePO);

    /**
     * 更新规则
     */
    int updateById(IntentRulePO rulePO);

    /**
     * 删除规则
     */
    int deleteById(Long id);

    /**
     * 批量删除规则
     */
    int batchDelete(@Param("ids") List<Long> ids);

}
