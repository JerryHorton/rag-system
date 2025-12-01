package cn.cug.sxy.ai.domain.rag.service.intent.tracking;

import cn.cug.sxy.ai.domain.rag.model.intent.QueryIntent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 路由决策追踪记录。
 * 用于可观测性和问题诊断。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingDecision {
    
    /**
     * 查询文本
     */
    private String queryText;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 每个检测器的结果
     */
    @Builder.Default
    private List<DetectorResult> detectorResults = new ArrayList<>();
    
    /**
     * 最终确定的意图
     */
    private QueryIntent finalIntent;
    
    /**
     * 决策原因
     */
    private String reason;
    
    /**
     * 总延迟（毫秒）
     */
    private long latencyMs;
    
    /**
     * 时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * 是否命中缓存
     */
    private boolean cached;
    
    /**
     * 检测器结果。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetectorResult {
        /**
         * 检测器名称
         */
        private String detectorName;
        
        /**
         * 是否匹配
         */
        private boolean matched;
        
        /**
         * 置信度
         */
        private double confidence;
        
        /**
         * 原因
         */
        private String reason;
        
        /**
         * 延迟（毫秒）
         */
        private long latencyMs;
    }
}

