package cn.cug.sxy.ai.infrastructure.dao;

import cn.cug.sxy.ai.infrastructure.dao.po.DocumentPO;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/8 16:40
 * @Description 文档数据访问接口
 * @Author jerryhotton
 */

@Mapper
public interface IDocumentDao {

    /**
     * 插入新文档
     *
     * @param documentPO 文档持久化对象
     * @return 受影响的行数
     */
    int insert(DocumentPO documentPO);

    /**
     * 批量插入多个文档
     *
     * @param documentPOs 文档持久化对象列表
     * @return 受影响的行数
     */
    int batchInsert(List<DocumentPO> documentPOs);

    /**
     * 根据ID更新文档
     *
     * @param documentPO 文档持久化对象（包含ID）
     * @return 受影响的行数
     */
    int updateById(DocumentPO documentPO);

    /**
     * 根据ID删除文档
     *
     * @param id 文档ID
     * @return 受影响的行数
     */
    int deleteById(Long id);

    /**
     * 根据ID查询文档
     *
     * @param id 文档ID
     * @return 文档持久化对象，如果不存在则返回null
     */
    DocumentPO selectById(Long id);

    /**
     * 根据条件查询文档列表
     *
     * @param params 查询条件参数
     * @return 文档持久化对象列表
     */
    List<DocumentPO> selectByCondition(Map<String, Object> params);

    /**
     * 根据状态查询文档列表
     *
     * @param status 文档状态
     * @return 文档持久化对象列表
     */
    List<DocumentPO> selectByStatus(String status);

    /**
     * 根据批次ID查询文档列表
     *
     * @param batchId 批次ID
     * @return 文档持久化对象列表
     */
    List<DocumentPO> selectByBatchId(String batchId);

    /**
     * 根据父文档ID查询文档列表
     *
     * @param parentId 父文档ID
     * @return 文档持久化对象列表
     */
    List<DocumentPO> selectByParentId(Long parentId);

    /**
     * 查询所有文档
     *
     * @return 文档持久化对象列表
     */
    List<DocumentPO> selectAll();

    /**
     * 更新文档状态
     *
     * @param id 文档ID
     * @param status 新状态
     * @param errorMessage 错误信息（可选）
     * @return 受影响的行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status,
                     @Param("errorMessage") String errorMessage);

    /**
     * 更新文档向量化状态
     *
     * @param id 文档ID
     * @param vectorized 是否已向量化
     * @return 受影响的行数
     */
    int updateVectorized(@Param("id") Long id, @Param("vectorized") Boolean vectorized);

    /**
     * 根据文档类型查询文档列表
     *
     * @param documentType 文档类型
     * @return 文档持久化对象列表
     */
    List<DocumentPO> selectByDocumentType(String documentType);

    /**
     * 查询未向量化的文档列表
     *
     * @param limit 限制返回数量
     * @return 文档持久化对象列表
     */
    List<DocumentPO> selectNonVectorizedDocuments(@Param("limit") int limit);

    /**
     * 统计各种状态的文档数量
     *
     * @return 状态及对应的文档数量
     */
    @MapKey("status")
    List<Map<String, Object>> countByStatus();

    /**
     * 全文搜索文档
     *
     * @param keyword 搜索关键词
     * @return 文档持久化对象列表
     */
    List<DocumentPO> searchByKeyword(String keyword);

    /**
     * 根据ID列表批量查询文档
     *
     * @param ids 文档ID列表
     * @return 文档持久化对象列表
     */
    List<DocumentPO> selectByIds(@Param("ids") List<Long> ids);

}
