-- 链路追踪运行记录表
CREATE TABLE IF NOT EXISTS `trace_run` (
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
  `metadata` text DEFAULT NULL COMMENT '元数据JSON',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_trace_run_trace_id` (`trace_id`) USING BTREE,
  KEY `idx_trace_run_business` (`business_type`, `business_id`) USING BTREE,
  KEY `idx_trace_run_status_time` (`status`, `start_time`) USING BTREE,
  KEY `idx_trace_run_tenant_time` (`tenant_id`, `start_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='链路追踪运行记录表' ROW_FORMAT=DYNAMIC;

-- 链路追踪节点记录表
CREATE TABLE IF NOT EXISTS `trace_node` (
  `id` bigint NOT NULL COMMENT '主键',
  `trace_id` varchar(64) NOT NULL COMMENT '链路ID',
  `node_id` varchar(64) NOT NULL COMMENT '节点ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `parent_node_id` varchar(64) DEFAULT NULL COMMENT '父节点ID',
  `node_name` varchar(128) NOT NULL COMMENT '节点名称',
  `node_type` varchar(64) NOT NULL COMMENT '节点类型',
  `depth` int DEFAULT 0 COMMENT '节点深度',
  `sort_order` int DEFAULT 0 COMMENT '排序',
  `class_name` varchar(255) DEFAULT NULL COMMENT '类名',
  `method_name` varchar(128) DEFAULT NULL COMMENT '方法名',
  `status` varchar(32) NOT NULL COMMENT '状态',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration_ms` bigint DEFAULT NULL COMMENT '耗时毫秒',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '错误摘要',
  `input_payload` text DEFAULT NULL COMMENT '输入JSON',
  `output_payload` text DEFAULT NULL COMMENT '输出JSON',
  `metadata` text DEFAULT NULL COMMENT '元数据JSON',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_trace_node_trace_id` (`trace_id`) USING BTREE,
  KEY `idx_trace_node_parent` (`trace_id`, `parent_node_id`) USING BTREE,
  KEY `idx_trace_node_time` (`trace_id`, `start_time`) USING BTREE,
  KEY `idx_trace_node_tenant_time` (`tenant_id`, `start_time`) USING BTREE,
  KEY `idx_trace_node_type_status` (`node_type`, `status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='链路追踪节点记录表' ROW_FORMAT=DYNAMIC;

-- 兼容已存在的旧 trace 表：CREATE TABLE IF NOT EXISTS 不会给旧表补新字段
SET @trace_run_add_tenant_sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `trace_run` ADD COLUMN `tenant_id` varchar(20) DEFAULT ''000000'' COMMENT ''租户编号'' AFTER `user_id`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'trace_run'
    AND COLUMN_NAME = 'tenant_id'
);
PREPARE trace_run_add_tenant_stmt FROM @trace_run_add_tenant_sql;
EXECUTE trace_run_add_tenant_stmt;
DEALLOCATE PREPARE trace_run_add_tenant_stmt;

SET @trace_run_add_tenant_idx_sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `trace_run` ADD INDEX `idx_trace_run_tenant_time` (`tenant_id`, `start_time`) USING BTREE',
    'SELECT 1'
  )
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'trace_run'
    AND INDEX_NAME = 'idx_trace_run_tenant_time'
);
PREPARE trace_run_add_tenant_idx_stmt FROM @trace_run_add_tenant_idx_sql;
EXECUTE trace_run_add_tenant_idx_stmt;
DEALLOCATE PREPARE trace_run_add_tenant_idx_stmt;

SET @trace_node_add_tenant_sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `trace_node` ADD COLUMN `tenant_id` varchar(20) DEFAULT ''000000'' COMMENT ''租户编号'' AFTER `node_id`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'trace_node'
    AND COLUMN_NAME = 'tenant_id'
);
PREPARE trace_node_add_tenant_stmt FROM @trace_node_add_tenant_sql;
EXECUTE trace_node_add_tenant_stmt;
DEALLOCATE PREPARE trace_node_add_tenant_stmt;

SET @trace_node_add_tenant_idx_sql = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `trace_node` ADD INDEX `idx_trace_node_tenant_time` (`tenant_id`, `start_time`) USING BTREE',
    'SELECT 1'
  )
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'trace_node'
    AND INDEX_NAME = 'idx_trace_node_tenant_time'
);
PREPARE trace_node_add_tenant_idx_stmt FROM @trace_node_add_tenant_idx_sql;
EXECUTE trace_node_add_tenant_idx_stmt;
DEALLOCATE PREPARE trace_node_add_tenant_idx_stmt;

-- 链路追踪监控菜单 & 按钮权限
INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `remark`)
SELECT (SELECT COALESCE(MAX(`menu_id`), 0) + 1 FROM (SELECT `menu_id` FROM `sys_menu`) t), '链路追踪', 2, 7, 'trace', 'monitor/trace/index', '', 1, 0, 'C', '0', '0', 'monitor:trace:list', 'tabler:route', 103, 1, NOW(), '链路追踪监控菜单';

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `remark`)
SELECT (SELECT COALESCE(MAX(`menu_id`), 0) + 1 FROM (SELECT `menu_id` FROM `sys_menu`) t), '链路追踪查询', (SELECT `menu_id` FROM `sys_menu` WHERE `perms` = 'monitor:trace:list' AND `menu_type` = 'C' LIMIT 1), 1, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:trace:query', '#', 103, 1, NOW(), '';
