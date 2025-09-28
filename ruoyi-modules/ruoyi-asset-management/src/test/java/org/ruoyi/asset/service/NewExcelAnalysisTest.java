package org.ruoyi.asset.service;

import com.alibaba.excel.EasyExcel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 新Excel文件分析测试
 *
 * @author cass
 * @date 2025-09-24
 */
@Slf4j
public class NewExcelAnalysisTest {

    /**
     * 新Excel文件路径
     */
    private static final String NEW_EXCEL_FILE_PATH = "E:/z. WorkSpace/ruoyi-ai/workspace/高等学校固定资产分类与代码.xlsx";

    /**
     * 分析新Excel文件结构
     */
    @Test
    public void analyzeNewExcelFile() {
        try {
            log.info("=== 开始分析新Excel文件结构 ===");
            
            File excelFile = new File(NEW_EXCEL_FILE_PATH);
            if (!excelFile.exists()) {
                log.error("Excel文件不存在: {}", NEW_EXCEL_FILE_PATH);
                return;
            }
            
            log.info("文件信息:");
            log.info("- 文件路径: {}", excelFile.getAbsolutePath());
            log.info("- 文件大小: {} bytes", excelFile.length());
            log.info("- 文件是否存在: {}", excelFile.exists());
            log.info("- 文件是否可读: {}", excelFile.canRead());
            
            // 1. 读取原始数据（不指定头部）
            log.info("\n1. 读取原始数据（前20行）...");
            try (InputStream inputStream = new FileInputStream(excelFile)) {
                List<Map<Integer, String>> rawData = EasyExcel.read(inputStream)
                    .sheet()
                    .headRowNumber(0) // 不跳过头部
                    .doReadSync();
                
                log.info("   总共读取到 {} 行数据", rawData.size());
                
                // 显示前20行数据
                for (int i = 0; i < Math.min(20, rawData.size()); i++) {
                    Map<Integer, String> row = rawData.get(i);
                    log.info("   第{}行: {}", i + 1, row);
                }
            }
            
            // 2. 分析数据结构
            log.info("\n2. 分析数据结构...");
            analyzeDataStructure(excelFile);
            
            // 3. 尝试按指定字段读取
            log.info("\n3. 尝试按指定字段读取...");
            tryReadWithFields(excelFile);
            
            log.info("=== 新Excel文件结构分析完成 ===");
            
        } catch (Exception e) {
            log.error("分析新Excel文件失败", e);
        }
    }

    /**
     * 分析数据结构
     */
    private void analyzeDataStructure(File excelFile) throws Exception {
        try (InputStream inputStream = new FileInputStream(excelFile)) {
            List<Map<Integer, String>> rawData = EasyExcel.read(inputStream)
                .sheet()
                .headRowNumber(0)
                .doReadSync();
            
            log.info("   数据行数: {}", rawData.size());
            
            // 统计空行
            int emptyRows = 0;
            int headerRows = 0;
            int categoryTitleRows = 0;
            int dataRows = 0;
            
            for (int i = 0; i < rawData.size(); i++) {
                Map<Integer, String> row = rawData.get(i);
                
                // 检查是否为空行
                boolean isEmpty = row.values().stream().allMatch(value -> 
                    value == null || value.trim().isEmpty());
                
                if (isEmpty) {
                    emptyRows++;
                } else {
                    // 检查是否是表头行
                    String firstCell = row.get(0);
                    if (firstCell != null && firstCell.contains("分类代码")) {
                        headerRows++;
                    } else if (firstCell != null && firstCell.trim().length() > 0 && 
                              (row.get(1) == null || row.get(1).trim().isEmpty())) {
                        // 只有第一列有值，其他列为空，可能是分类标题
                        categoryTitleRows++;
                    } else {
                        dataRows++;
                    }
                }
            }
            
            log.info("   空行数量: {}", emptyRows);
            log.info("   表头行数量: {}", headerRows);
            log.info("   分类标题行数量: {}", categoryTitleRows);
            log.info("   数据行数量: {}", dataRows);
            
            // 显示一些分类标题行的例子
            log.info("   分类标题行示例:");
            int categoryCount = 0;
            for (int i = 0; i < rawData.size() && categoryCount < 5; i++) {
                Map<Integer, String> row = rawData.get(i);
                String firstCell = row.get(0);
                if (firstCell != null && firstCell.trim().length() > 0 && 
                    (row.get(1) == null || row.get(1).trim().isEmpty())) {
                    log.info("     - 第{}行: {}", i + 1, firstCell);
                    categoryCount++;
                }
            }
        }
    }

    /**
     * 尝试按指定字段读取
     */
    private void tryReadWithFields(File excelFile) throws Exception {
        // 创建一个简单的VO类来测试字段映射
        try (InputStream inputStream = new FileInputStream(excelFile)) {
            List<Map<Integer, String>> rawData = EasyExcel.read(inputStream)
                .sheet()
                .headRowNumber(0)
                .doReadSync();
            
            log.info("   尝试识别有效数据行...");
            
            int validDataCount = 0;
            for (int i = 0; i < rawData.size(); i++) {
                Map<Integer, String> row = rawData.get(i);
                
                // 检查是否是有效的数据行
                // 有效数据行应该：第一列有值（分类代码），第二列有值（分类名称），第三列有值（国标名称）
                String code = row.get(0);
                String name = row.get(1);
                String gbName = row.get(2);
                
                if (code != null && !code.trim().isEmpty() &&
                    name != null && !name.trim().isEmpty() &&
                    gbName != null && !gbName.trim().isEmpty() &&
                    !code.contains("分类代码")) { // 排除表头行
                    
                    validDataCount++;
                    if (validDataCount <= 10) {
                        log.info("   有效数据第{}行: 代码={}, 名称={}, 国标名称={}", 
                            validDataCount, code, name, gbName);
                    }
                }
            }
            
            log.info("   识别到 {} 条有效数据", validDataCount);
        }
    }
}
