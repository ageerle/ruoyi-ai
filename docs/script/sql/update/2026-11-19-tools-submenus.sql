-- 在 2026-11-19-agent-skill.sql 之后执行。
-- 调整工具目录与菜单授权，撤下 URL 管理和目前的 RAG 链路追踪入口。
START TRANSACTION;

-- 复用原 MCP 目录的 ID 和路径，现有 /mcp/tool 访问路径不变。
INSERT IGNORE INTO `sys_menu`
    (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `menu_type`,
     `visible`, `status`, `icon`, `create_dept`, `create_by`, `create_time`, `remark`)
VALUES
    (2000, '工具管理', 0, 2, 'mcp', 'M', '0', '0', 'octicon:tools-24', 103, 1, NOW(), '工具管理目录');

UPDATE `sys_menu`
SET `menu_name` = '工具管理', `parent_id` = 0, `order_num` = 2,
    `visible` = '0', `status` = '0', `remark` = '工具管理目录'
WHERE `menu_id` = 2000 AND `menu_type` = 'M' AND `path` = 'mcp';

UPDATE `sys_menu` AS page
JOIN `sys_menu` AS folder
  ON folder.`menu_id` = 2000 AND folder.`menu_type` = 'M' AND folder.`path` = 'mcp'
SET page.`parent_id` = folder.`menu_id`, page.`order_num` = 1,
    page.`path` = 'tool', page.`menu_name` = 'MCP管理', page.`remark` = 'MCP 工具管理菜单'
WHERE page.`menu_id` = 2001 AND page.`component` = 'mcp/tool/index';

UPDATE `sys_menu` AS page
JOIN `sys_menu` AS folder
  ON folder.`menu_id` = 2000 AND folder.`menu_type` = 'M' AND folder.`path` = 'mcp'
SET page.`parent_id` = folder.`menu_id`, page.`order_num` = 2, page.`path` = 'skill'
WHERE page.`menu_id` = 2099111900010000100 AND page.`component` = 'agent/skill/index';

-- 继承父目录的可见性。子菜单和按钮权限继续由原授权控制。
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT grant_row.`role_id`, page.`parent_id`
FROM `sys_role_menu` AS grant_row
JOIN `sys_menu` AS page ON page.`menu_id` = grant_row.`menu_id`
WHERE page.`menu_id` IN (2001, 2099111900010000100) AND page.`parent_id` = 2000;

UPDATE `sys_tenant_package` AS pkg
SET pkg.`menu_ids` = CONCAT_WS(',', NULLIF(pkg.`menu_ids`, ''), '2000')
WHERE FIND_IN_SET('2000', COALESCE(pkg.`menu_ids`, '')) = 0
  AND EXISTS (
      SELECT 1 FROM `sys_menu` AS page
      WHERE page.`menu_id` IN (2001, 2099111900010000100)
        AND page.`parent_id` = 2000 AND FIND_IN_SET(page.`menu_id`, pkg.`menu_ids`) > 0
  );

DROP TEMPORARY TABLE IF EXISTS `retired_tools_adjustment_menus`;
CREATE TEMPORARY TABLE `retired_tools_adjustment_menus` (`menu_id` bigint PRIMARY KEY) ENGINE=InnoDB;

INSERT INTO `retired_tools_adjustment_menus` (`menu_id`)
SELECT `menu_id` FROM `sys_menu`
WHERE `component` IN ('system/url/index', 'monitor/trace/index')
   OR `perms` LIKE 'system:url:%'
   OR `perms` IN ('monitor:trace:list', 'monitor:trace:query');

DELETE grant_row FROM `sys_role_menu` AS grant_row
JOIN `retired_tools_adjustment_menus` AS retired ON retired.`menu_id` = grant_row.`menu_id`;

SET @retired_tools_menu_pattern = (
    SELECT CONCAT('(^|,)(', GROUP_CONCAT(`menu_id` ORDER BY `menu_id` SEPARATOR '|'), ')(?=,|$)')
    FROM `retired_tools_adjustment_menus`
);
UPDATE `sys_tenant_package`
SET `menu_ids` = TRIM(BOTH ',' FROM REGEXP_REPLACE(`menu_ids`, @retired_tools_menu_pattern, ''))
WHERE `menu_ids` REGEXP @retired_tools_menu_pattern;

DELETE menu FROM `sys_menu` AS menu
JOIN `retired_tools_adjustment_menus` AS retired ON retired.`menu_id` = menu.`menu_id`;

DROP TEMPORARY TABLE `retired_tools_adjustment_menus`;
COMMIT;
