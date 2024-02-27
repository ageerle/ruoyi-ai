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

 Date: 10/01/2024 11:33:28
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

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
INSERT INTO `sys_file_info` VALUES (1680497615330447362, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680497611488464896.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 16:40:28', NULL, NULL, '2023-07-16 16:40:28', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680498392497229826, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680498388747522048.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 16:43:34', NULL, NULL, '2023-07-16 16:43:34', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680498798468108290, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680498794991030272.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 16:45:10', NULL, NULL, '2023-07-16 16:45:10', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680499270151147522, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680499266577600512.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 16:47:03', NULL, NULL, '2023-07-16 16:47:03', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680501243424362498, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680501239389442048.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 16:54:53', NULL, NULL, '2023-07-16 16:54:53', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680504215915024386, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680504212333088768.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 17:06:42', NULL, NULL, '2023-07-16 17:06:42', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680504359825788929, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680504356621340672.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 17:07:16', NULL, NULL, '2023-07-16 17:07:16', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680504461810290689, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680504458815557632.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 17:07:41', NULL, NULL, '2023-07-16 17:07:41', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680504563446665217, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680504560128970752.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 17:08:05', NULL, NULL, '2023-07-16 17:08:05', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680504666311970817, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680504662826504192.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 17:08:29', NULL, NULL, '2023-07-16 17:08:29', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680504771475755009, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680504767772184576.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 17:08:54', NULL, NULL, '2023-07-16 17:08:54', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680504875007954945, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680504871493128192.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 17:09:19', NULL, NULL, '2023-07-16 17:09:19', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680504979622285314, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680504976069709824.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 17:09:44', NULL, NULL, '2023-07-16 17:09:44', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680505086774169602, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680505083313868800.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 17:10:10', NULL, NULL, '2023-07-16 17:10:10', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680505190952292354, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680505187441659904.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 17:10:34', NULL, NULL, '2023-07-16 17:10:34', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680505293607882754, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680505290164359168.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 17:10:59', NULL, NULL, '2023-07-16 17:10:59', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680505399979626497, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680505396498354176.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 17:11:24', NULL, NULL, '2023-07-16 17:11:24', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680505503469883394, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680505500106051584.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 17:11:49', NULL, NULL, '2023-07-16 17:11:49', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680505903468072962, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680505900011966464.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 17:13:24', NULL, NULL, '2023-07-16 17:13:24', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680506303004889090, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680506299167100928.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 17:15:00', NULL, NULL, '2023-07-16 17:15:00', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680506406075715586, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680506402477002752.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 17:15:24', NULL, NULL, '2023-07-16 17:15:24', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680506507569483778, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680506504318898176.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 17:15:48', NULL, NULL, '2023-07-16 17:15:48', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680506608983560193, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680506605502287872.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 17:16:13', NULL, NULL, '2023-07-16 17:16:13', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680506908322648066, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680506904853958656.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 17:17:24', NULL, NULL, '2023-07-16 17:17:24', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680507008256135169, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680507004808417280.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 17:17:48', NULL, NULL, '2023-07-16 17:17:48', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680507147406364674, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680507143727960064.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 17:18:21', NULL, NULL, '2023-07-16 17:18:21', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680507359759781889, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680507356144291840.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 17:19:12', NULL, NULL, '2023-07-16 17:19:12', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680507970840514561, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680507967422156800.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 17:21:37', NULL, NULL, '2023-07-16 17:21:37', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680526836295618562, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680526832839512064.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 18:36:35', NULL, NULL, '2023-07-16 18:36:35', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680526937252515842, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680526933968375808.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 18:36:59', NULL, NULL, '2023-07-16 18:36:59', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680527038914056193, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680527035281788928.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 18:37:23', NULL, NULL, '2023-07-16 18:37:23', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1680527140101640194, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-07-16/1680527136674893824.png', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'sd', NULL, NULL, '2023-07-16 18:37:48', NULL, NULL, '2023-07-16 18:37:48', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1688105795313041409, '/StableDiffusion/2023-08-06/1688105795258515456.png', NULL, NULL, NULL, NULL, NULL, NULL, '1', 'sd', NULL, 103, '2023-08-06 16:32:40', '1', '1', '2023-08-06 16:32:40', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1688105901743505409, '/StableDiffusion/2023-08-06/1688105901739311104.png', NULL, NULL, NULL, NULL, NULL, NULL, '1', 'sd', NULL, 103, '2023-08-06 16:33:05', '1', '1', '2023-08-06 16:33:05', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1688106313796100097, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-08-06/1688106310495182848.png', NULL, NULL, NULL, NULL, NULL, NULL, '1', 'sd', NULL, 103, '2023-08-06 16:34:43', '1', '1', '2023-08-06 16:34:43', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1688106572739846146, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-08-06/1688106568948195328.png', NULL, NULL, NULL, NULL, NULL, NULL, '1', 'sd', NULL, 103, '2023-08-06 16:35:45', '1', '1', '2023-08-06 16:35:45', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1688108066235031554, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-08-06/1688108062321745920.png', NULL, NULL, NULL, NULL, NULL, NULL, '1', 'sd', NULL, 103, '2023-08-06 16:41:41', '1', '1', '2023-08-06 16:41:41', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1688108539901984770, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-08-06/1688108525284835328.png', NULL, NULL, NULL, NULL, NULL, NULL, '1', 'sd', NULL, 103, '2023-08-06 16:43:34', '1', '1', '2023-08-06 16:43:34', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1688109337788628994, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-08-06/1688109333917286400.png', NULL, NULL, NULL, NULL, NULL, NULL, '1', 'sd', NULL, 103, '2023-08-06 16:46:44', '1', '1', '2023-08-06 16:46:44', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1688109526054158337, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-08-06/1688109522946179072.png', NULL, NULL, NULL, NULL, NULL, NULL, '1', 'sd', NULL, 103, '2023-08-06 16:47:29', '1', '1', '2023-08-06 16:47:29', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1688109680681369602, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-08-06/1688109676877135872.png', NULL, NULL, NULL, NULL, NULL, NULL, '1', 'sd', NULL, 103, '2023-08-06 16:48:06', '1', '1', '2023-08-06 16:48:06', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1688109962416963586, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-08-06/1688109958998605824.png', NULL, NULL, NULL, NULL, NULL, NULL, '1', 'sd', NULL, 103, '2023-08-06 16:49:13', '1', '1', '2023-08-06 16:49:13', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1688110249684844545, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-08-06/1688110246283264000.png', NULL, NULL, NULL, NULL, NULL, NULL, '1', 'sd', NULL, 103, '2023-08-06 16:50:22', '1', '1', '2023-08-06 16:50:22', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1688110430224465922, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-08-06/1688110426466369536.png', NULL, NULL, NULL, NULL, NULL, NULL, '1', 'sd', NULL, 103, '2023-08-06 16:51:05', '1', '1', '2023-08-06 16:51:05', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1688110522054557698, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-08-06/1688110519072407552.png', NULL, NULL, NULL, NULL, NULL, NULL, '1', 'sd', NULL, 103, '2023-08-06 16:51:27', '1', '1', '2023-08-06 16:51:27', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1688110601553395714, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-08-06/1688110598130843648.png', NULL, NULL, NULL, NULL, NULL, NULL, '1', 'sd', NULL, 103, '2023-08-06 16:51:46', '1', '1', '2023-08-06 16:51:46', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1688133085753352193, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-08-06/1688133081856843776.png', NULL, NULL, NULL, NULL, NULL, NULL, '1', 'sd', NULL, 103, '2023-08-06 18:21:06', '1', '1', '2023-08-06 18:21:06', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1688133343300395010, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-08-06/1688133339932368896.png', NULL, NULL, NULL, NULL, NULL, NULL, '1', 'sd', NULL, 103, '2023-08-06 18:22:08', '1', '1', '2023-08-06 18:22:08', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1688133448241881090, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-08-06/1688133445284896768.png', NULL, NULL, NULL, NULL, NULL, NULL, '1', 'sd', NULL, 103, '2023-08-06 18:22:33', '1', '1', '2023-08-06 18:22:33', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1688133518894931970, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-08-06/1688133515803729920.png', NULL, NULL, NULL, NULL, NULL, NULL, '1', 'sd', NULL, 103, '2023-08-06 18:22:50', '1', '1', '2023-08-06 18:22:50', NULL, NULL, '0', NULL, 0);
INSERT INTO `sys_file_info` VALUES (1688133688718106626, 'https://panda-1253683406.cos.ap-guangzhou.myqcloud.com/StableDiffusion/2023-08-06/1688133685488492544.png', NULL, NULL, NULL, NULL, NULL, NULL, '1', 'sd', NULL, 103, '2023-08-06 18:23:30', '1', '1', '2023-08-06 18:23:30', NULL, NULL, '0', NULL, 0);

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
-- Table structure for sys_menu_copy1
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu_copy1`;
CREATE TABLE `sys_menu_copy1`  (
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
-- Records of sys_menu_copy1
-- ----------------------------
INSERT INTO `sys_menu_copy1` VALUES (1, '系统管理', 0, 1, 'system', NULL, '', 1, 0, 'M', '0', '0', '', 'system', 103, 1, '2023-05-14 15:19:39', NULL, NULL, '系统管理目录');
INSERT INTO `sys_menu_copy1` VALUES (2, '系统监控', 0, 3, 'monitor', NULL, '', 1, 0, 'M', '0', '0', '', 'monitor', 103, 1, '2023-05-14 15:19:39', NULL, NULL, '系统监控目录');
INSERT INTO `sys_menu_copy1` VALUES (3, '系统工具', 0, 4, 'tool', NULL, '', 1, 0, 'M', '0', '0', '', 'tool', 103, 1, '2023-05-14 15:19:39', NULL, NULL, '系统工具目录');
INSERT INTO `sys_menu_copy1` VALUES (4, 'PLUS官网', 0, 5, 'https://gitee.com/dromara/RuoYi-Vue-Plus', NULL, '', 0, 0, 'M', '0', '0', '', 'guide', 103, 1, '2023-05-14 15:19:39', NULL, NULL, 'RuoYi-Vue-Plus官网地址');
INSERT INTO `sys_menu_copy1` VALUES (6, '租户管理', 0, 2, 'tenant', NULL, '', 1, 0, 'M', '0', '0', '', 'chart', 103, 1, '2023-05-14 15:19:39', NULL, NULL, '租户管理目录');
INSERT INTO `sys_menu_copy1` VALUES (100, '用户管理', 1, 1, 'user', 'system/user/index', '', 1, 0, 'C', '0', '0', 'system:user:list', 'user', 103, 1, '2023-05-14 15:19:39', NULL, NULL, '用户管理菜单');
INSERT INTO `sys_menu_copy1` VALUES (101, '角色管理', 1, 2, 'role', 'system/role/index', '', 1, 0, 'C', '0', '0', 'system:role:list', 'peoples', 103, 1, '2023-05-14 15:19:39', NULL, NULL, '角色管理菜单');
INSERT INTO `sys_menu_copy1` VALUES (102, '菜单管理', 1, 3, 'menu', 'system/menu/index', '', 1, 0, 'C', '0', '0', 'system:menu:list', 'tree-table', 103, 1, '2023-05-14 15:19:39', NULL, NULL, '菜单管理菜单');
INSERT INTO `sys_menu_copy1` VALUES (103, '部门管理', 1, 4, 'dept', 'system/dept/index', '', 1, 0, 'C', '0', '0', 'system:dept:list', 'tree', 103, 1, '2023-05-14 15:19:39', NULL, NULL, '部门管理菜单');
INSERT INTO `sys_menu_copy1` VALUES (104, '岗位管理', 1, 5, 'post', 'system/post/index', '', 1, 0, 'C', '0', '0', 'system:post:list', 'post', 103, 1, '2023-05-14 15:19:39', NULL, NULL, '岗位管理菜单');
INSERT INTO `sys_menu_copy1` VALUES (105, '字典管理', 1, 6, 'dict', 'system/dict/index', '', 1, 0, 'C', '0', '0', 'system:dict:list', 'dict', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '字典管理菜单');
INSERT INTO `sys_menu_copy1` VALUES (106, '参数设置', 1, 7, 'config', 'system/config/index', '', 1, 0, 'C', '0', '0', 'system:config:list', 'edit', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '参数设置菜单');
INSERT INTO `sys_menu_copy1` VALUES (107, '通知公告', 1, 8, 'notice', 'system/notice/index', '', 1, 0, 'C', '0', '0', 'system:notice:list', 'message', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '通知公告菜单');
INSERT INTO `sys_menu_copy1` VALUES (108, '日志管理', 1, 9, 'log', '', '', 1, 0, 'M', '0', '0', '', 'log', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '日志管理菜单');
INSERT INTO `sys_menu_copy1` VALUES (109, '在线用户', 2, 1, 'online', 'monitor/online/index', '', 1, 0, 'C', '0', '0', 'monitor:online:list', 'online', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '在线用户菜单');
INSERT INTO `sys_menu_copy1` VALUES (113, '缓存监控', 2, 5, 'cache', 'monitor/cache/index', '', 1, 0, 'C', '0', '0', 'monitor:cache:list', 'redis', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '缓存监控菜单');
INSERT INTO `sys_menu_copy1` VALUES (114, '表单构建', 3, 1, 'build', 'tool/build/index', '', 1, 0, 'C', '0', '0', 'tool:build:list', 'build', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '表单构建菜单');
INSERT INTO `sys_menu_copy1` VALUES (115, '代码生成', 3, 2, 'gen', 'tool/gen/index', '', 1, 0, 'C', '0', '0', 'tool:gen:list', 'code', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '代码生成菜单');
INSERT INTO `sys_menu_copy1` VALUES (118, '文件管理', 1, 10, 'oss', 'system/oss/index', '', 1, 0, 'C', '0', '0', 'system:oss:list', 'upload', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '文件管理菜单');
INSERT INTO `sys_menu_copy1` VALUES (121, '租户管理', 6, 1, 'tenant', 'system/tenant/index', '', 1, 0, 'C', '0', '0', 'system:tenant:list', 'list', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '租户管理菜单');
INSERT INTO `sys_menu_copy1` VALUES (122, '租户套餐管理', 6, 2, 'tenantPackage', 'system/tenantPackage/index', '', 1, 0, 'C', '0', '0', 'system:tenantPackage:list', 'form', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '租户套餐管理菜单');
INSERT INTO `sys_menu_copy1` VALUES (500, '操作日志', 108, 1, 'operlog', 'monitor/operlog/index', '', 1, 0, 'C', '0', '0', 'monitor:operlog:list', 'form', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '操作日志菜单');
INSERT INTO `sys_menu_copy1` VALUES (501, '登录日志', 108, 2, 'logininfor', 'monitor/logininfor/index', '', 1, 0, 'C', '0', '0', 'monitor:logininfor:list', 'logininfor', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '登录日志菜单');
INSERT INTO `sys_menu_copy1` VALUES (1001, '用户查询', 100, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:user:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1002, '用户新增', 100, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:user:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1003, '用户修改', 100, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:user:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1004, '用户删除', 100, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:user:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1005, '用户导出', 100, 5, '', '', '', 1, 0, 'F', '0', '0', 'system:user:export', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1006, '用户导入', 100, 6, '', '', '', 1, 0, 'F', '0', '0', 'system:user:import', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1007, '重置密码', 100, 7, '', '', '', 1, 0, 'F', '0', '0', 'system:user:resetPwd', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1008, '角色查询', 101, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:role:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1009, '角色新增', 101, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:role:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1010, '角色修改', 101, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:role:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1011, '角色删除', 101, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:role:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1012, '角色导出', 101, 5, '', '', '', 1, 0, 'F', '0', '0', 'system:role:export', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1013, '菜单查询', 102, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1014, '菜单新增', 102, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1015, '菜单修改', 102, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1016, '菜单删除', 102, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1017, '部门查询', 103, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1018, '部门新增', 103, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1019, '部门修改', 103, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1020, '部门删除', 103, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1021, '岗位查询', 104, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:post:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1022, '岗位新增', 104, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:post:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1023, '岗位修改', 104, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:post:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1024, '岗位删除', 104, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:post:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1025, '岗位导出', 104, 5, '', '', '', 1, 0, 'F', '0', '0', 'system:post:export', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1026, '字典查询', 105, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1027, '字典新增', 105, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1028, '字典修改', 105, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1029, '字典删除', 105, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1030, '字典导出', 105, 5, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:export', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1031, '参数查询', 106, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1032, '参数新增', 106, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1033, '参数修改', 106, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1034, '参数删除', 106, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1035, '参数导出', 106, 5, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:export', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1036, '公告查询', 107, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1037, '公告新增', 107, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1038, '公告修改', 107, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1039, '公告删除', 107, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1040, '操作查询', 500, 1, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1041, '操作删除', 500, 2, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1042, '日志导出', 500, 4, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:export', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1043, '登录查询', 501, 1, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1044, '登录删除', 501, 2, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1045, '日志导出', 501, 3, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:export', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1046, '在线查询', 109, 1, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:online:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1047, '批量强退', 109, 2, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:online:batchLogout', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1048, '单条强退', 109, 3, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:online:forceLogout', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1050, '账户解锁', 501, 4, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:unlock', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1055, '生成查询', 115, 1, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1056, '生成修改', 115, 2, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1057, '生成删除', 115, 3, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1058, '导入代码', 115, 2, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:import', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1059, '预览代码', 115, 4, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:preview', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1060, '生成代码', 115, 5, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:code', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1600, '文件查询', 118, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1601, '文件上传', 118, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:upload', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1602, '文件下载', 118, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:download', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1603, '文件删除', 118, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1604, '配置添加', 118, 5, '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1605, '配置编辑', 118, 6, '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1606, '租户查询', 121, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenant:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1607, '租户新增', 121, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenant:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1608, '租户修改', 121, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenant:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1609, '租户删除', 121, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenant:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1610, '租户导出', 121, 5, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenant:export', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1611, '租户套餐查询', 122, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenantPackage:query', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1612, '租户套餐新增', 122, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenantPackage:add', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1613, '租户套餐修改', 122, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenantPackage:edit', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1614, '租户套餐删除', 122, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenantPackage:remove', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1615, '租户套餐导出', 122, 5, '#', '', '', 1, 0, 'F', '0', '0', 'system:tenantPackage:export', '#', 103, 1, '2023-05-14 15:19:40', NULL, NULL, '');
INSERT INTO `sys_menu_copy1` VALUES (1689201668374556674, 'MJ服务器', 1689205943360188417, 1, 'mjserver', 'midjourney/mjserver/index', NULL, 1, 0, 'C', '0', '0', 'midjourney:mjserver:list', 'cascader', 103, 1, '2023-08-09 17:30:43', 1, '2023-08-09 17:41:50', 'MJ服务器信息配置菜单');
INSERT INTO `sys_menu_copy1` VALUES (1689205943360188417, 'Midjourney', 0, 1, 'Midjourney', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'documentation', 103, 1, '2023-08-09 17:24:15', 1, '2023-08-09 17:24:15', '');
INSERT INTO `sys_menu_copy1` VALUES (1689243465037561858, '创作记录', 1689205943360188417, 1, 'mjRoomMsg', 'midjourney/mjRoomMsg/index', NULL, 1, 0, 'C', '0', '0', 'midjourney:mjRoomMsg:list', 'documentation', 103, 1, '2023-08-09 20:10:20', 1, '2023-08-09 20:11:51', 'Midjourney 创作记录菜单');
INSERT INTO `sys_menu_copy1` VALUES (1689243466220355585, 'MJ创作会话', 1689205943360188417, 1, 'mjroom', 'midjourney/mjroom/index', NULL, 1, 0, 'C', '0', '0', 'midjourney:mjroom:list', 'example', 103, 1, '2023-08-09 20:10:04', 1, '2023-08-09 20:12:41', 'MJ创作会话菜单');

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
INSERT INTO `sys_oss` VALUES (1724093904361992194, '000000', '2023/11/13/e561a6c853c744a1963451063544b906.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/2023/11/13/e561a6c853c744a1963451063544b906.png', NULL, '2023-11-13 23:56:34', NULL, '2023-11-13 23:56:34', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724094429279137794, '000000', 'panda/2023/11/13/9045cff5d239475a8a33ab479801e9ca.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/13/9045cff5d239475a8a33ab479801e9ca.png', NULL, '2023-11-13 23:58:39', NULL, '2023-11-13 23:58:39', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724094894767144962, '000000', 'panda/2023/11/14/78bb8d6c5e0344bc8b422f8efe49b831.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/14/78bb8d6c5e0344bc8b422f8efe49b831.png', NULL, '2023-11-14 00:00:30', NULL, '2023-11-14 00:00:30', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724096506927583233, '000000', 'panda/2023/11/14/2ae75741f38746738a8387f2a6a52d38.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/14/2ae75741f38746738a8387f2a6a52d38.png', NULL, '2023-11-14 00:06:54', NULL, '2023-11-14 00:06:54', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724096774511595522, '000000', 'panda/2023/11/14/a684da9e0afc4d77a7d605594488c4e5.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/14/a684da9e0afc4d77a7d605594488c4e5.png', NULL, '2023-11-14 00:07:58', NULL, '2023-11-14 00:07:58', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724096873618804738, '000000', 'panda/2023/11/14/b77d26c97b424fd280c0a97cd573d52b.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/14/b77d26c97b424fd280c0a97cd573d52b.png', NULL, '2023-11-14 00:08:22', NULL, '2023-11-14 00:08:22', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724097020008402945, '000000', 'panda/2023/11/14/0299ff8929414f72ab8e2842506f46cb.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/14/0299ff8929414f72ab8e2842506f46cb.png', NULL, '2023-11-14 00:08:56', NULL, '2023-11-14 00:08:56', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724097652161318914, '000000', 'panda/2023/11/14/01856e57d24841ccb11a5394f0891697.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/14/01856e57d24841ccb11a5394f0891697.png', NULL, '2023-11-14 00:11:27', NULL, '2023-11-14 00:11:27', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724097778443423746, '000000', 'panda/2023/11/14/809300ca47e74931a6497b5c7c16cfa2.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/14/809300ca47e74931a6497b5c7c16cfa2.png', NULL, '2023-11-14 00:11:57', NULL, '2023-11-14 00:11:57', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724098776197042178, '000000', 'panda/2023/11/14/10718cd6cf1a428eb0a1f179d107b82c.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/14/10718cd6cf1a428eb0a1f179d107b82c.png', NULL, '2023-11-14 00:15:55', NULL, '2023-11-14 00:15:55', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724099123200200705, '000000', 'panda/2023/11/14/7768533b1e064c9d8a515a019df64485.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/14/7768533b1e064c9d8a515a019df64485.png', NULL, '2023-11-14 00:17:18', NULL, '2023-11-14 00:17:18', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724100420401958913, '000000', 'panda/2023/11/14/721015838ec847bfa7c723d93ab5eb3d.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/14/721015838ec847bfa7c723d93ab5eb3d.png', NULL, '2023-11-14 00:22:27', NULL, '2023-11-14 00:22:27', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724101239675359233, '000000', 'panda/2023/11/14/051b4994f0e24e0eb7b67b7c06ed1acb.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/14/051b4994f0e24e0eb7b67b7c06ed1acb.png', NULL, '2023-11-14 00:25:42', NULL, '2023-11-14 00:25:42', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724101751804071938, '000000', 'panda/2023/11/14/eac97a424b9e4afca5c7894387fa747e.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/14/eac97a424b9e4afca5c7894387fa747e.png', NULL, '2023-11-14 00:27:45', NULL, '2023-11-14 00:27:45', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724401488415498242, '000000', 'panda/2023/11/14/6d4cb7814bbf448eb0fc18449d009093.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/14/6d4cb7814bbf448eb0fc18449d009093.png', NULL, '2023-11-14 20:18:47', NULL, '2023-11-14 20:18:47', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724405287066607617, '000000', 'panda/2023/11/14/eee8205f028548cb895dc0cda171afee.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/14/eee8205f028548cb895dc0cda171afee.png', NULL, '2023-11-14 20:33:53', NULL, '2023-11-14 20:33:53', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724405540805222401, '000000', 'panda/2023/11/14/5a7b7c2871ac49bea266454f0d5b6ddc.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/14/5a7b7c2871ac49bea266454f0d5b6ddc.png', NULL, '2023-11-14 20:34:53', NULL, '2023-11-14 20:34:53', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724406694637281281, '000000', 'panda/2023/11/14/fcb8866034a24968bd2e3014c1ddba35.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/14/fcb8866034a24968bd2e3014c1ddba35.png', NULL, '2023-11-14 20:39:29', NULL, '2023-11-14 20:39:29', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724406885754937346, '000000', 'panda/2023/11/14/144295909be740139f00c78a72b3defe.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/14/144295909be740139f00c78a72b3defe.png', NULL, '2023-11-14 20:40:14', NULL, '2023-11-14 20:40:14', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724407020853469185, '000000', 'panda/2023/11/14/521e85dfbf774fa982324b82a0f88024.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/14/521e85dfbf774fa982324b82a0f88024.png', NULL, '2023-11-14 20:40:46', NULL, '2023-11-14 20:40:46', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724433064465604610, '000000', 'panda/2023/11/14/115f5d28e1224c82b27a8572fc76acbb.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/14/115f5d28e1224c82b27a8572fc76acbb.png', NULL, '2023-11-14 22:24:16', NULL, '2023-11-14 22:24:16', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724433914823032834, '000000', 'panda/2023/11/14/42815f5c2714433ab6826d7a33764e9b.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/14/42815f5c2714433ab6826d7a33764e9b.png', NULL, '2023-11-14 22:27:38', NULL, '2023-11-14 22:27:38', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724434495671193602, '000000', 'panda/2023/11/14/28f83274714f43fcaec23e04d386f94e.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/14/28f83274714f43fcaec23e04d386f94e.png', NULL, '2023-11-14 22:29:57', NULL, '2023-11-14 22:29:57', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724437756327305218, '000000', 'panda/2023/11/14/156fc5a9dd4f4ea3b9adfe29b364b4a3.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/14/156fc5a9dd4f4ea3b9adfe29b364b4a3.png', NULL, '2023-11-14 22:42:54', NULL, '2023-11-14 22:42:54', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724438370654429186, '000000', 'panda/2023/11/14/eaa08c2d5ad64b84bba08717eedaf929.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/14/eaa08c2d5ad64b84bba08717eedaf929.png', NULL, '2023-11-14 22:45:21', NULL, '2023-11-14 22:45:21', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724439770461446145, '000000', 'panda/2023/11/14/6f9a7f3a95824ed28214c3884a1bdb79.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/14/6f9a7f3a95824ed28214c3884a1bdb79.png', NULL, '2023-11-14 22:50:54', NULL, '2023-11-14 22:50:54', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724442703806668801, '000000', 'panda/2023/11/14/333b7056786f46c9b5eb53587e63f579.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/14/333b7056786f46c9b5eb53587e63f579.png', NULL, '2023-11-14 23:02:34', NULL, '2023-11-14 23:02:34', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724765725893611522, '000000', 'panda/2023/11/15/f9c2956e55e74bf0ac03683b688d5754.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/f9c2956e55e74bf0ac03683b688d5754.png', NULL, '2023-11-15 20:26:08', NULL, '2023-11-15 20:26:08', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724766090277965826, '000000', 'panda/2023/11/15/cb383b7672fd4e32b7f719b19ca7f5a4.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/cb383b7672fd4e32b7f719b19ca7f5a4.png', NULL, '2023-11-15 20:27:35', NULL, '2023-11-15 20:27:35', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724766602800922625, '000000', 'panda/2023/11/15/8d43969f4cdc4f8992f4058e427a8a44.png', '私有知识库业务架构图.drawio.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/8d43969f4cdc4f8992f4058e427a8a44.png', NULL, '2023-11-15 20:29:37', NULL, '2023-11-15 20:29:37', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724767373487505410, '000000', 'panda/2023/11/15/99ef8cb376d44ecf8271bb1cb2c57ab2.png', '私有知识库业务架构图.drawio.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/99ef8cb376d44ecf8271bb1cb2c57ab2.png', NULL, '2023-11-15 20:32:41', NULL, '2023-11-15 20:32:41', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724767516244836354, '000000', 'panda/2023/11/15/aef8264fc6404972a3a52076bc011d53.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/aef8264fc6404972a3a52076bc011d53.png', NULL, '2023-11-15 20:33:15', NULL, '2023-11-15 20:33:15', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724767702018949121, '000000', 'panda/2023/11/15/5f800b9dac3146169492e199f5310396.png', '私有知识库业务架构图.drawio.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/5f800b9dac3146169492e199f5310396.png', NULL, '2023-11-15 20:33:59', NULL, '2023-11-15 20:33:59', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724768592499048449, '000000', 'panda/2023/11/15/70d04883055a46ce81849304aa51a6f7.png', 'car.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/70d04883055a46ce81849304aa51a6f7.png', NULL, '2023-11-15 20:37:32', NULL, '2023-11-15 20:37:32', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724768986373554178, '000000', 'panda/2023/11/15/a6595c4dc5374352b825e933ffc7c229.png', 'car.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/a6595c4dc5374352b825e933ffc7c229.png', NULL, '2023-11-15 20:39:06', NULL, '2023-11-15 20:39:06', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724769126123569154, '000000', 'panda/2023/11/15/7b9b652cdbd8458aaab24929226ed0a8.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/7b9b652cdbd8458aaab24929226ed0a8.png', NULL, '2023-11-15 20:39:39', NULL, '2023-11-15 20:39:39', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724769445880528898, '000000', 'panda/2023/11/15/e0f7187f7ad2414190d8722e9b2070b8.png', 'car.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/e0f7187f7ad2414190d8722e9b2070b8.png', NULL, '2023-11-15 20:40:55', NULL, '2023-11-15 20:40:55', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724769768900657153, '000000', 'panda/2023/11/15/0c3cd50edd584d8bba214e26f0f034e2.png', 'car.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/0c3cd50edd584d8bba214e26f0f034e2.png', NULL, '2023-11-15 20:42:12', NULL, '2023-11-15 20:42:12', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724770054327238658, '000000', 'panda/2023/11/15/c012bfafc4b3433aadbe2ab51ed51826.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/c012bfafc4b3433aadbe2ab51ed51826.png', NULL, '2023-11-15 20:43:20', NULL, '2023-11-15 20:43:20', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724778597159854082, '000000', 'panda/2023/11/15/b34ab6fabd7f4cb895c53a562f54b667.png', 'car.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/b34ab6fabd7f4cb895c53a562f54b667.png', NULL, '2023-11-15 21:17:17', NULL, '2023-11-15 21:17:17', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724779240129880065, '000000', 'panda/2023/11/15/1d94798cb4604f2da5635e59a7d4c7a9.png', 'car.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/1d94798cb4604f2da5635e59a7d4c7a9.png', NULL, '2023-11-15 21:19:50', NULL, '2023-11-15 21:19:50', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724779383239532546, '000000', 'panda/2023/11/15/9530efe60c79407e9b285b3ae4c7f4b5.png', 'car.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/9530efe60c79407e9b285b3ae4c7f4b5.png', NULL, '2023-11-15 21:20:24', NULL, '2023-11-15 21:20:24', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724779749196750850, '000000', 'panda/2023/11/15/7f38b8cbef0d4b72b366f934db790cb2.png', 'car.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/7f38b8cbef0d4b72b366f934db790cb2.png', NULL, '2023-11-15 21:21:52', NULL, '2023-11-15 21:21:52', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724780087119241217, '000000', 'panda/2023/11/15/a391cf2a8fa649918785c0ebda8fffe8.png', 'car.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/a391cf2a8fa649918785c0ebda8fffe8.png', NULL, '2023-11-15 21:23:12', NULL, '2023-11-15 21:23:12', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724780197819506689, '000000', 'panda/2023/11/15/2bac630ba299447d9f5b5f389d29910f.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/2bac630ba299447d9f5b5f389d29910f.png', NULL, '2023-11-15 21:23:39', NULL, '2023-11-15 21:23:39', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724780567291551746, '000000', 'panda/2023/11/15/db5bb09cf6594ce997dd7948bb692c4a.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/db5bb09cf6594ce997dd7948bb692c4a.png', NULL, '2023-11-15 21:25:07', NULL, '2023-11-15 21:25:07', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724780734052884482, '000000', 'panda/2023/11/15/d000a64b62924e6e8c9fb1d38d251ba3.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/d000a64b62924e6e8c9fb1d38d251ba3.png', NULL, '2023-11-15 21:25:47', NULL, '2023-11-15 21:25:47', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724780853112397825, '000000', 'panda/2023/11/15/e0927eff92364d438f5d2f9f1e326f7a.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/e0927eff92364d438f5d2f9f1e326f7a.png', NULL, '2023-11-15 21:26:15', NULL, '2023-11-15 21:26:15', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724781086638661633, '000000', 'panda/2023/11/15/59a1adadaf45407e8c2b7ecdd6aed234.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/59a1adadaf45407e8c2b7ecdd6aed234.png', NULL, '2023-11-15 21:27:11', NULL, '2023-11-15 21:27:11', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724781170445049857, '000000', 'panda/2023/11/15/1753a19e6f3949ea8d898d250d814b2a.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/1753a19e6f3949ea8d898d250d814b2a.png', NULL, '2023-11-15 21:27:31', NULL, '2023-11-15 21:27:31', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724781331867033602, '000000', 'panda/2023/11/15/892b31f1fd644cc181503a82eaa91b56.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/892b31f1fd644cc181503a82eaa91b56.png', NULL, '2023-11-15 21:28:09', NULL, '2023-11-15 21:28:09', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724781489526726657, '000000', 'panda/2023/11/15/ba7c50a4bd8e4f218f8f4ec2f972e3bf.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/ba7c50a4bd8e4f218f8f4ec2f972e3bf.png', NULL, '2023-11-15 21:28:47', NULL, '2023-11-15 21:28:47', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724783215763841025, '000000', 'panda/2023/11/15/6299ff42e7ba4be08ddf8eb52b190aa9.png', 'car.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/6299ff42e7ba4be08ddf8eb52b190aa9.png', NULL, '2023-11-15 21:35:38', NULL, '2023-11-15 21:35:38', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724784653952933890, '000000', 'panda/2023/11/15/8aa89605883f486d82152cac156c67ba.png', '私有知识库业务架构图.drawio.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/8aa89605883f486d82152cac156c67ba.png', NULL, '2023-11-15 21:41:21', NULL, '2023-11-15 21:41:21', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724785428187897858, '000000', 'panda/2023/11/15/7cce512cce0b4694a2f2868f83885492.png', '私有知识库业务架构图.drawio.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/7cce512cce0b4694a2f2868f83885492.png', NULL, '2023-11-15 21:44:26', NULL, '2023-11-15 21:44:26', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724785906720235522, '000000', 'panda/2023/11/15/404adf4487ff47a7bf486bede0adb445.png', '私有知识库业务架构图.drawio.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/404adf4487ff47a7bf486bede0adb445.png', NULL, '2023-11-15 21:46:20', NULL, '2023-11-15 21:46:20', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724786545873444865, '000000', 'panda/2023/11/15/6f1bd7a432d6471489fa02b794154245.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/6f1bd7a432d6471489fa02b794154245.png', NULL, '2023-11-15 21:48:52', NULL, '2023-11-15 21:48:52', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724787008983326721, '000000', 'panda/2023/11/15/b8b0b5c46e0a42baa17b855b5125e709.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/b8b0b5c46e0a42baa17b855b5125e709.png', NULL, '2023-11-15 21:50:43', NULL, '2023-11-15 21:50:43', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724787081741918210, '000000', 'panda/2023/11/15/4e01e0df4e55433bbf11b73f7f170fdc.png', 'car.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/4e01e0df4e55433bbf11b73f7f170fdc.png', NULL, '2023-11-15 21:51:00', NULL, '2023-11-15 21:51:00', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724787195835375618, '000000', 'panda/2023/11/15/9717d8c966ab469780845c4ab22e9056.png', '私有知识库业务架构图.drawio.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/9717d8c966ab469780845c4ab22e9056.png', NULL, '2023-11-15 21:51:27', NULL, '2023-11-15 21:51:27', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724787290500816897, '000000', 'panda/2023/11/15/0bfa1344b01346a09910574162d743cf.png', 'car.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/0bfa1344b01346a09910574162d743cf.png', NULL, '2023-11-15 21:51:50', NULL, '2023-11-15 21:51:50', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724787396339884033, '000000', 'panda/2023/11/15/8624513df5834cbc87e690856ba02f18.png', 'car.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/8624513df5834cbc87e690856ba02f18.png', NULL, '2023-11-15 21:52:15', NULL, '2023-11-15 21:52:15', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724787589063958530, '000000', 'panda/2023/11/15/a85bc6d9df7145e7bfc2b5b295c7f92c.png', 'car.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/a85bc6d9df7145e7bfc2b5b295c7f92c.png', NULL, '2023-11-15 21:53:01', NULL, '2023-11-15 21:53:01', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724787711298560002, '000000', 'panda/2023/11/15/1c7145c5872b4bd494006acb9fb88573.png', 'car.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/1c7145c5872b4bd494006acb9fb88573.png', NULL, '2023-11-15 21:53:30', NULL, '2023-11-15 21:53:30', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724787763475701762, '000000', 'panda/2023/11/15/a55b72158eed48a6be736147c55bd0e4.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/a55b72158eed48a6be736147c55bd0e4.png', NULL, '2023-11-15 21:53:42', NULL, '2023-11-15 21:53:42', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724787893566234625, '000000', 'panda/2023/11/15/ebf7e48e1cf44fe08b6e4065fd67f478.png', 'car.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/ebf7e48e1cf44fe08b6e4065fd67f478.png', NULL, '2023-11-15 21:54:14', NULL, '2023-11-15 21:54:14', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724787927120666625, '000000', 'panda/2023/11/15/d728ca4da45f4ee3a1b850f3cdc959aa.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/d728ca4da45f4ee3a1b850f3cdc959aa.png', NULL, '2023-11-15 21:54:22', NULL, '2023-11-15 21:54:22', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724800813741961217, '000000', 'panda/2023/11/15/3d8104064d4b48d1b8fb0e2c0f9b6566.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/3d8104064d4b48d1b8fb0e2c0f9b6566.png', NULL, '2023-11-15 22:45:34', NULL, '2023-11-15 22:45:34', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724801133461172225, '000000', 'panda/2023/11/15/b82063694a48471db67f81ac2ad6ab90.png', 'car.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/b82063694a48471db67f81ac2ad6ab90.png', NULL, '2023-11-15 22:46:50', NULL, '2023-11-15 22:46:50', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724802328179306497, '000000', 'panda/2023/11/15/963a68501d714b1498573ca0e4d10ec3.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/15/963a68501d714b1498573ca0e4d10ec3.png', NULL, '2023-11-15 22:51:35', NULL, '2023-11-15 22:51:35', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724828266845761538, '000000', 'panda/2023/11/16/02bf7cf43551435db8dff93eb6d2c6a4.png', 'IMG_1699544354421.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/16/02bf7cf43551435db8dff93eb6d2c6a4.png', NULL, '2023-11-16 00:34:39', NULL, '2023-11-16 00:34:39', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724828390909079554, '000000', 'panda/2023/11/16/14f0015b41544ec482e0e53a5544d4f7.png', 'IMG_1699544354421.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/16/14f0015b41544ec482e0e53a5544d4f7.png', NULL, '2023-11-16 00:35:09', NULL, '2023-11-16 00:35:09', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724828548040290306, '000000', 'panda/2023/11/16/b4d13b6b3b3c49f28318d772036186df.png', 'IMG_1699544354421.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/16/b4d13b6b3b3c49f28318d772036186df.png', NULL, '2023-11-16 00:35:46', NULL, '2023-11-16 00:35:46', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724951618247782402, '000000', 'panda/2023/11/16/ae1e046de273436387864dbf16b395a1.png', '5fb9f9064adadc3a012626892ea10ed.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/16/ae1e046de273436387864dbf16b395a1.png', NULL, '2023-11-16 08:44:48', NULL, '2023-11-16 08:44:48', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724951857486688258, '000000', 'panda/2023/11/16/fc2576bd00774ca8851ea092f2e99dd0.png', '97d7354f2da41efb9816ec674e373da.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/16/fc2576bd00774ca8851ea092f2e99dd0.png', NULL, '2023-11-16 08:45:46', NULL, '2023-11-16 08:45:46', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1724996532088098818, '000000', 'panda/2023/11/16/2292c6e254bf40a2bd3ca202c5df1d75.png', '5-6-40-c.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/16/2292c6e254bf40a2bd3ca202c5df1d75.png', NULL, '2023-11-16 11:43:17', NULL, '2023-11-16 11:43:17', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1725132601643257857, '000000', 'panda/2023/11/16/a0b923c964814893a4458f91b6c830af.png', 'IMG_1332.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/16/a0b923c964814893a4458f91b6c830af.png', NULL, '2023-11-16 20:43:58', NULL, '2023-11-16 20:43:58', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1725433928357269506, '000000', 'panda/2023/11/17/c772ceaebf834c18874efc4bff0aa3f3.png', '丁修加钱.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/17/c772ceaebf834c18874efc4bff0aa3f3.png', NULL, '2023-11-17 16:41:20', NULL, '2023-11-17 16:41:20', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1725691686469931009, '000000', 'panda/2023/11/18/f4e4520cd2c749c09ae6b00938b81637.png', '5fb9f9064adadc3a012626892ea10ed.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/18/f4e4520cd2c749c09ae6b00938b81637.png', NULL, '2023-11-18 09:45:35', NULL, '2023-11-18 09:45:35', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1725691813939023874, '000000', 'panda/2023/11/18/8dd4b56f93a84f10ae13b8464203c05f.png', '5fb9f9064adadc3a012626892ea10ed.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/18/8dd4b56f93a84f10ae13b8464203c05f.png', NULL, '2023-11-18 09:46:05', NULL, '2023-11-18 09:46:05', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1725691969296044033, '000000', 'panda/2023/11/18/f63907009334477fb8f1d4fc98ceb1b8.png', '5fb9f9064adadc3a012626892ea10ed.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/18/f63907009334477fb8f1d4fc98ceb1b8.png', NULL, '2023-11-18 09:46:42', NULL, '2023-11-18 09:46:42', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1725781563089813505, '000000', 'panda/2023/11/18/22480c231c17466eb9b67ce85cee40e2.png', '5fb9f9064adadc3a012626892ea10ed.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/18/22480c231c17466eb9b67ce85cee40e2.png', NULL, '2023-11-18 15:42:43', NULL, '2023-11-18 15:42:43', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1725781986756427778, '000000', 'panda/2023/11/18/bd12eff2f1e444cf921184bf2a0ecf47.png', '5fb9f9064adadc3a012626892ea10ed.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/18/bd12eff2f1e444cf921184bf2a0ecf47.png', NULL, '2023-11-18 15:44:24', NULL, '2023-11-18 15:44:24', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1725782262561275906, '000000', 'panda/2023/11/18/6b269086b6c34aafbcecd53158171e73.png', 'cxlogo.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/18/6b269086b6c34aafbcecd53158171e73.png', NULL, '2023-11-18 15:45:30', NULL, '2023-11-18 15:45:30', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1725783179062837250, '000000', 'panda/2023/11/18/43b42971403a4369853ab70d2b5de87a.png', 'cxlogo.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/18/43b42971403a4369853ab70d2b5de87a.png', NULL, '2023-11-18 15:49:08', NULL, '2023-11-18 15:49:08', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1725787886057684993, '000000', 'panda/2023/11/18/f9cdc4ab356f40a78b58cafcceff0ad3.png', '2 - 副本.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/18/f9cdc4ab356f40a78b58cafcceff0ad3.png', NULL, '2023-11-18 16:07:50', NULL, '2023-11-18 16:07:50', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1725810738244878338, '000000', 'panda/2023/11/18/fbc1689264fb40f3a6741d9a8630497c.png', '01.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/18/fbc1689264fb40f3a6741d9a8630497c.png', NULL, '2023-11-18 17:38:39', NULL, '2023-11-18 17:38:39', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1725811449238126593, '000000', 'panda/2023/11/18/226bd230bde545a1944a9d9fb60928bf.png', '01.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/18/226bd230bde545a1944a9d9fb60928bf.png', NULL, '2023-11-18 17:41:28', NULL, '2023-11-18 17:41:28', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1726446207907397634, '000000', 'panda/2023/11/20/b83e2a218e4144a3bbde52fe4e09ab83.png', 'knowledge.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/20/b83e2a218e4144a3bbde52fe4e09ab83.png', NULL, '2023-11-20 11:43:46', NULL, '2023-11-20 11:43:46', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1726446287590785026, '000000', 'panda/2023/11/20/27428b0c89ac431484effbe04e7e0bb3.png', '各项目用例覆盖率和执行率.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/20/27428b0c89ac431484effbe04e7e0bb3.png', NULL, '2023-11-20 11:44:05', NULL, '2023-11-20 11:44:05', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1726508568504696834, '000000', 'panda/2023/11/20/b6c10c8b285f481bb5c88efc4c02fb56.png', 'welt.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/20/b6c10c8b285f481bb5c88efc4c02fb56.png', NULL, '2023-11-20 15:51:34', NULL, '2023-11-20 15:51:34', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1726795990043983873, '000000', 'panda/2023/11/21/9a755fa9b88e4c6d923c8579df762637.png', 'ec29ac14d10ea300952911ebecd02ce2.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/21/9a755fa9b88e4c6d923c8579df762637.png', NULL, '2023-11-21 10:53:41', NULL, '2023-11-21 10:53:41', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1727235517598400513, '000000', 'panda/2023/11/22/330b5c8b8ffb44068dd25fecbd4900b7.png', '微信截图_20231122155953.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/22/330b5c8b8ffb44068dd25fecbd4900b7.png', NULL, '2023-11-22 16:00:13', NULL, '2023-11-22 16:00:13', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1727488896908132354, '000000', 'panda/2023/11/23/07954d978c9c4c558e0b2fd4a7d5a9d8.png', '01asdasd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/23/07954d978c9c4c558e0b2fd4a7d5a9d8.png', NULL, '2023-11-23 08:47:03', NULL, '2023-11-23 08:47:03', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1727586852713791490, '000000', 'panda/2023/11/23/7d6acd12dc2f4735bfaeb8e45f196e32.png', '微信图片_20231123151602.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/23/7d6acd12dc2f4735bfaeb8e45f196e32.png', NULL, '2023-11-23 15:16:17', NULL, '2023-11-23 15:16:17', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1727586918321094657, '000000', 'panda/2023/11/23/f4099c1cd3654851a2edc8c3210c161d.png', '微信图片_20231123151602.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/23/f4099c1cd3654851a2edc8c3210c161d.png', NULL, '2023-11-23 15:16:33', NULL, '2023-11-23 15:16:33', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1727587636234944514, '000000', 'panda/2023/11/23/8e9719ebe5794aeab84488aa0d360ae6.png', '2.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/23/8e9719ebe5794aeab84488aa0d360ae6.png', NULL, '2023-11-23 15:19:24', NULL, '2023-11-23 15:19:24', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1727961424273342466, '000000', 'panda/2023/11/24/c62a8f5413a34ea986a0ea0ba4fc266c.png', '微信图片_20231123143515.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/24/c62a8f5413a34ea986a0ea0ba4fc266c.png', NULL, '2023-11-24 16:04:42', NULL, '2023-11-24 16:04:42', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1727973179221344258, '000000', 'panda/2023/11/24/6b0a354d387543b8a033114a98dc7323.png', '2023-11-24_165112.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/24/6b0a354d387543b8a033114a98dc7323.png', NULL, '2023-11-24 16:51:25', NULL, '2023-11-24 16:51:25', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1727973372570370049, '000000', 'panda/2023/11/24/e371ae8d4f9f42b8a3e6a4d5afe188a3.png', '2023-11-24_165112.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/24/e371ae8d4f9f42b8a3e6a4d5afe188a3.png', NULL, '2023-11-24 16:52:11', NULL, '2023-11-24 16:52:11', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1727979725120602113, '000000', 'panda/2023/11/24/46108eb4766d4e7ab4680edc0d9c0fe5.png', '2023-11-24_171709.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/24/46108eb4766d4e7ab4680edc0d9c0fe5.png', NULL, '2023-11-24 17:17:25', NULL, '2023-11-24 17:17:25', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1727996095774326785, '000000', 'panda/2023/11/24/be344d68bafb44fbaaa81207d2ea2869.png', 'p.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/24/be344d68bafb44fbaaa81207d2ea2869.png', NULL, '2023-11-24 18:22:29', NULL, '2023-11-24 18:22:29', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1727996120692686849, '000000', 'panda/2023/11/24/e33c75f6366e44639ea41602b10432b5.png', 'p.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/24/e33c75f6366e44639ea41602b10432b5.png', NULL, '2023-11-24 18:22:34', NULL, '2023-11-24 18:22:34', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1727996721103110145, '000000', 'panda/2023/11/24/69625eef260846b2893619001ce987ba.png', '1.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/24/69625eef260846b2893619001ce987ba.png', NULL, '2023-11-24 18:24:58', NULL, '2023-11-24 18:24:58', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1728066768941088770, '000000', 'panda/2023/11/24/dc1b0de759c542fb87da0160468f60fe.png', 'Screenshot_2023-11-24-22-50-06-755_com.phone.tenc.t.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/24/dc1b0de759c542fb87da0160468f60fe.png', NULL, '2023-11-24 23:03:18', NULL, '2023-11-24 23:03:18', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1728067360941932546, '000000', 'panda/2023/11/24/b7a5caca31084ae5801c2c357dc27fb5.png', 'Screenshot_2023-11-24-22-50-06-755_com.phone.tenc.t.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/11/24/b7a5caca31084ae5801c2c357dc27fb5.png', NULL, '2023-11-24 23:05:39', NULL, '2023-11-24 23:05:39', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1731199309428940802, '000000', 'panda/2023/12/03/806d33f647b14b4cb900341c40fdf02c.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/03/806d33f647b14b4cb900341c40fdf02c.png', NULL, '2023-12-03 14:30:54', NULL, '2023-12-03 14:30:54', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1731292337262895105, '000000', 'panda/2023/12/03/6e0147ed31bf4158b4caefb9421b6e3b.png', '屏幕截图 2023-11-22 223300.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/03/6e0147ed31bf4158b4caefb9421b6e3b.png', NULL, '2023-12-03 20:40:34', NULL, '2023-12-03 20:40:34', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1731292573242826753, '000000', 'panda/2023/12/03/91791610e7ec4c4297791f9ff33230fb.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/03/91791610e7ec4c4297791f9ff33230fb.png', NULL, '2023-12-03 20:41:30', NULL, '2023-12-03 20:41:30', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1731294145418993665, '000000', 'panda/2023/12/03/34441c45c25e4e2385da49c9635287ad.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/03/34441c45c25e4e2385da49c9635287ad.png', NULL, '2023-12-03 20:47:45', NULL, '2023-12-03 20:47:45', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1731294320925450241, '000000', 'panda/2023/12/03/38a5a060e8c74b94a239394e8ba2a14c.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/03/38a5a060e8c74b94a239394e8ba2a14c.png', NULL, '2023-12-03 20:48:27', NULL, '2023-12-03 20:48:27', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1731294871528513538, '000000', 'panda/2023/12/03/0e6cf9193db542ef87689b8c2e340a00.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/03/0e6cf9193db542ef87689b8c2e340a00.png', NULL, '2023-12-03 20:50:38', NULL, '2023-12-03 20:50:38', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1731295198453538817, '000000', 'panda/2023/12/03/ff4c2db9d18d4c2da398522a9cd7e44c.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/03/ff4c2db9d18d4c2da398522a9cd7e44c.png', NULL, '2023-12-03 20:51:56', NULL, '2023-12-03 20:51:56', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1731295666185543681, '000000', 'panda/2023/12/03/bafef9c90033450eadea5f5f78bc3111.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/03/bafef9c90033450eadea5f5f78bc3111.png', NULL, '2023-12-03 20:53:47', NULL, '2023-12-03 20:53:47', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1731295938131632130, '000000', 'panda/2023/12/03/16db4a9fb0924f5084fe73d4c967984f.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/03/16db4a9fb0924f5084fe73d4c967984f.png', NULL, '2023-12-03 20:54:52', NULL, '2023-12-03 20:54:52', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1731304487146557441, '000000', 'panda/2023/12/03/0618d3bb9ed3435bb864fb2efc2a4b28.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/03/0618d3bb9ed3435bb864fb2efc2a4b28.png', NULL, '2023-12-03 21:28:50', NULL, '2023-12-03 21:28:50', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1731358847409881089, '000000', 'panda/2023/12/04/963b258630724287b6b802d456bae5c9.png', '26440005_0_final.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/04/963b258630724287b6b802d456bae5c9.png', NULL, '2023-12-04 01:04:51', NULL, '2023-12-04 01:04:51', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1731529436212789249, '000000', 'panda/2023/12/04/c878925646224871866018c746364cdf.png', 'Screenshot_20231130_234800.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/04/c878925646224871866018c746364cdf.png', NULL, '2023-12-04 12:22:43', NULL, '2023-12-04 12:22:43', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1733021108662730753, '000000', 'panda/2023/12/08/63c681674ec146c9ab1fd9ed79ae22a2.png', '1.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/08/63c681674ec146c9ab1fd9ed79ae22a2.png', NULL, '2023-12-08 15:10:05', NULL, '2023-12-08 15:10:05', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1733022568297299969, '000000', 'panda/2023/12/08/cdbbdd6d055140ee8df54a608818932d.png', '1.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/08/cdbbdd6d055140ee8df54a608818932d.png', NULL, '2023-12-08 15:15:53', NULL, '2023-12-08 15:15:53', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1734834493800914945, '000000', 'panda/2023/12/13/3243b4994d914349924116d4e06b502e.png', '1881702451694_.pic.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/13/3243b4994d914349924116d4e06b502e.png', NULL, '2023-12-13 15:15:50', NULL, '2023-12-13 15:15:50', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1738112593976434690, '000000', 'panda/2023/12/22/ec9da3d18d1847b3a66a620075cd0843.png', '1737686178787409921_32.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/22/ec9da3d18d1847b3a66a620075cd0843.png', NULL, '2023-12-22 16:21:50', NULL, '2023-12-22 16:21:50', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1738112916665212929, '000000', 'panda/2023/12/22/63a19b2432ad46239e4ae7ac43c76b13.png', '1737686178787409921_32.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/22/63a19b2432ad46239e4ae7ac43c76b13.png', NULL, '2023-12-22 16:23:07', NULL, '2023-12-22 16:23:07', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1738112995123863554, '000000', 'panda/2023/12/22/83f563f665c94e47bff9f639c10e8e08.png', '1737686178787409921_32.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/22/83f563f665c94e47bff9f639c10e8e08.png', NULL, '2023-12-22 16:23:25', NULL, '2023-12-22 16:23:25', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1738114003216441345, '000000', 'panda/2023/12/22/7b2530f5d10549c19adefc4f4b968e85.png', 'test.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/22/7b2530f5d10549c19adefc4f4b968e85.png', NULL, '2023-12-22 16:27:26', NULL, '2023-12-22 16:27:26', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1738115155366584322, '000000', 'panda/2023/12/22/7fb08f8de97046928d1188f6c4cb1a85.png', 'test.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/22/7fb08f8de97046928d1188f6c4cb1a85.png', NULL, '2023-12-22 16:32:00', NULL, '2023-12-22 16:32:00', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1738115393653383169, '000000', 'panda/2023/12/22/c69f65a8faa74e07bb2093de8874be4e.png', 'test.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/22/c69f65a8faa74e07bb2093de8874be4e.png', NULL, '2023-12-22 16:32:57', NULL, '2023-12-22 16:32:57', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1738118191891685378, '000000', 'panda/2023/12/22/13d8ea2c3b6d42129ec47245beabcd14.png', 'test.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/22/13d8ea2c3b6d42129ec47245beabcd14.png', NULL, '2023-12-22 16:44:04', NULL, '2023-12-22 16:44:04', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1738486223826657282, '000000', 'panda/2023/12/23/dea2efa96eb64ef2b7661de7a1085097.png', '微信图片_20231223170531.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/23/dea2efa96eb64ef2b7661de7a1085097.png', NULL, '2023-12-23 17:06:30', NULL, '2023-12-23 17:06:30', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1738488595479076865, '000000', 'panda/2023/12/23/5742e78d1a1143ec87857608b39d81d2.png', 'test.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/23/5742e78d1a1143ec87857608b39d81d2.png', NULL, '2023-12-23 17:15:55', NULL, '2023-12-23 17:15:55', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1739860803409485826, '000000', 'panda/2023/12/27/ebcd3065114347419701df78c8164938.png', 'Midjourney绘画2.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/27/ebcd3065114347419701df78c8164938.png', NULL, '2023-12-27 12:08:35', NULL, '2023-12-27 12:08:35', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1740753638301171714, '000000', 'panda/2023/12/29/6f2a2127c30346ac916577bf5534da25.png', '屏幕截图(1).png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/29/6f2a2127c30346ac916577bf5534da25.png', NULL, '2023-12-29 23:16:24', NULL, '2023-12-29 23:16:24', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1741349001566375937, '000000', 'panda/2023/12/31/5567e12262c0486daa28bb03ed2af4fb.png', '100x124.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/31/5567e12262c0486daa28bb03ed2af4fb.png', NULL, '2023-12-31 14:42:09', NULL, '2023-12-31 14:42:09', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1741349297885564929, '000000', 'panda/2023/12/31/26b1e8ddb3b24d0ab8e474afd8b3e896.png', '100x124.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2023/12/31/26b1e8ddb3b24d0ab8e474afd8b3e896.png', NULL, '2023-12-31 14:43:20', NULL, '2023-12-31 14:43:20', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1741824910417219586, '000000', 'panda/2024/01/01/0e9dfd1c168646bc93322217b6296a70.png', 'nginx2.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2024/01/01/0e9dfd1c168646bc93322217b6296a70.png', NULL, '2024-01-01 22:13:15', NULL, '2024-01-01 22:13:15', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1742149439661359105, '000000', 'panda/2024/01/02/76439aa1dce54f3eb58a57fe55fd6dba.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2024/01/02/76439aa1dce54f3eb58a57fe55fd6dba.png', NULL, '2024-01-02 19:42:49', NULL, '2024-01-02 19:42:49', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1742149598428348418, '000000', 'panda/2024/01/02/d83e67d8142246dcb97f0ed49bb697f8.jpg', 'car.jpg', '.jpg', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2024/01/02/d83e67d8142246dcb97f0ed49bb697f8.jpg', NULL, '2024-01-02 19:43:27', NULL, '2024-01-02 19:43:27', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1742149624500142081, '000000', 'panda/2024/01/02/726de4384d4d425eaf1ada3f985e4f32.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2024/01/02/726de4384d4d425eaf1ada3f985e4f32.png', NULL, '2024-01-02 19:43:33', NULL, '2024-01-02 19:43:33', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1742158338359664641, '000000', 'panda/2024/01/02/3425e42cda3f4c39a54b3596be3227e9.jpg', 'car.jpg', '.jpg', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2024/01/02/3425e42cda3f4c39a54b3596be3227e9.jpg', NULL, '2024-01-02 20:18:10', 1714176194496339970, '2024-01-02 20:18:10', 1714176194496339970, 'qcloud');
INSERT INTO `sys_oss` VALUES (1742158569625198593, '000000', 'panda/2024/01/02/d4874f5dd5c24e5380dd1852cf4495e3.png', '私有知识库业务架构图.drawio.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2024/01/02/d4874f5dd5c24e5380dd1852cf4495e3.png', NULL, '2024-01-02 20:19:05', 1714176194496339970, '2024-01-02 20:19:05', 1714176194496339970, 'qcloud');
INSERT INTO `sys_oss` VALUES (1742158983951118338, '000000', 'panda/2024/01/02/01921182df474945a7572efafcc9584b.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2024/01/02/01921182df474945a7572efafcc9584b.png', NULL, '2024-01-02 20:20:44', 1714176194496339970, '2024-01-02 20:20:44', 1714176194496339970, 'qcloud');
INSERT INTO `sys_oss` VALUES (1742363801802051585, '000000', 'panda/2024/01/03/050a1eb181064a38ae75b1b7100fd81a.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2024/01/03/050a1eb181064a38ae75b1b7100fd81a.png', NULL, '2024-01-03 09:54:37', 1741845883943227393, '2024-01-03 09:54:37', 1741845883943227393, 'qcloud');
INSERT INTO `sys_oss` VALUES (1742375260808364033, '000000', 'panda/2024/01/03/fa0b93aa67bb42ed9dbd04db4c4d7e3d.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2024/01/03/fa0b93aa67bb42ed9dbd04db4c4d7e3d.png', NULL, '2024-01-03 10:40:09', 1714176194496339970, '2024-01-03 10:40:09', 1714176194496339970, 'qcloud');
INSERT INTO `sys_oss` VALUES (1742406106978271234, '000000', 'panda/2024/01/03/3d559970a7ba48c48b9c13f6996a6263.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2024/01/03/3d559970a7ba48c48b9c13f6996a6263.png', NULL, '2024-01-03 12:42:43', 1714176194496339970, '2024-01-03 12:42:43', 1714176194496339970, 'qcloud');
INSERT INTO `sys_oss` VALUES (1742416247656103937, '000000', 'panda/2024/01/03/9eb180968b164131a92d30b1cfdd0779.png', '私有知识库业务架构图.drawio.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2024/01/03/9eb180968b164131a92d30b1cfdd0779.png', NULL, '2024-01-03 13:23:01', 1714176194496339970, '2024-01-03 13:23:01', 1714176194496339970, 'qcloud');
INSERT INTO `sys_oss` VALUES (1742425665852489729, '000000', 'panda/2024/01/03/613d64ca45fd4d51bb6b246c1d7587c1.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2024/01/03/613d64ca45fd4d51bb6b246c1d7587c1.png', NULL, '2024-01-03 14:00:26', 1714176194496339970, '2024-01-03 14:00:26', 1714176194496339970, 'qcloud');
INSERT INTO `sys_oss` VALUES (1742425883994046466, '000000', 'panda/2024/01/03/9709adf9edf74b51865f72b78bbd6b5e.jpg', 'car.jpg', '.jpg', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2024/01/03/9709adf9edf74b51865f72b78bbd6b5e.jpg', NULL, '2024-01-03 14:01:18', 1714176194496339970, '2024-01-03 14:01:18', 1714176194496339970, 'qcloud');
INSERT INTO `sys_oss` VALUES (1742425928411725826, '000000', 'panda/2024/01/03/0e3600b455914b0dade9943f281be19b.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2024/01/03/0e3600b455914b0dade9943f281be19b.png', NULL, '2024-01-03 14:01:29', 1714176194496339970, '2024-01-03 14:01:29', 1714176194496339970, 'qcloud');
INSERT INTO `sys_oss` VALUES (1742540735914782721, '000000', 'panda/2024/01/03/cc6722915e274b519c30026258efae97.png', 'pd.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2024/01/03/cc6722915e274b519c30026258efae97.png', NULL, '2024-01-03 21:37:41', NULL, '2024-01-03 21:37:41', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1742798084927737857, '000000', 'panda/2024/01/04/9e7d1b86af2646cc86b305b4dd474653.png', '4.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2024/01/04/9e7d1b86af2646cc86b305b4dd474653.png', NULL, '2024-01-04 14:40:18', NULL, '2024-01-04 14:40:18', NULL, 'qcloud');
INSERT INTO `sys_oss` VALUES (1742798206709354497, '000000', 'panda/2024/01/04/7fa695b38eb74e38aa5fc67837a54f60.png', 'image.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2024/01/04/7fa695b38eb74e38aa5fc67837a54f60.png', NULL, '2024-01-04 14:40:47', 1722083875718737921, '2024-01-04 14:40:47', 1722083875718737921, 'qcloud');
INSERT INTO `sys_oss` VALUES (1742798249252179970, '000000', 'panda/2024/01/04/12aa0f1308f8429bacfdec966803e75b.png', 'image.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2024/01/04/12aa0f1308f8429bacfdec966803e75b.png', NULL, '2024-01-04 14:40:57', 1722083875718737921, '2024-01-04 14:40:57', 1722083875718737921, 'qcloud');
INSERT INTO `sys_oss` VALUES (1742800939394879489, '000000', 'panda/2024/01/04/022e9626b8314ddd8b5ddb25b53605c7.png', '29bc4a8389ea73fb8170408b39784e09a13011.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2024/01/04/022e9626b8314ddd8b5ddb25b53605c7.png', NULL, '2024-01-04 14:51:38', 1714536425072115714, '2024-01-04 14:51:38', 1714536425072115714, 'qcloud');
INSERT INTO `sys_oss` VALUES (1742808488634568705, '000000', 'panda/2024/01/04/876792ea9c5f4f89b683fae739a08f56.png', 'image.png', '.png', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2024/01/04/876792ea9c5f4f89b683fae739a08f56.png', NULL, '2024-01-04 15:21:38', 1714820415783976961, '2024-01-04 15:21:38', 1714820415783976961, 'qcloud');
INSERT INTO `sys_oss` VALUES (1742844890541805570, '000000', 'panda/2024/01/04/dd5bdec65131411b8616d0592152f127.jpg', 'psc.jpg', '.jpg', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2024/01/04/dd5bdec65131411b8616d0592152f127.jpg', NULL, '2024-01-04 17:46:17', 1716076523383177217, '2024-01-04 17:46:17', 1716076523383177217, 'qcloud');
INSERT INTO `sys_oss` VALUES (1743081813143277569, '000000', 'panda/2024/01/05/877152fc53ac42b59356fec648435d62.jpg', '124_20231222111128A001.jpg', '.jpg', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2024/01/05/877152fc53ac42b59356fec648435d62.jpg', NULL, '2024-01-05 09:27:44', 1743079200725225474, '2024-01-05 09:27:44', 1743079200725225474, 'qcloud');
INSERT INTO `sys_oss` VALUES (1743458219735973890, '000000', 'panda/2024/01/06/21ea270d16f9497e9cc4d52b876b0398.jpg', '1702362792732.jpg', '.jpg', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2024/01/06/21ea270d16f9497e9cc4d52b876b0398.jpg', 103, '2024-01-06 10:23:26', 1713724795803299841, '2024-01-06 10:23:26', 1713724795803299841, 'qcloud');

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
INSERT INTO `sys_oss_config` VALUES (4, '000000', 'qcloud', 'AKIDfDeswnSxypgUG0ncQ8lGbp5EAub8QTCj', 'JOEvQhldIl26wZx62qTt665uYxAAxXv8', 'panda-1253683406', 'panda', 'cos.ap-guangzhou.myqcloud.com', '', 'N', 'ap-guangzhou', '1', '0', '', 103, 1, '2023-05-14 15:19:42', 1, '2023-11-13 23:58:09', '');
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
INSERT INTO `sys_tenant` VALUES (1729685490647072769, '911866', '陈', '11111111111', '5126', '', '', '', '', '', 1729685389795033090, NULL, 1, '0', '2', 103, 1, '2023-11-29 10:15:32', 1, '2023-11-29 10:15:32');

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
INSERT INTO `sys_user` VALUES (1, NULL, '0', 4.00, '00000', 103, 'admin', '疯狂的狮子Li', 'sys_user', 'crazyLionLi@163.com', '15888888888', '1', NULL, NULL, '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '0:0:0:0:0:0:0:1', '2024-01-05 17:11:34', 103, 1, '2023-05-14 15:19:39', 1, '2024-01-05 17:11:34', '管理员');

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
INSERT INTO `sys_user_post` VALUES (2, 2);
INSERT INTO `sys_user_post` VALUES (1661660085084250114, 2);
INSERT INTO `sys_user_post` VALUES (1661660804847788034, 1);

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
INSERT INTO `sys_user_role` VALUES (2, 2);
INSERT INTO `sys_user_role` VALUES (3, 3);
INSERT INTO `sys_user_role` VALUES (4, 4);
INSERT INTO `sys_user_role` VALUES (1661646824293031937, 1661661183933177857);
INSERT INTO `sys_user_role` VALUES (1661660085084250114, 1661661183933177857);
INSERT INTO `sys_user_role` VALUES (1661660804847788034, 2);
INSERT INTO `sys_user_role` VALUES (1713427806956404738, 1);
INSERT INTO `sys_user_role` VALUES (1713439839684689921, 1);
INSERT INTO `sys_user_role` VALUES (1713440206715650049, 1);
INSERT INTO `sys_user_role` VALUES (1713724795803299841, 1);
INSERT INTO `sys_user_role` VALUES (1714176194496339970, 1);
INSERT INTO `sys_user_role` VALUES (1714267685998907393, 1);
INSERT INTO `sys_user_role` VALUES (1714269581270667265, 1);
INSERT INTO `sys_user_role` VALUES (1714270420659949569, 1);
INSERT INTO `sys_user_role` VALUES (1714455864827723777, 1);
INSERT INTO `sys_user_role` VALUES (1714536425072115714, 1);
INSERT INTO `sys_user_role` VALUES (1714819715117105153, 1);
INSERT INTO `sys_user_role` VALUES (1714820415783976961, 1);
INSERT INTO `sys_user_role` VALUES (1714820611611836417, 1);
INSERT INTO `sys_user_role` VALUES (1714820755698761729, 1);
INSERT INTO `sys_user_role` VALUES (1714823588305190914, 1);
INSERT INTO `sys_user_role` VALUES (1714829502936530945, 1);
INSERT INTO `sys_user_role` VALUES (1714835527643185154, 1);
INSERT INTO `sys_user_role` VALUES (1714835835278606337, 1);
INSERT INTO `sys_user_role` VALUES (1714898663033290754, 1);
INSERT INTO `sys_user_role` VALUES (1714942733206175746, 1);
INSERT INTO `sys_user_role` VALUES (1714943378361434113, 1);
INSERT INTO `sys_user_role` VALUES (1714943388671033346, 1);
INSERT INTO `sys_user_role` VALUES (1714945928464711682, 1);
INSERT INTO `sys_user_role` VALUES (1714946100850606082, 1);
INSERT INTO `sys_user_role` VALUES (1714952355237347329, 1);
INSERT INTO `sys_user_role` VALUES (1714954192279584770, 1);
INSERT INTO `sys_user_role` VALUES (1714960721598758913, 1);
INSERT INTO `sys_user_role` VALUES (1714961357132283906, 1);
INSERT INTO `sys_user_role` VALUES (1714963426656403458, 1);
INSERT INTO `sys_user_role` VALUES (1714980339130318850, 1);
INSERT INTO `sys_user_role` VALUES (1714985002550444034, 1);
INSERT INTO `sys_user_role` VALUES (1714996959085084674, 1);
INSERT INTO `sys_user_role` VALUES (1715000784541990913, 1);
INSERT INTO `sys_user_role` VALUES (1715160830886297602, 1);
INSERT INTO `sys_user_role` VALUES (1715174792021426177, 1);
INSERT INTO `sys_user_role` VALUES (1715176760861278209, 1);
INSERT INTO `sys_user_role` VALUES (1715187418688405506, 1);
INSERT INTO `sys_user_role` VALUES (1715263570077564930, 1);
INSERT INTO `sys_user_role` VALUES (1715273299113820162, 1);
INSERT INTO `sys_user_role` VALUES (1715289765028577281, 1);
INSERT INTO `sys_user_role` VALUES (1715642509052624897, 1);
INSERT INTO `sys_user_role` VALUES (1715645217792868353, 1);
INSERT INTO `sys_user_role` VALUES (1715655140035543041, 1);
INSERT INTO `sys_user_role` VALUES (1715688813166346242, 1);
INSERT INTO `sys_user_role` VALUES (1715695623109623810, 1);
INSERT INTO `sys_user_role` VALUES (1716076523383177217, 1);
INSERT INTO `sys_user_role` VALUES (1716077329079615490, 1);
INSERT INTO `sys_user_role` VALUES (1716316658037178370, 1);
INSERT INTO `sys_user_role` VALUES (1716375479287824386, 1);
INSERT INTO `sys_user_role` VALUES (1716376929359380482, 1);
INSERT INTO `sys_user_role` VALUES (1716449431389487106, 1);
INSERT INTO `sys_user_role` VALUES (1716626232627707906, 1);
INSERT INTO `sys_user_role` VALUES (1716668774639484929, 1);
INSERT INTO `sys_user_role` VALUES (1716723582348050434, 1);
INSERT INTO `sys_user_role` VALUES (1717010625036828674, 1);
INSERT INTO `sys_user_role` VALUES (1717112818712723458, 1);
INSERT INTO `sys_user_role` VALUES (1717171039955599361, 1);
INSERT INTO `sys_user_role` VALUES (1717382776042569730, 1);
INSERT INTO `sys_user_role` VALUES (1717383874597896194, 1);
INSERT INTO `sys_user_role` VALUES (1717463477270102018, 1);
INSERT INTO `sys_user_role` VALUES (1717550755342467074, 1);
INSERT INTO `sys_user_role` VALUES (1718643906618605569, 1);
INSERT INTO `sys_user_role` VALUES (1719357065528623105, 1);
INSERT INTO `sys_user_role` VALUES (1719629669720145921, 1);
INSERT INTO `sys_user_role` VALUES (1719631746265530370, 1);
INSERT INTO `sys_user_role` VALUES (1719969371128086529, 1);
INSERT INTO `sys_user_role` VALUES (1719994192431955970, 1);
INSERT INTO `sys_user_role` VALUES (1720001597920264194, 1);
INSERT INTO `sys_user_role` VALUES (1720054174099718145, 1);
INSERT INTO `sys_user_role` VALUES (1720373256426635265, 1);
INSERT INTO `sys_user_role` VALUES (1720615324298264578, 1);
INSERT INTO `sys_user_role` VALUES (1720966085100191746, 1);
INSERT INTO `sys_user_role` VALUES (1721433118342397954, 1);
INSERT INTO `sys_user_role` VALUES (1721798759096270850, 1);
INSERT INTO `sys_user_role` VALUES (1721869407395332097, 1);
INSERT INTO `sys_user_role` VALUES (1721869952080232450, 1);
INSERT INTO `sys_user_role` VALUES (1722083875718737921, 1);
INSERT INTO `sys_user_role` VALUES (1722126825769185282, 1);
INSERT INTO `sys_user_role` VALUES (1722453238653169665, 1);
INSERT INTO `sys_user_role` VALUES (1722501722198552577, 1);
INSERT INTO `sys_user_role` VALUES (1722546398997819394, 1);
INSERT INTO `sys_user_role` VALUES (1722635856464097281, 1);
INSERT INTO `sys_user_role` VALUES (1722652602847768578, 1);
INSERT INTO `sys_user_role` VALUES (1722787874222682114, 1);
INSERT INTO `sys_user_role` VALUES (1722799180870889473, 1);
INSERT INTO `sys_user_role` VALUES (1722872660475817986, 1);
INSERT INTO `sys_user_role` VALUES (1722874592401600514, 1);
INSERT INTO `sys_user_role` VALUES (1722883137289367554, 1);
INSERT INTO `sys_user_role` VALUES (1722918534182645762, 1);
INSERT INTO `sys_user_role` VALUES (1723173295586848769, 1);
INSERT INTO `sys_user_role` VALUES (1723222687891107841, 1);
INSERT INTO `sys_user_role` VALUES (1723224404040921089, 1);
INSERT INTO `sys_user_role` VALUES (1723225015520112641, 1);
INSERT INTO `sys_user_role` VALUES (1723278284531478529, 1);
INSERT INTO `sys_user_role` VALUES (1723330835209564161, 1);
INSERT INTO `sys_user_role` VALUES (1723708198137147393, 1);
INSERT INTO `sys_user_role` VALUES (1723754683843260417, 1);
INSERT INTO `sys_user_role` VALUES (1723878185250369537, 1);
INSERT INTO `sys_user_role` VALUES (1723940614634254337, 1);
INSERT INTO `sys_user_role` VALUES (1723975861757325314, 1);
INSERT INTO `sys_user_role` VALUES (1724306907803725826, 1);
INSERT INTO `sys_user_role` VALUES (1724308252862492673, 1);
INSERT INTO `sys_user_role` VALUES (1724382895124295681, 1);
INSERT INTO `sys_user_role` VALUES (1724727778758406145, 1);
INSERT INTO `sys_user_role` VALUES (1724815478295425026, 1);
INSERT INTO `sys_user_role` VALUES (1725026071145107458, 1);
INSERT INTO `sys_user_role` VALUES (1725026978817658881, 1);
INSERT INTO `sys_user_role` VALUES (1725043562961457154, 1);
INSERT INTO `sys_user_role` VALUES (1725058936893362178, 1);
INSERT INTO `sys_user_role` VALUES (1725363117009162242, 1);
INSERT INTO `sys_user_role` VALUES (1725538633251049474, 1);
INSERT INTO `sys_user_role` VALUES (1725564937467875329, 1);
INSERT INTO `sys_user_role` VALUES (1725891713243021314, 1);
INSERT INTO `sys_user_role` VALUES (1725905000621932546, 1);
INSERT INTO `sys_user_role` VALUES (1726440708294049793, 1);
INSERT INTO `sys_user_role` VALUES (1726443526979584002, 1);
INSERT INTO `sys_user_role` VALUES (1726445663797116929, 1);
INSERT INTO `sys_user_role` VALUES (1726452867329687553, 1);
INSERT INTO `sys_user_role` VALUES (1726472827451998209, 1);
INSERT INTO `sys_user_role` VALUES (1726479651370696705, 1);
INSERT INTO `sys_user_role` VALUES (1726487492674195458, 1);
INSERT INTO `sys_user_role` VALUES (1726496513055784961, 1);
INSERT INTO `sys_user_role` VALUES (1726498781398302722, 1);
INSERT INTO `sys_user_role` VALUES (1726506873632587778, 1);
INSERT INTO `sys_user_role` VALUES (1726529248394739714, 1);
INSERT INTO `sys_user_role` VALUES (1726578079102664705, 1);
INSERT INTO `sys_user_role` VALUES (1726582181383634946, 1);
INSERT INTO `sys_user_role` VALUES (1726583555672506369, 1);
INSERT INTO `sys_user_role` VALUES (1726596448690372609, 1);
INSERT INTO `sys_user_role` VALUES (1726599361261207553, 1);
INSERT INTO `sys_user_role` VALUES (1726604511749079041, 1);
INSERT INTO `sys_user_role` VALUES (1726606973822304258, 1);
INSERT INTO `sys_user_role` VALUES (1726609379524083713, 1);
INSERT INTO `sys_user_role` VALUES (1726616151265640450, 1);
INSERT INTO `sys_user_role` VALUES (1726775811478126594, 1);
INSERT INTO `sys_user_role` VALUES (1726795490141667329, 1);
INSERT INTO `sys_user_role` VALUES (1726798403169681410, 1);
INSERT INTO `sys_user_role` VALUES (1726830794655399937, 1);
INSERT INTO `sys_user_role` VALUES (1726862038013313026, 1);
INSERT INTO `sys_user_role` VALUES (1726919220696186882, 1);
INSERT INTO `sys_user_role` VALUES (1727140184050630658, 1);
INSERT INTO `sys_user_role` VALUES (1727506163368722433, 1);
INSERT INTO `sys_user_role` VALUES (1727518983086931969, 1);
INSERT INTO `sys_user_role` VALUES (1727580969606840321, 1);
INSERT INTO `sys_user_role` VALUES (1727590505323429890, 1);
INSERT INTO `sys_user_role` VALUES (1727918393172164609, 1);
INSERT INTO `sys_user_role` VALUES (1728249002000121857, 1);
INSERT INTO `sys_user_role` VALUES (1728680561446486017, 1);
INSERT INTO `sys_user_role` VALUES (1728964404182577153, 1);
INSERT INTO `sys_user_role` VALUES (1729020459675611137, 1);
INSERT INTO `sys_user_role` VALUES (1729051002043691009, 1);
INSERT INTO `sys_user_role` VALUES (1729423744832172033, 1);
INSERT INTO `sys_user_role` VALUES (1729429590291050497, 1);
INSERT INTO `sys_user_role` VALUES (1729685493222375426, 1729685491108446210);
INSERT INTO `sys_user_role` VALUES (1730050324466036738, 1);
INSERT INTO `sys_user_role` VALUES (1730102403335254018, 1);
INSERT INTO `sys_user_role` VALUES (1730129923250122754, 1);
INSERT INTO `sys_user_role` VALUES (1730155108925763586, 1);
INSERT INTO `sys_user_role` VALUES (1730273428207366145, 1);
INSERT INTO `sys_user_role` VALUES (1730498722784669697, 1);
INSERT INTO `sys_user_role` VALUES (1730815105229713410, 1);
INSERT INTO `sys_user_role` VALUES (1730858886951923714, 1);
INSERT INTO `sys_user_role` VALUES (1731357405659824130, 1);
INSERT INTO `sys_user_role` VALUES (1731475532557090818, 1);
INSERT INTO `sys_user_role` VALUES (1731480953627901953, 1);
INSERT INTO `sys_user_role` VALUES (1731502381106495490, 1);
INSERT INTO `sys_user_role` VALUES (1731524458442162177, 1);
INSERT INTO `sys_user_role` VALUES (1731524630094053377, 1);
INSERT INTO `sys_user_role` VALUES (1731524650293821441, 1);
INSERT INTO `sys_user_role` VALUES (1731529253710233601, 1);
INSERT INTO `sys_user_role` VALUES (1731559936046432258, 1);
INSERT INTO `sys_user_role` VALUES (1731564032228884482, 1);
INSERT INTO `sys_user_role` VALUES (1731565926737281026, 1);
INSERT INTO `sys_user_role` VALUES (1731566918589513729, 1);
INSERT INTO `sys_user_role` VALUES (1731567740094283778, 1);
INSERT INTO `sys_user_role` VALUES (1731575439263563777, 1);
INSERT INTO `sys_user_role` VALUES (1731583864055824385, 1);
INSERT INTO `sys_user_role` VALUES (1731588155382464513, 1);
INSERT INTO `sys_user_role` VALUES (1731589827840212993, 1);
INSERT INTO `sys_user_role` VALUES (1731635461435719682, 1);
INSERT INTO `sys_user_role` VALUES (1731668049902731266, 1);
INSERT INTO `sys_user_role` VALUES (1731922694168412162, 1);
INSERT INTO `sys_user_role` VALUES (1731944975456305153, 1);
INSERT INTO `sys_user_role` VALUES (1731949019394506753, 1);
INSERT INTO `sys_user_role` VALUES (1731951425054343170, 1);
INSERT INTO `sys_user_role` VALUES (1732000242621513729, 1);
INSERT INTO `sys_user_role` VALUES (1732027163380056066, 1);
INSERT INTO `sys_user_role` VALUES (1732289382269353985, 1);
INSERT INTO `sys_user_role` VALUES (1732289439282528258, 1);
INSERT INTO `sys_user_role` VALUES (1732289699585228801, 1);
INSERT INTO `sys_user_role` VALUES (1732290827173527553, 1);
INSERT INTO `sys_user_role` VALUES (1732291549344595969, 1);
INSERT INTO `sys_user_role` VALUES (1732293265184030721, 1);
INSERT INTO `sys_user_role` VALUES (1732329664117506049, 1);
INSERT INTO `sys_user_role` VALUES (1732334104450990081, 1);
INSERT INTO `sys_user_role` VALUES (1732578671045672962, 1);
INSERT INTO `sys_user_role` VALUES (1732584047426174978, 1);
INSERT INTO `sys_user_role` VALUES (1732608690321129474, 1);
INSERT INTO `sys_user_role` VALUES (1732678147815014401, 1);
INSERT INTO `sys_user_role` VALUES (1732731410102910977, 1);
INSERT INTO `sys_user_role` VALUES (1733005266763939841, 1);
INSERT INTO `sys_user_role` VALUES (1733016149837774850, 1);
INSERT INTO `sys_user_role` VALUES (1733053523871432705, 1);
INSERT INTO `sys_user_role` VALUES (1733061400367497218, 1);
INSERT INTO `sys_user_role` VALUES (1733167090469732353, 1);
INSERT INTO `sys_user_role` VALUES (1733298702729641986, 1);
INSERT INTO `sys_user_role` VALUES (1733488544511983617, 1);
INSERT INTO `sys_user_role` VALUES (1733720554119659521, 1);
INSERT INTO `sys_user_role` VALUES (1733846657777827842, 1);
INSERT INTO `sys_user_role` VALUES (1733859832720031745, 1);
INSERT INTO `sys_user_role` VALUES (1734137817339559938, 1);
INSERT INTO `sys_user_role` VALUES (1734227535762849793, 1);
INSERT INTO `sys_user_role` VALUES (1734492373726560257, 1);
INSERT INTO `sys_user_role` VALUES (1734508040978726914, 1);
INSERT INTO `sys_user_role` VALUES (1734513545461661697, 1);
INSERT INTO `sys_user_role` VALUES (1734581580998451202, 1);
INSERT INTO `sys_user_role` VALUES (1734751884580298754, 1);
INSERT INTO `sys_user_role` VALUES (1734781716483612674, 1);
INSERT INTO `sys_user_role` VALUES (1734833221987278849, 1);
INSERT INTO `sys_user_role` VALUES (1734834063154946050, 1);
INSERT INTO `sys_user_role` VALUES (1734880697666576386, 1);
INSERT INTO `sys_user_role` VALUES (1734891995888427009, 1);
INSERT INTO `sys_user_role` VALUES (1735132534701367297, 1);
INSERT INTO `sys_user_role` VALUES (1735242647239991298, 1);
INSERT INTO `sys_user_role` VALUES (1735486862444273666, 1);
INSERT INTO `sys_user_role` VALUES (1735487912727355394, 1);
INSERT INTO `sys_user_role` VALUES (1735542352767426561, 1);
INSERT INTO `sys_user_role` VALUES (1735551915889598466, 1);
INSERT INTO `sys_user_role` VALUES (1735616653411557377, 1);
INSERT INTO `sys_user_role` VALUES (1735835864146714626, 1);
INSERT INTO `sys_user_role` VALUES (1735953007769100289, 1);
INSERT INTO `sys_user_role` VALUES (1735960189784891393, 1);
INSERT INTO `sys_user_role` VALUES (1736265950381547522, 1);
INSERT INTO `sys_user_role` VALUES (1736577606684844034, 1);
INSERT INTO `sys_user_role` VALUES (1736638822375563266, 1);
INSERT INTO `sys_user_role` VALUES (1736779069306511361, 1);
INSERT INTO `sys_user_role` VALUES (1737028378602053634, 1);
INSERT INTO `sys_user_role` VALUES (1737271234797314050, 1);
INSERT INTO `sys_user_role` VALUES (1737315322405920770, 1);
INSERT INTO `sys_user_role` VALUES (1737445221154234370, 1);
INSERT INTO `sys_user_role` VALUES (1737452907568635906, 1);
INSERT INTO `sys_user_role` VALUES (1737453186955419649, 1);
INSERT INTO `sys_user_role` VALUES (1737717777685880833, 1);
INSERT INTO `sys_user_role` VALUES (1737768515594166274, 1);
INSERT INTO `sys_user_role` VALUES (1738108912170246145, 1);
INSERT INTO `sys_user_role` VALUES (1738118086488825858, 1);
INSERT INTO `sys_user_role` VALUES (1738520430804279297, 1);
INSERT INTO `sys_user_role` VALUES (1738802060248817666, 1);
INSERT INTO `sys_user_role` VALUES (1738812447119712257, 1);
INSERT INTO `sys_user_role` VALUES (1738941480197234689, 1);
INSERT INTO `sys_user_role` VALUES (1738963430776840194, 1);
INSERT INTO `sys_user_role` VALUES (1739121784341995522, 1);
INSERT INTO `sys_user_role` VALUES (1739166931951886338, 1);
INSERT INTO `sys_user_role` VALUES (1739272055240073217, 1);
INSERT INTO `sys_user_role` VALUES (1739451838930427905, 1);
INSERT INTO `sys_user_role` VALUES (1739452037375533057, 1);
INSERT INTO `sys_user_role` VALUES (1739452376946384898, 1);
INSERT INTO `sys_user_role` VALUES (1739484503888961537, 1);
INSERT INTO `sys_user_role` VALUES (1739485282335006722, 1);
INSERT INTO `sys_user_role` VALUES (1739577551431999490, 1);
INSERT INTO `sys_user_role` VALUES (1739825609910591489, 1);
INSERT INTO `sys_user_role` VALUES (1739916453439152130, 1);
INSERT INTO `sys_user_role` VALUES (1740188388454629378, 1);
INSERT INTO `sys_user_role` VALUES (1741339991320580097, 1);
INSERT INTO `sys_user_role` VALUES (1741803737633542145, 1);
INSERT INTO `sys_user_role` VALUES (1741823858229923841, 1);
INSERT INTO `sys_user_role` VALUES (1741845883943227393, 1);
INSERT INTO `sys_user_role` VALUES (1742179775941201921, 1);
INSERT INTO `sys_user_role` VALUES (1742437553771458562, 1);
INSERT INTO `sys_user_role` VALUES (1742451201315254273, 1);
INSERT INTO `sys_user_role` VALUES (1742469913120419841, 1);
INSERT INTO `sys_user_role` VALUES (1742798283280568321, 1);
INSERT INTO `sys_user_role` VALUES (1742798987701342210, 1);
INSERT INTO `sys_user_role` VALUES (1742799476950126594, 1);
INSERT INTO `sys_user_role` VALUES (1742799839619010562, 1);
INSERT INTO `sys_user_role` VALUES (1742801019527057410, 1);
INSERT INTO `sys_user_role` VALUES (1742804073915699202, 1);
INSERT INTO `sys_user_role` VALUES (1742821280687149058, 1);
INSERT INTO `sys_user_role` VALUES (1742821467476283394, 1);
INSERT INTO `sys_user_role` VALUES (1742822775600009217, 1);
INSERT INTO `sys_user_role` VALUES (1742823890928357377, 1);
INSERT INTO `sys_user_role` VALUES (1742838225297821697, 1);
INSERT INTO `sys_user_role` VALUES (1742902317295423490, 1);
INSERT INTO `sys_user_role` VALUES (1742910854243373058, 1);
INSERT INTO `sys_user_role` VALUES (1742961994725150721, 1);
INSERT INTO `sys_user_role` VALUES (1742969861079388161, 1);
INSERT INTO `sys_user_role` VALUES (1743068363130228737, 1);
INSERT INTO `sys_user_role` VALUES (1743075924621479938, 1);
INSERT INTO `sys_user_role` VALUES (1743079200725225474, 1);
INSERT INTO `sys_user_role` VALUES (1743085878682144769, 1);
INSERT INTO `sys_user_role` VALUES (1743110774967586818, 1);
INSERT INTO `sys_user_role` VALUES (1743162481042870274, 1);
INSERT INTO `sys_user_role` VALUES (1743166491284033537, 1);
INSERT INTO `sys_user_role` VALUES (1743251016219447297, 1);
INSERT INTO `sys_user_role` VALUES (1743469820367142914, 1);
INSERT INTO `sys_user_role` VALUES (1743514389280522242, 1);
INSERT INTO `sys_user_role` VALUES (1743519646916083714, 1);
INSERT INTO `sys_user_role` VALUES (1743670356026654722, 1);

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
INSERT INTO `test_demo` VALUES (1, '000000', 102, 4, 1, '测试数据权限', '测试', 0, 103, '2023-05-14 15:20:01', 1, NULL, NULL, 0);
INSERT INTO `test_demo` VALUES (2, '000000', 102, 3, 2, '子节点1', '111', 0, 103, '2023-05-14 15:20:01', 1, NULL, NULL, 0);
INSERT INTO `test_demo` VALUES (3, '000000', 102, 3, 3, '子节点2', '222', 0, 103, '2023-05-14 15:20:01', 1, NULL, NULL, 0);
INSERT INTO `test_demo` VALUES (4, '000000', 108, 4, 4, '测试数据', 'demo', 0, 103, '2023-05-14 15:20:01', 1, NULL, NULL, 0);
INSERT INTO `test_demo` VALUES (5, '000000', 108, 3, 13, '子节点11', '1111', 0, 103, '2023-05-14 15:20:01', 1, NULL, NULL, 0);
INSERT INTO `test_demo` VALUES (6, '000000', 108, 3, 12, '子节点22', '2222', 0, 103, '2023-05-14 15:20:01', 1, NULL, NULL, 0);
INSERT INTO `test_demo` VALUES (7, '000000', 108, 3, 11, '子节点33', '3333', 0, 103, '2023-05-14 15:20:01', 1, NULL, NULL, 0);
INSERT INTO `test_demo` VALUES (8, '000000', 108, 3, 10, '子节点44', '4444', 0, 103, '2023-05-14 15:20:01', 1, NULL, NULL, 0);
INSERT INTO `test_demo` VALUES (9, '000000', 108, 3, 9, '子节点55', '5555', 0, 103, '2023-05-14 15:20:01', 1, NULL, NULL, 0);
INSERT INTO `test_demo` VALUES (10, '000000', 108, 3, 8, '子节点66', '6666', 0, 103, '2023-05-14 15:20:01', 1, NULL, NULL, 0);
INSERT INTO `test_demo` VALUES (11, '000000', 108, 3, 7, '子节点77', '7777', 0, 103, '2023-05-14 15:20:01', 1, NULL, NULL, 0);
INSERT INTO `test_demo` VALUES (12, '000000', 108, 3, 6, '子节点88', '8888', 0, 103, '2023-05-14 15:20:01', 1, NULL, NULL, 0);
INSERT INTO `test_demo` VALUES (13, '000000', 108, 3, 5, '子节点99', '9999', 0, 103, '2023-05-14 15:20:01', 1, NULL, NULL, 0);

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
INSERT INTO `test_tree` VALUES (1, '000000', 0, 102, 4, '测试数据权限', 0, 103, '2023-05-14 15:20:01', 1, NULL, NULL, 0);
INSERT INTO `test_tree` VALUES (2, '000000', 1, 102, 3, '子节点1', 0, 103, '2023-05-14 15:20:01', 1, NULL, NULL, 0);
INSERT INTO `test_tree` VALUES (3, '000000', 2, 102, 3, '子节点2', 0, 103, '2023-05-14 15:20:01', 1, NULL, NULL, 0);
INSERT INTO `test_tree` VALUES (4, '000000', 0, 108, 4, '测试树1', 0, 103, '2023-05-14 15:20:01', 1, NULL, NULL, 0);
INSERT INTO `test_tree` VALUES (5, '000000', 4, 108, 3, '子节点11', 0, 103, '2023-05-14 15:20:01', 1, NULL, NULL, 0);
INSERT INTO `test_tree` VALUES (6, '000000', 4, 108, 3, '子节点22', 0, 103, '2023-05-14 15:20:01', 1, NULL, NULL, 0);
INSERT INTO `test_tree` VALUES (7, '000000', 4, 108, 3, '子节点33', 0, 103, '2023-05-14 15:20:01', 1, NULL, NULL, 0);
INSERT INTO `test_tree` VALUES (8, '000000', 5, 108, 3, '子节点44', 0, 103, '2023-05-14 15:20:01', 1, NULL, NULL, 0);
INSERT INTO `test_tree` VALUES (9, '000000', 6, 108, 3, '子节点55', 0, 103, '2023-05-14 15:20:01', 1, NULL, NULL, 0);
INSERT INTO `test_tree` VALUES (10, '000000', 7, 108, 3, '子节点66', 0, 103, '2023-05-14 15:20:01', 1, NULL, NULL, 0);
INSERT INTO `test_tree` VALUES (11, '000000', 7, 108, 3, '子节点77', 0, 103, '2023-05-14 15:20:01', 1, NULL, NULL, 0);
INSERT INTO `test_tree` VALUES (12, '000000', 10, 108, 3, '子节点88', 0, 103, '2023-05-14 15:20:01', 1, NULL, NULL, 0);
INSERT INTO `test_tree` VALUES (13, '000000', 10, 108, 3, '子节点99', 0, 103, '2023-05-14 15:20:01', 1, NULL, NULL, 0);

SET FOREIGN_KEY_CHECKS = 1;
