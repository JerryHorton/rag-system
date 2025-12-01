package cn.cug.sxy.ai.domain.rag.service.intent.detector;

import cn.cug.sxy.ai.domain.rag.model.entity.IntentRule;
import cn.cug.sxy.ai.domain.rag.model.intent.ComplexityLevel;
import cn.cug.sxy.ai.domain.rag.model.intent.IntentSource;
import cn.cug.sxy.ai.domain.rag.model.intent.QueryIntent;
import cn.cug.sxy.ai.domain.rag.model.intent.TaskType;
import cn.cug.sxy.ai.domain.rag.model.intent.TopicDomain;
import cn.cug.sxy.ai.domain.rag.service.intent.IntentDetector;
import cn.cug.sxy.ai.domain.rag.service.intent.IntentRuleService;
import cn.cug.sxy.ai.domain.rag.service.query.QueryType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * 规则驱动的快速分诊 (数据库配置化).
 */
@Slf4j
@Component
@Order(-100)
@RequiredArgsConstructor
public class RuleBasedIntentDetector implements IntentDetector {

    private final IntentRuleService intentRuleService;
    private final ConcurrentMap<Long, Pattern> patternCache = new ConcurrentHashMap<>();

    @Override
    public DetectionResult detect(IntentRequest request) {
        String text = StringUtils.defaultString(request.queryText()).trim();
        if (text.isEmpty()) {
            return null;
        }

        DetectionCandidate best = null;
        best = evaluateRules(intentRuleService.getRulesByType("KEYWORD"), text, best, false);
        if (best != null && !best.intent().isAllowCascade()) {
            return best.result();
        }
        best = evaluateRules(intentRuleService.getRulesByType("REGEX"), text, best, true);

        return best != null ? best.result() : null;
    }

    private DetectionCandidate evaluateRules(List<IntentRule> rules, String text,
                                             DetectionCandidate best, boolean treatAsRegex) {
        for (IntentRule rule : rules) {
            if (matches(text, rule, treatAsRegex)) {
                DetectionCandidate candidate = new DetectionCandidate(
                        createResult(rule, buildReason(rule, treatAsRegex)),
                        rule.getPriority() == null ? Integer.MAX_VALUE : rule.getPriority(),
                        rule.getConfidence() == null ? 0d : rule.getConfidence());
                if (Boolean.FALSE.equals(rule.getAllowCascade())) {
                    return candidate;
                }
                best = pickBetter(best, candidate);
            }
        }
        return best;
    }

    private boolean matches(String text, IntentRule rule, boolean treatAsRegex) {
        String content = rule.getRuleContent();
        if (StringUtils.isBlank(content)) {
            return false;
        }
        MatchMode mode = resolveMatchMode(rule, treatAsRegex);
        return switch (mode) {
            case EXACT -> text.equals(content);
            case CONTAINS -> text.contains(content);
            case PREFIX -> text.startsWith(content);
            case SUFFIX -> text.endsWith(content);
            case REGEX -> regexMatch(text, rule);
        };
    }

    private MatchMode resolveMatchMode(IntentRule rule, boolean treatAsRegex) {
        if (treatAsRegex) {
            return MatchMode.REGEX;
        }
        String mode = rule.getMatchMode();
        if (StringUtils.isBlank(mode)) {
            return MatchMode.CONTAINS;
        }
        try {
            return MatchMode.valueOf(mode.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return MatchMode.CONTAINS;
        }
    }

    private boolean regexMatch(String text, IntentRule rule) {
        try {
            Pattern pattern = patternCache.compute(rule.getId(), (id, existing) -> {
                if (existing != null && existing.pattern().equals(rule.getRuleContent())) {
                    return existing;
                }
                return Pattern.compile(rule.getRuleContent());
            });
            return pattern.matcher(text).find();
        } catch (Exception e) {
            log.warn("无效的正则规则: id={}, content={}", rule.getId(), rule.getRuleContent());
            patternCache.remove(rule.getId());
            return false;
        }
    }

    private DetectionCandidate pickBetter(DetectionCandidate best, DetectionCandidate candidate) {
        if (best == null) {
            return candidate;
        }
        if (candidate.priority() < best.priority()) {
            return candidate;
        }
        if (candidate.priority() == best.priority() && candidate.confidence() > best.confidence()) {
            return candidate;
        }
        return best;
    }

    private DetectionResult createResult(IntentRule rule, String reason) {
        QueryType processor = QueryType.valueOf(rule.getTargetProcessor());
        QueryIntent intent = QueryIntent.builder()
                .taskType(TaskType.valueOf(rule.getTaskType()))
                .domain(TopicDomain.valueOf(rule.getTopicDomain()))
                .recommendedProcessor(processor)
                .lockProcessor(Boolean.TRUE.equals(rule.getLockProcessor()))
                .allowCascade(Boolean.TRUE.equals(rule.getAllowCascade()))
                .complexity(ComplexityLevel.MEDIUM)
                .multiStep(!QueryType.BASIC.equals(processor))
                .summary(reason)
                .secondaryProcessors(EnumSet.of(QueryType.BASIC))
                .source(IntentSource.RULE_BASED)
                .confidence(rule.getConfidence() == null ? 0.8 : rule.getConfidence())
                .build();

        return new DetectionResult(true, intent.getConfidence(), intent, reason);
    }

    private String buildReason(IntentRule rule, boolean regex) {
        return (regex ? "regex" : "keyword") + "-matched: " + rule.getRuleContent();
    }

    private record DetectionCandidate(DetectionResult result, int priority, double confidence) {
        QueryIntent intent() {
            return result.intent();
        }
    }

    private enum MatchMode {
        EXACT, CONTAINS, PREFIX, SUFFIX, REGEX
    }
}

