-- P3-6.2：贡献度评定（BR-INC-09 / AC-INC-25~28）
-- 一项目一条评定记录，CONFIRMED 后通过 status 版本号 + leader_decided_at 区分
-- 幂等：表已存在则跳过（手工执行前请确认环境）

CREATE TABLE IF NOT EXISTS contributions
(
    id                          bigint         not null comment '主键（雪花）',
    project_id                  bigint         not null comment '项目 ID',
    status                      varchar(24)    not null default 'DRAFT' comment 'DRAFT|SUBMITTED|CONFIRMED',
    market_share                decimal(5, 2)  not null default 0.55 comment '市场 PM 比例（40%-65%）',
    rd_share                    decimal(5, 2)  not null default 0.45 comment '研发 PM 比例（联动 = 1 - market）',
    market_self_initiation      decimal(5, 2)  null comment '市场 PM 自评·立项主导 0-100',
    market_self_innovation      decimal(5, 2)  null comment '市场 PM 自评·差异化创新 0-100',
    market_self_launch          decimal(5, 2)  null comment '市场 PM 自评·上市节奏 0-100',
    market_self_market_result   decimal(5, 2)  null comment '市场 PM 自评·市场结果 0-100',
    market_self_leadership      decimal(5, 2)  null comment '市场 PM 自评·协同领导力 0-100',
    rd_self_initiation          decimal(5, 2)  null comment '研发 PM 自评·立项主导 0-100',
    rd_self_innovation          decimal(5, 2)  null comment '研发 PM 自评·差异化创新 0-100',
    rd_self_launch              decimal(5, 2)  null comment '研发 PM 自评·上市节奏 0-100',
    rd_self_market_result       decimal(5, 2)  null comment '研发 PM 自评·市场结果 0-100',
    rd_self_leadership          decimal(5, 2)  null comment '研发 PM 自评·协同领导力 0-100',
    market_comment              varchar(500)   null comment '市场 PM 自评备注',
    rd_comment                  varchar(500)   null comment '研发 PM 自评备注',
    tier_coefficient            decimal(5, 2)  null comment '五维度修正因子 = 加权得分 / 100（ZK-IPD §三.2.5）',
    leader_id                   bigint         null comment '产品组长 ID（确认人）',
    leader_decision             varchar(16)    null comment 'APPROVE|REJECT',
    leader_decided_at           datetime       null comment '组长决策时间',
    leader_opinion              varchar(500)   null comment '组长意见',
    submitted_at                datetime       null comment '双 PM 自评均完成时间',
    create_dept                 bigint         null,
    create_by                   bigint         null,
    create_time                 datetime       null default CURRENT_TIMESTAMP,
    update_by                   bigint         null,
    update_time                 datetime       null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id                   varchar(20)    null default '000000' comment '多租户',
    del_flag                    char(1)        null default '0' comment '软删除 0-有效 1-删除',
    remark                      varchar(500)   null,
    primary key (id),
    key idx_contrib_proj_status (project_id, status),
    key idx_contrib_tenant      (tenant_id)
) engine = innodb
  default charset = utf8mb4
  collate = utf8mb4_general_ci
  comment = 'IPD 贡献度评定（BR-INC-09，5 维度权重 25/25/20/20/10 = 100）';

-- 真库最小权限（与 ipd_app@127.0.0.1 表级 GRANT 模型对齐）
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ipd_dev.contributions TO 'ipd_app'@'127.0.0.1';