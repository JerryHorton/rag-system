package cn.cug.sxy.ai.domain.rag.service.parsing;

import cn.cug.sxy.ai.domain.rag.model.parsing.StructuredDocument;
import cn.cug.sxy.ai.domain.rag.model.parsing.StructuredDocument.LayoutElement;
import cn.cug.sxy.ai.domain.rag.model.parsing.StructuredDocument.Page;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 跨页表格检测与合并服务。
 * 
 * 技术指南核心策略：
 * "检测到一个表格在页面底部被截断，并且下一页的开头也是一个表格时，
 *  尝试将两者的表头进行比对，如果相似，则进行合并。"
 * 
 * @author jerryhotton
 */
@Slf4j
@Service
public class TableMergeService {
    
    // Markdown表格模式
    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "\\|(.+)\\|\\s*\\n\\|[-:|\\s]+\\|\\s*\\n((?:\\|.+\\|\\s*\\n?)+)",
            Pattern.MULTILINE
    );
    
    // 表格头模式
    private static final Pattern TABLE_HEADER_PATTERN = Pattern.compile("^\\|(.+?)\\|$", Pattern.MULTILINE);
    
    @Value("${rag.parsing.table-merge.enabled:true}")
    private boolean enabled;
    
    @Value("${rag.parsing.table-merge.header-similarity-threshold:0.7}")
    private double headerSimilarityThreshold;
    
    @Value("${rag.parsing.table-merge.bottom-threshold-ratio:0.85}")
    private double bottomThresholdRatio; // 页面底部阈值（相对于页面高度）
    
    @Value("${rag.parsing.table-merge.top-threshold-ratio:0.15}")
    private double topThresholdRatio; // 页面顶部阈值（相对于页面高度）
    
    /**
     * 处理结构化文档，检测并合并跨页表格。
     * 
     * @param document 原始结构化文档
     * @return 处理后的文档（跨页表格已合并）
     */
    public StructuredDocument processDocument(StructuredDocument document) {
        if (!enabled || document == null || document.getPages() == null || document.getPages().size() < 2) {
            return document;
        }
        
        log.debug("开始跨页表格检测，页数: {}", document.getPages().size());
        
        List<Page> processedPages = new ArrayList<>();
        List<TableMergeCandidate> mergeCandidates = new ArrayList<>();
        
        // 第一遍：检测跨页表格候选
        for (int i = 0; i < document.getPages().size() - 1; i++) {
            Page currentPage = document.getPages().get(i);
            Page nextPage = document.getPages().get(i + 1);
            
            TableMergeCandidate candidate = detectCrossPageTable(currentPage, nextPage);
            if (candidate != null) {
                mergeCandidates.add(candidate);
                log.info("检测到跨页表格: 页 {} -> 页 {}, 相似度: {:.2f}",
                        i + 1, i + 2, candidate.getHeaderSimilarity());
            }
        }
        
        // 第二遍：执行合并
        int skipNextPage = -1;
        for (int i = 0; i < document.getPages().size(); i++) {
            if (i == skipNextPage) {
                continue; // 跳过已合并的页面
            }
            
            Page currentPage = document.getPages().get(i);
            
            // 检查是否有需要合并的候选
            TableMergeCandidate candidate = findCandidateForPage(mergeCandidates, i);
            if (candidate != null) {
                Page nextPage = document.getPages().get(i + 1);
                Page mergedPage = mergePages(currentPage, nextPage, candidate);
                processedPages.add(mergedPage);
                skipNextPage = i + 1;
                log.info("已合并页 {} 和页 {} 的跨页表格", i + 1, i + 2);
            } else {
                processedPages.add(currentPage);
            }
        }
        
        // 构建处理后的文档
        return StructuredDocument.builder()
                .pages(processedPages)
                .modelInfo(document.getModelInfo())
                .processingTimeMs(document.getProcessingTimeMs())
                .metadata(document.getMetadata())
                .build();
    }
    
    /**
     * 检测两个相邻页面是否存在跨页表格。
     */
    private TableMergeCandidate detectCrossPageTable(Page currentPage, Page nextPage) {
        if (currentPage.getLayout() == null || nextPage.getLayout() == null) {
            return null;
        }
        
        // 获取页面尺寸
        int pageHeight = currentPage.getImageSize() != null && currentPage.getImageSize().size() > 1
                ? currentPage.getImageSize().get(1) : 1000;
        
        // 查找当前页底部的表格
        LayoutElement bottomTable = findTableAtPosition(currentPage, pageHeight, true);
        if (bottomTable == null) {
            return null;
        }
        
        // 查找下一页顶部的表格
        LayoutElement topTable = findTableAtPosition(nextPage, pageHeight, false);
        if (topTable == null) {
            return null;
        }
        
        // 比较表头相似度
        String bottomHeader = extractTableHeader(bottomTable.getMdText());
        String topHeader = extractTableHeader(topTable.getMdText());
        
        if (bottomHeader == null || topHeader == null) {
            return null;
        }
        
        double similarity = calculateHeaderSimilarity(bottomHeader, topHeader);
        
        if (similarity >= headerSimilarityThreshold) {
            return TableMergeCandidate.builder()
                    .pageIndex(currentPage.getPageNo() - 1)
                    .bottomTableIndex(findElementIndex(currentPage.getLayout(), bottomTable))
                    .topTableIndex(findElementIndex(nextPage.getLayout(), topTable))
                    .headerSimilarity(similarity)
                    .bottomTable(bottomTable)
                    .topTable(topTable)
                    .build();
        }
        
        return null;
    }
    
    /**
     * 在指定位置查找表格。
     * 
     * @param page 页面
     * @param pageHeight 页面高度
     * @param atBottom true=查找底部，false=查找顶部
     */
    private LayoutElement findTableAtPosition(Page page, int pageHeight, boolean atBottom) {
        if (page.getLayout() == null) {
            return null;
        }
        
        double threshold = atBottom ? pageHeight * bottomThresholdRatio : pageHeight * topThresholdRatio;
        
        for (LayoutElement element : page.getLayout()) {
            if (!"table".equalsIgnoreCase(element.getType())) {
                continue;
            }
            
            List<Double> bbox = element.getBbox();
            if (bbox == null || bbox.size() < 4) {
                // 如果没有边界框，使用启发式方法
                // 检查是否是页面的第一个/最后一个表格
                int index = page.getLayout().indexOf(element);
                if (atBottom && index == page.getLayout().size() - 1) {
                    return element;
                }
                if (!atBottom && index == 0) {
                    return element;
                }
                continue;
            }
            
            double elementY = bbox.get(1); // y坐标
            double elementBottom = elementY + (bbox.size() > 3 ? bbox.get(3) : 0); // y + height
            
            if (atBottom && elementBottom >= threshold) {
                return element;
            }
            if (!atBottom && elementY <= threshold) {
                return element;
            }
        }
        
        return null;
    }
    
    /**
     * 提取表格表头。
     */
    private String extractTableHeader(String tableMarkdown) {
        if (tableMarkdown == null || tableMarkdown.isEmpty()) {
            return null;
        }
        
        String[] lines = tableMarkdown.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("|") && line.endsWith("|") && !line.matches("^\\|[-:|\\s]+\\|$")) {
                return line;
            }
        }
        
        return null;
    }
    
    /**
     * 计算表头相似度（使用Jaccard相似度）。
     */
    private double calculateHeaderSimilarity(String header1, String header2) {
        if (header1 == null || header2 == null) {
            return 0.0;
        }
        
        // 提取列名
        String[] cols1 = header1.split("\\|");
        String[] cols2 = header2.split("\\|");
        
        // 清理空字符串
        List<String> list1 = new ArrayList<>();
        List<String> list2 = new ArrayList<>();
        
        for (String col : cols1) {
            String trimmed = col.trim().toLowerCase();
            if (!trimmed.isEmpty()) {
                list1.add(trimmed);
            }
        }
        for (String col : cols2) {
            String trimmed = col.trim().toLowerCase();
            if (!trimmed.isEmpty()) {
                list2.add(trimmed);
            }
        }
        
        // 计算Jaccard相似度
        int intersection = 0;
        for (String s : list1) {
            if (list2.contains(s)) {
                intersection++;
            }
        }
        
        int union = list1.size() + list2.size() - intersection;
        return union > 0 ? (double) intersection / union : 0.0;
    }
    
    /**
     * 查找元素在列表中的索引。
     */
    private int findElementIndex(List<LayoutElement> layout, LayoutElement element) {
        for (int i = 0; i < layout.size(); i++) {
            if (layout.get(i) == element) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * 查找指定页面的合并候选。
     */
    private TableMergeCandidate findCandidateForPage(List<TableMergeCandidate> candidates, int pageIndex) {
        for (TableMergeCandidate candidate : candidates) {
            if (candidate.getPageIndex() == pageIndex) {
                return candidate;
            }
        }
        return null;
    }
    
    /**
     * 合并两个页面（跨页表格）。
     */
    private Page mergePages(Page page1, Page page2, TableMergeCandidate candidate) {
        List<LayoutElement> mergedLayout = new ArrayList<>();
        
        // 复制page1的元素，但替换底部表格
        for (int i = 0; i < page1.getLayout().size(); i++) {
            if (i == candidate.getBottomTableIndex()) {
                // 合并表格
                LayoutElement mergedTable = mergeTables(candidate.getBottomTable(), candidate.getTopTable());
                mergedLayout.add(mergedTable);
            } else {
                mergedLayout.add(page1.getLayout().get(i));
            }
        }
        
        // 复制page2的元素，但跳过顶部表格
        for (int i = 0; i < page2.getLayout().size(); i++) {
            if (i != candidate.getTopTableIndex()) {
                mergedLayout.add(page2.getLayout().get(i));
            }
        }
        
        return Page.builder()
                .pageNo(page1.getPageNo())
                .imageSize(page1.getImageSize())
                .layout(mergedLayout)
                .build();
    }
    
    /**
     * 合并两个表格。
     */
    private LayoutElement mergeTables(LayoutElement table1, LayoutElement table2) {
        String md1 = table1.getMdText() != null ? table1.getMdText() : "";
        String md2 = table2.getMdText() != null ? table2.getMdText() : "";
        
        // 合并Markdown表格
        String mergedMd = mergeMarkdownTables(md1, md2);
        
        // 合并原始文本
        String text1 = table1.getText() != null ? table1.getText() : "";
        String text2 = table2.getText() != null ? table2.getText() : "";
        String mergedText = text1 + "\n" + text2;
        
        // 取较高的置信度
        Double confidence = Math.max(
                table1.getConfidence() != null ? table1.getConfidence() : 0.0,
                table2.getConfidence() != null ? table2.getConfidence() : 0.0
        );
        
        return LayoutElement.builder()
                .type("table")
                .bbox(table1.getBbox()) // 使用第一个表格的bbox
                .text(mergedText)
                .mdText(mergedMd)
                .confidence(confidence)
                .build();
    }
    
    /**
     * 合并两个Markdown表格（保留表头，合并数据行）。
     */
    private String mergeMarkdownTables(String table1, String table2) {
        String[] lines1 = table1.split("\n");
        String[] lines2 = table2.split("\n");
        
        StringBuilder merged = new StringBuilder();
        
        // 添加第一个表格的全部内容
        for (String line : lines1) {
            merged.append(line).append("\n");
        }
        
        // 添加第二个表格的数据行（跳过表头和分隔行）
        boolean headerSkipped = false;
        boolean separatorSkipped = false;
        for (String line : lines2) {
            String trimmed = line.trim();
            
            // 跳过表头
            if (!headerSkipped && trimmed.startsWith("|") && !trimmed.matches("^\\|[-:|\\s]+\\|$")) {
                headerSkipped = true;
                continue;
            }
            
            // 跳过分隔行
            if (!separatorSkipped && trimmed.matches("^\\|[-:|\\s]+\\|$")) {
                separatorSkipped = true;
                continue;
            }
            
            // 添加数据行
            if (!trimmed.isEmpty()) {
                merged.append(line).append("\n");
            }
        }
        
        return merged.toString().trim();
    }
    
    /**
     * 跨页表格合并候选。
     */
    @Data
    @Builder
    private static class TableMergeCandidate {
        /** 当前页索引（0-based） */
        private int pageIndex;
        /** 底部表格在layout中的索引 */
        private int bottomTableIndex;
        /** 顶部表格在layout中的索引 */
        private int topTableIndex;
        /** 表头相似度 */
        private double headerSimilarity;
        /** 底部表格元素 */
        private LayoutElement bottomTable;
        /** 顶部表格元素 */
        private LayoutElement topTable;
    }
}

