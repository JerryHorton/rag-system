package cn.cug.sxy.ai.domain.rag.service.indexing;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @version 1.0
 * @Date 2025/9/8 15:12
 * @Description HTML文档加载器实现
 * @Author jerryhotton
 */

@Component
public class HtmlDocumentLoader implements IDocumentLoader {

    /**
     * 从本地文件路径加载HTML文档
     *
     * @param filePath 文件路径
     * @return 文档内容和元数据的Map，包含text和metadata两个键
     * @throws IOException 如果文件读取或解析失败
     */
    @Override
    public Map<String, Object> loadFromFile(String filePath) throws IOException {
        File htmlFile = new File(filePath);
        Document doc = Jsoup.parse(htmlFile, "UTF-8");
        return extractContentAndMetadata(doc, htmlFile.getName());
    }

    /**
     * 从URL加载HTML文档
     *
     * @param url 文档URL
     * @return 文档内容和元数据的Map，包含text和metadata两个键
     * @throws IOException 如果URL连接或解析失败
     */
    @Override
    public Map<String, Object> loadFromUrl(String url) throws IOException {
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(10000)
                .get();
        String title = url.substring(url.lastIndexOf('/') + 1);
        if (title.isEmpty()) {
            title = new URL(url).getHost();
        }

        return extractContentAndMetadata(doc, title);
    }

    /**
     * 从Jsoup Document对象中提取文本内容和元数据
     *
     * @param doc Jsoup Document对象
     * @param title 文档标题
     * @return 包含文本内容和元数据的Map
     */
    private Map<String, Object> extractContentAndMetadata(Document doc, String title) {
        Map<String, Object> result = new HashMap<>();

        // 提取纯文本内容，移除脚本和样式元素
        String text = doc.body().text();
        result.put("text", text);
        // 提取元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title", doc.title().isEmpty() ? title : doc.title());
        metadata.put("description", doc.select("meta[name=description]").attr("content"));
        metadata.put("keywords", doc.select("meta[name=keywords]").attr("content"));
        metadata.put("author", doc.select("meta[name=author]").attr("content"));
        metadata.put("language", doc.select("html").attr("lang"));
        metadata.put("charSet", doc.charset().name());
        metadata.put("headings", doc.select("h1, h2, h3").text());

        result.put("metadata", metadata);

        return result;
    }

    /**
     * 判断当前加载器是否支持指定的文件类型
     *
     * @param fileExtension 文件扩展名
     * @return 如果是HTML文件(.html, .htm)则返回true
     */
    @Override
    public boolean supports(String fileExtension) {
        return fileExtension.equalsIgnoreCase("html") ||
                fileExtension.equalsIgnoreCase("htm");
    }

}
