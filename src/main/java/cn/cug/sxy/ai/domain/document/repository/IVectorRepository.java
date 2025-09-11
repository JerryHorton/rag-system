package cn.cug.sxy.ai.domain.document.repository;

import cn.cug.sxy.ai.domain.document.model.entity.Vector;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @version 1.0
 * @Date 2025/9/8 17:40
 * @Description 向量领域存储库接口
 * @Author jerryhotton
 */

public interface IVectorRepository {

    /**
     * 保存向量
     *
     * @param vector 向量对象
     */
    void save(Vector vector);

    /**
     * 批量保存向量
     *
     * @param vectors 向量列表
     * @return 保存的向量数量
     */
    int saveAll(List<Vector> vectors);

    /**
     * 根据ID查找向量
     *
     * @param id 向量ID
     * @return 包装的向量对象，如不存在则为空
     */
    Optional<Vector> findById(Long id);

    /**
     * 根据外部ID查找向量
     *
     * @param externalId 外部ID
     * @return 包装的向量对象，如不存在则为空
     */
    Optional<Vector> findByExternalId(String externalId);

    /**
     * 根据向量类型查找向量列表
     *
     * @param vectorType 向量类型
     * @return 向量列表
     */
    List<Vector> findByVectorType(String vectorType);

    /**
     * 根据索引名称查找向量列表
     *
     * @param indexName 索引名称
     * @return 向量列表
     */
    List<Vector> findByIndexName(String indexName);

    /**
     * 根据条件查找向量
     *
     * @param conditions 查询条件
     * @return 向量列表
     */
    List<Vector> findByConditions(Map<String, Object> conditions);

    /**
     * 删除向量
     *
     * @param id 向量ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);

    /**
     * 根据外部ID删除向量
     *
     * @param externalId 外部ID
     * @return 是否删除成功
     */
    boolean deleteByExternalId(String externalId);

    /**
     * 使用向量数据进行最近邻查询
     *
     * @param vector 查询向量
     * @param topK 返回的最大结果数
     * @param minScore 最小相似度分数
     * @param filter 过滤条件
     * @return 向量列表，按相似度降序排列
     */
    List<Map<String, Object>> findNearest(float[] vector, int topK, float minScore, Map<String, Object> filter);

    /**
     * 计算两个向量之间的相似度
     *
     * @param vector1 向量1
     * @param vector2 向量2
     * @param metric 度量方法（如余弦相似度、欧几里得距离）
     * @return 相似度分数
     */
    float calculateSimilarity(float[] vector1, float[] vector2, String metric);

}
