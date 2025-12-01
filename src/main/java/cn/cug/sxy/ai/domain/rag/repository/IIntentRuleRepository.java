package cn.cug.sxy.ai.domain.rag.repository;

import cn.cug.sxy.ai.domain.rag.model.entity.IntentRule;

import java.util.List;
import java.util.Optional;

/**
 * @version 1.0
 * @Description 意图规则仓储接口
 */
public interface IIntentRuleRepository {

    /**
     * 获取所有启用的规则
     * @return 规则列表
     */
    List<IntentRule> findAllActive();

    /**
     * 获取所有规则（含禁用）
     * @return 规则列表
     */
    List<IntentRule> findAll();

    /**
     * 根据ID查询规则
     * @param id 规则ID
     * @return 规则实体
     */
    Optional<IntentRule> findById(Long id);

    /**
     * 根据类型查询规则
     * @param ruleType 规则类型
     * @return 规则列表
     */
    List<IntentRule> findByType(String ruleType);

    /**
     * 保存规则
     * @param rule 规则实体
     * @return 影响行数
     */
    int save(IntentRule rule);

    /**
     * 更新规则
     * @param rule 规则实体
     * @return 影响行数
     */
    int update(IntentRule rule);

    /**
     * 删除规则
     * @param id 规则ID
     * @return 影响行数
     */
    int deleteById(Long id);

    /**
     * 批量删除规则
     * @param ids 规则ID列表
     * @return 影响行数
     */
    int batchDelete(List<Long> ids);
}
