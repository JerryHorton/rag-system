package cn.cug.sxy.ai.domain.rag.service.parsing;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF渲染服务。
 * 将PDF页面渲染为高质量图像（用于OCR）。
 *
 * @author jerryhotton
 */
@Slf4j
@Service
public class PdfRenderService {

    @Value("${rag.parsing.pdf.render-dpi:300}")
    private int renderDpi; // 渲染DPI，默认300（高质量）

    @Value("${rag.parsing.pdf.image-format:PNG}")
    private String imageFormat; // 图像格式，默认PNG

    /**
     * 将PDF文件的所有页面渲染为图像。
     *
     * @param filePath PDF文件路径
     * @return 每页的图像字节数组列表
     * @throws IOException 渲染失败
     */
    public List<byte[]> renderPdfToImages(String filePath) throws IOException {
        File file = new File(filePath);
        try (PDDocument document = Loader.loadPDF(file)) {
            return renderDocument(document);
        }
    }

    /**
     * 将PDF输入流的所有页面渲染为图像。
     *
     * @param inputStream PDF输入流
     * @return 每页的图像字节数组列表
     * @throws IOException 渲染失败
     */
    public List<byte[]> renderPdfToImages(InputStream inputStream) throws IOException {
        RandomAccessReadBuffer buffer = new RandomAccessReadBuffer(inputStream);
        try (PDDocument document = Loader.loadPDF(buffer)) {
            return renderDocument(document);
        }
    }

    /**
     * 渲染PDF文档。
     */
    private List<byte[]> renderDocument(PDDocument document) throws IOException {
        PDFRenderer renderer = new PDFRenderer(document);
        int pageCount = document.getNumberOfPages();
        List<byte[]> images = new ArrayList<>();
        log.debug("开始渲染PDF，页数: {}, DPI: {}", pageCount, renderDpi);
        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            try {
                // 渲染页面为BufferedImage
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, renderDpi);
                // 转换为字节数组
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                ImageIO.write(image, imageFormat, stream);
                byte[] imageBytes = stream.toByteArray();
                images.add(imageBytes);
                log.debug("已渲染第 {} 页，图像大小: {} bytes", pageIndex + 1, imageBytes.length);
            } catch (Exception e) {
                log.error("渲染第 {} 页失败", pageIndex + 1, e);
                // 继续渲染其他页面
            }
        }
        log.info("PDF渲染完成，成功渲染 {} 页", images.size());

        return images;
    }

    /**
     * 渲染指定页面。
     *
     * @param filePath  PDF文件路径
     * @param pageIndex 页面索引（从0开始）
     * @return 图像字节数组
     * @throws IOException 渲染失败
     */
    public byte[] renderPage(String filePath, int pageIndex) throws IOException {
        File file = new File(filePath);
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(pageIndex, renderDpi);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ImageIO.write(image, imageFormat, stream);

            return stream.toByteArray();
        }
    }

}

