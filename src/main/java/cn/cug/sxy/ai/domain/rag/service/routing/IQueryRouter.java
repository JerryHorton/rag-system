package cn.cug.sxy.ai.domain.rag.service.routing;

import cn.cug.sxy.ai.domain.rag.model.entity.Query;
import cn.cug.sxy.ai.domain.rag.service.query.IQueryProcessor;

import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/12 09:58
 * @Description 查询路由器接口
 * @Author jerryhotton
 */

public interface IQueryRouter {

    /**
     * 路由查询到适合的查询处理器
     *
     * @param query 查询对象
     * @return 选定的查询处理器
     */
    IQueryProcessor route(Query query);

    /**
     * 路由查询到适合的查询处理器，带额外参数
     *
     * @param query 查询对象
     * @param params 路由参数
     * @return 选定的查询处理器
     */
    IQueryProcessor route(Query query, Map<String, Object> params);

    /**
     * 获取路由类型
     *
     * @return 路由类型
     */
    RouterType getType();

}
