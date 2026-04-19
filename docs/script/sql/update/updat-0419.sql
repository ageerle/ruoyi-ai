/*
 Navicat Premium Dump SQL

 Source Server         : localhost-mysql
 Source Server Type    : MySQL
 Source Server Version : 80045 (8.0.45)
 Source Host           : localhost:3306
 Source Schema         : ruoyi-ai

 Target Server Type    : MySQL
 Target Server Version : 80045 (8.0.45)
 File Encoding         : 65001

 Date: 19/04/2026 13:36:41
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

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
INSERT INTO `chat_model` VALUES (2000585866022060033, 'chat', 'zai-org/glm-5', 'ppio', 'zai-org/glm-5', NULL, 'Y', 'https://api.ppio.com/openai', 'sk_xx', 103, 1, '2025-12-15 23:16:54', 1, '2026-03-15 19:18:48', 'DeepSeek-V3.2 是一款在高效推理、复杂推理能力与智能体场景中表现突出的领先模型。其基于 DeepSeek Sparse Attention（DSA）稀疏注意力机制，在显著降低计算开销的同时优化长上下文性能；通过可扩展强化学习框架，整体能力达到 GPT-5 同级，高算力版本 V3.2-Speciale 更在推理表现上接近 Gemini-3.0-Pro；同时，模型依托大型智能体任务合成管线，具备更强的工具调用与多步骤决策能力，并在 2025 年 IMO 与 IOI 中取得金牌级表现。作为 MaaS 平台，我们已对 DeepSeek-V3.2 完成深度适配，通过动态调度、批处理加速、低延迟推理与企业级 SLA 保障，进一步增强其在企业生产环境中的稳定性、性价比与可控性，适用于搜索、问答、智能体、代码、数据处理等多类高价值场景。', 0);
INSERT INTO `chat_model` VALUES (2007528268536287233, 'vector', 'baai/bge-m3', 'ppio', 'bge-m3', 1024, 'N', 'https://api.ppio.com/openai', 'sk_xx', 103, 1, '2026-01-04 03:03:32', 1, '2026-03-15 19:18:51', 'BGE-M3 是一款具备多维度能力的文本嵌入模型，可同时实现密集检索、多向量检索和稀疏检索三大核心功能。该模型设计上兼容超过100种语言，并支持从短句到长达8192词元的长文本等多种输入形式。在跨语言检索任务中，BGE-M3展现出显著优势，其性能在MIRACL、MKQA等国际基准测试中位居前列。此外，针对长文档检索场景，该模型在MLDR、NarritiveQA等数据集上的表现同样达到行业领先水平。', 0);
INSERT INTO `chat_model` VALUES (2045735140488847361, 'chat', 'deepseek-chat', 'custom_api', 'deepseek-chat', NULL, NULL, 'https://api.deepseek.com', 'sk_xx', 103, 1, '2026-04-19 13:24:00', 1, '2026-04-19 13:24:00', 'deepseek对话模型', 0);

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
) ENGINE = InnoDB AUTO_INCREMENT = 2045727230803255298 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '厂商管理表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of chat_provider
-- ----------------------------
INSERT INTO `chat_provider` VALUES (1, 'OpenAI', 'openai', 'https://ruoyiai-1254149996.cos.ap-guangzhou.myqcloud.com/2026/02/25/01091be272334383a1efd9bc22b73ee6.png', 'OpenAI官方API服务商', 'https://api.openai.com', '0', 1, 103, '2025-12-14 21:48:11', '1', '1', '2026-02-25 20:46:59', 'OpenAI厂商', NULL, '0', NULL, 0);
INSERT INTO `chat_provider` VALUES (11, '深度求索', 'deepseek', 'https://ruoyiai-1254149996.cos.ap-guangzhou.myqcloud.com/2026/04/19/5ba8c30f153246898a4d7dc7b846de8d.png', 'DeepSeek官方API', 'https://api.deepseek.com', '0', 0, 103, '2026-04-19 12:52:34', '1', '1', '2026-04-19 13:13:25', 'DeepSeek官方API', NULL, '0', NULL, 0);
INSERT INTO `chat_provider` VALUES (12, '智谱AI', 'zhipu', 'https://ruoyiai-1254149996.cos.ap-guangzhou.myqcloud.com/2026/04/19/da071783c9284fdd9ed1ce1b57b3c75c.png', '智谱AI大模型服务', 'https://open.bigmodel.cn', '0', 4, 103, '2025-12-14 21:48:11', '1', '1', '2026-04-19 13:14:00', '智谱AI厂商', NULL, '0', NULL, 0);
INSERT INTO `chat_provider` VALUES (13, '小米MIMO', 'xiaomi', 'https://ruoyiai-1254149996.cos.ap-guangzhou.myqcloud.com/2026/04/19/18dd39365ce244e3ae5e030da036760e.png', '小米官方API', 'https://api.xiaomimimo.com/anthropic/v1/messages', '0', 3, 103, '2026-04-19 12:48:24', '1', '1', '2026-04-19 13:14:22', '小米官方API', NULL, '0', NULL, 0);
INSERT INTO `chat_provider` VALUES (14, '阿里云百炼', 'qianwen', 'https://ruoyiai-1254149996.cos.ap-guangzhou.myqcloud.com/2026/02/25/de2aa7e649de44f3ba5c6380ac6acd04.png', '阿里云百炼大模型服务', 'https://dashscope.aliyuncs.com', '0', 2, 103, '2025-12-14 21:48:11', '1', '1', '2026-02-25 20:49:13', '阿里云厂商', NULL, '0', NULL, 0);
INSERT INTO `chat_provider` VALUES (15, 'PPIO', 'ppio', 'https://ruoyiai-1254149996.cos.ap-guangzhou.myqcloud.com/2026/02/25/049bb6a507174f73bba4b8d8b9e55b8a.png', 'api聚合厂商', 'https://api.ppinfra.com/openai', '0', 5, 103, '2025-12-15 23:13:42', '1', '1', '2026-02-25 20:49:01', 'api聚合厂商', NULL, '0', NULL, 0);
INSERT INTO `chat_provider` VALUES (16, 'MiniMax', 'minimax', 'https://ruoyiai-1254149996.cos.ap-guangzhou.myqcloud.com/2026/04/19/fdc712e90e0e4d78b05862ad230884e5.png', 'MiniMax大模型服务，支持M2.7、M2.5等模型', 'https://api.minimax.io/v1', '0', 6, 103, '2026-04-19 12:50:12', '1', '1', '2026-04-19 13:14:59', 'MiniMax厂商', NULL, '0', NULL, 0);
INSERT INTO `chat_provider` VALUES (17, 'ollama', 'ollama', 'https://ruoyiai-1254149996.cos.ap-guangzhou.myqcloud.com/2026/02/25/afecabebc8014d80b0f06b4796a74c5d.png', 'ollama大模型', 'http://127.0.0.1:11434', '0', 7, 103, '2025-12-14 21:48:11', '1', '1', '2026-02-25 20:48:48', 'ollama厂商', NULL, '0', NULL, 0);
INSERT INTO `chat_provider` VALUES (18, '自定义厂商', 'custom_api', 'https://ruoyiai-1254149996.cos.ap-guangzhou.myqcloud.com/2026/04/19/c1a8e122510f4e2f90deb36958af710b.png', 'OPENAI兼容格式', '自定义', '0', 8, 103, '2026-04-19 12:35:57', '1', '1', '2026-04-19 13:17:20', 'OPENAI兼容格式', NULL, '0', NULL, 0);

SET FOREIGN_KEY_CHECKS = 1;
