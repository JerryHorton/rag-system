package cn.cug.sxy.ai.domain.rag.model.strategy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 检索策略指令。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalStrategy {

    private Integer topK;
    private Integer limit;
    private Double minScore;
    private Double similarityThreshold;
    private Boolean hybridEnabled;
    private Boolean rerankerEnabled;
    private Boolean multiQueryEnabled;
    private Boolean hydeEnabled;
    private Boolean stepBackEnabled;
    private Boolean selfRagEnabled;
    private String indexName;
    private String router;
}

