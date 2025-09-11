package cn.cug.sxy.ai.domain.document.service.indexing;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * @version 1.0
 * @Date 2025/9/8 15:04
 * @Description Excel文档加载器实现
 * @Author jerryhotton
 */

@Component
public class ExcelDocumentLoader implements IDocumentLoader {

    /**
     * 从本地文件路径加载Excel文档
     *
     * @param filePath 文件路径
     * @return 文档内容和元数据的Map，包含text和metadata两个键
     * @throws IOException 如果文件读取或解析失败
     */
    @Override
    public Map<String, Object> loadFromFile(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            return extractContentAndMetadata(fis, filePath);
        }
    }

    /**
     * 从URL加载Excel文档
     *
     * @param url 文档URL
     * @return 文档内容和元数据的Map，包含text和metadata两个键
     * @throws IOException 如果URL连接或解析失败
     */
    @Override
    public Map<String, Object> loadFromUrl(String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        try (InputStream is = connection.getInputStream()) {
            return extractContentAndMetadata(is, url);
        }
    }

    /**
     * 从输入流中提取Excel文档的文本内容和元数据
     *
     * @param inputStream 文档输入流
     * @param source      文档来源（文件路径或URL）
     * @return 包含文本内容和元数据的Map
     * @throws IOException 如果解析失败
     */
    private Map<String, Object> extractContentAndMetadata(InputStream inputStream, String source) throws IOException {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> metadata = new HashMap<>();
        // 根据文件扩展名判断Excel文档类型
        boolean isXlsx = source.toLowerCase().endsWith(".xlsx");
        // 在POI 5.4.0中，可以使用WorkbookFactory统一创建工作簿
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            // 设置文档格式
            metadata.put("format", isXlsx ? "XLSX" : "XLS");
            // 提取工作簿元数据
            metadata.put("numberOfSheets", workbook.getNumberOfSheets());
            metadata.put("sheetNames", getSheetNames(workbook));
            // 从所有工作表中提取文本内容
            StringBuilder contentBuilder = new StringBuilder();
            // 遍历所有工作表
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                // 添加工作表名称作为标题
                contentBuilder.append("===== 工作表: ").append(sheet.getSheetName()).append(" =====\n");
                // 遍历所有行
                for (Row row : sheet) {
                    StringJoiner rowContent = new StringJoiner("\t");
                    // 遍历行中的所有单元格
                    for (Cell cell : row) {
                        String cellValue = getCellValueAsString(cell);
                        rowContent.add(cellValue);
                    }
                    // 添加行内容
                    contentBuilder.append(rowContent.toString()).append("\n");
                }

                contentBuilder.append("\n");
            }
            // 添加源文件信息到元数据
            String fileName = source;
            if (source.contains("/")) {
                fileName = source.substring(source.lastIndexOf('/') + 1);
            }
            metadata.put("source", source);
            metadata.put("fileName", fileName);

            result.put("text", contentBuilder.toString());
            result.put("metadata", metadata);
        }

        return result;
    }

    /**
     * 获取工作簿中所有工作表的名称
     *
     * @param workbook 工作簿对象
     * @return 包含所有工作表名称的数组
     */
    private String[] getSheetNames(Workbook workbook) {
        String[] sheetNames = new String[workbook.getNumberOfSheets()];
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            sheetNames[i] = workbook.getSheetAt(i).getSheetName();
        }
        return sheetNames;
    }

    /**
     * 将单元格的值转换为字符串
     *
     * @param cell 单元格对象
     * @return 单元格值的字符串表示
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                } else {
                    // 避免科学计数法
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    try {
                        return cell.getStringCellValue();
                    } catch (Exception ex) {
                        return cell.getCellFormula();
                    }
                }
            default:
                return "";
        }
    }

    /**
     * 判断当前加载器是否支持指定的文件类型
     *
     * @param fileExtension 文件扩展名
     * @return 如果是Excel文件(.xls, .xlsx)则返回true
     */
    @Override
    public boolean supports(String fileExtension) {
        return fileExtension.equalsIgnoreCase("xls") ||
                fileExtension.equalsIgnoreCase("xlsx");
    }

}
