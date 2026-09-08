-- ----------------------------------------------------------------------------
-- URL 管理模块增量脚本（可重复执行）
-- 1) 创建 sys_url 表
-- 2) 新增「系统管理 -> URL管理」菜单及 4 个按钮权限（按 perms 去重，幂等）
-- 3) 插入两条初始公开网站链接（按 name + url 去重，幂等）
-- 适用于已存在数据库的升级；全新安装请直接使用 ruoyi-ai.sql
-- ----------------------------------------------------------------------------

-- ----------------------------
-- 1. URL 管理表 sys_url
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_url`  (
    `url_id`      bigint       NOT NULL AUTO_INCREMENT COMMENT '链接ID',
    `tenant_id`   varchar(20)  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户编号',
    `name`        varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '链接名称',
    `url`         varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'HTTP(S) 地址',
    `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '链接说明',
    `sort_order`  int          NULL DEFAULT 0 COMMENT '显示顺序',
    `status`      char(1)      CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
    `create_dept` bigint       NULL DEFAULT NULL COMMENT '创建部门',
    `create_by`   bigint       NULL DEFAULT NULL COMMENT '创建者',
    `create_time` datetime     NULL DEFAULT NULL COMMENT '创建时间',
    `update_by`   bigint       NULL DEFAULT NULL COMMENT '更新者',
    `update_time` datetime     NULL DEFAULT NULL COMMENT '更新时间',
    `remark`      varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`url_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'URL 管理表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 2. 菜单 & 按钮权限（幂等：perms 不存在时才插入）
--    父菜单 1 = 系统管理目录
-- ----------------------------
INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `remark`)
SELECT (SELECT COALESCE(MAX(`menu_id`), 0) + 1 FROM (SELECT `menu_id` FROM `sys_menu`) t), 'URL管理', 1, 12, 'url', 'system/url/index', '', 1, 0, 'C', '0', '0', 'system:url:list', 'mdi:link-variant', 103, 1, NOW(), 'URL管理菜单'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:url:list' AND `menu_type` = 'C');

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `remark`)
SELECT (SELECT COALESCE(MAX(`menu_id`), 0) + 1 FROM (SELECT `menu_id` FROM `sys_menu`) t), 'URL查询', (SELECT `menu_id` FROM `sys_menu` WHERE `perms` = 'system:url:list' AND `menu_type` = 'C' LIMIT 1), 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:url:query', '#', 103, 1, NOW(), ''
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:url:query' AND `menu_type` = 'F');

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `remark`)
SELECT (SELECT COALESCE(MAX(`menu_id`), 0) + 1 FROM (SELECT `menu_id` FROM `sys_menu`) t), 'URL新增', (SELECT `menu_id` FROM `sys_menu` WHERE `perms` = 'system:url:list' AND `menu_type` = 'C' LIMIT 1), 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:url:add', '#', 103, 1, NOW(), ''
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:url:add' AND `menu_type` = 'F');

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `remark`)
SELECT (SELECT COALESCE(MAX(`menu_id`), 0) + 1 FROM (SELECT `menu_id` FROM `sys_menu`) t), 'URL修改', (SELECT `menu_id` FROM `sys_menu` WHERE `perms` = 'system:url:list' AND `menu_type` = 'C' LIMIT 1), 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:url:edit', '#', 103, 1, NOW(), ''
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:url:edit' AND `menu_type` = 'F');

INSERT INTO `sys_menu`
(`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `remark`)
SELECT (SELECT COALESCE(MAX(`menu_id`), 0) + 1 FROM (SELECT `menu_id` FROM `sys_menu`) t), 'URL删除', (SELECT `menu_id` FROM `sys_menu` WHERE `perms` = 'system:url:list' AND `menu_type` = 'C' LIMIT 1), 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:url:remove', '#', 103, 1, NOW(), ''
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'system:url:remove' AND `menu_type` = 'F');

-- ----------------------------
-- 3. 初始公开网站链接（幂等：name + url 均不存在时才插入）
-- ----------------------------
INSERT INTO `sys_url`
(`tenant_id`, `name`, `url`, `description`, `sort_order`, `status`, `create_dept`, `create_by`, `create_time`, `remark`)
SELECT '000000', '若依官网', 'https://ruoyi.vip', 'RuoYi 官方网站，提供文档、源码与社区支持', 1, '0', 103, 1, NOW(), '初始公开链接'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_url` WHERE `name` = '若依官网' AND `url` = 'https://ruoyi.vip');

INSERT INTO `sys_url`
(`tenant_id`, `name`, `url`, `description`, `sort_order`, `status`, `create_dept`, `create_by`, `create_time`, `remark`)
SELECT '000000', 'RuoYi-AI 开源仓库', 'https://github.com/ageerle/ruoyi-ai', 'RuoYi-AI 项目 GitHub 仓库', 2, '0', 103, 1, NOW(), '初始公开链接'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_url` WHERE `name` = 'RuoYi-AI 开源仓库' AND `url` = 'https://github.com/ageerle/ruoyi-ai');
