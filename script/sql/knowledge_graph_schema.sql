-- ========================================
-- RuoYi AI 知识图谱数据库表结构
-- ========================================
-- 创建时间: 2025-09-30
-- 说明: 知识图谱功能的MySQL表结构
-- ========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 知识图谱实例表
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_graph_instance`;
CREATE TABLE `knowledge_graph_instance` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `graph_uuid` VARCHAR(32) NOT NULL COMMENT '图谱UUID',
    `knowledge_id` VARCHAR(50) NOT NULL COMMENT '关联knowledge_info.kid',
    `graph_name` VARCHAR(100) NOT NULL COMMENT '图谱名称',
    `graph_status` TINYINT(2) DEFAULT 10 COMMENT '构建状态：10构建中、20已完成、30失败',
    `node_count` INT(11) DEFAULT 0 COMMENT '节点数量',
    `relationship_count` INT(11) DEFAULT 0 COMMENT '关系数量',
    `config` JSON COMMENT '图谱配置(JSON格式)',
    `model_name` VARCHAR(100) DEFAULT NULL COMMENT 'LLM模型名称',
    `entity_types` VARCHAR(500) DEFAULT NULL COMMENT '实体类型（逗号分隔）',
    `relation_types` VARCHAR(500) DEFAULT NULL COMMENT '关系类型（逗号分隔）',
    `error_message` TEXT COMMENT '错误信息',
    `create_dept` BIGINT(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_graph_uuid` (`graph_uuid`) USING BTREE,
    KEY `idx_knowledge_id` (`knowledge_id`) USING BTREE,
    KEY `idx_graph_status` (`graph_status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识图谱实例表';

-- ----------------------------
-- 2. 实体类型定义表
-- ----------------------------
DROP TABLE IF EXISTS `graph_entity_type`;
CREATE TABLE `graph_entity_type` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `type_name` VARCHAR(50) NOT NULL COMMENT '实体类型名称',
    `type_code` VARCHAR(20) NOT NULL COMMENT '类型编码',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
    `color` VARCHAR(10) DEFAULT '#1890ff' COMMENT '可视化颜色',
    `icon` VARCHAR(50) DEFAULT NULL COMMENT '图标',
    `sort` INT(4) DEFAULT 0 COMMENT '显示顺序',
    `is_enable` TINYINT(1) DEFAULT 1 COMMENT '是否启用（0否 1是）',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_type_code` (`type_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图谱实体类型定义表';

-- ----------------------------
-- 3. 关系类型定义表
-- ----------------------------
DROP TABLE IF EXISTS `graph_relation_type`;
CREATE TABLE `graph_relation_type` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `relation_name` VARCHAR(50) NOT NULL COMMENT '关系名称',
    `relation_code` VARCHAR(20) NOT NULL COMMENT '关系编码',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
    `direction` TINYINT(1) DEFAULT 1 COMMENT '关系方向：0双向、1单向',
    `style` JSON COMMENT '可视化样式(JSON格式)',
    `sort` INT(4) DEFAULT 0 COMMENT '显示顺序',
    `is_enable` TINYINT(1) DEFAULT 1 COMMENT '是否启用（0否 1是）',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_relation_code` (`relation_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图谱关系类型定义表';

-- ----------------------------
-- 4. 图谱构建任务表
-- ----------------------------
DROP TABLE IF EXISTS `graph_build_task`;
CREATE TABLE `graph_build_task` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_uuid` VARCHAR(32) NOT NULL COMMENT '任务UUID',
    `graph_uuid` VARCHAR(32) NOT NULL COMMENT '图谱UUID',
    `knowledge_id` VARCHAR(50) NOT NULL COMMENT '知识库ID',
    `doc_id` VARCHAR(50) DEFAULT NULL COMMENT '文档ID（可选，null表示全量构建）',
    `task_type` TINYINT(2) DEFAULT 1 COMMENT '任务类型：1全量构建、2增量更新、3重建',
    `task_status` TINYINT(2) DEFAULT 1 COMMENT '任务状态：1待执行、2执行中、3成功、4失败',
    `progress` INT(3) DEFAULT 0 COMMENT '进度百分比（0-100）',
    `total_docs` INT(11) DEFAULT 0 COMMENT '总文档数',
    `processed_docs` INT(11) DEFAULT 0 COMMENT '已处理文档数',
    `extracted_entities` INT(11) DEFAULT 0 COMMENT '提取的实体数',
    `extracted_relations` INT(11) DEFAULT 0 COMMENT '提取的关系数',
    `error_message` TEXT COMMENT '错误信息',
    `result_summary` JSON COMMENT '结果摘要(JSON格式)',
    `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `create_dept` BIGINT(20) DEFAULT NULL COMMENT '创建部门',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_task_uuid` (`task_uuid`) USING BTREE,
    KEY `idx_graph_uuid` (`graph_uuid`) USING BTREE,
    KEY `idx_knowledge_id` (`knowledge_id`) USING BTREE,
    KEY `idx_task_status` (`task_status`) USING BTREE,
    KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图谱构建任务表';

-- ----------------------------
-- 5. 图谱查询历史表
-- ----------------------------
DROP TABLE IF EXISTS `graph_query_history`;
CREATE TABLE `graph_query_history` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `query_uuid` VARCHAR(32) NOT NULL COMMENT '查询UUID',
    `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
    `knowledge_id` VARCHAR(50) DEFAULT NULL COMMENT '知识库ID',
    `graph_uuid` VARCHAR(32) DEFAULT NULL COMMENT '图谱UUID',
    `query_text` TEXT NOT NULL COMMENT '查询文本',
    `query_type` TINYINT(2) DEFAULT 1 COMMENT '查询类型：1实体查询、2关系查询、3路径查询、4混合查询',
    `cypher_query` TEXT COMMENT '生成的Cypher查询',
    `result_count` INT(11) DEFAULT 0 COMMENT '结果数量',
    `response_time` INT(11) DEFAULT 0 COMMENT '响应时间(ms)',
    `is_success` TINYINT(1) DEFAULT 1 COMMENT '是否成功（0否 1是）',
    `error_message` TEXT COMMENT '错误信息',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_query_uuid` (`query_uuid`) USING BTREE,
    KEY `idx_user_id` (`user_id`) USING BTREE,
    KEY `idx_knowledge_id` (`knowledge_id`) USING BTREE,
    KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图谱查询历史表';

-- ----------------------------
-- 6. 图谱统计信息表
-- ----------------------------
DROP TABLE IF EXISTS `graph_statistics`;
CREATE TABLE `graph_statistics` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `graph_uuid` VARCHAR(32) NOT NULL COMMENT '图谱UUID',
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `total_nodes` INT(11) DEFAULT 0 COMMENT '总节点数',
    `total_relationships` INT(11) DEFAULT 0 COMMENT '总关系数',
    `node_type_distribution` JSON COMMENT '节点类型分布(JSON格式)',
    `relation_type_distribution` JSON COMMENT '关系类型分布(JSON格式)',
    `query_count` INT(11) DEFAULT 0 COMMENT '查询次数',
    `avg_query_time` INT(11) DEFAULT 0 COMMENT '平均查询时间(ms)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_graph_date` (`graph_uuid`, `stat_date`) USING BTREE,
    KEY `idx_stat_date` (`stat_date`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图谱统计信息表';

-- ----------------------------
-- 初始化基础数据：实体类型
-- ----------------------------
INSERT INTO `graph_entity_type` (`type_name`, `type_code`, `description`, `color`, `icon`, `sort`) VALUES
('人物', 'PERSON', '人物实体，包括真实人物和虚拟角色', '#1890ff', 'user', 1),
('机构', 'ORGANIZATION', '组织机构，包括公司、政府机构等', '#52c41a', 'bank', 2),
('地点', 'LOCATION', '地理位置，包括国家、城市、地址等', '#fa8c16', 'environment', 3),
('概念', 'CONCEPT', '抽象概念，包括理论、方法等', '#722ed1', 'bulb', 4),
('事件', 'EVENT', '事件记录，包括历史事件、活动等', '#eb2f96', 'calendar', 5),
('产品', 'PRODUCT', '产品或服务', '#13c2c2', 'shopping', 6),
('技术', 'TECHNOLOGY', '技术或工具', '#2f54eb', 'tool', 7),
('文档', 'DOCUMENT', '文档或资料', '#faad14', 'file-text', 8);

-- ----------------------------
-- 初始化基础数据：关系类型
-- ----------------------------
INSERT INTO `graph_relation_type` (`relation_name`, `relation_code`, `description`, `direction`, `sort`) VALUES
('属于', 'BELONGS_TO', '隶属关系，表示从属或归属', 1, 1),
('位于', 'LOCATED_IN', '地理位置关系', 1, 2),
('相关', 'RELATED_TO', '一般关联关系', 0, 3),
('导致', 'CAUSES', '因果关系', 1, 4),
('包含', 'CONTAINS', '包含关系', 1, 5),
('提及', 'MENTIONS', '文档提及实体的关系', 1, 6),
('部分', 'PART_OF', '部分关系', 1, 7),
('实例', 'INSTANCE_OF', '实例关系', 1, 8),
('相似', 'SIMILAR_TO', '相似关系', 0, 9),
('前序', 'PRECEDES', '时序关系', 1, 10),
('工作于', 'WORKS_AT', '人物与机构的工作关系', 1, 11),
('创建', 'CREATED_BY', '创建关系', 1, 12),
('使用', 'USES', '使用关系', 1, 13);

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------
-- 完成
-- ----------------------------
-- 知识图谱数据库表结构创建完成
-- 请执行以下命令应用到数据库：
-- mysql -u root -p ruoyi-ai < knowledge_graph_schema.sql
-- ----------------------------
