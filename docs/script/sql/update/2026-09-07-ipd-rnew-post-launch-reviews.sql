-- R-NEW-ARCH-1 / R-NEW-SEC-6 收口（2026-09-07）：G5 上市 90 天复盘表 post_launch_reviews 建表迁移
-- 落点：对应 org.ruoyi.ipd.domain.PostLaunchReview（P2-5.6 卡片仅出设计稿，真库从未建表）
-- 现状证据（本卡开工前只读探针，2026-09-07 09:05 -07:00）：
--   ipd_dev@127.0.0.1:13306 的 information_schema.tables 中 post_launch_reviews 计数 = 0（表缺失）；
--   同库 launch_date_change_requests / kpi_shared_confirms / bonus_pools / handover_records 均存在。
--   → P2-5.6 当时只有 Mockito 单测绿，Service 写库路径在真库上不可用（属"代码已就位、表未上线"半套状态）。
-- 业务规则（不变，仅补持久层）：
--   ① scheduleReview：launchDate + 90d 生成 status=PENDING 待办；同 projectId 已有 PENDING ⇒ 复用不新建（幂等靠 Service 查询，非唯一约束）；
--   ② completeReview：填复盘数据 + status=COMPLETED + completed_at=now；已 COMPLETED 拒绝重复完成；
--   ③ assignee_id 取项目当前在任 MARKET_PM（ProjectMember exit_date IS NULL），无则回退 projects.create_by；
--   ④ 删除走 DeletionRequestService 软删除（del_flag），与其他业务表同款。
-- 租户口径：单企业私有部署（R8），tenant_id 常量 '000000' 无多租户语义，
--   必须与 persons/projects/handover_records 等一样登记 application.yml → tenant.excludes，
--   否则多租户插件自动追加 tenant_id 过滤使跨分组读审计链断行（本仓既有教训，见 tenant.excludes 段注释）。
-- 幂等策略：CREATE TABLE IF NOT EXISTS（可安全重放，同 2026-09-07-ipd-p312-kpi-shared-confirms.sql 模式）
-- 主键口径：实体用 @TableId(IdType.ASSIGN_ID) 由应用侧雪花生成 ⇒ 列不得带 AUTO_INCREMENT。

CREATE TABLE IF NOT EXISTS `post_launch_reviews` (
  `id` bigint NOT NULL COMMENT '主键（雪花，应用侧 ASSIGN_ID 生成）',
  `project_id` bigint NOT NULL COMMENT '项目ID（关联 projects.id）',
  `scheduled_at` datetime DEFAULT NULL COMMENT '复盘截止日 = launchDate + 90d',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING|COMPLETED|OVERDUE（OVERDUE 读时派生）',
  `assignee_id` bigint DEFAULT NULL COMMENT '当前负责 PM（personId，移交后跟随 ProjectMember 主 MARKET_PM）',
  `actual_revenue` decimal(14,2) DEFAULT NULL COMMENT '实际营收（复盘完成时填写）',
  `customer_feedback` varchar(2000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '客户反馈（≤2000 字符）',
  `kpi_achievement` varchar(2000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'KPI 达成情况（≤2000 字符）',
  `lessons` varchar(4000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '经验教训（≤4000 字符）',
  `completed_at` datetime DEFAULT NULL COMMENT '复盘完成时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000' COMMENT '租户编号（单企业部署常量）',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0 存在 1 已删，走 @TableLogic）',
  PRIMARY KEY (`id`),
  KEY `idx_plr_project_status` (`project_id`, `status`, `del_flag`) COMMENT 'scheduleReview 幂等查询 + 页47 PENDING 入口',
  KEY `idx_plr_assignee_status` (`assignee_id`, `status`) COMMENT '我的复盘待办列表',
  KEY `idx_plr_scheduled_at` (`scheduled_at`, `status`) COMMENT '超期扫描按截止日过滤'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='G5 上市90天复盘待办（P2-5.6 / R-NEW-ARCH-1 收口）';

-- ---------------------------------------------------------------------------
-- 应用账号最小权限（DCL）——本表为新增业务表，ipd_app 的表级 GRANT 清单不含它，
-- 必须由 DBA/owner 显式授权后应用才可读写；本轮不自动执行（DDL/DCL 均属真库变更）。
-- 账号与既有模型对齐：'ipd_app'@'127.0.0.1'（见 验收/P1-项5-DDL-apply核验-20260905.md、
-- 治理轮/DEF-5/PROPOSAL-01-脚本兜底.md 的表级 GRANT 口径）。
-- 授权语句（解注释后由 DBA 执行，执行后跑下方校验二）：
-- GRANT SELECT, INSERT, UPDATE ON `ipd_dev`.`post_launch_reviews` TO 'ipd_app'@'127.0.0.1';
-- FLUSH PRIVILEGES;
-- 说明：不给 DELETE —— 删除一律走 del_flag 软删（DeletionRequestService），与其余业务表一致。
-- ---------------------------------------------------------------------------

-- 执行后校验一（期望返回 1 行：post_launch_reviews）
SELECT table_name, table_comment
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'post_launch_reviews';

-- 执行后校验二（期望 4 个索引：PRIMARY + 上面三条 KEY；且 ipd_app 有 SELECT/INSERT/UPDATE）
SELECT index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS cols
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'post_launch_reviews'
GROUP BY index_name;

SELECT GRANTEE, PRIVILEGE_TYPE
FROM information_schema.TABLE_PRIVILEGES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'post_launch_reviews';

-- ---------------------------------------------------------------------------
-- ROLLBACK（环境异常回滚用；先 REVOKE 再 DROP，避免残留可连账号权限）
-- REVOKE SELECT, INSERT, UPDATE ON `ipd_dev`.`post_launch_reviews` FROM 'ipd_app'@'127.0.0.1';
-- DROP TABLE IF EXISTS `post_launch_reviews`;
-- 回滚同时须撤销 application.yml → tenant.excludes 中的 post_launch_reviews 一行，
-- 否则排除清单引用一张不存在的表（配置与 DDL 契约不一致，本卡验收项）。
-- ---------------------------------------------------------------------------
