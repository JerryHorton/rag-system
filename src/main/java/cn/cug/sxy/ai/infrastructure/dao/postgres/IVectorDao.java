package cn.cug.sxy.ai.infrastructure.dao.postgres;

import cn.cug.sxy.ai.infrastructure.dao.po.VectorPO;
import cn.cug.sxy.ai.infrastructure.repository.dto.NearestVectorRow;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/8 17:39
 * @Description 向量数据访问接口
 * @Author jerryhotton
 */

@Mapper
public interface IVectorDao {

    /**
     * 插入新向量
     *
     * @param vector 向量实体
     * @return 受影响的行数
     */
    int insert(VectorPO vector);

    /**
     * 批量插入多个向量
     *
     * @param vectors 向量实体列表
     * @return 受影响的行数
     */
    int batchInsert(List<VectorPO> vectors);

    /**
     * 根据ID更新向量
     *
     * @param vector 向量实体（包含ID）
     * @return 受影响的行数
     */
    int updateById(VectorPO vector);

    /**
     * 根据ID删除向量
     *
     * @param id 向量ID
     * @return 受影响的行数
     */
    int deleteById(Long id);

    /**
     * 根据外部ID删除向量
     *
     * @param externalId 外部ID
     * @return 受影响的行数
     */
    int deleteByExternalId(String externalId);

    /**
     * 根据ID查询向量
     *
     * @param id 向量ID
     * @return 向量实体
     */
    VectorPO selectById(Long id);

    /**
     * 根据外部ID查询向量
     *
     * @param externalId 外部ID
     * @return 向量实体
     */
    VectorPO selectByExternalId(String externalId);

    /**
     * 根据向量类型查询向量列表
     *
     * @param vectorType 向量类型
     * @return 向量实体列表
     */
    List<VectorPO> selectByVectorType(String vectorType);

    /**
     * 根据索引名称查询向量列表
     *
     * @param indexName 索引名称
     * @return 向量实体列表
     */
    List<VectorPO> selectByIndexName(String indexName);

    /**
     * 根据条件查询向量列表
     *
     * @param params 查询条件参数
     * @return 向量实体列表
     */
    List<VectorPO> selectByCondition(Map<String, Object> params);

    /**
     * 统计各向量类型的数量
     *
     * @return 向量类型及对应的数量
     */
    @MapKey("vectorType")
    List<Map<String, Object>> countByVectorType();

    /**
     * 统计各索引的向量数量
     *
     * @return 索引名称及对应的向量数量
     */
    @MapKey("indexName")
    List<Map<String, Object>> countByIndexName();

    /**
     * 根据ID列表批量查询向量
     *
     * @param ids 向量ID列表
     * @return 向量实体列表
     */
    List<VectorPO> selectByIds(@Param("ids") List<Long> ids);

    /**
     * 根据外部ID列表批量查询向量
     *
     * @param externalIds 外部ID列表
     * @return 向量实体列表
     */
    List<VectorPO> selectByExternalIds(@Param("externalIds") List<String> externalIds);

    /**
     * 根据向量类别查询向量列表
     *
     * @param vectorCategory 向量类别
     * @return 向量实体列表
     */
    List<VectorPO> selectByVectorCategory(String vectorCategory);

    /**
     * 根据空间类型查询向量列表
     *
     * @param spaceType 空间类型
     * @return 向量实体列表
     */
    List<VectorPO> selectBySpaceType(String spaceType);

    /**
     * 更新向量的pgvector列
     *
     * @param id               向量ID
     * @param embeddingLiteral 向量的pgvector字符串表示
     * @return 受影响的行数
     */
    int updateEmbedding(@Param("id") Long id, @Param("embeddingLiteral") String embeddingLiteral);

    /**
     * 基于pgvector的TopK近邻查询
     *
     * @param queryLiteral  查询向量的pgvector字符串表示
     * @param topK          返回的最近邻数量
     * @param minScore      最小相似度阈值
     * @param dimensions    向量维度
     * @param indexName     索引名称
     * @return 最近邻向量及相似度
     */
    List<NearestVectorRow> selectNearestByEmbedding(@Param("queryLiteral") String queryLiteral,
                                                    @Param("topK") int topK,
                                                    @Param("minScore") double minScore,
                                                    @Param("dimensions") Integer dimensions,
                                                    @Param("indexName") String indexName);

}
