package cn.cug.sxy.ai.domain.document.service.retrieval;

import cn.cug.sxy.ai.domain.document.model.entity.DocumentChunk;
import cn.cug.sxy.ai.domain.document.model.entity.Query;
import cn.cug.sxy.ai.domain.document.model.valobj.RetrievalParams;

import java.util.List;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/10 17:30
 * @Description 检索器接口
 * @Author jerryhotton
 */

public interface IRetriever {

    /**
     * 根据查询检索相关文档片段，带额外参数
     *
     * @param query 查询对象
     * @param params 检索参数
     * @return 相关文档片段列表，按相关性降序排序
     */
    List<Map<String, Object>> retrieve(Query query, RetrievalParams params);

    /**
     * 获取检索结果的相似度分数
     *
     * @param query 查询对象
     * @param chunk 文档片段
     * @return 相似度分数（0-1之间，越大表示越相似）
     */
    double getSimilarityScore(Query query, DocumentChunk chunk);

}
