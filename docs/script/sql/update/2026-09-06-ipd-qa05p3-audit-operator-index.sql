-- QA-05-P3 本体：audit_logs 加 (operator_id, seq) 覆盖索引
-- 根因：listByOperatorIds 的 `WHERE operator_id IN (...) ORDER BY seq DESC LIMIT m,n`
--   只有 uk_audit_seq (seq) 单列唯一索引——与 IN 谓词不可复合 → filesort + 深页 OFFSET 扫描，
--   50 万行 100 并发 p95=1640ms（超判定线 1500ms 约 9%）。
-- 配套代码改动：AuditLogService.listByOperatorIds 游标分页重载（seq < beforeSeq 降序窗口）+
--   导出硬上限 5 万行（PERF-AUD P2-2 防全量物化 OOM）。
-- 索引收益：operator_id 等值 IN + seq 降序反向扫描（MySQL 8 不支持 DESC 索引但反向扫描 OK），
--   游标模式彻底消除 OFFSET。
-- 幂等：重复执行安全（显式 information_schema 存在性检查，同 2026-09-05-ipd-stage-action-project-code-index.sql 模式）

SET @idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'audit_logs'
      AND index_name = 'idx_al_operator_seq'
);
SET @sql := IF(@idx_exists = 0,
    'CREATE INDEX idx_al_operator_seq ON audit_logs(operator_id, seq)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 执行后校验（期望返回 idx_al_operator_seq 两列）
SELECT index_name, column_name, seq_in_index
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'audit_logs'
  AND index_name = 'idx_al_operator_seq'
ORDER BY seq_in_index;

-- ROLLBACK（环境异常回滚用）
-- DROP INDEX idx_al_operator_seq ON audit_logs;
