-- 插入意图识别规则种子数据
-- 这些规则来自原来的硬编码配置

-- 关键词规则
INSERT INTO intent_rule (rule_type, rule_content, task_type, topic_domain, target_processor, confidence, priority, is_active) VALUES
('KEYWORD', '对比', 'COMPARISON', 'GENERAL', 'MULTI_QUERY', 0.92, 10, true),
('KEYWORD', '比较', 'COMPARISON', 'GENERAL', 'MULTI_QUERY', 0.92, 10, true),
('KEYWORD', '优缺点', 'COMPARISON', 'GENERAL', 'MULTI_QUERY', 0.92, 10, true),
('KEYWORD', '不同', 'COMPARISON', 'GENERAL', 'MULTI_QUERY', 0.92, 10, true),
('KEYWORD', '差异', 'COMPARISON', 'GENERAL', 'MULTI_QUERY', 0.92, 10, true),

('KEYWORD', '总结', 'SUMMARIZATION', 'GENERAL', 'BASIC', 0.85, 10, true),
('KEYWORD', '概括', 'SUMMARIZATION', 'GENERAL', 'BASIC', 0.85, 10, true),
('KEYWORD', '归纳', 'SUMMARIZATION', 'GENERAL', 'BASIC', 0.85, 10, true),
('KEYWORD', '简述', 'SUMMARIZATION', 'GENERAL', 'BASIC', 0.85, 10, true),

('KEYWORD', '分析', 'ANALYSIS', 'GENERAL', 'STEP_BACK', 0.82, 10, true),
('KEYWORD', '原因', 'ANALYSIS', 'GENERAL', 'STEP_BACK', 0.82, 10, true),
('KEYWORD', '为什么', 'ANALYSIS', 'GENERAL', 'STEP_BACK', 0.82, 10, true),
('KEYWORD', '如何改进', 'ANALYSIS', 'GENERAL', 'STEP_BACK', 0.82, 10, true),

('KEYWORD', '报错', 'TROUBLESHOOT', 'TECH_SUPPORT', 'DECOMPOSITION', 0.8, 10, true),
('KEYWORD', '异常', 'TROUBLESHOOT', 'TECH_SUPPORT', 'DECOMPOSITION', 0.8, 10, true),
('KEYWORD', '失败', 'TROUBLESHOOT', 'TECH_SUPPORT', 'DECOMPOSITION', 0.8, 10, true),
('KEYWORD', '故障', 'TROUBLESHOOT', 'TECH_SUPPORT', 'DECOMPOSITION', 0.8, 10, true),
('KEYWORD', '无法', 'TROUBLESHOOT', 'TECH_SUPPORT', 'DECOMPOSITION', 0.8, 10, true);

-- 正则表达式规则
INSERT INTO intent_rule (rule_type, rule_content, task_type, topic_domain, target_processor, confidence, priority, is_active) VALUES
('REGEX', 'OD\w{2,}', 'ORDER_LOOKUP', 'ORDER', 'RETRIEVAL_AWARE', 0.95, 20, true),
('REGEX', '天气|温度|下雨|降雨|气温', 'WEATHER', 'WEATHER', 'RETRIEVAL_AWARE', 0.9, 20, true),
('REGEX', '\d{6,}.*错误', 'TROUBLESHOOT', 'TECH_SUPPORT', 'DECOMPOSITION', 0.78, 15, true);

-- 语义示例规则
INSERT INTO intent_rule (rule_type, rule_content, task_type, topic_domain, target_processor, confidence, priority, is_active) VALUES
('SEMANTIC_EXAMPLE', '退货政策是什么', 'FAQ', 'GENERAL', 'BASIC', 0.8, 5, true),
('SEMANTIC_EXAMPLE', '保修多久', 'FAQ', 'GENERAL', 'BASIC', 0.8, 5, true),
('SEMANTIC_EXAMPLE', '如何找客服', 'FAQ', 'GENERAL', 'BASIC', 0.8, 5, true),
('SEMANTIC_EXAMPLE', '工作时间', 'FAQ', 'GENERAL', 'BASIC', 0.8, 5, true),

('SEMANTIC_EXAMPLE', '比较A和B', 'COMPARISON', 'GENERAL', 'MULTI_QUERY', 0.8, 5, true),
('SEMANTIC_EXAMPLE', '列出差异', 'COMPARISON', 'GENERAL', 'MULTI_QUERY', 0.8, 5, true),
('SEMANTIC_EXAMPLE', '两个产品哪个好', 'COMPARISON', 'GENERAL', 'MULTI_QUERY', 0.8, 5, true),

('SEMANTIC_EXAMPLE', '分析原因', 'ANALYSIS', 'GENERAL', 'STEP_BACK', 0.8, 5, true),
('SEMANTIC_EXAMPLE', '为什么会这样', 'ANALYSIS', 'GENERAL', 'STEP_BACK', 0.8, 5, true),
('SEMANTIC_EXAMPLE', '如何改进流程', 'ANALYSIS', 'GENERAL', 'STEP_BACK', 0.8, 5, true),

('SEMANTIC_EXAMPLE', '接口报错500', 'TROUBLESHOOT', 'TECH_SUPPORT', 'DECOMPOSITION', 0.8, 5, true),
('SEMANTIC_EXAMPLE', '数据库连接失败', 'TROUBLESHOOT', 'TECH_SUPPORT', 'DECOMPOSITION', 0.8, 5, true),
('SEMANTIC_EXAMPLE', '应用崩溃', 'TROUBLESHOOT', 'TECH_SUPPORT', 'DECOMPOSITION', 0.8, 5, true),

('SEMANTIC_EXAMPLE', '总结会议记录', 'SUMMARIZATION', 'GENERAL', 'BASIC', 0.8, 5, true),
('SEMANTIC_EXAMPLE', '概括报告', 'SUMMARIZATION', 'GENERAL', 'BASIC', 0.8, 5, true),
('SEMANTIC_EXAMPLE', '提炼重点', 'SUMMARIZATION', 'GENERAL', 'BASIC', 0.8, 5, true),

('SEMANTIC_EXAMPLE', '查询订单状态', 'ORDER_LOOKUP', 'ORDER', 'RETRIEVAL_AWARE', 0.8, 5, true),
('SEMANTIC_EXAMPLE', '我的订单到哪了', 'ORDER_LOOKUP', 'ORDER', 'RETRIEVAL_AWARE', 0.8, 5, true);
