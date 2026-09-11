-- =====================================================================
-- P3-8.2 负反馈停发减半与奖金资格联动（BR-INC-10 / AC-INC-36b/37/38/39/40）
-- 既有 negative_feedbacks 表 12 列已铺（batch_missing_tables.sql L74-92），
-- 本 ALTER 补齐 BR-INC-10 业务字段：trigger_type / 主责连带映射 / 执行动作 / 状态机列
-- 同步新增去重唯一索引 uk_nf_project_trigger_active（同项目同 triggerType 唯一，
--   防 AC-INC-40 重复不重复扣减）
-- 同步新增 idx_nf_status（按状态筛选用）
-- 同步登记租户共享表（不在本仓范围）
-- 幂等：先 INFORMATION_SCHEMA 检查再 ALTER；IF NOT EXISTS（MySQL 8 支持）
-- apply：mysql-migrator → ipd_dev
-- 日期：2026-09-06；卡 P3-8.2
-- =====================================================================

-- 1. 补 trigger_type（必填）—— 四种触发情形之一
ALTER TABLE `negative_feedbacks`
  ADD COLUMN IF NOT EXISTS `trigger_type` varchar(32) NOT NULL
    COMMENT 'REWORK_EXCEEDED|QUALITY_ACCIDENT|SPEC_PILE_COPY|MISSED_MARKET_WINDOW（BR-INC-10）'
    AFTER `project_id`;

-- 2. 主责方（MARKET_PM / RD_PM / BOTH）
ALTER TABLE `negative_feedbacks`
  ADD COLUMN IF NOT EXISTS `main_role` varchar(16) NOT NULL DEFAULT 'MARKET_PM'
    COMMENT '主责方 MARKET_PM|RD_PM|BOTH（MISSED_MARKET_WINDOW=BOTH）'
    AFTER `trigger_type`;

ALTER TABLE `negative_feedbacks`
  ADD COLUMN IF NOT EXISTS `main_person_id` bigint DEFAULT NULL
    COMMENT '主责人 person.id（BOTH 时也填主记录人；AC-INC-40 审计要求）'
    AFTER `main_role`;

-- 3. 连带方（可空：MISSED_MARKET_WINDOW 无主责/连带区分）
ALTER TABLE `negative_feedbacks`
  ADD COLUMN IF NOT EXISTS `related_role` varchar(16) DEFAULT NULL
    COMMENT '连带方 MARKET_PM|RD_PM（仅 REWORK/QUALITY/SPEC 三种触发）'
    AFTER `main_person_id`;

ALTER TABLE `negative_feedbacks`
  ADD COLUMN IF NOT EXISTS `related_person_id` bigint DEFAULT NULL
    COMMENT '连带人 person.id'
    AFTER `related_role`;

-- 4. 执行动作（BR-INC-10）
ALTER TABLE `negative_feedbacks`
  ADD COLUMN IF NOT EXISTS `main_execution` varchar(32) NOT NULL DEFAULT 'STOP_ALLOWANCE'
    COMMENT '主责方执行：STOP_ALLOWANCE|HALVE_ALLOWANCE|BONUS_DOWNGRADE|BONUS_DISQUALIFY'
    AFTER `related_person_id`;

ALTER TABLE `negative_feedbacks`
  ADD COLUMN IF NOT EXISTS `related_execution` varchar(32) DEFAULT NULL
    COMMENT '连带方执行：HALVE_ALLOWANCE 等'
    AFTER `main_execution`;

-- 5. 奖金资格影响（AC-INC-39）
ALTER TABLE `negative_feedbacks`
  ADD COLUMN IF NOT EXISTS `bonus_disqualify` tinyint(1) NOT NULL DEFAULT 0
    COMMENT '是否取消分配资格（0=否 1=是，AC-INC-39）'
    AFTER `related_execution`;

ALTER TABLE `negative_feedbacks`
  ADD COLUMN IF NOT EXISTS `tier_delta` decimal(5,2) NOT NULL DEFAULT 0
    COMMENT '贡献度系数降低值（默认 -0.5 → BR-INC-10）'
    AFTER `bonus_disqualify`;

-- 6. 月份（生效月 + 解除月）
ALTER TABLE `negative_feedbacks`
  ADD COLUMN IF NOT EXISTS `trigger_month` varchar(7) DEFAULT NULL
    COMMENT '生效月份 YYYY-MM'
    AFTER `tier_delta`;

ALTER TABLE `negative_feedbacks`
  ADD COLUMN IF NOT EXISTS `recovery_month` varchar(7) DEFAULT NULL
    COMMENT '解除月份 YYYY-MM（NULL=未解除）'
    AFTER `trigger_month`;

-- 7. 状态机列（与既有 status 字段语义升级：原 status=OPEN/INVESTIGATING/RESOLVED/CLOSED，
--    升级为 DRAFT/PENDING_DECISION/EXECUTED/LIFTED/REJECTED）
-- 既有 status 字段已存在，本 ALTER 不动列名，仅靠 DEFAULT 'DRAFT' 在新 INSERT 时生效；
-- 旧数据迁移由 service 层做幂等兼容（见 P382MigrationCompatTest）
-- ALTER TABLE `negative_feedbacks` MODIFY COLUMN `status` varchar(16) NOT NULL DEFAULT 'DRAFT'
--   COMMENT 'DRAFT|PENDING_DECISION|EXECUTED|LIFTED|REJECTED（AC-INC-40）';

-- 8. 审计落名（操作人 + 时间）
ALTER TABLE `negative_feedbacks`
  ADD COLUMN IF NOT EXISTS `triggered_by` bigint DEFAULT NULL
    COMMENT '录入人 person.id（AC-INC-40）'
    AFTER `status`;

ALTER TABLE `negative_feedbacks`
  ADD COLUMN IF NOT EXISTS `decided_by` bigint DEFAULT NULL
    COMMENT '认定人（GROUP_LEADER / SUPER_ADMIN）'
    AFTER `triggered_by`;

ALTER TABLE `negative_feedbacks`
  ADD COLUMN IF NOT EXISTS `decided_at` datetime DEFAULT NULL
    COMMENT '认定时间'
    AFTER `decided_by`;

ALTER TABLE `negative_feedbacks`
  ADD COLUMN IF NOT EXISTS `lifted_by` bigint DEFAULT NULL
    COMMENT '解除人'
    AFTER `decided_at`;

ALTER TABLE `negative_feedbacks`
  ADD COLUMN IF NOT EXISTS `lifted_at` datetime DEFAULT NULL
    COMMENT '解除时间'
    AFTER `lifted_by`;

ALTER TABLE `negative_feedbacks`
  ADD COLUMN IF NOT EXISTS `decision_comment` varchar(500) DEFAULT NULL
    COMMENT '认定/解除备注（≤500）'
    AFTER `lifted_at`;

-- 9. 去重唯一索引（AC-INC-40：同项目同 triggerType 唯一）
-- 注意：del_flag='0' 部分索引——MySQL 8 支持函数索引；此处用普通复合唯一键，service 层 del_flag='0' 二次校验
SET @idx_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'negative_feedbacks'
    AND INDEX_NAME = 'uk_nf_project_trigger_active'
);
SET @sql = IF(@idx_exists = 0,
  'ALTER TABLE `negative_feedbacks` ADD UNIQUE KEY `uk_nf_project_trigger_active`
    (`project_id`, `trigger_type`, `tenant_id`, `del_flag`)
    COMMENT ''AC-INC-40 同项目同 triggerType 唯一''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 10. 状态查询索引
SET @idx_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'negative_feedbacks'
    AND INDEX_NAME = 'idx_nf_status'
);
SET @sql = IF(@idx_exists = 0,
  'ALTER TABLE `negative_feedbacks` ADD KEY `idx_nf_status` (`status`)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;