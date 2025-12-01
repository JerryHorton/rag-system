package cn.cug.sxy.ai.domain.rag.service.parsing;

import cn.cug.sxy.ai.domain.rag.model.parsing.StructuredDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 端到端RAG解析评估器。
 * 
 * 技术指南核心评估方法：
 * "这是一种'黑盒'评估，直接衡量OCR的改进对最终业务指标的贡献。"
 * 
 * 评估维度（参考RAGAs框架）：
 * 1. 答案相关性（Answer Relevancy）
 * 2. 事实忠实度（Faithfulness）
 * 3. 上下文精确率（Context Precision）
 * 4. 上下文召回率（Context Recall）
 * 
 * @author jerryhotton
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagParsingEvaluator {
    
    private final OcrEvaluationService ocrEvaluationService;
    
    /**
     * 评估解析质量对RAG系统的影响。
     * 
     * @param parseResult 解析结果
     * @param groundTruthText 原始文档的真实文本（用于对比）
     * @param qaDataset 问答数据集（用于端到端评估）
     * @return 评估报告
     */
    public ParsingQualityReport evaluateParsingQuality(
            HybridDocumentParser.ParsingResult parseResult,
            String groundTruthText,
            List<QAPair> qaDataset) {
        
        log.info("开始端到端RAG解析评估...");
        
        // 1. 基础OCR评估
        String parsedText = parseResult.getText();
        OcrEvaluationService.EvaluationResult ocrResult = 
                ocrEvaluationService.evaluate(parsedText, groundTruthText, null, null);
        
        // 2. 结构化文档评估
        StructuredDocumentMetrics structuredMetrics = null;
        if (parseResult.getStructuredDocument() != null) {
            structuredMetrics = evaluateStructuredDocument(parseResult.getStructuredDocument());
        }
        
        // 3. Token压缩比评估
        OcrEvaluationService.CompressionMetrics compressionMetrics = 
                ocrEvaluationService.calculateCompressionRatio(
                        groundTruthText, parseResult.getStructuredDocument());
        
        // 4. Q&A数据集评估（如果提供）
        QAEvaluationMetrics qaMetrics = null;
        if (qaDataset != null && !qaDataset.isEmpty()) {
            qaMetrics = evaluateWithQADataset(parsedText, qaDataset);
        }
        
        // 5. 计算综合分数
        double overallScore = calculateOverallScore(ocrResult, structuredMetrics, 
                compressionMetrics, qaMetrics);
        
        // 6. 生成评估报告
        ParsingQualityReport report = ParsingQualityReport.builder()
                .parsingMethod(parseResult.getParsingMethod())
                .ocrMetrics(ocrResult)
                .structuredMetrics(structuredMetrics)
                .compressionMetrics(compressionMetrics)
                .qaMetrics(qaMetrics)
                .overallScore(overallScore)
                .recommendations(generateRecommendations(ocrResult, structuredMetrics, compressionMetrics))
                .build();
        
        log.info("端到端RAG解析评估完成，综合分数: {:.2f}", overallScore);
        
        return report;
    }
    
    /**
     * 快速评估（不使用Q&A数据集）。
     */
    public ParsingQualityReport quickEvaluate(
            HybridDocumentParser.ParsingResult parseResult,
            String groundTruthText) {
        return evaluateParsingQuality(parseResult, groundTruthText, null);
    }
    
    /**
     * 评估结构化文档质量。
     */
    private StructuredDocumentMetrics evaluateStructuredDocument(StructuredDocument document) {
        StructuredDocument.DocumentStats stats = document.getStats();
        
        // 计算结构完整性分数
        double structureScore = calculateStructureScore(stats);
        
        // 计算元素类型多样性分数
        double diversityScore = calculateDiversityScore(stats);
        
        // 计算置信度分布
        ConfidenceDistribution confidenceDistribution = 
                calculateConfidenceDistribution(document);
        
        return StructuredDocumentMetrics.builder()
                .pageCount(stats.getPageCount())
                .totalElements(stats.getTotalElements())
                .titleCount(stats.getTitleCount())
                .textCount(stats.getTextCount())
                .tableCount(stats.getTableCount())
                .imageCount(stats.getImageCount())
                .averageConfidence(stats.getAverageConfidence())
                .structureScore(structureScore)
                .diversityScore(diversityScore)
                .confidenceDistribution(confidenceDistribution)
                .build();
    }
    
    /**
     * 使用Q&A数据集评估。
     */
    private QAEvaluationMetrics evaluateWithQADataset(String parsedText, List<QAPair> qaDataset) {
        int totalQuestions = qaDataset.size();
        int answerable = 0;
        double totalRelevancy = 0.0;
        
        for (QAPair qa : qaDataset) {
            // 检查答案是否可以从解析文本中找到
            if (containsAnswer(parsedText, qa.getExpectedAnswer())) {
                answerable++;
            }
            
            // 计算相关性分数（简化：基于关键词覆盖）
            double relevancy = calculateRelevancy(parsedText, qa.getQuestion(), qa.getExpectedAnswer());
            totalRelevancy += relevancy;
        }
        
        double answerCoverage = totalQuestions > 0 ? (double) answerable / totalQuestions : 0.0;
        double averageRelevancy = totalQuestions > 0 ? totalRelevancy / totalQuestions : 0.0;
        
        return QAEvaluationMetrics.builder()
                .totalQuestions(totalQuestions)
                .answerableQuestions(answerable)
                .answerCoverage(answerCoverage)
                .averageRelevancy(averageRelevancy)
                .build();
    }
    
    /**
     * 计算结构完整性分数。
     */
    private double calculateStructureScore(StructuredDocument.DocumentStats stats) {
        // 结构完整性：至少有标题、正文
        double score = 0.0;
        
        if (stats.getTitleCount() > 0) score += 0.3;
        if (stats.getTextCount() > 0) score += 0.4;
        if (stats.getTableCount() > 0) score += 0.2;
        if (stats.getImageCount() > 0) score += 0.1;
        
        return Math.min(1.0, score);
    }
    
    /**
     * 计算元素类型多样性分数。
     */
    private double calculateDiversityScore(StructuredDocument.DocumentStats stats) {
        int types = 0;
        if (stats.getTitleCount() > 0) types++;
        if (stats.getTextCount() > 0) types++;
        if (stats.getTableCount() > 0) types++;
        if (stats.getImageCount() > 0) types++;
        if (stats.getOtherCount() > 0) types++;
        
        // 最多5种类型
        return types / 5.0;
    }
    
    /**
     * 计算置信度分布。
     */
    private ConfidenceDistribution calculateConfidenceDistribution(StructuredDocument document) {
        int highConfidence = 0;  // >= 0.9
        int mediumConfidence = 0; // 0.7-0.9
        int lowConfidence = 0;   // < 0.7
        
        if (document.getPages() != null) {
            for (StructuredDocument.Page page : document.getPages()) {
                if (page.getLayout() != null) {
                    for (StructuredDocument.LayoutElement element : page.getLayout()) {
                        Double conf = element.getConfidence();
                        if (conf == null) continue;
                        
                        if (conf >= 0.9) highConfidence++;
                        else if (conf >= 0.7) mediumConfidence++;
                        else lowConfidence++;
                    }
                }
            }
        }
        
        return ConfidenceDistribution.builder()
                .highConfidenceCount(highConfidence)
                .mediumConfidenceCount(mediumConfidence)
                .lowConfidenceCount(lowConfidence)
                .build();
    }
    
    /**
     * 检查文本中是否包含答案。
     */
    private boolean containsAnswer(String text, String answer) {
        if (text == null || answer == null) return false;
        
        String normalizedText = text.toLowerCase().replaceAll("\\s+", " ");
        String normalizedAnswer = answer.toLowerCase().replaceAll("\\s+", " ");
        
        // 检查是否包含答案的关键部分（至少70%的词）
        String[] answerWords = normalizedAnswer.split("\\s+");
        int matchedWords = 0;
        for (String word : answerWords) {
            if (word.length() > 2 && normalizedText.contains(word)) {
                matchedWords++;
            }
        }
        
        return answerWords.length > 0 && (double) matchedWords / answerWords.length >= 0.7;
    }
    
    /**
     * 计算相关性分数。
     */
    private double calculateRelevancy(String text, String question, String answer) {
        if (text == null || question == null || answer == null) return 0.0;
        
        // 简化实现：基于关键词覆盖
        String normalizedText = text.toLowerCase();
        String[] questionWords = question.toLowerCase().split("\\s+");
        String[] answerWords = answer.toLowerCase().split("\\s+");
        
        int questionCoverage = 0;
        for (String word : questionWords) {
            if (word.length() > 2 && normalizedText.contains(word)) {
                questionCoverage++;
            }
        }
        
        int answerCoverage = 0;
        for (String word : answerWords) {
            if (word.length() > 2 && normalizedText.contains(word)) {
                answerCoverage++;
            }
        }
        
        double qScore = questionWords.length > 0 ? (double) questionCoverage / questionWords.length : 0;
        double aScore = answerWords.length > 0 ? (double) answerCoverage / answerWords.length : 0;
        
        return (qScore + aScore) / 2.0;
    }
    
    /**
     * 计算综合分数。
     */
    private double calculateOverallScore(
            OcrEvaluationService.EvaluationResult ocrResult,
            StructuredDocumentMetrics structuredMetrics,
            OcrEvaluationService.CompressionMetrics compressionMetrics,
            QAEvaluationMetrics qaMetrics) {
        
        double score = 0.0;
        double weight = 0.0;
        
        // OCR准确率权重30%
        if (ocrResult != null) {
            score += ocrResult.getOverallScore() * 0.3;
            weight += 0.3;
        }
        
        // 结构化文档权重20%
        if (structuredMetrics != null) {
            double structScore = (structuredMetrics.getStructureScore() + 
                    structuredMetrics.getAverageConfidence()) / 2.0;
            score += structScore * 0.2;
            weight += 0.2;
        }
        
        // 压缩效率权重20%
        if (compressionMetrics != null) {
            // 压缩比越高越好，但需要平衡信息保留
            double compressionScore = Math.min(1.0, compressionMetrics.getCompressionRatio() / 10.0);
            double effectiveScore = compressionScore * compressionMetrics.getInformationRetention();
            score += effectiveScore * 0.2;
            weight += 0.2;
        }
        
        // Q&A评估权重30%（如果有）
        if (qaMetrics != null) {
            double qaScore = (qaMetrics.getAnswerCoverage() + qaMetrics.getAverageRelevancy()) / 2.0;
            score += qaScore * 0.3;
            weight += 0.3;
        }
        
        return weight > 0 ? score / weight : 0.0;
    }
    
    /**
     * 生成改进建议。
     */
    private List<String> generateRecommendations(
            OcrEvaluationService.EvaluationResult ocrResult,
            StructuredDocumentMetrics structuredMetrics,
            OcrEvaluationService.CompressionMetrics compressionMetrics) {
        
        List<String> recommendations = new ArrayList<>();
        
        // 基于OCR准确率
        if (ocrResult != null && ocrResult.getCharacterAccuracy() < 0.9) {
            recommendations.add("字符准确率较低（<90%），建议：1）检查图像质量 2）调整OCR模型参数 3）考虑使用更高分辨率的图像");
        }
        
        // 基于结构化文档
        if (structuredMetrics != null) {
            if (structuredMetrics.getAverageConfidence() < 0.8) {
                recommendations.add("平均置信度较低（<80%），建议：1）提高置信度阈值过滤低质量元素 2）检查文档清晰度");
            }
            if (structuredMetrics.getTableCount() == 0 && structuredMetrics.getTotalElements() > 10) {
                recommendations.add("未检测到表格，如果文档包含表格，建议检查OCR提示词或模型配置");
            }
        }
        
        // 基于压缩效率
        if (compressionMetrics != null) {
            if (compressionMetrics.getCompressionRatio() < 2.0) {
                recommendations.add("压缩比较低（<2x），Token成本可能较高，建议：1）优化Markdown输出格式 2）过滤冗余内容");
            }
            if (compressionMetrics.getInformationRetention() < 0.8) {
                recommendations.add("信息保留率较低（<80%），可能丢失关键内容，建议：1）降低压缩阈值 2）检查关键词提取");
            }
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("解析质量良好，无需特别调整");
        }
        
        return recommendations;
    }
    
    // ==================== 数据类 ====================
    
    /**
     * Q&A数据对。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QAPair {
        private String question;
        private String expectedAnswer;
        private Map<String, Object> metadata;
    }
    
    /**
     * 解析质量报告。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsingQualityReport {
        private String parsingMethod;
        private OcrEvaluationService.EvaluationResult ocrMetrics;
        private StructuredDocumentMetrics structuredMetrics;
        private OcrEvaluationService.CompressionMetrics compressionMetrics;
        private QAEvaluationMetrics qaMetrics;
        private double overallScore;
        private List<String> recommendations;
    }
    
    /**
     * 结构化文档指标。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StructuredDocumentMetrics {
        private int pageCount;
        private int totalElements;
        private int titleCount;
        private int textCount;
        private int tableCount;
        private int imageCount;
        private double averageConfidence;
        private double structureScore;
        private double diversityScore;
        private ConfidenceDistribution confidenceDistribution;
    }
    
    /**
     * 置信度分布。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfidenceDistribution {
        private int highConfidenceCount;   // >= 0.9
        private int mediumConfidenceCount; // 0.7-0.9
        private int lowConfidenceCount;    // < 0.7
    }
    
    /**
     * Q&A评估指标。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QAEvaluationMetrics {
        private int totalQuestions;
        private int answerableQuestions;
        private double answerCoverage;
        private double averageRelevancy;
    }
}

