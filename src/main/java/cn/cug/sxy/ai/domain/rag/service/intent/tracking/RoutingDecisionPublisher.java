package cn.cug.sxy.ai.domain.rag.service.intent.tracking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 路由决策发布器。
 * 发布路由决策事件，供监控和诊断使用。
 * 后续可以扩展为事件总线、消息队列等。
 */
@Slf4j
@Component
public class RoutingDecisionPublisher {
    
    /**
     * 发布路由决策。
     * 
     * @param decision 路由决策
     */
    public void publish(RoutingDecision decision) {
        // 当前实现：记录日志
        // 后续可以扩展为：
        // 1. 发送到消息队列（Kafka/RabbitMQ）
        // 2. 写入时序数据库（InfluxDB/Prometheus）
        // 3. 发送到监控系统（ELK/Grafana）
        
        if (decision.getDetectorResults() != null && !decision.getDetectorResults().isEmpty()) {
            log.info("路由决策: query={}, intent={}, latency={}ms, cached={}, detectors={}",
                    decision.getQueryText() != null ? 
                            decision.getQueryText().substring(0, Math.min(50, decision.getQueryText().length())) : "",
                    decision.getFinalIntent() != null ? decision.getFinalIntent().getTaskType() : "UNKNOWN",
                    decision.getLatencyMs(),
                    decision.isCached(),
                    decision.getDetectorResults().size());
        } else {
            log.debug("路由决策: query={}, intent={}, latency={}ms",
                    decision.getQueryText() != null ? 
                            decision.getQueryText().substring(0, Math.min(50, decision.getQueryText().length())) : "",
                    decision.getFinalIntent() != null ? decision.getFinalIntent().getTaskType() : "UNKNOWN",
                    decision.getLatencyMs());
        }
    }
}

