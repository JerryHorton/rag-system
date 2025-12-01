package cn.cug.sxy.ai.domain.rag.service.retrieval;

import cn.cug.sxy.ai.domain.rag.model.entity.Query;
import cn.cug.sxy.ai.domain.rag.model.valobj.RetrievalParams;
import cn.cug.sxy.ai.infrastructure.embedding.IEmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @version 1.0
 * @Date 2025/9/10 17:46
 * @Description 基于向量相似度的检索器实现
 * @Author jerryhotton
 */

@Slf4j
@Service("vectorSimilarityRetriever")
public class VectorSimilarityRetriever implements IRetriever {

    private final IEmbeddingService embeddingService;
    private final JdbcTemplate jdbcTemplate;

    public VectorSimilarityRetriever(
            IEmbeddingService embeddingService,
            JdbcTemplate jdbcTemplate) {
        this.embeddingService = embeddingService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Map<String, Object>> retrieve(Query query, RetrievalParams params) {
        log.info("执行向量相似度检索（Map结果），查询ID: {}", query.getId());
        // 读取参数与默认值
        int topK = params.getTopK();
        int limitParam = orDefault(params.getLimit(), topK);
        int candidateMultiplier = orDefault(params.getCandidateMultiplier(), 4);
        int candidateLimit = Math.max(topK, limitParam) * Math.max(1, candidateMultiplier);
        double minScore = params.getMinScore();
        String indexName = trimToNull(params.getIndexName());
        String docAgg = Optional.ofNullable(trimToNull(params.getDocAgg())).orElse("MEAN_TOP2");
        int neighborWindow = orDefault(params.getNeighborWindow(), 1);
        int perDocMaxChunks = orDefault(params.getPerDocMaxChunks(), 2);
        int maxContexts = orDefault(params.getMaxContexts(), Math.max(topK, 6));
        // 查询向量：优先使用 processedText；为空回退 originalText
        float[] queryVector = query.getVector();
        if (queryVector == null || queryVector.length == 0) {
            String text = query.getProcessedText() != null && !query.getProcessedText().isEmpty()
                    ? query.getProcessedText() : query.getOriginalText();
            queryVector = embeddingService.generateEmbedding(text);
            query.setVector(queryVector);
        }
        // 第一阶段：KNN 友好方式扩大候选集合
        List<Map<String, Object>> candidates = knnSearch(queryVector, candidateLimit, indexName);
        // 第二阶段：文档级聚合重排 + 邻域窗口拼接
        List<Map<String, Object>> reranked = rerankWithDocAggregation(candidates, topK, docAgg, neighborWindow, perDocMaxChunks, maxContexts);
        // 最终分数阈值过滤（避免过度提前过滤导致好候选被丢弃）
        List<Map<String, Object>> finalResults = reranked.stream()
                .filter(r -> getScore(r) >= minScore)
                .collect(Collectors.toList());
        // 容错重试：若为空则放宽条件（minScore=0，候选翻倍），保障至少返回一些候选
        if (finalResults.isEmpty()) {
            log.warn("初次检索结果为空，执行容错重试（minScore=0, candidateLimitx2）");
            List<Map<String, Object>> fallbackCandidates = knnSearch(queryVector, candidateLimit * 2, indexName);
            finalResults = rerankWithDocAggregation(fallbackCandidates, topK, docAgg, neighborWindow, perDocMaxChunks, maxContexts);
        }

        return finalResults;
    }

    /**
     * 第一阶段：KNN 友好检索
     * - ORDER BY 距离升序，LIMIT 控制候选规模
     * - 仅当 indexName 非空时追加索引过滤，避免空串条件导致误过滤
     */
    private List<Map<String, Object>> knnSearch(float[] queryVector, int limit, String indexName) {
        String vectorString = toPgVector(queryVector);
        StringBuilder sqlBuilder = new StringBuilder()
                .append("SELECT c.id, c.document_id, c.content, c.metadata, c.chunk_index, ")
                .append("c.start_position, c.end_position, ")
                .append("d.title, d.source, d.document_type AS type, ")
                .append("1 - (v.embedding <=> ?::vector) AS similarity_score ")
                .append("FROM t_vector v ")
                .append("JOIN document_chunk c ON c.vector_id = v.id ")
                .append("JOIN document d ON c.document_id = d.id ")
                .append("WHERE 1=1 ");
        boolean filterIndex = indexName != null && !indexName.isEmpty();
        if (filterIndex) {
            sqlBuilder.append("AND v.index_name = ? ");
        }
        // 关键：按距离升序排序，确保 KNN 索引生效
        sqlBuilder.append("ORDER BY v.embedding <=> ?::vector ASC LIMIT ?");
        String sql = sqlBuilder.toString();

        return jdbcTemplate.query(
                sql,
                ps -> {
                    int idx = 1;
                    ps.setString(idx++, vectorString);        // 用于 SELECT 中计算 similarity_score
                    if (filterIndex) ps.setString(idx++, indexName);
                    ps.setString(idx++, vectorString);        // 用于 ORDER BY 距离
                    ps.setInt(idx, limit);
                },
                (rs, rowNum) -> {
                    Map<String, Object> r = new HashMap<>();
                    r.put("id", rs.getLong("id"));
                    r.put("documentId", rs.getLong("document_id"));
                    r.put("content", rs.getString("content"));
                    r.put("metadata", rs.getObject("metadata"));
                    r.put("chunkIndex", rs.getInt("chunk_index"));
                    r.put("startPosition", rs.getInt("start_position"));
                    r.put("endPosition", rs.getInt("end_position"));
                    r.put("title", rs.getString("title"));
                    r.put("source", rs.getString("source"));
                    r.put("type", rs.getString("type"));
                    r.put("score", rs.getDouble("similarity_score"));
                    return r;
                }
        );
    }

    /**
     * 第二阶段：文档级聚合重排 + 邻域窗口拼接
     * 流程：
     * 1) 按 documentId 分组并计算文档分数（默认 MEAN_TOP2）
     * 2) 取前 N 个文档（N≈max(3, topK)）
     * 3) 对每个文档，选取最佳块并按 neighborWindow 扩展邻居，受 perDocMaxChunks 与 maxContexts 限制
     * 4) 全局按分数降序，截断到 maxContexts；不足再用候选补齐到 topK
     */
    private List<Map<String, Object>> rerankWithDocAggregation(List<Map<String, Object>> candidates,
                                                               int topK,
                                                               String docAgg,
                                                               int neighborWindow,
                                                               int perDocMaxChunks,
                                                               int maxContexts) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }
        // 文档分组
        Map<Long, List<Map<String, Object>>> byDoc = candidates.stream()
                .collect(Collectors.groupingBy(r -> ((Number) r.get("documentId")).longValue()));
        // 文档分数计算并排序
        List<Map.Entry<Long, Double>> docScores = byDoc.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), aggregateDocScore(e.getValue(), docAgg)))
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .toList();
        int docsToPick = Math.max(1, Math.min(docScores.size(), Math.max(3, topK)));
        List<Long> pickedDocs = docScores.subList(0, docsToPick).stream().map(Map.Entry::getKey).toList();
        List<Map<String, Object>> finalList = new ArrayList<>();
        Set<Long> seenChunkIds = new HashSet<>();
        for (Long docId : pickedDocs) {
            List<Map<String, Object>> docChunks = byDoc.get(docId);
            if (docChunks == null || docChunks.isEmpty()) {
                continue;
            }
            // 文档内按块分数降序
            List<Map<String, Object>> sorted = docChunks.stream()
                    .sorted((x, y) -> Double.compare(getScore(y), getScore(x)))
                    .toList();
            int added = 0;
            for (int anchorIdx = 0; anchorIdx < sorted.size() && added < perDocMaxChunks; anchorIdx++) {
                Map<String, Object> anchor = sorted.get(anchorIdx);
                int center = ((Number) anchor.get("chunkIndex")).intValue();
                // 1) 先加入中心块
                if (seenChunkIds.add(((Number) anchor.get("id")).longValue())) {
                    finalList.add(anchor);
                    added++;
                }
                if (added >= perDocMaxChunks) break;
                // 2) 再尝试左右邻居（仅从候选集合内选择，避免额外IO）
                if (neighborWindow > 0) {
                    for (int offset = 1; offset <= neighborWindow && added < perDocMaxChunks; offset++) {
                        int left = center - offset;
                        int right = center + offset;
                        Optional<Map<String, Object>> leftChunk = docChunks.stream()
                                .filter(r -> ((Number) r.get("chunkIndex")).intValue() == left)
                                .findFirst();
                        if (leftChunk.isPresent() && seenChunkIds.add(((Number) leftChunk.get().get("id")).longValue())) {
                            finalList.add(leftChunk.get());
                            added++;
                        }
                        if (added >= perDocMaxChunks) {
                            break;
                        }
                        Optional<Map<String, Object>> rightChunk = docChunks.stream()
                                .filter(r -> ((Number) r.get("chunkIndex")).intValue() == right)
                                .findFirst();
                        if (rightChunk.isPresent() && seenChunkIds.add(((Number) rightChunk.get().get("id")).longValue())) {
                            finalList.add(rightChunk.get());
                            added++;
                        }
                    }
                }
            }
            if (finalList.size() >= maxContexts) {
                break;
            }
        }
        // 全局排序并截断
        finalList.sort((x, y) -> Double.compare(getScore(y), getScore(x)));
        if (finalList.size() > maxContexts) {
            finalList = finalList.subList(0, maxContexts);
        }
        // 不足时用高分候选补齐到 topK（已基本具备文档多样性）
        if (finalList.size() < topK) {
            for (Map<String, Object> r : candidates) {
                if (finalList.size() >= topK) break;
                long id = ((Number) r.get("id")).longValue();
                boolean exists = finalList.stream().anyMatch(x -> ((Number) x.get("id")).longValue() == id);
                if (!exists) finalList.add(r);
            }
        }
        return finalList;
    }

    /**
     * 文档分数聚合：
     * - MAX：取该文档所有候选块中的最高分
     * - MEAN_TOP2（默认）：取最高的前两块的平均分，兼顾鲁棒与覆盖
     */
    private double aggregateDocScore(List<Map<String, Object>> chunks, String docAgg) {
        if (chunks == null || chunks.isEmpty()) return 0.0;
        List<Double> scores = chunks.stream().map(this::getScore).sorted(Comparator.reverseOrder()).toList();
        switch (docAgg.toUpperCase(Locale.ROOT)) {
            case "MAX":
                return scores.get(0);
            case "MEAN_TOP2":
            default:
                if (scores.size() == 1) return scores.get(0);
                return (scores.get(0) + scores.get(1)) / 2.0;
        }
    }

    /**
     * 从候选 Map 中读取分数，兼容 Number/String
     */
    private double getScore(Map<String, Object> r) {
        Object s = r.get("score");
        if (s instanceof Number) return ((Number) s).doubleValue();
        try {
            return s != null ? Double.parseDouble(String.valueOf(s)) : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static String toPgVector(float[] vector) {
        return "[" +
                IntStream.range(0, vector.length)
                        .mapToObj(i -> String.valueOf(vector[i]))
                        .collect(Collectors.joining(","))
                + "]";
    }

    private int orDefault(Integer v, int def) {
        return v != null ? v : def;
    }

    private double orDefault(Double v, double def) {
        return v != null ? v : def;
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }


}
