package cn.cug.sxy.ai.domain.document.repository;

import cn.cug.sxy.ai.domain.document.model.entity.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @version 1.0
 * @Date 2025/9/10 17:15
 * @Description 查询仓储接口
 * @Author jerryhotton
 */

public interface IQueryRepository {

    /**
     * 保存查询
     *
     * @param query 查询对象
     * @return 保存后的查询（包含ID）
     */
    Query save(Query query);

    /**
     * 批量保存查询
     *
     * @param queries 查询列表
     * @return 保存的查询数量
     */
    int saveAll(List<Query> queries);

    /**
     * 根据ID查找查询
     *
     * @param id 查询ID
     * @return 包装的查询对象，如不存在则为空
     */
    Optional<Query> findById(Long id);

    /**
     * 根据用户ID查找查询
     *
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 查询列表
     */
    List<Query> findByUserId(String userId, int limit);

    /**
     * 根据会话ID查找查询
     *
     * @param sessionId 会话ID
     * @return 查询列表
     */
    List<Query> findBySessionId(String sessionId);

    /**
     * 根据条件查找查询
     *
     * @param conditions 查询条件
     * @return 查询列表
     */
    List<Query> findByConditions(Map<String, Object> conditions);

    /**
     * 删除查询
     *
     * @param id 查询ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);

    /**
     * 更新查询状态
     *
     * @param id 查询ID
     * @param status 新状态
     * @param completeTime 完成时间
     * @param latencyMs 处理延迟（毫秒）
     * @param errorMessage 错误信息（可选）
     * @return 是否更新成功
     */
    boolean updateStatus(Long id, String status, LocalDateTime completeTime, Long latencyMs, String errorMessage);

    /**
     * 根据时间范围查找查询
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 查询列表
     */
    List<Query> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据查询类型查找查询
     *
     * @param queryType 查询类型
     * @return 查询列表
     */
    List<Query> findByQueryType(String queryType);

    /**
     * 统计各种状态的查询数量
     *
     * @return 状态及对应的查询数量
     */
    Map<String, Long> countByStatus();

}
