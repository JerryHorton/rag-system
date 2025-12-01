package cn.cug.sxy.ai.trigger.http;

import cn.cug.sxy.ai.api.IRagService;
import cn.cug.sxy.ai.api.dto.QueryRequestDTO;
import cn.cug.sxy.ai.api.response.ApiResponse;
import cn.cug.sxy.ai.api.vo.DocumentDetailVO;
import cn.cug.sxy.ai.api.vo.ResponseVO;
import cn.cug.sxy.ai.domain.rag.model.valobj.QueryParams;
import cn.cug.sxy.ai.domain.rag.service.IRagOrchestrationService;
import cn.cug.sxy.ai.domain.rag.model.entity.Document;
import cn.cug.sxy.ai.domain.rag.model.entity.Response;
import cn.cug.sxy.ai.domain.rag.service.IDocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * @version 1.0
 * @Date 2025/9/9 10:32
 * @Description RAG系统REST API控制器
 * @Author jerryhotton
 */

@Slf4j
@RestController
@RequestMapping("/api/rag")
public class RagController implements IRagService {

    private final IDocumentService documentService;
    private final IRagOrchestrationService ragService;

    public RagController(
            IDocumentService documentService,
            IRagOrchestrationService ragService) {
        this.documentService = documentService;
        this.ragService = ragService;
    }

    @RequestMapping(value = "/documents/upload", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DocumentDetailVO> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "metadata", required = false) String metadata) {
        try {
            log.info("收到文件上传请求: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());
            // 保存文件到临时目录
            Path tempFile = Files.createTempFile("upload_", "_" + file.getOriginalFilename());
            file.transferTo(tempFile.toFile());
            // 处理文档
            Document document = documentService.processFile(
                    tempFile.toString(),
                    file.getOriginalFilename(),
                    metadata
            );
            // 删除临时文件
            Files.deleteIfExists(tempFile);

            return ApiResponse.success("文档上传成功", DocumentDetailVO.convertToDocumentVO(document));
        } catch (Exception e) {
            log.error("文件处理失败: {}", e.getMessage(), e);
            return ApiResponse.error("文件处理失败: " + e.getMessage());
        }
    }

    @RequestMapping(value = "/query", method = RequestMethod.POST)
    @Override
    public ApiResponse<ResponseVO> queryDocument(@RequestBody QueryRequestDTO requestDTO) {
        log.info("收到查询请求: {}", requestDTO.getQuery());
        QueryRequestDTO.ExtraParams extraParams = requestDTO.getParams();
        String sessionId = requestDTO.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }
        try {
            Response response = ragService.processQuery(
                    requestDTO.getQuery(),
                    requestDTO.getUserId(),
                    sessionId,
                    buildQueryParams(extraParams)
            );
            ResponseVO responseVO = ResponseVO.convertToResponseVO(response);

            return ApiResponse.success(responseVO);
        } catch (Exception e) {
            log.error("查询处理失败: {}", e.getMessage(), e);
            return ApiResponse.error("查询处理失败: " + e.getMessage());
        }
    }

    private QueryParams buildQueryParams(QueryRequestDTO.ExtraParams params) {
        if (params == null) {
            return QueryParams.builder().build();
        }
        return QueryParams.builder()
                .topK(params.getTopK())
                .minScore(params.getMinScore())
                .router(params.getRouter())
                .indexName(params.getIndexName())
                .similarityThreshold(params.getSimilarityThreshold())
                .limit(params.getLimit())
                .candidateMultiplier(params.getCandidateMultiplier())
                .rerankerEnabled(params.getRerankerEnabled())
                .multiQueryEnabled(params.getMultiQueryEnabled())
                .selfRagEnabled(params.getSelfRagEnabled())
                .hydeEnabled(params.getHydeEnabled())
                .stepBackEnabled(params.getStepBackEnabled())
                .model(params.getModel())
                .clarify(params.getClarify())
                .forceType(params.getForceType())
                .build();
    }

}
