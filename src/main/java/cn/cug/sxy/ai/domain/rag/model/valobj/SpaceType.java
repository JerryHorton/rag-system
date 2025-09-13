package cn.cug.sxy.ai.domain.rag.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @version 1.0
 * @Date 2025/9/9 08:56
 * @Description 向量空间类型
 * @Author jerryhotton
 */

@Getter
@AllArgsConstructor
public enum SpaceType {

    COSINE("COSINE", "余弦空间"),
    ;

    private final String code;
    private final String info;

    /**
     * 根据代码获取枚举
     *
     * @param code 代码
     * @return 枚举
     */
    public static SpaceType fromCode(String code) {
        for (SpaceType spaceType : values()) {
            if (spaceType.getCode().equals(code)) {
                return spaceType;
            }
        }
        throw new IllegalArgumentException("No SpaceType found with code: " + code);
    }

}
