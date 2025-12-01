package cn.cug.sxy.ai.domain.rag.service.parsing;

import cn.cug.sxy.ai.domain.rag.model.parsing.ParsingMode;
import cn.cug.sxy.ai.domain.rag.model.parsing.StructuredDocument;
import cn.cug.sxy.ai.domain.rag.service.parsing.extractor.PdfBoxTextExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 混合文档解析器。
 * 实现技术指南中的"动态解析器"模式：
 * 1. 预检：尝试直接文本提取（最便宜）
 * 2. 质量评估：判断文本质量
 * 3. 降级：如果质量不足，升级到OCR
 * 4. 后处理：跨页表格合并、置信度过滤
 * <p>
 * 技术指南核心策略：
 * - "先局部细看 -> 再大幅压缩 -> 最后全局理解"
 * - 智能权衡成本、效率和质量
 *
 * @author jerryhotton
 */
@Slf4j
@Service
public class HybridDocumentParser {

    private final List<TextExtractor> textExtractors;
    private final TextQualityEvaluator qualityEvaluator;
    private final OcrService ocrService;
    private final PdfBoxTextExtractor pdfBoxTextExtractor;
    private final PdfRenderService pdfRenderService;
    private final TableMergeService tableMergeService;
    private final OcrEvaluationService evaluationService;
    private final PdfTableDetector tableDetector;
    private final ParsingResultCache parsingResultCache;
    private final DynamicTimeoutCalculator timeoutCalculator;

    @Value("${rag.parsing.force-ocr:false}")
    private boolean forceOcr;

    @Value("${rag.parsing.enable-hybrid-parser:true}")
    private boolean enableHybridParser;

    @Value("${rag.parsing.confidence-threshold:0.0}")
    private double confidenceThreshold; // 置信度阈值，低于此值的元素将被过滤

    @Value("${rag.parsing.parallel-pages:4}")
    private int parallelPages; // 并行处理的页面数

    @Value("${rag.parsing.enable-table-merge:true}")
    private boolean enableTableMerge; // 是否启用跨页表格合并

    @Value("${rag.parsing.enable-table-detection:true}")
    private boolean enableTableDetection; // 是否启用表格检测

    @Value("${rag.parsing.page-timeout-seconds:60}")
    private int pageTimeoutSeconds; // 单页处理超时时间（秒）

    @Value("${rag.parsing.max-total-timeout-seconds:3600}")
    private int maxTotalTimeoutSeconds; // 最大总超时时间（秒），默认1小时

    @Value("${rag.parsing.enable-cache:true}")
    private boolean enableCache; // 是否启用解析结果缓存

    @Value("${rag.parsing.enable-dynamic-timeout:true}")
    private boolean enableDynamicTimeout; // 是否启用动态超时计算

    @Value("${rag.parsing.max-page-retries:3}")
    private int maxPageRetries; // 单页最大重试次数

    @Value("${rag.parsing.timeout-multiplier:3.0}")
    private double timeoutMultiplier; // 超时时间倍数，用于计算总超时

    // 线程池用于并行处理
    private final ExecutorService executorService;

    public HybridDocumentParser(List<TextExtractor> textExtractors,
                                TextQualityEvaluator qualityEvaluator,
                                OcrService ocrService,
                                PdfBoxTextExtractor pdfBoxTextExtractor,
                                PdfRenderService pdfRenderService,
                                TableMergeService tableMergeService,
                                OcrEvaluationService evaluationService,
                                PdfTableDetector tableDetector,
                                ParsingResultCache parsingResultCache,
                                DynamicTimeoutCalculator timeoutCalculator) {
        this.textExtractors = textExtractors;
        this.qualityEvaluator = qualityEvaluator;
        this.ocrService = ocrService;
        this.pdfBoxTextExtractor = pdfBoxTextExtractor;
        this.pdfRenderService = pdfRenderService;
        this.tableMergeService = tableMergeService;
        this.evaluationService = evaluationService;
        this.tableDetector = tableDetector;
        this.parsingResultCache = parsingResultCache;
        this.timeoutCalculator = timeoutCalculator;

        // 创建固定大小的线程池
        // 确保线程池大小足够支持配置的并行度
        int poolSize = Math.max(parallelPages, Math.max(4, Runtime.getRuntime().availableProcessors()));
        this.executorService = Executors.newFixedThreadPool(poolSize);

        log.info("HybridDocumentParser初始化完成，线程池: {}, 并行页面数: {}, 置信度阈值: {}, 表格检测: {}, 缓存: {}, 动态超时: {}, 最大重试: {}",
                poolSize, parallelPages, confidenceThreshold, enableTableDetection, enableCache, enableDynamicTimeout, maxPageRetries);
    }

    // ==================== 用户选择模式的解析方法（推荐使用） ====================

    /**
     * 根据用户选择的模式解析文档（推荐方法）。
     * <p>
     * 使用示例：
     * - 纯文本提取：parseFromFile(path, "pdf", ParsingMode.SIMPLE)
     * - 逐页OCR（推荐）：parseFromFile(path, "pdf", ParsingMode.OCR)
     * - 让系统决定：parseFromFile(path, "pdf", ParsingMode.AUTO)
     *
     * @param filePath      文件路径
     * @param fileExtension 文件扩展名
     * @param mode          解析模式
     * @return 解析结果
     * @throws IOException 解析失败
     */
    public ParsingResult parseFromFile(String filePath, String fileExtension, ParsingMode mode) throws IOException {
        log.info("开始解析文档，文件: {}, 模式: {}", filePath, mode.getDisplayName());

        return switch (mode) {
            case SIMPLE ->
                // 简单模式：直接文本提取
                    parseSimpleMode(filePath, fileExtension);
            case OCR ->
                // OCR模式：逐页渲染+VLM识别（推荐）
                    parseOcrMode(filePath, fileExtension);
            default ->
                // 自动模式：智能检测
                    parseAutoMode(filePath, fileExtension);
        };
    }

    /**
     * 简单模式解析 - 纯文本提取。
     * 速度快、成本低，但不保留表格结构。
     */
    private ParsingResult parseSimpleMode(String filePath, String fileExtension) throws IOException {
        log.debug("使用简单模式（文本提取）解析，文件: {}", filePath);

        TextExtractor extractor = findExtractor(fileExtension);
        if (extractor == null) {
            log.warn("未找到文本提取器，文件扩展名: {}，降级到OCR", fileExtension);
            return parseWithOcr(filePath, fileExtension);
        }

        try {
            TextExtractor.ExtractionResult extractionResult = extractor.extractFromFile(filePath);
            String text = extractionResult.getText();

            if (text == null || text.isEmpty()) {
                log.warn("文本提取结果为空，降级到OCR，文件: {}", filePath);
                return parseWithOcr(filePath, fileExtension);
            }

            Map<String, Object> metadata = extractionResult.getMetadata();
            if (metadata == null) {
                metadata = new HashMap<>();
            }
            metadata.put("parsingMode", "SIMPLE");

            log.info("简单模式解析完成，文件: {}, 文本长度: {}", filePath, text.length());

            return ParsingResult.builder()
                    .text(text)
                    .structuredDocument(null)
                    .metadata(metadata)
                    .parsingMethod("text_extraction")
                    .qualityScore(qualityEvaluator.calculateQualityScore(text))
                    .build();

        } catch (Exception e) {
            log.warn("简单模式解析失败，降级到OCR，文件: {}, 错误: {}", filePath, e.getMessage());
            return parseWithOcr(filePath, fileExtension);
        }
    }

    /**
     * OCR模式解析 - 逐页渲染+VLM识别（推荐）。
     * <p>
     * 将PDF逐页渲染为图片，使用VLM进行识别：
     * - 保留表格结构（Markdown格式）
     * - 识别公式（LaTeX格式）
     * - 理解图片内容
     * - 无token限制（逐页处理）
     */
    private ParsingResult parseOcrMode(String filePath, String fileExtension) throws IOException {
        log.debug("使用OCR模式解析，文件: {}", filePath);

        ParsingResult result = parseWithOcr(filePath, fileExtension);

        if (result.getMetadata() == null) {
            result.setMetadata(new HashMap<>());
        }
        result.getMetadata().put("parsingMode", "OCR");

        log.info("OCR模式解析完成，文件: {}", filePath);
        return result;
    }

    /**
     * 自动模式解析 - 智能检测并选择最佳策略。
     * <p>
     * 策略：
     * 1. 首先尝试直接文本提取，评估质量
     * 2. 如果是纯文本且质量高，使用简单模式（快速、免费）
     * 3. 如果是扫描件或复杂文档，使用OCR模式（逐页渲染+VLM）
     */
    private ParsingResult parseAutoMode(String filePath, String fileExtension) throws IOException {
        log.debug("使用自动模式解析，文件: {}", filePath);

        // 仅对PDF使用智能检测，其他格式使用简单模式
        if (!"pdf".equalsIgnoreCase(fileExtension)) {
            return parseSimpleMode(filePath, fileExtension);
        }

        try {
            // 1. 尝试直接提取文本评估质量
            TextExtractor.ExtractionResult quickExtract = pdfBoxTextExtractor.extractFromFile(filePath);
            String text = quickExtract.getText();

            // 2. 评估文本质量，判断文档类型
            boolean isHighQuality = text != null && !text.isEmpty() && qualityEvaluator.isHighQualityText(text);
            boolean isScanDocument = text == null || text.length() < 100;

            // 3. 检测是否包含表格（需要OCR保留结构）
            boolean hasTable = false;
            if (enableTableDetection && !isScanDocument) {
                try {
                    PdfTableDetector.TableDetectionResult tableResult = tableDetector.detectTables(filePath);
                    hasTable = tableResult.isHasTable();
                    if (hasTable) {
                        log.debug("检测到表格，分数: {}", tableResult.getTableScore());
                    }
                } catch (Exception e) {
                    log.warn("表格检测失败: {}", e.getMessage());
                }
            }

            if (isScanDocument) {
                // 扫描件：使用OCR模式
                log.info("自动检测：疑似扫描件，使用OCR模式，文件: {}", filePath);
                ParsingResult result = parseOcrMode(filePath, fileExtension);
                result.getMetadata().put("autoDetectedType", "scan_document");
                result.getMetadata().put("parsingMode", "AUTO->OCR");
                return result;
            }

            if (hasTable || !isHighQuality) {
                // 包含表格或复杂文档：使用OCR模式保留结构
                log.info("自动检测：复杂文档（表格={}，质量={}），使用OCR模式，文件: {}",
                        hasTable, isHighQuality, filePath);
                ParsingResult result = parseOcrMode(filePath, fileExtension);
                result.getMetadata().put("autoDetectedType", "complex_document");
                result.getMetadata().put("hasTable", hasTable);
                result.getMetadata().put("parsingMode", "AUTO->OCR");
                return result;
            }

            // 纯文本且质量高：使用简单模式（快速、免费）
            log.info("自动检测：纯文本文档，使用简单模式，文件: {}", filePath);
            ParsingResult result = parseSimpleMode(filePath, fileExtension);
            result.getMetadata().put("autoDetectedType", "text_document");
            result.getMetadata().put("parsingMode", "AUTO->SIMPLE");
            return result;

        } catch (Exception e) {
            log.warn("自动检测失败，使用OCR模式，文件: {}, 错误: {}", filePath, e.getMessage());
            ParsingResult result = parseOcrMode(filePath, fileExtension);
            result.getMetadata().put("autoDetectedType", "fallback");
            result.getMetadata().put("parsingMode", "AUTO->OCR");
            return result;
        }
    }

    // ==================== 原有的自动检测解析方法 ====================

    /**
     * 解析文档（从文件）- 自动检测模式。
     * <p>
     * 智能解析策略：
     * 1. 强制OCR模式：直接使用OCR
     * 2. 表格检测：如果检测到表格，使用OCR保留结构
     * 3. 文本质量评估：如果文本质量高且无表格，使用文本提取
     * 4. 降级机制：文本提取失败或质量不足时，降级到OCR
     *
     * @param filePath      文件路径
     * @param fileExtension 文件扩展名
     * @return 解析结果
     * @throws IOException 解析失败
     */
    public ParsingResult parseFromFile(String filePath, String fileExtension) throws IOException {
        // 1. 强制OCR模式
        if (!enableHybridParser || forceOcr) {
            log.info("强制OCR模式，直接使用OCR解析");
            return parseWithOcr(filePath, fileExtension);
        }

        // 2. 表格检测（仅PDF）
        if ("pdf".equalsIgnoreCase(fileExtension) && enableTableDetection) {
            try {
                PdfTableDetector.TableDetectionResult tableResult = tableDetector.detectTables(filePath);

                if (tableResult.isHasTable()) {
                    log.info("检测到表格（分数: {}），使用OCR解析以保留表格结构，文件: {}",
                            tableResult.getTableScore(), filePath);

                    ParsingResult result = parseWithOcr(filePath, fileExtension);

                    // 在元数据中记录表格检测信息
                    if (result.getMetadata() != null) {
                        result.getMetadata().put("tableDetected", true);
                        result.getMetadata().put("tableScore", tableResult.getTableScore());
                        result.getMetadata().put("tablePages", tableResult.getTablePages());
                    }

                    return result;
                } else {
                    log.debug("未检测到表格（分数: {}），尝试文本提取，文件: {}",
                            tableResult.getTableScore(), filePath);
                }
            } catch (Exception e) {
                log.warn("表格检测失败，继续尝试文本提取，文件: {}, 错误: {}", filePath, e.getMessage());
            }
        }

        // 3. 尝试直接文本提取（预检）
        TextExtractor extractor = findExtractor(fileExtension);
        if (extractor == null) {
            log.warn("未找到文本提取器，文件扩展名: {}，直接使用OCR", fileExtension);
            return parseWithOcr(filePath, fileExtension);
        }

        try {
            log.debug("尝试直接文本提取，文件: {}", filePath);
            TextExtractor.ExtractionResult extractionResult = extractor.extractFromFile(filePath);
            String text = extractionResult.getText();

            // 4. 质量评估
            if (text != null && !text.isEmpty() && qualityEvaluator.isHighQualityText(text)) {
                log.info("直接文本提取成功，质量评估通过，文件: {}", filePath);

                Map<String, Object> metadata = extractionResult.getMetadata();
                if (metadata == null) {
                    metadata = new HashMap<>();
                }
                metadata.put("tableDetected", false);

                return ParsingResult.builder()
                        .text(text)
                        .structuredDocument(null) // 非结构化
                        .metadata(metadata)
                        .parsingMethod("text_extraction")
                        .qualityScore(qualityEvaluator.calculateQualityScore(text))
                        .build();
            } else {
                log.info("直接文本提取质量不足，降级到OCR，文件: {}", filePath);
                return parseWithOcr(filePath, fileExtension);
            }
        } catch (Exception e) {
            log.warn("直接文本提取失败，降级到OCR，文件: {}, 错误: {}", filePath, e.getMessage());
            return parseWithOcr(filePath, fileExtension);
        }
    }

    /**
     * 解析文档（从URL）。
     */
    public ParsingResult parseFromUrl(String url, String fileExtension) throws IOException {
        if (!enableHybridParser || forceOcr) {
            return parseWithOcrFromUrl(url, fileExtension);
        }

        TextExtractor extractor = findExtractor(fileExtension);
        if (extractor == null) {
            return parseWithOcrFromUrl(url, fileExtension);
        }

        try {
            TextExtractor.ExtractionResult extractionResult = extractor.extractFromUrl(url);
            String text = extractionResult.getText();

            if (text != null && !text.isEmpty() && qualityEvaluator.isHighQualityText(text)) {
                return ParsingResult.builder()
                        .text(text)
                        .structuredDocument(null)
                        .metadata(extractionResult.getMetadata())
                        .parsingMethod("text_extraction")
                        .qualityScore(qualityEvaluator.calculateQualityScore(text))
                        .build();
            } else {
                return parseWithOcrFromUrl(url, fileExtension);
            }
        } catch (Exception e) {
            log.warn("直接文本提取失败，降级到OCR，URL: {}", url, e);
            return parseWithOcrFromUrl(url, fileExtension);
        }
    }

    // 支持的图片格式
    private static final java.util.Set<String> SUPPORTED_IMAGE_FORMATS = java.util.Set.of(
            "png", "jpg", "jpeg", "tiff", "tif", "bmp", "webp", "gif"
    );

    /**
     * 使用OCR解析（支持PDF和图片格式，并行处理和后处理）。
     * <p>
     * 支持增量解析：
     * - 缓存已成功解析的页面
     * - 重试时只处理失败的页面
     * - 合并缓存和新解析的结果
     */
    private ParsingResult parseWithOcr(String filePath, String fileExtension) throws IOException {
        // 检查是否是图片格式
        if (SUPPORTED_IMAGE_FORMATS.contains(fileExtension.toLowerCase())) {
            return parseImageWithOcr(filePath);
        }

        // PDF格式
        if (!"pdf".equalsIgnoreCase(fileExtension)) {
            throw new IOException("OCR解析目前仅支持PDF和图片格式（PNG, JPEG, TIFF, BMP, WebP, GIF）");
        }

        try {
            log.info("开始OCR解析，文件: {}", filePath);
            long startTime = System.currentTimeMillis();

            // 1. 将PDF渲染为图像
            List<byte[]> pageImages = pdfRenderService.renderPdfToImages(filePath);
            if (pageImages.isEmpty()) {
                throw new IOException("PDF渲染失败，未生成图像");
            }

            int totalPages = pageImages.size();
            log.debug("PDF渲染完成，页数: {}", totalPages);

            // 2. 检查缓存，确定需要处理的页面
            String documentKey = null;
            List<Integer> pagesToProcess;
            boolean usingCache = false;

            if (enableCache) {
                File file = new File(filePath);
                documentKey = parsingResultCache.generateDocumentKey(
                        filePath, file.length(), file.lastModified());

                // 获取或创建解析状态
                ParsingResultCache.DocumentParsingState state =
                        parsingResultCache.getOrCreateState(documentKey, totalPages);

                // 检查是否有缓存的成功页面
                if (state.getSuccessCount() > 0) {
                    usingCache = true;
                    pagesToProcess = parsingResultCache.getPendingPageNumbers(documentKey, totalPages);
                    log.info("发现解析缓存: 已成功 {}/{} 页，需处理 {} 页",
                            state.getSuccessCount(), totalPages, pagesToProcess.size());

                    // 如果所有页面都已成功，直接返回缓存结果
                    if (pagesToProcess.isEmpty()) {
                        log.info("所有页面已缓存，直接返回缓存结果");
                        return buildResultFromCache(documentKey, filePath, startTime);
                    }
                } else {
                    // 没有缓存，处理所有页面
                    pagesToProcess = IntStream.rangeClosed(1, totalPages)
                            .boxed().collect(Collectors.toList());
                }
            } else {
                // 缓存禁用，处理所有页面
                pagesToProcess = IntStream.rangeClosed(1, totalPages)
                        .boxed().collect(Collectors.toList());
            }

            log.info("开始处理 {} 个页面（共 {} 页）", pagesToProcess.size(), totalPages);

            // 3. 并行OCR识别（只处理需要的页面）
            Map<Integer, StructuredDocument.Page> newPages =
                    parallelOcrRecognizeWithCache(pageImages, pagesToProcess, documentKey);

            long ocrTime = System.currentTimeMillis() - startTime;
            log.debug("OCR识别完成，耗时: {}ms，新解析: {} 页", ocrTime, newPages.size());

            // 4. 合并缓存和新解析的结果
            List<StructuredDocument.Page> allPages;
            if (usingCache && documentKey != null) {
                allPages = parsingResultCache.mergePages(documentKey, newPages);
                log.debug("合并缓存和新结果，总页数: {}", allPages.size());
            } else {
                allPages = new ArrayList<>(newPages.values());
            }

            // 5. 检查是否有失败的页面
            int failedCount = totalPages - allPages.size();
            if (failedCount > 0 && enableCache && documentKey != null) {
                List<Integer> failedPages = parsingResultCache.getFailedPageNumbers(documentKey);
                log.warn("有 {} 页解析失败，可重新上传文档进行重试，失败页码: {}", failedCount, failedPages);
            }

            // 6. 构建结构化文档
            StructuredDocument structuredDocument = StructuredDocument.builder()
                    .pages(allPages)
                    .modelInfo(ocrService.getServiceName())
                    .processingTimeMs(ocrTime)
                    .build();

            // 7. 后处理：跨页表格合并
            if (enableTableMerge && allPages.size() > 1) {
                structuredDocument = tableMergeService.processDocument(structuredDocument);
                log.debug("跨页表格合并完成");
            }

            // 8. 生成最终文本（应用置信度阈值）
            String finalText = structuredDocument.toMarkdown(confidenceThreshold);

            // 9. 计算质量分数和压缩统计
            double qualityScore = calculateOcrQualityScore(structuredDocument);

            // 10. 计算压缩比统计（技术指南核心指标）
            String originalText = pdfBoxTextExtractor.extractFromFile(filePath).getText();
            OcrEvaluationService.CompressionMetrics compressionMetrics =
                    evaluationService.calculateCompressionRatio(originalText, structuredDocument);

            long totalTime = System.currentTimeMillis() - startTime;

            // 11. 记录统计信息
            StructuredDocument.DocumentStats stats = structuredDocument.getStats();
            log.info("OCR解析完成: 页数={}/{}, 元素数={}, 表格数={}, 平均置信度={}, " +
                            "压缩比={}x, 处理时间={}ms, 使用缓存={}",
                    stats.getPageCount(), totalPages, stats.getTotalElements(), stats.getTableCount(),
                    stats.getAverageConfidence(), compressionMetrics.getCompressionRatio(),
                    totalTime, usingCache);

            // 12. 构建元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("pageCount", stats.getPageCount());
            metadata.put("totalPages", totalPages);
            metadata.put("elementCount", stats.getTotalElements());
            metadata.put("tableCount", stats.getTableCount());
            metadata.put("averageConfidence", stats.getAverageConfidence());
            metadata.put("compressionRatio", compressionMetrics.getCompressionRatio());
            metadata.put("originalTokens", compressionMetrics.getOriginalTokens());
            metadata.put("compressedTokens", compressionMetrics.getCompressedTokens());
            metadata.put("processingTimeMs", totalTime);
            metadata.put("usedCache", usingCache);
            metadata.put("failedPages", failedCount);

            if (enableCache && documentKey != null) {
                metadata.put("documentKey", documentKey);
                metadata.put("cacheProgress", parsingResultCache.getProgressSummary(documentKey));
            }

            // 检查是否完全成功
            if (failedCount > 0) {
                metadata.put("parsingStatus", "PARTIAL");
                metadata.put("failedPageNumbers", parsingResultCache.getFailedPageNumbers(documentKey));
            } else {
                metadata.put("parsingStatus", "COMPLETE");
            }

            return ParsingResult.builder()
                    .text(finalText)
                    .structuredDocument(structuredDocument)
                    .metadata(metadata)
                    .parsingMethod("ocr")
                    .qualityScore(qualityScore)
                    .build();

        } catch (Exception e) {
            log.error("OCR解析失败，文件: {}", filePath, e);
            throw new IOException("OCR解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从缓存构建解析结果。
     */
    private ParsingResult buildResultFromCache(String documentKey, String filePath, long startTime)
            throws IOException {
        List<StructuredDocument.Page> pages = parsingResultCache.getSuccessfulPages(documentKey);

        StructuredDocument structuredDocument = StructuredDocument.builder()
                .pages(pages)
                .modelInfo(ocrService.getServiceName() + " (cached)")
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();

        // 后处理
        if (enableTableMerge && pages.size() > 1) {
            structuredDocument = tableMergeService.processDocument(structuredDocument);
        }

        String finalText = structuredDocument.toMarkdown(confidenceThreshold);
        double qualityScore = calculateOcrQualityScore(structuredDocument);

        String originalText = pdfBoxTextExtractor.extractFromFile(filePath).getText();
        OcrEvaluationService.CompressionMetrics compressionMetrics =
                evaluationService.calculateCompressionRatio(originalText, structuredDocument);

        StructuredDocument.DocumentStats stats = structuredDocument.getStats();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("pageCount", stats.getPageCount());
        metadata.put("totalPages", stats.getPageCount());
        metadata.put("elementCount", stats.getTotalElements());
        metadata.put("tableCount", stats.getTableCount());
        metadata.put("averageConfidence", stats.getAverageConfidence());
        metadata.put("compressionRatio", compressionMetrics.getCompressionRatio());
        metadata.put("processingTimeMs", System.currentTimeMillis() - startTime);
        metadata.put("usedCache", true);
        metadata.put("fromCache", true);
        metadata.put("parsingStatus", "COMPLETE");
        metadata.put("documentKey", documentKey);

        log.info("从缓存返回解析结果: 页数={}", stats.getPageCount());

        return ParsingResult.builder()
                .text(finalText)
                .structuredDocument(structuredDocument)
                .metadata(metadata)
                .parsingMethod("ocr_cached")
                .qualityScore(qualityScore)
                .build();
    }

    /**
     * 并行OCR识别。
     * <p>
     * 每个页面独立处理，确保：
     * 1. 一个页面的失败不影响其他页面
     * 2. 页面结果按正确顺序排列
     * 3. 详细记录每个页面的处理状态
     */
    private List<StructuredDocument.Page> parallelOcrRecognize(List<byte[]> pageImages) {
        int pageCount = pageImages.size();

        // 如果页面数小于阈值，使用串行处理
        if (pageCount <= 2 || parallelPages <= 1) {
            return serialOcrRecognize(pageImages);
        }

        log.info("开始并行OCR处理，总页数: {}, 并行度: {}", pageCount, parallelPages);

        // 使用ConcurrentHashMap存储结果，保证线程安全
        java.util.concurrent.ConcurrentHashMap<Integer, StructuredDocument.Page> resultMap =
                new java.util.concurrent.ConcurrentHashMap<>();
        java.util.concurrent.ConcurrentHashMap<Integer, String> errorMap =
                new java.util.concurrent.ConcurrentHashMap<>();

        // 创建并行任务
        java.util.concurrent.atomic.AtomicInteger completedCount = new java.util.concurrent.atomic.AtomicInteger(0);
        List<CompletableFuture<Void>> futures = IntStream.range(0, pageCount)
                .mapToObj(i -> {
                    final int pageNo = i + 1;
                    return CompletableFuture.runAsync(() -> {
                        try {
                            log.debug("开始处理第 {}/{} 页", pageNo, pageCount);
                            long startTime = System.currentTimeMillis();

                            StructuredDocument.Page page = recognizeSinglePage(pageImages.get(i), pageNo);

                            if (page != null) {
                                resultMap.put(pageNo, page);
                                int completed = completedCount.incrementAndGet();
                                long duration = System.currentTimeMillis() - startTime;
                                log.debug("第 {}/{} 页处理成功，耗时: {}ms，进度: {}/{}",
                                        pageNo, pageCount, duration, completed, pageCount);

                                // 每完成10%或每10页输出一次进度
                                if (completed % Math.max(1, pageCount / 10) == 0 || completed % 10 == 0) {
                                    log.info("OCR处理进度: {}/{} 页 ({}%)", completed, pageCount,
                                            (completed * 100 / pageCount));
                                }
                            } else {
                                errorMap.put(pageNo, "OCR返回空结果");
                                completedCount.incrementAndGet();
                                log.warn("第 {}/{} 页OCR返回空结果", pageNo, pageCount);
                            }
                        } catch (Exception e) {
                            errorMap.put(pageNo, e.getMessage());
                            completedCount.incrementAndGet();
                            log.error("第 {}/{} 页OCR识别失败: {}", pageNo, pageCount, e.getMessage());
                        }
                    }, executorService);
                })
                .toList();

        // 等待所有任务完成
        // 动态计算超时时间：根据页数和并行度计算
        // 公式：总超时 = (页数 / 并行度) * 单页超时 + 缓冲时间
        int effectiveParallelism = Math.min(parallelPages, pageCount);
        long calculatedTimeout = (long) Math.ceil((double) pageCount / effectiveParallelism) * pageTimeoutSeconds + 60; // 额外60秒缓冲
        long totalTimeout = Math.min(calculatedTimeout, maxTotalTimeoutSeconds); // 不超过最大超时时间

        log.debug("并行OCR超时设置: 页数={}, 并行度={}, 单页超时={}s, 计算总超时={}s, 实际使用={}s",
                pageCount, effectiveParallelism, pageTimeoutSeconds, calculatedTimeout, totalTimeout);

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(totalTimeout, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("等待OCR并行任务超时: 总超时时间={}秒，已处理页数={}/{}",
                    totalTimeout, resultMap.size(), pageCount);
            // 即使超时，也继续收集已完成的结果
        } catch (Exception e) {
            log.error("等待OCR并行任务被中断", e);
        }

        // 按页码顺序收集结果（确保顺序正确）
        List<StructuredDocument.Page> pages = new ArrayList<>();
        int missingPages = 0;
        for (int pageNo = 1; pageNo <= pageCount; pageNo++) {
            StructuredDocument.Page page = resultMap.get(pageNo);
            if (page != null) {
                pages.add(page);
            } else {
                missingPages++;
                log.warn("第 {} 页处理失败或超时，将在结果中跳过", pageNo);
            }
        }

        // 记录处理结果统计
        int successCount = resultMap.size();
        int errorCount = errorMap.size();
        log.info("并行OCR处理完成，成功: {}/{} 页，失败: {} 页，缺失: {} 页",
                successCount, pageCount, errorCount, missingPages);

        if (!errorMap.isEmpty()) {
            log.warn("失败页面详情: {}", errorMap);
        }

        if (missingPages > 0 && missingPages != errorCount) {
            log.warn("部分页面可能因超时未完成处理，建议增加超时时间或减少并行度");
        }

        return pages;
    }

    /**
     * 带缓存和自动重试的并行OCR识别。
     * <p>
     * 功能增强：
     * 1. 只处理指定的页面（支持增量解析）
     * 2. 动态调整超时时间
     * 3. 自动重试失败的页面（最多重试 maxPageRetries 次）
     * 4. 记录成功/失败到缓存
     */
    private Map<Integer, StructuredDocument.Page> parallelOcrRecognizeWithCache(
            List<byte[]> pageImages,
            List<Integer> pagesToProcess,
            String documentKey) {

        int totalPages = pageImages.size();
        List<Integer> pendingPages = new ArrayList<>(pagesToProcess);

        // 重置动态超时计算器的窗口（新文档开始）
        if (enableDynamicTimeout) {
            timeoutCalculator.resetWindowStats();
        }

        // 最终结果存储
        ConcurrentHashMap<Integer, StructuredDocument.Page> finalResultMap = new ConcurrentHashMap<>();

        // 执行多轮处理（首次 + 重试）
        for (int round = 0; round <= maxPageRetries && !pendingPages.isEmpty(); round++) {
            String roundLabel = round == 0 ? "首次处理" : "第" + round + "次重试";
            int processCount = pendingPages.size();

            log.info("开始{}，页数: {}/{}, 并行度: {}", roundLabel, processCount, totalPages, parallelPages);

            // 执行一轮并行处理
            Map<Integer, StructuredDocument.Page> roundResult =
                    executeParallelOcrRound(pageImages, pendingPages, documentKey, round);

            // 合并成功结果
            finalResultMap.putAll(roundResult);

            // 找出失败的页面，准备重试
            List<Integer> failedPages = new ArrayList<>();
            for (Integer pageNo : pendingPages) {
                if (!roundResult.containsKey(pageNo)) {
                    failedPages.add(pageNo);
                }
            }

            if (failedPages.isEmpty()) {
                log.info("{}全部成功，共 {} 页", roundLabel, processCount);
                break;
            } else {
                log.warn("{}后仍有 {} 页失败: {}", roundLabel, failedPages.size(), failedPages);
                pendingPages = failedPages;

                // 如果还有重试机会，等待一小段时间再重试
                if (round < maxPageRetries && !pendingPages.isEmpty()) {
                    int waitSeconds = (round + 1) * 2; // 递增等待时间
                    log.info("等待 {} 秒后进行第 {} 次重试...", waitSeconds, round + 1);
                    try {
                        Thread.sleep(waitSeconds * 1000L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // 最终统计
        int successCount = finalResultMap.size();
        int failedCount = pagesToProcess.size() - successCount;
        log.info("OCR处理最终结果: 成功 {}/{} 页，失败 {} 页（已重试 {} 次）",
                successCount, pagesToProcess.size(), failedCount, Math.min(maxPageRetries, failedCount > 0 ? maxPageRetries : 0));

        // 输出动态超时统计
        if (enableDynamicTimeout && successCount > 0) {
            log.debug("动态超时统计: {}", timeoutCalculator.getStatsSummary());
        }

        return finalResultMap;
    }

    /**
     * 执行一轮并行OCR处理。
     */
    private Map<Integer, StructuredDocument.Page> executeParallelOcrRound(
            List<byte[]> pageImages,
            List<Integer> pagesToProcess,
            String documentKey,
            int retryRound) {

        int totalPages = pageImages.size();
        int processCount = pagesToProcess.size();

        // 如果处理页面数小于阈值，使用串行处理
        if (processCount <= 2 || parallelPages <= 1) {
            return serialOcrRecognizeRound(pageImages, pagesToProcess, documentKey, retryRound);
        }

        // 结果存储
        ConcurrentHashMap<Integer, StructuredDocument.Page> resultMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, String> errorMap = new ConcurrentHashMap<>();
        java.util.concurrent.atomic.AtomicInteger completedCount = new java.util.concurrent.atomic.AtomicInteger(0);

        // 创建并行任务
        List<CompletableFuture<Void>> futures = pagesToProcess.stream()
                .map(pageNo -> CompletableFuture.runAsync(() -> {
                    try {
                        log.debug("处理第 {} 页（轮次: {}）", pageNo, retryRound);
                        long startTime = System.currentTimeMillis();

                        // 执行OCR识别
                        StructuredDocument.Page page = recognizeSinglePage(pageImages.get(pageNo - 1), pageNo);

                        long duration = System.currentTimeMillis() - startTime;

                        if (page != null) {
                            resultMap.put(pageNo, page);
                            int completed = completedCount.incrementAndGet();

                            // 记录耗时用于动态超时计算
                            if (enableDynamicTimeout) {
                                timeoutCalculator.recordDuration(duration);
                            }

                            // 记录到缓存
                            if (enableCache && documentKey != null) {
                                parsingResultCache.recordPageSuccess(documentKey, pageNo, page);
                            }

                            log.debug("第 {} 页处理成功，耗时: {}ms，进度: {}/{}",
                                    pageNo, duration, completed, processCount);

                            // 进度日志
                            if (completed % Math.max(1, processCount / 10) == 0 || completed % 5 == 0) {
                                String networkStatus = enableDynamicTimeout ?
                                        timeoutCalculator.getNetworkStatusDescription() : "未知";
                                log.info("OCR处理进度: {}/{} 页 ({}%), 平均耗时: {}ms, 网络: {}",
                                        completed, processCount, (completed * 100 / processCount),
                                        enableDynamicTimeout ? timeoutCalculator.getAverageDuration() : "N/A",
                                        networkStatus);
                            }
                        } else {
                            String error = "OCR返回空结果";
                            errorMap.put(pageNo, error);
                            completedCount.incrementAndGet();

                            // 记录失败到缓存
                            if (enableCache && documentKey != null) {
                                parsingResultCache.recordPageFailure(documentKey, pageNo, error);
                            }

                            log.warn("第 {} 页OCR返回空结果", pageNo);
                        }
                    } catch (Exception e) {
                        String error = e.getMessage();
                        errorMap.put(pageNo, error);
                        completedCount.incrementAndGet();

                        // 记录失败到缓存
                        if (enableCache && documentKey != null) {
                            parsingResultCache.recordPageFailure(documentKey, pageNo, error);
                        }

                        log.error("第 {} 页OCR识别失败: {}", pageNo, error);
                    }
                }, executorService))
                .toList();

        // 计算超时时间（更宽松的计算）
        long totalTimeout = calculateTotalTimeout(processCount);

        log.info("并行OCR超时设置: 处理页数={}, 并行度={}, 总超时={}秒",
                processCount, parallelPages, totalTimeout);

        // 等待所有任务完成
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(totalTimeout, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("等待OCR并行任务超时: 总超时时间={}秒，已处理页数={}/{}",
                    totalTimeout, resultMap.size(), processCount);
            // 超时后继续收集已完成的结果
        } catch (Exception e) {
            log.error("等待OCR并行任务被中断", e);
        }

        // 记录处理结果统计
        int successCount = resultMap.size();
        int failedCount = errorMap.size();
        log.info("本轮并行OCR处理完成，成功: {}/{} 页，失败: {} 页",
                successCount, processCount, failedCount);

        if (!errorMap.isEmpty()) {
            log.warn("本轮失败页面详情: {}", errorMap);
        }

        return resultMap;
    }

    /**
     * 串行OCR处理单轮。
     */
    private Map<Integer, StructuredDocument.Page> serialOcrRecognizeRound(
            List<byte[]> pageImages,
            List<Integer> pagesToProcess,
            String documentKey,
            int retryRound) {

        Map<Integer, StructuredDocument.Page> resultMap = new HashMap<>();
        int processCount = pagesToProcess.size();

        log.info("开始串行OCR处理（轮次: {}），处理页数: {}", retryRound, processCount);

        for (int i = 0; i < processCount; i++) {
            int pageNo = pagesToProcess.get(i);
            try {
                log.debug("处理第 {}/{} 页（页码: {}，轮次: {}）", i + 1, processCount, pageNo, retryRound);
                long startTime = System.currentTimeMillis();

                StructuredDocument.Page page = recognizeSinglePage(pageImages.get(pageNo - 1), pageNo);
                long duration = System.currentTimeMillis() - startTime;

                if (page != null) {
                    resultMap.put(pageNo, page);

                    // 记录耗时
                    if (enableDynamicTimeout) {
                        timeoutCalculator.recordDuration(duration);
                    }

                    // 记录到缓存
                    if (enableCache && documentKey != null) {
                        parsingResultCache.recordPageSuccess(documentKey, pageNo, page);
                    }

                    log.debug("第 {} 页处理成功，耗时: {}ms", pageNo, duration);
                } else {
                    if (enableCache && documentKey != null) {
                        parsingResultCache.recordPageFailure(documentKey, pageNo, "OCR返回空结果");
                    }
                    log.warn("第 {} 页OCR返回空结果", pageNo);
                }
            } catch (Exception e) {
                if (enableCache && documentKey != null) {
                    parsingResultCache.recordPageFailure(documentKey, pageNo, e.getMessage());
                }
                log.error("第 {} 页OCR识别失败: {}", pageNo, e.getMessage());
            }
        }

        return resultMap;
    }

    /**
     * 计算总超时时间。
     */
    /**
     * 计算总超时时间。
     * <p>
     * 使用更宽松的计算方式，考虑：
     * 1. 网络波动
     * 2. 模型响应延迟
     * 3. 重试机制需要的额外时间
     */
    private long calculateTotalTimeout(int pageCount) {
        int effectiveParallelism = Math.min(parallelPages, pageCount);
        int batchCount = (int) Math.ceil((double) pageCount / effectiveParallelism);

        long perPageTimeout;
        if (enableDynamicTimeout && timeoutCalculator.getAverageDuration() > 0) {
            // 使用动态计算的超时时间
            perPageTimeout = timeoutCalculator.calculateTimeout() / 1000; // 转换为秒
        } else {
            // 使用静态配置的超时时间
            perPageTimeout = pageTimeoutSeconds;
        }

        // 使用超时倍数使计算更宽松
        // 公式：批次数 * 单页超时 * 超时倍数 + 缓冲时间
        long calculatedTimeout = (long) (batchCount * perPageTimeout * timeoutMultiplier) + 120; // 120秒缓冲

        log.debug("超时计算: 页数={}, 并行度={}, 批次={}, 单页超时={}s, 倍数={}, 计算超时={}s",
                pageCount, effectiveParallelism, batchCount, perPageTimeout, timeoutMultiplier, calculatedTimeout);

        return Math.min(calculatedTimeout, maxTotalTimeoutSeconds);
    }

    /**
     * 串行OCR识别。
     * 逐页处理，一个页面失败不影响其他页面。
     */
    private List<StructuredDocument.Page> serialOcrRecognize(List<byte[]> pageImages) {
        List<StructuredDocument.Page> pages = new ArrayList<>();
        int pageCount = pageImages.size();
        int successCount = 0;
        int errorCount = 0;

        log.info("开始串行OCR处理，总页数: {}", pageCount);

        for (int i = 0; i < pageCount; i++) {
            int pageNo = i + 1;
            try {
                log.debug("开始处理第 {}/{} 页", pageNo, pageCount);
                long startTime = System.currentTimeMillis();

                StructuredDocument.Page page = recognizeSinglePage(pageImages.get(i), pageNo);

                if (page != null) {
                    pages.add(page);
                    successCount++;
                    log.debug("第 {} 页处理成功，耗时: {}ms，已完成: {}/{}",
                            pageNo, System.currentTimeMillis() - startTime, successCount, pageCount);
                } else {
                    errorCount++;
                    log.warn("第 {} 页OCR返回空结果", pageNo);
                }
            } catch (Exception e) {
                errorCount++;
                log.error("第 {} 页OCR识别失败: {}", pageNo, e.getMessage());
            }
        }

        log.info("串行OCR处理完成，成功: {}/{} 页，失败: {} 页", successCount, pageCount, errorCount);

        return pages;
    }

    /**
     * 识别单个页面。
     * <p>
     * 注意：每次调用都是独立的，页码信息由调用方指定，
     * 不依赖OCR返回的page_no（因为单页OCR总是返回page_no=1）
     */
    private StructuredDocument.Page recognizeSinglePage(byte[] imageBytes, int pageNo)
            throws OcrService.OcrException {

        // 调用OCR服务识别单页
        StructuredDocument pageDoc = ocrService.recognize(imageBytes);

        if (pageDoc == null || pageDoc.getPages() == null || pageDoc.getPages().isEmpty()) {
            log.warn("第 {} 页OCR返回空文档结构", pageNo);
            return null;
        }

        // 取第一页（因为是单页识别，只有一页）
        StructuredDocument.Page recognizedPage = pageDoc.getPages().get(0);

        // 重新设置正确的页码（OCR返回的总是1，需要修正为实际页码）
        return StructuredDocument.Page.builder()
                .pageNo(pageNo)  // 使用实际页码
                .imageSize(recognizedPage.getImageSize())
                .layout(recognizedPage.getLayout() != null ?
                        updateElementIdsForPage(recognizedPage.getLayout(), pageNo) :
                        new ArrayList<>())
                .build();
    }

    /**
     * 更新布局元素的ID，添加页码前缀以确保全局唯一。
     */
    private List<StructuredDocument.LayoutElement> updateElementIdsForPage(
            List<StructuredDocument.LayoutElement> elements, int pageNo) {

        if (elements == null) {
            return new ArrayList<>();
        }

        return elements.stream()
                .map(element -> {
                    // 如果elementId已经有页码前缀，跳过
                    String originalId = element.getElementId();
                    if (originalId != null && !originalId.startsWith("p" + pageNo + "_")) {
                        // 添加页码前缀确保全局唯一
                        element.setElementId("p" + pageNo + "_" + originalId);
                    }

                    // 同样处理parentId
                    String parentId = element.getParentId();
                    if (parentId != null && !parentId.startsWith("p" + pageNo + "_")) {
                        element.setParentId("p" + pageNo + "_" + parentId);
                    }

                    return element;
                })
                .collect(Collectors.toList());
    }

    /**
     * 计算OCR质量分数。
     */
    private double calculateOcrQualityScore(StructuredDocument document) {
        if (document == null || document.getPages() == null || document.getPages().isEmpty()) {
            return 0.0;
        }

        StructuredDocument.DocumentStats stats = document.getStats();

        // 基于元素数量和置信度计算质量分数
        double elementScore = Math.min(1.0, stats.getTotalElements() / 10.0);
        double confidenceScore = stats.getAverageConfidence();

        // 综合分数
        return elementScore * 0.3 + confidenceScore * 0.7;
    }

    private ParsingResult parseWithOcrFromUrl(String url, String fileExtension) throws IOException {
        // URL的OCR解析需要先下载文件，这里简化处理
        throw new IOException("URL的OCR解析暂未实现，请先下载文件");
    }

    /**
     * 直接对图片文件进行OCR解析。
     * 支持PNG、JPEG、TIFF、BMP、WebP、GIF等格式。
     */
    private ParsingResult parseImageWithOcr(String filePath) throws IOException {
        log.info("开始图片OCR解析，文件: {}", filePath);
        long startTime = System.currentTimeMillis();

        try {
            // 1. 读取图片文件
            byte[] imageBytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath));
            log.debug("图片文件读取成功，大小: {} bytes", imageBytes.length);

            // 2. 直接进行OCR识别（单张图片）
            StructuredDocument pageDoc = ocrService.recognize(imageBytes);

            if (pageDoc == null || pageDoc.getPages() == null || pageDoc.getPages().isEmpty()) {
                throw new IOException("OCR识别返回空结果");
            }

            long processingTime = System.currentTimeMillis() - startTime;

            // 3. 设置处理时间
            pageDoc.setProcessingTimeMs(processingTime);
            pageDoc.setModelInfo(ocrService.getServiceName());

            // 4. 生成最终文本（应用置信度阈值）
            String finalText = pageDoc.toMarkdown(confidenceThreshold);

            // 5. 计算质量分数
            double qualityScore = calculateOcrQualityScore(pageDoc);

            // 6. 构建元数据
            StructuredDocument.DocumentStats stats = pageDoc.getStats();
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("pageCount", 1);
            metadata.put("elementCount", stats.getTotalElements());
            metadata.put("tableCount", stats.getTableCount());
            metadata.put("averageConfidence", stats.getAverageConfidence());
            metadata.put("imageSizeBytes", imageBytes.length);
            metadata.put("processingTimeMs", processingTime);

            log.info("图片OCR解析完成: 元素数={}, 表格数={}, 平均置信度={}, 处理时间={}ms",
                    stats.getTotalElements(), stats.getTableCount(),
                    stats.getAverageConfidence(), processingTime);

            return ParsingResult.builder()
                    .text(finalText)
                    .structuredDocument(pageDoc)
                    .metadata(metadata)
                    .parsingMethod("ocr_image")
                    .qualityScore(qualityScore)
                    .build();

        } catch (OcrService.OcrException e) {
            log.error("图片OCR解析失败，文件: {}", filePath, e);
            throw new IOException("图片OCR解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查找合适的文本提取器。
     */
    private TextExtractor findExtractor(String fileExtension) {
        return textExtractors.stream()
                .filter(extractor -> extractor.supports(fileExtension))
                .findFirst()
                .orElse(null);
    }

    /**
     * 解析结果。
     * <p>
     * L2层的核心输出，包含：
     * 1. 原始文本（向后兼容）
     * 2. 结构化文档（完整的版面信息）
     * 3. 为L3层Chunking优化的输出接口
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ParsingResult {
        /**
         * 纯文本内容（向后兼容）
         */
        private String text;

        /**
         * 结构化文档（如果使用OCR）
         */
        private StructuredDocument structuredDocument;

        /**
         * 元数据
         */
        private Map<String, Object> metadata;

        /**
         * 解析方法：text_extraction 或 ocr
         */
        private String parsingMethod;

        /**
         * 质量分数（0-1）
         */
        private Double qualityScore;

        /**
         * 获取最终文本（优先使用结构化文档的Markdown，否则使用纯文本）
         */
        public String getFinalText() {
            if (structuredDocument != null) {
                return structuredDocument.toMarkdown();
            }
            return text != null ? text : "";
        }

        /**
         * 获取为L3层Chunking优化的可分块单元列表。
         * <p>
         * 技术指南L3层核心需求：
         * "每个Chunk本身应该是一个逻辑上自洽、语义完整的单元"
         */
        public List<StructuredDocument.ChunkableUnit> getChunkableUnits() {
            if (structuredDocument != null) {
                return structuredDocument.toChunkableUnits();
            }
            return new ArrayList<>();
        }

        /**
         * 获取表格的混合索引Chunk集合。
         * <p>
         * 技术指南L3层：
         * "面对表格，最佳实践往往是混合索引（Hybrid Indexing）"
         */
        public List<StructuredDocument.TableChunkSet> getTableChunkSets() {
            if (structuredDocument != null) {
                return structuredDocument.getTableChunkSets();
            }
            return new ArrayList<>();
        }

        /**
         * 获取文档统计信息
         */
        public StructuredDocument.DocumentStats getDocumentStats() {
            if (structuredDocument != null) {
                return structuredDocument.getStats();
            }
            return null;
        }

        /**
         * 检查是否包含表格（需要特殊处理）
         */
        public boolean hasTables() {
            if (structuredDocument != null) {
                StructuredDocument.DocumentStats stats = structuredDocument.getStats();
                return stats != null && stats.getTableCount() > 0;
            }
            return false;
        }

        /**
         * 获取语义边界数量（用于判断是否适合语义分块）
         */
        public int getSemanticBoundaryCount() {
            if (structuredDocument != null) {
                return (int) structuredDocument.toChunkableUnits().stream()
                        .filter(u -> Boolean.TRUE.equals(u.getIsSemanticBoundary()))
                        .count();
            }
            return 0;
        }
    }
}

