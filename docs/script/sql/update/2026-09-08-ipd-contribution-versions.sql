-- 2026-09-08 贡献度确认归档快照表（BR-INC-09「归档版本可追溯；奖金引用同一版本」）
-- 背景：contributions 为单行状态机（DRAFT→SUBMITTED→CONFIRMED），REJECT 退回后
-- 再确认会覆盖，历史确认版次不可追溯。本表在每次 APPROVE 确认时落一份不可变快照。
-- 索引：project_id + version_no 唯一（同项目版次递增不重复）。
CREATE TABLE IF NOT EXISTS contribution_versions
(
    id                          bigint         not null comment '主键（雪花）',
    source_id                   bigint         not null comment '来源 contributions.id',
    project_id                  bigint         not null comment '项目 ID',
    version_no                  int            not null comment '确认版次（同项目从 1 递增）',
    status                      varchar(24)    not null comment '快照时刻状态，恒为 CONFIRMED',
    market_share                decimal(5, 2)  null comment '市场 PM 比例（快照）',
    rd_share                    decimal(5, 2)  null comment '研发 PM 比例（快照）',
    market_self_initiation      decimal(5, 2)  null comment '市场 PM 自评·立项主导（快照）',
    market_self_innovation      decimal(5, 2)  null comment '市场 PM 自评·差异化创新（快照）',
    market_self_launch          decimal(5, 2)  null comment '市场 PM 自评·上市节奏（快照）',
    market_self_market_result   decimal(5, 2)  null comment '市场 PM 自评·市场结果（快照）',
    market_self_leadership      decimal(5, 2)  null comment '市场 PM 自评·协同领导力（快照）',
    rd_self_initiation          decimal(5, 2)  null comment '研发 PM 自评·立项主导（快照）',
    rd_self_innovation          decimal(5, 2)  null comment '研发 PM 自评·差异化创新（快照）',
    rd_self_launch              decimal(5, 2)  null comment '研发 PM 自评·上市节奏（快照）',
    rd_self_market_result       decimal(5, 2)  null comment '研发 PM 自评·市场结果（快照）',
    rd_self_leadership          decimal(5, 2)  null comment '研发 PM 自评·协同领导力（快照）',
    tier_coefficient            decimal(5, 2)  null comment '五维度修正因子（快照）',
    market_comment              varchar(500)   null comment '市场 PM 自评备注（快照）',
    rd_comment                  varchar(500)   null comment '研发 PM 自评备注（快照）',
    leader_id                   bigint         null comment '确认组长 ID（快照）',
    leader_decision             varchar(16)    null comment 'APPROVE（快照）',
    leader_decided_at           datetime       null comment '组长决策时间（快照）',
    leader_opinion              varchar(500)   null comment '组长意见（快照）',
    submitted_at                datetime       null comment '双 PM 自评完成时间（快照）',
    archived_by                 bigint         null comment '归档操作人',
    archived_at                 datetime       null comment '归档时间',
    create_dept                 bigint         null,
    create_by                   bigint         null,
    create_time                 datetime       null default CURRENT_TIMESTAMP,
    update_by                   bigint         null,
    update_time                 datetime       null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id                   varchar(20)    null default '000000' comment '多租户',
    del_flag                    char(1)        null default '0' comment '软删除（0 正常 1 已删）',
    primary key (id),
    unique key uk_contribution_version (project_id, version_no),
    key idx_contribution_version_project (project_id)
) engine = InnoDB
  default charset = utf8mb4 comment ='贡献度确认归档快照（BR-INC-09 版本可追溯）';
