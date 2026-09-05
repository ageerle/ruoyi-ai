-- PERF-01 项目编码并发安全：uk_code + uk_product 唯一索引
-- 说明：
--   * uk_code(code)        强制项目编码全局唯一，DB 层兜底 nextCode 竞态（应用层已加 synchronized 锁保护）
--   * uk_product(product_id, del_flag) 强制产品:项目 1:1（仅统计 del_flag='0' 的活跃项目）
-- 应用层：
--   * nextCode() 已有 synchronized 锁（ProjectService 单例 bean）
--   * create() 当前依赖 selectCount(eq productId, eq delFlag='0') 应用层 1:1 校验；
--     DB 唯一索引升级后建议改为捕 DuplicateKeyException 后转 ServiceException("产品已被项目占用")，
--     后续 PERF-04 实现。
-- 与 2026-09-04-ipd-p0-tables.sql 配套；运行前确认 projects 表已建。

-- 1) 项目编码全局唯一（PERF-01）
ALTER TABLE projects
    ADD UNIQUE KEY uk_projects_code (code);

-- 2) 产品:项目 1:1（Q5，仅统计活跃项目）
ALTER TABLE projects
    ADD UNIQUE KEY uk_projects_product (product_id, del_flag);
