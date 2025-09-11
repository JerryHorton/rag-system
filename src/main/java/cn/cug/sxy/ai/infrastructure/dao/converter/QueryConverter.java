package cn.cug.sxy.ai.infrastructure.dao.converter;

import cn.cug.sxy.ai.domain.document.model.entity.Query;
import cn.cug.sxy.ai.infrastructure.dao.po.QueryPO;
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
 * @Date 2025/9/8 17:25
 * @Description 查询实体与持久化对象转换器
 * @Author jerryhotton
 */

@Slf4j
@Component
public class QueryConverter {

    private final ObjectMapper objectMapper;

    public QueryConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将领域实体转换为持久化对象
     *
     * @param entity 查询领域实体
     * @return 查询持久化对象
     */
    public QueryPO toPO(Query entity) {
        if (entity == null) {
            return null;
        }

        String metadataJson = null;
        if (entity.getMetadata() != null) {
            try {
                metadataJson = objectMapper.writeValueAsString(entity.getMetadata());
            } catch (JsonProcessingException e) {
                log.error("将查询元数据转换为JSON时出错", e);
                metadataJson = "{}";
            }
        }

        return QueryPO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .sessionId(entity.getSessionId())
                .originalText(entity.getOriginalText())
                .processedText(entity.getProcessedText())
                .status(entity.getStatus())
                .queryType(entity.getQueryType())
                .routeTarget(entity.getRouteTarget())
                .createTime(entity.getCreateTime())
                .completeTime(entity.getCompleteTime())
                .latencyMs(entity.getLatencyMs())
                .metadataJson(metadataJson)
                .queryVariants(entity.getQueryVariants())
                .decomposedQueries(entity.getDecomposedQueries())
                .responseIds(entity.getResponseIds())
                .errorMessage(entity.getErrorMessage())
                .retrievalParams(entity.getRetrievalParams())
                .build();
    }

    /**
     * 将持久化对象转换为领域实体
     *
     * @param po 查询持久化对象
     * @return 查询领域实体
     */
    public Query toEntity(QueryPO po) {
        if (po == null) {
            return null;
        }

        Map<String, Object> metadata = new HashMap<>();
        if (po.getMetadataJson() != null && !po.getMetadataJson().isEmpty()) {
            try {
                metadata = objectMapper.readValue(po.getMetadataJson(),
                        new TypeReference<Map<String, Object>>() {
                        });
            } catch (JsonProcessingException e) {
                log.error("将JSON转换为查询元数据时出错", e);
            }
        }

        return Query.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .sessionId(po.getSessionId())
                .originalText(po.getOriginalText())
                .processedText(po.getProcessedText())
                .status(po.getStatus())
                .queryType(po.getQueryType())
                .routeTarget(po.getRouteTarget())
                .createTime(po.getCreateTime())
                .completeTime(po.getCompleteTime())
                .latencyMs(po.getLatencyMs())
                .metadata(metadata)
                .queryVariants(po.getQueryVariants())
                .decomposedQueries(po.getDecomposedQueries())
                .responseIds(po.getResponseIds())
                .errorMessage(po.getErrorMessage())
                .retrievalParams(po.getRetrievalParams())
                .build();
    }

    /**
     * 批量将领域实体转换为持久化对象
     *
     * @param entities 查询领域实体列表
     * @return 查询持久化对象列表
     */
    public List<QueryPO> toPOList(List<Query> entities) {
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
     * @param pos 查询持久化对象列表
     * @return 查询领域实体列表
     */
    public List<Query> toEntityList(List<QueryPO> pos) {
        if (pos == null) {
            return Collections.emptyList();
        }

        return pos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

}
