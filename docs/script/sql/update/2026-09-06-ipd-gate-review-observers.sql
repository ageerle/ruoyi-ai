-- =====================================================================
-- IPD Gate 评审列席人员邀请（MEDIUM-1.3）
-- G1/G5 等关键评审缺列席意见字段，按 ZK 邀请销售/供应/售后/品质/合规列席
-- 列席人仅提交观察意见，不进入主审投票（不参与 gate_reviews 表）
-- 依据：docs/ipd-系统说明/外部资源/IPD系统_AI开发主Prompt_v3.md TS-06
-- 适配：docs/ipd-系统说明/type-mapping.md（PG→MySQL：BIGINT 雪花主键 / DATETIME UTC）
-- 日期：2026-09-06
-- =====================================================================

CREATE TABLE IF NOT EXISTS gate_review_observers (
    id            bigint       not null comment '主键（雪花）',
    gate_id       bigint       not null comment 'Gate ID',
    observer_id   bigint       not null comment '列席人 personId',
    role          varchar(32)  not null comment '列席角色：SALES/SUPPLY/AFTERSALES/QUALITY/COMPLIANCE',
    invited_by    bigint       not null comment '邀请人 personId',
    invited_at    datetime     not null,
    attended      tinyint(1)   not null default 0,
    opinion       varchar(2000) null comment '列席意见',
    create_dept   bigint       null,
    create_by     bigint       null,
    create_time   datetime     null default CURRENT_TIMESTAMP,
    update_by     bigint       null,
    update_time   datetime     null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id     varchar(20)  null default '000000',
    del_flag      char(1)      null default '0',
    primary key (id),
    key idx_observer_gate (gate_id),
    unique key uk_observer (gate_id, observer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Gate 评审列席人员（销售/供应/售后/品质/合规）';