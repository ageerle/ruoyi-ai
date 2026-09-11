-- P3-1.2 共担 KPI 归集追加版本
-- 幂等：存量/新库均不因重复执行报 1060/1061/1022。
-- 约束：kpi_records.shared_detail 为 JSON，源码以 AuditEventData.json 写合法 JSON。

SET @column_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'kpi_records'
    AND COLUMN_NAME = 'revision'
);
SET @sql := IF(@column_exists = 0,
  'ALTER TABLE kpi_records ADD COLUMN revision INT NOT NULL DEFAULT 1 COMMENT ''归集追加版本（P3-1.2）'' AFTER segment',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'kpi_records'
    AND INDEX_NAME = 'uk_kpi_project_person_type_period_revision'
);
SET @sql := IF(@index_exists = 0,
  'ALTER TABLE kpi_records ADD UNIQUE KEY uk_kpi_project_person_type_period_revision (project_id, person_id, kpi_type, period, revision)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 回滚（人工维护脚本，不自动执行）：
-- ALTER TABLE kpi_records DROP INDEX uk_kpi_project_person_type_period_revision;
-- ALTER TABLE kpi_records DROP COLUMN revision;
