package cn.cug.sxy.ai.domain.rag.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @version 1.0
 * @Date 2025/9/9 09:07
 * @Description 向量类型
 * @Author jerryhotton
 */

@Getter
@AllArgsConstructor
public enum VectorType {

    DOCUMENT("DOCUMENT", "文档向量"),
    CHUNK("CHUNK", "文档片段向量"),
    QUERY("QUERY", "查询向量"),
    HYBRID("HYBRID", "混合向量"),
    ;

    private final String code;
    private final String info;

    public static VectorType fromCode(String code) {
        for (VectorType vectorType : values()) {
            if (vectorType.getCode().equals(code)) {
                return vectorType;
            }
        }
        throw new IllegalArgumentException("No VectorType found with code: " + code);
    }

}
