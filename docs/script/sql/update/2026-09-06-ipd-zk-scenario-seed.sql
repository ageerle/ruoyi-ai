-- =====================================================================
-- G4 数据门：ZK-IPD 场景种子（对齐原型 scenario.mjs + 前端 workbench 原硬编码 demo）
-- 6 产品组 + 13 人（傅志谦超管等，密码=dev 初始口令，哈希复用 Mock 账号同明文 BCrypt）
-- + 3 完结项目（ENT-AC-100/ZK-IAT-ATT/VIS-RD-100）+ 2 运行态项目（如门禁测试/熵基互联+智能锁联动）
-- 运行态动作含超期（IN_PROGRESS/DELAYED 过去 due）与临期（NOT_STARTED 未来 due），供工作台真实测试
-- 幂等：NOT EXISTS 判存在即跳过；固定 ID 段（persons 911xxxx / groups 912xxxx / products 913xxxx /
--   projects 914xxxx / stages 915xxxx / actions 916xxxx / members 917xxxx）
-- 配套：IpdZkScenarioInitializer（dev 重启时幂等兜底，与本文互不重复）
-- 日期：2026-09-06；apply：mysql-migrator 直灌 ipd_dev
-- =====================================================================

-- ---- 1. 产品组（6）----
insert into product_groups (id, group_name, leader_person_id, description, create_time, tenant_id, del_flag)
select 9120001, '时间安全管理产品组', 9110002, 'ZK-IPD 场景种子（市场）', now(), '000000', '0' from dual
where not exists (select 1 from product_groups where group_name = '时间安全管理产品组' and del_flag = '0');
insert into product_groups (id, group_name, leader_person_id, description, create_time, tenant_id, del_flag)
select 9120002, '云应用产品组', 9110004, 'ZK-IPD 场景种子（市场）', now(), '000000', '0' from dual
where not exists (select 1 from product_groups where group_name = '云应用产品组' and del_flag = '0');
insert into product_groups (id, group_name, leader_person_id, description, create_time, tenant_id, del_flag)
select 9120003, '项目集成产品组', 9110006, 'ZK-IPD 场景种子（市场）', now(), '000000', '0' from dual
where not exists (select 1 from product_groups where group_name = '项目集成产品组' and del_flag = '0');
insert into product_groups (id, group_name, leader_person_id, description, create_time, tenant_id, del_flag)
select 9120004, '时间安全管理产品线', 9110008, 'ZK-IPD 场景种子（研发）', now(), '000000', '0' from dual
where not exists (select 1 from product_groups where group_name = '时间安全管理产品线' and del_flag = '0');
insert into product_groups (id, group_name, leader_person_id, description, create_time, tenant_id, del_flag)
select 9120005, '云应用产品线', 9110010, 'ZK-IPD 场景种子（研发）', now(), '000000', '0' from dual
where not exists (select 1 from product_groups where group_name = '云应用产品线' and del_flag = '0');
insert into product_groups (id, group_name, leader_person_id, description, create_time, tenant_id, del_flag)
select 9120006, '项目集成产品线', 9110012, 'ZK-IPD 场景种子（研发）', now(), '000000', '0' from dual
where not exists (select 1 from product_groups where group_name = '项目集成产品线' and del_flag = '0');

-- ---- 2. 人员（13；密码=BCrypt(dev 初始口令)，哈希与 Mock 账号同明文互验）----
insert into persons (id, name, employee_no, person_type, group_id, level, level_source, account_status, employment_status, username, password_hash, must_change_pwd, remark, create_time, tenant_id, del_flag)
select 9110001, '傅志谦', 'GMP-001', 'SUPER_ADMIN', null, 'L5', 'MOCK', 'ACTIVE', 'ACTIVE', '傅志谦', '$2a$10$AMdqWT6Eib4GKjLahn2I6exr0v2QZlTDbHvGLVG0rLeiyYp90fw3S', '1', 'ZK-IPD 场景：超级管理员', now(), '000000', '0' from dual
where not exists (select 1 from persons where employee_no = 'GMP-001' and del_flag = '0');
insert into persons (id, name, employee_no, person_type, group_id, level, level_source, account_status, employment_status, username, password_hash, must_change_pwd, remark, create_time, tenant_id, del_flag)
select 9110002, '杨波', 'MHW-L01', 'GROUP_LEADER', 9120001, 'L5', 'MOCK', 'ACTIVE', 'ACTIVE', '杨波', '$2a$10$AMdqWT6Eib4GKjLahn2I6exr0v2QZlTDbHvGLVG0rLeiyYp90fw3S', '1', 'ZK-IPD 场景：时间安全管理产品组组长', now(), '000000', '0' from dual
where not exists (select 1 from persons where employee_no = 'MHW-L01' and del_flag = '0');
insert into persons (id, name, employee_no, person_type, group_id, level, level_source, account_status, employment_status, username, password_hash, must_change_pwd, remark, create_time, tenant_id, del_flag)
select 9110003, '段进科', 'MHW-P01', 'MARKET_PM', 9120001, 'L3', 'MOCK', 'ACTIVE', 'ACTIVE', '段进科', '$2a$10$AMdqWT6Eib4GKjLahn2I6exr0v2QZlTDbHvGLVG0rLeiyYp90fw3S', '1', 'ZK-IPD 场景：市场PM', now(), '000000', '0' from dual
where not exists (select 1 from persons where employee_no = 'MHW-P01' and del_flag = '0');
insert into persons (id, name, employee_no, person_type, group_id, level, level_source, account_status, employment_status, username, password_hash, must_change_pwd, remark, create_time, tenant_id, del_flag)
select 9110004, '文元彪', 'MSW-L01', 'GROUP_LEADER', 9120002, 'L4', 'MOCK', 'ACTIVE', 'ACTIVE', '文元彪', '$2a$10$AMdqWT6Eib4GKjLahn2I6exr0v2QZlTDbHvGLVG0rLeiyYp90fw3S', '1', 'ZK-IPD 场景：云应用产品组组长', now(), '000000', '0' from dual
where not exists (select 1 from persons where employee_no = 'MSW-L01' and del_flag = '0');
insert into persons (id, name, employee_no, person_type, group_id, level, level_source, account_status, employment_status, username, password_hash, must_change_pwd, remark, create_time, tenant_id, del_flag)
select 9110005, '胡蛟露', 'MSW-P01', 'MARKET_PM', 9120002, 'L4', 'MOCK', 'ACTIVE', 'ACTIVE', '胡蛟露', '$2a$10$AMdqWT6Eib4GKjLahn2I6exr0v2QZlTDbHvGLVG0rLeiyYp90fw3S', '1', 'ZK-IPD 场景：市场PM', now(), '000000', '0' from dual
where not exists (select 1 from persons where employee_no = 'MSW-P01' and del_flag = '0');
insert into persons (id, name, employee_no, person_type, group_id, level, level_source, account_status, employment_status, username, password_hash, must_change_pwd, remark, create_time, tenant_id, del_flag)
select 9110006, '陈泽鹏', 'MIT-L01', 'GROUP_LEADER', 9120003, 'L4', 'MOCK', 'ACTIVE', 'ACTIVE', '陈泽鹏', '$2a$10$AMdqWT6Eib4GKjLahn2I6exr0v2QZlTDbHvGLVG0rLeiyYp90fw3S', '1', 'ZK-IPD 场景：项目集成产品组组长', now(), '000000', '0' from dual
where not exists (select 1 from persons where employee_no = 'MIT-L01' and del_flag = '0');
insert into persons (id, name, employee_no, person_type, group_id, level, level_source, account_status, employment_status, username, password_hash, must_change_pwd, remark, create_time, tenant_id, del_flag)
select 9110007, '陈必勤', 'MIT-P01', 'MARKET_PM', 9120003, 'L3', 'MOCK', 'ACTIVE', 'ACTIVE', '陈必勤', '$2a$10$AMdqWT6Eib4GKjLahn2I6exr0v2QZlTDbHvGLVG0rLeiyYp90fw3S', '1', 'ZK-IPD 场景：市场PM', now(), '000000', '0' from dual
where not exists (select 1 from persons where employee_no = 'MIT-P01' and del_flag = '0');
insert into persons (id, name, employee_no, person_type, group_id, level, level_source, account_status, employment_status, username, password_hash, must_change_pwd, remark, create_time, tenant_id, del_flag)
select 9110008, '肖敬龙', 'RHW-L01', 'GROUP_LEADER', 9120004, 'L5', 'MOCK', 'ACTIVE', 'ACTIVE', '肖敬龙', '$2a$10$AMdqWT6Eib4GKjLahn2I6exr0v2QZlTDbHvGLVG0rLeiyYp90fw3S', '1', 'ZK-IPD 场景：时间安全管理产品线组长', now(), '000000', '0' from dual
where not exists (select 1 from persons where employee_no = 'RHW-L01' and del_flag = '0');
insert into persons (id, name, employee_no, person_type, group_id, level, level_source, account_status, employment_status, username, password_hash, must_change_pwd, remark, create_time, tenant_id, del_flag)
select 9110009, '程龙', 'RHW-P01', 'RD_PM', 9120004, 'L4', 'MOCK', 'ACTIVE', 'ACTIVE', '程龙', '$2a$10$AMdqWT6Eib4GKjLahn2I6exr0v2QZlTDbHvGLVG0rLeiyYp90fw3S', '1', 'ZK-IPD 场景：研发PM', now(), '000000', '0' from dual
where not exists (select 1 from persons where employee_no = 'RHW-P01' and del_flag = '0');
insert into persons (id, name, employee_no, person_type, group_id, level, level_source, account_status, employment_status, username, password_hash, must_change_pwd, remark, create_time, tenant_id, del_flag)
select 9110010, '杨志君', 'RSW-L01', 'GROUP_LEADER', 9120005, 'L4', 'MOCK', 'ACTIVE', 'ACTIVE', '杨志君', '$2a$10$AMdqWT6Eib4GKjLahn2I6exr0v2QZlTDbHvGLVG0rLeiyYp90fw3S', '1', 'ZK-IPD 场景：云应用产品线组长', now(), '000000', '0' from dual
where not exists (select 1 from persons where employee_no = 'RSW-L01' and del_flag = '0');
insert into persons (id, name, employee_no, person_type, group_id, level, level_source, account_status, employment_status, username, password_hash, must_change_pwd, remark, create_time, tenant_id, del_flag)
select 9110011, '林立杰', 'RSW-P01', 'RD_PM', 9120005, 'L3', 'MOCK', 'ACTIVE', 'ACTIVE', '林立杰', '$2a$10$AMdqWT6Eib4GKjLahn2I6exr0v2QZlTDbHvGLVG0rLeiyYp90fw3S', '1', 'ZK-IPD 场景：研发PM', now(), '000000', '0' from dual
where not exists (select 1 from persons where employee_no = 'RSW-P01' and del_flag = '0');
insert into persons (id, name, employee_no, person_type, group_id, level, level_source, account_status, employment_status, username, password_hash, must_change_pwd, remark, create_time, tenant_id, del_flag)
select 9110012, '上官志昌', 'RIT-L01', 'GROUP_LEADER', 9120006, 'L4', 'MOCK', 'ACTIVE', 'ACTIVE', '上官志昌', '$2a$10$AMdqWT6Eib4GKjLahn2I6exr0v2QZlTDbHvGLVG0rLeiyYp90fw3S', '1', 'ZK-IPD 场景：项目集成产品线组长', now(), '000000', '0' from dual
where not exists (select 1 from persons where employee_no = 'RIT-L01' and del_flag = '0');
insert into persons (id, name, employee_no, person_type, group_id, level, level_source, account_status, employment_status, username, password_hash, must_change_pwd, remark, create_time, tenant_id, del_flag)
select 9110013, '方武略', 'RIT-P01', 'RD_PM', 9120006, 'L4', 'MOCK', 'ACTIVE', 'ACTIVE', '方武略', '$2a$10$AMdqWT6Eib4GKjLahn2I6exr0v2QZlTDbHvGLVG0rLeiyYp90fw3S', '1', 'ZK-IPD 场景：研发PM', now(), '000000', '0' from dual
where not exists (select 1 from persons where employee_no = 'RIT-P01' and del_flag = '0');

-- ---- 3. 产品（5；1:1 绑项目）----
insert into products (id, product_code, product_name, source, group_id, status, remark, create_time, tenant_id, del_flag)
select 9130001, 'ZK-ENT-AC-100', '入门级门禁产品', 'PM_NEW', 9120001, 'ACTIVE', 'ZK-IPD 场景种子', now(), '000000', '0' from dual
where not exists (select 1 from products where product_name = '入门级门禁产品' and del_flag = '0');
insert into products (id, product_code, product_name, source, group_id, status, remark, create_time, tenant_id, del_flag)
select 9130002, 'ZK-IAT-ATT', '熵基互联考勤模块', 'PM_NEW', 9120002, 'ACTIVE', 'ZK-IPD 场景种子', now(), '000000', '0' from dual
where not exists (select 1 from products where product_name = '熵基互联考勤模块' and del_flag = '0');
insert into products (id, product_code, product_name, source, group_id, status, remark, create_time, tenant_id, del_flag)
select 9130003, 'ZK-VIS-RD-100', '访客机＋万傲瑞达', 'PM_NEW', 9120003, 'ACTIVE', 'ZK-IPD 场景种子', now(), '000000', '0' from dual
where not exists (select 1 from products where product_name = '访客机＋万傲瑞达' and del_flag = '0');
insert into products (id, product_code, product_name, source, group_id, status, remark, create_time, tenant_id, del_flag)
select 9130004, 'ZK-GATE-TEST', '如门禁测试', 'PM_NEW', 9120001, 'ACTIVE', 'ZK-IPD 场景种子（运行态）', now(), '000000', '0' from dual
where not exists (select 1 from products where product_name = '如门禁测试' and del_flag = '0');
insert into products (id, product_code, product_name, source, group_id, status, remark, create_time, tenant_id, del_flag)
select 9130005, 'ZK-IAT-LOCK', '熵基互联+智能锁联动', 'PM_NEW', 9120002, 'ACTIVE', 'ZK-IPD 场景种子（运行态）', now(), '000000', '0' from dual
where not exists (select 1 from products where product_name = '熵基互联+智能锁联动' and del_flag = '0');

-- ---- 4. 项目（5）----
insert into projects (id, code, name, product_id, template_type, level, target_sales_amount, current_stage, lifecycle_status, source, status, main_group_id, launch_date, create_time, tenant_id, del_flag)
select 9140001, 'ENT-AC-100', '入门级门禁产品', 9130001, 'HARDWARE', 'A', 8000000.00, 'LIFECYCLE', 'ARCHIVED', 'NEW', 'ARCHIVED', 9120001, now() - interval 180 day, now() - interval 400 day, '000000', '0' from dual
where not exists (select 1 from projects where code = 'ENT-AC-100' and del_flag = '0');
insert into projects (id, code, name, product_id, template_type, level, target_sales_amount, current_stage, lifecycle_status, source, status, main_group_id, launch_date, create_time, tenant_id, del_flag)
select 9140002, 'ZK-IAT-ATT', '熵基互联考勤模块', 9130002, 'SOFTWARE', 'S', 12000000.00, 'LIFECYCLE', 'ARCHIVED', 'NEW', 'ARCHIVED', 9120002, now() - interval 180 day, now() - interval 400 day, '000000', '0' from dual
where not exists (select 1 from projects where code = 'ZK-IAT-ATT' and del_flag = '0');
insert into projects (id, code, name, product_id, template_type, level, target_sales_amount, current_stage, lifecycle_status, source, status, main_group_id, launch_date, create_time, tenant_id, del_flag)
select 9140003, 'VIS-RD-100', '访客机＋万傲瑞达', 9130003, 'SOLUTION', 'A', 6000000.00, 'LIFECYCLE', 'ARCHIVED', 'NEW', 'ARCHIVED', 9120003, now() - interval 180 day, now() - interval 400 day, '000000', '0' from dual
where not exists (select 1 from projects where code = 'VIS-RD-100' and del_flag = '0');
insert into projects (id, code, name, product_id, template_type, level, target_sales_amount, current_stage, source, status, main_group_id, create_time, tenant_id, del_flag)
select 9140004, 'ZK-GATE-TEST', '如门禁测试', 9130004, 'HARDWARE', 'B', null, 'PLAN', 'NEW', 'ACTIVE', 9120001, now(), '000000', '0' from dual
where not exists (select 1 from projects where code = 'ZK-GATE-TEST' and del_flag = '0');
insert into projects (id, code, name, product_id, template_type, level, target_sales_amount, current_stage, source, status, main_group_id, create_time, tenant_id, del_flag)
select 9140005, 'ZK-IAT-LOCK', '熵基互联+智能锁联动', 9130005, 'SOFTWARE', 'A', null, 'DEV', 'NEW', 'ACTIVE', 9120002, now(), '000000', '0' from dual
where not exists (select 1 from projects where code = 'ZK-IAT-LOCK' and del_flag = '0');

-- 产品回绑项目（1:1）
update products set project_id = 9140001 where id = 9130001 and project_id is null;
update products set project_id = 9140002 where id = 9130002 and project_id is null;
update products set project_id = 9140003 where id = 9130003 and project_id is null;
update products set project_id = 9140004 where id = 9130004 and project_id is null;
update products set project_id = 9140005 where id = 9130005 and project_id is null;

-- ---- 5. 项目成员（市场PM + 研发PM；locked_level/locked_amount=绑定快照 BR-INC-02）----
insert into project_members (id, project_id, person_id, role, member_type, join_date, locked_level, locked_amount, bonus_eligible, create_time, tenant_id, del_flag)
select 9170001, 9140001, 9110003, 'MARKET_PM', 'PRIMARY', now() - interval 400 day, 'L3', 2000.00, '1', now(), '000000', '0' from dual
where not exists (select 1 from project_members where project_id = 9140001 and person_id = 9110003);
insert into project_members (id, project_id, person_id, role, member_type, join_date, locked_level, locked_amount, bonus_eligible, create_time, tenant_id, del_flag)
select 9170002, 9140001, 9110009, 'RD_PM', 'PRIMARY', now() - interval 400 day, 'L4', 2500.00, '1', now(), '000000', '0' from dual
where not exists (select 1 from project_members where project_id = 9140001 and person_id = 9110009);
insert into project_members (id, project_id, person_id, role, member_type, join_date, locked_level, locked_amount, bonus_eligible, create_time, tenant_id, del_flag)
select 9170003, 9140002, 9110005, 'MARKET_PM', 'PRIMARY', now() - interval 400 day, 'L4', 2500.00, '1', now(), '000000', '0' from dual
where not exists (select 1 from project_members where project_id = 9140002 and person_id = 9110005);
insert into project_members (id, project_id, person_id, role, member_type, join_date, locked_level, locked_amount, bonus_eligible, create_time, tenant_id, del_flag)
select 9170004, 9140002, 9110011, 'RD_PM', 'PRIMARY', now() - interval 400 day, 'L3', 2000.00, '1', now(), '000000', '0' from dual
where not exists (select 1 from project_members where project_id = 9140002 and person_id = 9110011);
insert into project_members (id, project_id, person_id, role, member_type, join_date, locked_level, locked_amount, bonus_eligible, create_time, tenant_id, del_flag)
select 9170005, 9140003, 9110007, 'MARKET_PM', 'PRIMARY', now() - interval 400 day, 'L3', 2000.00, '1', now(), '000000', '0' from dual
where not exists (select 1 from project_members where project_id = 9140003 and person_id = 9110007);
insert into project_members (id, project_id, person_id, role, member_type, join_date, locked_level, locked_amount, bonus_eligible, create_time, tenant_id, del_flag)
select 9170006, 9140003, 9110013, 'RD_PM', 'PRIMARY', now() - interval 400 day, 'L4', 2500.00, '1', now(), '000000', '0' from dual
where not exists (select 1 from project_members where project_id = 9140003 and person_id = 9110013);
insert into project_members (id, project_id, person_id, role, member_type, join_date, locked_level, locked_amount, bonus_eligible, create_time, tenant_id, del_flag)
select 9170007, 9140004, 9110003, 'MARKET_PM', 'PRIMARY', now(), 'L3', 2000.00, '1', now(), '000000', '0' from dual
where not exists (select 1 from project_members where project_id = 9140004 and person_id = 9110003);
insert into project_members (id, project_id, person_id, role, member_type, join_date, locked_level, locked_amount, bonus_eligible, create_time, tenant_id, del_flag)
select 9170008, 9140004, 9110009, 'RD_PM', 'PRIMARY', now(), 'L4', 2500.00, '1', now(), '000000', '0' from dual
where not exists (select 1 from project_members where project_id = 9140004 and person_id = 9110009);
insert into project_members (id, project_id, person_id, role, member_type, join_date, locked_level, locked_amount, bonus_eligible, create_time, tenant_id, del_flag)
select 9170009, 9140005, 9110005, 'MARKET_PM', 'PRIMARY', now(), 'L4', 2500.00, '1', now(), '000000', '0' from dual
where not exists (select 1 from project_members where project_id = 9140005 and person_id = 9110005);
insert into project_members (id, project_id, person_id, role, member_type, join_date, locked_level, locked_amount, bonus_eligible, create_time, tenant_id, del_flag)
select 9170010, 9140005, 9110011, 'RD_PM', 'PRIMARY', now(), 'L3', 2000.00, '1', now(), '000000', '0' from dual
where not exists (select 1 from project_members where project_id = 9140005 and person_id = 9110011);

-- ---- 6. 阶段实例（5 项目 × 6 阶段；完结项目全 DONE）----
-- 9140001/9140002/9140003（完结，全 DONE）
insert into project_stages (id, project_id, stage_code, stage_name, sort_order, status, create_time, tenant_id, del_flag)
select 9150001 + (t.p - 9140001) * 6 + s.s - 1, t.p, s.code, s.name, s.s,
       'DONE', now(), '000000', '0'
from (select 9140001 p union select 9140002 union select 9140003) t
join (select 'CONCEPT' code, '概念' name, 1 s union select 'PLAN', '计划', 2 union select 'DEV', '开发', 3
      union select 'VALID', '验证', 4 union select 'LAUNCH', '发布', 5 union select 'LIFECYCLE', '生命周期', 6) s
where not exists (select 1 from project_stages where id = 9150001 + (t.p - 9140001) * 6 + s.s - 1);
-- 9140004 如门禁测试（CONCEPT DONE / PLAN IN_PROGRESS / 后续 NOT_STARTED）
insert into project_stages (id, project_id, stage_code, stage_name, sort_order, status, create_time, tenant_id, del_flag)
select 9150019 + t.s - 1, 9140004, t.code, t.name, t.s,
       case when t.s = 1 then 'DONE' when t.s = 2 then 'IN_PROGRESS' else 'NOT_STARTED' end,
       now(), '000000', '0'
from (select 'CONCEPT' code, '概念' name, 1 s union select 'PLAN', '计划', 2 union select 'DEV', '开发', 3
      union select 'VALID', '验证', 4 union select 'LAUNCH', '发布', 5 union select 'LIFECYCLE', '生命周期', 6) t
where not exists (select 1 from project_stages where id = 9150019 + t.s - 1);
-- 9140005 熵基互联+智能锁（CONCEPT/PLAN DONE / DEV IN_PROGRESS）
insert into project_stages (id, project_id, stage_code, stage_name, sort_order, status, create_time, tenant_id, del_flag)
select 9150025 + t.s - 1, 9140005, t.code, t.name, t.s,
       case when t.s <= 2 then 'DONE' when t.s = 3 then 'IN_PROGRESS' else 'NOT_STARTED' end,
       now(), '000000', '0'
from (select 'CONCEPT' code, '概念' name, 1 s union select 'PLAN', '计划', 2 union select 'DEV', '开发', 3
      union select 'VALID', '验证', 4 union select 'LAUNCH', '发布', 5 union select 'LIFECYCLE', '生命周期', 6) t
where not exists (select 1 from project_stages where id = 9150025 + t.s - 1);

-- ---- 7. 阶段动作（完结×12 + 运行态待办）----
-- 完结项目：每阶段评审 + 归档（DONE）共 36
insert into stage_actions (id, project_id, stage_id, action_code, action_name, owner_role, depth, status, is_blocking, actual_done_at, create_time, tenant_id, del_flag)
select 9160000 + (t.p - 9140001) * 12 + (s.s - 1) * 2 + k.k, t.p,
       9150001 + (t.p - 9140001) * 6 + s.s - 1,
       concat('A', k.k), concat(s.name, '阶段', case when k.k = 1 then '评审' else '交付物归档' end),
       'BOTH', 'DEEP', 'DONE', '0', now() - interval 170 day, now() - interval 400 day, '000000', '0'
from (select 9140001 p union select 9140002 union select 9140003) t
join (select 'CONCEPT' name, 1 s union select 'PLAN', 2 union select 'DEV', 3
      union select 'VALID', 4 union select 'LAUNCH', 5 union select 'LIFECYCLE', 6) s
join (select 1 k union select 2) k
where not exists (select 1 from stage_actions where id = 9160000 + (t.p - 9140001) * 12 + (s.s - 1) * 2 + k.k);
-- 9140004 如门禁测试（PLAN 阶段：2 超期 + 2 未来 + CONCEPT 2 DONE）
insert into stage_actions (id, project_id, stage_id, action_code, action_name, owner_role, depth, status, is_blocking, due_date, actual_done_at, create_time, tenant_id, del_flag)
select 9160037, 9140004, 9150020, 'P01', '市场准入合规清单梳理', 'MARKET_PM', 'DEEP', 'IN_PROGRESS', '0', now() - interval 5 day, null, now() - interval 30 day, '000000', '0' from dual
where not exists (select 1 from stage_actions where id = 9160037);
insert into stage_actions (id, project_id, stage_id, action_code, action_name, owner_role, depth, status, is_blocking, due_date, actual_done_at, create_time, tenant_id, del_flag)
select 9160038, 9140004, 9150020, 'P02', '渠道商对接名单确认', 'MARKET_PM', 'DEEP', 'DELAYED', '0', now() - interval 10 day, null, now() - interval 30 day, '000000', '0' from dual
where not exists (select 1 from stage_actions where id = 9160038);
insert into stage_actions (id, project_id, stage_id, action_code, action_name, owner_role, depth, status, is_blocking, due_date, actual_done_at, create_time, tenant_id, del_flag)
select 9160039, 9140004, 9150020, 'P03', '门禁协议对接联调', 'RD_PM', 'DEEP', 'NOT_STARTED', '0', now() + interval 14 day, null, now() - interval 30 day, '000000', '0' from dual
where not exists (select 1 from stage_actions where id = 9160039);
insert into stage_actions (id, project_id, stage_id, action_code, action_name, owner_role, depth, status, is_blocking, due_date, actual_done_at, create_time, tenant_id, del_flag)
select 9160040, 9140004, 9150020, 'P04', '认证送样准备', 'BOTH', 'DEEP', 'NOT_STARTED', '0', now() + interval 30 day, null, now() - interval 30 day, '000000', '0' from dual
where not exists (select 1 from stage_actions where id = 9160040);
insert into stage_actions (id, project_id, stage_id, action_code, action_name, owner_role, depth, status, is_blocking, due_date, actual_done_at, create_time, tenant_id, del_flag)
select 9160041, 9140004, 9150019, 'C01', '概念立项评审', 'BOTH', 'DEEP', 'DONE', '0', null, now() - interval 35 day, now() - interval 40 day, '000000', '0' from dual
where not exists (select 1 from stage_actions where id = 9160041);
insert into stage_actions (id, project_id, stage_id, action_code, action_name, owner_role, depth, status, is_blocking, due_date, actual_done_at, create_time, tenant_id, del_flag)
select 9160042, 9140004, 9150019, 'C02', '目标市场与竞品分析', 'MARKET_PM', 'DEEP', 'DONE', '0', null, now() - interval 32 day, now() - interval 40 day, '000000', '0' from dual
where not exists (select 1 from stage_actions where id = 9160042);
-- 9140005 熵基互联+智能锁（DEV 阶段：1 临期 + 1 未来 + 1 超期 + 2 DONE）
insert into stage_actions (id, project_id, stage_id, action_code, action_name, owner_role, depth, status, is_blocking, due_date, actual_done_at, create_time, tenant_id, del_flag)
select 9160043, 9140005, 9150027, 'D01', '智能锁通信协议评审', 'RD_PM', 'DEEP', 'IN_PROGRESS', '0', now() + interval 7 day, null, now() - interval 20 day, '000000', '0' from dual
where not exists (select 1 from stage_actions where id = 9160043);
insert into stage_actions (id, project_id, stage_id, action_code, action_name, owner_role, depth, status, is_blocking, due_date, actual_done_at, create_time, tenant_id, del_flag)
select 9160044, 9140005, 9150027, 'D02', '联动场景用例设计', 'MARKET_PM', 'DEEP', 'NOT_STARTED', '0', now() + interval 10 day, null, now() - interval 20 day, '000000', '0' from dual
where not exists (select 1 from stage_actions where id = 9160044);
insert into stage_actions (id, project_id, stage_id, action_code, action_name, owner_role, depth, status, is_blocking, due_date, actual_done_at, create_time, tenant_id, del_flag)
select 9160045, 9140005, 9150027, 'D03', '固件联调计划', 'RD_PM', 'DEEP', 'NOT_STARTED', '0', now() - interval 2 day, null, now() - interval 20 day, '000000', '0' from dual
where not exists (select 1 from stage_actions where id = 9160045);
insert into stage_actions (id, project_id, stage_id, action_code, action_name, owner_role, depth, status, is_blocking, due_date, actual_done_at, create_time, tenant_id, del_flag)
select 9160046, 9140005, 9150026, 'P01', '项目计划评审', 'BOTH', 'DEEP', 'DONE', '0', null, now() - interval 25 day, now() - interval 30 day, '000000', '0' from dual
where not exists (select 1 from stage_actions where id = 9160046);
insert into stage_actions (id, project_id, stage_id, action_code, action_name, owner_role, depth, status, is_blocking, due_date, actual_done_at, create_time, tenant_id, del_flag)
select 9160047, 9140005, 9150025, 'C01', '概念立项评审', 'BOTH', 'DEEP', 'DONE', '0', null, now() - interval 28 day, now() - interval 35 day, '000000', '0' from dual
where not exists (select 1 from stage_actions where id = 9160047);

-- ---- 回读校验 ----
select (select count(*) from persons where id between 9110001 and 9110013) persons_13,
       (select count(*) from product_groups where id between 9120001 and 9120006) groups_6,
       (select count(*) from products where id between 9130001 and 9130005) products_5,
       (select count(*) from projects where id between 9140001 and 9140005) projects_5,
       (select count(*) from project_stages where id between 9150001 and 9150030) stages_30,
       (select count(*) from stage_actions where id between 9160037 and 9160047
          and status in ('NOT_STARTED','IN_PROGRESS','DELAYED')) open_actions_7,
       (select count(*) from project_members where id between 9170001 and 9170010) members_10;
