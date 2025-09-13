package cn.cug.sxy.ai.domain.rag.service.generation;

import cn.cug.sxy.ai.domain.rag.model.valobj.GenerateParams;

import java.util.List;

/**
 * @version 1.0
 * @Date 2025/9/11 09:31
 * @Description 生成器接口
 * @Author jerryhotton
 */

public interface IGenerator {

    /**
     * 生成回答
     *
     * @param query    查询文本
     * @param contexts 相关上下文列表
     * @param params   生成参数
     * @return 生成的回答文本
     */
    String generate(String query, List<String> contexts, GenerateParams params);

}
