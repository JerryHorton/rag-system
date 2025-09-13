package cn.cug.sxy.ai.domain.rag.repository;


import cn.cug.sxy.ai.domain.rag.model.entity.Document;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @version 1.0
 * @Date 2025/9/8 16:38
 * @Description 文档领域存储库接口
 * @Author jerryhotton
 */

public interface IDocumentRepository {

    /**
     * 保存文档
     *
     * @param document 文档对象
     * @return 保存后的文档（包含ID）
     */
    Document save(Document document);

    /**
     * 批量保存文档
     *
     * @param documents 文档列表
     * @return 保存的文档数量
     */
    int saveAll(List<Document> documents);

    /**
     * 根据ID查找文档
     *
     * @param id 文档ID
     * @return 包装的文档对象，如不存在则为空
     */
    Optional<Document> findById(Long id);

    /**
     * 根据状态查找文档
     *
     * @param status 文档状态
     * @return 文档列表
     */
    List<Document> findByStatus(String status);

    /**
     * 根据条件查找文档
     *
     * @param conditions 查询条件
     * @return 文档列表
     */
    List<Document> findByConditions(Map<String, Object> conditions);

    /**
     * 查找所有文档
     *
     * @return 文档列表
     */
    List<Document> findAll();

    /**
     * 分页查询文档
     *
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @param status 可选的状态过滤
     * @return 包含分页信息和文档列表的Map
     */
    Map<String, Object> findWithPagination(int page, int size, String status);

    /**
     * 删除文档
     *
     * @param id 文档ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);

    /**
     * 更新文档状态
     *
     * @param id 文档ID
     * @param status 新状态
     * @param errorMessage 错误信息（可选）
     * @return 是否更新成功
     */
    boolean updateStatus(Long id, String status, String errorMessage);

    /**
     * 更新文档向量化状态
     *
     * @param id 文档ID
     * @param vectorized 是否已向量化
     * @return 是否更新成功
     */
    boolean updateVectorized(Long id, boolean vectorized);

    /**
     * 查找未向量化的文档
     *
     * @param limit 数量限制
     * @return 未向量化的文档列表
     */
    List<Document> findNonVectorizedDocuments(int limit);

    /**
     * 根据关键词搜索文档
     *
     * @param keyword 搜索关键词
     * @return 匹配的文档列表
     */
    List<Document> searchByKeyword(String keyword);

}
