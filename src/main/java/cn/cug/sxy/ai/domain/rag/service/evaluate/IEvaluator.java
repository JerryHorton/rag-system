package cn.cug.sxy.ai.domain.rag.service.evaluate;

import cn.cug.sxy.ai.domain.rag.model.entity.Query;
import cn.cug.sxy.ai.domain.rag.model.entity.Response;

import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/11 10:42
 * @Description 评估器接口
 * @Author jerryhotton
 */

public interface IEvaluator {

    /**
     * 评估单个回答的质量
     *
     * @param query    查询实体
     * @param response 响应实体
     * @return 包含各项评估指标的Map
     */
    Map<String, Object> evaluate(Query query, Response response);

}
