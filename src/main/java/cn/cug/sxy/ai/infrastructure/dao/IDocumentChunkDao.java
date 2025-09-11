package cn.cug.sxy.ai.infrastructure.dao;

import cn.cug.sxy.ai.infrastructure.dao.po.DocumentChunkPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/8 17:13
 * @Description 文档块数据访问接口
 * @Author jerryhotton
 */

@Mapper
public interface IDocumentChunkDao {

    /**
     * 插入新文档块
     *
     * @param chunkPO 文档块持久化对象
     * @return 受影响的行数
     */
    int insert(DocumentChunkPO chunkPO);

    /**
     * 批量插入多个文档块
     *
     * @param chunkPOs 文档块持久化对象列表
     * @return 受影响的行数
     */
    int batchInsert(List<DocumentChunkPO> chunkPOs);

    /**
     * 根据ID更新文档块
     *
     * @param chunkPO 文档块持久化对象（包含ID）
     * @return 受影响的行数
     */
    int updateById(DocumentChunkPO chunkPO);

    /**
     * 根据ID删除文档块
     *
     * @param id 文档块ID
     * @return 受影响的行数
     */
    int deleteById(Long id);

    /**
     * 根据文档ID删除所有相关文档块
     *
     * @param documentId 文档ID
     * @return 受影响的行数
     */
    int deleteByDocumentId(Long documentId);

    /**
     * 根据ID查询文档块
     *
     * @param id 文档块ID
     * @return 文档块持久化对象，如果不存在则返回null
     */
    DocumentChunkPO selectById(Long id);

    /**
     * 根据文档ID查询所有文档块
     *
     * @param documentId 文档ID
     * @return 文档块持久化对象列表
     */
    List<DocumentChunkPO> selectByDocumentId(Long documentId);

    /**
     * 根据条件查询文档块列表
     *
     * @param params 查询条件参数
     * @return 文档块持久化对象列表
     */
    List<DocumentChunkPO> selectByCondition(Map<String, Object> params);

    /**
     * 查询所有文档块
     *
     * @return 文档块持久化对象列表
     */
    List<DocumentChunkPO> selectAll();

    /**
     * 更新文档块向量化状态
     *
     * @param id         文档块ID
     * @param vectorized 是否已向量化
     * @param vectorId   向量ID
     * @return 受影响的行数
     */
    int updateVectorized(@Param("id") Long id, @Param("vectorized") Boolean vectorized,
                         @Param("vectorId") Long vectorId);

    /**
     * 查询未向量化的文档块
     *
     * @param limit 限制返回数量
     * @return 文档块持久化对象列表
     */
    List<DocumentChunkPO> selectNonVectorizedChunks(@Param("limit") int limit);

    /**
     * 根据向量ID查询文档块
     *
     * @param vectorId 向量ID
     * @return 文档块持久化对象
     */
    DocumentChunkPO selectByVectorId(String vectorId);

    /**
     * 根据关键词搜索文档块
     *
     * @param keyword 搜索关键词
     * @return 文档块持久化对象列表
     */
    List<DocumentChunkPO> searchByKeyword(String keyword);

    /**
     * 统计某文档未向量化片段数量
     *
     * @param documentId 文档ID
     * @return 未向量化片段数量
     */
    int countNonVectorizedByDocumentId(Long documentId);

}
