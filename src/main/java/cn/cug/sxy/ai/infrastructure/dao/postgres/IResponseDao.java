package cn.cug.sxy.ai.infrastructure.dao.postgres;

import cn.cug.sxy.ai.infrastructure.dao.po.ResponsePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/11 11:30
 * @Description 响应数据访问接口
 * @Author jerryhotton
 */

@Mapper
public interface IResponseDao {

    /**
     * 插入新响应
     *
     * @param response 响应对象
     * @return 影响行数
     */
    int insert(ResponsePO response);

    /**
     * 通过ID查询响应
     *
     * @param id 响应ID
     * @return 响应对象
     */
    ResponsePO selectById(Long id);

    /**
     * 查询指定查询ID的所有响应
     *
     * @param queryId 查询ID
     * @return 响应列表
     */
    List<ResponsePO> selectByQueryId(Long queryId);

    /**
     * 通过会话ID查询响应
     *
     * @param sessionId 会话ID
     * @param limit 限制数量
     * @return 响应列表
     */
    List<ResponsePO> selectBySessionId(@Param("sessionId") String sessionId, @Param("limit") int limit);

    /**
     * 更新响应
     *
     * @param response 响应对象
     * @return 影响行数
     */
    int updateById(ResponsePO response);

    /**
     * 删除响应
     *
     * @param id 响应ID
     * @return 影响行数
     */
    int deleteById(Long id);

    /**
     * 查询指定查询ID的响应数量
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
    List<Map<String, Object>> selectSourceChunksByDocumentId(Long documentId);

}
