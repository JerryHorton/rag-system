package cn.cug.sxy.ai.infrastructure.repository;

import cn.cug.sxy.ai.domain.rag.model.entity.Query;
import cn.cug.sxy.ai.domain.rag.repository.IQueryRepository;
import cn.cug.sxy.ai.infrastructure.dao.postgres.IQueryDao;
import cn.cug.sxy.ai.infrastructure.dao.converter.QueryConverter;
import cn.cug.sxy.ai.infrastructure.dao.po.QueryPO;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @version 1.0
 * @Date 2025/9/11 10:52
 * @Description 查询仓储实现类
 * @Author jerryhotton
 */

@Repository
public class QueryRepository implements IQueryRepository {

    private final IQueryDao queryDao;
    private final QueryConverter queryConverter;

    public QueryRepository(
            IQueryDao queryDao,
            QueryConverter queryConverter) {
        this.queryDao = queryDao;
        this.queryConverter = queryConverter;
    }

    @Override
    public Query save(Query query) {
        QueryPO po = queryConverter.toPO(query);
        if (po.getId() == null) {
            queryDao.insert(po);
            // 将生成的ID回填到实体对象
            query.setId(po.getId());
        } else {
            queryDao.updateById(po);
        }

        return query;
    }

    @Override
    public int saveAll(List<Query> queries) {
        List<QueryPO> pos = queryConverter.toPOList(queries);
        int count = 0;
        for (QueryPO po : pos) {
            count += queryDao.insert(po);
        }
        // 回填ID到实体对象
        for (int i = 0; i < queries.size(); i++) {
            queries.get(i).setId(pos.get(i).getId());
        }

        return count;
    }

    @Override
    public Optional<Query> findById(Long id) {
        QueryPO po = queryDao.selectById(id);
        return Optional.ofNullable(queryConverter.toEntity(po));
    }

    @Override
    public List<Query> findByUserId(String userId, int limit) {
        List<QueryPO> pos = queryDao.selectByUserId(userId, limit);
        return queryConverter.toEntityList(pos);
    }

    @Override
    public List<Query> findBySessionId(String sessionId) {
        List<QueryPO> pos = queryDao.selectBySessionId(sessionId);
        return queryConverter.toEntityList(pos);
    }

    @Override
    public List<Query> findByConditions(Map<String, Object> conditions) {
        List<QueryPO> pos = queryDao.selectByCondition(conditions);
        return queryConverter.toEntityList(pos);
    }

    @Override
    public boolean deleteById(Long id) {
        int result = queryDao.deleteById(id);
        return result > 0;
    }

    @Override
    public boolean updateStatus(Long id, String status, LocalDateTime completeTime, Long latencyMs, String errorMessage) {
        int result = queryDao.updateStatus(id, status, completeTime, latencyMs, errorMessage);
        return result > 0;
    }

    @Override
    public List<Query> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        List<QueryPO> pos = queryDao.selectByTimeRange(startTime, endTime);
        return queryConverter.toEntityList(pos);
    }

    @Override
    public List<Query> findByQueryType(String queryType) {
        List<QueryPO> pos = queryDao.selectByQueryType(queryType);
        return queryConverter.toEntityList(pos);
    }

    @Override
    public Map<String, Long> countByStatus(LocalDateTime startTime, LocalDateTime endTime) {
        List<Map<String, Object>> results = queryDao.countByStatus(startTime, endTime);
        Map<String, Long> countMap = new HashMap<>();

        for (Map<String, Object> result : results) {
            String status = (String) result.get("status");
            Long count = ((Number) result.get("count")).longValue();
            countMap.put(status, count);
        }

        return countMap;
    }

}
