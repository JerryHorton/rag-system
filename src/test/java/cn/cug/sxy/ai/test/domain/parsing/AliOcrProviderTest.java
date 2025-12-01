package cn.cug.sxy.ai.test.domain.parsing;

import cn.cug.sxy.ai.domain.rag.model.parsing.StructuredDocument;
import cn.cug.sxy.ai.domain.rag.service.parsing.ocr.AliOcrProvider;
import cn.cug.sxy.ai.domain.rag.service.parsing.ocr.OcrProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AliOcrProvider 集成测试类
 * 用于测试本地图像文件的OCR识别功能
 * 
 * 使用说明：
 * 1. 确保配置了 DASHSCOPE_API_KEY 环境变量
 * 2. 将测试图像文件放在 src/test/resources/images/ 目录下
 * 3. 运行测试前确保网络连接正常
 * 
 * @author jerryhotton
 */
@DisplayName("阿里云OCR提供者集成测试")
class AliOcrProviderTest {

    private ObjectMapper objectMapper;
    private AliOcrProvider aliOcrProvider;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        // 从环境变量获取API Key
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = System.getProperty("dashscope.api.key", "");
        }
        
        // 创建OCR提供者
        aliOcrProvider = new AliOcrProvider(
                objectMapper,
                apiKey,
                "qwen3-vl-plus",
                true
        );
    }

    @Test
    @DisplayName("测试读取本地图像文件进行OCR识别")
    void testRecognizeLocalImageFile() throws Exception {
        // 检查服务是否可用
        if (!aliOcrProvider.isAvailable()) {
            System.out.println("⚠️  警告: 阿里云OCR服务未配置，跳过测试");
            System.out.println("   请设置环境变量 DASHSCOPE_API_KEY");
            return;
        }
        
        // 测试图像文件路径
        String imagePath = "src/test/resources/images/test_image.png";
        Path imageFile = Paths.get(imagePath);
        
        // 如果默认路径不存在，尝试其他常见路径
        if (!Files.exists(imageFile)) {
            String[] possiblePaths = {
                "test_images/test.png",
                "test_images/test.jpg",
                "images/test.png",
                "test.png"
            };
            
            boolean found = false;
            for (String path : possiblePaths) {
                imageFile = Paths.get(path);
                if (Files.exists(imageFile)) {
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                System.out.println("⚠️  警告: 未找到测试图像文件");
                System.out.println("   请将测试图像放在以下任一位置:");
                System.out.println("   - src/test/resources/images/test_image.png");
                return;
            }
        }
        
        System.out.println("📸 读取图像文件: " + imageFile.toAbsolutePath());
        
        // 读取图像文件
        byte[] imageBytes = Files.readAllBytes(imageFile);
        System.out.println("✅ 图像文件读取成功，大小: " + imageBytes.length + " bytes");
        
        // 执行OCR识别
        System.out.println("🔍 开始OCR识别...");
        long startTime = System.currentTimeMillis();
        
        StructuredDocument result = aliOcrProvider.recognize(imageBytes);
        
        long duration = System.currentTimeMillis() - startTime;
        
        // 验证结果
        assertNotNull(result, "OCR识别结果不应为空");
        System.out.println("✅ OCR识别成功，耗时: " + duration + " ms");
        
        // 打印识别结果
        printResult(result);
    }

    @Test
    @DisplayName("测试读取指定路径的图像文件")
    void testRecognizeCustomImageFile() throws Exception {
        if (!aliOcrProvider.isAvailable()) {
            System.out.println("⚠️  警告: 阿里云OCR服务未配置，跳过测试");
            return;
        }
        
        String customImagePath = System.getProperty("test.image.path");
        if (customImagePath == null || customImagePath.isEmpty()) {
            System.out.println("ℹ️  提示: 未指定测试图像路径");
            System.out.println("   可以通过系统属性指定: -Dtest.image.path=/path/to/image.png");
            return;
        }
        
        Path imageFile = Paths.get(customImagePath);
        if (!Files.exists(imageFile)) {
            System.out.println("❌ 错误: 指定的图像文件不存在: " + imageFile.toAbsolutePath());
            return;
        }
        
        System.out.println("📸 读取图像文件: " + imageFile.toAbsolutePath());
        
        byte[] imageBytes = Files.readAllBytes(imageFile);
        System.out.println("✅ 图像文件读取成功，大小: " + imageBytes.length + " bytes");
        
        System.out.println("🔍 开始OCR识别...");
        StructuredDocument result = aliOcrProvider.recognize(imageBytes);
        
        assertNotNull(result, "OCR识别结果不应为空");
        System.out.println("✅ OCR识别成功");
        
        printResult(result);
    }

    @Test
    @DisplayName("测试服务可用性")
    void testServiceAvailability() {
        boolean available = aliOcrProvider.isAvailable();
        System.out.println("服务可用性: " + (available ? "✅ 可用" : "❌ 不可用"));
        
        if (!available) {
            System.out.println("   原因: API Key未配置或服务未启用");
            System.out.println("   解决方法: 设置环境变量 DASHSCOPE_API_KEY");
        }
    }

    @Test
    @DisplayName("测试提供者名称和类型")
    void testProviderInfo() {
        String providerName = aliOcrProvider.getProviderName();
        OcrProvider.ProviderType providerType = aliOcrProvider.getProviderType();
        int priority = aliOcrProvider.getPriority();
        
        System.out.println("提供者名称: " + providerName);
        System.out.println("提供者类型: " + providerType);
        System.out.println("优先级: " + priority);
        
        assertTrue(providerName.contains("阿里云"));
        assertEquals(OcrProvider.ProviderType.CLOUD_API, providerType);
    }
    
    private void printResult(StructuredDocument result) {
        if (result.getPages() != null && !result.getPages().isEmpty()) {
            System.out.println("\n📄 识别结果:");
            System.out.println("   页面数: " + result.getPages().size());
            System.out.println("   模型: " + result.getModelInfo());
            
            for (var page : result.getPages()) {
                System.out.println("\n   页面 " + page.getPageNo() + ":");
                System.out.println("     图像尺寸: " + page.getImageSize());
                
                if (page.getLayout() != null && !page.getLayout().isEmpty()) {
                    System.out.println("     布局元素数: " + page.getLayout().size());
                    
                    for (int j = 0; j < Math.min(page.getLayout().size(), 5); j++) {
                        var element = page.getLayout().get(j);
                        System.out.println("\n     元素 " + (j + 1) + ":");
                        System.out.println("       类型: " + element.getType());
                        String text = element.getText();
                        if (text != null) {
                            System.out.println("       文本: " + 
                                (text.length() > 100 ? text.substring(0, 100) + "..." : text));
                        }
                        System.out.println("       置信度: " + element.getConfidence());
                    }
                    
                    if (page.getLayout().size() > 5) {
                        System.out.println("\n     ... 还有 " + (page.getLayout().size() - 5) + " 个元素");
                    }
                }
            }
            
            String markdown = result.toMarkdown();
            if (markdown != null && !markdown.isEmpty()) {
                System.out.println("\n📝 Markdown格式文本:");
                System.out.println("---");
                String preview = markdown.length() > 500 
                    ? markdown.substring(0, 500) + "\n... (截断)" 
                    : markdown;
                System.out.println(preview);
                System.out.println("---");
            }
        }
    }
}

