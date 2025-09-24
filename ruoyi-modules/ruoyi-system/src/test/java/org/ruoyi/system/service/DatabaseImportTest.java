package org.ruoyi.system.service;

import com.alibaba.excel.EasyExcel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.ruoyi.system.domain.MinUsagePeriod;
import org.ruoyi.system.domain.vo.MinUsagePeriodImportVo;
import org.ruoyi.system.listener.MinUsagePeriodImportListener;
import org.ruoyi.system.mapper.MinUsagePeriodMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * 数据库导入测试
 *
 * @author cass
 * @date 2025-09-24
 */
@Slf4j
@SpringBootTest(classes = {org.ruoyi.RuoYiAIApplication.class})
@ActiveProfiles("test")
public class DatabaseImportTest {

    @Autowired
    private MinUsagePeriodMapper minUsagePeriodMapper;

    @Autowired
    private IMinUsagePeriodService minUsagePeriodService;

    /**
     * Excel文件路径
     */
    private static final String EXCEL_FILE_PATH = "E:/z. WorkSpace/ruoyi-ai/workspace/教育部直属高校固定资产最低使用年限表.xlsx";

    /**
     * 测试Excel数据导入到数据库
     */
    @Test
    @Transactional
    public void testExcelImportToDatabase() {
        try {
            log.info("=== 开始Excel数据导入到数据库测试 ===");
            
            // 1. 清空测试数据
            log.info("1. 清空测试数据...");
            minUsagePeriodMapper.delete(null);
            log.info("   清空完成");
            
            // 2. 读取Excel文件
            log.info("2. 读取Excel文件...");
            File excelFile = new File(EXCEL_FILE_PATH);
            if (!excelFile.exists()) {
                log.error("Excel文件不存在: {}", EXCEL_FILE_PATH);
                return;
            }
            
            List<MinUsagePeriodImportVo> dataList;
            try (InputStream inputStream = new FileInputStream(excelFile)) {
                dataList = EasyExcel.read(inputStream)
                    .head(MinUsagePeriodImportVo.class)
                    .sheet()
                    .doReadSync();
            }
            log.info("   读取到 {} 条数据", dataList.size());
            
            // 3. 处理数据（填充固定资产类别）
            log.info("3. 处理数据...");
            processData(dataList);
            log.info("   数据处理完成");
            
            // 4. 批量插入数据库
            log.info("4. 批量插入数据库...");
            int successCount = 0;
            int failCount = 0;
            
            for (MinUsagePeriodImportVo importVo : dataList) {
                try {
                    // 转换为实体对象
                    MinUsagePeriod entity = convertToEntity(importVo);
                    
                    // 插入数据库
                    minUsagePeriodMapper.insert(entity);
                    successCount++;
                    
                    if (successCount <= 5) {
                        log.info("   插入成功: {} - {}", entity.getContent(), entity.getGbCode());
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("   插入失败: {} - {}", importVo.getContent(), importVo.getGbCode(), e);
                }
            }
            
            log.info("   插入完成: 成功 {} 条, 失败 {} 条", successCount, failCount);
            
            // 5. 验证数据库中的数据
            log.info("5. 验证数据库中的数据...");
            List<MinUsagePeriod> dbData = minUsagePeriodMapper.selectList(null);
            log.info("   数据库中共有 {} 条记录", dbData.size());
            
            // 显示前10条数据
            for (int i = 0; i < Math.min(10, dbData.size()); i++) {
                MinUsagePeriod data = dbData.get(i);
                log.info("   第{}条: 类别={}, 内容={}, 年限={}, 国标代码={}", 
                    i + 1, data.getCategory(), data.getContent(), data.getMinYears(), data.getGbCode());
            }
            
            // 6. 验证数据完整性
            log.info("6. 验证数据完整性...");
            validateDatabaseData(dbData);
            
            log.info("=== Excel数据导入到数据库测试完成 ===");
            
        } catch (Exception e) {
            log.error("Excel数据导入到数据库测试失败", e);
        }
    }

    /**
     * 测试使用Service层导入数据
     */
    @Test
    @Transactional
    public void testServiceImportData() {
        try {
            log.info("=== 开始Service层导入数据测试 ===");
            
            // 1. 清空测试数据
            log.info("1. 清空测试数据...");
            minUsagePeriodMapper.delete(null);
            log.info("   清空完成");
            
            // 2. 使用Service层导入数据
            log.info("2. 使用Service层导入数据...");
            File excelFile = new File(EXCEL_FILE_PATH);
            if (!excelFile.exists()) {
                log.error("Excel文件不存在: {}", EXCEL_FILE_PATH);
                return;
            }
            
            try (InputStream inputStream = new FileInputStream(excelFile)) {
                // 使用自定义监听器导入数据
                MinUsagePeriodImportListener listener = new MinUsagePeriodImportListener(true);
                
                EasyExcel.read(inputStream)
                    .head(MinUsagePeriodImportVo.class)
                    .registerReadListener(listener)
                    .sheet()
                    .doRead();
                
                log.info("   Service层导入完成");
            }
            
            // 3. 验证数据库中的数据
            log.info("3. 验证数据库中的数据...");
            List<MinUsagePeriod> dbData = minUsagePeriodMapper.selectList(null);
            log.info("   数据库中共有 {} 条记录", dbData.size());
            
            // 显示前10条数据
            for (int i = 0; i < Math.min(10, dbData.size()); i++) {
                MinUsagePeriod data = dbData.get(i);
                log.info("   第{}条: 类别={}, 内容={}, 年限={}, 国标代码={}", 
                    i + 1, data.getCategory(), data.getContent(), data.getMinYears(), data.getGbCode());
            }
            
            log.info("=== Service层导入数据测试完成 ===");
            
        } catch (Exception e) {
            log.error("Service层导入数据测试失败", e);
        }
    }

    /**
     * 处理数据，填充固定资产类别
     */
    private void processData(List<MinUsagePeriodImportVo> dataList) {
        String currentCategory = null;
        
        for (MinUsagePeriodImportVo data : dataList) {
            // 如果当前行有固定资产类别，更新当前类别
            if (data.getCategory() != null && !data.getCategory().trim().isEmpty()) {
                currentCategory = data.getCategory().trim();
            }
            
            // 如果当前行没有固定资产类别，使用当前类别
            if (data.getCategory() == null || data.getCategory().trim().isEmpty()) {
                data.setCategory(currentCategory);
            }
        }
    }

    /**
     * 转换为实体对象
     */
    private MinUsagePeriod convertToEntity(MinUsagePeriodImportVo importVo) {
        MinUsagePeriod entity = new MinUsagePeriod();
        entity.setCategory(importVo.getCategory());
        entity.setContent(importVo.getContent());
        entity.setMinYears(importVo.getMinYears());
        entity.setGbCode(importVo.getGbCode());
        return entity;
    }

    /**
     * 验证数据库中的数据
     */
    private void validateDatabaseData(List<MinUsagePeriod> dbData) {
        log.info("   数据验证结果:");
        log.info("   - 总记录数: {}", dbData.size());
        
        // 统计各类别的数量
        long categoryCount = dbData.stream()
            .filter(data -> data.getCategory() != null && !data.getCategory().trim().isEmpty())
            .count();
        log.info("   - 有固定资产类别的记录: {}/{}", categoryCount, dbData.size());
        
        // 统计各年限的数量
        long yearsCount = dbData.stream()
            .filter(data -> data.getMinYears() != null)
            .count();
        log.info("   - 有最低使用年限的记录: {}/{}", yearsCount, dbData.size());
        
        // 统计各国标代码的数量
        long gbCodeCount = dbData.stream()
            .filter(data -> data.getGbCode() != null && !data.getGbCode().trim().isEmpty())
            .count();
        log.info("   - 有国标代码的记录: {}/{}", gbCodeCount, dbData.size());
        
        // 检查重复的国标代码
        long uniqueGbCodeCount = dbData.stream()
            .filter(data -> data.getGbCode() != null && !data.getGbCode().trim().isEmpty())
            .map(MinUsagePeriod::getGbCode)
            .distinct()
            .count();
        log.info("   - 唯一国标代码数量: {}", uniqueGbCodeCount);
        
        if (uniqueGbCodeCount != gbCodeCount) {
            log.warn("   - 发现重复的国标代码！");
        } else {
            log.info("   - 国标代码唯一性检查通过");
        }
        
        // 数据质量评估
        if (categoryCount == dbData.size() && 
            yearsCount == dbData.size() && 
            gbCodeCount == dbData.size()) {
            log.info("   - 数据质量: 优秀 (所有字段都完整)");
        } else if (yearsCount == dbData.size() && gbCodeCount == dbData.size()) {
            log.info("   - 数据质量: 良好 (核心字段完整)");
        } else {
            log.warn("   - 数据质量: 需要改进 (存在缺失字段)");
        }
    }
}
