package cn.cug.sxy.ai.trigger.http.converter;

/**
 * @version 1.0
 * @Date 2025/9/9 10:38
 * @Description vo转换器
 * @Author jerryhotton
 */

import cn.cug.sxy.ai.api.dto.IntentRuleDTO;
import cn.cug.sxy.ai.api.vo.IntentRuleVO;
import cn.cug.sxy.ai.domain.rag.model.entity.IntentRule;

public class ToVOConverter {

    private ToVOConverter() {
    }

    public static IntentRuleVO toIntentRuleVO(IntentRule rule) {
        if (rule == null) {
            return null;
        }
        return IntentRuleVO.builder()
                .id(rule.getId())
                .ruleType(rule.getRuleType())
                .ruleContent(rule.getRuleContent())
                .matchMode(rule.getMatchMode())
                .taskType(rule.getTaskType())
                .topicDomain(rule.getTopicDomain())
                .targetProcessor(rule.getTargetProcessor())
                .confidence(rule.getConfidence())
                .priority(rule.getPriority())
                .isActive(rule.getIsActive())
                .allowCascade(rule.getAllowCascade())
                .lockProcessor(rule.getLockProcessor())
                .routeKey(rule.getRouteKey())
                .semanticThreshold(rule.getSemanticThreshold())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }

    public static IntentRule toIntentRuleEntity(IntentRuleDTO dto) {
        if (dto == null) {
            return null;
        }
        return IntentRule.builder()
                .id(dto.getId())
                .ruleType(dto.getRuleType())
                .ruleContent(dto.getRuleContent())
                .matchMode(dto.getMatchMode())
                .taskType(dto.getTaskType())
                .topicDomain(dto.getTopicDomain())
                .targetProcessor(dto.getTargetProcessor())
                .confidence(dto.getConfidence())
                .priority(dto.getPriority())
                .isActive(dto.getIsActive())
                .allowCascade(dto.getAllowCascade())
                .lockProcessor(dto.getLockProcessor())
                .routeKey(dto.getRouteKey())
                .semanticThreshold(dto.getSemanticThreshold())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}
