package cn.cug.sxy.ai.domain.rag.service.parsing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.regex.Pattern;

/**
 * PDF表格检测器。
 * 
 * 解决的核心问题：
 * 直接文本提取（PDFBox）虽然能提取文字，但会完全丢失表格结构。
 * 本检测器在解析前检测PDF是否包含表格，用于决定是否需要使用OCR。
 * 
 * 检测策略：
 * 1. 文本特征检测：检查提取的文本是否有表格特征（制表符、对齐的数字等）
 * 2. 布局分析：分析文字的空间分布，检测是否有网格状排列
 * 3. 线条检测：检测PDF中是否有表格边框线（可选，需要额外处理）
 * 
 * @author jerryhotton
 */
@Slf4j
@Service
public class PdfTableDetector {
    
    // 表格特征的正则模式
    private static final Pattern TAB_PATTERN = Pattern.compile("\\t");
    private static final Pattern MULTIPLE_SPACES_PATTERN = Pattern.compile(" {3,}");
    private static final Pattern NUMBER_COLUMN_PATTERN = Pattern.compile("\\d+\\.?\\d*\\s{2,}\\d+\\.?\\d*");
    private static final Pattern TABLE_HEADER_PATTERN = Pattern.compile(
            "(序号|编号|名称|数量|金额|日期|时间|姓名|部门|合计|总计|小计|No\\.|ID|Name|Date|Amount|Total|Qty)",
            Pattern.CASE_INSENSITIVE
    );
    
    /**
     * 检测PDF文件是否包含表格。
     * 
     * @param filePath PDF文件路径
     * @return 检测结果
     */
    public TableDetectionResult detectTables(String filePath) throws IOException {
        log.debug("开始表格检测，文件: {}", filePath);
        
        File file = new File(filePath);
        try (PDDocument document = Loader.loadPDF(file)) {
            return analyzeDocument(document, filePath);
        }
    }
    
    /**
     * 分析文档检测表格。
     */
    private TableDetectionResult analyzeDocument(PDDocument document, String filePath) throws IOException {
        int pageCount = document.getNumberOfPages();
        List<PageTableInfo> pageInfos = new ArrayList<>();
        int totalTableIndicators = 0;
        boolean hasDefiniteTable = false;
        
        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            PageTableInfo pageInfo = analyzePage(document, pageIndex);
            pageInfos.add(pageInfo);
            totalTableIndicators += pageInfo.getTableIndicatorCount();
            
            if (pageInfo.isHasTable()) {
                hasDefiniteTable = true;
            }
        }
        
        // 计算表格可能性分数
        double tableScore = calculateTableScore(pageInfos, pageCount);
        boolean likelyHasTable = hasDefiniteTable || tableScore > 0.5;
        
        TableDetectionResult result = TableDetectionResult.builder()
                .filePath(filePath)
                .pageCount(pageCount)
                .hasTable(likelyHasTable)
                .tableScore(tableScore)
                .pageInfos(pageInfos)
                .recommendation(likelyHasTable ? ParsingRecommendation.USE_OCR : ParsingRecommendation.USE_TEXT_EXTRACTION)
                .build();
        
        log.info("表格检测完成: 文件={}, 页数={}, 检测到表格={}, 表格分数={}, 建议={}",
                filePath, pageCount, likelyHasTable, tableScore, result.getRecommendation());
        
        return result;
    }
    
    /**
     * 分析单个页面。
     */
    private PageTableInfo analyzePage(PDDocument document, int pageIndex) throws IOException {
        // 1. 提取页面文本
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(pageIndex + 1);
        stripper.setEndPage(pageIndex + 1);
        String pageText = stripper.getText(document);
        
        // 2. 分析文本特征
        int tabCount = countMatches(TAB_PATTERN, pageText);
        int multiSpaceCount = countMatches(MULTIPLE_SPACES_PATTERN, pageText);
        int numberColumnCount = countMatches(NUMBER_COLUMN_PATTERN, pageText);
        int tableHeaderCount = countMatches(TABLE_HEADER_PATTERN, pageText);
        
        // 3. 分析行对齐（检测是否有多行具有相似的空格位置）
        AlignmentAnalysis alignmentAnalysis = analyzeTextAlignment(pageText);
        
        // 4. 综合判断
        int indicatorCount = 0;
        List<String> indicators = new ArrayList<>();
        
        if (tabCount > 2) {
            indicatorCount += tabCount;
            indicators.add("制表符: " + tabCount);
        }
        if (multiSpaceCount > 5) {
            indicatorCount += multiSpaceCount / 2;
            indicators.add("多空格对齐: " + multiSpaceCount);
        }
        if (numberColumnCount > 2) {
            indicatorCount += numberColumnCount * 2;
            indicators.add("数字列: " + numberColumnCount);
        }
        if (tableHeaderCount > 2) {
            indicatorCount += tableHeaderCount * 3;
            indicators.add("表头关键词: " + tableHeaderCount);
        }
        if (alignmentAnalysis.getAlignedRowCount() > 3) {
            indicatorCount += alignmentAnalysis.getAlignedRowCount() * 2;
            indicators.add("对齐行: " + alignmentAnalysis.getAlignedRowCount());
        }
        
        boolean hasTable = indicatorCount > 10 || 
                          (tableHeaderCount >= 2 && (multiSpaceCount > 3 || alignmentAnalysis.getAlignedRowCount() > 2));
        
        return PageTableInfo.builder()
                .pageNo(pageIndex + 1)
                .hasTable(hasTable)
                .tableIndicatorCount(indicatorCount)
                .indicators(indicators)
                .tabCount(tabCount)
                .multiSpaceCount(multiSpaceCount)
                .alignedRowCount(alignmentAnalysis.getAlignedRowCount())
                .build();
    }
    
    /**
     * 分析文本对齐。
     * 检测是否有多行在相同位置有空格（表格特征）。
     */
    private AlignmentAnalysis analyzeTextAlignment(String text) {
        String[] lines = text.split("\n");
        if (lines.length < 3) {
            return new AlignmentAnalysis(0, new ArrayList<>());
        }
        
        // 统计每个位置的空格频率
        Map<Integer, Integer> spacePositionCount = new HashMap<>();
        
        for (String line : lines) {
            if (line.length() < 10) continue;
            
            Set<Integer> lineSpacePositions = new HashSet<>();
            for (int i = 0; i < line.length(); i++) {
                if (line.charAt(i) == ' ' && i > 0 && i < line.length() - 1) {
                    // 检查是否是分隔空格（前后都有非空格字符）
                    if (line.charAt(i - 1) != ' ' || (i + 1 < line.length() && line.charAt(i + 1) != ' ')) {
                        lineSpacePositions.add(i / 5 * 5); // 按5字符分组
                    }
                }
            }
            
            for (Integer pos : lineSpacePositions) {
                spacePositionCount.merge(pos, 1, Integer::sum);
            }
        }
        
        // 找出在多行中出现的空格位置（表格列分隔符）
        List<Integer> alignedPositions = new ArrayList<>();
        int alignedRowCount = 0;
        
        for (Map.Entry<Integer, Integer> entry : spacePositionCount.entrySet()) {
            if (entry.getValue() >= 3) { // 至少3行在相同位置有空格
                alignedPositions.add(entry.getKey());
                alignedRowCount = Math.max(alignedRowCount, entry.getValue());
            }
        }
        
        return new AlignmentAnalysis(alignedRowCount, alignedPositions);
    }
    
    /**
     * 计算表格分数。
     */
    private double calculateTableScore(List<PageTableInfo> pageInfos, int pageCount) {
        if (pageInfos.isEmpty()) {
            return 0.0;
        }
        
        int totalIndicators = pageInfos.stream().mapToInt(PageTableInfo::getTableIndicatorCount).sum();
        int pagesWithTable = (int) pageInfos.stream().filter(PageTableInfo::isHasTable).count();
        
        // 基础分数
        double indicatorScore = Math.min(1.0, totalIndicators / 50.0);
        double pageRatioScore = (double) pagesWithTable / pageCount;
        
        return indicatorScore * 0.6 + pageRatioScore * 0.4;
    }
    
    /**
     * 统计正则匹配数。
     */
    private int countMatches(Pattern pattern, String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) pattern.matcher(text).results().count();
    }
    
    // ==================== 内部类 ====================
    
    @Data
    @AllArgsConstructor
    private static class AlignmentAnalysis {
        private int alignedRowCount;
        private List<Integer> alignedPositions;
    }
    
    /**
     * 表格检测结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableDetectionResult {
        /**
         * 文件路径
         */
        private String filePath;
        
        /**
         * 页面数
         */
        private int pageCount;
        
        /**
         * 是否包含表格
         */
        private boolean hasTable;
        
        /**
         * 表格可能性分数（0-1）
         */
        private double tableScore;
        
        /**
         * 各页面的检测信息
         */
        private List<PageTableInfo> pageInfos;
        
        /**
         * 解析建议
         */
        private ParsingRecommendation recommendation;
        
        /**
         * 获取包含表格的页码列表
         */
        public List<Integer> getTablePages() {
            if (pageInfos == null) return new ArrayList<>();
            return pageInfos.stream()
                    .filter(PageTableInfo::isHasTable)
                    .map(PageTableInfo::getPageNo)
                    .toList();
        }
    }
    
    /**
     * 页面表格信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageTableInfo {
        private int pageNo;
        private boolean hasTable;
        private int tableIndicatorCount;
        private List<String> indicators;
        private int tabCount;
        private int multiSpaceCount;
        private int alignedRowCount;
    }
    
    /**
     * 解析建议
     */
    public enum ParsingRecommendation {
        /**
         * 使用文本提取（无表格或表格简单）
         */
        USE_TEXT_EXTRACTION,
        
        /**
         * 使用OCR（检测到复杂表格）
         */
        USE_OCR,
        
        /**
         * 使用混合模式（部分页面有表格）
         */
        USE_HYBRID
    }
}

