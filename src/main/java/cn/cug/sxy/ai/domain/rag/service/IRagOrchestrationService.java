package cn.cug.sxy.ai.domain.rag.service;

import cn.cug.sxy.ai.domain.rag.model.entity.Response;
import cn.cug.sxy.ai.domain.rag.model.valobj.QueryParams;

import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/12 09:51
 * @Description RAG业务流程编排服务接口
 * @Author jerryhotton
 */

public interface IRagOrchestrationService {

    /**
     * 处理查询请求
     *
     * @param queryText 原始查询文本
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return 响应实体
     */
    Response processQuery(String queryText, String userId, String sessionId);

    /**
     * 处理查询请求（支持高级参数）
     *
     * @param queryText 原始查询文本
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param params 高级参数，如topK、similarityThreshold等
     * @return 响应实体
     */
    Response processQuery(String queryText, String userId, String sessionId, QueryParams params);

}
