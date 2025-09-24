package org.ruoyi.system.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.ruoyi.system.mapper.MinUsagePeriodMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * 数据库初始化测试
 *
 * @author cass
 * @date 2025-09-24
 */
@Slf4j
@SpringBootTest(classes = {org.ruoyi.RuoYiAIApplication.class})
@ActiveProfiles("test")
public class DatabaseInitTest {

    @Autowired
    private MinUsagePeriodMapper minUsagePeriodMapper;

    /**
     * 测试数据库连接和表创建
     */
    @Test
    public void testDatabaseConnectionAndTableCreation() {
        try {
            log.info("=== 开始数据库连接和表创建测试 ===");
            
            // 1. 测试数据库连接
            log.info("1. 测试数据库连接...");
            String url = "jdbc:mysql://127.0.0.1:3306/ruoyi-ai?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8";
            String username = "root";
            String password = "666666";
            
            try (Connection connection = DriverManager.getConnection(url, username, password)) {
                log.info("   数据库连接成功");
                
                // 2. 检查表是否存在
                log.info("2. 检查min_usage_period表是否存在...");
                boolean tableExists = checkTableExists(connection);
                
                if (!tableExists) {
                    log.info("   表不存在，开始创建表...");
                    createTable(connection);
                    log.info("   表创建完成");
                } else {
                    log.info("   表已存在");
                }
                
                // 3. 测试Mapper
                log.info("3. 测试Mapper...");
                try {
                    Long count = minUsagePeriodMapper.selectCount(null);
                    log.info("   当前表中有 {} 条记录", count);
                } catch (Exception e) {
                    log.error("   Mapper测试失败", e);
                }
                
            }
            
            log.info("=== 数据库连接和表创建测试完成 ===");
            
        } catch (Exception e) {
            log.error("数据库连接和表创建测试失败", e);
        }
    }

    /**
     * 检查表是否存在
     */
    private boolean checkTableExists(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            statement.executeQuery("SELECT 1 FROM min_usage_period LIMIT 1");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 创建表
     */
    private void createTable(Connection connection) {
        String createTableSql = """
            CREATE TABLE `min_usage_period` (
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
}
