-- P1-4.3 状态机并发锁：stage_actions 加 version 列（MyBatis-Plus @Version 乐观锁）
-- 说明：现有 69 动作实例全部为新列，默认 0 即可；transit() 必须走 SELECT（拿 version）→ UPDATE，
--      并发同 id 同时 transit 由 OptimisticLockerInnerInterceptor 仅 1 成功，其它抛异常被回滚。
-- 与 2026-09-04-ipd-p0-tables.sql 配套；运行前确认 stage_actions 已建表。

ALTER TABLE stage_actions
    ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（P1-4.3 状态机并发控制）' AFTER sop_id;

-- 兼容可能已存在的历史实例：补默认值（NOT NULL DEFAULT 0 已在列定义兜底，此 UPDATE 仅做明示归一）
UPDATE stage_actions SET version = 0 WHERE version IS NULL;
