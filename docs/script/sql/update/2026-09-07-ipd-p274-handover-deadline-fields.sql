-- =====================================================================
-- P2-7.4 AC-HAND-02：handover_records 加 deadline_at / last_remind_at / escalated_at
-- 看板卡：3b4c82a1-486d-4a6c-ab6f-e9381abf624e
-- SSOT：IPD系统_验收清单.md L206 + AC-真值回对矩阵 L198（三方一致）
-- 卡面口径：15 日内未完成移交 ⇒ 自动升级超管告警 + 每日提醒
-- 字段用途：
--   deadline_at    DRAFT 起算 + 15d；调度扫描锚（idx_hr_deadline）
--   last_remind_at 每日提醒最后时间（同日去重，AC-HAND-02 每日提醒语义）
--   escalated_at   升级超管时间（一次性事件，NULL=未升级，AC-HAND-02 升级语义）
-- 幂等：information_schema 判列/索引存在即跳过；隔离库人工 apply
-- =====================================================================

-- 1) deadline_at
set @has_deadline := (
    select count(*) from information_schema.columns
    where table_schema = database() and table_name = 'handover_records' and column_name = 'deadline_at');
set @ddl := if(@has_deadline = 0,
    'alter table handover_records add column deadline_at datetime null comment ''P2-7.4 AC-HAND-02：15日截止时间（DRAFT起算）'' after completed_at',
    'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

-- 2) last_remind_at
set @has_remind := (
    select count(*) from information_schema.columns
    where table_schema = database() and table_name = 'handover_records' and column_name = 'last_remind_at');
set @ddl := if(@has_remind = 0,
    'alter table handover_records add column last_remind_at datetime null comment ''P2-7.4 AC-HAND-02：每日提醒最后时间（同日去重）'' after deadline_at',
    'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

-- 3) escalated_at
set @has_escalated := (
    select count(*) from information_schema.columns
    where table_schema = database() and table_name = 'handover_records' and column_name = 'escalated_at');
set @ddl := if(@has_escalated = 0,
    'alter table handover_records add column escalated_at datetime null comment ''P2-7.4 AC-HAND-02：超管升级时间（一次性，NULL=未升级）'' after last_remind_at',
    'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

-- 4) idx_hr_deadline（调度扫描锚）
set @has_idx := (
    select count(*) from information_schema.statistics
    where table_schema = database() and table_name = 'handover_records' and index_name = 'idx_hr_deadline');
set @ddl := if(@has_idx = 0,
    'alter table handover_records add index idx_hr_deadline (deadline_at)',
    'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

-- 回读校验1：三列在位
select column_name, column_type, column_comment
from information_schema.columns
where table_schema = database() and table_name = 'handover_records'
  and column_name in ('deadline_at', 'last_remind_at', 'escalated_at')
order by column_name;

-- 回读校验2：idx_hr_deadline 在位
select index_name, column_name, seq_in_index
from information_schema.statistics
where table_schema = database() and table_name = 'handover_records'
  and index_name = 'idx_hr_deadline';
