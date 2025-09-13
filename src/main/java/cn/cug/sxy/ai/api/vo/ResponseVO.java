package cn.cug.sxy.ai.api.vo;

import cn.cug.sxy.ai.domain.rag.model.entity.Response;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/12 09:22
 * @Description
 * @Author jerryhotton
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseVO {

    /**
     * 响应ID
     */
    private Long id;
    /**
     * 查询ID
     */
    private Long queryId;
    /**
     * 生成的回答文本
     */
    private String answer;
    /**
     * 来源文档信息列表
     */
    private List<SourceInfo> sources;
    /**
     * 评估指标
     */
    private Map<String, Number> evaluationMetrics;
    /**
     * 响应状态
     */
    private String status;
    /**
     * 响应时间
     */
    private LocalDateTime timestamp;
    /**
     * 处理延迟（毫秒）
     */
    private Long latencyMs;
    /**
     * 使用的处理类型（基础RAG、多查询、HyDE等）
     */
    private String processingType;
    /**
     * 其他元数据信息
     */
    private Map<String, Object> metadata;

    /**
     * 将Response实体转换为ResponseDto
     *
     * @param response 响应实体
     * @return ResponseDto对象
     */
    public static ResponseVO convertToResponseVO(Response response) {
        if (response == null) {
            return null;
        }
        ResponseVO vo = new ResponseVO();
        vo.setId(response.getId());
        vo.setQueryId(response.getQueryId());
        // 优先使用经过纠正的回答，如果有的话
        if (response.getCorrectedAnswerText() != null && !response.getCorrectedAnswerText().isEmpty()) {
            vo.setAnswer(response.getCorrectedAnswerText());
        } else {
            vo.setAnswer(response.getAnswerText());
        }
        // 设置评估指标
        Map<String, Number> metrics = new HashMap<>();
        if (response.getFaithfulnessScore() != null) {
            metrics.put("faithfulness", response.getFaithfulnessScore());
        }
        if (response.getRelevanceScore() != null) {
            metrics.put("relevance", response.getRelevanceScore());
        }
        // 可以从evaluationMetrics JSON中添加更多指标
        vo.setEvaluationMetrics(metrics);
        vo.setStatus(response.getStatus());
        vo.setTimestamp(response.getCompleteTime() != null ?
                response.getCompleteTime() : response.getCreateTime());
        vo.setLatencyMs(response.getLatencyMs());
        vo.setProcessingType(response.getResponseType());
        vo.setMetadata(response.getMetadata());
        // 解析来源信息
        vo.setSources(parseSourceInfo(response));

        return vo;
    }

    /**
     * 解析来源信息
     *
     * @param response 响应实体
     * @return 来源信息列表
     */
    private static List<SourceInfo> parseSourceInfo(Response response) {
        String json = response.getContextSources();
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> raw = mapper.readValue(json, new TypeReference<>() {
            });
            if (raw == null || raw.isEmpty()) {
                return null;
            }
            List<SourceInfo> list = new ArrayList<>();
            for (Map<String, Object> m : raw) {
                Long id = null;
                Object idObj = m.get("documentId");
                if (idObj instanceof Number) {
                    id = ((Number) idObj).longValue();
                } else if (idObj != null) {
                    try {
                        id = Long.parseLong(String.valueOf(idObj));
                    } catch (Exception ignore) {
                    }
                }
                String title = asString(m.get("title"));
                String snippet = asString(m.get("snippet"));
                if (snippet == null || snippet.isEmpty()) {
                    // 回退用 content 生成预览
                    String content = asString(m.get("content"));
                    if (content != null) {
                        snippet = content.length() > 200 ? content.substring(0, 200) + "..." : content;
                    }
                }
                String source = asString(m.get("source"));
                Double score = null;
                Object sc = m.get("score");
                if (sc instanceof Number) {
                    score = ((Number) sc).doubleValue();
                } else if (sc != null) {
                    try {
                        score = Double.parseDouble(String.valueOf(sc));
                    } catch (Exception ignore) {
                    }
                }
                Integer startPosition = null;
                Object sp = m.get("startPosition");
                if (sp instanceof Number) startPosition = ((Number) sp).intValue();
                Integer endPosition = null;
                Object ep = m.get("endPosition");
                if (ep instanceof Number) endPosition = ((Number) ep).intValue();
                if (id != null || title != null || snippet != null) {
                    SourceInfo si = new SourceInfo();
                    si.setId(id);
                    si.setTitle(title);
                    si.setSnippet(snippet);
                    si.setSource(source);
                    si.setScore(score);
                    si.setStartPosition(startPosition);
                    si.setEndPosition(endPosition);
                    list.add(si);
                }
            }
            return list.isEmpty() ? null : list;
        } catch (Exception e) {
            // 解析失败返回null，避免影响主结果
            return null;
        }
    }

    private static String asString(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o);
        return s.equalsIgnoreCase("null") ? null : s;
    }

    /**
     * 文档来源信息内部类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceInfo {

        /**
         * 文档ID
         */
        private Long id;
        /**
         * 文档标题
         */
        private String title;
        /**
         * 文档摘要或相关片段
         */
        private String snippet;
        /**
         * 文档来源（文件名/URL等）
         */
        private String source;
        /**
         * 相似度分数（0-1）
         */
        private Double score;
        /**
         * 片段起始位置（用于高亮）
         */
        private Integer startPosition;
        /**
         * 片段结束位置（用于高亮）
         */
        private Integer endPosition;
    }

}
