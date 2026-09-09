-- =====================================================================
-- 2026-09-08-ipd-c-batch-4-tables-draft.sql
-- 状态:已 apply（2026-09-09 owner「以上全部都要完整执行」授权，覆盖 R13 登记的待拍板项）
-- apply 勘误:原 AFTER launch_date 锚点列在真库 products 不存在（SHOW COLUMNS 实查），
--   实际按 AFTER status / retired_at 执行;下次重放请用本勘误锚点。
-- 来源:docs/ipd-系统说明/缺表4类-14问澄清-20260908.md（建议全部选甲）
-- 拍板汇总:
--   Q1=甲  Q2=乙(加 due_at 列)  Q3=甲  Q4=乙(三签)  Q5=甲
--   Q6=甲  Q7=甲              Q8=甲
--   Q9=甲  Q10=甲             Q11=甲
--   Q12=甲 Q13=甲             Q14=甲
-- 草稿与拍板偏差会触发此 SQL 升版;当前默认全部按建议落,owner 改拍后改本文件即可。
-- =====================================================================

-- =====================================================================
-- 1. gate_waivers · 阶段门禁豁免(Q3=甲仅阶段门禁 + Q4=乙三签市场组长+研发组长+超管 + Q5=甲照搬硬规则)
-- 生命周期:DRAFT → PENDING_MARKET_LEADER → PENDING_RD_LEADER → PENDING_SUPER_ADMIN → APPROVED|REJECTED
-- 不可豁免门禁:concept / lifecycle / G0(原型 final-rules.mjs:338-340,本表不强制,业务侧校验)
-- =====================================================================
CREATE TABLE IF NOT EXISTS gate_waivers (
  id                     BIGINT       NOT NULL                COMMENT '主键(雪花)',
  project_id             BIGINT       NOT NULL                COMMENT '豁免项目',
  gate_code              VARCHAR(8)   NOT NULL                COMMENT '豁免门禁 G1|G2|G3|G4|G5',
  reason                 VARCHAR(500) NOT NULL                COMMENT '豁免理由',
  proposer_id            BIGINT       NOT NULL                COMMENT '提议人(产品组长 / 项目经理)',
  proposer_role          VARCHAR(32)  NOT NULL                COMMENT '提议人角色',
  status                 VARCHAR(24)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT|PENDING_MARKET_LEADER|PENDING_RD_LEADER|PENDING_SUPER_ADMIN|APPROVED|REJECTED',
  market_leader_id       BIGINT       DEFAULT NULL            COMMENT '市场组长审批人',
  market_decision        VARCHAR(16)  DEFAULT NULL            COMMENT 'APPROVE|REJECT',
  market_decided_at      DATETIME     DEFAULT NULL            COMMENT '市场组长决策时间',
  market_opinion         VARCHAR(500) DEFAULT NULL            COMMENT '市场组长意见',
  rd_leader_id           BIGINT       DEFAULT NULL            COMMENT '研发组长审批人',
  rd_decision            VARCHAR(16)  DEFAULT NULL            COMMENT 'APPROVE|REJECT',
  rd_decided_at          DATETIME     DEFAULT NULL            COMMENT '研发组长决策时间',
  rd_opinion             VARCHAR(500) DEFAULT NULL            COMMENT '研发组长意见',
  super_admin_id         BIGINT       DEFAULT NULL            COMMENT '超管终审人',
  super_decision         VARCHAR(16)  DEFAULT NULL            COMMENT 'APPROVE|REJECT',
  super_decided_at       DATETIME     DEFAULT NULL            COMMENT '超管终审时间',
  super_opinion          VARCHAR(500) DEFAULT NULL            COMMENT '超管意见',
  joint_decision         VARCHAR(16)  DEFAULT NULL            COMMENT '三签联合决策 APPROVE|REJECT(Q4=乙联合口径)',
  due_at                 DATETIME     DEFAULT NULL            COMMENT '审批期限(Q2=乙加列,3 自然日,与 BR-GATE-04 对齐)',
  approved_at            DATETIME     DEFAULT NULL            COMMENT '生效时间',
  rejected_at            DATETIME     DEFAULT NULL            COMMENT '驳回时间',
  create_dept            BIGINT       DEFAULT NULL,
  create_by              BIGINT       DEFAULT NULL,
  create_time            DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_by              BIGINT       DEFAULT NULL,
  update_time            DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  tenant_id              VARCHAR(20)  DEFAULT '000000',
  del_flag               CHAR(1)      DEFAULT '0',
  remark                 VARCHAR(500) DEFAULT NULL,
  version                INT          NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  KEY idx_gw_project (project_id),
  KEY idx_gw_status (status),
  KEY idx_gw_due (due_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 阶段门禁豁免申请(Q3-Q5 三签联合,联合决策)';

-- =====================================================================
-- 2. rd_replacements · 研发 PM 替换申请(Q6=甲独立链 + Q7=甲双表 + Q8=甲四步链 + 等级锁定 + 原子责任转移)
-- 四步审批链:新 PM 本人确认 → 市场组长 → 研发组长 → 超管
-- =====================================================================
CREATE TABLE IF NOT EXISTS rd_replacements (
  id                     BIGINT       NOT NULL                COMMENT '主键(雪花)',
  project_id             BIGINT       NOT NULL                COMMENT '被替换 PM 所在项目',
  out_person_id          BIGINT       NOT NULL                COMMENT '被替换的研发 PM',
  out_role               VARCHAR(32)  NOT NULL DEFAULT 'RD_PM' COMMENT '固定 RD_PM(Q6=甲研发线)',
  in_person_id           BIGINT       NOT NULL                COMMENT '新研发 PM(本人确认步)',
  in_role                VARCHAR(32)  NOT NULL DEFAULT 'RD_PM' COMMENT '固定 RD_PM',
  out_level_locked       VARCHAR(8)   NOT NULL                COMMENT '被替换时等级快照(BR-INC-02 锁定)',
  out_amount_locked      DECIMAL(10,2) NOT NULL              COMMENT '被替换时月度津贴快照',
  effective_date         DATETIME     NOT NULL                COMMENT '生效日期(原子责任转移日)',
  status                 VARCHAR(24)  NOT NULL DEFAULT 'PENDING_SELF' COMMENT 'PENDING_SELF|PENDING_MARKET_LEADER|PENDING_RD_LEADER|PENDING_SUPER_ADMIN|APPROVED|REJECTED|CANCELLED',
  due_at                 DATETIME     DEFAULT NULL            COMMENT '审批期限(Q2=乙加列)',
  reason                 VARCHAR(500) NOT NULL                COMMENT '替换理由',
  out_exit_reason        VARCHAR(16)  DEFAULT NULL            COMMENT 'TRANSFER|VOLUNTARY|LOW_PERF',
  atomic_locked          CHAR(1)      NOT NULL DEFAULT '1'    COMMENT '替换期间原子责任转移锁 1锁 0解(Q8 禁双 PM 同岗)',
  create_dept            BIGINT       DEFAULT NULL,
  create_by              BIGINT       DEFAULT NULL,
  create_time            DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_by              BIGINT       DEFAULT NULL,
  update_time            DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  tenant_id              VARCHAR(20)  DEFAULT '000000',
  del_flag               CHAR(1)      DEFAULT '0',
  remark                 VARCHAR(500) DEFAULT NULL,
  version                INT          NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  KEY idx_rdr_project (project_id),
  KEY idx_rdr_out (out_person_id),
  KEY idx_rdr_in (in_person_id),
  KEY idx_rdr_status (status),
  KEY idx_rdr_due (due_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 研发 PM 替换申请(四步审批链 + 等级锁定 + 原子责任转移)';

CREATE TABLE IF NOT EXISTS rd_replacement_approvals (
  id                     BIGINT       NOT NULL                COMMENT '主键(雪花)',
  replacement_id         BIGINT       NOT NULL                COMMENT '所属替换申请',
  step                   VARCHAR(24)  NOT NULL                COMMENT 'SELF|MARKET_LEADER|RD_LEADER|SUPER_ADMIN',
  approver_id            BIGINT       NOT NULL                COMMENT '审批人',
  approver_role          VARCHAR(32)  NOT NULL                COMMENT '审批人角色',
  decision               VARCHAR(16)  NOT NULL                COMMENT 'APPROVE|REJECT',
  opinion                VARCHAR(500) DEFAULT NULL            COMMENT '审批意见',
  decided_at             DATETIME     NOT NULL                COMMENT '审批时间',
  create_dept            BIGINT       DEFAULT NULL,
  create_by              BIGINT       DEFAULT NULL,
  create_time            DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_by              BIGINT       DEFAULT NULL,
  update_time            DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  tenant_id              VARCHAR(20)  DEFAULT '000000',
  del_flag               CHAR(1)      DEFAULT '0',
  PRIMARY KEY (id),
  KEY idx_rdra_repl (replacement_id),
  KEY idx_rdra_step (step),
  UNIQUE KEY uk_rdra_repl_step (replacement_id, step)        -- 四步每步一条,不可重复
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 研发 PM 替换审批留痕(每步一条,留痕完整)';

-- =====================================================================
-- 3. product_retirements · 产品退市(Q9=甲产品退市 + Q10=甲新表+扩列 + Q11=甲三检查+两级审批+全产品线只读)
-- readiness 三检查:在途项目 / 在途评审 / 未结事项
-- 两级审批:研发 PM(主) → 超管(终)
-- 退市后全产品线只读:products.retirement_locked = 1 联动
-- =====================================================================
CREATE TABLE IF NOT EXISTS product_retirements (
  id                     BIGINT       NOT NULL                COMMENT '主键(雪花)',
  product_id             BIGINT       NOT NULL                COMMENT '退市产品',
  proposer_id            BIGINT       NOT NULL                COMMENT '提议人(研发 PM)',
  proposer_role          VARCHAR(32)  NOT NULL DEFAULT 'RD_PM' COMMENT '研发 PM(Q11 主)',
  reason                 VARCHAR(500) NOT NULL                COMMENT '退市理由',
  readiness_active_projects INT       NOT NULL DEFAULT '0'    COMMENT 'readiness 检查① 在途项目数',
  readiness_active_reviews  INT       NOT NULL DEFAULT '0'    COMMENT 'readiness 检查② 在途评审数',
  readiness_open_issues     INT       NOT NULL DEFAULT '0'    COMMENT 'readiness 检查③ 未结事项数',
  readiness_passed       CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '三项检查全部 0 才算通过 1是',
  status                 VARCHAR(24)  NOT NULL DEFAULT 'PENDING_RD_LEADER' COMMENT 'PENDING_RD_LEADER|PENDING_SUPER_ADMIN|APPROVED|REJECTED',
  rd_leader_id           BIGINT       DEFAULT NULL            COMMENT '研发组长 / 研发 PM 审批人',
  rd_decision            VARCHAR(16)  DEFAULT NULL            COMMENT 'APPROVE|REJECT',
  rd_decided_at          DATETIME     DEFAULT NULL            COMMENT '一级审批时间',
  rd_opinion             VARCHAR(500) DEFAULT NULL            COMMENT '一级审批意见',
  super_admin_id         BIGINT       DEFAULT NULL            COMMENT '超管终审',
  super_decision         VARCHAR(16)  DEFAULT NULL            COMMENT 'APPROVE|REJECT',
  super_decided_at       DATETIME     DEFAULT NULL            COMMENT '终审时间',
  super_opinion          VARCHAR(500) DEFAULT NULL            COMMENT '终审意见',
  due_at                 DATETIME     DEFAULT NULL            COMMENT '审批期限(Q2=乙加列)',
  approved_at            DATETIME     DEFAULT NULL            COMMENT '退市生效时间(联动 products.retired_at)',
  rejected_at            DATETIME     DEFAULT NULL            COMMENT '驳回时间',
  create_dept            BIGINT       DEFAULT NULL,
  create_by              BIGINT       DEFAULT NULL,
  create_time            DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_by              BIGINT       DEFAULT NULL,
  update_time            DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  tenant_id              VARCHAR(20)  DEFAULT '000000',
  del_flag               CHAR(1)      DEFAULT '0',
  remark                 VARCHAR(500) DEFAULT NULL,
  version                INT          NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  UNIQUE KEY uk_pr_product_active (product_id, del_flag)     -- 单产品活跃退市申请唯一(del_flag=0 时生效)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 产品退市申请(readiness 三检查 + 两级审批 + 全产品线只读联动)';

-- products 表扩列(Q10=甲):retired_at / retirement_locked
-- 列守卫幂等:information_schema.COLUMNS 判后再 ALTER(避免重放重复加列)
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA='ipd_dev' AND TABLE_NAME='products' AND COLUMN_NAME='retired_at');
SET @sql := IF(@col_exists=0,
  'ALTER TABLE products ADD COLUMN retired_at DATETIME DEFAULT NULL COMMENT ''产品退市生效时间(Q10 联动 product_retirements.approved_at)'' AFTER status',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA='ipd_dev' AND TABLE_NAME='products' AND COLUMN_NAME='retirement_locked');
SET @sql := IF(@col_exists=0,
  'ALTER TABLE products ADD COLUMN retirement_locked CHAR(1) DEFAULT ''0'' COMMENT ''退市后全产品线只读锁 1锁 0解(Q11 联动,数据安全底线)'' AFTER retired_at',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =====================================================================
-- 4. multi_project_capacity_approvals · 多项目产能备案(Q12=甲备案保留 + Q13=甲≥3 触发+一单一项目)
-- 触发:PM 活动项目数 ≥3 时新建项目 → 强制产能备案
-- 消费:备案单一次只消费一个项目(非长期资格)
-- =====================================================================
CREATE TABLE IF NOT EXISTS multi_project_capacity_approvals (
  id                     BIGINT       NOT NULL                COMMENT '主键(雪花)',
  applicant_id           BIGINT       NOT NULL                COMMENT '申请人(PM)',
  applicant_role         VARCHAR(32)  NOT NULL                COMMENT 'MARKET_PM|RD_PM',
  target_project_id      BIGINT       NOT NULL                COMMENT '被消费的待建项目',
  active_project_count   INT          NOT NULL                COMMENT '申请时本人活动项目数(Q13=甲 ≥3)',
  reason                 VARCHAR(500) NOT NULL                COMMENT '产能说明',
  status                 VARCHAR(24)  NOT NULL DEFAULT 'PENDING_GROUP_LEADER' COMMENT 'PENDING_GROUP_LEADER|PENDING_SUPER_ADMIN|APPROVED|REJECTED|CONSUMED|EXPIRED',
  consumed_at            DATETIME     DEFAULT NULL            COMMENT '消费时间(挂在目标项目下,Q13 一单一项目)',
  consumed_project_id    BIGINT       DEFAULT NULL            COMMENT '实际消费项目(冗余,便于追溯)',
  group_leader_id        BIGINT       DEFAULT NULL            COMMENT '组长审批人',
  group_decision         VARCHAR(16)  DEFAULT NULL            COMMENT 'APPROVE|REJECT',
  group_decided_at       DATETIME     DEFAULT NULL            COMMENT '组长决策时间',
  group_opinion          VARCHAR(500) DEFAULT NULL            COMMENT '组长意见',
  super_admin_id         BIGINT       DEFAULT NULL            COMMENT '超管终审',
  super_decision         VARCHAR(16)  DEFAULT NULL            COMMENT 'APPROVE|REJECT',
  super_decided_at       DATETIME     DEFAULT NULL            COMMENT '终审时间',
  super_opinion          VARCHAR(500) DEFAULT NULL            COMMENT '终审意见',
  due_at                 DATETIME     DEFAULT NULL            COMMENT '审批期限(Q2=乙加列,3 自然日)',
  approved_at            DATETIME     DEFAULT NULL            COMMENT '审批通过时间(此后待消费)',
  rejected_at            DATETIME     DEFAULT NULL            COMMENT '驳回时间',
  valid_until            DATETIME     DEFAULT NULL            COMMENT '审批后有效期(Q13 一单一项目消费,默认 30 天)',
  create_dept            BIGINT       DEFAULT NULL,
  create_by              BIGINT       DEFAULT NULL,
  create_time            DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_by              BIGINT       DEFAULT NULL,
  update_time            DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  tenant_id              VARCHAR(20)  DEFAULT '000000',
  del_flag               CHAR(1)      DEFAULT '0',
  remark                 VARCHAR(500) DEFAULT NULL,
  version                INT          NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  KEY idx_mpca_applicant (applicant_id),
  KEY idx_mpca_target (target_project_id),
  KEY idx_mpca_status (status),
  KEY idx_mpca_due (due_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 多项目产能备案(≥3 活动项目触发,一单一消费,非长期资格)';

-- =====================================================================
-- apply 后回读核验(手工跑)
--   SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES
--   WHERE TABLE_SCHEMA='ipd_dev' AND TABLE_NAME IN
--     ('gate_waivers','rd_replacements','rd_replacement_approvals',
--      'product_retirements','multi_project_capacity_approvals');
--   SHOW COLUMNS FROM products LIKE 'retired%';
-- 期望:5 张新表存在 + products 扩 retired_at / retirement_locked 两列
-- apply 后 GRANT 补登:
--   新表必须在 application.yml tenant.excludes 补登,否则查询被自动追加租户过滤
--   ipd_app@127.0.0.1 表级 GRANT SELECT/INSERT/UPDATE/DELETE(对照惯例 p362-contribution.sql :46-47)
-- =====================================================================
