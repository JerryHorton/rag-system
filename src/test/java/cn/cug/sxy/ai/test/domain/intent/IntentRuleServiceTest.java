package cn.cug.sxy.ai.test.domain.intent;

import cn.cug.sxy.ai.domain.rag.model.entity.IntentRule;
import cn.cug.sxy.ai.domain.rag.repository.IIntentRuleRepository;
import cn.cug.sxy.ai.domain.rag.service.intent.IntentRuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntentRuleServiceTest {

    @Mock
    private IIntentRuleRepository intentRuleRepository;

    private IntentRuleService intentRuleService;

    @BeforeEach
    void setUp() {
        intentRuleService = new IntentRuleService(intentRuleRepository);
    }

    @Test
    void testRefreshRules() {
        // Arrange
        IntentRule rule1 = IntentRule.builder()
                .id(1L)
                .ruleType("KEYWORD")
                .ruleContent("test")
                .build();
        IntentRule rule2 = IntentRule.builder()
                .id(2L)
                .ruleType("REGEX")
                .ruleContent("\\d+")
                .build();

        when(intentRuleRepository.findAllActive()).thenReturn(Arrays.asList(rule1, rule2));

        // Act
        intentRuleService.refreshRules();

        // Assert
        List<IntentRule> keywordRules = intentRuleService.getRulesByType("KEYWORD");
        assertEquals(1, keywordRules.size());
        assertEquals("test", keywordRules.get(0).getRuleContent());

        List<IntentRule> regexRules = intentRuleService.getRulesByType("REGEX");
        assertEquals(1, regexRules.size());
        assertEquals("\\d+", regexRules.get(0).getRuleContent());

        List<IntentRule> semanticRules = intentRuleService.getRulesByType("SEMANTIC_EXAMPLE");
        assertTrue(semanticRules.isEmpty());
    }
}
