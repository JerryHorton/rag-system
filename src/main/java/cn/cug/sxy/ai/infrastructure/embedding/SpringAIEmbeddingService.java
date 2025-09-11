package cn.cug.sxy.ai.infrastructure.embedding;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @version 1.0
 * @Date 2025/9/8 11:47
 * @Description Spring AI嵌入服务实现
 * @Author jerryhotton
 */

@Service
@Slf4j
public class SpringAIEmbeddingService implements IEmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final CacheManager cacheManager;

    // 向量维度缓存，避免重复计算
    private Integer cachedDimensions;

    // 自我注入，解决@Cacheable自调用问题
    @Resource
    @Lazy
    private SpringAIEmbeddingService self;

    public SpringAIEmbeddingService(
            EmbeddingModel embeddingModel,
            CacheManager cacheManager) {
        this.embeddingModel = embeddingModel;
        this.cacheManager = cacheManager;
    }

    /**
     * 为单个文本生成向量嵌入
     *
     * @param text 输入文本
     * @return 向量嵌入数组
     */
    @Override
    @Cacheable(value = "embeddings", key = "#text.hashCode()", unless = "#result == null")
    public float[] generateEmbedding(String text) {
        if (StringUtils.isBlank(text)) {
            throw new IllegalArgumentException("嵌入文本不能为空");
        }
        try {
            log.info("生成文本嵌入向量，文本长度: {}", text.length());
            return embeddingModel.embed(text);
        } catch (Exception e) {
            log.error("生成嵌入向量失败: {}", e.getMessage());
            throw new RuntimeException("生成嵌入向量失败: " + e.getMessage(), e);
        }
    }

    /**
     * 为多个文本批量生成向量嵌入
     *
     * @param texts 输入文本列表
     * @return 向量嵌入数组列表
     */
    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        log.info("批量生成嵌入向量，文本数量: {}", texts.size());
        // 结果列表
        float[][] resultArray = new float[texts.size()][];
        // 访问 Spring Cache（与单条 @Cacheable 同名的 cache）
        Cache cache = Optional.ofNullable(cacheManager.getCache("embeddings"))
                .orElseThrow(() -> new IllegalStateException("未配置名为 'embeddings' 的缓存"));
        // 未缓存的文本列表及其索引
        List<String> uncachedTexts = new ArrayList<>();
        List<Integer> uncachedIndices = new ArrayList<>();
        // 先检查每个文本是否已缓存
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            if (!StringUtils.isNotBlank(text)) {
                throw new IllegalArgumentException("第 " + i + " 个文本为空");
            }
            float[] cached = cache.get(text, float[].class);
            if (cached != null) {
                resultArray[i] = cached;
            } else {
                uncachedTexts.add(text);
                uncachedIndices.add(i);
            }
        }
        // 批量处理未缓存的文本
        if (!uncachedTexts.isEmpty()) {
            try {
                // 一次性批量生成未命中项
                List<float[]> newEmbeddings = embeddingModel.embed(uncachedTexts);
                if (newEmbeddings.size() != uncachedTexts.size()) {
                    throw new IllegalStateException("返回的嵌入数量与请求不一致: "
                            + newEmbeddings.size() + " != " + uncachedTexts.size());
                }
                // 将新生成的向量放入结果列表的正确位置并写入缓存
                for (int i = 0; i < newEmbeddings.size(); i++) {
                    int originalIndex = uncachedIndices.get(i);
                    float[] vec = newEmbeddings.get(i);
                    resultArray[originalIndex] = vec;
                    // 写入缓存
                    cache.put(texts.get(originalIndex), vec);
                }
            } catch (Exception e) {
                // 批量处理失败，降级为逐条（仍然写缓存）
                log.warn("批量生成嵌入向量失败，尝试逐个生成", e);
                for (int i = 0; i < uncachedTexts.size(); i++) {
                    String text = uncachedTexts.get(i);
                    int originalIndex = uncachedIndices.get(i);
                    float[] vec = embeddingModel.embed(text);
                    resultArray[originalIndex] = vec;
                    // 写入缓存
                    cache.put(texts.get(originalIndex), vec);
                }
            }
        }
        // 将数组转换为列表返回
        return Arrays.asList(resultArray);
    }

    /**
     * 计算两个向量之间的余弦相似度
     *
     * @param vec1 向量1
     * @param vec2 向量2
     * @return 余弦相似度值，范围[-1,1]
     */
    @Override
    public double calculateCosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null || vec1.length != vec2.length) {
            throw new IllegalArgumentException("向量不能为null且维度必须相同");
        }
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }
        // 避免除以0
        if (norm1 <= 0 || norm2 <= 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 计算两个向量之间的欧几里得距离
     *
     * @param vec1 向量1
     * @param vec2 向量2
     * @return 欧几里得距离值
     */
    @Override
    public double calculateEuclideanDistance(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null || vec1.length != vec2.length) {
            throw new IllegalArgumentException("向量不能为null且维度必须相同");
        }
        double sum = 0.0;
        for (int i = 0; i < vec1.length; i++) {
            double diff = vec1[i] - vec2[i];
            sum += diff * diff;
        }

        return Math.sqrt(sum);
    }

    /**
     * 获取嵌入模型信息
     *
     * @return 模型信息Map
     */
    @Override
    public Map<String, Object> getModelInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("provider", "Spring AI");
        info.put("dimensions", getDimensions());

        return info;
    }

    /**
     * 获取嵌入向量的维度
     *
     * @return 向量维度
     */
    @Override
    public int getDimensions() {
        // 如果已有缓存，直接返回
        if (cachedDimensions != null) {
            return cachedDimensions;
        }
        try {
            // 使用self引用调用缓存方法，确保缓存生效
            float[] sampleEmbedding = self.generateEmbedding("测试嵌入维度");
            cachedDimensions = sampleEmbedding.length;
            return cachedDimensions;
        } catch (Exception e) {
            log.error("获取嵌入维度失败", e);
            // 默认返回OpenAI Ada嵌入模型的维度
            return 1536;
        }
    }

}
