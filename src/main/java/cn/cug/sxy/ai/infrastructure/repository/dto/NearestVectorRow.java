package cn.cug.sxy.ai.infrastructure.repository.dto;

import lombok.Data;

/**
 * @version 1.0
 * @Date 2025/9/9 16:23
 * @Description 最近向量行
 * @Author jerryhotton
 */

@Data
public class NearestVectorRow {

    private Long id;
    private String externalId;
    private String indexName;
    private Double score;

}
