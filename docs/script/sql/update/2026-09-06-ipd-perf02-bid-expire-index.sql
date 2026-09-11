-- PERF-P0-2：bid_invitations 加 (status, expire_at) 联合索引
-- 配合 BidInvitationService.expireOverdue 批量化改造（单 SQL 条件 UPDATE）：
--   过期扫描谓词 status='OPEN' AND expire_at<now 由本索引覆盖，
--   消除定时扫描全表扫。
-- 幂等：重复执行安全（显式 information_schema 存在性检查，同 2026-09-06-ipd-perf01-deletion-escalate-index.sql 模式）

SET @idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'bid_invitations'
      AND index_name = 'idx_bi_status_expire'
);
SET @sql := IF(@idx_exists = 0,
    'CREATE INDEX idx_bi_status_expire ON bid_invitations(status, expire_at)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 执行后校验（期望返回 idx_bi_status_expire 两列）
SELECT index_name, column_name, seq_in_index
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'bid_invitations'
  AND index_name = 'idx_bi_status_expire'
ORDER BY seq_in_index;

-- ROLLBACK（环境异常回滚用）
-- DROP INDEX idx_bi_status_expire ON bid_invitations;
