/*
 Navicat MySQL Dump SQL

 Source Server         : 本地
 Source Server Type    : MySQL
 Source Server Version : 80045 (8.0.45)
 Source Host           : localhost:3306
 Source Schema         : ruoyi-ai

 Target Server Type    : MySQL
 Target Server Version : 80045 (8.0.45)
 File Encoding         : 65001

 Date: 27/02/2026 13:59:20
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
-- 忽略所有错误，继续执行
SET sql_mode = '';

-- ----------------------------
-- Table structure for chat_config
-- ----------------------------
DROP TABLE IF EXISTS `chat_config`;
CREATE TABLE `chat_config`  (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                `category` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置类型',
                                `config_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置名称',
                                `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置值',
                                `config_dict` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '说明',
                                `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                                `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
                                `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
                                `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
                                `version` int NULL DEFAULT NULL COMMENT '版本',
                                `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
                                `update_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新IP',
                                `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户Id',
                                PRIMARY KEY (`id`) USING BTREE,
                                UNIQUE INDEX `unique_category_key`(`category` ASC, `config_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '配置信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of chat_config
-- ----------------------------

-- ----------------------------
-- Table structure for chat_message
-- ----------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message`  (
                                 `id` bigint NOT NULL COMMENT '主键',
                                 `session_id` bigint NULL DEFAULT NULL COMMENT '会话id',
                                 `user_id` bigint NOT NULL COMMENT '用户id',
                                 `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '消息内容',
                                 `role` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '对话角色',
                                 `deduct_cost` double(20, 2) NULL DEFAULT 0.00 COMMENT '扣除金额',
                                 `total_tokens` int NULL DEFAULT 0 COMMENT '累计 Tokens',
                                 `model_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型名称',
                                 `billing_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '计费类型（1-token计费，2-次数计费）',
                                 `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                                 `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                                 `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                 `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                                 `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                 `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
                                 `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户Id',
                                 PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '聊天消息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of chat_message
-- ----------------------------

-- ----------------------------
-- Table structure for chat_model
-- ----------------------------
DROP TABLE IF EXISTS `chat_model`;
CREATE TABLE `chat_model`  (
                               `id` bigint NOT NULL COMMENT '主键',
                               `category` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型分类',
                               `model_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型名称',
                               `provider_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型供应商',
                               `model_describe` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型描述',
                               `model_price` double NULL DEFAULT NULL COMMENT '模型价格',
                               `model_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '计费类型',
                               `model_show` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否显示',
                               `model_free` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否免费',
                               `priority` int NULL DEFAULT 1 COMMENT '模型优先级(值越大优先级越高)',
                               `api_host` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求地址',
                               `api_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '密钥',
                               `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                               `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                               `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                               `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                               `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                               `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
                               `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户Id',
                               PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '模型管理' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of chat_model
-- ----------------------------
INSERT INTO `chat_model` VALUES (2000585866022060033, 'chat', 'deepseek/deepseek-v3.2', 'openai', 'deepseek', 1, '1', 'Y', 'Y', 1, 'https://api.ppinfra.com/openai', 'sk_xx', 103, 1, '2025-12-15 23:16:54', 1, '2026-02-06 01:02:31', 'DeepSeek-V3.2 是一款在高效推理、复杂推理能力与智能体场景中表现突出的领先模型。其基于 DeepSeek Sparse Attention（DSA）稀疏注意力机制，在显著降低计算开销的同时优化长上下文性能；通过可扩展强化学习框架，整体能力达到 GPT-5 同级，高算力版本 V3.2-Speciale 更在推理表现上接近 Gemini-3.0-Pro；同时，模型依托大型智能体任务合成管线，具备更强的工具调用与多步骤决策能力，并在 2025 年 IMO 与 IOI 中取得金牌级表现。作为 MaaS 平台，我们已对 DeepSeek-V3.2 完成深度适配，通过动态调度、批处理加速、低延迟推理与企业级 SLA 保障，进一步增强其在企业生产环境中的稳定性、性价比与可控性，适用于搜索、问答、智能体、代码、数据处理等多类高价值场景。', 0);
INSERT INTO `chat_model` VALUES (2007528268536287233, 'vector', 'baai/bge-m3', 'openai', 'bge-m3', 0, '1', 'N', 'Y', 1, 'https://api.ppinfra.com/openai', 'sk_xx', 103, 1, '2026-01-04 03:03:32', 1, '2026-02-06 01:02:35', 'bge-large-zh-v1.5', 0);

-- ----------------------------
-- Table structure for chat_provider
-- ----------------------------
DROP TABLE IF EXISTS `chat_provider`;
CREATE TABLE `chat_provider`  (
                                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                  `provider_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '厂商名称',
                                  `provider_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '厂商编码',
                                  `provider_icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '厂商图标',
                                  `provider_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '厂商描述',
                                  `api_host` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'API地址',
                                  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
                                  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
                                  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                                  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
                                  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
                                  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
                                  `version` int NULL DEFAULT NULL COMMENT '版本',
                                  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
                                  `update_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新IP',
                                  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户Id',
                                  PRIMARY KEY (`id`) USING BTREE,
                                  UNIQUE INDEX `unique_provider_code`(`provider_code` ASC, `tenant_id` ASC) USING BTREE,
                                  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2008460994477690882 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '厂商管理表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of chat_provider
-- ----------------------------
INSERT INTO `chat_provider` VALUES (1, 'OpenAI', 'openai', 'https://ruoyi-ai-1254149996.cos.ap-guangzhou.myqcloud.com/2025/12/15/9d944a6abfcd46e2bd6e364f07202589.png', 'OpenAI官方API服务商', 'https://api.openai.com', '0', 1, NULL, '2025-12-14 21:48:11', '1', '1', '2026-01-22 15:05:55', 'OpenAI厂商', NULL, '0', NULL, 0);
INSERT INTO `chat_provider` VALUES (2, '阿里云百炼', 'qianwen', 'https://ruoyi-ai-1254149996.cos.ap-guangzhou.myqcloud.com/2025/12/15/039ad13f690649f0ade139f8c803727b.png', '阿里云百炼大模型服务', 'https://dashscope.aliyuncs.com', '0', 2, NULL, '2025-12-14 21:48:11', '1', '1', '2026-02-06 00:58:22', '阿里云厂商', NULL, '0', NULL, 0);
INSERT INTO `chat_provider` VALUES (3, '智谱AI', 'zhipu', 'https://ruoyi-ai-1254149996.cos.ap-guangzhou.myqcloud.com/2025/12/15/a43e98fb7b3b4861b8caa6184e6fa40a.png', '智谱AI大模型服务', 'https://open.bigmodel.cn', '0', 3, NULL, '2025-12-14 21:48:11', '1', '1', '2026-02-06 00:49:14', '智谱AI厂商', NULL, '1', NULL, 0);
INSERT INTO `chat_provider` VALUES (5, 'ollama', 'ollama', 'https://ruoyi-ai-1254149996.cos.ap-guangzhou.myqcloud.com/2025/12/15/2ff984bc9e4249df992733b31959056b.png', 'ollama大模型', 'http://127.0.0.1:11434', '0', 5, NULL, '2025-12-14 21:48:11', '1', '1', '2025-12-15 00:49:05', 'ollama厂商', NULL, '0', NULL, 0);
INSERT INTO `chat_provider` VALUES (2000585060904435714, 'PPIO', 'ppio', 'https://ruoyi-ai-1254149996.cos.ap-guangzhou.myqcloud.com/2025/12/15/c4f8e304ce7740029b0024934d4625bc.png', 'api聚合厂商', 'https://api.ppinfra.com/openai', '0', 5, 103, '2025-12-15 23:13:42', '1', '1', '2026-01-02 00:54:45', 'api聚合厂商', NULL, '0', NULL, 0);

-- ----------------------------
-- Table structure for chat_session
-- ----------------------------
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session`  (
                                 `id` bigint NOT NULL COMMENT '主键',
                                 `user_id` bigint NULL DEFAULT NULL COMMENT '用户id',
                                 `session_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '会话标题',
                                 `session_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '会话内容',
                                 `create_dept` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '部门',
                                 `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                                 `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                 `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                                 `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                 `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
                                 `conversation_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '会话ID',
                                 `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户Id',
                                 PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会话管理' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of chat_session
-- ----------------------------

-- ----------------------------
-- Table structure for flow_category
-- ----------------------------
DROP TABLE IF EXISTS `flow_category`;
CREATE TABLE `flow_category`  (
                                  `category_id` bigint NOT NULL COMMENT '流程分类ID',
                                  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户编号',
                                  `parent_id` bigint NULL DEFAULT 0 COMMENT '父流程分类id',
                                  `ancestors` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '祖级列表',
                                  `category_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程分类名称',
                                  `order_num` int NULL DEFAULT 0 COMMENT '显示顺序',
                                  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
                                  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                                  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                                  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                                  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                  PRIMARY KEY (`category_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流程分类' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of flow_category
-- ----------------------------
INSERT INTO `flow_category` VALUES (100, '000000', 0, '0', 'OA审批', 0, '0', 103, 1, '2026-01-05 14:39:32', 1, '2026-01-09 10:09:44');
INSERT INTO `flow_category` VALUES (101, '000000', 100, '0,100', '假勤管理', 0, '0', 103, 1, '2026-01-05 14:39:32', 1, '2026-01-21 14:35:48');
INSERT INTO `flow_category` VALUES (102, '000000', 100, '0,100', '人事管理', 1, '0', 103, 1, '2026-01-05 14:39:32', NULL, NULL);
INSERT INTO `flow_category` VALUES (103, '000000', 101, '0,100,101', '请假', 0, '0', 103, 1, '2026-01-05 14:39:32', NULL, NULL);
INSERT INTO `flow_category` VALUES (104, '000000', 101, '0,100,101', '出差', 1, '0', 103, 1, '2026-01-05 14:39:32', 1, '2026-01-08 13:25:05');
INSERT INTO `flow_category` VALUES (105, '000000', 101, '0,100,101', '加班', 2, '0', 103, 1, '2026-01-05 14:39:32', NULL, NULL);
INSERT INTO `flow_category` VALUES (106, '000000', 101, '0,100,101', '换班', 3, '0', 103, 1, '2026-01-05 14:39:32', NULL, NULL);
INSERT INTO `flow_category` VALUES (107, '000000', 101, '0,100,101', '外出', 4, '0', 103, 1, '2026-01-05 14:39:33', NULL, NULL);
INSERT INTO `flow_category` VALUES (108, '000000', 102, '0,100,102', '转正', 1, '0', 103, 1, '2026-01-05 14:39:33', NULL, NULL);
INSERT INTO `flow_category` VALUES (109, '000000', 102, '0,100,102', '离职', 2, '0', 103, 1, '2026-01-05 14:39:33', NULL, NULL);
INSERT INTO `flow_category` VALUES (2008115831037366274, '000000', 103, '0,100,101,103', '11', 1, '1', 103, 1, '2026-01-05 17:58:18', 1, '2026-02-06 00:52:13');
INSERT INTO `flow_category` VALUES (2010981722649399297, '000000', 2008115831037366274, '0,100,101,103,2008115831037366274', 'ddw', 2, '1', 103, 1, '2026-01-13 15:46:20', 1, '2026-02-06 00:52:10');
INSERT INTO `flow_category` VALUES (2013552152677584898, '566749', 0, '0', 'OA审批', 0, '0', 103, 1, '2026-01-20 18:00:18', 1, '2026-01-20 18:00:18');
INSERT INTO `flow_category` VALUES (2014230589054521346, '000000', 100, '0,100', 'test001', 3, '1', 103, 1, '2026-01-22 14:56:10', 1, '2026-02-06 00:51:22');
INSERT INTO `flow_category` VALUES (2016836555117826050, '000000', 100, '0,100', '123', 213, '1', 103, 1, '2026-01-29 19:31:20', 1, '2026-02-06 00:51:19');
INSERT INTO `flow_category` VALUES (2018858143858167810, '154726', 0, '0', 'OA审批', 0, '0', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25');

-- ----------------------------
-- Table structure for flow_definition
-- ----------------------------
DROP TABLE IF EXISTS `flow_definition`;
CREATE TABLE `flow_definition`  (
                                    `id` bigint NOT NULL COMMENT '主键id',
                                    `flow_code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程编码',
                                    `flow_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程名称',
                                    `model_value` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'CLASSICS' COMMENT '设计器模型（CLASSICS经典模型 MIMIC仿钉钉模型）',
                                    `category` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '流程类别',
                                    `version` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程版本',
                                    `is_publish` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否发布（0未发布 1已发布 9失效）',
                                    `form_custom` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
                                    `form_path` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审批表单路径',
                                    `activity_status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '流程激活状态（0挂起 1激活）',
                                    `listener_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '监听器类型',
                                    `listener_path` varchar(400) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '监听器路径',
                                    `ext` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务详情 存业务表对象json字符串',
                                    `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                    `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建人',
                                    `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                    `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新人',
                                    `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志',
                                    `tenant_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '租户id',
                                    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流程定义表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of flow_definition
-- ----------------------------
INSERT INTO `flow_definition` VALUES (2008122523722002434, 'leave1', '请假申请-普通', 'CLASSICS', '103', '1', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-05 18:24:53', '1', '2026-01-05 18:25:52', '1', '1', '000000');
INSERT INTO `flow_definition` VALUES (2008122793122148354, 'leave2', '请假申请-排他网关', 'CLASSICS', '103', '1', 1, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-05 18:25:58', '1', '2026-01-06 13:53:34', '1', '0', '000000');
INSERT INTO `flow_definition` VALUES (2008125302444208129, 'leave1', '请假申请-普通', 'CLASSICS', '103', '1', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-05 18:35:56', '1', '2026-01-05 18:38:00', '1', '0', '000000');
INSERT INTO `flow_definition` VALUES (2008125415698804737, 'leave3', '请假申请-并行网关', 'CLASSICS', '103', '1', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '0', '000000');
INSERT INTO `flow_definition` VALUES (2008125510645264385, 'leave4', '请假申请-会签', 'CLASSICS', '103', '1', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '0', '000000');
INSERT INTO `flow_definition` VALUES (2008125553481691138, 'leave5', '请假申请-并行会签网关', 'MIMIC', '103', '1', 1, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-05 18:36:56', '1', '2026-01-28 10:41:46', '1', '0', '000000');
INSERT INTO `flow_definition` VALUES (2008125579910000641, 'leave6', '请假申请-排他并行会签', 'CLASSICS', '103', '1', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-05 18:37:02', '1', '2026-01-06 13:57:50', '1', '1', '000000');
INSERT INTO `flow_definition` VALUES (2008416817060646913, 'leave2', '请假申请-排他网关', 'CLASSICS', '103', '2', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-06 13:54:19', '1', '2026-01-06 13:54:31', '1', '0', '000000');
INSERT INTO `flow_definition` VALUES (2008418755936391170, 'level2', 'test', 'CLASSICS', '105', '1', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-06 14:02:01', '1', '2026-01-06 14:02:01', '1', '0', '000000');
INSERT INTO `flow_definition` VALUES (2009146711222652929, '12', '12', 'MIMIC', '105', '1', 0, 'N', '2', 1, NULL, NULL, NULL, '2026-01-08 14:14:39', '1', '2026-01-08 14:14:39', '1', '0', '000000');
INSERT INTO `flow_definition` VALUES (2010596518348853250, 'leave2', '请假申请-排他网关', 'CLASSICS', '103', '3', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '0', '000000');
INSERT INTO `flow_definition` VALUES (2010596885002326018, 'leave2', '请假申请-排他网关', 'CLASSICS', '103', '4', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '0', '000000');
INSERT INTO `flow_definition` VALUES (2010596973812518914, 'leave2', '请假申请-排他网关', 'CLASSICS', '103', '5', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '0', '000000');
INSERT INTO `flow_definition` VALUES (2010658026118320130, 'ew', 'ert', 'CLASSICS', '104', '1', 0, 'N', 'wer', 1, NULL, NULL, NULL, '2026-01-12 18:20:04', '1', '2026-01-12 18:20:04', '1', '0', '000000');
INSERT INTO `flow_definition` VALUES (2010981883312214018, 'ddd', 'dddd', 'CLASSICS', '2010981722649399297', '1', 0, 'N', 'dddd', 1, NULL, NULL, NULL, '2026-01-13 15:46:58', '1', '2026-02-06 00:52:00', '1', '1', '000000');
INSERT INTO `flow_definition` VALUES (2010987094571356161, '1111', '加班申请单', 'MIMIC', '105', '1', 0, 'N', '/user/list', 0, NULL, NULL, NULL, '2026-01-13 16:07:40', '1', '2026-01-13 16:07:57', '1', '0', '000000');
INSERT INTO `flow_definition` VALUES (2011373377982435329, 'abc', '测试出差', 'MIMIC', '104', '1', 0, 'N', 'chuchai', 0, NULL, NULL, NULL, '2026-01-14 17:42:37', '1', '2026-01-14 17:42:37', '1', '0', '000000');
INSERT INTO `flow_definition` VALUES (2012898128861204482, 'test121', '请假23', 'CLASSICS', '103', '1', 0, 'N', '无', 1, NULL, NULL, NULL, '2026-01-18 22:41:26', '1', '2026-01-18 22:41:26', '1', '0', '000000');
INSERT INTO `flow_definition` VALUES (2013548221742321665, '222', '2323', 'CLASSICS', '100', '1', 0, 'N', '2323', 1, NULL, NULL, NULL, '2026-01-20 17:44:41', '1', '2026-01-20 17:44:41', '1', '0', '000000');
INSERT INTO `flow_definition` VALUES (2013552152736305153, 'leave2', '请假申请-排他网关', 'CLASSICS', '2013552152677584898', '1', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-05 18:25:58', '1', '2026-01-06 13:53:34', '1', '0', '566749');
INSERT INTO `flow_definition` VALUES (2013552152971186178, 'leave1', '请假申请-普通', 'CLASSICS', '2013552152677584898', '1', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-05 18:35:56', '1', '2026-01-05 18:38:00', '1', '0', '566749');
INSERT INTO `flow_definition` VALUES (2013552153080238082, 'leave3', '请假申请-并行网关', 'CLASSICS', '2013552152677584898', '1', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '0', '566749');
INSERT INTO `flow_definition` VALUES (2013552153306730497, 'leave4', '请假申请-会签', 'CLASSICS', '2013552152677584898', '1', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '0', '566749');
INSERT INTO `flow_definition` VALUES (2013552153411588098, 'leave5', '请假申请-并行会签网关', 'MIMIC', '2013552152677584898', '1', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-05 18:36:56', '1', '2026-01-17 01:48:47', '1', '0', '566749');
INSERT INTO `flow_definition` VALUES (2013552153612914690, 'leave2', '请假申请-排他网关', 'CLASSICS', '2013552152677584898', '2', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-06 13:54:19', '1', '2026-01-06 13:54:31', '1', '0', '566749');
INSERT INTO `flow_definition` VALUES (2013552153831018498, 'level2', 'test', 'CLASSICS', '2013552152677584898', '1', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-06 14:02:01', '1', '2026-01-06 14:02:01', '1', '0', '566749');
INSERT INTO `flow_definition` VALUES (2013552153851990018, '12', '12', 'MIMIC', '2013552152677584898', '1', 0, 'N', '2', 1, NULL, NULL, NULL, '2026-01-08 14:14:39', '1', '2026-01-08 14:14:39', '1', '0', '566749');
INSERT INTO `flow_definition` VALUES (2013552153868767234, 'leave2', '请假申请-排他网关', 'CLASSICS', '2013552152677584898', '3', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '0', '566749');
INSERT INTO `flow_definition` VALUES (2013552154099453953, 'leave2', '请假申请-排他网关', 'CLASSICS', '2013552152677584898', '4', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '0', '566749');
INSERT INTO `flow_definition` VALUES (2013552154317557762, 'leave2', '请假申请-排他网关', 'CLASSICS', '2013552152677584898', '5', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '0', '566749');
INSERT INTO `flow_definition` VALUES (2013552154548244481, 'ew', 'ert', 'CLASSICS', '2013552152677584898', '1', 0, 'N', 'wer', 1, NULL, NULL, NULL, '2026-01-12 18:20:04', '1', '2026-01-12 18:20:04', '1', '0', '566749');
INSERT INTO `flow_definition` VALUES (2013552154569216002, 'ddd', 'dddd', 'CLASSICS', '2013552152677584898', '1', 0, 'N', 'dddd', 1, NULL, NULL, NULL, '2026-01-13 15:46:58', '1', '2026-01-13 15:46:58', '1', '0', '566749');
INSERT INTO `flow_definition` VALUES (2013552154590187522, '1111', '加班申请单', 'MIMIC', '2013552152677584898', '1', 0, 'N', '/user/list', 0, NULL, NULL, NULL, '2026-01-13 16:07:40', '1', '2026-01-13 16:07:57', '1', '0', '566749');
INSERT INTO `flow_definition` VALUES (2013552154606964737, 'abc', '测试出差', 'MIMIC', '2013552152677584898', '1', 0, 'N', 'chuchai', 0, NULL, NULL, NULL, '2026-01-14 17:42:37', '1', '2026-01-14 17:42:37', '1', '0', '566749');
INSERT INTO `flow_definition` VALUES (2013552154632130561, 'test121', '请假23', 'CLASSICS', '2013552152677584898', '1', 0, 'N', '无', 1, NULL, NULL, NULL, '2026-01-18 22:41:26', '1', '2026-01-18 22:41:26', '1', '0', '566749');
INSERT INTO `flow_definition` VALUES (2013552154653102081, '222', '2323', 'CLASSICS', '2013552152677584898', '1', 0, 'N', '2323', 1, NULL, NULL, NULL, '2026-01-20 17:44:41', '1', '2026-01-20 17:44:41', '1', '0', '566749');
INSERT INTO `flow_definition` VALUES (2015610460729118721, 'chuchai', '出差', 'MIMIC', '104', '1', 0, 'N', 'test', 1, NULL, NULL, NULL, '2026-01-26 10:19:17', '1', '2026-01-27 17:45:24', '1', '0', '000000');
INSERT INTO `flow_definition` VALUES (2016028336518729730, 'leave5', '请假申请-并行会签网关', 'MIMIC', '103', '2', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '0', '000000');
INSERT INTO `flow_definition` VALUES (2016799132254081025, 'eefw', '23', 'CLASSICS', '101', '1', 0, 'N', 'ewe', 1, NULL, NULL, NULL, '2026-01-29 17:02:38', '1', '2026-01-29 17:02:38', '1', '0', '000000');
INSERT INTO `flow_definition` VALUES (2018216957082472449, 'leave5', '请假申请-并行会签网关', 'MIMIC', '103', '3', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '0', '000000');
INSERT INTO `flow_definition` VALUES (2018858143975608322, 'leave2', '请假申请-排他网关', 'CLASSICS', '2018858143858167810', '1', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-05 18:25:58', '1', '2026-01-06 13:53:34', '1', '0', '154726');
INSERT INTO `flow_definition` VALUES (2018858144244043777, 'leave1', '请假申请-普通', 'CLASSICS', '2018858143858167810', '1', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-05 18:35:56', '1', '2026-01-05 18:38:00', '1', '0', '154726');
INSERT INTO `flow_definition` VALUES (2018858144369872897, 'leave3', '请假申请-并行网关', 'CLASSICS', '2018858143858167810', '1', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '0', '154726');
INSERT INTO `flow_definition` VALUES (2018858144617336833, 'leave4', '请假申请-会签', 'CLASSICS', '2018858143858167810', '1', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '0', '154726');
INSERT INTO `flow_definition` VALUES (2018858144743165953, 'leave5', '请假申请-并行会签网关', 'MIMIC', '2018858143858167810', '1', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-05 18:36:56', '1', '2026-01-28 10:41:46', '1', '0', '154726');
INSERT INTO `flow_definition` VALUES (2018858145003212802, 'leave2', '请假申请-排他网关', 'CLASSICS', '2018858143858167810', '2', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-06 13:54:19', '1', '2026-01-06 13:54:31', '1', '0', '154726');
INSERT INTO `flow_definition` VALUES (2018858145267453953, 'level2', 'test', 'CLASSICS', '2018858143858167810', '1', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-06 14:02:01', '1', '2026-01-06 14:02:01', '1', '0', '154726');
INSERT INTO `flow_definition` VALUES (2018858145288425473, '12', '12', 'MIMIC', '2018858143858167810', '1', 0, 'N', '2', 1, NULL, NULL, NULL, '2026-01-08 14:14:39', '1', '2026-01-08 14:14:39', '1', '0', '154726');
INSERT INTO `flow_definition` VALUES (2018858145313591297, 'leave2', '请假申请-排他网关', 'CLASSICS', '2018858143858167810', '3', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '0', '154726');
INSERT INTO `flow_definition` VALUES (2018858145577832449, 'leave2', '请假申请-排他网关', 'CLASSICS', '2018858143858167810', '4', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '0', '154726');
INSERT INTO `flow_definition` VALUES (2018858145875628033, 'leave2', '请假申请-排他网关', 'CLASSICS', '2018858143858167810', '5', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '0', '154726');
INSERT INTO `flow_definition` VALUES (2018858146144063489, 'ew', 'ert', 'CLASSICS', '2018858143858167810', '1', 0, 'N', 'wer', 1, NULL, NULL, NULL, '2026-01-12 18:20:04', '1', '2026-01-12 18:20:04', '1', '0', '154726');
INSERT INTO `flow_definition` VALUES (2018858146169229314, 'ddd', 'dddd', 'CLASSICS', '2018858143858167810', '1', 0, 'N', 'dddd', 1, NULL, NULL, NULL, '2026-01-13 15:46:58', '1', '2026-01-13 15:46:58', '1', '0', '154726');
INSERT INTO `flow_definition` VALUES (2018858146194395137, '1111', '加班申请单', 'MIMIC', '2018858143858167810', '1', 0, 'N', '/user/list', 0, NULL, NULL, NULL, '2026-01-13 16:07:40', '1', '2026-01-13 16:07:57', '1', '0', '154726');
INSERT INTO `flow_definition` VALUES (2018858146219560961, 'abc', '测试出差', 'MIMIC', '2018858143858167810', '1', 0, 'N', 'chuchai', 0, NULL, NULL, NULL, '2026-01-14 17:42:37', '1', '2026-01-14 17:42:37', '1', '0', '154726');
INSERT INTO `flow_definition` VALUES (2018858146244726786, 'test121', '请假23', 'CLASSICS', '2018858143858167810', '1', 0, 'N', '无', 1, NULL, NULL, NULL, '2026-01-18 22:41:26', '1', '2026-01-18 22:41:26', '1', '0', '154726');
INSERT INTO `flow_definition` VALUES (2018858146269892610, '222', '2323', 'CLASSICS', '2018858143858167810', '1', 0, 'N', '2323', 1, NULL, NULL, NULL, '2026-01-20 17:44:41', '1', '2026-01-20 17:44:41', '1', '0', '154726');
INSERT INTO `flow_definition` VALUES (2018858146290864130, 'chuchai', '出差', 'MIMIC', '2018858143858167810', '1', 0, 'N', 'test', 1, NULL, NULL, NULL, '2026-01-26 10:19:17', '1', '2026-01-27 17:45:24', '1', '0', '154726');
INSERT INTO `flow_definition` VALUES (2018858146316029954, 'leave5', '请假申请-并行会签网关', 'MIMIC', '2018858143858167810', '2', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '0', '154726');
INSERT INTO `flow_definition` VALUES (2018858146559299585, 'eefw', '23', 'CLASSICS', '2018858143858167810', '1', 0, 'N', 'ewe', 1, NULL, NULL, NULL, '2026-01-29 17:02:38', '1', '2026-01-29 17:02:38', '1', '0', '154726');
INSERT INTO `flow_definition` VALUES (2018858146580271106, 'leave5', '请假申请-并行会签网关', 'MIMIC', '2018858143858167810', '3', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '0', '154726');

-- ----------------------------
-- Table structure for flow_his_task
-- ----------------------------
DROP TABLE IF EXISTS `flow_his_task`;
CREATE TABLE `flow_his_task`  (
                                  `id` bigint NOT NULL COMMENT '主键id',
                                  `definition_id` bigint NOT NULL COMMENT '对应flow_definition表的id',
                                  `instance_id` bigint NOT NULL COMMENT '对应flow_instance表的id',
                                  `task_id` bigint NOT NULL COMMENT '对应flow_task表的id',
                                  `node_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '开始节点编码',
                                  `node_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '开始节点名称',
                                  `node_type` tinyint(1) NULL DEFAULT NULL COMMENT '开始节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
                                  `target_node_code` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '目标节点编码',
                                  `target_node_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '结束节点名称',
                                  `approver` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审批人',
                                  `cooperate_type` tinyint(1) NOT NULL DEFAULT 0 COMMENT '协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)',
                                  `collaborator` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '协作人',
                                  `skip_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流转类型（PASS通过 REJECT退回 NONE无动作）',
                                  `flow_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
                                  `form_custom` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
                                  `form_path` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审批表单路径',
                                  `message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审批意见',
                                  `variable` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '任务变量',
                                  `ext` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '业务详情 存业务表对象json字符串',
                                  `create_time` datetime NULL DEFAULT NULL COMMENT '任务开始时间',
                                  `update_time` datetime NULL DEFAULT NULL COMMENT '审批完成时间',
                                  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志',
                                  `tenant_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '租户id',
                                  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '历史任务记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of flow_his_task
-- ----------------------------
INSERT INTO `flow_his_task` VALUES (2016396747937550338, 2008122793122148354, 2016396747920773122, 2016396747929161730, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', '开始', 0, 'fdcae93b-b69c-498a-b231-09255e74bcbd', '申请人', '1', 1, NULL, 'PASS', 'draft', 'N', NULL, NULL, '{\n  \"leaveDays\" : 3,\n  \"userList\" : [ \"1\", \"3\", \"4\" ],\n  \"initiator\" : \"1\",\n  \"initiatorDeptId\" : 103,\n  \"businessId\" : \"2016396745383219202\",\n  \"autoPass\" : false,\n  \"businessCode\" : \"1769581421689\"\n}', NULL, '2026-01-28 14:23:43', '2026-01-28 14:23:43', '1', '000000');
INSERT INTO `flow_his_task` VALUES (2016396783530414081, 2008122793122148354, 2016396747920773122, 2016396747937550339, 'fdcae93b-b69c-498a-b231-09255e74bcbd', '申请人', 1, '5ed2362b-fc0c-4d52-831f-95208b830605', '部门领导', '1', 1, NULL, 'PASS', 'pass', 'N', '/workflow/leaveEdit/index', NULL, '{\n  \"businessCode\" : \"1769581421689\",\n  \"leaveDays\" : 3,\n  \"userList\" : [ \"1\", \"3\", \"4\" ],\n  \"messageType\" : [ \"1\" ],\n  \"submit\" : true,\n  \"initiator\" : \"1\",\n  \"businessId\" : \"2016396745383219202\",\n  \"initiatorDeptId\" : 103,\n  \"autoPass\" : false\n}', '', '2026-01-28 14:23:42', '2026-01-28 14:23:51', '1', '000000');

-- ----------------------------
-- Table structure for flow_instance
-- ----------------------------
DROP TABLE IF EXISTS `flow_instance`;
CREATE TABLE `flow_instance`  (
                                  `id` bigint NOT NULL COMMENT '主键id',
                                  `definition_id` bigint NOT NULL COMMENT '对应flow_definition表的id',
                                  `business_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务id',
                                  `node_type` tinyint(1) NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
                                  `node_code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程节点编码',
                                  `node_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '流程节点名称',
                                  `variable` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '任务变量',
                                  `flow_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
                                  `activity_status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '流程激活状态（0挂起 1激活）',
                                  `def_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '流程定义json',
                                  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建人',
                                  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新人',
                                  `ext` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '扩展字段，预留给业务系统使用',
                                  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志',
                                  `tenant_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '租户id',
                                  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流程实例表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of flow_instance
-- ----------------------------
INSERT INTO `flow_instance` VALUES (2016396747920773122, 2008122793122148354, '2016396745383219202', 1, '5ed2362b-fc0c-4d52-831f-95208b830605', '部门领导', '{\n  \"leaveDays\" : 3,\n  \"userList\" : [ \"1\", \"3\", \"4\" ],\n  \"initiator\" : \"1\",\n  \"initiatorDeptId\" : 103,\n  \"businessId\" : \"2016396745383219202\",\n  \"autoPass\" : false,\n  \"businessCode\" : \"1769581421689\"\n}', 'waiting', 1, '{\n  \"flowCode\" : \"leave2\",\n  \"flowName\" : \"请假申请-排他网关\",\n  \"modelValue\" : \"CLASSICS\",\n  \"category\" : \"103\",\n  \"version\" : \"1\",\n  \"isPublish\" : 1,\n  \"formCustom\" : \"N\",\n  \"formPath\" : \"/workflow/leaveEdit/index\",\n  \"nodeList\" : [ {\n    \"nodeType\" : 0,\n    \"nodeCode\" : \"cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a\",\n    \"nodeName\" : \"开始\",\n    \"nodeRatio\" : 0.0,\n    \"coordinate\" : \"300,240|300,240\",\n    \"formCustom\" : \"N\",\n    \"ext\" : \"[]\",\n    \"status\" : 2,\n    \"skipList\" : [ {\n      \"nowNodeCode\" : \"cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a\",\n      \"nextNodeCode\" : \"fdcae93b-b69c-498a-b231-09255e74bcbd\",\n      \"skipType\" : \"PASS\",\n      \"coordinate\" : \"320,240;390,240\",\n      \"status\" : 2,\n      \"createBy\" : \"1\",\n      \"updateBy\" : \"1\"\n    } ],\n    \"createBy\" : \"1\",\n    \"updateBy\" : \"1\"\n  }, {\n    \"nodeType\" : 1,\n    \"nodeCode\" : \"fdcae93b-b69c-498a-b231-09255e74bcbd\",\n    \"nodeName\" : \"申请人\",\n    \"nodeRatio\" : 0.0,\n    \"coordinate\" : \"440,240|440,240\",\n    \"formCustom\" : \"N\",\n    \"ext\" : \"[{\\\"code\\\":\\\"ButtonPermissionEnum\\\",\\\"value\\\":\\\"back,termination,file\\\"}]\",\n    \"status\" : 2,\n    \"skipList\" : [ {\n      \"nowNodeCode\" : \"fdcae93b-b69c-498a-b231-09255e74bcbd\",\n      \"nextNodeCode\" : \"7b8c7ead-7dc8-4951-a7f3-f0c41995909e\",\n      \"skipType\" : \"PASS\",\n      \"coordinate\" : \"490,240;535,240\",\n      \"status\" : 2,\n      \"createBy\" : \"1\",\n      \"updateBy\" : \"1\"\n    } ],\n    \"createBy\" : \"1\",\n    \"updateBy\" : \"1\"\n  }, {\n    \"nodeType\" : 3,\n    \"nodeCode\" : \"7b8c7ead-7dc8-4951-a7f3-f0c41995909e\",\n    \"nodeRatio\" : 0.0,\n    \"coordinate\" : \"560,240\",\n    \"formCustom\" : \"N\",\n    \"ext\" : \"[]\",\n    \"status\" : 2,\n    \"skipList\" : [ {\n      \"nowNodeCode\" : \"7b8c7ead-7dc8-4951-a7f3-f0c41995909e\",\n      \"nextNodeCode\" : \"b3528155-dcb7-4445-bbdf-3d00e3499e86\",\n      \"skipType\" : \"PASS\",\n      \"skipCondition\" : \"le@@leaveDays|2\",\n      \"coordinate\" : \"560,265;560,320;670,320\",\n      \"status\" : 0,\n      \"createBy\" : \"1\",\n      \"updateBy\" : \"1\"\n    }, {\n      \"nowNodeCode\" : \"7b8c7ead-7dc8-4951-a7f3-f0c41995909e\",\n      \"nextNodeCode\" : \"5ed2362b-fc0c-4d52-831f-95208b830605\",\n      \"skipName\" : \"大于两天\",\n      \"skipType\" : \"PASS\",\n      \"skipCondition\" : \"gt@@leaveDays|2\",\n      \"coordinate\" : \"560,215;560,160;670,160|560,187\",\n      \"status\" : 2,\n      \"createBy\" : \"1\",\n      \"updateBy\" : \"1\"\n    } ],\n    \"createBy\" : \"1\",\n    \"updateBy\" : \"1\"\n  }, {\n    \"nodeType\" : 1,\n    \"nodeCode\" : \"b3528155-dcb7-4445-bbdf-3d00e3499e86\",\n    \"nodeName\" : \"组长\",\n    \"permissionFlag\" : \"3@@4\",\n    \"nodeRatio\" : 0.0,\n    \"coordinate\" : \"720,320|720,320\",\n    \"formCustom\" : \"N\",\n    \"ext\" : \"[{\\\"code\\\":\\\"ButtonPermissionEnum\\\",\\\"value\\\":\\\"back,termination,file,transfer,trust,copy\\\"}]\",\n    \"status\" : 0,\n    \"skipList\" : [ {\n      \"nowNodeCode\" : \"b3528155-dcb7-4445-bbdf-3d00e3499e86\",\n      \"nextNodeCode\" : \"c9fa6d7d-2a74-4e78-b947-0cad8a6af869\",\n      \"skipType\" : \"PASS\",\n      \"coordinate\" : \"770,320;860,320;860,280\",\n      \"status\" : 0,\n      \"createBy\" : \"1\",\n      \"updateBy\" : \"1\"\n    } ],\n    \"createBy\" : \"1\",\n    \"updateBy\" : \"1\"\n  }, {\n    \"nodeType\" : 1,\n    \"nodeCode\" : \"c9fa6d7d-2a74-4e78-b947-0cad8a6af869\",\n    \"nodeName\" : \"总经理\",\n    \"permissionFlag\" : \"role:1\",\n    \"nodeRatio\" : 0.0,\n    \"coordinate\" : \"860,240|860,240\",\n    \"formCustom\" : \"N\",\n    \"ext\" : \"[{\\\"code\\\":\\\"ButtonPermissionEnum\\\",\\\"value\\\":\\\"back,termination,file,transfer,trust,copy\\\"}]\",\n    \"status\" : 0,\n    \"skipList\" : [ {\n      \"nowNodeCode\" : \"c9fa6d7d-2a74-4e78-b947-0cad8a6af869\",\n      \"nextNodeCode\" : \"40aa65fd-0712-4d23-b6f7-d0432b920fd1\",\n      \"skipType\" : \"PASS\",\n      \"coordinate\" : \"910,240;980,240\",\n      \"status\" : 0,\n      \"createBy\" : \"1\",\n      \"updateBy\" : \"1\"\n    } ],\n    \"createBy\" : \"1\",\n    \"updateBy\" : \"1\"\n  }, {\n    \"nodeType\" : 2,\n    \"nodeCode\" : \"40aa65fd-0712-4d23-b6f7-d0432b920fd1\",\n    \"nodeName\" : \"结束\",\n    \"nodeRatio\" : 0.0,\n    \"coordinate\" : \"1000,240|1000,240\",\n    \"formCustom\" : \"N\",\n    \"ext\" : \"[]\",\n    \"status\" : 0,\n    \"createBy\" : \"1\",\n    \"updateBy\" : \"1\"\n  }, {\n    \"nodeType\" : 1,\n    \"nodeCode\" : \"5ed2362b-fc0c-4d52-831f-95208b830605\",\n    \"nodeName\" : \"部门领导\",\n    \"permissionFlag\" : \"role:1\",\n    \"nodeRatio\" : 0.0,\n    \"coordinate\" : \"720,160|720,160\",\n    \"formCustom\" : \"N\",\n    \"ext\" : \"[{\\\"code\\\":\\\"ButtonPermissionEnum\\\",\\\"value\\\":\\\"back,termination,file,transfer,trust,copy\\\"}]\",\n    \"status\" : 1,\n    \"skipList\" : [ {\n      \"nowNodeCode\" : \"5ed2362b-fc0c-4d52-831f-95208b830605\",\n      \"nextNodeCode\" : \"c9fa6d7d-2a74-4e78-b947-0cad8a6af869\",\n      \"skipType\" : \"PASS\",\n      \"coordinate\" : \"770,160;860,160;860,200\",\n      \"status\" : 0,\n      \"createBy\" : \"1\",\n      \"updateBy\" : \"1\"\n    } ],\n    \"createBy\" : \"1\",\n    \"updateBy\" : \"1\"\n  } ],\n  \"topTextShow\" : false,\n  \"createBy\" : \"1\",\n  \"updateBy\" : \"1\"\n}', '2026-01-28 14:23:42', '1', '2026-02-06 00:52:47', '1', NULL, '1', '000000');

-- ----------------------------
-- Table structure for flow_instance_biz_ext
-- ----------------------------
DROP TABLE IF EXISTS `flow_instance_biz_ext`;
CREATE TABLE `flow_instance_biz_ext`  (
                                          `id` bigint NOT NULL COMMENT '主键id',
                                          `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户编号',
                                          `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                                          `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                                          `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                          `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                                          `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                          `business_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务编码',
                                          `business_title` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务标题',
                                          `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
                                          `instance_id` bigint NULL DEFAULT NULL COMMENT '流程实例Id',
                                          `business_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务Id',
                                          PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流程实例业务扩展表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of flow_instance_biz_ext
-- ----------------------------
INSERT INTO `flow_instance_biz_ext` VALUES (2016396751087472641, '000000', 103, 1, '2026-01-28 14:23:43', 1, '2026-01-28 14:23:43', '1769581421689', NULL, '0', 2016396747920773122, '2016396745383219202');

-- ----------------------------
-- Table structure for flow_node
-- ----------------------------
DROP TABLE IF EXISTS `flow_node`;
CREATE TABLE `flow_node`  (
                              `id` bigint NOT NULL COMMENT '主键id',
                              `node_type` tinyint(1) NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
                              `definition_id` bigint NOT NULL COMMENT '流程定义id',
                              `node_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程节点编码',
                              `node_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '流程节点名称',
                              `permission_flag` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '权限标识（权限类型:权限标识，可以多个，用@@隔开)',
                              `node_ratio` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '流程签署比例值',
                              `handler_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
                              `coordinate` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '坐标',
                              `any_node_skip` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '任意结点跳转',
                              `listener_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '监听器类型',
                              `listener_path` varchar(400) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '监听器路径',
                              `form_custom` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
                              `form_path` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审批表单路径',
                              `version` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '版本',
                              `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                              `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建人',
                              `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                              `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新人',
                              `ext` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '节点扩展属性',
                              `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志',
                              `tenant_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '租户id',
                              `handler_path` varchar(400) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '监听器路径',
                              PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流程节点表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of flow_node
-- ----------------------------
INSERT INTO `flow_node` VALUES (2008122524263067649, 0, 2008122523722002434, 'd5ee3ddf-3968-4379-a86f-9ceabde5faac', '开始', NULL, '0.000', NULL, '200,200|200,200', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:24:54', '1', '2026-01-05 18:24:54', '1', '[]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008122524263067650, 1, 2008122523722002434, 'dd515cdd-59f6-446f-94ca-25ca062afb42', '申请人', '', '0.000', NULL, '360,200|360,200', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:24:54', '1', '2026-01-05 18:24:54', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,copy\"}]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008122524263067651, 1, 2008122523722002434, '78fa8e5b-e809-44ed-978a-41092409ebcf', '组长', 'role:1', '0.000', NULL, '540,200|540,200', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:24:54', '1', '2026-01-05 18:24:54', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,copy,transfer,trust,file\"}]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008122524263067652, 1, 2008122523722002434, 'a8abf15f-b83e-428a-86cc-033555ea9bbe', '部门主管', 'role:3@@role:4', '0.000', NULL, '720,200|720,200', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:24:54', '1', '2026-01-05 18:24:54', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,copy,transfer,trust,file\"}]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008122524263067653, 2, 2008122523722002434, '8b82b7d7-8660-455e-b880-d6d22ea3eb6d', '结束', NULL, '0.000', NULL, '900,200|900,200', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:24:54', '1', '2026-01-05 18:24:54', '1', '[]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008122793638047746, 0, 2008122793122148354, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', '开始', NULL, '0.000', NULL, '300,240|300,240', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008122793638047747, 1, 2008122793122148354, 'fdcae93b-b69c-498a-b231-09255e74bcbd', '申请人', '', '0.000', NULL, '440,240|440,240', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008122793638047748, 3, 2008122793122148354, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', NULL, NULL, '0.000', NULL, '560,240', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008122793638047749, 1, 2008122793122148354, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', '组长', '3@@4', '0.000', NULL, '720,320|720,320', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008122793638047750, 1, 2008122793122148354, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', '总经理', 'role:1', '0.000', NULL, '860,240|860,240', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008122793638047751, 2, 2008122793122148354, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', '结束', NULL, '0.000', NULL, '1000,240|1000,240', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008122793638047752, 1, 2008122793122148354, '5ed2362b-fc0c-4d52-831f-95208b830605', '部门领导', 'role:1', '0.000', NULL, '720,160|720,160', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125302918164481, 0, 2008125302444208129, 'd5ee3ddf-3968-4379-a86f-9ceabde5faac', '开始', NULL, '0.000', NULL, '200,200|200,200', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:35:56', '1', '2026-01-05 18:35:56', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125302918164482, 1, 2008125302444208129, 'dd515cdd-59f6-446f-94ca-25ca062afb42', '申请人', '', '0.000', NULL, '360,200|360,200', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:35:56', '1', '2026-01-05 18:35:56', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,copy\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125302918164483, 1, 2008125302444208129, '78fa8e5b-e809-44ed-978a-41092409ebcf', '组长', 'role:1', '0.000', NULL, '540,200|540,200', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:35:56', '1', '2026-01-05 18:35:56', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,copy,transfer,trust,file\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125302918164484, 1, 2008125302444208129, 'a8abf15f-b83e-428a-86cc-033555ea9bbe', '部门主管', 'role:3@@role:4', '0.000', NULL, '720,200|720,200', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:35:56', '1', '2026-01-05 18:35:56', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,copy,transfer,trust,file\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125302918164485, 2, 2008125302444208129, '8b82b7d7-8660-455e-b880-d6d22ea3eb6d', '结束', NULL, '0.000', NULL, '900,200|900,200', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:35:56', '1', '2026-01-05 18:35:56', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125416223092738, 0, 2008125415698804737, 'a80ecf9f-f465-4ae5-a429-e30ec5d0f957', '开始', NULL, '0.000', NULL, '380,220|380,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125416223092739, 1, 2008125415698804737, 'b7bbb571-06de-455c-8083-f83c07bf0b99', '申请人', '', '0.000', NULL, '520,220|520,220', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125416223092740, 4, 2008125415698804737, '84d7ed24-bb44-4ba1-bf1f-e6f5092d3f0a', NULL, NULL, '0.000', NULL, '680,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125416223092741, 1, 2008125415698804737, '4b7743cd-940c-431b-926f-e7b614fbf1fe', '市场部', 'role:1', '0.000', NULL, '800,140|800,140', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125416223092742, 4, 2008125415698804737, 'b66b6563-f9fe-41cc-a782-f7837bb6f3d2', NULL, NULL, '0.000', NULL, '920,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125416223092743, 1, 2008125415698804737, '23e7429e-2b47-4431-b93e-40db7c431ce6', 'CEO', '1', '0.000', NULL, '1040,220|1040,220', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125416223092744, 2, 2008125415698804737, 'f5ace37f-5a5e-4e64-a6f6-913ab9a71cd1', '结束', NULL, '0.000', NULL, '1160,220|1160,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125416223092745, 1, 2008125415698804737, '762cb975-37d8-4276-b6db-79a4c3606394', '综合部', 'role:3@@role:4', '0.000', NULL, '800,300|800,300', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125511194718210, 0, 2008125510645264385, '9ce8bf00-f25b-4fc6-91b8-827082fc4876', '开始', NULL, '0.000', NULL, '320,240|320,240', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125511194718211, 1, 2008125510645264385, 'e90b98ef-35b4-410c-a663-bae8b7624b9f', '申请人', '', '0.000', NULL, '460,240|460,240', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125511194718212, 1, 2008125510645264385, '768b5b1a-6726-4d67-8853-4cc70d5b1045', '百分之60通过', '${userList}', '60.000', NULL, '640,240|640,240', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,addSign,subSign\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125511194718213, 1, 2008125510645264385, '2f9f2e21-9bcf-42a3-a07c-13037aad22d1', '全部审批通过', 'role:1@@role:3', '100.000', NULL, '820,240|820,240', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,addSign,subSign\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125511194718214, 1, 2008125510645264385, '27461e01-3d9f-4530-8fe3-bd5ec7f9571f', 'CEO', '1', '0.000', NULL, '1000,240|1000,240', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125511194718215, 2, 2008125510645264385, 'b62b88c3-8d8d-4969-911e-2aaea219e7fc', '结束', NULL, '0.000', NULL, '1120,240|1120,240', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125554068893698, 0, 2008125553481691138, 'ebebaf26-9cb6-497e-8119-4c9fed4c597c', '开始', NULL, '0.000', NULL, '300,220|300,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125554068893699, 1, 2008125553481691138, 'e1b04e96-dc81-4858-a309-2fe945d2f374', '申请人', '', '0.000', NULL, '420,220|420,220', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125554068893700, 4, 2008125553481691138, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', NULL, NULL, '0.000', NULL, '560,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125554068893701, 1, 2008125553481691138, 'c80f273e-1f17-4bd8-9ad1-04a4a94ea862', '会签', 'role:1@@role:3', '100.000', NULL, '700,320|700,320', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,addSign,subSign\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125554068893702, 4, 2008125553481691138, '1a20169e-3d82-4926-a151-e2daad28de1b', NULL, NULL, '0.000', NULL, '860,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125554068893703, 1, 2008125553481691138, '7a8f0473-e409-442e-a843-5c2b813d00e9', 'CEO', '1', '0.000', NULL, '1000,220|1000,220', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125554068893704, 2, 2008125553481691138, '03c4d2bc-58b5-4408-a2e4-65afb046f169', '结束', NULL, '0.000', NULL, '1140,220|1140,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125554068893705, 1, 2008125553481691138, '1e3e8d3b-18ae-4d6c-a814-ce0d724adfa4', '百分之60票签', '${userList}', '60.000', NULL, '700,120|700,120', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,addSign,subSign\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125580362985474, 0, 2008125579910000641, '122b89a5-7c6f-40a3-aa09-7a263f902054', '开始', NULL, '0.000', NULL, '240,300|240,300', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:37:02', '1', '2026-01-05 18:37:02', '1', '[]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125580362985475, 1, 2008125579910000641, 'c25a0e86-fdd1-4f03-8e22-14db70389dbd', '申请人', '', '0.000', NULL, '400,300|400,300', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:37:02', '1', '2026-01-05 18:37:02', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125580362985476, 1, 2008125579910000641, '2bfa3919-78cf-4bc1-b59b-df463a4546f9', '副经理', 'role:1@@role:3@@role:4', '0.000', NULL, '860,200|860,200', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:37:02', '1', '2026-01-05 18:37:02', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination\"}]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125580362985477, 1, 2008125579910000641, 'ec17f60e-94e0-4d96-a3ce-3417e9d32d60', '组长', '1', '0.000', NULL, '860,400|860,400', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:37:02', '1', '2026-01-05 18:37:02', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination\"}]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125580362985478, 1, 2008125579910000641, '07ecda1d-7a0a-47b5-8a91-6186c9473742', '副组长', '1', '0.000', NULL, '560,300|560,300', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:37:02', '1', '2026-01-05 18:37:02', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,transfer,copy,pop\"}]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125580362985479, 3, 2008125579910000641, '48117e2c-6328-406b-b102-c4a9d115bb13', NULL, NULL, '0.000', NULL, '700,300', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:37:02', '1', '2026-01-05 18:37:02', '1', '[]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125580362985480, 3, 2008125579910000641, '394e1cc8-b8b2-4189-9f81-44448e88ac32', NULL, NULL, '0.000', NULL, '1000,300', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:37:02', '1', '2026-01-05 18:37:02', '1', '[]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125580362985481, 1, 2008125579910000641, '9c93a195-cff2-4e17-ab0a-a4f264191496', '经理会签', '1@@3', '100.000', NULL, '1180,300|1180,300', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:37:02', '1', '2026-01-05 18:37:02', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,pop,addSign,subSign\"}]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125580362985482, 4, 2008125579910000641, 'a1a42056-afd1-4e90-88bc-36cbf5a66992', NULL, NULL, '0.000', NULL, '1340,300', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:37:02', '1', '2026-01-05 18:37:02', '1', '[]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125580362985483, 1, 2008125579910000641, '350dfa0c-a77c-4efa-8527-10efa02d8be4', '总经理', '3@@1', '0.000', NULL, '1480,200|1480,200', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:37:02', '1', '2026-01-05 18:37:02', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination\"}]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125580362985484, 1, 2008125579910000641, 'fcfdd9f6-f526-4c1a-b71d-88afa31aebc5', '副总经理', '1@@3', '0.000', NULL, '1480,400|1480,400', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:37:02', '1', '2026-01-05 18:37:02', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination\"}]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125580362985485, 4, 2008125579910000641, 'c36a46ef-04f9-463f-bad7-4b395c818519', NULL, NULL, '0.000', NULL, '1640,300', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:37:02', '1', '2026-01-05 18:37:02', '1', '[]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125580362985486, 1, 2008125579910000641, '3fcea762-b53a-4ae1-8365-7bec90444828', '董事', '1', '0.000', NULL, '1820,300|1820,300', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:37:02', '1', '2026-01-05 18:37:02', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination\"}]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125580362985487, 2, 2008125579910000641, '9cfbfd3e-6c04-41d6-9fc2-6787a7d2cd31', '结束', NULL, '0.000', NULL, '1980,300|1980,300', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:37:02', '1', '2026-01-05 18:37:02', '1', '[]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008416817064841217, 0, 2008416817060646913, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', '开始', NULL, '0.0', NULL, '300,240|300,240', NULL, NULL, NULL, 'N', NULL, '2', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008416817064841218, 1, 2008416817060646913, 'fdcae93b-b69c-498a-b231-09255e74bcbd', '申请人', '', '0.0', NULL, '440,240|440,240', NULL, '', '', 'N', NULL, '2', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008416817064841219, 3, 2008416817060646913, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', NULL, NULL, '0.0', NULL, '560,240', NULL, NULL, NULL, 'N', NULL, '2', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008416817064841220, 1, 2008416817060646913, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', '组长', '3@@4', '0.0', NULL, '720,320|720,320', NULL, '', '', 'N', NULL, '2', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008416817064841221, 1, 2008416817060646913, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', '总经理', 'role:1', '0.0', NULL, '860,240|860,240', NULL, '', '', 'N', NULL, '2', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008416817064841222, 2, 2008416817060646913, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', '结束', NULL, '0.0', NULL, '1000,240|1000,240', NULL, NULL, NULL, 'N', NULL, '2', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008416817064841223, 1, 2008416817060646913, '5ed2362b-fc0c-4d52-831f-95208b830605', '部门领导', 'role:1', '0.0', NULL, '720,160|720,160', NULL, '', '', 'N', NULL, '2', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2010596518348853251, 0, 2010596518348853250, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', '开始', NULL, '0.0', NULL, '300,240|300,240', NULL, NULL, NULL, 'N', NULL, '3', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2010596518353047554, 1, 2010596518348853250, 'fdcae93b-b69c-498a-b231-09255e74bcbd', '申请人', '', '0.0', NULL, '440,240|440,240', NULL, '', '', 'N', NULL, '3', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2010596518353047555, 3, 2010596518348853250, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', NULL, NULL, '0.0', NULL, '560,240', NULL, NULL, NULL, 'N', NULL, '3', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2010596518353047556, 1, 2010596518348853250, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', '组长', '3@@4', '0.0', NULL, '720,320|720,320', NULL, '', '', 'N', NULL, '3', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2010596518353047557, 1, 2010596518348853250, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', '总经理', 'role:1', '0.0', NULL, '860,240|860,240', NULL, '', '', 'N', NULL, '3', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2010596518353047558, 2, 2010596518348853250, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', '结束', NULL, '0.0', NULL, '1000,240|1000,240', NULL, NULL, NULL, 'N', NULL, '3', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2010596518353047559, 1, 2010596518348853250, '5ed2362b-fc0c-4d52-831f-95208b830605', '部门领导', 'role:1', '0.0', NULL, '720,160|720,160', NULL, '', '', 'N', NULL, '3', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2010596885002326019, 0, 2010596885002326018, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', '开始', NULL, '0.0', NULL, '300,240|300,240', NULL, NULL, NULL, 'N', NULL, '4', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2010596885002326020, 1, 2010596885002326018, 'fdcae93b-b69c-498a-b231-09255e74bcbd', '申请人', '', '0.0', NULL, '440,240|440,240', NULL, '', '', 'N', NULL, '4', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2010596885002326021, 3, 2010596885002326018, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', NULL, NULL, '0.0', NULL, '560,240', NULL, NULL, NULL, 'N', NULL, '4', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2010596885002326022, 1, 2010596885002326018, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', '组长', '3@@4', '0.0', NULL, '720,320|720,320', NULL, '', '', 'N', NULL, '4', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2010596885002326023, 1, 2010596885002326018, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', '总经理', 'role:1', '0.0', NULL, '860,240|860,240', NULL, '', '', 'N', NULL, '4', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2010596885002326024, 2, 2010596885002326018, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', '结束', NULL, '0.0', NULL, '1000,240|1000,240', NULL, NULL, NULL, 'N', NULL, '4', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2010596885002326025, 1, 2010596885002326018, '5ed2362b-fc0c-4d52-831f-95208b830605', '部门领导', 'role:1', '0.0', NULL, '720,160|720,160', NULL, '', '', 'N', NULL, '4', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2010596973812518915, 0, 2010596973812518914, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', '开始', NULL, '0.0', NULL, '300,240|300,240', NULL, NULL, NULL, 'N', NULL, '5', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2010596973812518916, 1, 2010596973812518914, 'fdcae93b-b69c-498a-b231-09255e74bcbd', '申请人', '', '0.0', NULL, '440,240|440,240', NULL, '', '', 'N', NULL, '5', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2010596973816713217, 3, 2010596973812518914, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', NULL, NULL, '0.0', NULL, '560,240', NULL, NULL, NULL, 'N', NULL, '5', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2010596973816713218, 1, 2010596973812518914, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', '组长', '3@@4', '0.0', NULL, '720,320|720,320', NULL, '', '', 'N', NULL, '5', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2010596973816713219, 1, 2010596973812518914, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', '总经理', 'role:1', '0.0', NULL, '860,240|860,240', NULL, '', '', 'N', NULL, '5', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2010596973816713220, 2, 2010596973812518914, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', '结束', NULL, '0.0', NULL, '1000,240|1000,240', NULL, NULL, NULL, 'N', NULL, '5', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2010596973816713221, 1, 2010596973812518914, '5ed2362b-fc0c-4d52-831f-95208b830605', '部门领导', 'role:1', '0.0', NULL, '720,160|720,160', NULL, '', '', 'N', NULL, '5', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2013552152769859586, 0, 2013552152736305153, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', '开始', NULL, '0.0', NULL, '300,240|300,240', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552152769859587, 1, 2013552152736305153, 'fdcae93b-b69c-498a-b231-09255e74bcbd', '申请人', NULL, '0.0', NULL, '440,240|440,240', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552152769859588, 3, 2013552152736305153, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', NULL, NULL, '0.0', NULL, '560,240', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552152769859589, 1, 2013552152736305153, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', '组长', NULL, '0.0', NULL, '720,320|720,320', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552152774053889, 1, 2013552152736305153, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', '总经理', NULL, '0.0', NULL, '860,240|860,240', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552152774053890, 2, 2013552152736305153, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', '结束', NULL, '0.0', NULL, '1000,240|1000,240', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552152774053891, 1, 2013552152736305153, '5ed2362b-fc0c-4d52-831f-95208b830605', '部门领导', NULL, '0.0', NULL, '720,160|720,160', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552152992157697, 0, 2013552152971186178, 'd5ee3ddf-3968-4379-a86f-9ceabde5faac', '开始', NULL, '0.0', NULL, '200,200|200,200', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:35:56', '1', '2026-01-05 18:35:56', '1', '[]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552152992157698, 1, 2013552152971186178, 'dd515cdd-59f6-446f-94ca-25ca062afb42', '申请人', NULL, '0.0', NULL, '360,200|360,200', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:35:56', '1', '2026-01-05 18:35:56', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,copy\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552152996352002, 1, 2013552152971186178, '78fa8e5b-e809-44ed-978a-41092409ebcf', '组长', NULL, '0.0', NULL, '540,200|540,200', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:35:56', '1', '2026-01-05 18:35:56', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,copy,transfer,trust,file\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552152996352003, 1, 2013552152971186178, 'a8abf15f-b83e-428a-86cc-033555ea9bbe', '部门主管', NULL, '0.0', NULL, '720,200|720,200', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:35:56', '1', '2026-01-05 18:35:56', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,copy,transfer,trust,file\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552152996352004, 2, 2013552152971186178, '8b82b7d7-8660-455e-b880-d6d22ea3eb6d', '结束', NULL, '0.0', NULL, '900,200|900,200', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:35:56', '1', '2026-01-05 18:35:56', '1', '[]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153113792514, 0, 2013552153080238082, 'a80ecf9f-f465-4ae5-a429-e30ec5d0f957', '开始', NULL, '0.0', NULL, '380,220|380,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '[]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153113792515, 1, 2013552153080238082, 'b7bbb571-06de-455c-8083-f83c07bf0b99', '申请人', NULL, '0.0', NULL, '520,220|520,220', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153117986817, 4, 2013552153080238082, '84d7ed24-bb44-4ba1-bf1f-e6f5092d3f0a', NULL, NULL, '0.0', NULL, '680,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '[]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153117986818, 1, 2013552153080238082, '4b7743cd-940c-431b-926f-e7b614fbf1fe', '市场部', NULL, '0.0', NULL, '800,140|800,140', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153117986819, 4, 2013552153080238082, 'b66b6563-f9fe-41cc-a782-f7837bb6f3d2', NULL, NULL, '0.0', NULL, '920,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '[]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153117986820, 1, 2013552153080238082, '23e7429e-2b47-4431-b93e-40db7c431ce6', 'CEO', NULL, '0.0', NULL, '1040,220|1040,220', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153117986821, 2, 2013552153080238082, 'f5ace37f-5a5e-4e64-a6f6-913ab9a71cd1', '结束', NULL, '0.0', NULL, '1160,220|1160,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '[]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153122181121, 1, 2013552153080238082, '762cb975-37d8-4276-b6db-79a4c3606394', '综合部', NULL, '0.0', NULL, '800,300|800,300', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153327702017, 0, 2013552153306730497, '9ce8bf00-f25b-4fc6-91b8-827082fc4876', '开始', NULL, '0.0', NULL, '320,240|320,240', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '[]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153331896321, 1, 2013552153306730497, 'e90b98ef-35b4-410c-a663-bae8b7624b9f', '申请人', NULL, '0.0', NULL, '460,240|460,240', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153331896322, 1, 2013552153306730497, '768b5b1a-6726-4d67-8853-4cc70d5b1045', '百分之60通过', NULL, '60.0', NULL, '640,240|640,240', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,addSign,subSign\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153331896323, 1, 2013552153306730497, '2f9f2e21-9bcf-42a3-a07c-13037aad22d1', '全部审批通过', NULL, '100.0', NULL, '820,240|820,240', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,addSign,subSign\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153331896324, 1, 2013552153306730497, '27461e01-3d9f-4530-8fe3-bd5ec7f9571f', 'CEO', NULL, '0.0', NULL, '1000,240|1000,240', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153331896325, 2, 2013552153306730497, 'b62b88c3-8d8d-4969-911e-2aaea219e7fc', '结束', NULL, '0.0', NULL, '1120,240|1120,240', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '[]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153436753921, 0, 2013552153411588098, 'ebebaf26-9cb6-497e-8119-4c9fed4c597c', '开始', NULL, '0.0', NULL, '300,220|300,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153436753922, 1, 2013552153411588098, 'e1b04e96-dc81-4858-a309-2fe945d2f374', '申请人', NULL, '0.0', NULL, '420,220|420,220', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153436753923, 4, 2013552153411588098, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', NULL, NULL, '0.0', NULL, '560,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153436753924, 1, 2013552153411588098, 'c80f273e-1f17-4bd8-9ad1-04a4a94ea862', '会签', NULL, '100.0', NULL, '700,320|700,320', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,addSign,subSign\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153440948226, 4, 2013552153411588098, '1a20169e-3d82-4926-a151-e2daad28de1b', NULL, NULL, '0.0', NULL, '860,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153440948227, 1, 2013552153411588098, '7a8f0473-e409-442e-a843-5c2b813d00e9', 'CEO', NULL, '0.0', NULL, '1000,220|1000,220', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153440948228, 2, 2013552153411588098, '03c4d2bc-58b5-4408-a2e4-65afb046f169', '结束', NULL, '0.0', NULL, '1140,220|1140,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153440948229, 1, 2013552153411588098, '1e3e8d3b-18ae-4d6c-a814-ce0d724adfa4', '百分之60票签', NULL, '60.0', NULL, '700,120|700,120', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,addSign,subSign\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153633886210, 0, 2013552153612914690, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', '开始', NULL, '0.0', NULL, '300,240|300,240', NULL, NULL, NULL, 'N', NULL, '2', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '[]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153638080514, 1, 2013552153612914690, 'fdcae93b-b69c-498a-b231-09255e74bcbd', '申请人', NULL, '0.0', NULL, '440,240|440,240', NULL, '', '', 'N', NULL, '2', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153638080515, 3, 2013552153612914690, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', NULL, NULL, '0.0', NULL, '560,240', NULL, NULL, NULL, 'N', NULL, '2', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '[]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153638080516, 1, 2013552153612914690, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', '组长', NULL, '0.0', NULL, '720,320|720,320', NULL, '', '', 'N', NULL, '2', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153638080517, 1, 2013552153612914690, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', '总经理', NULL, '0.0', NULL, '860,240|860,240', NULL, '', '', 'N', NULL, '2', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153638080518, 2, 2013552153612914690, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', '结束', NULL, '0.0', NULL, '1000,240|1000,240', NULL, NULL, NULL, 'N', NULL, '2', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '[]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153642274818, 1, 2013552153612914690, '5ed2362b-fc0c-4d52-831f-95208b830605', '部门领导', NULL, '0.0', NULL, '720,160|720,160', NULL, '', '', 'N', NULL, '2', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153889738753, 0, 2013552153868767234, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', '开始', NULL, '0.0', NULL, '300,240|300,240', NULL, NULL, NULL, 'N', NULL, '3', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '[]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153889738754, 1, 2013552153868767234, 'fdcae93b-b69c-498a-b231-09255e74bcbd', '申请人', NULL, '0.0', NULL, '440,240|440,240', NULL, '', '', 'N', NULL, '3', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153889738755, 3, 2013552153868767234, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', NULL, NULL, '0.0', NULL, '560,240', NULL, NULL, NULL, 'N', NULL, '3', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '[]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153889738756, 1, 2013552153868767234, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', '组长', NULL, '0.0', NULL, '720,320|720,320', NULL, '', '', 'N', NULL, '3', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153893933058, 1, 2013552153868767234, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', '总经理', NULL, '0.0', NULL, '860,240|860,240', NULL, '', '', 'N', NULL, '3', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153893933059, 2, 2013552153868767234, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', '结束', NULL, '0.0', NULL, '1000,240|1000,240', NULL, NULL, NULL, 'N', NULL, '3', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '[]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552153893933060, 1, 2013552153868767234, '5ed2362b-fc0c-4d52-831f-95208b830605', '部门领导', NULL, '0.0', NULL, '720,160|720,160', NULL, '', '', 'N', NULL, '3', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552154120425473, 0, 2013552154099453953, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', '开始', NULL, '0.0', NULL, '300,240|300,240', NULL, NULL, NULL, 'N', NULL, '4', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '[]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552154120425474, 1, 2013552154099453953, 'fdcae93b-b69c-498a-b231-09255e74bcbd', '申请人', NULL, '0.0', NULL, '440,240|440,240', NULL, '', '', 'N', NULL, '4', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552154120425475, 3, 2013552154099453953, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', NULL, NULL, '0.0', NULL, '560,240', NULL, NULL, NULL, 'N', NULL, '4', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '[]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552154120425476, 1, 2013552154099453953, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', '组长', NULL, '0.0', NULL, '720,320|720,320', NULL, '', '', 'N', NULL, '4', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552154120425477, 1, 2013552154099453953, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', '总经理', NULL, '0.0', NULL, '860,240|860,240', NULL, '', '', 'N', NULL, '4', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552154120425478, 2, 2013552154099453953, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', '结束', NULL, '0.0', NULL, '1000,240|1000,240', NULL, NULL, NULL, 'N', NULL, '4', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '[]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552154124619777, 1, 2013552154099453953, '5ed2362b-fc0c-4d52-831f-95208b830605', '部门领导', NULL, '0.0', NULL, '720,160|720,160', NULL, '', '', 'N', NULL, '4', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552154338529282, 0, 2013552154317557762, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', '开始', NULL, '0.0', NULL, '300,240|300,240', NULL, NULL, NULL, 'N', NULL, '5', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '[]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552154342723585, 1, 2013552154317557762, 'fdcae93b-b69c-498a-b231-09255e74bcbd', '申请人', NULL, '0.0', NULL, '440,240|440,240', NULL, '', '', 'N', NULL, '5', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552154342723586, 3, 2013552154317557762, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', NULL, NULL, '0.0', NULL, '560,240', NULL, NULL, NULL, 'N', NULL, '5', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '[]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552154342723587, 1, 2013552154317557762, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', '组长', NULL, '0.0', NULL, '720,320|720,320', NULL, '', '', 'N', NULL, '5', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552154342723588, 1, 2013552154317557762, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', '总经理', NULL, '0.0', NULL, '860,240|860,240', NULL, '', '', 'N', NULL, '5', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552154342723589, 2, 2013552154317557762, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', '结束', NULL, '0.0', NULL, '1000,240|1000,240', NULL, NULL, NULL, 'N', NULL, '5', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '[]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2013552154346917890, 1, 2013552154317557762, '5ed2362b-fc0c-4d52-831f-95208b830605', '部门领导', NULL, '0.0', NULL, '720,160|720,160', NULL, '', '', 'N', NULL, '5', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '566749', NULL);
INSERT INTO `flow_node` VALUES (2016028336518729731, 0, 2016028336518729730, 'ebebaf26-9cb6-497e-8119-4c9fed4c597c', '开始', NULL, '0.0', NULL, '300,220|300,220', NULL, NULL, NULL, 'N', NULL, '2', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2016028336518729732, 1, 2016028336518729730, 'e1b04e96-dc81-4858-a309-2fe945d2f374', '申请人', '', '0.0', NULL, '420,220|420,220', NULL, '', '', 'N', NULL, '2', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2016028336518729733, 4, 2016028336518729730, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', NULL, NULL, '0.0', NULL, '560,220', NULL, NULL, NULL, 'N', NULL, '2', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2016028336518729734, 1, 2016028336518729730, 'c80f273e-1f17-4bd8-9ad1-04a4a94ea862', '会签', 'role:1@@role:3', '100.0', NULL, '700,320|700,320', NULL, '', '', 'N', NULL, '2', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,addSign,subSign\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2016028336518729735, 4, 2016028336518729730, '1a20169e-3d82-4926-a151-e2daad28de1b', NULL, NULL, '0.0', NULL, '860,220', NULL, NULL, NULL, 'N', NULL, '2', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2016028336522924033, 1, 2016028336518729730, '7a8f0473-e409-442e-a843-5c2b813d00e9', 'CEO', '1', '0.0', NULL, '1000,220|1000,220', NULL, '', '', 'N', NULL, '2', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2016028336522924034, 2, 2016028336518729730, '03c4d2bc-58b5-4408-a2e4-65afb046f169', '结束', NULL, '0.0', NULL, '1140,220|1140,220', NULL, NULL, NULL, 'N', NULL, '2', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2016028336522924035, 1, 2016028336518729730, '1e3e8d3b-18ae-4d6c-a814-ce0d724adfa4', '百分之60票签', '${userList}', '60.0', NULL, '700,120|700,120', NULL, '', '', 'N', NULL, '2', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,addSign,subSign\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2018216957082472450, 0, 2018216957082472449, 'ebebaf26-9cb6-497e-8119-4c9fed4c597c', '开始', NULL, '0.0', NULL, '300,220|300,220', NULL, NULL, NULL, 'N', NULL, '3', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2018216957082472451, 1, 2018216957082472449, 'e1b04e96-dc81-4858-a309-2fe945d2f374', '申请人', '', '0.0', NULL, '420,220|420,220', NULL, '', '', 'N', NULL, '3', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2018216957082472452, 4, 2018216957082472449, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', NULL, NULL, '0.0', NULL, '560,220', NULL, NULL, NULL, 'N', NULL, '3', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2018216957082472453, 1, 2018216957082472449, 'c80f273e-1f17-4bd8-9ad1-04a4a94ea862', '会签', 'role:1@@role:3', '100.0', NULL, '700,320|700,320', NULL, '', '', 'N', NULL, '3', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,addSign,subSign\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2018216957082472454, 4, 2018216957082472449, '1a20169e-3d82-4926-a151-e2daad28de1b', NULL, NULL, '0.0', NULL, '860,220', NULL, NULL, NULL, 'N', NULL, '3', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2018216957082472455, 1, 2018216957082472449, '7a8f0473-e409-442e-a843-5c2b813d00e9', 'CEO', '1', '0.0', NULL, '1000,220|1000,220', NULL, '', '', 'N', NULL, '3', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2018216957082472456, 2, 2018216957082472449, '03c4d2bc-58b5-4408-a2e4-65afb046f169', '结束', NULL, '0.0', NULL, '1140,220|1140,220', NULL, NULL, NULL, 'N', NULL, '3', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '[]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2018216957082472457, 1, 2018216957082472449, '1e3e8d3b-18ae-4d6c-a814-ce0d724adfa4', '百分之60票签', '${userList}', '60.0', NULL, '700,120|700,120', NULL, '', '', 'N', NULL, '3', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,addSign,subSign\"}]', '0', '000000', NULL);
INSERT INTO `flow_node` VALUES (2018858144000774145, 0, 2018858143975608322, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', '开始', NULL, '0.0', NULL, '300,240|300,240', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144004968449, 1, 2018858143975608322, 'fdcae93b-b69c-498a-b231-09255e74bcbd', '申请人', NULL, '0.0', NULL, '440,240|440,240', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144004968450, 3, 2018858143975608322, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', NULL, NULL, '0.0', NULL, '560,240', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144004968451, 1, 2018858143975608322, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', '组长', NULL, '0.0', NULL, '720,320|720,320', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144004968452, 1, 2018858143975608322, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', '总经理', NULL, '0.0', NULL, '860,240|860,240', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144004968453, 2, 2018858143975608322, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', '结束', NULL, '0.0', NULL, '1000,240|1000,240', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144009162753, 1, 2018858143975608322, '5ed2362b-fc0c-4d52-831f-95208b830605', '部门领导', NULL, '0.0', NULL, '720,160|720,160', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144269209602, 0, 2018858144244043777, 'd5ee3ddf-3968-4379-a86f-9ceabde5faac', '开始', NULL, '0.0', NULL, '200,200|200,200', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:35:56', '1', '2026-01-05 18:35:56', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144269209603, 1, 2018858144244043777, 'dd515cdd-59f6-446f-94ca-25ca062afb42', '申请人', NULL, '0.0', NULL, '360,200|360,200', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:35:56', '1', '2026-01-05 18:35:56', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,copy\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144273403906, 1, 2018858144244043777, '78fa8e5b-e809-44ed-978a-41092409ebcf', '组长', NULL, '0.0', NULL, '540,200|540,200', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:35:56', '1', '2026-01-05 18:35:56', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,copy,transfer,trust,file\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144273403907, 1, 2018858144244043777, 'a8abf15f-b83e-428a-86cc-033555ea9bbe', '部门主管', NULL, '0.0', NULL, '720,200|720,200', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:35:56', '1', '2026-01-05 18:35:56', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,copy,transfer,trust,file\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144273403908, 2, 2018858144244043777, '8b82b7d7-8660-455e-b880-d6d22ea3eb6d', '结束', NULL, '0.0', NULL, '900,200|900,200', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:35:56', '1', '2026-01-05 18:35:56', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144395038722, 0, 2018858144369872897, 'a80ecf9f-f465-4ae5-a429-e30ec5d0f957', '开始', NULL, '0.0', NULL, '380,220|380,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144395038723, 1, 2018858144369872897, 'b7bbb571-06de-455c-8083-f83c07bf0b99', '申请人', NULL, '0.0', NULL, '520,220|520,220', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144395038724, 4, 2018858144369872897, '84d7ed24-bb44-4ba1-bf1f-e6f5092d3f0a', NULL, NULL, '0.0', NULL, '680,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144399233026, 1, 2018858144369872897, '4b7743cd-940c-431b-926f-e7b614fbf1fe', '市场部', NULL, '0.0', NULL, '800,140|800,140', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144399233027, 4, 2018858144369872897, 'b66b6563-f9fe-41cc-a782-f7837bb6f3d2', NULL, NULL, '0.0', NULL, '920,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144399233028, 1, 2018858144369872897, '23e7429e-2b47-4431-b93e-40db7c431ce6', 'CEO', NULL, '0.0', NULL, '1040,220|1040,220', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144399233029, 2, 2018858144369872897, 'f5ace37f-5a5e-4e64-a6f6-913ab9a71cd1', '结束', NULL, '0.0', NULL, '1160,220|1160,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144403427330, 1, 2018858144369872897, '762cb975-37d8-4276-b6db-79a4c3606394', '综合部', NULL, '0.0', NULL, '800,300|800,300', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144642502657, 0, 2018858144617336833, '9ce8bf00-f25b-4fc6-91b8-827082fc4876', '开始', NULL, '0.0', NULL, '320,240|320,240', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144642502658, 1, 2018858144617336833, 'e90b98ef-35b4-410c-a663-bae8b7624b9f', '申请人', NULL, '0.0', NULL, '460,240|460,240', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144646696962, 1, 2018858144617336833, '768b5b1a-6726-4d67-8853-4cc70d5b1045', '百分之60通过', NULL, '60.0', NULL, '640,240|640,240', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,addSign,subSign\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144646696963, 1, 2018858144617336833, '2f9f2e21-9bcf-42a3-a07c-13037aad22d1', '全部审批通过', NULL, '100.0', NULL, '820,240|820,240', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,addSign,subSign\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144646696964, 1, 2018858144617336833, '27461e01-3d9f-4530-8fe3-bd5ec7f9571f', 'CEO', NULL, '0.0', NULL, '1000,240|1000,240', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144646696965, 2, 2018858144617336833, 'b62b88c3-8d8d-4969-911e-2aaea219e7fc', '结束', NULL, '0.0', NULL, '1120,240|1120,240', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144768331777, 0, 2018858144743165953, 'ebebaf26-9cb6-497e-8119-4c9fed4c597c', '开始', NULL, '0.0', NULL, '300,220|300,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144768331778, 1, 2018858144743165953, 'e1b04e96-dc81-4858-a309-2fe945d2f374', '申请人', NULL, '0.0', NULL, '420,220|420,220', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144768331779, 4, 2018858144743165953, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', NULL, NULL, '0.0', NULL, '560,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144772526081, 1, 2018858144743165953, 'c80f273e-1f17-4bd8-9ad1-04a4a94ea862', '会签', NULL, '100.0', NULL, '700,320|700,320', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,addSign,subSign\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144772526082, 4, 2018858144743165953, '1a20169e-3d82-4926-a151-e2daad28de1b', NULL, NULL, '0.0', NULL, '860,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144772526083, 1, 2018858144743165953, '7a8f0473-e409-442e-a843-5c2b813d00e9', 'CEO', NULL, '0.0', NULL, '1000,220|1000,220', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144772526084, 2, 2018858144743165953, '03c4d2bc-58b5-4408-a2e4-65afb046f169', '结束', NULL, '0.0', NULL, '1140,220|1140,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858144772526085, 1, 2018858144743165953, '1e3e8d3b-18ae-4d6c-a814-ce0d724adfa4', '百分之60票签', NULL, '60.0', NULL, '700,120|700,120', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,addSign,subSign\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145028378626, 0, 2018858145003212802, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', '开始', NULL, '0.0', NULL, '300,240|300,240', NULL, NULL, NULL, 'N', NULL, '2', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145028378627, 1, 2018858145003212802, 'fdcae93b-b69c-498a-b231-09255e74bcbd', '申请人', NULL, '0.0', NULL, '440,240|440,240', NULL, '', '', 'N', NULL, '2', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145028378628, 3, 2018858145003212802, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', NULL, NULL, '0.0', NULL, '560,240', NULL, NULL, NULL, 'N', NULL, '2', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145032572930, 1, 2018858145003212802, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', '组长', NULL, '0.0', NULL, '720,320|720,320', NULL, '', '', 'N', NULL, '2', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145032572931, 1, 2018858145003212802, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', '总经理', NULL, '0.0', NULL, '860,240|860,240', NULL, '', '', 'N', NULL, '2', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145032572932, 2, 2018858145003212802, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', '结束', NULL, '0.0', NULL, '1000,240|1000,240', NULL, NULL, NULL, 'N', NULL, '2', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145032572933, 1, 2018858145003212802, '5ed2362b-fc0c-4d52-831f-95208b830605', '部门领导', NULL, '0.0', NULL, '720,160|720,160', NULL, '', '', 'N', NULL, '2', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145338757121, 0, 2018858145313591297, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', '开始', NULL, '0.0', NULL, '300,240|300,240', NULL, NULL, NULL, 'N', NULL, '3', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145338757122, 1, 2018858145313591297, 'fdcae93b-b69c-498a-b231-09255e74bcbd', '申请人', NULL, '0.0', NULL, '440,240|440,240', NULL, '', '', 'N', NULL, '3', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145338757123, 3, 2018858145313591297, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', NULL, NULL, '0.0', NULL, '560,240', NULL, NULL, NULL, 'N', NULL, '3', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145338757124, 1, 2018858145313591297, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', '组长', NULL, '0.0', NULL, '720,320|720,320', NULL, '', '', 'N', NULL, '3', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145338757125, 1, 2018858145313591297, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', '总经理', NULL, '0.0', NULL, '860,240|860,240', NULL, '', '', 'N', NULL, '3', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145342951426, 2, 2018858145313591297, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', '结束', NULL, '0.0', NULL, '1000,240|1000,240', NULL, NULL, NULL, 'N', NULL, '3', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145342951427, 1, 2018858145313591297, '5ed2362b-fc0c-4d52-831f-95208b830605', '部门领导', NULL, '0.0', NULL, '720,160|720,160', NULL, '', '', 'N', NULL, '3', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145602998273, 0, 2018858145577832449, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', '开始', NULL, '0.0', NULL, '300,240|300,240', NULL, NULL, NULL, 'N', NULL, '4', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145602998274, 1, 2018858145577832449, 'fdcae93b-b69c-498a-b231-09255e74bcbd', '申请人', NULL, '0.0', NULL, '440,240|440,240', NULL, '', '', 'N', NULL, '4', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145602998275, 3, 2018858145577832449, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', NULL, NULL, '0.0', NULL, '560,240', NULL, NULL, NULL, 'N', NULL, '4', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145607192578, 1, 2018858145577832449, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', '组长', NULL, '0.0', NULL, '720,320|720,320', NULL, '', '', 'N', NULL, '4', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145607192579, 1, 2018858145577832449, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', '总经理', NULL, '0.0', NULL, '860,240|860,240', NULL, '', '', 'N', NULL, '4', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145607192580, 2, 2018858145577832449, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', '结束', NULL, '0.0', NULL, '1000,240|1000,240', NULL, NULL, NULL, 'N', NULL, '4', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145607192581, 1, 2018858145577832449, '5ed2362b-fc0c-4d52-831f-95208b830605', '部门领导', NULL, '0.0', NULL, '720,160|720,160', NULL, '', '', 'N', NULL, '4', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145900793858, 0, 2018858145875628033, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', '开始', NULL, '0.0', NULL, '300,240|300,240', NULL, NULL, NULL, 'N', NULL, '5', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145904988161, 1, 2018858145875628033, 'fdcae93b-b69c-498a-b231-09255e74bcbd', '申请人', NULL, '0.0', NULL, '440,240|440,240', NULL, '', '', 'N', NULL, '5', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145904988162, 3, 2018858145875628033, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', NULL, NULL, '0.0', NULL, '560,240', NULL, NULL, NULL, 'N', NULL, '5', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145904988163, 1, 2018858145875628033, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', '组长', NULL, '0.0', NULL, '720,320|720,320', NULL, '', '', 'N', NULL, '5', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145904988164, 1, 2018858145875628033, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', '总经理', NULL, '0.0', NULL, '860,240|860,240', NULL, '', '', 'N', NULL, '5', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145904988165, 2, 2018858145875628033, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', '结束', NULL, '0.0', NULL, '1000,240|1000,240', NULL, NULL, NULL, 'N', NULL, '5', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858145904988166, 1, 2018858145875628033, '5ed2362b-fc0c-4d52-831f-95208b830605', '部门领导', NULL, '0.0', NULL, '720,160|720,160', NULL, '', '', 'N', NULL, '5', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858146341195777, 0, 2018858146316029954, 'ebebaf26-9cb6-497e-8119-4c9fed4c597c', '开始', NULL, '0.0', NULL, '300,220|300,220', NULL, NULL, NULL, 'N', NULL, '2', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858146341195778, 1, 2018858146316029954, 'e1b04e96-dc81-4858-a309-2fe945d2f374', '申请人', NULL, '0.0', NULL, '420,220|420,220', NULL, '', '', 'N', NULL, '2', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858146341195779, 4, 2018858146316029954, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', NULL, NULL, '0.0', NULL, '560,220', NULL, NULL, NULL, 'N', NULL, '2', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858146341195780, 1, 2018858146316029954, 'c80f273e-1f17-4bd8-9ad1-04a4a94ea862', '会签', NULL, '100.0', NULL, '700,320|700,320', NULL, '', '', 'N', NULL, '2', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,addSign,subSign\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858146345390082, 4, 2018858146316029954, '1a20169e-3d82-4926-a151-e2daad28de1b', NULL, NULL, '0.0', NULL, '860,220', NULL, NULL, NULL, 'N', NULL, '2', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858146345390083, 1, 2018858146316029954, '7a8f0473-e409-442e-a843-5c2b813d00e9', 'CEO', NULL, '0.0', NULL, '1000,220|1000,220', NULL, '', '', 'N', NULL, '2', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858146345390084, 2, 2018858146316029954, '03c4d2bc-58b5-4408-a2e4-65afb046f169', '结束', NULL, '0.0', NULL, '1140,220|1140,220', NULL, NULL, NULL, 'N', NULL, '2', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858146345390085, 1, 2018858146316029954, '1e3e8d3b-18ae-4d6c-a814-ce0d724adfa4', '百分之60票签', NULL, '60.0', NULL, '700,120|700,120', NULL, '', '', 'N', NULL, '2', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,addSign,subSign\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858146605436930, 0, 2018858146580271106, 'ebebaf26-9cb6-497e-8119-4c9fed4c597c', '开始', NULL, '0.0', NULL, '300,220|300,220', NULL, NULL, NULL, 'N', NULL, '3', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858146609631234, 1, 2018858146580271106, 'e1b04e96-dc81-4858-a309-2fe945d2f374', '申请人', NULL, '0.0', NULL, '420,220|420,220', NULL, '', '', 'N', NULL, '3', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858146609631235, 4, 2018858146580271106, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', NULL, NULL, '0.0', NULL, '560,220', NULL, NULL, NULL, 'N', NULL, '3', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858146609631236, 1, 2018858146580271106, 'c80f273e-1f17-4bd8-9ad1-04a4a94ea862', '会签', NULL, '100.0', NULL, '700,320|700,320', NULL, '', '', 'N', NULL, '3', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,addSign,subSign\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858146609631237, 4, 2018858146580271106, '1a20169e-3d82-4926-a151-e2daad28de1b', NULL, NULL, '0.0', NULL, '860,220', NULL, NULL, NULL, 'N', NULL, '3', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858146609631238, 1, 2018858146580271106, '7a8f0473-e409-442e-a843-5c2b813d00e9', 'CEO', NULL, '0.0', NULL, '1000,220|1000,220', NULL, '', '', 'N', NULL, '3', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858146609631239, 2, 2018858146580271106, '03c4d2bc-58b5-4408-a2e4-65afb046f169', '结束', NULL, '0.0', NULL, '1140,220|1140,220', NULL, NULL, NULL, 'N', NULL, '3', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '[]', '0', '154726', NULL);
INSERT INTO `flow_node` VALUES (2018858146609631240, 1, 2018858146580271106, '1e3e8d3b-18ae-4d6c-a814-ce0d724adfa4', '百分之60票签', NULL, '60.0', NULL, '700,120|700,120', NULL, '', '', 'N', NULL, '3', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,addSign,subSign\"}]', '0', '154726', NULL);

-- ----------------------------
-- Table structure for flow_skip
-- ----------------------------
DROP TABLE IF EXISTS `flow_skip`;
CREATE TABLE `flow_skip`  (
                              `id` bigint NOT NULL COMMENT '主键id',
                              `definition_id` bigint NOT NULL COMMENT '流程定义id',
                              `now_node_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '当前流程节点的编码',
                              `now_node_type` tinyint(1) NULL DEFAULT NULL COMMENT '当前节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
                              `next_node_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '下一个流程节点的编码',
                              `next_node_type` tinyint(1) NULL DEFAULT NULL COMMENT '下一个节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
                              `skip_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '跳转名称',
                              `skip_type` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '跳转类型（PASS审批通过 REJECT退回）',
                              `skip_condition` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '跳转条件',
                              `coordinate` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '坐标',
                              `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                              `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建人',
                              `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                              `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新人',
                              `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志',
                              `tenant_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '租户id',
                              PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '节点跳转关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of flow_skip
-- ----------------------------
INSERT INTO `flow_skip` VALUES (2008122525873680386, 2008122523722002434, 'd5ee3ddf-3968-4379-a86f-9ceabde5faac', 0, 'dd515cdd-59f6-446f-94ca-25ca062afb42', 1, NULL, 'PASS', NULL, '220,200;310,200', '2026-01-05 18:24:54', '1', '2026-01-05 18:24:54', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008122525873680387, 2008122523722002434, 'dd515cdd-59f6-446f-94ca-25ca062afb42', 1, '78fa8e5b-e809-44ed-978a-41092409ebcf', 1, NULL, 'PASS', NULL, '410,200;490,200', '2026-01-05 18:24:54', '1', '2026-01-05 18:24:54', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008122525873680388, 2008122523722002434, '78fa8e5b-e809-44ed-978a-41092409ebcf', 1, 'a8abf15f-b83e-428a-86cc-033555ea9bbe', 1, NULL, 'PASS', NULL, '590,200;670,200', '2026-01-05 18:24:54', '1', '2026-01-05 18:24:54', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008122525873680389, 2008122523722002434, 'a8abf15f-b83e-428a-86cc-033555ea9bbe', 1, '8b82b7d7-8660-455e-b880-d6d22ea3eb6d', 2, NULL, 'PASS', NULL, '770,200;880,200', '2026-01-05 18:24:54', '1', '2026-01-05 18:24:54', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008122795634536449, 2008122793122148354, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', 0, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, NULL, 'PASS', NULL, '320,240;390,240', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008122795634536450, 2008122793122148354, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, NULL, 'PASS', NULL, '490,240;535,240', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008122795634536451, 2008122793122148354, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, NULL, 'PASS', 'le@@leaveDays|2', '560,265;560,320;670,320', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008122795634536452, 2008122793122148354, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, '大于两天', 'PASS', 'gt@@leaveDays|2', '560,215;560,160;670,160|560,187', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008122795634536453, 2008122793122148354, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,320;860,320;860,280', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008122795634536454, 2008122793122148354, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', 2, NULL, 'PASS', NULL, '910,240;980,240', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008122795634536455, 2008122793122148354, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,160;860,160;860,200', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008125304423919617, 2008125302444208129, 'd5ee3ddf-3968-4379-a86f-9ceabde5faac', 0, 'dd515cdd-59f6-446f-94ca-25ca062afb42', 1, NULL, 'PASS', NULL, '220,200;310,200', '2026-01-05 18:35:56', '1', '2026-01-05 18:35:56', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008125304423919618, 2008125302444208129, 'dd515cdd-59f6-446f-94ca-25ca062afb42', 1, '78fa8e5b-e809-44ed-978a-41092409ebcf', 1, NULL, 'PASS', NULL, '410,200;490,200', '2026-01-05 18:35:56', '1', '2026-01-05 18:35:56', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008125304423919619, 2008125302444208129, '78fa8e5b-e809-44ed-978a-41092409ebcf', 1, 'a8abf15f-b83e-428a-86cc-033555ea9bbe', 1, NULL, 'PASS', NULL, '590,200;670,200', '2026-01-05 18:35:56', '1', '2026-01-05 18:35:56', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008125304423919620, 2008125302444208129, 'a8abf15f-b83e-428a-86cc-033555ea9bbe', 1, '8b82b7d7-8660-455e-b880-d6d22ea3eb6d', 2, NULL, 'PASS', NULL, '770,200;880,200', '2026-01-05 18:35:56', '1', '2026-01-05 18:35:56', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008125418534154241, 2008125415698804737, 'a80ecf9f-f465-4ae5-a429-e30ec5d0f957', 0, 'b7bbb571-06de-455c-8083-f83c07bf0b99', 1, NULL, 'PASS', NULL, '400,220;470,220', '2026-01-05 18:36:24', '1', '2026-01-05 18:36:24', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008125418534154242, 2008125415698804737, 'b7bbb571-06de-455c-8083-f83c07bf0b99', 1, '84d7ed24-bb44-4ba1-bf1f-e6f5092d3f0a', 4, NULL, 'PASS', NULL, '570,220;655,220', '2026-01-05 18:36:24', '1', '2026-01-05 18:36:24', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008125418534154243, 2008125415698804737, '84d7ed24-bb44-4ba1-bf1f-e6f5092d3f0a', 4, '4b7743cd-940c-431b-926f-e7b614fbf1fe', 1, NULL, 'PASS', NULL, '680,195;680,140;750,140', '2026-01-05 18:36:24', '1', '2026-01-05 18:36:24', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008125418534154244, 2008125415698804737, '84d7ed24-bb44-4ba1-bf1f-e6f5092d3f0a', 4, '762cb975-37d8-4276-b6db-79a4c3606394', 1, NULL, 'PASS', NULL, '680,245;680,300;750,300', '2026-01-05 18:36:24', '1', '2026-01-05 18:36:24', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008125418534154245, 2008125415698804737, '4b7743cd-940c-431b-926f-e7b614fbf1fe', 1, 'b66b6563-f9fe-41cc-a782-f7837bb6f3d2', 4, NULL, 'PASS', NULL, '850,140;920,140;920,195', '2026-01-05 18:36:24', '1', '2026-01-05 18:36:24', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008125418534154246, 2008125415698804737, 'b66b6563-f9fe-41cc-a782-f7837bb6f3d2', 4, '23e7429e-2b47-4431-b93e-40db7c431ce6', 1, NULL, 'PASS', NULL, '945,220;975,220;975,220;960,220;960,220;990,220', '2026-01-05 18:36:24', '1', '2026-01-05 18:36:24', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008125418534154247, 2008125415698804737, '23e7429e-2b47-4431-b93e-40db7c431ce6', 1, 'f5ace37f-5a5e-4e64-a6f6-913ab9a71cd1', 2, NULL, 'PASS', NULL, '1090,220;1140,220', '2026-01-05 18:36:24', '1', '2026-01-05 18:36:24', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008125418597068802, 2008125415698804737, '762cb975-37d8-4276-b6db-79a4c3606394', 1, 'b66b6563-f9fe-41cc-a782-f7837bb6f3d2', 4, NULL, 'PASS', NULL, '850,300;920,300;920,245', '2026-01-05 18:36:24', '1', '2026-01-05 18:36:24', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008125512977297410, 2008125510645264385, '9ce8bf00-f25b-4fc6-91b8-827082fc4876', 0, 'e90b98ef-35b4-410c-a663-bae8b7624b9f', 1, NULL, 'PASS', NULL, '340,240;410,240', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008125512977297411, 2008125510645264385, 'e90b98ef-35b4-410c-a663-bae8b7624b9f', 1, '768b5b1a-6726-4d67-8853-4cc70d5b1045', 1, NULL, 'PASS', NULL, '510,240;590,240', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008125512977297412, 2008125510645264385, '768b5b1a-6726-4d67-8853-4cc70d5b1045', 1, '2f9f2e21-9bcf-42a3-a07c-13037aad22d1', 1, NULL, 'PASS', NULL, '690,240;770,240', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008125512977297413, 2008125510645264385, '2f9f2e21-9bcf-42a3-a07c-13037aad22d1', 1, '27461e01-3d9f-4530-8fe3-bd5ec7f9571f', 1, NULL, 'PASS', NULL, '870,240;950,240', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008125512977297414, 2008125510645264385, '27461e01-3d9f-4530-8fe3-bd5ec7f9571f', 1, 'b62b88c3-8d8d-4969-911e-2aaea219e7fc', 2, NULL, 'PASS', NULL, '1050,240;1080,240;1080,240;1070,240;1070,240;1100,240', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008125556442869762, 2008125553481691138, 'ebebaf26-9cb6-497e-8119-4c9fed4c597c', 0, 'e1b04e96-dc81-4858-a309-2fe945d2f374', 1, NULL, 'PASS', NULL, '320,220;350,220;350,220;340,220;340,220;370,220', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008125556442869763, 2008125553481691138, 'e1b04e96-dc81-4858-a309-2fe945d2f374', 1, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', 4, NULL, 'PASS', NULL, '470,220;535,220', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008125556442869764, 2008125553481691138, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', 4, 'c80f273e-1f17-4bd8-9ad1-04a4a94ea862', 1, NULL, 'PASS', NULL, '560,245;560,320;650,320', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008125556442869765, 2008125553481691138, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', 4, '1e3e8d3b-18ae-4d6c-a814-ce0d724adfa4', 1, NULL, 'PASS', NULL, '560,195;560,120;650,120', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008125556442869766, 2008125553481691138, 'c80f273e-1f17-4bd8-9ad1-04a4a94ea862', 1, '1a20169e-3d82-4926-a151-e2daad28de1b', 4, NULL, 'PASS', NULL, '750,320;860,320;860,245', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008125556442869767, 2008125553481691138, '1a20169e-3d82-4926-a151-e2daad28de1b', 4, '7a8f0473-e409-442e-a843-5c2b813d00e9', 1, NULL, 'PASS', NULL, '885,220;950,220', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008125556442869768, 2008125553481691138, '7a8f0473-e409-442e-a843-5c2b813d00e9', 1, '03c4d2bc-58b5-4408-a2e4-65afb046f169', 2, NULL, 'PASS', NULL, '1050,220;1120,220', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008125556442869769, 2008125553481691138, '1e3e8d3b-18ae-4d6c-a814-ce0d724adfa4', 1, '1a20169e-3d82-4926-a151-e2daad28de1b', 4, NULL, 'PASS', NULL, '750,120;860,120;860,195', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008125584578260994, 2008125579910000641, '122b89a5-7c6f-40a3-aa09-7a263f902054', 0, 'c25a0e86-fdd1-4f03-8e22-14db70389dbd', 1, NULL, 'PASS', NULL, '260,300;350,300', '2026-01-05 18:37:03', '1', '2026-01-05 18:37:03', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008125584578260995, 2008125579910000641, 'c25a0e86-fdd1-4f03-8e22-14db70389dbd', 1, '07ecda1d-7a0a-47b5-8a91-6186c9473742', 1, NULL, 'PASS', NULL, '450,300;510,300', '2026-01-05 18:37:03', '1', '2026-01-05 18:37:03', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008125584578260996, 2008125579910000641, '2bfa3919-78cf-4bc1-b59b-df463a4546f9', 1, '394e1cc8-b8b2-4189-9f81-44448e88ac32', 3, NULL, 'PASS', NULL, '910,200;1000,200;1000,275', '2026-01-05 18:37:03', '1', '2026-01-05 18:37:03', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008125584578260997, 2008125579910000641, 'ec17f60e-94e0-4d96-a3ce-3417e9d32d60', 1, '394e1cc8-b8b2-4189-9f81-44448e88ac32', 3, NULL, 'PASS', NULL, '910,400;1000,400;1000,325', '2026-01-05 18:37:03', '1', '2026-01-05 18:37:03', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008125584578260998, 2008125579910000641, '07ecda1d-7a0a-47b5-8a91-6186c9473742', 1, '48117e2c-6328-406b-b102-c4a9d115bb13', 3, NULL, 'PASS', NULL, '610,300;675,300', '2026-01-05 18:37:03', '1', '2026-01-05 18:37:03', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008125584578260999, 2008125579910000641, '48117e2c-6328-406b-b102-c4a9d115bb13', 3, '2bfa3919-78cf-4bc1-b59b-df463a4546f9', 1, '大于两天', 'PASS', 'default@@${leaveDays > 2}', '700,275;700,200;810,200|700,237', '2026-01-05 18:37:03', '1', '2026-01-05 18:37:03', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008125584578261000, 2008125579910000641, '48117e2c-6328-406b-b102-c4a9d115bb13', 3, 'ec17f60e-94e0-4d96-a3ce-3417e9d32d60', 1, NULL, 'PASS', 'spel@@#{@testLeaveServiceImpl.eval(#leaveDays)}', '700,325;700,400;810,400', '2026-01-05 18:37:03', '1', '2026-01-05 18:37:03', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008125584578261001, 2008125579910000641, '394e1cc8-b8b2-4189-9f81-44448e88ac32', 3, '9c93a195-cff2-4e17-ab0a-a4f264191496', 1, NULL, 'PASS', NULL, '1025,300;1130,300', '2026-01-05 18:37:03', '1', '2026-01-05 18:37:03', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008125584578261002, 2008125579910000641, '9c93a195-cff2-4e17-ab0a-a4f264191496', 1, 'a1a42056-afd1-4e90-88bc-36cbf5a66992', 4, NULL, 'PASS', NULL, '1230,300;1315,300', '2026-01-05 18:37:03', '1', '2026-01-05 18:37:03', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008125584578261003, 2008125579910000641, 'a1a42056-afd1-4e90-88bc-36cbf5a66992', 4, 'fcfdd9f6-f526-4c1a-b71d-88afa31aebc5', 1, NULL, 'PASS', NULL, '1340,325;1340,400;1430,400', '2026-01-05 18:37:03', '1', '2026-01-05 18:37:03', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008125584578261004, 2008125579910000641, 'a1a42056-afd1-4e90-88bc-36cbf5a66992', 4, '350dfa0c-a77c-4efa-8527-10efa02d8be4', 1, NULL, 'PASS', NULL, '1340,275;1340,200;1430,200', '2026-01-05 18:37:03', '1', '2026-01-05 18:37:03', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008125584578261005, 2008125579910000641, '350dfa0c-a77c-4efa-8527-10efa02d8be4', 1, 'c36a46ef-04f9-463f-bad7-4b395c818519', 4, NULL, 'PASS', NULL, '1530,200;1640,200;1640,275', '2026-01-05 18:37:03', '1', '2026-01-05 18:37:03', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008125584578261006, 2008125579910000641, 'fcfdd9f6-f526-4c1a-b71d-88afa31aebc5', 1, 'c36a46ef-04f9-463f-bad7-4b395c818519', 4, NULL, 'PASS', NULL, '1530,400;1640,400;1640,325', '2026-01-05 18:37:03', '1', '2026-01-05 18:37:03', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008125584578261007, 2008125579910000641, 'c36a46ef-04f9-463f-bad7-4b395c818519', 4, '3fcea762-b53a-4ae1-8365-7bec90444828', 1, NULL, 'PASS', NULL, '1665,300;1770,300', '2026-01-05 18:37:03', '1', '2026-01-05 18:37:03', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008125584578261008, 2008125579910000641, '3fcea762-b53a-4ae1-8365-7bec90444828', 1, '9cfbfd3e-6c04-41d6-9fc2-6787a7d2cd31', 2, NULL, 'PASS', NULL, '1870,300;1960,300', '2026-01-05 18:37:03', '1', '2026-01-05 18:37:03', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008416817261973506, 2008416817060646913, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', 0, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, NULL, 'PASS', NULL, '320,240;390,240', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008416817261973507, 2008416817060646913, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, NULL, 'PASS', NULL, '490,240;535,240', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008416817261973508, 2008416817060646913, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, NULL, 'PASS', 'le@@leaveDays|2', '560,265;560,320;670,320', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008416817261973509, 2008416817060646913, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, '大于两天', 'PASS', 'gt@@leaveDays|2', '560,215;560,160;670,160|560,187', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008416817261973510, 2008416817060646913, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,320;860,320;860,280', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008416817261973511, 2008416817060646913, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', 2, NULL, 'PASS', NULL, '910,240;980,240', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2008416817261973512, 2008416817060646913, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,160;860,160;860,200', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2010596519380652033, 2010596518348853250, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', 0, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, NULL, 'PASS', NULL, '320,240;390,240', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2010596519380652034, 2010596518348853250, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, NULL, 'PASS', NULL, '490,240;535,240', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2010596519380652035, 2010596518348853250, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, NULL, 'PASS', 'le@@leaveDays|2', '560,265;560,320;670,320', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2010596519380652036, 2010596518348853250, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, '大于两天', 'PASS', 'gt@@leaveDays|2', '560,215;560,160;670,160|560,187', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2010596519380652037, 2010596518348853250, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,320;860,320;860,280', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2010596519380652038, 2010596518348853250, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', 2, NULL, 'PASS', NULL, '910,240;980,240', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2010596519380652039, 2010596518348853250, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,160;860,160;860,200', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2010596885182681089, 2010596885002326018, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', 0, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, NULL, 'PASS', NULL, '320,240;390,240', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2010596885182681090, 2010596885002326018, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, NULL, 'PASS', NULL, '490,240;535,240', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2010596885182681091, 2010596885002326018, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, NULL, 'PASS', 'le@@leaveDays|2', '560,265;560,320;670,320', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2010596885182681092, 2010596885002326018, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, '大于两天', 'PASS', 'gt@@leaveDays|2', '560,215;560,160;670,160|560,187', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2010596885182681093, 2010596885002326018, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,320;860,320;860,280', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2010596885182681094, 2010596885002326018, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', 2, NULL, 'PASS', NULL, '910,240;980,240', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2010596885186875394, 2010596885002326018, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,160;860,160;860,200', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2010596973976096769, 2010596973812518914, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', 0, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, NULL, 'PASS', NULL, '320,240;390,240', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2010596973976096770, 2010596973812518914, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, NULL, 'PASS', NULL, '490,240;535,240', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2010596973976096771, 2010596973812518914, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, NULL, 'PASS', 'le@@leaveDays|2', '560,265;560,320;670,320', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2010596973976096772, 2010596973812518914, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, '大于两天', 'PASS', 'gt@@leaveDays|2', '560,215;560,160;670,160|560,187', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2010596973976096773, 2010596973812518914, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,320;860,320;860,280', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2010596973976096774, 2010596973812518914, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', 2, NULL, 'PASS', NULL, '910,240;980,240', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2010596973976096775, 2010596973812518914, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,160;860,160;860,200', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2013552152895688705, 2013552152736305153, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', 0, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, NULL, 'PASS', NULL, '320,240;390,240', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552152895688706, 2013552152736305153, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, NULL, 'PASS', NULL, '490,240;535,240', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552152895688707, 2013552152736305153, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, NULL, 'PASS', 'le@@leaveDays|2', '560,265;560,320;670,320', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552152895688708, 2013552152736305153, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, '大于两天', 'PASS', 'gt@@leaveDays|2', '560,215;560,160;670,160|560,187', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552152899883009, 2013552152736305153, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,320;860,320;860,280', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552152899883010, 2013552152736305153, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', 2, NULL, 'PASS', NULL, '910,240;980,240', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552152899883011, 2013552152736305153, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,160;860,160;860,200', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153055072258, 2013552152971186178, 'd5ee3ddf-3968-4379-a86f-9ceabde5faac', 0, 'dd515cdd-59f6-446f-94ca-25ca062afb42', 1, NULL, 'PASS', NULL, '220,200;310,200', '2026-01-05 18:35:56', '1', '2026-01-05 18:35:56', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153055072259, 2013552152971186178, 'dd515cdd-59f6-446f-94ca-25ca062afb42', 1, '78fa8e5b-e809-44ed-978a-41092409ebcf', 1, NULL, 'PASS', NULL, '410,200;490,200', '2026-01-05 18:35:56', '1', '2026-01-05 18:35:56', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153055072260, 2013552152971186178, '78fa8e5b-e809-44ed-978a-41092409ebcf', 1, 'a8abf15f-b83e-428a-86cc-033555ea9bbe', 1, NULL, 'PASS', NULL, '590,200;670,200', '2026-01-05 18:35:56', '1', '2026-01-05 18:35:56', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153059266562, 2013552152971186178, 'a8abf15f-b83e-428a-86cc-033555ea9bbe', 1, '8b82b7d7-8660-455e-b880-d6d22ea3eb6d', 2, NULL, 'PASS', NULL, '770,200;880,200', '2026-01-05 18:35:56', '1', '2026-01-05 18:35:56', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153277370370, 2013552153080238082, 'a80ecf9f-f465-4ae5-a429-e30ec5d0f957', 0, 'b7bbb571-06de-455c-8083-f83c07bf0b99', 1, NULL, 'PASS', NULL, '400,220;470,220', '2026-01-05 18:36:24', '1', '2026-01-05 18:36:24', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153281564674, 2013552153080238082, 'b7bbb571-06de-455c-8083-f83c07bf0b99', 1, '84d7ed24-bb44-4ba1-bf1f-e6f5092d3f0a', 4, NULL, 'PASS', NULL, '570,220;655,220', '2026-01-05 18:36:24', '1', '2026-01-05 18:36:24', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153281564675, 2013552153080238082, '84d7ed24-bb44-4ba1-bf1f-e6f5092d3f0a', 4, '4b7743cd-940c-431b-926f-e7b614fbf1fe', 1, NULL, 'PASS', NULL, '680,195;680,140;750,140', '2026-01-05 18:36:24', '1', '2026-01-05 18:36:24', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153281564676, 2013552153080238082, '84d7ed24-bb44-4ba1-bf1f-e6f5092d3f0a', 4, '762cb975-37d8-4276-b6db-79a4c3606394', 1, NULL, 'PASS', NULL, '680,245;680,300;750,300', '2026-01-05 18:36:24', '1', '2026-01-05 18:36:24', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153281564677, 2013552153080238082, '4b7743cd-940c-431b-926f-e7b614fbf1fe', 1, 'b66b6563-f9fe-41cc-a782-f7837bb6f3d2', 4, NULL, 'PASS', NULL, '850,140;920,140;920,195', '2026-01-05 18:36:24', '1', '2026-01-05 18:36:24', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153281564678, 2013552153080238082, 'b66b6563-f9fe-41cc-a782-f7837bb6f3d2', 4, '23e7429e-2b47-4431-b93e-40db7c431ce6', 1, NULL, 'PASS', NULL, '945,220;975,220;975,220;960,220;960,220;990,220', '2026-01-05 18:36:24', '1', '2026-01-05 18:36:24', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153281564679, 2013552153080238082, '23e7429e-2b47-4431-b93e-40db7c431ce6', 1, 'f5ace37f-5a5e-4e64-a6f6-913ab9a71cd1', 2, NULL, 'PASS', NULL, '1090,220;1140,220', '2026-01-05 18:36:24', '1', '2026-01-05 18:36:24', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153281564680, 2013552153080238082, '762cb975-37d8-4276-b6db-79a4c3606394', 1, 'b66b6563-f9fe-41cc-a782-f7837bb6f3d2', 4, NULL, 'PASS', NULL, '850,300;920,300;920,245', '2026-01-05 18:36:24', '1', '2026-01-05 18:36:24', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153390616577, 2013552153306730497, '9ce8bf00-f25b-4fc6-91b8-827082fc4876', 0, 'e90b98ef-35b4-410c-a663-bae8b7624b9f', 1, NULL, 'PASS', NULL, '340,240;410,240', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153390616578, 2013552153306730497, 'e90b98ef-35b4-410c-a663-bae8b7624b9f', 1, '768b5b1a-6726-4d67-8853-4cc70d5b1045', 1, NULL, 'PASS', NULL, '510,240;590,240', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153394810882, 2013552153306730497, '768b5b1a-6726-4d67-8853-4cc70d5b1045', 1, '2f9f2e21-9bcf-42a3-a07c-13037aad22d1', 1, NULL, 'PASS', NULL, '690,240;770,240', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153394810883, 2013552153306730497, '2f9f2e21-9bcf-42a3-a07c-13037aad22d1', 1, '27461e01-3d9f-4530-8fe3-bd5ec7f9571f', 1, NULL, 'PASS', NULL, '870,240;950,240', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153394810884, 2013552153306730497, '27461e01-3d9f-4530-8fe3-bd5ec7f9571f', 1, 'b62b88c3-8d8d-4969-911e-2aaea219e7fc', 2, NULL, 'PASS', NULL, '1050,240;1080,240;1080,240;1070,240;1070,240;1100,240', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153587748865, 2013552153411588098, 'ebebaf26-9cb6-497e-8119-4c9fed4c597c', 0, 'e1b04e96-dc81-4858-a309-2fe945d2f374', 1, NULL, 'PASS', NULL, '320,220;350,220;350,220;340,220;340,220;370,220', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153591943169, 2013552153411588098, 'e1b04e96-dc81-4858-a309-2fe945d2f374', 1, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', 4, NULL, 'PASS', NULL, '470,220;535,220', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153591943170, 2013552153411588098, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', 4, 'c80f273e-1f17-4bd8-9ad1-04a4a94ea862', 1, NULL, 'PASS', NULL, '560,245;560,320;650,320', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153591943171, 2013552153411588098, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', 4, '1e3e8d3b-18ae-4d6c-a814-ce0d724adfa4', 1, NULL, 'PASS', NULL, '560,195;560,120;650,120', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153591943172, 2013552153411588098, 'c80f273e-1f17-4bd8-9ad1-04a4a94ea862', 1, '1a20169e-3d82-4926-a151-e2daad28de1b', 4, NULL, 'PASS', NULL, '750,320;860,320;860,245', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153591943173, 2013552153411588098, '1a20169e-3d82-4926-a151-e2daad28de1b', 4, '7a8f0473-e409-442e-a843-5c2b813d00e9', 1, NULL, 'PASS', NULL, '885,220;950,220', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153591943174, 2013552153411588098, '7a8f0473-e409-442e-a843-5c2b813d00e9', 1, '03c4d2bc-58b5-4408-a2e4-65afb046f169', 2, NULL, 'PASS', NULL, '1050,220;1120,220', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153591943175, 2013552153411588098, '1e3e8d3b-18ae-4d6c-a814-ce0d724adfa4', 1, '1a20169e-3d82-4926-a151-e2daad28de1b', 4, NULL, 'PASS', NULL, '750,120;860,120;860,195', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153755521025, 2013552153612914690, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', 0, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, NULL, 'PASS', NULL, '320,240;390,240', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153755521026, 2013552153612914690, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, NULL, 'PASS', NULL, '490,240;535,240', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153755521027, 2013552153612914690, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, NULL, 'PASS', 'le@@leaveDays|2', '560,265;560,320;670,320', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153755521028, 2013552153612914690, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, '大于两天', 'PASS', 'gt@@leaveDays|2', '560,215;560,160;670,160|560,187', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153755521029, 2013552153612914690, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,320;860,320;860,280', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153759715329, 2013552153612914690, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', 2, NULL, 'PASS', NULL, '910,240;980,240', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552153759715330, 2013552153612914690, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,160;860,160;860,200', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552154019762178, 2013552153868767234, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', 0, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, NULL, 'PASS', NULL, '320,240;390,240', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552154019762179, 2013552153868767234, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, NULL, 'PASS', NULL, '490,240;535,240', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552154023956481, 2013552153868767234, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, NULL, 'PASS', 'le@@leaveDays|2', '560,265;560,320;670,320', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552154023956482, 2013552153868767234, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, '大于两天', 'PASS', 'gt@@leaveDays|2', '560,215;560,160;670,160|560,187', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552154023956483, 2013552153868767234, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,320;860,320;860,280', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552154023956484, 2013552153868767234, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', 2, NULL, 'PASS', NULL, '910,240;980,240', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552154023956485, 2013552153868767234, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,160;860,160;860,200', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552154233671681, 2013552154099453953, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', 0, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, NULL, 'PASS', NULL, '320,240;390,240', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552154233671682, 2013552154099453953, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, NULL, 'PASS', NULL, '490,240;535,240', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552154237865985, 2013552154099453953, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, NULL, 'PASS', 'le@@leaveDays|2', '560,265;560,320;670,320', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552154237865986, 2013552154099453953, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, '大于两天', 'PASS', 'gt@@leaveDays|2', '560,215;560,160;670,160|560,187', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552154242060290, 2013552154099453953, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,320;860,320;860,280', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552154242060291, 2013552154099453953, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', 2, NULL, 'PASS', NULL, '910,240;980,240', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552154242060292, 2013552154099453953, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,160;860,160;860,200', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552154460164097, 2013552154317557762, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', 0, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, NULL, 'PASS', NULL, '320,240;390,240', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552154464358401, 2013552154317557762, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, NULL, 'PASS', NULL, '490,240;535,240', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552154464358402, 2013552154317557762, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, NULL, 'PASS', 'le@@leaveDays|2', '560,265;560,320;670,320', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552154464358403, 2013552154317557762, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, '大于两天', 'PASS', 'gt@@leaveDays|2', '560,215;560,160;670,160|560,187', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552154464358404, 2013552154317557762, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,320;860,320;860,280', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552154468552706, 2013552154317557762, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', 2, NULL, 'PASS', NULL, '910,240;980,240', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2013552154468552707, 2013552154317557762, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,160;860,160;860,200', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '0', '566749');
INSERT INTO `flow_skip` VALUES (2016028336699084801, 2016028336518729730, 'ebebaf26-9cb6-497e-8119-4c9fed4c597c', 0, 'e1b04e96-dc81-4858-a309-2fe945d2f374', 1, NULL, 'PASS', NULL, '320,220;350,220;350,220;340,220;340,220;370,220', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2016028336699084802, 2016028336518729730, 'e1b04e96-dc81-4858-a309-2fe945d2f374', 1, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', 4, NULL, 'PASS', NULL, '470,220;535,220', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2016028336699084803, 2016028336518729730, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', 4, 'c80f273e-1f17-4bd8-9ad1-04a4a94ea862', 1, NULL, 'PASS', NULL, '560,245;560,320;650,320', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2016028336699084804, 2016028336518729730, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', 4, '1e3e8d3b-18ae-4d6c-a814-ce0d724adfa4', 1, NULL, 'PASS', NULL, '560,195;560,120;650,120', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2016028336699084805, 2016028336518729730, 'c80f273e-1f17-4bd8-9ad1-04a4a94ea862', 1, '1a20169e-3d82-4926-a151-e2daad28de1b', 4, NULL, 'PASS', NULL, '750,320;860,320;860,245', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2016028336699084806, 2016028336518729730, '1a20169e-3d82-4926-a151-e2daad28de1b', 4, '7a8f0473-e409-442e-a843-5c2b813d00e9', 1, NULL, 'PASS', NULL, '885,220;950,220', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2016028336699084807, 2016028336518729730, '7a8f0473-e409-442e-a843-5c2b813d00e9', 1, '03c4d2bc-58b5-4408-a2e4-65afb046f169', 2, NULL, 'PASS', NULL, '1050,220;1120,220', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2016028336699084808, 2016028336518729730, '1e3e8d3b-18ae-4d6c-a814-ce0d724adfa4', 1, '1a20169e-3d82-4926-a151-e2daad28de1b', 4, NULL, 'PASS', NULL, '750,120;860,120;860,195', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2018216957275410433, 2018216957082472449, 'ebebaf26-9cb6-497e-8119-4c9fed4c597c', 0, 'e1b04e96-dc81-4858-a309-2fe945d2f374', 1, NULL, 'PASS', NULL, '320,220;350,220;350,220;340,220;340,220;370,220', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2018216957275410434, 2018216957082472449, 'e1b04e96-dc81-4858-a309-2fe945d2f374', 1, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', 4, NULL, 'PASS', NULL, '470,220;535,220', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2018216957275410435, 2018216957082472449, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', 4, 'c80f273e-1f17-4bd8-9ad1-04a4a94ea862', 1, NULL, 'PASS', NULL, '560,245;560,320;650,320', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2018216957275410436, 2018216957082472449, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', 4, '1e3e8d3b-18ae-4d6c-a814-ce0d724adfa4', 1, NULL, 'PASS', NULL, '560,195;560,120;650,120', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2018216957275410437, 2018216957082472449, 'c80f273e-1f17-4bd8-9ad1-04a4a94ea862', 1, '1a20169e-3d82-4926-a151-e2daad28de1b', 4, NULL, 'PASS', NULL, '750,320;860,320;860,245', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2018216957275410438, 2018216957082472449, '1a20169e-3d82-4926-a151-e2daad28de1b', 4, '7a8f0473-e409-442e-a843-5c2b813d00e9', 1, NULL, 'PASS', NULL, '885,220;950,220', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2018216957275410439, 2018216957082472449, '7a8f0473-e409-442e-a843-5c2b813d00e9', 1, '03c4d2bc-58b5-4408-a2e4-65afb046f169', 2, NULL, 'PASS', NULL, '1050,220;1120,220', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2018216957275410440, 2018216957082472449, '1e3e8d3b-18ae-4d6c-a814-ce0d724adfa4', 1, '1a20169e-3d82-4926-a151-e2daad28de1b', 4, NULL, 'PASS', NULL, '750,120;860,120;860,195', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '0', '000000');
INSERT INTO `flow_skip` VALUES (2018858144147574786, 2018858143975608322, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', 0, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, NULL, 'PASS', NULL, '320,240;390,240', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144147574787, 2018858143975608322, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, NULL, 'PASS', NULL, '490,240;535,240', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144147574788, 2018858143975608322, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, NULL, 'PASS', 'le@@leaveDays|2', '560,265;560,320;670,320', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144147574789, 2018858143975608322, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, '大于两天', 'PASS', 'gt@@leaveDays|2', '560,215;560,160;670,160|560,187', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144147574790, 2018858143975608322, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,320;860,320;860,280', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144151769090, 2018858143975608322, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', 2, NULL, 'PASS', NULL, '910,240;980,240', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144151769091, 2018858143975608322, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,160;860,160;860,200', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144344707074, 2018858144244043777, 'd5ee3ddf-3968-4379-a86f-9ceabde5faac', 0, 'dd515cdd-59f6-446f-94ca-25ca062afb42', 1, NULL, 'PASS', NULL, '220,200;310,200', '2026-01-05 18:35:56', '1', '2026-01-05 18:35:56', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144344707075, 2018858144244043777, 'dd515cdd-59f6-446f-94ca-25ca062afb42', 1, '78fa8e5b-e809-44ed-978a-41092409ebcf', 1, NULL, 'PASS', NULL, '410,200;490,200', '2026-01-05 18:35:56', '1', '2026-01-05 18:35:56', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144344707076, 2018858144244043777, '78fa8e5b-e809-44ed-978a-41092409ebcf', 1, 'a8abf15f-b83e-428a-86cc-033555ea9bbe', 1, NULL, 'PASS', NULL, '590,200;670,200', '2026-01-05 18:35:56', '1', '2026-01-05 18:35:56', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144344707077, 2018858144244043777, 'a8abf15f-b83e-428a-86cc-033555ea9bbe', 1, '8b82b7d7-8660-455e-b880-d6d22ea3eb6d', 2, NULL, 'PASS', NULL, '770,200;880,200', '2026-01-05 18:35:56', '1', '2026-01-05 18:35:56', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144587976706, 2018858144369872897, 'a80ecf9f-f465-4ae5-a429-e30ec5d0f957', 0, 'b7bbb571-06de-455c-8083-f83c07bf0b99', 1, NULL, 'PASS', NULL, '400,220;470,220', '2026-01-05 18:36:24', '1', '2026-01-05 18:36:24', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144587976707, 2018858144369872897, 'b7bbb571-06de-455c-8083-f83c07bf0b99', 1, '84d7ed24-bb44-4ba1-bf1f-e6f5092d3f0a', 4, NULL, 'PASS', NULL, '570,220;655,220', '2026-01-05 18:36:24', '1', '2026-01-05 18:36:24', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144587976708, 2018858144369872897, '84d7ed24-bb44-4ba1-bf1f-e6f5092d3f0a', 4, '4b7743cd-940c-431b-926f-e7b614fbf1fe', 1, NULL, 'PASS', NULL, '680,195;680,140;750,140', '2026-01-05 18:36:24', '1', '2026-01-05 18:36:24', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144587976709, 2018858144369872897, '84d7ed24-bb44-4ba1-bf1f-e6f5092d3f0a', 4, '762cb975-37d8-4276-b6db-79a4c3606394', 1, NULL, 'PASS', NULL, '680,245;680,300;750,300', '2026-01-05 18:36:24', '1', '2026-01-05 18:36:24', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144592171010, 2018858144369872897, '4b7743cd-940c-431b-926f-e7b614fbf1fe', 1, 'b66b6563-f9fe-41cc-a782-f7837bb6f3d2', 4, NULL, 'PASS', NULL, '850,140;920,140;920,195', '2026-01-05 18:36:24', '1', '2026-01-05 18:36:24', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144592171011, 2018858144369872897, 'b66b6563-f9fe-41cc-a782-f7837bb6f3d2', 4, '23e7429e-2b47-4431-b93e-40db7c431ce6', 1, NULL, 'PASS', NULL, '945,220;975,220;975,220;960,220;960,220;990,220', '2026-01-05 18:36:24', '1', '2026-01-05 18:36:24', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144592171012, 2018858144369872897, '23e7429e-2b47-4431-b93e-40db7c431ce6', 1, 'f5ace37f-5a5e-4e64-a6f6-913ab9a71cd1', 2, NULL, 'PASS', NULL, '1090,220;1140,220', '2026-01-05 18:36:24', '1', '2026-01-05 18:36:24', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144592171013, 2018858144369872897, '762cb975-37d8-4276-b6db-79a4c3606394', 1, 'b66b6563-f9fe-41cc-a782-f7837bb6f3d2', 4, NULL, 'PASS', NULL, '850,300;920,300;920,245', '2026-01-05 18:36:24', '1', '2026-01-05 18:36:24', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144718000129, 2018858144617336833, '9ce8bf00-f25b-4fc6-91b8-827082fc4876', 0, 'e90b98ef-35b4-410c-a663-bae8b7624b9f', 1, NULL, 'PASS', NULL, '340,240;410,240', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144718000130, 2018858144617336833, 'e90b98ef-35b4-410c-a663-bae8b7624b9f', 1, '768b5b1a-6726-4d67-8853-4cc70d5b1045', 1, NULL, 'PASS', NULL, '510,240;590,240', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144718000131, 2018858144617336833, '768b5b1a-6726-4d67-8853-4cc70d5b1045', 1, '2f9f2e21-9bcf-42a3-a07c-13037aad22d1', 1, NULL, 'PASS', NULL, '690,240;770,240', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144718000132, 2018858144617336833, '2f9f2e21-9bcf-42a3-a07c-13037aad22d1', 1, '27461e01-3d9f-4530-8fe3-bd5ec7f9571f', 1, NULL, 'PASS', NULL, '870,240;950,240', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144718000133, 2018858144617336833, '27461e01-3d9f-4530-8fe3-bd5ec7f9571f', 1, 'b62b88c3-8d8d-4969-911e-2aaea219e7fc', 2, NULL, 'PASS', NULL, '1050,240;1080,240;1080,240;1070,240;1070,240;1100,240', '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144957075457, 2018858144743165953, 'ebebaf26-9cb6-497e-8119-4c9fed4c597c', 0, 'e1b04e96-dc81-4858-a309-2fe945d2f374', 1, NULL, 'PASS', NULL, '320,220;350,220;350,220;340,220;340,220;370,220', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144957075458, 2018858144743165953, 'e1b04e96-dc81-4858-a309-2fe945d2f374', 1, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', 4, NULL, 'PASS', NULL, '470,220;535,220', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144957075459, 2018858144743165953, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', 4, 'c80f273e-1f17-4bd8-9ad1-04a4a94ea862', 1, NULL, 'PASS', NULL, '560,245;560,320;650,320', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144957075460, 2018858144743165953, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', 4, '1e3e8d3b-18ae-4d6c-a814-ce0d724adfa4', 1, NULL, 'PASS', NULL, '560,195;560,120;650,120', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144957075461, 2018858144743165953, 'c80f273e-1f17-4bd8-9ad1-04a4a94ea862', 1, '1a20169e-3d82-4926-a151-e2daad28de1b', 4, NULL, 'PASS', NULL, '750,320;860,320;860,245', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144957075462, 2018858144743165953, '1a20169e-3d82-4926-a151-e2daad28de1b', 4, '7a8f0473-e409-442e-a843-5c2b813d00e9', 1, NULL, 'PASS', NULL, '885,220;950,220', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144957075463, 2018858144743165953, '7a8f0473-e409-442e-a843-5c2b813d00e9', 1, '03c4d2bc-58b5-4408-a2e4-65afb046f169', 2, NULL, 'PASS', NULL, '1050,220;1120,220', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858144957075464, 2018858144743165953, '1e3e8d3b-18ae-4d6c-a814-ce0d724adfa4', 1, '1a20169e-3d82-4926-a151-e2daad28de1b', 4, NULL, 'PASS', NULL, '750,120;860,120;860,195', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858145170984961, 2018858145003212802, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', 0, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, NULL, 'PASS', NULL, '320,240;390,240', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858145170984962, 2018858145003212802, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, NULL, 'PASS', NULL, '490,240;535,240', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858145170984963, 2018858145003212802, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, NULL, 'PASS', 'le@@leaveDays|2', '560,265;560,320;670,320', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858145170984964, 2018858145003212802, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, '大于两天', 'PASS', 'gt@@leaveDays|2', '560,215;560,160;670,160|560,187', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858145175179265, 2018858145003212802, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,320;860,320;860,280', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858145175179266, 2018858145003212802, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', 2, NULL, 'PASS', NULL, '910,240;980,240', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858145175179267, 2018858145003212802, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,160;860,160;860,200', '2026-01-06 13:54:18', '1', '2026-01-06 13:54:18', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858145481363458, 2018858145313591297, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', 0, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, NULL, 'PASS', NULL, '320,240;390,240', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858145481363459, 2018858145313591297, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, NULL, 'PASS', NULL, '490,240;535,240', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858145481363460, 2018858145313591297, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, NULL, 'PASS', 'le@@leaveDays|2', '560,265;560,320;670,320', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858145485557761, 2018858145313591297, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, '大于两天', 'PASS', 'gt@@leaveDays|2', '560,215;560,160;670,160|560,187', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858145485557762, 2018858145313591297, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,320;860,320;860,280', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858145485557763, 2018858145313591297, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', 2, NULL, 'PASS', NULL, '910,240;980,240', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858145485557764, 2018858145313591297, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,160;860,160;860,200', '2026-01-12 14:15:40', '1', '2026-01-12 14:15:40', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858145779159041, 2018858145577832449, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', 0, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, NULL, 'PASS', NULL, '320,240;390,240', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858145779159042, 2018858145577832449, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, NULL, 'PASS', NULL, '490,240;535,240', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858145779159043, 2018858145577832449, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, NULL, 'PASS', 'le@@leaveDays|2', '560,265;560,320;670,320', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858145783353346, 2018858145577832449, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, '大于两天', 'PASS', 'gt@@leaveDays|2', '560,215;560,160;670,160|560,187', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858145783353347, 2018858145577832449, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,320;860,320;860,280', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858145783353348, 2018858145577832449, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', 2, NULL, 'PASS', NULL, '910,240;980,240', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858145783353349, 2018858145577832449, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,160;860,160;860,200', '2026-01-12 14:17:07', '1', '2026-01-12 14:17:07', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858146047594498, 2018858145875628033, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', 0, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, NULL, 'PASS', NULL, '320,240;390,240', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858146047594499, 2018858145875628033, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, NULL, 'PASS', NULL, '490,240;535,240', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858146047594500, 2018858145875628033, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, NULL, 'PASS', 'le@@leaveDays|2', '560,265;560,320;670,320', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858146047594501, 2018858145875628033, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, '大于两天', 'PASS', 'gt@@leaveDays|2', '560,215;560,160;670,160|560,187', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858146051788802, 2018858145875628033, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,320;860,320;860,280', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858146051788803, 2018858145875628033, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', 2, NULL, 'PASS', NULL, '910,240;980,240', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858146051788804, 2018858145875628033, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,160;860,160;860,200', '2026-01-12 14:17:28', '1', '2026-01-12 14:17:28', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858146529939457, 2018858146316029954, 'ebebaf26-9cb6-497e-8119-4c9fed4c597c', 0, 'e1b04e96-dc81-4858-a309-2fe945d2f374', 1, NULL, 'PASS', NULL, '320,220;350,220;350,220;340,220;340,220;370,220', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858146529939458, 2018858146316029954, 'e1b04e96-dc81-4858-a309-2fe945d2f374', 1, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', 4, NULL, 'PASS', NULL, '470,220;535,220', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858146529939459, 2018858146316029954, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', 4, 'c80f273e-1f17-4bd8-9ad1-04a4a94ea862', 1, NULL, 'PASS', NULL, '560,245;560,320;650,320', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858146534133762, 2018858146316029954, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', 4, '1e3e8d3b-18ae-4d6c-a814-ce0d724adfa4', 1, NULL, 'PASS', NULL, '560,195;560,120;650,120', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858146534133763, 2018858146316029954, 'c80f273e-1f17-4bd8-9ad1-04a4a94ea862', 1, '1a20169e-3d82-4926-a151-e2daad28de1b', 4, NULL, 'PASS', NULL, '750,320;860,320;860,245', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858146534133764, 2018858146316029954, '1a20169e-3d82-4926-a151-e2daad28de1b', 4, '7a8f0473-e409-442e-a843-5c2b813d00e9', 1, NULL, 'PASS', NULL, '885,220;950,220', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858146534133765, 2018858146316029954, '7a8f0473-e409-442e-a843-5c2b813d00e9', 1, '03c4d2bc-58b5-4408-a2e4-65afb046f169', 2, NULL, 'PASS', NULL, '1050,220;1120,220', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858146534133766, 2018858146316029954, '1e3e8d3b-18ae-4d6c-a814-ce0d724adfa4', 1, '1a20169e-3d82-4926-a151-e2daad28de1b', 4, NULL, 'PASS', NULL, '750,120;860,120;860,195', '2026-01-27 13:59:46', '1', '2026-01-27 13:59:46', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858146794180609, 2018858146580271106, 'ebebaf26-9cb6-497e-8119-4c9fed4c597c', 0, 'e1b04e96-dc81-4858-a309-2fe945d2f374', 1, NULL, 'PASS', NULL, '320,220;350,220;350,220;340,220;340,220;370,220', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858146794180610, 2018858146580271106, 'e1b04e96-dc81-4858-a309-2fe945d2f374', 1, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', 4, NULL, 'PASS', NULL, '470,220;535,220', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858146794180611, 2018858146580271106, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', 4, 'c80f273e-1f17-4bd8-9ad1-04a4a94ea862', 1, NULL, 'PASS', NULL, '560,245;560,320;650,320', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858146794180612, 2018858146580271106, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', 4, '1e3e8d3b-18ae-4d6c-a814-ce0d724adfa4', 1, NULL, 'PASS', NULL, '560,195;560,120;650,120', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858146802569217, 2018858146580271106, 'c80f273e-1f17-4bd8-9ad1-04a4a94ea862', 1, '1a20169e-3d82-4926-a151-e2daad28de1b', 4, NULL, 'PASS', NULL, '750,320;860,320;860,245', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858146802569218, 2018858146580271106, '1a20169e-3d82-4926-a151-e2daad28de1b', 4, '7a8f0473-e409-442e-a843-5c2b813d00e9', 1, NULL, 'PASS', NULL, '885,220;950,220', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858146802569219, 2018858146580271106, '7a8f0473-e409-442e-a843-5c2b813d00e9', 1, '03c4d2bc-58b5-4408-a2e4-65afb046f169', 2, NULL, 'PASS', NULL, '1050,220;1120,220', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '0', '154726');
INSERT INTO `flow_skip` VALUES (2018858146802569220, 2018858146580271106, '1e3e8d3b-18ae-4d6c-a814-ce0d724adfa4', 1, '1a20169e-3d82-4926-a151-e2daad28de1b', 4, NULL, 'PASS', NULL, '750,120;860,120;860,195', '2026-02-02 14:56:34', '1', '2026-02-02 14:56:34', '1', '0', '154726');

-- ----------------------------
-- Table structure for flow_spel
-- ----------------------------
DROP TABLE IF EXISTS `flow_spel`;
CREATE TABLE `flow_spel`  (
                              `id` bigint NOT NULL COMMENT '主键id',
                              `component_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '组件名称',
                              `method_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '方法名',
                              `method_params` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '参数',
                              `view_spel` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预览spel表达式',
                              `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
                              `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
                              `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志',
                              `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                              `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                              `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                              `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                              `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                              PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流程spel表达式定义表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of flow_spel
-- ----------------------------
INSERT INTO `flow_spel` VALUES (1, 'spelRuleComponent', 'selectDeptLeaderById', 'initiatorDeptId', '#{@spelRuleComponent.selectDeptLeaderById(#initiatorDeptId)}', '根据部门id获取部门负责人', '0', '0', 103, 1, '2026-01-05 14:39:33', 1, '2026-01-05 14:39:33');
INSERT INTO `flow_spel` VALUES (2, NULL, NULL, 'initiator', '${initiator}', '流程发起人', '0', '0', 103, 1, '2026-01-05 14:39:33', 1, '2026-01-05 14:39:33');
INSERT INTO `flow_spel` VALUES (2010657778968956929, 'we', 'we', 'we', '#{@we.we(#we)}', NULL, '0', '0', 103, 1, '2026-01-12 18:19:05', 1, '2026-01-12 18:19:05');
INSERT INTO `flow_spel` VALUES (2012219843198193666, '测试', '测试', '测试', '#{@测试.测试(#测试)}', '测设', '0', '0', 103, 1, '2026-01-17 01:46:11', 1, '2026-01-17 01:46:11');

-- ----------------------------
-- Table structure for flow_task
-- ----------------------------
DROP TABLE IF EXISTS `flow_task`;
CREATE TABLE `flow_task`  (
                              `id` bigint NOT NULL COMMENT '主键id',
                              `definition_id` bigint NOT NULL COMMENT '对应flow_definition表的id',
                              `instance_id` bigint NOT NULL COMMENT '对应flow_instance表的id',
                              `node_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '节点编码',
                              `node_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '节点名称',
                              `node_type` tinyint(1) NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
                              `flow_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
                              `form_custom` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
                              `form_path` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审批表单路径',
                              `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                              `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建人',
                              `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                              `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新人',
                              `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志',
                              `tenant_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '租户id',
                              PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '待办任务表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of flow_task
-- ----------------------------
INSERT INTO `flow_task` VALUES (2016396747937550339, 2008122793122148354, 2016396747920773122, 'fdcae93b-b69c-498a-b231-09255e74bcbd', '申请人', 1, 'draft', 'N', '/workflow/leaveEdit/index', '2026-01-28 14:23:42', '1', '2026-01-28 14:23:51', '1', '1', '000000');
INSERT INTO `flow_task` VALUES (2016396783471693825, 2008122793122148354, 2016396747920773122, '5ed2362b-fc0c-4d52-831f-95208b830605', '部门领导', 1, 'waiting', 'N', '/workflow/leaveEdit/index', '2026-01-28 14:23:51', '1', '2026-01-28 14:23:51', '1', '1', '000000');

-- ----------------------------
-- Table structure for flow_user
-- ----------------------------
DROP TABLE IF EXISTS `flow_user`;
CREATE TABLE `flow_user`  (
                              `id` bigint NOT NULL COMMENT '主键id',
                              `type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '人员类型（1待办任务的审批人权限 2待办任务的转办人权限 3待办任务的委托人权限）',
                              `processed_by` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '权限人',
                              `associated` bigint NOT NULL COMMENT '任务表id',
                              `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                              `create_by` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
                              `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                              `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建人',
                              `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志',
                              `tenant_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '租户id',
                              PRIMARY KEY (`id`) USING BTREE,
                              INDEX `user_processed_type`(`processed_by` ASC, `type` ASC) USING BTREE,
                              INDEX `user_associated`(`associated` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '流程用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of flow_user
-- ----------------------------
INSERT INTO `flow_user` VALUES (2016396748927406082, '1', '1', 2016396747937550339, '2026-01-28 14:23:43', '1', '2026-01-28 14:23:43', '1', '1', '000000');
INSERT INTO `flow_user` VALUES (2016396783605911553, '1', '1', 2016396783471693825, '2026-01-28 14:23:51', '1', '2026-01-28 14:23:51', '1', '1', '000000');

-- ----------------------------
-- Table structure for gen_table
-- ----------------------------
DROP TABLE IF EXISTS `gen_table`;
CREATE TABLE `gen_table`  (
                              `table_id` bigint NOT NULL COMMENT '编号',
                              `data_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '数据源名称',
                              `table_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '表名称',
                              `table_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '表描述',
                              `sub_table_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联子表的表名',
                              `sub_table_fk_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '子表关联的外键名',
                              `class_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '实体类名称',
                              `tpl_category` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作）',
                              `package_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生成包路径',
                              `module_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生成模块名',
                              `business_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生成业务名',
                              `function_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生成功能名',
                              `function_author` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生成功能作者',
                              `gen_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
                              `gen_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
                              `options` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '其它生成选项',
                              `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                              `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                              `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                              `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                              `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                              `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
                              PRIMARY KEY (`table_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '代码生成业务表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of gen_table
-- ----------------------------
INSERT INTO `gen_table` VALUES (2018961776515878913, 'master', 'sys_user', '用户信息表', NULL, NULL, 'SysUser', 'crud', 'org.ruoyi.system', 'system', 'user', '用户信息', 'ageerle', '0', '/', NULL, 103, 1, '2026-02-04 16:16:13', 1, '2026-02-04 16:16:13', NULL);

-- ----------------------------
-- Table structure for gen_table_column
-- ----------------------------
DROP TABLE IF EXISTS `gen_table_column`;
CREATE TABLE `gen_table_column`  (
                                     `column_id` bigint NOT NULL COMMENT '编号',
                                     `table_id` bigint NULL DEFAULT NULL COMMENT '归属表编号',
                                     `column_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '列名称',
                                     `column_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '列描述',
                                     `column_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '列类型',
                                     `java_type` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'JAVA类型',
                                     `java_field` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'JAVA字段名',
                                     `is_pk` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否主键（1是）',
                                     `is_increment` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否自增（1是）',
                                     `is_required` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否必填（1是）',
                                     `is_insert` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否为插入字段（1是）',
                                     `is_edit` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否编辑字段（1是）',
                                     `is_list` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否列表字段（1是）',
                                     `is_query` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否查询字段（1是）',
                                     `query_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'EQ' COMMENT '查询方式（等于、不等于、大于、小于、范围）',
                                     `html_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
                                     `dict_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '字典类型',
                                     `sort` int NULL DEFAULT NULL COMMENT '排序',
                                     `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                                     `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                                     `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                     `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                                     `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                     PRIMARY KEY (`column_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '代码生成业务表字段' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of gen_table_column
-- ----------------------------
INSERT INTO `gen_table_column` VALUES (2018961776713011202, 2018961776515878913, 'user_id', '用户ID', 'bigint(20)', 'Long', 'userId', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, 103, 1, '2026-02-04 16:16:13', 1, '2026-02-04 16:20:23');
INSERT INTO `gen_table_column` VALUES (2018961776713011203, 2018961776515878913, 'tenant_id', '租户编号', 'varchar(20)', 'String', 'tenantId', '0', '0', '0', NULL, NULL, NULL, NULL, 'EQ', 'input', '', 2, 103, 1, '2026-02-04 16:16:13', 1, '2026-02-04 16:20:23');
INSERT INTO `gen_table_column` VALUES (2018961776713011204, 2018961776515878913, 'dept_id', '部门ID', 'bigint(20)', 'Long', 'deptId', '0', '0', '0', '1', '1', '1', '1', 'EQ', 'input', '', 3, 103, 1, '2026-02-04 16:16:13', 1, '2026-02-04 16:20:23');
INSERT INTO `gen_table_column` VALUES (2018961776713011205, 2018961776515878913, 'user_name', '用户账号', 'varchar(30)', 'String', 'userName', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 4, 103, 1, '2026-02-04 16:16:13', 1, '2026-02-04 16:20:23');
INSERT INTO `gen_table_column` VALUES (2018961776713011206, 2018961776515878913, 'nick_name', '用户昵称', 'varchar(30)', 'String', 'nickName', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 5, 103, 1, '2026-02-04 16:16:13', 1, '2026-02-04 16:20:23');
INSERT INTO `gen_table_column` VALUES (2018961776713011207, 2018961776515878913, 'user_type', '用户类型（sys_user系统用户）', 'varchar(10)', 'String', 'userType', '0', '0', '0', '1', '1', '1', '1', 'EQ', 'select', '', 6, 103, 1, '2026-02-04 16:16:13', 1, '2026-02-04 16:20:23');
INSERT INTO `gen_table_column` VALUES (2018961776713011208, 2018961776515878913, 'email', '用户邮箱', 'varchar(50)', 'String', 'email', '0', '0', '0', '1', '1', '1', '1', 'EQ', 'input', '', 7, 103, 1, '2026-02-04 16:16:13', 1, '2026-02-04 16:20:23');
INSERT INTO `gen_table_column` VALUES (2018961776713011209, 2018961776515878913, 'phonenumber', '手机号码', 'varchar(11)', 'String', 'phonenumber', '0', '0', '0', '1', '1', '1', '1', 'EQ', 'input', '', 8, 103, 1, '2026-02-04 16:16:13', 1, '2026-02-04 16:20:23');
INSERT INTO `gen_table_column` VALUES (2018961776717205506, 2018961776515878913, 'sex', '用户性别（0男 1女 2未知）', 'char(1)', 'String', 'sex', '0', '0', '0', '1', '1', '1', '1', 'EQ', 'select', '', 9, 103, 1, '2026-02-04 16:16:13', 1, '2026-02-04 16:20:23');
INSERT INTO `gen_table_column` VALUES (2018961776717205507, 2018961776515878913, 'avatar', '头像地址', 'bigint(20)', 'Long', 'avatar', '0', '0', '0', '1', '1', '1', '1', 'EQ', 'input', '', 10, 103, 1, '2026-02-04 16:16:13', 1, '2026-02-04 16:20:23');
INSERT INTO `gen_table_column` VALUES (2018961776717205508, 2018961776515878913, 'password', '密码', 'varchar(100)', 'String', 'password', '0', '0', '0', '1', '1', '1', '1', 'EQ', 'input', '', 11, 103, 1, '2026-02-04 16:16:13', 1, '2026-02-04 16:20:23');
INSERT INTO `gen_table_column` VALUES (2018961776717205509, 2018961776515878913, 'status', '帐号状态（0正常 1停用）', 'char(1)', 'String', 'status', '0', '0', '0', '1', '1', '1', '1', 'EQ', 'radio', '', 12, 103, 1, '2026-02-04 16:16:13', 1, '2026-02-04 16:20:23');
INSERT INTO `gen_table_column` VALUES (2018961776717205510, 2018961776515878913, 'del_flag', '删除标志（0代表存在 1代表删除）', 'char(1)', 'String', 'delFlag', '0', '0', '0', NULL, NULL, NULL, NULL, 'EQ', 'input', '', 13, 103, 1, '2026-02-04 16:16:13', 1, '2026-02-04 16:20:23');
INSERT INTO `gen_table_column` VALUES (2018961776717205511, 2018961776515878913, 'login_ip', '最后登录IP', 'varchar(128)', 'String', 'loginIp', '0', '0', '0', '1', '1', '1', '1', 'EQ', 'input', '', 14, 103, 1, '2026-02-04 16:16:13', 1, '2026-02-04 16:20:23');
INSERT INTO `gen_table_column` VALUES (2018961776717205512, 2018961776515878913, 'login_date', '最后登录时间', 'datetime', 'Date', 'loginDate', '0', '0', '0', '1', '1', '1', '1', 'EQ', 'datetime', '', 15, 103, 1, '2026-02-04 16:16:13', 1, '2026-02-04 16:20:23');
INSERT INTO `gen_table_column` VALUES (2018961776717205513, 2018961776515878913, 'create_dept', '创建部门', 'bigint(20)', 'Long', 'createDept', '0', '0', '0', NULL, NULL, NULL, NULL, 'EQ', 'input', '', 16, 103, 1, '2026-02-04 16:16:13', 1, '2026-02-04 16:20:23');
INSERT INTO `gen_table_column` VALUES (2018961776717205514, 2018961776515878913, 'create_by', '创建者', 'bigint(20)', 'Long', 'createBy', '0', '0', '0', NULL, NULL, NULL, NULL, 'EQ', 'input', '', 17, 103, 1, '2026-02-04 16:16:13', 1, '2026-02-04 16:20:23');
INSERT INTO `gen_table_column` VALUES (2018961776717205515, 2018961776515878913, 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', '0', NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 18, 103, 1, '2026-02-04 16:16:13', 1, '2026-02-04 16:20:23');
INSERT INTO `gen_table_column` VALUES (2018961776721399809, 2018961776515878913, 'update_by', '更新者', 'bigint(20)', 'Long', 'updateBy', '0', '0', '0', NULL, NULL, NULL, NULL, 'EQ', 'input', '', 19, 103, 1, '2026-02-04 16:16:13', 1, '2026-02-04 16:20:23');
INSERT INTO `gen_table_column` VALUES (2018961776721399810, 2018961776515878913, 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', '0', NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 20, 103, 1, '2026-02-04 16:16:13', 1, '2026-02-04 16:20:23');
INSERT INTO `gen_table_column` VALUES (2018961776721399811, 2018961776515878913, 'remark', '备注', 'varchar(500)', 'String', 'remark', '0', '0', '0', '1', '1', '1', NULL, 'EQ', 'textarea', '', 21, 103, 1, '2026-02-04 16:16:13', 1, '2026-02-04 16:20:23');
INSERT INTO `gen_table_column` VALUES (2018961776721399812, 2018961776515878913, 'open_id', '微信用户标识', 'varchar(100)', 'String', 'openId', '0', '0', '0', '1', '1', '1', '1', 'EQ', 'input', '', 22, 103, 1, '2026-02-04 16:16:13', 1, '2026-02-04 16:20:23');
INSERT INTO `gen_table_column` VALUES (2018961776721399813, 2018961776515878913, 'user_balance', '账户余额', 'double(20,2)', 'Long', 'userBalance', '0', '0', '0', '1', '1', '1', '1', 'EQ', 'input', '', 23, 103, 1, '2026-02-04 16:16:13', 1, '2026-02-04 16:20:23');

-- ----------------------------
-- Table structure for graph_build_task
-- ----------------------------
DROP TABLE IF EXISTS `graph_build_task`;
CREATE TABLE `graph_build_task`  (
                                     `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `task_uuid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务UUID',
                                     `graph_uuid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图谱UUID',
                                     `knowledge_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '知识库ID',
                                     `doc_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文档ID（可选，null表示全量构建）',
                                     `task_type` tinyint NULL DEFAULT 1 COMMENT '任务类型：1全量构建、2增量更新、3重建',
                                     `task_status` tinyint NULL DEFAULT 1 COMMENT '任务状态：1待执行、2执行中、3成功、4失败',
                                     `progress` int NULL DEFAULT 0 COMMENT '进度百分比（0-100）',
                                     `total_docs` int NULL DEFAULT 0 COMMENT '总文档数',
                                     `processed_docs` int NULL DEFAULT 0 COMMENT '已处理文档数',
                                     `extracted_entities` int NULL DEFAULT 0 COMMENT '提取的实体数',
                                     `extracted_relations` int NULL DEFAULT 0 COMMENT '提取的关系数',
                                     `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误信息',
                                     `result_summary` json NULL COMMENT '结果摘要(JSON格式)',
                                     `start_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
                                     `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
                                     `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                                     `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
                                     `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
                                     `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                     PRIMARY KEY (`id`) USING BTREE,
                                     UNIQUE INDEX `uk_task_uuid`(`task_uuid` ASC) USING BTREE,
                                     INDEX `idx_graph_uuid`(`graph_uuid` ASC) USING BTREE,
                                     INDEX `idx_knowledge_id`(`knowledge_id` ASC) USING BTREE,
                                     INDEX `idx_task_status`(`task_status` ASC) USING BTREE,
                                     INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '图谱构建任务表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of graph_build_task
-- ----------------------------

-- ----------------------------
-- Table structure for graph_entity_type
-- ----------------------------
DROP TABLE IF EXISTS `graph_entity_type`;
CREATE TABLE `graph_entity_type`  (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                      `type_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '实体类型名称',
                                      `type_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型编码',
                                      `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
                                      `color` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '#1890ff' COMMENT '可视化颜色',
                                      `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图标',
                                      `sort` int NULL DEFAULT 0 COMMENT '显示顺序',
                                      `is_enable` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用（0否 1是）',
                                      `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
                                      `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
                                      `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
                                      PRIMARY KEY (`id`) USING BTREE,
                                      UNIQUE INDEX `uk_type_code`(`type_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '图谱实体类型定义表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of graph_entity_type
-- ----------------------------
INSERT INTO `graph_entity_type` VALUES (1, '人物', 'PERSON', '人物实体，包括真实人物和虚拟角色', '#1890ff', 'user', 1, 1, '', '2025-11-07 16:33:37', '', '2025-11-07 16:33:37', NULL);
INSERT INTO `graph_entity_type` VALUES (2, '机构', 'ORGANIZATION', '组织机构，包括公司、政府机构等', '#52c41a', 'bank', 2, 1, '', '2025-11-07 16:33:37', '', '2025-11-07 16:33:37', NULL);
INSERT INTO `graph_entity_type` VALUES (3, '地点', 'LOCATION', '地理位置，包括国家、城市、地址等', '#fa8c16', 'environment', 3, 1, '', '2025-11-07 16:33:37', '', '2025-11-07 16:33:37', NULL);
INSERT INTO `graph_entity_type` VALUES (4, '概念', 'CONCEPT', '抽象概念，包括理论、方法等', '#722ed1', 'bulb', 4, 1, '', '2025-11-07 16:33:37', '', '2025-11-07 16:33:37', NULL);
INSERT INTO `graph_entity_type` VALUES (5, '事件', 'EVENT', '事件记录，包括历史事件、活动等', '#eb2f96', 'calendar', 5, 1, '', '2025-11-07 16:33:37', '', '2025-11-07 16:33:37', NULL);
INSERT INTO `graph_entity_type` VALUES (6, '产品', 'PRODUCT', '产品或服务', '#13c2c2', 'shopping', 6, 1, '', '2025-11-07 16:33:37', '', '2025-11-07 16:33:37', NULL);
INSERT INTO `graph_entity_type` VALUES (7, '技术', 'TECHNOLOGY', '技术或工具', '#2f54eb', 'tool', 7, 1, '', '2025-11-07 16:33:37', '', '2025-11-07 16:33:37', NULL);
INSERT INTO `graph_entity_type` VALUES (8, '文档', 'DOCUMENT', '文档或资料', '#faad14', 'file-text', 8, 1, '', '2025-11-07 16:33:37', '', '2025-11-07 16:33:37', NULL);

-- ----------------------------
-- Table structure for graph_query_history
-- ----------------------------
DROP TABLE IF EXISTS `graph_query_history`;
CREATE TABLE `graph_query_history`  (
                                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                        `query_uuid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '查询UUID',
                                        `user_id` bigint NOT NULL COMMENT '用户ID',
                                        `knowledge_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '知识库ID',
                                        `graph_uuid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图谱UUID',
                                        `query_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '查询文本',
                                        `query_type` tinyint NULL DEFAULT 1 COMMENT '查询类型：1实体查询、2关系查询、3路径查询、4混合查询',
                                        `cypher_query` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '生成的Cypher查询',
                                        `result_count` int NULL DEFAULT 0 COMMENT '结果数量',
                                        `response_time` int NULL DEFAULT 0 COMMENT '响应时间(ms)',
                                        `is_success` tinyint(1) NULL DEFAULT 1 COMMENT '是否成功（0否 1是）',
                                        `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误信息',
                                        `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        PRIMARY KEY (`id`) USING BTREE,
                                        UNIQUE INDEX `uk_query_uuid`(`query_uuid` ASC) USING BTREE,
                                        INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
                                        INDEX `idx_knowledge_id`(`knowledge_id` ASC) USING BTREE,
                                        INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '图谱查询历史表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of graph_query_history
-- ----------------------------

-- ----------------------------
-- Table structure for graph_relation_type
-- ----------------------------
DROP TABLE IF EXISTS `graph_relation_type`;
CREATE TABLE `graph_relation_type`  (
                                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                        `relation_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '关系名称',
                                        `relation_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '关系编码',
                                        `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
                                        `direction` tinyint(1) NULL DEFAULT 1 COMMENT '关系方向：0双向、1单向',
                                        `style` json NULL COMMENT '可视化样式(JSON格式)',
                                        `sort` int NULL DEFAULT 0 COMMENT '显示顺序',
                                        `is_enable` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用（0否 1是）',
                                        `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
                                        `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
                                        `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                        `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
                                        PRIMARY KEY (`id`) USING BTREE,
                                        UNIQUE INDEX `uk_relation_code`(`relation_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '图谱关系类型定义表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of graph_relation_type
-- ----------------------------
INSERT INTO `graph_relation_type` VALUES (1, '属于', 'BELONGS_TO', '隶属关系，表示从属或归属', 1, NULL, 1, 1, '', '2025-11-07 16:33:37', '', '2025-11-07 16:33:37', NULL);
INSERT INTO `graph_relation_type` VALUES (2, '位于', 'LOCATED_IN', '地理位置关系', 1, NULL, 2, 1, '', '2025-11-07 16:33:37', '', '2025-11-07 16:33:37', NULL);
INSERT INTO `graph_relation_type` VALUES (3, '相关', 'RELATED_TO', '一般关联关系', 0, NULL, 3, 1, '', '2025-11-07 16:33:37', '', '2025-11-07 16:33:37', NULL);
INSERT INTO `graph_relation_type` VALUES (4, '导致', 'CAUSES', '因果关系', 1, NULL, 4, 1, '', '2025-11-07 16:33:37', '', '2025-11-07 16:33:37', NULL);
INSERT INTO `graph_relation_type` VALUES (5, '包含', 'CONTAINS', '包含关系', 1, NULL, 5, 1, '', '2025-11-07 16:33:37', '', '2025-11-07 16:33:37', NULL);
INSERT INTO `graph_relation_type` VALUES (6, '提及', 'MENTIONS', '文档提及实体的关系', 1, NULL, 6, 1, '', '2025-11-07 16:33:37', '', '2025-11-07 16:33:37', NULL);
INSERT INTO `graph_relation_type` VALUES (7, '部分', 'PART_OF', '部分关系', 1, NULL, 7, 1, '', '2025-11-07 16:33:37', '', '2025-11-07 16:33:37', NULL);
INSERT INTO `graph_relation_type` VALUES (8, '实例', 'INSTANCE_OF', '实例关系', 1, NULL, 8, 1, '', '2025-11-07 16:33:37', '', '2025-11-07 16:33:37', NULL);
INSERT INTO `graph_relation_type` VALUES (9, '相似', 'SIMILAR_TO', '相似关系', 0, NULL, 9, 1, '', '2025-11-07 16:33:37', '', '2025-11-07 16:33:37', NULL);
INSERT INTO `graph_relation_type` VALUES (10, '前序', 'PRECEDES', '时序关系', 1, NULL, 10, 1, '', '2025-11-07 16:33:37', '', '2025-11-07 16:33:37', NULL);
INSERT INTO `graph_relation_type` VALUES (11, '工作于', 'WORKS_AT', '人物与机构的工作关系', 1, NULL, 11, 1, '', '2025-11-07 16:33:37', '', '2025-11-07 16:33:37', NULL);
INSERT INTO `graph_relation_type` VALUES (12, '创建', 'CREATED_BY', '创建关系', 1, NULL, 12, 1, '', '2025-11-07 16:33:37', '', '2025-11-07 16:33:37', NULL);
INSERT INTO `graph_relation_type` VALUES (13, '使用', 'USES', '使用关系', 1, NULL, 13, 1, '', '2025-11-07 16:33:37', '', '2025-11-07 16:33:37', NULL);

-- ----------------------------
-- Table structure for graph_statistics
-- ----------------------------
DROP TABLE IF EXISTS `graph_statistics`;
CREATE TABLE `graph_statistics`  (
                                     `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `graph_uuid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图谱UUID',
                                     `stat_date` date NOT NULL COMMENT '统计日期',
                                     `total_nodes` int NULL DEFAULT 0 COMMENT '总节点数',
                                     `total_relationships` int NULL DEFAULT 0 COMMENT '总关系数',
                                     `node_type_distribution` json NULL COMMENT '节点类型分布(JSON格式)',
                                     `relation_type_distribution` json NULL COMMENT '关系类型分布(JSON格式)',
                                     `query_count` int NULL DEFAULT 0 COMMENT '查询次数',
                                     `avg_query_time` int NULL DEFAULT 0 COMMENT '平均查询时间(ms)',
                                     `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                     PRIMARY KEY (`id`) USING BTREE,
                                     UNIQUE INDEX `uk_graph_date`(`graph_uuid` ASC, `stat_date` ASC) USING BTREE,
                                     INDEX `idx_stat_date`(`stat_date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '图谱统计信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of graph_statistics
-- ----------------------------

-- ----------------------------
-- Table structure for knowledge_attach
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_attach`;
CREATE TABLE `knowledge_attach`  (
                                     `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `knowledge_id` bigint NOT NULL COMMENT '知识库ID',
                                     `oss_id` bigint NULL DEFAULT NULL COMMENT '对象存储ID',
                                     `doc_id` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文档ID',
                                     `name` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '附件名称',
                                     `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '附件类型',
                                     `create_dept` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '部门',
                                     `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
                                     `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                     `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                                     `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                     `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
                                     `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户Id',
                                     PRIMARY KEY (`id`) USING BTREE,
                                     UNIQUE INDEX `idx_kname`(`knowledge_id` ASC, `name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2016797369199366146 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库附件' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of knowledge_attach
-- ----------------------------

-- ----------------------------
-- Table structure for knowledge_fragment
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_fragment`;
CREATE TABLE `knowledge_fragment`  (
                                       `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                       `idx` int NOT NULL COMMENT '片段索引下标',
                                       `doc_id` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文档ID',
                                       `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文档内容',
                                       `create_dept` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '部门',
                                       `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
                                       `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                       `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                                       `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                       `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
                                       `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户Id',
                                       PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2016797369027399683 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识片段' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of knowledge_fragment
-- ----------------------------

-- ----------------------------
-- Table structure for knowledge_graph_instance
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_graph_instance`;
CREATE TABLE `knowledge_graph_instance`  (
                                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                             `graph_uuid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图谱UUID',
                                             `knowledge_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '关联knowledge_info.kid',
                                             `graph_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图谱名称',
                                             `graph_status` tinyint NULL DEFAULT 10 COMMENT '构建状态：10构建中、20已完成、30失败',
                                             `node_count` int NULL DEFAULT 0 COMMENT '节点数量',
                                             `relationship_count` int NULL DEFAULT 0 COMMENT '关系数量',
                                             `config` json NULL COMMENT '图谱配置(JSON格式)',
                                             `model_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'LLM模型名称',
                                             `entity_types` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '实体类型（逗号分隔）',
                                             `relation_types` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关系类型（逗号分隔）',
                                             `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误信息',
                                             `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                                             `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
                                             `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
                                             `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
                                             `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
                                             PRIMARY KEY (`id`) USING BTREE,
                                             UNIQUE INDEX `uk_graph_uuid`(`graph_uuid` ASC) USING BTREE,
                                             INDEX `idx_knowledge_id`(`knowledge_id` ASC) USING BTREE,
                                             INDEX `idx_graph_status`(`graph_status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识图谱实例表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of knowledge_graph_instance
-- ----------------------------

-- ----------------------------
-- Table structure for knowledge_graph_segment
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_graph_segment`;
CREATE TABLE `knowledge_graph_segment`  (
                                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                            `uuid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '片段UUID',
                                            `kb_uuid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '知识库UUID',
                                            `kb_item_uuid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '知识库条目UUID',
                                            `doc_uuid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文档UUID',
                                            `segment_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '片段文本内容',
                                            `chunk_index` int NULL DEFAULT 0 COMMENT '片段索引（第几个片段）',
                                            `total_chunks` int NULL DEFAULT 1 COMMENT '总片段数',
                                            `extraction_status` tinyint NULL DEFAULT 0 COMMENT '抽取状态：0-待处理 1-处理中 2-已完成 3-失败',
                                            `entity_count` int NULL DEFAULT 0 COMMENT '抽取的实体数量',
                                            `relation_count` int NULL DEFAULT 0 COMMENT '抽取的关系数量',
                                            `token_used` int NULL DEFAULT 0 COMMENT '消耗的token数',
                                            `error_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '错误信息',
                                            `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
                                            `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                            `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
                                            PRIMARY KEY (`id`) USING BTREE,
                                            UNIQUE INDEX `uk_uuid`(`uuid` ASC) USING BTREE,
                                            INDEX `idx_kb_uuid`(`kb_uuid` ASC) USING BTREE,
                                            INDEX `idx_kb_item_uuid`(`kb_item_uuid` ASC) USING BTREE,
                                            INDEX `idx_doc_uuid`(`doc_uuid` ASC) USING BTREE,
                                            INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
                                            INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识图谱片段表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of knowledge_graph_segment
-- ----------------------------

-- ----------------------------
-- Table structure for knowledge_info
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_info`;
CREATE TABLE `knowledge_info`  (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                   `user_id` bigint NOT NULL DEFAULT 0 COMMENT '用户ID',
                                   `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '知识库名称',
                                   `share` tinyint NULL DEFAULT NULL COMMENT '是否公开知识库（0 否 1是）',
                                   `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '知识库描述',
                                   `separator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '知识分隔符',
                                   `overlap_char` int NULL DEFAULT NULL COMMENT '重叠字符数',
                                   `retrieve_limit` int NULL DEFAULT NULL COMMENT '知识库中检索的条数',
                                   `text_block_size` int NULL DEFAULT NULL COMMENT '文本块大小',
                                   `vector_model` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '向量库',
                                   `embedding_model` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '向量模型',
                                   `create_dept` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '部门',
                                   `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
                                   `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                   `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                                   `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                   `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
                                   `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户Id',
                                   PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2018245281372573699 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of knowledge_info
-- ----------------------------

-- ----------------------------
-- Table structure for sj_distributed_lock
-- ----------------------------
DROP TABLE IF EXISTS `sj_distributed_lock`;
CREATE TABLE `sj_distributed_lock`  (
                                        `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '锁名称',
                                        `lock_until` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '锁定时长',
                                        `locked_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '锁定时间',
                                        `locked_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '锁定者',
                                        `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
                                        PRIMARY KEY (`name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '锁定表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sj_distributed_lock
-- ----------------------------

-- ----------------------------
-- Table structure for sj_group_config
-- ----------------------------
DROP TABLE IF EXISTS `sj_group_config`;
CREATE TABLE `sj_group_config`  (
                                    `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                    `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '组名称',
                                    `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '组描述',
                                    `token` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SJ_cKqBTPzCsWA3VyuCfFoccmuIEGXjr5KT' COMMENT 'token',
                                    `group_status` tinyint NOT NULL DEFAULT 0 COMMENT '组状态 0、未启用 1、启用',
                                    `version` int NOT NULL COMMENT '版本号',
                                    `group_partition` int NOT NULL COMMENT '分区',
                                    `id_generator_mode` tinyint NOT NULL DEFAULT 1 COMMENT '唯一id生成模式 默认号段模式',
                                    `init_scene` tinyint NOT NULL DEFAULT 0 COMMENT '是否初始化场景 0:否 1:是',
                                    `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                    PRIMARY KEY (`id`) USING BTREE,
                                    UNIQUE INDEX `uk_namespace_id_group_name`(`namespace_id` ASC, `group_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '组配置' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sj_group_config
-- ----------------------------
INSERT INTO `sj_group_config` VALUES (1, 'dev', 'ruoyi_group', '', 'SJ_cKqBTPzCsWA3VyuCfFoccmuIEGXjr5KT', 1, 1, 0, 1, 1, '2026-02-05 10:17:11', '2026-02-05 10:17:11');
INSERT INTO `sj_group_config` VALUES (2, 'prod', 'ruoyi_group', '', 'SJ_cKqBTPzCsWA3VyuCfFoccmuIEGXjr5KT', 1, 1, 0, 1, 1, '2026-02-05 10:17:11', '2026-02-05 10:17:11');

-- ----------------------------
-- Table structure for sj_job
-- ----------------------------
DROP TABLE IF EXISTS `sj_job`;
CREATE TABLE `sj_job`  (
                           `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                           `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                           `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
                           `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '名称',
                           `args_str` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '执行方法参数',
                           `args_type` tinyint NOT NULL DEFAULT 1 COMMENT '参数类型 ',
                           `next_trigger_at` bigint NOT NULL COMMENT '下次触发时间',
                           `job_status` tinyint NOT NULL DEFAULT 1 COMMENT '任务状态 0、关闭、1、开启',
                           `task_type` tinyint NOT NULL DEFAULT 1 COMMENT '任务类型 1、集群 2、广播 3、切片',
                           `route_key` tinyint NOT NULL DEFAULT 4 COMMENT '路由策略',
                           `executor_type` tinyint NOT NULL DEFAULT 1 COMMENT '执行器类型',
                           `executor_info` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '执行器名称',
                           `trigger_type` tinyint NOT NULL COMMENT '触发类型 1.CRON 表达式 2. 固定时间',
                           `trigger_interval` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '间隔时长',
                           `block_strategy` tinyint NOT NULL DEFAULT 1 COMMENT '阻塞策略 1、丢弃 2、覆盖 3、并行 4、恢复',
                           `executor_timeout` int NOT NULL DEFAULT 0 COMMENT '任务执行超时时间，单位秒',
                           `max_retry_times` int NOT NULL DEFAULT 0 COMMENT '最大重试次数',
                           `parallel_num` int NOT NULL DEFAULT 1 COMMENT '并行数',
                           `retry_interval` int NOT NULL DEFAULT 0 COMMENT '重试间隔(s)',
                           `bucket_index` int NOT NULL DEFAULT 0 COMMENT 'bucket',
                           `resident` tinyint NOT NULL DEFAULT 0 COMMENT '是否是常驻任务',
                           `notify_ids` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '通知告警场景配置id列表',
                           `owner_id` bigint NULL DEFAULT NULL COMMENT '负责人id',
                           `labels` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '标签',
                           `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '描述',
                           `ext_attrs` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '扩展字段',
                           `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 1、删除',
                           `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                           `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                           PRIMARY KEY (`id`) USING BTREE,
                           INDEX `idx_namespace_id_group_name`(`namespace_id` ASC, `group_name` ASC) USING BTREE,
                           INDEX `idx_job_status_bucket_index`(`job_status` ASC, `bucket_index` ASC) USING BTREE,
                           INDEX `idx_create_dt`(`create_dt` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '任务信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sj_job
-- ----------------------------
INSERT INTO `sj_job` VALUES (1, 'dev', 'ruoyi_group', 'demo-job', NULL, 1, 1710344035622, 1, 1, 4, 1, 'testJobExecutor', 2, '60', 1, 60, 3, 1, 1, 116, 0, '', 1, '', '', '', 0, '2026-02-05 10:17:13', '2026-02-05 10:17:13');

-- ----------------------------
-- Table structure for sj_job_executor
-- ----------------------------
DROP TABLE IF EXISTS `sj_job_executor`;
CREATE TABLE `sj_job_executor`  (
                                    `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                    `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
                                    `executor_info` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务执行器名称',
                                    `executor_type` varchar(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '1:java 2:python 3:go',
                                    `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                    PRIMARY KEY (`id`) USING BTREE,
                                    INDEX `idx_namespace_id_group_name`(`namespace_id` ASC, `group_name` ASC) USING BTREE,
                                    INDEX `idx_create_dt`(`create_dt` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '任务执行器信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sj_job_executor
-- ----------------------------

-- ----------------------------
-- Table structure for sj_job_log_message
-- ----------------------------
DROP TABLE IF EXISTS `sj_job_log_message`;
CREATE TABLE `sj_job_log_message`  (
                                       `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                                       `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                       `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
                                       `job_id` bigint NOT NULL COMMENT '任务信息id',
                                       `task_batch_id` bigint NOT NULL COMMENT '任务批次id',
                                       `task_id` bigint NOT NULL COMMENT '调度任务id',
                                       `message` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调度信息',
                                       `log_num` int NOT NULL DEFAULT 1 COMMENT '日志数量',
                                       `real_time` bigint NOT NULL DEFAULT 0 COMMENT '上报时间',
                                       `ext_attrs` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '扩展字段',
                                       `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       PRIMARY KEY (`id`) USING BTREE,
                                       INDEX `idx_task_batch_id_task_id`(`task_batch_id` ASC, `task_id` ASC) USING BTREE,
                                       INDEX `idx_create_dt`(`create_dt` ASC) USING BTREE,
                                       INDEX `idx_namespace_id_group_name`(`namespace_id` ASC, `group_name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '调度日志' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sj_job_log_message
-- ----------------------------

-- ----------------------------
-- Table structure for sj_job_summary
-- ----------------------------
DROP TABLE IF EXISTS `sj_job_summary`;
CREATE TABLE `sj_job_summary`  (
                                   `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                                   `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                   `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '组名称',
                                   `business_id` bigint NOT NULL COMMENT '业务id (job_id或workflow_id)',
                                   `system_task_type` tinyint NOT NULL DEFAULT 3 COMMENT '任务类型 3、JOB任务 4、WORKFLOW任务',
                                   `trigger_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '统计时间',
                                   `success_num` int NOT NULL DEFAULT 0 COMMENT '执行成功-日志数量',
                                   `fail_num` int NOT NULL DEFAULT 0 COMMENT '执行失败-日志数量',
                                   `fail_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '失败原因',
                                   `stop_num` int NOT NULL DEFAULT 0 COMMENT '执行失败-日志数量',
                                   `stop_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '失败原因',
                                   `cancel_num` int NOT NULL DEFAULT 0 COMMENT '执行失败-日志数量',
                                   `cancel_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '失败原因',
                                   `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                   PRIMARY KEY (`id`) USING BTREE,
                                   UNIQUE INDEX `uk_trigger_at_system_task_type_business_id`(`trigger_at` ASC, `system_task_type` ASC, `business_id` ASC) USING BTREE,
                                   INDEX `idx_namespace_id_group_name_business_id`(`namespace_id` ASC, `group_name` ASC, `business_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'DashBoard_Job' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sj_job_summary
-- ----------------------------

-- ----------------------------
-- Table structure for sj_job_task
-- ----------------------------
DROP TABLE IF EXISTS `sj_job_task`;
CREATE TABLE `sj_job_task`  (
                                `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                                `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
                                `job_id` bigint NOT NULL COMMENT '任务信息id',
                                `task_batch_id` bigint NOT NULL COMMENT '调度任务id',
                                `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父执行器id',
                                `task_status` tinyint NOT NULL DEFAULT 0 COMMENT '执行的状态 0、失败 1、成功',
                                `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
                                `mr_stage` tinyint NULL DEFAULT NULL COMMENT '动态分片所处阶段 1:map 2:reduce 3:mergeReduce',
                                `leaf` tinyint NOT NULL DEFAULT 1 COMMENT '叶子节点',
                                `task_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '任务名称',
                                `client_info` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户端地址 clientId#ip:port',
                                `wf_context` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '工作流全局上下文',
                                `result_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '执行结果',
                                `args_str` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '执行方法参数',
                                `args_type` tinyint NOT NULL DEFAULT 1 COMMENT '参数类型 ',
                                `ext_attrs` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '扩展字段',
                                `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                PRIMARY KEY (`id`) USING BTREE,
                                INDEX `idx_task_batch_id_task_status`(`task_batch_id` ASC, `task_status` ASC) USING BTREE,
                                INDEX `idx_create_dt`(`create_dt` ASC) USING BTREE,
                                INDEX `idx_namespace_id_group_name`(`namespace_id` ASC, `group_name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '任务实例' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sj_job_task
-- ----------------------------

-- ----------------------------
-- Table structure for sj_job_task_batch
-- ----------------------------
DROP TABLE IF EXISTS `sj_job_task_batch`;
CREATE TABLE `sj_job_task_batch`  (
                                      `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                                      `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                      `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
                                      `job_id` bigint NOT NULL COMMENT '任务id',
                                      `workflow_node_id` bigint NOT NULL DEFAULT 0 COMMENT '工作流节点id',
                                      `parent_workflow_node_id` bigint NOT NULL DEFAULT 0 COMMENT '工作流任务父批次id',
                                      `workflow_task_batch_id` bigint NOT NULL DEFAULT 0 COMMENT '工作流任务批次id',
                                      `task_batch_status` tinyint NOT NULL DEFAULT 0 COMMENT '任务批次状态 0、失败 1、成功',
                                      `operation_reason` tinyint NOT NULL DEFAULT 0 COMMENT '操作原因',
                                      `execution_at` bigint NOT NULL DEFAULT 0 COMMENT '任务执行时间',
                                      `system_task_type` tinyint NOT NULL DEFAULT 3 COMMENT '任务类型 3、JOB任务 4、WORKFLOW任务',
                                      `parent_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '父节点',
                                      `ext_attrs` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '扩展字段',
                                      `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 1、删除',
                                      `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                      PRIMARY KEY (`id`) USING BTREE,
                                      INDEX `idx_job_id_task_batch_status`(`job_id` ASC, `task_batch_status` ASC) USING BTREE,
                                      INDEX `idx_create_dt`(`create_dt` ASC) USING BTREE,
                                      INDEX `idx_namespace_id_group_name`(`namespace_id` ASC, `group_name` ASC) USING BTREE,
                                      INDEX `idx_workflow_task_batch_id_workflow_node_id`(`workflow_task_batch_id` ASC, `workflow_node_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '任务批次' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sj_job_task_batch
-- ----------------------------

-- ----------------------------
-- Table structure for sj_namespace
-- ----------------------------
DROP TABLE IF EXISTS `sj_namespace`;
CREATE TABLE `sj_namespace`  (
                                 `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                                 `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '名称',
                                 `unique_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '唯一id',
                                 `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '描述',
                                 `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 1、删除',
                                 `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                 PRIMARY KEY (`id`) USING BTREE,
                                 UNIQUE INDEX `uk_unique_id`(`unique_id` ASC) USING BTREE,
                                 INDEX `idx_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '命名空间' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sj_namespace
-- ----------------------------
INSERT INTO `sj_namespace` VALUES (1, 'Development', 'dev', '', 0, '2026-02-05 10:17:10', '2026-02-05 10:17:10');
INSERT INTO `sj_namespace` VALUES (2, 'Production', 'prod', '', 0, '2026-02-05 10:17:10', '2026-02-05 10:17:10');

-- ----------------------------
-- Table structure for sj_notify_config
-- ----------------------------
DROP TABLE IF EXISTS `sj_notify_config`;
CREATE TABLE `sj_notify_config`  (
                                     `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                     `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
                                     `notify_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '通知名称',
                                     `system_task_type` tinyint NOT NULL DEFAULT 3 COMMENT '任务类型 1. 重试任务 2. 重试回调 3、JOB任务 4、WORKFLOW任务',
                                     `notify_status` tinyint NOT NULL DEFAULT 0 COMMENT '通知状态 0、未启用 1、启用',
                                     `recipient_ids` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '接收人id列表',
                                     `notify_threshold` int NOT NULL DEFAULT 0 COMMENT '通知阈值',
                                     `notify_scene` tinyint NOT NULL DEFAULT 0 COMMENT '通知场景',
                                     `rate_limiter_status` tinyint NOT NULL DEFAULT 0 COMMENT '限流状态 0、未启用 1、启用',
                                     `rate_limiter_threshold` int NOT NULL DEFAULT 0 COMMENT '每秒限流阈值',
                                     `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '描述',
                                     `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                     PRIMARY KEY (`id`) USING BTREE,
                                     INDEX `idx_namespace_id_group_name_scene_name`(`namespace_id` ASC, `group_name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通知配置' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sj_notify_config
-- ----------------------------

-- ----------------------------
-- Table structure for sj_notify_recipient
-- ----------------------------
DROP TABLE IF EXISTS `sj_notify_recipient`;
CREATE TABLE `sj_notify_recipient`  (
                                        `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                                        `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                        `recipient_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '接收人名称',
                                        `notify_type` tinyint NOT NULL DEFAULT 0 COMMENT '通知类型 1、钉钉 2、邮件 3、企业微信 4 飞书 5 webhook',
                                        `notify_attribute` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置属性',
                                        `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '描述',
                                        `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                        PRIMARY KEY (`id`) USING BTREE,
                                        INDEX `idx_namespace_id`(`namespace_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '告警通知接收人' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sj_notify_recipient
-- ----------------------------

-- ----------------------------
-- Table structure for sj_retry
-- ----------------------------
DROP TABLE IF EXISTS `sj_retry`;
CREATE TABLE `sj_retry`  (
                             `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
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
                             `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
                             `retry_status` tinyint NOT NULL DEFAULT 0 COMMENT '重试状态 0、重试中 1、成功 2、最大重试次数',
                             `task_type` tinyint NOT NULL DEFAULT 1 COMMENT '任务类型 1、重试数据 2、回调数据',
                             `bucket_index` int NOT NULL DEFAULT 0 COMMENT 'bucket',
                             `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父节点id',
                             `deleted` bigint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
                             `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                             `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                             PRIMARY KEY (`id`) USING BTREE,
                             UNIQUE INDEX `uk_scene_tasktype_idempotentid_deleted`(`scene_id` ASC, `task_type` ASC, `idempotent_id` ASC, `deleted` ASC) USING BTREE,
                             INDEX `idx_biz_no`(`biz_no` ASC) USING BTREE,
                             INDEX `idx_idempotent_id`(`idempotent_id` ASC) USING BTREE,
                             INDEX `idx_retry_status_bucket_index`(`retry_status` ASC, `bucket_index` ASC) USING BTREE,
                             INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
                             INDEX `idx_create_dt`(`create_dt` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '重试信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sj_retry
-- ----------------------------

-- ----------------------------
-- Table structure for sj_retry_dead_letter
-- ----------------------------
DROP TABLE IF EXISTS `sj_retry_dead_letter`;
CREATE TABLE `sj_retry_dead_letter`  (
                                         `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
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
                                         INDEX `idx_namespace_id_group_name_scene_name`(`namespace_id` ASC, `group_name` ASC, `scene_name` ASC) USING BTREE,
                                         INDEX `idx_idempotent_id`(`idempotent_id` ASC) USING BTREE,
                                         INDEX `idx_biz_no`(`biz_no` ASC) USING BTREE,
                                         INDEX `idx_create_dt`(`create_dt` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '死信队列表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sj_retry_dead_letter
-- ----------------------------

-- ----------------------------
-- Table structure for sj_retry_scene_config
-- ----------------------------
DROP TABLE IF EXISTS `sj_retry_scene_config`;
CREATE TABLE `sj_retry_scene_config`  (
                                          `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                                          `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                          `scene_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '场景名称',
                                          `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
                                          `scene_status` tinyint NOT NULL DEFAULT 0 COMMENT '组状态 0、未启用 1、启用',
                                          `max_retry_count` int NOT NULL DEFAULT 5 COMMENT '最大重试次数',
                                          `back_off` tinyint NOT NULL DEFAULT 1 COMMENT '1、默认等级 2、固定间隔时间 3、CRON 表达式',
                                          `trigger_interval` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '间隔时长',
                                          `notify_ids` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '通知告警场景配置id列表',
                                          `deadline_request` bigint UNSIGNED NOT NULL DEFAULT 60000 COMMENT 'Deadline Request 调用链超时 单位毫秒',
                                          `executor_timeout` int UNSIGNED NOT NULL DEFAULT 5 COMMENT '任务执行超时时间，单位秒',
                                          `route_key` tinyint NOT NULL DEFAULT 4 COMMENT '路由策略',
                                          `block_strategy` tinyint NOT NULL DEFAULT 1 COMMENT '阻塞策略 1、丢弃 2、覆盖 3、并行',
                                          `cb_status` tinyint NOT NULL DEFAULT 0 COMMENT '回调状态 0、不开启 1、开启',
                                          `cb_trigger_type` tinyint NOT NULL DEFAULT 1 COMMENT '1、默认等级 2、固定间隔时间 3、CRON 表达式',
                                          `cb_max_count` int NOT NULL DEFAULT 16 COMMENT '回调的最大执行次数',
                                          `cb_trigger_interval` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '回调的最大执行次数',
                                          `owner_id` bigint NULL DEFAULT NULL COMMENT '负责人id',
                                          `labels` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '标签',
                                          `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '描述',
                                          `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                          `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                          PRIMARY KEY (`id`) USING BTREE,
                                          UNIQUE INDEX `uk_namespace_id_group_name_scene_name`(`namespace_id` ASC, `group_name` ASC, `scene_name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '场景配置' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sj_retry_scene_config
-- ----------------------------

-- ----------------------------
-- Table structure for sj_retry_summary
-- ----------------------------
DROP TABLE IF EXISTS `sj_retry_summary`;
CREATE TABLE `sj_retry_summary`  (
                                     `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                     `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '组名称',
                                     `scene_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '场景名称',
                                     `trigger_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '统计时间',
                                     `running_num` int NOT NULL DEFAULT 0 COMMENT '重试中-日志数量',
                                     `finish_num` int NOT NULL DEFAULT 0 COMMENT '重试完成-日志数量',
                                     `max_count_num` int NOT NULL DEFAULT 0 COMMENT '重试到达最大次数-日志数量',
                                     `suspend_num` int NOT NULL DEFAULT 0 COMMENT '暂停重试-日志数量',
                                     `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                     PRIMARY KEY (`id`) USING BTREE,
                                     UNIQUE INDEX `uk_scene_name_trigger_at`(`namespace_id` ASC, `group_name` ASC, `scene_name` ASC, `trigger_at` ASC) USING BTREE,
                                     INDEX `idx_trigger_at`(`trigger_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'DashBoard_Retry' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sj_retry_summary
-- ----------------------------

-- ----------------------------
-- Table structure for sj_retry_task
-- ----------------------------
DROP TABLE IF EXISTS `sj_retry_task`;
CREATE TABLE `sj_retry_task`  (
                                  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                                  `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                  `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
                                  `scene_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '场景名称',
                                  `retry_id` bigint NOT NULL COMMENT '重试信息Id',
                                  `ext_attrs` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '扩展字段',
                                  `task_status` tinyint NOT NULL DEFAULT 1 COMMENT '重试状态',
                                  `task_type` tinyint NOT NULL DEFAULT 1 COMMENT '任务类型 1、重试数据 2、回调数据',
                                  `operation_reason` tinyint NOT NULL DEFAULT 0 COMMENT '操作原因',
                                  `client_info` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户端地址 clientId#ip:port',
                                  `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                  PRIMARY KEY (`id`) USING BTREE,
                                  INDEX `idx_group_name_scene_name`(`namespace_id` ASC, `group_name` ASC, `scene_name` ASC) USING BTREE,
                                  INDEX `task_status`(`task_status` ASC) USING BTREE,
                                  INDEX `idx_create_dt`(`create_dt` ASC) USING BTREE,
                                  INDEX `idx_retry_id`(`retry_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '重试任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sj_retry_task
-- ----------------------------

-- ----------------------------
-- Table structure for sj_retry_task_log_message
-- ----------------------------
DROP TABLE IF EXISTS `sj_retry_task_log_message`;
CREATE TABLE `sj_retry_task_log_message`  (
                                              `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                                              `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                              `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
                                              `retry_id` bigint NOT NULL COMMENT '重试信息Id',
                                              `retry_task_id` bigint NOT NULL COMMENT '重试任务Id',
                                              `message` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '异常信息',
                                              `log_num` int NOT NULL DEFAULT 1 COMMENT '日志数量',
                                              `real_time` bigint NOT NULL DEFAULT 0 COMMENT '上报时间',
                                              `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                              PRIMARY KEY (`id`) USING BTREE,
                                              INDEX `idx_namespace_id_group_name_retry_task_id`(`namespace_id` ASC, `group_name` ASC, `retry_task_id` ASC) USING BTREE,
                                              INDEX `idx_create_dt`(`create_dt` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '任务调度日志信息记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sj_retry_task_log_message
-- ----------------------------

-- ----------------------------
-- Table structure for sj_server_node
-- ----------------------------
DROP TABLE IF EXISTS `sj_server_node`;
CREATE TABLE `sj_server_node`  (
                                   `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                                   `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                   `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
                                   `host_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主机id',
                                   `host_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '机器ip',
                                   `host_port` int NOT NULL COMMENT '机器端口',
                                   `expire_at` datetime NOT NULL COMMENT '过期时间',
                                   `node_type` tinyint NOT NULL COMMENT '节点类型 1、客户端 2、是服务端',
                                   `ext_attrs` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '扩展字段',
                                   `labels` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '标签',
                                   `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                   PRIMARY KEY (`id`) USING BTREE,
                                   UNIQUE INDEX `uk_host_id_host_ip`(`host_id` ASC, `host_ip` ASC) USING BTREE,
                                   INDEX `idx_namespace_id_group_name`(`namespace_id` ASC, `group_name` ASC) USING BTREE,
                                   INDEX `idx_expire_at_node_type`(`expire_at` ASC, `node_type` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '服务器节点' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sj_server_node
-- ----------------------------

-- ----------------------------
-- Table structure for sj_system_user
-- ----------------------------
DROP TABLE IF EXISTS `sj_system_user`;
CREATE TABLE `sj_system_user`  (
                                   `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                                   `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '账号',
                                   `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
                                   `role` tinyint NOT NULL DEFAULT 0 COMMENT '角色：1-普通用户、2-管理员',
                                   `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                   PRIMARY KEY (`id`) USING BTREE,
                                   UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sj_system_user
-- ----------------------------
INSERT INTO `sj_system_user` VALUES (1, 'admin', '465c194afb65670f38322df087f0a9bb225cc257e43eb4ac5a0c98ef5b3173ac', 2, '2026-02-05 10:17:13', '2026-02-05 10:17:13');

-- ----------------------------
-- Table structure for sj_system_user_permission
-- ----------------------------
DROP TABLE IF EXISTS `sj_system_user_permission`;
CREATE TABLE `sj_system_user_permission`  (
                                              `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                                              `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
                                              `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                              `system_user_id` bigint NOT NULL COMMENT '系统用户id',
                                              `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                              `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                              PRIMARY KEY (`id`) USING BTREE,
                                              UNIQUE INDEX `uk_namespace_id_group_name_system_user_id`(`namespace_id` ASC, `group_name` ASC, `system_user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统用户权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sj_system_user_permission
-- ----------------------------

-- ----------------------------
-- Table structure for sj_workflow
-- ----------------------------
DROP TABLE IF EXISTS `sj_workflow`;
CREATE TABLE `sj_workflow`  (
                                `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                                `workflow_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工作流名称',
                                `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
                                `workflow_status` tinyint NOT NULL DEFAULT 1 COMMENT '工作流状态 0、关闭、1、开启',
                                `trigger_type` tinyint NOT NULL COMMENT '触发类型 1.CRON 表达式 2. 固定时间',
                                `trigger_interval` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '间隔时长',
                                `next_trigger_at` bigint NOT NULL COMMENT '下次触发时间',
                                `block_strategy` tinyint NOT NULL DEFAULT 1 COMMENT '阻塞策略 1、丢弃 2、覆盖 3、并行',
                                `executor_timeout` int NOT NULL DEFAULT 0 COMMENT '任务执行超时时间，单位秒',
                                `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '描述',
                                `flow_info` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '流程信息',
                                `wf_context` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '上下文',
                                `notify_ids` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '通知告警场景配置id列表',
                                `bucket_index` int NOT NULL DEFAULT 0 COMMENT 'bucket',
                                `version` int NOT NULL COMMENT '版本号',
                                `owner_id` bigint NULL DEFAULT NULL COMMENT '负责人id',
                                `ext_attrs` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '扩展字段',
                                `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 1、删除',
                                `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                PRIMARY KEY (`id`) USING BTREE,
                                INDEX `idx_create_dt`(`create_dt` ASC) USING BTREE,
                                INDEX `idx_namespace_id_group_name`(`namespace_id` ASC, `group_name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工作流' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sj_workflow
-- ----------------------------

-- ----------------------------
-- Table structure for sj_workflow_node
-- ----------------------------
DROP TABLE IF EXISTS `sj_workflow_node`;
CREATE TABLE `sj_workflow_node`  (
                                     `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                                     `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                     `node_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '节点名称',
                                     `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
                                     `job_id` bigint NOT NULL COMMENT '任务信息id',
                                     `workflow_id` bigint NOT NULL COMMENT '工作流ID',
                                     `node_type` tinyint NOT NULL DEFAULT 1 COMMENT '1、任务节点 2、条件节点',
                                     `expression_type` tinyint NOT NULL DEFAULT 0 COMMENT '1、SpEl、2、Aviator 3、QL',
                                     `fail_strategy` tinyint NOT NULL DEFAULT 1 COMMENT '失败策略 1、跳过 2、阻塞',
                                     `workflow_node_status` tinyint NOT NULL DEFAULT 1 COMMENT '工作流节点状态 0、关闭、1、开启',
                                     `priority_level` int NOT NULL DEFAULT 1 COMMENT '优先级',
                                     `node_info` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '节点信息 ',
                                     `version` int NOT NULL COMMENT '版本号',
                                     `ext_attrs` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '扩展字段',
                                     `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 1、删除',
                                     `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                     PRIMARY KEY (`id`) USING BTREE,
                                     INDEX `idx_create_dt`(`create_dt` ASC) USING BTREE,
                                     INDEX `idx_namespace_id_group_name`(`namespace_id` ASC, `group_name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工作流节点' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sj_workflow_node
-- ----------------------------

-- ----------------------------
-- Table structure for sj_workflow_task_batch
-- ----------------------------
DROP TABLE IF EXISTS `sj_workflow_task_batch`;
CREATE TABLE `sj_workflow_task_batch`  (
                                           `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
                                           `namespace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '764d604ec6fc45f68cd92514c40e9e1a' COMMENT '命名空间id',
                                           `group_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组名称',
                                           `workflow_id` bigint NOT NULL COMMENT '工作流任务id',
                                           `task_batch_status` tinyint NOT NULL DEFAULT 0 COMMENT '任务批次状态 0、失败 1、成功',
                                           `operation_reason` tinyint NOT NULL DEFAULT 0 COMMENT '操作原因',
                                           `flow_info` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '流程信息',
                                           `wf_context` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '全局上下文',
                                           `execution_at` bigint NOT NULL DEFAULT 0 COMMENT '任务执行时间',
                                           `ext_attrs` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '扩展字段',
                                           `version` int NOT NULL DEFAULT 1 COMMENT '版本号',
                                           `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 1、删除',
                                           `create_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           `update_dt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                                           PRIMARY KEY (`id`) USING BTREE,
                                           INDEX `idx_job_id_task_batch_status`(`workflow_id` ASC, `task_batch_status` ASC) USING BTREE,
                                           INDEX `idx_create_dt`(`create_dt` ASC) USING BTREE,
                                           INDEX `idx_namespace_id_group_name`(`namespace_id` ASC, `group_name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工作流批次' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sj_workflow_task_batch
-- ----------------------------

-- ----------------------------
-- Table structure for sys_client
-- ----------------------------
DROP TABLE IF EXISTS `sys_client`;
CREATE TABLE `sys_client`  (
                               `id` bigint NOT NULL COMMENT 'id',
                               `client_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户端id',
                               `client_key` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户端key',
                               `client_secret` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户端秘钥',
                               `grant_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '授权类型',
                               `device_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备类型',
                               `active_timeout` int NULL DEFAULT 1800 COMMENT 'token活跃超时时间',
                               `timeout` int NULL DEFAULT 604800 COMMENT 'token固定超时',
                               `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
                               `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
                               `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                               `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                               `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                               `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                               `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                               PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统授权表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_client
-- ----------------------------
INSERT INTO `sys_client` VALUES (1, 'e5cd7e4891bf95d1d19206ce24a7b32e', 'pc', 'pc123', 'password,social', 'pc', 1800, 604800, '0', '0', 103, 1, '2026-02-03 05:14:53', 1, '2026-02-03 05:14:53');
INSERT INTO `sys_client` VALUES (2, '428a8310cd442757ae699df5d894f051', 'app', 'app123', 'password,sms,social', 'android', 1800, 604800, '0', '0', 103, 1, '2026-02-03 05:14:53', 1, '2026-02-03 05:14:53');
INSERT INTO `sys_client` VALUES (2033738530356912129, '0d4c873ff6146ecd7f38e2e45526ab1b', 'web', 'web123', 'sms,email,password', 'pc', 1800, 604800, '0', '0', 103, 1, '2026-03-17 10:53:45', 1, '2026-03-17 10:59:16');

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
                               `config_id` bigint NOT NULL COMMENT '参数主键',
                               `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户编号',
                               `config_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '参数名称',
                               `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '参数键名',
                               `config_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '参数键值',
                               `config_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
                               `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                               `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                               `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                               `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                               `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                               `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
                               PRIMARY KEY (`config_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '参数配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config` VALUES (1, '000000', '主框架页-默认皮肤样式名称', 'sys.index.skinName', 'skin-blue', 'Y', 103, 1, '2026-02-03 05:14:52', NULL, NULL, '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow');
INSERT INTO `sys_config` VALUES (2, '000000', '用户管理-账号初始密码', 'sys.user.initPassword', '123456', 'Y', 103, 1, '2026-02-03 05:14:52', NULL, NULL, '初始化密码 123456');
INSERT INTO `sys_config` VALUES (3, '000000', '主框架页-侧边栏主题', 'sys.index.sideTheme', 'theme-dark', 'Y', 103, 1, '2026-02-03 05:14:52', NULL, NULL, '深色主题theme-dark，浅色主题theme-light');
INSERT INTO `sys_config` VALUES (5, '000000', '账号自助-是否开启用户注册功能', 'sys.account.registerUser', 'false', 'Y', 103, 1, '2026-02-03 05:14:52', NULL, NULL, '是否开启注册用户功能（true开启，false关闭）');
INSERT INTO `sys_config` VALUES (11, '000000', 'OSS预览列表资源开关', 'sys.oss.previewListResource', 'true', 'Y', 103, 1, '2026-02-03 05:14:52', NULL, NULL, 'true:开启, false:关闭');
INSERT INTO `sys_config` VALUES (2018858143803641858, '154726', '主框架页-默认皮肤样式名称', 'sys.index.skinName', 'skin-blue', 'Y', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow');
INSERT INTO `sys_config` VALUES (2018858143803641859, '154726', '用户管理-账号初始密码', 'sys.user.initPassword', '123456', 'Y', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '初始化密码 123456');
INSERT INTO `sys_config` VALUES (2018858143803641860, '154726', '主框架页-侧边栏主题', 'sys.index.sideTheme', 'theme-dark', 'Y', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '深色主题theme-dark，浅色主题theme-light');
INSERT INTO `sys_config` VALUES (2018858143803641861, '154726', '账号自助-是否开启用户注册功能', 'sys.account.registerUser', 'false', 'Y', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '是否开启注册用户功能（true开启，false关闭）');
INSERT INTO `sys_config` VALUES (2018858143803641862, '154726', 'OSS预览列表资源开关', 'sys.oss.previewListResource', 'true', 'Y', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', 'true:开启, false:关闭');

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`  (
                             `dept_id` bigint NOT NULL COMMENT '部门id',
                             `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户编号',
                             `parent_id` bigint NULL DEFAULT 0 COMMENT '父部门id',
                             `ancestors` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '祖级列表',
                             `dept_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '部门名称',
                             `dept_category` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '部门类别编码',
                             `order_num` int NULL DEFAULT 0 COMMENT '显示顺序',
                             `leader` bigint NULL DEFAULT NULL COMMENT '负责人',
                             `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
                             `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
                             `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
                             `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
                             `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                             `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                             `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                             `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                             `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                             PRIMARY KEY (`dept_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '部门表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
INSERT INTO `sys_dept` VALUES (100, '000000', 0, '0', '熊猫科技', NULL, 0, NULL, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2026-02-03 05:14:38', 1, '2026-02-06 00:38:24');
INSERT INTO `sys_dept` VALUES (101, '000000', 100, '0,100', '深圳总公司', NULL, 1, NULL, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2026-02-03 05:14:38', NULL, NULL);
INSERT INTO `sys_dept` VALUES (102, '000000', 100, '0,100', '长沙分公司', NULL, 2, NULL, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2026-02-03 05:14:38', NULL, NULL);
INSERT INTO `sys_dept` VALUES (103, '000000', 101, '0,100,101', '研发部门', NULL, 1, 1, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2026-02-03 05:14:39', NULL, NULL);
INSERT INTO `sys_dept` VALUES (104, '000000', 101, '0,100,101', '市场部门', NULL, 2, NULL, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2026-02-03 05:14:39', NULL, NULL);
INSERT INTO `sys_dept` VALUES (105, '000000', 101, '0,100,101', '测试部门', NULL, 3, NULL, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2026-02-03 05:14:39', NULL, NULL);
INSERT INTO `sys_dept` VALUES (106, '000000', 101, '0,100,101', '财务部门', NULL, 4, NULL, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2026-02-03 05:14:39', NULL, NULL);
INSERT INTO `sys_dept` VALUES (107, '000000', 101, '0,100,101', '运维部门', NULL, 5, NULL, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2026-02-03 05:14:39', NULL, NULL);
INSERT INTO `sys_dept` VALUES (108, '000000', 102, '0,100,102', '市场部门', NULL, 1, NULL, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2026-02-03 05:14:39', NULL, NULL);
INSERT INTO `sys_dept` VALUES (109, '000000', 102, '0,100,102', '财务部门', NULL, 2, NULL, '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2026-02-03 05:14:39', NULL, NULL);
INSERT INTO `sys_dept` VALUES (2018858143262576642, '154726', 0, '0', '测试', NULL, 0, 2018858143623286785, NULL, NULL, '0', '0', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25');

-- ----------------------------
-- Table structure for sys_dict_data
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data`  (
                                  `dict_code` bigint NOT NULL COMMENT '字典编码',
                                  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户编号',
                                  `dict_sort` int NULL DEFAULT 0 COMMENT '字典排序',
                                  `dict_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '字典标签',
                                  `dict_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '字典键值',
                                  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '字典类型',
                                  `css_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
                                  `list_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '表格回显样式',
                                  `is_default` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
                                  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                                  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                                  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                                  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
                                  PRIMARY KEY (`dict_code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '字典数据表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_dict_data
-- ----------------------------
INSERT INTO `sys_dict_data` VALUES (1, '000000', 1, '男', '0', 'sys_user_sex', '', '', 'Y', 103, 1, '2026-02-03 05:14:50', NULL, NULL, '性别男');
INSERT INTO `sys_dict_data` VALUES (2, '000000', 2, '女', '1', 'sys_user_sex', '', '', 'N', 103, 1, '2026-02-03 05:14:50', NULL, NULL, '性别女');
INSERT INTO `sys_dict_data` VALUES (3, '000000', 3, '未知', '2', 'sys_user_sex', '', '', 'N', 103, 1, '2026-02-03 05:14:50', NULL, NULL, '性别未知');
INSERT INTO `sys_dict_data` VALUES (4, '000000', 1, '显示', '0', 'sys_show_hide', '', 'primary', 'Y', 103, 1, '2026-02-03 05:14:50', NULL, NULL, '显示菜单');
INSERT INTO `sys_dict_data` VALUES (5, '000000', 2, '隐藏', '1', 'sys_show_hide', '', 'danger', 'N', 103, 1, '2026-02-03 05:14:50', NULL, NULL, '隐藏菜单');
INSERT INTO `sys_dict_data` VALUES (6, '000000', 1, '正常', '0', 'sys_normal_disable', '', 'primary', 'Y', 103, 1, '2026-02-03 05:14:50', NULL, NULL, '正常状态');
INSERT INTO `sys_dict_data` VALUES (7, '000000', 2, '停用', '1', 'sys_normal_disable', '', 'danger', 'N', 103, 1, '2026-02-03 05:14:51', NULL, NULL, '停用状态');
INSERT INTO `sys_dict_data` VALUES (12, '000000', 1, '是', 'Y', 'sys_yes_no', '', 'primary', 'Y', 103, 1, '2026-02-03 05:14:51', NULL, NULL, '系统默认是');
INSERT INTO `sys_dict_data` VALUES (13, '000000', 2, '否', 'N', 'sys_yes_no', '', 'danger', 'N', 103, 1, '2026-02-03 05:14:51', NULL, NULL, '系统默认否');
INSERT INTO `sys_dict_data` VALUES (14, '000000', 1, '通知', '1', 'sys_notice_type', '', 'warning', 'Y', 103, 1, '2026-02-03 05:14:51', NULL, NULL, '通知');
INSERT INTO `sys_dict_data` VALUES (15, '000000', 2, '公告', '2', 'sys_notice_type', '', 'success', 'N', 103, 1, '2026-02-03 05:14:51', NULL, NULL, '公告');
INSERT INTO `sys_dict_data` VALUES (16, '000000', 1, '正常', '0', 'sys_notice_status', '', 'primary', 'Y', 103, 1, '2026-02-03 05:14:51', NULL, NULL, '正常状态');
INSERT INTO `sys_dict_data` VALUES (17, '000000', 2, '关闭', '1', 'sys_notice_status', '', 'danger', 'N', 103, 1, '2026-02-03 05:14:51', NULL, NULL, '关闭状态');
INSERT INTO `sys_dict_data` VALUES (18, '000000', 1, '新增', '1', 'sys_oper_type', '', 'info', 'N', 103, 1, '2026-02-03 05:14:51', NULL, NULL, '新增操作');
INSERT INTO `sys_dict_data` VALUES (19, '000000', 2, '修改', '2', 'sys_oper_type', '', 'info', 'N', 103, 1, '2026-02-03 05:14:51', NULL, NULL, '修改操作');
INSERT INTO `sys_dict_data` VALUES (20, '000000', 3, '删除', '3', 'sys_oper_type', '', 'danger', 'N', 103, 1, '2026-02-03 05:14:51', NULL, NULL, '删除操作');
INSERT INTO `sys_dict_data` VALUES (21, '000000', 4, '授权', '4', 'sys_oper_type', '', 'primary', 'N', 103, 1, '2026-02-03 05:14:51', NULL, NULL, '授权操作');
INSERT INTO `sys_dict_data` VALUES (22, '000000', 5, '导出', '5', 'sys_oper_type', '', 'warning', 'N', 103, 1, '2026-02-03 05:14:51', NULL, NULL, '导出操作');
INSERT INTO `sys_dict_data` VALUES (23, '000000', 6, '导入', '6', 'sys_oper_type', '', 'warning', 'N', 103, 1, '2026-02-03 05:14:51', NULL, NULL, '导入操作');
INSERT INTO `sys_dict_data` VALUES (24, '000000', 7, '强退', '7', 'sys_oper_type', '', 'danger', 'N', 103, 1, '2026-02-03 05:14:51', NULL, NULL, '强退操作');
INSERT INTO `sys_dict_data` VALUES (25, '000000', 8, '生成代码', '8', 'sys_oper_type', '', 'warning', 'N', 103, 1, '2026-02-03 05:14:51', NULL, NULL, '生成操作');
INSERT INTO `sys_dict_data` VALUES (26, '000000', 9, '清空数据', '9', 'sys_oper_type', '', 'danger', 'N', 103, 1, '2026-02-03 05:14:51', NULL, NULL, '清空操作');
INSERT INTO `sys_dict_data` VALUES (27, '000000', 1, '成功', '0', 'sys_common_status', '', 'primary', 'N', 103, 1, '2026-02-03 05:14:51', NULL, NULL, '正常状态');
INSERT INTO `sys_dict_data` VALUES (28, '000000', 2, '失败', '1', 'sys_common_status', '', 'danger', 'N', 103, 1, '2026-02-03 05:14:51', NULL, NULL, '停用状态');
INSERT INTO `sys_dict_data` VALUES (29, '000000', 99, '其他', '0', 'sys_oper_type', '', 'info', 'N', 103, 1, '2026-02-03 05:14:51', NULL, NULL, '其他操作');
INSERT INTO `sys_dict_data` VALUES (30, '000000', 0, '密码认证', 'password', 'sys_grant_type', 'el-check-tag', 'default', 'N', 103, 1, '2026-02-03 05:14:51', NULL, NULL, '密码认证');
INSERT INTO `sys_dict_data` VALUES (31, '000000', 0, '短信认证', 'sms', 'sys_grant_type', 'el-check-tag', 'default', 'N', 103, 1, '2026-02-03 05:14:51', NULL, NULL, '短信认证');
INSERT INTO `sys_dict_data` VALUES (32, '000000', 0, '邮件认证', 'email', 'sys_grant_type', 'el-check-tag', 'default', 'N', 103, 1, '2026-02-03 05:14:51', NULL, NULL, '邮件认证');
INSERT INTO `sys_dict_data` VALUES (33, '000000', 0, '小程序认证', 'xcx', 'sys_grant_type', 'el-check-tag', 'default', 'N', 103, 1, '2026-02-03 05:14:51', NULL, NULL, '小程序认证');
INSERT INTO `sys_dict_data` VALUES (34, '000000', 0, '三方登录认证', 'social', 'sys_grant_type', 'el-check-tag', 'default', 'N', 103, 1, '2026-02-03 05:14:51', NULL, NULL, '三方登录认证');
INSERT INTO `sys_dict_data` VALUES (35, '000000', 0, 'PC', 'pc', 'sys_device_type', '', 'default', 'N', 103, 1, '2026-02-03 05:14:51', NULL, NULL, 'PC');
INSERT INTO `sys_dict_data` VALUES (36, '000000', 0, '安卓', 'android', 'sys_device_type', '', 'default', 'N', 103, 1, '2026-02-03 05:14:52', NULL, NULL, '安卓');
INSERT INTO `sys_dict_data` VALUES (37, '000000', 0, 'iOS', 'ios', 'sys_device_type', '', 'default', 'N', 103, 1, '2026-02-03 05:14:52', NULL, NULL, 'iOS');
INSERT INTO `sys_dict_data` VALUES (38, '000000', 0, '小程序', 'xcx', 'sys_device_type', '', 'default', 'N', 103, 1, '2026-02-03 05:14:52', NULL, NULL, '小程序');
INSERT INTO `sys_dict_data` VALUES (2018858143749115906, '154726', 1, '男', '0', 'sys_user_sex', '', '', 'Y', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '性别男');
INSERT INTO `sys_dict_data` VALUES (2018858143749115907, '154726', 2, '女', '1', 'sys_user_sex', '', '', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '性别女');
INSERT INTO `sys_dict_data` VALUES (2018858143749115908, '154726', 3, '未知', '2', 'sys_user_sex', '', '', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '性别未知');
INSERT INTO `sys_dict_data` VALUES (2018858143749115909, '154726', 1, '显示', '0', 'sys_show_hide', '', 'primary', 'Y', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '显示菜单');
INSERT INTO `sys_dict_data` VALUES (2018858143749115910, '154726', 2, '隐藏', '1', 'sys_show_hide', '', 'danger', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '隐藏菜单');
INSERT INTO `sys_dict_data` VALUES (2018858143749115911, '154726', 1, '正常', '0', 'sys_normal_disable', '', 'primary', 'Y', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '正常状态');
INSERT INTO `sys_dict_data` VALUES (2018858143749115912, '154726', 2, '停用', '1', 'sys_normal_disable', '', 'danger', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '停用状态');
INSERT INTO `sys_dict_data` VALUES (2018858143749115913, '154726', 1, '是', 'Y', 'sys_yes_no', '', 'primary', 'Y', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '系统默认是');
INSERT INTO `sys_dict_data` VALUES (2018858143753310209, '154726', 2, '否', 'N', 'sys_yes_no', '', 'danger', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '系统默认否');
INSERT INTO `sys_dict_data` VALUES (2018858143753310210, '154726', 1, '通知', '1', 'sys_notice_type', '', 'warning', 'Y', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '通知');
INSERT INTO `sys_dict_data` VALUES (2018858143753310211, '154726', 2, '公告', '2', 'sys_notice_type', '', 'success', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '公告');
INSERT INTO `sys_dict_data` VALUES (2018858143753310212, '154726', 1, '正常', '0', 'sys_notice_status', '', 'primary', 'Y', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '正常状态');
INSERT INTO `sys_dict_data` VALUES (2018858143753310213, '154726', 2, '关闭', '1', 'sys_notice_status', '', 'danger', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '关闭状态');
INSERT INTO `sys_dict_data` VALUES (2018858143753310214, '154726', 1, '新增', '1', 'sys_oper_type', '', 'info', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '新增操作');
INSERT INTO `sys_dict_data` VALUES (2018858143753310215, '154726', 2, '修改', '2', 'sys_oper_type', '', 'info', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '修改操作');
INSERT INTO `sys_dict_data` VALUES (2018858143753310216, '154726', 3, '删除', '3', 'sys_oper_type', '', 'danger', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '删除操作');
INSERT INTO `sys_dict_data` VALUES (2018858143753310217, '154726', 4, '授权', '4', 'sys_oper_type', '', 'primary', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '授权操作');
INSERT INTO `sys_dict_data` VALUES (2018858143753310218, '154726', 5, '导出', '5', 'sys_oper_type', '', 'warning', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '导出操作');
INSERT INTO `sys_dict_data` VALUES (2018858143753310219, '154726', 6, '导入', '6', 'sys_oper_type', '', 'warning', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '导入操作');
INSERT INTO `sys_dict_data` VALUES (2018858143753310220, '154726', 7, '强退', '7', 'sys_oper_type', '', 'danger', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '强退操作');
INSERT INTO `sys_dict_data` VALUES (2018858143753310221, '154726', 8, '生成代码', '8', 'sys_oper_type', '', 'warning', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '生成操作');
INSERT INTO `sys_dict_data` VALUES (2018858143753310222, '154726', 9, '清空数据', '9', 'sys_oper_type', '', 'danger', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '清空操作');
INSERT INTO `sys_dict_data` VALUES (2018858143757504514, '154726', 1, '成功', '0', 'sys_common_status', '', 'primary', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '正常状态');
INSERT INTO `sys_dict_data` VALUES (2018858143757504515, '154726', 2, '失败', '1', 'sys_common_status', '', 'danger', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '停用状态');
INSERT INTO `sys_dict_data` VALUES (2018858143757504516, '154726', 99, '其他', '0', 'sys_oper_type', '', 'info', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '其他操作');
INSERT INTO `sys_dict_data` VALUES (2018858143757504517, '154726', 0, '密码认证', 'password', 'sys_grant_type', 'el-check-tag', 'default', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '密码认证');
INSERT INTO `sys_dict_data` VALUES (2018858143757504518, '154726', 0, '短信认证', 'sms', 'sys_grant_type', 'el-check-tag', 'default', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '短信认证');
INSERT INTO `sys_dict_data` VALUES (2018858143757504519, '154726', 0, '邮件认证', 'email', 'sys_grant_type', 'el-check-tag', 'default', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '邮件认证');
INSERT INTO `sys_dict_data` VALUES (2018858143757504520, '154726', 0, '小程序认证', 'xcx', 'sys_grant_type', 'el-check-tag', 'default', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '小程序认证');
INSERT INTO `sys_dict_data` VALUES (2018858143757504521, '154726', 0, '三方登录认证', 'social', 'sys_grant_type', 'el-check-tag', 'default', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '三方登录认证');
INSERT INTO `sys_dict_data` VALUES (2018858143757504522, '154726', 0, 'PC', 'pc', 'sys_device_type', '', 'default', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', 'PC');
INSERT INTO `sys_dict_data` VALUES (2018858143761698817, '154726', 0, '安卓', 'android', 'sys_device_type', '', 'default', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '安卓');
INSERT INTO `sys_dict_data` VALUES (2018858143761698818, '154726', 0, 'iOS', 'ios', 'sys_device_type', '', 'default', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', 'iOS');
INSERT INTO `sys_dict_data` VALUES (2018858143761698819, '154726', 0, '小程序', 'xcx', 'sys_device_type', '', 'default', 'N', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '小程序');

-- ----------------------------
-- Table structure for sys_dict_type
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type`  (
                                  `dict_id` bigint NOT NULL COMMENT '字典主键',
                                  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户编号',
                                  `dict_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '字典名称',
                                  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '字典类型',
                                  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                                  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                                  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                                  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
                                  PRIMARY KEY (`dict_id`) USING BTREE,
                                  UNIQUE INDEX `tenant_id`(`tenant_id` ASC, `dict_type` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '字典类型表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_dict_type
-- ----------------------------
INSERT INTO `sys_dict_type` VALUES (1, '000000', '用户性别', 'sys_user_sex', 103, 1, '2026-02-03 05:14:50', NULL, NULL, '用户性别列表');
INSERT INTO `sys_dict_type` VALUES (2, '000000', '菜单状态', 'sys_show_hide', 103, 1, '2026-02-03 05:14:50', NULL, NULL, '菜单状态列表');
INSERT INTO `sys_dict_type` VALUES (3, '000000', '系统开关', 'sys_normal_disable', 103, 1, '2026-02-03 05:14:50', NULL, NULL, '系统开关列表');
INSERT INTO `sys_dict_type` VALUES (6, '000000', '系统是否', 'sys_yes_no', 103, 1, '2026-02-03 05:14:50', NULL, NULL, '系统是否列表');
INSERT INTO `sys_dict_type` VALUES (7, '000000', '通知类型', 'sys_notice_type', 103, 1, '2026-02-03 05:14:50', NULL, NULL, '通知类型列表');
INSERT INTO `sys_dict_type` VALUES (8, '000000', '通知状态', 'sys_notice_status', 103, 1, '2026-02-03 05:14:50', NULL, NULL, '通知状态列表');
INSERT INTO `sys_dict_type` VALUES (9, '000000', '操作类型', 'sys_oper_type', 103, 1, '2026-02-03 05:14:50', NULL, NULL, '操作类型列表');
INSERT INTO `sys_dict_type` VALUES (10, '000000', '系统状态', 'sys_common_status', 103, 1, '2026-02-03 05:14:50', NULL, NULL, '登录状态列表');
INSERT INTO `sys_dict_type` VALUES (11, '000000', '授权类型', 'sys_grant_type', 103, 1, '2026-02-03 05:14:50', NULL, NULL, '认证授权类型');
INSERT INTO `sys_dict_type` VALUES (12, '000000', '设备类型', 'sys_device_type', 103, 1, '2026-02-03 05:14:50', NULL, NULL, '客户端设备类型');
INSERT INTO `sys_dict_type` VALUES (2018858143719755777, '154726', '用户性别', 'sys_user_sex', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '用户性别列表');
INSERT INTO `sys_dict_type` VALUES (2018858143719755778, '154726', '菜单状态', 'sys_show_hide', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '菜单状态列表');
INSERT INTO `sys_dict_type` VALUES (2018858143723950081, '154726', '系统开关', 'sys_normal_disable', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '系统开关列表');
INSERT INTO `sys_dict_type` VALUES (2018858143723950082, '154726', '系统是否', 'sys_yes_no', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '系统是否列表');
INSERT INTO `sys_dict_type` VALUES (2018858143723950083, '154726', '通知类型', 'sys_notice_type', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '通知类型列表');
INSERT INTO `sys_dict_type` VALUES (2018858143723950084, '154726', '通知状态', 'sys_notice_status', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '通知状态列表');
INSERT INTO `sys_dict_type` VALUES (2018858143723950085, '154726', '操作类型', 'sys_oper_type', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '操作类型列表');
INSERT INTO `sys_dict_type` VALUES (2018858143723950086, '154726', '系统状态', 'sys_common_status', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '登录状态列表');
INSERT INTO `sys_dict_type` VALUES (2018858143723950087, '154726', '授权类型', 'sys_grant_type', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '认证授权类型');
INSERT INTO `sys_dict_type` VALUES (2018858143723950088, '154726', '设备类型', 'sys_device_type', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', '客户端设备类型');

-- ----------------------------
-- Table structure for sys_logininfor
-- ----------------------------
DROP TABLE IF EXISTS `sys_logininfor`;
CREATE TABLE `sys_logininfor`  (
                                   `info_id` bigint NOT NULL COMMENT '访问ID',
                                   `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户编号',
                                   `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '用户账号',
                                   `client_key` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '客户端',
                                   `device_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '设备类型',
                                   `ipaddr` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '登录IP地址',
                                   `login_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '登录地点',
                                   `browser` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '浏览器类型',
                                   `os` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '操作系统',
                                   `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
                                   `msg` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '提示消息',
                                   `login_time` datetime NULL DEFAULT NULL COMMENT '访问时间',
                                   PRIMARY KEY (`info_id`) USING BTREE,
                                   INDEX `idx_sys_logininfor_s`(`status` ASC) USING BTREE,
                                   INDEX `idx_sys_logininfor_lt`(`login_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统访问记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_logininfor
-- ----------------------------
INSERT INTO `sys_logininfor` VALUES (2018437160497668098, '000000', 'admin', 'web', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 05:31:34');
INSERT INTO `sys_logininfor` VALUES (2018489328494317570, '000000', 'admin', 'web', 'pc', '58.56.198.114', '中国|山东省|泰安市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 08:58:52');
INSERT INTO `sys_logininfor` VALUES (2018490223370047490, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 09:02:26');
INSERT INTO `sys_logininfor` VALUES (2018494821124149250, '000000', 'admin', 'web', 'pc', '180.165.21.147', '中国|上海|上海市|电信', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 09:20:42');
INSERT INTO `sys_logininfor` VALUES (2018495323429801985, '000000', 'admin', 'web', 'pc', '218.1.209.117', '中国|上海|上海市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 09:22:42');
INSERT INTO `sys_logininfor` VALUES (2018496463106084866, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 09:27:13');
INSERT INTO `sys_logininfor` VALUES (2018506873276338177, '000000', 'admin', 'web', 'pc', '14.155.110.230', '中国|广东省|深圳市|电信', 'Chrome', 'OSX', '0', 'Login successful', '2026-02-03 10:08:35');
INSERT INTO `sys_logininfor` VALUES (2018507142684872705, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'OSX', '0', '登录成功', '2026-02-03 10:09:40');
INSERT INTO `sys_logininfor` VALUES (2018507255759114241, '000000', 'admin', 'web', 'pc', '27.156.68.14', '中国|福建省|福州市|电信', 'Chrome', 'OSX', '0', 'Login successful', '2026-02-03 10:10:06');
INSERT INTO `sys_logininfor` VALUES (2018507799294775297, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 10:12:16');
INSERT INTO `sys_logininfor` VALUES (2018507878344822786, '000000', 'admin', 'web', 'pc', '39.78.246.253', '中国|山东省|济南市|联通', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 10:12:35');
INSERT INTO `sys_logininfor` VALUES (2018508087959359489, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 10:13:25');
INSERT INTO `sys_logininfor` VALUES (2018513467045187586, '000000', 'admin', 'web', 'pc', '111.175.56.210', '中国|湖北省|武汉市|电信', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 10:34:47');
INSERT INTO `sys_logininfor` VALUES (2018515864068952066, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 10:44:19');
INSERT INTO `sys_logininfor` VALUES (2018516837059399682, '000000', 'admin', 'web', 'pc', '115.236.45.35', '中国|浙江省|杭州市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 10:48:11');
INSERT INTO `sys_logininfor` VALUES (2018517630747545602, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 10:51:20');
INSERT INTO `sys_logininfor` VALUES (2018530038144700418, '000000', 'admin', 'web', 'pc', '116.128.248.194', '中国|湖南省|长沙市|联通', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 11:40:38');
INSERT INTO `sys_logininfor` VALUES (2018531663223590914, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 11:47:06');
INSERT INTO `sys_logininfor` VALUES (2018534417249734658, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 11:58:02');
INSERT INTO `sys_logininfor` VALUES (2018537768200835074, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 12:11:21');
INSERT INTO `sys_logininfor` VALUES (2018537900388519937, '000000', 'admin', 'web', 'pc', '183.54.238.100', '中国|广东省|广州市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 12:11:53');
INSERT INTO `sys_logininfor` VALUES (2018549455184334849, '000000', 'admin', 'web', 'pc', '13.212.58.4', '美国|康涅狄格|亚马逊', 'Chrome', 'OSX', '0', 'Login successful', '2026-02-03 12:57:48');
INSERT INTO `sys_logininfor` VALUES (2018554371911061506, '000000', 'admin', 'web', 'pc', '61.182.224.157', '中国|河北省|石家庄市|联通', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 13:17:20');
INSERT INTO `sys_logininfor` VALUES (2018560767549378562, '000000', 'admin', 'web', 'pc', '106.39.125.218', '中国|北京|北京市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 13:42:45');
INSERT INTO `sys_logininfor` VALUES (2018567861803552769, '000000', 'admin', 'web', 'pc', '58.16.14.89', '中国|贵州省|贵阳市|联通', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 14:10:56');
INSERT INTO `sys_logininfor` VALUES (2018568178360258561, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 14:12:12');
INSERT INTO `sys_logininfor` VALUES (2018571219020943362, '000000', 'admin', 'web', 'pc', '116.169.71.139', '中国|辽宁省|威瑞森', 'Chrome', 'OSX', '0', 'Login successful', '2026-02-03 14:24:17');
INSERT INTO `sys_logininfor` VALUES (2018574610824564737, '000000', 'admin', 'web', 'pc', '113.13.108.124', '中国|广西|柳州市|电信', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 14:37:45');
INSERT INTO `sys_logininfor` VALUES (2018574668861149186, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 14:37:59');
INSERT INTO `sys_logininfor` VALUES (2018577941697531906, '000000', 'admin', 'web', 'pc', '36.110.12.226', '中国|北京|北京市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 14:50:59');
INSERT INTO `sys_logininfor` VALUES (2018578431793565697, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'OSX', '0', '登录成功', '2026-02-03 14:52:56');
INSERT INTO `sys_logininfor` VALUES (2018582006717775874, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 15:07:08');
INSERT INTO `sys_logininfor` VALUES (2018582568484605953, '000000', 'admin', 'web', 'pc', '223.104.43.1', '中国|北京|北京市|移动', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 15:09:22');
INSERT INTO `sys_logininfor` VALUES (2018585018155274242, '000000', 'admin', 'web', 'pc', '116.169.127.68', '中国|辽宁省|威瑞森', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 15:19:06');
INSERT INTO `sys_logininfor` VALUES (2018586528248791041, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 15:25:07');
INSERT INTO `sys_logininfor` VALUES (2018586925091393537, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 15:26:41');
INSERT INTO `sys_logininfor` VALUES (2018587111276548097, '000000', 'admin', 'web', 'pc', '117.159.171.116', '中国|河南省|焦作市|移动', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 15:27:26');
INSERT INTO `sys_logininfor` VALUES (2018588210859479041, '000000', 'admin', 'web', 'pc', '61.169.93.182', '中国|上海|上海市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 15:31:48');
INSERT INTO `sys_logininfor` VALUES (2018588453218947074, '000000', 'admin', 'web', 'pc', '61.133.210.59', '中国|宁夏|银川市|电信', 'MSEdge', 'Windows 10 or Windows Server 2016', '1', 'Password input error 1 times', '2026-02-03 15:32:45');
INSERT INTO `sys_logininfor` VALUES (2018588518431985666, '000000', 'admin', 'web', 'pc', '61.133.210.59', '中国|宁夏|银川市|电信', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 15:33:01');
INSERT INTO `sys_logininfor` VALUES (2018588841208844289, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 15:34:18');
INSERT INTO `sys_logininfor` VALUES (2018589986920730625, '000000', 'admin', 'web', 'pc', '115.236.69.226', '中国|浙江省|杭州市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 15:38:51');
INSERT INTO `sys_logininfor` VALUES (2018590616590618625, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 15:41:21');
INSERT INTO `sys_logininfor` VALUES (2018593068400381953, '000000', 'admin', 'web', 'pc', '180.123.251.84', '中国|江苏省|徐州市|电信', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 15:51:06');
INSERT INTO `sys_logininfor` VALUES (2018594019760803842, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 15:54:53');
INSERT INTO `sys_logininfor` VALUES (2018595653618372609, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 16:01:22');
INSERT INTO `sys_logininfor` VALUES (2018596016505360386, '000000', 'admin', 'web', 'pc', '114.242.16.161', '中国|北京|北京市|联通', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 16:02:49');
INSERT INTO `sys_logininfor` VALUES (2018596170675392513, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 16:03:25');
INSERT INTO `sys_logininfor` VALUES (2018599719354372098, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 16:17:32');
INSERT INTO `sys_logininfor` VALUES (2018599739910656001, '000000', 'admin', 'web', 'pc', '42.235.239.253', '中国|河南省|新乡市|联通', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 16:17:36');
INSERT INTO `sys_logininfor` VALUES (2018600606151872513, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 16:21:03');
INSERT INTO `sys_logininfor` VALUES (2018600933580214274, '000000', 'admin', 'web', 'pc', '219.142.141.194', '中国|北京|北京市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 16:22:21');
INSERT INTO `sys_logininfor` VALUES (2018602589315272705, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'OSX', '0', '登录成功', '2026-02-03 16:28:56');
INSERT INTO `sys_logininfor` VALUES (2018605159794479105, '000000', 'admin', 'web', 'pc', '222.168.89.242', '中国|吉林省|长春市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 16:39:09');
INSERT INTO `sys_logininfor` VALUES (2018609628598898690, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'OSX', '0', '登录成功', '2026-02-03 16:56:54');
INSERT INTO `sys_logininfor` VALUES (2018609656977559553, '000000', 'admin', 'web', 'pc', '121.35.46.195', '中国|广东省|深圳市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 16:57:01');
INSERT INTO `sys_logininfor` VALUES (2018610104228777985, '000000', 'admin', 'web', 'pc', '115.236.69.226', '中国|浙江省|杭州市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 16:58:47');
INSERT INTO `sys_logininfor` VALUES (2018610284827119618, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 16:59:31');
INSERT INTO `sys_logininfor` VALUES (2018611513854660610, '000000', 'admin ', 'web', 'pc', '222.90.12.243', '中国|陕西省|西安市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '1', 'Password input error 1 times', '2026-02-03 17:04:24');
INSERT INTO `sys_logininfor` VALUES (2018611519181426690, '000000', 'admin ', 'web', 'pc', '222.90.12.243', '中国|陕西省|西安市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '1', 'Password input error 2 times', '2026-02-03 17:04:25');
INSERT INTO `sys_logininfor` VALUES (2018611644431732738, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 17:04:55');
INSERT INTO `sys_logininfor` VALUES (2018611724014456833, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 17:05:14');
INSERT INTO `sys_logininfor` VALUES (2018612993819021314, '000000', 'admin', 'web', 'pc', '223.160.130.33', '中国|北京|北京市|广电', 'Chrome', 'OSX', '0', 'Login successful', '2026-02-03 17:10:16');
INSERT INTO `sys_logininfor` VALUES (2018614987845668866, '000000', 'admin ', 'web', 'pc', '222.90.12.243', '中国|陕西省|西安市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '1', 'Password input error 1 times', '2026-02-03 17:18:12');
INSERT INTO `sys_logininfor` VALUES (2018615005365276674, '000000', 'admin', 'web', 'pc', '222.90.12.243', '中国|陕西省|西安市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 17:18:16');
INSERT INTO `sys_logininfor` VALUES (2018615245262688258, '000000', 'admin', 'web', 'pc', '61.184.94.104', '中国|湖北省|十堰市|电信', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 17:19:13');
INSERT INTO `sys_logininfor` VALUES (2018615451685359617, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 17:20:02');
INSERT INTO `sys_logininfor` VALUES (2018615588662939650, '000000', 'admin', 'web', 'pc', '183.241.255.169', '中国|北京|北京市|移动', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 17:20:35');
INSERT INTO `sys_logininfor` VALUES (2018615733957824514, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 17:21:10');
INSERT INTO `sys_logininfor` VALUES (2018618361873829889, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 17:31:36');
INSERT INTO `sys_logininfor` VALUES (2018619606214774785, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 17:36:33');
INSERT INTO `sys_logininfor` VALUES (2018621042193469441, '000000', 'admin', 'web', 'pc', '27.227.167.177', '中国|甘肃省|兰州市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 17:42:15');
INSERT INTO `sys_logininfor` VALUES (2018623610328059905, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 17:52:28');
INSERT INTO `sys_logininfor` VALUES (2018623677315289089, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'OSX', '0', '登录成功', '2026-02-03 17:52:44');
INSERT INTO `sys_logininfor` VALUES (2018623870995664897, '000000', 'admin', 'web', 'pc', '59.57.134.162', '中国|福建省|厦门市|电信', 'Chrome', 'OSX', '0', 'Login successful', '2026-02-03 17:53:30');
INSERT INTO `sys_logininfor` VALUES (2018624044400775169, '000000', 'admin', 'web', 'pc', '39.162.49.29', '中国|河南省|郑州市|移动', 'Chrome', 'Windows 10 or Windows Server 2016', '1', 'Password input error 1 times', '2026-02-03 17:54:11');
INSERT INTO `sys_logininfor` VALUES (2018624054987198465, '000000', 'admin', 'web', 'pc', '39.162.49.29', '中国|河南省|郑州市|移动', 'Chrome', 'Windows 10 or Windows Server 2016', '1', 'Password input error 2 times', '2026-02-03 17:54:14');
INSERT INTO `sys_logininfor` VALUES (2018624077900681218, '000000', 'admin', 'web', 'pc', '39.162.49.29', '中国|河南省|郑州市|移动', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 17:54:19');
INSERT INTO `sys_logininfor` VALUES (2018625637493903362, '000000', 'admin', 'web', 'pc', '114.253.10.114', '中国|北京|北京市|联通', 'Chrome', 'OSX', '0', 'Login successful', '2026-02-03 18:00:31');
INSERT INTO `sys_logininfor` VALUES (2018625743718846465, '000000', 'admin', 'web', 'pc', '111.198.137.189', '中国|北京|北京市|联通', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 18:00:56');
INSERT INTO `sys_logininfor` VALUES (2018625749372768258, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'OSX', '0', '登录成功', '2026-02-03 18:00:58');
INSERT INTO `sys_logininfor` VALUES (2018625771480944641, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 18:01:03');
INSERT INTO `sys_logininfor` VALUES (2018625852410040321, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'OSX', '0', '退出成功', '2026-02-03 18:01:22');
INSERT INTO `sys_logininfor` VALUES (2018625877240320002, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'OSX', '0', '登录成功', '2026-02-03 18:01:28');
INSERT INTO `sys_logininfor` VALUES (2018627882260238337, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 18:09:26');
INSERT INTO `sys_logininfor` VALUES (2018628150670528513, '000000', 'admin', 'web', 'pc', '114.253.10.114', '中国|北京|北京市|联通', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 18:10:30');
INSERT INTO `sys_logininfor` VALUES (2018672955786137601, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 21:08:32');
INSERT INTO `sys_logininfor` VALUES (2018689142062452738, '000000', 'admin ', 'web', 'pc', '221.205.106.23', '中国|山西省|太原市|联通', 'Chrome', 'Windows 10 or Windows Server 2016', '1', 'Password input error 1 times', '2026-02-03 22:12:52');
INSERT INTO `sys_logininfor` VALUES (2018689178624200706, '000000', 'admin', 'web', 'pc', '221.205.106.23', '中国|山西省|太原市|联通', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 22:13:00');
INSERT INTO `sys_logininfor` VALUES (2018689429430996993, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 22:14:00');
INSERT INTO `sys_logininfor` VALUES (2018693323410247681, '000000', 'admin', 'web', 'pc', '140.206.143.69', '中国|上海|上海市|联通', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 22:29:28');
INSERT INTO `sys_logininfor` VALUES (2018694798878314498, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'OSX', '0', '登录成功', '2026-02-03 22:35:20');
INSERT INTO `sys_logininfor` VALUES (2018710386166075394, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 23:37:17');
INSERT INTO `sys_logininfor` VALUES (2018715692476534785, '000000', 'admin', 'web', 'pc', '113.248.49.12', '中国|重庆|重庆市|电信', 'Firefox', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-03 23:58:22');
INSERT INTO `sys_logininfor` VALUES (2018715768049504257, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Firefox', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-03 23:58:40');
INSERT INTO `sys_logininfor` VALUES (2018745408302485505, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 01:56:26');
INSERT INTO `sys_logininfor` VALUES (2018745476409593857, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '退出成功', '2026-02-04 01:56:43');
INSERT INTO `sys_logininfor` VALUES (2018852568944480258, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'OSX', '0', '登录成功', '2026-02-04 09:02:16');
INSERT INTO `sys_logininfor` VALUES (2018852760607395842, '000000', 'admin', 'web', 'pc', '39.170.0.61', '中国|浙江省|杭州市|移动', 'Chrome', 'OSX', '0', 'Login successful', '2026-02-04 09:03:01');
INSERT INTO `sys_logininfor` VALUES (2018855559080579073, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 09:14:08');
INSERT INTO `sys_logininfor` VALUES (2018856680327090177, '000000', 'admin', 'web', 'pc', '180.201.162.34', '中国|山东省|青岛市|教育网', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 09:18:36');
INSERT INTO `sys_logininfor` VALUES (2018856825580032001, '000000', 'admin', 'web', 'pc', '60.10.20.65', '中国|河北省|廊坊市|联通', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 09:19:10');
INSERT INTO `sys_logininfor` VALUES (2018857125414047745, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 09:20:22');
INSERT INTO `sys_logininfor` VALUES (2018857183823925249, '000000', 'admin', 'web', 'pc', '114.253.10.114', '中国|北京|北京市|联通', 'Chrome', 'OSX', '0', 'Login successful', '2026-02-04 09:20:36');
INSERT INTO `sys_logininfor` VALUES (2018857493854294017, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'OSX', '0', '登录成功', '2026-02-04 09:21:50');
INSERT INTO `sys_logininfor` VALUES (2018860201030062082, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 09:32:35');
INSERT INTO `sys_logininfor` VALUES (2018860399353532418, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 09:33:22');
INSERT INTO `sys_logininfor` VALUES (2018860419058372609, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 09:33:27');
INSERT INTO `sys_logininfor` VALUES (2018862938719391745, '000000', 'admin', 'web', 'pc', '113.65.15.168', '中国|广东省|广州市|电信', 'Firefox', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 09:43:28');
INSERT INTO `sys_logininfor` VALUES (2018863017278705665, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Firefox', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 09:43:47');
INSERT INTO `sys_logininfor` VALUES (2018863424306548737, '000000', 'admin', 'web', 'pc', '114.253.10.114', '中国|北京|北京市|联通', 'Chrome', 'OSX', '0', 'Login successful', '2026-02-04 09:45:24');
INSERT INTO `sys_logininfor` VALUES (2018864538947031042, '000000', 'admin', 'web', 'pc', '114.224.36.132', '中国|江苏省|无锡市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 09:49:49');
INSERT INTO `sys_logininfor` VALUES (2018864646790975490, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 09:50:15');
INSERT INTO `sys_logininfor` VALUES (2018864913326411777, '000000', 'admin', 'web', 'pc', '14.220.151.192', '中国|广东省|东莞市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 09:51:19');
INSERT INTO `sys_logininfor` VALUES (2018865224053035009, '000000', 'admin', 'web', 'pc', '8.220.210.62', '中国|阿里巴巴', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 09:52:33');
INSERT INTO `sys_logininfor` VALUES (2018865354428780546, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 09:53:04');
INSERT INTO `sys_logininfor` VALUES (2018865432400891905, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 09:53:22');
INSERT INTO `sys_logininfor` VALUES (2018865469902163970, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '退出成功', '2026-02-04 09:53:31');
INSERT INTO `sys_logininfor` VALUES (2018865485031018498, '154726', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 09:53:35');
INSERT INTO `sys_logininfor` VALUES (2018865600516984833, '154726', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '退出成功', '2026-02-04 09:54:03');
INSERT INTO `sys_logininfor` VALUES (2018865826023739394, '000000', 'admin', 'web', 'pc', '113.104.237.0', '中国|广东省|深圳市|电信', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 09:54:56');
INSERT INTO `sys_logininfor` VALUES (2018865939207032834, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 09:55:23');
INSERT INTO `sys_logininfor` VALUES (2018868158258089986, '000000', 'admin', 'web', 'pc', '39.170.71.231', '中国|浙江省|杭州市|移动', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 10:04:12');
INSERT INTO `sys_logininfor` VALUES (2018868610617970690, '000000', 'admin', 'web', 'pc', '219.152.39.216', '中国|重庆|重庆市|电信', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 10:06:00');
INSERT INTO `sys_logininfor` VALUES (2018870568124813313, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 10:13:47');
INSERT INTO `sys_logininfor` VALUES (2018882173784952834, '000000', 'admin', 'web', 'pc', '220.196.184.73', '中国|上海|上海市|联通', 'Chrome', 'Windows 10 or Windows Server 2016', '1', 'Password input error 1 times', '2026-02-04 10:59:54');
INSERT INTO `sys_logininfor` VALUES (2018882179346599938, '000000', 'admin', 'web', 'pc', '220.196.184.73', '中国|上海|上海市|联通', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 10:59:55');
INSERT INTO `sys_logininfor` VALUES (2018882457076633602, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 11:01:01');
INSERT INTO `sys_logininfor` VALUES (2018882886007132162, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 11:02:44');
INSERT INTO `sys_logininfor` VALUES (2018883626641526786, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Firefox', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 11:05:40');
INSERT INTO `sys_logininfor` VALUES (2018885851870793730, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 11:14:31');
INSERT INTO `sys_logininfor` VALUES (2018887170559971330, '000000', 'admin', 'web', 'pc', '114.222.24.76', '中国|江苏省|南京市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 11:19:45');
INSERT INTO `sys_logininfor` VALUES (2018887913358626817, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 11:22:42');
INSERT INTO `sys_logininfor` VALUES (2018889490362404865, '000000', 'admin', 'web', 'pc', '124.165.224.106', '中国|山西省|吕梁市|联通', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 11:28:58');
INSERT INTO `sys_logininfor` VALUES (2018891240427360257, '000000', 'admin', 'web', 'pc', '163.142.243.239', '中国|广东省|佛山市|联通', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 11:35:56');
INSERT INTO `sys_logininfor` VALUES (2018895246847512578, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 11:51:51');
INSERT INTO `sys_logininfor` VALUES (2018897167293485058, '000000', 'admin', 'web', 'pc', '113.57.48.151', '中国|湖北省|武汉市|联通', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 11:59:29');
INSERT INTO `sys_logininfor` VALUES (2018898894247825410, '000000', 'admin', 'web', 'pc', '114.254.1.2', '中国|北京|北京市|联通', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 12:06:20');
INSERT INTO `sys_logininfor` VALUES (2018899094601338882, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 12:07:08');
INSERT INTO `sys_logininfor` VALUES (2018916480444403714, '000000', 'admin', 'web', 'pc', '219.143.206.106', '中国|北京|北京市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 13:16:13');
INSERT INTO `sys_logininfor` VALUES (2018916794622939137, '000000', 'admin', 'web', 'pc', '116.252.72.152', '中国|广西|南宁市|电信', 'Chrome', 'OSX', '0', 'Login successful', '2026-02-04 13:17:28');
INSERT INTO `sys_logininfor` VALUES (2018917938204119041, '000000', 'admin', 'web', 'pc', '219.143.206.106', '中国|北京|北京市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 13:22:01');
INSERT INTO `sys_logininfor` VALUES (2018918018160136193, '000000', 'admin', 'web', 'pc', '218.240.181.5', '中国|北京|北京市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 13:22:20');
INSERT INTO `sys_logininfor` VALUES (2018918326378565634, '000000', 'admin', 'web', 'pc', '218.240.181.5', '中国|北京|北京市|电信', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 13:23:33');
INSERT INTO `sys_logininfor` VALUES (2018920804255928322, '000000', 'admin', 'web', 'pc', '219.143.206.106', '中国|北京|北京市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 13:33:24');
INSERT INTO `sys_logininfor` VALUES (2018921548166074369, '000000', 'admin', 'web', 'pc', '219.143.206.106', '中国|北京|北京市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 13:36:21');
INSERT INTO `sys_logininfor` VALUES (2018922995012210690, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Quark', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 13:42:06');
INSERT INTO `sys_logininfor` VALUES (2018926354117038082, '000000', 'admin', 'web', 'pc', '8.220.210.62', '中国|阿里巴巴', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 13:55:27');
INSERT INTO `sys_logininfor` VALUES (2018927011129593858, '000000', 'admin', 'web', 'pc', '60.190.252.115', '中国|浙江省|杭州市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 13:58:04');
INSERT INTO `sys_logininfor` VALUES (2018927468619108353, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 13:59:53');
INSERT INTO `sys_logininfor` VALUES (2018927515784056834, '000000', 'admin', 'web', 'pc', '14.111.243.245', '中国|重庆|重庆市|电信', 'MSEdge', 'Windows 10 or Windows Server 2016', '1', 'Password input error 1 times', '2026-02-04 14:00:04');
INSERT INTO `sys_logininfor` VALUES (2018927562521186305, '000000', 'admin', 'web', 'pc', '14.111.243.245', '中国|重庆|重庆市|电信', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 14:00:15');
INSERT INTO `sys_logininfor` VALUES (2018929485043339265, '000000', 'admin', 'web', 'pc', '124.114.150.254', '中国|陕西省|西安市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 14:07:54');
INSERT INTO `sys_logininfor` VALUES (2018931277877612545, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 14:15:01');
INSERT INTO `sys_logininfor` VALUES (2018933698284621826, '000000', 'admin', 'web', 'pc', '222.35.11.50', '中国|北京|北京市|铁通', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 14:24:38');
INSERT INTO `sys_logininfor` VALUES (2018933723182010369, '000000', 'admin', 'web', 'pc', '183.162.217.80', '中国|安徽省|蚌埠市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 14:24:44');
INSERT INTO `sys_logininfor` VALUES (2018933918011625473, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 14:25:31');
INSERT INTO `sys_logininfor` VALUES (2018935595993272322, '000000', 'admin', 'web', 'pc', '122.225.239.202', '中国|浙江省|嘉兴市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 14:32:11');
INSERT INTO `sys_logininfor` VALUES (2018937821423865857, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 14:41:01');
INSERT INTO `sys_logininfor` VALUES (2018938936521527298, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 14:45:27');
INSERT INTO `sys_logininfor` VALUES (2018946088032145409, '000000', 'admin', 'web', 'pc', '223.104.202.88', '中国|陕西省|咸阳市|移动', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 15:13:52');
INSERT INTO `sys_logininfor` VALUES (2018946463766286337, '000000', 'admin', 'web', 'pc', '114.139.48.241', '中国|贵州省|遵义市|电信', 'Chrome', 'OSX', '0', 'Login successful', '2026-02-04 15:15:22');
INSERT INTO `sys_logininfor` VALUES (2018948323336130562, '000000', 'admin', 'web', 'pc', '113.207.43.98', '中国|重庆|重庆市|联通', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 15:22:45');
INSERT INTO `sys_logininfor` VALUES (2018948839856279554, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 15:24:48');
INSERT INTO `sys_logininfor` VALUES (2018950286861799426, '000000', 'admin', 'web', 'pc', '112.224.162.237', '中国|山东省|联通', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 15:30:33');
INSERT INTO `sys_logininfor` VALUES (2018950904724721665, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 15:33:01');
INSERT INTO `sys_logininfor` VALUES (2018950963323342849, '000000', 'admin', 'web', 'pc', '182.132.201.232', '中国|四川省|眉山市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 15:33:15');
INSERT INTO `sys_logininfor` VALUES (2018950986727559170, '000000', 'admin', 'web', 'pc', '111.8.48.211', '中国|湖南省|长沙市|移动', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 15:33:20');
INSERT INTO `sys_logininfor` VALUES (2018951848111771650, '000000', 'admin', 'web', 'pc', '219.138.228.254', '中国|湖北省|鄂州市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 15:36:46');
INSERT INTO `sys_logininfor` VALUES (2018952415047454722, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 15:39:01');
INSERT INTO `sys_logininfor` VALUES (2018954681871634433, '000000', 'admin', 'web', 'pc', '123.235.153.115', '中国|山东省|青岛市|联通', 'MicroMessenger', 'OSX', '0', 'Login successful', '2026-02-04 15:48:01');
INSERT INTO `sys_logininfor` VALUES (2018959635684397057, '000000', 'admin', 'web', 'pc', '58.56.198.114', '中国|山东省|泰安市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 16:07:42');
INSERT INTO `sys_logininfor` VALUES (2018961274495438849, '000000', 'admin', 'web', 'pc', '120.197.21.117', '中国|广东省|广州市|移动', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 16:14:13');
INSERT INTO `sys_logininfor` VALUES (2018961365956431873, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 16:14:35');
INSERT INTO `sys_logininfor` VALUES (2018961449058177025, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 16:14:55');
INSERT INTO `sys_logininfor` VALUES (2018961841162686465, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 16:16:28');
INSERT INTO `sys_logininfor` VALUES (2018961976873586689, '000000', 'admin', 'web', 'pc', '36.112.184.220', '中国|北京|北京市|电信', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 16:17:00');
INSERT INTO `sys_logininfor` VALUES (2018962175272554497, '000000', 'admin', 'web', 'pc', '39.144.212.209', '中国|移动', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 16:17:48');
INSERT INTO `sys_logininfor` VALUES (2018962369447858177, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 16:18:34');
INSERT INTO `sys_logininfor` VALUES (2018963149303189505, '000000', 'admin', 'web', 'pc', '112.97.202.126', '中国|广东省|东莞市|联通', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 16:21:40');
INSERT INTO `sys_logininfor` VALUES (2018963440199143425, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 16:22:49');
INSERT INTO `sys_logininfor` VALUES (2018966187199827970, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '退出成功', '2026-02-04 16:33:44');
INSERT INTO `sys_logininfor` VALUES (2018966195156422657, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 16:33:46');
INSERT INTO `sys_logininfor` VALUES (2018967272132055041, '000000', 'admin', 'web', 'pc', '121.8.154.218', '中国|广东省|广州市|电信', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 16:38:03');
INSERT INTO `sys_logininfor` VALUES (2018967970655637505, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 16:40:49');
INSERT INTO `sys_logininfor` VALUES (2018968030697099265, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 16:41:04');
INSERT INTO `sys_logininfor` VALUES (2018970160656945154, '000000', 'admin', 'web', 'pc', '183.209.146.73', '中国|江苏省|南京市|移动', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 16:49:32');
INSERT INTO `sys_logininfor` VALUES (2018970873374052354, '000000', 'admin', 'web', 'pc', '60.1.71.196', '中国|河北省|石家庄市|联通', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 16:52:22');
INSERT INTO `sys_logininfor` VALUES (2018970947504181249, '000000', 'admin', 'web', 'pc', '223.88.78.216', '中国|河南省|郑州市|移动', 'Chrome', 'Windows 10 or Windows Server 2016', '0', 'Login successful', '2026-02-04 16:52:39');
INSERT INTO `sys_logininfor` VALUES (2018971098872418305, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 16:53:15');
INSERT INTO `sys_logininfor` VALUES (2018978580902580226, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 17:22:59');
INSERT INTO `sys_logininfor` VALUES (2018978998839808002, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 17:24:39');
INSERT INTO `sys_logininfor` VALUES (2018984098123616257, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 17:44:55');
INSERT INTO `sys_logininfor` VALUES (2018988627267293186, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 18:02:54');
INSERT INTO `sys_logininfor` VALUES (2018990733382520834, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 18:11:17');
INSERT INTO `sys_logininfor` VALUES (2019029939576246274, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 20:47:04');
INSERT INTO `sys_logininfor` VALUES (2019040366678904834, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 21:28:30');
INSERT INTO `sys_logininfor` VALUES (2019057692774109186, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 22:37:21');
INSERT INTO `sys_logininfor` VALUES (2019057715637260289, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-04 22:37:26');
INSERT INTO `sys_logininfor` VALUES (2019113073072279553, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'OSX', '0', '登录成功', '2026-02-05 02:17:25');
INSERT INTO `sys_logininfor` VALUES (2019203832710565889, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-05 08:18:03');
INSERT INTO `sys_logininfor` VALUES (2019210865128116226, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'OSX', '0', '登录成功', '2026-02-05 08:46:00');
INSERT INTO `sys_logininfor` VALUES (2019213517912150018, '154726', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-05 08:56:33');
INSERT INTO `sys_logininfor` VALUES (2019217038615121921, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-05 09:10:32');
INSERT INTO `sys_logininfor` VALUES (2019219134710157314, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Linux', '0', '登录成功', '2026-02-05 09:18:52');
INSERT INTO `sys_logininfor` VALUES (2019222079201157121, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-05 09:30:34');
INSERT INTO `sys_logininfor` VALUES (2019223047400198146, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'OSX', '0', '登录成功', '2026-02-05 09:34:25');
INSERT INTO `sys_logininfor` VALUES (2019224288503140354, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'Chrome', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-05 09:39:20');
INSERT INTO `sys_logininfor` VALUES (2019224998575153153, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '退出成功', '2026-02-05 09:42:10');
INSERT INTO `sys_logininfor` VALUES (2019225059417726977, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-05 09:42:24');
INSERT INTO `sys_logininfor` VALUES (2019240817392693249, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-05 10:45:01');
INSERT INTO `sys_logininfor` VALUES (2019447979716972545, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-02-06 00:28:13');

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
                             `menu_id` bigint NOT NULL COMMENT '菜单ID',
                             `menu_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜单名称',
                             `parent_id` bigint NULL DEFAULT 0 COMMENT '父菜单ID',
                             `order_num` int NULL DEFAULT 0 COMMENT '显示顺序',
                             `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '路由地址',
                             `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '组件路径',
                             `query_param` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '路由参数',
                             `is_frame` int NULL DEFAULT 1 COMMENT '是否为外链（0是 1否）',
                             `is_cache` int NULL DEFAULT 0 COMMENT '是否缓存（0缓存 1不缓存）',
                             `menu_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
                             `visible` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '显示状态（0显示 1隐藏）',
                             `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
                             `perms` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '权限标识',
                             `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '#' COMMENT '菜单图标',
                             `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                             `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                             `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                             `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                             `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                             `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '备注',
                             PRIMARY KEY (`menu_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜单权限表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (1, '系统管理', 0, 3, 'system', '', '', 1, 0, 'M', '0', '0', '', 'eos-icons:system-group', 103, 1, '2025-12-14 16:11:49', 1, '2026-01-01 19:06:19', '系统管理目录');
INSERT INTO `sys_menu` VALUES (2, '系统监控', 0, 3, 'monitor', '', '', 1, 0, 'M', '0', '0', '', 'solar:monitor-camera-outline', 103, 1, '2025-12-14 16:11:49', 1, '2025-12-14 17:56:44', '系统监控目录');
INSERT INTO `sys_menu` VALUES (3, '系统工具', 0, 4, 'tool', NULL, '', 1, 0, 'M', '0', '0', '', 'ant-design:tool-outlined', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '系统工具目录');
INSERT INTO `sys_menu` VALUES (6, '租户管理', 0, 2, 'tenant', NULL, '', 1, 0, 'M', '0', '0', '', 'ph:users-light', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '租户管理目录');
INSERT INTO `sys_menu` VALUES (100, '用户管理', 1, 1, 'user', 'system/user/index', '', 1, 0, 'C', '0', '0', 'system:user:list', 'ant-design:user-outlined', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '用户管理菜单');
INSERT INTO `sys_menu` VALUES (101, '角色管理', 1, 2, 'role', 'system/role/index', '', 1, 0, 'C', '0', '0', 'system:role:list', 'eos-icons:role-binding-outlined', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '角色管理菜单');
INSERT INTO `sys_menu` VALUES (102, '菜单管理', 1, 3, 'menu', 'system/menu/index', '', 1, 0, 'C', '0', '0', 'system:menu:list', 'ic:sharp-menu', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '菜单管理菜单');
INSERT INTO `sys_menu` VALUES (103, '部门管理', 1, 4, 'dept', 'system/dept/index', '', 1, 0, 'C', '0', '0', 'system:dept:list', 'mingcute:department-line', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '部门管理菜单');
INSERT INTO `sys_menu` VALUES (104, '岗位管理', 1, 5, 'post', 'system/post/index', '', 1, 0, 'C', '0', '0', 'system:post:list', 'icon-park-outline:appointment', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '岗位管理菜单');
INSERT INTO `sys_menu` VALUES (105, '字典管理', 1, 6, 'dict', 'system/dict/index', '', 1, 0, 'C', '0', '0', 'system:dict:list', 'fluent-mdl2:dictionary', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '字典管理菜单');
INSERT INTO `sys_menu` VALUES (106, '参数设置', 1, 7, 'config', 'system/config/index', '', 1, 0, 'C', '0', '0', 'system:config:list', 'ant-design:setting-outlined', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '参数设置菜单');
INSERT INTO `sys_menu` VALUES (107, '通知公告', 1, 8, 'notice', 'system/notice/index', '', 1, 0, 'C', '0', '0', 'system:notice:list', 'fe:notice-push', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '通知公告菜单');
INSERT INTO `sys_menu` VALUES (108, '日志管理', 1, 9, 'log', '', '', 1, 0, 'M', '0', '0', '', 'material-symbols:logo-dev-outline', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '日志管理菜单');
INSERT INTO `sys_menu` VALUES (109, '在线用户', 2, 1, 'online', 'monitor/online/index', '', 1, 0, 'C', '0', '0', 'monitor:online:list', 'material-symbols:generating-tokens-outline', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '在线用户菜单');
INSERT INTO `sys_menu` VALUES (113, '缓存监控', 2, 5, 'cache', 'monitor/cache/index', '', 1, 0, 'C', '0', '0', 'monitor:cache:list', 'devicon:redis-wordmark', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '缓存监控菜单');
INSERT INTO `sys_menu` VALUES (115, '代码生成', 3, 2, 'gen', 'tool/gen/index', '', 1, 0, 'C', '0', '0', 'tool:gen:list', 'tabler:code', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '代码生成菜单');
INSERT INTO `sys_menu` VALUES (116, '修改生成配置', 3, 2, 'gen-edit/index/:tableId', 'tool/gen/editTable', '', 1, 1, 'C', '1', '0', 'tool:gen:edit', 'tabler:code', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '/tool/gen');
INSERT INTO `sys_menu` VALUES (117, 'Admin监控', 2, 5, 'Admin', 'monitor/admin/index', '', 1, 0, 'C', '1', '0', 'monitor:admin:list', 'devicon:spring-wordmark', 103, 1, '2025-12-14 16:11:49', 1, '2025-12-14 17:56:54', 'Admin监控菜单');
INSERT INTO `sys_menu` VALUES (118, '文件管理', 1, 10, 'oss', 'system/oss/index', '', 1, 0, 'C', '0', '0', 'system:oss:list', 'solar:folder-with-files-outline', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '文件管理菜单');
INSERT INTO `sys_menu` VALUES (120, '任务调度中心', 2, 6, 'snailjob', 'monitor/snailjob/index', '', 1, 0, 'C', '1', '0', 'monitor:snailjob:list', 'svg:snail-job', 103, 1, '2025-12-14 16:11:49', 1, '2025-12-14 17:56:59', 'SnailJob控制台菜单');
INSERT INTO `sys_menu` VALUES (121, '租户管理', 6, 1, 'tenant', 'system/tenant/index', '', 1, 0, 'C', '0', '0', 'system:tenant:list', 'ph:user-list', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '租户管理菜单');
INSERT INTO `sys_menu` VALUES (122, '租户套餐管理', 6, 2, 'tenantPackage', 'system/tenantPackage/index', '', 1, 0, 'C', '0', '0', 'system:tenantPackage:list', 'bx:package', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '租户套餐管理菜单');
INSERT INTO `sys_menu` VALUES (123, '客户端管理', 1, 11, 'client', 'system/client/index', '', 1, 0, 'C', '0', '0', 'system:client:list', 'solar:monitor-smartphone-outline', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '客户端管理菜单');
INSERT INTO `sys_menu` VALUES (130, '分配用户', 1, 2, 'role-auth/user/:roleId', 'system/role/authUser', '', 1, 1, 'C', '1', '0', 'system:role:edit', 'eos-icons:role-binding-outlined', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '/system/role');
INSERT INTO `sys_menu` VALUES (131, '分配角色', 1, 1, 'user-auth/role/:userId', 'system/user/authRole', '', 1, 1, 'C', '1', '0', 'system:user:edit', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '/system/user');
INSERT INTO `sys_menu` VALUES (132, '字典数据', 1, 6, 'dict-data/index/:dictId', 'system/dict/data', '', 1, 1, 'C', '1', '0', 'system:dict:list', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '/system/dict');
INSERT INTO `sys_menu` VALUES (133, '文件配置管理', 1, 10, 'oss-config/index', 'system/oss/config', '', 1, 1, 'C', '1', '0', 'system:ossConfig:list', 'ant-design:setting-outlined', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '/system/oss');
INSERT INTO `sys_menu` VALUES (500, '操作日志', 108, 1, 'operlog', 'monitor/operlog/index', '', 1, 0, 'C', '0', '0', 'monitor:operlog:list', 'arcticons:one-hand-operation', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '操作日志菜单');
INSERT INTO `sys_menu` VALUES (501, '登录日志', 108, 2, 'logininfor', 'monitor/logininfor/index', '', 1, 0, 'C', '0', '0', 'monitor:logininfor:list', 'streamline:interface-login-dial-pad-finger-password-dial-pad-dot-finger', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '登录日志菜单');
INSERT INTO `sys_menu` VALUES (1001, '用户查询', 100, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:user:query', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1002, '用户新增', 100, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:user:add', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1003, '用户修改', 100, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:user:edit', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1004, '用户删除', 100, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:user:remove', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1005, '用户导出', 100, 5, '', '', '', 1, 0, 'F', '0', '0', 'system:user:export', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1006, '用户导入', 100, 6, '', '', '', 1, 0, 'F', '0', '0', 'system:user:import', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1007, '重置密码', 100, 7, '', '', '', 1, 0, 'F', '0', '0', 'system:user:resetPwd', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1008, '角色查询', 101, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:role:query', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1009, '角色新增', 101, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:role:add', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1010, '角色修改', 101, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:role:edit', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1011, '角色删除', 101, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:role:remove', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1012, '角色导出', 101, 5, '', '', '', 1, 0, 'F', '0', '0', 'system:role:export', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1013, '菜单查询', 102, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:query', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1014, '菜单新增', 102, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:add', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1015, '菜单修改', 102, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:edit', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1016, '菜单删除', 102, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:remove', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1017, '部门查询', 103, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:query', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1018, '部门新增', 103, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:add', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1019, '部门修改', 103, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:edit', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1020, '部门删除', 103, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:remove', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1021, '岗位查询', 104, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:post:query', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1022, '岗位新增', 104, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:post:add', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1023, '岗位修改', 104, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:post:edit', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1024, '岗位删除', 104, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:post:remove', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1025, '岗位导出', 104, 5, '', '', '', 1, 0, 'F', '0', '0', 'system:post:export', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1026, '字典查询', 105, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:query', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1027, '字典新增', 105, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:add', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1028, '字典修改', 105, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:edit', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1029, '字典删除', 105, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:remove', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1030, '字典导出', 105, 5, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:export', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1031, '参数查询', 106, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:query', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1032, '参数新增', 106, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:add', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1033, '参数修改', 106, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:edit', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1034, '参数删除', 106, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:remove', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1035, '参数导出', 106, 5, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:export', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1036, '公告查询', 107, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:query', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1037, '公告新增', 107, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:add', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1038, '公告修改', 107, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:edit', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1039, '公告删除', 107, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:remove', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1040, '操作查询', 500, 1, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:query', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1041, '操作删除', 500, 2, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:remove', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1042, '日志导出', 500, 4, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:export', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1043, '登录查询', 501, 1, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:query', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1044, '登录删除', 501, 2, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:remove', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1045, '日志导出', 501, 3, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:export', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1046, '在线查询', 109, 1, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:online:query', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1047, '批量强退', 109, 2, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:online:batchLogout', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1048, '单条强退', 109, 3, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:online:forceLogout', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1050, '账户解锁', 501, 4, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:unlock', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1055, '生成查询', 115, 1, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:query', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1056, '生成修改', 115, 2, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:edit', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1057, '生成删除', 115, 3, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:remove', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1058, '导入代码', 115, 2, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:import', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1059, '预览代码', 115, 4, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:preview', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1060, '生成代码', 115, 5, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:code', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1061, '客户端管理查询', 123, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:client:query', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1062, '客户端管理新增', 123, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:client:add', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1063, '客户端管理修改', 123, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:client:edit', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1064, '客户端管理删除', 123, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:client:remove', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1065, '客户端管理导出', 123, 5, '#', '', '', 1, 0, 'F', '0', '0', 'system:client:export', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1600, '文件查询', 118, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:query', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1601, '文件上传', 118, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:upload', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1602, '文件下载', 118, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:download', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1603, '文件删除', 118, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:remove', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1606, '租户查询', 121, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenant:query', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1607, '租户新增', 121, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenant:add', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1608, '租户修改', 121, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenant:edit', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1609, '租户删除', 121, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenant:remove', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1610, '租户导出', 121, 5, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenant:export', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1611, '租户套餐查询', 122, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenantPackage:query', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1612, '租户套餐新增', 122, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenantPackage:add', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1613, '租户套餐修改', 122, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenantPackage:edit', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1614, '租户套餐删除', 122, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenantPackage:remove', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1615, '租户套餐导出', 122, 5, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenantPackage:export', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1620, '配置列表', 118, 5, '#', '', '', 1, 0, 'F', '0', '0', 'system:ossConfig:list', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1621, '配置添加', 118, 6, '#', '', '', 1, 0, 'F', '0', '0', 'system:ossConfig:add', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1622, '配置编辑', 118, 6, '#', '', '', 1, 0, 'F', '0', '0', 'system:ossConfig:edit', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1623, '配置删除', 118, 6, '#', '', '', 1, 0, 'F', '0', '0', 'system:ossConfig:remove', '#', 103, 1, '2025-12-14 16:11:49', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (11616, '工作流', 0, 6, 'workflow', '', '', 1, 0, 'M', '0', '0', '', 'mdi:workflow-outline', 103, 1, '2026-01-05 14:39:33', 1, '2026-01-05 14:56:07', '');
INSERT INTO `sys_menu` VALUES (11618, '我的任务', 0, 7, 'task', '', '', 1, 0, 'M', '0', '0', '', 'carbon:task-approved', 103, 1, '2026-01-05 14:39:33', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (11619, '我的待办', 11618, 2, 'taskWaiting', 'workflow/task/taskWaiting', '', 1, 1, 'C', '0', '0', '', 'ri:todo-line', 103, 1, '2026-01-05 14:39:33', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (11620, '流程定义', 11616, 3, 'processDefinition', 'workflow/processDefinition/index', '', 1, 1, 'C', '0', '0', '', 'fluent-mdl2:build-definition', 103, 1, '2026-01-05 14:39:33', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (11621, '流程实例', 11630, 1, 'processInstance', 'workflow/processInstance/index', '', 1, 1, 'C', '0', '0', '', 'ri:instance-line', 103, 1, '2026-01-05 14:39:33', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (11622, '流程分类', 11616, 1, 'category', 'workflow/category/index', '', 1, 0, 'C', '0', '0', 'workflow:category:list', 'tabler:category-plus', 103, 1, '2026-01-05 14:39:33', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (11623, '流程分类查询', 11622, 1, '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:query', '#', 103, 1, '2026-01-05 14:39:33', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (11624, '流程分类新增', 11622, 2, '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:add', '#', 103, 1, '2026-01-05 14:39:33', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (11625, '流程分类修改', 11622, 3, '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:edit', '#', 103, 1, '2026-01-05 14:39:33', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (11626, '流程分类删除', 11622, 4, '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:remove', '#', 103, 1, '2026-01-05 14:39:33', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (11627, '流程分类导出', 11622, 5, '#', '', '', 1, 0, 'F', '0', '0', 'workflow:category:export', '#', 103, 1, '2026-01-05 14:39:33', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (11629, '我发起的', 11618, 1, 'myDocument', 'workflow/task/myDocument', '', 1, 1, 'C', '0', '0', '', 'ic:round-launch', 103, 1, '2026-01-05 14:39:33', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (11630, '流程监控', 11616, 4, 'monitor', '', '', 1, 0, 'M', '0', '0', '', 'icon-park-outline:monitor', 103, 1, '2026-01-05 14:39:33', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (11631, '待办任务', 11630, 2, 'allTaskWaiting', 'workflow/task/allTaskWaiting', '', 1, 1, 'C', '0', '0', '', 'ri:todo-line', 103, 1, '2026-01-05 14:39:33', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (11632, '我的已办', 11618, 3, 'taskFinish', 'workflow/task/taskFinish', '', 1, 1, 'C', '0', '0', '', 'material-symbols:cloud-done-outline-rounded', 103, 1, '2026-01-05 14:39:33', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (11633, '我的抄送', 11618, 4, 'taskCopyList', 'workflow/task/taskCopyList', '', 1, 1, 'C', '0', '0', '', 'mdi:cc-outline', 103, 1, '2026-01-05 14:39:33', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (11700, '流程设计', 11616, 5, 'design/index', 'workflow/processDefinition/design', '', 1, 1, 'C', '1', '0', 'workflow:leave:edit', 'fluent-mdl2:flow', 103, 1, '2026-01-05 14:39:33', 1, '2026-02-06 01:23:10', '/workflow/processDefinition');
INSERT INTO `sys_menu` VALUES (11701, '请假申请', 11616, 6, 'leaveEdit/index', 'workflow/leave/leaveEdit', '', 1, 1, 'C', '1', '0', 'workflow:leave:edit', 'flat-color-icons:leave', 103, 1, '2026-01-05 14:39:33', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (11801, '流程表达式', 11616, 2, 'spel', 'workflow/spel/index', '', 1, 0, 'C', '0', '0', 'workflow:spel:list', 'material-symbols:regular-expression-rounded', 103, 1, '2026-01-05 14:39:33', 1, '2026-01-05 14:39:33', '流程达式定义菜单');
INSERT INTO `sys_menu` VALUES (11802, '流程达式定义查询', 11801, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'workflow:spel:query', '#', 103, 1, '2026-01-05 14:39:33', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (11803, '流程达式定义新增', 11801, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'workflow:spel:add', '#', 103, 1, '2026-01-05 14:39:33', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (11804, '流程达式定义修改', 11801, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'workflow:spel:edit', '#', 103, 1, '2026-01-05 14:39:33', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (11805, '流程达式定义删除', 11801, 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'workflow:spel:remove', '#', 103, 1, '2026-01-05 14:39:33', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (11806, '流程达式定义导出', 11801, 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'workflow:spel:export', '#', 103, 1, '2026-01-05 14:39:33', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1971546066781597696, '数字人体验', 2019459914910994434, 10, 'aihumanPublish', 'aihuman/aihumanPublish/index', NULL, 1, 0, 'C', '0', '0', '', 'mdi:human-child', 103, 1, '2026-02-06 01:13:22', 1, '2026-02-06 01:29:34', '数字人信息管理菜单');
INSERT INTO `sys_menu` VALUES (1971546066781597697, '数字人信息管理查询', 1971546066781597696, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'aihuman:aihumanInfo:query', '#', 103, 1, '2026-02-06 01:13:22', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1971546066781597698, '数字人信息管理新增', 1971546066781597696, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'aihuman:aihumanInfo:add', '#', 103, 1, '2026-02-06 01:13:22', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1971546066781597699, '数字人信息管理修改', 1971546066781597696, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'aihuman:aihumanInfo:edit', '#', 103, 1, '2026-02-06 01:13:22', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1971546066781597700, '数字人信息管理删除', 1971546066781597696, 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'aihuman:aihumanInfo:remove', '#', 103, 1, '2026-02-06 01:13:22', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1971546066781597701, '数字人信息管理导出', 1971546066781597696, 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'aihuman:aihumanInfo:export', '#', 103, 1, '2026-02-06 01:13:22', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1980480880138051584, '数字人配置', 2019459914910994434, 1, 'aihumanConfig', 'aihuman/aihumanConfig/index', NULL, 1, 0, 'C', '0', '0', 'aihuman:aihumanConfig:list', 'mdi:human-child', 103, 1, '2026-02-06 01:13:35', 1, '2026-02-06 01:28:12', '');
INSERT INTO `sys_menu` VALUES (2000209300188356609, '对话管理', 0, 0, 'chat', '', NULL, 1, 0, 'M', '0', '0', NULL, 'material-symbols:chat-outline', 103, 1, '2025-12-14 22:20:34', 1, '2025-12-14 22:21:24', '');
INSERT INTO `sys_menu` VALUES (2000210913451892738, '厂商管理', 2000209300188356609, 1, 'provider', 'chat/provider/index', NULL, 1, 0, 'C', '0', '0', 'system:provider:list', 'tabler:cube-spark', 103, 1, '2025-12-14 22:28:05', 1, '2025-12-14 23:42:55', '厂商管理菜单');
INSERT INTO `sys_menu` VALUES (2000210913451892739, '厂商管理查询', 2000210913451892738, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:provider:query', '#', 103, 1, '2025-12-14 22:28:05', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2000210913451892740, '厂商管理新增', 2000210913451892738, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:provider:add', '#', 103, 1, '2025-12-14 22:28:05', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2000210913451892741, '厂商管理修改', 2000210913451892738, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:provider:edit', '#', 103, 1, '2025-12-14 22:28:05', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2000210913451892742, '厂商管理删除', 2000210913451892738, 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:provider:remove', '#', 103, 1, '2025-12-14 22:28:05', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2000210913451892743, '厂商管理导出', 2000210913451892738, 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:provider:export', '#', 103, 1, '2025-12-14 22:28:05', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2000210913846157314, '模型管理', 2000209300188356609, 1, 'model', 'chat/model/index', NULL, 1, 0, 'C', '0', '0', 'system:model:list', 'carbon:model-alt', 103, 1, '2025-12-14 22:27:59', 1, '2025-12-15 00:51:01', '模型管理菜单');
INSERT INTO `sys_menu` VALUES (2000210913846157315, '模型管理查询', 2000210913846157314, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:model:query', '#', 103, 1, '2025-12-14 22:27:59', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2000210913846157316, '模型管理新增', 2000210913846157314, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:model:add', '#', 103, 1, '2025-12-14 22:27:59', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2000210913846157317, '模型管理修改', 2000210913846157314, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:model:edit', '#', 103, 1, '2025-12-14 22:27:59', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2000210913846157318, '模型管理删除', 2000210913846157314, 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:model:remove', '#', 103, 1, '2025-12-14 22:27:59', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2000210913846157319, '模型管理导出', 2000210913846157314, 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:model:export', '#', 103, 1, '2025-12-14 22:28:00', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2000210914299142145, '聊天配置', 2000209300188356609, 1, 'config', 'chat/config/index', NULL, 1, 0, 'C', '0', '0', 'system:config:list', 'tdesign:task-setting', 103, 1, '2025-12-14 22:27:46', 1, '2025-12-15 00:59:48', '配置信息菜单');
INSERT INTO `sys_menu` VALUES (2000210914299142146, '配置信息查询', 2000210914299142145, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:config:query', '#', 103, 1, '2025-12-14 22:27:46', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2000210914299142147, '配置信息新增', 2000210914299142145, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:config:add', '#', 103, 1, '2025-12-14 22:27:46', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2000210914299142148, '配置信息修改', 2000210914299142145, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:config:edit', '#', 103, 1, '2025-12-14 22:27:46', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2000210914299142149, '配置信息删除', 2000210914299142145, 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:config:remove', '#', 103, 1, '2025-12-14 22:27:46', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2000210914299142150, '配置信息导出', 2000210914299142145, 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:config:export', '#', 103, 1, '2025-12-14 22:27:46', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2000210914680823809, '聊天消息', 2000209300188356609, 1, 'message', 'chat/message/index', NULL, 1, 0, 'C', '0', '0', 'system:message:list', 'system-uicons:message', 103, 1, '2025-12-14 22:27:54', 1, '2025-12-15 00:53:47', '聊天消息菜单');
INSERT INTO `sys_menu` VALUES (2000210914680823810, '聊天消息查询', 2000210914680823809, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:message:query', '#', 103, 1, '2025-12-14 22:27:54', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2000210914680823811, '聊天消息新增', 2000210914680823809, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:message:add', '#', 103, 1, '2025-12-14 22:27:54', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2000210914680823812, '聊天消息修改', 2000210914680823809, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:message:edit', '#', 103, 1, '2025-12-14 22:27:54', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2000210914680823813, '聊天消息删除', 2000210914680823809, 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:message:remove', '#', 103, 1, '2025-12-14 22:27:54', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2000210914680823814, '聊天消息导出', 2000210914680823809, 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:message:export', '#', 103, 1, '2025-12-14 22:27:54', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2006681261898813441, '知识库', 2006683336984580098, 1, 'info', 'knowledge/info/index', NULL, 1, 0, 'C', '0', '0', 'knowledge:info:list', 'solar:book-line-duotone', 103, 1, '2026-01-01 18:59:05', 1, '2026-01-01 19:08:03', '知识库菜单');
INSERT INTO `sys_menu` VALUES (2006681261898813442, '知识库查询', 2006681261898813441, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:info:query', '#', 103, 1, '2026-01-01 18:59:05', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2006681261898813443, '知识库新增', 2006681261898813441, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:info:add', '#', 103, 1, '2026-01-01 18:59:05', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2006681261898813444, '知识库修改', 2006681261898813441, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:info:edit', '#', 103, 1, '2026-01-01 18:59:05', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2006681261898813445, '知识库删除', 2006681261898813441, 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:info:remove', '#', 103, 1, '2026-01-01 18:59:06', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2006681261898813446, '知识库导出', 2006681261898813441, 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:info:export', '#', 103, 1, '2026-01-01 18:59:06', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2006683336984580098, '知识管理', 0, 2, 'knowledge', '', NULL, 1, 0, 'M', '0', '0', NULL, 'bx:book', 103, 1, '2026-01-01 19:06:05', 1, '2026-01-01 19:06:05', '');
INSERT INTO `sys_menu` VALUES (2019459914910994434, '数字人管理', 0, 2, 'human', '', NULL, 1, 0, 'M', '0', '0', NULL, 'tdesign:user', 103, 1, '2026-02-06 01:15:38', 1, '2026-02-06 01:16:58', '');
INSERT INTO `sys_menu` VALUES (2019464280262905857, '图谱实例', 2019464531388469250, 1, 'graphInstance', 'graph/graphInstance/index', NULL, 1, 0, 'C', '0', '0', 'operator:graph:list', 'ant-design:node-index-outlined', 103, 1, '2026-02-06 01:32:59', 1, '2026-02-06 01:40:06', '');
INSERT INTO `sys_menu` VALUES (2019464531388469250, '知识图谱', 2006683336984580098, 15, 'graph', '', NULL, 1, 0, 'M', '0', '0', NULL, 'carbon:chart-relationship', 103, 1, '2026-02-06 01:33:59', 1, '2026-02-06 01:33:59', '');
INSERT INTO `sys_menu` VALUES (2019464779217309697, '图谱可视化', 2019464531388469250, 2, 'graphVisualization', 'graph/graphVisualization/index', NULL, 1, 0, 'C', '0', '0', 'operator:graph:view', 'carbon:chart-network', 103, 1, '2026-02-06 01:34:58', 1, '2026-02-06 01:40:14', '');
INSERT INTO `sys_menu` VALUES (2019464917407043585, '图谱检索', 2019464531388469250, 3, 'graphRAG', 'graph/graphRAG/index', NULL, 1, 0, 'C', '0', '0', 'operator:graph:retrieve', 'carbon:search-advanced', 103, 1, '2026-02-06 01:35:31', 1, '2026-02-06 01:40:19', '');

-- ----------------------------
-- Table structure for sys_notice
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice`  (
                               `notice_id` bigint NOT NULL COMMENT '公告ID',
                               `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户编号',
                               `notice_title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '公告标题',
                               `notice_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '公告类型（1通知 2公告）',
                               `notice_content` longblob NULL COMMENT '公告内容',
                               `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '公告状态（0正常 1关闭）',
                               `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                               `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                               `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                               `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                               `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                               `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
                               PRIMARY KEY (`notice_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通知公告表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_notice
-- ----------------------------
INSERT INTO `sys_notice` VALUES (1, '000000', '温馨提醒：2018-07-01 新版本发布啦', '2', 0xE696B0E78988E69CACE58685E5AEB9, '0', 103, 1, '2026-02-03 05:14:52', NULL, NULL, '管理员');
INSERT INTO `sys_notice` VALUES (2, '000000', '维护通知：2018-07-01 系统凌晨维护', '1', 0xE7BBB4E68AA4E58685E5AEB9, '0', 103, 1, '2026-02-03 05:14:52', NULL, NULL, '管理员');

-- ----------------------------
-- Table structure for sys_oper_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log`  (
                                 `oper_id` bigint NOT NULL COMMENT '日志主键',
                                 `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户编号',
                                 `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '模块标题',
                                 `business_type` int NULL DEFAULT 0 COMMENT '业务类型（0其它 1新增 2修改 3删除）',
                                 `method` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '方法名称',
                                 `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '请求方式',
                                 `operator_type` int NULL DEFAULT 0 COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
                                 `oper_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '操作人员',
                                 `dept_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '部门名称',
                                 `oper_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '请求URL',
                                 `oper_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '主机地址',
                                 `oper_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '操作地点',
                                 `oper_param` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '请求参数',
                                 `json_result` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '返回参数',
                                 `status` int NULL DEFAULT 0 COMMENT '操作状态（0正常 1异常）',
                                 `error_msg` varchar(4000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '错误消息',
                                 `oper_time` datetime NULL DEFAULT NULL COMMENT '操作时间',
                                 `cost_time` bigint NULL DEFAULT 0 COMMENT '消耗时间',
                                 PRIMARY KEY (`oper_id`) USING BTREE,
                                 INDEX `idx_sys_oper_log_bt`(`business_type` ASC) USING BTREE,
                                 INDEX `idx_sys_oper_log_s`(`status` ASC) USING BTREE,
                                 INDEX `idx_sys_oper_log_ot`(`oper_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '操作日志记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_oper_log
-- ----------------------------

-- ----------------------------
-- Table structure for sys_oss
-- ----------------------------
DROP TABLE IF EXISTS `sys_oss`;
CREATE TABLE `sys_oss`  (
                            `oss_id` bigint NOT NULL COMMENT '对象存储主键',
                            `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户编号',
                            `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '文件名',
                            `original_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '原名',
                            `file_suffix` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '文件后缀名',
                            `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'URL地址',
                            `ext1` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扩展字段',
                            `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                            `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                            `create_by` bigint NULL DEFAULT NULL COMMENT '上传人',
                            `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                            `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
                            `service` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'minio' COMMENT '服务商',
                            PRIMARY KEY (`oss_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'OSS对象存储表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_oss
-- ----------------------------

-- ----------------------------
-- Table structure for sys_oss_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_oss_config`;
CREATE TABLE `sys_oss_config`  (
                                   `oss_config_id` bigint NOT NULL COMMENT '主键',
                                   `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户编号',
                                   `config_key` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '配置key',
                                   `access_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT 'accessKey',
                                   `secret_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '秘钥',
                                   `bucket_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '桶名称',
                                   `prefix` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '前缀',
                                   `endpoint` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '访问站点',
                                   `domain` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '自定义域名',
                                   `is_https` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'N' COMMENT '是否https（Y=是,N=否）',
                                   `region` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '域',
                                   `access_policy` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '1' COMMENT '桶权限类型(0=private 1=public 2=custom)',
                                   `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '1' COMMENT '是否默认（0=是,1=否）',
                                   `ext1` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '扩展字段',
                                   `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                                   `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                                   `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                   `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                                   `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                   `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
                                   PRIMARY KEY (`oss_config_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '对象存储配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_oss_config
-- ----------------------------
INSERT INTO `sys_oss_config` VALUES (1, '000000', 'minio', 'ruoyi', 'ruoyi123', 'ruoyi', '', '127.0.0.1:9000', '', 'N', '', '1', '0', '', 103, 1, '2026-02-03 05:14:52', 1, '2026-02-03 05:14:52', NULL);
INSERT INTO `sys_oss_config` VALUES (2, '000000', 'qiniu', 'XXXXXXXXXXXXXXX', 'XXXXXXXXXXXXXXX', 'ruoyi', '', 's3-cn-north-1.qiniucs.com', '', 'N', '', '1', '1', '', 103, 1, '2026-02-03 05:14:52', 1, '2026-02-03 05:14:52', NULL);
INSERT INTO `sys_oss_config` VALUES (3, '000000', 'aliyun', 'XXXXXXXXXXXXXXX', 'XXXXXXXXXXXXXXX', 'ruoyi', '', 'oss-cn-beijing.aliyuncs.com', '', 'N', '', '1', '1', '', 103, 1, '2026-02-03 05:14:52', 1, '2026-02-03 05:14:52', NULL);
INSERT INTO `sys_oss_config` VALUES (4, '000000', 'qcloud', 'XXXXXXXXXXXXXXX', 'XXXXXXXXXXXXXXX', 'ruoyi-1240000000', '', 'cos.ap-beijing.myqcloud.com', '', 'N', 'ap-beijing', '1', '1', '', 103, 1, '2026-02-03 05:14:52', 1, '2026-02-03 05:14:52', NULL);
INSERT INTO `sys_oss_config` VALUES (5, '000000', 'image', 'ruoyi', 'ruoyi123', 'ruoyi', 'image', '127.0.0.1:9000', '', 'N', '', '1', '1', '', 103, 1, '2026-02-03 05:14:53', 1, '2026-02-03 05:14:53', NULL);

-- ----------------------------
-- Table structure for sys_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post`  (
                             `post_id` bigint NOT NULL COMMENT '岗位ID',
                             `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户编号',
                             `dept_id` bigint NOT NULL COMMENT '部门id',
                             `post_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位编码',
                             `post_category` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '岗位类别编码',
                             `post_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位名称',
                             `post_sort` int NOT NULL COMMENT '显示顺序',
                             `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '状态（0正常 1停用）',
                             `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                             `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                             `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                             `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                             `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                             `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
                             PRIMARY KEY (`post_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '岗位信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_post
-- ----------------------------
INSERT INTO `sys_post` VALUES (1, '000000', 103, 'ceo', NULL, '董事长', 1, '0', 103, 1, '2026-02-03 05:14:39', NULL, NULL, '');
INSERT INTO `sys_post` VALUES (2, '000000', 100, 'se', NULL, '项目经理', 2, '0', 103, 1, '2026-02-03 05:14:39', NULL, NULL, '');
INSERT INTO `sys_post` VALUES (3, '000000', 100, 'hr', NULL, '人力资源', 3, '0', 103, 1, '2026-02-03 05:14:39', NULL, NULL, '');
INSERT INTO `sys_post` VALUES (4, '000000', 100, 'user', NULL, '普通员工', 4, '0', 103, 1, '2026-02-03 05:14:39', NULL, NULL, '');

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
                             `role_id` bigint NOT NULL COMMENT '角色ID',
                             `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户编号',
                             `role_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
                             `role_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色权限字符串',
                             `role_sort` int NOT NULL COMMENT '显示顺序',
                             `data_scope` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限 6：部门及以下或本人数据权限）',
                             `menu_check_strictly` tinyint(1) NULL DEFAULT 1 COMMENT '菜单树选择项是否关联显示',
                             `dept_check_strictly` tinyint(1) NULL DEFAULT 1 COMMENT '部门树选择项是否关联显示',
                             `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色状态（0正常 1停用）',
                             `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
                             `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                             `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                             `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                             `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                             `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                             `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
                             PRIMARY KEY (`role_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '000000', '超级管理员', 'superadmin', 1, '1', 1, 1, '0', '0', 103, 1, '2026-02-03 05:14:39', NULL, NULL, '超级管理员');
INSERT INTO `sys_role` VALUES (3, '000000', '本部门及以下', 'test1', 3, '4', 1, 1, '0', '0', 103, 1, '2026-02-03 05:14:39', NULL, NULL, '');
INSERT INTO `sys_role` VALUES (4, '000000', '仅本人', 'test2', 4, '5', 1, 1, '0', '0', 103, 1, '2026-02-03 05:14:39', NULL, NULL, '');
INSERT INTO `sys_role` VALUES (2018858143199662082, '154726', '管理员', 'admin', 1, '1', 1, 1, '0', '0', 103, 1, '2026-02-04 09:24:25', 1, '2026-02-04 09:24:25', NULL);

-- ----------------------------
-- Table structure for sys_role_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept`  (
                                  `role_id` bigint NOT NULL COMMENT '角色ID',
                                  `dept_id` bigint NOT NULL COMMENT '部门ID',
                                  PRIMARY KEY (`role_id`, `dept_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色和部门关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role_dept
-- ----------------------------
INSERT INTO `sys_role_dept` VALUES (2018858143199662082, 2018858143262576642);

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
                                  `role_id` bigint NOT NULL COMMENT '角色ID',
                                  `menu_id` bigint NOT NULL COMMENT '菜单ID',
                                  PRIMARY KEY (`role_id`, `menu_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色和菜单关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (3, 1);
INSERT INTO `sys_role_menu` VALUES (3, 5);
INSERT INTO `sys_role_menu` VALUES (3, 100);
INSERT INTO `sys_role_menu` VALUES (3, 101);
INSERT INTO `sys_role_menu` VALUES (3, 102);
INSERT INTO `sys_role_menu` VALUES (3, 103);
INSERT INTO `sys_role_menu` VALUES (3, 104);
INSERT INTO `sys_role_menu` VALUES (3, 105);
INSERT INTO `sys_role_menu` VALUES (3, 106);
INSERT INTO `sys_role_menu` VALUES (3, 107);
INSERT INTO `sys_role_menu` VALUES (3, 108);
INSERT INTO `sys_role_menu` VALUES (3, 118);
INSERT INTO `sys_role_menu` VALUES (3, 123);
INSERT INTO `sys_role_menu` VALUES (3, 130);
INSERT INTO `sys_role_menu` VALUES (3, 131);
INSERT INTO `sys_role_menu` VALUES (3, 132);
INSERT INTO `sys_role_menu` VALUES (3, 133);
INSERT INTO `sys_role_menu` VALUES (3, 500);
INSERT INTO `sys_role_menu` VALUES (3, 501);
INSERT INTO `sys_role_menu` VALUES (3, 1001);
INSERT INTO `sys_role_menu` VALUES (3, 1002);
INSERT INTO `sys_role_menu` VALUES (3, 1003);
INSERT INTO `sys_role_menu` VALUES (3, 1004);
INSERT INTO `sys_role_menu` VALUES (3, 1005);
INSERT INTO `sys_role_menu` VALUES (3, 1006);
INSERT INTO `sys_role_menu` VALUES (3, 1007);
INSERT INTO `sys_role_menu` VALUES (3, 1008);
INSERT INTO `sys_role_menu` VALUES (3, 1009);
INSERT INTO `sys_role_menu` VALUES (3, 1010);
INSERT INTO `sys_role_menu` VALUES (3, 1011);
INSERT INTO `sys_role_menu` VALUES (3, 1012);
INSERT INTO `sys_role_menu` VALUES (3, 1013);
INSERT INTO `sys_role_menu` VALUES (3, 1014);
INSERT INTO `sys_role_menu` VALUES (3, 1015);
INSERT INTO `sys_role_menu` VALUES (3, 1016);
INSERT INTO `sys_role_menu` VALUES (3, 1017);
INSERT INTO `sys_role_menu` VALUES (3, 1018);
INSERT INTO `sys_role_menu` VALUES (3, 1019);
INSERT INTO `sys_role_menu` VALUES (3, 1020);
INSERT INTO `sys_role_menu` VALUES (3, 1021);
INSERT INTO `sys_role_menu` VALUES (3, 1022);
INSERT INTO `sys_role_menu` VALUES (3, 1023);
INSERT INTO `sys_role_menu` VALUES (3, 1024);
INSERT INTO `sys_role_menu` VALUES (3, 1025);
INSERT INTO `sys_role_menu` VALUES (3, 1026);
INSERT INTO `sys_role_menu` VALUES (3, 1027);
INSERT INTO `sys_role_menu` VALUES (3, 1028);
INSERT INTO `sys_role_menu` VALUES (3, 1029);
INSERT INTO `sys_role_menu` VALUES (3, 1030);
INSERT INTO `sys_role_menu` VALUES (3, 1031);
INSERT INTO `sys_role_menu` VALUES (3, 1032);
INSERT INTO `sys_role_menu` VALUES (3, 1033);
INSERT INTO `sys_role_menu` VALUES (3, 1034);
INSERT INTO `sys_role_menu` VALUES (3, 1035);
INSERT INTO `sys_role_menu` VALUES (3, 1036);
INSERT INTO `sys_role_menu` VALUES (3, 1037);
INSERT INTO `sys_role_menu` VALUES (3, 1038);
INSERT INTO `sys_role_menu` VALUES (3, 1039);
INSERT INTO `sys_role_menu` VALUES (3, 1040);
INSERT INTO `sys_role_menu` VALUES (3, 1041);
INSERT INTO `sys_role_menu` VALUES (3, 1042);
INSERT INTO `sys_role_menu` VALUES (3, 1043);
INSERT INTO `sys_role_menu` VALUES (3, 1044);
INSERT INTO `sys_role_menu` VALUES (3, 1045);
INSERT INTO `sys_role_menu` VALUES (3, 1050);
INSERT INTO `sys_role_menu` VALUES (3, 1061);
INSERT INTO `sys_role_menu` VALUES (3, 1062);
INSERT INTO `sys_role_menu` VALUES (3, 1063);
INSERT INTO `sys_role_menu` VALUES (3, 1064);
INSERT INTO `sys_role_menu` VALUES (3, 1065);
INSERT INTO `sys_role_menu` VALUES (3, 1500);
INSERT INTO `sys_role_menu` VALUES (3, 1501);
INSERT INTO `sys_role_menu` VALUES (3, 1502);
INSERT INTO `sys_role_menu` VALUES (3, 1503);
INSERT INTO `sys_role_menu` VALUES (3, 1504);
INSERT INTO `sys_role_menu` VALUES (3, 1505);
INSERT INTO `sys_role_menu` VALUES (3, 1506);
INSERT INTO `sys_role_menu` VALUES (3, 1507);
INSERT INTO `sys_role_menu` VALUES (3, 1508);
INSERT INTO `sys_role_menu` VALUES (3, 1509);
INSERT INTO `sys_role_menu` VALUES (3, 1510);
INSERT INTO `sys_role_menu` VALUES (3, 1511);
INSERT INTO `sys_role_menu` VALUES (3, 1600);
INSERT INTO `sys_role_menu` VALUES (3, 1601);
INSERT INTO `sys_role_menu` VALUES (3, 1602);
INSERT INTO `sys_role_menu` VALUES (3, 1603);
INSERT INTO `sys_role_menu` VALUES (3, 1620);
INSERT INTO `sys_role_menu` VALUES (3, 1621);
INSERT INTO `sys_role_menu` VALUES (3, 1622);
INSERT INTO `sys_role_menu` VALUES (3, 1623);
INSERT INTO `sys_role_menu` VALUES (3, 11616);
INSERT INTO `sys_role_menu` VALUES (3, 11618);
INSERT INTO `sys_role_menu` VALUES (3, 11619);
INSERT INTO `sys_role_menu` VALUES (3, 11622);
INSERT INTO `sys_role_menu` VALUES (3, 11623);
INSERT INTO `sys_role_menu` VALUES (3, 11629);
INSERT INTO `sys_role_menu` VALUES (3, 11632);
INSERT INTO `sys_role_menu` VALUES (3, 11633);
INSERT INTO `sys_role_menu` VALUES (3, 11701);
INSERT INTO `sys_role_menu` VALUES (4, 5);
INSERT INTO `sys_role_menu` VALUES (4, 1500);
INSERT INTO `sys_role_menu` VALUES (4, 1501);
INSERT INTO `sys_role_menu` VALUES (4, 1502);
INSERT INTO `sys_role_menu` VALUES (4, 1503);
INSERT INTO `sys_role_menu` VALUES (4, 1504);
INSERT INTO `sys_role_menu` VALUES (4, 1505);
INSERT INTO `sys_role_menu` VALUES (4, 1506);
INSERT INTO `sys_role_menu` VALUES (4, 1507);
INSERT INTO `sys_role_menu` VALUES (4, 1508);
INSERT INTO `sys_role_menu` VALUES (4, 1509);
INSERT INTO `sys_role_menu` VALUES (4, 1510);
INSERT INTO `sys_role_menu` VALUES (4, 1511);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 2);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 3);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 4);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 5);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 100);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 101);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 102);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 103);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 104);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 105);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 106);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 107);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 108);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 109);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 113);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 115);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 116);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 117);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 118);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 120);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 123);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 130);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 131);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 132);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 133);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 500);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 501);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1001);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1002);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1003);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1004);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1005);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1006);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1007);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1008);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1009);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1010);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1011);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1012);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1013);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1014);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1015);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1016);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1017);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1018);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1019);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1020);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1021);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1022);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1023);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1024);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1025);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1026);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1027);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1028);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1029);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1030);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1031);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1032);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1033);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1034);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1035);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1036);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1037);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1038);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1039);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1040);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1041);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1042);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1043);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1044);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1045);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1046);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1047);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1048);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1050);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1055);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1056);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1057);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1058);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1059);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1060);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1061);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1062);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1063);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1064);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1065);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1500);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1501);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1502);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1503);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1504);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1505);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1506);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1507);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1508);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1509);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1510);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1511);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1600);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1601);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1602);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1603);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1620);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1621);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1622);
INSERT INTO `sys_role_menu` VALUES (2018858143199662082, 1623);

-- ----------------------------
-- Table structure for sys_social
-- ----------------------------
DROP TABLE IF EXISTS `sys_social`;
CREATE TABLE `sys_social`  (
                               `id` bigint NOT NULL COMMENT '主键',
                               `user_id` bigint NOT NULL COMMENT '用户ID',
                               `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户id',
                               `auth_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '平台+平台唯一id',
                               `source` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户来源',
                               `open_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '平台编号唯一id',
                               `user_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录账号',
                               `nick_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '用户昵称',
                               `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '用户邮箱',
                               `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '头像地址',
                               `access_token` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户的授权令牌',
                               `expire_in` int NULL DEFAULT NULL COMMENT '用户的授权令牌的有效期，部分平台可能没有',
                               `refresh_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '刷新令牌，部分平台可能没有',
                               `access_code` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '平台的授权信息，部分平台可能没有',
                               `union_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户的 unionid',
                               `scope` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '授予的权限，部分平台可能没有',
                               `token_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '个别平台的授权信息，部分平台可能没有',
                               `id_token` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'id token，部分平台可能没有',
                               `mac_algorithm` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '小米平台用户的附带属性，部分平台可能没有',
                               `mac_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '小米平台用户的附带属性，部分平台可能没有',
                               `code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户的授权code，部分平台可能没有',
                               `oauth_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Twitter平台用户的附带属性，部分平台可能没有',
                               `oauth_token_secret` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Twitter平台用户的附带属性，部分平台可能没有',
                               `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                               `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                               `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                               `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                               `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                               `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
                               PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社会化关系表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_social
-- ----------------------------

-- ----------------------------
-- Table structure for sys_tenant
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant`;
CREATE TABLE `sys_tenant`  (
                               `id` bigint NOT NULL COMMENT 'id',
                               `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户编号',
                               `contact_user_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人',
                               `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
                               `company_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '企业名称',
                               `license_number` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '统一社会信用代码',
                               `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地址',
                               `intro` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '企业简介',
                               `domain` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '域名',
                               `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
                               `package_id` bigint NULL DEFAULT NULL COMMENT '租户套餐编号',
                               `expire_time` datetime NULL DEFAULT NULL COMMENT '过期时间',
                               `account_count` int NULL DEFAULT -1 COMMENT '用户数量（-1不限制）',
                               `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '租户状态（0正常 1停用）',
                               `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
                               `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                               `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                               `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                               `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                               `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                               PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_tenant
-- ----------------------------
INSERT INTO `sys_tenant` VALUES (1, '000000', '管理组', '15888888888', '熊猫科技有限公司', NULL, NULL, '多租户通用后台管理管理系统', NULL, NULL, 2018611998196109314, NULL, -1, '0', '0', 103, 1, '2026-02-03 05:14:38', NULL, NULL);

-- ----------------------------
-- Table structure for sys_tenant_package
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant_package`;
CREATE TABLE `sys_tenant_package`  (
                                       `package_id` bigint NOT NULL COMMENT '租户套餐id',
                                       `package_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '套餐名称',
                                       `menu_ids` varchar(3000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联菜单id',
                                       `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
                                       `menu_check_strictly` tinyint(1) NULL DEFAULT 1 COMMENT '菜单树选择项是否关联显示',
                                       `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
                                       `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
                                       `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                                       `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                                       `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                       `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                                       `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                       PRIMARY KEY (`package_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租户套餐表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_tenant_package
-- ----------------------------
INSERT INTO `sys_tenant_package` VALUES (2018611998196109314, '测试套餐', '1,100,101,102,103,104,105,106,107,108,109,115,118,123,2,3,500,501,1001,1002,1003,1004,1005,1006,1007,131,1008,1009,1010,1011,1012,130,1013,1014,1015,1016,1017,1018,1019,1020,1021,1022,1023,1024,1025,1026,1027,1028,1029,1030,132,1031,1032,1033,1034,1035,1036,1037,1038,1039,1040,1041,1042,1043,1044,1045,1050,1600,1601,1602,1603,1620,1621,1622,1623,133,1061,1062,1063,1064,1065,1046,1047,1048,113,117,120,1055,1056,1058,1057,1059,1060,116', '测试套餐', 1, '0', '0', 103, 1, '2026-02-03 17:06:19', 1, '2026-02-06 00:37:44');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
                             `user_id` bigint NOT NULL COMMENT '用户ID',
                             `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户编号',
                             `dept_id` bigint NULL DEFAULT NULL COMMENT '部门ID',
                             `user_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户账号',
                             `nick_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户昵称',
                             `user_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'sys_user' COMMENT '用户类型（sys_user系统用户）',
                             `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '用户邮箱',
                             `phonenumber` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '手机号码',
                             `sex` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
                             `avatar` bigint NULL DEFAULT NULL COMMENT '头像地址',
                             `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '密码',
                             `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
                             `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
                             `login_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '最后登录IP',
                             `login_date` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
                             `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                             `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                             `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                             `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                             `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                             `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
                             `open_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '微信用户标识',
                             `user_balance` double(20, 2) NULL DEFAULT 0.00 COMMENT '账户余额',
                             PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, '000000', 103, 'admin', 'admin', 'sys_user', 'ageerle@163.com', '15888888888', '1', NULL, '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', '2026-02-06 00:28:13', 103, 1, '2026-02-05 09:22:12', -1, '2026-02-06 00:28:13', '管理员', NULL, 0.00);
INSERT INTO `sys_user` VALUES (3, '000000', 108, 'test', '本部门及以下 密码666666', 'sys_user', '', '', '0', NULL, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', '127.0.0.1', '2026-02-05 09:22:12', 103, 1, '2026-02-05 09:22:12', 3, '2026-02-05 09:22:12', NULL, NULL, 0.00);
INSERT INTO `sys_user` VALUES (4, '000000', 102, 'test1', '仅本人 密码666666', 'sys_user', '', '', '0', NULL, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', '127.0.0.1', '2026-02-05 09:22:12', 103, 1, '2026-02-05 09:22:12', 4, '2026-02-05 09:22:12', NULL, NULL, 0.00);

-- ----------------------------
-- Table structure for sys_user_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_post`;
CREATE TABLE `sys_user_post`  (
                                  `user_id` bigint NOT NULL COMMENT '用户ID',
                                  `post_id` bigint NOT NULL COMMENT '岗位ID',
                                  PRIMARY KEY (`user_id`, `post_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户与岗位关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user_post
-- ----------------------------
INSERT INTO `sys_user_post` VALUES (1, 1);

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
                                  `user_id` bigint NOT NULL COMMENT '用户ID',
                                  `role_id` bigint NOT NULL COMMENT '角色ID',
                                  PRIMARY KEY (`user_id`, `role_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户和角色关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (1, 1);
INSERT INTO `sys_user_role` VALUES (3, 3);
INSERT INTO `sys_user_role` VALUES (4, 4);
INSERT INTO `sys_user_role` VALUES (2018858143623286785, 2018858143199662082);

-- ----------------------------
-- Table structure for t_workflow
-- ----------------------------
DROP TABLE IF EXISTS `t_workflow`;
CREATE TABLE `t_workflow`  (
                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
                               `uuid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'uuid',
                               `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '标题',
                               `user_id` bigint NOT NULL DEFAULT 0 COMMENT '用户ID',
                               `is_public` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否公开',
                               `is_enable` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
                               `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '备注',
                               `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 默认0不删除',
                               `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户编号',
                               PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 120 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工作流定义（用户定义的工作流）| Workflow Definition' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_workflow
-- ----------------------------
INSERT INTO `t_workflow` VALUES (119, '7c95c7892dd544788d90e49ce2fad966', '测试工作流', 1, 1, 1, '2025-11-07 16:44:41', '2025-11-07 16:44:41', '测试工作流', 0, '000000');

-- ----------------------------
-- Table structure for t_workflow_component
-- ----------------------------
DROP TABLE IF EXISTS `t_workflow_component`;
CREATE TABLE `t_workflow_component`  (
                                         `id` bigint NOT NULL AUTO_INCREMENT,
                                         `uuid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
                                         `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
                                         `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
                                         `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
                                         `display_order` int NOT NULL DEFAULT 0,
                                         `is_enable` tinyint(1) NOT NULL DEFAULT 0,
                                         `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         `is_deleted` tinyint(1) NOT NULL DEFAULT 0,
                                         `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户编号',
                                         PRIMARY KEY (`id`) USING BTREE,
                                         INDEX `idx_display_order`(`display_order` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工作流组件库 | Workflow Component' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_workflow_component
-- ----------------------------
INSERT INTO `t_workflow_component` VALUES (17, '5cd68dccbbb411f0bb7840c2ba9a7fbc', 'Start', '开始', '流程由此开始', 0, 1, '2025-11-07 16:32:49', '2025-11-07 16:32:49', 0, '000000');
INSERT INTO `t_workflow_component` VALUES (18, '5cd6ac69bbb411f0bb7840c2ba9a7fbc', 'End', '结束', '流程由此结束', 0, 1, '2025-11-07 16:32:49', '2025-11-07 16:32:49', 0, '000000');
INSERT INTO `t_workflow_component` VALUES (19, '5cd6c8eabbb411f0bb7840c2ba9a7fbc', 'Answer', '生成回答', '调用大语言模型回答问题', 0, 1, '2025-11-07 16:32:49', '2025-11-07 16:32:49', 0, '000000');

-- ----------------------------
-- Table structure for t_workflow_edge
-- ----------------------------
DROP TABLE IF EXISTS `t_workflow_edge`;
CREATE TABLE `t_workflow_edge`  (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `uuid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '边唯一标识',
                                    `workflow_id` bigint NOT NULL DEFAULT 0 COMMENT '所属工作流定义 id',
                                    `source_node_uuid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '起始节点 uuid',
                                    `source_handle` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '起始锚点标识',
                                    `target_node_uuid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '目标节点 uuid',
                                    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 已删',
                                    `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户编号',
                                    PRIMARY KEY (`id`) USING BTREE,
                                    INDEX `idx_workflow_edge_workflow_id`(`workflow_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 201 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工作流定义的边 | Edge of Workflow Definition' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_workflow_edge
-- ----------------------------
INSERT INTO `t_workflow_edge` VALUES (199, '6eddc13ea3e44baebaaca5bbc7f5ba8f', 119, 'f4660cebe26b439f8264ad0111b56c85', '', '1b9c6f83822546b9af497e48108cea71', '2026-02-02 18:21:18', '2026-02-02 18:21:18', 0, '000000');
INSERT INTO `t_workflow_edge` VALUES (200, '9f500dce66354be99f14f53a7dbaa6c7', 119, '1b9c6f83822546b9af497e48108cea71', '', 'b3a95e6a16104598909269ed8cb3bb04', '2026-02-02 18:21:18', '2026-02-02 18:21:18', 0, '000000');

-- ----------------------------
-- Table structure for t_workflow_node
-- ----------------------------
DROP TABLE IF EXISTS `t_workflow_node`;
CREATE TABLE `t_workflow_node`  (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                    `uuid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '节点唯一标识',
                                    `workflow_id` bigint NOT NULL DEFAULT 0 COMMENT '所属工作流定义 id',
                                    `workflow_component_id` bigint NOT NULL DEFAULT 0 COMMENT '引用的组件 id',
                                    `user_id` bigint NOT NULL DEFAULT 0 COMMENT '创建人',
                                    `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '节点标题',
                                    `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '节点备注',
                                    `input_config` json NOT NULL COMMENT '输入参数模板，例：{\"params\":[{\"name\":\"user_define_param01\",\"type\":\"string\"}]}',
                                    `node_config` json NULL COMMENT '节点执行配置，例：{\"params\":[{\"prompt\":\"Summarize the following content:{user_define_param01}\"}]}',
                                    `position_x` double NOT NULL DEFAULT 0 COMMENT '画布 x 坐标',
                                    `position_y` double NOT NULL DEFAULT 0 COMMENT '画布 y 坐标',
                                    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 已删',
                                    `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户编号',
                                    PRIMARY KEY (`id`) USING BTREE,
                                    INDEX `idx_workflow_node_workflow_id`(`workflow_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 272 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工作流定义的节点 | Node of Workflow Definition' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_workflow_node
-- ----------------------------
INSERT INTO `t_workflow_node` VALUES (269, 'f4660cebe26b439f8264ad0111b56c85', 119, 17, 0, '开始', '用户输入', '{\"ref_inputs\": [], \"user_inputs\": [{\"name\": \"var_user_input\", \"type\": 1, \"uuid\": \"dc9590d781764ace943bf03b383e742b\", \"title\": \"用户输入\", \"required\": false, \"max_length\": 1000}]}', '{}', -336.4794845046116, -67.23231445984582, '2025-11-07 16:44:41', '2026-02-02 18:21:17', 0, '000000');
INSERT INTO `t_workflow_node` VALUES (270, 'b3a95e6a16104598909269ed8cb3bb04', 119, 18, 0, '结束', '', '{\"ref_inputs\": [], \"user_inputs\": []}', '{\"result\": \"\"}', 186.97027554892657, 275.3464833818887, '2026-02-02 18:21:17', '2026-02-02 18:21:17', 0, '000000');
INSERT INTO `t_workflow_node` VALUES (271, '1b9c6f83822546b9af497e48108cea71', 119, 19, 0, '生成回答', '', '{\"ref_inputs\": [], \"user_inputs\": []}', '{\"prompt\": \"\", \"category\": \"\", \"model_name\": \"\"}', -46.766188892645644, 7.548790841996912, '2026-02-02 18:21:17', '2026-02-02 18:21:17', 0, '000000');

-- ----------------------------
-- Table structure for t_workflow_runtime
-- ----------------------------
DROP TABLE IF EXISTS `t_workflow_runtime`;
CREATE TABLE `t_workflow_runtime`  (
                                       `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                       `uuid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '运行实例唯一标识',
                                       `user_id` bigint NOT NULL DEFAULT 0 COMMENT '启动人',
                                       `workflow_id` bigint NOT NULL DEFAULT 0 COMMENT '对应工作流定义 id',
                                       `input` json NULL COMMENT '运行输入，例：{\"userInput01\":\"text01\",\"userInput02\":true,\"userInput03\":10,\"userInput04\":[\"selectedA\",\"selectedB\"],\"userInput05\":[\"https://a.com/a.xlsx\",\"https://a.com/b.png\"]}',
                                       `output` json NULL COMMENT '运行输出，成功或失败的结果',
                                       `status` smallint NOT NULL DEFAULT 1 COMMENT '执行状态：1 就绪，2 执行中，3 成功，4 失败',
                                       `status_remark` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '状态补充说明，如失败原因',
                                       `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 已删',
                                       `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户编号',
                                       PRIMARY KEY (`id`) USING BTREE,
                                       INDEX `idx_workflow_runtime_workflow_id`(`workflow_id` ASC) USING BTREE,
                                       INDEX `idx_workflow_runtime_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 297 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工作流实例（运行时）| Workflow Runtime' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_workflow_runtime
-- ----------------------------

-- ----------------------------
-- Table structure for t_workflow_runtime_node
-- ----------------------------
DROP TABLE IF EXISTS `t_workflow_runtime_node`;
CREATE TABLE `t_workflow_runtime_node`  (
                                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                            `uuid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '节点运行实例唯一标识',
                                            `user_id` bigint NOT NULL DEFAULT 0 COMMENT '创建人',
                                            `workflow_runtime_id` bigint NOT NULL DEFAULT 0 COMMENT '所属运行实例 id',
                                            `node_id` bigint NOT NULL DEFAULT 0 COMMENT '对应工作流定义里的节点 id',
                                            `input` json NULL COMMENT '节点本次输入数据',
                                            `output` json NULL COMMENT '节点本次输出数据',
                                            `status` smallint NOT NULL DEFAULT 1 COMMENT '节点执行状态：1 进行中，2 失败，3 成功',
                                            `status_remark` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '状态补充说明，如失败堆栈',
                                            `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                            `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常，1 已删',
                                            `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户编号',
                                            PRIMARY KEY (`id`) USING BTREE,
                                            INDEX `idx_runtime_node_runtime_id`(`workflow_runtime_id` ASC) USING BTREE,
                                            INDEX `idx_runtime_node_node_id`(`node_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 805 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工作流实例（运行时）- 节点 | Workflow Runtime Node' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_workflow_runtime_node
-- ----------------------------

-- ----------------------------
-- Table structure for test_demo
-- ----------------------------
DROP TABLE IF EXISTS `test_demo`;
CREATE TABLE `test_demo`  (
                              `id` bigint NOT NULL COMMENT '主键',
                              `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户编号',
                              `dept_id` bigint NULL DEFAULT NULL COMMENT '部门id',
                              `user_id` bigint NULL DEFAULT NULL COMMENT '用户id',
                              `order_num` int NULL DEFAULT 0 COMMENT '排序号',
                              `test_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'key键',
                              `value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '值',
                              `version` int NULL DEFAULT 0 COMMENT '版本',
                              `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                              `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                              `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
                              `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                              `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
                              `del_flag` int NULL DEFAULT 0 COMMENT '删除标志',
                              PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '测试单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of test_demo
-- ----------------------------
INSERT INTO `test_demo` VALUES (1, '000000', 102, 4, 1, '测试数据权限', '测试', 0, 103, '2026-02-03 05:14:53', 1, NULL, NULL, 0);
INSERT INTO `test_demo` VALUES (2, '000000', 102, 3, 2, '子节点1', '111', 0, 103, '2026-02-03 05:14:53', 1, NULL, NULL, 0);
INSERT INTO `test_demo` VALUES (3, '000000', 102, 3, 3, '子节点2', '222', 0, 103, '2026-02-03 05:14:53', 1, NULL, NULL, 0);
INSERT INTO `test_demo` VALUES (4, '000000', 108, 4, 4, '测试数据', 'demo', 0, 103, '2026-02-03 05:14:53', 1, NULL, NULL, 0);
INSERT INTO `test_demo` VALUES (5, '000000', 108, 3, 13, '子节点11', '1111', 0, 103, '2026-02-03 05:14:53', 1, NULL, NULL, 0);
INSERT INTO `test_demo` VALUES (6, '000000', 108, 3, 12, '子节点22', '2222', 0, 103, '2026-02-03 05:14:53', 1, NULL, NULL, 0);
INSERT INTO `test_demo` VALUES (7, '000000', 108, 3, 11, '子节点33', '3333', 0, 103, '2026-02-03 05:14:53', 1, NULL, NULL, 0);
INSERT INTO `test_demo` VALUES (8, '000000', 108, 3, 10, '子节点44', '4444', 0, 103, '2026-02-03 05:14:53', 1, NULL, NULL, 0);
INSERT INTO `test_demo` VALUES (9, '000000', 108, 3, 9, '子节点55', '5555', 0, 103, '2026-02-03 05:14:53', 1, NULL, NULL, 0);
INSERT INTO `test_demo` VALUES (10, '000000', 108, 3, 8, '子节点66', '6666', 0, 103, '2026-02-03 05:14:53', 1, NULL, NULL, 0);
INSERT INTO `test_demo` VALUES (11, '000000', 108, 3, 7, '子节点77', '7777', 0, 103, '2026-02-03 05:14:53', 1, NULL, NULL, 0);
INSERT INTO `test_demo` VALUES (12, '000000', 108, 3, 6, '子节点88', '8888', 0, 103, '2026-02-03 05:14:53', 1, NULL, NULL, 0);
INSERT INTO `test_demo` VALUES (13, '000000', 108, 3, 5, '子节点99', '9999', 0, 103, '2026-02-03 05:14:53', 1, NULL, NULL, 0);

-- ----------------------------
-- Table structure for test_leave
-- ----------------------------
DROP TABLE IF EXISTS `test_leave`;
CREATE TABLE `test_leave`  (
                               `id` bigint NOT NULL COMMENT 'id',
                               `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户编号',
                               `apply_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '申请编号',
                               `leave_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '请假类型',
                               `start_date` datetime NOT NULL COMMENT '开始时间',
                               `end_date` datetime NOT NULL COMMENT '结束时间',
                               `leave_days` int NOT NULL COMMENT '请假天数',
                               `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请假原因',
                               `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '状态',
                               `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                               `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                               `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                               `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                               `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                               PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '请假申请表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of test_leave
-- ----------------------------
INSERT INTO `test_leave` VALUES (2015843012517695489, '000000', '1769449401394', '2', '2026-01-27 01:42:57', '2026-02-26 01:42:57', 31, '123', 'draft', 103, 1, '2026-01-27 01:43:21', 1, '2026-01-27 01:43:21');
INSERT INTO `test_leave` VALUES (2015843045463953410, '000000', '1769449409249', '2', '2026-01-27 01:42:57', '2026-02-26 01:42:57', 31, '123', 'draft', 103, 1, '2026-01-27 01:43:29', 1, '2026-01-27 01:43:29');
INSERT INTO `test_leave` VALUES (2018244808141836290, '000000', '1770022034093', '3', '2026-02-02 16:46:36', '2026-03-27 16:46:36', 54, NULL, 'draft', 103, 1, '2026-02-02 16:47:14', 1, '2026-02-02 16:47:14');
INSERT INTO `test_leave` VALUES (2018244840488308738, '000000', '1770022041805', '3', '2026-02-02 16:46:36', '2026-03-27 16:46:36', 54, NULL, 'draft', 103, 1, '2026-02-02 16:47:22', 1, '2026-02-02 16:47:22');

-- ----------------------------
-- Table structure for test_tree
-- ----------------------------
DROP TABLE IF EXISTS `test_tree`;
CREATE TABLE `test_tree`  (
                              `id` bigint NOT NULL COMMENT '主键',
                              `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户编号',
                              `parent_id` bigint NULL DEFAULT 0 COMMENT '父id',
                              `dept_id` bigint NULL DEFAULT NULL COMMENT '部门id',
                              `user_id` bigint NULL DEFAULT NULL COMMENT '用户id',
                              `tree_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '值',
                              `version` int NULL DEFAULT 0 COMMENT '版本',
                              `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                              `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                              `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
                              `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                              `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
                              `del_flag` int NULL DEFAULT 0 COMMENT '删除标志',
                              PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '测试树表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of test_tree
-- ----------------------------
INSERT INTO `test_tree` VALUES (1, '000000', 0, 102, 4, '测试数据权限', 0, 103, '2026-02-03 05:14:53', 1, NULL, NULL, 0);
INSERT INTO `test_tree` VALUES (2, '000000', 1, 102, 3, '子节点1', 0, 103, '2026-02-03 05:14:53', 1, NULL, NULL, 0);
INSERT INTO `test_tree` VALUES (3, '000000', 2, 102, 3, '子节点2', 0, 103, '2026-02-03 05:14:53', 1, NULL, NULL, 0);
INSERT INTO `test_tree` VALUES (4, '000000', 0, 108, 4, '测试树1', 0, 103, '2026-02-03 05:14:53', 1, NULL, NULL, 0);
INSERT INTO `test_tree` VALUES (5, '000000', 4, 108, 3, '子节点11', 0, 103, '2026-02-03 05:14:53', 1, NULL, NULL, 0);
INSERT INTO `test_tree` VALUES (6, '000000', 4, 108, 3, '子节点22', 0, 103, '2026-02-03 05:14:54', 1, NULL, NULL, 0);
INSERT INTO `test_tree` VALUES (7, '000000', 4, 108, 3, '子节点33', 0, 103, '2026-02-03 05:14:54', 1, NULL, NULL, 0);
INSERT INTO `test_tree` VALUES (8, '000000', 5, 108, 3, '子节点44', 0, 103, '2026-02-03 05:14:54', 1, NULL, NULL, 0);
INSERT INTO `test_tree` VALUES (9, '000000', 6, 108, 3, '子节点55', 0, 103, '2026-02-03 05:14:54', 1, NULL, NULL, 0);
INSERT INTO `test_tree` VALUES (10, '000000', 7, 108, 3, '子节点66', 0, 103, '2026-02-03 05:14:54', 1, NULL, NULL, 0);
INSERT INTO `test_tree` VALUES (11, '000000', 7, 108, 3, '子节点77', 0, 103, '2026-02-03 05:14:54', 1, NULL, NULL, 0);
INSERT INTO `test_tree` VALUES (12, '000000', 10, 108, 3, '子节点88', 0, 103, '2026-02-03 05:14:54', 1, NULL, NULL, 0);
INSERT INTO `test_tree` VALUES (13, '000000', 10, 108, 3, '子节点99', 0, 103, '2026-02-03 05:14:54', 1, NULL, NULL, 0);

SET FOREIGN_KEY_CHECKS = 1;


-- MCP 模块数据库表结构
-- 版本: V3.0.0
-- 描述: MCP 工具管理和 MCP 市场管理表

-- ----------------------------
-- MCP 工具表
-- ----------------------------
DROP TABLE IF EXISTS `mcp_tool_info`;
CREATE TABLE `mcp_tool_info`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '工具ID',
    `name`        varchar(200) NOT NULL COMMENT '工具名称',
    `description` text COMMENT '工具描述',
    `type`        varchar(20) DEFAULT 'LOCAL' COMMENT '工具类型：LOCAL-本地, REMOTE-远程, BUILTIN-内置',
    `status`      varchar(20) DEFAULT 'ENABLED' COMMENT '状态：ENABLED-启用, DISABLED-禁用',
    `config_json` text COMMENT '配置信息（JSON格式）',
    `tenant_id`   varchar(20) DEFAULT '000000' COMMENT '租户编号',
    `create_dept` bigint      DEFAULT NULL COMMENT '创建部门',
    `create_by`   bigint      DEFAULT NULL COMMENT '创建者',
    `create_time` datetime    DEFAULT NULL COMMENT '创建时间',
    `update_by`   bigint      DEFAULT NULL COMMENT '更新者',
    `update_time` datetime    DEFAULT NULL COMMENT '更新时间',
    `del_flag`    char(1)     DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
    PRIMARY KEY (`id`),
    KEY           `idx_name` (`name`),
    KEY           `idx_type` (`type`),
    KEY           `idx_status` (`status`),
    KEY           `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MCP工具表';

-- ----------------------------
-- MCP 市场表
-- ----------------------------
DROP TABLE IF EXISTS `mcp_market_info`;
CREATE TABLE `mcp_market_info`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '市场ID',
    `name`        varchar(200) NOT NULL COMMENT '市场名称',
    `url`         varchar(500) NOT NULL COMMENT '市场URL',
    `description` text COMMENT '市场描述',
    `auth_config` text COMMENT '认证配置（JSON格式）',
    `status`      varchar(20) DEFAULT 'ENABLED' COMMENT '状态：ENABLED-启用, DISABLED-禁用',
    `tenant_id`   varchar(20) DEFAULT '000000' COMMENT '租户编号',
    `create_dept` bigint      DEFAULT NULL COMMENT '创建部门',
    `create_by`   bigint      DEFAULT NULL COMMENT '创建者',
    `create_time` datetime    DEFAULT NULL COMMENT '创建时间',
    `update_by`   bigint      DEFAULT NULL COMMENT '更新者',
    `update_time` datetime    DEFAULT NULL COMMENT '更新时间',
    `del_flag`    char(1)     DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
    PRIMARY KEY (`id`),
    KEY           `idx_name` (`name`),
    KEY           `idx_status` (`status`),
    KEY           `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MCP市场表';

-- ----------------------------
-- MCP 市场工具关联表
-- ----------------------------
DROP TABLE IF EXISTS `mcp_market_tool`;
CREATE TABLE `mcp_market_tool`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `market_id`        bigint       NOT NULL COMMENT '市场ID',
    `tool_name`        varchar(200) NOT NULL COMMENT '工具名称',
    `tool_description` text COMMENT '工具描述',
    `tool_version`     varchar(50) COMMENT '工具版本',
    `tool_metadata`    json COMMENT '工具元数据（JSON格式）',
    `is_loaded`        tinyint(1) DEFAULT 0 COMMENT '是否已加载到本地',
    `local_tool_id`    bigint COMMENT '关联的本地工具ID',
    `create_time`      datetime DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY                `idx_market_id` (`market_id`),
    KEY                `idx_tool_name` (`tool_name`),
    KEY                `idx_is_loaded` (`is_loaded`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MCP市场工具关联表';



-- MCP 模块菜单权限 SQL
-- 版本: V3.0.1
-- 描述: MCP 工具管理和 MCP 市场管理菜单权限
-- 菜单 ID 规划: 2000-2199

-- ----------------------------
-- MCP 主菜单
-- ----------------------------
INSERT INTO sys_menu
VALUES (2000, 'MCP管理', 0, 5, 'mcp', '', '', 1, 0, 'M', '0', '0', '',
        'mdi:robot-industrial', 103, 1, NOW(), NULL, NULL, 'MCP模块管理菜单');

-- ----------------------------
-- MCP 工具管理
-- ----------------------------
INSERT INTO sys_menu
VALUES (2001, 'MCP工具管理', 2000, 1, 'tool', 'mcp/tool/index', '', 1, 0, 'C', '0',
        '0', 'mcp:tool:list', 'material-symbols:tools-hammer-outline', 103, 1, NOW(), NULL,
        NULL, 'MCP工具管理菜单');

-- MCP 工具管理按钮权限
INSERT INTO sys_menu
VALUES (2002, 'MCP工具查询', 2001, 1, '#', '', '', 1, 0, 'F', '0', '0',
        'mcp:tool:query', '#', 103, 1, NOW(), NULL, NULL, '');
INSERT INTO sys_menu
VALUES (2003, 'MCP工具新增', 2001, 2, '#', '', '', 1, 0, 'F', '0', '0',
        'mcp:tool:add', '#', 103, 1, NOW(), NULL, NULL, '');
INSERT INTO sys_menu
VALUES (2004, 'MCP工具修改', 2001, 3, '#', '', '', 1, 0, 'F', '0', '0',
        'mcp:tool:edit', '#', 103, 1, NOW(), NULL, NULL, '');
INSERT INTO sys_menu
VALUES (2005, 'MCP工具删除', 2001, 4, '#', '', '', 1, 0, 'F', '0', '0',
        'mcp:tool:remove', '#', 103, 1, NOW(), NULL, NULL, '');
INSERT INTO sys_menu
VALUES (2006, 'MCP工具测试', 2001, 5, '#', '', '', 1, 0, 'F', '0', '0',
        'mcp:tool:test', '#', 103, 1, NOW(), NULL, NULL, '');
INSERT INTO sys_menu
VALUES (2007, 'MCP工具导出', 2001, 6, '#', '', '', 1, 0, 'F', '0', '0',
        'mcp:tool:export', '#', 103, 1, NOW(), NULL, NULL, '');

-- ----------------------------
-- MCP 市场管理
-- ----------------------------
INSERT INTO sys_menu
VALUES (2010, 'MCP市场管理', 2000, 2, 'market', 'mcp/market/index', '', 1, 0, 'C', '0',
        '0', 'mcp:market:list', 'mdi:storefront-outline', 103, 1, NOW(), NULL, NULL,
        'MCP市场管理菜单');

-- MCP 市场管理按钮权限
INSERT INTO sys_menu
VALUES (2011, 'MCP市场查询', 2010, 1, '#', '', '', 1, 0, 'F', '0', '0',
        'mcp:market:query', '#', 103, 1, NOW(), NULL, NULL, '');
INSERT INTO sys_menu
VALUES (2012, 'MCP市场新增', 2010, 2, '#', '', '', 1, 0, 'F', '0', '0',
        'mcp:market:add', '#', 103, 1, NOW(), NULL, NULL, '');
INSERT INTO sys_menu
VALUES (2013, 'MCP市场修改', 2010, 3, '#', '', '', 1, 0, 'F', '0', '0',
        'mcp:market:edit', '#', 103, 1, NOW(), NULL, NULL, '');
INSERT INTO sys_menu
VALUES (2014, 'MCP市场删除', 2010, 4, '#', '', '', 1, 0, 'F', '0', '0',
        'mcp:market:remove', '#', 103, 1, NOW(), NULL, NULL, '');
INSERT INTO sys_menu
VALUES (2015, 'MCP市场刷新', 2010, 5, '#', '', '', 1, 0, 'F', '0', '0',
        'mcp:market:refresh', '#', 103, 1, NOW(), NULL, NULL, '');
INSERT INTO sys_menu
VALUES (2016, 'MCP工具加载', 2010, 6, '#', '', '', 1, 0, 'F', '0', '0',
        'mcp:market:load', '#', 103, 1, NOW(), NULL, NULL, '');
INSERT INTO sys_menu
VALUES (2017, 'MCP市场导出', 2010, 7, '#', '', '', 1, 0, 'F', '0', '0',
        'mcp:market:export', '#', 103, 1, NOW(), NULL, NULL, '');

-- ----------------------------
-- MCP 配置管理 (可选，预留扩展)
-- ----------------------------
-- INSERT INTO sys_menu VALUES (2020, 'MCP配置管理', 2000, 3, 'config', 'mcp/config/index', '', 1, 0, 'C', '0',
--                              '0', 'mcp:config:list', 'ant-design:setting-outlined', 103, 1, NOW(), NULL, NULL,
--                              'MCP配置管理菜单');


