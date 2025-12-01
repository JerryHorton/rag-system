package cn.cug.sxy.ai.domain.rag.service.parsing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 文本质量评估器。
 * 用于判断直接提取的文本是否高质量，决定是否需要OCR。
 * 
 * @author jerryhotton
 */
@Slf4j
@Component
public class TextQualityEvaluator {
    
    // 乱码模式：连续的特殊字符或数字
    private static final Pattern GIBBERISH_PATTERN = Pattern.compile("[^\\p{L}\\p{N}\\s]{3,}");
    
    // 中文字符模式
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]");
    
    /**
     * 评估文本质量。
     * 
     * @param text 待评估的文本
     * @return 是否高质量
     */
    public boolean isHighQualityText(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.debug("文本为空，质量评估: false");
            return false;
        }
        
        // 1. 长度检查：至少100个字符
        if (text.length() < 100) {
            log.debug("文本长度过短: {}，质量评估: false", text.length());
            return false;
        }
        
        // 2. 乱码率检查
        double gibberishRatio = calculateGibberishRatio(text);
        if (gibberishRatio > 0.15) { // 乱码率超过15%
            log.debug("乱码率过高: {:.2f}，质量评估: false", gibberishRatio);
            return false;
        }
        
        // 3. 平均词长检查
        double avgWordLength = calculateAvgWordLength(text);
        if (avgWordLength < 1.5 || avgWordLength > 20.0) {
            log.debug("平均词长异常: {:.2f}，质量评估: false", avgWordLength);
            return false;
        }
        
        // 4. 可读性检查：单词密度
        double wordCharRatio = calculateWordCharRatio(text);
        if (wordCharRatio < 0.08) { // 单词密度过低
            log.debug("单词密度过低: {:.2f}，质量评估: false", wordCharRatio);
            return false;
        }
        
        // 5. 中英文混合检查（中文文档的特殊处理）
        if (containsChinese(text)) {
            // 中文文档：检查中文字符比例
            double chineseRatio = calculateChineseRatio(text);
            if (chineseRatio < 0.1) {
                log.debug("中文字符比例过低: {:.2f}，质量评估: false", chineseRatio);
                return false;
            }
        }
        
        log.debug("文本质量评估通过，长度: {}, 乱码率: {:.2f}, 平均词长: {:.2f}", 
                text.length(), gibberishRatio, avgWordLength);
        return true;
    }
    
    /**
     * 计算乱码率。
     */
    private double calculateGibberishRatio(String text) {
        if (text == null || text.isEmpty()) {
            return 1.0;
        }
        
        // 统计连续特殊字符的数量
        long gibberishChars = GIBBERISH_PATTERN.matcher(text).results()
                .mapToLong(match -> match.group().length())
                .sum();
        
        return (double) gibberishChars / text.length();
    }
    
    /**
     * 计算平均词长。
     */
    private double calculateAvgWordLength(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }
        
        String[] words = text.trim().split("\\s+");
        if (words.length == 0) {
            return 0.0;
        }
        
        long totalLength = 0;
        for (String word : words) {
            totalLength += word.length();
        }
        
        return (double) totalLength / words.length;
    }
    
    /**
     * 计算单词字符比（单词数/总字符数）。
     */
    private double calculateWordCharRatio(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }
        
        String[] words = text.trim().split("\\s+");
        long wordCount = words.length;
        long charCount = text.length();
        
        return charCount > 0 ? (double) wordCount / charCount : 0.0;
    }
    
    /**
     * 检查是否包含中文。
     */
    private boolean containsChinese(String text) {
        return text != null && CHINESE_PATTERN.matcher(text).find();
    }
    
    /**
     * 计算中文字符比例。
     */
    private double calculateChineseRatio(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }
        
        long chineseCount = CHINESE_PATTERN.matcher(text).results().count();
        return (double) chineseCount / text.length();
    }
    
    /**
     * 计算文本质量分数（0-1）。
     */
    public double calculateQualityScore(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }
        
        // 长度分数
        double lengthScore = Math.min(1.0, text.length() / 1000.0);
        
        // 乱码率分数（越低越好）
        double gibberishRatio = calculateGibberishRatio(text);
        double gibberishScore = Math.max(0.0, 1.0 - gibberishRatio * 2);
        
        // 可读性分数
        double wordCharRatio = calculateWordCharRatio(text);
        double readabilityScore = Math.min(1.0, wordCharRatio * 10);
        
        // 综合分数（加权平均）
        return lengthScore * 0.3 + gibberishScore * 0.4 + readabilityScore * 0.3;
    }
}

