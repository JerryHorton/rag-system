package cn.cug.sxy.ai.infrastructure.dao;

import cn.cug.sxy.ai.infrastructure.dao.po.QueryPO;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/8 17:23
 * @Description 查询数据访问接口
 * @Author jerryhotton
 */

@Mapper
public interface IQueryDao {

    /**
     * 插入新查询
     *
     * @param query 查询实体
     * @return 受影响的行数
     */
    int insert(QueryPO query);

    /**
     * 根据ID更新查询
     *
     * @param query 查询实体（包含ID）
     * @return 受影响的行数
     */
    int updateById(QueryPO query);

    /**
     * 根据ID删除查询
     *
     * @param id 查询ID
     * @return 受影响的行数
     */
    int deleteById(Long id);

    /**
     * 根据ID查询
     *
     * @param id 查询ID
     * @return 查询实体
     */
    QueryPO selectById(Long id);

    /**
     * 根据用户ID查询列表
     *
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 查询实体列表
     */
    List<QueryPO> selectByUserId(@Param("userId") String userId, @Param("limit") int limit);

    /**
     * 根据会话ID查询列表
     *
     * @param sessionId 会话ID
     * @return 查询实体列表
     */
    List<QueryPO> selectBySessionId(String sessionId);

    /**
     * 根据条件查询列表
     *
     * @param params 查询条件参数
     * @return 查询实体列表
     */
    List<QueryPO> selectByCondition(Map<String, Object> params);

    /**
     * 更新查询状态和完成时间
     *
     * @param id 查询ID
     * @param status 新状态
     * @param completeTime 完成时间
     * @param latencyMs 处理延迟（毫秒）
     * @param errorMessage 错误信息（可选）
     * @return 受影响的行数
     */
    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("completeTime") LocalDateTime completeTime,
                     @Param("latencyMs") Long latencyMs,
                     @Param("errorMessage") String errorMessage);

    /**
     * 根据时间范围查询列表
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 查询实体列表
     */
    List<QueryPO> selectByTimeRange(@Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);

    /**
     * 根据查询类型查询列表
     *
     * @param queryType 查询类型
     * @return 查询实体列表
     */
    List<QueryPO> selectByQueryType(String queryType);

    /**
     * 根据路由目标查询列表
     *
     * @param routeTarget 路由目标
     * @return 查询实体列表
     */
    List<QueryPO> selectByRouteTarget(String routeTarget);

    /**
     * 统计一定时间段内各种状态的查询数量
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 状态及对应的查询数量
     */
    @MapKey("status")
    List<Map<String, Object>> countByStatus(@Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);

    /**
     * 统计一定时间段内各种查询类型的查询数量
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 查询类型及对应的查询数量
     */
    @MapKey("query_type")
    List<Map<String, Object>> countByQueryType(@Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);

    /**
     * 查询平均响应时间
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 平均响应时间（毫秒）
     */
    Double selectAvgLatency(@Param("startTime") LocalDateTime startTime,
                            @Param("endTime") LocalDateTime endTime);

    /**
     * 关联响应IDs
     *
     * @param id 查询ID
     * @param responseIds 响应ID列表（JSON字符串）
     * @return 受影响的行数
     */
    int updateResponseIds(@Param("id") Long id, @Param("responseIds") String responseIds);

}
