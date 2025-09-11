package cn.cug.sxy.ai.infrastructure.embedding;

import java.util.List;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/8 11:46
 * @Description 嵌入服务接口
 * @Author jerryhotton
 */

public interface IEmbeddingService {

    /**
     * 为单个文本生成向量嵌入
     *
     * @param text 输入文本
     * @return 向量嵌入数组
     */
    float[] generateEmbedding(String text);

    /**
     * 为多个文本批量生成向量嵌入
     *
     * @param texts 输入文本列表
     * @return 向量嵌入数组列表
     */
    List<float[]> generateEmbeddings(List<String> texts);

    /**
     * 计算两个向量之间的余弦相似度
     *
     * @param vec1 向量1
     * @param vec2 向量2
     * @return 余弦相似度值，范围[-1,1]
     */
    double calculateCosineSimilarity(float[] vec1, float[] vec2);

    /**
     * 计算两个向量之间的欧几里得距离
     *
     * @param vec1 向量1
     * @param vec2 向量2
     * @return 欧几里得距离值
     */
    double calculateEuclideanDistance(float[] vec1, float[] vec2);

    /**
     * 获取嵌入模型信息
     *
     * @return 模型信息Map
     */
    Map<String, Object> getModelInfo();

    /**
     * 获取嵌入向量的维度
     *
     * @return 向量维度
     */
    int getDimensions();

}
