package cn.cug.sxy.ai.domain.document.service.indexing;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/8 15:01
 * @Description 文档加载器接口
 * @Author jerryhotton
 */

public interface IDocumentLoader {

    /**
     * 从文件加载文档内容
     *
     * @param filePath 文件路径
     * @return 包含文档内容和元数据的Map
     * @throws IOException 文件读取异常
     */
    Map<String, Object> loadFromFile(String filePath) throws IOException;

    /**
     * 从URL加载文档内容
     *
     * @param url 文档URL
     * @return 包含文档内容和元数据的Map
     * @throws IOException URL访问异常
     */
    Map<String, Object> loadFromUrl(String url) throws IOException;

    /**
     * 判断当前加载器是否支持指定的文件类型
     *
     * @param fileExtension 文件扩展名
     * @return 如果支持该文件类型则返回true
     */
    boolean supports(String fileExtension);

}
