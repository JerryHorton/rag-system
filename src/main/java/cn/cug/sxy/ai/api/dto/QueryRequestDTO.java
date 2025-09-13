package cn.cug.sxy.ai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @version 1.0
 * @Date 2025/9/12 09:45
 * @Description 查询请求DTO
 * @Author jerryhotton
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QueryRequestDTO {

    /**
     * 查询语句
     */
    private String query;
    /**
     * 用户ID
     */
    private String userId;
    /**
     * 会话ID
     */
    private String sessionId;
    /**
     * 查询参数
     */
    private ExtraParams params;

    @Data
    public static class ExtraParams {

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

    }

}
