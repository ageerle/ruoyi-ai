-- =====================================================================
-- P2-7.1 单项目角色移交（AC-HAND-01c/01d/03/06；BR-HAND-01/04）
-- handover_records 加 handover_role（移交的角色维度 RD_PM|MARKET_PM，AC-HAND-06 角色隔离）
-- 与 note（移交说明 ≤1000，spec 字段模型 note 列；remark varchar(500) 不足）
-- 幂等：information_schema 判列存在即跳过；日期：2026-09-06；隔离库人工 apply
-- =====================================================================

set @has_role := (
    select count(*) from information_schema.columns
    where table_schema = database() and table_name = 'handover_records' and column_name = 'handover_role');
set @ddl := if(@has_role = 0,
    'alter table handover_records add column handover_role varchar(16) null comment ''移交角色维度 RD_PM或MARKET_PM（AC-HAND-06 仅目标角色变更）'' after to_person_id',
    'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

set @has_note := (
    select count(*) from information_schema.columns
    where table_schema = database() and table_name = 'handover_records' and column_name = 'note');
set @ddl := if(@has_note = 0,
    'alter table handover_records add column note varchar(1000) null comment ''移交说明（≤1000字符，spec 字段模型）'' after scope',
    'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

-- 回读校验（apply 后人工核对两列在位）
select column_name, column_type, column_comment
from information_schema.columns
where table_schema = database() and table_name = 'handover_records'
  and column_name in ('handover_role', 'note');
