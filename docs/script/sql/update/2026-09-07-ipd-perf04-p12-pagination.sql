-- =====================================================================
-- PERF-P0-4 + PERF-P1-2：bid_responses 应标记录索引 + 分页重构
--
-- PERF-P0-4：BidResponseService.listByRdPm 当前不带分页 + 无 (rd_pm_id, create_time) 索引，
--           单研发PM 应标量过百即全表扫。复合索引 (rd_pm_id, create_time DESC) 让 ORDER BY
--           走索引直接排序，避免 filesort。
--
-- PERF-P1-2：BidInvitationService.listResponses 当前 selectList 全拉，应标量过千即内存炸。
--           既已存在 idx_br_invitation(invitation_id) 覆盖等值过滤；分页层在 service 改造，
--           DDL 不再加新索引（避免索引爆炸）。
--
-- 幂等：information_schema 存在性检查 + 条件 DDL，重复执行安全
--       （同 2026-09-06-ipd-perf02-bid-expire-index.sql 模式）
-- 依赖：bid_responses.idx_br_invitation 已存在（TS-05 DDL 2026-09-04-ipd-p0-tables.sql:431）
-- 日期：2026-09-07
-- =====================================================================

-- ---------------------------------------------------------------------
-- PERF-P0-4：bid_responses 加 (rd_pm_id, create_time) 联合索引
-- 配合 BidResponseService.listByRdPmPaged 分页重构：
--   查询谓词 rd_pm_id=? 走索引等值；ORDER BY create_time DESC 走索引第二列避免 filesort；
--   LIMIT (pageNo, pageSize) 让 MySQL 提前停止扫描。
-- ---------------------------------------------------------------------
SET @idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'bid_responses'
      AND index_name = 'idx_br_rd_pm'
);
SET @sql := IF(@idx_exists = 0,
    'CREATE INDEX idx_br_rd_pm ON bid_responses(rd_pm_id, create_time)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 执行后校验（期望返回 idx_br_rd_pm 两列）
SELECT index_name, column_name, seq_in_index
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'bid_responses'
  AND index_name = 'idx_br_rd_pm'
ORDER BY seq_in_index;

-- PERF-P1-2：listResponses 复用既有 idx_br_invitation，不再加新索引。
-- 校验既有索引仍生效（期望返回 idx_br_invitation 单列）
SELECT index_name, column_name, seq_in_index
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'bid_responses'
  AND index_name = 'idx_br_invitation'
ORDER BY seq_in_index;

-- ROLLBACK（环境异常回滚用）
-- DROP INDEX idx_br_rd_pm ON bid_responses;