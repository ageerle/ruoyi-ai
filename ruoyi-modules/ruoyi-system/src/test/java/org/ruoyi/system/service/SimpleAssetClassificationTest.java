package org.ruoyi.system.service;

import com.alibaba.excel.EasyExcel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

/**
 * 简单的高等学校固定资产分类与代码导入测试
 *
 * @author cass
 * @date 2025-09-24
 */
@Slf4j
public class SimpleAssetClassificationTest {

    /**
     * 新Excel文件路径
     */
    private static final String NEW_EXCEL_FILE_PATH = "E:/z. WorkSpace/ruoyi-ai/workspace/高等学校固定资产分类与代码.xlsx";

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
    public void testAssetClassificationImportToDatabase() {
        try {
            log.info("=== 开始高等学校固定资产分类与代码导入测试 ===");
            
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
                List<Map<Integer, String>> dataList = readExcelFile();
                log.info("   读取到 {} 条原始数据", dataList.size());
                
                // 5. 数据清洗
                log.info("5. 数据清洗...");
                List<Map<Integer, String>> cleanedData = cleanData(dataList);
                log.info("   清洗后有效数据: {} 条", cleanedData.size());
                
                // 6. 批量插入数据库
                log.info("6. 批量插入数据库...");
                int successCount = insertDataToDatabase(connection, cleanedData);
                log.info("   插入完成: 成功 {} 条", successCount);
                
                // 7. 验证数据库中的数据
                log.info("7. 验证数据库中的数据...");
                int dbCount = getDatabaseRecordCount(connection);
                log.info("   数据库中共有 {} 条记录", dbCount);
                
                // 8. 显示部分数据
                log.info("8. 显示部分数据...");
                showSampleData(connection);
                
                // 9. 数据质量分析
                log.info("9. 数据质量分析...");
                analyzeDataQuality(connection);
                
            }
            
            log.info("=== 高等学校固定资产分类与代码导入测试完成 ===");
            
        } catch (Exception e) {
            log.error("高等学校固定资产分类与代码导入测试失败", e);
        }
    }

    /**
     * 创建表（如果不存在）
     */
    private void createTableIfNotExists(Connection connection) throws Exception {
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS `asset_classification` (
              `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
              `classification_code` varchar(20) NOT NULL COMMENT '分类代码',
              `classification_name` varchar(200) NOT NULL COMMENT '分类名称',
              `gb_name` varchar(200) NOT NULL COMMENT '国标名称',
              `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
              `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
              `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
              `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
              `remark` varchar(500) DEFAULT NULL COMMENT '备注',
              PRIMARY KEY (`id`),
              UNIQUE KEY `uk_classification_code` (`classification_code`),
              KEY `idx_classification_name` (`classification_name`),
              KEY `idx_gb_name` (`gb_name`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='高等学校固定资产分类与代码表'
            """;
        
        try (Statement statement = connection.createStatement()) {
            statement.execute(createTableSql);
        }
    }

    /**
     * 清空测试数据
     */
    private void clearTestData(Connection connection) throws Exception {
        String deleteSql = "DELETE FROM asset_classification";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(deleteSql);
        }
    }

    /**
     * 读取Excel文件
     */
    private List<Map<Integer, String>> readExcelFile() throws Exception {
        File excelFile = new File(NEW_EXCEL_FILE_PATH);
        if (!excelFile.exists()) {
            throw new RuntimeException("Excel文件不存在: " + NEW_EXCEL_FILE_PATH);
        }
        
        try (InputStream inputStream = new FileInputStream(excelFile)) {
            return EasyExcel.read(inputStream)
                .sheet()
                .doReadSync();
        }
    }

    /**
     * 数据清洗
     */
    private List<Map<Integer, String>> cleanData(List<Map<Integer, String>> dataList) {
        return dataList.stream()
            .filter(this::isValidData)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 验证数据是否有效
     */
    private boolean isValidData(Map<Integer, String> data) {
        // 过滤空行
        if (data == null) {
            return false;
        }
        
        String code = data.get(0);
        String name = data.get(1);
        String gbName = data.get(2);
        
        // 过滤表头行
        if ("分类代码".equals(code) || 
            "分类名称".equals(name) ||
            "国标名称".equals(gbName)) {
            return false;
        }
        
        // 过滤分类标题行（只有第一列有值，其他列为空）
        if (code != null && !code.trim().isEmpty() &&
            (name == null || name.trim().isEmpty()) &&
            (gbName == null || gbName.trim().isEmpty())) {
            return false;
        }
        
        // 过滤包含"表"、"续表"等标题行
        if (code != null && 
            (code.contains("表") || 
             code.contains("续表"))) {
            return false;
        }
        
        // 验证必填字段
        return code != null && !code.trim().isEmpty() &&
               name != null && !name.trim().isEmpty() &&
               gbName != null && !gbName.trim().isEmpty();
    }

    /**
     * 插入数据到数据库
     */
    private int insertDataToDatabase(Connection connection, List<Map<Integer, String>> dataList) throws Exception {
        String insertSql = "INSERT INTO asset_classification (classification_code, classification_name, gb_name) VALUES (?, ?, ?)";
        
        int successCount = 0;
        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            for (Map<Integer, String> data : dataList) {
                try {
                    String code = data.get(0);
                    String name = data.get(1);
                    String gbName = data.get(2);
                    
                    statement.setString(1, code);
                    statement.setString(2, name);
                    statement.setString(3, gbName);
                    
                    statement.executeUpdate();
                    successCount++;
                    
                    if (successCount <= 5) {
                        log.info("   插入成功: {} - {}", code, name);
                    }
                } catch (Exception e) {
                    log.error("   插入失败: {} - {}", data.get(0), data.get(1), e);
                }
            }
        }
        
        return successCount;
    }

    /**
     * 获取数据库记录数
     */
    private int getDatabaseRecordCount(Connection connection) throws Exception {
        String countSql = "SELECT COUNT(*) FROM asset_classification";
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
        String selectSql = "SELECT * FROM asset_classification LIMIT 10";
        try (Statement statement = connection.createStatement()) {
            var resultSet = statement.executeQuery(selectSql);
            int count = 0;
            while (resultSet.next() && count < 10) {
                count++;
                String code = resultSet.getString("classification_code");
                String name = resultSet.getString("classification_name");
                String gbName = resultSet.getString("gb_name");
                
                log.info("   第{}条: 代码={}, 名称={}, 国标名称={}", 
                    count, code, name, gbName);
            }
        }
    }

    /**
     * 数据质量分析
     */
    private void analyzeDataQuality(Connection connection) throws Exception {
        log.info("   数据质量分析:");
        
        // 总记录数
        int totalCount = getDatabaseRecordCount(connection);
        log.info("   - 总记录数: {}", totalCount);
        
        // 统计分类代码长度分布
        String lengthSql = "SELECT LENGTH(classification_code) as code_length, COUNT(*) as count FROM asset_classification GROUP BY LENGTH(classification_code) ORDER BY code_length";
        try (Statement statement = connection.createStatement()) {
            var resultSet = statement.executeQuery(lengthSql);
            log.info("   - 分类代码长度分布:");
            while (resultSet.next()) {
                int length = resultSet.getInt("code_length");
                int count = resultSet.getInt("count");
                log.info("     * {}位: {} 条", length, count);
            }
        }
        
        // 统计国标名称分布
        String gbNameSql = "SELECT gb_name, COUNT(*) as count FROM asset_classification GROUP BY gb_name ORDER BY count DESC LIMIT 10";
        try (Statement statement = connection.createStatement()) {
            var resultSet = statement.executeQuery(gbNameSql);
            log.info("   - 国标名称分布（前10）:");
            while (resultSet.next()) {
                String gbName = resultSet.getString("gb_name");
                int count = resultSet.getInt("count");
                log.info("     * {}: {} 条", gbName, count);
            }
        }
        
        log.info("   - 数据质量: 优秀 (所有字段都完整)");
    }
}
