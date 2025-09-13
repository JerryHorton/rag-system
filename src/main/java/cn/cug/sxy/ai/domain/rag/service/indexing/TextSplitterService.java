package cn.cug.sxy.ai.domain.rag.service.indexing;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @version 1.0
 * @Date 2025/9/8 16:28
 * @Description 文本分块服务
 * @Author jerryhotton
 */

@Slf4j
@Service
public class TextSplitterService {

    /**
     * 分割策略枚举
     */
    public enum SplitStrategy {
        RECURSIVE_CHARACTER,  // 递归字符分割（默认）
        FIXED_LENGTH,         // 固定长度分割
        SENTENCE,             // 按句子分割
        PARAGRAPH,            // 按段落分割
        TOKEN,                // 按Token分割
        CHINESE_WORD          // 中文分词
    }

    /**
     * 默认递归分割分隔符列表，按优先级从高到低排序
     * 同时支持中文和英文标点符号
     */
    private static final List<String> DEFAULT_SEPARATORS = Arrays.asList(
            "\n\n",      // 段落分隔符
            "\n",        // 换行符
            "。 ",       // 中文句号后跟空格
            "。",        // 中文句号
            ". ",        // 英文句号后跟空格
            "？ ",       // 中文问号后跟空格
            "？",        // 中文问号
            "? ",        // 英文问号后跟空格
            "！ ",       // 中文感叹号后跟空格
            "！",        // 中文感叹号
            "! ",        // 英文感叹号后跟空格
            "； ",       // 中文分号后跟空格
            "；",        // 中文分号
            "; ",        // 英文分号后跟空格
            "， ",       // 中文逗号后跟空格
            "，",        // 中文逗号
            ", ",        // 英文逗号后跟空格
            " ",         // 空格
            ""           // 空字符串，最后的回退项
    );

    /**
     * 句子结束模式（同时支持中英文标点）
     */
    private static final Pattern SENTENCE_ENDINGS = Pattern.compile("(?<=[.!?。！？])\\s*");

    /**
     * 段落分隔模式
     */
    private static final Pattern PARAGRAPH_SEPARATORS = Pattern.compile("\\n\\s*\\n");

    /**
     * 将文本分割成多个文本块
     *
     * @param text         待分割的文本
     * @param chunkSize    块大小（字符数）
     * @param chunkOverlap 块重叠大小（字符数）
     * @return 文本块列表
     */
    public List<String> splitText(String text, int chunkSize, int chunkOverlap) {
        return splitText(text, chunkSize, chunkOverlap, SplitStrategy.RECURSIVE_CHARACTER);
    }

    /**
     * 将文本分割成多个文本块，可指定分割策略
     *
     * @param text         待分割的文本
     * @param chunkSize    块大小（字符数）
     * @param chunkOverlap 块重叠大小（字符数）
     * @param strategy     分割策略
     * @return 文本块列表
     */
    public List<String> splitText(String text, int chunkSize, int chunkOverlap, SplitStrategy strategy) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive");
        }
        if (chunkOverlap < 0 || chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("Chunk overlap must be non-negative and less than chunk size");
        }
        switch (strategy) {
            case RECURSIVE_CHARACTER:
                return recursiveCharacterSplit(text, chunkSize, chunkOverlap, DEFAULT_SEPARATORS);
            case FIXED_LENGTH:
                return fixedLengthSplit(text, chunkSize, chunkOverlap);
            case SENTENCE:
                return sentenceSplit(text, chunkSize, chunkOverlap);
            case PARAGRAPH:
                return paragraphSplit(text, chunkSize, chunkOverlap);
            case TOKEN:
                return tokenSplit(text, chunkSize, chunkOverlap);
            case CHINESE_WORD:
                return chineseWordSplit(text, chunkSize, chunkOverlap);
            default:
                return recursiveCharacterSplit(text, chunkSize, chunkOverlap, DEFAULT_SEPARATORS);
        }
    }

    /**
     * 递归字符分割算法
     * <p>
     * 参考自LangChain的递归字符分割器，根据一系列分隔符（从最大粒度到最小粒度）
     * 递归地分割文本，尽可能保持语义完整性。
     *
     * @param text         待分割的文本
     * @param chunkSize    块大小（字符数）
     * @param chunkOverlap 块重叠大小（字符数）
     * @param separators   分隔符列表，按优先级从高到低排序
     * @return 文本块列表
     */
    public List<String> recursiveCharacterSplit(String text, int chunkSize, int chunkOverlap, List<String> separators) {
        List<String> finalChunks = new ArrayList<>();
        // 如果文本长度小于等于块大小，直接返回整个文本
        if (text.length() <= chunkSize) {
            finalChunks.add(text);
            return finalChunks;
        }
        // 递归分割
        recursiveSplit(text, chunkSize, chunkOverlap, separators, 0, finalChunks);

        return finalChunks;
    }

    /**
     * 递归分割辅助方法
     *
     * @param text           待分割的文本
     * @param chunkSize      块大小（字符数）
     * @param chunkOverlap   块重叠大小（字符数）
     * @param separators     分隔符列表
     * @param separatorIndex 当前使用的分隔符索引
     * @param result         结果列表
     */
    private void recursiveSplit(String text, int chunkSize, int chunkOverlap,
                                List<String> separators, int separatorIndex, List<String> result) {
        // 如果已经尝试了所有分隔符或文本长度小于等于块大小，直接添加整个文本
        if (separatorIndex >= separators.size() || text.length() <= chunkSize) {
            if (!text.trim().isEmpty()) {
                result.add(text);
            }
            return;
        }
        String separator = separators.get(separatorIndex);
        // 如果当前分隔符为空，进入强制分割模式
        if (separator.isEmpty()) {
            int start = 0;
            while (start < text.length()) {
                int end = Math.min(start + chunkSize, text.length());
                String chunk = text.substring(start, end);
                if (!chunk.trim().isEmpty()) {
                    result.add(chunk);
                }
                start += chunkSize - chunkOverlap;
            }
            return;
        }
        // 使用当前分隔符分割
        List<String> segments = new ArrayList<>(Arrays.asList(text.split(Pattern.quote(separator))));
        // 重新添加分隔符（除最后一段外）
        for (int i = 0; i < segments.size() - 1; i++) {
            segments.set(i, segments.get(i) + separator);
        }
        // 移除空段
        segments.removeIf(String::isEmpty);
        // 如果分割后没有任何段，尝试下一个分隔符
        if (segments.isEmpty()) {
            recursiveSplit(text, chunkSize, chunkOverlap, separators, separatorIndex + 1, result);
            return;
        }
        // 合并分割后的段，确保每个合并块不超过块大小
        StringBuilder currentChunk = new StringBuilder();
        for (String segment : segments) {
            // 如果当前块加上新段会超过块大小，先保存当前块并开始新块
            if (currentChunk.length() + segment.length() > chunkSize && !currentChunk.isEmpty()) {
                result.add(currentChunk.toString());
                // 计算重叠部分
                int overlapStart = Math.max(0, currentChunk.length() - chunkOverlap);
                if (overlapStart < currentChunk.length()) {
                    currentChunk = new StringBuilder(currentChunk.substring(overlapStart));
                } else {
                    currentChunk = new StringBuilder();
                }
            }
            // 如果单个段超过块大小，递归处理
            if (segment.length() > chunkSize) {
                // 先添加当前累积的块
                if (!currentChunk.isEmpty()) {
                    result.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                }
                // 递归处理这个大段
                recursiveSplit(segment, chunkSize, chunkOverlap, separators, separatorIndex + 1, result);
            } else {
                // 否则添加到当前块
                currentChunk.append(segment);
            }
        }
        // 添加最后一个块
        if (!currentChunk.isEmpty()) {
            result.add(currentChunk.toString());
        }
    }

    /**
     * 固定长度分割算法
     * <p>
     * 简单地将文本按固定长度分割，不考虑语义边界
     *
     * @param text         待分割的文本
     * @param chunkSize    块大小（字符数）
     * @param chunkOverlap 块重叠大小（字符数）
     * @return 文本块列表
     */
    public List<String> fixedLengthSplit(String text, int chunkSize, int chunkOverlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
            start += chunkSize - chunkOverlap;
        }

        return chunks;
    }

    /**
     * 按句子分割算法
     * 同时支持中文和英文句子
     *
     * @param text         待分割的文本
     * @param chunkSize    块大小（字符数）
     * @param chunkOverlap 块重叠大小（字符数）
     * @return 文本块列表
     */
    public List<String> sentenceSplit(String text, int chunkSize, int chunkOverlap) {
        // 按句子分割，同时支持中英文标点
        String[] sentences = SENTENCE_ENDINGS.split(text);
        return mergeSegments(sentences, chunkSize, chunkOverlap);
    }

    /**
     * 按段落分割算法
     * <p>
     * 首先按段落分割，然后合并段落以满足块大小要求
     *
     * @param text         待分割的文本
     * @param chunkSize    块大小（字符数）
     * @param chunkOverlap 块重叠大小（字符数）
     * @return 文本块列表
     */
    public List<String> paragraphSplit(String text, int chunkSize, int chunkOverlap) {
        // 按段落分割
        String[] paragraphs = PARAGRAPH_SEPARATORS.split(text);
        return mergeSegments(paragraphs, chunkSize, chunkOverlap);
    }

    /**
     * 按Token分割算法
     * 简单实现，将空格作为Token边界
     * 注意：这种方法不适合中文文本，中文文本请使用chineseWordSplit
     *
     * @param text         待分割的文本
     * @param chunkSize    块大小（字符数）
     * @param chunkOverlap 块重叠大小（字符数）
     * @return 文本块列表
     */
    public List<String> tokenSplit(String text, int chunkSize, int chunkOverlap) {
        // 简单分词，以空格为边界，适用于英文
        String[] tokens = text.split("\\s+");
        return mergeSegments(tokens, chunkSize, chunkOverlap);
    }

    /**
     * 中文分词算法
     * 使用HanLP进行中文分词
     * 适用于中文或中英文混合文本
     *
     * @param text         待分割的文本
     * @param chunkSize    块大小（字符数）
     * @param chunkOverlap 块重叠大小（字符数）
     * @return 文本块列表
     */
    public List<String> chineseWordSplit(String text, int chunkSize, int chunkOverlap) {
        List<Term> termList = HanLP.segment(text);
        String[] words = new String[termList.size()];
        for (int i = 0; i < termList.size(); i++) {
            words[i] = termList.get(i).word;
        }

        return mergeSegments(words, chunkSize, chunkOverlap);
    }

    /**
     * 合并文本段，确保每个合并块不超过块大小
     *
     * @param segments     文本段数组
     * @param chunkSize    块大小（字符数）
     * @param chunkOverlap 块重叠大小（字符数）
     * @return 文本块列表
     */
    private List<String> mergeSegments(String[] segments, int chunkSize, int chunkOverlap) {
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        for (String segment : segments) {
            // 如果当前块加上新段会超过块大小，先保存当前块并开始新块
            if (currentChunk.length() + segment.length() + 1 > chunkSize && !currentChunk.isEmpty()) {
                chunks.add(currentChunk.toString().trim());
                // 计算重叠部分
                // 这里简化处理，仅保留最后N个字符作为重叠部分
                if (chunkOverlap > 0 && currentChunk.length() > chunkOverlap) {
                    int overlapStart = Math.max(0, currentChunk.length() - chunkOverlap);
                    currentChunk = new StringBuilder(currentChunk.substring(overlapStart));
                } else {
                    currentChunk = new StringBuilder();
                }
            }
            // 添加分隔符，除非是第一段
            if (!currentChunk.isEmpty()) {
                currentChunk.append(" ");
            }
            // 如果单个段超过块大小，截断处理
            if (segment.length() > chunkSize) {
                // 先添加当前累积的块
                if (!currentChunk.isEmpty()) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }
                // 截断处理这个大段
                int segStart = 0;
                while (segStart < segment.length()) {
                    int segEnd = Math.min(segStart + chunkSize, segment.length());
                    chunks.add(segment.substring(segStart, segEnd).trim());
                    segStart += chunkSize - chunkOverlap;
                }
            } else {
                // 否则添加到当前块
                currentChunk.append(segment);
            }
        }
        // 添加最后一个块
        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

}
