-- MySQL dump 10.13  Distrib 8.0.46, for Linux (aarch64)
--
-- Host: host.docker.internal    Database: ipd_dev
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `ipd_dev`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `ipd_dev` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `ipd_dev`;

--
-- Table structure for table `_ipd_schema_history`
--

DROP TABLE IF EXISTS `_ipd_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `_ipd_schema_history` (
  `version` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `checksum` char(64) COLLATE utf8mb4_general_ci NOT NULL,
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL,
  `applied_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `agent_info`
--

DROP TABLE IF EXISTS `agent_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agent_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '智能体ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户ID',
  `agent_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '智能体名称',
  `agent_describe` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '智能体描述（下拉展示用）',
  `agent_show` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '展示图标/头像URL',
  `model_id` bigint NOT NULL COMMENT '绑定的聊天模型ID（chat_model.id, category=chat）',
  `enable_thinking` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '是否启用深度思考(ReAct多子Agent)：0 否 1 是',
  `system_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '自定义系统提示词',
  `mcp_tool_ids` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '关联MCP工具ID列表（JSON数组，[Long]）',
  `skill_names` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '关联磁盘技能名列表（JSON数组，[String]）',
  `knowledge_ids` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '关联知识库ID列表（JSON数组，[Long]）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态：0 正常 1 停用',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_agent_tenant_id` (`tenant_id`) USING BTREE,
  KEY `idx_agent_model_id` (`model_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='智能体信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_documents`
--

DROP TABLE IF EXISTS `ai_documents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_documents` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `project_id` bigint NOT NULL,
  `doc_type` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `title` varchar(200) COLLATE utf8mb4_general_ci NOT NULL,
  `content` mediumtext COLLATE utf8mb4_general_ci,
  `model` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '生成模型',
  `token_prompt` int DEFAULT NULL,
  `token_completion` int DEFAULT NULL,
  `content_sha256` char(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '内容摘要 sha256 hex（P1-10.1 版本不可变锚点，写入时计算）',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'GENERATED' COMMENT 'GENERATED|REVIEWED|ARCHIVED（未审核不可归档）',
  `parent_version_id` bigint DEFAULT NULL COMMENT '版本链父文档',
  `version_no` int NOT NULL DEFAULT '1',
  `reviewed_by` bigint DEFAULT NULL COMMENT '审核人ID',
  `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
  `review_comment` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审阅意见',
  `archived_at` datetime DEFAULT NULL COMMENT '归档时间',
  `archived_by` bigint DEFAULT NULL COMMENT '归档人',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_doc_parent` (`parent_version_id`),
  KEY `idx_ai_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD AI 文档（生成+人工审核+版本链+token 统计）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_model_configs`
--

DROP TABLE IF EXISTS `ai_model_configs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_model_configs` (
  `id` bigint NOT NULL,
  `model_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `provider` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `api_key_encrypted` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `endpoint_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `config_json` json DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_model_name` (`model_name`,`tenant_id`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI模型配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `allowance_ledgers`
--

DROP TABLE IF EXISTS `allowance_ledgers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `allowance_ledgers` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `person_id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `month` varchar(7) COLLATE utf8mb4_general_ci NOT NULL COMMENT '台账月份 YYYY-MM',
  `locked_level` varchar(8) COLLATE utf8mb4_general_ci NOT NULL COMMENT '评级（绑定时锁定）',
  `base_amount` decimal(10,2) NOT NULL COMMENT 'allowance.L1..L5 基准',
  `final_amount` decimal(10,2) NOT NULL COMMENT '终额（多项目叠加、2 倍封顶 capMultiplier）',
  `cap_applied` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '是否触发封顶',
  `stop_reason` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '停发原因（<60 分/无产出 noOutput.days=60）',
  `stop_start_date` datetime DEFAULT NULL COMMENT '停发起始日期',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_al_person_project_month` (`person_id`,`project_id`,`month`),
  KEY `idx_al_month` (`month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 月度津贴台账（锁定评级/叠加/封顶/停发）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `audit_log_chain_heads`
--

DROP TABLE IF EXISTS `audit_log_chain_heads`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_log_chain_heads` (
  `chain_key` varchar(32) COLLATE utf8mb4_bin NOT NULL,
  `last_seq` bigint NOT NULL,
  `last_hash` char(64) COLLATE utf8mb4_bin NOT NULL,
  `next_seq` bigint NOT NULL,
  `initialized_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`chain_key`),
  CONSTRAINT `chk_audit_chain_sequence` CHECK (((`last_seq` >= 0) and (`next_seq` > `last_seq`)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Mutable transaction-locked allocator; audit_logs itself remains append-only';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `audit_logs`
--

DROP TABLE IF EXISTS `audit_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_logs` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `seq` bigint NOT NULL COMMENT '全局递增序号（hash 链顺序锚；①②③ 由 audit_log_chain_heads 原子分配，非 DB 自增）',
  `operator_id` bigint DEFAULT NULL,
  `operator_name` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `operator_role` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `action` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'CREATE|UPDATE|DELETE|APPROVE|REJECT|HANDOVER|LOGIN...',
  `entity_type` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `entity_id` bigint DEFAULT NULL,
  `before_data` longtext COLLATE utf8mb4_general_ci COMMENT '变更前载荷（DEF-6：原 json 列，改 longtext 保字节精确往返以自洽 hash 链；JSON 合法性由 AuditLogService.append 应用层护栏强制）',
  `after_data` longtext COLLATE utf8mb4_general_ci COMMENT '变更后载荷（DEF-6：同上）',
  `reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `prev_hash` char(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '前条 hash（链首为 64 个 0）',
  `curr_hash` char(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'SHA256(prevHash+本条内容)',
  `ip_address` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（唯一时间字段，只追加）',
  `hash_version` int DEFAULT NULL COMMENT 'NULL=legacy-v1,2=canonical-json-v2',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_audit_seq` (`seq`),
  KEY `idx_al_operator_seq` (`operator_id`,`seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 审计日志（只追加+SHA256 hash 链，AC-AUD-01）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bid_invitations`
--

DROP TABLE IF EXISTS `bid_invitations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bid_invitations` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `project_id` bigint NOT NULL,
  `mode` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PUBLIC' COMMENT '招标方式 ONE_TO_ONE|PUBLIC',
  `target_person_id` bigint DEFAULT NULL COMMENT '一对一指定研发PM',
  `title` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `content` text COLLATE utf8mb4_general_ci,
  `expire_at` datetime DEFAULT NULL COMMENT '有效期（bid.expireWarnDays=3 预警）',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN|SELECTED|EXPIRED|CLOSED',
  `selected_response_id` bigint DEFAULT NULL,
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `confirm_token` varchar(8) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'P1-5.2 遴选二次确认 token',
  `confirm_token_expires` datetime DEFAULT NULL COMMENT 'P1-5.2 token 24h 过期',
  PRIMARY KEY (`id`),
  KEY `idx_bi_project` (`project_id`),
  KEY `idx_bi_status_expire` (`status`,`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 招标单（组队招标）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bid_responses`
--

DROP TABLE IF EXISTS `bid_responses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bid_responses` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `invitation_id` bigint NOT NULL,
  `rd_pm_id` bigint DEFAULT NULL,
  `response_note` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING|ACCEPTED|REJECTED|WITHDRAWN',
  `responded_at` datetime DEFAULT NULL,
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_br_invitation` (`invitation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 应标记录（研发PM 应标）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bonus_allocations`
--

DROP TABLE IF EXISTS `bonus_allocations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bonus_allocations` (
  `id` bigint NOT NULL,
  `bonus_pool_id` bigint NOT NULL,
  `person_id` bigint NOT NULL,
  `role_in_project` varchar(16) COLLATE utf8mb4_general_ci NOT NULL,
  `contribution_rate` decimal(5,4) NOT NULL,
  `performance_coefficient` decimal(5,2) NOT NULL,
  `allocated_amount` decimal(18,2) NOT NULL,
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_alloc_pool_person` (`bonus_pool_id`,`person_id`,`tenant_id`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='奖金分配台账';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bonus_pools`
--

DROP TABLE IF EXISTS `bonus_pools`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bonus_pools` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `project_id` bigint NOT NULL COMMENT '项目（1:1）',
  `target_sales` decimal(18,2) NOT NULL COMMENT '目标销售额（回款口径 salesSource=RECEIPT）',
  `pool_rate` decimal(5,4) NOT NULL DEFAULT '0.0500' COMMENT '奖金池比例（bonus.poolRate）',
  `base_pool` decimal(18,2) DEFAULT NULL COMMENT '基础奖金池=目标销售额×5%',
  `coefficient` decimal(5,2) DEFAULT NULL COMMENT '项目系数（S/A/B，G1 双签决定）',
  `achievement_rate` decimal(5,2) DEFAULT NULL COMMENT '回款达成率%',
  `tier_coefficient` decimal(5,2) DEFAULT NULL COMMENT '达成率 6 档阶梯系数',
  `final_pool` decimal(18,2) DEFAULT NULL COMMENT '最终奖金池',
  `distributions` json DEFAULT NULL COMMENT '个人分配结果（五维贡献+绩效系数）',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT|CONFIRMED|DISTRIBUTED',
  `calculated_at` datetime DEFAULT NULL COMMENT '计算时间',
  `distributed_at` datetime DEFAULT NULL COMMENT '发放时间',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bp_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 项目奖金池（目标销售额×5%×系数→6档阶梯→分配）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cert_templates`
--

DROP TABLE IF EXISTS `cert_templates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cert_templates` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `country_code` varchar(8) COLLATE utf8mb4_general_ci NOT NULL COMMENT '国家/地区代码（如 SA）',
  `country_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '国家名（如 沙特阿拉伯）',
  `cert_name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '认证名（如 SABER）',
  `cert_authority` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `requirement_desc` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `is_mandatory` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '1',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ct_country` (`country_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 国别认证清单模板库（目标市场自动带出）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `chat_message`
--

DROP TABLE IF EXISTS `chat_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_message` (
  `id` bigint NOT NULL COMMENT '主键',
  `session_id` bigint DEFAULT NULL COMMENT '会话id',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '消息内容',
  `role` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '对话角色',
  `total_tokens` int DEFAULT '0' COMMENT '累计 Tokens',
  `model_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '模型名称',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户Id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='聊天消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `chat_model`
--

DROP TABLE IF EXISTS `chat_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_model` (
  `id` bigint NOT NULL COMMENT '主键',
  `category` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '模型分类',
  `model_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '模型名称',
  `provider_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '模型供应商',
  `model_describe` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '模型描述',
  `model_dimension` int DEFAULT NULL COMMENT '模型维度',
  `model_show` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否显示',
  `api_host` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '请求地址',
  `api_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '密钥',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户Id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='模型管理';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `chat_provider`
--

DROP TABLE IF EXISTS `chat_provider`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_provider` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `provider_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '厂商名称',
  `provider_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '厂商编码',
  `provider_icon` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '厂商图标',
  `provider_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '厂商描述',
  `api_host` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'API地址',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `version` int DEFAULT NULL COMMENT '版本',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标志',
  `update_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '更新IP',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户Id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `unique_provider_code` (`provider_code`,`tenant_id`,`del_flag`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2096618412485652483 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='厂商管理表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `chat_session`
--

DROP TABLE IF EXISTS `chat_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_session` (
  `id` bigint NOT NULL COMMENT '主键',
  `user_id` bigint DEFAULT NULL COMMENT '用户id',
  `session_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '会话标题',
  `session_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '会话内容',
  `create_dept` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `conversation_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '会话ID',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户Id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='会话管理';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `coefficient_change_requests`
--

DROP TABLE IF EXISTS `coefficient_change_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coefficient_change_requests` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `project_id` bigint NOT NULL COMMENT '目标项目',
  `proposed_coefficient` decimal(5,2) NOT NULL COMMENT '提议系数',
  `reason` varchar(500) COLLATE utf8mb4_general_ci NOT NULL COMMENT '定值理由（写审计）',
  `market_pm_id` bigint NOT NULL COMMENT '市场PM（联合提议方）',
  `rd_pm_id` bigint NOT NULL COMMENT '研发PM（联合提议方）',
  `proposer_id` bigint NOT NULL COMMENT '提交人（双PM之一或超管代提）',
  `status` varchar(24) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING_LEADER' COMMENT 'PENDING_LEADER|CONFIRMED|REJECTED',
  `leader_id` bigint DEFAULT NULL COMMENT '产品组长确认人',
  `leader_decision` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'APPROVE|REJECT',
  `leader_decided_at` datetime DEFAULT NULL COMMENT '组长决策时间',
  `leader_opinion` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组长意见',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ccr_project_status` (`project_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD S/B 级系数定值申请（AC-INC-15c）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `contribution_versions`
--

DROP TABLE IF EXISTS `contribution_versions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `contribution_versions` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `source_id` bigint NOT NULL COMMENT '来源 contributions.id',
  `project_id` bigint NOT NULL COMMENT '项目 ID',
  `version_no` int NOT NULL COMMENT '确认版次（同项目从 1 递增）',
  `status` varchar(24) NOT NULL COMMENT '快照时刻状态，恒为 CONFIRMED',
  `market_share` decimal(5,2) DEFAULT NULL COMMENT '市场 PM 比例（快照）',
  `rd_share` decimal(5,2) DEFAULT NULL COMMENT '研发 PM 比例（快照）',
  `market_self_initiation` decimal(5,2) DEFAULT NULL COMMENT '市场 PM 自评·立项主导（快照）',
  `market_self_innovation` decimal(5,2) DEFAULT NULL COMMENT '市场 PM 自评·差异化创新（快照）',
  `market_self_launch` decimal(5,2) DEFAULT NULL COMMENT '市场 PM 自评·上市节奏（快照）',
  `market_self_market_result` decimal(5,2) DEFAULT NULL COMMENT '市场 PM 自评·市场结果（快照）',
  `market_self_leadership` decimal(5,2) DEFAULT NULL COMMENT '市场 PM 自评·协同领导力（快照）',
  `rd_self_initiation` decimal(5,2) DEFAULT NULL COMMENT '研发 PM 自评·立项主导（快照）',
  `rd_self_innovation` decimal(5,2) DEFAULT NULL COMMENT '研发 PM 自评·差异化创新（快照）',
  `rd_self_launch` decimal(5,2) DEFAULT NULL COMMENT '研发 PM 自评·上市节奏（快照）',
  `rd_self_market_result` decimal(5,2) DEFAULT NULL COMMENT '研发 PM 自评·市场结果（快照）',
  `rd_self_leadership` decimal(5,2) DEFAULT NULL COMMENT '研发 PM 自评·协同领导力（快照）',
  `tier_coefficient` decimal(5,2) DEFAULT NULL COMMENT '五维度修正因子（快照）',
  `market_comment` varchar(500) DEFAULT NULL COMMENT '市场 PM 自评备注（快照）',
  `rd_comment` varchar(500) DEFAULT NULL COMMENT '研发 PM 自评备注（快照）',
  `leader_id` bigint DEFAULT NULL COMMENT '确认组长 ID（快照）',
  `leader_decision` varchar(16) DEFAULT NULL COMMENT 'APPROVE（快照）',
  `leader_decided_at` datetime DEFAULT NULL COMMENT '组长决策时间（快照）',
  `leader_opinion` varchar(500) DEFAULT NULL COMMENT '组长意见（快照）',
  `submitted_at` datetime DEFAULT NULL COMMENT '双 PM 自评完成时间（快照）',
  `archived_by` bigint DEFAULT NULL COMMENT '归档操作人',
  `archived_at` datetime DEFAULT NULL COMMENT '归档时间',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '多租户',
  `del_flag` char(1) DEFAULT '0' COMMENT '软删除（0 正常 1 已删）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contribution_version` (`project_id`,`version_no`),
  KEY `idx_contribution_version_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='贡献度确认归档快照（BR-INC-09 版本可追溯）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `contributions`
--

DROP TABLE IF EXISTS `contributions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `contributions` (
  `id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `person_id` bigint NOT NULL,
  `market_contribution_rate` decimal(5,4) NOT NULL,
  `market_share` decimal(18,4) DEFAULT NULL COMMENT '市场占比',
  `rd_share` decimal(18,4) DEFAULT NULL COMMENT '研发占比',
  `market_self_initiation` decimal(5,2) DEFAULT NULL COMMENT '市场自评_立项',
  `market_self_innovation` decimal(5,2) DEFAULT NULL COMMENT '市场自评_创新',
  `market_self_launch` decimal(5,2) DEFAULT NULL COMMENT '市场自评_发布',
  `market_self_market_result` decimal(5,2) DEFAULT NULL COMMENT '市场自评_市场结果',
  `market_self_leadership` decimal(5,2) DEFAULT NULL COMMENT '市场自评_领导力',
  `rd_self_initiation` decimal(5,2) DEFAULT NULL COMMENT '研发自评_立项',
  `rd_self_innovation` decimal(5,2) DEFAULT NULL COMMENT '研发自评_创新',
  `rd_self_launch` decimal(5,2) DEFAULT NULL COMMENT '研发自评_发布',
  `rd_self_market_result` decimal(5,2) DEFAULT NULL COMMENT '研发自评_市场结果',
  `rd_self_leadership` decimal(5,2) DEFAULT NULL COMMENT '研发自评_领导力',
  `tier_coefficient` decimal(5,2) DEFAULT NULL COMMENT '档次系数',
  `market_comment` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '市场评语',
  `rd_comment` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '研发评语',
  `leader_id` bigint DEFAULT NULL COMMENT '上级领导ID',
  `leader_decision` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '上级决定',
  `leader_decided_at` datetime DEFAULT NULL COMMENT '上级决定时间',
  `leader_opinion` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '上级意见',
  `submitted_at` datetime DEFAULT NULL COMMENT '提交时间',
  `rd_contribution_rate` decimal(5,4) GENERATED ALWAYS AS ((1.0000 - `market_contribution_rate`)) STORED,
  `self_evaluation` json DEFAULT NULL,
  `leader_evaluation` json DEFAULT NULL,
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT',
  `archive_version` int DEFAULT NULL,
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contrib_project_person` (`project_id`,`person_id`,`tenant_id`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='贡献度评定';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `correction_logs`
--

DROP TABLE IF EXISTS `correction_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `correction_logs` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `entity_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '实体类型',
  `entity_id` bigint NOT NULL COMMENT '实体ID',
  `field_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '字段名',
  `old_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '旧值',
  `new_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '新值',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修正原因',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作人姓名',
  `operated_at` datetime DEFAULT NULL COMMENT '操作时间',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '000000' COMMENT '租户',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_cl_entity` (`entity_type`,`entity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='勘误日志（CorrectionLog）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `deletion_requests`
--

DROP TABLE IF EXISTS `deletion_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `deletion_requests` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `entity_type` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '目标实体类型',
  `entity_id` bigint NOT NULL COMMENT '目标实体ID',
  `entity_snapshot` json DEFAULT NULL COMMENT '删除前快照',
  `reason` varchar(500) COLLATE utf8mb4_general_ci NOT NULL COMMENT '删除理由',
  `requester_id` bigint NOT NULL,
  `status` varchar(24) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT|LEADER_REVIEW|ADMIN_REVIEW|DELETED|REJECTED',
  `leader_id` bigint DEFAULT NULL,
  `leader_decision` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'APPROVE|REJECT',
  `leader_decided_at` datetime DEFAULT NULL,
  `leader_due_at` datetime DEFAULT NULL COMMENT '组长审核期限（deletion.leaderDeadlineDays=2 工作日）',
  `admin_id` bigint DEFAULT NULL,
  `admin_decision` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `admin_decided_at` datetime DEFAULT NULL,
  `admin_due_at` datetime DEFAULT NULL COMMENT '超管审核期限（deletion.adminDeadlineDays=2 工作日）',
  `executed_at` datetime DEFAULT NULL COMMENT '实际执行软删除时间',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_dr_entity` (`entity_type`,`entity_id`),
  KEY `idx_del_status_leader` (`status`,`leader_due_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 删除申请（两级审核，禁直接物理删除 G-02）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `deliverables`
--

DROP TABLE IF EXISTS `deliverables`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `deliverables` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `action_id` bigint NOT NULL COMMENT '所属动作实例',
  `project_id` bigint NOT NULL COMMENT '项目（冗余，便于查询）',
  `file_name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件名',
  `oss_id` bigint DEFAULT NULL COMMENT 'OSS 存储 ID（MinIO，ruoyi-common-oss）',
  `file_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `file_size` bigint DEFAULT NULL,
  `uploaded_by` bigint DEFAULT NULL,
  `uploaded_at` datetime DEFAULT NULL,
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_deliv_action` (`action_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 交付物（附件关联）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flow_category`
--

DROP TABLE IF EXISTS `flow_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flow_category` (
  `category_id` bigint NOT NULL COMMENT '流程分类ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '000000' COMMENT '租户编号',
  `parent_id` bigint DEFAULT '0' COMMENT '父流程分类id',
  `ancestors` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '祖级列表',
  `category_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程分类名称',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`category_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='流程分类';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flow_definition`
--

DROP TABLE IF EXISTS `flow_definition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flow_definition` (
  `id` bigint NOT NULL COMMENT '主键id',
  `flow_code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程编码',
  `flow_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程名称',
  `model_value` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'CLASSICS' COMMENT '设计器模型（CLASSICS经典模型 MIMIC仿钉钉模型）',
  `category` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程类别',
  `version` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程版本',
  `is_publish` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否发布（0未发布 1已发布 9失效）',
  `form_custom` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
  `form_path` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '审批表单路径',
  `activity_status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '流程激活状态（0挂起 1激活）',
  `listener_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '监听器类型',
  `listener_path` varchar(400) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '监听器路径',
  `ext` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '业务详情 存业务表对象json字符串',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新人',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标志',
  `tenant_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='流程定义表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flow_his_task`
--

DROP TABLE IF EXISTS `flow_his_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flow_his_task` (
  `id` bigint NOT NULL COMMENT '主键id',
  `definition_id` bigint NOT NULL COMMENT '对应flow_definition表的id',
  `instance_id` bigint NOT NULL COMMENT '对应flow_instance表的id',
  `task_id` bigint NOT NULL COMMENT '对应flow_task表的id',
  `node_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '开始节点编码',
  `node_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '开始节点名称',
  `node_type` tinyint(1) DEFAULT NULL COMMENT '开始节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
  `target_node_code` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '目标节点编码',
  `target_node_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '结束节点名称',
  `approver` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '审批人',
  `cooperate_type` tinyint(1) NOT NULL DEFAULT '0' COMMENT '协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)',
  `collaborator` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '协作人',
  `skip_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流转类型（PASS通过 REJECT退回 NONE无动作）',
  `flow_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
  `form_custom` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
  `form_path` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '审批表单路径',
  `message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '审批意见',
  `variable` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '任务变量',
  `ext` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '业务详情 存业务表对象json字符串',
  `create_time` datetime DEFAULT NULL COMMENT '任务开始时间',
  `update_time` datetime DEFAULT NULL COMMENT '审批完成时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标志',
  `tenant_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='历史任务记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flow_instance`
--

DROP TABLE IF EXISTS `flow_instance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flow_instance` (
  `id` bigint NOT NULL COMMENT '主键id',
  `definition_id` bigint NOT NULL COMMENT '对应flow_definition表的id',
  `business_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务id',
  `node_type` tinyint(1) NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
  `node_code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程节点编码',
  `node_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程节点名称',
  `variable` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '任务变量',
  `flow_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
  `activity_status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '流程激活状态（0挂起 1激活）',
  `def_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '流程定义json',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新人',
  `ext` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '扩展字段，预留给业务系统使用',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标志',
  `tenant_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='流程实例表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flow_instance_biz_ext`
--

DROP TABLE IF EXISTS `flow_instance_biz_ext`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flow_instance_biz_ext` (
  `id` bigint NOT NULL COMMENT '主键id',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '000000' COMMENT '租户编号',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `business_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '业务编码',
  `business_title` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '业务标题',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `instance_id` bigint DEFAULT NULL COMMENT '流程实例Id',
  `business_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '业务Id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='流程实例业务扩展表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flow_node`
--

DROP TABLE IF EXISTS `flow_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flow_node` (
  `id` bigint NOT NULL COMMENT '主键id',
  `node_type` tinyint(1) NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
  `definition_id` bigint NOT NULL COMMENT '流程定义id',
  `node_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程节点编码',
  `node_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程节点名称',
  `permission_flag` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '权限标识（权限类型:权限标识，可以多个，用@@隔开)',
  `node_ratio` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '流程签署比例值',
  `handler_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `coordinate` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '坐标',
  `any_node_skip` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '任意结点跳转',
  `listener_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '监听器类型',
  `listener_path` varchar(400) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '监听器路径',
  `form_custom` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
  `form_path` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '审批表单路径',
  `version` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '版本',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新人',
  `ext` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '节点扩展属性',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标志',
  `tenant_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '租户id',
  `handler_path` varchar(400) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '监听器路径',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='流程节点表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flow_skip`
--

DROP TABLE IF EXISTS `flow_skip`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flow_skip` (
  `id` bigint NOT NULL COMMENT '主键id',
  `definition_id` bigint NOT NULL COMMENT '流程定义id',
  `now_node_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '当前流程节点的编码',
  `now_node_type` tinyint(1) DEFAULT NULL COMMENT '当前节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
  `next_node_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '下一个流程节点的编码',
  `next_node_type` tinyint(1) DEFAULT NULL COMMENT '下一个节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
  `skip_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '跳转名称',
  `skip_type` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '跳转类型（PASS审批通过 REJECT退回）',
  `skip_condition` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '跳转条件',
  `coordinate` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '坐标',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新人',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标志',
  `tenant_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='节点跳转关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flow_spel`
--

DROP TABLE IF EXISTS `flow_spel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flow_spel` (
  `id` bigint NOT NULL COMMENT '主键id',
  `component_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '组件名称',
  `method_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '方法名',
  `method_params` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '参数',
  `view_spel` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '预览spel表达式',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标志',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='流程spel表达式定义表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flow_task`
--

DROP TABLE IF EXISTS `flow_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flow_task` (
  `id` bigint NOT NULL COMMENT '主键id',
  `definition_id` bigint NOT NULL COMMENT '对应flow_definition表的id',
  `instance_id` bigint NOT NULL COMMENT '对应flow_instance表的id',
  `node_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '节点编码',
  `node_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '节点名称',
  `node_type` tinyint(1) NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
  `flow_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
  `form_custom` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
  `form_path` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '审批表单路径',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '更新人',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标志',
  `tenant_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='待办任务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flow_user`
--

DROP TABLE IF EXISTS `flow_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flow_user` (
  `id` bigint NOT NULL COMMENT '主键id',
  `type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '人员类型（1待办任务的审批人权限 2待办任务的转办人权限 3待办任务的委托人权限）',
  `processed_by` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '权限人',
  `associated` bigint NOT NULL COMMENT '任务表id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建人',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标志',
  `tenant_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `user_processed_type` (`processed_by`,`type`) USING BTREE,
  KEY `user_associated` (`associated`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='流程用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gate_arbitrations`
--

DROP TABLE IF EXISTS `gate_arbitrations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gate_arbitrations` (
  `id` bigint NOT NULL COMMENT '主键（雪花算法）',
  `gate_id` bigint NOT NULL COMMENT 'Gate 实例ID',
  `round` int NOT NULL COMMENT '评审轮次（与冲突发生轮对齐）',
  `arbitrator_type` varchar(16) NOT NULL COMMENT 'GROUP_LEADER|SUPER_ADMIN',
  `arbitrator_id` bigint NOT NULL COMMENT '仲裁人/终裁人ID',
  `decision` varchar(16) DEFAULT NULL COMMENT 'APPROVE|REJECT（NULL=待裁，开仲裁时预落行）',
  `opinion` varchar(1000) DEFAULT NULL COMMENT '仲裁/终裁意见',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) DEFAULT '000000',
  `del_flag` char(1) DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ga_gate_round_arb` (`gate_id`,`round`,`arbitrator_id`),
  KEY `idx_ga_gate` (`gate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Gate 冲突仲裁与超管终裁意见（P2-5.4 AC-GATE-10）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gate_element_results`
--

DROP TABLE IF EXISTS `gate_element_results`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gate_element_results` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `gate_id` bigint NOT NULL COMMENT 'Gate 实例',
  `element_id` bigint NOT NULL COMMENT '要素定义',
  `result` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT '判定 PASS|CONDITIONAL|FAIL（✅/⚠️/❌）',
  `condition_note` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '带条件通过说明',
  `evidence_ref` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '判定证据附件引用（FAIL 必填 AC-GATE-02）',
  `leftover_item` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '遗留项跟踪',
  `responsible_person_id` bigint DEFAULT NULL COMMENT '条件遗留责任人（AC-GATE-16 CONDITIONAL 必填）',
  `leftover_due_at` datetime DEFAULT NULL,
  `closed_evidence` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '遗留关闭凭证（close 必填）',
  `leftover_status` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '遗留项状态 OPEN|CLOSED',
  `sign_due_at` datetime DEFAULT NULL COMMENT '签署到期',
  `element_snapshot` longtext COLLATE utf8mb4_general_ci COMMENT '要素快照',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ger_gate` (`gate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD Gate 要素逐项判定（含遗留项跟踪）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gate_review_elements`
--

DROP TABLE IF EXISTS `gate_review_elements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gate_review_elements` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `gate_code` varchar(8) COLLATE utf8mb4_general_ci NOT NULL COMMENT '适用 Gate G1..G5',
  `element_code` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT '要素编号',
  `element_name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '要素名',
  `pass_standard` text COLLATE utf8mb4_general_ci COMMENT '通过标准',
  `threshold_json` varchar(512) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '阈值配置 JSON 对象（键非空、值均为整数），如 {"minCustomerVerifications":3}',
  `sign_due_at` datetime DEFAULT NULL COMMENT '签署到期',
  `sign_extension_count` int DEFAULT NULL COMMENT '签署延期次数',
  `is_veto` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '是否否决项（14 项，命中无法提交通过）',
  `veto_dual_required` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '双否决位：1=该否决项命中需双签确认（定义层标记，评审侧 P2-5.2 消费）',
  `sort_order` int NOT NULL DEFAULT '0',
  `enabled` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '1',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'published' COMMENT '生命周期: draft/published/archived（页47）；draft 与 archived 对业务不可见',
  `version` int NOT NULL DEFAULT '1' COMMENT '发布版本号：新建草稿=0，每次 publish 递增',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_gate_element_code` (`element_code`),
  KEY `idx_gre_gate` (`gate_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD Gate 评审要素定义（33 项+14 否决项）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gate_review_observers`
--

DROP TABLE IF EXISTS `gate_review_observers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gate_review_observers` (
  `id` bigint NOT NULL,
  `gate_id` bigint DEFAULT NULL,
  `observer_id` bigint DEFAULT NULL,
  `role` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `invited_by` bigint DEFAULT NULL,
  `invited_at` datetime DEFAULT NULL,
  `attended` int DEFAULT NULL,
  `opinion` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_gro_gate` (`gate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Gate 评审观察员';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gate_reviews`
--

DROP TABLE IF EXISTS `gate_reviews`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gate_reviews` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `project_id` bigint DEFAULT NULL COMMENT '项目ID',
  `gate_id` bigint NOT NULL COMMENT 'Gate 实例',
  `reviewer_type` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT '签署方 MARKET_PM|RD_PM|GROUP_LEADER|SUPER_ADMIN',
  `reviewer_id` bigint NOT NULL,
  `decision` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '决定 APPROVE|REJECT|ABSTAIN',
  `opinion` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '意见（双方都提交前互不可见）',
  `signed_at` datetime DEFAULT NULL,
  `due_at` datetime NOT NULL COMMENT '签署期限（BR-GATE-04，3 天超时弃权）',
  `sign_due_at` datetime DEFAULT NULL COMMENT '签署到期',
  `sign_extension_count` int DEFAULT NULL COMMENT '签署延期次数',
  `round` int NOT NULL DEFAULT '1' COMMENT '评审轮次',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `gate_code` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'G1/G2/G3/G4/G5',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_gr_gate_type_round` (`gate_id`,`reviewer_type`,`round`),
  KEY `idx_gate_reviews_gate_code` (`gate_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD Gate 评审双签记录（每方一条）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gate_waivers`
--

DROP TABLE IF EXISTS `gate_waivers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gate_waivers` (
  `id` bigint NOT NULL COMMENT '主键(雪花)',
  `project_id` bigint NOT NULL COMMENT '豁免项目',
  `gate_code` varchar(8) COLLATE utf8mb4_general_ci NOT NULL COMMENT '豁免门禁 G1|G2|G3|G4|G5',
  `reason` varchar(500) COLLATE utf8mb4_general_ci NOT NULL COMMENT '豁免理由',
  `proposer_id` bigint NOT NULL COMMENT '提议人(产品组长 / 项目经理)',
  `proposer_role` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '提议人角色',
  `status` varchar(24) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT|PENDING_MARKET_LEADER|PENDING_RD_LEADER|PENDING_SUPER_ADMIN|APPROVED|REJECTED',
  `market_leader_id` bigint DEFAULT NULL COMMENT '市场组长审批人',
  `market_decision` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'APPROVE|REJECT',
  `market_decided_at` datetime DEFAULT NULL COMMENT '市场组长决策时间',
  `market_opinion` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '市场组长意见',
  `rd_leader_id` bigint DEFAULT NULL COMMENT '研发组长审批人',
  `rd_decision` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'APPROVE|REJECT',
  `rd_decided_at` datetime DEFAULT NULL COMMENT '研发组长决策时间',
  `rd_opinion` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '研发组长意见',
  `super_admin_id` bigint DEFAULT NULL COMMENT '超管终审人',
  `super_decision` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'APPROVE|REJECT',
  `super_decided_at` datetime DEFAULT NULL COMMENT '超管终审时间',
  `super_opinion` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '超管意见',
  `joint_decision` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '三签联合决策 APPROVE|REJECT(Q4=乙联合口径)',
  `due_at` datetime DEFAULT NULL COMMENT '审批期限(Q2=乙加列,3 自然日,与 BR-GATE-04 对齐)',
  `approved_at` datetime DEFAULT NULL COMMENT '生效时间',
  `rejected_at` datetime DEFAULT NULL COMMENT '驳回时间',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `version` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_gw_project` (`project_id`),
  KEY `idx_gw_status` (`status`),
  KEY `idx_gw_due` (`due_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 阶段门禁豁免申请(Q3-Q5 三签联合,联合决策)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gates`
--

DROP TABLE IF EXISTS `gates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gates` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `project_id` bigint NOT NULL COMMENT '项目',
  `gate_code` varchar(8) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'G1..G5',
  `status` varchar(24) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING|APPROVED|REJECTED|ABSTAINED_TIMEOUT',
  `current_round` int NOT NULL DEFAULT '1' COMMENT '当前评审轮次（BR-GATE-06：第3轮组长列席/第5轮超管介入）',
  `gate_coefficient` decimal(5,2) DEFAULT NULL COMMENT 'Gate 系数（G1 双签决定，bonus.coefficientDecider）',
  `element_snapshot` longtext COLLATE utf8mb4_general_ci COMMENT 'P2-5.1 提交时冻结的要素定义快照 JSON（后续编辑/停用不影响在途评审）',
  `planned_at` datetime DEFAULT NULL,
  `started_at` datetime DEFAULT NULL,
  `sign_due_at` datetime DEFAULT NULL COMMENT '签署期限（BR-GATE-04 3 自然日；submit/reopen 起算，超管可延长 AC-GATE-21）',
  `sign_extension_count` int NOT NULL DEFAULT '0' COMMENT '签署期限已延长次数（AC-GATE-21 上限 3）',
  `concluded_at` datetime DEFAULT NULL,
  `materials_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '材料URL',
  `meeting_minutes_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '会议纪要URL',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_gates_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD Gate 实例（五大联合评审）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gen_table`
--

DROP TABLE IF EXISTS `gen_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gen_table` (
  `table_id` bigint NOT NULL COMMENT '编号',
  `data_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '数据源名称',
  `table_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '表名称',
  `table_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '表描述',
  `sub_table_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '关联子表的表名',
  `sub_table_fk_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '子表关联的外键名',
  `class_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '实体类名称',
  `tpl_category` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作）',
  `package_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '生成包路径',
  `module_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '生成模块名',
  `business_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '生成业务名',
  `function_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '生成功能名',
  `function_author` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '生成功能作者',
  `gen_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
  `gen_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
  `options` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '其它生成选项',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`table_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='代码生成业务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `gen_table_column`
--

DROP TABLE IF EXISTS `gen_table_column`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gen_table_column` (
  `column_id` bigint NOT NULL COMMENT '编号',
  `table_id` bigint DEFAULT NULL COMMENT '归属表编号',
  `column_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '列名称',
  `column_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '列描述',
  `column_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '列类型',
  `java_type` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'JAVA类型',
  `java_field` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'JAVA字段名',
  `is_pk` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否主键（1是）',
  `is_increment` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否自增（1是）',
  `is_required` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否必填（1是）',
  `is_insert` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否为插入字段（1是）',
  `is_edit` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否编辑字段（1是）',
  `is_list` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否列表字段（1是）',
  `is_query` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '是否查询字段（1是）',
  `query_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'EQ' COMMENT '查询方式（等于、不等于、大于、小于、范围）',
  `html_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
  `dict_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '字典类型',
  `sort` int DEFAULT NULL COMMENT '排序',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`column_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='代码生成业务表字段';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `handover_records`
--

DROP TABLE IF EXISTS `handover_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `handover_records` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `handover_type` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT '类型 PROJECT|SUPER_ADMIN|BATCH',
  `from_person_id` bigint NOT NULL,
  `to_person_id` bigint DEFAULT NULL COMMENT '承接人',
  `handover_role` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '移交角色维度 RD_PM或MARKET_PM（AC-HAND-06 仅目标角色变更）',
  `project_id` bigint DEFAULT NULL,
  `scope` json DEFAULT NULL COMMENT '移交范围（角色独立移交：市场PM/研发PM 各自数据跟随）',
  `note` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '移交说明（≤1000字符，spec 字段模型）',
  `rollback_reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '回退原因',
  `rollback_at` datetime DEFAULT NULL COMMENT '回退时间',
  `archived_at` datetime DEFAULT NULL COMMENT '归档时间',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT|CONFIRMED|COMPLETED',
  `confirmed_at` datetime DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL,
  `deadline_at` datetime DEFAULT NULL COMMENT '截止时间',
  `last_remind_at` datetime DEFAULT NULL COMMENT '最近提醒时间',
  `escalated_at` datetime DEFAULT NULL COMMENT '升级时间',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_hr_from` (`from_person_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 移交记录（先移交后禁用 BR-HAND）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ipd_business_config`
--

DROP TABLE IF EXISTS `ipd_business_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ipd_business_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_key` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `config_value` text COLLATE utf8mb4_general_ci,
  `value_type` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `scope` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `enabled` int DEFAULT NULL,
  `version` int DEFAULT NULL,
  `cache_ttl` int DEFAULT NULL,
  `description` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ibc_key` (`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 业务配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ipd_business_config_versions`
--

DROP TABLE IF EXISTS `ipd_business_config_versions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ipd_business_config_versions` (
  `id` bigint NOT NULL,
  `config_id` bigint DEFAULT NULL,
  `config_key` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `config_value` text COLLATE utf8mb4_general_ci,
  `version` int DEFAULT NULL,
  `enabled` int DEFAULT NULL,
  `effective_from` datetime DEFAULT NULL,
  `effective_to` datetime DEFAULT NULL,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ibcv_config_id` (`config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 业务配置版本';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `knowledge_attach`
--

DROP TABLE IF EXISTS `knowledge_attach`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `knowledge_attach` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `knowledge_id` bigint NOT NULL COMMENT '知识库ID',
  `oss_id` bigint DEFAULT NULL COMMENT '对象存储ID',
  `doc_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '文档ID',
  `file_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '文件SHA-256摘要',
  `name` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '附件名称',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '附件类型',
  `create_dept` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '部门',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户Id',
  `status` tinyint DEFAULT '0' COMMENT '解析状态: 0待解析, 1解析中, 2已解析, 3解析失败',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_kname` (`knowledge_id`,`name`) USING BTREE,
  UNIQUE KEY `uk_knowledge_file_hash` (`knowledge_id`,`file_hash`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2033199209203183619 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='知识库附件';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `knowledge_fragment`
--

DROP TABLE IF EXISTS `knowledge_fragment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `knowledge_fragment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `fid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '向量库片段ID',
  `idx` int NOT NULL COMMENT '片段索引下标',
  `doc_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '文档ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文档内容',
  `create_dept` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '部门',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户Id',
  `knowledge_id` bigint DEFAULT NULL COMMENT '知识库ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_fid` (`fid`) USING BTREE,
  KEY `idx_doc_id` (`doc_id`) USING BTREE,
  KEY `idx_knowledge_id` (`knowledge_id`) USING BTREE,
  FULLTEXT KEY `ft_content` (`content`) /*!50100 WITH PARSER `ngram` */ 
) ENGINE=InnoDB AUTO_INCREMENT=2033199209131880451 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='知识片段';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `knowledge_info`
--

DROP TABLE IF EXISTS `knowledge_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `knowledge_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL DEFAULT '0' COMMENT '用户ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '知识库名称',
  `share` tinyint DEFAULT NULL COMMENT '是否公开知识库（0 否 1是）',
  `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '知识库描述',
  `separator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '知识分隔符',
  `overlap_char` int DEFAULT NULL COMMENT '重叠字符数',
  `retrieve_limit` int DEFAULT NULL COMMENT '知识库中检索的条数',
  `similarity_threshold` double DEFAULT '0.5' COMMENT '相似度阈值',
  `text_block_size` int DEFAULT NULL COMMENT '文本块大小',
  `vector_model` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '向量库',
  `embedding_model` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '向量模型',
  `create_dept` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '部门',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户Id',
  `enable_rerank` tinyint DEFAULT '0' COMMENT '是否启用重排序（0否 1是）',
  `rerank_score_threshold` double DEFAULT NULL COMMENT '重排序相关性分数阈值',
  `rerank_top_n` int DEFAULT NULL COMMENT '重排序后返回的文档数量',
  `rerank_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '重排序模型名称',
  `enable_hybrid` tinyint(1) DEFAULT '0' COMMENT '是否启用混合检索',
  `hybrid_alpha` double DEFAULT '0.5' COMMENT '混合检索权重比例 (0.0=纯向量, 1.0=纯关键词)',
  `system_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '系统提示词',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tenant_user` (`tenant_id`,`user_id`) USING BTREE,
  KEY `idx_tenant_share` (`tenant_id`,`share`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2097275993856180226 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='知识库';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `kpi_records`
--

DROP TABLE IF EXISTS `kpi_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `kpi_records` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `project_id` bigint DEFAULT NULL,
  `person_id` bigint NOT NULL,
  `kpi_type` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'FUNCTIONAL|SHARED（共担四项归集由产品组长）',
  `period` varchar(7) COLLATE utf8mb4_general_ci NOT NULL COMMENT '考核周期 YYYY-MM（每月 5 日截止 kpi.monthlyDeadlineDay）',
  `functional_score` decimal(5,2) DEFAULT NULL,
  `shared_detail` json DEFAULT NULL COMMENT '共担四项明细',
  `comprehensive_score` decimal(5,2) DEFAULT NULL COMMENT '综合得分（kpi.functionalWeight=0.6/0.4）',
  `segment` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '在研分段',
  `revision` int DEFAULT NULL COMMENT '版本',
  `scored_by` bigint DEFAULT NULL,
  `scored_at` datetime DEFAULT NULL COMMENT '评分时间',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_kpi_person_period` (`person_id`,`period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD KPI 记录（功能+共担）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `kpi_rule_snapshots`
--

DROP TABLE IF EXISTS `kpi_rule_snapshots`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `kpi_rule_snapshots` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `version` bigint DEFAULT NULL COMMENT '规则版本号',
  `effective_from` datetime DEFAULT NULL COMMENT '生效起',
  `effective_to` datetime DEFAULT NULL COMMENT '生效止',
  `rule_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '规则 JSON 快照',
  `created_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '000000' COMMENT '租户',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_krs_version` (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='KPI 规则快照（KpiRuleSnapshot）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `kpi_shared_confirms`
--

DROP TABLE IF EXISTS `kpi_shared_confirms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `kpi_shared_confirms` (
  `id` bigint NOT NULL,
  `project_id` bigint DEFAULT NULL,
  `period` varchar(7) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `person_id` bigint DEFAULT NULL,
  `metric_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `metric_name` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `weight` decimal(5,2) DEFAULT NULL,
  `deadline_at` datetime DEFAULT NULL,
  `status` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `first_confirmed_by` bigint DEFAULT NULL,
  `first_confirmed_at` datetime DEFAULT NULL,
  `second_confirmed_by` bigint DEFAULT NULL,
  `second_confirmed_at` datetime DEFAULT NULL,
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ksc_proj_period` (`project_id`,`period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='KPI 共担确认';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `launch_date_change_requests`
--

DROP TABLE IF EXISTS `launch_date_change_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `launch_date_change_requests` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `project_id` bigint NOT NULL COMMENT '目标项目',
  `proposed_launch_date` datetime NOT NULL COMMENT '提议上市日期',
  `previous_launch_date` datetime DEFAULT NULL COMMENT '变更前上市日期（可空=首次录入）',
  `reason` varchar(500) COLLATE utf8mb4_general_ci NOT NULL COMMENT '变更理由（写审计）',
  `proposer_id` bigint NOT NULL COMMENT '提议人',
  `proposer_role` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'MARKET_PM|RD_PM|SUPER_ADMIN',
  `status` varchar(24) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING_SECOND' COMMENT 'PENDING_SECOND|CONFIRMED|REJECTED',
  `confirmer_id` bigint DEFAULT NULL COMMENT '第二签确认人',
  `confirmer_role` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '确认人角色',
  `confirmed_at` datetime DEFAULT NULL COMMENT '确认时间',
  `decision` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'APPROVE|REJECT',
  `opinion` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '确认/驳回意见',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号（P1 项1b 并发第二签）',
  `pending_project_id` bigint GENERATED ALWAYS AS (if(((`status` = _utf8mb4'PENDING_SECOND') and (ifnull(`del_flag`,_utf8mb4'0') = _utf8mb4'0')),`project_id`,NULL)) STORED COMMENT '部分唯一索引等价列：仅活跃在途申请=project_id，否则 NULL',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ldcr_pending_project` (`pending_project_id`),
  KEY `idx_ldcr_project_status` (`project_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 上市日期变更双签申请（AC-INC-33）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `legacy_imports`
--

DROP TABLE IF EXISTS `legacy_imports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `legacy_imports` (
  `id` bigint NOT NULL,
  `batch_no` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `source_system` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `import_status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING',
  `total_count` int DEFAULT NULL,
  `success_count` int DEFAULT NULL,
  `error_count` int DEFAULT NULL,
  `error_details` json DEFAULT NULL,
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
  UNIQUE KEY `uk_batch_no` (`batch_no`,`tenant_id`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='存量导入批次';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mcp_market_info`
--

DROP TABLE IF EXISTS `mcp_market_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mcp_market_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '市场ID',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '市场名称',
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '市场URL',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '市场描述',
  `auth_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '认证配置（JSON格式）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'ENABLED' COMMENT '状态：ENABLED-启用, DISABLED-禁用',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户编号',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_name` (`name`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='MCP市场表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mcp_market_tool`
--

DROP TABLE IF EXISTS `mcp_market_tool`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mcp_market_tool` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `market_id` bigint NOT NULL COMMENT '市场ID',
  `tool_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工具名称',
  `tool_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '工具描述',
  `tool_version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工具版本',
  `tool_metadata` json DEFAULT NULL COMMENT '工具元数据（JSON格式）',
  `is_loaded` tinyint(1) DEFAULT '0' COMMENT '是否已加载到本地',
  `local_tool_id` bigint DEFAULT NULL COMMENT '关联的本地工具ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_market_id` (`market_id`) USING BTREE,
  KEY `idx_tool_name` (`tool_name`) USING BTREE,
  KEY `idx_is_loaded` (`is_loaded`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='MCP市场工具关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mcp_tool_info`
--

DROP TABLE IF EXISTS `mcp_tool_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mcp_tool_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工具ID',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工具名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '工具描述',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'LOCAL' COMMENT '工具类型：LOCAL-本地, REMOTE-远程, BUILTIN-内置',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'ENABLED' COMMENT '状态：ENABLED-启用, DISABLED-禁用',
  `config_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '配置信息（JSON格式）',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '000000' COMMENT '租户编号',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_name` (`name`) USING BTREE,
  KEY `idx_type` (`type`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='MCP工具表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `multi_project_capacity_approvals`
--

DROP TABLE IF EXISTS `multi_project_capacity_approvals`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `multi_project_capacity_approvals` (
  `id` bigint NOT NULL COMMENT '主键(雪花)',
  `applicant_id` bigint NOT NULL COMMENT '申请人(PM)',
  `applicant_role` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'MARKET_PM|RD_PM',
  `target_project_id` bigint NOT NULL COMMENT '被消费的待建项目',
  `active_project_count` int NOT NULL COMMENT '申请时本人活动项目数(Q13=甲 ≥3)',
  `reason` varchar(500) COLLATE utf8mb4_general_ci NOT NULL COMMENT '产能说明',
  `status` varchar(24) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING_GROUP_LEADER' COMMENT 'PENDING_GROUP_LEADER|PENDING_SUPER_ADMIN|APPROVED|REJECTED|CONSUMED|EXPIRED',
  `consumed_at` datetime DEFAULT NULL COMMENT '消费时间(挂在目标项目下,Q13 一单一项目)',
  `consumed_project_id` bigint DEFAULT NULL COMMENT '实际消费项目(冗余,便于追溯)',
  `group_leader_id` bigint DEFAULT NULL COMMENT '组长审批人',
  `group_decision` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'APPROVE|REJECT',
  `group_decided_at` datetime DEFAULT NULL COMMENT '组长决策时间',
  `group_opinion` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组长意见',
  `super_admin_id` bigint DEFAULT NULL COMMENT '超管终审',
  `super_decision` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'APPROVE|REJECT',
  `super_decided_at` datetime DEFAULT NULL COMMENT '终审时间',
  `super_opinion` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '终审意见',
  `due_at` datetime DEFAULT NULL COMMENT '审批期限(Q2=乙加列,3 自然日)',
  `approved_at` datetime DEFAULT NULL COMMENT '审批通过时间(此后待消费)',
  `rejected_at` datetime DEFAULT NULL COMMENT '驳回时间',
  `valid_until` datetime DEFAULT NULL COMMENT '审批后有效期(Q13 一单一项目消费,默认 30 天)',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `version` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_mpca_applicant` (`applicant_id`),
  KEY `idx_mpca_target` (`target_project_id`),
  KEY `idx_mpca_status` (`status`),
  KEY `idx_mpca_due` (`due_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 多项目产能备案(≥3 活动项目触发,一单一消费,非长期资格)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `negative_feedbacks`
--

DROP TABLE IF EXISTS `negative_feedbacks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `negative_feedbacks` (
  `id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `source` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `content` text COLLATE utf8mb4_general_ci NOT NULL,
  `severity` varchar(16) COLLATE utf8mb4_general_ci NOT NULL,
  `trigger_type` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '触发类型',
  `main_role` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主角色',
  `main_person_id` bigint DEFAULT NULL COMMENT '主人员ID',
  `related_role` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联角色',
  `related_person_id` bigint DEFAULT NULL COMMENT '关联人员ID',
  `main_execution` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主执行',
  `related_execution` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联执行',
  `bonus_disqualify` int DEFAULT NULL COMMENT '奖金取消',
  `tier_delta` decimal(5,2) DEFAULT NULL COMMENT '档位变化',
  `trigger_month` varchar(7) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '触发月份',
  `recovery_month` varchar(7) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '恢复月份',
  `trigger_evidence` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '触发证据',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'OPEN',
  `triggered_by` bigint DEFAULT NULL COMMENT '触发人',
  `decided_by` bigint DEFAULT NULL COMMENT '决策人',
  `decided_at` datetime DEFAULT NULL COMMENT '决策时间',
  `lifted_by` bigint DEFAULT NULL COMMENT '解除人',
  `lifted_at` datetime DEFAULT NULL COMMENT '解除时间',
  `decision_comment` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '决策意见',
  `handler_id` bigint DEFAULT NULL,
  `resolution` text COLLATE utf8mb4_general_ci,
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notification_events`
--

DROP TABLE IF EXISTS `notification_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_events` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `receiver_id` bigint NOT NULL COMMENT '接收者（persons.id；收件箱仅本人可见 AC-TEAM-01）',
  `event_type` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '事件类型（目录见 NotificationService.Types：BID_INVITED|BID_WON|BID_LOST|BID_EXPIRING_SOON|BID_SELECT_OVERDUE|BID_CONDITIONS_CHANGED|GATE_REJECTED|GATE_SIGN_SOON|G5_REVIEW_TODO|GATE_CONDITION_OVERDUE|DEL_CROSS_GROUP_CC|DEL_REJECTED|DEL_REVIEW_OVERDUE）',
  `kind` varchar(8) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'FYI' COMMENT 'FYI=跨组知会 / ACTION=可执行审批行动（两者严格分开）',
  `source_type` varchar(40) COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务来源表：bid_invitations|gates|gate_reviews|deletion_requests|projects',
  `source_id` bigint NOT NULL COMMENT '业务来源行ID',
  `dedup_key` varchar(160) COLLATE utf8mb4_general_ci NOT NULL COMMENT '幂等去重键 = source_type:event_type:source_id:receiver_id',
  `title` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '标题（页03 站内信列表）',
  `content` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '正文',
  `action_url` varchar(300) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '行动跳转链接（kind=ACTION 时填写）',
  `channel` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'MOCK' COMMENT '投递渠道（一期 MOCK，标识清楚防误当真实发送）',
  `delivery_status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING|SENT|FAILED|DEAD',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '已重试次数（达上限转 DEAD）',
  `next_retry_at` datetime DEFAULT NULL COMMENT '下次可投递时间（指数退避；PENDING 为 NULL）',
  `read_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '已读：0未读 1已读',
  `read_at` datetime DEFAULT NULL COMMENT '已读时间',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `target_channel` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'INBOX/EMAIL/WEBSOCKET；NULL=INBOX',
  `locale` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'zh-CN/en-US 等；NULL=zh-CN',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notify_dedup` (`dedup_key`),
  KEY `idx_notify_inbox` (`receiver_id`,`read_flag`,`create_time`),
  KEY `idx_notify_dispatch` (`delivery_status`,`next_retry_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 站内通知与可执行待办事件（OPS-05 outbox）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `persons`
--

DROP TABLE IF EXISTS `persons`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `persons` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '姓名',
  `employee_no` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '工号',
  `person_type` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '人员类型 MARKET_PM|RD_PM|GROUP_LEADER|SUPER_ADMIN',
  `group_id` bigint DEFAULT NULL COMMENT '所属产品组',
  `level` varchar(8) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '职级 L1..L5（API 唯一权威源 B6）',
  `level_source` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '等级来源（仅 API）',
  `level_updated_at` datetime DEFAULT NULL COMMENT '等级最近变更时间（B6 新旧额度判定）',
  `account_status` varchar(32) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '账号状态 ACTIVE|FROZEN_PENDING_HANDOVER|DISABLED|RESIGNED',
  `employment_status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '在职状态 ACTIVE|RESIGNED',
  `wecom_user_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '企微绑定ID',
  `wecom_bound_at` datetime DEFAULT NULL COMMENT '企微绑定时间',
  `username` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '登录用户名（=姓名）',
  `password_hash` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '密码哈希',
  `must_change_pwd` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '1' COMMENT '首登强制改密（1是 0否）',
  `last_login_at` datetime DEFAULT NULL COMMENT '最近登录时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000' COMMENT '租户ID',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_persons_employee_no` (`employee_no`),
  UNIQUE KEY `uk_persons_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 人员（市场PM/研发PM/产品组长/超管）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `post_launch_reviews`
--

DROP TABLE IF EXISTS `post_launch_reviews`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_launch_reviews` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `project_id` bigint NOT NULL COMMENT '项目ID（关联 projects.id）',
  `scheduled_at` datetime NOT NULL COMMENT '复盘待办截止日 = launchDate + 90d',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/COMPLETED/OVERDUE',
  `assignee_id` bigint DEFAULT NULL COMMENT '当前负责 PM（移交后跟随 ProjectMember）',
  `actual_revenue` decimal(18,2) DEFAULT NULL COMMENT '实际营收（复盘完成时填写）',
  `customer_feedback` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '客户反馈（≤2000 字符）',
  `kpi_achievement` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT 'KPI 达成（≤2000 字符）',
  `lessons` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '经验教训（≤4000 字符）',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `tenant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '000000' COMMENT '租户 ID（多租户隔离）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '逻辑删除',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_project_status` (`project_id`,`status`),
  KEY `idx_assignee` (`assignee_id`),
  KEY `idx_scheduled_at` (`scheduled_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='G5 上市 90 天复盘待办（P2-5.6 / AC-GATE-13）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_groups`
--

DROP TABLE IF EXISTS `product_groups`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_groups` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `group_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '产品组名称',
  `leader_person_id` bigint DEFAULT NULL COMMENT '组长（随 HR API 同步）',
  `parent_id` bigint DEFAULT NULL COMMENT '上级组（预留）',
  `description` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 产品组（组织架构）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_retirements`
--

DROP TABLE IF EXISTS `product_retirements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_retirements` (
  `id` bigint NOT NULL COMMENT '主键(雪花)',
  `product_id` bigint NOT NULL COMMENT '退市产品',
  `proposer_id` bigint NOT NULL COMMENT '提议人(研发 PM)',
  `proposer_role` varchar(32) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'RD_PM' COMMENT '研发 PM(Q11 主)',
  `reason` varchar(500) COLLATE utf8mb4_general_ci NOT NULL COMMENT '退市理由',
  `readiness_active_projects` int NOT NULL DEFAULT '0' COMMENT 'readiness 检查① 在途项目数',
  `readiness_active_reviews` int NOT NULL DEFAULT '0' COMMENT 'readiness 检查② 在途评审数',
  `readiness_open_issues` int NOT NULL DEFAULT '0' COMMENT 'readiness 检查③ 未结事项数',
  `readiness_passed` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '三项检查全部 0 才算通过 1是',
  `status` varchar(24) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING_RD_LEADER' COMMENT 'PENDING_RD_LEADER|PENDING_SUPER_ADMIN|APPROVED|REJECTED',
  `rd_leader_id` bigint DEFAULT NULL COMMENT '研发组长 / 研发 PM 审批人',
  `rd_decision` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'APPROVE|REJECT',
  `rd_decided_at` datetime DEFAULT NULL COMMENT '一级审批时间',
  `rd_opinion` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '一级审批意见',
  `super_admin_id` bigint DEFAULT NULL COMMENT '超管终审',
  `super_decision` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'APPROVE|REJECT',
  `super_decided_at` datetime DEFAULT NULL COMMENT '终审时间',
  `super_opinion` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '终审意见',
  `due_at` datetime DEFAULT NULL COMMENT '审批期限(Q2=乙加列)',
  `approved_at` datetime DEFAULT NULL COMMENT '退市生效时间(联动 products.retired_at)',
  `rejected_at` datetime DEFAULT NULL COMMENT '驳回时间',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `version` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pr_product_active` (`product_id`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 产品退市申请(readiness 三检查 + 两级审批 + 全产品线只读联动)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `products`
--

DROP TABLE IF EXISTS `products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `products` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `product_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '产品编码',
  `product_name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '产品名称',
  `model_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '在售型号编码（超管导入）',
  `source` varchar(32) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PM_NEW' COMMENT '来源 ADMIN_IMPORT|PM_NEW|GUEST_OTHER',
  `project_id` bigint DEFAULT NULL COMMENT '关联项目（1:1 唯一）',
  `group_id` bigint DEFAULT NULL COMMENT '归属产品组',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '状态 ACTIVE|INACTIVE',
  `retired_at` datetime DEFAULT NULL COMMENT '产品退市生效时间',
  `retirement_locked` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '退市后全产品线只读锁',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_products_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 产品（项目与需求上层实体）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_cert_items`
--

DROP TABLE IF EXISTS `project_cert_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_cert_items` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `project_id` bigint NOT NULL COMMENT '归属项目',
  `template_id` bigint DEFAULT NULL COMMENT '来源 cert_templates.id；手工可空',
  `country_code` varchar(8) COLLATE utf8mb4_general_ci NOT NULL COMMENT '国家/地区代码',
  `country_name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '国家名',
  `cert_name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '认证名',
  `cert_authority` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `requirement_desc` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `is_mandatory` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '1',
  `source` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'AUTO' COMMENT 'AUTO|MANUAL',
  `status` varchar(24) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING|IN_PROGRESS|DONE|NA',
  `version` int NOT NULL DEFAULT '0' COMMENT 'MyBatis-Plus 乐观锁',
  `catalog_version` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '带出时模板快照版本',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pci_project_cert` (`project_id`,`country_code`,`cert_name`),
  KEY `idx_pci_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 项目认证清单（AC-PROD-10/11/12）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_circle_comments`
--

DROP TABLE IF EXISTS `project_circle_comments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_circle_comments` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `post_id` bigint NOT NULL COMMENT '动态（project_circle_posts.id）',
  `author_id` bigint NOT NULL COMMENT '作者（persons.id）',
  `parent_id` bigint DEFAULT NULL COMMENT '父评论（楼中楼，可空）',
  `content` varchar(2000) COLLATE utf8mb4_general_ci NOT NULL COMMENT '评论内容（2-2000 字）',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_pcc_post` (`post_id`,`create_time`),
  KEY `idx_pcc_author` (`author_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 项目协作圈评论（楼中楼）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_circle_posts`
--

DROP TABLE IF EXISTS `project_circle_posts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_circle_posts` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `project_id` bigint NOT NULL COMMENT '项目',
  `author_id` bigint NOT NULL COMMENT '作者（persons.id）',
  `object_type` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联对象类型（需求/变更/闸门等，可空）',
  `object_id` bigint DEFAULT NULL COMMENT '关联对象ID（可空）',
  `content` varchar(3000) COLLATE utf8mb4_general_ci NOT NULL COMMENT '动态内容（2-3000 字）',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_pcp_project` (`project_id`,`create_time`),
  KEY `idx_pcp_author` (`author_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 项目协作圈动态';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_followers`
--

DROP TABLE IF EXISTS `project_followers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_followers` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `project_id` bigint NOT NULL COMMENT '项目',
  `user_id` bigint NOT NULL COMMENT '人员（persons.id）',
  `circle_role` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'FOLLOWER' COMMENT '圈角色 FOLLOWER|COMMENTER|CONTRIBUTOR',
  `added_by` bigint DEFAULT NULL COMMENT '添加人',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_followers_project_user` (`project_id`,`user_id`),
  KEY `idx_followers_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 项目协作圈成员';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_members`
--

DROP TABLE IF EXISTS `project_members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_members` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `project_id` bigint NOT NULL COMMENT '项目',
  `person_id` bigint NOT NULL COMMENT '人员',
  `role` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色 MARKET_PM|RD_PM（固定不可跨 B7）',
  `member_type` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主项目PRIMARY|附加ADDITIONAL（P2-4.2，首个活跃绑定=PRIMARY）',
  `approval_ref` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '评级委员会审批备案编号（绑第N个项目必填，N=allowance.projectCountThreshold）',
  `locked_level` varchar(8) COLLATE utf8mb4_general_ci NOT NULL COMMENT '绑定时评级快照（BR-INC-02）',
  `locked_amount` decimal(10,2) NOT NULL COMMENT '锁定月度津贴额',
  `join_date` datetime NOT NULL COMMENT '加入日期',
  `exit_date` datetime DEFAULT NULL COMMENT '退出日期',
  `exit_reason` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '退出原因 TRANSFER|VOLUNTARY|LOW_PERF',
  `bonus_eligible` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '1' COMMENT '奖金资格（放弃置 0，BR-INC-09）',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_pm_project` (`project_id`),
  KEY `idx_pm_person` (`person_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 项目成员（双PM 绑定+评级快照）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_score_records`
--

DROP TABLE IF EXISTS `project_score_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_score_records` (
  `id` bigint NOT NULL,
  `project_id` bigint DEFAULT NULL,
  `person_id` bigint DEFAULT NULL,
  `pm_role` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `component_type` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `score` decimal(18,2) DEFAULT NULL,
  `version_no` int DEFAULT NULL,
  `rule_version` int DEFAULT NULL,
  `rule_snapshot` text COLLATE utf8mb4_general_ci,
  `status` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `author_id` bigint DEFAULT NULL,
  `author_role` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `submitted_at` datetime DEFAULT NULL,
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_psr_proj` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目评分记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_score_tasks`
--

DROP TABLE IF EXISTS `project_score_tasks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_score_tasks` (
  `id` bigint NOT NULL,
  `project_id` bigint DEFAULT NULL,
  `person_id` bigint DEFAULT NULL,
  `target_type` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `due_at` datetime DEFAULT NULL,
  `launch_date_snapshot` datetime DEFAULT NULL,
  `status` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `action_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_pst_proj` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目评分任务';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_scores`
--

DROP TABLE IF EXISTS `project_scores`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_scores` (
  `id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `person_id` bigint NOT NULL,
  `dimension_1` decimal(5,2) NOT NULL,
  `dimension_2` decimal(5,2) NOT NULL,
  `dimension_3` decimal(5,2) NOT NULL,
  `dimension_4` decimal(5,2) NOT NULL,
  `dimension_5` decimal(5,2) NOT NULL,
  `total_score` decimal(6,2) GENERATED ALWAYS AS (((((`dimension_1` + `dimension_2`) + `dimension_3`) + `dimension_4`) + `dimension_5`)) STORED,
  `evaluator_id` bigint NOT NULL,
  `pm_role` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'PM角色',
  `self_score` decimal(6,2) DEFAULT NULL COMMENT '自评',
  `market_leader_score` decimal(6,2) DEFAULT NULL COMMENT '市场组长评分',
  `rd_leader_score` decimal(6,2) DEFAULT NULL COMMENT '研发组长评分',
  `weighted_score` decimal(6,2) DEFAULT NULL COMMENT '加权评分',
  `evidence_urls` json DEFAULT NULL,
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT',
  `scored_at` datetime DEFAULT NULL COMMENT '评分时间',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_score_project_person` (`project_id`,`person_id`,`tenant_id`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='五维贡献评定';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_stages`
--

DROP TABLE IF EXISTS `project_stages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_stages` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `project_id` bigint NOT NULL COMMENT '项目',
  `stage_code` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT '阶段 CONCEPT|PLAN|DEV|VALID|LAUNCH|LIFECYCLE',
  `stage_name` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '阶段名（概念/计划/开发/验证/发布/生命周期）',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '顺序',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'NOT_STARTED' COMMENT '状态 NOT_STARTED|IN_PROGRESS|DONE',
  `gate_id` bigint DEFAULT NULL COMMENT '出口 Gate 实例',
  `started_at` datetime DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL,
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_stages_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 项目六阶段实例';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `projects`
--

DROP TABLE IF EXISTS `projects`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `projects` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `code` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '项目编码 PRJ-YYYY-NNN 自动生成',
  `name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '项目名称',
  `product_id` bigint NOT NULL COMMENT '归属产品（1:1 唯一，Q5）',
  `template_type` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT '模板类型 HARDWARE|SOFTWARE|SOLUTION',
  `target_markets` json DEFAULT NULL COMMENT '目标市场国家/地区代码数组（M1 驱动认证清单）',
  `level` varchar(8) COLLATE utf8mb4_general_ci NOT NULL COMMENT '项目级别 S|A|B',
  `level_coefficient` decimal(5,2) DEFAULT NULL COMMENT '差异化系数（立项录入 BR-INC-05）',
  `level_coefficient_reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '系数定值理由（S/B 必填，写审计）',
  `target_sales_amount` decimal(18,2) DEFAULT NULL COMMENT '立项目标销售额（奖金池基数 BR-INC-04）',
  `target_channel_count` int DEFAULT NULL COMMENT '立项目标渠道商数',
  `target_nps` int DEFAULT NULL COMMENT '立项 NPS 目标',
  `target_scene_count` int DEFAULT NULL COMMENT '立项目标场景数',
  `launch_date` datetime DEFAULT NULL COMMENT '上市日期（后置指标起算原点 BR-IPD-08）',
  `current_stage` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'CONCEPT' COMMENT '当前阶段 CONCEPT|PLAN|DEV|VALID|LAUNCH|LIFECYCLE',
  `declared_stage` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '存量申报当前阶段',
  `lifecycle_status` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '生命周期 ON_SALE|LIMITED|EOL|ARCHIVED',
  `source` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'NEW' COMMENT '来源 NEW|LEGACY（存量导入）',
  `legacy_effective_at` datetime DEFAULT NULL COMMENT '存量导入生效日',
  `last_activity_at` datetime DEFAULT NULL COMMENT 'P1-9.2 最近活动日',
  `missing_history_ack` char(1) COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '历史缺失声明',
  `catchup_status` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'IN_PROGRESS|COMPLETE',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT|TEAMING|ACTIVE|SUSPENDED|ARCHIVED',
  `main_group_id` bigint DEFAULT NULL COMMENT '主组=市场PM 所在产品组（BR-ORG-01）',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `archived_at` datetime DEFAULT NULL COMMENT 'P2-7.4 归档时间',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_projects_code` (`code`),
  UNIQUE KEY `uk_projects_product` (`product_id`),
  KEY `idx_projects_last_activity_at` (`last_activity_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 项目（核心实体）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `rd_replacement_approvals`
--

DROP TABLE IF EXISTS `rd_replacement_approvals`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rd_replacement_approvals` (
  `id` bigint NOT NULL COMMENT '主键(雪花)',
  `replacement_id` bigint NOT NULL COMMENT '所属替换申请',
  `step` varchar(24) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'SELF|MARKET_LEADER|RD_LEADER|SUPER_ADMIN',
  `approver_id` bigint NOT NULL COMMENT '审批人',
  `approver_role` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '审批人角色',
  `decision` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'APPROVE|REJECT',
  `opinion` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审批意见',
  `decided_at` datetime NOT NULL COMMENT '审批时间',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rdra_repl_step` (`replacement_id`,`step`),
  KEY `idx_rdra_repl` (`replacement_id`),
  KEY `idx_rdra_step` (`step`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 研发 PM 替换审批留痕(每步一条,留痕完整)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `rd_replacements`
--

DROP TABLE IF EXISTS `rd_replacements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rd_replacements` (
  `id` bigint NOT NULL COMMENT '主键(雪花)',
  `project_id` bigint NOT NULL COMMENT '被替换 PM 所在项目',
  `out_person_id` bigint NOT NULL COMMENT '被替换的研发 PM',
  `out_role` varchar(32) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'RD_PM' COMMENT '固定 RD_PM(Q6=甲研发线)',
  `in_person_id` bigint NOT NULL COMMENT '新研发 PM(本人确认步)',
  `in_role` varchar(32) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'RD_PM' COMMENT '固定 RD_PM',
  `out_level_locked` varchar(8) COLLATE utf8mb4_general_ci NOT NULL COMMENT '被替换时等级快照(BR-INC-02 锁定)',
  `out_amount_locked` decimal(10,2) NOT NULL COMMENT '被替换时月度津贴快照',
  `effective_date` datetime NOT NULL COMMENT '生效日期(原子责任转移日)',
  `status` varchar(24) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING_SELF' COMMENT 'PENDING_SELF|PENDING_MARKET_LEADER|PENDING_RD_LEADER|PENDING_SUPER_ADMIN|APPROVED|REJECTED|CANCELLED',
  `due_at` datetime DEFAULT NULL COMMENT '审批期限(Q2=乙加列)',
  `reason` varchar(500) COLLATE utf8mb4_general_ci NOT NULL COMMENT '替换理由',
  `out_exit_reason` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'TRANSFER|VOLUNTARY|LOW_PERF',
  `atomic_locked` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '1' COMMENT '替换期间原子责任转移锁 1锁 0解(Q8 禁双 PM 同岗)',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `version` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_rdr_project` (`project_id`),
  KEY `idx_rdr_out` (`out_person_id`),
  KEY `idx_rdr_in` (`in_person_id`),
  KEY `idx_rdr_status` (`status`),
  KEY `idx_rdr_due` (`due_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 研发 PM 替换申请(四步审批链 + 等级锁定 + 原子责任转移)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `receipt_ledger`
--

DROP TABLE IF EXISTS `receipt_ledger`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `receipt_ledger` (
  `id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `bonus_pool_id` bigint DEFAULT NULL,
  `receipt_month` varchar(7) COLLATE utf8mb4_general_ci NOT NULL,
  `receipt_amount` decimal(18,2) NOT NULL,
  `refund_amount` decimal(18,2) DEFAULT '0.00',
  `net_amount` decimal(18,2) GENERATED ALWAYS AS ((`receipt_amount` - `refund_amount`)) STORED,
  `voucher_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `voucher_hash` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `source` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'RECEIPT',
  `window_start` date DEFAULT NULL,
  `window_end` date DEFAULT NULL,
  `in_window` tinyint(1) GENERATED ALWAYS AS (((`receipt_month` >= date_format(`window_start`,_utf8mb4'%Y-%m')) and (`receipt_month` <= date_format(`window_end`,_utf8mb4'%Y-%m')))) STORED,
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_receipt_project_month` (`project_id`,`receipt_month`,`tenant_id`,`del_flag`),
  KEY `idx_receipt_bonus_pool` (`bonus_pool_id`),
  KEY `idx_receipt_window` (`window_start`,`window_end`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='销售回款台账P3-4.1';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `requirement_changes`
--

DROP TABLE IF EXISTS `requirement_changes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `requirement_changes` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `requirement_id` bigint NOT NULL,
  `project_id` bigint DEFAULT NULL,
  `change_type` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `before_snapshot` json DEFAULT NULL,
  `after_snapshot` json DEFAULT NULL,
  `reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT|PENDING_SIGN|APPROVED|REJECTED',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `signatures` text COLLATE utf8mb4_general_ci COMMENT '签约快照 JSON',
  PRIMARY KEY (`id`),
  KEY `idx_rc_requirement` (`requirement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 需求变更单（双签否决）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `requirement_pool`
--

DROP TABLE IF EXISTS `requirement_pool`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `requirement_pool` (
  `id` bigint NOT NULL,
  `project_id` bigint DEFAULT NULL,
  `title` varchar(256) COLLATE utf8mb4_general_ci NOT NULL,
  `description` text COLLATE utf8mb4_general_ci,
  `source` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `priority` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'MEDIUM',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'SUBMITTED',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='需求池';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `requirements`
--

DROP TABLE IF EXISTS `requirements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `requirements` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `product_id` bigint DEFAULT NULL COMMENT '产品（游客「其他」可空路由）',
  `project_id` bigint DEFAULT NULL,
  `source` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PORTAL_GUEST' COMMENT '来源 PORTAL_GUEST|INTERNAL',
  `submitter_name` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `customer_name` varchar(120) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '客户名称（页38 customerName）',
  `contact` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `raw_model` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户输入原始型号文本（其他/未找到记录原输入）',
  `element_snapshot` longtext COLLATE utf8mb4_general_ci COMMENT '要素快照',
  `title` varchar(200) COLLATE utf8mb4_general_ci NOT NULL,
  `content` text COLLATE utf8mb4_general_ci,
  `query_code` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '查询码（游客凭码查进度）',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED|ACCEPTED|EVALUATING|SCHEDULED|PROCESSING|CLOSED|ARCHIVED',
  `market_pm_id` bigint DEFAULT NULL,
  `rd_pm_id` bigint DEFAULT NULL,
  `routed_at` datetime DEFAULT NULL COMMENT '按产品路由双PM 时间',
  `accepted_at` datetime DEFAULT NULL COMMENT '受理时间',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_req_query_code` (`query_code`),
  KEY `idx_req_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 需求池（免登录提交+查询码）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `short_drama_audio`
--

DROP TABLE IF EXISTS `short_drama_audio`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `short_drama_audio` (
  `id` bigint NOT NULL COMMENT '主键',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '语音资产名称',
  `audio_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'narration' COMMENT '语音类型：narration(旁白)/dialogue(对白)',
  `text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '语音文案（生成语音用的文本）',
  `voice` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '音色（如 alloy/onyx）',
  `audio_oss_id` bigint DEFAULT NULL COMMENT '音频文件OSS ID',
  `audio_url` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '音频文件URL',
  `linked_storyboard_id` bigint DEFAULT NULL COMMENT '对白关联的分镜ID（NULL=全局旁白）',
  `duration_seconds` int DEFAULT NULL COMMENT '音频时长（秒）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户Id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_project_id` (`project_id`) USING BTREE,
  KEY `idx_linked_storyboard_id` (`linked_storyboard_id`) USING BTREE,
  KEY `idx_tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='短剧语音资产表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `short_drama_character`
--

DROP TABLE IF EXISTS `short_drama_character`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `short_drama_character` (
  `id` bigint NOT NULL COMMENT '主键',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名',
  `aliases` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '别名/称呼（逗号分隔）',
  `introduction` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '角色介绍（身份、关系、称呼映射）',
  `role_level` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'B' COMMENT '角色层级：S/A/B/C/D',
  `gender` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '性别',
  `age_range` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '年龄段（如：约二十五岁）',
  `personality_tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '性格标签（逗号分隔）',
  `costume_tier` int DEFAULT '2' COMMENT '服装华丽度1-5',
  `visual_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '视觉外貌描述（详细，用于图片生成）',
  `reference_image_url` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '角色参考图URL',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户Id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_project_id` (`project_id`) USING BTREE,
  KEY `idx_tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='短剧角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `short_drama_character_appearance`
--

DROP TABLE IF EXISTS `short_drama_character_appearance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `short_drama_character_appearance` (
  `id` bigint NOT NULL COMMENT '主键',
  `character_id` bigint NOT NULL COMMENT '角色ID',
  `appearance_index` int NOT NULL DEFAULT '0' COMMENT '形象序号（0=主形象）',
  `change_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '初始形象' COMMENT '变化原因',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '形象视觉描述',
  `reference_image_url` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '形象参考图URL',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户Id',
  `image_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '生成图片URL列表（JSON数组）',
  `image_descriptions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '每张图片对应的提示词（JSON数组）',
  `selected_image_index` int DEFAULT '0' COMMENT '当前选中的图片索引',
  `previous_image_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '上一轮图片URL列表（撤销用，JSON数组）',
  `previous_descriptions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '上一轮提示词列表（撤销用，JSON数组）',
  `voice` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '音色名（如 zh_male_taocheng_uranus_bigtts），用于该形象对白配音',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_character_appearance` (`character_id`,`appearance_index`) USING BTREE,
  KEY `idx_character_id` (`character_id`) USING BTREE,
  KEY `idx_tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='短剧角色子形象表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `short_drama_location`
--

DROP TABLE IF EXISTS `short_drama_location`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `short_drama_location` (
  `id` bigint NOT NULL COMMENT '主键',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '场景名（如：客厅_白天）',
  `summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '场景简要说明',
  `has_crowd` tinyint(1) DEFAULT '0' COMMENT '是否有背景人群',
  `crowd_description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '背景人群描述',
  `available_slots` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '可站位置列表（JSON数组）',
  `descriptions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '场景描述列表（JSON数组，3条差异化描述）',
  `reference_image_url` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '场景全景参考图URL',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户Id',
  `image_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '生成图片URL列表（JSON数组）',
  `image_descriptions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '每张图片对应的提示词（JSON数组）',
  `selected_image_index` int DEFAULT '0' COMMENT '当前选中的图片索引',
  `previous_image_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '上一轮图片URL列表（撤销用，JSON数组）',
  `previous_descriptions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '上一轮提示词列表（撤销用，JSON数组）',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_project_id` (`project_id`) USING BTREE,
  KEY `idx_tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='短剧场景表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `short_drama_project`
--

DROP TABLE IF EXISTS `short_drama_project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `short_drama_project` (
  `id` bigint NOT NULL COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `project_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '项目名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '项目描述',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'draft' COMMENT '状态：draft/active/archived',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户Id',
  `art_style` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'realistic' COMMENT '视觉风格：american-comic=美漫, chinese-comic=国漫, japanese-anime=日系, realistic=写实',
  `composed_video_oss_id` bigint DEFAULT NULL COMMENT '最新成片OSS对象ID',
  `compose_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '合成状态：NULL未合成，pending/processing/done/failed',
  `compose_job_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '当前合成任务标识（并发防重）',
  `compose_progress` int NOT NULL DEFAULT '0' COMMENT '合成进度：0-100',
  `compose_transition_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'dissolve' COMMENT '转场类型：none/dissolve/fade/slide',
  `compose_transition_duration_seconds` decimal(6,3) NOT NULL DEFAULT '0.500' COMMENT '单次转场时长（秒）',
  `compose_aspect_ratio` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '9:16' COMMENT '成片画幅：9:16/16:9/1:1',
  `composed_video_duration_seconds` decimal(12,3) DEFAULT NULL COMMENT '最终成片实际时长（秒，ffprobe测量）',
  `compose_error_message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '最近一次合成失败原因',
  `composed_at` datetime DEFAULT NULL COMMENT '最近一次合成完成时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='短剧项目表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `short_drama_script`
--

DROP TABLE IF EXISTS `short_drama_script`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `short_drama_script` (
  `id` bigint NOT NULL COMMENT '主键',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `script_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '剧本名称',
  `script_text` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '剧本文本',
  `outline_text` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '大纲文本',
  `tone` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '风格/基调',
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'manual' COMMENT '来源：manual/ai',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户Id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_project_id` (`project_id`) USING BTREE,
  KEY `idx_tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='短剧剧本表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `short_drama_storyboard`
--

DROP TABLE IF EXISTS `short_drama_storyboard`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `short_drama_storyboard` (
  `id` bigint NOT NULL COMMENT '主键',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `script_id` bigint NOT NULL COMMENT '剧本ID',
  `scene_no` int NOT NULL COMMENT '分镜序号',
  `scene_title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '分镜标题',
  `scene_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '分镜说明',
  `scene_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'daily' COMMENT '场景类型：daily/emotion/action/epic/suspense',
  `shot_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '镜头景别：如平视中景/俯拍远景',
  `camera_move` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '镜头运动：如缓推/固定/跟随/环绕',
  `characters_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '镜头角色列表JSON：[{name,appearance,slot}]',
  `location_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '场景名称（匹配资产库）',
  `photography_rules` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '摄影规则JSON',
  `acting_notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '表演指导JSON',
  `continuity_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '镜头连续性JSON：起始状态、结束状态、动作承接、空间锚点、在场人物',
  `source_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '原文片段',
  `image_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '图片生成提示词',
  `duration_seconds` int NOT NULL DEFAULT '15' COMMENT '时长秒数',
  `video_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '视频提示词',
  `video_url` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '视频地址',
  `video_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '视频生成任务ID',
  `video_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'pending' COMMENT '视频状态：pending/generating/done/failed',
  `last_frame_url` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '上一镜末帧URL（同场景连续镜头首帧承接用）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户Id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_script_scene` (`script_id`,`scene_no`) USING BTREE,
  KEY `idx_project_id` (`project_id`) USING BTREE,
  KEY `idx_script_id` (`script_id`) USING BTREE,
  KEY `idx_tenant_id` (`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='短剧分镜表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sj_distributed_lock`
--

DROP TABLE IF EXISTS `sj_distributed_lock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sj_distributed_lock` (
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '锁名称',
  `lock_until` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '锁定时长',
  `locked_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '锁定时间',
  `locked_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '锁定者',
  `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='锁定表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sj_group_config`
--

DROP TABLE IF EXISTS `sj_group_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sj_group_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '组名称',
  `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '组描述',
  `token` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SJ_cKqBTPzCsWA3VyuCfFoccmuIEGXjr5KT' COMMENT 'token',
  `group_status` tinyint NOT NULL DEFAULT '0' COMMENT '组状态 0、未启用 1、启用',
  `version` int NOT NULL COMMENT '版本号',
  `group_partition` int NOT NULL COMMENT '分区',
  `id_generator_mode` tinyint NOT NULL DEFAULT '1' COMMENT '唯一id生成模式 默认号段模式',
  `init_scene` tinyint NOT NULL DEFAULT '0' COMMENT '是否初始化场景 0:否 1:是',
  `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_namespace_id_group_name` (`namespace_id`,`group_name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='组配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sj_job`
--

DROP TABLE IF EXISTS `sj_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sj_job` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '名称',
  `args_str` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '执行方法参数',
  `args_type` tinyint NOT NULL DEFAULT '1' COMMENT '参数类型 ',
  `next_trigger_at` bigint NOT NULL COMMENT '下次触发时间',
  `job_status` tinyint NOT NULL DEFAULT '1' COMMENT '任务状态 0、关闭、1、开启',
  `task_type` tinyint NOT NULL DEFAULT '1' COMMENT '任务类型 1、集群 2、广播 3、切片',
  `route_key` tinyint NOT NULL DEFAULT '4' COMMENT '路由策略',
  `executor_type` tinyint NOT NULL DEFAULT '1' COMMENT '执行器类型',
  `executor_info` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '执行器名称',
  `trigger_type` tinyint NOT NULL COMMENT '触发类型 1.CRON 表达式 2. 固定时间',
  `trigger_interval` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '间隔时长',
  `block_strategy` tinyint NOT NULL DEFAULT '1' COMMENT '阻塞策略 1、丢弃 2、覆盖 3、并行 4、恢复',
  `executor_timeout` int NOT NULL DEFAULT '0' COMMENT '任务执行超时时间，单位秒',
  `max_retry_times` int NOT NULL DEFAULT '0' COMMENT '最大重试次数',
  `parallel_num` int NOT NULL DEFAULT '1' COMMENT '并行数',
  `retry_interval` int NOT NULL DEFAULT '0' COMMENT '重试间隔(s)',
  `bucket_index` int NOT NULL DEFAULT '0' COMMENT 'bucket',
  `resident` tinyint NOT NULL DEFAULT '0' COMMENT '是否是常驻任务',
  `notify_ids` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '通知告警场景配置id列表',
  `owner_id` bigint DEFAULT NULL COMMENT '负责人id',
  `labels` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '标签',
  `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '描述',
  `ext_attrs` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '扩展字段',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 1、删除',
  `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_namespace_id_group_name` (`namespace_id`,`group_name`) USING BTREE,
  KEY `idx_job_status_bucket_index` (`job_status`,`bucket_index`) USING BTREE,
  KEY `idx_create_dt` (`create_dt`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='任务信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sj_job_executor`
--

DROP TABLE IF EXISTS `sj_job_executor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sj_job_executor` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
  `executor_info` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务执行器名称',
  `executor_type` varchar(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '1:java 2:python 3:go',
  `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_namespace_id_group_name` (`namespace_id`,`group_name`) USING BTREE,
  KEY `idx_create_dt` (`create_dt`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='任务执行器信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sj_job_log_message`
--

DROP TABLE IF EXISTS `sj_job_log_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sj_job_log_message` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
  `job_id` bigint NOT NULL COMMENT '任务信息id',
  `task_batch_id` bigint NOT NULL COMMENT '任务批次id',
  `task_id` bigint NOT NULL COMMENT '调度任务id',
  `message` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度信息',
  `log_num` int NOT NULL DEFAULT '1' COMMENT '日志数量',
  `real_time` bigint NOT NULL DEFAULT '0' COMMENT '上报时间',
  `ext_attrs` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '扩展字段',
  `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_task_batch_id_task_id` (`task_batch_id`,`task_id`) USING BTREE,
  KEY `idx_create_dt` (`create_dt`) USING BTREE,
  KEY `idx_namespace_id_group_name` (`namespace_id`,`group_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='调度日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sj_job_summary`
--

DROP TABLE IF EXISTS `sj_job_summary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sj_job_summary` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '组名称',
  `business_id` bigint NOT NULL COMMENT '业务id (job_id或workflow_id)',
  `system_task_type` tinyint NOT NULL DEFAULT '3' COMMENT '任务类型 3、JOB任务 4、WORKFLOW任务',
  `trigger_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '统计时间',
  `success_num` int NOT NULL DEFAULT '0' COMMENT '执行成功-日志数量',
  `fail_num` int NOT NULL DEFAULT '0' COMMENT '执行失败-日志数量',
  `fail_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '失败原因',
  `stop_num` int NOT NULL DEFAULT '0' COMMENT '执行失败-日志数量',
  `stop_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '失败原因',
  `cancel_num` int NOT NULL DEFAULT '0' COMMENT '执行失败-日志数量',
  `cancel_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '失败原因',
  `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_trigger_at_system_task_type_business_id` (`trigger_at`,`system_task_type`,`business_id`) USING BTREE,
  KEY `idx_namespace_id_group_name_business_id` (`namespace_id`,`group_name`,`business_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='DashBoard_Job';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sj_job_task`
--

DROP TABLE IF EXISTS `sj_job_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sj_job_task` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
  `job_id` bigint NOT NULL COMMENT '任务信息id',
  `task_batch_id` bigint NOT NULL COMMENT '调度任务id',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父执行器id',
  `task_status` tinyint NOT NULL DEFAULT '0' COMMENT '执行的状态 0、失败 1、成功',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '重试次数',
  `mr_stage` tinyint DEFAULT NULL COMMENT '动态分片所处阶段 1:map 2:reduce 3:mergeReduce',
  `leaf` tinyint NOT NULL DEFAULT '1' COMMENT '叶子节点',
  `task_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '任务名称',
  `client_info` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '客户端地址 clientId#ip:port',
  `wf_context` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '工作流全局上下文',
  `result_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '执行结果',
  `args_str` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '执行方法参数',
  `args_type` tinyint NOT NULL DEFAULT '1' COMMENT '参数类型 ',
  `ext_attrs` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '扩展字段',
  `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_task_batch_id_task_status` (`task_batch_id`,`task_status`) USING BTREE,
  KEY `idx_create_dt` (`create_dt`) USING BTREE,
  KEY `idx_namespace_id_group_name` (`namespace_id`,`group_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='任务实例';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sj_job_task_batch`
--

DROP TABLE IF EXISTS `sj_job_task_batch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sj_job_task_batch` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
  `job_id` bigint NOT NULL COMMENT '任务id',
  `workflow_node_id` bigint NOT NULL DEFAULT '0' COMMENT '工作流节点id',
  `parent_workflow_node_id` bigint NOT NULL DEFAULT '0' COMMENT '工作流任务父批次id',
  `workflow_task_batch_id` bigint NOT NULL DEFAULT '0' COMMENT '工作流任务批次id',
  `task_batch_status` tinyint NOT NULL DEFAULT '0' COMMENT '任务批次状态 0、失败 1、成功',
  `operation_reason` tinyint NOT NULL DEFAULT '0' COMMENT '操作原因',
  `execution_at` bigint NOT NULL DEFAULT '0' COMMENT '任务执行时间',
  `system_task_type` tinyint NOT NULL DEFAULT '3' COMMENT '任务类型 3、JOB任务 4、WORKFLOW任务',
  `parent_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '父节点',
  `ext_attrs` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '扩展字段',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 1、删除',
  `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_job_id_task_batch_status` (`job_id`,`task_batch_status`) USING BTREE,
  KEY `idx_create_dt` (`create_dt`) USING BTREE,
  KEY `idx_namespace_id_group_name` (`namespace_id`,`group_name`) USING BTREE,
  KEY `idx_workflow_task_batch_id_workflow_node_id` (`workflow_task_batch_id`,`workflow_node_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='任务批次';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sj_namespace`
--

DROP TABLE IF EXISTS `sj_namespace`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sj_namespace` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '名称',
  `unique_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '唯一id',
  `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '描述',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 1、删除',
  `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_unique_id` (`unique_id`) USING BTREE,
  KEY `idx_name` (`name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='命名空间';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sj_notify_config`
--

DROP TABLE IF EXISTS `sj_notify_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sj_notify_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
  `notify_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '通知名称',
  `system_task_type` tinyint NOT NULL DEFAULT '3' COMMENT '任务类型 1. 重试任务 2. 重试回调 3、JOB任务 4、WORKFLOW任务',
  `notify_status` tinyint NOT NULL DEFAULT '0' COMMENT '通知状态 0、未启用 1、启用',
  `recipient_ids` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '接收人id列表',
  `notify_threshold` int NOT NULL DEFAULT '0' COMMENT '通知阈值',
  `notify_scene` tinyint NOT NULL DEFAULT '0' COMMENT '通知场景',
  `rate_limiter_status` tinyint NOT NULL DEFAULT '0' COMMENT '限流状态 0、未启用 1、启用',
  `rate_limiter_threshold` int NOT NULL DEFAULT '0' COMMENT '每秒限流阈值',
  `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '描述',
  `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_namespace_id_group_name_scene_name` (`namespace_id`,`group_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='通知配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sj_notify_recipient`
--

DROP TABLE IF EXISTS `sj_notify_recipient`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sj_notify_recipient` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `recipient_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '接收人名称',
  `notify_type` tinyint NOT NULL DEFAULT '0' COMMENT '通知类型 1、钉钉 2、邮件 3、企业微信 4 飞书 5 webhook',
  `notify_attribute` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置属性',
  `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '描述',
  `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_namespace_id` (`namespace_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='告警通知接收人';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sj_retry`
--

DROP TABLE IF EXISTS `sj_retry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sj_retry` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
  `group_id` bigint NOT NULL COMMENT '组Id',
  `scene_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '场景名称',
  `scene_id` bigint NOT NULL COMMENT '场景ID',
  `idempotent_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '幂等id',
  `biz_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '业务编号',
  `executor_name` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '执行器名称',
  `args_str` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '执行方法参数',
  `ext_attrs` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '扩展字段',
  `serializer_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'jackson' COMMENT '执行方法参数序列化器名称',
  `next_trigger_at` bigint NOT NULL COMMENT '下次触发时间',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '重试次数',
  `retry_status` tinyint NOT NULL DEFAULT '0' COMMENT '重试状态 0、重试中 1、成功 2、最大重试次数',
  `task_type` tinyint NOT NULL DEFAULT '1' COMMENT '任务类型 1、重试数据 2、回调数据',
  `bucket_index` int NOT NULL DEFAULT '0' COMMENT 'bucket',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父节点id',
  `deleted` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_scene_tasktype_idempotentid_deleted` (`scene_id`,`task_type`,`idempotent_id`,`deleted`) USING BTREE,
  KEY `idx_biz_no` (`biz_no`) USING BTREE,
  KEY `idx_idempotent_id` (`idempotent_id`) USING BTREE,
  KEY `idx_retry_status_bucket_index` (`retry_status`,`bucket_index`) USING BTREE,
  KEY `idx_parent_id` (`parent_id`) USING BTREE,
  KEY `idx_create_dt` (`create_dt`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='重试信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sj_retry_dead_letter`
--

DROP TABLE IF EXISTS `sj_retry_dead_letter`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sj_retry_dead_letter` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
  `group_id` bigint NOT NULL COMMENT '组Id',
  `scene_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '场景名称',
  `scene_id` bigint NOT NULL COMMENT '场景ID',
  `idempotent_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '幂等id',
  `biz_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '业务编号',
  `executor_name` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '执行器名称',
  `serializer_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'jackson' COMMENT '执行方法参数序列化器名称',
  `args_str` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '执行方法参数',
  `ext_attrs` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '扩展字段',
  `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_namespace_id_group_name_scene_name` (`namespace_id`,`group_name`,`scene_name`) USING BTREE,
  KEY `idx_idempotent_id` (`idempotent_id`) USING BTREE,
  KEY `idx_biz_no` (`biz_no`) USING BTREE,
  KEY `idx_create_dt` (`create_dt`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='死信队列表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sj_retry_scene_config`
--

DROP TABLE IF EXISTS `sj_retry_scene_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sj_retry_scene_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `scene_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '场景名称',
  `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
  `scene_status` tinyint NOT NULL DEFAULT '0' COMMENT '组状态 0、未启用 1、启用',
  `max_retry_count` int NOT NULL DEFAULT '5' COMMENT '最大重试次数',
  `back_off` tinyint NOT NULL DEFAULT '1' COMMENT '1、默认等级 2、固定间隔时间 3、CRON 表达式',
  `trigger_interval` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '间隔时长',
  `notify_ids` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '通知告警场景配置id列表',
  `deadline_request` bigint unsigned NOT NULL DEFAULT '60000' COMMENT 'Deadline Request 调用链超时 单位毫秒',
  `executor_timeout` int unsigned NOT NULL DEFAULT '5' COMMENT '任务执行超时时间，单位秒',
  `route_key` tinyint NOT NULL DEFAULT '4' COMMENT '路由策略',
  `block_strategy` tinyint NOT NULL DEFAULT '1' COMMENT '阻塞策略 1、丢弃 2、覆盖 3、并行',
  `cb_status` tinyint NOT NULL DEFAULT '0' COMMENT '回调状态 0、不开启 1、开启',
  `cb_trigger_type` tinyint NOT NULL DEFAULT '1' COMMENT '1、默认等级 2、固定间隔时间 3、CRON 表达式',
  `cb_max_count` int NOT NULL DEFAULT '16' COMMENT '回调的最大执行次数',
  `cb_trigger_interval` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '回调的最大执行次数',
  `owner_id` bigint DEFAULT NULL COMMENT '负责人id',
  `labels` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '标签',
  `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '描述',
  `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_namespace_id_group_name_scene_name` (`namespace_id`,`group_name`,`scene_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='场景配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sj_retry_summary`
--

DROP TABLE IF EXISTS `sj_retry_summary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sj_retry_summary` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '组名称',
  `scene_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '场景名称',
  `trigger_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '统计时间',
  `running_num` int NOT NULL DEFAULT '0' COMMENT '重试中-日志数量',
  `finish_num` int NOT NULL DEFAULT '0' COMMENT '重试完成-日志数量',
  `max_count_num` int NOT NULL DEFAULT '0' COMMENT '重试到达最大次数-日志数量',
  `suspend_num` int NOT NULL DEFAULT '0' COMMENT '暂停重试-日志数量',
  `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_scene_name_trigger_at` (`namespace_id`,`group_name`,`scene_name`,`trigger_at`) USING BTREE,
  KEY `idx_trigger_at` (`trigger_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='DashBoard_Retry';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sj_retry_task`
--

DROP TABLE IF EXISTS `sj_retry_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sj_retry_task` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
  `scene_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '场景名称',
  `retry_id` bigint NOT NULL COMMENT '重试信息Id',
  `ext_attrs` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '扩展字段',
  `task_status` tinyint NOT NULL DEFAULT '1' COMMENT '重试状态',
  `task_type` tinyint NOT NULL DEFAULT '1' COMMENT '任务类型 1、重试数据 2、回调数据',
  `operation_reason` tinyint NOT NULL DEFAULT '0' COMMENT '操作原因',
  `client_info` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '客户端地址 clientId#ip:port',
  `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_group_name_scene_name` (`namespace_id`,`group_name`,`scene_name`) USING BTREE,
  KEY `task_status` (`task_status`) USING BTREE,
  KEY `idx_create_dt` (`create_dt`) USING BTREE,
  KEY `idx_retry_id` (`retry_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='重试任务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sj_retry_task_log_message`
--

DROP TABLE IF EXISTS `sj_retry_task_log_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sj_retry_task_log_message` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
  `retry_id` bigint NOT NULL COMMENT '重试信息Id',
  `retry_task_id` bigint NOT NULL COMMENT '重试任务Id',
  `message` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '异常信息',
  `log_num` int NOT NULL DEFAULT '1' COMMENT '日志数量',
  `real_time` bigint NOT NULL DEFAULT '0' COMMENT '上报时间',
  `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_namespace_id_group_name_retry_task_id` (`namespace_id`,`group_name`,`retry_task_id`) USING BTREE,
  KEY `idx_create_dt` (`create_dt`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='任务调度日志信息记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sj_server_node`
--

DROP TABLE IF EXISTS `sj_server_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sj_server_node` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
  `host_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主机id',
  `host_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '机器ip',
  `host_port` int NOT NULL COMMENT '机器端口',
  `expire_at` datetime NOT NULL COMMENT '过期时间',
  `node_type` tinyint NOT NULL COMMENT '节点类型 1、客户端 2、是服务端',
  `ext_attrs` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '扩展字段',
  `labels` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '标签',
  `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_host_id_host_ip` (`host_id`,`host_ip`) USING BTREE,
  KEY `idx_namespace_id_group_name` (`namespace_id`,`group_name`) USING BTREE,
  KEY `idx_expire_at_node_type` (`expire_at`,`node_type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='服务器节点';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sj_system_user`
--

DROP TABLE IF EXISTS `sj_system_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sj_system_user` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '账号',
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
  `role` tinyint NOT NULL DEFAULT '0' COMMENT '角色：1-普通用户、2-管理员',
  `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_username` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='系统用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sj_system_user_permission`
--

DROP TABLE IF EXISTS `sj_system_user_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sj_system_user_permission` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
  `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `system_user_id` bigint NOT NULL COMMENT '系统用户id',
  `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_namespace_id_group_name_system_user_id` (`namespace_id`,`group_name`,`system_user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='系统用户权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sj_workflow`
--

DROP TABLE IF EXISTS `sj_workflow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sj_workflow` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `workflow_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工作流名称',
  `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
  `workflow_status` tinyint NOT NULL DEFAULT '1' COMMENT '工作流状态 0、关闭、1、开启',
  `trigger_type` tinyint NOT NULL COMMENT '触发类型 1.CRON 表达式 2. 固定时间',
  `trigger_interval` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '间隔时长',
  `next_trigger_at` bigint NOT NULL COMMENT '下次触发时间',
  `block_strategy` tinyint NOT NULL DEFAULT '1' COMMENT '阻塞策略 1、丢弃 2、覆盖 3、并行',
  `executor_timeout` int NOT NULL DEFAULT '0' COMMENT '任务执行超时时间，单位秒',
  `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '描述',
  `flow_info` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '流程信息',
  `wf_context` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '上下文',
  `notify_ids` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '通知告警场景配置id列表',
  `bucket_index` int NOT NULL DEFAULT '0' COMMENT 'bucket',
  `version` int NOT NULL COMMENT '版本号',
  `owner_id` bigint DEFAULT NULL COMMENT '负责人id',
  `ext_attrs` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '扩展字段',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 1、删除',
  `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_create_dt` (`create_dt`) USING BTREE,
  KEY `idx_namespace_id_group_name` (`namespace_id`,`group_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='工作流';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sj_workflow_node`
--

DROP TABLE IF EXISTS `sj_workflow_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sj_workflow_node` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `node_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '节点名称',
  `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
  `job_id` bigint NOT NULL COMMENT '任务信息id',
  `workflow_id` bigint NOT NULL COMMENT '工作流ID',
  `node_type` tinyint NOT NULL DEFAULT '1' COMMENT '1、任务节点 2、条件节点',
  `expression_type` tinyint NOT NULL DEFAULT '0' COMMENT '1、SpEl、2、Aviator 3、QL',
  `fail_strategy` tinyint NOT NULL DEFAULT '1' COMMENT '失败策略 1、跳过 2、阻塞',
  `workflow_node_status` tinyint NOT NULL DEFAULT '1' COMMENT '工作流节点状态 0、关闭、1、开启',
  `priority_level` int NOT NULL DEFAULT '1' COMMENT '优先级',
  `node_info` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '节点信息 ',
  `version` int NOT NULL COMMENT '版本号',
  `ext_attrs` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '扩展字段',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 1、删除',
  `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_create_dt` (`create_dt`) USING BTREE,
  KEY `idx_namespace_id_group_name` (`namespace_id`,`group_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='工作流节点';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sj_workflow_task_batch`
--

DROP TABLE IF EXISTS `sj_workflow_task_batch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sj_workflow_task_batch` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
  `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
  `workflow_id` bigint NOT NULL COMMENT '工作流任务id',
  `task_batch_status` tinyint NOT NULL DEFAULT '0' COMMENT '任务批次状态 0、失败 1、成功',
  `operation_reason` tinyint NOT NULL DEFAULT '0' COMMENT '操作原因',
  `flow_info` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '流程信息',
  `wf_context` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '全局上下文',
  `execution_at` bigint NOT NULL DEFAULT '0' COMMENT '任务执行时间',
  `ext_attrs` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '扩展字段',
  `version` int NOT NULL DEFAULT '1' COMMENT '版本号',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 1、删除',
  `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_job_id_task_batch_status` (`workflow_id`,`task_batch_status`) USING BTREE,
  KEY `idx_create_dt` (`create_dt`) USING BTREE,
  KEY `idx_namespace_id_group_name` (`namespace_id`,`group_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='工作流批次';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sop_template_instances`
--

DROP TABLE IF EXISTS `sop_template_instances`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sop_template_instances` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `template_id` bigint NOT NULL COMMENT '模板主键（关联 sop_templates.id）',
  `instance_version` bigint NOT NULL DEFAULT '1' COMMENT '实例版本号（项目级自增；与 SopTemplate.version 同源）',
  `project_id` bigint NOT NULL COMMENT '项目主键（关联 projects.id）',
  `snapshot_json` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '快照 JSON（不可变：含 actionList/responsibilityMatrix/phaseDeadlineMap）',
  `instantiated_at` datetime DEFAULT NULL COMMENT '实例化时间',
  `instantiated_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '实例化人 Person ID（String 与 Person.id 雪花位一致）',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '状态 ACTIVE/SUPERSEDED/ARCHIVED',
  `tenant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '000000' COMMENT '租户 ID（多租户隔离）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '逻辑删除',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_project_template_status` (`project_id`,`template_id`,`status`),
  KEY `idx_template` (`template_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='SOP 模板项目实例快照（P1-3.3 / BR-IPD-SOP-03；同一项目同一模板仅一条 ACTIVE）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sop_templates`
--

DROP TABLE IF EXISTS `sop_templates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sop_templates` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `template_code` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '模板编码',
  `template_name` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '模板名称',
  `description` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '模板描述',
  `effective_from` datetime DEFAULT NULL COMMENT '生效起',
  `effective_to` datetime DEFAULT NULL COMMENT '生效止',
  `category` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分类',
  `created_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `action_code` varchar(8) COLLATE utf8mb4_general_ci NOT NULL COMMENT '绑定动作编号（深管 42 动作）',
  `title` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `content` mediumtext COLLATE utf8mb4_general_ci COMMENT 'SOP 富文本',
  `version` int NOT NULL DEFAULT '1' COMMENT '版本',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT|PUBLISHED|ARCHIVED',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_sop_action` (`action_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD SOP 模板（标准作业程序+版本）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stage_actions`
--

DROP TABLE IF EXISTS `stage_actions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stage_actions` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `project_id` bigint NOT NULL COMMENT '项目',
  `stage_id` bigint NOT NULL COMMENT '阶段实例',
  `action_code` varchar(8) COLLATE utf8mb4_general_ci NOT NULL COMMENT '动作编号 C01/P01/D05/C12/D11/V10...（69 个）',
  `action_name` varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '动作名称',
  `owner_role` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT '责任角色 MARKET_PM|RD_PM|BOTH',
  `depth` varchar(8) COLLATE utf8mb4_general_ci NOT NULL COMMENT '管理深度 DEEP|LIGHT（BR-IPD-03/04）',
  `status` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'NOT_STARTED' COMMENT '状态 NOT_STARTED|IN_PROGRESS|DONE|DELAYED|NA',
  `confirmed_at` datetime DEFAULT NULL COMMENT '确认时间',
  `confirmed_by` bigint DEFAULT NULL COMMENT '确认人',
  `history_mark` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'HISTORICAL_MISSING',
  `is_blocking` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '是否阻断跳阶（1是）',
  `actual_done_at` datetime DEFAULT NULL COMMENT '实际完成时间（轻管核心字段 BR-IPD-05）',
  `far_value` decimal(10,6) DEFAULT NULL COMMENT 'D11/Z01 BioCV 误识率 FAR',
  `frr_value` decimal(10,6) DEFAULT NULL COMMENT 'D11/Z01 BioCV 拒识率 FRR',
  `cert_no` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'V02 认证证书编号',
  `cert_passed_at` datetime DEFAULT NULL COMMENT 'V02 认证通过日期',
  `algo_type` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'BioCV 算法 FINGERPRINT|FACE|PALM|VEIN|MULTI',
  `is_bio_feature` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '涉生物特征（驱动 C12 强制挂载）',
  `due_date` datetime DEFAULT NULL,
  `sop_id` bigint DEFAULT NULL,
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号（P1-4.3 状态机并发控制）',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_sa_project` (`project_id`),
  KEY `idx_sa_stage` (`stage_id`),
  KEY `idx_sa_code` (`action_code`),
  KEY `idx_sa_project_code` (`project_id`,`action_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 阶段动作实例（69 动作，深管/轻管）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `switching_acceptance`
--

DROP TABLE IF EXISTS `switching_acceptance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `switching_acceptance` (
  `id` bigint NOT NULL,
  `project_id` bigint DEFAULT NULL,
  `month` varchar(7) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `ran_at` datetime DEFAULT NULL,
  `ran_by` bigint DEFAULT NULL,
  `report_json` text COLLATE utf8mb4_general_ci,
  `diff_rate` decimal(18,4) DEFAULT NULL,
  `passed` tinyint(1) DEFAULT NULL,
  `is_locked` tinyint(1) DEFAULT NULL,
  `locked_at` datetime DEFAULT NULL,
  `locked_by` bigint DEFAULT NULL,
  `unlock_reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `unlocked_at` datetime DEFAULT NULL,
  `unlocked_by` bigint DEFAULT NULL,
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_sa_proj_month` (`project_id`,`month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='切换验收';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_client`
--

DROP TABLE IF EXISTS `sys_client`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_client` (
  `id` bigint NOT NULL COMMENT 'id',
  `client_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '客户端id',
  `client_key` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '客户端key',
  `client_secret` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '客户端秘钥',
  `grant_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '授权类型',
  `device_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '设备类型',
  `active_timeout` int DEFAULT '1800' COMMENT 'token活跃超时时间',
  `timeout` int DEFAULT '604800' COMMENT 'token固定超时',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='系统授权表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_config`
--

DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config` (
  `config_id` bigint NOT NULL COMMENT '参数主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '000000' COMMENT '租户编号',
  `config_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '参数键值',
  `config_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`config_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='参数配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_dept`
--

DROP TABLE IF EXISTS `sys_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dept` (
  `dept_id` bigint NOT NULL COMMENT '部门id',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '000000' COMMENT '租户编号',
  `parent_id` bigint DEFAULT '0' COMMENT '父部门id',
  `ancestors` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '部门名称',
  `dept_category` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '部门类别编码',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `leader` bigint DEFAULT NULL COMMENT '负责人',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '联系电话',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '邮箱',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`dept_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='部门表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_dict_data`
--

DROP TABLE IF EXISTS `sys_dict_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_data` (
  `dict_code` bigint NOT NULL COMMENT '字典编码',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '000000' COMMENT '租户编号',
  `dict_sort` int DEFAULT '0' COMMENT '字典排序',
  `dict_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
  `list_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='字典数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_dict_type`
--

DROP TABLE IF EXISTS `sys_dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_type` (
  `dict_id` bigint NOT NULL COMMENT '字典主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '000000' COMMENT '租户编号',
  `dict_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '字典名称',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '字典类型',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_id`) USING BTREE,
  UNIQUE KEY `tenant_id` (`tenant_id`,`dict_type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='字典类型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_logininfor`
--

DROP TABLE IF EXISTS `sys_logininfor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_logininfor` (
  `info_id` bigint NOT NULL COMMENT '访问ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '000000' COMMENT '租户编号',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '用户账号',
  `client_key` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '客户端',
  `device_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '设备类型',
  `ipaddr` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '登录IP地址',
  `login_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '登录地点',
  `browser` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '浏览器类型',
  `os` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '操作系统',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
  `msg` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '提示消息',
  `login_time` datetime DEFAULT NULL COMMENT '访问时间',
  PRIMARY KEY (`info_id`) USING BTREE,
  KEY `idx_sys_logininfor_s` (`status`) USING BTREE,
  KEY `idx_sys_logininfor_lt` (`login_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='系统访问记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_menu`
--

DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  `menu_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜单名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父菜单ID',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组件路径',
  `query_param` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '路由参数',
  `is_frame` int DEFAULT '1' COMMENT '是否为外链（0是 1否）',
  `is_cache` int DEFAULT '0' COMMENT '是否缓存（0缓存 1不缓存）',
  `menu_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '显示状态（0显示 1隐藏）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
  `perms` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '#' COMMENT '菜单图标',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`menu_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='菜单权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_notice`
--

DROP TABLE IF EXISTS `sys_notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notice` (
  `notice_id` bigint NOT NULL COMMENT '公告ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '000000' COMMENT '租户编号',
  `notice_title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '公告标题',
  `notice_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '公告类型（1通知 2公告）',
  `notice_content` longblob COMMENT '公告内容',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '公告状态（0正常 1关闭）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`notice_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='通知公告表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_oper_log`
--

DROP TABLE IF EXISTS `sys_oper_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_oper_log` (
  `oper_id` bigint NOT NULL COMMENT '日志主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '000000' COMMENT '租户编号',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '模块标题',
  `business_type` int DEFAULT '0' COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '请求方式',
  `operator_type` int DEFAULT '0' COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  `oper_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '操作人员',
  `dept_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '部门名称',
  `oper_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '请求URL',
  `oper_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '主机地址',
  `oper_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '操作地点',
  `oper_param` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '请求参数',
  `json_result` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '返回参数',
  `status` int DEFAULT '0' COMMENT '操作状态（0正常 1异常）',
  `error_msg` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '错误消息',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  `cost_time` bigint DEFAULT '0' COMMENT '消耗时间',
  PRIMARY KEY (`oper_id`) USING BTREE,
  KEY `idx_sys_oper_log_bt` (`business_type`) USING BTREE,
  KEY `idx_sys_oper_log_s` (`status`) USING BTREE,
  KEY `idx_sys_oper_log_ot` (`oper_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='操作日志记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_oss`
--

DROP TABLE IF EXISTS `sys_oss`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_oss` (
  `oss_id` bigint NOT NULL COMMENT '对象存储主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '000000' COMMENT '租户编号',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '文件名',
  `original_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '原名',
  `file_suffix` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '文件后缀名',
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'URL地址',
  `ext1` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '扩展字段',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` bigint DEFAULT NULL COMMENT '上传人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `service` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'minio' COMMENT '服务商',
  PRIMARY KEY (`oss_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='OSS对象存储表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_oss_config`
--

DROP TABLE IF EXISTS `sys_oss_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_oss_config` (
  `oss_config_id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '000000' COMMENT '租户编号',
  `config_key` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '配置key',
  `access_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT 'accessKey',
  `secret_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '秘钥',
  `bucket_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '桶名称',
  `prefix` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '前缀',
  `endpoint` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '访问站点',
  `domain` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '自定义域名',
  `is_https` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'N' COMMENT '是否https（Y=是,N=否）',
  `region` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '域',
  `access_policy` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '1' COMMENT '桶权限类型(0=private 1=public 2=custom)',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '1' COMMENT '是否默认（0=是,1=否）',
  `ext1` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '扩展字段',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`oss_config_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='对象存储配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_post`
--

DROP TABLE IF EXISTS `sys_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_post` (
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '000000' COMMENT '租户编号',
  `dept_id` bigint NOT NULL COMMENT '部门id',
  `post_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位编码',
  `post_category` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '岗位类别编码',
  `post_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位名称',
  `post_sort` int NOT NULL COMMENT '显示顺序',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '状态（0正常 1停用）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`post_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='岗位信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '000000' COMMENT '租户编号',
  `role_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色权限字符串',
  `role_sort` int NOT NULL COMMENT '显示顺序',
  `data_scope` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限 6：部门及以下或本人数据权限）',
  `menu_check_strictly` tinyint(1) DEFAULT '1' COMMENT '菜单树选择项是否关联显示',
  `dept_check_strictly` tinyint(1) DEFAULT '1' COMMENT '部门树选择项是否关联显示',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='角色信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role_dept`
--

DROP TABLE IF EXISTS `sys_role_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_dept` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`role_id`,`dept_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='角色和部门关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role_menu`
--

DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`,`menu_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='角色和菜单关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_social`
--

DROP TABLE IF EXISTS `sys_social`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_social` (
  `id` bigint NOT NULL COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '000000' COMMENT '租户id',
  `auth_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '平台+平台唯一id',
  `source` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户来源',
  `open_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '平台编号唯一id',
  `user_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录账号',
  `nick_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '用户昵称',
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '用户邮箱',
  `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '头像地址',
  `access_token` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户的授权令牌',
  `expire_in` int DEFAULT NULL COMMENT '用户的授权令牌的有效期，部分平台可能没有',
  `refresh_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '刷新令牌，部分平台可能没有',
  `access_code` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '平台的授权信息，部分平台可能没有',
  `union_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '用户的 unionid',
  `scope` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '授予的权限，部分平台可能没有',
  `token_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '个别平台的授权信息，部分平台可能没有',
  `id_token` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'id token，部分平台可能没有',
  `mac_algorithm` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '小米平台用户的附带属性，部分平台可能没有',
  `mac_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '小米平台用户的附带属性，部分平台可能没有',
  `code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '用户的授权code，部分平台可能没有',
  `oauth_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'Twitter平台用户的附带属性，部分平台可能没有',
  `oauth_token_secret` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'Twitter平台用户的附带属性，部分平台可能没有',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='社会化关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_tenant`
--

DROP TABLE IF EXISTS `sys_tenant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_tenant` (
  `id` bigint NOT NULL COMMENT 'id',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户编号',
  `contact_user_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '联系电话',
  `company_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '企业名称',
  `license_number` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '统一社会信用代码',
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '地址',
  `intro` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '企业简介',
  `domain` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '域名',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `package_id` bigint DEFAULT NULL COMMENT '租户套餐编号',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `account_count` int DEFAULT '-1' COMMENT '用户数量（-1不限制）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '租户状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='租户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_tenant_package`
--

DROP TABLE IF EXISTS `sys_tenant_package`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_tenant_package` (
  `package_id` bigint NOT NULL COMMENT '租户套餐id',
  `package_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '套餐名称',
  `menu_ids` varchar(3000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '关联菜单id',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `menu_check_strictly` tinyint(1) DEFAULT '1' COMMENT '菜单树选择项是否关联显示',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`package_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='租户套餐表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '000000' COMMENT '租户编号',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `user_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户账号',
  `nick_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户昵称',
  `user_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'sys_user' COMMENT '用户类型（sys_user系统用户）',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '用户邮箱',
  `phonenumber` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '手机号码',
  `sex` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar` bigint DEFAULT NULL COMMENT '头像地址',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '密码',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `login_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '备注',
  `open_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '微信用户标识',
  `user_balance` double(20,2) DEFAULT '0.00' COMMENT '账户余额',
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='用户信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user_post`
--

DROP TABLE IF EXISTS `sys_user_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_post` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`user_id`,`post_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='用户与岗位关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`,`role_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='用户和角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `system_config_versions`
--

DROP TABLE IF EXISTS `system_config_versions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_config_versions` (
  `id` bigint NOT NULL,
  `config_key` varchar(128) COLLATE utf8mb4_general_ci NOT NULL,
  `config_value` text COLLATE utf8mb4_general_ci NOT NULL,
  `version` int NOT NULL,
  `effective_from` datetime NOT NULL,
  `effective_to` datetime DEFAULT NULL,
  `is_immutable` tinyint(1) NOT NULL DEFAULT '1',
  `changed_by` bigint NOT NULL,
  `change_reason` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_version` (`config_key`,`version`,`tenant_id`),
  KEY `idx_config_effective` (`config_key`,`effective_from`,`effective_to`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='参数版本持久化';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `system_configs`
--

DROP TABLE IF EXISTS `system_configs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_configs` (
  `id` bigint NOT NULL COMMENT '主键（雪花）',
  `config_key` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '参数键（如 bonus.salesSource / allowance.L3 / gate.signDeadlineDays）',
  `config_value` text COLLATE utf8mb4_general_ci NOT NULL,
  `value_type` varchar(16) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'STRING' COMMENT 'STRING|NUMBER|JSON|BOOL',
  `default_value` text COLLATE utf8mb4_general_ci,
  `description` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_dept` bigint DEFAULT NULL,
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci DEFAULT '000000',
  `del_flag` char(1) COLLATE utf8mb4_general_ci DEFAULT '0',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `validation_rule` json DEFAULT NULL,
  `source_ref` varchar(1024) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sc_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='IPD 系统参数（6 项涉钱参数等全部可配置 G-05）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `t_workflow`
--

DROP TABLE IF EXISTS `t_workflow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_workflow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'uuid',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '标题',
  `user_id` bigint NOT NULL DEFAULT '0' COMMENT '用户ID',
  `is_public` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否公开',
  `is_enable` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '备注',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除 默认0不删除',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=120 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='工作流定义（用户定义的工作流）| Workflow Definition';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `t_workflow_component`
--

DROP TABLE IF EXISTS `t_workflow_component`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_workflow_component` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `uuid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `display_order` int NOT NULL DEFAULT '0',
  `is_enable` tinyint(1) NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_display_order` (`display_order`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='工作流组件库 | Workflow Component';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `t_workflow_edge`
--

DROP TABLE IF EXISTS `t_workflow_edge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_workflow_edge` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `uuid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '边唯一标识',
  `workflow_id` bigint NOT NULL DEFAULT '0' COMMENT '所属工作流定义 id',
  `source_node_uuid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '起始节点 uuid',
  `source_handle` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '起始锚点标识',
  `target_node_uuid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '目标节点 uuid',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 正常，1 已删',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_workflow_edge_workflow_id` (`workflow_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=201 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='工作流定义的边 | Edge of Workflow Definition';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `t_workflow_node`
--

DROP TABLE IF EXISTS `t_workflow_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_workflow_node` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `uuid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '节点唯一标识',
  `workflow_id` bigint NOT NULL DEFAULT '0' COMMENT '所属工作流定义 id',
  `workflow_component_id` bigint NOT NULL DEFAULT '0' COMMENT '引用的组件 id',
  `user_id` bigint NOT NULL DEFAULT '0' COMMENT '创建人',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '节点标题',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '节点备注',
  `input_config` json NOT NULL COMMENT '输入参数模板，例：{"params":[{"name":"user_define_param01","type":"string"}]}',
  `node_config` json DEFAULT NULL COMMENT '节点执行配置，例：{"params":[{"prompt":"Summarize the following content:{user_define_param01}"}]}',
  `position_x` double NOT NULL DEFAULT '0' COMMENT '画布 x 坐标',
  `position_y` double NOT NULL DEFAULT '0' COMMENT '画布 y 坐标',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 正常，1 已删',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_workflow_node_workflow_id` (`workflow_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=272 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='工作流定义的节点 | Node of Workflow Definition';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `t_workflow_runtime`
--

DROP TABLE IF EXISTS `t_workflow_runtime`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_workflow_runtime` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `uuid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '运行实例唯一标识',
  `user_id` bigint NOT NULL DEFAULT '0' COMMENT '启动人',
  `workflow_id` bigint NOT NULL DEFAULT '0' COMMENT '对应工作流定义 id',
  `input` json DEFAULT NULL COMMENT '运行输入，例：{"userInput01":"text01","userInput02":true,"userInput03":10,"userInput04":["selectedA","selectedB"],"userInput05":["https://a.com/a.xlsx","https://a.com/b.png"]}',
  `output` json DEFAULT NULL COMMENT '运行输出，成功或失败的结果',
  `status` smallint NOT NULL DEFAULT '1' COMMENT '执行状态：1 就绪，2 执行中，3 成功，4 失败',
  `status_remark` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '状态补充说明，如失败原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 正常，1 已删',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_workflow_runtime_workflow_id` (`workflow_id`) USING BTREE,
  KEY `idx_workflow_runtime_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=297 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='工作流实例（运行时）| Workflow Runtime';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `t_workflow_runtime_node`
--

DROP TABLE IF EXISTS `t_workflow_runtime_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_workflow_runtime_node` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `uuid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '节点运行实例唯一标识',
  `user_id` bigint NOT NULL DEFAULT '0' COMMENT '创建人',
  `workflow_runtime_id` bigint NOT NULL DEFAULT '0' COMMENT '所属运行实例 id',
  `node_id` bigint NOT NULL DEFAULT '0' COMMENT '对应工作流定义里的节点 id',
  `input` json DEFAULT NULL COMMENT '节点本次输入数据',
  `output` json DEFAULT NULL COMMENT '节点本次输出数据',
  `status` smallint NOT NULL DEFAULT '1' COMMENT '节点执行状态：1 进行中，2 失败，3 成功',
  `status_remark` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '状态补充说明，如失败堆栈',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 正常，1 已删',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_runtime_node_runtime_id` (`workflow_runtime_id`) USING BTREE,
  KEY `idx_runtime_node_node_id` (`node_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=805 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='工作流实例（运行时）- 节点 | Workflow Runtime Node';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `test_demo`
--

DROP TABLE IF EXISTS `test_demo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `test_demo` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '000000' COMMENT '租户编号',
  `dept_id` bigint DEFAULT NULL COMMENT '部门id',
  `user_id` bigint DEFAULT NULL COMMENT '用户id',
  `order_num` int DEFAULT '0' COMMENT '排序号',
  `test_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'key键',
  `value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '值',
  `version` int DEFAULT '0' COMMENT '版本',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `del_flag` int DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='测试单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `test_leave`
--

DROP TABLE IF EXISTS `test_leave`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `test_leave` (
  `id` bigint NOT NULL COMMENT 'id',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '000000' COMMENT '租户编号',
  `apply_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '申请编号',
  `leave_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '请假类型',
  `start_date` datetime NOT NULL COMMENT '开始时间',
  `end_date` datetime NOT NULL COMMENT '结束时间',
  `leave_days` int NOT NULL COMMENT '请假天数',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '请假原因',
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '状态',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='请假申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `test_tree`
--

DROP TABLE IF EXISTS `test_tree`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `test_tree` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '000000' COMMENT '租户编号',
  `parent_id` bigint DEFAULT '0' COMMENT '父id',
  `dept_id` bigint DEFAULT NULL COMMENT '部门id',
  `user_id` bigint DEFAULT NULL COMMENT '用户id',
  `tree_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '值',
  `version` int DEFAULT '0' COMMENT '版本',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `del_flag` int DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='测试树表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `trace_node`
--

DROP TABLE IF EXISTS `trace_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trace_node` (
  `id` bigint NOT NULL COMMENT '主键',
  `trace_id` varchar(64) NOT NULL COMMENT '链路ID',
  `node_id` varchar(64) NOT NULL COMMENT '节点ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `parent_node_id` varchar(64) DEFAULT NULL COMMENT '父节点ID',
  `node_name` varchar(128) NOT NULL COMMENT '节点名称',
  `node_type` varchar(64) NOT NULL COMMENT '节点类型',
  `depth` int DEFAULT '0' COMMENT '节点深度',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `class_name` varchar(255) DEFAULT NULL COMMENT '类名',
  `method_name` varchar(128) DEFAULT NULL COMMENT '方法名',
  `status` varchar(32) NOT NULL COMMENT '状态',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration_ms` bigint DEFAULT NULL COMMENT '耗时毫秒',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '错误摘要',
  `input_payload` text COMMENT '输入JSON',
  `output_payload` text COMMENT '输出JSON',
  `metadata` text COMMENT '元数据JSON',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_trace_node_trace_id` (`trace_id`) USING BTREE,
  KEY `idx_trace_node_parent` (`trace_id`,`parent_node_id`) USING BTREE,
  KEY `idx_trace_node_time` (`trace_id`,`start_time`) USING BTREE,
  KEY `idx_trace_node_tenant_time` (`tenant_id`,`start_time`) USING BTREE,
  KEY `idx_trace_node_type_status` (`node_type`,`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='链路追踪节点记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `trace_run`
--

DROP TABLE IF EXISTS `trace_run`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trace_run` (
  `id` bigint NOT NULL COMMENT '主键',
  `trace_id` varchar(64) NOT NULL COMMENT '链路ID',
  `trace_name` varchar(128) NOT NULL COMMENT '链路名称',
  `business_type` varchar(64) NOT NULL COMMENT '业务类型',
  `business_id` varchar(128) DEFAULT NULL COMMENT '业务ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `status` varchar(32) NOT NULL COMMENT '状态',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration_ms` bigint DEFAULT NULL COMMENT '耗时毫秒',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '错误摘要',
  `metadata` text COMMENT '元数据JSON',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_trace_run_trace_id` (`trace_id`) USING BTREE,
  KEY `idx_trace_run_business` (`business_type`,`business_id`) USING BTREE,
  KEY `idx_trace_run_status_time` (`status`,`start_time`) USING BTREE,
  KEY `idx_trace_run_tenant_time` (`tenant_id`,`start_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='链路追踪运行记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-09  9:59:02
