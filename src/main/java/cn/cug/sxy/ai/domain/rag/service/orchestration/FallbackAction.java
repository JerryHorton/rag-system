package cn.cug.sxy.ai.domain.rag.service.orchestration;

/**
 * 动态执行过程中可能采取的回退动作。
 * 实现三级降级机制：工具级重试 -> 任务级澄清 -> 系统级致歉
 */
public enum FallbackAction {
    /**
     * 使用相同工具重新执行一次（可调整参数）
     * 第一级：工具级重试
     */
    RETRY_SAME_TOOL,
    /**
     * 切换到其他工具继续执行
     * 第一级：工具级切换
     */
    SWITCH_TOOL,
    /**
     * 请求用户澄清意图
     * 第二级：任务级澄清
     */
    CLARIFY,
    /**
     * 终止当前任务计划
     * 第三级：系统级致歉
     */
    ABORT
}

