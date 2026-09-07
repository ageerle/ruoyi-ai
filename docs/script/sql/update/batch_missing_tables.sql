-- 12 MISSING 表批量 DDL（排除已闭环的 allowance_ledgers 和设计选择的 action_catalog）
-- 执行前确认：SHOW TABLES LIKE '<name>' → 空

-- 1. bonus_allocations（P3-4.4 奖金分配台账）
CREATE TABLE IF NOT EXISTS `bonus_allocations` (
  `id` bigint NOT NULL,
  `bonus_pool_id` bigint NOT NULL COMMENT '关联 bonus_pools.id',
  `person_id` bigint NOT NULL COMMENT '被分配人',
  `role_in_project` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'MARKET_PM|RD_PM',
  `contribution_rate` decimal(5,4) NOT NULL COMMENT '贡献度（0.4000-0.6500）',
  `performance_coefficient` decimal(5,2) NOT NULL COMMENT '绩效系数',
  `allocated_amount` decimal(18,2) NOT NULL COMMENT '分配金额',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT|CONFIRMED|ARCHIVED',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_alloc_pool_person` (`bonus_pool_id`, `person_id`, `tenant_id`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='奖金分配台账（AC-INC-35）';

-- 2. project_scores（P3-6.x 五维贡献评定）
CREATE TABLE IF NOT EXISTS `project_scores` (
  `id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `person_id` bigint NOT NULL,
  `dimension_1` decimal(5,2) NOT NULL COMMENT '维度1（25%）',
  `dimension_2` decimal(5,2) NOT NULL COMMENT '维度2（25%）',
  `dimension_3` decimal(5,2) NOT NULL COMMENT '维度3（20%）',
  `dimension_4` decimal(5,2) NOT NULL COMMENT '维度4（20%）',
  `dimension_5` decimal(5,2) NOT NULL COMMENT '维度5（10%）',
  `total_score` decimal(6,2) GENERATED ALWAYS AS (`dimension_1`+`dimension_2`+`dimension_3`+`dimension_4`+`dimension_5`) STORED,
  `evaluator_id` bigint NOT NULL COMMENT '评定人（组长）',
  `evidence_urls` json DEFAULT NULL COMMENT '原始证据附件URL数组',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_score_project_person` (`project_id`, `person_id`, `tenant_id`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='五维贡献评定（AC-INC-25/26/27/28）';

-- 3. contributions（P3-6.1 贡献度评定表）
CREATE TABLE IF NOT EXISTS `contributions` (
  `id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `person_id` bigint NOT NULL,
  `market_contribution_rate` decimal(5,4) NOT NULL COMMENT '市场贡献度（0.40-0.65）',
  `rd_contribution_rate` decimal(5,4) GENERATED ALWAYS AS (1.0000 - `market_contribution_rate`) STORED COMMENT '研发贡献度（联动）',
  `self_evaluation` json DEFAULT NULL COMMENT '双PM自评',
  `leader_evaluation` json DEFAULT NULL COMMENT '组长评定',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT|SUBMITTED|APPROVED|ARCHIVED',
  `archive_version` int DEFAULT NULL COMMENT '归档版本号（P3-6.2）',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contrib_project_person` (`project_id`, `person_id`, `tenant_id`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='贡献度评定（AC-INC-25/26/27/28）';

-- 4. negative_feedbacks（P3-7.x 负面反馈台账）
CREATE TABLE IF NOT EXISTS `negative_feedbacks` (
  `id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `source` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '来源渠道',
  `content` text COLLATE utf8mb4_general_ci NOT NULL,
  `severity` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'LOW|MEDIUM|HIGH|CRITICAL',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN|INVESTIGATING|RESOLVED|CLOSED',
  `handler_id` bigint DEFAULT NULL,
  `resolution` text COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_feedback_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='负面反馈台账';

-- 5. requirement_pool（P2-6.x 需求池）
CREATE TABLE IF NOT EXISTS `requirement_pool` (
  `id` bigint NOT NULL,
  `project_id` bigint DEFAULT NULL COMMENT '关联项目（可为空=未立项需求）',
  `title` varchar(256) COLLATE utf8mb4_general_ci NOT NULL,
  `description` text COLLATE utf8mb4_general_ci,
  `source` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'CUSTOMER|MARKET|INTERNAL',
  `priority` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'MEDIUM' COMMENT 'LOW|MEDIUM|HIGH|CRITICAL',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED|REVIEWING|APPROVED|REJECTED|IMPLEMENTED',
  `submitter_id` bigint NOT NULL,
  `reviewer_id` bigint DEFAULT NULL,
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_req_project` (`project_id`),
  KEY `idx_req_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='需求池（AC-REQ-09）';

-- 6. ai_model_configs（P4-x AI 模型配置）
CREATE TABLE IF NOT EXISTS `ai_model_configs` (
  `id` bigint NOT NULL,
  `model_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `provider` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'OPENAI|AZURE|LOCAL',
  `api_key_encrypted` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `endpoint_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `config_json` json DEFAULT NULL COMMENT '模型参数配置',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_model_name` (`model_name`, `tenant_id`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI 模型配置';

-- 7. system_config_versions（P0-3.3 参数版本持久化）
CREATE TABLE IF NOT EXISTS `system_config_versions` (
  `id` bigint NOT NULL,
  `config_key` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置键',
  `config_value` text COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置值',
  `version` int NOT NULL COMMENT '版本号（递增）',
  `effective_from` datetime NOT NULL COMMENT '生效起始时间',
  `effective_to` datetime DEFAULT NULL COMMENT '生效截止时间（NULL=当前有效）',
  `is_immutable` tinyint(1) NOT NULL DEFAULT '1' COMMENT '不可变快照（AC-GLB-09）',
  `changed_by` bigint NOT NULL,
  `change_reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_version` (`config_key`, `version`, `tenant_id`),
  KEY `idx_config_effective` (`config_key`, `effective_from`, `effective_to`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='参数版本持久化（AC-GLB-09/10）';

-- 8. legacy_imports（P1-9.1 存量导入批次）
CREATE TABLE IF NOT EXISTS `legacy_imports` (
  `id` bigint NOT NULL,
  `batch_no` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '批次号',
  `source_system` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '来源系统',
  `import_status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING|PROCESSING|COMPLETE|FAILED',
  `total_count` int DEFAULT NULL,
  `success_count` int DEFAULT NULL,
  `error_count` int DEFAULT NULL,
  `error_details` json DEFAULT NULL COMMENT '逐行错误报告',
  `imported_by` bigint NOT NULL,
  `started_at` datetime DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL,
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_no` (`batch_no`, `tenant_id`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='存量导入批次（P1-9.1）';

-- 9. person_roles —— 2026-09-06 决议：本表 owner 裁决为「不建」（persons.role 单角色足够），
--    删除占位注释块（消除 QA-04 幻影 CREATE TABLE 误报）。
--    若后续需要多角色支持，按 persons.role -> person_roles 拆分迁移，独立 ALTER 单跑。
-- [owner-decision-not-to-build] 同步删除 application.yml tenant.excludes 中的 person_roles 登记
