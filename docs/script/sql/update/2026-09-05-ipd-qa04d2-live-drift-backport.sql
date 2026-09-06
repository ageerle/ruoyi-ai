-- =====================================================================
-- QA-04-D2③ 线上库漂移列回写：ipd_dev 存在、仓库 DDL 缺失的 3 列
--   audit_logs.hash_version        int           NULL  'NULL=legacy-v1,2=canonical-json-v2'
--   system_configs.validation_rule json          NULL
--   system_configs.source_ref      varchar(1024) NULL
-- 定义来源：ipd_dev 线上库 SHOW CREATE TABLE（2026-09-05 实测，root 只读会话），
--   按线上真实形态逐字回写，新库执行本脚本后 schema 与线上一致。
-- 幂等：每列先查 information_schema.columns——
--   * 基线 DDL（2026-09-04-ipd-p0-tables.sql）已同步回写 3 列，新库建表即含列，
--     本脚本在其上重放必须零报错；
--   * 存量库（列缺失）按 AFTER 锚点补列，与线上列位一致。
-- 范围：仅 DDL。不在 ipd_dev 线上库执行（该库已含 3 列，无需变更）。
-- 注：线上 system_configs.config_value / default_value 实为 text（仓库 DDL varchar(500)），
--   属列类型漂移，超出本卡列集范围，未在本脚本处理——已登记 owner 裁决项（见验收报告）。
-- 日期：2026-09-05
-- =====================================================================

-- 1) audit_logs.hash_version（审计 hash 链算法版本，AFTER create_time 与线上一致）
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'audit_logs'
        AND column_name = 'hash_version') = 0,
    'ALTER TABLE audit_logs ADD COLUMN hash_version int NULL COMMENT ''NULL=legacy-v1,2=canonical-json-v2'' AFTER create_time',
    'SELECT 1 AS skip_audit_logs_hash_version');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) system_configs.validation_rule（参数校验规则，AFTER remark 与线上一致）
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'system_configs'
        AND column_name = 'validation_rule') = 0,
    'ALTER TABLE system_configs ADD COLUMN validation_rule json NULL COMMENT ''校验规则（QA-04-D2 回写线上形态）'' AFTER remark',
    'SELECT 1 AS skip_system_configs_validation_rule');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) system_configs.source_ref（参数来源引用，AFTER validation_rule 与线上一致）
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'system_configs'
        AND column_name = 'source_ref') = 0,
    'ALTER TABLE system_configs ADD COLUMN source_ref varchar(1024) NULL COMMENT ''来源引用（QA-04-D2 回写线上形态）'' AFTER validation_rule',
    'SELECT 1 AS skip_system_configs_source_ref');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
