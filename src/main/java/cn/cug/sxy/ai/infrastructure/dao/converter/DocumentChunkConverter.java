package cn.cug.sxy.ai.infrastructure.dao.converter;

import cn.cug.sxy.ai.domain.document.model.entity.DocumentChunk;
import cn.cug.sxy.ai.infrastructure.dao.po.DocumentChunkPO;
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
 * @Date 2025/9/8 16:54
 * @Description 文档块实体与持久化对象转换器
 * @Author jerryhotton
 */

@Slf4j
@Component
public class DocumentChunkConverter {

    private final ObjectMapper objectMapper;

    public DocumentChunkConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将领域实体转换为持久化对象
     *
     * @param entity 文档块领域实体
     * @return 文档块持久化对象
     */
    public DocumentChunkPO toDataObject(DocumentChunk entity) {
        if (entity == null) {
            return null;
        }

        String metadataJson = null;
        if (entity.getMetadata() != null) {
            try {
                metadataJson = objectMapper.writeValueAsString(entity.getMetadata());
            } catch (JsonProcessingException e) {
                log.error("将文档块元数据转换为JSON时出错", e);
                metadataJson = "{}";
            }
        }

        return DocumentChunkPO.builder()
                .id(entity.getId())
                .documentId(entity.getDocumentId())
                .content(entity.getContent())
                .startPosition(entity.getStartPosition())
                .endPosition(entity.getEndPosition())
                .chunkIndex(entity.getChunkIndex())
                .vectorId(entity.getVectorId())
                .vectorized(entity.getVectorized())
                .metadata(metadataJson)
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .overlapLength(entity.getOverlapLength())
                .qualityScore(entity.getQualityScore())
                .build();
    }

    /**
     * 将持久化对象转换为领域实体
     *
     * @param po 文档块持久化对象
     * @return 文档块领域实体
     */
    public DocumentChunk toEntity(DocumentChunkPO po) {
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
                log.error("将JSON转换为文档块元数据时出错", e);
            }
        }

        return DocumentChunk.builder()
                .id(po.getId())
                .documentId(po.getDocumentId())
                .content(po.getContent())
                .startPosition(po.getStartPosition())
                .endPosition(po.getEndPosition())
                .chunkIndex(po.getChunkIndex())
                .vectorId(po.getVectorId())
                .vectorized(po.getVectorized())
                .metadata(metadata)
                .createTime(po.getCreateTime())
                .updateTime(po.getUpdateTime())
                .overlapLength(po.getOverlapLength())
                .qualityScore(po.getQualityScore())
                .build();
    }

    /**
     * 批量将领域实体转换为持久化对象
     *
     * @param entities 文档块领域实体列表
     * @return 文档块持久化对象列表
     */
    public List<DocumentChunkPO> toPOList(List<DocumentChunk> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }

        return entities.stream()
                .map(this::toDataObject)
                .collect(Collectors.toList());
    }

    /**
     * 批量将持久化对象转换为领域实体
     *
     * @param pos 文档块持久化对象列表
     * @return 文档块领域实体列表
     */
    public List<DocumentChunk> toEntityList(List<DocumentChunkPO> pos) {
        if (pos == null) {
            return Collections.emptyList();
        }

        return pos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

}
