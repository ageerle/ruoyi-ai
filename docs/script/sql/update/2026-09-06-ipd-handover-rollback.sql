-- =====================================================================
-- HIGH-3.1：HandoverService 加撤销接受接口（ROLLED_BACK 终态 + /cancel 端点）
-- handover_records 加 rollback_reason + rollback_at 两列；幂等 ALTER，
-- information_schema 判列存在即跳过；日期：2026-09-06；隔离库人工 apply
-- =====================================================================

set @has_rollback_reason := (
    select count(*) from information_schema.columns
    where table_schema = database() and table_name = 'handover_records' and column_name = 'rollback_reason');
set @ddl := if(@has_rollback_reason = 0,
    'alter table handover_records add column rollback_reason varchar(500) null comment ''HIGH-3.1：撤销原因（≤500字符）'' after note',
    'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

set @has_rollback_at := (
    select count(*) from information_schema.columns
    where table_schema = database() and table_name = 'handover_records' and column_name = 'rollback_at');
set @ddl := if(@has_rollback_at = 0,
    'alter table handover_records add column rollback_at datetime null comment ''HIGH-3.1：撤销时间—COMPLETED → ROLLED_BACK'' after rollback_reason',
    'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

-- 回读校验（apply 后人工核对两列在位）
select column_name, column_type, column_comment
from information_schema.columns
where table_schema = database() and table_name = 'handover_records'
  and column_name in ('rollback_reason', 'rollback_at');