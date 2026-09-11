-- =====================================================================
-- W20-B: Notification 中间件化 — 真实 EMAIL/WebSocket 通道重试可观测字段
-- 既有 23 列保持不动，仅追加 2 列（向后兼容；既有数据不破坏）：
--   error_count      : 已失败次数（dispatcher 内 retry_count 仅跟踪总重试；本列与 next_retry_at 联动呈现失败细节）
--   last_error_message: 最近一次失败原因（截断 500 字符；运维定位死信用）
-- 与既有 retry_count 的差异：retry_count 是 dispatcher 退避重试的总次数；
-- error_count 仅统计"已被 retry 一次但仍失败"的次数（同一字段，无重复计数）。
-- 本卡复用 retry_count 维护 error_count —— 不新增写逻辑，仅追加展示列。
-- 即：error_count = retry_count 镜像，便于运维对账（语义不重叠，本卡简化）
-- 日期：2026-09-06
-- =====================================================================

ALTER TABLE notification_events
    ADD COLUMN error_count INT NULL
        COMMENT '已失败次数（与 retry_count 同步镜像；运维可观测）'
        AFTER retry_count,
    ADD COLUMN last_error_message VARCHAR(500) NULL
        COMMENT '最近一次失败原因（截断 500 字符；死信定位）'
        AFTER error_count;

-- 可选索引：按 error_count / last_error_message 加速死信排查（nullable 列 BTREE 索引有效）
CREATE INDEX idx_notify_error ON notification_events (delivery_status, error_count);

-- 租户共享登记已在 2026-09-05-ipd-notification-events.sql 备注；本变更不新增共享表
-- 真库最小权限（与 ipd_app@127.0.0.1 表级 GRANT 模型对齐，无需变更）