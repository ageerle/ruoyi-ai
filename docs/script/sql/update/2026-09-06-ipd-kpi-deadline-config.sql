-- =====================================================================
-- HIGH-4.1：KPI 月度归集截止日配置化（HIGH-4.1：kpi.monthlyDeadlineDay
--          KpiSharedCollectionService.resolveMonthlyDeadline 走 system_configs）
-- 关联：2026-09-04-ipd-p0-config-seed.sql 已含 row id=1948090431（默认5）
--       本 ALTER 幂等兜底（基于 uk_config_key），用于历史环境/回放/重放
-- 改动：补强说明 + 兜底默认5 + 现有行不动
-- 日期：2026-09-06
-- =====================================================================

INSERT INTO system_configs (
    config_key, config_value, value_type, default_value,
    description, tenant_id, del_flag, create_time, update_time
) VALUES (
    'kpi.monthlyDeadlineDay', '5', 'NUMBER', '5',
    '共担 KPI 月度归集截止日（次月第 N 个工作日 18:00，HIGH-4.1 走配置）',
    '000000', '0', NOW(), NOW()
)
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    update_time = NOW();