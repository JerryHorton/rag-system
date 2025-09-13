package cn.cug.sxy.ai.domain.rag.repository;

import cn.cug.sxy.ai.domain.rag.model.entity.DocumentChunk;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @version 1.0
 * @Date 2025/9/8 17:50
 * @Description 文档块仓储接口
 * @Author jerryhotton
 */

public interface IDocumentChunkRepository {

    /**
     * 保存文档块
     *
     * @param chunk 文档块对象
     * @return 保存后的文档块（包含ID）
     */
    DocumentChunk save(DocumentChunk chunk);

    /**
     * 批量保存文档块
     *
     * @param chunks 文档块列表
     * @return 保存的文档块数量
     */
    int saveAll(List<DocumentChunk> chunks);

    /**
     * 根据ID查找文档块
     *
     * @param id 文档块ID
     * @return 包装的文档块对象，如不存在则为空
     */
    Optional<DocumentChunk> findById(Long id);

    /**
     * 根据文档ID查找文档块
     *
     * @param documentId 文档ID
     * @return 文档块列表
     */
    List<DocumentChunk> findByDocumentId(Long documentId);

    /**
     * 根据条件查找文档块
     *
     * @param conditions 查询条件
     * @return 文档块列表
     */
    List<DocumentChunk> findByConditions(Map<String, Object> conditions);

    /**
     * 查找所有文档块
     *
     * @return 文档块列表
     */
    List<DocumentChunk> findAll();

    /**
     * 删除文档块
     *
     * @param id 文档块ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);

    /**
     * 根据文档ID删除所有相关文档块
     *
     * @param documentId 文档ID
     * @return 删除的文档块数量
     */
    int deleteByDocumentId(Long documentId);

    /**
     * 更新文档块向量化状态
     *
     * @param id 文档块ID
     * @param vectorized 是否已向量化
     * @param vectorId 向量ID
     * @return 是否更新成功
     */
    boolean updateVectorized(Long id, boolean vectorized, Long vectorId);

    /**
     * 查找未向量化的文档块
     *
     * @param limit 数量限制
     * @return 未向量化的文档块列表
     */
    List<DocumentChunk> findNonVectorizedChunks(int limit);

    /**
     * 根据向量ID查找文档块
     *
     * @param vectorId 向量ID
     * @return 包装的文档块对象，如不存在则为空
     */
    Optional<DocumentChunk> findByVectorId(String vectorId);

    /**
     * 根据关键词搜索文档块
     *
     * @param keyword 搜索关键词
     * @return 匹配的文档块列表
     */
    List<DocumentChunk> searchByKeyword(String keyword);

    /**
     * 统计某文档未向量化片段数量
     *
     * @param documentId 文档ID
     * @return 未向量化片段数量
     */
    int countNonVectorizedByDocumentId(Long documentId);

}
