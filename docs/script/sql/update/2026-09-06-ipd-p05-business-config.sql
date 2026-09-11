-- =====================================================================
-- IPD 业务参数表 ipd_business_config（ROOT-R1 业务参数配置化根治，P0-5 落地）
--
-- 触发：勘察卡 R1「业务架构师缺失（参数应可配置未配置）」9 子项 B-CONS-03/B-RULE-01/
--       B-RULE-05/V5/V8/ZK-05/13/14/17 全部根因
-- 设计：与 system_configs 并存（解耦业务参数与系统参数），独立 ROOT-R1-CONFIG 表
--       按 ROOT-R1-P0-5 卡要点：key/value/scope/enabled/version + Redis 缓存 + 热更新
--
-- 与 system_configs 区别：
--   - system_configs = 治理/审计/通知等系统级参数（SystemConfigService + 版本链快照）
--   - ipd_business_config = 业务规则参数（奖金池基数/KPI门槛/冷静期/双签人数）——ROOT-R1 根治
--   - 两者通过 key 前缀区分（system_configs 用 s_/ops_/；ipd_business_config 用 business_）
--
-- 租户：单企业私有部署，全表带 tenant_id 默认 '000000'，统一登记 tenant.excludes
-- 日期：2026-09-06
-- =====================================================================

-- 表：业务参数（KPI 阈值 / 奖金池基数 / 冷静期 / 双签人数 / 阶梯系数 等业务规则参数）
-- 存算解耦：业务参数与 system_configs 并存，本表专管"影响业务结果计算"的参数
create table if not exists ipd_business_config
(
    id            bigint       not null comment '主键（雪花）',
    config_key    varchar(128) not null comment '参数键（如 bonus.poolRate / kpi.stopThreshold / deletion.cooldownDays / gate.dualSignCount）',
    config_value  varchar(500) not null comment '参数值（字符串持久化，读取方按 value_type 解析）',
    value_type    varchar(16)  not null default 'STRING' comment 'STRING|NUMBER|JSON|BOOL',
    scope         varchar(16)  not null default 'GLOBAL' comment 'GLOBAL|GROUP|PROJECT 作用域',
    enabled       tinyint(1)   not null default 1 comment '是否启用（0=禁用读取 fallback 默认值）',
    version       int          not null default 1 comment '当前版本号（每次更新自增，订阅用）',
    cache_ttl     int          not null default 60 comment '缓存 TTL（秒（），0=不缓存）',
    description   varchar(256) null     comment '参数描述（如"奖金池基数比例"）',
    tenant_id     varchar(20)  not null default '000000' comment '租户ID（单企业私有部署统一值）',
    del_flag      char(1)      not null default '0' comment '软删除（0正常 1已删）',
    create_dept   bigint NULL NULL comment '创建部门',
    create_by     bigint NULL NULL comment '创建人',
    create_time   datetime     null     comment '创建时间',
    update_by     bigint NULL NULL comment '更新人',
    update_time   datetime     null     comment '更新时间',
    primary key (id, config_key) using btree,
    unique key uk_ipd_business_config_key (config_key, tenant_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_unicode_ci comment 'IPD 业务参数表（ROOT-R1 业务参数配置化）';

-- 历史版本表（业务参数版本链，与 system_config_versions 模式一致）
create table if not exists ipd_business_config_versions
(
    id            bigint       not null comment '主键',
    config_id     bigint       not null comment '业务参数ID（关联 ipd_business_config.id）',
    config_key    varchar(128) not null comment '参数键（冗余便于历史追溯）',
    config_value  varchar(500) not null comment '历史参数值',
    version       int          not null comment '历史版本号',
    enabled       tinyint(1)   not null comment '当时是否启用',
    effective_from datetime    not null comment '生效起点',
    effective_to   datetime    null     comment '生效终点（null=当前生效）',
    tenant_id     varchar(20)  not null default '000000' comment '租户ID',
    create_by     bigint NULL NULL comment '变更人',
    create_time   datetime     null     comment '变更时间',
    primary key (id, config_key) using btree,
    unique key uk_ipd_business_config_versions (config_id, version)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_unicode_ci comment 'IPD 业务参数历史版本表';

-- 初始 seed 数据：9 子项 ROOT-R1 根治
-- 注意：表值种子表只表值种子表值种子（表表）表「 bonus.poolRate = 0.0500」（表表表）
-- 「 kpi.stopThreshold = 60」（ 表表表表表）
-- 「 deletion.cooldownDays = 3」（ 表表表表）
-- 「 gate.dualSignCount = 3」（ 表表表表）
-- 「 bonus.tierCoefficient_50 = 0.50 / 80 = 0.80 / 100 = 1.00 / 120 = 1.20」（ 表表表表表）
-- 「 kpi.revision.mode = append」（ 表表表表）
-- 「 gate.signDeadlineDays = 3」（ 表表表）
-- 「 gate.extension.maxCount = 1」（ 表表表）
-- 「 deletion.escalateTimeoutHours = 48」（ 表表表）
insert into ipd_business_config (config_key, config_value, value_type, scope, enabled, version, cache_ttl, description)
values
    ('bonus.poolRate',           '0.0500', 'NUMBER', 'GLOBAL', 1, 1, 60, '奖金池基数比例（默认 5%；业务口径：实际回款×5%×S/A/B）'),
    ('kpi.stopThreshold',         '60',     'NUMBER', 'GLOBAL', 1, 1, 60, 'KPI 停发阈值（分以下停发）'),
    ('deletion.cooldownDays',    '3',      'NUMBER', 'GLOBAL', 1, 1, 60, '删除申请冷静期（天）'),
    ('gate.dualSignCount',       '3',      'NUMBER', 'GLOBAL', 1, 1, 60, 'Gate 双签最低人数（评审通过人数下限）'),
    ('bonus.tierCoefficient_50',  '0.50',   'NUMBER', 'GLOBAL', 1, 1, 60, '奖金池阶梯系数（达成率<50%）'),
    ('bonus.tierCoefficient_80',  '0.80',   'NUMBER', 'GLOBAL', 1, 1, 60, '奖金池阶梯系数（达成率 50-80%）'),
    ('bonus.tierCoefficient_100', '1.00',   'NUMBER', 'GLOBAL', 1, 1, 60, '奖金池阶梯系数（达成率 80-100%）'),
    ('bonus.tierCoefficient_120', '1.20',   'NUMBER', 'GLOBAL', 1, 1, 60, '奖金池阶梯系数（达成率 ≥100%））'),
    ('kpi.revision.mode',         'append', 'STRING', 'GLOBAL', 1, 1, 60, 'KPI 修订模式（append=追加归集/replace=覆盖）'),
    ('gate.signDeadlineDays',     '3',      'NUMBER', 'GLOBAL', 1, 1, 60, 'Gate 评审签字期限（自然日）'),
    ('gate.extension.maxCount',   '1',      'NUMBER', 'GLOBAL', 1, 1, 60, 'Gate 签字期限超管可延长次数上限'),
    ('deletion.escalateTimeoutHours','48',  'NUMBER', 'GLOBAL', 1, 1, 60, '删除申请组长审核超时自动升级超管的小时数')
on duplicate key update
    description = values(description),
    update_time = now();

-- 补登 tenant.excludes（新表 ipd_business_config + versions 单企业私有部署）
-- 该变更需在 application.yml tenant.excludes 区段添加（ROOT-R1-P0-5 闭环条件）
