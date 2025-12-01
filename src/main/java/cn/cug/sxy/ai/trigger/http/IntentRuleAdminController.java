package cn.cug.sxy.ai.trigger.http;

import cn.cug.sxy.ai.api.IIntentRuleAdminService;
import cn.cug.sxy.ai.api.dto.IntentRuleDTO;
import cn.cug.sxy.ai.api.response.ApiResponse;
import cn.cug.sxy.ai.api.vo.IntentRuleVO;
import cn.cug.sxy.ai.domain.rag.model.entity.IntentRule;
import cn.cug.sxy.ai.domain.rag.service.intent.IntentRuleService;
import cn.cug.sxy.ai.domain.rag.service.intent.detector.SemanticIntentDetector;
import cn.cug.sxy.ai.trigger.http.converter.ToVOConverter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @Date 2025/11/20 15:00
 * @Description 意图规则管理API接口实现
 * @Author jerryhotton
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/intent-rules")
@RequiredArgsConstructor
public class IntentRuleAdminController implements IIntentRuleAdminService {

    private final IntentRuleService intentRuleService;
    private final SemanticIntentDetector semanticIntentDetector;

    @Override
    @PostMapping("/refresh")
    public ApiResponse<Map<String, Object>> refreshRules() {
        try {
            refreshCaches();
            return ApiResponse.success("意图规则已刷新", Map.of("timestamp", LocalDateTime.now()));
        } catch (Exception e) {
            log.error("刷新意图规则失败", e);
            return ApiResponse.error("刷新失败: " + e.getMessage());
        }
    }

    @Override
    @GetMapping
    public ApiResponse<List<IntentRuleVO>> listRules() {
        List<IntentRule> rules = intentRuleService.getAllRules();
        List<IntentRuleVO> vos = rules.stream()
                .map(ToVOConverter::toIntentRuleVO)
                .collect(Collectors.toList());
        return ApiResponse.success(vos);
    }

    @Override
    @PostMapping
    public ApiResponse<IntentRuleVO> createRule(@RequestBody @Valid IntentRuleDTO dto) {
        IntentRule rule = ToVOConverter.toIntentRuleEntity(dto);
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        if (rule.getIsActive() == null) {
            rule.setIsActive(true);
        }
        
        IntentRule savedRule = intentRuleService.createRule(rule);
        refreshCaches();
        return ApiResponse.success("规则创建成功", ToVOConverter.toIntentRuleVO(savedRule));
    }

    @Override
    @PutMapping("/{id}")
    public ApiResponse<IntentRuleVO> updateRule(@PathVariable Long id,
                                                                @RequestBody @Valid IntentRuleDTO dto) {
        IntentRule existingRule = intentRuleService.getRuleById(id);
        if (existingRule == null) {
            return ApiResponse.error("规则不存在");
        }

        IntentRule rule = ToVOConverter.toIntentRuleEntity(dto);
        rule.setId(id);
        rule.setCreatedAt(existingRule.getCreatedAt());
        rule.setUpdatedAt(LocalDateTime.now());
        
        IntentRule updatedRule = intentRuleService.updateRule(rule);
        refreshCaches();
        return ApiResponse.success("规则更新成功", ToVOConverter.toIntentRuleVO(updatedRule));
    }

    @Override
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRule(@PathVariable Long id) {
        intentRuleService.deleteRule(id);
        refreshCaches();
        return ApiResponse.success("规则删除成功", null);
    }

    private void refreshCaches() {
        intentRuleService.refreshRules();
        semanticIntentDetector.refreshRoutes();
    }

}
