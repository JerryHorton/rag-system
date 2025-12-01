package cn.cug.sxy.ai.domain.rag.service.intent;

import cn.cug.sxy.ai.domain.rag.model.intent.QueryIntent;

/**
 * 意图检测器接口。
 */
public interface IntentDetector {

    DetectionResult detect(IntentRequest request);

    /**
     * 顺序越小优先级越高。
     */
    default int getOrder() {
        return 0;
    }

    default String getName() {
        return getClass().getSimpleName();
    }

    record DetectionResult(boolean matched, double confidence, QueryIntent intent, String reason) { }

    record IntentRequest(String queryText,
                         String userId,
                         String sessionId,
                         cn.cug.sxy.ai.domain.rag.model.valobj.QueryParams params) { }
}

