package cn.cug.sxy.ai.domain.document.repository;

import cn.cug.sxy.ai.domain.document.model.entity.Response;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @version 1.0
 * @Date 2025/9/10 17:18
 * @Description 响应仓储接口
 * @Author jerryhotton
 */

public interface IResponseRepository {

    /**
     * 保存响应
     *
     * @param response 响应对象
     * @return 保存后的响应（包含ID）
     */
    Response save(Response response);

    /**
     * 根据ID查找响应
     *
     * @param id 响应ID
     * @return 包装的响应对象，如不存在则为空
     */
    Optional<Response> findById(Long id);

    /**
     * 根据查询ID查找响应列表
     *
     * @param queryId 查询ID
     * @return 响应列表
     */
    List<Response> findByQueryId(Long queryId);

    /**
     * 根据会话ID查找响应列表
     *
     * @param sessionId 会话ID
     * @param limit     限制数量
     * @return 响应列表
     */
    List<Response> findBySessionId(String sessionId, int limit);

    /**
     * 删除响应
     *
     * @param id 响应ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);

    /**
     * 统计指定查询ID的响应数量
     *
     * @param queryId 查询ID
     * @return 响应数量
     */
    int countByQueryId(Long queryId);

    /**
     * 查询指定文档的源内容片段
     *
     * @param documentId 文档ID
     * @return 文档内容片段列表
     */
    List<Map<String, Object>> findSourceChunksByDocumentId(Long documentId);

}
