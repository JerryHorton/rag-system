package cn.cug.sxy.ai.domain.rag.model.intent;

/**
 * 意图检测来源类型。
 */
public enum IntentSource {
    RULE_BASED,
    SEMANTIC_ROUTER,
    LLM_PLANNER,
    FALLBACK
}

