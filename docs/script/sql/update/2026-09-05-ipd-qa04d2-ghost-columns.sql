-- =====================================================================
-- QA-04-D2① 幻影列补齐：6 个实体字段在仓库 DDL / 线上库均缺列
--   ai_documents.reviewed_by / reviewed_at   （AiDocument.reviewedBy Long / reviewedAt Date）
--   allowance_ledgers.stop_start_date        （AllowanceLedger.stopStartDate Date）
--   bonus_pools.calculated_at / distributed_at（BonusPool.calculatedAt / distributedAt Date）
--   kpi_records.scored_at                    （KpiRecord.scoredAt Date）
-- 依据：docs/ipd-系统说明/验收/QA-04-mapping-result.json（COLUMN_NOT_IN_DDL × 6）
-- 幂等：每列先查 information_schema.columns，存在则跳过——
--   * 基线 DDL（2026-09-04-ipd-p0-tables.sql）已同步回写 6 列，新库建表即含列，
--     本脚本在其上重放必须零报错（否则与 nextcode 1061 同病）；
--   * 存量库（列缺失）按 AFTER 锚点补列，与基线列位一致。
-- 范围：仅 DDL。不在 ipd_dev 线上库执行（共享主库，加列交 owner 裁决，见验收报告）。
-- 日期：2026-09-05
-- =====================================================================

-- 1) ai_documents.reviewed_by（对齐同表 *_by 惯例：create_by bigint）
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'ai_documents'
        AND column_name = 'reviewed_by') = 0,
    'ALTER TABLE ai_documents ADD COLUMN reviewed_by bigint NULL COMMENT ''审核人ID（QA-04-D2 补列）'' AFTER version_no',
    'SELECT 1 AS skip_ai_documents_reviewed_by');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) ai_documents.reviewed_at
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'ai_documents'
        AND column_name = 'reviewed_at') = 0,
    'ALTER TABLE ai_documents ADD COLUMN reviewed_at datetime NULL COMMENT ''审核时间（QA-04-D2 补列）'' AFTER reviewed_by',
    'SELECT 1 AS skip_ai_documents_reviewed_at');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) allowance_ledgers.stop_start_date
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'allowance_ledgers'
        AND column_name = 'stop_start_date') = 0,
    'ALTER TABLE allowance_ledgers ADD COLUMN stop_start_date datetime NULL COMMENT ''停发开始日期（QA-04-D2 补列）'' AFTER stop_reason',
    'SELECT 1 AS skip_allowance_ledgers_stop_start_date');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) bonus_pools.calculated_at
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'bonus_pools'
        AND column_name = 'calculated_at') = 0,
    'ALTER TABLE bonus_pools ADD COLUMN calculated_at datetime NULL COMMENT ''计算完成时间（QA-04-D2 补列）'' AFTER status',
    'SELECT 1 AS skip_bonus_pools_calculated_at');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5) bonus_pools.distributed_at
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'bonus_pools'
        AND column_name = 'distributed_at') = 0,
    'ALTER TABLE bonus_pools ADD COLUMN distributed_at datetime NULL COMMENT ''发放时间（QA-04-D2 补列）'' AFTER calculated_at',
    'SELECT 1 AS skip_bonus_pools_distributed_at');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6) kpi_records.scored_at
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'kpi_records'
        AND column_name = 'scored_at') = 0,
    'ALTER TABLE kpi_records ADD COLUMN scored_at datetime NULL COMMENT ''评分日期（QA-04-D2 补列）'' AFTER scored_by',
    'SELECT 1 AS skip_kpi_records_scored_at');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
