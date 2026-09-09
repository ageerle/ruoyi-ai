-- 2026-09-08-ipd-ga-nullable-decision.sql
-- WB-17-1 Gate 仲裁三层缺口修复①（配套 Java：openArbitration 预落待裁行 + arbitrate UPDATE 语义）：
-- gate_arbitrations.decision NOT NULL 封死「未裁 = decision IS NULL」查询语义（未裁中间态无法落库）。
-- 对齐 gate_reviews 真库范式（decision DEFAULT NULL + 预建占位行 + IS NULL 查待办）。
-- 存量 3 行均非 NULL，无需回填。
-- 幂等：IS_NULLABLE 守卫，已可空则跳过。

SET @col_nullable := (
  SELECT IS_NULLABLE FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'gate_arbitrations' AND COLUMN_NAME = 'decision'
);
SET @sql := IF(@col_nullable = 'NO',
  'ALTER TABLE gate_arbitrations MODIFY COLUMN decision varchar(16) NULL COMMENT ''APPROVE|REJECT（NULL=待裁，开仲裁时预落行）''',
  'SELECT ''decision already nullable, skipped'' AS note');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 回读校验（apply 后必须 fresh 核验，「SQL 已执行」不等于「约束已生效」）
SELECT TABLE_NAME, COLUMN_NAME, IS_NULLABLE, COLUMN_TYPE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'gate_arbitrations' AND COLUMN_NAME = 'decision';
