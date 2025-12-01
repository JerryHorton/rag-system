-- 意图规则表
CREATE TABLE IF NOT EXISTS intent_rule (
    id BIGSERIAL PRIMARY KEY,
    rule_type VARCHAR(50) NOT NULL, -- KEYWORD, REGEX, SEMANTIC_EXAMPLE
    rule_content TEXT NOT NULL,
    task_type VARCHAR(50) NOT NULL,
    topic_domain VARCHAR(50) NOT NULL,
    target_processor VARCHAR(50) NOT NULL,
    confidence DOUBLE PRECISION DEFAULT 0.5,
    priority INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE intent_rule IS '意图识别规则表';
COMMENT ON COLUMN intent_rule.rule_type IS '规则类型: KEYWORD, REGEX, SEMANTIC_EXAMPLE';
COMMENT ON COLUMN intent_rule.rule_content IS '规则内容: 关键词、正则模式或语义示例';
COMMENT ON COLUMN intent_rule.task_type IS '目标任务类型';
COMMENT ON COLUMN intent_rule.topic_domain IS '目标领域';
COMMENT ON COLUMN intent_rule.target_processor IS '推荐的查询处理器';
COMMENT ON COLUMN intent_rule.confidence IS '规则置信度';
COMMENT ON COLUMN intent_rule.priority IS '优先级(数值越大越优先)';
COMMENT ON COLUMN intent_rule.is_active IS '是否启用';

-- 创建索引
CREATE INDEX idx_intent_rule_type ON intent_rule(rule_type);
CREATE INDEX idx_intent_rule_active ON intent_rule(is_active);
