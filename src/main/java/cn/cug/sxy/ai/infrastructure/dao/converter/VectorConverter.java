package cn.cug.sxy.ai.infrastructure.dao.converter;

import cn.cug.sxy.ai.domain.rag.model.entity.Vector;
import cn.cug.sxy.ai.domain.rag.model.valobj.SpaceType;
import cn.cug.sxy.ai.domain.rag.model.valobj.VectorCategory;
import cn.cug.sxy.ai.domain.rag.model.valobj.VectorType;
import cn.cug.sxy.ai.infrastructure.dao.po.VectorPO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @Date 2025/9/8 17:37
 * @Description 向量实体与持久化对象转换器
 * @Author jerryhotton
 */

@Slf4j
@Component
public class VectorConverter {

    private final ObjectMapper objectMapper;

    public VectorConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将领域实体转换为持久化对象
     *
     * @param entity 向量领域实体
     * @return 向量持久化对象
     */
    public VectorPO toPO(Vector entity) {
        if (entity == null) {
            return null;
        }

        String metadataJson = null;
        if (entity.getMetadata() != null) {
            try {
                metadataJson = objectMapper.writeValueAsString(entity.getMetadata());
            } catch (JsonProcessingException e) {
                log.error("将向量元数据转换为JSON时出错", e);
                metadataJson = "{}";
            }
        }

        return VectorPO.builder()
                .id(entity.getId())
                .externalId(entity.getExternalId())
                .vectorType(entity.getVectorType().getCode())
                .dimensions(entity.getDimensions())
                .embedding(entity.getEmbedding())
                .indexName(entity.getIndexName())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .metadata(metadataJson)
                .embeddingModel(entity.getEmbeddingModel())
                .vectorNorm(entity.getVectorNorm())
                .vectorCategory(entity.getVectorCategory().getCode())
                .contentSummary(entity.getContentSummary())
                .spaceType(entity.getSpaceType().getCode())
                .isPrimary(entity.getIsPrimary())
                .build();
    }

    /**
     * 将持久化对象转换为领域实体
     *
     * @param po 向量持久化对象
     * @return 向量领域实体
     */
    public Vector toEntity(VectorPO po) {
        if (po == null) {
            return null;
        }
        Map<String, Object> metadata = new HashMap<>();
        if (po.getMetadata() != null && !po.getMetadata().isEmpty()) {
            try {
                metadata = objectMapper.readValue(po.getMetadata(),
                        new TypeReference<>() {
                        });
            } catch (JsonProcessingException e) {
                log.error("将JSON转换为向量元数据时出错", e);
            }
        }

        return Vector.builder()
                .id(po.getId())
                .externalId(po.getExternalId())
                .vectorType(VectorType.fromCode(po.getVectorType()))
                .dimensions(po.getDimensions())
                .embedding(po.getEmbedding())
                .indexName(po.getIndexName())
                .createTime(po.getCreateTime())
                .updateTime(po.getUpdateTime())
                .metadata(metadata)
                .embeddingModel(po.getEmbeddingModel())
                .vectorNorm(po.getVectorNorm())
                .vectorCategory(VectorCategory.fromCode(po.getVectorCategory()))
                .contentSummary(po.getContentSummary())
                .spaceType(SpaceType.fromCode(po.getSpaceType()))
                .isPrimary(po.getIsPrimary())
                .build();
    }

    /**
     * 批量将领域实体转换为持久化对象
     *
     * @param entities 向量领域实体列表
     * @return 向量持久化对象列表
     */
    public List<VectorPO> toPOList(List<Vector> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }

        return entities.stream()
                .map(this::toPO)
                .collect(Collectors.toList());
    }

    /**
     * 批量将持久化对象转换为领域实体
     *
     * @param pos 向量持久化对象列表
     * @return 向量领域实体列表
     */
    public List<Vector> toEntityList(List<VectorPO> pos) {
        if (pos == null) {
            return Collections.emptyList();
        }

        return pos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

}
