-- AC-INC-15c / Q4：S/B 级差异化系数定值 = 双PM 联合提议 → 产品组长确认 → 写入项目档案
-- 幂等：表已存在则跳过（手工执行前请确认环境）

CREATE TABLE IF NOT EXISTS coefficient_change_requests
(
    id                     bigint         not null comment '主键（雪花）',
    project_id             bigint         not null comment '目标项目',
    proposed_coefficient   decimal(5, 2)  not null comment '提议系数',
    reason                 varchar(500)   not null comment '定值理由（写审计）',
    market_pm_id           bigint         not null comment '市场PM（联合提议方）',
    rd_pm_id               bigint         not null comment '研发PM（联合提议方）',
    proposer_id            bigint         not null comment '提交人（双PM之一或超管代提）',
    status                 varchar(24)    not null default 'PENDING_LEADER' comment 'PENDING_LEADER|CONFIRMED|REJECTED',
    leader_id              bigint         null comment '产品组长确认人',
    leader_decision        varchar(16)    null comment 'APPROVE|REJECT',
    leader_decided_at      datetime       null comment '组长决策时间',
    leader_opinion         varchar(500)   null comment '组长意见',
    create_dept            bigint         null,
    create_by              bigint         null,
    create_time            datetime       null default CURRENT_TIMESTAMP,
    update_by              bigint         null,
    update_time            datetime       null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id              varchar(20)    null default '000000',
    del_flag               char(1)        null default '0',
    remark                 varchar(500)   null,
    primary key (id),
    key idx_ccr_project_status (project_id, status)
) engine = innodb
  default charset = utf8mb4
  collate = utf8mb4_general_ci
  comment = 'IPD S/B 级系数定值申请（AC-INC-15c）';

-- 真库最小权限（与 ipd_app@127.0.0.1 表级 GRANT 模型对齐）
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ipd_dev.coefficient_change_requests TO 'ipd_app'@'127.0.0.1';
