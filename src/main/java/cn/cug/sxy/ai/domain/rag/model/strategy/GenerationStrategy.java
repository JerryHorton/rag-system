package cn.cug.sxy.ai.domain.rag.model.strategy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 生成策略。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationStrategy {

    private String model;
    private Double temperature;
    private Integer maxTokens;
    private String promptTemplate;
    private boolean citationsRequired;
}

