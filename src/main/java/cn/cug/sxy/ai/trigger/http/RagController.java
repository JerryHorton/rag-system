package cn.cug.sxy.ai.trigger.http;

import cn.cug.sxy.ai.api.IRagService;
import cn.cug.sxy.ai.api.vo.DocumentDetailVO;
import cn.cug.sxy.ai.domain.document.model.entity.Document;
import cn.cug.sxy.ai.domain.document.service.indexing.DocumentProcessingService;
import cn.cug.sxy.ai.trigger.http.converter.ToVOConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

    private final DocumentProcessingService documentService;

    public RagController(DocumentProcessingService documentService) {
        this.documentService = documentService;
    }

    @RequestMapping(value = "/documents/upload", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentDetailVO> uploadDocument(
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

            return ResponseEntity.status(HttpStatus.CREATED).body(ToVOConverter.convertToDocumentVO(document));
        } catch (IOException e) {
            log.error("文件处理失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
