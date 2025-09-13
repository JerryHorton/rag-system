package cn.cug.sxy.ai.domain.rag.service.indexing;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/8 16:08
 * @Description 文本文档加载器实现
 * @Author jerryhotton
 */

@Component
public class TextDocumentLoader implements IDocumentLoader {

    /**
     * 从本地文件路径加载文本文档
     *
     * @param filePath 文件路径
     * @return 文档内容和元数据的Map，包含text和metadata两个键
     * @throws IOException 如果文件读取失败
     */
    @Override
    public Map<String, Object> loadFromFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        String content = Files.readString(path, StandardCharsets.UTF_8);

        File file = path.toFile();
        Map<String, Object> metadata = extractMetadata(file, path);

        Map<String, Object> result = new HashMap<>();
        result.put("text", content);
        result.put("metadata", metadata);

        return result;
    }

    /**
     * 从URL加载文本文档
     *
     * @param url 文档URL
     * @return 文档内容和元数据的Map，包含text和metadata两个键
     * @throws IOException 如果URL连接或读取失败
     */
    @Override
    public Map<String, Object> loadFromUrl(String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        String content;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            content = sb.toString();
        }
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", url);
        metadata.put("contentType", connection.getContentType());
        metadata.put("contentLength", connection.getContentLength());
        metadata.put("lastModified", connection.getLastModified());

        String fileName = url.substring(url.lastIndexOf('/') + 1);
        if (!fileName.contains(".")) {
            fileName = "document.txt";
        }
        metadata.put("fileName", fileName);

        Map<String, Object> result = new HashMap<>();
        result.put("text", content);
        result.put("metadata", metadata);

        return result;
    }

    /**
     * 提取文件的元数据信息
     *
     * @param file 文件对象
     * @param path 文件路径
     * @return 包含文件元数据的Map
     * @throws IOException 如果无法获取文件属性
     */
    private Map<String, Object> extractMetadata(File file, Path path) throws IOException {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileName", file.getName());
        metadata.put("fileSize", file.length());
        metadata.put("lastModified", file.lastModified());
        metadata.put("absolutePath", file.getAbsolutePath());
        metadata.put("isHidden", file.isHidden());
        metadata.put("isReadOnly", !file.canWrite());
        metadata.put("encoding", StandardCharsets.UTF_8.name());
        // 获取额外的文件属性
        try {
            metadata.put("creationTime", Files.getAttribute(path, "creationTime"));
            metadata.put("lastAccessTime", Files.getAttribute(path, "lastAccessTime"));
            metadata.put("lastModifiedTime", Files.getAttribute(path, "lastModifiedTime"));
            metadata.put("owner", Files.getOwner(path).getName());
        } catch (UnsupportedOperationException | IOException e) {
            // 某些文件系统可能不支持特定属性，忽略这些异常
            metadata.put("attributeError", e.getMessage());
        }

        return metadata;
    }

    /**
     * 判断当前加载器是否支持指定的文件类型
     *
     * @param fileExtension 文件扩展名
     * @return 如果是文本文件(.txt)则返回true
     */
    @Override
    public boolean supports(String fileExtension) {
        return fileExtension.equalsIgnoreCase("txt");
    }

}
