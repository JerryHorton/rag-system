package cn.cug.sxy.ai.domain.armory.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @version 1.0
 * @Date 2025/11/25 14:53
 * @Description API配置 值对象
 * @Author jerryhotton
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiClientApiVO {

    /**
     * API ID
     */
    private String apiId;
    /**
     * 基础URL
     */
    private String baseUrl;
    /**
     * API密钥
     */
    private String apiKey;
    /**
     * 对话补全路径
     */
    private String completionsPath;
    /**
     * 嵌入向量路径
     */
    private String embeddingsPath;

}
