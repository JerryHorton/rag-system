package cn.cug.sxy.ai.domain.rag.service.retrieval;

import cn.cug.sxy.ai.domain.rag.model.entity.Query;
import cn.cug.sxy.ai.domain.rag.model.valobj.RetrievalParams;
import cn.cug.sxy.ai.infrastructure.embedding.IEmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        // 获取或生成查询向量
        float[] vector = query.getVector();
        if (vector == null || vector.length == 0) {
            vector = embeddingService.generateEmbedding(query.getProcessedText());
            query.setVector(vector);
        }
        // 构建PGVector查询
        String vectorString = toPgVector(vector);
        StringBuilder sqlBuilder = new StringBuilder()
                .append("SELECT c.id, c.document_id, c.content, c.metadata, c.chunk_index, ")
                .append("c.start_position, c.end_position, ")
                .append("d.title, d.source, d.document_type AS type, ")
                .append("1 - (v.embedding <=> ?::vector) AS similarity_score ")
                .append("FROM t_vector v ")
                .append("JOIN t_document_chunk c ON c.vector_id = v.id ")
                .append("JOIN t_document d ON c.document_id = d.id ")
                .append("WHERE 1=1 ");
        if (StringUtils.isNotBlank(params.getIndexName())) {
            sqlBuilder.append("AND v.index_name = ? ");
        }
        sqlBuilder.append("ORDER BY similarity_score DESC LIMIT ?");
        String sql = sqlBuilder.toString();

        List<Map<String, Object>> results = jdbcTemplate.query(
                sql,
                ps -> {
                    int idx = 1;
                    ps.setString(idx++, vectorString);
                    if (StringUtils.isNotBlank(params.getIndexName())) {
                        ps.setString(idx++, params.getIndexName());
                    }
                    ps.setInt(idx, params.getTopK());
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

        return results.stream()
                .filter(r -> (Double) r.get("score") >= params.getMinScore())
                .collect(Collectors.toList());
    }

    private static String toPgVector(float[] vector) {
        return "[" +
                IntStream.range(0, vector.length)
                        .mapToObj(i -> String.valueOf(vector[i]))
                        .collect(Collectors.joining(","))
                + "]";
    }

}
