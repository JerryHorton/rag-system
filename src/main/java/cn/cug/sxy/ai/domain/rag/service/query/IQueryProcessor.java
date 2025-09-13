package cn.cug.sxy.ai.domain.rag.service.query;

import cn.cug.sxy.ai.domain.rag.model.entity.Query;
import cn.cug.sxy.ai.domain.rag.model.entity.Response;

/**
 * @version 1.0
 * @Date 2025/9/10 16:42
 * @Description 查询处理器接口
 * @Author jerryhotton
 */

public interface IQueryProcessor {

    /**
     * 处理查询请求
     *
     * @param query 查询对象
     * @return 响应对象
     */
    Response processQuery(Query query);

    /**
     * 获取查询处理器类型
     *
     * @return 处理器类型
     */
    QueryType getType();

}
