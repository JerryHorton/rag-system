package cn.cug.sxy.ai.api;

import cn.cug.sxy.ai.api.vo.DocumentDetailVO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/**
 * @version 1.0
 * @Date 2025/9/9 10:22
 * @Description Rag服务接口
 * @Author jerryhotton
 */

public interface IRagService {

    /**
     * 上传文档
     *
     * @param file     文档文件
     * @param metadata 文档元数据
     * @return 文档详情VO
     */
    ResponseEntity<DocumentDetailVO> uploadDocument(MultipartFile file, String metadata);

}
