package cn.cug.sxy.ai.domain.rag.service.indexing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @version 1.0
 * @Date 2025/9/8 15:02
 * @Description 文档加载器工厂
 * @Author jerryhotton
 */

@Slf4j
@Component
public class DocumentLoaderFactory {

    /**
     * 文档加载器映射表：扩展名 -> 加载器实例
     */
    private final Map<String, IDocumentLoader> loaderMap = new HashMap<>();

    /**
     * 默认文档加载器，用于未知格式
     */
    private IDocumentLoader defaultLoader;

    /**
     * 构造函数，自动注入所有实现了DocumentLoader接口的Bean
     *
     * @param loaders DocumentLoader实现类集合
     */
    public DocumentLoaderFactory(Set<IDocumentLoader> loaders) {
        for (IDocumentLoader loader : loaders) {
            registerLoader(loader);
        }

        // 检查是否有默认加载器
        if (defaultLoader == null && !loaderMap.isEmpty()) {
            defaultLoader = loaderMap.values().iterator().next();
            log.info("Set first available loader as default: {}", defaultLoader.getClass().getSimpleName());
        }
    }

    /**
     * 注册文档加载器
     *
     * @param loader 文档加载器实例
     */
    public void registerLoader(IDocumentLoader loader) {
        if (loader instanceof TextDocumentLoader) {
            loaderMap.put("txt", loader);
            defaultLoader = loader;
            log.info("Registered TextDocumentLoader for extension: txt");
        } else if (loader instanceof PdfDocumentLoader) {
            loaderMap.put("pdf", loader);
            log.info("Registered PdfDocumentLoader for extension: pdf");
        } else if (loader instanceof HtmlDocumentLoader) {
            loaderMap.put("html", loader);
            loaderMap.put("htm", loader);
            log.info("Registered HtmlDocumentLoader for extensions: html, htm");
        } else if (loader instanceof WordDocumentLoader) {
            loaderMap.put("docx", loader);
            loaderMap.put("doc", loader);
            log.info("Registered WordDocumentLoader for extensions: docx, doc");
        } else if (loader instanceof ExcelDocumentLoader) {
            loaderMap.put("xlsx", loader);
            loaderMap.put("xls", loader);
            log.info("Registered ExcelDocumentLoader for extensions: xlsx, xls");
        } else if (loader instanceof PowerPointDocumentLoader) {
            loaderMap.put("pptx", loader);
            loaderMap.put("ppt", loader);
            log.info("Registered PowerPointDocumentLoader for extensions: pptx, ppt");
        } else {
            log.warn("Unknown DocumentLoader type: {}", loader.getClass().getName());
        }
    }

    /**
     * 注册文档加载器，可指定支持的文件扩展名
     *
     * @param extensions 文件扩展名数组
     * @param loader 文档加载器实例
     */
    public void registerLoader(String[] extensions, IDocumentLoader loader) {
        for (String extension : extensions) {
            loaderMap.put(extension.toLowerCase(), loader);
            log.info("Registered {} for extension: {}", loader.getClass().getSimpleName(), extension);
        }
    }

    /**
     * 根据文件扩展名获取合适的文档加载器
     *
     * @param fileExtension 文件扩展名
     * @return 文档加载器实例
     */
    public IDocumentLoader getLoader(String fileExtension) {
        if (fileExtension == null || fileExtension.isEmpty()) {
            log.warn("Empty file extension, using default loader");
            return defaultLoader;
        }

        String extension = fileExtension.toLowerCase();
        IDocumentLoader loader = loaderMap.get(extension);

        if (loader == null) {
            log.warn("No loader found for extension: {}, using default loader", extension);
            return defaultLoader;
        }

        return loader;
    }

    /**
     * 设置默认文档加载器
     *
     * @param loader 默认文档加载器实例
     */
    public void setDefaultLoader(IDocumentLoader loader) {
        this.defaultLoader = loader;
        log.info("Set default loader: {}", loader.getClass().getSimpleName());
    }

}
