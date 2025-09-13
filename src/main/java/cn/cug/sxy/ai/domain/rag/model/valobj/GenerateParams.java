package cn.cug.sxy.ai.domain.rag.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @version 1.0
 * @Date 2025/9/11 09:40
 * @Description 生成参数对象
 * @Author jerryhotton
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GenerateParams {

    /**
     * 模型名称
     */
    private String model;
    /**
     * 温度参数，控制生成文本的随机性
     */
    private Double temperature;
    /**
     * 最大令牌数，限制生成文本的长度
     */
    private Integer maxTokens;
    /**
     * nucleus 采样参数，控制生成文本的多样性
     */
    private Double topP;
    /**
     * 频率惩罚参数，控制生成文本的重复度
     */
    private Integer topK;

}
