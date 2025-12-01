package cn.cug.sxy.ai.domain.rag.service.parsing;

import cn.cug.sxy.ai.domain.rag.model.parsing.StructuredDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OCR评估服务。
 * 提供字符准确率、TEDS（表格结构准确率）等评估指标。
 * 
 * 技术指南核心评估体系：
 * 1. 字符/词级别准确率 - 使用编辑距离（Levenshtein Distance）
 * 2. 表格结构准确率（TEDS）- Tree-Edit-Distance-based Similarity
 * 3. Token压缩比 - 视觉上下文压缩效率
 * 4. 端到端RAG评估 - 最终业务指标
 * 
 * @author jerryhotton
 */
@Slf4j
@Service
public class OcrEvaluationService {
    
    // Markdown表格行模式
    private static final Pattern TABLE_ROW_PATTERN = Pattern.compile("^\\|(.+)\\|$", Pattern.MULTILINE);
    // Markdown表格分隔行模式
    private static final Pattern TABLE_SEPARATOR_PATTERN = Pattern.compile("^\\|[-:|\\s]+\\|$", Pattern.MULTILINE);
    
    /**
     * 计算字符级别准确率（使用编辑距离）。
     * 
     * @param predicted 预测文本
     * @param groundTruth 真实文本（黄金标准）
     * @return 准确率（0-1）
     */
    public double calculateCharacterAccuracy(String predicted, String groundTruth) {
        if (groundTruth == null || groundTruth.isEmpty()) {
            return predicted == null || predicted.isEmpty() ? 1.0 : 0.0;
        }
        
        if (predicted == null || predicted.isEmpty()) {
            return 0.0;
        }
        
        // 使用Levenshtein距离
        int distance = levenshteinDistance(predicted, groundTruth);
        int maxLength = Math.max(predicted.length(), groundTruth.length());
        
        return maxLength > 0 ? 1.0 - (double) distance / maxLength : 1.0;
    }
    
    /**
     * 计算词级别准确率（考虑词序）。
     */
    public double calculateWordAccuracy(String predicted, String groundTruth) {
        if (groundTruth == null || groundTruth.isEmpty()) {
            return predicted == null || predicted.isEmpty() ? 1.0 : 0.0;
        }
        
        if (predicted == null || predicted.isEmpty()) {
            return 0.0;
        }
        
        String[] predictedWords = normalizeText(predicted).split("\\s+");
        String[] groundTruthWords = normalizeText(groundTruth).split("\\s+");
        
        // 使用词级别的编辑距离
        int distance = wordLevenshteinDistance(predictedWords, groundTruthWords);
        int maxWords = Math.max(predictedWords.length, groundTruthWords.length);
        
        return maxWords > 0 ? 1.0 - (double) distance / maxWords : 1.0;
    }
    
    /**
     * 计算TEDS（Tree-Edit-Distance-based Similarity）分数。
     * 用于评估表格结构准确率。
     * 
     * TEDS算法核心思想：
     * 1. 将表格解析为树结构（根节点 -> 行节点 -> 单元格节点）
     * 2. 计算树编辑距离（插入、删除、重命名节点的最小操作数）
     * 3. 归一化为相似度分数
     * 
     * @param predictedTable 预测的表格结构（Markdown格式）
     * @param groundTruthTable 真实的表格结构（Markdown格式）
     * @return TEDS分数（0-1）
     */
    public double calculateTEDS(String predictedTable, String groundTruthTable) {
        if (groundTruthTable == null || groundTruthTable.isEmpty()) {
            return predictedTable == null || predictedTable.isEmpty() ? 1.0 : 0.0;
        }
        
        if (predictedTable == null || predictedTable.isEmpty()) {
            return 0.0;
        }
        
        // 1. 将Markdown表格解析为树结构
        TableTree predictedTree = parseMarkdownTable(predictedTable);
        TableTree groundTruthTree = parseMarkdownTable(groundTruthTable);
        
        // 2. 计算树编辑距离
        int editDistance = calculateTreeEditDistance(predictedTree, groundTruthTree);
        
        // 3. 计算归一化的TEDS分数
        // TEDS = 1 - (edit_distance / max(|T1|, |T2|))
        // 其中|T|是树的节点总数
        int maxNodes = Math.max(predictedTree.getNodeCount(), groundTruthTree.getNodeCount());
        
        if (maxNodes == 0) {
            return 1.0;
        }
        
        double teds = 1.0 - (double) editDistance / maxNodes;
        return Math.max(0.0, Math.min(1.0, teds)); // 确保在[0,1]范围内
    }
    
    /**
     * 计算Token压缩比。
     * 这是技术指南强调的核心指标："从O(字符数)转变为O(视觉复杂性)"
     * 
     * @param originalText 原始文本
     * @param compressedDocument 压缩后的结构化文档
     * @return 压缩比（原始Token数 / 压缩后Token数）
     */
    public CompressionMetrics calculateCompressionRatio(String originalText, 
                                                         StructuredDocument compressedDocument) {
        // 估算原始文本的Token数（简化：每4个字符约1个Token）
        int originalTokens = estimateTokenCount(originalText);
        
        // 估算压缩后的Token数（使用Markdown输出）
        String compressedText = compressedDocument != null ? compressedDocument.toMarkdown() : "";
        int compressedTokens = estimateTokenCount(compressedText);
        
        // 计算压缩比
        double compressionRatio = compressedTokens > 0 ? 
                (double) originalTokens / compressedTokens : 1.0;
        
        // 计算信息保留率（基于关键词覆盖）
        double informationRetention = calculateInformationRetention(originalText, compressedText);
        
        return CompressionMetrics.builder()
                .originalTokens(originalTokens)
                .compressedTokens(compressedTokens)
                .compressionRatio(compressionRatio)
                .informationRetention(informationRetention)
                .effectiveCompressionScore(compressionRatio * informationRetention)
                .build();
    }
    
    /**
     * 综合评估结果。
     */
    public EvaluationResult evaluate(String predicted, String groundTruth, 
                                     String predictedTable, String groundTruthTable) {
        double charAccuracy = calculateCharacterAccuracy(predicted, groundTruth);
        double wordAccuracy = calculateWordAccuracy(predicted, groundTruth);
        double teds = predictedTable != null && groundTruthTable != null ?
                calculateTEDS(predictedTable, groundTruthTable) : 0.0;
        
        // 综合分数：字符准确率40% + 词准确率30% + TEDS 30%
        double overallScore = charAccuracy * 0.4 + wordAccuracy * 0.3 + teds * 0.3;
        
        return EvaluationResult.builder()
                .characterAccuracy(charAccuracy)
                .wordAccuracy(wordAccuracy)
                .tedsScore(teds)
                .overallScore(overallScore)
                .build();
    }
    
    /**
     * 完整评估（包含压缩比）。
     */
    public FullEvaluationResult evaluateFull(String predicted, String groundTruth,
                                              String predictedTable, String groundTruthTable,
                                              String originalText, StructuredDocument compressedDocument) {
        EvaluationResult basicResult = evaluate(predicted, groundTruth, predictedTable, groundTruthTable);
        CompressionMetrics compressionMetrics = calculateCompressionRatio(originalText, compressedDocument);
        
        return FullEvaluationResult.builder()
                .characterAccuracy(basicResult.getCharacterAccuracy())
                .wordAccuracy(basicResult.getWordAccuracy())
                .tedsScore(basicResult.getTedsScore())
                .overallScore(basicResult.getOverallScore())
                .compressionMetrics(compressionMetrics)
                .build();
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 解析Markdown表格为树结构。
     */
    private TableTree parseMarkdownTable(String markdown) {
        TableTree tree = new TableTree();
        
        if (markdown == null || markdown.isEmpty()) {
            return tree;
        }
        
        String[] lines = markdown.split("\n");
        boolean headerParsed = false;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // 跳过分隔行（如 |---|---|）
            if (TABLE_SEPARATOR_PATTERN.matcher(line).matches()) {
                continue;
            }
            
            // 解析表格行
            if (line.startsWith("|") && line.endsWith("|")) {
                String[] cells = line.substring(1, line.length() - 1).split("\\|");
                TableRow row = new TableRow();
                row.setHeader(!headerParsed);
                headerParsed = true;
                
                for (String cell : cells) {
                    row.getCells().add(cell.trim());
                }
                tree.getRows().add(row);
            }
        }
        
        return tree;
    }
    
    /**
     * 计算树编辑距离（Zhang-Shasha算法简化版）。
     */
    private int calculateTreeEditDistance(TableTree tree1, TableTree tree2) {
        int rows1 = tree1.getRows().size();
        int rows2 = tree2.getRows().size();
        
        // 创建DP表
        int[][] dp = new int[rows1 + 1][rows2 + 1];
        
        // 初始化
        for (int i = 0; i <= rows1; i++) {
            dp[i][0] = i; // 删除所有行
        }
        for (int j = 0; j <= rows2; j++) {
            dp[0][j] = j; // 插入所有行
        }
        
        // 动态规划
        for (int i = 1; i <= rows1; i++) {
            for (int j = 1; j <= rows2; j++) {
                TableRow row1 = tree1.getRows().get(i - 1);
                TableRow row2 = tree2.getRows().get(j - 1);
                
                // 计算行级别的编辑代价
                int rowEditCost = calculateRowEditCost(row1, row2);
                
                dp[i][j] = Math.min(Math.min(
                        dp[i - 1][j] + 1,           // 删除行
                        dp[i][j - 1] + 1),          // 插入行
                        dp[i - 1][j - 1] + rowEditCost); // 替换/编辑行
            }
        }
        
        return dp[rows1][rows2];
    }
    
    /**
     * 计算行编辑代价。
     */
    private int calculateRowEditCost(TableRow row1, TableRow row2) {
        List<String> cells1 = row1.getCells();
        List<String> cells2 = row2.getCells();
        
        int cols1 = cells1.size();
        int cols2 = cells2.size();
        
        // 列数差异代价
        int colDiffCost = Math.abs(cols1 - cols2);
        
        // 内容差异代价
        int contentCost = 0;
        int minCols = Math.min(cols1, cols2);
        for (int i = 0; i < minCols; i++) {
            if (!normalizeCell(cells1.get(i)).equals(normalizeCell(cells2.get(i)))) {
                contentCost++;
            }
        }
        
        // 如果行完全相同，代价为0
        if (colDiffCost == 0 && contentCost == 0) {
            return 0;
        }
        
        // 归一化代价（0或1）
        return 1;
    }
    
    /**
     * 归一化单元格内容。
     */
    private String normalizeCell(String cell) {
        if (cell == null) return "";
        // 移除多余空格、特殊格式
        return cell.trim()
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1") // 移除粗体
                .replaceAll("\\s+", " ")
                .toLowerCase();
    }
    
    /**
     * 归一化文本。
     */
    private String normalizeText(String text) {
        if (text == null) return "";
        return text.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase();
    }
    
    /**
     * Levenshtein距离（字符级别编辑距离）算法。
     */
    private int levenshteinDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        
        // 空间优化：只保留两行
        int[] prev = new int[n + 1];
        int[] curr = new int[n + 1];
        
        for (int j = 0; j <= n; j++) {
            prev[j] = j;
        }
        
        for (int i = 1; i <= m; i++) {
            curr[0] = i;
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    curr[j] = prev[j - 1];
                } else {
                    curr[j] = Math.min(Math.min(
                            prev[j] + 1,      // 删除
                            curr[j - 1] + 1), // 插入
                            prev[j - 1] + 1); // 替换
                }
            }
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }
        
        return prev[n];
    }
    
    /**
     * 词级别的编辑距离。
     */
    private int wordLevenshteinDistance(String[] words1, String[] words2) {
        int m = words1.length;
        int n = words2.length;
        int[][] dp = new int[m + 1][n + 1];
        
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (words1[i - 1].equalsIgnoreCase(words2[j - 1])) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(Math.min(
                            dp[i - 1][j] + 1,
                            dp[i][j - 1] + 1),
                            dp[i - 1][j - 1] + 1);
                }
            }
        }
        
        return dp[m][n];
    }
    
    /**
     * 估算Token数量。
     * 简化估算：英文约每4字符1个Token，中文约每2字符1个Token
     */
    private int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        int chineseCount = 0;
        int otherCount = 0;
        
        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                chineseCount++;
            } else {
                otherCount++;
            }
        }
        
        // 中文：约每1.5字符1个Token，其他：约每4字符1个Token
        return (int) Math.ceil(chineseCount / 1.5) + (int) Math.ceil(otherCount / 4.0);
    }
    
    /**
     * 计算信息保留率。
     */
    private double calculateInformationRetention(String original, String compressed) {
        if (original == null || original.isEmpty()) {
            return 1.0;
        }
        if (compressed == null || compressed.isEmpty()) {
            return 0.0;
        }
        
        // 提取关键词（简化：提取所有词）
        String[] originalWords = normalizeText(original).split("\\s+");
        String[] compressedWords = normalizeText(compressed).split("\\s+");
        
        // 计算关键词覆盖率
        int covered = 0;
        for (String word : originalWords) {
            if (word.length() > 2) { // 忽略短词
                for (String cWord : compressedWords) {
                    if (cWord.contains(word) || word.contains(cWord)) {
                        covered++;
                        break;
                    }
                }
            }
        }
        
        long significantWords = java.util.Arrays.stream(originalWords)
                .filter(w -> w.length() > 2)
                .count();
        
        return significantWords > 0 ? (double) covered / significantWords : 1.0;
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 表格树结构。
     */
    @Data
    private static class TableTree {
        private List<TableRow> rows = new ArrayList<>();
        
        public int getNodeCount() {
            int count = 1; // 根节点
            for (TableRow row : rows) {
                count += 1 + row.getCells().size(); // 行节点 + 单元格节点
            }
            return count;
        }
    }
    
    /**
     * 表格行。
     */
    @Data
    private static class TableRow {
        private boolean header;
        private List<String> cells = new ArrayList<>();
    }
    
    /**
     * 评估结果。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluationResult {
        private double characterAccuracy;
        private double wordAccuracy;
        private double tedsScore;
        private double overallScore;
    }
    
    /**
     * 压缩指标。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompressionMetrics {
        /** 原始Token数 */
        private int originalTokens;
        /** 压缩后Token数 */
        private int compressedTokens;
        /** 压缩比（原始/压缩） */
        private double compressionRatio;
        /** 信息保留率 */
        private double informationRetention;
        /** 有效压缩分数（压缩比 × 信息保留率） */
        private double effectiveCompressionScore;
    }
    
    /**
     * 完整评估结果（包含压缩指标）。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FullEvaluationResult {
        private double characterAccuracy;
        private double wordAccuracy;
        private double tedsScore;
        private double overallScore;
        private CompressionMetrics compressionMetrics;
    }
}

