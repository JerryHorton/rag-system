package cn.cug.sxy.ai.domain.rag.service.intent;

import cn.cug.sxy.ai.domain.rag.model.intent.IntentSource;
import cn.cug.sxy.ai.domain.rag.model.intent.QueryIntent;
import cn.cug.sxy.ai.domain.rag.model.intent.TaskType;
import cn.cug.sxy.ai.domain.rag.model.intent.TopicDomain;
import cn.cug.sxy.ai.domain.rag.model.plan.TaskPlan;
import cn.cug.sxy.ai.domain.rag.service.intent.IntentDetector.DetectionResult;
import cn.cug.sxy.ai.domain.rag.service.intent.IntentDetector.IntentRequest;
import cn.cug.sxy.ai.domain.rag.service.intent.tracking.RoutingDecision;
import cn.cug.sxy.ai.domain.rag.service.intent.tracking.RoutingDecisionPublisher;
import cn.cug.sxy.ai.domain.rag.service.query.QueryType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 负责 orchestrate 多层意图识别。
 * 集成缓存和路由决策追踪，提升性能和可观测性。
 */
@Slf4j
@Service
public class IntentDetectionService {

    private final List<IntentDetector> detectors;
    
    private final RoutingDecisionPublisher routingDecisionPublisher;

    @Value("${rag.intent.confidence-threshold:0.65}")
    private double confidenceThreshold;
    
    @Value("${rag.intent.enable-cache:true}")
    private boolean enableCache;
    
    @Value("${rag.intent.enable-tracking:true}")
    private boolean enableTracking;

    public IntentDetectionService(List<IntentDetector> detectors,
                                 RoutingDecisionPublisher routingDecisionPublisher) {
        this.detectors = detectors.stream()
                .sorted(Comparator.comparingInt(IntentDetector::getOrder))
                .toList();
        this.routingDecisionPublisher = routingDecisionPublisher;
        log.info("已注册{}个意图检测器: {}", this.detectors.size(),
                this.detectors.stream().map(IntentDetector::getName).toList());
    }

    /**
     * 检测意图（带缓存和追踪）。
     * 注意：缓存条件为不缓存需要澄清的意图，因为澄清需要实时处理。
     */
    @Cacheable(value = "intentCache", key = "#request.queryText.hashCode()", 
               unless = "#result == null || #result.isRequiresClarification()")
    public QueryIntent detect(IntentRequest request) {
        Instant start = Instant.now();
        RoutingDecision decision = RoutingDecision.builder()
                .queryText(request.queryText())
                .userId(request.userId())
                .sessionId(request.sessionId())
                .timestamp(LocalDateTime.now())
                .detectorResults(new ArrayList<>())
                .cached(false)
                .build();
        
        DetectionResult bestCandidate = null;
        boolean cached = false;
        
        for (IntentDetector detector : detectors) {
            Instant detectorStart = Instant.now();
            try {
                DetectionResult result = detector.detect(request);
                long detectorLatency = java.time.Duration.between(detectorStart, Instant.now()).toMillis();
                
                if (enableTracking) {
                    decision.getDetectorResults().add(RoutingDecision.DetectorResult.builder()
                            .detectorName(detector.getName())
                            .matched(result != null && result.matched())
                            .confidence(result != null ? result.confidence() : 0.0)
                            .reason(result != null ? result.reason() : "未匹配")
                            .latencyMs(detectorLatency)
                            .build());
                }
                
                if (result != null && result.matched() && result.intent() != null) {
                    log.debug("意图检测器 {} 命中，confidence={}, reason={}",
                            detector.getName(), result.confidence(), result.reason());
                    QueryIntent intent = result.intent();
                    
                    // 检查是否来自缓存
                    if (intent.getAttributes() != null && 
                        "cached".equals(intent.getAttributes().get("plannerModel"))) {
                        cached = true;
                        decision.setCached(true);
                    }
                    
                    if ((result.confidence() >= confidenceThreshold && !intent.isAllowCascade())
                            || intent.isRequiresClarification()) {
                        decision.setFinalIntent(intent);
                        decision.setReason("高置信度匹配或需要澄清: " + detector.getName());
                        decision.setLatencyMs(java.time.Duration.between(start, Instant.now()).toMillis());
                        
                        if (enableTracking) {
                            routingDecisionPublisher.publish(decision);
                        }
                        
                        return intent;
                    }
                    if (bestCandidate == null || result.confidence() > bestCandidate.confidence()) {
                        bestCandidate = result;
                    }
                }
            } catch (Exception ex) {
                log.warn("意图检测器 {} 执行失败: {}", detector.getName(), ex.getMessage());
                if (enableTracking) {
                    decision.getDetectorResults().add(RoutingDecision.DetectorResult.builder()
                            .detectorName(detector.getName())
                            .matched(false)
                            .confidence(0.0)
                            .reason("执行失败: " + ex.getMessage())
                            .latencyMs(java.time.Duration.between(detectorStart, Instant.now()).toMillis())
                            .build());
                }
            }
        }
        
        if (bestCandidate != null) {
            decision.setFinalIntent(bestCandidate.intent());
            decision.setReason("选择最佳候选: " + bestCandidate.confidence());
        } else {
            QueryIntent fallbackIntent = buildFallbackIntent(request.queryText());
            decision.setFinalIntent(fallbackIntent);
            decision.setReason("所有检测器未命中，返回默认意图");
            log.info("所有检测器未命中，返回默认意图");
        }
        
        decision.setLatencyMs(java.time.Duration.between(start, Instant.now()).toMillis());
        
        if (enableTracking) {
            routingDecisionPublisher.publish(decision);
        }
        
        return decision.getFinalIntent();
    }

    private QueryIntent buildFallbackIntent(String queryText) {
        return QueryIntent.builder()
                .source(IntentSource.FALLBACK)
                .taskType(TaskType.UNKNOWN)
                .domain(TopicDomain.UNKNOWN)
                .complexity(queryText != null && queryText.length() > 120 ? cn.cug.sxy.ai.domain.rag.model.intent.ComplexityLevel.HIGH :
                        cn.cug.sxy.ai.domain.rag.model.intent.ComplexityLevel.MEDIUM)
                .multiStep(false)
                .requiresClarification(true)
                .lockProcessor(false)
                .allowCascade(false)
                .summary("无法识别的意图，建议向用户澄清。")
                .recommendedProcessor(QueryType.BASIC)
                .confidence(0.3)
                .taskPlan(TaskPlan.builder().summary("clarify").build())
                .build();
    }
}

