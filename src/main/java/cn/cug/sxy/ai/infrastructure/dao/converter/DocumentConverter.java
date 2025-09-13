package cn.cug.sxy.ai.infrastructure.dao.converter;

import cn.cug.sxy.ai.domain.rag.model.entity.Document;
import cn.cug.sxy.ai.infrastructure.dao.po.DocumentPO;
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
 * @Date 2025/9/8 16:44
 * @Description 文档实体与持久化对象转换器
 * @Author jerryhotton
 */

@Slf4j
@Component
public class DocumentConverter {

    private final ObjectMapper objectMapper;

    public DocumentConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DocumentPO toPO(Document entity) {
        if (entity == null) {
            return null;
        }
        String metadataJson = null;
        if (entity.getMetadata() != null) {
            try {
                metadataJson = objectMapper.writeValueAsString(entity.getMetadata());
            } catch (JsonProcessingException e) {
                log.error("将文档元数据转换为JSON时出错", e);
                metadataJson = "{}";
            }
        }

        return DocumentPO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .source(entity.getSource())
                .documentType(entity.getDocumentType())
                .status(entity.getStatus())
                .batchId(entity.getBatchId())
                .parentId(entity.getParentId())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .metadata(metadataJson)
                .position(entity.getPosition())
                .vectorized(entity.getVectorized())
                .errorMessage(entity.getErrorMessage())
                .build();
    }

    public Document toEntity(DocumentPO po) {
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
                log.error("将JSON转换为文档元数据时出错", e);
            }
        }

        return Document.builder()
                .id(po.getId())
                .title(po.getTitle())
                .content(po.getContent())
                .source(po.getSource())
                .documentType(po.getDocumentType())
                .status(po.getStatus())
                .batchId(po.getBatchId())
                .parentId(po.getParentId())
                .createTime(po.getCreateTime())
                .updateTime(po.getUpdateTime())
                .metadata(metadata)
                .position(po.getPosition())
                .vectorized(po.getVectorized())
                .errorMessage(po.getErrorMessage())
                .build();
    }

    public List<Document> toEntityList(List<DocumentPO> pos) {
        if (pos == null) {
            return Collections.emptyList();
        }

        return pos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    public List<DocumentPO> toPOList(List<Document> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }

        return entities.stream()
                .map(this::toPO)
                .collect(Collectors.toList());
    }

}
