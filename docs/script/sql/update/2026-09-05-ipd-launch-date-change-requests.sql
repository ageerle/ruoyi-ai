-- AC-INC-33 / P1-2.2：上市日期修改 = 一方提议 → 另一方确认（双签）→ 写入 projects.launch_date
-- 幂等：表已存在则跳过

CREATE TABLE IF NOT EXISTS launch_date_change_requests
(
    id                bigint       not null comment '主键（雪花）',
    project_id        bigint       not null comment '目标项目',
    proposed_launch_date datetime  not null comment '提议上市日期',
    previous_launch_date datetime  null comment '变更前上市日期（可空=首次录入）',
    reason            varchar(500) not null comment '变更理由（写审计）',
    proposer_id       bigint       not null comment '提议人',
    proposer_role     varchar(32)  not null comment 'MARKET_PM|RD_PM|SUPER_ADMIN',
    status            varchar(24)  not null default 'PENDING_SECOND' comment 'PENDING_SECOND|CONFIRMED|REJECTED',
    confirmer_id      bigint       null comment '第二签确认人',
    confirmer_role    varchar(32)  null comment '确认人角色',
    confirmed_at      datetime     null comment '确认时间',
    decision          varchar(16)  null comment 'APPROVE|REJECT',
    opinion           varchar(500) null comment '确认/驳回意见',
    create_dept       bigint       null,
    create_by         bigint       null,
    create_time       datetime     null default CURRENT_TIMESTAMP,
    update_by         bigint       null,
    update_time       datetime     null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id         varchar(20)  null default '000000',
    del_flag          char(1)      null default '0',
    remark            varchar(500) null,
    primary key (id),
    key idx_ldcr_project_status (project_id, status)
) engine = innodb
  default charset = utf8mb4
  collate = utf8mb4_general_ci
  comment = 'IPD 上市日期变更双签申请（AC-INC-33）';

-- GRANT SELECT, INSERT, UPDATE, DELETE ON ipd_dev.launch_date_change_requests TO 'ipd_app'@'127.0.0.1';

-- ROLLBACK（环境异常回滚用；version/pending_project_id 回滚见 2026-09-05-ipd-launch-date-pending-unique.sql）
-- DROP TABLE IF EXISTS `launch_date_change_requests`;

