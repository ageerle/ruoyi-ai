-- =====================================================================
-- P1-1.1：产品 ↔ 项目 双向 1:1 绑定修复（数据库层兜底）
--
-- 业务规则（AC-PROD-01）：一个产品同一时刻只能挂一个项目；一个项目同一时刻只能挂一个产品。
-- 三道防线：
--   1) Service 层条件 UPDATE（product 端 id=? AND project_id IS NULL；project 端 id=? AND product_id IS NULL）
--   2) Service 层重读自洽终态校验
--   3) DB UNIQUE INDEX（本脚本兜底）—— uk_products_project(project_id) + uk_projects_product(product_id)
--
-- 历史事实（2026-09-04 / 2026-09-05）：
--   uk_products_project / uk_projects_product 在 2026-09-04-ipd-p0-tables.sql 已创建；
--   2026-09-05-ipd-perf01-nextcode-unique.sql 加 uk_projects_code 同批补登。
--   本脚本为「隔离库兜底」——若线上某老库无 UK，重复执行安全（IF NOT EXISTS）。
--
-- 隔离库人工 apply；日期：2026-09-07
-- =====================================================================

-- 1) products.project_id UNIQUE（已存在则跳过）
set @has_uk_products_project := (
    select count(1) from information_schema.statistics
    where table_schema = database()
      and table_name = 'products'
      and index_name = 'uk_products_project'
);
set @sql := if(@has_uk_products_project = 0,
    'alter table products add unique key uk_products_project (project_id)',
    'select "uk_products_project 已存在，跳过" as msg');
prepare stmt from @sql; execute stmt; deallocate prepare stmt;

-- 2) projects.product_id UNIQUE（已存在则跳过）
set @has_uk_projects_product := (
    select count(1) from information_schema.statistics
    where table_schema = database()
      and table_name = 'projects'
      and index_name = 'uk_projects_product'
);
set @sql := if(@has_uk_projects_product = 0,
    'alter table projects add unique key uk_projects_product (product_id)',
    'select "uk_projects_product 已存在，跳过" as msg');
prepare stmt from @sql; execute stmt; deallocate prepare stmt;

-- 3) 回读校验（apply 后人工核对）
show indexes from products where key_name = 'uk_products_project';
show indexes from projects where key_name = 'uk_projects_product';

-- 4) 备注：uk_products_project / uk_projects_product 允许 NULL 多行（MySQL UNIQUE 对 NULL 不唯一），
--    即「产品未绑任何项目 / 项目未绑任何产品」可存在多行，符合 P1-1.1 业务规则。