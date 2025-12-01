package cn.cug.sxy.ai.test.domain.parsing;

import cn.cug.sxy.ai.domain.rag.model.parsing.ParsingMode;
import cn.cug.sxy.ai.domain.rag.model.parsing.StructuredDocument;
import cn.cug.sxy.ai.domain.rag.service.parsing.HybridDocumentParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * PDF解析测试。
 * <p>
 * 解析模式说明：
 * - SIMPLE：简单模式，适用于纯文本PDF，速度快、成本低
 * - COMPLEX：复杂模式，适用于含表格/图片的PDF，保留结构
 * - AUTO：自动模式，系统智能检测并选择最佳策略
 * <p>
 * 使用方法：
 * 1. 设置环境变量 DASHSCOPE_API_KEY（复杂模式需要）
 * 2. 准备测试PDF文件
 * 3. 运行测试：
 * - mvn test -Dtest=PdfParsingTest -Dtest.pdf.path=/path/to/test.pdf
 * - mvn test -Dtest=PdfParsingTest -Dtest.pdf.path=/path/to/test.pdf -Dtest.mode=COMPLEX
 *
 * @author jerryhotton
 */
@SpringBootTest
class PdfParsingTest {

    @Autowired
    private HybridDocumentParser parser;

    @Test
    @DisplayName("简单模式 - 纯文本PDF解析")
    void testSimpleModeParsing() throws Exception {
        String pdfPath = findTestPdf();
        assumeTrue(pdfPath != null, "请指定测试PDF路径: -Dtest.pdf.path=/path/to/test.pdf");

        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║           简单模式 - 纯文本PDF解析                        ║");
        System.out.println("║  适用于：纯文字文档、无表格、非扫描件                     ║");
        System.out.println("║  特点：速度快、成本低、不保留版面结构                     ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println("PDF文件: " + pdfPath);
        System.out.println();

        long startTime = System.currentTimeMillis();
        HybridDocumentParser.ParsingResult result = parser.parseFromFile(pdfPath, "pdf", ParsingMode.SIMPLE);
        long duration = System.currentTimeMillis() - startTime;

        printResult(result, duration);
    }

    @Test
    @DisplayName("复杂模式 - 含表格/图片的PDF解析")
    void testComplexModeParsing() throws Exception {
        String pdfPath = findTestPdf();
        assumeTrue(pdfPath != null, "请指定测试PDF路径: -Dtest.pdf.path=/path/to/test.pdf");

        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║           复杂模式 - 含表格/图片的PDF解析                 ║");
        System.out.println("║  适用于：含表格、图表、公式、扫描件                       ║");
        System.out.println("║  特点：保留表格结构、识别公式代码、速度较慢               ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println("PDF文件: " + pdfPath);
        System.out.println();

        long startTime = System.currentTimeMillis();
        HybridDocumentParser.ParsingResult result = parser.parseFromFile(pdfPath, "pdf", ParsingMode.OCR);
        long duration = System.currentTimeMillis() - startTime;

        printResult(result, duration);
    }

    @Test
    @DisplayName("自动模式 - 智能检测PDF类型")
    void testAutoModeParsing() throws Exception {
        String pdfPath = findTestPdf();
        assumeTrue(pdfPath != null, "请指定测试PDF路径: -Dtest.pdf.path=/path/to/test.pdf");

        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║           自动模式 - 智能检测PDF类型                      ║");
        System.out.println("║  系统自动检测文档是否包含表格，智能选择解析策略           ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println("PDF文件: " + pdfPath);
        System.out.println();

        long startTime = System.currentTimeMillis();
        HybridDocumentParser.ParsingResult result = parser.parseFromFile(pdfPath, "pdf", ParsingMode.AUTO);
        long duration = System.currentTimeMillis() - startTime;

        printResult(result, duration);
    }

    @Test
    @DisplayName("根据参数选择模式解析")
    void testParsePdfWithMode() throws Exception {
        String pdfPath = findTestPdf();
        assumeTrue(pdfPath != null, "请指定测试PDF路径: -Dtest.pdf.path=/path/to/test.pdf");

        // 从系统属性读取模式，默认AUTO
        String modeStr = System.getProperty("test.mode", "AUTO");
        ParsingMode mode;
        try {
            mode = ParsingMode.valueOf(modeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("无效的模式: " + modeStr + "，使用默认AUTO模式");
            mode = ParsingMode.AUTO;
        }

        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                    PDF解析测试                            ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println("PDF文件: " + pdfPath);
        System.out.println("解析模式: " + mode.getDisplayName());
        System.out.println("模式说明: " + mode.getDescription());
        System.out.println();

        long startTime = System.currentTimeMillis();
        HybridDocumentParser.ParsingResult result = parser.parseFromFile(pdfPath, "pdf", mode);
        long duration = System.currentTimeMillis() - startTime;

        printResult(result, duration);
    }

    /**
     * 查找测试PDF文件
     */
    private String findTestPdf() {
        String pdfPath = "src/test/resources/pdfs/pdf_test_1.pdf";

        return Files.exists(Paths.get(pdfPath)) ? pdfPath : null;
    }

    /**
     * 打印解析结果
     */
    private void printResult(HybridDocumentParser.ParsingResult result, long duration) {
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│              解析结果                   │");
        System.out.println("├─────────────────────────────────────────┤");
        System.out.println("│ 解析方法: " + padRight(result.getParsingMethod(), 28) + " │");
        System.out.println("│ 质量分数: " + padRight(String.format("%.2f", result.getQualityScore()), 28) + " │");
        System.out.println("│ 处理时间: " + padRight(duration + "ms", 28) + " │");
        System.out.println("└─────────────────────────────────────────┘");

        if (result.getStructuredDocument() != null) {
            StructuredDocument doc = result.getStructuredDocument();
            StructuredDocument.DocumentStats stats = doc.getStats();

            System.out.println();
            System.out.println("┌─────────────────────────────────────────┐");
            System.out.println("│              文档统计                   │");
            System.out.println("├─────────────────────────────────────────┤");
            System.out.println("│ 页数:       " + padRight(String.valueOf(stats.getPageCount()), 26) + " │");
            System.out.println("│ 元素总数:   " + padRight(String.valueOf(stats.getTotalElements()), 26) + " │");
            System.out.println("│ 标题数:     " + padRight(String.valueOf(stats.getTitleCount()), 26) + " │");
            System.out.println("│ 段落数:     " + padRight(String.valueOf(stats.getTextCount()), 26) + " │");
            System.out.println("│ 表格数:     " + padRight(String.valueOf(stats.getTableCount()), 26) + " │");
            System.out.println("│ 平均置信度: " + padRight(String.format("%.2f", stats.getAverageConfidence()), 26) + " │");
            System.out.println("│ 估算Token:  " + padRight(String.valueOf(stats.getEstimatedTokens()), 26) + " │");
            System.out.println("└─────────────────────────────────────────┘");
        }

        System.out.println();
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│           L3层Chunking信息              │");
        System.out.println("├─────────────────────────────────────────┤");
        System.out.println("│ 可分块单元: " + padRight(String.valueOf(result.getChunkableUnits().size()), 26) + " │");
        System.out.println("│ 表格索引集: " + padRight(String.valueOf(result.getTableChunkSets().size()), 26) + " │");
        System.out.println("│ 语义边界数: " + padRight(String.valueOf(result.getSemanticBoundaryCount()), 26) + " │");
        System.out.println("│ 包含表格:   " + padRight(result.hasTables() ? "是" : "否", 26) + " │");
        System.out.println("└─────────────────────────────────────────┘");

        // Markdown预览
        System.out.println();
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│           Markdown预览                  │");
        System.out.println("└─────────────────────────────────────────┘");
        String markdown = result.getFinalText();

        System.out.println(markdown);

        System.out.println();
        System.out.println("═══════════════ 测试完成 ═══════════════");
    }

    private String padRight(String str, int length) {
        if (str.length() >= length) {
            return str.substring(0, length);
        }
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < length) {
            sb.append(" ");
        }
        return sb.toString();
    }
}
