/*
 Navicat MySQL Data Transfer

 Source Server         : 本地
 Source Server Type    : MySQL
 Source Server Version : 80034
 Source Host           : localhost:3306
 Source Schema         : ry-vue

 Target Server Type    : MySQL
 Target Server Version : 80034
 File Encoding         : 65001

 Date: 26/03/2024 18:15:47
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for access_token
-- ----------------------------
DROP TABLE IF EXISTS `access_token`;
CREATE TABLE `access_token`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `app` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '应用',
  `token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'token值',
  `expired_time` datetime NOT NULL COMMENT '过期时间',
  `create_time` datetime NULL DEFAULT NULL,
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '第三方接口访问token' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of access_token
-- ----------------------------

-- ----------------------------
-- Table structure for agent_field
-- ----------------------------
DROP TABLE IF EXISTS `agent_field`;
CREATE TABLE `agent_field`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `fid` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '领域ID',
  `field_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '领域名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '领域描述',
  `status` bit(1) NULL DEFAULT b'0' COMMENT '是否生效：0无效1有效',
  `create_time` datetime NULL DEFAULT NULL,
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '代理领域' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of agent_field
-- ----------------------------

-- ----------------------------
-- Table structure for chat_message
-- ----------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message`  (
  `id` bigint NOT NULL COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '消息内容',
  `deduct_cost` double(20, 10) NULL DEFAULT 0.0000000000 COMMENT '扣除金额\r\n\r\n',
  `total_tokens` int NULL DEFAULT NULL COMMENT '累计 Tokens',
  `model_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '模型名称',
  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '聊天消息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of chat_message
-- ----------------------------

-- ----------------------------
-- Table structure for chat_request_log
-- ----------------------------
DROP TABLE IF EXISTS `chat_request_log`;
CREATE TABLE `chat_request_log`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL COMMENT '用户ID',
  `kid` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '知识库ID',
  `request_time` datetime NOT NULL COMMENT '请求时间',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '内容',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '对话请求日志' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of chat_request_log
-- ----------------------------

-- ----------------------------
-- Table structure for chat_token
-- ----------------------------
DROP TABLE IF EXISTS `chat_token`;
CREATE TABLE `chat_token`  (
  `id` bigint NOT NULL COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户',
  `token` int NULL DEFAULT NULL COMMENT '待结算token',
  `model_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '模型名称',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'token信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of chat_token
-- ----------------------------

-- ----------------------------
-- Table structure for demand
-- ----------------------------
DROP TABLE IF EXISTS `demand`;
CREATE TABLE `demand`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `did` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '需求ID',
  `fid` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '所属领域ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '需求内容',
  `user_id` int NULL DEFAULT NULL COMMENT '用户ID',
  `unambiguous` bit(1) NULL DEFAULT b'0' COMMENT '明确的',
  `create_time` datetime NULL DEFAULT NULL,
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '需求' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of demand
-- ----------------------------

-- ----------------------------
-- Table structure for demand_step
-- ----------------------------
DROP TABLE IF EXISTS `demand_step`;
CREATE TABLE `demand_step`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `did` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '需求ID',
  `fid` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '所属领域ID',
  `step_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '步骤名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '步骤描述',
  `role` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '负责人角色',
  `user_id` int NULL DEFAULT NULL COMMENT '用户ID',
  `create_time` datetime NULL DEFAULT NULL,
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '需求步骤' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of demand_step
-- ----------------------------

-- ----------------------------
-- Table structure for gen_table
-- ----------------------------
DROP TABLE IF EXISTS `gen_table`;
CREATE TABLE `gen_table`  (
  `table_id` bigint NOT NULL COMMENT '编号',
  `table_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '表名称',
  `table_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '表描述',
  `sub_table_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '关联子表的表名',
  `sub_table_fk_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '子表关联的外键名',
  `class_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '实体类名称',
  `tpl_category` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作）',
  `package_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '生成包路径',
  `module_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '生成模块名',
  `business_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '生成业务名',
  `function_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '生成功能名',
  `function_author` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '生成功能作者',
  `gen_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
  `gen_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
  `options` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '其它生成选项',
  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`table_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '代码生成业务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of gen_table
-- ----------------------------
INSERT INTO `gen_table` VALUES (1661288222902505474, 'sys_notice', '通知公告表', NULL, NULL, 'SysNotice', 'crud', 'org.dromara.system', 'system', 'notice', '通知公告', 'Lion Li', '0', '/', NULL, 103, 1, '2023-05-20 18:05:11', 1, '2023-05-20 18:05:11', NULL);
INSERT INTO `gen_table` VALUES (1661288223338713089, 'sys_oper_log', '操作日志记录', NULL, NULL, 'SysOperLog', 'crud', 'org.dromara.system', 'system', 'operLog', '操作日志记录', 'Lion Li', '0', '/', NULL, 103, 1, '2023-05-20 18:05:11', 1, '2023-05-20 18:05:11', NULL);
INSERT INTO `gen_table` VALUES (1661288223477125122, 'sys_oss', 'OSS对象存储表', NULL, NULL, 'SysOss', 'crud', 'org.dromara.system', 'system', 'oss', 'OSS对象存储', 'Lion Li', '0', '/', NULL, 103, 1, '2023-05-20 18:05:11', 1, '2023-05-20 18:05:11', NULL);
INSERT INTO `gen_table` VALUES (1661288223586177025, 'sys_oss_config', '对象存储配置表', NULL, NULL, 'SysOssConfig', 'crud', 'org.dromara.system', 'system', 'ossConfig', '对象存储配置', 'Lion Li', '0', '/', NULL, 103, 1, '2023-05-20 18:05:11', 1, '2023-05-20 18:05:11', NULL);
INSERT INTO `gen_table` VALUES (1661288223728783361, 'sys_post', '岗位信息表', NULL, NULL, 'SysPost', 'crud', 'org.dromara.system', 'system', 'post', '岗位信息', 'Lion Li', '0', '/', NULL, 103, 1, '2023-05-20 18:05:11', 1, '2023-05-20 18:05:11', NULL);
INSERT INTO `gen_table` VALUES (1661288223821058050, 'sys_role', '角色信息表', NULL, NULL, 'SysRole', 'crud', 'org.dromara.system', 'system', 'role', '角色信息', 'Lion Li', '0', '/', NULL, 103, 1, '2023-05-20 18:05:11', 1, '2023-05-20 18:05:11', NULL);
INSERT INTO `gen_table` VALUES (1661288223925915650, 'sys_user_post', '用户与岗位关联表', NULL, NULL, 'SysUserPost', 'crud', 'org.dromara.system', 'system', 'userPost', '用户与岗位关联', 'Lion Li', '0', '/', NULL, 103, 1, '2023-05-20 18:05:11', 1, '2023-05-20 18:05:11', NULL);
INSERT INTO `gen_table` VALUES (1661288223967858689, 'sys_user_role', '用户和角色关联表', NULL, NULL, 'SysUserRole', 'crud', 'org.dromara.system', 'system', 'userRole', '用户和角色关联', 'Lion Li', '0', '/', NULL, 103, 1, '2023-05-20 18:05:11', 1, '2023-05-20 18:05:11', NULL);
INSERT INTO `gen_table` VALUES (1661288224013996034, 'test_demo', '测试单表', NULL, NULL, 'TestDemo', 'crud', 'org.dromara.system', 'system', 'demo', '测试单', 'Lion Li', '0', '/', NULL, 103, 1, '2023-05-20 18:05:11', 1, '2023-05-20 18:05:11', NULL);
INSERT INTO `gen_table` VALUES (1661288224185962497, 'test_tree', '测试树表', NULL, NULL, 'TestTree', 'crud', 'org.dromara.system', 'system', 'tree', '测试树', 'Lion Li', '0', '/', NULL, 103, 1, '2023-05-20 18:05:11', 1, '2023-05-20 18:05:11', NULL);
INSERT INTO `gen_table` VALUES (1661288385096241154, 'sys_config', '参数配置表', NULL, NULL, 'SysConfig', 'crud', 'org.dromara.system', 'system', 'config', '参数配置', 'Lion Li', '0', '/', NULL, 103, 1, '2023-05-20 18:05:10', 1, '2023-05-20 18:05:10', NULL);
INSERT INTO `gen_table` VALUES (1680196323445579778, 'sys_file_detail', '文件记录表', NULL, NULL, 'SysFileDetail', 'crud', 'com.xmzs.system', 'system', 'fileDetail', '文件记录', 'Lion Li', '0', '/', NULL, 103, 1, '2023-07-15 20:40:00', 1, '2023-07-15 20:40:00', NULL);
INSERT INTO `gen_table` VALUES (1680196323521077249, 'sys_file_detail', '文件记录表', NULL, NULL, 'SysFileDetail', 'crud', 'com.xmzs.system', 'system', 'fileDetail', '文件记录', 'Lion Li', '0', '/', NULL, 103, 1, '2023-07-15 20:40:00', 1, '2023-07-15 20:40:00', NULL);
INSERT INTO `gen_table` VALUES (1680199147407806465, 'sys_file_info', '文件记录表', NULL, NULL, 'SysFileInfo', 'crud', 'com.xmzs.system', 'system', 'fileInfo', '文件记录', 'Lion Li', '0', '/', NULL, 103, 1, '2023-07-15 20:53:56', 1, '2023-07-15 20:53:56', NULL);
INSERT INTO `gen_table` VALUES (1680481752850145282, 'sd_model_param', '模型参数信息表', NULL, NULL, 'SdModelParam', 'crud', 'com.xmzs.system', 'system', 'modelParam', '模型参数信息', 'Lion Li', '0', '/', NULL, 103, 1, '2023-07-16 15:18:34', 1, '2023-07-16 15:18:34', NULL);
INSERT INTO `gen_table` VALUES (1728684654923988993, 'chat_message', '聊天消息表', NULL, NULL, 'ChatMessage', 'crud', 'com.xmzs.system', 'system', 'message', '聊天消息', 'Lion Li', '0', '/', NULL, 103, 1, '2023-11-26 15:28:10', 1, '2023-11-26 15:28:10', NULL);
INSERT INTO `gen_table` VALUES (1740573614897897473, 'payment_orders', '支付订单表', NULL, NULL, 'PaymentOrders', 'crud', 'com.xmzs.system', 'system', 'orders', '支付订单', 'Lion Li', '0', '/', NULL, 103, 1, '2023-12-27 23:04:45', 1, '2023-12-27 23:04:45', NULL);

-- ----------------------------
-- Table structure for gen_table_column
-- ----------------------------
DROP TABLE IF EXISTS `gen_table_column`;
CREATE TABLE `gen_table_column`  (
  `column_id` bigint NOT NULL COMMENT '编号',
  `table_id` bigint NULL DEFAULT NULL COMMENT '归属表编号',
  `column_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '列名称',
  `column_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '列描述',
  `column_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '列类型',
  `java_type` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'JAVA类型',
  `java_field` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'JAVA字段名',
  `is_pk` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否主键（1是）',
  `is_increment` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否自增（1是）',
  `is_required` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否必填（1是）',
  `is_insert` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否为插入字段（1是）',
  `is_edit` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否编辑字段（1是）',
  `is_list` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否列表字段（1是）',
  `is_query` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否查询字段（1是）',
  `query_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'EQ' COMMENT '查询方式（等于、不等于、大于、小于、范围）',
  `html_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
  `dict_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '字典类型',
  `sort` int NULL DEFAULT NULL COMMENT '排序',
  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`column_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '代码生成业务表字段' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of gen_table_column
-- ----------------------------
INSERT INTO `gen_table_column` VALUES (1661288223078666241, 1661288222902505474, 'notice_id', '公告ID', 'bigint(20)', 'Long', 'noticeId', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223108026369, 1661288222902505474, 'tenant_id', '租户编号', 'varchar(20)', 'String', 'tenantId', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 2, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223108026370, 1661288222902505474, 'notice_title', '公告标题', 'varchar(50)', 'String', 'noticeTitle', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 3, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223108026371, 1661288222902505474, 'notice_type', '公告类型（1通知 2公告）', 'char(1)', 'String', 'noticeType', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'select', '', 4, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223108026372, 1661288222902505474, 'notice_content', '公告内容', 'longblob', 'String', 'noticeContent', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'editor', '', 5, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223120609282, 1661288222902505474, 'status', '公告状态（0正常 1关闭）', 'char(1)', 'String', 'status', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'radio', '', 6, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223120609283, 1661288222902505474, 'create_dept', '创建部门', 'bigint(20)', 'Long', 'createDept', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 7, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223120609284, 1661288222902505474, 'create_by', '创建者', 'bigint(20)', 'Long', 'createBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 8, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223120609285, 1661288222902505474, 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 9, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223120609286, 1661288222902505474, 'update_by', '更新者', 'bigint(20)', 'Long', 'updateBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 10, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223120609287, 1661288222902505474, 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 11, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223120609288, 1661288222902505474, 'remark', '备注', 'varchar(255)', 'String', 'remark', '0', '0', '1', '1', '1', '1', NULL, 'EQ', 'input', '', 12, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223363878913, 1661288223338713089, 'oper_id', '日志主键', 'bigint(20)', 'Long', 'operId', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223363878914, 1661288223338713089, 'tenant_id', '租户编号', 'varchar(20)', 'String', 'tenantId', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 2, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223363878915, 1661288223338713089, 'title', '模块标题', 'varchar(50)', 'String', 'title', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 3, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223363878916, 1661288223338713089, 'business_type', '业务类型（0其它 1新增 2修改 3删除）', 'int(2)', 'Integer', 'businessType', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'select', '', 4, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223401627649, 1661288223338713089, 'method', '方法名称', 'varchar(100)', 'String', 'method', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 5, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223401627650, 1661288223338713089, 'request_method', '请求方式', 'varchar(10)', 'String', 'requestMethod', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 6, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223401627651, 1661288223338713089, 'operator_type', '操作类别（0其它 1后台用户 2手机端用户）', 'int(1)', 'Integer', 'operatorType', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'select', '', 7, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223401627652, 1661288223338713089, 'oper_name', '操作人员', 'varchar(50)', 'String', 'operName', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 8, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223401627653, 1661288223338713089, 'dept_name', '部门名称', 'varchar(50)', 'String', 'deptName', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 9, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223401627654, 1661288223338713089, 'oper_url', '请求URL', 'varchar(255)', 'String', 'operUrl', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 10, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223401627655, 1661288223338713089, 'oper_ip', '主机地址', 'varchar(128)', 'String', 'operIp', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 11, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223401627656, 1661288223338713089, 'oper_location', '操作地点', 'varchar(255)', 'String', 'operLocation', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 12, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223401627657, 1661288223338713089, 'oper_param', '请求参数', 'varchar(2000)', 'String', 'operParam', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'textarea', '', 13, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223401627658, 1661288223338713089, 'json_result', '返回参数', 'varchar(2000)', 'String', 'jsonResult', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'textarea', '', 14, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223401627659, 1661288223338713089, 'status', '操作状态（0正常 1异常）', 'int(1)', 'Integer', 'status', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'radio', '', 15, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223401627660, 1661288223338713089, 'error_msg', '错误消息', 'varchar(2000)', 'String', 'errorMsg', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'textarea', '', 16, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223401627661, 1661288223338713089, 'oper_time', '操作时间', 'datetime', 'Date', 'operTime', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'datetime', '', 17, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223401627662, 1661288223338713089, 'cost_time', '消耗时间', 'bigint(20)', 'Long', 'costTime', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 18, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223502290946, 1661288223477125122, 'oss_id', '对象存储主键', 'bigint(20)', 'Long', 'ossId', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223502290947, 1661288223477125122, 'tenant_id', '租户编号', 'varchar(20)', 'String', 'tenantId', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 2, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223502290948, 1661288223477125122, 'file_name', '文件名', 'varchar(255)', 'String', 'fileName', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 3, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223502290949, 1661288223477125122, 'original_name', '原名', 'varchar(255)', 'String', 'originalName', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 4, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223502290950, 1661288223477125122, 'file_suffix', '文件后缀名', 'varchar(10)', 'String', 'fileSuffix', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 5, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223502290951, 1661288223477125122, 'url', 'URL地址', 'varchar(500)', 'String', 'url', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'textarea', '', 6, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223502290952, 1661288223477125122, 'create_dept', '创建部门', 'bigint(20)', 'Long', 'createDept', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 7, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223502290953, 1661288223477125122, 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 8, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223502290954, 1661288223477125122, 'create_by', '上传人', 'bigint(20)', 'Long', 'createBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 9, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223502290955, 1661288223477125122, 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 10, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223502290956, 1661288223477125122, 'update_by', '更新人', 'bigint(20)', 'Long', 'updateBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 11, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223502290957, 1661288223477125122, 'service', '服务商', 'varchar(20)', 'String', 'service', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 12, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223611342850, 1661288223586177025, 'oss_config_id', '主建', 'bigint(20)', 'Long', 'ossConfigId', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223611342851, 1661288223586177025, 'tenant_id', '租户编号', 'varchar(20)', 'String', 'tenantId', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 2, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223611342852, 1661288223586177025, 'config_key', '配置key', 'varchar(20)', 'String', 'configKey', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 3, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223611342853, 1661288223586177025, 'access_key', 'accessKey', 'varchar(255)', 'String', 'accessKey', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 4, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223611342854, 1661288223586177025, 'secret_key', '秘钥', 'varchar(255)', 'String', 'secretKey', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 5, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223611342855, 1661288223586177025, 'bucket_name', '桶名称', 'varchar(255)', 'String', 'bucketName', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 6, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223611342856, 1661288223586177025, 'prefix', '前缀', 'varchar(255)', 'String', 'prefix', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 7, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223611342857, 1661288223586177025, 'endpoint', '访问站点', 'varchar(255)', 'String', 'endpoint', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 8, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223611342858, 1661288223586177025, 'domain', '自定义域名', 'varchar(255)', 'String', 'domain', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 9, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223611342859, 1661288223586177025, 'is_https', '是否https（Y=是,N=否）', 'char(1)', 'String', 'isHttps', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 10, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223611342860, 1661288223586177025, 'region', '域', 'varchar(255)', 'String', 'region', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 11, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223653285889, 1661288223586177025, 'access_policy', '桶权限类型(0=private 1=public 2=custom)', 'char(1)', 'String', 'accessPolicy', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 12, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223653285890, 1661288223586177025, 'status', '是否默认（0=是,1=否）', 'char(1)', 'String', 'status', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'radio', '', 13, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223653285891, 1661288223586177025, 'ext1', '扩展字段', 'varchar(255)', 'String', 'ext1', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 14, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223653285892, 1661288223586177025, 'create_dept', '创建部门', 'bigint(20)', 'Long', 'createDept', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 15, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223653285893, 1661288223586177025, 'create_by', '创建者', 'bigint(20)', 'Long', 'createBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 16, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223653285894, 1661288223586177025, 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 17, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223653285895, 1661288223586177025, 'update_by', '更新者', 'bigint(20)', 'Long', 'updateBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 18, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223653285896, 1661288223586177025, 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 19, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223653285897, 1661288223586177025, 'remark', '备注', 'varchar(500)', 'String', 'remark', '0', '0', '1', '1', '1', '1', NULL, 'EQ', 'textarea', '', 20, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223749754881, 1661288223728783361, 'post_id', '岗位ID', 'bigint(20)', 'Long', 'postId', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223749754882, 1661288223728783361, 'tenant_id', '租户编号', 'varchar(20)', 'String', 'tenantId', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 2, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223749754883, 1661288223728783361, 'post_code', '岗位编码', 'varchar(64)', 'String', 'postCode', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 3, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223749754884, 1661288223728783361, 'post_name', '岗位名称', 'varchar(50)', 'String', 'postName', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 4, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223749754885, 1661288223728783361, 'post_sort', '显示顺序', 'int(4)', 'Integer', 'postSort', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 5, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223749754886, 1661288223728783361, 'status', '状态（0正常 1停用）', 'char(1)', 'String', 'status', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'radio', '', 6, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223749754887, 1661288223728783361, 'create_dept', '创建部门', 'bigint(20)', 'Long', 'createDept', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 7, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223749754888, 1661288223728783361, 'create_by', '创建者', 'bigint(20)', 'Long', 'createBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 8, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223749754889, 1661288223728783361, 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 9, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223749754890, 1661288223728783361, 'update_by', '更新者', 'bigint(20)', 'Long', 'updateBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 10, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223749754891, 1661288223728783361, 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 11, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223749754892, 1661288223728783361, 'remark', '备注', 'varchar(500)', 'String', 'remark', '0', '0', '1', '1', '1', '1', NULL, 'EQ', 'textarea', '', 12, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223846223874, 1661288223821058050, 'role_id', '角色ID', 'bigint(20)', 'Long', 'roleId', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223846223875, 1661288223821058050, 'tenant_id', '租户编号', 'varchar(20)', 'String', 'tenantId', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 2, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223846223876, 1661288223821058050, 'role_name', '角色名称', 'varchar(30)', 'String', 'roleName', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 3, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223846223877, 1661288223821058050, 'role_key', '角色权限字符串', 'varchar(100)', 'String', 'roleKey', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 4, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223846223878, 1661288223821058050, 'role_sort', '显示顺序', 'int(4)', 'Integer', 'roleSort', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 5, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223846223879, 1661288223821058050, 'data_scope', '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）', 'char(1)', 'String', 'dataScope', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 6, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223846223880, 1661288223821058050, 'menu_check_strictly', '菜单树选择项是否关联显示', 'tinyint(1)', 'Integer', 'menuCheckStrictly', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 7, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223846223881, 1661288223821058050, 'dept_check_strictly', '部门树选择项是否关联显示', 'tinyint(1)', 'Integer', 'deptCheckStrictly', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 8, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223846223882, 1661288223821058050, 'status', '角色状态（0正常 1停用）', 'char(1)', 'String', 'status', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'radio', '', 9, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223846223883, 1661288223821058050, 'del_flag', '删除标志（0代表存在 2代表删除）', 'char(1)', 'String', 'delFlag', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 10, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223846223884, 1661288223821058050, 'create_dept', '创建部门', 'bigint(20)', 'Long', 'createDept', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 11, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223846223885, 1661288223821058050, 'create_by', '创建者', 'bigint(20)', 'Long', 'createBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 12, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223846223886, 1661288223821058050, 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 13, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223846223887, 1661288223821058050, 'update_by', '更新者', 'bigint(20)', 'Long', 'updateBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 14, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223846223888, 1661288223821058050, 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 15, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223846223889, 1661288223821058050, 'remark', '备注', 'varchar(500)', 'String', 'remark', '0', '0', '1', '1', '1', '1', NULL, 'EQ', 'textarea', '', 16, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223951081474, 1661288223925915650, 'user_id', '用户ID', 'bigint(20)', 'Long', 'userId', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223951081475, 1661288223925915650, 'post_id', '岗位ID', 'bigint(20)', 'Long', 'postId', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 2, 103, 1, '2023-05-24 16:29:12', 1, '2023-05-24 16:29:12');
INSERT INTO `gen_table_column` VALUES (1661288223993024514, 1661288223967858689, 'user_id', '用户ID', 'bigint(20)', 'Long', 'userId', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288223993024515, 1661288223967858689, 'role_id', '角色ID', 'bigint(20)', 'Long', 'roleId', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 2, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288224043356162, 1661288224013996034, 'id', '主键', 'bigint(20)', 'Long', 'id', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288224043356163, 1661288224013996034, 'tenant_id', '租户编号', 'varchar(20)', 'String', 'tenantId', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 2, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288224043356164, 1661288224013996034, 'dept_id', '部门id', 'bigint(20)', 'Long', 'deptId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 3, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288224043356165, 1661288224013996034, 'user_id', '用户id', 'bigint(20)', 'Long', 'userId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 4, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288224043356166, 1661288224013996034, 'order_num', '排序号', 'int(11)', 'Long', 'orderNum', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 5, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288224043356167, 1661288224013996034, 'test_key', 'key键', 'varchar(255)', 'String', 'testKey', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 6, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288224043356168, 1661288224013996034, 'value', '值', 'varchar(255)', 'String', 'value', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 7, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288224043356169, 1661288224013996034, 'version', '版本', 'int(11)', 'Long', 'version', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 8, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288224043356170, 1661288224013996034, 'create_dept', '创建部门', 'bigint(20)', 'Long', 'createDept', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 9, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288224043356171, 1661288224013996034, 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 10, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288224043356172, 1661288224013996034, 'create_by', '创建人', 'bigint(20)', 'Long', 'createBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 11, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288224043356173, 1661288224013996034, 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 12, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288224118853634, 1661288224013996034, 'update_by', '更新人', 'bigint(20)', 'Long', 'updateBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 13, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288224123047938, 1661288224013996034, 'del_flag', '删除标志', 'int(11)', 'Long', 'delFlag', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 14, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288224206934017, 1661288224185962497, 'id', '主键', 'bigint(20)', 'Long', 'id', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288224206934018, 1661288224185962497, 'tenant_id', '租户编号', 'varchar(20)', 'String', 'tenantId', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 2, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288224206934019, 1661288224185962497, 'parent_id', '父id', 'bigint(20)', 'Long', 'parentId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 3, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288224206934020, 1661288224185962497, 'dept_id', '部门id', 'bigint(20)', 'Long', 'deptId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 4, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288224206934021, 1661288224185962497, 'user_id', '用户id', 'bigint(20)', 'Long', 'userId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 5, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288224223711233, 1661288224185962497, 'tree_name', '值', 'varchar(255)', 'String', 'treeName', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 6, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288224223711234, 1661288224185962497, 'version', '版本', 'int(11)', 'Long', 'version', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 7, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288224223711235, 1661288224185962497, 'create_dept', '创建部门', 'bigint(20)', 'Long', 'createDept', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 8, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288224223711236, 1661288224185962497, 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 9, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288224223711237, 1661288224185962497, 'create_by', '创建人', 'bigint(20)', 'Long', 'createBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 10, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288224223711238, 1661288224185962497, 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 11, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288224223711239, 1661288224185962497, 'update_by', '更新人', 'bigint(20)', 'Long', 'updateBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 12, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288224223711240, 1661288224185962497, 'del_flag', '删除标志', 'int(11)', 'Long', 'delFlag', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 13, 103, 1, '2023-05-24 16:29:13', 1, '2023-05-24 16:29:13');
INSERT INTO `gen_table_column` VALUES (1661288385121406978, 1661288385096241154, 'config_id', '参数主键', 'bigint(20)', 'Long', 'configId', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, 103, 1, '2023-05-24 16:29:51', 1, '2023-05-24 16:29:51');
INSERT INTO `gen_table_column` VALUES (1661288385121406979, 1661288385096241154, 'tenant_id', '租户编号', 'varchar(20)', 'String', 'tenantId', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 2, 103, 1, '2023-05-24 16:29:51', 1, '2023-05-24 16:29:51');
INSERT INTO `gen_table_column` VALUES (1661288385121406980, 1661288385096241154, 'config_name', '参数名称', 'varchar(100)', 'String', 'configName', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 3, 103, 1, '2023-05-24 16:29:51', 1, '2023-05-24 16:29:51');
INSERT INTO `gen_table_column` VALUES (1661288385121406981, 1661288385096241154, 'config_key', '参数键名', 'varchar(100)', 'String', 'configKey', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 4, 103, 1, '2023-05-24 16:29:51', 1, '2023-05-24 16:29:51');
INSERT INTO `gen_table_column` VALUES (1661288385121406982, 1661288385096241154, 'config_value', '参数键值', 'varchar(500)', 'String', 'configValue', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'textarea', '', 5, 103, 1, '2023-05-24 16:29:51', 1, '2023-05-24 16:29:51');
INSERT INTO `gen_table_column` VALUES (1661288385121406983, 1661288385096241154, 'config_type', '系统内置（Y是 N否）', 'char(1)', 'String', 'configType', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'select', '', 6, 103, 1, '2023-05-24 16:29:51', 1, '2023-05-24 16:29:51');
INSERT INTO `gen_table_column` VALUES (1661288385121406984, 1661288385096241154, 'create_dept', '创建部门', 'bigint(20)', 'Long', 'createDept', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 7, 103, 1, '2023-05-24 16:29:51', 1, '2023-05-24 16:29:51');
INSERT INTO `gen_table_column` VALUES (1661288385142378498, 1661288385096241154, 'create_by', '创建者', 'bigint(20)', 'Long', 'createBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 8, 103, 1, '2023-05-24 16:29:51', 1, '2023-05-24 16:29:51');
INSERT INTO `gen_table_column` VALUES (1661288385142378499, 1661288385096241154, 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 9, 103, 1, '2023-05-24 16:29:51', 1, '2023-05-24 16:29:51');
INSERT INTO `gen_table_column` VALUES (1661288385142378500, 1661288385096241154, 'update_by', '更新者', 'bigint(20)', 'Long', 'updateBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 10, 103, 1, '2023-05-24 16:29:51', 1, '2023-05-24 16:29:51');
INSERT INTO `gen_table_column` VALUES (1661288385142378501, 1661288385096241154, 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 11, 103, 1, '2023-05-24 16:29:51', 1, '2023-05-24 16:29:51');
INSERT INTO `gen_table_column` VALUES (1661288385142378502, 1661288385096241154, 'remark', '备注', 'varchar(500)', 'String', 'remark', '0', '0', '1', '1', '1', '1', NULL, 'EQ', 'textarea', '', 12, 103, 1, '2023-05-24 16:29:51', 1, '2023-05-24 16:29:51');
INSERT INTO `gen_table_column` VALUES (1680196323806289921, 1680196323521077249, 'id', '文件id', 'bigint(20) unsigned', 'Long', 'id', '1', '1', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323806289922, 1680196323521077249, 'url', '文件访问地址', 'varchar(512)', 'String', 'url', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'textarea', '', 2, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323835650050, 1680196323445579778, 'id', '文件id', 'bigint(20) unsigned', 'Long', 'id', '1', '1', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323835650051, 1680196323445579778, 'url', '文件访问地址', 'varchar(512)', 'String', 'url', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'textarea', '', 2, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323835650052, 1680196323445579778, 'size', '文件大小，单位字节', 'bigint(20)', 'Long', 'size', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 3, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323873398785, 1680196323445579778, 'filename', '文件名称', 'varchar(256)', 'String', 'filename', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 4, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323873398786, 1680196323445579778, 'original_filename', '原始文件名', 'varchar(256)', 'String', 'originalFilename', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 5, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323873398787, 1680196323445579778, 'base_path', '基础存储路径', 'varchar(256)', 'String', 'basePath', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 6, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323873398788, 1680196323445579778, 'path', '存储路径', 'varchar(256)', 'String', 'path', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 7, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323873398789, 1680196323445579778, 'ext', '文件扩展名', 'varchar(32)', 'String', 'ext', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 8, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323873398790, 1680196323445579778, 'object_id', '文件所属对象id', 'varchar(32)', 'String', 'objectId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 9, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323873398791, 1680196323445579778, 'file_type', '文件类型', 'varchar(32)', 'String', 'fileType', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'select', '', 10, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323932119041, 1680196323445579778, 'attr', '附加属性', 'text', 'String', 'attr', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'textarea', '', 11, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323932119042, 1680196323445579778, 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 12, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323940507649, 1680196323445579778, 'create_by', '创建者', 'varchar(64)', 'String', 'createBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 13, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323940507650, 1680196323445579778, 'update_by', '更新者', 'varchar(64)', 'String', 'updateBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 14, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323940507651, 1680196323445579778, 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 15, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323940507652, 1680196323445579778, 'remark', '备注', 'varchar(500)', 'String', 'remark', '0', '0', '1', '1', '1', '1', NULL, 'EQ', 'textarea', '', 16, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323940507653, 1680196323445579778, 'version', '版本', 'int(11)', 'Long', 'version', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 17, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323940507654, 1680196323445579778, 'del_flag', '删除标志（0代表存在 1代表删除）', 'char(1)', 'String', 'delFlag', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 18, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323961479170, 1680196323521077249, 'size', '文件大小，单位字节', 'bigint(20)', 'Long', 'size', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 3, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323961479171, 1680196323521077249, 'filename', '文件名称', 'varchar(256)', 'String', 'filename', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 4, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323961479172, 1680196323521077249, 'original_filename', '原始文件名', 'varchar(256)', 'String', 'originalFilename', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 5, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323961479173, 1680196323521077249, 'base_path', '基础存储路径', 'varchar(256)', 'String', 'basePath', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 6, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323961479174, 1680196323521077249, 'path', '存储路径', 'varchar(256)', 'String', 'path', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 7, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323961479175, 1680196323521077249, 'ext', '文件扩展名', 'varchar(32)', 'String', 'ext', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 8, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323961479176, 1680196323521077249, 'object_id', '文件所属对象id', 'varchar(32)', 'String', 'objectId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 9, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323961479177, 1680196323521077249, 'file_type', '文件类型', 'varchar(32)', 'String', 'fileType', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'select', '', 10, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323961479178, 1680196323521077249, 'attr', '附加属性', 'text', 'String', 'attr', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'textarea', '', 11, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323999227905, 1680196323445579778, 'update_ip', '更新IP', 'varchar(128)', 'String', 'updateIp', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 19, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196323999227906, 1680196323445579778, 'tenant_id', '租户Id', 'bigint(20)', 'Long', 'tenantId', '0', '0', '1', NULL, NULL, NULL, NULL, 'EQ', 'input', '', 20, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196324020199425, 1680196323521077249, 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 12, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196324020199426, 1680196323521077249, 'create_by', '创建者', 'varchar(64)', 'String', 'createBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 13, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196324020199427, 1680196323521077249, 'update_by', '更新者', 'varchar(64)', 'String', 'updateBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 14, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196324020199428, 1680196323521077249, 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 15, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196324020199429, 1680196323521077249, 'remark', '备注', 'varchar(500)', 'String', 'remark', '0', '0', '1', '1', '1', '1', NULL, 'EQ', 'textarea', '', 16, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196324020199430, 1680196323521077249, 'version', '版本', 'int(11)', 'Long', 'version', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 17, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196324020199431, 1680196323521077249, 'del_flag', '删除标志（0代表存在 1代表删除）', 'char(1)', 'String', 'delFlag', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 18, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196324020199432, 1680196323521077249, 'update_ip', '更新IP', 'varchar(128)', 'String', 'updateIp', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 19, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680196324020199433, 1680196323521077249, 'tenant_id', '租户Id', 'bigint(20)', 'Long', 'tenantId', '0', '0', '1', NULL, NULL, NULL, NULL, 'EQ', 'input', '', 20, 103, 1, '2023-07-15 20:43:15', 1, '2023-07-15 20:43:15');
INSERT INTO `gen_table_column` VALUES (1680199147667853313, 1680199147407806465, 'id', '文件id', 'bigint(20) unsigned', 'Long', 'id', '1', '1', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, 103, 1, '2023-07-15 20:54:28', 1, '2023-07-15 20:54:28');
INSERT INTO `gen_table_column` VALUES (1680199147667853314, 1680199147407806465, 'url', '文件访问地址', 'varchar(512)', 'String', 'url', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'textarea', '', 2, 103, 1, '2023-07-15 20:54:28', 1, '2023-07-15 20:54:28');
INSERT INTO `gen_table_column` VALUES (1680199147667853315, 1680199147407806465, 'size', '文件大小，单位字节', 'bigint(20)', 'Long', 'size', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 3, 103, 1, '2023-07-15 20:54:28', 1, '2023-07-15 20:54:28');
INSERT INTO `gen_table_column` VALUES (1680199147667853316, 1680199147407806465, 'filename', '文件名称', 'varchar(256)', 'String', 'filename', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 4, 103, 1, '2023-07-15 20:54:28', 1, '2023-07-15 20:54:28');
INSERT INTO `gen_table_column` VALUES (1680199147667853317, 1680199147407806465, 'original_filename', '原始文件名', 'varchar(256)', 'String', 'originalFilename', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 5, 103, 1, '2023-07-15 20:54:28', 1, '2023-07-15 20:54:28');
INSERT INTO `gen_table_column` VALUES (1680199147667853318, 1680199147407806465, 'base_path', '基础存储路径', 'varchar(256)', 'String', 'basePath', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 6, 103, 1, '2023-07-15 20:54:28', 1, '2023-07-15 20:54:28');
INSERT INTO `gen_table_column` VALUES (1680199147734962178, 1680199147407806465, 'path', '存储路径', 'varchar(256)', 'String', 'path', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 7, 103, 1, '2023-07-15 20:54:28', 1, '2023-07-15 20:54:28');
INSERT INTO `gen_table_column` VALUES (1680199147734962179, 1680199147407806465, 'ext', '文件扩展名', 'varchar(32)', 'String', 'ext', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 8, 103, 1, '2023-07-15 20:54:28', 1, '2023-07-15 20:54:28');
INSERT INTO `gen_table_column` VALUES (1680199147734962180, 1680199147407806465, 'object_id', '文件所属对象id', 'varchar(32)', 'String', 'objectId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 9, 103, 1, '2023-07-15 20:54:28', 1, '2023-07-15 20:54:28');
INSERT INTO `gen_table_column` VALUES (1680199147734962181, 1680199147407806465, 'file_type', '文件类型', 'varchar(32)', 'String', 'fileType', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'select', '', 10, 103, 1, '2023-07-15 20:54:28', 1, '2023-07-15 20:54:28');
INSERT INTO `gen_table_column` VALUES (1680199147734962182, 1680199147407806465, 'attr', '附加属性', 'text', 'String', 'attr', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'textarea', '', 11, 103, 1, '2023-07-15 20:54:28', 1, '2023-07-15 20:54:28');
INSERT INTO `gen_table_column` VALUES (1680199147734962183, 1680199147407806465, 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 12, 103, 1, '2023-07-15 20:54:28', 1, '2023-07-15 20:54:28');
INSERT INTO `gen_table_column` VALUES (1680199147734962184, 1680199147407806465, 'create_by', '创建者', 'varchar(64)', 'String', 'createBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 13, 103, 1, '2023-07-15 20:54:28', 1, '2023-07-15 20:54:28');
INSERT INTO `gen_table_column` VALUES (1680199147734962185, 1680199147407806465, 'update_by', '更新者', 'varchar(64)', 'String', 'updateBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 14, 103, 1, '2023-07-15 20:54:28', 1, '2023-07-15 20:54:28');
INSERT INTO `gen_table_column` VALUES (1680199147734962186, 1680199147407806465, 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 15, 103, 1, '2023-07-15 20:54:28', 1, '2023-07-15 20:54:28');
INSERT INTO `gen_table_column` VALUES (1680199147734962187, 1680199147407806465, 'remark', '备注', 'varchar(500)', 'String', 'remark', '0', '0', '1', '1', '1', '1', NULL, 'EQ', 'textarea', '', 16, 103, 1, '2023-07-15 20:54:28', 1, '2023-07-15 20:54:28');
INSERT INTO `gen_table_column` VALUES (1680199147734962188, 1680199147407806465, 'version', '版本', 'int(11)', 'Long', 'version', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 17, 103, 1, '2023-07-15 20:54:28', 1, '2023-07-15 20:54:28');
INSERT INTO `gen_table_column` VALUES (1680199147734962189, 1680199147407806465, 'del_flag', '删除标志（0代表存在 1代表删除）', 'char(1)', 'String', 'delFlag', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 18, 103, 1, '2023-07-15 20:54:28', 1, '2023-07-15 20:54:28');
INSERT INTO `gen_table_column` VALUES (1680199147734962190, 1680199147407806465, 'update_ip', '更新IP', 'varchar(128)', 'String', 'updateIp', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 19, 103, 1, '2023-07-15 20:54:28', 1, '2023-07-15 20:54:28');
INSERT INTO `gen_table_column` VALUES (1680199147734962191, 1680199147407806465, 'tenant_id', '租户Id', 'bigint(20)', 'Long', 'tenantId', '0', '0', '1', NULL, NULL, NULL, NULL, 'EQ', 'input', '', 20, 103, 1, '2023-07-15 20:54:28', 1, '2023-07-15 20:54:28');
INSERT INTO `gen_table_column` VALUES (1680481753240215553, 1680481752850145282, 'id', 'id', 'bigint(20) unsigned', 'Long', 'id', '1', '1', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, 103, 1, '2023-07-16 15:37:26', 1, '2023-07-16 15:37:26');
INSERT INTO `gen_table_column` VALUES (1680481753240215554, 1680481752850145282, 'prompt', '描述词', 'text', 'String', 'prompt', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'textarea', '', 2, 103, 1, '2023-07-16 15:37:26', 1, '2023-07-16 15:37:26');
INSERT INTO `gen_table_column` VALUES (1680481753240215555, 1680481752850145282, 'negative_prompt', '负面词', 'text', 'String', 'negativePrompt', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'textarea', '', 3, 103, 1, '2023-07-16 15:37:26', 1, '2023-07-16 15:37:26');
INSERT INTO `gen_table_column` VALUES (1680481753240215556, 1680481752850145282, 'model_name', '模型名称', 'varchar(256)', 'String', 'modelName', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 4, 103, 1, '2023-07-16 15:37:26', 1, '2023-07-16 15:37:26');
INSERT INTO `gen_table_column` VALUES (1680481753240215557, 1680481752850145282, 'steps', '迭代步数', 'int(10)', 'Integer', 'steps', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 5, 103, 1, '2023-07-16 15:37:26', 1, '2023-07-16 15:37:26');
INSERT INTO `gen_table_column` VALUES (1680481753240215558, 1680481752850145282, 'seed', '种子', 'varchar(256)', 'String', 'seed', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 6, 103, 1, '2023-07-16 15:37:26', 1, '2023-07-16 15:37:26');
INSERT INTO `gen_table_column` VALUES (1680481753240215559, 1680481752850145282, 'width', '图片宽度', 'varchar(256)', 'String', 'width', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 7, 103, 1, '2023-07-16 15:37:26', 1, '2023-07-16 15:37:26');
INSERT INTO `gen_table_column` VALUES (1680481753240215560, 1680481752850145282, 'height', '图片高度', 'varchar(32)', 'String', 'height', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 8, 103, 1, '2023-07-16 15:37:26', 1, '2023-07-16 15:37:26');
INSERT INTO `gen_table_column` VALUES (1680481753240215561, 1680481752850145282, 'sampler_name', '采样方法', 'varchar(32)', 'String', 'samplerName', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 9, 103, 1, '2023-07-16 15:37:26', 1, '2023-07-16 15:37:26');
INSERT INTO `gen_table_column` VALUES (1680481753240215562, 1680481752850145282, 'create_dept', '创建部门', 'bigint(20)', 'Long', 'createDept', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 10, 103, 1, '2023-07-16 15:37:26', 1, '2023-07-16 15:37:26');
INSERT INTO `gen_table_column` VALUES (1680481753240215563, 1680481752850145282, 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 11, 103, 1, '2023-07-16 15:37:26', 1, '2023-07-16 15:37:26');
INSERT INTO `gen_table_column` VALUES (1680481753240215564, 1680481752850145282, 'create_by', '创建者', 'varchar(64)', 'String', 'createBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 12, 103, 1, '2023-07-16 15:37:26', 1, '2023-07-16 15:37:26');
INSERT INTO `gen_table_column` VALUES (1680481753240215565, 1680481752850145282, 'update_by', '更新者', 'varchar(64)', 'String', 'updateBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 13, 103, 1, '2023-07-16 15:37:26', 1, '2023-07-16 15:37:26');
INSERT INTO `gen_table_column` VALUES (1680481753240215566, 1680481752850145282, 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 14, 103, 1, '2023-07-16 15:37:26', 1, '2023-07-16 15:37:26');
INSERT INTO `gen_table_column` VALUES (1680481753240215567, 1680481752850145282, 'remark', '备注', 'varchar(500)', 'String', 'remark', '0', '0', '1', '1', '1', '1', NULL, 'EQ', 'textarea', '', 15, 103, 1, '2023-07-16 15:37:26', 1, '2023-07-16 15:37:26');
INSERT INTO `gen_table_column` VALUES (1680481753240215568, 1680481752850145282, 'version', '版本', 'int(11)', 'Long', 'version', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 16, 103, 1, '2023-07-16 15:37:26', 1, '2023-07-16 15:37:26');
INSERT INTO `gen_table_column` VALUES (1680481753240215569, 1680481752850145282, 'del_flag', '删除标志（0代表存在 1代表删除）', 'char(1)', 'String', 'delFlag', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 17, 103, 1, '2023-07-16 15:37:26', 1, '2023-07-16 15:37:26');
INSERT INTO `gen_table_column` VALUES (1680481753240215570, 1680481752850145282, 'update_ip', '更新IP', 'varchar(128)', 'String', 'updateIp', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 18, 103, 1, '2023-07-16 15:37:26', 1, '2023-07-16 15:37:26');
INSERT INTO `gen_table_column` VALUES (1680481753240215571, 1680481752850145282, 'tenant_id', '租户Id', 'bigint(20)', 'Long', 'tenantId', '0', '0', '1', NULL, NULL, NULL, NULL, 'EQ', 'input', '', 19, 103, 1, '2023-07-16 15:37:26', 1, '2023-07-16 15:37:26');
INSERT INTO `gen_table_column` VALUES (1728684655246950402, 1728684654923988993, 'id', '主键', 'bigint(20)', 'Long', 'id', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, 103, 1, '2023-11-26 15:58:34', 1, '2023-11-26 15:58:34');
INSERT INTO `gen_table_column` VALUES (1728684655246950403, 1728684654923988993, 'message_id', '消息 id', 'varchar(64)', 'String', 'messageId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 2, 103, 1, '2023-11-26 15:58:34', 1, '2023-11-26 15:58:34');
INSERT INTO `gen_table_column` VALUES (1728684655246950404, 1728684654923988993, 'parent_message_id', '父级消息 id', 'varchar(64)', 'String', 'parentMessageId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 3, 103, 1, '2023-11-26 15:58:34', 1, '2023-11-26 15:58:34');
INSERT INTO `gen_table_column` VALUES (1728684655246950405, 1728684654923988993, 'parent_answer_message_id', '父级回答消息 id', 'varchar(64)', 'String', 'parentAnswerMessageId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 4, 103, 1, '2023-11-26 15:58:34', 1, '2023-11-26 15:58:34');
INSERT INTO `gen_table_column` VALUES (1728684655246950406, 1728684654923988993, 'parent_question_message_id', '父级问题消息 id', 'varchar(64)', 'String', 'parentQuestionMessageId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 5, 103, 1, '2023-11-26 15:58:34', 1, '2023-11-26 15:58:34');
INSERT INTO `gen_table_column` VALUES (1728684655246950407, 1728684654923988993, 'context_count', '上下文数量', 'bigint(20)', 'Long', 'contextCount', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 6, 103, 1, '2023-11-26 15:58:34', 1, '2023-11-26 15:58:34');
INSERT INTO `gen_table_column` VALUES (1728684655246950408, 1728684654923988993, 'question_context_count', '问题上下文数量', 'bigint(20)', 'Long', 'questionContextCount', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 7, 103, 1, '2023-11-26 15:58:34', 1, '2023-11-26 15:58:34');
INSERT INTO `gen_table_column` VALUES (1728684655246950409, 1728684654923988993, 'message_type', '消息类型枚举', 'int(11)', 'Long', 'messageType', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'select', '', 8, 103, 1, '2023-11-26 15:58:34', 1, '2023-11-26 15:58:34');
INSERT INTO `gen_table_column` VALUES (1728684655246950410, 1728684654923988993, 'chat_room_id', '聊天室 id', 'bigint(20)', 'Long', 'chatRoomId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 9, 103, 1, '2023-11-26 15:58:34', 1, '2023-11-26 15:58:34');
INSERT INTO `gen_table_column` VALUES (1728684655246950411, 1728684654923988993, 'conversation_id', '对话 id', 'varchar(255)', 'String', 'conversationId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 10, 103, 1, '2023-11-26 15:58:34', 1, '2023-11-26 15:58:34');
INSERT INTO `gen_table_column` VALUES (1728684655246950412, 1728684654923988993, 'api_type', 'API 类型', 'varchar(20)', 'String', 'apiType', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'select', '', 11, 103, 1, '2023-11-26 15:58:34', 1, '2023-11-26 15:58:34');
INSERT INTO `gen_table_column` VALUES (1728684655246950413, 1728684654923988993, 'api_key', 'ApiKey', 'varchar(255)', 'String', 'apiKey', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 12, 103, 1, '2023-11-26 15:58:34', 1, '2023-11-26 15:58:34');
INSERT INTO `gen_table_column` VALUES (1728684655246950414, 1728684654923988993, 'content', '消息内容', 'varchar(5000)', 'String', 'content', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'editor', '', 13, 103, 1, '2023-11-26 15:58:34', 1, '2023-11-26 15:58:34');
INSERT INTO `gen_table_column` VALUES (1728684655246950415, 1728684654923988993, 'original_data', '消息的原始请求或响应数据', 'text', 'String', 'originalData', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'textarea', '', 14, 103, 1, '2023-11-26 15:58:34', 1, '2023-11-26 15:58:34');
INSERT INTO `gen_table_column` VALUES (1728684655246950416, 1728684654923988993, 'response_error_data', '错误的响应数据', 'text', 'String', 'responseErrorData', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'textarea', '', 15, 103, 1, '2023-11-26 15:58:34', 1, '2023-11-26 15:58:34');
INSERT INTO `gen_table_column` VALUES (1728684655246950417, 1728684654923988993, 'prompt_tokens', '输入消息的 tokens', 'bigint(20)', 'Long', 'promptTokens', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 16, 103, 1, '2023-11-26 15:58:34', 1, '2023-11-26 15:58:34');
INSERT INTO `gen_table_column` VALUES (1728684655246950418, 1728684654923988993, 'completion_tokens', '输出消息的 tokens', 'bigint(20)', 'Long', 'completionTokens', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 17, 103, 1, '2023-11-26 15:58:34', 1, '2023-11-26 15:58:34');
INSERT INTO `gen_table_column` VALUES (1728684655246950419, 1728684654923988993, 'total_tokens', '累计 Tokens', 'bigint(20)', 'Long', 'totalTokens', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 18, 103, 1, '2023-11-26 15:58:34', 1, '2023-11-26 15:58:34');
INSERT INTO `gen_table_column` VALUES (1728684655246950420, 1728684654923988993, 'model_name', '模型名称', 'varchar(255)', 'String', 'modelName', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 19, 103, 1, '2023-11-26 15:58:34', 1, '2023-11-26 15:58:34');
INSERT INTO `gen_table_column` VALUES (1728684655246950421, 1728684654923988993, 'status', '聊天记录状态', 'int(11)', 'Long', 'status', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'radio', '', 20, 103, 1, '2023-11-26 15:58:34', 1, '2023-11-26 15:58:34');
INSERT INTO `gen_table_column` VALUES (1728684655246950422, 1728684654923988993, 'is_hide', '是否隐藏 0 否 1 是', 'tinyint(4)', 'Integer', 'isHide', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 21, 103, 1, '2023-11-26 15:58:34', 1, '2023-11-26 15:58:34');
INSERT INTO `gen_table_column` VALUES (1728684655246950423, 1728684654923988993, 'create_dept', '创建部门', 'bigint(20)', 'Long', 'createDept', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 22, 103, 1, '2023-11-26 15:58:34', 1, '2023-11-26 15:58:34');
INSERT INTO `gen_table_column` VALUES (1728684655246950424, 1728684654923988993, 'create_by', '创建者', 'bigint(20)', 'Long', 'createBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 23, 103, 1, '2023-11-26 15:58:34', 1, '2023-11-26 15:58:34');
INSERT INTO `gen_table_column` VALUES (1728684655246950425, 1728684654923988993, 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 24, 103, 1, '2023-11-26 15:58:34', 1, '2023-11-26 15:58:34');
INSERT INTO `gen_table_column` VALUES (1728684655246950426, 1728684654923988993, 'update_by', '更新者', 'bigint(20)', 'Long', 'updateBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 25, 103, 1, '2023-11-26 15:58:34', 1, '2023-11-26 15:58:34');
INSERT INTO `gen_table_column` VALUES (1728684655246950427, 1728684654923988993, 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 26, 103, 1, '2023-11-26 15:58:34', 1, '2023-11-26 15:58:34');
INSERT INTO `gen_table_column` VALUES (1728684655246950428, 1728684654923988993, 'remark', '备注', 'varchar(500)', 'String', 'remark', '0', '0', '1', '1', '1', '1', NULL, 'EQ', 'textarea', '', 27, 103, 1, '2023-11-26 15:58:34', 1, '2023-11-26 15:58:34');
INSERT INTO `gen_table_column` VALUES (1740573615225053185, 1740573614897897473, 'id', '主键', 'int(11)', 'Long', 'id', '1', '1', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, 103, 1, '2023-12-29 11:21:03', 1, '2023-12-29 11:21:03');
INSERT INTO `gen_table_column` VALUES (1740573615225053186, 1740573614897897473, 'order_no', '订单编号', 'varchar(20)', 'String', 'orderNo', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 2, 103, 1, '2023-12-29 11:21:03', 1, '2023-12-29 11:21:03');
INSERT INTO `gen_table_column` VALUES (1740573615225053187, 1740573614897897473, 'order_name', '订单名称', 'varchar(100)', 'String', 'orderName', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 3, 103, 1, '2023-12-29 11:21:03', 1, '2023-12-29 11:21:03');
INSERT INTO `gen_table_column` VALUES (1740573615225053188, 1740573614897897473, 'amount', '金额', 'decimal(10,2)', 'BigDecimal', 'amount', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 4, 103, 1, '2023-12-29 11:21:03', 1, '2023-12-29 11:21:03');
INSERT INTO `gen_table_column` VALUES (1740573615225053189, 1740573614897897473, 'payment_status', '支付状态', 'char(1)', 'String', 'paymentStatus', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'radio', '', 5, 103, 1, '2023-12-29 11:21:03', 1, '2023-12-29 11:21:03');
INSERT INTO `gen_table_column` VALUES (1740573615225053190, 1740573614897897473, 'payment_method', '支付方式', 'char(1)', 'String', 'paymentMethod', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 6, 103, 1, '2023-12-29 11:21:03', 1, '2023-12-29 11:21:03');
INSERT INTO `gen_table_column` VALUES (1740573615225053191, 1740573614897897473, 'user_id', '用户ID', 'timestamp', 'Date', 'userId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'datetime', '', 7, 103, 1, '2023-12-29 11:21:03', 1, '2023-12-29 11:21:03');
INSERT INTO `gen_table_column` VALUES (1740573615225053192, 1740573614897897473, 'create_by', '创建者', 'bigint(20)', 'Long', 'createBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 8, 103, 1, '2023-12-29 11:21:03', 1, '2023-12-29 11:21:03');
INSERT INTO `gen_table_column` VALUES (1740573615225053193, 1740573614897897473, 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 9, 103, 1, '2023-12-29 11:21:03', 1, '2023-12-29 11:21:03');
INSERT INTO `gen_table_column` VALUES (1740573615225053194, 1740573614897897473, 'update_by', '更新者', 'bigint(20)', 'Long', 'updateBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 10, 103, 1, '2023-12-29 11:21:03', 1, '2023-12-29 11:21:03');
INSERT INTO `gen_table_column` VALUES (1740573615225053195, 1740573614897897473, 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 11, 103, 1, '2023-12-29 11:21:03', 1, '2023-12-29 11:21:03');
INSERT INTO `gen_table_column` VALUES (1740573615225053196, 1740573614897897473, 'remark', '备注', 'varchar(500)', 'String', 'remark', '0', '0', '1', '1', '1', '1', NULL, 'EQ', 'textarea', '', 12, 103, 1, '2023-12-29 11:21:03', 1, '2023-12-29 11:21:03');

-- ----------------------------
-- Table structure for knowledge
-- ----------------------------
DROP TABLE IF EXISTS `knowledge`;
CREATE TABLE `knowledge`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `kid` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '知识库ID',
  `uid` bigint NOT NULL DEFAULT 0 COMMENT '用户ID',
  `kname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '知识库名称',
  `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '描述',
  `create_time` datetime NULL DEFAULT NULL,
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_kid`(`kid` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识库' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of knowledge
-- ----------------------------

-- ----------------------------
-- Table structure for knowledge_attach
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_attach`;
CREATE TABLE `knowledge_attach`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `kid` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '知识库ID',
  `doc_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文档ID',
  `doc_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文档名称',
  `doc_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文档类型',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '文档内容',
  `create_time` datetime NULL DEFAULT NULL,
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_kname`(`kid` ASC, `doc_name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识库附件' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of knowledge_attach
-- ----------------------------

-- ----------------------------
-- Table structure for knowledge_fragment
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_fragment`;
CREATE TABLE `knowledge_fragment`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `kid` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '知识库ID',
  `doc_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文档ID',
  `fid` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '知识片段ID',
  `idx` int NOT NULL COMMENT '片段索引下标',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文档内容',
  `create_time` datetime NULL DEFAULT NULL,
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识片段' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of knowledge_fragment
-- ----------------------------

-- ----------------------------
-- Table structure for knowledge_share
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_share`;
CREATE TABLE `knowledge_share`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `kid` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '知识库ID',
  `uid` int NOT NULL DEFAULT 0 COMMENT '用户ID',
  `kname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '知识库名称',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识库分享表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of knowledge_share
-- ----------------------------

-- ----------------------------
-- Table structure for media_file
-- ----------------------------
DROP TABLE IF EXISTS `media_file`;
CREATE TABLE `media_file`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `cid` int NOT NULL DEFAULT 0 COMMENT '对话ID',
  `mfid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '媒体文件ID',
  `media_type` int NOT NULL COMMENT '媒体类型：1视频，2音频，3图片',
  `file_suffix` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件后缀',
  `file_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件名',
  `file_size` bigint NOT NULL COMMENT '文件大小(单位字节)',
  `file_time` bigint NULL DEFAULT NULL COMMENT '时长(单位秒)',
  `file_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件路径',
  `http_url` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'http全路径',
  `create_time` datetime NULL DEFAULT NULL,
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '媒体文件' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of media_file
-- ----------------------------

-- ----------------------------
-- Table structure for mj_room
-- ----------------------------
DROP TABLE IF EXISTS `mj_room`;
CREATE TABLE `mj_room`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户ID',
  `mj_guild_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'MJ服务器ID',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建日期',
  `expiration_date` datetime NULL DEFAULT NULL COMMENT '截止有效期',
  `enabled_local_sensitive_word` int NULL DEFAULT 0 COMMENT '是否启用本地敏感词过滤，0：不启用；1：启用',
  `enabled_baidu_aip` int NULL DEFAULT 0 COMMENT '是否启用百度内容审核，0：不启用；1：启用',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'MJ创作会话' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of mj_room
-- ----------------------------

-- ----------------------------
-- Table structure for mj_room_msg
-- ----------------------------
DROP TABLE IF EXISTS `mj_room_msg`;
CREATE TABLE `mj_room_msg`  (
  `id` bigint NOT NULL COMMENT '主键',
  `mj_room_id` bigint NOT NULL COMMENT '会话 id',
  `guild_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '服务器ID',
  `user_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户 id',
  `type` tinyint NOT NULL COMMENT '消息类型',
  `prompt` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户输入',
  `final_prompt` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最终的输入',
  `response_content` varchar(5000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '响应内容',
  `action` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '指令动作',
  `compressed_image_name` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '压缩图名称',
  `original_image_name` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '原图名称',
  `uv_parent_id` bigint NULL DEFAULT NULL COMMENT 'uv 指令的父消息 id',
  `u_use_bit` int NULL DEFAULT NULL COMMENT 'u 指令使用比特位',
  `uv_index` tinyint(1) NULL DEFAULT NULL COMMENT 'uv 位置',
  `status` tinyint NOT NULL COMMENT '状态',
  `discord_finish_time` datetime NULL DEFAULT NULL COMMENT 'discord 结束时间',
  `discord_start_time` datetime NULL DEFAULT NULL COMMENT 'discord 开始时间',
  `discord_message_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'discord 消息 id',
  `discord_channel_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'discord 频道 id',
  `discord_image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'discord 图片地址',
  `template_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '模板ID',
  `failure_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '失败原因',
  `is_deleted` tinyint(1) NOT NULL COMMENT '是否删除 0 否 1 是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'Midjourney 创作记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of mj_room_msg
-- ----------------------------

-- ----------------------------
-- Table structure for mj_server
-- ----------------------------
DROP TABLE IF EXISTS `mj_server`;
CREATE TABLE `mj_server`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `guild_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '服务器ID',
  `channel_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '频道ID',
  `bot_token` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '机器人token',
  `bot_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `user_token` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户token',
  `user_agent` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `max_execute_queue_size` int NULL DEFAULT NULL COMMENT '执行队列最大长度',
  `max_file_size` int NULL DEFAULT NULL COMMENT '最大文件大小',
  `max_wait_queue_size` int NULL DEFAULT NULL COMMENT '等待队列最大长度',
  `status` int NULL DEFAULT NULL COMMENT '状态：0：禁用，1：可用',
  `discord_api_url` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'Discord Api Url',
  `discord_upload_url` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'discordUploadUrl',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'MJ服务器信息配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of mj_server
-- ----------------------------

-- ----------------------------
-- Table structure for payment_orders
-- ----------------------------
DROP TABLE IF EXISTS `payment_orders`;
CREATE TABLE `payment_orders`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '订单编号',
  `order_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '订单名称',
  `amount` decimal(10, 2) NOT NULL COMMENT '金额',
  `payment_status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '支付状态',
  `payment_method` char(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '支付方式',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1743813012090322946 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '支付订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of payment_orders
-- ----------------------------

-- ----------------------------
-- Table structure for sd_model_param
-- ----------------------------
DROP TABLE IF EXISTS `sd_model_param`;
CREATE TABLE `sd_model_param`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'id',
  `prompt` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '描述词',
  `negative_prompt` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL COMMENT '负面词',
  `model_name` varchar(256) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '模型名称',
  `steps` int NULL DEFAULT NULL COMMENT '迭代步数',
  `seed` varchar(256) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '种子',
  `width` varchar(256) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '图片宽度',
  `height` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '图片高度',
  `sampler_name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '采样方法',
  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `version` int NULL DEFAULT NULL COMMENT '版本',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `update_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新IP',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户Id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '模型参数信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sd_model_param
-- ----------------------------
INSERT INTO `sd_model_param` VALUES (1, '', '(((simple background))),monochrome ,lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, fewer digits, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username, blurry, lowres, bad anatomy, bad hands, text, error, extra digit, fewer digits, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username, blurry, ugly,pregnant,vore,duplicate,morbid,mut ilated,tran nsexual, hermaphrodite,long neck,mutated hands,poorly drawn hands,poorly drawn face,mutation,deformed,blurry,bad anatomy,bad proportions,malformed limbs,extra limbs,cloned face,disfigured,gross proportions, (((missing arms))),(((missing legs))), (((extra arms))),(((extra legs))),pubic hair, plump,bad legs,error legs,username,blurry,bad feet', '国风3 GuoFeng3_v3.4.safetensors [a83e25fe5b]', 30, '-1', '768', '1024', 'DPM++ SDE Karras', NULL, NULL, '', '', NULL, NULL, NULL, '0', NULL, 0);

-- ----------------------------
-- Table structure for sensitive_word
-- ----------------------------
DROP TABLE IF EXISTS `sensitive_word`;
CREATE TABLE `sensitive_word`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `word` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '敏感词内容',
  `status` int NOT NULL COMMENT '状态 1 启用 2 停用',
  `is_deleted` int NULL DEFAULT 0 COMMENT '是否删除 0 否 NULL 是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '敏感词表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sensitive_word
-- ----------------------------

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `config_id` bigint NOT NULL COMMENT '参数主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '000000' COMMENT '租户编号',
  `config_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '参数键值',
  `config_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`config_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '参数配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config` VALUES (1, '000000', '主框架页-默认皮肤样式名称', 'sys.index.skinName', 'skin-blue', 'Y', 103, 1, '2023-05-14 15:19:42', NULL, NULL, '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow');
INSERT INTO `sys_config` VALUES (2, '000000', '用户管理-账号初始密码', 'sys.user.initPassword', '123456', 'Y', 103, 1, '2023-05-14 15:19:42', NULL, NULL, '初始化密码 123456');
INSERT INTO `sys_config` VALUES (3, '000000', '主框架页-侧边栏主题', 'sys.index.sideTheme', 'theme-dark', 'Y', 103, 1, '2023-05-14 15:19:42', NULL, NULL, '深色主题theme-dark，浅色主题theme-light');
INSERT INTO `sys_config` VALUES (5, '000000', '账号自助-是否开启用户注册功能', 'sys.account.registerUser', 'false', 'Y', 103, 1, '2023-05-14 15:19:42', NULL, NULL, '是否开启注册用户功能（true开启，false关闭）');
INSERT INTO `sys_config` VALUES (11, '000000', 'OSS预览列表资源开关', 'sys.oss.previewListResource', 'true', 'Y', 103, 1, '2023-05-14 15:19:42', NULL, NULL, 'true:开启, false:关闭');
INSERT INTO `sys_config` VALUES (1729685495520854018, '911866', '主框架页-默认皮肤样式名称', 'sys.index.skinName', 'skin-blue', 'Y', 103, 1, '2023-05-14 15:19:42', 1, '2023-05-14 15:19:42', '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow');
INSERT INTO `sys_config` VALUES (1729685495520854019, '911866', '用户管理-账号初始密码', 'sys.user.initPassword', '123456', 'Y', 103, 1, '2023-05-14 15:19:42', 1, '2023-05-14 15:19:42', '初始化密码 123456');
INSERT INTO `sys_config` VALUES (1729685495520854020, '911866', '主框架页-侧边栏主题', 'sys.index.sideTheme', 'theme-dark', 'Y', 103, 1, '2023-05-14 15:19:42', 1, '2023-05-14 15:19:42', '深色主题theme-dark，浅色主题theme-light');
INSERT INTO `sys_config` VALUES (1729685495583768578, '911866', '账号自助-是否开启用户注册功能', 'sys.account.registerUser', 'false', 'Y', 103, 1, '2023-05-14 15:19:42', 1, '2023-05-14 15:19:42', '是否开启注册用户功能（true开启，false关闭）');
INSERT INTO `sys_config` VALUES (1729685495583768579, '911866', 'OSS预览列表资源开关', 'sys.oss.previewListResource', 'true', 'Y', 103, 1, '2023-05-14 15:19:42', 1, '2023-05-14 15:19:42', 'true:开启, false:关闭');

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`  (
  `dept_id` bigint NOT NULL COMMENT '部门id',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '000000' COMMENT '租户编号',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父部门id',
  `ancestors` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '部门名称',
  `order_num` int NULL DEFAULT 0 COMMENT '显示顺序',
  `leader` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '负责人',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '联系电话',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '邮箱',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`dept_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '部门表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
INSERT INTO `sys_dept` VALUES (100, '000000', 0, '0', '熊猫科技', 0, 'ageerle', '15888888888', 'ageerle@163.com', '0', '0', 103, 1, '2023-05-14 15:19:39', 1, '2023-12-29 11:18:24');
INSERT INTO `sys_dept` VALUES (101, '000000', 100, '0,100', '深圳总公司', 1, '疯狂的狮子Li', '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2023-05-14 15:19:39', NULL, NULL);
INSERT INTO `sys_dept` VALUES (102, '000000', 100, '0,100', '长沙分公司', 2, '疯狂的狮子Li', '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2023-05-14 15:19:39', NULL, NULL);
INSERT INTO `sys_dept` VALUES (103, '000000', 101, '0,100,101', '研发部门', 1, '疯狂的狮子Li', '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2023-05-14 15:19:39', NULL, NULL);
INSERT INTO `sys_dept` VALUES (104, '000000', 101, '0,100,101', '市场部门', 2, '疯狂的狮子Li', '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2023-05-14 15:19:39', NULL, NULL);
INSERT INTO `sys_dept` VALUES (105, '000000', 101, '0,100,101', '测试部门', 3, '疯狂的狮子Li', '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2023-05-14 15:19:39', NULL, NULL);
INSERT INTO `sys_dept` VALUES (106, '000000', 101, '0,100,101', '财务部门', 4, '疯狂的狮子Li', '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2023-05-14 15:19:39', NULL, NULL);
INSERT INTO `sys_dept` VALUES (107, '000000', 101, '0,100,101', '运维部门', 5, '疯狂的狮子Li', '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2023-05-14 15:19:39', NULL, NULL);
INSERT INTO `sys_dept` VALUES (108, '000000', 102, '0,100,102', '市场部门', 1, '疯狂的狮子Li', '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2023-05-14 15:19:39', NULL, NULL);
INSERT INTO `sys_dept` VALUES (109, '000000', 102, '0,100,102', '财务部门', 2, '疯狂的狮子Li', '15888888888', 'xxx@qq.com', '0', '0', 103, 1, '2023-05-14 15:19:39', NULL, NULL);
INSERT INTO `sys_dept` VALUES (1729685491964084226, '911866', 0, '0', '5126', 0, 'admin', NULL, NULL, '0', '2', 103, 1, '2023-11-29 10:15:32', 1, '2023-11-29 10:15:32');

-- ----------------------------
-- Table structure for sys_dict_data
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data`  (
  `dict_code` bigint NOT NULL COMMENT '字典编码',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '000000' COMMENT '租户编号',
  `dict_sort` int NULL DEFAULT 0 COMMENT '字典排序',
  `dict_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
  `list_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典数据表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dict_data
-- ----------------------------
INSERT INTO `sys_dict_data` VALUES (1, '000000', 1, '男', '0', 'sys_user_sex', '', '', 'Y', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '性别男');
INSERT INTO `sys_dict_data` VALUES (2, '000000', 2, '女', '1', 'sys_user_sex', '', '', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '性别女');
INSERT INTO `sys_dict_data` VALUES (3, '000000', 3, '未知', '2', 'sys_user_sex', '', '', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '性别未知');
INSERT INTO `sys_dict_data` VALUES (4, '000000', 1, '显示', '0', 'sys_show_hide', '', 'primary', 'Y', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '显示菜单');
INSERT INTO `sys_dict_data` VALUES (5, '000000', 2, '隐藏', '1', 'sys_show_hide', '', 'danger', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '隐藏菜单');
INSERT INTO `sys_dict_data` VALUES (6, '000000', 1, '正常', '0', 'sys_normal_disable', '', 'primary', 'Y', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '正常状态');
INSERT INTO `sys_dict_data` VALUES (7, '000000', 2, '停用', '1', 'sys_normal_disable', '', 'danger', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '停用状态');
INSERT INTO `sys_dict_data` VALUES (12, '000000', 1, '是', 'Y', 'sys_yes_no', '', 'primary', 'Y', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '系统默认是');
INSERT INTO `sys_dict_data` VALUES (13, '000000', 2, '否', 'N', 'sys_yes_no', '', 'danger', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '系统默认否');
INSERT INTO `sys_dict_data` VALUES (14, '000000', 1, '通知', '1', 'sys_notice_type', '', 'warning', 'Y', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '通知');
INSERT INTO `sys_dict_data` VALUES (15, '000000', 2, '公告', '2', 'sys_notice_type', '', 'success', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '公告');
INSERT INTO `sys_dict_data` VALUES (16, '000000', 1, '正常', '0', 'sys_notice_status', '', 'primary', 'Y', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '正常状态');
INSERT INTO `sys_dict_data` VALUES (17, '000000', 2, '关闭', '1', 'sys_notice_status', '', 'danger', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '关闭状态');
INSERT INTO `sys_dict_data` VALUES (18, '000000', 1, '新增', '1', 'sys_oper_type', '', 'info', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '新增操作');
INSERT INTO `sys_dict_data` VALUES (19, '000000', 2, '修改', '2', 'sys_oper_type', '', 'info', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '修改操作');
INSERT INTO `sys_dict_data` VALUES (20, '000000', 3, '删除', '3', 'sys_oper_type', '', 'danger', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '删除操作');
INSERT INTO `sys_dict_data` VALUES (21, '000000', 4, '授权', '4', 'sys_oper_type', '', 'primary', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '授权操作');
INSERT INTO `sys_dict_data` VALUES (22, '000000', 5, '导出', '5', 'sys_oper_type', '', 'warning', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '导出操作');
INSERT INTO `sys_dict_data` VALUES (23, '000000', 6, '导入', '6', 'sys_oper_type', '', 'warning', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '导入操作');
INSERT INTO `sys_dict_data` VALUES (24, '000000', 7, '强退', '7', 'sys_oper_type', '', 'danger', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '强退操作');
INSERT INTO `sys_dict_data` VALUES (25, '000000', 8, '生成代码', '8', 'sys_oper_type', '', 'warning', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '生成操作');
INSERT INTO `sys_dict_data` VALUES (26, '000000', 9, '清空数据', '9', 'sys_oper_type', '', 'danger', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '清空操作');
INSERT INTO `sys_dict_data` VALUES (27, '000000', 1, '成功', '0', 'sys_common_status', '', 'primary', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '正常状态');
INSERT INTO `sys_dict_data` VALUES (28, '000000', 2, '失败', '1', 'sys_common_status', '', 'danger', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '停用状态');
INSERT INTO `sys_dict_data` VALUES (29, '000000', 99, '其他', '0', 'sys_oper_type', '', 'info', 'N', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '其他操作');
INSERT INTO `sys_dict_data` VALUES (1729685494870736897, '911866', 1, '男', '0', 'sys_user_sex', '', '', 'Y', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '性别男');
INSERT INTO `sys_dict_data` VALUES (1729685494870736898, '911866', 2, '女', '1', 'sys_user_sex', '', '', 'N', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '性别女');
INSERT INTO `sys_dict_data` VALUES (1729685494870736899, '911866', 3, '未知', '2', 'sys_user_sex', '', '', 'N', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '性别未知');
INSERT INTO `sys_dict_data` VALUES (1729685494870736900, '911866', 1, '显示', '0', 'sys_show_hide', '', 'primary', 'Y', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '显示菜单');
INSERT INTO `sys_dict_data` VALUES (1729685494870736901, '911866', 2, '隐藏', '1', 'sys_show_hide', '', 'danger', 'N', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '隐藏菜单');
INSERT INTO `sys_dict_data` VALUES (1729685494870736902, '911866', 1, '正常', '0', 'sys_normal_disable', '', 'primary', 'Y', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '正常状态');
INSERT INTO `sys_dict_data` VALUES (1729685494870736903, '911866', 2, '停用', '1', 'sys_normal_disable', '', 'danger', 'N', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '停用状态');
INSERT INTO `sys_dict_data` VALUES (1729685494870736904, '911866', 1, '是', 'Y', 'sys_yes_no', '', 'primary', 'Y', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '系统默认是');
INSERT INTO `sys_dict_data` VALUES (1729685494937845761, '911866', 2, '否', 'N', 'sys_yes_no', '', 'danger', 'N', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '系统默认否');
INSERT INTO `sys_dict_data` VALUES (1729685494937845762, '911866', 1, '通知', '1', 'sys_notice_type', '', 'warning', 'Y', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '通知');
INSERT INTO `sys_dict_data` VALUES (1729685494937845763, '911866', 2, '公告', '2', 'sys_notice_type', '', 'success', 'N', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '公告');
INSERT INTO `sys_dict_data` VALUES (1729685494937845764, '911866', 1, '正常', '0', 'sys_notice_status', '', 'primary', 'Y', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '正常状态');
INSERT INTO `sys_dict_data` VALUES (1729685494937845765, '911866', 2, '关闭', '1', 'sys_notice_status', '', 'danger', 'N', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '关闭状态');
INSERT INTO `sys_dict_data` VALUES (1729685494937845766, '911866', 1, '新增', '1', 'sys_oper_type', '', 'info', 'N', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '新增操作');
INSERT INTO `sys_dict_data` VALUES (1729685494937845767, '911866', 2, '修改', '2', 'sys_oper_type', '', 'info', 'N', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '修改操作');
INSERT INTO `sys_dict_data` VALUES (1729685494937845768, '911866', 3, '删除', '3', 'sys_oper_type', '', 'danger', 'N', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '删除操作');
INSERT INTO `sys_dict_data` VALUES (1729685494937845769, '911866', 4, '授权', '4', 'sys_oper_type', '', 'primary', 'N', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '授权操作');
INSERT INTO `sys_dict_data` VALUES (1729685494937845770, '911866', 5, '导出', '5', 'sys_oper_type', '', 'warning', 'N', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '导出操作');
INSERT INTO `sys_dict_data` VALUES (1729685494937845771, '911866', 6, '导入', '6', 'sys_oper_type', '', 'warning', 'N', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '导入操作');
INSERT INTO `sys_dict_data` VALUES (1729685494937845772, '911866', 7, '强退', '7', 'sys_oper_type', '', 'danger', 'N', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '强退操作');
INSERT INTO `sys_dict_data` VALUES (1729685494937845773, '911866', 8, '生成代码', '8', 'sys_oper_type', '', 'warning', 'N', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '生成操作');
INSERT INTO `sys_dict_data` VALUES (1729685494937845774, '911866', 9, '清空数据', '9', 'sys_oper_type', '', 'danger', 'N', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '清空操作');
INSERT INTO `sys_dict_data` VALUES (1729685494937845775, '911866', 1, '成功', '0', 'sys_common_status', '', 'primary', 'N', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '正常状态');
INSERT INTO `sys_dict_data` VALUES (1729685494937845776, '911866', 2, '失败', '1', 'sys_common_status', '', 'danger', 'N', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '停用状态');
INSERT INTO `sys_dict_data` VALUES (1729685494937845777, '911866', 99, '其他', '0', 'sys_oper_type', '', 'info', 'N', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '其他操作');

-- ----------------------------
-- Table structure for sys_dict_type
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type`  (
  `dict_id` bigint NOT NULL COMMENT '字典主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '000000' COMMENT '租户编号',
  `dict_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '字典名称',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '字典类型',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_id`) USING BTREE,
  UNIQUE INDEX `tenant_id`(`tenant_id` ASC, `dict_type` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典类型表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dict_type
-- ----------------------------
INSERT INTO `sys_dict_type` VALUES (1, '000000', '用户性别', 'sys_user_sex', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '用户性别列表');
INSERT INTO `sys_dict_type` VALUES (2, '000000', '菜单状态', 'sys_show_hide', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '菜单状态列表');
INSERT INTO `sys_dict_type` VALUES (3, '000000', '系统开关', 'sys_normal_disable', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '系统开关列表');
INSERT INTO `sys_dict_type` VALUES (6, '000000', '系统是否', 'sys_yes_no', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '系统是否列表');
INSERT INTO `sys_dict_type` VALUES (7, '000000', '通知类型', 'sys_notice_type', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '通知类型列表');
INSERT INTO `sys_dict_type` VALUES (8, '000000', '通知状态', 'sys_notice_status', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '通知状态列表');
INSERT INTO `sys_dict_type` VALUES (9, '000000', '操作类型', 'sys_oper_type', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '操作类型列表');
INSERT INTO `sys_dict_type` VALUES (10, '000000', '系统状态', 'sys_common_status', '0', 103, 1, '2023-05-14 15:19:41', NULL, NULL, '登录状态列表');
INSERT INTO `sys_dict_type` VALUES (1729685494468083714, '911866', '用户性别', 'sys_user_sex', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '用户性别列表');
INSERT INTO `sys_dict_type` VALUES (1729685494468083715, '911866', '菜单状态', 'sys_show_hide', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '菜单状态列表');
INSERT INTO `sys_dict_type` VALUES (1729685494468083716, '911866', '系统开关', 'sys_normal_disable', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '系统开关列表');
INSERT INTO `sys_dict_type` VALUES (1729685494468083717, '911866', '系统是否', 'sys_yes_no', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '系统是否列表');
INSERT INTO `sys_dict_type` VALUES (1729685494468083718, '911866', '通知类型', 'sys_notice_type', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '通知类型列表');
INSERT INTO `sys_dict_type` VALUES (1729685494468083719, '911866', '通知状态', 'sys_notice_status', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '通知状态列表');
INSERT INTO `sys_dict_type` VALUES (1729685494468083720, '911866', '操作类型', 'sys_oper_type', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '操作类型列表');
INSERT INTO `sys_dict_type` VALUES (1729685494468083721, '911866', '系统状态', 'sys_common_status', '0', 103, 1, '2023-05-14 15:19:41', 1, '2023-05-14 15:19:41', '登录状态列表');

-- ----------------------------
-- Table structure for sys_file_info
-- ----------------------------
DROP TABLE IF EXISTS `sys_file_info`;
CREATE TABLE `sys_file_info`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '文件id',
  `url` varchar(512) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '文件访问地址',
  `size` bigint NULL DEFAULT NULL COMMENT '文件大小，单位字节',
  `filename` varchar(256) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '文件名称',
  `original_filename` varchar(256) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '原始文件名',
  `base_path` varchar(256) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '基础存储路径',
  `path` varchar(256) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '存储路径',
  `ext` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '文件扩展名',
  `user_id` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '文件所属用户',
  `file_type` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '文件类型',
  `attr` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL COMMENT '附加属性',
  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '创建者',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `version` int NULL DEFAULT NULL COMMENT '版本',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `update_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新IP',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户Id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1688133688718106627 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '文件记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_file_info
-- ----------------------------

-- ----------------------------
-- Table structure for sys_logininfor
-- ----------------------------
DROP TABLE IF EXISTS `sys_logininfor`;
CREATE TABLE `sys_logininfor`  (
  `info_id` bigint NOT NULL COMMENT '访问ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '000000' COMMENT '租户编号',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '用户账号',
  `ipaddr` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '登录IP地址',
  `login_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '登录地点',
  `browser` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '浏览器类型',
  `os` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '操作系统',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
  `msg` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '提示消息',
  `login_time` datetime NULL DEFAULT NULL COMMENT '访问时间',
  PRIMARY KEY (`info_id`) USING BTREE,
  INDEX `idx_sys_logininfor_s`(`status` ASC) USING BTREE,
  INDEX `idx_sys_logininfor_lt`(`login_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统访问记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_logininfor
-- ----------------------------

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜单权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (1, '系统管理', 0, 1, 'system', NULL, '', 1, 0, 'M', '0', '0', '', 'system', 103, 1, '2023-05-14 15:19:39', NULL, NULL, '系统管理目录');
INSERT INTO `sys_menu` VALUES (2, '系统监控', 0, 3, 'monitor', NULL, '', 1, 0, 'M', '0', '0', '', 'monitor', 103, 1, '2023-05-14 15:19:39', NULL, NULL, '系统监控目录');
INSERT INTO `sys_menu` VALUES (6, '租户管理', 0, 2, 'tenant', NULL, '', 1, 0, 'M', '0', '0', '', 'chart', 103, 1, '2023-05-14 15:19:39', NULL, NULL, '租户管理目录');
INSERT INTO `sys_menu` VALUES (100, '用户管理', 1, 1, 'user', 'system/user/index', '', 1, 0, 'C', '0', '0', 'system:user:list', 'user', 103, 1, '2023-05-14 15:19:39', NULL, NULL, '用户管理菜单');
INSERT INTO `sys_menu` VALUES (101, '角色管理', 1, 2, 'role', 'system/role/index', '', 1, 0, 'C', '0', '0', 'system:role:list', 'peoples', 103, 1, '2023-05-14 15:19:39', NULL, NULL, '角色管理菜单');
INSERT INTO `sys_menu` VALUES (102, '菜单管理', 1, 3, 'menu', 'system/menu/index', '', 1, 0, 'C', '0', '0', 'system:menu:list', 'tree-table', 103, 1, '2023-05-14 15:19:39', NULL, NULL, '菜单管理菜单');
INSERT INTO `sys_menu` VALUES (103, '部门管理', 1, 4, 'dept', 'system/dept/index', '', 1, 0, 'C', '0', '0', 'system:dept:list', 'tree', 103, 1, '2023-05-14 15:19:39', NULL, NULL, '部门管理菜单');
INSERT INTO `sys_menu` VALUES (104, '岗位管理', 1, 5, 'post', 'system/post/index', '', 1, 0, 'C', '0', '0', 'system:post:list', 'post', 103, 1, '2023-05-14 15:19:39', NULL, NULL, '岗位管理菜单');
INSERT INTO `sys_menu` VALUES (105, '字典管理', 1, 6, 'dict', 'system/dict/index', '', 1, 0, 'C', '0', '0', 'system:dict:list', 'dict', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '字典管理菜单');
INSERT INTO `sys_menu` VALUES (106, '参数设置', 1, 7, 'config', 'system/config/index', '', 1, 0, 'C', '0', '0', 'system:config:list', 'edit', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '参数设置菜单');
INSERT INTO `sys_menu` VALUES (107, '通知公告', 1, 8, 'notice', 'system/notice/index', '', 1, 0, 'C', '0', '0', 'system:notice:list', 'message', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '通知公告菜单');
INSERT INTO `sys_menu` VALUES (108, '日志管理', 1, 9, 'log', '', '', 1, 0, 'M', '0', '0', '', 'log', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '日志管理菜单');
INSERT INTO `sys_menu` VALUES (109, '在线用户', 2, 1, 'online', 'monitor/online/index', '', 1, 0, 'C', '0', '0', 'monitor:online:list', 'online', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '在线用户菜单');
INSERT INTO `sys_menu` VALUES (113, '缓存监控', 2, 5, 'cache', 'monitor/cache/index', '', 1, 0, 'C', '0', '0', 'monitor:cache:list', 'redis', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '缓存监控菜单');
INSERT INTO `sys_menu` VALUES (115, '代码生成', 1, 2, 'gen', 'tool/gen/index', '', 1, 0, 'C', '0', '0', 'tool:gen:list', 'code', 103, 1, '2023-05-14 15:19:40', 1, '2023-12-29 11:33:01', '代码生成菜单');
INSERT INTO `sys_menu` VALUES (118, '文件管理', 1, 10, 'oss', 'system/oss/index', '', 1, 0, 'C', '0', '0', 'system:oss:list', 'upload', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '文件管理菜单');
INSERT INTO `sys_menu` VALUES (121, '租户管理', 6, 1, 'tenant', 'system/tenant/index', '', 1, 0, 'C', '0', '0', 'system:tenant:list', 'list', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '租户管理菜单');
INSERT INTO `sys_menu` VALUES (122, '租户套餐管理', 6, 2, 'tenantPackage', 'system/tenantPackage/index', '', 1, 0, 'C', '0', '0', 'system:tenantPackage:list', 'form', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '租户套餐管理菜单');
INSERT INTO `sys_menu` VALUES (500, '操作日志', 108, 1, 'operlog', 'monitor/operlog/index', '', 1, 0, 'C', '0', '0', 'monitor:operlog:list', 'form', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '操作日志菜单');
INSERT INTO `sys_menu` VALUES (501, '登录日志', 108, 2, 'logininfor', 'monitor/logininfor/index', '', 1, 0, 'C', '0', '0', 'monitor:logininfor:list', 'logininfor', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '登录日志菜单');
INSERT INTO `sys_menu` VALUES (1001, '用户查询', 100, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:user:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1002, '用户新增', 100, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:user:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1003, '用户修改', 100, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:user:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1004, '用户删除', 100, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:user:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1005, '用户导出', 100, 5, '', '', '', 1, 0, 'F', '0', '0', 'system:user:export', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1006, '用户导入', 100, 6, '', '', '', 1, 0, 'F', '0', '0', 'system:user:import', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1007, '重置密码', 100, 7, '', '', '', 1, 0, 'F', '0', '0', 'system:user:resetPwd', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1008, '角色查询', 101, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:role:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1009, '角色新增', 101, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:role:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1010, '角色修改', 101, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:role:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1011, '角色删除', 101, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:role:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1012, '角色导出', 101, 5, '', '', '', 1, 0, 'F', '0', '0', 'system:role:export', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1013, '菜单查询', 102, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1014, '菜单新增', 102, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1015, '菜单修改', 102, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1016, '菜单删除', 102, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1017, '部门查询', 103, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1018, '部门新增', 103, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1019, '部门修改', 103, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1020, '部门删除', 103, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1021, '岗位查询', 104, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:post:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1022, '岗位新增', 104, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:post:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1023, '岗位修改', 104, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:post:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1024, '岗位删除', 104, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:post:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1025, '岗位导出', 104, 5, '', '', '', 1, 0, 'F', '0', '0', 'system:post:export', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1026, '字典查询', 105, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1027, '字典新增', 105, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1028, '字典修改', 105, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1029, '字典删除', 105, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1030, '字典导出', 105, 5, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:export', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1031, '参数查询', 106, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1032, '参数新增', 106, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1033, '参数修改', 106, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1034, '参数删除', 106, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1035, '参数导出', 106, 5, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:export', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1036, '公告查询', 107, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1037, '公告新增', 107, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1038, '公告修改', 107, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1039, '公告删除', 107, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1040, '操作查询', 500, 1, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1041, '操作删除', 500, 2, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1042, '日志导出', 500, 4, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:export', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1043, '登录查询', 501, 1, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1044, '登录删除', 501, 2, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1045, '日志导出', 501, 3, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:export', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1046, '在线查询', 109, 1, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:online:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1047, '批量强退', 109, 2, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:online:batchLogout', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1048, '单条强退', 109, 3, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:online:forceLogout', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1050, '账户解锁', 501, 4, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:unlock', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1055, '生成查询', 115, 1, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1056, '生成修改', 115, 2, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1057, '生成删除', 115, 3, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1058, '导入代码', 115, 2, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:import', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1059, '预览代码', 115, 4, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:preview', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1060, '生成代码', 115, 5, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:code', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1600, '文件查询', 118, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1601, '文件上传', 118, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:upload', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1602, '文件下载', 118, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:download', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1603, '文件删除', 118, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1604, '配置添加', 118, 5, '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1605, '配置编辑', 118, 6, '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1606, '租户查询', 121, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenant:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1607, '租户新增', 121, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenant:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1608, '租户修改', 121, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenant:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1609, '租户删除', 121, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenant:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1610, '租户导出', 121, 5, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenant:export', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1611, '租户套餐查询', 122, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenantPackage:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1612, '租户套餐新增', 122, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenantPackage:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1613, '租户套餐修改', 122, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenantPackage:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1614, '租户套餐删除', 122, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenantPackage:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu` VALUES (1615, '租户套餐导出', 122, 5, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenantPackage:export', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');

-- ----------------------------
-- Table structure for sys_model
-- ----------------------------
DROP TABLE IF EXISTS `sys_model`;
CREATE TABLE `sys_model`  (
  `id` int NOT NULL,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模型名称',
  `describe` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '描述',
  `local` bit(1) NULL DEFAULT b'0' COMMENT '是否本地模型',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像',
  `free` bit(1) NULL DEFAULT b'0' COMMENT '是否免费',
  `std_rate` decimal(10, 8) NULL DEFAULT NULL COMMENT '标准token比',
  `role_text` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '角色设定',
  `temperature` decimal(10, 2) NULL DEFAULT NULL COMMENT '模型默认温度',
  `top_p` int NULL DEFAULT NULL COMMENT '默认结果',
  `create_time` datetime NULL DEFAULT NULL,
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统模型' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_model
-- ----------------------------

-- ----------------------------
-- Table structure for sys_notice
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice`  (
  `notice_id` bigint NOT NULL COMMENT '公告ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '000000' COMMENT '租户编号',
  `notice_title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告标题',
  `notice_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告类型（1通知 2公告）',
  `notice_content` longblob NULL COMMENT '公告内容',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '公告状态（0正常 1关闭）',
  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`notice_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '通知公告表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_notice
-- ----------------------------
INSERT INTO `sys_notice` VALUES (1, '000000', '温馨提醒：2018-07-01 新版本发布啦', '2', 0xE696B0E78988E69CACE58685E5AEB9, '0', 103, 1, '2023-05-14 15:19:42', NULL, NULL, '管理员');
INSERT INTO `sys_notice` VALUES (2, '000000', '维护通知：2018-07-01 系统凌晨维护', '1', 0xE7BBB4E68AA4E58685E5AEB9, '0', 103, 1, '2023-05-14 15:19:42', NULL, NULL, '管理员');

-- ----------------------------
-- Table structure for sys_oper_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log`  (
  `oper_id` bigint NOT NULL COMMENT '日志主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '000000' COMMENT '租户编号',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '模块标题',
  `business_type` int NULL DEFAULT 0 COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '请求方式',
  `operator_type` int NULL DEFAULT 0 COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  `oper_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '操作人员',
  `dept_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '部门名称',
  `oper_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '请求URL',
  `oper_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '主机地址',
  `oper_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '操作地点',
  `oper_param` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '请求参数',
  `json_result` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '返回参数',
  `status` int NULL DEFAULT 0 COMMENT '操作状态（0正常 1异常）',
  `error_msg` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '错误消息',
  `oper_time` datetime NULL DEFAULT NULL COMMENT '操作时间',
  `cost_time` bigint NULL DEFAULT 0 COMMENT '消耗时间',
  PRIMARY KEY (`oper_id`) USING BTREE,
  INDEX `idx_sys_oper_log_bt`(`business_type` ASC) USING BTREE,
  INDEX `idx_sys_oper_log_s`(`status` ASC) USING BTREE,
  INDEX `idx_sys_oper_log_ot`(`oper_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '操作日志记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_oper_log
-- ----------------------------

-- ----------------------------
-- Table structure for sys_oss
-- ----------------------------
DROP TABLE IF EXISTS `sys_oss`;
CREATE TABLE `sys_oss`  (
  `oss_id` bigint NOT NULL COMMENT '对象存储主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '000000' COMMENT '租户编号',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '文件名',
  `original_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '原名',
  `file_suffix` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '文件后缀名',
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'URL地址',
  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '上传人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `service` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'minio' COMMENT '服务商',
  PRIMARY KEY (`oss_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'OSS对象存储表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_oss
-- ----------------------------

-- ----------------------------
-- Table structure for sys_oss_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_oss_config`;
CREATE TABLE `sys_oss_config`  (
  `oss_config_id` bigint NOT NULL COMMENT '主建',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '000000' COMMENT '租户编号',
  `config_key` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '配置key',
  `access_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT 'accessKey',
  `secret_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '秘钥',
  `bucket_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '桶名称',
  `prefix` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '前缀',
  `endpoint` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '访问站点',
  `domain` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '自定义域名',
  `is_https` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'N' COMMENT '是否https（Y=是,N=否）',
  `region` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '域',
  `access_policy` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '1' COMMENT '桶权限类型(0=private 1=public 2=custom)',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '1' COMMENT '是否默认（0=是,1=否）',
  `ext1` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '扩展字段',
  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`oss_config_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '对象存储配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_oss_config
-- ----------------------------
INSERT INTO `sys_oss_config` VALUES (1, '000000', 'minio', 'ruoyi', 'ruoyi123', 'ruoyi', '', '127.0.0.1:9000', '', 'N', '', '1', '1', '', 103, 1, '2023-05-14 15:19:42', 1, '2023-07-13 23:28:18', NULL);
INSERT INTO `sys_oss_config` VALUES (2, '000000', 'qiniu', 'XXXXXXXXXXXXXXX', 'XXXXXXXXXXXXXXX', 'ruoyi', '', 's3-cn-north-1.qiniucs.com', '', 'N', '', '1', '1', '', 103, 1, '2023-05-14 15:19:42', 1, '2023-05-14 15:19:42', NULL);
INSERT INTO `sys_oss_config` VALUES (3, '000000', 'aliyun', 'XXXXXXXXXXXXXXX', 'XXXXXXXXXXXXXXX', 'ruoyi', '', 'oss-cn-beijing.aliyuncs.com', '', 'N', '', '1', '1', '', 103, 1, '2023-05-14 15:19:42', 1, '2023-07-13 23:35:23', NULL);
INSERT INTO `sys_oss_config` VALUES (4, '000000', 'qcloud', 'XXXXXXXXXXXXXXX', 'XXXXXXXXXXXXXXX', 'ruoyi', 'image', '127.0.0.1:9000', '', 'N', '', '1', '0', '', 103, 1, '2023-05-14 15:19:42', 1, '2023-11-13 23:58:09', '');
INSERT INTO `sys_oss_config` VALUES (5, '000000', 'image', 'ruoyi', 'ruoyi123', 'ruoyi', 'image', '127.0.0.1:9000', '', 'N', '', '1', '1', '', 103, 1, '2023-05-14 15:19:42', 1, '2023-05-14 15:19:42', NULL);

-- ----------------------------
-- Table structure for sys_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post`  (
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '000000' COMMENT '租户编号',
  `post_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '岗位编码',
  `post_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '岗位名称',
  `post_sort` int NOT NULL COMMENT '显示顺序',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '状态（0正常 1停用）',
  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`post_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '岗位信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_post
-- ----------------------------
INSERT INTO `sys_post` VALUES (1, '000000', 'ceo', '董事长', 1, '0', 103, 1, '2023-05-14 15:19:39', NULL, NULL, '');
INSERT INTO `sys_post` VALUES (2, '000000', 'se', '项目经理', 2, '0', 103, 1, '2023-05-14 15:19:39', NULL, NULL, '');
INSERT INTO `sys_post` VALUES (3, '000000', 'hr', '人力资源', 3, '0', 103, 1, '2023-05-14 15:19:39', NULL, NULL, '');
INSERT INTO `sys_post` VALUES (4, '000000', 'user', '普通员工', 4, '0', 103, 1, '2023-05-14 15:19:39', NULL, NULL, '');

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '000000' COMMENT '租户编号',
  `role_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色权限字符串',
  `role_sort` int NOT NULL COMMENT '显示顺序',
  `data_scope` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
  `menu_check_strictly` tinyint(1) NULL DEFAULT 1 COMMENT '菜单树选择项是否关联显示',
  `dept_check_strictly` tinyint(1) NULL DEFAULT 1 COMMENT '部门树选择项是否关联显示',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '000000', '超级管理员', 'superadmin', 1, '1', 1, 1, '0', '0', 103, 1, '2023-05-14 15:19:39', NULL, NULL, '超级管理员');
INSERT INTO `sys_role` VALUES (2, '000000', '普通角色', 'common', 2, '2', 1, 1, '0', '0', 103, 1, '2023-05-14 15:19:39', NULL, NULL, '普通角色');
INSERT INTO `sys_role` VALUES (3, '000000', '本部门及以下', 'test1', 3, '4', 1, 1, '0', '0', 103, 1, '2023-05-14 15:20:00', 1, '2023-06-04 10:20:43', NULL);
INSERT INTO `sys_role` VALUES (4, '000000', '仅本人', 'test2', 4, '5', 1, 1, '0', '0', 103, 1, '2023-05-14 15:20:00', 1, '2023-06-04 10:21:01', NULL);
INSERT INTO `sys_role` VALUES (1661661183933177857, '000000', '小程序管理员', 'xcxadmin', 1, '1', 1, 1, '0', '0', 103, 1, '2023-05-25 17:11:13', 1, '2023-05-25 17:11:13', '');
INSERT INTO `sys_role` VALUES (1729685491108446210, '911866', '管理员', 'admin', 1, '1', 1, 1, '0', '0', 103, 1, '2023-11-29 10:15:32', 1, '2023-11-29 10:15:32', NULL);

-- ----------------------------
-- Table structure for sys_role_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept`  (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`role_id`, `dept_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色和部门关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_dept
-- ----------------------------
INSERT INTO `sys_role_dept` VALUES (2, 100);
INSERT INTO `sys_role_dept` VALUES (2, 101);
INSERT INTO `sys_role_dept` VALUES (2, 105);
INSERT INTO `sys_role_dept` VALUES (1729685491108446210, 1729685491964084226);

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`, `menu_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色和菜单关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (2, 1);
INSERT INTO `sys_role_menu` VALUES (2, 2);
INSERT INTO `sys_role_menu` VALUES (2, 3);
INSERT INTO `sys_role_menu` VALUES (2, 4);
INSERT INTO `sys_role_menu` VALUES (2, 100);
INSERT INTO `sys_role_menu` VALUES (2, 101);
INSERT INTO `sys_role_menu` VALUES (2, 102);
INSERT INTO `sys_role_menu` VALUES (2, 103);
INSERT INTO `sys_role_menu` VALUES (2, 104);
INSERT INTO `sys_role_menu` VALUES (2, 105);
INSERT INTO `sys_role_menu` VALUES (2, 106);
INSERT INTO `sys_role_menu` VALUES (2, 107);
INSERT INTO `sys_role_menu` VALUES (2, 108);
INSERT INTO `sys_role_menu` VALUES (2, 109);
INSERT INTO `sys_role_menu` VALUES (2, 110);
INSERT INTO `sys_role_menu` VALUES (2, 111);
INSERT INTO `sys_role_menu` VALUES (2, 112);
INSERT INTO `sys_role_menu` VALUES (2, 113);
INSERT INTO `sys_role_menu` VALUES (2, 114);
INSERT INTO `sys_role_menu` VALUES (2, 115);
INSERT INTO `sys_role_menu` VALUES (2, 116);
INSERT INTO `sys_role_menu` VALUES (2, 500);
INSERT INTO `sys_role_menu` VALUES (2, 501);
INSERT INTO `sys_role_menu` VALUES (2, 1000);
INSERT INTO `sys_role_menu` VALUES (2, 1001);
INSERT INTO `sys_role_menu` VALUES (2, 1002);
INSERT INTO `sys_role_menu` VALUES (2, 1003);
INSERT INTO `sys_role_menu` VALUES (2, 1004);
INSERT INTO `sys_role_menu` VALUES (2, 1005);
INSERT INTO `sys_role_menu` VALUES (2, 1006);
INSERT INTO `sys_role_menu` VALUES (2, 1007);
INSERT INTO `sys_role_menu` VALUES (2, 1008);
INSERT INTO `sys_role_menu` VALUES (2, 1009);
INSERT INTO `sys_role_menu` VALUES (2, 1010);
INSERT INTO `sys_role_menu` VALUES (2, 1011);
INSERT INTO `sys_role_menu` VALUES (2, 1012);
INSERT INTO `sys_role_menu` VALUES (2, 1013);
INSERT INTO `sys_role_menu` VALUES (2, 1014);
INSERT INTO `sys_role_menu` VALUES (2, 1015);
INSERT INTO `sys_role_menu` VALUES (2, 1016);
INSERT INTO `sys_role_menu` VALUES (2, 1017);
INSERT INTO `sys_role_menu` VALUES (2, 1018);
INSERT INTO `sys_role_menu` VALUES (2, 1019);
INSERT INTO `sys_role_menu` VALUES (2, 1020);
INSERT INTO `sys_role_menu` VALUES (2, 1021);
INSERT INTO `sys_role_menu` VALUES (2, 1022);
INSERT INTO `sys_role_menu` VALUES (2, 1023);
INSERT INTO `sys_role_menu` VALUES (2, 1024);
INSERT INTO `sys_role_menu` VALUES (2, 1025);
INSERT INTO `sys_role_menu` VALUES (2, 1026);
INSERT INTO `sys_role_menu` VALUES (2, 1027);
INSERT INTO `sys_role_menu` VALUES (2, 1028);
INSERT INTO `sys_role_menu` VALUES (2, 1029);
INSERT INTO `sys_role_menu` VALUES (2, 1030);
INSERT INTO `sys_role_menu` VALUES (2, 1031);
INSERT INTO `sys_role_menu` VALUES (2, 1032);
INSERT INTO `sys_role_menu` VALUES (2, 1033);
INSERT INTO `sys_role_menu` VALUES (2, 1034);
INSERT INTO `sys_role_menu` VALUES (2, 1035);
INSERT INTO `sys_role_menu` VALUES (2, 1036);
INSERT INTO `sys_role_menu` VALUES (2, 1037);
INSERT INTO `sys_role_menu` VALUES (2, 1038);
INSERT INTO `sys_role_menu` VALUES (2, 1039);
INSERT INTO `sys_role_menu` VALUES (2, 1040);
INSERT INTO `sys_role_menu` VALUES (2, 1041);
INSERT INTO `sys_role_menu` VALUES (2, 1042);
INSERT INTO `sys_role_menu` VALUES (2, 1043);
INSERT INTO `sys_role_menu` VALUES (2, 1044);
INSERT INTO `sys_role_menu` VALUES (2, 1045);
INSERT INTO `sys_role_menu` VALUES (2, 1046);
INSERT INTO `sys_role_menu` VALUES (2, 1047);
INSERT INTO `sys_role_menu` VALUES (2, 1048);
INSERT INTO `sys_role_menu` VALUES (2, 1050);
INSERT INTO `sys_role_menu` VALUES (2, 1055);
INSERT INTO `sys_role_menu` VALUES (2, 1056);
INSERT INTO `sys_role_menu` VALUES (2, 1057);
INSERT INTO `sys_role_menu` VALUES (2, 1058);
INSERT INTO `sys_role_menu` VALUES (2, 1059);
INSERT INTO `sys_role_menu` VALUES (2, 1060);
INSERT INTO `sys_role_menu` VALUES (3, 1);
INSERT INTO `sys_role_menu` VALUES (3, 100);
INSERT INTO `sys_role_menu` VALUES (3, 101);
INSERT INTO `sys_role_menu` VALUES (3, 102);
INSERT INTO `sys_role_menu` VALUES (3, 103);
INSERT INTO `sys_role_menu` VALUES (3, 104);
INSERT INTO `sys_role_menu` VALUES (3, 105);
INSERT INTO `sys_role_menu` VALUES (3, 106);
INSERT INTO `sys_role_menu` VALUES (3, 107);
INSERT INTO `sys_role_menu` VALUES (3, 108);
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
INSERT INTO `sys_role_menu` VALUES (1661661183933177857, 1);
INSERT INTO `sys_role_menu` VALUES (1661661183933177857, 100);
INSERT INTO `sys_role_menu` VALUES (1661661183933177857, 107);
INSERT INTO `sys_role_menu` VALUES (1661661183933177857, 1001);
INSERT INTO `sys_role_menu` VALUES (1661661183933177857, 1002);
INSERT INTO `sys_role_menu` VALUES (1661661183933177857, 1003);
INSERT INTO `sys_role_menu` VALUES (1661661183933177857, 1004);
INSERT INTO `sys_role_menu` VALUES (1661661183933177857, 1005);
INSERT INTO `sys_role_menu` VALUES (1661661183933177857, 1006);
INSERT INTO `sys_role_menu` VALUES (1661661183933177857, 1007);
INSERT INTO `sys_role_menu` VALUES (1661661183933177857, 1036);
INSERT INTO `sys_role_menu` VALUES (1661661183933177857, 1037);
INSERT INTO `sys_role_menu` VALUES (1661661183933177857, 1038);
INSERT INTO `sys_role_menu` VALUES (1661661183933177857, 1039);
INSERT INTO `sys_role_menu` VALUES (1729685491108446210, 1689201668374556674);
INSERT INTO `sys_role_menu` VALUES (1729685491108446210, 1689205943360188417);
INSERT INTO `sys_role_menu` VALUES (1729685491108446210, 1689243465037561858);
INSERT INTO `sys_role_menu` VALUES (1729685491108446210, 1689243466220355585);

-- ----------------------------
-- Table structure for sys_tenant
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant`;
CREATE TABLE `sys_tenant`  (
  `id` bigint NOT NULL COMMENT 'id',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编号',
  `contact_user_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '联系电话',
  `company_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '企业名称',
  `license_number` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '统一社会信用代码',
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '地址',
  `intro` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '企业简介',
  `domain` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '域名',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `package_id` bigint NULL DEFAULT NULL COMMENT '租户套餐编号',
  `expire_time` datetime NULL DEFAULT NULL COMMENT '过期时间',
  `account_count` int NULL DEFAULT -1 COMMENT '用户数量（-1不限制）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '租户状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '租户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_tenant
-- ----------------------------
INSERT INTO `sys_tenant` VALUES (1, '000000', '管理组', '15888888888', 'XXX有限公司', NULL, NULL, '多租户通用后台管理管理系统', NULL, NULL, NULL, NULL, -1, '0', '0', 103, 1, '2023-05-14 15:19:39', NULL, NULL);

-- ----------------------------
-- Table structure for sys_tenant_package
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant_package`;
CREATE TABLE `sys_tenant_package`  (
  `package_id` bigint NOT NULL COMMENT '租户套餐id',
  `package_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '套餐名称',
  `menu_ids` varchar(3000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '关联菜单id',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `menu_check_strictly` tinyint(1) NULL DEFAULT 1 COMMENT '菜单树选择项是否关联显示',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`package_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '租户套餐表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_tenant_package
-- ----------------------------
INSERT INTO `sys_tenant_package` VALUES (1729685389795033090, '绘画', '1689205943360188417, 1689243466220355585, 1689201668374556674, 1689243465037561858', '', 1, '0', '2', 103, 1, '2023-11-29 10:15:08', 1, '2023-11-29 10:15:08');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `open_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '微信用户标识',
  `user_grade` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '用户等级',
  `user_balance` double(20, 2) NULL DEFAULT 0.00 COMMENT '账户余额',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '000000' COMMENT '租户编号',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '部门ID',
  `user_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户账号',
  `nick_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户昵称',
  `user_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'sys_user' COMMENT '用户类型（sys_user系统用户）',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '用户邮箱',
  `phonenumber` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '手机号码',
  `sex` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像地址',
  `wx_avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '微信头像地址',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '密码',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `login_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, NULL, '0', 4.00, '00000', 103, 'admin', '疯狂的狮子Li', 'sys_user', 'crazyLionLi@163.com', '15888888888', '1', NULL, NULL, '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '0:0:0:0:0:0:0:1', '2024-03-19 17:57:07', 103, 1, '2023-05-14 15:19:39', 1, '2024-03-19 17:57:07', '管理员');

-- ----------------------------
-- Table structure for sys_user_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_post`;
CREATE TABLE `sys_user_post`  (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`user_id`, `post_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户与岗位关联表' ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户和角色关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (1, 1);

-- ----------------------------
-- Table structure for test_demo
-- ----------------------------
DROP TABLE IF EXISTS `test_demo`;
CREATE TABLE `test_demo`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '000000' COMMENT '租户编号',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '部门id',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户id',
  `order_num` int NULL DEFAULT 0 COMMENT '排序号',
  `test_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'key键',
  `value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '值',
  `version` int NULL DEFAULT 0 COMMENT '版本',
  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `del_flag` int NULL DEFAULT 0 COMMENT '删除标志',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '测试单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of test_demo
-- ----------------------------

-- ----------------------------
-- Table structure for test_tree
-- ----------------------------
DROP TABLE IF EXISTS `test_tree`;
CREATE TABLE `test_tree`  (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '000000' COMMENT '租户编号',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父id',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '部门id',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户id',
  `tree_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '值',
  `version` int NULL DEFAULT 0 COMMENT '版本',
  `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新人',
  `del_flag` int NULL DEFAULT 0 COMMENT '删除标志',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '测试树表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of test_tree
-- ----------------------------

-- ----------------------------
-- Table structure for voice_role
-- ----------------------------
DROP TABLE IF EXISTS `voice_role`;
CREATE TABLE `voice_role`  (
  `id` bigint NOT NULL COMMENT 'id',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '角色描述',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像',
  `voice_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '角色id',
  `file_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '音频地址',
  `pre_process` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '音频预处理（实验性）',
  `create_dept` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `voiceId`(`voice_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '配音角色' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of voice_role
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
