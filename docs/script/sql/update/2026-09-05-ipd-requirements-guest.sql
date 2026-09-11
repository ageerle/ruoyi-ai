-- =====================================================================
-- P4-1.1 游客需求提交模型与三路产品归属（页38；BR-REQ-01/02/02b/03/04）
-- requirements 基线（2026-09-04-ipd-p0-tables.sql §15）缺页38 表单字段：
--   customer_name（客户名称，≤120）与 raw_model（用户输入原始型号，第③类记录原输入）
-- 幂等：information_schema 检查已存在则跳过；日期：2026-09-05；隔离库人工 apply
-- =====================================================================

SET @schema = DATABASE();

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'requirements' AND COLUMN_NAME = 'customer_name') = 0,
  'ALTER TABLE requirements ADD COLUMN customer_name varchar(120) null COMMENT ''客户名称（页38 customerName）'' AFTER submitter_name',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'requirements' AND COLUMN_NAME = 'raw_model') = 0,
  'ALTER TABLE requirements ADD COLUMN raw_model varchar(64) null COMMENT ''用户输入原始型号文本（其他/未找到记录原输入）'' AFTER contact',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
