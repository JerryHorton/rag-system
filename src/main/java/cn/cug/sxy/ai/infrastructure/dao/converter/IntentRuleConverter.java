package cn.cug.sxy.ai.infrastructure.dao.converter;

import cn.cug.sxy.ai.domain.rag.model.entity.IntentRule;
import cn.cug.sxy.ai.infrastructure.dao.po.IntentRulePO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @Description IntentRule 实体与PO转换器
 */
@Component
public class IntentRuleConverter {

    public IntentRulePO toPO(IntentRule entity) {
        if (entity == null) {
            return null;
        }
        return IntentRulePO.builder()
                .id(entity.getId())
                .ruleType(entity.getRuleType())
                .ruleContent(entity.getRuleContent())
                .taskType(entity.getTaskType())
                .topicDomain(entity.getTopicDomain())
                .targetProcessor(entity.getTargetProcessor())
                .confidence(entity.getConfidence())
                .priority(entity.getPriority())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public IntentRule toEntity(IntentRulePO po) {
        if (po == null) {
            return null;
        }
        return IntentRule.builder()
                .id(po.getId())
                .ruleType(po.getRuleType())
                .ruleContent(po.getRuleContent())
                .taskType(po.getTaskType())
                .topicDomain(po.getTopicDomain())
                .targetProcessor(po.getTargetProcessor())
                .confidence(po.getConfidence())
                .priority(po.getPriority())
                .isActive(po.getIsActive())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    public List<IntentRule> toEntityList(List<IntentRulePO> pos) {
        if (pos == null) {
            return null;
        }
        return pos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
}
