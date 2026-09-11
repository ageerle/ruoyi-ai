-- =====================================================================
-- PERF-01 项目编码并发安全：uk_projects_code 唯一索引（QA-04-D2② 幂等化改写）
-- 背景：本脚本原样（ALTER TABLE ... ADD UNIQUE KEY）在「按主 DDL 建库」环境必报
--   ERROR 1061（duplicate key name）——基线 2026-09-04-ipd-p0-tables.sql 的
--   projects 建表已含 uk_projects_code (code)，脚本不可重复执行。
-- 改写：MySQL 8.0 无 DROP INDEX IF EXISTS / ADD INDEX IF NOT EXISTS，采用
--   information_schema.statistics 判断 + PREPARE 动态 SQL 的成熟模式：
--     * 索引不存在 → 补建（存量库迁移路径）；
--     * 索引已存在且列集为 (code) → 跳过（新库 / ipd_dev 已建路径）；
--     * 索引已存在但列集不同 → SIGNAL 45000 终止，人工核查（防同名异列静默漂移）。
-- 应用层：nextCode() 已有 synchronized 锁（ProjectService 单例 bean），
--   DB 层 uk_code 仅为竞态兜底（PERF-01）。
-- 日期：2026-09-05（QA-04-D2 幂等化改写）
-- =====================================================================

-- 1) 项目编码全局唯一（PERF-01）——幂等
SET @idx_exists := (
    SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'projects'
      AND index_name = 'uk_projects_code');
SET @idx_is_code := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'projects'
      AND index_name = 'uk_projects_code' AND column_name = 'code');
SET @ddl := IF(@idx_exists > 0 AND @idx_is_code = 0,
    'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''uk_projects_code already exists with different column set, manual check required''',
    IF(@idx_exists = 0,
       'ALTER TABLE projects ADD UNIQUE KEY uk_projects_code (code)',
       'SELECT 1 AS skip_uk_projects_code'));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =====================================================================
-- 2) 产品:项目 1:1（Q5）——【owner 裁决项，本脚本不执行】
-- ---------------------------------------------------------------------
-- 基线建表已含单列唯一键 uk_projects_product (product_id)。原脚本此处试图
-- ADD UNIQUE KEY uk_projects_product (product_id, del_flag)（复合键），同样必报
-- 1061，且与基线同名键列集冲突。是否切复合键属语义裁决，交 owner 定夺：
--
-- 方案 A（现状，基线维持）：单列唯一 uk_projects_product (product_id)
--   * 语义：product_id 全局唯一；软删行（del_flag='1'）仍占坑，
--     同一产品软删后无法新建项目占用同一 product_id；
--   * 配套要求：项目删除执行器必须回写 product_id=NULL 释放占坑（现状口径）；
--   * 优点：语义直白（product_id IS NULL 即释放）；DuplicateKeyException 含义单一
--     （「产品已被活跃项目占用」），PERF-04 计划的冲突转 ServiceException 改动小；
--   * 缺点：依赖执行器纪律，漏回写即占坑。
--
-- 方案 B：复合唯一 uk_projects_product (product_id, del_flag)
--   * 语义：活跃行（del_flag='0'）内 product_id 唯一，软删行可共存、不占坑；
--   * 优点：删除无需回写 product_id，释放由键语义自动完成；
--   * 缺点/风险：
--     - del_flag 为 CHAR(1) 仅 '0'/'1' 两值 → 同一 product_id 最多 1 活跃 + 1 软删，
--       第二次软删仍 1062 冲突，无法覆盖多次删除/重建场景；
--     - 需同步评估 ProjectService 冲突重试语义：DuplicateKeyException 将同时表示
--       「活跃占用」与「软删残留」，create() 的 selectCount 1:1 校验与异常翻译
--       （PERF-04 待办）须一并梳理，排障语义变差；
--     - 线上 ipd_dev 现为单列键，切换需 DROP + ADD 两次索引变更（共享主库，须 owner 排期）。
--
-- 推荐：维持方案 A（单列 + 执行器回写 product_id=NULL 释放占坑）。
--   理由：del_flag 两值域使方案 B 的「免占坑」收益只覆盖一轮删除/重建，边际收益低；
--   而 A 的占坑释放责任集中且已实现，冲突异常语义单一，无须触碰 ProjectService。
--   若 owner 选 B：需同步 2026-09-04-ipd-p0-tables.sql 建表键定义 + 本脚本追加
--   幂等 DROP/ADD 迁移 + PERF-04 冲突语义评估，三处一起改，不接受只改一处。
-- =====================================================================
