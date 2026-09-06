-- PERF-P0-1：deletion_requests 加 (status, leader_due_at) 联合索引
-- 配合 DeletionRequestService.escalateOverdueLeaderReview 批量化改造（单 SQL 条件 UPDATE）：
--   升级扫描谓词 status='LEADER_REVIEW' AND leader_due_at<now 由本索引覆盖，
--   消除定时扫描全表扫；listOverdueAdminReview 的 (status, admin_due_at) 谓词
--   同样受益于本索引前缀（status 等值 + 范围列不同，至少避免全表）。
-- 幂等：重复执行安全（显式 information_schema 存在性检查，同 2026-09-05-ipd-stage-action-project-code-index.sql 模式）

SET @idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'deletion_requests'
      AND index_name = 'idx_del_status_leader'
);
SET @sql := IF(@idx_exists = 0,
    'CREATE INDEX idx_del_status_leader ON deletion_requests(status, leader_due_at)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 执行后校验（期望返回 idx_del_status_leader 两列）
SELECT index_name, column_name, seq_in_index
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'deletion_requests'
  AND index_name = 'idx_del_status_leader'
ORDER BY seq_in_index;

-- ROLLBACK（环境异常回滚用）
-- DROP INDEX idx_del_status_leader ON deletion_requests;
