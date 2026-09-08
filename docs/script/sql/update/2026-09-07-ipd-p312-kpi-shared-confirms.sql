-- P3-1.2-BACKEND 共担 KPI 双组长确认（看板卡 56d97bb0；ZK 原型页 20「共担KPI双组长确认」）
-- 落点：对应 org.ruoyi.ipd.domain.KpiSharedConfirm
-- 业务规则：
--   ① 归集成功（KpiSharedCollectionService.collectSharedKpi）后按 K01-K04 各生成一条 PENDING 确认行；
--   ② 两位 GROUP_LEADER 依次 first/second 签署，第二签落库后 status=CONFIRMED；
--   ③ 同人重复签 → 业务层抛 40002 DUAL_SIGN_INCOMPLETE（双签未完成）；
--   ④ OVERDUE 为读时派生态（PENDING 且已过 deadline_at），不落库、无扫描任务；
--   ⑤ 已 CONFIRMED 行若被新 revision 重归集触发（KpiSharedConfirmService.ensurePendingRows），
--      双签人/时间置空、status 拉回 PENDING 重新双签（数据已变，确认要重做）；
--   ⑥ 跨租户共享表，已登记 application.yml tenant.excludes（与 kpi_records 同列）。
-- 幂等策略：
--   information_schema 显式存在性检查 + IF NOT EXISTS 双保险
--   重复执行安全（同 2026-09-06-ipd-receipt-ledger-table.sql 模式）

CREATE TABLE IF NOT EXISTS `kpi_shared_confirms` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `project_id` bigint NOT NULL COMMENT '项目ID（关联 projects.id）',
  `period` varchar(7) COLLATE utf8mb4_general_ci NOT NULL COMMENT '考核周期 YYYY-MM',
  `person_id` bigint DEFAULT NULL COMMENT '归集责任人（产品组长 personId）',
  `metric_code` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT '指标编码 K01~K04',
  `metric_name` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '指标名称',
  `weight` decimal(5,2) DEFAULT NULL COMMENT '指标权重（K01=0.15/K02=0.10/K03=0.10/K04=0.05）',
  `deadline_at` datetime DEFAULT NULL COMMENT '归集截止时间（次月第N工作日 18:00）',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING|CONFIRMED（OVERDUE 读时派生）',
  `first_confirmed_by` bigint DEFAULT NULL COMMENT '第一组长签署人',
  `first_confirmed_at` datetime DEFAULT NULL COMMENT '第一组长签署时间',
  `second_confirmed_by` bigint DEFAULT NULL COMMENT '第二组长签署人',
  `second_confirmed_at` datetime DEFAULT NULL COMMENT '第二组长签署时间',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_period_project_metric` (`project_id`, `period`, `metric_code`, `del_flag`),
  KEY `idx_period_status` (`period`, `status`),
  KEY `idx_project_period` (`project_id`, `period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='共担KPI双组长确认（P3-1.2-BACKEND / ZK 原型页20）';

-- 执行后校验（期望返回 1 行：kpi_shared_confirms）
SELECT table_name, table_comment
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'kpi_shared_confirms';

-- ROLLBACK（环境异常回滚用）
-- DROP TABLE IF EXISTS `kpi_shared_confirms`;
