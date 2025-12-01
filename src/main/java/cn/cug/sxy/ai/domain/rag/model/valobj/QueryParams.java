package cn.cug.sxy.ai.domain.rag.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @version 1.0
 * @Date 2025/9/12 16:10
 * @Description 查询参数
 * @Author jerryhotton
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QueryParams {

    /**
     * TopK
     */
    private Integer topK;
    /**
     * 最小相似度分数阈值（0-1）；为null时由检索器使用默认值
     */
    private Double minScore;
    /**
     * 相似度阈值（0-1）；为null时由检索器使用默认值
     */
    private Double similarityThreshold;
    /**
     * 路由
     */
    private String router;
    /**
     * 索引名称
     */
    private String indexName;

    /**
     * 额外限制
     */
    private Integer limit;

    private Integer candidateMultiplier;

    private Boolean rerankerEnabled;

    private Boolean multiQueryEnabled;

    private Boolean hydeEnabled;

    private Boolean stepBackEnabled;

    private Boolean selfRagEnabled;

    private String model;

    private Boolean clarify;

    private String forceType;

}
