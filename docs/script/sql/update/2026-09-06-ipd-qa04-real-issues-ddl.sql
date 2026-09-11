-- =====================================================================
-- QA-04 真问题清单闭环：22 COLUMN_NOT_IN_DDL 幻影列补齐
--   依据：docs/ipd-系统说明/验收/QA-04-mapping-result.json
--   A 类（DDL 漏列，entity 有）
--     gate_review_elements + sign_due_at / sign_extension_count
--     gate_reviews        + sign_due_at / sign_extension_count
--     ipd_business_config_versions + create_dept / update_by / update_time / del_flag
--     project_scores      + pm_role / self_score / market_leader_score / rd_leader_score / weighted_score / scored_at
--     requirements        + accepted_at / element_snapshot
--     requirement_changes + signatures
--     switching_acceptance + ran_at / ran_by
--   注：negative_feedbacks.trigger_evidence P3-8.2 文件未补，本卡一并补齐 ALTER；
--       project_members.handover_role/note 字段语义在 HandoverRecord（不在 project_members），本次走 entity 端删除，不动 DDL。
--   注：gates.sign_due_at / sign_extension_count 已在 P2-5.4 (2026-09-06-ipd-p254-sign-deadline-arbitration.sql) ALTER；
--       gate_reviews / gate_review_elements 同步落副本列（业务侧按需读取）。
--   幂等：每列先查 information_schema.columns，存在则跳过——
--     * 基线 DDL 已同步回写，新库建表即含列，重放零报错；
--     * 存量库（列缺失）按 AFTER 锚点补列。
--   范围：仅 DDL。共享主库加列交 owner 裁决。
--   日期：2026-09-06；卡 QA-04 REAL-ISSUES A
-- =====================================================================

-- ===== gate_review_elements =====
-- 1) sign_due_at
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'gate_review_elements'
        AND column_name = 'sign_due_at') = 0,
    'ALTER TABLE gate_review_elements ADD COLUMN sign_due_at datetime NULL COMMENT ''签署期限（BR-GATE-04，副本列；权威在 gates.sign_due_at）'' AFTER threshold_json',
    'SELECT 1 AS skip_gr_elements_sign_due_at');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) sign_extension_count
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'gate_review_elements'
        AND column_name = 'sign_extension_count') = 0,
    'ALTER TABLE gate_review_elements ADD COLUMN sign_extension_count int NOT NULL DEFAULT 0 COMMENT ''签署期限已延长次数（副本列）'' AFTER sign_due_at',
    'SELECT 1 AS skip_gr_elements_sign_extension_count');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ===== gate_reviews =====
-- 3) sign_due_at
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'gate_reviews'
        AND column_name = 'sign_due_at') = 0,
    'ALTER TABLE gate_reviews ADD COLUMN sign_due_at datetime NULL COMMENT ''签署期限（BR-GATE-04，副本列；权威在 gates.sign_due_at）'' AFTER due_at',
    'SELECT 1 AS skip_gr_reviews_sign_due_at');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) sign_extension_count
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'gate_reviews'
        AND column_name = 'sign_extension_count') = 0,
    'ALTER TABLE gate_reviews ADD COLUMN sign_extension_count int NOT NULL DEFAULT 0 COMMENT ''签署期限已延长次数（副本列）'' AFTER sign_due_at',
    'SELECT 1 AS skip_gr_reviews_sign_extension_count');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ===== ipd_business_config_versions =====
-- 5) create_dept（BaseEntity 基字段）
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'ipd_business_config_versions'
        AND column_name = 'create_dept') = 0,
    'ALTER TABLE ipd_business_config_versions ADD COLUMN create_dept bigint NULL COMMENT ''创建部门（BaseEntity）'' AFTER tenant_id',
    'SELECT 1 AS skip_ibcv_create_dept');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6) update_by（BaseEntity 基字段）
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'ipd_business_config_versions'
        AND column_name = 'update_by') = 0,
    'ALTER TABLE ipd_business_config_versions ADD COLUMN update_by bigint NULL COMMENT ''更新人（BaseEntity）'' AFTER create_time',
    'SELECT 1 AS skip_ibcv_update_by');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 7) update_time（BaseEntity 基字段）
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'ipd_business_config_versions'
        AND column_name = 'update_time') = 0,
    'ALTER TABLE ipd_business_config_versions ADD COLUMN update_time datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间（BaseEntity）'' AFTER update_by',
    'SELECT 1 AS skip_ibcv_update_time');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 8) del_flag（Entity 声明了 @TableLogic，需要该列；后续 D 类兜底）
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'ipd_business_config_versions'
        AND column_name = 'del_flag') = 0,
    'ALTER TABLE ipd_business_config_versions ADD COLUMN del_flag char(1) NULL DEFAULT ''0'' COMMENT ''软删除（0正常 1已删；@TableLogic 守卫）'' AFTER update_time',
    'SELECT 1 AS skip_ibcv_del_flag');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ===== project_scores（v2 绩效 20/40/40；v1 五维已沉淀 P3-6.x） =====
-- 9) pm_role
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'project_scores'
        AND column_name = 'pm_role') = 0,
    'ALTER TABLE project_scores ADD COLUMN pm_role varchar(16) NULL COMMENT ''PM 角色 MARKET_PM|RD_PM（v2 P3-2.2 绩效 20/40/40）'' AFTER person_id',
    'SELECT 1 AS skip_ps_pm_role');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 10) self_score
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'project_scores'
        AND column_name = 'self_score') = 0,
    'ALTER TABLE project_scores ADD COLUMN self_score decimal(5,2) NULL COMMENT ''自评 0~100（权重 20%）'' AFTER pm_role',
    'SELECT 1 AS skip_ps_self_score');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 11) market_leader_score
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'project_scores'
        AND column_name = 'market_leader_score') = 0,
    'ALTER TABLE project_scores ADD COLUMN market_leader_score decimal(5,2) NULL COMMENT ''市场组长评 0~100（权重 40%）'' AFTER self_score',
    'SELECT 1 AS skip_ps_market_leader_score');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 12) rd_leader_score
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'project_scores'
        AND column_name = 'rd_leader_score') = 0,
    'ALTER TABLE project_scores ADD COLUMN rd_leader_score decimal(5,2) NULL COMMENT ''研发组长评 0~100（权重 40%）'' AFTER market_leader_score',
    'SELECT 1 AS skip_ps_rd_leader_score');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 13) weighted_score
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'project_scores'
        AND column_name = 'weighted_score') = 0,
    'ALTER TABLE project_scores ADD COLUMN weighted_score decimal(5,2) NULL COMMENT ''加权汇总 = self×0.2 + ml×0.4 + rl×0.4'' AFTER rd_leader_score',
    'SELECT 1 AS skip_ps_weighted_score');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 14) scored_at
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'project_scores'
        AND column_name = 'scored_at') = 0,
    'ALTER TABLE project_scores ADD COLUMN scored_at datetime NULL COMMENT ''评分日期'' AFTER status',
    'SELECT 1 AS skip_ps_scored_at');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ===== requirements =====
-- 15) accepted_at
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'requirements'
        AND column_name = 'accepted_at') = 0,
    'ALTER TABLE requirements ADD COLUMN accepted_at datetime NULL COMMENT ''需求接受时间（P4-1.1 市场 PM 接受后落库）'' AFTER routed_at',
    'SELECT 1 AS skip_req_accepted_at');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 16) element_snapshot
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'requirements'
        AND column_name = 'element_snapshot') = 0,
    'ALTER TABLE requirements ADD COLUMN element_snapshot longtext NULL COMMENT ''要素快照（GateElement JSON 备份，便于审计）'' AFTER accepted_at',
    'SELECT 1 AS skip_req_element_snapshot');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ===== requirement_changes =====
-- 17) signatures（P2-6.1 双签聚合 JSON：MARKET_PM=APPROVE;RD_PM=APPROVE 形式）
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'requirement_changes'
        AND column_name = 'signatures') = 0,
    'ALTER TABLE requirement_changes ADD COLUMN signatures json NULL COMMENT ''双签签名聚合（P2-6.1；DRAFT/PENDING_SIGN 提交时冻结）'' AFTER reason',
    'SELECT 1 AS skip_rc_signatures');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ===== switching_acceptance =====
-- 18) ran_at
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'switching_acceptance'
        AND column_name = 'ran_at') = 0,
    'ALTER TABLE switching_acceptance ADD COLUMN ran_at datetime NULL COMMENT ''运行时间'' AFTER month',
    'SELECT 1 AS skip_sa_ran_at');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 19) ran_by
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'switching_acceptance'
        AND column_name = 'ran_by') = 0,
    'ALTER TABLE switching_acceptance ADD COLUMN ran_by bigint NULL COMMENT ''运行人 personId'' AFTER ran_at',
    'SELECT 1 AS skip_sa_ran_by');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ===== negative_feedbacks（P3-8.2 漏补的 trigger_evidence） =====
-- 20) trigger_evidence
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'negative_feedbacks'
        AND column_name = 'trigger_evidence') = 0,
    'ALTER TABLE negative_feedbacks ADD COLUMN trigger_evidence varchar(1000) NULL COMMENT ''触发证据描述（≤1000；QA-04-A-FIX 补列）'' AFTER recovery_month',
    'SELECT 1 AS skip_nf_trigger_evidence');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ===== gate_reviews（兄弟流新增 projectId） =====
-- 21) project_id
SET @ddl := IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'gate_reviews'
        AND column_name = 'project_id') = 0,
    'ALTER TABLE gate_reviews ADD COLUMN project_id bigint NULL COMMENT ''项目ID（§5.3 / P1：G3 评审自动创建入口 /api/v1/projects/{id}/gates?gateCode= 用；QA-04-A-FIX 补列）'' AFTER id',
    'SELECT 1 AS skip_gr_project_id');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
