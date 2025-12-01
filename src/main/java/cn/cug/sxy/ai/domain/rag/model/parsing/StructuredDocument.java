package cn.cug.sxy.ai.domain.rag.model.parsing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 结构化文档模型。
 * 支持第四波OCR（如qwen3-vl-plus）的结构化输出格式。
 * 
 * 技术指南核心：
 * 1. 类型化（type）：识别文本的角色（title, text, table等）
 * 2. 空间信息（bbox）：保留元素位置信息
 * 3. 结构化输出（md_text）：直接生成Markdown格式
 * 4. 置信度（confidence）：用于质量评估和过滤
 * 
 * @author jerryhotton
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StructuredDocument {
    
    /**
     * 文档页面列表
     */
    @JsonProperty("pages")
    private List<Page> pages;
    
    /**
     * 模型信息
     */
    @JsonProperty("model_info")
    private String modelInfo;
    
    /**
     * 处理时间（毫秒）
     */
    @JsonProperty("processing_time_ms")
    private Long processingTimeMs;
    
    /**
     * 元数据
     */
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    /**
     * 页面模型
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Page {
        /**
         * 页码（从1开始）
         */
        @JsonProperty("page_no")
        private Integer pageNo;
        
        /**
         * 图像尺寸 [width, height]
         */
        @JsonProperty("image_size")
        private List<Integer> imageSize;
        
        /**
         * 版面元素列表
         */
        @JsonProperty("layout")
        private List<LayoutElement> layout;
        
        /**
         * 获取页面的元素数量
         */
        @JsonIgnore
        public int getElementCount() {
            return layout != null ? layout.size() : 0;
        }
        
        /**
         * 获取页面的平均置信度
         */
        @JsonIgnore
        public double getAverageConfidence() {
            if (layout == null || layout.isEmpty()) {
                return 0.0;
            }
            return layout.stream()
                    .filter(e -> e.getConfidence() != null)
                    .mapToDouble(LayoutElement::getConfidence)
                    .average()
                    .orElse(0.0);
        }
    }
    
    /**
     * 版面元素模型
     * 
     * 为L3层Chunking优化的设计：
     * 1. 支持标题层级（headingLevel）
     * 2. 支持表格结构化信息（tableInfo）
     * 3. 支持父子关系（parentId）
     * 4. 支持语义边界标记
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LayoutElement {
        /**
         * 元素唯一ID（用于建立父子关系）
         */
        @JsonProperty("element_id")
        private String elementId;
        
        /**
         * 父元素ID（用于建立层级关系，如章节包含段落）
         */
        @JsonProperty("parent_id")
        private String parentId;
        
        /**
         * 元素类型：title, text, table, image, formula, code, list等
         */
        @JsonProperty("type")
        private String type;
        
        /**
         * 标题层级（1=h1, 2=h2, 3=h3...）仅当type为title时有效
         * 这对L3层的递归字符分块至关重要
         */
        @JsonProperty("heading_level")
        private Integer headingLevel;
        
        /**
         * 边界框 [x, y, width, height]
         */
        @JsonProperty("bbox")
        private List<Double> bbox;
        
        /**
         * 原始文本内容
         */
        @JsonProperty("text")
        private String text;
        
        /**
         * Markdown格式文本
         */
        @JsonProperty("md_text")
        private String mdText;
        
        /**
         * 置信度（0-1）
         */
        @JsonProperty("confidence")
        private Double confidence;
        
        /**
         * 额外属性
         */
        @JsonProperty("attributes")
        private Map<String, Object> attributes;
        
        /**
         * 表格结构化信息（仅当type为table时有效）
         * 这是L3层"按行分块并注入上下文"策略的关键
         */
        @JsonProperty("table_info")
        private TableInfo tableInfo;
        
        /**
         * 获取最佳文本（优先md_text）
         */
        @JsonIgnore
        public String getBestText() {
            if (mdText != null && !mdText.isEmpty()) {
                return mdText;
            }
            return text != null ? text : "";
        }
        
        /**
         * 检查是否为标题元素
         */
        @JsonIgnore
        public boolean isTitle() {
            return type != null && 
                   (type.equalsIgnoreCase("title") || 
                    type.equalsIgnoreCase("heading") ||
                    type.equalsIgnoreCase("header"));
        }
        
        /**
         * 检查是否为表格元素
         */
        @JsonIgnore
        public boolean isTable() {
            return type != null && type.equalsIgnoreCase("table");
        }
        
        /**
         * 获取标题层级（默认为1）
         */
        @JsonIgnore
        public int getEffectiveHeadingLevel() {
            return headingLevel != null ? headingLevel : 1;
        }
    }
    
    /**
     * 表格结构化信息。
     * 
     * 技术指南L3层核心需求：
     * "表格应该被视为一个整体，而不是被切成零散的行。"
     * "单独的一行数据如果没有表头和表格标题，其语义也是不完整的。"
     * 
     * 该类支持L3层的三种表格处理策略：
     * 1. 完整分块：将整个表格作为一个Chunk
     * 2. 按行分块+上下文注入：每行携带表头和标题
     * 3. LLM摘要：提供表格摘要供索引
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableInfo {
        /**
         * 表格标题（如"Table 1: Financial Summary"）
         */
        @JsonProperty("title")
        private String title;
        
        /**
         * 表格说明/描述
         */
        @JsonProperty("caption")
        private String caption;
        
        /**
         * 表头行（用于"按行分块+上下文注入"策略）
         */
        @JsonProperty("headers")
        private List<String> headers;
        
        /**
         * 表格行数
         */
        @JsonProperty("row_count")
        private Integer rowCount;
        
        /**
         * 表格列数
         */
        @JsonProperty("column_count")
        private Integer columnCount;
        
        /**
         * 表格数据行（每行是一个String列表）
         */
        @JsonProperty("rows")
        private List<List<String>> rows;
        
        /**
         * 表格的LLM摘要（可选，用于L3层策略三）
         */
        @JsonProperty("summary")
        private String summary;
        
        /**
         * 获取共享上下文（标题+表头），用于L3层"按行分块+上下文注入"策略
         */
        @JsonIgnore
        public String getSharedContext() {
            StringBuilder sb = new StringBuilder();
            
            // 添加表格标题
            if (title != null && !title.isEmpty()) {
                sb.append(title).append("\n");
            }
            
            // 添加表头（Markdown格式）
            if (headers != null && !headers.isEmpty()) {
                sb.append("|");
                for (String header : headers) {
                    sb.append(" ").append(header).append(" |");
                }
                sb.append("\n|");
                for (int i = 0; i < headers.size(); i++) {
                    sb.append("---|");
                }
            }
            
            return sb.toString();
        }
        
        /**
         * 将指定行转换为带上下文的Chunk（用于L3层）
         */
        @JsonIgnore
        public String getRowWithContext(int rowIndex) {
            if (rows == null || rowIndex < 0 || rowIndex >= rows.size()) {
                return "";
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append(getSharedContext()).append("\n");
            
            // 添加数据行
            List<String> row = rows.get(rowIndex);
            sb.append("|");
            for (String cell : row) {
                sb.append(" ").append(cell).append(" |");
            }
            
            return sb.toString();
        }
        
        /**
         * 获取所有带上下文的行Chunks（用于L3层混合索引策略）
         */
        @JsonIgnore
        public List<String> getAllRowsWithContext() {
            List<String> chunks = new java.util.ArrayList<>();
            if (rows == null) return chunks;
            
            String sharedContext = getSharedContext();
            for (List<String> row : rows) {
                StringBuilder sb = new StringBuilder();
                sb.append(sharedContext).append("\n|");
                for (String cell : row) {
                    sb.append(" ").append(cell).append(" |");
                }
                chunks.add(sb.toString());
            }
            
            return chunks;
        }
    }
    
    // ==================== 转换方法 ====================
    
    /**
     * 转换为纯文本（用于向后兼容）
     */
    public String toPlainText() {
        if (pages == null || pages.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (Page page : pages) {
            if (page.getLayout() != null) {
                for (LayoutElement element : page.getLayout()) {
                    String text = element.getText();
                    if (text != null && !text.isEmpty()) {
                        sb.append(text).append("\n\n");
                    }
                }
            }
        }
        return sb.toString().trim();
    }
    
    /**
     * 转换为Markdown格式（根据元素类型生成适当的Markdown语法）
     */
    public String toMarkdown() {
        return toMarkdown(0.0); // 默认不过滤低置信度元素
    }
    
    /**
     * 转换为Markdown格式（可指定置信度阈值过滤）
     * 
     * @param confidenceThreshold 置信度阈值，低于此值的元素将被跳过
     */
    public String toMarkdown(double confidenceThreshold) {
        if (pages == null || pages.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        boolean isFirstElement = true;
        
        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            Page page = pages.get(pageIndex);
            
            // 多页时添加页面分隔
            if (pageIndex > 0 && page.getLayout() != null && !page.getLayout().isEmpty()) {
                sb.append("\n\n---\n\n");
            }
            
            if (page.getLayout() != null) {
                for (LayoutElement element : page.getLayout()) {
                    // 置信度过滤
                    if (element.getConfidence() != null && element.getConfidence() < confidenceThreshold) {
                        continue;
                    }
                    
                    String markdown = elementToMarkdown(element);
                    if (!markdown.isEmpty()) {
                        if (!isFirstElement) {
                            sb.append("\n\n");
                        }
                        sb.append(markdown);
                        isFirstElement = false;
                    }
                }
            }
        }
        
        return sb.toString().trim();
    }
    
    /**
     * 将单个元素转换为Markdown（根据类型生成适当格式）
     */
    private String elementToMarkdown(LayoutElement element) {
        String type = element.getType();
        String content = element.getMdText();
        
        // 如果没有md_text，使用text
        if (content == null || content.isEmpty()) {
            content = element.getText();
        }
        
        if (content == null || content.isEmpty()) {
            return "";
        }
        
        // 根据类型生成适当的Markdown格式
        if (type == null) {
            return content;
        }
        
        switch (type.toLowerCase()) {
            case "title":
            case "heading":
            case "header":
                // 标题：如果不是以#开头，添加#
                if (!content.startsWith("#")) {
                    return "# " + content;
                }
                return content;
                
            case "subtitle":
            case "subheading":
                // 副标题：使用##
                if (!content.startsWith("#")) {
                    return "## " + content;
                }
                return content;
                
            case "text":
            case "paragraph":
                // 普通文本：直接返回
                return content;
                
            case "table":
                // 表格：确保是Markdown表格格式
                if (content.contains("|")) {
                    return content;
                }
                // 如果不是Markdown表格，尝试转换
                return convertToMarkdownTable(content);
                
            case "list":
            case "bullet":
                // 列表：确保有列表标记
                return ensureListFormat(content);
                
            case "code":
                // 代码：使用代码块
                if (!content.startsWith("```")) {
                    return "```\n" + content + "\n```";
                }
                return content;
                
            case "formula":
            case "math":
            case "equation":
                // 公式：使用LaTeX格式
                if (!content.startsWith("$") && !content.startsWith("\\[")) {
                    return "$" + content + "$";
                }
                return content;
                
            case "image":
            case "figure":
                // 图片说明
                return "*" + content + "*";
                
            case "caption":
                // 图表标题
                return "*" + content + "*";
                
            case "footnote":
                // 脚注
                return "[^note]: " + content;
                
            case "quote":
            case "blockquote":
                // 引用
                if (!content.startsWith(">")) {
                    return "> " + content.replace("\n", "\n> ");
                }
                return content;
                
            default:
                return content;
        }
    }
    
    /**
     * 将文本转换为Markdown表格格式
     */
    private String convertToMarkdownTable(String text) {
        // 简单处理：尝试将制表符或多空格分隔的文本转换为表格
        String[] lines = text.split("\n");
        if (lines.length < 2) {
            return text;
        }
        
        StringBuilder sb = new StringBuilder();
        boolean headerDone = false;
        
        for (String line : lines) {
            String[] cells = line.split("\t|\\s{2,}");
            if (cells.length > 1) {
                sb.append("|");
                for (String cell : cells) {
                    sb.append(" ").append(cell.trim()).append(" |");
                }
                sb.append("\n");
                
                // 在表头后添加分隔行
                if (!headerDone) {
                    sb.append("|");
                    for (int i = 0; i < cells.length; i++) {
                        sb.append("---|");
                    }
                    sb.append("\n");
                    headerDone = true;
                }
            } else {
                sb.append(line).append("\n");
            }
        }
        
        return sb.toString().trim();
    }
    
    /**
     * 确保文本是列表格式
     */
    private String ensureListFormat(String text) {
        String[] lines = text.split("\n");
        StringBuilder sb = new StringBuilder();
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            
            // 如果不是以列表标记开头，添加标记
            if (!trimmed.matches("^[-*+]\\s.*") && !trimmed.matches("^\\d+\\.\\s.*")) {
                sb.append("- ").append(trimmed).append("\n");
            } else {
                sb.append(trimmed).append("\n");
            }
        }
        
        return sb.toString().trim();
    }
    
    // ==================== L3层Chunking支持方法 ====================
    
    /**
     * 获取为Chunking优化的可分块单元列表。
     * 
     * 技术指南L3层核心：
     * "Chunking的本质，是在'保持语义完整性'和'控制块大小以适应模型'这两个
     * 相互冲突的目标之间，寻找最佳的平衡点。"
     * 
     * 该方法返回的ChunkableUnit包含：
     * 1. 完整的语义内容
     * 2. 元素类型信息（用于不同处理策略）
     * 3. 层级信息（用于递归分块）
     * 4. 表格特殊处理信息
     */
    public List<ChunkableUnit> toChunkableUnits() {
        List<ChunkableUnit> units = new java.util.ArrayList<>();
        
        if (pages == null || pages.isEmpty()) {
            return units;
        }
        
        // 当前章节路径（用于上下文追踪）
        java.util.Deque<String> sectionPath = new java.util.ArrayDeque<>();
        
        for (Page page : pages) {
            if (page.getLayout() == null) continue;
            
            for (LayoutElement element : page.getLayout()) {
                ChunkableUnit unit = createChunkableUnit(element, page.getPageNo(), sectionPath);
                if (unit != null) {
                    units.add(unit);
                }
                
                // 更新章节路径（用于标题层级追踪）
                if (element.isTitle()) {
                    updateSectionPath(sectionPath, element);
                }
            }
        }
        
        return units;
    }
    
    /**
     * 创建可分块单元
     */
    private ChunkableUnit createChunkableUnit(LayoutElement element, 
                                               Integer pageNo, 
                                               java.util.Deque<String> sectionPath) {
        String content = element.getBestText();
        if (content == null || content.isEmpty()) {
            return null;
        }
        
        ChunkableUnit.ChunkableUnitBuilder builder = ChunkableUnit.builder()
                .elementId(element.getElementId())
                .type(element.getType())
                .content(content)
                .pageNo(pageNo)
                .confidence(element.getConfidence())
                .sectionPath(new java.util.ArrayList<>(sectionPath));
        
        // 设置标题层级
        if (element.isTitle()) {
            builder.headingLevel(element.getEffectiveHeadingLevel());
            builder.isSemanticBoundary(true); // 标题是语义边界
        }
        
        // 表格特殊处理
        if (element.isTable() && element.getTableInfo() != null) {
            builder.tableInfo(element.getTableInfo());
            builder.isSemanticBoundary(true); // 表格也是语义边界
        }
        
        // 代码块和公式也是语义边界
        String type = element.getType();
        if (type != null && 
            (type.equalsIgnoreCase("code") || 
             type.equalsIgnoreCase("formula") ||
             type.equalsIgnoreCase("equation"))) {
            builder.isSemanticBoundary(true);
        }
        
        return builder.build();
    }
    
    /**
     * 更新章节路径
     */
    private void updateSectionPath(java.util.Deque<String> sectionPath, LayoutElement titleElement) {
        int level = titleElement.getEffectiveHeadingLevel();
        String title = titleElement.getBestText();
        
        // 移除同级或更低级的标题
        while (!sectionPath.isEmpty() && sectionPath.size() >= level) {
            sectionPath.pollLast();
        }
        
        // 添加当前标题
        sectionPath.addLast(title);
    }
    
    /**
     * 获取表格的混合索引Chunks（用于L3层混合索引策略）。
     * 
     * 技术指南：
     * "面对表格，最佳实践往往是混合索引（Hybrid Indexing）"
     * 1. 为整个表格创建一个摘要Chunk（捕捉宏观语义）
     * 2. 为每一行创建一个带上下文的行Chunk（精确查找）
     * 3. 为整个表格的原文创建一个完整Chunk（黄金上下文）
     */
    public List<TableChunkSet> getTableChunkSets() {
        List<TableChunkSet> result = new java.util.ArrayList<>();
        
        if (pages == null) return result;
        
        for (Page page : pages) {
            if (page.getLayout() == null) continue;
            
            for (LayoutElement element : page.getLayout()) {
                if (element.isTable()) {
                    TableChunkSet chunkSet = createTableChunkSet(element, page.getPageNo());
                    if (chunkSet != null) {
                        result.add(chunkSet);
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * 创建表格的混合索引Chunk集合
     */
    private TableChunkSet createTableChunkSet(LayoutElement tableElement, Integer pageNo) {
        String fullContent = tableElement.getBestText();
        if (fullContent == null || fullContent.isEmpty()) {
            return null;
        }
        
        TableChunkSet.TableChunkSetBuilder builder = TableChunkSet.builder()
                .elementId(tableElement.getElementId())
                .pageNo(pageNo)
                .fullTableChunk(fullContent); // 策略一：完整表格Chunk
        
        TableInfo tableInfo = tableElement.getTableInfo();
        if (tableInfo != null) {
            // 策略二：带上下文的行Chunks
            builder.rowChunksWithContext(tableInfo.getAllRowsWithContext());
            
            // 策略三：表格摘要（如果有）
            if (tableInfo.getSummary() != null && !tableInfo.getSummary().isEmpty()) {
                builder.summaryChunk(tableInfo.getSummary());
            }
            
            builder.tableTitle(tableInfo.getTitle());
            builder.headers(tableInfo.getHeaders());
        }
        
        return builder.build();
    }
    
    /**
     * 可分块单元。
     * L2层输出给L3层Chunking的核心数据结构。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChunkableUnit {
        /**
         * 元素ID
         */
        private String elementId;
        
        /**
         * 元素类型
         */
        private String type;
        
        /**
         * 内容（Markdown格式）
         */
        private String content;
        
        /**
         * 页码
         */
        private Integer pageNo;
        
        /**
         * 置信度
         */
        private Double confidence;
        
        /**
         * 标题层级（仅标题类型有效）
         */
        private Integer headingLevel;
        
        /**
         * 是否为语义边界（标题、表格、代码块等）
         * L3层的递归字符分块应优先在此处切分
         */
        private Boolean isSemanticBoundary;
        
        /**
         * 章节路径（如["第一章", "1.1 概述"]）
         * 用于为Chunk添加上下文元数据
         */
        private List<String> sectionPath;
        
        /**
         * 表格信息（仅表格类型有效）
         */
        private TableInfo tableInfo;
        
        /**
         * 获取章节路径字符串
         */
        @JsonIgnore
        public String getSectionPathString() {
            if (sectionPath == null || sectionPath.isEmpty()) {
                return "";
            }
            return String.join(" > ", sectionPath);
        }
        
        /**
         * 估算Token数量
         */
        @JsonIgnore
        public int estimateTokens() {
            if (content == null) return 0;
            return (int) Math.ceil(content.length() / 3.0);
        }
    }
    
    /**
     * 表格混合索引Chunk集合。
     * 
     * L3层技术指南的表格混合索引策略实现载体：
     * "在检索时，可以并行地从这几种不同类型的Chunk中进行检索，
     * 然后将结果融合，送给LLM。这最大化了信息检索的全面性和精确性。"
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableChunkSet {
        /**
         * 元素ID
         */
        private String elementId;
        
        /**
         * 页码
         */
        private Integer pageNo;
        
        /**
         * 表格标题
         */
        private String tableTitle;
        
        /**
         * 表头
         */
        private List<String> headers;
        
        /**
         * 策略一：完整表格Chunk（用于提供完整上下文）
         */
        private String fullTableChunk;
        
        /**
         * 策略二：带上下文的行Chunks（用于精确查找）
         */
        private List<String> rowChunksWithContext;
        
        /**
         * 策略三：表格摘要Chunk（用于语义检索）
         */
        private String summaryChunk;
        
        /**
         * 获取所有用于索引的Chunks
         */
        @JsonIgnore
        public List<String> getAllIndexableChunks() {
            List<String> chunks = new java.util.ArrayList<>();
            
            // 添加完整表格Chunk
            if (fullTableChunk != null && !fullTableChunk.isEmpty()) {
                chunks.add(fullTableChunk);
            }
            
            // 添加行Chunks
            if (rowChunksWithContext != null) {
                chunks.addAll(rowChunksWithContext);
            }
            
            // 添加摘要Chunk
            if (summaryChunk != null && !summaryChunk.isEmpty()) {
                chunks.add(summaryChunk);
            }
            
            return chunks;
        }
    }
    
    // ==================== 统计方法 ====================
    
    /**
     * 获取文档统计信息
     */
    @JsonIgnore
    public DocumentStats getStats() {
        if (pages == null || pages.isEmpty()) {
            return DocumentStats.builder()
                    .pageCount(0)
                    .totalElements(0)
                    .build();
        }
        
        int totalElements = 0;
        int titleCount = 0;
        int textCount = 0;
        int tableCount = 0;
        int imageCount = 0;
        int otherCount = 0;
        double totalConfidence = 0.0;
        int confidenceCount = 0;
        int totalChars = 0;
        
        for (Page page : pages) {
            if (page.getLayout() != null) {
                for (LayoutElement element : page.getLayout()) {
                    totalElements++;
                    
                    // 统计类型
                    String type = element.getType();
                    if (type != null) {
                        switch (type.toLowerCase()) {
                            case "title":
                            case "heading":
                            case "header":
                            case "subtitle":
                                titleCount++;
                                break;
                            case "text":
                            case "paragraph":
                                textCount++;
                                break;
                            case "table":
                                tableCount++;
                                break;
                            case "image":
                            case "figure":
                                imageCount++;
                                break;
                            default:
                                otherCount++;
                        }
                    }
                    
                    // 统计置信度
                    if (element.getConfidence() != null) {
                        totalConfidence += element.getConfidence();
                        confidenceCount++;
                    }
                    
                    // 统计字符数
                    String text = element.getBestText();
                    if (text != null) {
                        totalChars += text.length();
                    }
                }
            }
        }
        
        double avgConfidence = confidenceCount > 0 ? totalConfidence / confidenceCount : 0.0;
        int estimatedTokens = estimateTokenCount(totalChars);
        
        return DocumentStats.builder()
                .pageCount(pages.size())
                .totalElements(totalElements)
                .titleCount(titleCount)
                .textCount(textCount)
                .tableCount(tableCount)
                .imageCount(imageCount)
                .otherCount(otherCount)
                .averageConfidence(avgConfidence)
                .totalCharacters(totalChars)
                .estimatedTokens(estimatedTokens)
                .build();
    }
    
    /**
     * 估算Token数量
     */
    private int estimateTokenCount(int charCount) {
        // 简化估算：平均每3个字符约1个Token（中英文混合）
        return (int) Math.ceil(charCount / 3.0);
    }
    
    /**
     * 文档统计信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentStats {
        private int pageCount;
        private int totalElements;
        private int titleCount;
        private int textCount;
        private int tableCount;
        private int imageCount;
        private int otherCount;
        private double averageConfidence;
        private int totalCharacters;
        private int estimatedTokens;
    }
}

