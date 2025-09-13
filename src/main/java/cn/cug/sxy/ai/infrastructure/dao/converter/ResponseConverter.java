package cn.cug.sxy.ai.infrastructure.dao.converter;

import cn.cug.sxy.ai.domain.rag.model.entity.Response;
import cn.cug.sxy.ai.infrastructure.dao.po.ResponsePO;
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
 * @Date 2025/9/11 11:33
 * @Description ResponseConverter
 * @Author jerryhotton
 */

@Slf4j
@Component
public class ResponseConverter {

    private final ObjectMapper objectMapper;

    public ResponseConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将领域实体转换为持久化对象
     *
     * @param entity 响应领域实体
     * @return 响应持久化对象
     */
    public ResponsePO toPO(Response entity) {
        if (entity == null) {
            return null;
        }
        String metadata = null;
        if (entity.getMetadata() != null) {
            try {
                metadata = objectMapper.writeValueAsString(entity.getMetadata());
            } catch (JsonProcessingException e) {
                log.error("将响应元数据转换为JSON时出错", e);
                metadata = "{}";
            }
        }

        return ResponsePO.builder()
                .id(entity.getId())
                .queryId(entity.getQueryId())
                .sessionId(entity.getSessionId())
                .answerText(entity.getAnswerText())
                .retrievedContext(entity.getRetrievedContext())
                .contextSources(entity.getContextSources())
                .status(entity.getStatus())
                .createTime(entity.getCreateTime())
                .completeTime(entity.getCompleteTime())
                .latencyMs(entity.getLatencyMs())
                .modelName(entity.getModelName())
                .generationParams(entity.getGenerationParams())
                .evaluationMetrics(entity.getEvaluationMetrics())
                .faithfulnessScore(entity.getFaithfulnessScore())
                .relevanceScore(entity.getRelevanceScore())
                .errorMessage(entity.getErrorMessage())
                .needReview(entity.getNeedReview())
                .metadata(metadata)
                .correctedAnswerText(entity.getCorrectedAnswerText())
                .responseType(entity.getResponseType())
                .build();
    }

    /**
     * 将持久化对象转换为领域实体
     *
     * @param po 响应持久化对象
     * @return 响应领域实体
     */
    public Response toEntity(ResponsePO po) {
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
                log.error("将JSON转换为响应元数据时出错", e);
            }
        }

        return Response.builder()
                .id(po.getId())
                .queryId(po.getQueryId())
                .sessionId(po.getSessionId())
                .answerText(po.getAnswerText())
                .retrievedContext(po.getRetrievedContext())
                .contextSources(po.getContextSources())
                .status(po.getStatus())
                .createTime(po.getCreateTime())
                .completeTime(po.getCompleteTime())
                .latencyMs(po.getLatencyMs())
                .modelName(po.getModelName())
                .generationParams(po.getGenerationParams())
                .evaluationMetrics(po.getEvaluationMetrics())
                .faithfulnessScore(po.getFaithfulnessScore())
                .relevanceScore(po.getRelevanceScore())
                .errorMessage(po.getErrorMessage())
                .needReview(po.getNeedReview())
                .metadata(metadata)
                .correctedAnswerText(po.getCorrectedAnswerText())
                .responseType(po.getResponseType())
                .build();
    }

    /**
     * 批量将领域实体转换为持久化对象
     *
     * @param entities 响应领域实体列表
     * @return 响应持久化对象列表
     */
    public List<ResponsePO> toDataObjects(List<Response> entities) {
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
     * @param pos 响应持久化对象列表
     * @return 响应领域实体列表
     */
    public List<Response> toEntities(List<ResponsePO> pos) {
        if (pos == null) {
            return Collections.emptyList();
        }

        return pos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

}
