-- V001__project_product_unique.sql
-- P1-1.1 (卡 key=P1-1.1) 产品↔项目 双向 1:1 唯一约束 — DB 层兜底
-- 真实生产 DB 已存在该约束（运行手册初始化阶段已加），本文件作为版本化记录；
-- 任何新部署或 OPS-02 重跑都必须保证两条唯一键同时存在。
-- 双向约束语义：同一 productId 在 projects 中仅能出现一次；同一 projectId 在 products 中仅能出现一次。
-- 幂等性保证：通过 INFORMATION_SCHEMA 检查后再决定是否建索引。

SET @unique_check_v001 := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'projects'
     AND INDEX_NAME = 'uk_projects_product'
);
SET @sql_v001 := IF(@unique_check_v001 = 0,
  'ALTER TABLE projects ADD UNIQUE KEY uk_projects_product (product_id)',
  'SELECT 1');
PREPARE stmt_v001 FROM @sql_v001;
EXECUTE stmt_v001;
DEALLOCATE PREPARE stmt_v001;

SET @unique_check_v001b := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE()
     AND TABLE_NAME = 'products'
     AND INDEX_NAME = 'uk_products_project'
);
SET @sql_v001b := IF(@unique_check_v001b = 0,
  'ALTER TABLE products ADD UNIQUE KEY uk_products_project (project_id)',
  'SELECT 1');
PREPARE stmt_v001b FROM @sql_v001b;
EXECUTE stmt_v001b;
DEALLOCATE PREPARE stmt_v001b;
