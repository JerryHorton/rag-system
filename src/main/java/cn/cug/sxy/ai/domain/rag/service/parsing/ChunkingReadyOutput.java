package cn.cug.sxy.ai.domain.rag.service.parsing;

import cn.cug.sxy.ai.domain.rag.model.parsing.StructuredDocument;
import cn.cug.sxy.ai.domain.rag.model.parsing.StructuredDocument.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * L2层为L3层Chunking准备的输出服务。
 * 
 * 技术指南L3层核心需求：
 * 1. "语义完整性"：每个Chunk本身应该是一个逻辑上自洽、语义完整的单元
 * 2. "粒度适中"：Chunk的大小应该与期望回答的问题的粒度相匹配
 * 3. "上下文感知"：Chunk应该携带足够的上下文信息
 * 4. "结构保留"：分块过程不应破坏原始文档的结构信息
 * 
 * 本服务将L2层的StructuredDocument转换为L3层Chunking所需的格式。
 * 
 * @author jerryhotton
 */
@Slf4j
@Service
public class ChunkingReadyOutput {
    
    /**
     * 将结构化文档转换为Chunking就绪输出。
     * 这是L2层与L3层之间的桥梁。
     * 
     * @param document L2层解析的结构化文档
     * @return L3层Chunking就绪的输出
     */
    public ChunkingInput prepareForChunking(StructuredDocument document) {
        if (document == null) {
            return ChunkingInput.builder()
                    .chunkableUnits(new ArrayList<>())
                    .tableChunkSets(new ArrayList<>())
                    .build();
        }
        
        log.debug("准备Chunking输入，文档页数: {}", 
                document.getPages() != null ? document.getPages().size() : 0);
        
        // 获取可分块单元
        List<ChunkableUnit> units = document.toChunkableUnits();
        
        // 获取表格混合索引集合
        List<TableChunkSet> tableSets = document.getTableChunkSets();
        
        // 提取语义边界点
        List<SemanticBoundary> boundaries = extractSemanticBoundaries(units);
        
        // 构建输出
        ChunkingInput input = ChunkingInput.builder()
                .chunkableUnits(units)
                .tableChunkSets(tableSets)
                .semanticBoundaries(boundaries)
                .documentStats(document.getStats())
                .fullMarkdown(document.toMarkdown())
                .build();
        
        log.info("Chunking输入准备完成: 可分块单元={}, 表格集合={}, 语义边界={}",
                units.size(), tableSets.size(), boundaries.size());
        
        return input;
    }
    
    /**
     * 提取语义边界点。
     * L3层的递归字符分块应优先在这些点切分。
     */
    private List<SemanticBoundary> extractSemanticBoundaries(List<ChunkableUnit> units) {
        List<SemanticBoundary> boundaries = new ArrayList<>();
        int charOffset = 0;
        
        for (int i = 0; i < units.size(); i++) {
            ChunkableUnit unit = units.get(i);
            
            if (Boolean.TRUE.equals(unit.getIsSemanticBoundary())) {
                boundaries.add(SemanticBoundary.builder()
                        .unitIndex(i)
                        .charOffset(charOffset)
                        .type(unit.getType())
                        .headingLevel(unit.getHeadingLevel())
                        .isTable(unit.getTableInfo() != null)
                        .build());
            }
            
            // 更新字符偏移
            if (unit.getContent() != null) {
                charOffset += unit.getContent().length() + 2; // +2 for \n\n separator
            }
        }
        
        return boundaries;
    }
    
    /**
     * 获取递归字符分块的分隔符优先级列表。
     * 
     * 技术指南：
     * "递归字符分块：提供一个分隔符列表，按优先级进行递归尝试。
     * 首先用最高优先级的分隔符（\n\n）来分割，如果仍然大于chunk_size，
     * 则使用次一级的分隔符（\n）进行再次分割..."
     */
    public List<String> getRecommendedSeparators() {
        return List.of(
                "\n\n",      // 段落边界（最高优先级）
                "\n",        // 换行边界
                "。",        // 中文句号
                "！",        // 中文感叹号
                "？",        // 中文问号
                ". ",        // 英文句号
                "! ",        // 英文感叹号
                "? ",        // 英文问号
                "；",        // 中文分号
                "; ",        // 英文分号
                "，",        // 中文逗号
                ", ",        // 英文逗号
                " "          // 空格（最低优先级）
        );
    }
    
    /**
     * L3层Chunking的输入数据结构。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChunkingInput {
        /**
         * 可分块单元列表
         */
        private List<ChunkableUnit> chunkableUnits;
        
        /**
         * 表格混合索引集合（用于表格的特殊处理）
         */
        private List<TableChunkSet> tableChunkSets;
        
        /**
         * 语义边界点列表（用于递归字符分块）
         */
        private List<SemanticBoundary> semanticBoundaries;
        
        /**
         * 文档统计信息
         */
        private DocumentStats documentStats;
        
        /**
         * 完整的Markdown文本（用于固定大小分块的fallback）
         */
        private String fullMarkdown;
        
        /**
         * 获取所有标题元素（用于构建文档大纲）
         */
        public List<ChunkableUnit> getTitles() {
            if (chunkableUnits == null) return new ArrayList<>();
            return chunkableUnits.stream()
                    .filter(u -> u.getHeadingLevel() != null)
                    .collect(Collectors.toList());
        }
        
        /**
         * 获取指定类型的元素
         */
        public List<ChunkableUnit> getByType(String type) {
            if (chunkableUnits == null || type == null) return new ArrayList<>();
            return chunkableUnits.stream()
                    .filter(u -> type.equalsIgnoreCase(u.getType()))
                    .collect(Collectors.toList());
        }
        
        /**
         * 估算总Token数
         */
        public int estimateTotalTokens() {
            if (chunkableUnits == null) return 0;
            return chunkableUnits.stream()
                    .mapToInt(ChunkableUnit::estimateTokens)
                    .sum();
        }
        
        /**
         * 获取表格数量
         */
        public int getTableCount() {
            return tableChunkSets != null ? tableChunkSets.size() : 0;
        }
    }
    
    /**
     * 语义边界点。
     * L3层分块时的优先切分位置。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SemanticBoundary {
        /**
         * 在chunkableUnits列表中的索引
         */
        private int unitIndex;
        
        /**
         * 在完整Markdown文本中的字符偏移量
         */
        private int charOffset;
        
        /**
         * 边界类型（title, table, code, formula等）
         */
        private String type;
        
        /**
         * 标题层级（仅标题有效）
         */
        private Integer headingLevel;
        
        /**
         * 是否为表格
         */
        private Boolean isTable;
    }
    
    /**
     * 推荐的Chunking策略。
     * 根据文档特点返回建议的分块策略。
     */
    public ChunkingRecommendation recommendStrategy(ChunkingInput input) {
        if (input == null || input.getChunkableUnits() == null) {
            return ChunkingRecommendation.builder()
                    .primaryStrategy(ChunkingStrategy.FIXED_SIZE)
                    .reason("文档为空，使用默认固定大小分块")
                    .build();
        }
        
        int totalTokens = input.estimateTotalTokens();
        int tableCount = input.getTableCount();
        int titleCount = input.getTitles().size();
        int boundaryCount = input.getSemanticBoundaries() != null ? 
                input.getSemanticBoundaries().size() : 0;
        
        ChunkingRecommendation.ChunkingRecommendationBuilder builder = 
                ChunkingRecommendation.builder();
        
        // 根据文档特点推荐策略
        if (tableCount > 0) {
            // 有表格：推荐混合策略
            builder.primaryStrategy(ChunkingStrategy.HYBRID)
                   .tableStrategy(TableChunkingStrategy.HYBRID_INDEXING)
                   .reason("文档包含" + tableCount + "个表格，建议使用混合索引策略");
        } else if (boundaryCount >= 10) {
            // 有丰富的语义边界：推荐语义分块
            builder.primaryStrategy(ChunkingStrategy.SEMANTIC)
                   .reason("文档有" + boundaryCount + "个语义边界，建议使用语义分块");
        } else if (titleCount >= 5) {
            // 有章节结构：推荐递归字符分块
            builder.primaryStrategy(ChunkingStrategy.RECURSIVE_CHARACTER)
                   .reason("文档有" + titleCount + "个标题，建议使用递归字符分块");
        } else {
            // 默认：固定大小分块
            builder.primaryStrategy(ChunkingStrategy.FIXED_SIZE)
                   .reason("文档结构简单，使用固定大小分块");
        }
        
        // 推荐的块大小
        builder.recommendedChunkSize(calculateRecommendedChunkSize(totalTokens))
               .recommendedOverlap(200);
        
        return builder.build();
    }
    
    /**
     * 计算推荐的块大小
     */
    private int calculateRecommendedChunkSize(int totalTokens) {
        // 小文档：较小的块
        if (totalTokens < 1000) {
            return 256;
        }
        // 中等文档：中等块
        if (totalTokens < 10000) {
            return 512;
        }
        // 大文档：较大的块
        return 1024;
    }
    
    /**
     * Chunking策略枚举
     */
    public enum ChunkingStrategy {
        FIXED_SIZE,           // 固定大小分块
        RECURSIVE_CHARACTER,  // 递归字符分块
        SEMANTIC,             // 语义分块
        AGENTIC,              // Agent分块（LLM驱动）
        HYBRID                // 混合策略
    }
    
    /**
     * 表格Chunking策略枚举
     */
    public enum TableChunkingStrategy {
        WHOLE_TABLE,          // 整表作为一个Chunk
        ROW_WITH_CONTEXT,     // 按行分块+上下文注入
        LLM_SUMMARY,          // LLM摘要
        HYBRID_INDEXING       // 混合索引（推荐）
    }
    
    /**
     * Chunking策略推荐
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChunkingRecommendation {
        /**
         * 主要分块策略
         */
        private ChunkingStrategy primaryStrategy;
        
        /**
         * 表格处理策略
         */
        private TableChunkingStrategy tableStrategy;
        
        /**
         * 推荐的块大小（Token数）
         */
        private Integer recommendedChunkSize;
        
        /**
         * 推荐的重叠大小（字符数）
         */
        private Integer recommendedOverlap;
        
        /**
         * 推荐理由
         */
        private String reason;
    }
}

