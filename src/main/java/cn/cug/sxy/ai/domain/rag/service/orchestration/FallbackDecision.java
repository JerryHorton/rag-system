package cn.cug.sxy.ai.domain.rag.service.orchestration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * FallbackManager 的决策结果。
 */
@Data
@Builder
@AllArgsConstructor
public class FallbackDecision {
    private FallbackAction action;
    private String nextToolName;
    private String message;

    public static FallbackDecision retry(String message) {
        return FallbackDecision.builder()
                .action(FallbackAction.RETRY_SAME_TOOL)
                .message(message)
                .build();
    }

    public static FallbackDecision switchTo(String toolName, String message) {
        return FallbackDecision.builder()
                .action(FallbackAction.SWITCH_TOOL)
                .nextToolName(toolName)
                .message(message)
                .build();
    }

    public static FallbackDecision abort(String message) {
        return FallbackDecision.builder()
                .action(FallbackAction.ABORT)
                .message(message)
                .build();
    }

    public static FallbackDecision clarify(String message) {
        return FallbackDecision.builder()
                .action(FallbackAction.CLARIFY)
                .message(message)
                .build();
    }
}

