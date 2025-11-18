package cn.cug.sxy.ai.domain.rag.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @version 1.0
 * @Date 2025/9/10 17:53
 * @Description 检索参数对象
 * @Author jerryhotton
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RetrievalParams {

    /**
     * 返回结果数量（TopK）；为null时由检索器使用默认值
     */
    private Integer topK;
    /**
     * 最小相似度分数阈值（0-1）；为null时由检索器使用默认值
     */
    private Double minScore;
    /**
     * 指定索引名称（可选）
     */
    private String indexName;
    /**
     * 候选扩大倍数，默认4
     */
    private Integer candidateMultiplier;
    /**
     * 文档聚合策略：MEAN_TOP2 | MAX
     */
    private String docAgg;
    /**
     * 邻域窗口大小，默认1（左右各取1）
     */
    private Integer neighborWindow;
    /**
     * 每个文档最多选择的块数，默认2
     */
    private Integer perDocMaxChunks;
    /**
     * 最终上下文返回的全局上限，默认>=topK
     */
    private Integer maxContexts;
    /**
     * 兼容：如调用方仍希望单独传limit，可设置；若为空则使用topK
     */
    private Integer limit;

}
