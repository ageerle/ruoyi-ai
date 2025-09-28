package org.ruoyi.asset.service;

import com.alibaba.excel.EasyExcel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.ruoyi.asset.domain.vo.MinUsagePeriodImportVo;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * 简单的Excel导入测试（不依赖Spring Boot）
 *
 * @author cass
 * @date 2025-09-24
 */
@Slf4j
public class ExcelImportSimpleTest {

    /**
     * Excel文件路径
     */
    private static final String EXCEL_FILE_PATH = "E:/z. WorkSpace/ruoyi-ai/workspace/教育部直属高校固定资产最低使用年限表.xlsx";

    public static void main(String[] args) {
        ExcelImportSimpleTest test = new ExcelImportSimpleTest();
        test.testExcelFileRead();
    }

    /**
     * 测试Excel文件读取和解析
     */
    @Test
    public void testExcelFileRead() {
        try {
            File excelFile = new File(EXCEL_FILE_PATH);
            
            if (!excelFile.exists()) {
                log.error("Excel文件不存在: {}", EXCEL_FILE_PATH);
                return;
            }
            
            log.info("=== Excel文件信息 ===");
            log.info("文件路径: {}", excelFile.getAbsolutePath());
            log.info("文件大小: {} bytes", excelFile.length());
            log.info("文件是否存在: {}", excelFile.exists());
            log.info("文件是否可读: {}", excelFile.canRead());
            
            // 使用EasyExcel直接读取
            try (InputStream inputStream = new FileInputStream(excelFile)) {
                List<MinUsagePeriodImportVo> dataList = EasyExcel.read(inputStream)
                    .head(MinUsagePeriodImportVo.class)
                    .sheet()
                    .doReadSync();
                
                log.info("=== 读取结果 ===");
                log.info("EasyExcel直接读取到 {} 条数据", dataList.size());
                
                // 打印前10条数据
                for (int i = 0; i < Math.min(10, dataList.size()); i++) {
                    MinUsagePeriodImportVo data = dataList.get(i);
                    log.info("第{}条: 类别={}, 内容={}, 年限={}, 国标代码={}", 
                        i + 1, data.getCategory(), data.getContent(), data.getMinYears(), data.getGbCode());
                }
                
                // 验证数据完整性
                validateData(dataList);
                
            }
            
        } catch (Exception e) {
            log.error("Excel文件读取测试失败", e);
        }
    }

    /**
     * 验证数据完整性
     */
    private void validateData(List<MinUsagePeriodImportVo> dataList) {
        log.info("=== 数据验证结果 ===");
        
        int validCategoryCount = 0;
        int validContentCount = 0;
        int validMinYearsCount = 0;
        int validGbCodeCount = 0;
        
        for (MinUsagePeriodImportVo data : dataList) {
            if (data.getCategory() != null && !data.getCategory().trim().isEmpty()) {
                validCategoryCount++;
            }
            if (data.getContent() != null && !data.getContent().trim().isEmpty()) {
                validContentCount++;
            }
            if (data.getMinYears() != null) {
                validMinYearsCount++;
            }
            if (data.getGbCode() != null && !data.getGbCode().trim().isEmpty()) {
                validGbCodeCount++;
            }
        }
        
        log.info("总行数: {}", dataList.size());
        log.info("有效固定资产类别: {}/{}", validCategoryCount, dataList.size());
        log.info("有效内容: {}/{}", validContentCount, dataList.size());
        log.info("有效最低使用年限: {}/{}", validMinYearsCount, dataList.size());
        log.info("有效国标代码: {}/{}", validGbCodeCount, dataList.size());
        
        // 检查重复的国标代码
        long uniqueGbCodeCount = dataList.stream()
            .filter(data -> data.getGbCode() != null && !data.getGbCode().trim().isEmpty())
            .map(MinUsagePeriodImportVo::getGbCode)
            .distinct()
            .count();
        
        log.info("唯一国标代码数量: {}", uniqueGbCodeCount);
        
        if (uniqueGbCodeCount != validGbCodeCount) {
            log.warn("发现重复的国标代码！");
        }
        
        // 检查数据质量
        if (validCategoryCount == dataList.size() && 
            validContentCount == dataList.size() && 
            validMinYearsCount == dataList.size() && 
            validGbCodeCount == dataList.size()) {
            log.info("✓ 数据质量检查通过，所有字段都完整");
        } else {
            log.warn("✗ 数据质量检查未通过，存在缺失字段");
        }
    }

    /**
     * 测试Excel文件格式
     */
    @Test
    public void testExcelFormat() {
        try {
            File excelFile = new File(EXCEL_FILE_PATH);
            
            if (!excelFile.exists()) {
                log.error("Excel文件不存在: {}", EXCEL_FILE_PATH);
                return;
            }
            
            log.info("=== 开始Excel格式测试 ===");
            
            try (InputStream inputStream = new FileInputStream(excelFile)) {
                // 测试读取第一行（标题行）
                List<MinUsagePeriodImportVo> dataList = EasyExcel.read(inputStream)
                    .head(MinUsagePeriodImportVo.class)
                    .sheet()
                    .doReadSync();
                
                if (!dataList.isEmpty()) {
                    MinUsagePeriodImportVo firstData = dataList.get(0);
                    log.info("第一行数据示例:");
                    log.info("- 固定资产类别: {}", firstData.getCategory());
                    log.info("- 内容: {}", firstData.getContent());
                    log.info("- 最低使用年限: {}", firstData.getMinYears());
                    log.info("- 国标代码: {}", firstData.getGbCode());
                }
                
                log.info("Excel格式测试完成，共读取 {} 行数据", dataList.size());
            }
            
        } catch (Exception e) {
            log.error("Excel格式测试失败", e);
        }
    }
}
