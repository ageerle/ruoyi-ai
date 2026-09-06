-- =====================================================================
-- SEC-REV round 3 安全修复 DDL（2026-09-06）
-- 范围：Bug#9 unique key + Bug#10 tenant-isolation
-- 涉及表：contributions, negative_feedbacks
-- 幂等：IF NOT EXISTS + INFORMATION_SCHEMA 检查
-- apply：mysql-migrator → ipd_dev（生产环境需 owner 手动 review）
-- 来源：本会话累计触发的 10 项未覆盖安全发现
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. contributions 表：补 uk_contrib_project 唯一键 + version_no 版本列
--    SEC-REV-round3 Bug#9（中危 data-integrity-stale-record）：
--    DDL 缺 UNIQUE KEY uk_contrib_project（project_id）—— 同一项目可被并发插入多条贡献度评定，
--    旧记录与新记录版本号无差异，无法区分「最新一条」与「历史回写」。
--    修复：加唯一键（防并发重复 init）+ version_no（乐观锁基础）。
-- ---------------------------------------------------------------------

-- 1a. version_no 列（乐观锁基础；缺省 1，旧数据回填 1）
ALTER TABLE `contributions`
  ADD COLUMN IF NOT EXISTS `version_no` INT NOT NULL DEFAULT 1
    COMMENT '乐观锁版本号（DEFAULT 1；UPDATE 时 +1 用于并发冲突检测）'
    AFTER `del_flag`;

-- 1b. uk_contrib_project 唯一键
SET @dbname = DATABASE();
SET @exists := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @dbname
    AND TABLE_NAME = 'contributions'
    AND INDEX_NAME = 'uk_contrib_project'
);
SET @sql := IF(@exists = 0,
  'ALTER TABLE `contributions` ADD UNIQUE KEY `uk_contrib_project` (`project_id`)',
  'SELECT "uk_contrib_project already exists" AS info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 2. negative_feedbacks 表：tenant_id NOT NULL 强化（Bug#10 同步修复）
--    SEC-REV-round3 Bug#10（中危 multi-tenant-data-isolation）：
--    DDL tenant_id VARCHAR(20) NULL DEFAULT '000000' —— NULL 租户行会被 MyBatis-Plus
--    多租户过滤器误判为「跨租户空串」而被错误归类；缺显式 NOT NULL 约束时，mapper
--    .eq(tenantId, ...) 也无法保证查到非空记录。
--    修复：tenant_id NOT NULL DEFAULT '000000'（业务显式默认）+ service/Mapper 显式 .eq(tenantId, ...)。
--    （注：Bug#10 主要修复在 service/Mapper，DDL 此处仅做 NOT NULL 加固，作为防御纵深）
-- ---------------------------------------------------------------------

-- 2a. tenant_id NOT NULL（兼容历史数据：先把 NULL 改为 '000000'，再加约束）
UPDATE `negative_feedbacks` SET `tenant_id` = '000000' WHERE `tenant_id` IS NULL;

ALTER TABLE `negative_feedbacks`
  MODIFY COLUMN `tenant_id` VARCHAR(20) NOT NULL DEFAULT '000000'
    COMMENT '租户 ID（多租户隔离；DEFAULT 000000 兼容存量）';

-- ---------------------------------------------------------------------
-- 3. 验证（部署后由 mysql-migrator 自动跑；人工验收时执行）
-- ---------------------------------------------------------------------

-- 3a. 校验 uk_contrib_project 唯一键存在
SELECT INDEX_NAME, COLUMN_NAME, NON_UNIQUE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'contributions'
  AND INDEX_NAME = 'uk_contrib_project';

-- 3b. 校验 version_no 列存在且默认值 1
SELECT COLUMN_NAME, COLUMN_DEFAULT, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'contributions'
  AND COLUMN_NAME = 'version_no';

-- 3c. 校验 tenant_id NOT NULL
SELECT COLUMN_NAME, COLUMN_DEFAULT, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'negative_feedbacks'
  AND COLUMN_NAME = 'tenant_id';

-- =====================================================================
-- END OF SEC-REV round 3 DDL
-- =====================================================================