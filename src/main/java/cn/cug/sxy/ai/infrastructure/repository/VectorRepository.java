package cn.cug.sxy.ai.infrastructure.repository;

import cn.cug.sxy.ai.domain.rag.model.entity.Vector;
import cn.cug.sxy.ai.domain.rag.repository.IVectorRepository;
import cn.cug.sxy.ai.infrastructure.dao.IVectorDao;
import cn.cug.sxy.ai.infrastructure.dao.converter.VectorConverter;
import cn.cug.sxy.ai.infrastructure.dao.po.VectorPO;
import cn.cug.sxy.ai.infrastructure.repository.dto.NearestVectorRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @Date 2025/9/8 17:51
 * @Description 向量仓储实现类
 * @Author jerryhotton
 */

@Slf4j
@Repository
public class VectorRepository implements IVectorRepository {

    private final IVectorDao vectorDao;
    private final VectorConverter vectorConverter;

    public VectorRepository(
            IVectorDao vectorDao,
            VectorConverter vectorConverter) {
        this.vectorDao = vectorDao;
        this.vectorConverter = vectorConverter;
    }

    @Override
    public void save(Vector vector) {
        VectorPO po = vectorConverter.toPO(vector);
        if (po.getId() == null) {
            vectorDao.insert(po);
            // 将生成的ID回填到实体对象
            vector.setId(po.getId());
        } else {
            vectorDao.updateById(po);
        }
    }

    @Override
    public int saveAll(List<Vector> vectors) {
        List<VectorPO> pos = vectorConverter.toPOList(vectors);
        return vectorDao.batchInsert(pos);
    }

    @Override
    public Optional<Vector> findById(Long id) {
        VectorPO po = vectorDao.selectById(id);
        return Optional.ofNullable(vectorConverter.toEntity(po));
    }

    @Override
    public Optional<Vector> findByExternalId(String externalId) {
        VectorPO po = vectorDao.selectByExternalId(externalId);
        return Optional.ofNullable(vectorConverter.toEntity(po));
    }

    @Override
    public List<Vector> findByVectorType(String vectorType) {
        List<VectorPO> pos = vectorDao.selectByVectorType(vectorType);
        return vectorConverter.toEntityList(pos);
    }

    @Override
    public List<Vector> findByIndexName(String indexName) {
        List<VectorPO> pos = vectorDao.selectByIndexName(indexName);
        return vectorConverter.toEntityList(pos);
    }

    @Override
    public List<Vector> findByConditions(Map<String, Object> conditions) {
        List<VectorPO> pos = vectorDao.selectByCondition(conditions);
        return vectorConverter.toEntityList(pos);
    }

    @Override
    public boolean deleteById(Long id) {
        int result = vectorDao.deleteById(id);
        return result > 0;
    }

    @Override
    public boolean deleteByExternalId(String externalId) {
        int result = vectorDao.deleteByExternalId(externalId);
        return result > 0;
    }

    @Override
    public List<Map<String, Object>> findNearest(float[] vector, int topK, float minScore, Map<String, Object> filter) {
        if (vector == null || vector.length == 0) return new ArrayList<>();
        String queryLiteral = toVectorLiteral(vector);
        Integer dimensions = vector.length;
        String indexName = filter != null ? (String) filter.get("indexName") : null;
        List<NearestVectorRow> rows = vectorDao.selectNearestByEmbedding(queryLiteral, Math.max(topK, 0), minScore, dimensions, indexName);
        if (rows == null) return new ArrayList<>();
        return rows.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("externalId", r.getExternalId());
            m.put("indexName", r.getIndexName());
            m.put("score", r.getScore());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public float calculateSimilarity(float[] vector1, float[] vector2, String metric) {
        if (vector1 == null || vector2 == null) return 0.0f;
        if (metric == null || metric.equalsIgnoreCase("COSINE")) return cosine(vector1, vector2);
        if (metric.equalsIgnoreCase("DOT")) return dot(vector1, vector2);
        return cosine(vector1, vector2);
    }

    private void tryUpdateEmbedding(Vector vector) {
        try {
            if (vector.getId() == null) return;
            float[] embeddingArray = vector.getEmbedding();
            if (embeddingArray == null || embeddingArray.length == 0) return;
            String literal = toVectorLiteral(embeddingArray);
            vectorDao.updateEmbedding(vector.getId(), literal);
        } catch (Exception e) {
            log.warn("update embedding failed id={} err={}", vector.getId(), e.getMessage());
        }
    }

    private String toVectorLiteral(float[] f) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < f.length; i++) {
            if (i > 0) sb.append(',');
            // 使用十进制字符串，避免科学计数法
            sb.append(f[i]);
        }
        sb.append("]::vector");
        return sb.toString();
    }

    private float dot(float[] a, float[] b) {
        int n = Math.min(a.length, b.length);
        double s = 0.0;
        for (int i = 0; i < n; i++) s += a[i] * b[i];
        return (float) s;
    }

    private float cosine(float[] a, float[] b) {
        int n = Math.min(a.length, b.length);
        double dot = 0.0, na = 0.0, nb = 0.0;
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        double denom = Math.sqrt(na) * Math.sqrt(nb);
        if (denom == 0.0) return 0.0f;
        return (float) (dot / denom);
    }

}
