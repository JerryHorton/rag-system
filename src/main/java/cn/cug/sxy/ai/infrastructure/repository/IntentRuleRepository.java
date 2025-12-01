package cn.cug.sxy.ai.infrastructure.repository;

import cn.cug.sxy.ai.domain.rag.model.entity.IntentRule;
import cn.cug.sxy.ai.domain.rag.repository.IIntentRuleRepository;
import cn.cug.sxy.ai.infrastructure.dao.postgres.IIntentRuleDao;
import cn.cug.sxy.ai.infrastructure.dao.converter.IntentRuleConverter;
import cn.cug.sxy.ai.infrastructure.dao.po.IntentRulePO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @version 1.0
 * @Description 意图规则仓储实现
 */
@Repository
public class IntentRuleRepository implements IIntentRuleRepository {

    private final IIntentRuleDao intentRuleDao;
    private final IntentRuleConverter intentRuleConverter;

    public IntentRuleRepository(
            IIntentRuleDao intentRuleDao,
            IntentRuleConverter intentRuleConverter) {
        this.intentRuleDao = intentRuleDao;
        this.intentRuleConverter = intentRuleConverter;
    }

    @Override
    public List<IntentRule> findAllActive() {
        List<IntentRulePO> pos = intentRuleDao.selectAllActive();
        return intentRuleConverter.toEntityList(pos);
    }

    @Override
    public List<IntentRule> findAll() {
        List<IntentRulePO> pos = intentRuleDao.selectAll();
        return intentRuleConverter.toEntityList(pos);
    }

    @Override
    public Optional<IntentRule> findById(Long id) {
        IntentRulePO po = intentRuleDao.selectById(id);
        return Optional.ofNullable(intentRuleConverter.toEntity(po));
    }

    @Override
    public List<IntentRule> findByType(String ruleType) {
        List<IntentRulePO> pos = intentRuleDao.selectByType(ruleType);
        return intentRuleConverter.toEntityList(pos);
    }

    @Override
    public int save(IntentRule rule) {
        IntentRulePO po = intentRuleConverter.toPO(rule);
        int result = intentRuleDao.insert(po);
        rule.setId(po.getId());
        return result;
    }

    @Override
    public int update(IntentRule rule) {
        IntentRulePO po = intentRuleConverter.toPO(rule);
        return intentRuleDao.updateById(po);
    }

    @Override
    public int deleteById(Long id) {
        return intentRuleDao.deleteById(id);
    }

    @Override
    public int batchDelete(List<Long> ids) {
        return intentRuleDao.batchDelete(ids);
    }
}
