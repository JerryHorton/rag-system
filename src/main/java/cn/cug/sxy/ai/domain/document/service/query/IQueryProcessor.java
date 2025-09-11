package cn.cug.sxy.ai.domain.document.service.query;

import cn.cug.sxy.ai.domain.document.model.entity.Query;
import cn.cug.sxy.ai.domain.document.model.entity.Response;

import java.util.concurrent.CompletableFuture;

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
     * 异步处理查询请求
     *
     * @param query 查询对象
     * @return 异步响应任务
     */
    CompletableFuture<Response> processQueryAsync(Query query);

    /**
     * 获取处理器描述
     * @return 描述
     */
    String getDescription();

}
