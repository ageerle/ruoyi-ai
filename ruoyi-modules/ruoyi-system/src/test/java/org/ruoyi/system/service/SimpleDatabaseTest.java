package org.ruoyi.system.service;

import com.alibaba.excel.EasyExcel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.ruoyi.system.domain.MinUsagePeriod;
import org.ruoyi.system.domain.vo.MinUsagePeriodImportVo;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

/**
 * 简单数据库测试（不依赖Spring Boot）
 *
 * @author cass
 * @date 2025-09-24
 */
@Slf4j
public class SimpleDatabaseTest {

    /**
     * Excel文件路径
     */
    private static final String EXCEL_FILE_PATH = "E:/z. WorkSpace/ruoyi-ai/workspace/教育部直属高校固定资产最低使用年限表.xlsx";

    /**
     * 数据库连接信息
     */
    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/ruoyi-ai?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "666666";

    /**
     * 测试Excel数据导入到数据库
     */
    @Test
    public void testExcelImportToDatabase() {
        try {
            log.info("=== 开始Excel数据导入到数据库测试 ===");
            
            // 1. 连接数据库
            log.info("1. 连接数据库...");
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD)) {
                log.info("   数据库连接成功");
                
                // 2. 创建表（如果不存在）
                log.info("2. 创建表（如果不存在）...");
                createTableIfNotExists(connection);
                log.info("   表创建完成");
                
                // 3. 清空测试数据
                log.info("3. 清空测试数据...");
                clearTestData(connection);
                log.info("   清空完成");
                
                // 4. 读取Excel文件
                log.info("4. 读取Excel文件...");
                List<MinUsagePeriodImportVo> dataList = readExcelFile();
                log.info("   读取到 {} 条数据", dataList.size());
                
                // 5. 处理数据（填充固定资产类别）
                log.info("5. 处理数据...");
                processData(dataList);
                log.info("   数据处理完成");
                
                // 6. 批量插入数据库
                log.info("6. 批量插入数据库...");
                int successCount = insertDataToDatabase(connection, dataList);
                log.info("   插入完成: 成功 {} 条", successCount);
                
                // 7. 验证数据库中的数据
                log.info("7. 验证数据库中的数据...");
                int dbCount = getDatabaseRecordCount(connection);
                log.info("   数据库中共有 {} 条记录", dbCount);
                
                // 8. 显示部分数据
                log.info("8. 显示部分数据...");
                showSampleData(connection);
                
            }
            
            log.info("=== Excel数据导入到数据库测试完成 ===");
            
        } catch (Exception e) {
            log.error("Excel数据导入到数据库测试失败", e);
        }
    }

    /**
     * 创建表（如果不存在）
     */
    private void createTableIfNotExists(Connection connection) throws Exception {
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS `min_usage_period` (
              `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
              `category` varchar(100) NOT NULL COMMENT '固定资产类别',
              `content` varchar(200) NOT NULL COMMENT '内容',
              `min_years` int(11) NOT NULL COMMENT '最低使用年限（年）',
              `gb_code` varchar(20) NOT NULL COMMENT '国标代码',
              `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
              `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
              `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
              `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
              `remark` varchar(500) DEFAULT NULL COMMENT '备注',
              PRIMARY KEY (`id`),
              UNIQUE KEY `uk_gb_code` (`gb_code`),
              KEY `idx_category` (`category`),
              KEY `idx_content` (`content`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='最低使用年限表'
            """;
        
        try (Statement statement = connection.createStatement()) {
            statement.execute(createTableSql);
        }
    }

    /**
     * 清空测试数据
     */
    private void clearTestData(Connection connection) throws Exception {
        String deleteSql = "DELETE FROM min_usage_period";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(deleteSql);
        }
    }

    /**
     * 读取Excel文件
     */
    private List<MinUsagePeriodImportVo> readExcelFile() throws Exception {
        File excelFile = new File(EXCEL_FILE_PATH);
        if (!excelFile.exists()) {
            throw new RuntimeException("Excel文件不存在: " + EXCEL_FILE_PATH);
        }
        
        try (InputStream inputStream = new FileInputStream(excelFile)) {
            return EasyExcel.read(inputStream)
                .head(MinUsagePeriodImportVo.class)
                .sheet()
                .doReadSync();
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
     * 插入数据到数据库
     */
    private int insertDataToDatabase(Connection connection, List<MinUsagePeriodImportVo> dataList) throws Exception {
        String insertSql = "INSERT INTO min_usage_period (category, content, min_years, gb_code) VALUES (?, ?, ?, ?)";
        
        int successCount = 0;
        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            for (MinUsagePeriodImportVo data : dataList) {
                try {
                    statement.setString(1, data.getCategory());
                    statement.setString(2, data.getContent());
                    statement.setInt(3, data.getMinYears());
                    statement.setString(4, data.getGbCode());
                    
                    statement.executeUpdate();
                    successCount++;
                    
                    if (successCount <= 5) {
                        log.info("   插入成功: {} - {}", data.getContent(), data.getGbCode());
                    }
                } catch (Exception e) {
                    log.error("   插入失败: {} - {}", data.getContent(), data.getGbCode(), e);
                }
            }
        }
        
        return successCount;
    }

    /**
     * 获取数据库记录数
     */
    private int getDatabaseRecordCount(Connection connection) throws Exception {
        String countSql = "SELECT COUNT(*) FROM min_usage_period";
        try (Statement statement = connection.createStatement()) {
            var resultSet = statement.executeQuery(countSql);
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }
        return 0;
    }

    /**
     * 显示部分数据
     */
    private void showSampleData(Connection connection) throws Exception {
        String selectSql = "SELECT * FROM min_usage_period LIMIT 10";
        try (Statement statement = connection.createStatement()) {
            var resultSet = statement.executeQuery(selectSql);
            int count = 0;
            while (resultSet.next() && count < 10) {
                count++;
                String category = resultSet.getString("category");
                String content = resultSet.getString("content");
                int minYears = resultSet.getInt("min_years");
                String gbCode = resultSet.getString("gb_code");
                
                log.info("   第{}条: 类别={}, 内容={}, 年限={}, 国标代码={}", 
                    count, category, content, minYears, gbCode);
            }
        }
    }
}
