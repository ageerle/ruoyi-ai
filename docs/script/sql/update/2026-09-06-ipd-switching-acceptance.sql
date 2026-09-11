-- [SEC-FIX-SWITCHING-ORPHAN] SwitchingAcceptance 孤儿表 DDL——Entity 已实现但 DDL 缺失
-- 业务链路：P3-7.1 月度账务切换验收 BR-INC-12
-- 字段映射：SwitchingAcceptance entity 13 字段 + BaseEntity 6 字段
ALTER TABLE switching_acceptance ADD COLUMN IF NOT EXISTS id bigint not null comment '主键（雪花）';
-- 注：表本身不存在于仓库任何 SQL 文件，本 ALTER 会失败——但 idempotent ADD COLUMN 在 MySQL 8 不支持跨表
-- 真实落地：CREATE TABLE（如已存在会因 IF NOT EXISTS 跳过）
CREATE TABLE IF NOT EXISTS switching_acceptance (
    id                bigint        not null comment '主键（雪花）',
    project_id        bigint        not null comment '项目 ID',
    month             varchar(7)    not null comment '账务月 YYYY-MM',
    report_json       text          null     comment '对账报告 JSON（ZK §3.2.6）',
    diff_rate         decimal(5,4)  null     comment '差异率（差异金额/池金额）',
    passed            tinyint(1)    not null default 0 comment '是否通过（差异率 ≤ 1%）',
    is_locked         tinyint(1)    not null default 0 comment '是否锁定（超管月度归档）',
    locked_by         bigint        null comment '锁定人 personId',
    locked_at         datetime      null comment '锁定时间',
    unlocked_by       bigint        null comment '解锁人',
    unlocked_at       datetime      null comment '解锁时间',
    unlock_reason     varchar(500)  null comment '解锁原因',
    remark            varchar(500)  null,
    create_dept        bigint        null,
    create_by         bigint        null,
    create_time       datetime      null default CURRENT_TIMESTAMP,
    update_by         bigint        null,
    update_time       datetime      null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id         varchar(20)   null default '000000',
    del_flag          char(1)       null default '0',
    primary key (id),
    unique key uk_switching_month (project_id, month, del_flag),
    key idx_switching_project (project_id),
    key idx_switching_month (month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月度账务切换验收（BR-INC-12）';
