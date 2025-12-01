package cn.cug.sxy.ai.domain.rag.service.intent;

import cn.cug.sxy.ai.domain.rag.model.entity.IntentRule;
import cn.cug.sxy.ai.domain.rag.repository.IIntentRuleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @Date 2025/11/20 15:00
 * @Description 意图规则领域服务，负责规则的加载、缓存和CRUD操作
 * @Author jerryhotton
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentRuleService {

    private final IIntentRuleRepository intentRuleRepository;

    // 缓存规则：Type -> List<Rule>
    private final Map<String, List<IntentRule>> ruleCache = new ConcurrentHashMap<>();

    private static final Comparator<IntentRule> RULE_COMPARATOR = Comparator
            .comparing((IntentRule r) -> r.getPriority() == null ? Integer.MAX_VALUE : r.getPriority())
            .thenComparing((IntentRule r) -> r.getConfidence() == null ? 0d : r.getConfidence(), Comparator.reverseOrder());

    //@PostConstruct
    public void init() {
        refreshRules();
    }

    public void refreshRules() {
        log.info("开始刷新意图识别规则...");
        try {
            // 1. 从数据库加载所有启用状态的规则
            List<IntentRule> allRules = intentRuleRepository.findAllActive();
            
            // 2. 按规则类型 (KEYWORD, REGEX, SEMANTIC_EXAMPLE) 进行分组并排序
            Map<String, List<IntentRule>> groupedRules = allRules.stream()
                    .collect(Collectors.groupingBy(IntentRule::getRuleType,
                            Collectors.collectingAndThen(Collectors.toList(), list -> list.stream()
                                    .sorted(RULE_COMPARATOR)
                                    .toList())));

            // 3. 原子替换缓存
            ruleCache.clear();
            ruleCache.putAll(groupedRules);
            
            log.info("意图规则刷新完成，共加载 {} 条规则", allRules.size());
            ruleCache.forEach((type, rules) -> log.info("类型: {}, 数量: {}", type, rules.size()));
        } catch (Exception e) {
            log.error("刷新意图规则失败", e);
        }
    }

    /**
     * 获取指定类型的规则
     * @param ruleType 规则类型 (KEYWORD, REGEX, SEMANTIC_EXAMPLE)
     * @return 规则列表
     */
    public List<IntentRule> getRulesByType(String ruleType) {
        return ruleCache.getOrDefault(ruleType, Collections.emptyList());
    }

    /**
     * 获取所有规则（包括禁用）
     */
    public List<IntentRule> getAllRules() {
        return intentRuleRepository.findAll();
    }

    /**
     * 创建新规则
     */
    public IntentRule createRule(IntentRule rule) {
        intentRuleRepository.save(rule);
        refreshRules(); // 立即刷新缓存
        return rule;
    }

    /**
     * 更新规则
     */
    public IntentRule updateRule(IntentRule rule) {
        intentRuleRepository.update(rule);
        refreshRules(); // 立即刷新缓存
        return rule;
    }

    /**
     * 删除规则
     */
    public void deleteRule(Long id) {
        intentRuleRepository.deleteById(id);
        refreshRules(); // 立即刷新缓存
    }

    /**
     * 批量删除规则
     */
    public void batchDeleteRules(List<Long> ids) {
        intentRuleRepository.batchDelete(ids);
        refreshRules(); // 立即刷新缓存
    }

    /**
     * 根据ID获取规则
     */
    public IntentRule getRuleById(Long id) {
        return intentRuleRepository.findById(id).orElse(null);
    }
}
