package cn.cug.sxy.ai.api;

import cn.cug.sxy.ai.api.dto.IntentRuleDTO;
import cn.cug.sxy.ai.api.response.ApiResponse;
import cn.cug.sxy.ai.api.vo.IntentRuleVO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/11/20 15:00
 * @Description 意图规则管理API接口定义
 * @Author jerryhotton
 */
public interface IIntentRuleAdminService {

    /**
     * 手动刷新规则缓存
     *
     * @return 刷新结果
     */
    ApiResponse<Map<String, Object>> refreshRules();

    /**
     * 获取所有规则列表
     *
     * @return 规则列表DTO
     */
    ApiResponse<List<IntentRuleVO>> listRules();

    /**
     * 创建新规则
     *
     * @param dto 规则DTO对象
     * @return 创建后的规则DTO
     */
    ApiResponse<IntentRuleVO> createRule(@Valid IntentRuleDTO dto);

    /**
     * 更新规则
     *
     * @param id  规则ID
     * @param dto 规则DTO对象
     * @return 更新后的规则DTO
     */
    ApiResponse<IntentRuleVO> updateRule(Long id, @Valid IntentRuleDTO dto);

    /**
     * 删除规则
     *
     * @param id 规则ID
     * @return 空响应
     */
    ApiResponse<Void> deleteRule(Long id);
}

