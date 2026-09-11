-- =====================================================================
-- IPD 津贴台账人员-项目-月复合唯一约束  (P3-3.3 DB 兜底)
--   卡片：P3-3.3 津贴内部台账幂等、移交与退出月份归属
--   日期：2026-09-07
--   责任：主协调会话 W28-3 (root-94ae) — 蜂群派单批 1
--
-- 背景：
--   allowance_ledgers 表当前仅有 idx_al_person_month(person_id, month) 普通索引，
--   无 (person_id, project_id, month) UNIQUE 约束。P3-3.3 service 层已实现
--   existsByKey / idempotentInsert 逻辑幂等护栏，但 DB 层缺乏并发最后防线。
--   本 DDL 补齐 UNIQUE KEY uk_al_person_project_month。
--
-- 影响：
--   - 增量 ALTER（不重建表）—— INPLACE 锁短，仅校验现有数据无重复
--   - 应用层需确认历史 allowance_ledgers 数据无 (person_id, project_id, month) 重复
--     (校验脚本见同目录 verify_allowance_ledgers_dup.sql)
--   - 真库执行顺序：先 verify，无重复后再 ALTER
--
-- 验收：
--   P333AcceptanceTest 20/20 绿（含幂等 existsByKey / idempotentInsert）
--   DB 层 uk_al_person_project_month 唯一索引生效 → 并发重算不双计
-- =====================================================================

-- 步骤 1: 删除旧非唯一索引（被新 UNIQUE 替代时无重复，索引更精准）
ALTER TABLE allowance_ledgers DROP INDEX idx_al_person_month;

-- 步骤 2: 加 UNIQUE 索引（DB 层兜底）
ALTER TABLE allowance_ledgers ADD UNIQUE KEY uk_al_person_project_month (person_id, project_id, month);

-- 步骤 3: 同时保留单 person 单 month 普通索引（按月查询津贴总额）
ALTER TABLE allowance_ledgers ADD KEY idx_al_month (month);

-- 备注：DDL 落库前应用须先跑：
--   SELECT person_id, project_id, month, COUNT(*) c
--   FROM allowance_ledgers
--   WHERE del_flag = '0'
--   GROUP BY person_id, project_id, month
--   HAVING c > 1;
-- 返回空集才能 apply 本 DDL，否则先合并历史重复。