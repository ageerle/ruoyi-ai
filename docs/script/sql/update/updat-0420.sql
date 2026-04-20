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

 Date: 20/04/2026 15:30:00
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 新增：重排序模型（chat_model）
-- ----------------------------
INSERT INTO `chat_model`
(id, category, model_name, provider_code, model_describe, model_dimension, model_show, api_host, api_key, create_dept, create_by, create_time, update_by, update_time, remark, tenant_id)
VALUES(2045071617578237953, 'rerank', 'rerank', 'zhipu', '智谱重排序', NULL, 'Y', 'https://open.bigmodel.cn', 'e9xx', 103, 1, '2026-04-17 17:27:24', 1, '2026-04-20 15:21:48', '智谱重排序', 0);

INSERT INTO `chat_model`
(id, category, model_name, provider_code, model_describe, model_dimension, model_show, api_host, api_key, create_dept, create_by, create_time, update_by, update_time, remark, tenant_id)
VALUES(2046119803482902530, 'rerank', 'qwen3-rerank', 'qianwen', '千问3重排序', NULL, NULL, 'https://dashscope.aliyuncs.com', 'sk-xx', 103, 1, '2026-04-20 14:52:31', 1, '2026-04-20 15:03:13', '千问3文本重排序', 0);

-- ----------------------------
-- 新增：字典类型 - 重排序模型分类
-- ----------------------------
INSERT INTO `sys_dict_data`
(dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES(2045070879435259905, '000000', 4, '重排序', 'rerank', 'chat_model_category', NULL, '#000000', 'N', 103, 1, '2026-04-17 17:24:28', 1, '2026-04-19 01:02:20', '重排序模型');

-- ----------------------------
-- 修改表：knowledge_info 增加重排序相关字段
-- ----------------------------
ALTER TABLE `knowledge_info` ADD COLUMN `enable_rerank` tinyint DEFAULT 0 NULL COMMENT '是否启用重排序（0否 1是）';
ALTER TABLE `knowledge_info` ADD COLUMN `rerank_score_threshold` double NULL COMMENT '重排序相关性分数阈值';
ALTER TABLE `knowledge_info` ADD COLUMN `rerank_top_n` int NULL COMMENT '重排序后返回的文档数量';
ALTER TABLE `knowledge_info` ADD COLUMN `rerank_model` varchar(100) NULL COMMENT '重排序模型名称';

SET FOREIGN_KEY_CHECKS = 1;