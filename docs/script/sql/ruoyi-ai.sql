/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80045 (8.0.45)
 Source Host           : localhost:3306
 Source Schema         : ruoyi-ai

 Target Server Type    : MySQL
 Target Server Version : 80045 (8.0.45)
 File Encoding         : 65001

 Date: 14/07/2026 21:36:39
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for agent_info
-- ----------------------------
DROP TABLE IF EXISTS `agent_info`;
CREATE TABLE `agent_info`  (
                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '智能体ID',
                               `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户ID',
                               `agent_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '智能体名称',
                               `agent_describe` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '智能体描述（下拉展示用）',
                               `agent_show` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '展示图标/头像URL',
                               `model_id` bigint NOT NULL COMMENT '绑定的聊天模型ID（chat_model.id, category=chat）',
                               `enable_thinking` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '是否启用深度思考(ReAct多子Agent)：0 否 1 是',
                               `system_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '自定义系统提示词',
                               `mcp_tool_ids` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联MCP工具ID列表（JSON数组，[Long]）',
                               `skill_names` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联磁盘技能名列表（JSON数组，[String]）',
                               `knowledge_ids` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联知识库ID列表（JSON数组，[Long]）',
                               `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '状态：0 正常 1 停用',
                               `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
                               `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                               `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                               `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                               `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                               `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                               PRIMARY KEY (`id`) USING BTREE,
                               INDEX `idx_agent_tenant_id`(`tenant_id` ASC) USING BTREE,
                               INDEX `idx_agent_model_id`(`model_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '智能体信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of agent_info
-- ----------------------------
INSERT INTO `agent_info` VALUES (1, 0, '对话智能体', '对话智能体', NULL, 2000585866022060033, '0', '你是一个乐于助人的通用 AI 助手，请用简洁、准确的中文回答用户的问题。', '[]', '[\"docx\"]', '[]', '0', '系统初始化的默认智能体，自动绑定首个启用的对话模型', 103, 1, '2026-07-14 16:12:21', 1, '2026-07-14 16:18:04');

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
                                 `total_tokens` int NULL DEFAULT 0 COMMENT '累计 Tokens',
                                 `model_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模型名称',
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
INSERT INTO `chat_message` VALUES (2076944340894203905, 2076944338398593026, 1, '你好', 'user', 0, 'deepseek-v4-flash', 103, 1, '2026-07-14 16:18:13', 1, '2026-07-14 16:18:13', NULL, 0);
INSERT INTO `chat_message` VALUES (2076944348947267585, 2076944338398593026, 1, '你好！有什么可以帮你的吗？', 'assistant', 0, 'deepseek-v4-flash', -1, -1, '2026-07-14 16:18:15', -1, '2026-07-14 16:18:15', NULL, 0);

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
                               `model_dimension` int NULL DEFAULT NULL COMMENT '模型维度',
                               `model_show` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否显示',
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
INSERT INTO `chat_model` VALUES (2000585866022060033, 'chat', 'deepseek-v4-flash', 'deepseek', 'deepseek-v4-flash', NULL, 'Y', 'https://api.deepseek.com', 'sk_xx', 103, 1, '2025-12-15 23:16:54', 1, '2026-03-15 19:18:48', '对话模型', 0);
INSERT INTO `chat_model` VALUES (2007528268536287233, 'vector', 'embedding-3', 'zhipu', 'embedding-3', 2048, 'N', 'https://open.bigmodel.cn', 'sk_xx', 103, 1, '2026-01-04 03:03:32', 1, '2026-03-15 19:18:51', '向量模型', 0);
INSERT INTO `chat_model` VALUES (2045071617578237953, 'rerank', 'rerank', 'zhipu', 'rerank', NULL, 'N', 'https://open.bigmodel.cn', 'sk_xx', 103, 1, '2026-04-17 17:27:24', 1, '2026-04-20 15:21:48', '重排序模型', 0);
INSERT INTO `chat_model` VALUES (2000585866022060003, 'chat', 'deepseek-ai/deepseek-v4-flash', 'atlas', 'deepseek-v4-flash', NULL, 'Y', 'https://api.atlascloud.ai/v1', 'sk_xx', 103, 1, '2025-12-15 23:16:54', 1, '2026-03-15 19:18:48', '对话模型', 0);
INSERT INTO `chat_model` VALUES (2050000000000000001, 'video', 'bytedance/seedance-2.0/text-to-video', 'atlas', 'Seedance 2.0 文生视频（字节跳动）', NULL, 'Y', 'https://api.atlascloud.ai/v1', 'sk_xx', 103, 1, '2026-06-17 18:39:20', 1, '2026-06-17 18:39:20', 'Atlas Cloud 视频模型 - Seedance 2.0', 0);
INSERT INTO `chat_model` VALUES (2060622000000000001, 'image', 'openai/gpt-image-2/text-to-image', 'atlas', 'GPT-IMAGE-2 文生图', NULL, 'Y', 'https://api.atlascloud.ai/v1', 'sk_xx', 103, 1, '2026-06-22 14:45:54', 1, '2026-06-22 14:45:54', 'Atlas Cloud 图片模型 - 文生图，支持三视图、角色设定、场景图', 0);
INSERT INTO `chat_model` VALUES (2060622000000000002, 'image', 'openai/gpt-image-2/edit', 'atlas', 'GPT-IMAGE-2 图生图编辑', NULL, 'Y', 'https://api.atlascloud.ai/v1', 'sk_xx', 103, 1, '2026-06-22 14:45:54', 1, '2026-06-22 14:45:54', 'Atlas Cloud 图片模型 - 图生图/编辑，支持基于参考图修改风格、纹理等', 0);
INSERT INTO `chat_model` VALUES (2060622000000000003, 'video', 'bytedance/seedance-2.0/image-to-video', 'atlas', 'Seedance 2.0 图生视频（字节跳动）', NULL, 'Y', 'https://api.atlascloud.ai/v1', 'sk_xx', 103, 1, '2026-06-22 17:24:07', 1, '2026-06-22 17:24:07', 'Atlas Cloud 视频模型 - Seedance 2.0 图生视频，接收参考图+提示词生成视频', 0);
INSERT INTO `chat_model` VALUES (2060622000000000004, 'video', 'bytedance/seedance-2.0/reference-to-video', 'atlas', 'Seedance 2.0 多参考图生视频（字节跳动）', NULL, 'Y', 'https://api.atlascloud.ai/v1', 'sk_xx', 103, 1, '2026-06-22 20:24:44', 1, '2026-06-22 20:24:44', 'Atlas Cloud 视频模型 - Seedance 2.0 多参考图生视频，接收多张参考图+带@imageN标记的提示词生成视频', 0);
INSERT INTO `chat_model` VALUES (2070700000000000002, 'chat', 'dify-chat', 'dify', 'Dify Chat App', NULL, 'Y', 'https://api.dify.ai/v1', '替换为你的DIFY_APP_API_KEY', 103, 1, '2026-07-14 11:03:44', 1, '2026-07-14 11:03:44', 'Dify 聊天应用；api_key 为 Dify App API Key，model_name 可按应用名修改', 0);
INSERT INTO `chat_model` VALUES (2070700000000000004, 'chat', '替换为你的COZE_BOT_ID', 'coze', 'Coze Bot', NULL, 'Y', 'https://api.coze.cn', '替换为你的COZE_PAT', 103, 1, '2026-07-14 11:03:38', 1, '2026-07-14 11:03:38', 'Coze 聊天 Bot；model_name 为 Coze Bot ID，api_key 为 PAT 或 OAuth access token', 0);
INSERT INTO `chat_model` VALUES (2070700000000000010, 'audio', 'bytedance/seed-audio-1.0', 'atlas', 'Seed Audio 1.0 语音生成（字节跳动）', NULL, 'Y', 'https://api.atlascloud.ai/v1', 'sk_xx', 103, 1, '2026-07-16 18:00:00', 1, '2026-07-16 18:00:00', 'Atlas Cloud 语音模型 - 支持多角色对白配音，references 指定 speaker 音色，text 中用 @audioN 引用', 0);

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
                                  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志',
                                  `update_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新IP',
                                  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户Id',
                                  PRIMARY KEY (`id`) USING BTREE,
                                  UNIQUE INDEX `unique_provider_code`(`provider_code` ASC, `tenant_id` ASC, `del_flag` ASC) USING BTREE,
                                  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2070700000000000004 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '厂商管理表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of chat_provider
-- ----------------------------
INSERT INTO `chat_provider` VALUES (11, '深度求索', 'deepseek', 'https://example-1234567890.cos.ap-guangzhou.myqcloud.com/2026/04/19/5ba8c30f153246898a4d7dc7b846de8d.png', 'DeepSeek官方API', 'https://api.deepseek.com', '0', 0, 103, '2026-04-19 12:52:34', '1', '1', '2026-04-19 13:13:25', 'DeepSeek官方API', NULL, '0', NULL, 0);
INSERT INTO `chat_provider` VALUES (12, '智谱AI', 'zhipu', 'https://example-1234567890.cos.ap-guangzhou.myqcloud.com/2026/04/19/da071783c9284fdd9ed1ce1b57b3c75c.png', '智谱AI大模型服务', 'https://open.bigmodel.cn', '0', 4, 103, '2025-12-14 21:48:11', '1', '1', '2026-04-19 13:14:00', '智谱AI厂商', NULL, '0', NULL, 0);
INSERT INTO `chat_provider` VALUES (13, '小米MIMO', 'xiaomi', 'https://example-1234567890.cos.ap-guangzhou.myqcloud.com/2026/04/19/18dd39365ce244e3ae5e030da036760e.png', '小米官方API', 'https://api.xiaomimimo.com/v1', '0', 3, 103, '2026-04-19 12:48:24', '1', '1', '2026-04-19 13:14:22', '小米官方API', NULL, '0', NULL, 0);
INSERT INTO `chat_provider` VALUES (14, '阿里云百炼', 'qianwen', 'https://example-1234567890.cos.ap-guangzhou.myqcloud.com/2026/02/25/de2aa7e649de44f3ba5c6380ac6acd04.png', '阿里云百炼大模型服务', 'https://dashscope.aliyuncs.com', '0', 2, 103, '2025-12-14 21:48:11', '1', '1', '2026-02-25 20:49:13', '阿里云厂商', NULL, '0', NULL, 0);
INSERT INTO `chat_provider` VALUES (15, 'Atlas Cloud', 'atlas', 'https://example-1234567890.cos.ap-guangzhou.myqcloud.com/2026/04/19/atlascloud.png', '全模态AI推理平台', 'https://api.atlascloud.ai/v1', '0', 5, 103, '2025-12-15 23:13:42', '1', '1', '2026-02-25 20:49:01', 'Atlas Cloud AI平台', NULL, '0', NULL, 0);
INSERT INTO `chat_provider` VALUES (16, 'MiniMax', 'minimax', 'https://example-1234567890.cos.ap-guangzhou.myqcloud.com/2026/04/19/fdc712e90e0e4d78b05862ad230884e5.png', 'MiniMax model service supporting MiniMax-M3 and MiniMax-M2.7', 'https://api.minimax.io/v1', '0', 6, 103, '2026-04-19 12:50:12', '1', '1', '2026-04-19 13:14:59', 'MiniMax provider', NULL, '0', NULL, 0);
INSERT INTO `chat_provider` VALUES (17, 'ollama', 'ollama', 'https://example-1234567890.cos.ap-guangzhou.myqcloud.com/2026/02/25/afecabebc8014d80b0f06b4796a74c5d.png', 'ollama大模型', 'http://127.0.0.1:11434', '0', 7, 103, '2025-12-14 21:48:11', '1', '1', '2026-02-25 20:48:48', 'ollama厂商', NULL, '0', NULL, 0);
INSERT INTO `chat_provider` VALUES (18, '自定义厂商', 'custom_api', 'https://example-1234567890.cos.ap-guangzhou.myqcloud.com/2026/04/19/c1a8e122510f4e2f90deb36958af710b.png', 'OPENAI兼容格式', '自定义', '0', 8, 103, '2026-04-19 12:35:57', '1', '1', '2026-04-19 13:17:20', 'OPENAI兼容格式', NULL, '0', NULL, 0);
INSERT INTO `chat_provider` VALUES (2070700000000000001, 'Dify', 'dify', NULL, 'Dify 应用 API 服务', 'https://api.dify.ai/v1', '0', 9, 103, '2026-07-14 11:03:44', '1', '1', '2026-07-14 11:03:44', 'Dify 应用 API 服务商', NULL, '0', NULL, 0);
INSERT INTO `chat_provider` VALUES (2070700000000000003, 'Coze', 'coze', NULL, 'Coze Bot API 服务', 'https://api.coze.cn', '0', 10, 103, '2026-07-14 11:03:38', '1', '1', '2026-07-14 11:03:38', 'Coze Bot API 服务商', NULL, '0', NULL, 0);

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
INSERT INTO `chat_session` VALUES (2076944338398593026, 1, '你好', '你好', '103', 1, '2026-07-14 16:18:13', 1, '2026-07-14 16:18:13', '你好', NULL, 0);
INSERT INTO `chat_session` VALUES (2076957117012561921, 1, '测试工作流', NULL, '103', 1, '2026-07-14 17:08:59', 1, '2026-07-14 17:08:59', NULL, NULL, 0);
INSERT INTO `chat_session` VALUES (2076958796772593665, 1, '测试工作流', NULL, '103', 1, '2026-07-14 17:15:40', 1, '2026-07-14 17:15:40', NULL, NULL, 0);
INSERT INTO `chat_session` VALUES (2076958822525620225, 1, '测试工作流', NULL, '103', 1, '2026-07-14 17:15:46', 1, '2026-07-14 17:15:46', NULL, NULL, 0);
INSERT INTO `chat_session` VALUES (2076964726549565441, 1, '测试工作流', NULL, '103', 1, '2026-07-14 17:39:14', 1, '2026-07-14 17:39:14', NULL, NULL, 0);
INSERT INTO `chat_session` VALUES (2076964778940616705, 1, '测试工作流', NULL, '103', 1, '2026-07-14 17:39:26', 1, '2026-07-14 17:39:26', NULL, NULL, 0);
INSERT INTO `chat_session` VALUES (2076971705284218881, 1, '你好2阿', '你好2阿', '103', 1, '2026-07-14 18:06:57', 1, '2026-07-14 18:06:57', '你好2阿', NULL, 0);

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
INSERT INTO `flow_definition` VALUES (2008122793122148354, 'leave2', '请假申请-排他网关', 'CLASSICS', '103', '1', 1, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-05 18:25:58', '1', '2026-03-10 21:24:36', '1', '1', '000000');
INSERT INTO `flow_definition` VALUES (2008125302444208129, 'leave1', '请假申请-普通', 'CLASSICS', '103', '1', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-05 18:35:56', '1', '2026-01-05 18:38:00', '1', '0', '000000');
INSERT INTO `flow_definition` VALUES (2008125415698804737, 'leave3', '请假申请-并行网关', 'CLASSICS', '103', '1', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-05 18:36:23', '1', '2026-01-05 18:36:23', '1', '0', '000000');
INSERT INTO `flow_definition` VALUES (2008125510645264385, 'leave4', '请假申请-会签', 'CLASSICS', '103', '1', 0, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-05 18:36:46', '1', '2026-01-05 18:36:46', '1', '0', '000000');
INSERT INTO `flow_definition` VALUES (2008125553481691138, 'leave5', '请假申请-并行会签网关', 'MIMIC', '103', '1', 1, 'N', '/workflow/leaveEdit/index', 1, NULL, NULL, NULL, '2026-01-05 18:36:56', '1', '2026-03-10 21:24:34', '1', '1', '000000');
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
INSERT INTO `flow_node` VALUES (2008122793638047746, 0, 2008122793122148354, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', '开始', NULL, '0.000', NULL, '300,240|300,240', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008122793638047747, 1, 2008122793122148354, 'fdcae93b-b69c-498a-b231-09255e74bcbd', '申请人', '', '0.000', NULL, '440,240|440,240', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008122793638047748, 3, 2008122793122148354, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', NULL, NULL, '0.000', NULL, '560,240', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008122793638047749, 1, 2008122793122148354, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', '组长', '3@@4', '0.000', NULL, '720,320|720,320', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008122793638047750, 1, 2008122793122148354, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', '总经理', 'role:1', '0.000', NULL, '860,240|860,240', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008122793638047751, 2, 2008122793122148354, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', '结束', NULL, '0.000', NULL, '1000,240|1000,240', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008122793638047752, 1, 2008122793122148354, '5ed2362b-fc0c-4d52-831f-95208b830605', '部门领导', 'role:1', '0.000', NULL, '720,160|720,160', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '1', '000000', NULL);
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
INSERT INTO `flow_node` VALUES (2008125554068893698, 0, 2008125553481691138, 'ebebaf26-9cb6-497e-8119-4c9fed4c597c', '开始', NULL, '0.000', NULL, '300,220|300,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125554068893699, 1, 2008125553481691138, 'e1b04e96-dc81-4858-a309-2fe945d2f374', '申请人', '', '0.000', NULL, '420,220|420,220', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file\"}]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125554068893700, 4, 2008125553481691138, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', NULL, NULL, '0.000', NULL, '560,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125554068893701, 1, 2008125553481691138, 'c80f273e-1f17-4bd8-9ad1-04a4a94ea862', '会签', 'role:1@@role:3', '100.000', NULL, '700,320|700,320', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,addSign,subSign\"}]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125554068893702, 4, 2008125553481691138, '1a20169e-3d82-4926-a151-e2daad28de1b', NULL, NULL, '0.000', NULL, '860,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125554068893703, 1, 2008125553481691138, '7a8f0473-e409-442e-a843-5c2b813d00e9', 'CEO', '1', '0.000', NULL, '1000,220|1000,220', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,transfer,trust,copy\"}]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125554068893704, 2, 2008125553481691138, '03c4d2bc-58b5-4408-a2e4-65afb046f169', '结束', NULL, '0.000', NULL, '1140,220|1140,220', NULL, NULL, NULL, 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[]', '1', '000000', NULL);
INSERT INTO `flow_node` VALUES (2008125554068893705, 1, 2008125553481691138, '1e3e8d3b-18ae-4d6c-a814-ce0d724adfa4', '百分之60票签', '${userList}', '60.000', NULL, '700,120|700,120', NULL, '', '', 'N', NULL, '1', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '[{\"code\":\"ButtonPermissionEnum\",\"value\":\"back,termination,file,addSign,subSign\"}]', '1', '000000', NULL);
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
INSERT INTO `flow_skip` VALUES (2008122795634536449, 2008122793122148354, 'cef3895c-f7d8-4598-8bf3-8ec2ef6ce84a', 0, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, NULL, 'PASS', NULL, '320,240;390,240', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008122795634536450, 2008122793122148354, 'fdcae93b-b69c-498a-b231-09255e74bcbd', 1, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, NULL, 'PASS', NULL, '490,240;535,240', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008122795634536451, 2008122793122148354, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, NULL, 'PASS', 'le@@leaveDays|2', '560,265;560,320;670,320', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008122795634536452, 2008122793122148354, '7b8c7ead-7dc8-4951-a7f3-f0c41995909e', 3, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, '大于两天', 'PASS', 'gt@@leaveDays|2', '560,215;560,160;670,160|560,187', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008122795634536453, 2008122793122148354, 'b3528155-dcb7-4445-bbdf-3d00e3499e86', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,320;860,320;860,280', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008122795634536454, 2008122793122148354, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, '40aa65fd-0712-4d23-b6f7-d0432b920fd1', 2, NULL, 'PASS', NULL, '910,240;980,240', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008122795634536455, 2008122793122148354, '5ed2362b-fc0c-4d52-831f-95208b830605', 1, 'c9fa6d7d-2a74-4e78-b947-0cad8a6af869', 1, NULL, 'PASS', NULL, '770,160;860,160;860,200', '2026-01-05 18:25:58', '1', '2026-01-05 18:25:58', '1', '1', '000000');
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
INSERT INTO `flow_skip` VALUES (2008125556442869762, 2008125553481691138, 'ebebaf26-9cb6-497e-8119-4c9fed4c597c', 0, 'e1b04e96-dc81-4858-a309-2fe945d2f374', 1, NULL, 'PASS', NULL, '320,220;350,220;350,220;340,220;340,220;370,220', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008125556442869763, 2008125553481691138, 'e1b04e96-dc81-4858-a309-2fe945d2f374', 1, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', 4, NULL, 'PASS', NULL, '470,220;535,220', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008125556442869764, 2008125553481691138, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', 4, 'c80f273e-1f17-4bd8-9ad1-04a4a94ea862', 1, NULL, 'PASS', NULL, '560,245;560,320;650,320', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008125556442869765, 2008125553481691138, '3e743f4f-51ca-41d4-8e94-21f5dd9b59c9', 4, '1e3e8d3b-18ae-4d6c-a814-ce0d724adfa4', 1, NULL, 'PASS', NULL, '560,195;560,120;650,120', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008125556442869766, 2008125553481691138, 'c80f273e-1f17-4bd8-9ad1-04a4a94ea862', 1, '1a20169e-3d82-4926-a151-e2daad28de1b', 4, NULL, 'PASS', NULL, '750,320;860,320;860,245', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008125556442869767, 2008125553481691138, '1a20169e-3d82-4926-a151-e2daad28de1b', 4, '7a8f0473-e409-442e-a843-5c2b813d00e9', 1, NULL, 'PASS', NULL, '885,220;950,220', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008125556442869768, 2008125553481691138, '7a8f0473-e409-442e-a843-5c2b813d00e9', 1, '03c4d2bc-58b5-4408-a2e4-65afb046f169', 2, NULL, 'PASS', NULL, '1050,220;1120,220', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '1', '000000');
INSERT INTO `flow_skip` VALUES (2008125556442869769, 2008125553481691138, '1e3e8d3b-18ae-4d6c-a814-ce0d724adfa4', 1, '1a20169e-3d82-4926-a151-e2daad28de1b', 4, NULL, 'PASS', NULL, '750,120;860,120;860,195', '2026-01-05 18:36:56', '1', '2026-01-05 18:36:56', '1', '1', '000000');
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
                                     `status` tinyint NULL DEFAULT 0 COMMENT '解析状态: 0待解析, 1解析中, 2已解析, 3解析失败',
                                     PRIMARY KEY (`id`) USING BTREE,
                                     UNIQUE INDEX `idx_kname`(`knowledge_id` ASC, `name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2033199209203183619 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库附件' ROW_FORMAT = DYNAMIC;

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
                                       `knowledge_id` bigint NULL DEFAULT NULL COMMENT '知识库ID',
                                       PRIMARY KEY (`id`) USING BTREE,
                                       FULLTEXT INDEX `ft_content`(`content`) WITH PARSER `ngram`
) ENGINE = InnoDB AUTO_INCREMENT = 2033199209131880451 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识片段' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of knowledge_fragment
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
                                   `similarity_threshold` double NULL DEFAULT 0.5 COMMENT '相似度阈值',
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
                                   `enable_rerank` tinyint NULL DEFAULT 0 COMMENT '是否启用重排序（0否 1是）',
                                   `rerank_score_threshold` double NULL DEFAULT NULL COMMENT '重排序相关性分数阈值',
                                   `rerank_top_n` int NULL DEFAULT NULL COMMENT '重排序后返回的文档数量',
                                   `rerank_model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '重排序模型名称',
                                   `enable_hybrid` tinyint(1) NULL DEFAULT 0 COMMENT '是否启用混合检索',
                                   `hybrid_alpha` double NULL DEFAULT 0.5 COMMENT '混合检索权重比例 (0.0=纯向量, 1.0=纯关键词)',
                                   `system_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '系统提示词',
                                   PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2033198818050781187 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of knowledge_info
-- ----------------------------

-- ----------------------------
-- Table structure for mcp_market_info
-- ----------------------------
DROP TABLE IF EXISTS `mcp_market_info`;
CREATE TABLE `mcp_market_info`  (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '市场ID',
                                    `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '市场名称',
                                    `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '市场URL',
                                    `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '市场描述',
                                    `auth_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '认证配置（JSON格式）',
                                    `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED-启用, DISABLED-禁用',
                                    `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '000000' COMMENT '租户编号',
                                    `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                                    `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                                    `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                    `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                                    `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                    `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                                    PRIMARY KEY (`id`) USING BTREE,
                                    INDEX `idx_name`(`name` ASC) USING BTREE,
                                    INDEX `idx_status`(`status` ASC) USING BTREE,
                                    INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'MCP市场表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of mcp_market_info
-- ----------------------------

-- ----------------------------
-- Table structure for mcp_market_tool
-- ----------------------------
DROP TABLE IF EXISTS `mcp_market_tool`;
CREATE TABLE `mcp_market_tool`  (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                                    `market_id` bigint NOT NULL COMMENT '市场ID',
                                    `tool_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工具名称',
                                    `tool_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '工具描述',
                                    `tool_version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '工具版本',
                                    `tool_metadata` json NULL COMMENT '工具元数据（JSON格式）',
                                    `is_loaded` tinyint(1) NULL DEFAULT 0 COMMENT '是否已加载到本地',
                                    `local_tool_id` bigint NULL DEFAULT NULL COMMENT '关联的本地工具ID',
                                    `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                    PRIMARY KEY (`id`) USING BTREE,
                                    INDEX `idx_market_id`(`market_id` ASC) USING BTREE,
                                    INDEX `idx_tool_name`(`tool_name` ASC) USING BTREE,
                                    INDEX `idx_is_loaded`(`is_loaded` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'MCP市场工具关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of mcp_market_tool
-- ----------------------------

-- ----------------------------
-- Table structure for mcp_tool_info
-- ----------------------------
DROP TABLE IF EXISTS `mcp_tool_info`;
CREATE TABLE `mcp_tool_info`  (
                                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工具ID',
                                  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工具名称',
                                  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '工具描述',
                                  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'LOCAL' COMMENT '工具类型：LOCAL-本地, REMOTE-远程, BUILTIN-内置',
                                  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED-启用, DISABLED-禁用',
                                  `config_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '配置信息（JSON格式）',
                                  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '000000' COMMENT '租户编号',
                                  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                                  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                                  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                                  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                                  PRIMARY KEY (`id`) USING BTREE,
                                  INDEX `idx_name`(`name` ASC) USING BTREE,
                                  INDEX `idx_type`(`type` ASC) USING BTREE,
                                  INDEX `idx_status`(`status` ASC) USING BTREE,
                                  INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'MCP工具表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of mcp_tool_info
-- ----------------------------
INSERT INTO `mcp_tool_info` VALUES (1, 'edit_file', '', 'BUILTIN', 'ENABLED', NULL, '000000', -1, -1, '2026-02-24 20:19:41', -1, '2026-06-22 16:34:29', '0');
INSERT INTO `mcp_tool_info` VALUES (2, 'list_directory', '', 'BUILTIN', 'ENABLED', NULL, '000000', -1, -1, '2026-02-24 20:19:41', -1, '2026-06-22 16:34:29', '0');
INSERT INTO `mcp_tool_info` VALUES (3, 'read_file', '', 'BUILTIN', 'ENABLED', NULL, '000000', -1, -1, '2026-02-24 20:19:41', -1, '2026-06-22 16:34:29', '0');
INSERT INTO `mcp_tool_info` VALUES (4, 'query_all_tables', '', 'BUILTIN', 'ENABLED', NULL, '000000', -1, -1, '2026-03-10 21:21:09', -1, '2026-06-22 16:34:29', '0');
INSERT INTO `mcp_tool_info` VALUES (5, 'execute_sql_query', '', 'BUILTIN', 'ENABLED', NULL, '000000', -1, -1, '2026-03-10 21:21:09', -1, '2026-06-22 16:34:29', '0');
INSERT INTO `mcp_tool_info` VALUES (6, 'query_table_schema', '', 'BUILTIN', 'ENABLED', NULL, '000000', -1, -1, '2026-03-10 21:21:09', -1, '2026-06-22 16:34:29', '0');
INSERT INTO `mcp_tool_info` VALUES (7, 'bing-cn-mcp-server', '必应中文联网查询工具，支持实时搜索和网页内容抓取。内置工具：bing_search、fetch_webpage。', 'LOCAL', 'ENABLED', '{\n  \"command\": \"npx\",\n  \"args\": [\n    \"-y\",\n    \"bing-cn-mcp\"\n  ]\n}\n', '000000', -1, -1, '2026-06-22 16:33:22', -1, '2026-06-22 16:33:22', '0');

-- ----------------------------
-- Table structure for short_drama_character
-- ----------------------------
DROP TABLE IF EXISTS `short_drama_character`;
CREATE TABLE `short_drama_character`  (
                                          `id` bigint NOT NULL COMMENT '主键',
                                          `project_id` bigint NOT NULL COMMENT '项目ID',
                                          `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名',
                                          `aliases` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '别名/称呼（逗号分隔）',
                                          `introduction` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '角色介绍（身份、关系、称呼映射）',
                                          `role_level` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'B' COMMENT '角色层级：S/A/B/C/D',
                                          `gender` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '性别',
                                          `age_range` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '年龄段（如：约二十五岁）',
                                          `personality_tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '性格标签（逗号分隔）',
                                          `costume_tier` int NULL DEFAULT 2 COMMENT '服装华丽度1-5',
                                          `visual_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '视觉外貌描述（详细，用于图片生成）',
                                          `reference_image_url` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '角色参考图URL',
                                          `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                                          `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                                          `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                          `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                                          `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                          `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户Id',
                                          PRIMARY KEY (`id`) USING BTREE,
                                          INDEX `idx_project_id`(`project_id` ASC) USING BTREE,
                                          INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '短剧角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of short_drama_character
-- ----------------------------
INSERT INTO `short_drama_character` VALUES (2075609273630703616, 2075608659739787264, '队医', '', '峨眉队跟队医师，负责队员伤病的现场处理与康复监督。在钰珑脚踝旧伤复发时为其做紧急处理。无特定人际称呼，队员通常只喊「医生」或「队医」。', 'C', '女', '约三十岁', '温柔,细心', 2, '鹅蛋脸线条柔和，双眼皮杏眼明亮有神，小巧挺拔的鼻梁，薄厚适中的嘴唇微抿，柳叶眉自然舒展。乌黑过肩直发扎成利落低马尾，额前碎发自然垂落。身形修长匀称，穿简洁白色短袖Polo衫，左胸有队徽刺绣，搭配卡其色工装长裤和白色运动鞋，腰间斜挎急救小包。', NULL, -1, -1, '2026-07-10 23:53:08', -1, '2026-07-10 23:53:08', 0);
INSERT INTO `short_drama_character` VALUES (2075609329431724032, 2075608659739787264, '徐风', '徐教练', '峨眉队前任功夫教练，决赛前转投对手东亚商联，成为贯穿剧情的主要对手。曾是双双与钰珑的授业者，后利用对她们的了解反制旧部。被队员普遍称呼为「徐教练」。', 'A', '男', '约三十五岁', '高冷,腹黑,专业,野心', 3, '脸型棱角分明，下颌线条锋利。细长眼型，双眼皮，高挺精致的鼻梁，薄唇平直，眉峰上扬的剑眉浓黑有型，右眼尾点缀一颗小痣。乌黑短发以利落背头后梳，露出饱满额头，发质柔顺服帖。身形修长，高挑匀称。身穿深灰色高支羊毛西装，剪裁修身，肩线平直，内搭黑色精纺高领毛衣，下身同色西裤线条流畅，脚著黑色牛津皮鞋。鼻梁上架一副极细金属框运动墨镜，左手腕佩戴银色纤薄腕表。', NULL, -1, -1, '2026-07-10 23:53:22', -1, '2026-07-10 23:53:22', 0);
INSERT INTO `short_drama_character` VALUES (2075609380031807488, 2075608659739787264, '钰珑', '', '峨眉女足主力前锋，曾深度依赖徐风传授的功夫射门技术，后在双双引导下融入团队体系。与双双为挚友，视徐风为前恩师后对手。被双双及其他队友直呼「钰珑」。', 'A', '女', '约二十四岁', '直爽,冲动,坚韧,热情', 2, '钰珑生着一张轮廓分明的瓜子脸，浓黑剑眉斜抬，衬出几分不驯。眼形偏窄的杏眼微微上挑，单眼皮收紧眼尾，转折处利落带锋。鼻梁小巧挺直，嘴唇薄厚适中，下颌角附近有一道浅淡旧伤痕。一头乌黑长发高高束成马尾，额前碎发桀骜翘起，发尾带着自然微卷。身形修长结实，肩背紧绷，常年奔跑的线条紧致流畅。她穿着白色速干运动背心、深灰色运动短裤，脚踩抓地强劲的黑色跑鞋。右脚踝始终缠着防护绷带，步态间略带拖沓，却像随时要蹬地冲出。手腕戴一只简约的黑色运动手表。', NULL, -1, -1, '2026-07-10 23:53:34', -1, '2026-07-10 23:53:34', 0);
INSERT INTO `short_drama_character` VALUES (2075609601688190976, 2075608659739787264, '双双', '', '故事女主角，峨眉女足队长，球队战术核心与精神领袖。前锋钰珑的队友兼好友，前教练徐风曾执教的队员。被队友称呼为「队长」，被钰珑直接叫名字「双双」。', 'S', '女', '约二十五岁', '冷静,果断,温柔,坚毅', 2, '瓜子脸，线条利落分明。一双眼裂纤长的丹凤眼，眼尾如燕尾般锐利上扬，内眼角尖细，双眼皮折痕深刻清晰，注视时自带压迫感。鼻梁挺直高耸，鼻翼收窄，鼻尖精致小巧。双唇薄而轮廓分明，上唇弓形，下唇微微内收。眉骨高隆，眉形为天然剑眉，眉头略粗，眉峰微挑，眉尾收得细长利落。鼻梁中段偏左有一颗极浅小痣。一头乌黑长发在脑后高高束成利落马尾，扎紧于顶骨后方，用黑色弹力绳固定，发丝柔顺垂坠，发尾齐整落至肩胛骨下缘，额前无一丝刘海，露出光洁额头。体型修长而匀称，身姿高挑挺拔。上身穿一件白色速干圆领运动短袖T恤，面料带有微透气网眼，外罩一件深蓝色立领轻型教练夹克，拉链半开，袖口设有魔术贴收束，左臂佩戴红色弹性针织队长袖标，表面印有白色「CAPTAIN」字样。下身为黑色弹力紧身运动长裤，高腰设计，膝盖处有拼接剪裁，脚踝处有短拉链。脚蹬一双白色轻量跑步鞋，鞋面为透气网布拼接合成革，鞋带系紧，鞋底防滑纹理清晰。左手腕佩戴一块简约黑色运动电子手表。', NULL, -1, -1, '2026-07-10 23:54:27', -1, '2026-07-10 23:54:27', 0);
INSERT INTO `short_drama_character` VALUES (2077008099927412736, 2077007721974484992, '百货经理', '经理', '百货公司经理，在工商检查时出现，对小明提出的信息平台方案表示认可并推动放行。角色身份为商业系统干部，被小明称为「经理」。', 'C', '男', '约四十岁', '务实,深谙利益,精明', 3, '方形脸，浓黑剑眉，双眼皮，眼神锐利，鼻梁高挺，嘴唇线条分明。乌黑三七分背头，整齐服帖。身形匀称，身高适中。身穿藏青色修身西装外套，内搭白色衬衫，深灰色西裤，黑色系带皮鞋，手腕佩戴简约钢带手表。', NULL, -1, -1, '2026-07-14 20:31:35', -1, '2026-07-14 20:31:35', 0);
INSERT INTO `short_drama_character` VALUES (2077008101332504576, 2077007721974484992, '老陈', '陈厂长', '工厂厂长，小明的关键支持者和合作伙伴。性格谨慎但敢于冒险，信任小明的能力。被小明称为「陈厂长」，被工人和下属称为「厂长」或「老陈」。', 'A', '男', '约五十岁', '谨慎,务实,有魄力,正直', 2, '长方脸型，轮廓分明，额间有浅横纹，颧骨平直。浓眉微斜，眉尾稍疏，单眼皮细长眼，眼窝微陷，目光专注。鼻梁高直，鼻翼内收，唇形薄而线条清晰，下唇中央有浅凹痕。下巴方圆，无明显痣疤。发色花白，短发偏分，发质硬而蓬松，略见鬓角银丝。身形匀称中等，肩背挺直。上身着灰色中山装，领口紧扣齐整，左下口袋别一支钢笔。下装深灰西裤，裤线笔直，脚穿黑色系带皮鞋，鞋面光亮。无多余配饰。', NULL, -1, -1, '2026-07-14 20:31:35', -1, '2026-07-14 20:31:35', 0);
INSERT INTO `short_drama_character` VALUES (2077008104104939520, 2077007721974484992, '小明', '无', '故事核心主角，从2025年穿越回1985年的重生者。原本是50岁的失意中年，穿越后成为年轻工人。老陈的合作伙伴，被团队成员称为「小明」或「同志」，被老陈直呼「小明」。', 'S', '男', '约二十五岁（实际心理年龄五十岁）', '远见,执着,果断,坚毅', 2, '轮廓分明的瓜子脸，下颌线条利落。深邃的双眼皮眼睛眼型偏长，目光沉静；鼻梁高挺笔直；嘴唇厚薄适中，唇峰清晰；浓密的剑眉斜飞入鬓。右侧眉尾处一颗小痣。发色乌黑，及耳的长度微卷自然蓬松，额前碎发刘海随意散落。身形修长挺拔，身高约一米七八。穿着简约白色棉质衬衫，袖口卷至小臂，搭配深灰色直筒长裤与黑色帆布鞋。', NULL, -1, -1, '2026-07-14 20:31:36', -1, '2026-07-14 20:31:36', 0);

-- ----------------------------
-- Table structure for short_drama_character_appearance
-- ----------------------------
DROP TABLE IF EXISTS `short_drama_character_appearance`;
CREATE TABLE `short_drama_character_appearance`  (
                                                     `id` bigint NOT NULL COMMENT '主键',
                                                     `character_id` bigint NOT NULL COMMENT '角色ID',
                                                     `appearance_index` int NOT NULL DEFAULT 0 COMMENT '形象序号（0=主形象）',
                                                     `change_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '初始形象' COMMENT '变化原因',
                                                     `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '形象视觉描述',
                                                     `reference_image_url` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '形象参考图URL',
                                                     `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                                                     `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                                                     `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                     `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                                                     `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                     `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户Id',
                                                     `image_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '生成图片URL列表（JSON数组）',
                                                     `image_descriptions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '每张图片对应的提示词（JSON数组）',
                                                     `selected_image_index` int NULL DEFAULT 0 COMMENT '当前选中的图片索引',
                                                     `previous_image_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '上一轮图片URL列表（撤销用，JSON数组）',
                                                     `previous_descriptions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '上一轮提示词列表（撤销用，JSON数组）',
                                                     `voice` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '音色名（如 zh_male_taocheng_uranus_bigtts），用于该形象对白配音',
                                                     PRIMARY KEY (`id`) USING BTREE,
                                                     UNIQUE INDEX `uk_character_appearance`(`character_id` ASC, `appearance_index` ASC) USING BTREE,
                                                     INDEX `idx_character_id`(`character_id` ASC) USING BTREE,
                                                     INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '短剧角色子形象表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of short_drama_character_appearance
-- ----------------------------
INSERT INTO `short_drama_character_appearance` (`id`, `character_id`, `appearance_index`, `change_reason`, `description`, `reference_image_url`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `tenant_id`, `image_urls`, `image_descriptions`, `selected_image_index`, `previous_image_urls`, `previous_descriptions`) VALUES (2075609273836224512, 2075609273630703616, 0, '初始形象', '鹅蛋脸线条柔和，双眼皮杏眼明亮有神，小巧挺拔的鼻梁，薄厚适中的嘴唇微抿，柳叶眉自然舒展。乌黑过肩直发扎成利落低马尾，额前碎发自然垂落。身形修长匀称，穿简洁白色短袖Polo衫，左胸有队徽刺绣，搭配卡其色工装长裤和白色运动鞋，腰间斜挎急救小包。', NULL, -1, -1, '2026-07-10 23:53:08', -1, '2026-07-10 23:53:08', 0, NULL, NULL, 0, NULL, NULL);
INSERT INTO `short_drama_character_appearance` (`id`, `character_id`, `appearance_index`, `change_reason`, `description`, `reference_image_url`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `tenant_id`, `image_urls`, `image_descriptions`, `selected_image_index`, `previous_image_urls`, `previous_descriptions`) VALUES (2075609329570136064, 2075609329431724032, 0, '初始形象', '脸型棱角分明，下颌线条锋利。细长眼型，双眼皮，高挺精致的鼻梁，薄唇平直，眉峰上扬的剑眉浓黑有型，右眼尾点缀一颗小痣。乌黑短发以利落背头后梳，露出饱满额头，发质柔顺服帖。身形修长，高挑匀称。身穿深灰色高支羊毛西装，剪裁修身，肩线平直，内搭黑色精纺高领毛衣，下身同色西裤线条流畅，脚著黑色牛津皮鞋。鼻梁上架一副极细金属框运动墨镜，左手腕佩戴银色纤薄腕表。', NULL, -1, -1, '2026-07-10 23:53:22', -1, '2026-07-10 23:53:22', 0, NULL, NULL, 0, NULL, NULL);
INSERT INTO `short_drama_character_appearance` (`id`, `character_id`, `appearance_index`, `change_reason`, `description`, `reference_image_url`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `tenant_id`, `image_urls`, `image_descriptions`, `selected_image_index`, `previous_image_urls`, `previous_descriptions`) VALUES (2075609380207968256, 2075609380031807488, 0, '初始形象', '钰珑生着一张轮廓分明的瓜子脸，浓黑剑眉斜抬，衬出几分不驯。眼形偏窄的杏眼微微上挑，单眼皮收紧眼尾，转折处利落带锋。鼻梁小巧挺直，嘴唇薄厚适中，下颌角附近有一道浅淡旧伤痕。一头乌黑长发高高束成马尾，额前碎发桀骜翘起，发尾带着自然微卷。身形修长结实，肩背紧绷，常年奔跑的线条紧致流畅。她穿着白色速干运动背心、深灰色运动短裤，脚踩抓地强劲的黑色跑鞋。右脚踝始终缠着防护绷带，步态间略带拖沓，却像随时要蹬地冲出。手腕戴一只简约的黑色运动手表。', NULL, -1, -1, '2026-07-10 23:53:34', -1, '2026-07-10 23:53:34', 0, NULL, NULL, 0, NULL, NULL);
INSERT INTO `short_drama_character_appearance` (`id`, `character_id`, `appearance_index`, `change_reason`, `description`, `reference_image_url`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `tenant_id`, `image_urls`, `image_descriptions`, `selected_image_index`, `previous_image_urls`, `previous_descriptions`) VALUES (2075609601822408704, 2075609601688190976, 0, '初始形象', '瓜子脸，线条利落分明。一双眼裂纤长的丹凤眼，眼尾如燕尾般锐利上扬，内眼角尖细，双眼皮折痕深刻清晰，注视时自带压迫感。鼻梁挺直高耸，鼻翼收窄，鼻尖精致小巧。双唇薄而轮廓分明，上唇弓形，下唇微微内收。眉骨高隆，眉形为天然剑眉，眉头略粗，眉峰微挑，眉尾收得细长利落。鼻梁中段偏左有一颗极浅小痣。一头乌黑长发在脑后高高束成利落马尾，扎紧于顶骨后方，用黑色弹力绳固定，发丝柔顺垂坠，发尾齐整落至肩胛骨下缘，额前无一丝刘海，露出光洁额头。体型修长而匀称，身姿高挑挺拔。上身穿一件白色速干圆领运动短袖T恤，面料带有微透气网眼，外罩一件深蓝色立领轻型教练夹克，拉链半开，袖口设有魔术贴收束，左臂佩戴红色弹性针织队长袖标，表面印有白色「CAPTAIN」字样。下身为黑色弹力紧身运动长裤，高腰设计，膝盖处有拼接剪裁，脚踝处有短拉链。脚蹬一双白色轻量跑步鞋，鞋面为透气网布拼接合成革，鞋带系紧，鞋底防滑纹理清晰。左手腕佩戴一块简约黑色运动电子手表。', NULL, -1, -1, '2026-07-10 23:54:27', -1, '2026-07-10 23:54:27', 0, NULL, NULL, 0, NULL, NULL);
INSERT INTO `short_drama_character_appearance` (`id`, `character_id`, `appearance_index`, `change_reason`, `description`, `reference_image_url`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `tenant_id`, `image_urls`, `image_descriptions`, `selected_image_index`, `previous_image_urls`, `previous_descriptions`) VALUES (2077008100070019072, 2077008099927412736, 0, '初始形象', '方形脸，浓黑剑眉，双眼皮，眼神锐利，鼻梁高挺，嘴唇线条分明。乌黑三七分背头，整齐服帖。身形匀称，身高适中。身穿藏青色修身西装外套，内搭白色衬衫，深灰色西裤，黑色系带皮鞋，手腕佩戴简约钢带手表。', 'https://atlas-media.oss-us-west-1.aliyuncs.com/images/dd7454cd25a6486aa5c7c3f8c7085041-3742113260bb4ee9.jpg', -1, -1, '2026-07-14 20:31:35', 1, '2026-07-14 20:38:50', 0, '[\"https://atlas-media.oss-us-west-1.aliyuncs.com/images/dd7454cd25a6486aa5c7c3f8c7085041-3742113260bb4ee9.jpg\"]', '[\"真实电影级画面质感，真实现实场景，色彩饱满通透，画面干净精致，真实感，character design sheet, multiple views reference sheet, front view / side view / back view full body, clean white background, no props, no text. 方形脸，浓黑剑眉，双眼皮，眼神锐利，鼻梁高挺，嘴唇线条分明。乌黑三七分背头，整齐服帖。身形匀称，身高适中。身穿藏青色修身西装外套，内搭白色衬衫，深灰色西裤，黑色系带皮鞋，手腕佩戴简约钢带手表。。角色设定图，画面分为左右两个区域：【左侧区域】占约1/3宽度，是角色的正面特写（完整正脸，最具辨识度的正面形态）；【右侧区域】占约2/3宽度，是角色三视图横向排列（从左到右依次为：正面全身、侧面全身、背面全身），三视图高度一致。纯白色背景，无其他元素。\"]', 0, NULL, NULL);
INSERT INTO `short_drama_character_appearance` (`id`, `character_id`, `appearance_index`, `change_reason`, `description`, `reference_image_url`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `tenant_id`, `image_urls`, `image_descriptions`, `selected_image_index`, `previous_image_urls`, `previous_descriptions`) VALUES (2077008101487693824, 2077008101332504576, 0, '初始形象', '长方脸型，轮廓分明，额间有浅横纹，颧骨平直。浓眉微斜，眉尾稍疏，单眼皮细长眼，眼窝微陷，目光专注。鼻梁高直，鼻翼内收，唇形薄而线条清晰，下唇中央有浅凹痕。下巴方圆，无明显痣疤。发色花白，短发偏分，发质硬而蓬松，略见鬓角银丝。身形匀称中等，肩背挺直。上身着灰色中山装，领口紧扣齐整，左下口袋别一支钢笔。下装深灰西裤，裤线笔直，脚穿黑色系带皮鞋，鞋面光亮。无多余配饰。', 'https://atlas-media.oss-us-west-1.aliyuncs.com/images/4729daf662de42c0a3472bc05043529c-01a8aaf57a08ea21.jpg', -1, -1, '2026-07-14 20:31:35', 1, '2026-07-14 20:38:50', 0, '[\"https://atlas-media.oss-us-west-1.aliyuncs.com/images/4729daf662de42c0a3472bc05043529c-01a8aaf57a08ea21.jpg\"]', '[\"真实电影级画面质感，真实现实场景，色彩饱满通透，画面干净精致，真实感，character design sheet, multiple views reference sheet, front view / side view / back view full body, clean white background, no props, no text. 长方脸型，轮廓分明，额间有浅横纹，颧骨平直。浓眉微斜，眉尾稍疏，单眼皮细长眼，眼窝微陷，目光专注。鼻梁高直，鼻翼内收，唇形薄而线条清晰，下唇中央有浅凹痕。下巴方圆，无明显痣疤。发色花白，短发偏分，发质硬而蓬松，略见鬓角银丝。身形匀称中等，肩背挺直。上身着灰色中山装，领口紧扣齐整，左下口袋别一支钢笔。下装深灰西裤，裤线笔直，脚穿黑色系带皮鞋，鞋面光亮。无多余配饰。。角色设定图，画面分为左右两个区域：【左侧区域】占约1/3宽度，是角色的正面特写（完整正脸，最具辨识度的正面形态）；【右侧区域】占约2/3宽度，是角色三视图横向排列（从左到右依次为：正面全身、侧面全身、背面全身），三视图高度一致。纯白色背景，无其他元素。\"]', 0, NULL, NULL);
INSERT INTO `short_drama_character_appearance` (`id`, `character_id`, `appearance_index`, `change_reason`, `description`, `reference_image_url`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `tenant_id`, `image_urls`, `image_descriptions`, `selected_image_index`, `previous_image_urls`, `previous_descriptions`) VALUES (2077008104272711680, 2077008104104939520, 0, '初始形象', '轮廓分明的瓜子脸，下颌线条利落。深邃的双眼皮眼睛眼型偏长，目光沉静；鼻梁高挺笔直；嘴唇厚薄适中，唇峰清晰；浓密的剑眉斜飞入鬓。右侧眉尾处一颗小痣。发色乌黑，及耳的长度微卷自然蓬松，额前碎发刘海随意散落。身形修长挺拔，身高约一米七八。穿着简约白色棉质衬衫，袖口卷至小臂，搭配深灰色直筒长裤与黑色帆布鞋。', 'https://atlas-media.oss-us-west-1.aliyuncs.com/images/aab75b6b-865d-4825-8eee-9e3a45ccec62.png', -1, -1, '2026-07-14 20:31:36', 1, '2026-07-14 20:38:50', 0, '[\"https://atlas-media.oss-us-west-1.aliyuncs.com/images/aab75b6b-865d-4825-8eee-9e3a45ccec62.png\"]', '[\"真实电影级画面质感，真实现实场景，色彩饱满通透，画面干净精致，真实感，character design sheet, multiple views reference sheet, front view / side view / back view full body, clean white background, no props, no text. 轮廓分明的瓜子脸，下颌线条利落。深邃的双眼皮眼睛眼型偏长，目光沉静；鼻梁高挺笔直；嘴唇厚薄适中，唇峰清晰；浓密的剑眉斜飞入鬓。右侧眉尾处一颗小痣。发色乌黑，及耳的长度微卷自然蓬松，额前碎发刘海随意散落。身形修长挺拔，身高约一米七八。穿着简约白色棉质衬衫，袖口卷至小臂，搭配深灰色直筒长裤与黑色帆布鞋。。角色设定图，画面分为左右两个区域：【左侧区域】占约1/3宽度，是角色的正面特写（完整正脸，最具辨识度的正面形态）；【右侧区域】占约2/3宽度，是角色三视图横向排列（从左到右依次为：正面全身、侧面全身、背面全身），三视图高度一致。纯白色背景，无其他元素。\"]', 0, NULL, NULL);

-- ----------------------------
-- Table structure for short_drama_location
-- ----------------------------
DROP TABLE IF EXISTS `short_drama_location`;
CREATE TABLE `short_drama_location`  (
                                         `id` bigint NOT NULL COMMENT '主键',
                                         `project_id` bigint NOT NULL COMMENT '项目ID',
                                         `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '场景名（如：客厅_白天）',
                                         `summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '场景简要说明',
                                         `has_crowd` tinyint(1) NULL DEFAULT 0 COMMENT '是否有背景人群',
                                         `crowd_description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '背景人群描述',
                                         `available_slots` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '可站位置列表（JSON数组）',
                                         `descriptions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '场景描述列表（JSON数组，3条差异化描述）',
                                         `reference_image_url` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '场景全景参考图URL',
                                         `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                                         `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                                         `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                                         `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                         `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户Id',
                                         `image_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '生成图片URL列表（JSON数组）',
                                         `image_descriptions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '每张图片对应的提示词（JSON数组）',
                                         `selected_image_index` int NULL DEFAULT 0 COMMENT '当前选中的图片索引',
                                         `previous_image_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '上一轮图片URL列表（撤销用，JSON数组）',
                                         `previous_descriptions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '上一轮提示词列表（撤销用，JSON数组）',
                                         PRIMARY KEY (`id`) USING BTREE,
                                         INDEX `idx_project_id`(`project_id` ASC) USING BTREE,
                                         INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '短剧场景表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of short_drama_location
-- ----------------------------
INSERT INTO `short_drama_location` VALUES (2075609601935654912, 2075608659739787264, '峨眉队训练基地_傍晚', '金色夕阳下的足球训练场，质朴的草地与球门构成核心训练区', 1, '远处有模糊的队员正在进行抢圈练习，人影被拉长', '[\"土场中央中圈开球点的位置\",\"远端球门右侧角旗区附近的位置\",\"场边简易长条凳与战术板之间的空地\"]', '[\"「峨眉队训练基地傍晚」傍晚的余晖将整片土场染成浓郁的金色，宽广的球场向远处延伸，边界由低矮的铁丝围栏框定。左侧设置一座标准尺寸的球门，白色球网微微颤动，右侧对等位置的球门轮廓在逆光中略显模糊。四周几盏简易训练灯尚未点亮，整个空间被暖调的自然光笼罩，光从西侧斜射过来，将球门阴影长长地拖在东侧草坪上。中圈弧线清晰可见，两侧边线外留有宽敞的跑动通道，场边零散放着几个训练标志盘和水瓶。\",\"「峨眉队训练基地傍晚」土质训练场开阔而硬朗，天空半边橙红半边浅紫。镜头从底线角球区拉开，可以看到两座球门、两侧边线以及远处低矮平房的轮廓。西边的斜阳穿过球门网，在泥土地上投出网格状的影子。场地中央被踩实的黄土露出些许草根，四周则还留着稀疏的绿草。场边一侧摆着一条木质长凳，旁边立着一块可移动的战术白板，为这片朴素的空间增添了几分专业气息。空旷的球场除了风声，只剩远处模糊的呼喝与足球撞击的闷响。\",\"「峨眉队训练基地傍晚」广角俯瞰下，整个训练基地被夕阳包裹，像一座独立于时间之外的金色擂台。两座标准足球门遥相对望，球门后的拦网高而完整。中圈向外辐射，中轴线上的草稀而坚实。光线从画面右侧灌入，将左侧的球门区域照得最亮，右侧则稍稍藏在阴影里。场边零散堆放着几件训练背心和几只足球，没有围观的观众，只有空间本身在等待下一场奔跑。远处的训练队员身影模糊成小小的剪影，在远处半场进行着无声的抢圈游戏。\"]', NULL, -1, -1, '2026-07-10 23:54:27', -1, '2026-07-10 23:54:27', 0, NULL, NULL, 0, NULL, NULL);
INSERT INTO `short_drama_location` VALUES (2075609601960820736, 2075608659739787264, '球队更衣室_夜', '赛前赛后使用的长方形更衣室，木凳、储物柜和散落的球衣构成封闭空间', 0, '', '[\"中央长条木凳前方的过道空间\",\"靠近门口储物柜与墙壁之间的角落\",\"另一侧更衣柜前空出的站立区域\"]', '[\"「球队更衣室夜」封闭的长方形房间，天花板上的日光灯管发出冷白色的光，照亮了整间屋子。三面墙壁装着浅绿色金属储物柜，柜门大多虚掩或敞开，里面挂着几件换下的球衣，左侧墙壁上挂着一块白板，上面还留着隐约的战术标记。房间中央横置一条厚重的长条木凳，凳面磨损光滑，周围散落着几双球鞋和护腿板。光从顶部均匀洒下，地面是深灰色的防滑地胶，靠门的角落扔着几件绿色训练背心。门口一方空间相对空阔，连着一条通往淋浴区的走廊。\",\"「球队更衣室夜」灯光通明，更衣室被冷色调充满。正对面的储物柜墙几乎占据了整个视野，柜子编号清晰，其中几个柜门开着，球衣搭在柜沿外边。中央的木凳长度几乎横贯房间，凳下塞着几个运动包。左侧墙角立着一面战术白板，笔迹被擦去大半，残留的弧线隐约可见。右侧是另一排储物柜，柜门反光出冷白的灯管倒影。整个空间静静封闭，只有头顶排风扇发出轻微的嗡鸣，地面清洁但带有些许草屑，门口的通道空出明显的站立空间。\",\"「球队更衣室夜」紧凑但有序的球队更衣室，三面排列着标准体育储物柜，中间一条厚木长凳将空间划为两半。日光灯从天花中央铺开，柜门的金属边框反射出明亮光线。左侧柜组上方的墙壁挂着一面简朴的电子钟，时间显示在晚间。长凳上扔着一双刚脱下的球袜，地板在灯下呈现出深灰色橡胶纹理。靠门口的区域相对整洁，空出的地面足够两人并肩站立，那一侧的门把手还带些微使用过的痕迹。整个房间弥漫着一种刚结束训练的沉默，等待被下一句话打破。\"]', NULL, -1, -1, '2026-07-10 23:54:27', -1, '2026-07-10 23:54:27', 0, NULL, NULL, 0, NULL, NULL);
INSERT INTO `short_drama_location` VALUES (2075609602027929600, 2075608659739787264, '训练场_日', '白天的足球训练场，阳光充足，设有球门、绳梯和战术布置区域，空旷而专注', 0, '', '[\"禁区弧顶正对球门的罚球点附近\",\"中圈开球点周围的开阔区域\",\"场边摆放战术板与训练器材的教练区域\"]', '[\"「训练场日」白日下的标准足球场，草坪修剪整齐，浓绿色向远处铺展。遥相对望的两座白色球门在午后阳光下十分醒目，近端球门的网子绑得紧实。场地中央的中圈白线清晰，禁区线、边线等标记一丝不苟。右侧边线外摆着一架绳梯和几个锥形标志桶，还有一块战术白板支在折叠支架上。阳光从偏南方向照射，将球门立柱的影子投在草皮上。整个训练场除了远处围栏外偶尔经过的人影，场内一片空旷，只剩下训练器材静待使用。\",\"「训练场日」晴朗天气下，训练场被明亮的光线填满，没有观众，只有训练的味道。从底线角度望过去，纵深极强：近处角球弧的弧线、中圈、远方另一侧球门的全景尽收眼底。场地偏左的位置安放着一组红白相间的训练桩，右侧边线附近零星散落着足球。光线柔和地从画面左上方射入，草尖泛出微光。战术白板立在阴凉一侧，上面画着交叉跑位的弧线和点位标记，旁边放着一只医疗箱和几瓶喷雾，这方空间留有足够的站立和讲解余地。\",\"「训练场日」俯瞰的训练场呈现出清晰的几何线条，白色的场地标记在阳光下异常鲜明。两座球门对称分布于两端，禁区的矩形线条和中圈圆环完整。靠近远端球门的一侧，绳梯铺在阴影与阳光的交界处，橙色的标志碟在绿色背景上跳跃。画面中没有风吹动的痕迹，只有光从南偏西的方向大面积洒下，使得左侧球门区域更亮，右侧微暗。边线外的教练区空着，战术板上的弧线图被画得用力，旁边几件荧光训练背心堆在地上，等着一场无声的预演。\"]', NULL, -1, -1, '2026-07-10 23:54:27', -1, '2026-07-10 23:54:27', 0, NULL, NULL, 0, NULL, NULL);
INSERT INTO `short_drama_location` VALUES (2075609602099232768, 2075608659739787264, '亚冠决赛球场_夜', '盛大的亚冠决赛夜球场，灯光璀璨，看台满座，记分牌显示着紧张的下半场比分', 1, '看台上密密麻麻的观众，挥舞着旗帜和围巾，形成人浪与声浪', '[\"中圈弧附近的开阔地带\",\"禁区外正对球门的任意球主罚点\",\"靠近客队教练区一侧的边线区域\"]', '[\"「亚冠决赛球场夜」灯火如昼的巨型体育场，草皮在夜间照明下泛出整齐的墨绿。四面看台坐满了观众，数万人的衣色和旗帜连成涌动的人海，模糊的面孔只看得见张开的嘴巴和举起的围巾。巨大的电子记分牌高悬于一侧看台上方，红色的数字显示下半场72分钟，比分暂为悬殊的落后。两座球门背后，摄影记者长枪短炮已对准禁区。光线来自顶棚密集的灯阵，无死角照亮整个场地，使场边的教练区、替补席和第四官员席都清晰可见。草地中圈弧线被踩得微脏，比赛正进入最窒息的时间。\",\"「亚冠决赛球场夜」从高空俯瞰，这座宏大的足球圣殿被灯光分割成无数块面。绿茵场居于正中，被一圈暗红色的跑道环绕，再往外是层层叠叠的看台，人群如密织的织物铺满每一块区域。左侧球门后方的看台爆发出一片摇动的旗海，右侧观众席则因紧张而微微前倾。记分牌上，时间与比分在夜色中极其刺目。教练区空空地等着下一个指令，边线旁的摄影机摇臂缓缓移动。光从顶棚巨大的灯架倾泻而下，在草皮上几乎没有影子，整个赛场明如白昼，气氛却重如铅水。\",\"「亚冠决赛球场夜」镜头从角球区低角度向上拉升，首先看到近处被蹭掉几条草痕的角旗杆，然后中景闯入密实的禁区线，接着整座球场的壮阔扑面而来。看台组成一道陡峭的人墙，无数手臂在挥舞，助威声浪似有形物质充满空气。正对面远端球门上方，记分牌的数字亮得发烫，时间在流逝，比分残酷。教练区域设置在中线旁，简易的白色画线框内放着水壶和战术纸条。球场灯光从四面八方浇筑，使得场上即将发生的一切都无从遁形，只留下宽广的草皮等待下一脚触球。\"]', NULL, -1, -1, '2026-07-10 23:54:27', -1, '2026-07-10 23:54:27', 0, NULL, NULL, 0, NULL, NULL);
INSERT INTO `short_drama_location` VALUES (2075609602162147328, 2075608659739787264, '球场_伤停补时阶段', '同片决赛球场，但已进入伤停补时，气氛沸腾至顶点，人墙与禁区形成焦点', 1, '全场观众几乎全部站立，紧张地注视着场内，呐喊与屏息交替', '[\"禁区弧前方任意球人墙后的区域\",\"中圈附近等待反击的空旷地带\",\"对方球门远角一侧的包抄路线区域\"]', '[\"「球场伤停补时阶段」伤停补时的灯光仿佛更加灼热，整个体育场被推至沸点。草坪上，禁区前沿排着紧密的人墙，球员们肩并着肩，手臂护住胸前，形成一道移动的屏障。远处的球门在这一刻显得格外宽阔，门柱的影子硬朗地打在草皮上。看台上所有观众都站了起来，无数的身影形成一圈颤抖的轮廓，模糊的旗帜在夜风中猎猎作响。记分牌上的数字显示着3:3的平局，时间刻度停在伤停补时。光从顶棚强力射出，将禁区弧顶的罚球点照得如同舞台中央，四周的空气几乎凝固。\",\"「球场伤停补时阶段」广角定格在球门与禁区之间，人墙严阵以待，守门员在门线前压低重心，整座体育场的视线都聚焦于罚球点。左侧看台无声地亮起一片手机灯光，右侧球迷则咬住围巾。草地被踩踏出无数重叠的足迹，尤其是禁区线附近草皮翻起几道浅痕。球门右上角网窝静止，等待撞击。教练区空无一人，但第四官员举起的电子牌清晰可辨。光从正上方与左右顶棚交织投下，把每一个远近空间填满，唯有远端的底线角旗处在微微的阴影中，为即将可能发生的反抢留出视觉余地。\",\"「球场伤停补时阶段」补时中的决赛草场弥漫着接近终局的肃杀感。中圈到禁区弧这一段区域被无形的手攥紧，人墙背后，球门远角的空间仿佛在主动收缩。看台化为巨大的黑红色背景，点缀着无数晃动的小点。球场中央地带相对空虚，只有几名中场球员的站位拉开宽度，形成随时可以冲刺的空档。远处的角旗区和边线在强烈灯光下泛白，教练区的白线边界依旧清晰，战术纸条散落在地。这个广阔的空间里，下一个呼吸就可能决定所有。顶光无遮无拦，把即将落下的射门路线照得坦坦荡荡。\"]', NULL, -1, -1, '2026-07-10 23:54:27', -1, '2026-07-10 23:54:27', 0, NULL, NULL, 0, NULL, NULL);
INSERT INTO `short_drama_location` VALUES (2077008104339820544, 2077007721974484992, '现代办公室_夜', '深夜的现代办公室，电脑屏幕亮光，桌面杂乱，电水壶和插座位于角落。', 0, '', '[\"办公桌前椅子的正前方位置\",\"门口内侧靠墙的空地\",\"电水壶倾倒处的桌子左侧空地\"]', '[\"「现代办公室_夜」深夜的办公室，空间约二十平米，深灰色地毯覆盖地面，左侧是两扇玻璃窗，窗外城市霓虹灯隐约。右侧墙壁悬挂企业标语，下方是文件柜。中央一张深色办公桌，电脑屏幕为唯一光源，桌面散落纸张和水杯。天花板荧光灯未开，光线从电脑屏幕向右前方散射。前景桌子右侧有空地，左侧靠窗也有站立空间。\",\"「现代办公室_夜」一个中等规模的现代办公室，后墙整面白色，中央挂钟指针指向11点。左侧墙壁有白色百叶窗，光线从窗外微光透入与电脑屏幕蓝光混合。右侧墙壁是嵌入式书架，书本整齐排列。办公桌位于房间中央偏后，桌上一台显示器、电水壶、凌乱文件。天花板吊灯未亮，整体光线昏暗。门口处有一块可站人区域，桌子左侧也有空位。\",\"「现代办公室_夜」约二十平米的封闭办公室，三面墙壁可见：正面墙有白色防火板，左侧墙为玻璃隔断（窗外黑色夜空），右侧墙为木质护墙板。地面铺深色橡胶地板。办公桌位于房间中央稍靠右，桌上电脑亮着，电水壶倒在桌边，插座位在桌下墙角。天花板中央有方形灯槽但未开灯。门口与桌子之间有两米见方的空白区域，可以站立一人；窗户左侧也有约一米宽的通道。\"]', 'https://atlas-media.oss-us-west-1.aliyuncs.com/images/e927b16c08d64b72a0039fcc6232c626-d9b1460b47849a88.jpg', -1, -1, '2026-07-14 20:31:36', 1, '2026-07-14 20:38:50', 0, '[\"https://atlas-media.oss-us-west-1.aliyuncs.com/images/e927b16c08d64b72a0039fcc6232c626-d9b1460b47849a88.jpg\"]', '[\"宽广空间全景，[\\\"「现代办公室_夜」深夜的办公室，空间约二十平米，深灰色地毯覆盖地面，左侧是两扇玻璃窗，窗外城市霓虹灯隐约。右侧墙壁悬挂企业标语，下方是文件柜。中央一张深色办公桌，电脑屏幕为唯一光源，桌面散落纸张和水杯。天花板荧光灯未开，光线从电脑屏幕向右前方散射。前景桌子右侧有空地，左侧靠窗也有站立空间。\\\",\\\"「现代办公室_夜」一个中等规模的现代办公室，后墙整面白色，中央挂钟指针指向11点。左侧墙壁有白色百叶窗，光线从窗外微光透入与电脑屏幕蓝光混合。右侧墙壁是嵌入式书架，书本整齐排列。办公桌位于房间中央偏后，桌上一台显示器、电水壶、凌乱文件。天花板吊灯未亮，整体光线昏暗。门口处有一块可站人区域，桌子左侧也有空位。\\\",\\\"「现代办公室_夜」约二十平米的封闭办公室，三面墙壁可见：正面墙有白色防火板，左侧墙为玻璃隔断（窗外黑色夜空），右侧墙为木质护墙板。地面铺深色橡胶地板。办公桌位于房间中央稍靠右，桌上电脑亮着，电水壶倒在桌边，插座位在桌下墙角。天花板中央有方形灯槽但未开灯。门口与桌子之间有两米见方的空白区域，可以站立一人；窗户左侧也有约一米宽的通道。\\\"]，禁止出现任何角色，纯背景板真实电影级画面质感，真实现实场景，色彩饱满通透，画面干净精致，真实感\"]', 0, NULL, NULL);
INSERT INTO `short_drama_location` VALUES (2077008104394346496, 2077007721974484992, '小巷_日_1985', '八十年代老式小巷，石板路面，墙上有红色标语，二八自行车靠在墙边。', 0, '', '[\"巷口地面中央位置\",\"右侧墙边二八自行车旁的空地\",\"左侧墙标语下方的台阶处\"]', '[\"「小巷_日_1985」一条狭窄的老式小巷，宽约三米，两侧是青砖老墙，墙高约四米，墙面斑驳。右侧墙上用红漆写着「时间就是金钱」的标语，字迹清晰。地面是水泥路面，有些裂缝。巷口方向有一辆黑色的二八自行车斜靠在墙边。光线从巷口一端射入，形成明暗对比。巷子中央地面可以站立，左侧墙根下也有约一米宽的通道。\",\"「小巷_日_1985」巷子长约五十米，两端可见开阔。左侧墙壁爬满半枯的爬山虎，右墙每隔几米有木质门窗（紧闭）。地面是青石板铺成，略有水渍。巷子尽头有一棵老槐树伸出墙头。阳光从头顶偏左照下，在地面投下窗格阴影。巷口方向有一辆老式二八自行车，车把挂着布包。巷子中央空旷处可站人，右侧自行车旁也有空间。\",\"「小巷_日_1985」宽度不足三米的小巷，两侧墙壁均用灰砖砌成，左墙上有模糊的「工业学大庆」标语残迹，右墙新刷了白色石灰。地面为水泥抹平，中央微微隆起。巷口处堆着几个空木箱。远处可见巷子拐角，墙上挂着一个老式电表箱。光线从正上方偏东方向照入，地面光影斑驳。巷口木箱旁、巷子中央靠近右墙位置均有可供落脚的平坦地面。\"]', 'https://atlas-media.oss-us-west-1.aliyuncs.com/images/c1ef1eeb2a64424798068602a181c0cc-e1074ab882e7517b.jpg', -1, -1, '2026-07-14 20:31:36', 1, '2026-07-14 20:38:50', 0, '[\"https://atlas-media.oss-us-west-1.aliyuncs.com/images/c1ef1eeb2a64424798068602a181c0cc-e1074ab882e7517b.jpg\"]', '[\"宽广空间全景，[\\\"「小巷_日_1985」一条狭窄的老式小巷，宽约三米，两侧是青砖老墙，墙高约四米，墙面斑驳。右侧墙上用红漆写着「时间就是金钱」的标语，字迹清晰。地面是水泥路面，有些裂缝。巷口方向有一辆黑色的二八自行车斜靠在墙边。光线从巷口一端射入，形成明暗对比。巷子中央地面可以站立，左侧墙根下也有约一米宽的通道。\\\",\\\"「小巷_日_1985」巷子长约五十米，两端可见开阔。左侧墙壁爬满半枯的爬山虎，右墙每隔几米有木质门窗（紧闭）。地面是青石板铺成，略有水渍。巷子尽头有一棵老槐树伸出墙头。阳光从头顶偏左照下，在地面投下窗格阴影。巷口方向有一辆老式二八自行车，车把挂着布包。巷子中央空旷处可站人，右侧自行车旁也有空间。\\\",\\\"「小巷_日_1985」宽度不足三米的小巷，两侧墙壁均用灰砖砌成，左墙上有模糊的「工业学大庆」标语残迹，右墙新刷了白色石灰。地面为水泥抹平，中央微微隆起。巷口处堆着几个空木箱。远处可见巷子拐角，墙上挂着一个老式电表箱。光线从正上方偏东方向照入，地面光影斑驳。巷口木箱旁、巷子中央靠近右墙位置均有可供落脚的平坦地面。\\\"]，禁止出现任何角色，纯背景板真实电影级画面质感，真实现实场景，色彩饱满通透，画面干净精致，真实感\"]', 0, NULL, NULL);
INSERT INTO `short_drama_location` VALUES (2077008104432095232, 2077007721974484992, '街道办门口_日', '街道办事处门口，有传达室和铁门，门口台阶下是水泥路，对面有行道树。', 0, '', '[\"街道办门口台阶正下方的位置\",\"传达室窗户左侧墙边\",\"路对面人行道靠近电线杆处\"]', '[\"「街道办门口_日」一栋灰砖二层小楼，门口上方挂着「XX街道办事处」木牌。楼前有三级水泥台阶，台阶两侧铁质扶手。左侧是传达室，小窗户上方有遮阳棚。门前空地为水泥路面，约十平方米，对面是普通的居民楼围墙，墙脚种着冬青。光线从右上方斜照，在台阶上形成阴影。台阶正下方中央、传达室窗边、路对面冬青旁都有平坦可站区域。\",\"「街道办门口_日」政府机关式的老旧建筑，门口为两扇对开铁栅栏门，门内是院落。门外台阶较宽，约五级，两侧有花盆。右侧墙面上贴着红色通知栏。地面为水磨石，有些破损。远处街道上有电线杆和停着的二八大杠自行车。阳光从左侧高层建筑缝隙中射下，照亮铁门局部。台阶中段、传达室门口左侧、路对面树下空地均可站立。\",\"「街道办门口_日」典型八十年代街道办公点，单层门厅带歇山顶。门厅前有宽大平台，平台边缘有低矮水泥护栏。传达室位于左侧，窗口很小。平台下是水泥马路，马路对面是砖砌围墙，墙上有「为人民服务」标语。光线从东南方照射，平台右侧有树影。平台上中央区域、传达室窗口下方、马路对面围墙根处均可落位。\"]', 'https://atlas-media.oss-us-west-1.aliyuncs.com/images/ee4c0cd60f6b43f3a459937e0272cc88-1f5cd218d1905a4d.jpg', -1, -1, '2026-07-14 20:31:36', 1, '2026-07-14 20:38:50', 0, '[\"https://atlas-media.oss-us-west-1.aliyuncs.com/images/ee4c0cd60f6b43f3a459937e0272cc88-1f5cd218d1905a4d.jpg\"]', '[\"宽广空间全景，[\\\"「街道办门口_日」一栋灰砖二层小楼，门口上方挂着「XX街道办事处」木牌。楼前有三级水泥台阶，台阶两侧铁质扶手。左侧是传达室，小窗户上方有遮阳棚。门前空地为水泥路面，约十平方米，对面是普通的居民楼围墙，墙脚种着冬青。光线从右上方斜照，在台阶上形成阴影。台阶正下方中央、传达室窗边、路对面冬青旁都有平坦可站区域。\\\",\\\"「街道办门口_日」政府机关式的老旧建筑，门口为两扇对开铁栅栏门，门内是院落。门外台阶较宽，约五级，两侧有花盆。右侧墙面上贴着红色通知栏。地面为水磨石，有些破损。远处街道上有电线杆和停着的二八大杠自行车。阳光从左侧高层建筑缝隙中射下，照亮铁门局部。台阶中段、传达室门口左侧、路对面树下空地均可站立。\\\",\\\"「街道办门口_日」典型八十年代街道办公点，单层门厅带歇山顶。门厅前有宽大平台，平台边缘有低矮水泥护栏。传达室位于左侧，窗口很小。平台下是水泥马路，马路对面是砖砌围墙，墙上有「为人民服务」标语。光线从东南方照射，平台右侧有树影。平台上中央区域、传达室窗口下方、马路对面围墙根处均可落位。\\\"]，禁止出现任何角色，纯背景板真实电影级画面质感，真实现实场景，色彩饱满通透，画面干净精致，真实感\"]', 0, NULL, NULL);
INSERT INTO `short_drama_location` VALUES (2077008104465649664, 2077007721974484992, '废弃仓库_日', '空旷的旧仓库内部，水泥地面，墙面有老标语，中央放置一张木桌和两把椅子。', 0, '', '[\"木桌左侧椅子前方的位置\",\"仓库大门内侧正对门的位置\",\"右侧墙「工业学大庆」标语下方\"]', '[\"「废弃仓库_日」高大空旷的老仓库，内部面积约两百平米，顶棚为弧形钢架，高约八米。地面是水泥抹平，有多处裂缝和油渍。前墙（南墙）有一排高窗，玻璃蒙尘，光线从窗中斜射入形成光柱。后墙正中写有红色大字「工业学大庆」，字迹斑驳。仓库中央放着一张老式木桌，两侧各一把木椅。左侧墙边堆着几个空铁桶。右墙下靠近标语处有一块干净地面，木桌前后也有空间。\",\"「废弃仓库_日」长方形仓库，东西走向，北侧墙面有一扇双开铁皮大门，半开着。室内光线从大门和南墙高窗进入，混合照亮。地面为灰色水泥，有推车压痕。仓库左侧靠墙有木架，架上零散工具。右侧墙有白色涂料脱落，露出红砖。中央木桌桌面放着两盒烟和几张纸。桌椅周围有约两米见方空地。大门内侧、桌子前方、右侧墙根可站人。\",\"「废弃仓库_日」仓库内部宽畅，纵深约四十米，天花板可见裸露木桁架和瓦片。墙面为抹灰白墙，部分脱落，露出砖块。南侧高窗透入冷色天光，地面有光斑。北侧大门关闭，有门缝透光。仓库中央偏左摆放一张旧木桌，桌面摆放烟盒和铅笔。左侧墙边有生锈铁架。右侧墙「工业学大庆」标语下方地面干净。桌子前侧、左侧铁架旁、右侧标语下均有可供人物站立的平整区域。\"]', 'https://atlas-media.oss-us-west-1.aliyuncs.com/images/22364ee6ea8649788330827d67015aaf-390fb3c2f4c24ee7.jpg', -1, -1, '2026-07-14 20:31:36', 1, '2026-07-14 20:38:50', 0, '[\"https://atlas-media.oss-us-west-1.aliyuncs.com/images/22364ee6ea8649788330827d67015aaf-390fb3c2f4c24ee7.jpg\"]', '[\"宽广空间全景，[\\\"「废弃仓库_日」高大空旷的老仓库，内部面积约两百平米，顶棚为弧形钢架，高约八米。地面是水泥抹平，有多处裂缝和油渍。前墙（南墙）有一排高窗，玻璃蒙尘，光线从窗中斜射入形成光柱。后墙正中写有红色大字「工业学大庆」，字迹斑驳。仓库中央放着一张老式木桌，两侧各一把木椅。左侧墙边堆着几个空铁桶。右墙下靠近标语处有一块干净地面，木桌前后也有空间。\\\",\\\"「废弃仓库_日」长方形仓库，东西走向，北侧墙面有一扇双开铁皮大门，半开着。室内光线从大门和南墙高窗进入，混合照亮。地面为灰色水泥，有推车压痕。仓库左侧靠墙有木架，架上零散工具。右侧墙有白色涂料脱落，露出红砖。中央木桌桌面放着两盒烟和几张纸。桌椅周围有约两米见方空地。大门内侧、桌子前方、右侧墙根可站人。\\\",\\\"「废弃仓库_日」仓库内部宽畅，纵深约四十米，天花板可见裸露木桁架和瓦片。墙面为抹灰白墙，部分脱落，露出砖块。南侧高窗透入冷色天光，地面有光斑。北侧大门关闭，有门缝透光。仓库中央偏左摆放一张旧木桌，桌面摆放烟盒和铅笔。左侧墙边有生锈铁架。右侧墙「工业学大庆」标语下方地面干净。桌子前侧、左侧铁架旁、右侧标语下均有可供人物站立的平整区域。\\\"]，禁止出现任何角色，纯背景板真实电影级画面质感，真实现实场景，色彩饱满通透，画面干净精致，真实感\"]', 0, NULL, NULL);
INSERT INTO `short_drama_location` VALUES (2077008104541147136, 2077007721974484992, '工厂办公室_日', '八十年代工厂办公室，木制办公桌、转盘电话、文件柜，窗外可见厂区。', 0, '', '[\"办公桌前椅子正前方的位置\",\"窗台下方靠墙的空地\",\"门口内侧文件柜旁\"]', '[\"「工厂办公室_日」一间十五平米的老式办公室，水泥地面刷了绿色油漆，略有磨损。正面墙有木框玻璃窗，窗外是工厂院落，可见烟囱和车间屋顶。窗下放着一张深褐色办公桌，桌面有玻璃板压着照片，一部黑色转盘电话放在右侧。左侧墙立着铁皮文件柜，绿色漆。右墙贴着生产进度表。阳光从窗外照射进来，在桌面形成光斑。桌前空地约两平米，窗台下、门口文件柜旁也有空间。\",\"「工厂办公室_日」房间方正，南墙整面为窗户，光线充足。窗户为老式钢窗，窗外有梧桐树枝叶。屋顶为白色涂料，日光灯管一支亮着。地面铺设浅色地板革，有磨损。办公桌靠东墙放置，桌上有笔筒、茶杯和电话。西墙有木制衣架和镜子。北墙门边有铁皮柜。桌前方地板、西墙衣架旁、北墙柜前均可落位。\",\"「工厂办公室_日」约十二平米的办公空间，乳胶漆墙面局部发黄。南侧窗户透入明亮日光，照射在东侧白色墙壁上。办公桌为深棕色三抽屉木桌，居中放置，桌上电话、台历、一部老式计算机。右侧墙角有饮水机和水桶。左侧墙角放置扫帚和簸箕。门在西墙。地板为红砖铺成，铺垫草席。桌前方、门口左侧墙角、右墙饮水机旁都有站立空间。\"]', 'https://atlas-media.oss-us-west-1.aliyuncs.com/images/86f032880f2e43409a9df446490e7032-6d12c0e8763f4c3e.jpg', -1, -1, '2026-07-14 20:31:36', 1, '2026-07-14 20:38:51', 0, '[\"https://atlas-media.oss-us-west-1.aliyuncs.com/images/86f032880f2e43409a9df446490e7032-6d12c0e8763f4c3e.jpg\"]', '[\"宽广空间全景，[\\\"「工厂办公室_日」一间十五平米的老式办公室，水泥地面刷了绿色油漆，略有磨损。正面墙有木框玻璃窗，窗外是工厂院落，可见烟囱和车间屋顶。窗下放着一张深褐色办公桌，桌面有玻璃板压着照片，一部黑色转盘电话放在右侧。左侧墙立着铁皮文件柜，绿色漆。右墙贴着生产进度表。阳光从窗外照射进来，在桌面形成光斑。桌前空地约两平米，窗台下、门口文件柜旁也有空间。\\\",\\\"「工厂办公室_日」房间方正，南墙整面为窗户，光线充足。窗户为老式钢窗，窗外有梧桐树枝叶。屋顶为白色涂料，日光灯管一支亮着。地面铺设浅色地板革，有磨损。办公桌靠东墙放置，桌上有笔筒、茶杯和电话。西墙有木制衣架和镜子。北墙门边有铁皮柜。桌前方地板、西墙衣架旁、北墙柜前均可落位。\\\",\\\"「工厂办公室_日」约十二平米的办公空间，乳胶漆墙面局部发黄。南侧窗户透入明亮日光，照射在东侧白色墙壁上。办公桌为深棕色三抽屉木桌，居中放置，桌上电话、台历、一部老式计算机。右侧墙角有饮水机和水桶。左侧墙角放置扫帚和簸箕。门在西墙。地板为红砖铺成，铺垫草席。桌前方、门口左侧墙角、右墙饮水机旁都有站立空间。\\\"]，禁止出现任何角色，纯背景板真实电影级画面质感，真实现实场景，色彩饱满通透，画面干净精致，真实感\"]', 0, NULL, NULL);
INSERT INTO `short_drama_location` VALUES (2077008104583090176, 2077007721974484992, '出租屋_夜', '狭小的出租屋，室内有床、桌子和黑板，黑板画着时间轴，灯光昏暗。', 0, '', '[\"黑板前方半米处地面\",\"桌子右侧靠墙位置\",\"门口内侧的空地\"]', '[\"「出租屋_夜」一间约十平米的单间，水泥地面，白灰墙面陈旧泛黄。北墙上有小窗户，挂着花布窗帘。东墙摆放一张单人木床，铺着格子床单。西墙靠着一张方桌，桌面散落纸笔和茶杯。南墙立着一块小黑板，黑板用粉笔画着时间轴和文字。屋顶悬着一盏白炽灯泡，发出昏黄光线，向下照射。床前、黑板前、桌子右侧均有可站立空地。\",\"「出租屋_夜」逼仄的出租屋，空间狭小，内有双层铁床靠左墙，下铺放行李。房间中央是一张折叠桌，桌上有电炉和锅。右墙钉着一块黑板，黑板边角有残缺，上面用粉笔写着「1985-2000」等字样。地面为砖地，不平整。灯光从屋顶一盏裸露灯泡发出，光线偏黄，在墙角形成阴影。桌子前方空地、双层床铺下方前端、黑板的左侧均可落位。\",\"「出租屋_夜」低矮的出租屋，层高约两米五，屋顶有木梁。西墙有一扇木门，门边有挂锁。南墙是大面积窗户，但被报纸糊住。北墙放置一个老旧衣柜，柜门敞开。中央一张矮方桌，桌上有手绘图纸和圆珠笔。地面铺着塑料地板革，已有破损。灯光从屋顶日光灯管发出，白色冷光，照亮全屋。黑板挂于东墙，上面时间轴清晰。桌子周围、衣柜前、门口都有站人空间。\"]', 'https://atlas-media.oss-us-west-1.aliyuncs.com/images/1c3fe372d8c34a28ad4d157e973a8342-acdc977ee2a76eab.jpg', -1, -1, '2026-07-14 20:31:36', 1, '2026-07-14 20:38:51', 0, '[\"https://atlas-media.oss-us-west-1.aliyuncs.com/images/1c3fe372d8c34a28ad4d157e973a8342-acdc977ee2a76eab.jpg\"]', '[\"宽广空间全景，[\\\"「出租屋_夜」一间约十平米的单间，水泥地面，白灰墙面陈旧泛黄。北墙上有小窗户，挂着花布窗帘。东墙摆放一张单人木床，铺着格子床单。西墙靠着一张方桌，桌面散落纸笔和茶杯。南墙立着一块小黑板，黑板用粉笔画着时间轴和文字。屋顶悬着一盏白炽灯泡，发出昏黄光线，向下照射。床前、黑板前、桌子右侧均有可站立空地。\\\",\\\"「出租屋_夜」逼仄的出租屋，空间狭小，内有双层铁床靠左墙，下铺放行李。房间中央是一张折叠桌，桌上有电炉和锅。右墙钉着一块黑板，黑板边角有残缺，上面用粉笔写着「1985-2000」等字样。地面为砖地，不平整。灯光从屋顶一盏裸露灯泡发出，光线偏黄，在墙角形成阴影。桌子前方空地、双层床铺下方前端、黑板的左侧均可落位。\\\",\\\"「出租屋_夜」低矮的出租屋，层高约两米五，屋顶有木梁。西墙有一扇木门，门边有挂锁。南墙是大面积窗户，但被报纸糊住。北墙放置一个老旧衣柜，柜门敞开。中央一张矮方桌，桌上有手绘图纸和圆珠笔。地面铺着塑料地板革，已有破损。灯光从屋顶日光灯管发出，白色冷光，照亮全屋。黑板挂于东墙，上面时间轴清晰。桌子周围、衣柜前、门口都有站人空间。\\\"]，禁止出现任何角色，纯背景板真实电影级画面质感，真实现实场景，色彩饱满通透，画面干净精致，真实感\"]', 0, NULL, NULL);
INSERT INTO `short_drama_location` VALUES (2077008104604061696, 2077007721974484992, '百货大楼门口_日', '八十年代百货大楼正门，台阶宽大，门口有立柱，橱窗陈列商品，街道上有行人。', 1, '几位穿着中山装和军便装的市民站在台阶上下，有的正在看橱窗，有的在交谈。', '[\"百货大楼正门台阶最高一级中央\",\"右侧立柱旁的地面\",\"台阶下左侧报栏前\",\"对面人行道树下\"]', '[\"「百货大楼门口_日」一栋三层百货大楼，正立面贴白色瓷砖，一层为玻璃橱窗，展示布料、搪瓷盆等商品。楼顶立着「百货大楼」红色大字。门前有五级水磨石台阶，两侧有方形立柱，柱身贴着宣传画。台阶下是宽敞的马路，路边有梧桐树。光学从东南方照来，大楼正面明亮，橱窗玻璃反光。台阶上、立柱旁、报栏前、对面树荫下均有可供人物站立的空间。\",\"「百货大楼门口_日」八十年代百货商场大门，门楣上方有遮雨棚，棚下悬挂「为人民服务」标语。大门为三扇玻璃推拉门，门内可见柜台。左右两侧橱窗内陈列着自行车、缝纫机等展品。门前广场铺着六边形水泥砖，有数根电线杆。光线充足，阳光从右后方照射，左前方有阴影。大门正前方台阶、右侧电线杆附近、左侧橱窗旁均可作为人物落位点。\",\"「百货大楼门口_日」大型百货商店正门区域，建筑为现代主义风格，立面简洁。大门前有宽阔平台，平台四角有花坛，内种月季。地面为水磨石，洁净明亮。左侧有一块公告栏，贴着红色海报。远处可见公交站台，有行人等车。光线从正头顶略偏南，地面有微弱阴影。平台中央、左侧公告栏前、右侧花坛旁均有平坦地面适合站立。\"]', 'https://atlas-media.oss-us-west-1.aliyuncs.com/images/c94f6cb0c4364d5db271b9606c2bde94-101b8d0c012901fe.jpg', -1, -1, '2026-07-14 20:31:36', 1, '2026-07-14 20:38:51', 0, '[\"https://atlas-media.oss-us-west-1.aliyuncs.com/images/c94f6cb0c4364d5db271b9606c2bde94-101b8d0c012901fe.jpg\"]', '[\"宽广空间全景，[\\\"「百货大楼门口_日」一栋三层百货大楼，正立面贴白色瓷砖，一层为玻璃橱窗，展示布料、搪瓷盆等商品。楼顶立着「百货大楼」红色大字。门前有五级水磨石台阶，两侧有方形立柱，柱身贴着宣传画。台阶下是宽敞的马路，路边有梧桐树。光学从东南方照来，大楼正面明亮，橱窗玻璃反光。台阶上、立柱旁、报栏前、对面树荫下均有可供人物站立的空间。\\\",\\\"「百货大楼门口_日」八十年代百货商场大门，门楣上方有遮雨棚，棚下悬挂「为人民服务」标语。大门为三扇玻璃推拉门，门内可见柜台。左右两侧橱窗内陈列着自行车、缝纫机等展品。门前广场铺着六边形水泥砖，有数根电线杆。光线充足，阳光从右后方照射，左前方有阴影。大门正前方台阶、右侧电线杆附近、左侧橱窗旁均可作为人物落位点。\\\",\\\"「百货大楼门口_日」大型百货商店正门区域，建筑为现代主义风格，立面简洁。大门前有宽阔平台，平台四角有花坛，内种月季。地面为水磨石，洁净明亮。左侧有一块公告栏，贴着红色海报。远处可见公交站台，有行人等车。光线从正头顶略偏南，地面有微弱阴影。平台中央、左侧公告栏前、右侧花坛旁均有平坦地面适合站立。\\\"]，禁止出现任何角色，纯背景板真实电影级画面质感，真实现实场景，色彩饱满通透，画面干净精致，真实感\"]', 0, NULL, NULL);
INSERT INTO `short_drama_location` VALUES (2077008104675364864, 2077007721974484992, '厂庆礼堂_夜', '工厂礼堂内部，舞台上有麦克风，台下长凳坐满工人，红灯笼和横幅装饰。', 1, '几十名穿着蓝色工装的工人整齐坐在长凳上，面向舞台，表情喜悦。', '[\"舞台中央麦克风架前方位置\",\"舞台左侧幕布旁\",\"观众席前排中间走道\",\"礼堂后门入口处\"]', '[\"「厂庆礼堂_夜」一座中型礼堂，空间约三百平米，两侧墙壁挂满红色锦旗和奖状。舞台为砖砌，高约一米，铺着红色地毯，中央立着麦克风，背景幕布为深红色绒布，上方悬挂「厂庆联欢晚会」横幅。观众席摆着二十排长木凳，坐满穿工装的工人。屋顶悬挂多盏白炽灯和串串红灯笼，灯光暖黄。舞台侧方、观众席中间走廊、后门口均有空地可站人。\",\"「厂庆礼堂_夜」礼堂内部呈长方形，前高后低，地板为水泥磨光。天花板有木横梁，垂吊着彩带和纸花。舞台左后侧有上台台阶，右侧放置锣鼓乐器。台下长凳密集排列，过道狭窄，工人拥挤而坐，有些站着。礼堂后墙有双开木门，紧闭。光线主要来自舞台上的聚光灯和两侧壁灯，明亮温暖。舞台中央地面、右侧锣鼓旁、后墙门边有可落位空间。\",\"「厂庆礼堂_夜」宽大的老式礼堂，墙面下半部刷绿色墙裙，上半部白墙。窗户用深色帘幕遮住。舞台台口有弧形边缘，台上有两张木椅和一张讲台。台前摆满花篮。观众席中央过道约一米宽，两侧长凳坐满人。天花板中央一盏大吊灯，四周小灯，整体照明均匀。舞台正前方、左侧幕布缺口处、礼堂后门旁均可站立。\"]', 'https://atlas-media.oss-us-west-1.aliyuncs.com/images/a9295d479fef45f198bffd3ef850eacd-588538d1e9d8e094.jpg', -1, -1, '2026-07-14 20:31:36', 1, '2026-07-14 20:38:51', 0, '[\"https://atlas-media.oss-us-west-1.aliyuncs.com/images/a9295d479fef45f198bffd3ef850eacd-588538d1e9d8e094.jpg\"]', '[\"宽广空间全景，[\\\"「厂庆礼堂_夜」一座中型礼堂，空间约三百平米，两侧墙壁挂满红色锦旗和奖状。舞台为砖砌，高约一米，铺着红色地毯，中央立着麦克风，背景幕布为深红色绒布，上方悬挂「厂庆联欢晚会」横幅。观众席摆着二十排长木凳，坐满穿工装的工人。屋顶悬挂多盏白炽灯和串串红灯笼，灯光暖黄。舞台侧方、观众席中间走廊、后门口均有空地可站人。\\\",\\\"「厂庆礼堂_夜」礼堂内部呈长方形，前高后低，地板为水泥磨光。天花板有木横梁，垂吊着彩带和纸花。舞台左后侧有上台台阶，右侧放置锣鼓乐器。台下长凳密集排列，过道狭窄，工人拥挤而坐，有些站着。礼堂后墙有双开木门，紧闭。光线主要来自舞台上的聚光灯和两侧壁灯，明亮温暖。舞台中央地面、右侧锣鼓旁、后墙门边有可落位空间。\\\",\\\"「厂庆礼堂_夜」宽大的老式礼堂，墙面下半部刷绿色墙裙，上半部白墙。窗户用深色帘幕遮住。舞台台口有弧形边缘，台上有两张木椅和一张讲台。台前摆满花篮。观众席中央过道约一米宽，两侧长凳坐满人。天花板中央一盏大吊灯，四周小灯，整体照明均匀。舞台正前方、左侧幕布缺口处、礼堂后门旁均可站立。\\\"]，禁止出现任何角色，纯背景板真实电影级画面质感，真实现实场景，色彩饱满通透，画面干净精致，真实感\"]', 0, NULL, NULL);
INSERT INTO `short_drama_location` VALUES (2077008104721502208, 2077007721974484992, '工厂大门_晨', '清晨工厂大门，新挂牌「华夏信息服务中心」，自行车棚，远处烟囱冒烟。', 1, '几名穿着蓝色工装的工人推着自行车或步行进入厂门，路边有早点摊的模糊身影。', '[\"厂门口新挂牌下方正中间\",\"右侧自行车棚外侧\",\"左侧传达室门口台阶\",\"马路对面老槐树旁\"]', '[\"「工厂大门_晨」工厂大门为铁制对开栅栏门，门柱为红砖砌成，左侧门柱上挂着崭新白底黑字牌子「华夏信息服务中心」。进门可见一条水泥路通向厂区。右侧有一排自行车棚，棚内停着几十辆自行车。远处工厂烟囱缓缓冒白烟，背景是朦胧的晨曦天空。阳光从东方斜照，拉长门柱影子。大门正下方、自行车棚外面、左侧传达室门口、马路对面树荫下均有平坦地面。\",\"「工厂大门_晨」晨曦中的老工厂入口，灰砖围墙高约三米，大门敞开。左侧传达室为平房，窗户透出灯光。门右侧墙上贴着红色通知。路面是水泥铺设，有些许落叶。厂内远处可见车间屋顶和冒烟烟囱。天空泛鱼肚白，东边有朝霞。光线从右前方射来，门牌上的字清晰反光。大门中央、传达室左侧空地、路边电线杆旁都可以站人。\",\"「工厂大门_晨」工厂正门区域，宽阔的厂前空地上矗立着新立的招牌。大门两侧各有一棵法国梧桐，枝叶繁茂。地面为柏油路，路中央画有白色虚线。厂门内侧两边是花坛，种着冬青。远处厂房轮廓在晨雾中若隐若现，烟囱吐烟。光线柔和，晨光从建筑物缝隙中洒落。招牌下方、左侧花坛旁、右侧梧桐树下均是可站立的空旷位置。\"]', 'https://atlas-media.oss-us-west-1.aliyuncs.com/images/50f5cc1fb8e74dac95d589c23dc2daf7-7fb4da35d3075e95.jpg', -1, -1, '2026-07-14 20:31:36', 1, '2026-07-14 20:38:51', 0, '[\"https://atlas-media.oss-us-west-1.aliyuncs.com/images/50f5cc1fb8e74dac95d589c23dc2daf7-7fb4da35d3075e95.jpg\"]', '[\"宽广空间全景，[\\\"「工厂大门_晨」工厂大门为铁制对开栅栏门，门柱为红砖砌成，左侧门柱上挂着崭新白底黑字牌子「华夏信息服务中心」。进门可见一条水泥路通向厂区。右侧有一排自行车棚，棚内停着几十辆自行车。远处工厂烟囱缓缓冒白烟，背景是朦胧的晨曦天空。阳光从东方斜照，拉长门柱影子。大门正下方、自行车棚外面、左侧传达室门口、马路对面树荫下均有平坦地面。\\\",\\\"「工厂大门_晨」晨曦中的老工厂入口，灰砖围墙高约三米，大门敞开。左侧传达室为平房，窗户透出灯光。门右侧墙上贴着红色通知。路面是水泥铺设，有些许落叶。厂内远处可见车间屋顶和冒烟烟囱。天空泛鱼肚白，东边有朝霞。光线从右前方射来，门牌上的字清晰反光。大门中央、传达室左侧空地、路边电线杆旁都可以站人。\\\",\\\"「工厂大门_晨」工厂正门区域，宽阔的厂前空地上矗立着新立的招牌。大门两侧各有一棵法国梧桐，枝叶繁茂。地面为柏油路，路中央画有白色虚线。厂门内侧两边是花坛，种着冬青。远处厂房轮廓在晨雾中若隐若现，烟囱吐烟。光线柔和，晨光从建筑物缝隙中洒落。招牌下方、左侧花坛旁、右侧梧桐树下均是可站立的空旷位置。\\\"]，禁止出现任何角色，纯背景板真实电影级画面质感，真实现实场景，色彩饱满通透，画面干净精致，真实感\"]', 0, NULL, NULL);

-- ----------------------------
-- Table structure for short_drama_audio
-- ----------------------------
DROP TABLE IF EXISTS `short_drama_audio`;
CREATE TABLE `short_drama_audio`  (
                                       `id` bigint NOT NULL COMMENT '主键',
                                       `project_id` bigint NOT NULL COMMENT '项目ID',
                                       `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '语音资产名称',
                                       `audio_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'narration' COMMENT '语音类型：narration(旁白)/dialogue(对白)',
                                       `text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '语音文案（生成语音用的文本）',
                                       `voice` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '音色（如 alloy/onyx）',
                                       `audio_oss_id` bigint NULL DEFAULT NULL COMMENT '音频文件OSS ID',
                                       `audio_url` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '音频文件URL',
                                       `linked_storyboard_id` bigint NULL DEFAULT NULL COMMENT '对白关联的分镜ID（NULL=全局旁白）',
                                       `duration_seconds` int NULL DEFAULT NULL COMMENT '音频时长（秒）',
                                       `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                                       `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                                       `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                                       `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户Id',
                                       PRIMARY KEY (`id`) USING BTREE,
                                       INDEX `idx_project_id`(`project_id` ASC) USING BTREE,
                                       INDEX `idx_linked_storyboard_id`(`linked_storyboard_id` ASC) USING BTREE,
                                       INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '短剧语音资产表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for short_drama_project
-- ----------------------------
DROP TABLE IF EXISTS `short_drama_project`;
CREATE TABLE `short_drama_project`  (
                                        `id` bigint NOT NULL COMMENT '主键',
                                        `user_id` bigint NOT NULL COMMENT '用户ID',
                                        `project_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '项目名称',
                                        `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '项目描述',
                                        `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'draft' COMMENT '状态：draft/active/archived',
                                        `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                                        `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                                        `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                                        `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                        `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户Id',
                                        `art_style` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'realistic' COMMENT '视觉风格：american-comic=美漫, chinese-comic=国漫, japanese-anime=日系, realistic=写实',
                                        `composed_video_oss_id` bigint NULL DEFAULT NULL COMMENT '最新成片OSS对象ID',
                                        `compose_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '合成状态：NULL未合成，pending/processing/done/failed',
                                        `compose_job_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '当前合成任务标识（并发防重）',
                                        `compose_progress` int NOT NULL DEFAULT 0 COMMENT '合成进度：0-100',
                                        `compose_transition_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'dissolve' COMMENT '转场类型：none/dissolve/fade/slide',
                                        `compose_transition_duration_seconds` decimal(6, 3) NOT NULL DEFAULT 0.500 COMMENT '单次转场时长（秒）',
                                        `compose_aspect_ratio` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '9:16' COMMENT '成片画幅：9:16/16:9/1:1',
                                        `composed_video_duration_seconds` decimal(12, 3) NULL DEFAULT NULL COMMENT '最终成片实际时长（秒，ffprobe测量）',
                                        `compose_error_message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最近一次合成失败原因',
                                        `composed_at` datetime NULL DEFAULT NULL COMMENT '最近一次合成完成时间',
                                        PRIMARY KEY (`id`) USING BTREE,
                                        INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
                                        INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '短剧项目表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of short_drama_project
-- ----------------------------
INSERT INTO `short_drama_project` VALUES (2077007721974484992, 1, '短剧项目', '50岁失意中年小明意外穿越到1985年，利用未来知识创业，在时代洪流中改写命运，最终留下并影响一代人。', 'draft', -1, -1, '2026-07-14 20:30:05', -1, '2026-07-14 20:51:14', 0, 'realistic', NULL, 'done', NULL, 100, 'fade', 0.300, '9:16', 32.267, NULL, '2026-07-14 20:51:14');

-- ----------------------------
-- Table structure for short_drama_script
-- ----------------------------
DROP TABLE IF EXISTS `short_drama_script`;
CREATE TABLE `short_drama_script`  (
                                       `id` bigint NOT NULL COMMENT '主键',
                                       `project_id` bigint NOT NULL COMMENT '项目ID',
                                       `script_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '剧本名称',
                                       `script_text` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '剧本文本',
                                       `outline_text` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '大纲文本',
                                       `tone` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '风格/基调',
                                       `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'manual' COMMENT '来源：manual/ai',
                                       `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                                       `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                                       `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                                       `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户Id',
                                       PRIMARY KEY (`id`) USING BTREE,
                                       INDEX `idx_project_id`(`project_id` ASC) USING BTREE,
                                       INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '短剧剧本表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of short_drama_script
-- ----------------------------
INSERT INTO `short_drama_script` VALUES (2077007722058371072, 2077007721974484992, '重启1985', '1. 内景 现代办公室 夜\n\n办公室昏暗，只有电脑屏幕亮着。50岁的小明穿着皱巴巴的衬衫，盯着屏幕上的辞退邮件。他拿起手机，看到妻子发来的离婚协议截图。手抖着放下手机，碰倒桌上的电水壶，水洒在插座上。火花闪过，小明倒在椅子上。\n\n2. 外景 小巷 日 1985年\n\n小明醒来，发现自己躺在地上，身穿80年代蓝色工装。他摸着脸——没有皱纹。旁边老式自行车铃响，一个年轻人喊：“小明，上班迟到了！”小明看着路口的二八自行车，墙上的标语“时间就是金钱”，愣住。他低头看手腕上的电子表：1985年6月。他深吸一口气，眼神从迷茫变成坚定：“再来一次。”\n\n3. 外景 街道办门口 日\n\n小明拿着一叠手写方案，站在街道办门口。门卫拦住他。小明：“同志，我有商业计划，能帮工厂接全国订单！”门卫：“胡闹！”推他出去。小明捡起散落的纸，抬头看到路对面一个穿中山装的中年男人（老陈）正在看他的方案。老陈：“你这上面写的‘电商’是啥？”小明眼睛一亮：“陈厂长，我能让您仓库里的积压货三天卖掉。”\n\n4. 内景 废弃仓库 日\n\n老陈和小明对面坐着。桌上摆着两盒烟。老陈：“我信你一次，但怎么弄？”小明在纸上画：“用电话线连电脑，搞个库存信息库。我写程序。”老陈：“钱呢？”小明：“我先拉商户，您出设备。”老陈犹豫后点头。小明看着仓库墙上的“工业学大庆”，低声：“这次一定要成。”\n\n5. 内景 工厂办公室 日\n\n办公桌上电话响起。老陈接起，脸色变白。挂断后对小明：“国营百货举报我们投机倒把，工商明天来查。”小明快速翻看账本：“我们走的是信息服务，不是倒卖。”老陈：“没人认可这个。”小明：“明天我来说。”\n\n6. 内景 小明的出租屋 夜\n\n团队成员（两个年轻人）拍桌：“不干了，风险太大！”老陈沉默。小明拿出一块黑板，画出时间轴：“1985年，电脑刚进中国，我们的优势是信息差。不做商品买卖，做平台——帮工厂和供销社匹配库存。”他指着黑板：“明天工商来，我就说这是‘技术咨询’。”老陈抬头：“你哪来的这些？”小明：“梦里学的。”\n\n7. 外景 百货大楼门口 日\n\n工商人员和小明、老陈站在门口。小明拿出一份表格：“这是通过我们信息服务成交的订单，全部有记录，不碰资金，只收咨询费。”工商人员翻看，递给旁边一个戴眼镜的中年人（百货公司经理）。经理看了后低声：“这方法能帮我们清库存？”小明：“三天见效。”经理点头。工商人员收起表格：“先观察。”\n\n8. 内景 厂庆礼堂 夜\n\n工人坐满长凳。老陈上台：“感谢小明的信息平台，咱们工厂活了。”台下鼓掌。小明站在侧台，看着灯光下兴奋的面孔。他手摸到口袋里的现代身份证——早已发黄。他默默撕碎。主持人：“请小明同志讲话！”小明上前，拿着话筒停顿三秒：“各位，未来三十年，互联网会改变一切。今天，我们从这里开始。”台下掌声雷动。\n\n9. 外景 工厂大门 晨\n\n阳光照在新挂牌上：“华夏信息服务中心”。小明推着自行车出来，老陈追上：“下一步？”小明：“跑遍全国，建数据库。”老陈笑：“疯了。”小明跨上车：“1985年，疯的人才能赢。”他蹬车远去，街道两旁的工厂烟囱开始冒烟。', '1.现代办公室夜，小明被老板辞退，回家发现妻子要离婚，绝望中碰倒电水壶触电。2.1985年小巷外景清晨，小明醒来发现变成25岁身体，确认穿越，决定利用记忆创业。3.街道办外景日，小明推销‘互联网购物’概念，被当成疯子赶出，遇到工厂副厂长老陈愿意听。4.废弃仓库内景，小明说服老陈入股，用记忆设计简易电商系统，团队组建。5.工厂办公室内景，初获订单，却遭国营百货公司经理举报，面临查封危机。6.小作家中内景夜，团队内讧，老陈动摇，小明用未来管理经验分析市场，决定转型做信息中介。7.百货大楼外景日，小明带团队利用数据库帮商家库存匹配，化解危机，赢取首个大客户。8.厂庆礼堂内景夜，小明站在台上，回想现代生活，决定留下，带领工人转型。9.同场景，小明讲话，镜头拉远，工厂转型成功。', '现实与励志', 'llm', -1, -1, '2026-07-14 20:30:05', -1, '2026-07-14 20:30:05', 0);

-- ----------------------------
-- Table structure for short_drama_storyboard
-- ----------------------------
DROP TABLE IF EXISTS `short_drama_storyboard`;
CREATE TABLE `short_drama_storyboard`  (
                                           `id` bigint NOT NULL COMMENT '主键',
                                           `project_id` bigint NOT NULL COMMENT '项目ID',
                                           `script_id` bigint NOT NULL COMMENT '剧本ID',
                                           `scene_no` int NOT NULL COMMENT '分镜序号',
                                           `scene_title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分镜标题',
                                           `scene_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '分镜说明',
                                           `scene_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'daily' COMMENT '场景类型：daily/emotion/action/epic/suspense',
                                           `shot_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '镜头景别：如平视中景/俯拍远景',
                                           `camera_move` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '镜头运动：如缓推/固定/跟随/环绕',
                                           `characters_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '镜头角色列表JSON：[{name,appearance,slot}]',
                                           `location_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '场景名称（匹配资产库）',
                                           `photography_rules` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '摄影规则JSON',
                                           `acting_notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '表演指导JSON',
                                           `continuity_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '镜头连续性JSON：起始状态、结束状态、动作承接、空间锚点、在场人物',
                                           `source_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '原文片段',
                                           `image_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '图片生成提示词',
                                           `duration_seconds` int NOT NULL DEFAULT 15 COMMENT '时长秒数',
                                           `video_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '视频提示词',
                                           `video_url` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '视频地址',
                                           `video_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '视频生成任务ID',
                                           `video_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'pending' COMMENT '视频状态：pending/generating/done/failed',
                                           `last_frame_url` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '上一镜末帧URL（同场景连续镜头首帧承接用）',
                                           `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                                           `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                                           `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                                           `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                           `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户Id',
                                           PRIMARY KEY (`id`) USING BTREE,
                                           UNIQUE INDEX `uk_script_scene`(`script_id` ASC, `scene_no` ASC) USING BTREE,
                                           INDEX `idx_project_id`(`project_id` ASC) USING BTREE,
                                           INDEX `idx_script_id`(`script_id` ASC) USING BTREE,
                                           INDEX `idx_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '短剧分镜表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of short_drama_storyboard
-- ----------------------------
INSERT INTO `short_drama_storyboard` (`id`, `project_id`, `script_id`, `scene_no`, `scene_title`, `scene_text`, `scene_type`, `shot_type`, `camera_move`, `characters_json`, `location_name`, `photography_rules`, `acting_notes`, `continuity_json`, `source_text`, `image_prompt`, `duration_seconds`, `video_prompt`, `video_url`, `video_id`, `video_status`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `tenant_id`) VALUES (2077008643517599744, 2077007721974484992, 2077007722058371072, 1, '辞退邮件与触电穿越', '深夜办公室，电脑屏幕亮光映照50岁小明苍老疲惫的脸，他盯着屏幕上的辞退邮件，眼神失焦。他拿起手机看到离婚协议截图，手开始颤抖，手机滑落。他试图扶住桌子，手肘碰倒旁边电水壶，水洒向插座，火花迸溅，小明身体后仰倒在椅子上，画面渐黑。', 'suspense', '平视近景', '缓推至固定', '[{\"name\":\"小明\",\"appearance\":\"初始形象\",\"slot\":\"办公桌前\"}]', '现代办公室_夜', '{\"panel_number\":1,\"scene_summary\":\"深夜办公室，电脑屏幕亮光映照50岁小明苍老疲惫的脸，他盯着屏幕上的辞退邮件，眼神失焦。他拿起手机看到离婚协议截图，手开始颤抖，手机滑落。他试图扶住桌子，手肘碰倒旁边电水壶，水洒向插座，火花迸溅，小明身体后仰倒在椅子上，画面渐黑。\",\"lighting\":{\"direction\":\"根据场景主光方向保持连续\",\"quality\":\"低调硬光，保留阴影层次\"},\"characters\":[{\"name\":\"小明\",\"screen_position\":\"办公桌前\",\"posture\":\"保持符合当前动作的自然姿态\",\"facing\":\"面向动作目标或对话对象\"}],\"depth_of_field\":\"中浅景深，保留环境压迫感\",\"color_tone\":\"冷暗色调\"}', '[{\"name\":\"小明\",\"acting\":\"控制呼吸与眨眼频率，用迟疑、停顿和缓慢转头制造紧张感，按前段、中段、后段完成三个连续表演节拍\"}]', '{\"start_state\":\"新场景建立：现代办公室_夜，重新交代人物位置、朝向与环境\",\"end_state\":\"小明倒在椅子上，身体后仰，火花在插座处闪烁，画面渐黑\",\"continuity_action\":\"小明身体失去平衡向后倒，火花仍在闪烁\",\"spatial_anchor\":\"小明位于画面中央偏左，办公桌桌面杂乱的纸张和电脑屏幕占据右半，电水壶翻倒在桌角\",\"present_characters\":[\"小明\"],\"scene_number\":1,\"segment_number\":1,\"segment_goal\":\"建立小明被辞退后的绝望状态并触发穿越\",\"segment_result\":\"小明触电倒下，穿越启动\",\"bridge_in\":\"开场建立办公室环境与人物状态\",\"bridge_out\":\"火花和倒下的动作形成视觉钩子，画面切黑\",\"beat_type\":\"trigger\",\"narrative_cause\":\"小明收到辞退邮件和离婚协议，情绪崩溃导致意外\",\"character_goal\":\"缓解内心痛苦，但无意识动作导致触电\",\"story_action\":\"手抖放下手机，碰倒水壶，水溅到插座引发火花\",\"story_result\":\"小明触电昏迷，穿越时空\",\"next_hook\":\"下一镜头需要展示小明醒来后的新环境\"}', '办公室昏暗，只有电脑屏幕亮着。50岁的小明穿着皱巴巴的衬衫，盯着屏幕上的辞退邮件。他拿起手机，看到妻子发来的离婚协议截图。手抖着放下手机，碰倒桌上的电水壶，水洒在插座上。火花闪过，小明倒在椅子上。', '小明坐在深夜办公室的办公桌前，电脑屏幕蓝光照亮他苍老疲惫的脸，桌上散落纸张，他手中拿着手机看着离婚协议截图，旁边的电水壶摇摇欲坠，背景黑暗，气氛压抑。', 8, '深夜办公室，中年男子坐在办公桌前，电脑屏幕光映照脸庞，他盯着屏幕，眼神失焦。他拿起手机看向屏幕，手开始颤抖，手机滑落。他试图扶住桌子，手肘碰倒旁边的电水壶，水洒向插座，火花迸溅，他身体后仰倒在椅子上。镜头从平视中景缓缓推近至脸部特写，然后固定，画面渐黑。', 'https://atlas-media.oss-us-west-1.aliyuncs.com/videos/cgt-20260714203912-pshlw-0.mp4', '472c35ef433744c8a4cbd35a438ad35e', 'done', -1, -1, '2026-07-14 20:33:44', -1, '2026-07-14 20:41:42', 0);
INSERT INTO `short_drama_storyboard` (`id`, `project_id`, `script_id`, `scene_no`, `scene_title`, `scene_text`, `scene_type`, `shot_type`, `camera_move`, `characters_json`, `location_name`, `photography_rules`, `acting_notes`, `continuity_json`, `source_text`, `image_prompt`, `duration_seconds`, `video_prompt`, `video_url`, `video_id`, `video_status`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `tenant_id`) VALUES (2077008643727314944, 2077007721974484992, 2077007722058371072, 2, '重生1985', '小明躺在地上，睁开眼，阳光刺眼。他坐起身，低头看见自己穿着蓝色工装，手背皮肤光滑无皱纹。他摸自己的脸，感觉紧致年轻。旁边老式自行车铃响，一个年轻人骑车路过喊“小明，上班迟到了！”小明看向路口停着的二八自行车和墙上红色标语“时间就是金钱”，低头看手腕电子表显示1985.6，他深吸一口气，眼神从迷茫变成坚定，低语“再来一次”。', 'emotion', '平视近景转特写', '缓推加环绕', '[{\"name\":\"小明\",\"appearance\":\"初始形象\",\"slot\":\"地面中央\"}]', '小巷_日_1985', '{\"panel_number\":2,\"scene_summary\":\"小明躺在地上，睁开眼，阳光刺眼。他坐起身，低头看见自己穿着蓝色工装，手背皮肤光滑无皱纹。他摸自己的脸，感觉紧致年轻。旁边老式自行车铃响，一个年轻人骑车路过喊“小明，上班迟到了！”小明看向路口停着的二八自行车和墙上红色标语“时间就是金钱”，低头看手腕电子表显示1985.6，他深吸一口气，眼神从迷茫变成坚定，低语“再来一次”。\",\"lighting\":{\"direction\":\"根据场景主光方向保持连续\",\"quality\":\"柔和侧光，突出面部情绪\"},\"characters\":[{\"name\":\"小明\",\"screen_position\":\"地面中央\",\"posture\":\"保持符合当前动作的自然姿态\",\"facing\":\"面向动作目标或对话对象\"}],\"depth_of_field\":\"浅景深，突出角色表情\",\"color_tone\":\"克制柔和色调\"}', '[{\"name\":\"小明\",\"acting\":\"表情逐步变化，先克制呼吸，再通过眼神和细微肢体释放情绪，按前段、中段、后段完成三个连续表演节拍\"}]', '{\"start_state\":\"新场景建立：小巷_日_1985，重新交代人物位置、朝向与环境\",\"end_state\":\"小明站立，眼神坚定，低头看电子表，嘴唇微动说“再来一次”\",\"continuity_action\":\"小明视线从手表移向前方街道，准备行动\",\"spatial_anchor\":\"小明位于小巷中央地面，左侧墙上有红色标语，右侧有自行车和骑车年轻人，背景是老旧居民楼\",\"present_characters\":[\"小明\",\"年轻人（群众）\"],\"scene_number\":2,\"segment_number\":1,\"segment_goal\":\"展示小明穿越后醒来发现变成年轻模样\",\"segment_result\":\"小明确认自己回到1985年，眼神由迷茫变坚定\",\"bridge_in\":\"画面从黑场渐亮，小明面部特写从模糊到清晰\",\"bridge_out\":\"小明深吸一口气并说出内心决定，视线看向前方街道\",\"beat_type\":\"reaction\",\"narrative_cause\":\"小明触电昏迷，穿越时空\",\"character_goal\":\"确认自身状态和时代\",\"story_action\":\"坐起、摸脸、看周围、看电子表\",\"story_result\":\"小明确定自己重生，产生第二次人生的决心\",\"next_hook\":\"下一镜头展示小明带着方案前往街道办\"}', '小明醒来，发现自己躺在地上，身穿80年代蓝色工装。他摸着脸——没有皱纹。旁边老式自行车铃响，一个年轻人喊：“小明，上班迟到了！”小明看着路口的二八自行车，墙上的标语“时间就是金钱”，愣住。他低头看手腕上的电子表：1985年6月。他深吸一口气，眼神从迷茫变成坚定：“再来一次。”', '小明坐在小巷地面，身穿蓝色工装，阳光从巷口射入照亮他的脸，他摸着自己年轻的脸庞，眼神迷茫；背景是青砖墙和红色标语，一辆老式自行车靠在墙边。', 10, '年轻男子躺在地面上，阳光刺眼，他缓缓睁开眼，然后坐起身，低头看自己双手和蓝色工装，摸自己的脸，表情从迷茫到惊讶。旁边传来自行车铃声，他转头看向巷口的二八自行车和「时间就是金钱」标语，再低头看手腕上的电子表，眼神从迷茫变为坚定。镜头从全景缓慢推近至脸部特写，同时环绕拍摄，最后固定。', 'https://atlas-media.oss-us-west-1.aliyuncs.com/videos/cgt-20260714203913-lxtrb-0.mp4', 'b3b6843f0b78433893608675852797bd', 'done', -1, -1, '2026-07-14 20:33:44', -1, '2026-07-14 20:47:08', 0);
INSERT INTO `short_drama_storyboard` (`id`, `project_id`, `script_id`, `scene_no`, `scene_title`, `scene_text`, `scene_type`, `shot_type`, `camera_move`, `characters_json`, `location_name`, `photography_rules`, `acting_notes`, `continuity_json`, `source_text`, `image_prompt`, `duration_seconds`, `video_prompt`, `video_url`, `video_id`, `video_status`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `tenant_id`) VALUES (2077008643781840896, 2077007721974484992, 2077007722058371072, 3, '街道办受阻遇老陈', '街道办门口，小明拿着一叠手写方案快步走向大门。门卫伸出手臂挡住他。小明急切地解释“同志，我有商业计划，能帮工厂接全国订单！”门卫皱眉推他出去，纸张散落一地。小明蹲下慌乱捡纸。路对面一个穿中山装的中年男人（老陈）弯腰捡起一张，看着上面的字，抬头问：“你这上面写的‘电商’是啥？”小明抬头，眼睛一亮：“陈厂长，我能让您仓库里的积压货三天卖掉。”', 'daily', '平视中景', '轻微跟随', '[{\"name\":\"小明\",\"appearance\":\"初始形象\",\"slot\":\"门口台阶下\"},{\"name\":\"老陈\",\"appearance\":\"初始形象\",\"slot\":\"路对面人行道\"}]', '街道办门口_日', '{\"panel_number\":3,\"scene_summary\":\"街道办门口，小明拿着一叠手写方案快步走向大门。门卫伸出手臂挡住他。小明急切地解释“同志，我有商业计划，能帮工厂接全国订单！”门卫皱眉推他出去，纸张散落一地。小明蹲下慌乱捡纸。路对面一个穿中山装的中年男人（老陈）弯腰捡起一张，看着上面的字，抬头问：“你这上面写的‘电商’是啥？”小明抬头，眼睛一亮：“陈厂长，我能让您仓库里的积压货三天卖掉。”\",\"lighting\":{\"direction\":\"根据场景主光方向保持连续\",\"quality\":\"自然柔光，肤色与环境协调\"},\"characters\":[{\"name\":\"小明\",\"screen_position\":\"门口台阶下\",\"posture\":\"保持符合当前动作的自然姿态\",\"facing\":\"面向动作目标或对话对象\"},{\"name\":\"老陈\",\"screen_position\":\"路对面人行道\",\"posture\":\"保持符合当前动作的自然姿态\",\"facing\":\"面向动作目标或对话对象\"}],\"depth_of_field\":\"中等景深，兼顾角色与环境\",\"color_tone\":\"自然统一色调\"}', '[{\"name\":\"小明\",\"acting\":\"保持自然呼吸和目光交流，动作从起始状态平滑过渡到结束状态，按前段、中段、后段完成三个连续表演节拍\"},{\"name\":\"老陈\",\"acting\":\"保持自然呼吸和目光交流，动作从起始状态平滑过渡到结束状态，按前段、中段、后段完成三个连续表演节拍\"}]', '{\"start_state\":\"新场景建立：街道办门口_日，重新交代人物位置、朝向与环境\",\"end_state\":\"小明蹲在地上抬头看向老陈，老陈站在路对面手持一张纸，两人视线交汇\",\"continuity_action\":\"小明视线锁定老陈，准备进一步解释\",\"spatial_anchor\":\"街道办铁门在画面左侧，门卫站在台阶上；小明在台阶下中央；老陈在画面右侧路对面行道树旁\",\"present_characters\":[\"小明\",\"老陈\",\"门卫（群众）\"],\"scene_number\":3,\"segment_number\":1,\"segment_goal\":\"小明尝试进入街道办被门卫阻拦，吸引老陈注意\",\"segment_result\":\"老陈捡起散落的方案并开始询问\",\"bridge_in\":\"小明拿着方案走向街道办大门，眼神充满希望\",\"bridge_out\":\"老陈目光落在方案上，问题引发后续对话\",\"beat_type\":\"trigger\",\"narrative_cause\":\"小明确定自己重生，产生第二次人生的决心\",\"character_goal\":\"进入街道办寻求支持\",\"story_action\":\"小明上前解释，被推倒，捡纸时发现老陈在看方案\",\"story_result\":\"老陈对方案产生好奇并主动与小明对话\",\"next_hook\":\"下一镜头展示两人在废弃仓库深入讨论\"}', '小明拿着一叠手写方案，站在街道办门口。门卫拦住他。小明：“同志，我有商业计划，能帮工厂接全国订单！”门卫：“胡闹！”推他出去。小明捡起散落的纸，抬头看到路对面一个穿中山装的中年男人（老陈）正在看他的方案。老陈：“你这上面写的‘电商’是啥？”小明眼睛一亮：“陈厂长，我能让您仓库里的积压货三天卖掉。”', '小明站在街道办门口台阶下，手拿一叠方案，表情急切；门卫站在台阶上伸手阻拦；路对面老陈穿中山装低头看纸，阳光从右上方照射。', 8, '年轻男子拿着一叠手写方案快步走向街道办大门，门卫伸出手臂阻挡。年轻男子急切地解释，门卫皱眉推他出去，纸张散落。年轻男子蹲下慌乱捡纸。路对面一位中年男子弯腰捡起一张纸，看着上面，抬头问话。镜头平视跟随年轻男子移动，然后切换到越肩视角展示门卫的阻拦动作。', 'https://atlas-media.oss-us-west-1.aliyuncs.com/videos/cgt-20260714203920-6rw8b-0.mp4', '7173c12461574b8abd063c46fc625ebe', 'done', -1, -1, '2026-07-14 20:33:44', -1, '2026-07-14 20:47:09', 0);
INSERT INTO `short_drama_storyboard` (`id`, `project_id`, `script_id`, `scene_no`, `scene_title`, `scene_text`, `scene_type`, `shot_type`, `camera_move`, `characters_json`, `location_name`, `photography_rules`, `acting_notes`, `continuity_json`, `source_text`, `image_prompt`, `duration_seconds`, `video_prompt`, `video_url`, `video_id`, `video_status`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `tenant_id`) VALUES (2077008643823783936, 2077007721974484992, 2077007722058371072, 4, '仓库达成合作', '废弃仓库内，一张木桌两把椅子，小明和老陈对面而坐。桌上摆着两盒烟，老陈抽出一根点燃：“我信你一次，但怎么弄？”小明拿起笔在纸上画示意图：“用电话线连电脑，搞个库存信息库。我写程序。”老陈皱眉：“钱呢？”小明：“我先拉商户，您出设备。”老陈抖了抖烟灰，沉默几秒后点头。小明转头看向墙上的标语“工业学大庆”，低声：“这次一定要成。”', 'daily', '平视中景', '缓推', '[{\"name\":\"小明\",\"appearance\":\"初始形象\",\"slot\":\"桌子一侧\"},{\"name\":\"老陈\",\"appearance\":\"初始形象\",\"slot\":\"桌子另一侧\"}]', '废弃仓库_日', '{\"panel_number\":4,\"scene_summary\":\"废弃仓库内，一张木桌两把椅子，小明和老陈对面而坐。桌上摆着两盒烟，老陈抽出一根点燃：“我信你一次，但怎么弄？”小明拿起笔在纸上画示意图：“用电话线连电脑，搞个库存信息库。我写程序。”老陈皱眉：“钱呢？”小明：“我先拉商户，您出设备。”老陈抖了抖烟灰，沉默几秒后点头。小明转头看向墙上的标语“工业学大庆”，低声：“这次一定要成。”\",\"lighting\":{\"direction\":\"根据场景主光方向保持连续\",\"quality\":\"自然柔光，肤色与环境协调\"},\"characters\":[{\"name\":\"小明\",\"screen_position\":\"桌子一侧\",\"posture\":\"保持符合当前动作的自然姿态\",\"facing\":\"面向动作目标或对话对象\"},{\"name\":\"老陈\",\"screen_position\":\"桌子另一侧\",\"posture\":\"保持符合当前动作的自然姿态\",\"facing\":\"面向动作目标或对话对象\"}],\"depth_of_field\":\"中等景深，兼顾角色与环境\",\"color_tone\":\"自然统一色调\"}', '[{\"name\":\"小明\",\"acting\":\"保持自然呼吸和目光交流，动作从起始状态平滑过渡到结束状态，按前段、中段、后段完成三个连续表演节拍\"},{\"name\":\"老陈\",\"acting\":\"保持自然呼吸和目光交流，动作从起始状态平滑过渡到结束状态，按前段、中段、后段完成三个连续表演节拍\"}]', '{\"start_state\":\"新场景建立：废弃仓库_日，重新交代人物位置、朝向与环境\",\"end_state\":\"老陈点头，小明目光从墙上标语收回，表情坚定\",\"continuity_action\":\"小明视线从标语移回老陈，准备下一步计划\",\"spatial_anchor\":\"桌子位于仓库中央，小明坐画面左侧，老陈坐右侧，后面墙上有标语，水泥地面空旷\",\"present_characters\":[\"小明\",\"老陈\"],\"scene_number\":4,\"segment_number\":1,\"segment_goal\":\"小明与老陈在仓库达成合作意向\",\"segment_result\":\"老陈决定信任小明，提供设备支持\",\"bridge_in\":\"两人对面而坐，桌上两盒烟，老陈面露犹豫\",\"bridge_out\":\"老陈点头，小明看向墙上标语，低声坚定信念\",\"beat_type\":\"action\",\"narrative_cause\":\"老陈对方案产生好奇并主动与小明对话\",\"character_goal\":\"说服老陈提供设备支持\",\"story_action\":\"小明画图解释技术方案，提出资源分工\",\"story_result\":\"老陈同意合作，小明获得初步资源\",\"next_hook\":\"下一镜头展示工厂办公室内新进展与危机\"}', '老陈和小明对面坐着。桌上摆着两盒烟。老陈：“我信你一次，但怎么弄？”小明在纸上画：“用电话线连电脑，搞个库存信息库。我写程序。”老陈：“钱呢？”小明：“我先拉商户，您出设备。”老陈犹豫后点头。小明看着仓库墙上的“工业学大庆”，低声：“这次一定要成。”', '小明和老陈在废弃仓库的旧木桌对面而坐，桌上两盒烟和散落的纸张；老陈夹烟沉思，小明执笔画示意图；背景墙上红色大字「工业学大庆」，高窗透入光线。', 8, '年轻男子和中年男子对面坐在木桌两侧，桌上摆着两盒烟。中年男子抽出一根点燃，年轻男子拿起笔在纸上画图并说话。中年男子皱眉问话，年轻男子抬头回答，伸手指向纸面。中年男子抖烟灰后点头。年轻男子转头看向墙上的标语，低声自语。镜头从两人的中景缓缓推近至年轻男子的脸部，然后固定。', NULL, 'd4665ee140f34673914292855d389d38', 'generating', -1, -1, '2026-07-14 20:33:44', -1, '2026-07-14 20:39:20', 0);
INSERT INTO `short_drama_storyboard` (`id`, `project_id`, `script_id`, `scene_no`, `scene_title`, `scene_text`, `scene_type`, `shot_type`, `camera_move`, `characters_json`, `location_name`, `photography_rules`, `acting_notes`, `continuity_json`, `source_text`, `image_prompt`, `duration_seconds`, `video_prompt`, `video_url`, `video_id`, `video_status`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `tenant_id`) VALUES (2077008643874115584, 2077007721974484992, 2077007722058371072, 5, '工商检查危机', '工厂办公室内，木制办公桌上转盘电话响起。老陈拿起听筒，脸色逐渐变白，手指捏紧话筒。挂断后他转身对旁边椅子上的小明说：“国营百货举报我们投机倒把，工商明天来查。”小明快速翻看桌上的账本，抬起头：“我们走的是信息服务，不是倒卖。”老陈叹气：“没人认可这个。”小明合上账本，直视老陈：“明天我来说。”', 'suspense', '平视近景', '缓推', '[{\"name\":\"老陈\",\"appearance\":\"初始形象\",\"slot\":\"办公桌后\"},{\"name\":\"小明\",\"appearance\":\"初始形象\",\"slot\":\"办公桌旁椅子\"}]', '工厂办公室_日', '{\"panel_number\":5,\"scene_summary\":\"工厂办公室内，木制办公桌上转盘电话响起。老陈拿起听筒，脸色逐渐变白，手指捏紧话筒。挂断后他转身对旁边椅子上的小明说：“国营百货举报我们投机倒把，工商明天来查。”小明快速翻看桌上的账本，抬起头：“我们走的是信息服务，不是倒卖。”老陈叹气：“没人认可这个。”小明合上账本，直视老陈：“明天我来说。”\",\"lighting\":{\"direction\":\"根据场景主光方向保持连续\",\"quality\":\"低调硬光，保留阴影层次\"},\"characters\":[{\"name\":\"老陈\",\"screen_position\":\"办公桌后\",\"posture\":\"保持符合当前动作的自然姿态\",\"facing\":\"面向动作目标或对话对象\"},{\"name\":\"小明\",\"screen_position\":\"办公桌旁椅子\",\"posture\":\"保持符合当前动作的自然姿态\",\"facing\":\"面向动作目标或对话对象\"}],\"depth_of_field\":\"中浅景深，保留环境压迫感\",\"color_tone\":\"冷暗色调\"}', '[{\"name\":\"老陈\",\"acting\":\"控制呼吸与眨眼频率，用迟疑、停顿和缓慢转头制造紧张感\"},{\"name\":\"小明\",\"acting\":\"控制呼吸与眨眼频率，用迟疑、停顿和缓慢转头制造紧张感\"}]', '{\"start_state\":\"新场景建立：工厂办公室_日，重新交代人物位置、朝向与环境\",\"end_state\":\"小明合上账本，直视老陈，表情自信；老陈眉头紧锁但不再反驳\",\"continuity_action\":\"小明视线坚定，老陈目光仍带忧虑但转向信任\",\"spatial_anchor\":\"老陈位于画面左侧办公桌后，小明在右侧椅子，窗外可见厂区烟囱，文件柜靠墙\",\"present_characters\":[\"小明\",\"老陈\"],\"scene_number\":5,\"segment_number\":1,\"segment_goal\":\"工厂办公室内老陈接到工商查处的电话，小明提出应对\",\"segment_result\":\"小明决定亲自去面对工商检查\",\"bridge_in\":\"办公室电话铃响打破平静，老陈接听表情变化\",\"bridge_out\":\"老陈看向小明，小明坚定地说“明天我来说”\",\"beat_type\":\"trigger\",\"narrative_cause\":\"老陈同意合作，小明获得初步资源\",\"character_goal\":\"让老陈相信自己能解决危机\",\"story_action\":\"小明解释模式合法性，并主动承担交涉责任\",\"story_result\":\"老陈部分安心，小明获得应对危机的授权\",\"next_hook\":\"下一镜头展示出租屋内团队内部争吵与统一思想\"}', '办公桌上电话响起。老陈接起，脸色变白。挂断后对小明：“国营百货举报我们投机倒把，工商明天来查。”小明快速翻看账本：“我们走的是信息服务，不是倒卖。”老陈：“没人认可这个。”小明：“明天我来说。”', '工厂办公室内，老陈坐在办公桌后接电话，脸色凝重；小明坐在侧面椅子上翻看账本，窗外可见工厂烟囱；桌上转盘电话和文件散落。', 7, '办公桌上黑色转盘电话响起。中年男子坐在桌后拿起听筒，脸色逐渐变白，手指捏紧话筒。他挂断后转身对旁边的年轻男子说话。年轻男子快速翻看账本，抬起头直视对方回话，然后合上账本。镜头从电话特写缓慢推近至中年男子面部，再缓拉到两人中景。', 'https://atlas-media.oss-us-west-1.aliyuncs.com/videos/cgt-20260714203917-gnb2m-0.mp4', '61edb37e979a426f9e8272786243c243', 'done', -1, -1, '2026-07-14 20:33:44', -1, '2026-07-14 20:41:08', 0);
INSERT INTO `short_drama_storyboard` (`id`, `project_id`, `script_id`, `scene_no`, `scene_title`, `scene_text`, `scene_type`, `shot_type`, `camera_move`, `characters_json`, `location_name`, `photography_rules`, `acting_notes`, `continuity_json`, `source_text`, `image_prompt`, `duration_seconds`, `video_prompt`, `video_url`, `video_id`, `video_status`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `tenant_id`) VALUES (2077008643920252928, 2077007721974484992, 2077007722058371072, 6, '出租屋统一思想', '狭小出租屋，灯光昏暗。两个年轻团队成员拍桌站起：“不干了，风险太大！”老陈坐在床边沉默，低头看地面。小明从墙角拿起一块黑板放在桌上，画出一条时间轴，标注1985年，指向电脑图标。他转头对大家说：“1985年，电脑刚进中国，我们的优势是信息差。不做商品买卖，做平台——帮工厂和供销社匹配库存。”他指着黑板：“明天工商来，我就说这是‘技术咨询’。”老陈抬头，皱眉问：“你哪来的这些？”小明停顿一下：“梦里学的。”', 'emotion', '平视近景转特写', '缓推加环绕', '[{\"name\":\"小明\",\"appearance\":\"初始形象\",\"slot\":\"桌边站立\"},{\"name\":\"老陈\",\"appearance\":\"初始形象\",\"slot\":\"床边坐着\"}]', '出租屋_夜', '{\"panel_number\":6,\"scene_summary\":\"狭小出租屋，灯光昏暗。两个年轻团队成员拍桌站起：“不干了，风险太大！”老陈坐在床边沉默，低头看地面。小明从墙角拿起一块黑板放在桌上，画出一条时间轴，标注1985年，指向电脑图标。他转头对大家说：“1985年，电脑刚进中国，我们的优势是信息差。不做商品买卖，做平台——帮工厂和供销社匹配库存。”他指着黑板：“明天工商来，我就说这是‘技术咨询’。”老陈抬头，皱眉问：“你哪来的这些？”小明停顿一下：“梦里学的。”\",\"lighting\":{\"direction\":\"根据场景主光方向保持连续\",\"quality\":\"柔和侧光，突出面部情绪\"},\"characters\":[{\"name\":\"小明\",\"screen_position\":\"桌边站立\",\"posture\":\"保持符合当前动作的自然姿态\",\"facing\":\"面向动作目标或对话对象\"},{\"name\":\"老陈\",\"screen_position\":\"床边坐着\",\"posture\":\"保持符合当前动作的自然姿态\",\"facing\":\"面向动作目标或对话对象\"}],\"depth_of_field\":\"浅景深，突出角色表情\",\"color_tone\":\"克制柔和色调\"}', '[{\"name\":\"小明\",\"acting\":\"表情逐步变化，先克制呼吸，再通过眼神和细微肢体释放情绪，按前段、中段、后段完成三个连续表演节拍\"},{\"name\":\"老陈\",\"acting\":\"表情逐步变化，先克制呼吸，再通过眼神和细微肢体释放情绪，按前段、中段、后段完成三个连续表演节拍\"}]', '{\"start_state\":\"新场景建立：出租屋_夜，重新交代人物位置、朝向与环境\",\"end_state\":\"老陈抬头看向小明，两个年轻人停止争吵望向黑板，小黑板上画有时间轴和“电脑”字样\",\"continuity_action\":\"小明手指还指着黑板上的“技术咨询”，等待异议\",\"spatial_anchor\":\"画面中央是一张桌子，黑板靠在桌上，小明站在桌子左侧画黑板，老陈坐在右侧床边，两个年轻人站在桌子远端\",\"present_characters\":[\"小明\",\"老陈\",\"两个年轻人（群众）\"],\"scene_number\":6,\"segment_number\":1,\"segment_goal\":\"出租屋内团队成员因风险而退缩，小明用时间轴规划说服大家\",\"segment_result\":\"老陈和小明达成共识，团队重拾信心\",\"bridge_in\":\"出租屋内两个年轻人拍桌起身，气氛紧张\",\"bridge_out\":\"老陈抬头看小明，眼神中带着疑惑与信任\",\"beat_type\":\"decision\",\"narrative_cause\":\"老陈部分安心，小明获得应对危机的授权\",\"character_goal\":\"稳定团队，统一认知\",\"story_action\":\"小明画出时间轴，解释信息差优势和应对策略\",\"story_result\":\"团队情绪稳定，老陈虽疑惑但接受解释\",\"next_hook\":\"下一镜头展示百货大楼门口工商检查的结果\"}', '团队成员（两个年轻人）拍桌：“不干了，风险太大！”老陈沉默。小明拿出一块黑板，画出时间轴：“1985年，电脑刚进中国，我们的优势是信息差。不做商品买卖，做平台——帮工厂和供销社匹配库存。”他指着黑板：“明天工商来，我就说这是‘技术咨询’。”老陈抬头：“你哪来的这些？”小明：“梦里学的。”', '出租屋内，小明站在桌子左侧画黑板，黑板上粉笔写着时间轴和「电脑」字样；老陈坐在床边抬头看；两个年轻人站在桌子远端，表情从愤怒转为倾听；昏黄灯光下气氛紧张。', 10, '狭小出租屋内，两个年轻人拍桌站起情绪激动。中年男子坐在床边低头沉默。年轻男子从墙角拿起一块黑板放在桌上，用粉笔画出一条时间轴，标注1985和电脑图标，转头对大家说话并指向黑板。中年男子抬起头皱眉问话，年轻男子停顿后回答。镜头从全景缓慢推近至年轻男子面部特写，然后环绕拍摄黑板上的时间轴。', NULL, NULL, 'pending', -1, -1, '2026-07-14 20:33:44', -1, '2026-07-14 20:33:44', 0);
INSERT INTO `short_drama_storyboard` (`id`, `project_id`, `script_id`, `scene_no`, `scene_title`, `scene_text`, `scene_type`, `shot_type`, `camera_move`, `characters_json`, `location_name`, `photography_rules`, `acting_notes`, `continuity_json`, `source_text`, `image_prompt`, `duration_seconds`, `video_prompt`, `video_url`, `video_id`, `video_status`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `tenant_id`) VALUES (2077008643978973184, 2077007721974484992, 2077007722058371072, 7, '工商检查通过', '百货大楼门口台阶上，工商人员和穿藏青色西装的中年经理站在一起。小明从公文包拿出一叠表格递过去：“这是通过我们信息服务成交的订单，全部有记录，不碰资金，只收咨询费。”工商人员翻看后递给旁边的百货经理。经理推了推眼镜细看，低声问小明：“这方法能帮我们清库存？”小明点头：“三天见效。”经理转向工商人员点了点头。工商人员收起表格：“先观察。”', 'daily', '平视中景', '轻微跟随', '[{\"name\":\"小明\",\"appearance\":\"初始形象\",\"slot\":\"台阶上右侧\"},{\"name\":\"老陈\",\"appearance\":\"初始形象\",\"slot\":\"台阶上小明身后\"},{\"name\":\"百货经理\",\"appearance\":\"初始形象\",\"slot\":\"台阶上中央\"}]', '百货大楼门口_日', '{\"panel_number\":7,\"scene_summary\":\"百货大楼门口台阶上，工商人员和穿藏青色西装的中年经理站在一起。小明从公文包拿出一叠表格递过去：“这是通过我们信息服务成交的订单，全部有记录，不碰资金，只收咨询费。”工商人员翻看后递给旁边的百货经理。经理推了推眼镜细看，低声问小明：“这方法能帮我们清库存？”小明点头：“三天见效。”经理转向工商人员点了点头。工商人员收起表格：“先观察。”\",\"lighting\":{\"direction\":\"根据场景主光方向保持连续\",\"quality\":\"自然柔光，肤色与环境协调\"},\"characters\":[{\"name\":\"小明\",\"screen_position\":\"台阶上右侧\",\"posture\":\"保持符合当前动作的自然姿态\",\"facing\":\"面向动作目标或对话对象\"},{\"name\":\"老陈\",\"screen_position\":\"台阶上小明身后\",\"posture\":\"保持符合当前动作的自然姿态\",\"facing\":\"面向动作目标或对话对象\"},{\"name\":\"百货经理\",\"screen_position\":\"台阶上中央\",\"posture\":\"保持符合当前动作的自然姿态\",\"facing\":\"面向动作目标或对话对象\"}],\"depth_of_field\":\"中等景深，兼顾角色与环境\",\"color_tone\":\"自然统一色调\"}', '[{\"name\":\"小明\",\"acting\":\"保持自然呼吸和目光交流，动作从起始状态平滑过渡到结束状态，按前段、中段、后段完成三个连续表演节拍\"},{\"name\":\"老陈\",\"acting\":\"保持自然呼吸和目光交流，动作从起始状态平滑过渡到结束状态，按前段、中段、后段完成三个连续表演节拍\"},{\"name\":\"百货经理\",\"acting\":\"保持自然呼吸和目光交流，动作从起始状态平滑过渡到结束状态，按前段、中段、后段完成三个连续表演节拍\"}]', '{\"start_state\":\"新场景建立：百货大楼门口_日，重新交代人物位置、朝向与环境\",\"end_state\":\"工商人员收起表格，经理向小明点头，小明露出微笑\",\"continuity_action\":\"小明视线跟随工商人员离开，准备下一步行动\",\"spatial_anchor\":\"百货大楼正门台阶为背景，工商人员在中央偏左，经理在左侧，小明在右侧，老陈在小明后方半步\",\"present_characters\":[\"小明\",\"老陈\",\"百货经理\",\"工商人员（群众）\"],\"scene_number\":7,\"segment_number\":1,\"segment_goal\":\"工商检查现场，小明用订单记录证明信息服务合法性，获得百货经理认可\",\"segment_result\":\"工商同意观察，项目得以继续\",\"bridge_in\":\"百货大楼门口，工商人员和小明、老陈站立，气氛凝重\",\"bridge_out\":\"工商人员收起表格，经理点头，危机解除\",\"beat_type\":\"result\",\"narrative_cause\":\"团队情绪稳定，老陈虽疑惑但接受解释\",\"character_goal\":\"用事实凭证打消工商疑虑\",\"story_action\":\"小明出示订单记录并解释运营模式\",\"story_result\":\"工商暂缓处理，项目获得官方默许\",\"next_hook\":\"下一镜头展示庆功礼堂的成果与小明的情感时刻\"}', '工商人员和小明、老陈站在门口。小明拿出一份表格：“这是通过我们信息服务成交的订单，全部有记录，不碰资金，只收咨询费。”工商人员翻看，递给旁边一个戴眼镜的中年人（百货公司经理）。经理看了后低声：“这方法能帮我们清库存？”小明：“三天见效。”经理点头。工商人员收起表格：“先观察。”', '小明站在百货大楼门口台阶上，从公文包中拿出表格递给工商人员；老陈站在他身后；旁边穿藏青色西装的百货经理低头查看表格；阳光照亮大楼立面。', 8, '百货大楼门口台阶上，工商人员和穿西装的中年男子站在一起。年轻男子从公文包拿出一叠表格递出并说话。工商人员翻看后递给旁边的经理。经理推了推眼镜细看，低声问年轻男子，年轻男子点头回答。经理转向工商人员点头。工商人员收起表格。镜头平视跟随年轻男子的动作，使用越肩视角展示表格内容。', NULL, NULL, 'pending', -1, -1, '2026-07-14 20:33:44', -1, '2026-07-14 20:33:44', 0);
INSERT INTO `short_drama_storyboard` (`id`, `project_id`, `script_id`, `scene_no`, `scene_title`, `scene_text`, `scene_type`, `shot_type`, `camera_move`, `characters_json`, `location_name`, `photography_rules`, `acting_notes`, `continuity_json`, `source_text`, `image_prompt`, `duration_seconds`, `video_prompt`, `video_url`, `video_id`, `video_status`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `tenant_id`) VALUES (2077008644041887744, 2077007721974484992, 2077007722058371072, 8, '厂庆宣言互联网愿景', '厂庆礼堂，红灯笼和横幅装饰，舞台上麦克风前，老陈对着台下说：“感谢小明的信息平台，咱们工厂活了。”台下工人鼓掌。小明站在侧台阴影里，手伸进口袋摸到发黄的现代身份证，他看着灯光下兴奋的面孔，默默将身份证撕碎。主持人喊：“请小明同志讲话！”小明走上舞台中央，拿起话筒，停顿三秒：“各位，未来三十年，互联网会改变一切。今天，我们从这里开始。”台下掌声雷动。', 'epic', '大远景转特写', '俯拍后缓降推近', '[{\"name\":\"小明\",\"appearance\":\"初始形象\",\"slot\":\"舞台上中央\"},{\"name\":\"老陈\",\"appearance\":\"初始形象\",\"slot\":\"舞台侧边\"}]', '厂庆礼堂_夜', '{\"panel_number\":8,\"scene_summary\":\"厂庆礼堂，红灯笼和横幅装饰，舞台上麦克风前，老陈对着台下说：“感谢小明的信息平台，咱们工厂活了。”台下工人鼓掌。小明站在侧台阴影里，手伸进口袋摸到发黄的现代身份证，他看着灯光下兴奋的面孔，默默将身份证撕碎。主持人喊：“请小明同志讲话！”小明走上舞台中央，拿起话筒，停顿三秒：“各位，未来三十年，互联网会改变一切。今天，我们从这里开始。”台下掌声雷动。\",\"lighting\":{\"direction\":\"根据场景主光方向保持连续\",\"quality\":\"大范围自然光，突出空间规模\"},\"characters\":[{\"name\":\"小明\",\"screen_position\":\"舞台上中央\",\"posture\":\"保持符合当前动作的自然姿态\",\"facing\":\"面向动作目标或对话对象\"},{\"name\":\"老陈\",\"screen_position\":\"舞台侧边\",\"posture\":\"保持符合当前动作的自然姿态\",\"facing\":\"面向动作目标或对话对象\"}],\"depth_of_field\":\"深景深，清晰展现环境规模\",\"color_tone\":\"宏大通透色调\"}', '[{\"name\":\"小明\",\"acting\":\"动作沉稳有力量，视线跟随环境或对手变化，保持画面张力，按前段、中段、后段完成三个连续表演节拍\"},{\"name\":\"老陈\",\"acting\":\"动作沉稳有力量，视线跟随环境或对手变化，保持画面张力，按前段、中段、后段完成三个连续表演节拍\"}]', '{\"start_state\":\"新场景建立：厂庆礼堂_夜，重新交代人物位置、朝向与环境\",\"end_state\":\"小明站在舞台中央举起话筒，台下工人站立鼓掌，老陈在侧台微笑\",\"continuity_action\":\"小明视线扫过全场，准备下台开始新阶段\",\"spatial_anchor\":\"舞台在画面中央靠上，台下长凳坐满工人，小明位于舞台中央，老陈在舞台左侧靠边\",\"present_characters\":[\"小明\",\"老陈\",\"工人（群众）\",\"主持人（群众）\"],\"scene_number\":8,\"segment_number\":1,\"segment_goal\":\"厂庆礼堂中老陈表扬小明，小明撕碎现代身份证并发表讲话\",\"segment_result\":\"小明公开宣告互联网未来愿景，全体工人热烈鼓掌\",\"bridge_in\":\"礼堂内工人坐满长凳，老陈在台上讲话\",\"bridge_out\":\"掌声雷动中，小明眼神坚定望向未来\",\"beat_type\":\"action\",\"narrative_cause\":\"工商暂缓处理，项目获得官方默许\",\"character_goal\":\"激励团队并宣告新方向\",\"story_action\":\"小明撕碎现代身份证，上台演讲\",\"story_result\":\"全体工人被鼓舞，小明确立新一代领导地位\",\"next_hook\":\"下一镜头展示清晨新挂牌和下一步计划\"}', '工人坐满长凳。老陈上台：“感谢小明的信息平台，咱们工厂活了。”台下鼓掌。小明站在侧台，看着灯光下兴奋的面孔。他手摸到口袋里的现代身份证——早已发黄。他默默撕碎。主持人：“请小明同志讲话！”小明上前，拿着话筒停顿三秒：“各位，未来三十年，互联网会改变一切。今天，我们从这里开始。”台下掌声雷动。', '厂庆礼堂内，舞台中央老陈握着话筒讲话，台下工人鼓掌；小明站在侧幕阴影中，手中捏着撕碎的身份证，灯光照亮舞台，红灯笼和横幅装饰，气氛热烈。', 10, '礼堂内工人坐满长凳，舞台中央中年男子在讲话，台下鼓掌。年轻男子站在侧台阴影中，手伸进口袋掏出一张发黄的身份证，默默撕碎。主持人喊话，年轻男子走上舞台中央，拿起话筒停顿三秒后说话，台下掌声雷动。镜头从礼堂大远景俯拍开始，然后缓缓下降并推近至年轻男子面部特写。', NULL, NULL, 'pending', -1, -1, '2026-07-14 20:33:44', -1, '2026-07-14 20:33:44', 0);
INSERT INTO `short_drama_storyboard` (`id`, `project_id`, `script_id`, `scene_no`, `scene_title`, `scene_text`, `scene_type`, `shot_type`, `camera_move`, `characters_json`, `location_name`, `photography_rules`, `acting_notes`, `continuity_json`, `source_text`, `image_prompt`, `duration_seconds`, `video_prompt`, `video_url`, `video_id`, `video_status`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `tenant_id`) VALUES (2077008644113190912, 2077007721974484992, 2077007722058371072, 9, '新征程启程', '清晨，阳光照在工厂大门新挂牌上，金边字体“华夏信息服务中心”。小明推着自行车走出来，老陈从后面追上：“下一步？”小明跨上车：“跑遍全国，建数据库。”老陈笑着摇头：“疯了。”小明回以笑容：“1985年，疯的人才能赢。”他蹬车远去，街道两旁的工厂烟囱开始冒烟，晨光中骑车背影越来越小。', 'epic', '大远景', '缓拉升起', '[{\"name\":\"小明\",\"appearance\":\"初始形象\",\"slot\":\"工厂大门前\"},{\"name\":\"老陈\",\"appearance\":\"初始形象\",\"slot\":\"门口台阶上\"}]', '工厂大门_晨', '{\"panel_number\":9,\"scene_summary\":\"清晨，阳光照在工厂大门新挂牌上，金边字体“华夏信息服务中心”。小明推着自行车走出来，老陈从后面追上：“下一步？”小明跨上车：“跑遍全国，建数据库。”老陈笑着摇头：“疯了。”小明回以笑容：“1985年，疯的人才能赢。”他蹬车远去，街道两旁的工厂烟囱开始冒烟，晨光中骑车背影越来越小。\",\"lighting\":{\"direction\":\"根据场景主光方向保持连续\",\"quality\":\"大范围自然光，突出空间规模\"},\"characters\":[{\"name\":\"小明\",\"screen_position\":\"工厂大门前\",\"posture\":\"保持符合当前动作的自然姿态\",\"facing\":\"面向动作目标或对话对象\"},{\"name\":\"老陈\",\"screen_position\":\"门口台阶上\",\"posture\":\"保持符合当前动作的自然姿态\",\"facing\":\"面向动作目标或对话对象\"}],\"depth_of_field\":\"深景深，清晰展现环境规模\",\"color_tone\":\"宏大通透色调\"}', '[{\"name\":\"小明\",\"acting\":\"动作沉稳有力量，视线跟随环境或对手变化，保持画面张力，按前段、中段、后段完成三个连续表演节拍\"},{\"name\":\"老陈\",\"acting\":\"动作沉稳有力量，视线跟随环境或对手变化，保持画面张力，按前段、中段、后段完成三个连续表演节拍\"}]', '{\"start_state\":\"新场景建立：工厂大门_晨，重新交代人物位置、朝向与环境\",\"end_state\":\"小明骑车远去的背影在街道尽头，烟囱冒烟，晨光笼罩\",\"continuity_action\":\"小明继续蹬车，老陈目送，街道向前延伸\",\"spatial_anchor\":\"工厂大门在画面左侧，新挂牌醒目；小明骑车向右远去，街道两旁有工厂围墙和烟囱；老陈站在门口台阶上\",\"present_characters\":[\"小明\",\"老陈\"],\"scene_number\":9,\"segment_number\":1,\"segment_goal\":\"新挂牌的清晨，小明与老陈对话后骑车远去，象征新征程\",\"segment_result\":\"小明骑车远去，烟囱冒烟，新的一天开始\",\"bridge_in\":\"阳光照在新挂牌上，小明推车出现\",\"bridge_out\":\"镜头定格在远去的背影和工厂烟囱，留下开放节奏\",\"beat_type\":\"result\",\"narrative_cause\":\"全体工人被鼓舞，小明确立新一代领导地位\",\"character_goal\":\"确定下一阶段行动计划\",\"story_action\":\"小明跨上自行车说出全国数据库计划\",\"story_result\":\"小明开始实际行动，留下奋斗的背影\",\"next_hook\":\"故事结束，无下一镜头\"}', '阳光照在新挂牌上：“华夏信息服务中心”。小明推着自行车出来，老陈追上：“下一步？”小明：“跑遍全国，建数据库。”老陈笑：“疯了。”小明跨上车：“1985年，疯的人才能赢。”他蹬车远去，街道两旁的工厂烟囱开始冒烟。', '清晨阳光照射工厂大门新挂牌「华夏信息服务中心」，小明推自行车站在门口，老陈站在台阶上微笑；远处烟囱冒烟，天空泛鱼肚白，街道延伸向远方。', 10, '清晨阳光照在工厂大门新挂牌上，金色字体「华夏信息服务中心」。年轻男子推自行车从门内走出，中年男子从后面追上，两人对话。年轻男子跨上自行车，蹬车远去，街道两旁的工厂烟囱开始冒烟，晨光中骑车背影越来越小。镜头从新挂牌特写缓缓拉远并升起，展现工厂全貌和远去的背影。', NULL, NULL, 'pending', -1, -1, '2026-07-14 20:33:44', -1, '2026-07-14 20:33:44', 0);

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '锁定表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '组配置' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '任务信息' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '任务执行器信息' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '调度日志' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'DashBoard_Job' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '任务实例' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '任务批次' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '命名空间' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通知配置' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '告警通知接收人' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '重试信息表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '死信队列表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '场景配置' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'DashBoard_Retry' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '重试任务表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '任务调度日志信息记录表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '服务器节点' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统用户表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统用户权限表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工作流' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工作流节点' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工作流批次' ROW_FORMAT = DYNAMIC;

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
INSERT INTO `sys_dict_data` VALUES (2026642472673288194, '000000', 0, '对话', 'chat', 'chat_model_category', NULL, 'cyan', 'N', 103, 1, '2026-02-25 20:56:33', 1, '2026-02-25 21:01:42', NULL);
INSERT INTO `sys_dict_data` VALUES (2026642525081116674, '000000', 1, '图像', 'image', 'chat_model_category', NULL, 'success', 'N', 103, 1, '2026-02-25 20:56:46', 1, '2026-02-25 21:01:37', NULL);
INSERT INTO `sys_dict_data` VALUES (2026643983713247233, '000000', 1, '次数计费', '1', 'sys_model_billing', NULL, 'green', 'N', 103, 1, '2026-02-25 21:02:34', 1, '2026-02-25 21:02:56', NULL);
INSERT INTO `sys_dict_data` VALUES (2026644058522853378, '000000', 2, 'token计费', '2', 'sys_model_billing', NULL, 'primary', 'N', 103, 1, '2026-02-25 21:02:51', 1, '2026-02-25 21:02:51', NULL);
INSERT INTO `sys_dict_data` VALUES (2027261114955931650, '000000', 2, '向量', 'vector', 'chat_model_category', NULL, 'default', 'N', 103, 1, '2026-02-27 13:54:49', 1, '2026-02-27 13:54:54', NULL);
INSERT INTO `sys_dict_data` VALUES (2045070879435259905, '000000', 4, '重排序', 'rerank', 'chat_model_category', NULL, '#000000', 'N', 103, 1, '2026-04-17 17:24:28', 1, '2026-04-19 01:02:20', '重排序模型');

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
INSERT INTO `sys_dict_type` VALUES (2026642112982360066, '000000', '模型分类', 'chat_model_category', 103, 1, '2026-02-25 20:55:08', 1, '2026-02-25 20:55:08', '模型分类');
INSERT INTO `sys_dict_type` VALUES (2026642183606050817, '000000', '计费方式', 'sys_model_billing', 103, 1, '2026-02-25 20:55:24', 1, '2026-02-25 20:55:24', '计费方式');

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
INSERT INTO `sys_logininfor` VALUES (2067087010153709570, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-06-17 11:28:42');
INSERT INTO `sys_logininfor` VALUES (2067120324839292929, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-06-17 13:41:05');
INSERT INTO `sys_logininfor` VALUES (2067178210017693697, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-06-17 17:31:06');
INSERT INTO `sys_logininfor` VALUES (2067439136780369921, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-06-18 10:47:56');
INSERT INTO `sys_logininfor` VALUES (2067509245943230465, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-06-18 15:26:31');
INSERT INTO `sys_logininfor` VALUES (2067515184486297601, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-06-18 15:50:07');
INSERT INTO `sys_logininfor` VALUES (2067536325229756417, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-06-18 17:14:08');
INSERT INTO `sys_logininfor` VALUES (2067599466735149057, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-06-18 21:25:02');
INSERT INTO `sys_logininfor` VALUES (2068942142755569665, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-06-22 14:20:21');
INSERT INTO `sys_logininfor` VALUES (2068963471252791297, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-06-22 15:45:06');
INSERT INTO `sys_logininfor` VALUES (2068986934684389377, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-06-22 17:18:20');
INSERT INTO `sys_logininfor` VALUES (2069012061732057090, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '1', '密码输入错误1次', '2026-06-22 18:58:11');
INSERT INTO `sys_logininfor` VALUES (2069012086394564609, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-06-22 18:58:16');
INSERT INTO `sys_logininfor` VALUES (2069034797917339649, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-06-22 20:28:31');
INSERT INTO `sys_logininfor` VALUES (2075512593476263937, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-07-10 17:28:58');
INSERT INTO `sys_logininfor` VALUES (2075536378350891010, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-07-10 19:03:29');
INSERT INTO `sys_logininfor` VALUES (2075536785592643586, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-07-10 19:05:06');
INSERT INTO `sys_logininfor` VALUES (2075539196780572674, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-07-10 19:14:41');
INSERT INTO `sys_logininfor` VALUES (2075539270906507265, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-07-10 19:14:59');
INSERT INTO `sys_logininfor` VALUES (2075544342642126850, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-07-10 19:35:08');
INSERT INTO `sys_logininfor` VALUES (2075603310492020737, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-07-10 23:29:27');
INSERT INTO `sys_logininfor` VALUES (2075608688223178753, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-07-10 23:50:49');
INSERT INTO `sys_logininfor` VALUES (2076851040791674882, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-07-14 10:07:29');
INSERT INTO `sys_logininfor` VALUES (2076867711472353281, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-07-14 11:13:43');
INSERT INTO `sys_logininfor` VALUES (2076916180597719042, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-07-14 14:26:19');
INSERT INTO `sys_logininfor` VALUES (2076935225945997313, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-07-14 15:42:00');
INSERT INTO `sys_logininfor` VALUES (2076944166545375233, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-07-14 16:17:32');
INSERT INTO `sys_logininfor` VALUES (2076955097467768833, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-07-14 17:00:58');
INSERT INTO `sys_logininfor` VALUES (2076972033710804993, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-07-14 18:08:16');
INSERT INTO `sys_logininfor` VALUES (2077003517935505409, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-07-14 20:13:22');
INSERT INTO `sys_logininfor` VALUES (2077008039416188930, '000000', 'admin', 'pc', 'pc', '127.0.0.1', '内网IP', 'MSEdge', 'Windows 10 or Windows Server 2016', '0', '登录成功', '2026-07-14 20:31:20');

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
INSERT INTO `sys_menu` VALUES (6, '租户管理', 0, 8, 'tenant', '', '', 1, 0, 'M', '0', '0', '', 'ph:users-light', 103, 1, '2025-12-14 16:11:49', 1, '2026-02-25 20:42:14', '租户管理目录');
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
INSERT INTO `sys_menu` VALUES (2000, 'MCP管理', 0, 2, 'mcp', '', '', 1, 0, 'M', '0', '0', '', 'mdi:robot-industrial', 103, 1, '2026-02-24 20:02:47', 1, '2026-02-25 20:41:54', 'MCP模块管理菜单');
INSERT INTO `sys_menu` VALUES (2001, 'MCP工具管理', 2000, 1, 'tool', 'mcp/tool/index', '', 1, 0, 'C', '0', '0', 'mcp:tool:list', 'octicon:tools-24', 103, 1, '2026-02-24 20:02:47', 1, '2026-02-25 20:41:27', 'MCP工具管理菜单');
INSERT INTO `sys_menu` VALUES (2002, 'MCP工具查询', 2001, 1, '#', '', '', 1, 0, 'F', '0', '0', 'mcp:tool:query', '#', 103, 1, '2026-02-24 20:02:47', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2003, 'MCP工具新增', 2001, 2, '#', '', '', 1, 0, 'F', '0', '0', 'mcp:tool:add', '#', 103, 1, '2026-02-24 20:02:47', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2004, 'MCP工具修改', 2001, 3, '#', '', '', 1, 0, 'F', '0', '0', 'mcp:tool:edit', '#', 103, 1, '2026-02-24 20:02:47', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2005, 'MCP工具删除', 2001, 4, '#', '', '', 1, 0, 'F', '0', '0', 'mcp:tool:remove', '#', 103, 1, '2026-02-24 20:02:47', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2006, 'MCP工具测试', 2001, 5, '#', '', '', 1, 0, 'F', '0', '0', 'mcp:tool:test', '#', 103, 1, '2026-02-24 20:02:47', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2007, 'MCP工具导出', 2001, 6, '#', '', '', 1, 0, 'F', '0', '0', 'mcp:tool:export', '#', 103, 1, '2026-02-24 20:02:47', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2010, 'MCP市场管理', 2000, 2, 'market', 'mcp/market/index', '', 1, 0, 'C', '0', '0', 'mcp:market:list', 'mdi:storefront-outline', 103, 1, '2026-02-24 20:02:47', NULL, NULL, 'MCP市场管理菜单');
INSERT INTO `sys_menu` VALUES (2011, 'MCP市场查询', 2010, 1, '#', '', '', 1, 0, 'F', '0', '0', 'mcp:market:query', '#', 103, 1, '2026-02-24 20:02:47', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2012, 'MCP市场新增', 2010, 2, '#', '', '', 1, 0, 'F', '0', '0', 'mcp:market:add', '#', 103, 1, '2026-02-24 20:02:47', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2013, 'MCP市场修改', 2010, 3, '#', '', '', 1, 0, 'F', '0', '0', 'mcp:market:edit', '#', 103, 1, '2026-02-24 20:02:47', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2014, 'MCP市场删除', 2010, 4, '#', '', '', 1, 0, 'F', '0', '0', 'mcp:market:remove', '#', 103, 1, '2026-02-24 20:02:47', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2015, 'MCP市场刷新', 2010, 5, '#', '', '', 1, 0, 'F', '0', '0', 'mcp:market:refresh', '#', 103, 1, '2026-02-24 20:02:47', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2016, 'MCP工具加载', 2010, 6, '#', '', '', 1, 0, 'F', '0', '0', 'mcp:market:load', '#', 103, 1, '2026-02-24 20:02:47', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2017, 'MCP市场导出', 2010, 7, '#', '', '', 1, 0, 'F', '0', '0', 'mcp:market:export', '#', 103, 1, '2026-02-24 20:02:47', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (3000, '智能体管理', 0, 1, 'agent', '', '', 1, 0, 'M', '0', '0', '', 'mdi:robot', 103, 1, '2026-07-07 21:37:37', NULL, NULL, '智能体管理目录');
INSERT INTO `sys_menu` VALUES (3001, '智能体列表', 3000, 1, 'agent', 'agent/agent/index', '', 1, 0, 'C', '0', '0', 'agent:agent:list', 'mdi:robot-outline', 103, 1, '2026-07-07 21:37:37', NULL, NULL, '智能体列表菜单');
INSERT INTO `sys_menu` VALUES (3002, '智能体查询', 3001, 1, '#', '', '', 1, 0, 'F', '0', '0', 'agent:agent:query', '#', 103, 1, '2026-07-07 21:37:37', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (3003, '智能体新增', 3001, 2, '#', '', '', 1, 0, 'F', '0', '0', 'agent:agent:add', '#', 103, 1, '2026-07-07 21:37:37', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (3004, '智能体修改', 3001, 3, '#', '', '', 1, 0, 'F', '0', '0', 'agent:agent:edit', '#', 103, 1, '2026-07-07 21:37:37', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (3005, '智能体删除', 3001, 4, '#', '', '', 1, 0, 'F', '0', '0', 'agent:agent:remove', '#', 103, 1, '2026-07-07 21:37:37', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (3006, '智能体导出', 3001, 5, '#', '', '', 1, 0, 'F', '0', '0', 'agent:agent:export', '#', 103, 1, '2026-07-07 21:37:37', NULL, NULL, '');
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
INSERT INTO `sys_menu` VALUES (2000210914680823809, '聊天消息', 2000209300188356609, 1, 'message', 'chat/message/index', NULL, 1, 0, 'C', '0', '0', 'system:message:list', 'system-uicons:message', 103, 1, '2025-12-14 22:27:54', 1, '2025-12-15 00:53:47', '聊天消息菜单');
INSERT INTO `sys_menu` VALUES (2000210914680823810, '聊天消息查询', 2000210914680823809, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:message:query', '#', 103, 1, '2025-12-14 22:27:54', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2000210914680823811, '聊天消息新增', 2000210914680823809, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:message:add', '#', 103, 1, '2025-12-14 22:27:54', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2000210914680823812, '聊天消息修改', 2000210914680823809, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:message:edit', '#', 103, 1, '2025-12-14 22:27:54', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2000210914680823813, '聊天消息删除', 2000210914680823809, 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:message:remove', '#', 103, 1, '2025-12-14 22:27:54', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2000210914680823814, '聊天消息导出', 2000210914680823809, 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:message:export', '#', 103, 1, '2025-12-14 22:27:54', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2006681261898813441, '知识管理', 2000209300188356609, 1, 'info', 'knowledge/info/index', NULL, 1, 0, 'C', '0', '0', 'knowledge:info:list', 'solar:book-line-duotone', 103, 1, '2026-01-01 18:59:05', 1, '2026-03-15 21:07:50', '知识库菜单');
INSERT INTO `sys_menu` VALUES (2006681261898813442, '知识库查询', 2006681261898813441, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:info:query', '#', 103, 1, '2026-01-01 18:59:05', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2006681261898813443, '知识库新增', 2006681261898813441, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:info:add', '#', 103, 1, '2026-01-01 18:59:05', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2006681261898813444, '知识库修改', 2006681261898813441, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:info:edit', '#', 103, 1, '2026-01-01 18:59:05', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2006681261898813445, '知识库删除', 2006681261898813441, 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:info:remove', '#', 103, 1, '2026-01-01 18:59:06', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2006681261898813446, '知识库导出', 2006681261898813441, 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:info:export', '#', 103, 1, '2026-01-01 18:59:06', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (2031361596464902145, '编排管理', 2000209300188356609, 1, 'aiflow', 'aiflow/index', NULL, 1, 0, 'C', '0', '0', NULL, 'carbon:flow', 103, 1, '2026-03-10 21:28:40', 1, '2026-03-15 21:06:01', '');

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
INSERT INTO `sys_oper_log` VALUES (2056756437140963329, '000000', 'OSS对象存储', 1, 'org.ruoyi.system.controller.system.SysOssController.upload()', 'POST', 1, 'admin', '研发部门', '/resource/oss/upload', '127.0.0.1', '内网IP', '', '', 1, '上传文件失败，请检查配置信息:[software.amazon.awssdk.services.s3.model.S3Exception: The access key Id format you provided is invalid. (Service: S3, Status Code: 403, Request ID: NmEwYzdmNTFfOGEzMTI3MGJfOGE2Y19lNDUyZDk=)]', '2026-05-19 23:18:42', 2621);
INSERT INTO `sys_oper_log` VALUES (2076944303757836289, '000000', '智能体管理', 2, 'org.ruoyi.controller.agent.AgentController.edit()', 'PUT', 1, 'admin', '研发部门', '/agent/agent', '127.0.0.1', '内网IP', '{\"id\":\"1\",\"agentName\":\"对话智能体\",\"agentDescribe\":\"对话智能体\",\"modelId\":\"2000585866022060033\",\"enableThinking\":\"0\",\"systemPrompt\":\"你是一个乐于助人的通用 AI 助手，请用简洁、准确的中文回答用户的问题。\",\"mcpToolIds\":[],\"skillNames\":[\"docx\"],\"knowledgeIds\":[],\"status\":\"0\",\"remark\":\"系统初始化的默认智能体，自动绑定首个启用的对话模型\"}', '{\"code\":200,\"msg\":\"操作成功\"}', 0, '', '2026-07-14 16:18:04', 23);
INSERT INTO `sys_oper_log` VALUES (2076944338465701889, '000000', '会话管理', 1, 'org.ruoyi.controller.chat.ChatSessionController.add()', 'POST', 1, 'admin', '研发部门', '/system/session', '127.0.0.1', '内网IP', '{\"id\":\"2076944338398593026\",\"userId\":\"1\",\"sessionTitle\":\"你好\",\"sessionContent\":\"你好\",\"remark\":\"你好\"}', '{\"code\":200,\"msg\":\"操作成功\",\"data\":\"2076944338398593026\"}', 0, '', '2026-07-14 16:18:13', 19);
INSERT INTO `sys_oper_log` VALUES (2076957117130002434, '000000', '会话管理', 1, 'org.ruoyi.controller.chat.ChatSessionController.add()', 'POST', 1, 'admin', '研发部门', '/system/session', '127.0.0.1', '内网IP', '{\"id\":\"2076957117012561921\",\"userId\":\"1\",\"sessionTitle\":\"测试工作流\"}', '{\"code\":200,\"msg\":\"操作成功\",\"data\":\"2076957117012561921\"}', 0, '', '2026-07-14 17:08:59', 24);
INSERT INTO `sys_oper_log` VALUES (2076958796898422785, '000000', '会话管理', 1, 'org.ruoyi.controller.chat.ChatSessionController.add()', 'POST', 1, 'admin', '研发部门', '/system/session', '127.0.0.1', '内网IP', '{\"id\":\"2076958796772593665\",\"userId\":\"1\",\"sessionTitle\":\"测试工作流\"}', '{\"code\":200,\"msg\":\"操作成功\",\"data\":\"2076958796772593665\"}', 0, '', '2026-07-14 17:15:40', 18);
INSERT INTO `sys_oper_log` VALUES (2076958822575951874, '000000', '会话管理', 1, 'org.ruoyi.controller.chat.ChatSessionController.add()', 'POST', 1, 'admin', '研发部门', '/system/session', '127.0.0.1', '内网IP', '{\"id\":\"2076958822525620225\",\"userId\":\"1\",\"sessionTitle\":\"测试工作流\"}', '{\"code\":200,\"msg\":\"操作成功\",\"data\":\"2076958822525620225\"}', 0, '', '2026-07-14 17:15:46', 15);
INSERT INTO `sys_oper_log` VALUES (2076964726599897090, '000000', '会话管理', 1, 'org.ruoyi.controller.chat.ChatSessionController.add()', 'POST', 1, 'admin', '研发部门', '/system/session', '127.0.0.1', '内网IP', '{\"id\":\"2076964726549565441\",\"userId\":\"1\",\"sessionTitle\":\"测试工作流\"}', '{\"code\":200,\"msg\":\"操作成功\",\"data\":\"2076964726549565441\"}', 0, '', '2026-07-14 17:39:14', 9);
INSERT INTO `sys_oper_log` VALUES (2076964779045474305, '000000', '会话管理', 1, 'org.ruoyi.controller.chat.ChatSessionController.add()', 'POST', 1, 'admin', '研发部门', '/system/session', '127.0.0.1', '内网IP', '{\"id\":\"2076964778940616705\",\"userId\":\"1\",\"sessionTitle\":\"测试工作流\"}', '{\"code\":200,\"msg\":\"操作成功\",\"data\":\"2076964778940616705\"}', 0, '', '2026-07-14 17:39:26', 23);
INSERT INTO `sys_oper_log` VALUES (2076971705414242305, '000000', '会话管理', 1, 'org.ruoyi.controller.chat.ChatSessionController.add()', 'POST', 1, 'admin', '研发部门', '/system/session', '127.0.0.1', '内网IP', '{\"id\":\"2076971705284218881\",\"userId\":\"1\",\"sessionTitle\":\"你好2阿\",\"sessionContent\":\"你好2阿\",\"remark\":\"你好2阿\"}', '{\"code\":200,\"msg\":\"操作成功\",\"data\":\"2076971705284218881\"}', 0, '', '2026-07-14 18:06:57', 24);

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
INSERT INTO `sys_oss` VALUES (2026580908423340033, '000000', '2026/02/25/9219d6d71a6d45e19192014609d92dc9.png', 'logo.png', '.png', 'https://example-1234567890.cos.ap-guangzhou.myqcloud.com/2026/02/25/9219d6d71a6d45e19192014609d92dc9.png', '{\"fileSize\":\"183613\",\"contentType\":\"image/png\"}', 103, '2026-02-25 16:51:55', 1, '2026-02-25 16:51:55', 1, 'qcloud');
INSERT INTO `sys_oss` VALUES (2026640059920883713, '000000', '2026/02/25/01091be272334383a1efd9bc22b73ee6.png', 'openai.png', '.png', 'https://example-1234567890.cos.ap-guangzhou.myqcloud.com/2026/02/25/01091be272334383a1efd9bc22b73ee6.png', '{\"fileSize\":\"11297\",\"contentType\":\"image/png\"}', 103, '2026-02-25 20:46:58', 1, '2026-02-25 20:46:58', 1, 'qcloud');
INSERT INTO `sys_oss` VALUES (2026640515967557633, '000000', '2026/02/25/afecabebc8014d80b0f06b4796a74c5d.png', 'ollama.png', '.png', 'https://example-1234567890.cos.ap-guangzhou.myqcloud.com/2026/02/25/afecabebc8014d80b0f06b4796a74c5d.png', '{\"fileSize\":\"8746\",\"contentType\":\"image/png\"}', 103, '2026-02-25 20:48:47', 1, '2026-02-25 20:48:47', 1, 'qcloud');
INSERT INTO `sys_oss` VALUES (2026640548213366785, '000000', '2026/02/25/e16429a462e54e14a1d36673146b9e3c.png', 'ppio-color.png', '.png', 'https://example-1234567890.cos.ap-guangzhou.myqcloud.com/2026/02/25/e16429a462e54e14a1d36673146b9e3c.png', '{\"fileSize\":\"7382\",\"contentType\":\"image/png\"}', 103, '2026-02-25 20:48:55', 1, '2026-02-25 20:48:55', 1, 'qcloud');
INSERT INTO `sys_oss` VALUES (2026640572443860993, '000000', '2026/02/25/049bb6a507174f73bba4b8d8b9e55b8a.png', 'ppio-color.png', '.png', 'https://example-1234567890.cos.ap-guangzhou.myqcloud.com/2026/02/25/049bb6a507174f73bba4b8d8b9e55b8a.png', '{\"fileSize\":\"7382\",\"contentType\":\"image/png\"}', 103, '2026-02-25 20:49:00', 1, '2026-02-25 20:49:00', 1, 'qcloud');
INSERT INTO `sys_oss` VALUES (2026640621945036802, '000000', '2026/02/25/de2aa7e649de44f3ba5c6380ac6acd04.png', 'bailian-color.png', '.png', 'https://example-1234567890.cos.ap-guangzhou.myqcloud.com/2026/02/25/de2aa7e649de44f3ba5c6380ac6acd04.png', '{\"fileSize\":\"5901\",\"contentType\":\"image/png\"}', 103, '2026-02-25 20:49:12', 1, '2026-02-25 20:49:12', 1, 'qcloud');
INSERT INTO `sys_oss` VALUES (2033120065673043969, '000000', '2026/03/15/68c4d853814e444982c2517ffabac0f3.jpg', '9b75e600b2df6160261a507055eabfdf.jpg', '.jpg', 'https://example-1234567890.cos.ap-guangzhou.myqcloud.com/2026/03/15/68c4d853814e444982c2517ffabac0f3.jpg', '{\"fileSize\":\"28027\",\"contentType\":\"image/jpeg\"}', 103, '2026-03-15 17:56:12', 1, '2026-03-15 17:56:12', 1, 'qcloud');
INSERT INTO `sys_oss` VALUES (2033169884118593537, '000000', '2026/03/15/4b7e93a72bf04805ae59985cc0845ef1.png', 'logo.png', '.png', 'https://example-1234567890.cos.ap-guangzhou.myqcloud.com/2026/03/15/4b7e93a72bf04805ae59985cc0845ef1.png', '{\"fileSize\":\"61537\",\"contentType\":\"image/png\"}', 103, '2026-03-15 21:14:10', 1, '2026-03-15 21:14:10', 1, 'qcloud');
INSERT INTO `sys_oss` VALUES (2033198191581147137, '000000', '2026/03/15/66d9e6d216c74652bb466a13d24f4440.txt', 'ruoyi-ai介绍.txt', '.txt', 'https://example-1234567890.cos.ap-guangzhou.myqcloud.com/2026/03/15/66d9e6d216c74652bb466a13d24f4440.txt', '{\"fileSize\":\"1166\",\"contentType\":\"text/plain\"}', 103, '2026-03-15 23:06:39', 1, '2026-03-15 23:06:39', 1, 'qcloud');
INSERT INTO `sys_oss` VALUES (2033198447232364546, '000000', '2026/03/15/ae1b1e0d363e4bc1b3321d696453fdbf.txt', 'ruoyi-ai介绍.txt', '.txt', 'https://example-1234567890.cos.ap-guangzhou.myqcloud.com/2026/03/15/ae1b1e0d363e4bc1b3321d696453fdbf.txt', '{\"fileSize\":\"1166\",\"contentType\":\"text/plain\"}', 103, '2026-03-15 23:07:39', 1, '2026-03-15 23:07:39', 1, 'qcloud');
INSERT INTO `sys_oss` VALUES (2033198841211727874, '000000', '2026/03/15/83564059b4f643b69a1e0ea727d17364.txt', 'ruoyi-ai介绍.txt', '.txt', 'https://example-1234567890.cos.ap-guangzhou.myqcloud.com/2026/03/15/83564059b4f643b69a1e0ea727d17364.txt', '{\"fileSize\":\"1166\",\"contentType\":\"text/plain\"}', 103, '2026-03-15 23:09:13', 1, '2026-03-15 23:09:13', 1, 'qcloud');
INSERT INTO `sys_oss` VALUES (2033199209064771586, '000000', '2026/03/15/695360eb380d43d6af34e8a308c09696.txt', 'ruoyi-ai介绍.txt', '.txt', 'https://example-1234567890.cos.ap-guangzhou.myqcloud.com/2026/03/15/695360eb380d43d6af34e8a308c09696.txt', '{\"fileSize\":\"1166\",\"contentType\":\"text/plain\"}', 103, '2026-03-15 23:10:41', 1, '2026-03-15 23:10:41', 1, 'qcloud');

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
INSERT INTO `sys_oss_config` VALUES (1, '000000', 'minio', 'ruoyi', 'ruoyi123', 'ruoyi', '', '127.0.0.1:9000', '', 'N', '', '1', '1', '', 103, 1, '2026-02-03 05:14:52', 1, '2026-02-25 15:44:13', NULL);
INSERT INTO `sys_oss_config` VALUES (2, '000000', 'qiniu', 'XXXXXXXXXXXXXXX', 'XXXXXXXXXXXXXXX', 'ruoyi', '', 's3-cn-north-1.qiniucs.com', '', 'N', '', '1', '1', '', 103, 1, '2026-02-03 05:14:52', 1, '2026-02-03 05:14:52', NULL);
INSERT INTO `sys_oss_config` VALUES (3, '000000', 'aliyun', 'XXXXXXXXXXXXXXX', 'XXXXXXXXXXXXXXX', 'ruoyi', '', 'oss-cn-beijing.aliyuncs.com', '', 'N', '', '1', '1', '', 103, 1, '2026-02-03 05:14:52', 1, '2026-02-03 05:14:52', NULL);
INSERT INTO `sys_oss_config` VALUES (4, '000000', 'qcloud', 'xx', 'xx', 'example-1234567890', '', 'cos.ap-guangzhou.myqcloud.com', '', 'Y', 'ap-guangzhou', '1', '0', '', 103, 1, '2026-02-03 05:14:52', 1, '2026-03-15 17:56:06', '');
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
INSERT INTO `sys_user` VALUES (1, '000000', 103, 'admin', 'admin', 'sys_user', 'ageerle@163.com', '15888888888', '1', NULL, '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', '2026-07-14 20:31:20', 103, 1, '2026-02-05 09:22:12', -1, '2026-07-14 20:31:20', '管理员', NULL, 0.00);
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
) ENGINE = InnoDB AUTO_INCREMENT = 37 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工作流组件库 | Workflow Component' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_workflow_component
-- ----------------------------
INSERT INTO `t_workflow_component` VALUES (17, '5cd68dccbbb411f0bb7840c2ba9a7fbc', 'Start', '开始', '流程由此开始', 0, 1, '2025-11-07 16:32:49', '2025-11-07 16:32:49', 0, '000000');
INSERT INTO `t_workflow_component` VALUES (18, '5cd6ac69bbb411f0bb7840c2ba9a7fbc', 'End', '结束', '流程由此结束', 0, 1, '2025-11-07 16:32:49', '2025-11-07 16:32:49', 0, '000000');
INSERT INTO `t_workflow_component` VALUES (19, '5cd6c8eabbb411f0bb7840c2ba9a7fbc', 'Answer', '生成回答', '调用大语言模型回答问题', 0, 1, '2025-11-07 16:32:49', '2025-11-07 16:32:49', 0, '000000');
INSERT INTO `t_workflow_component` VALUES (25, '0b4369bb60dc46d6bd84ceb4e36184dc', 'KeywordExtractor', '关键词提取', '从文本中提取关键词', 0, 1, '2025-12-26 16:30:05', '2025-12-26 16:30:05', 0, '000000');
INSERT INTO `t_workflow_component` VALUES (26, 'bb00fc2f52c74fec82ee3f99725b56bb', 'Switcher', '条件分支', '根据条件执行不同分支', 0, 1, '2025-12-26 16:30:46', '2025-12-26 16:30:46', 0, '000000');
INSERT INTO `t_workflow_component` VALUES (36, 'f37dbcb8f0d5464d90fbb22774490a56', 'HumanFeedback', '人类', '人机沟通', 0, 1, '2025-12-30 17:37:14', '2025-12-30 17:37:14', 0, '000000');

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
