package cn.cug.sxy.ai.domain.rag.model.parsing;

/**
 * 文档解析模式。
 * <p>
 * 让用户根据文档特点选择最合适的解析策略，简单直观。
 *
 * @author jerryhotton
 */
public enum ParsingMode {

    /**
     * 简单模式 - 纯文本提取
     * <p>
     * 适用场景：
     * - 纯文字的PDF/Word文档
     * - 只需要提取文字内容
     * - 不关心文档结构
     * <p>
     * 特点：
     * - 速度最快
     * - 成本最低（不调用任何API）
     * - 不保留版面结构、标题层级
     * - 不识别表格和图片
     */
    SIMPLE("简单模式", "纯文本提取，速度最快，不保留结构"),

    /**
     * OCR模式 - 逐页渲染+VLM识别（推荐）
     * <p>
     * 适用场景：
     * - 需要保留文档结构的PDF
     * - 包含表格、公式、图片的文档
     * - 扫描件、图片型PDF
     * - 各种复杂排版的文档
     * <p>
     * 特点：
     * - 逐页渲染为图片，使用VLM识别
     * - 保留表格结构（Markdown格式）
     * - 识别公式（LaTeX格式）
     * - 理解图片内容
     * - 无token限制（逐页处理）
     * - 有API成本
     */
    OCR("OCR模式", "逐页渲染+VLM识别，保留结构，适用于各类文档"),

    /**
     * 自动模式 - 智能检测
     * <p>
     * 系统自动检测文档类型，智能选择最佳解析策略：
     * 1. 首先尝试直接提取文本，评估质量
     * 2. 如果是扫描件或复杂文档，使用OCR模式
     * 3. 如果是简单纯文本，使用简单模式
     * <p>
     * 特点：
     * - 无需用户判断文档类型
     * - 自动平衡效果和成本
     */
    AUTO("自动模式", "智能检测文档类型，自动选择最佳策略");

    private final String displayName;
    private final String description;

    ParsingMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 是否需要使用OCR
     */
    public boolean requiresOcr() {
        return this == OCR;
    }

    /**
     * 是否需要自动检测
     */
    public boolean requiresAutoDetection() {
        return this == AUTO;
    }

    /**
     * 是否保留文档结构
     */
    public boolean preservesStructure() {
        return this == OCR;
    }
}
