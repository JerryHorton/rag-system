package cn.cug.sxy.ai.domain.document.model.valobj;

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

}
