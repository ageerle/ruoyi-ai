-- =====================================================================
-- P2-4.2 主/附加项目定义 + 超额备案审核（AC-TEAM-11；BR-ORG-03/BR-TEAM-09）
-- project_members 加 member_type（首个活跃绑定=PRIMARY，其余 ADDITIONAL）
-- 与 approval_ref（第 3 个项目起的评级委员会审批备案编号，阈值 allowance.projectCountThreshold=3）
-- 幂等：information_schema 判列存在即跳过；日期：2026-09-06；隔离库人工 apply
-- =====================================================================

set @has_member_type := (
    select count(*) from information_schema.columns
    where table_schema = database() and table_name = 'project_members' and column_name = 'member_type');
set @ddl := if(@has_member_type = 0,
    'alter table project_members add column member_type varchar(16) null comment ''主项目PRIMARY|附加ADDITIONAL（P2-4.2，首个活跃绑定=PRIMARY）'' after role',
    'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

set @has_approval_ref := (
    select count(*) from information_schema.columns
    where table_schema = database() and table_name = 'project_members' and column_name = 'approval_ref');
set @ddl := if(@has_approval_ref = 0,
    'alter table project_members add column approval_ref varchar(64) null comment ''评级委员会审批备案编号（绑第N个项目必填，N=allowance.projectCountThreshold）'' after member_type',
    'select 1');
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

-- 回读校验（apply 后人工核对两列在位）
select column_name, column_type, column_comment
from information_schema.columns
where table_schema = database() and table_name = 'project_members'
  and column_name in ('member_type', 'approval_ref');
