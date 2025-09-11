package cn.cug.sxy.ai.trigger.http.converter;

import cn.cug.sxy.ai.api.vo.DocumentDetailVO;
import cn.cug.sxy.ai.domain.document.model.entity.Document;

/**
 * @version 1.0
 * @Date 2025/9/9 10:38
 * @Description vo转换器
 * @Author jerryhotton
 */

public class ToVOConverter {

    public static DocumentDetailVO convertToDocumentVO(Document document) {
        DocumentDetailVO vo = new DocumentDetailVO();
        vo.setId(document.getId());
        vo.setTitle(document.getTitle());
        vo.setSource(document.getSource());
        vo.setDocumentType(document.getDocumentType());
        vo.setStatus(document.getStatus());
        vo.setVectorized(document.getVectorized());
        vo.setCreateTime(document.getCreateTime());
        vo.setUpdateTime(document.getUpdateTime());
        vo.setMetadata(document.getMetadata());
        vo.setErrorMessage(document.getErrorMessage());
        String content = document.getContent();
        if (content != null) {
            vo.setContentPreview(content.length() > 200 ? content.substring(0, 200) + "..." : content);
        }

        return vo;
    }

}
