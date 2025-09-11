package cn.cug.sxy.ai.domain.document.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @version 1.0
 * @Date 2025/9/9 08:53
 * @Description
 * @Author jerryhotton
 */

@Getter
@AllArgsConstructor
public enum VectorCategory {

    TEXT("TEXT", "文本向量"),
    CHUNK("CHUNK", "文档片段向量"),
    ;

    private final String code;
    private final String info;

    public static VectorCategory fromCode(String code) {
        for (VectorCategory vectorCategory : VectorCategory.values()) {
            if (vectorCategory.getCode().equals(code)) {
                return vectorCategory;
            }
        }
        throw new IllegalArgumentException("No VectorCategory found with code: " + code);
    }

}
