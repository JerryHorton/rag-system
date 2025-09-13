package cn.cug.sxy.ai.domain.rag.service;

import cn.cug.sxy.ai.domain.rag.model.entity.Document;

import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/9 10:17
 * @Description 文档管理服务接口
 * @Author jerryhotton
 */

public interface IDocumentService {

    /**
     * 处理已保存到本地的上传文件（控制器已保存临时文件）
     *
     * @param absolutePath     绝对路径
     * @param originalFilename 原始文件名
     * @param metadataJson     元数据json字符串
     * @return 文档实体
     * @throws Exception 异常
     */
    Document processFile(String absolutePath, String originalFilename, String metadataJson) throws Exception;

    /**
     * 处理URL来源的文档
     *
     * @param url      url
     * @param metadata 元数据
     * @return 文档实体
     * @throws Exception 异常
     */
    Document processUrl(String url, Map<String, Object> metadata) throws Exception;

    /**
     * 处理文本来源的文档
     *
     * @param content  文本内容
     * @param title    标题
     * @param metadata 元数据
     * @return 文档实体
     * @throws Exception 异常
     */
    Document processText(String content, String title, Map<String, Object> metadata) throws Exception;

}
