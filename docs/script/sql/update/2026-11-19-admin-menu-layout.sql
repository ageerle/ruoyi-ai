-- 后台菜单调整。按顺序先应用 2026-07-24 的权限与缺失菜单更新。
START TRANSACTION;

SET @knowledge_menu_id = 2099010100000000040;
INSERT IGNORE INTO `sys_menu`
    (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `menu_type`, `icon`, `create_dept`, `create_by`, `create_time`, `remark`)
VALUES
    (@knowledge_menu_id, '知识管理', 0, 1, 'knowledge', 'M', 'solar:book-line-duotone', 103, 1, NOW(), '知识管理目录');

-- 只有已经拥有知识页面及其原目录的角色、套餐继承新目录。
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT leaf_grant.`role_id`, @knowledge_menu_id
FROM `sys_role_menu` AS leaf_grant
JOIN `sys_menu` AS leaf ON leaf.`menu_id` = leaf_grant.`menu_id`
JOIN `sys_role_menu` AS folder_grant
    ON folder_grant.`role_id` = leaf_grant.`role_id` AND folder_grant.`menu_id` = leaf.`parent_id`
WHERE leaf.`menu_id` IN (2099010100000000010, 2099010100000000020, 2006681261898813441);

UPDATE `sys_tenant_package` AS pkg
SET pkg.`menu_ids` = CONCAT(pkg.`menu_ids`, ',', @knowledge_menu_id)
WHERE FIND_IN_SET(@knowledge_menu_id, pkg.`menu_ids`) = 0
  AND EXISTS (
      SELECT 1 FROM `sys_menu` AS leaf
      WHERE leaf.`menu_id` IN (2099010100000000010, 2099010100000000020, 2006681261898813441)
        AND FIND_IN_SET(leaf.`menu_id`, pkg.`menu_ids`) > 0
        AND FIND_IN_SET(leaf.`parent_id`, pkg.`menu_ids`) > 0
  );

UPDATE `sys_menu` SET `order_num` = 2
WHERE `menu_id` = 2000210913846157314;
UPDATE `sys_menu` SET `order_num` = 3
WHERE `menu_id` = 2000210914680823809;
UPDATE `sys_menu` SET `visible` = '1'
WHERE `menu_id` = 2099010100000000001;

UPDATE `sys_menu` SET `parent_id` = @knowledge_menu_id, `order_num` = 1, `visible` = '0'
WHERE `menu_id` = 2099010100000000010;
UPDATE `sys_menu` SET `parent_id` = @knowledge_menu_id, `order_num` = 2, `visible` = '0'
WHERE `menu_id` = 2099010100000000020;
UPDATE `sys_menu`
SET `parent_id` = @knowledge_menu_id, `order_num` = 3, `menu_name` = '知识库', `perms` = 'system:info:list', `visible` = '0'
WHERE `menu_id` = 2006681261898813441;

-- 一级页面沿用原访问路径和菜单 ID，按钮授权继续关联原菜单。
UPDATE `sys_menu`
SET `parent_id` = 0, `order_num` = 2, `menu_name` = '工具管理', `path` = 'mcp/tool', `remark` = '工具管理菜单'
WHERE `menu_id` = 2001 AND `component` = 'mcp/tool/index';

UPDATE `sys_menu`
SET `parent_id` = 0, `order_num` = 3, `menu_name` = '流程编排', `path` = 'chat/aiflow'
WHERE `menu_id` = 2031361596464902145 AND `component` = 'aiflow/index';

UPDATE `sys_menu`
SET `parent_id` = 0, `order_num` = 4, `menu_name` = '智能体管理', `path` = 'agent/agent', `remark` = '智能体管理菜单'
WHERE `menu_id` = 3001 AND `component` = 'agent/agent/index';

-- 为五个 AI 菜单腾出顺序，只移动使用原顺序的系统目录。
UPDATE `sys_menu` SET `order_num` = 5
WHERE `parent_id` = 0 AND `menu_id` IN (1, 2) AND `order_num` = 3;
UPDATE `sys_menu` SET `order_num` = 6
WHERE `parent_id` = 0 AND `menu_id` = 3 AND `order_num` = 4;

CREATE TEMPORARY TABLE `retired_ai_admin_menus` (
    `menu_id` bigint PRIMARY KEY
) ENGINE = InnoDB;

INSERT INTO `retired_ai_admin_menus`
SELECT menu.`menu_id`
FROM `sys_menu` AS menu
LEFT JOIN `sys_menu` AS parent ON parent.`menu_id` = menu.`parent_id`
WHERE menu.`component` = 'mcp/market/index'
   OR menu.`perms` LIKE 'mcp:market:%'
   OR (menu.`menu_type` = 'F' AND parent.`component` = 'mcp/market/index');

INSERT INTO `retired_ai_admin_menus`
SELECT folder.`menu_id`
FROM `sys_menu` AS folder
WHERE folder.`menu_type` = 'M'
  AND ((folder.`menu_id` = 2000 AND folder.`path` = 'mcp')
       OR (folder.`menu_id` = 3000 AND folder.`path` = 'agent'))
  AND NOT EXISTS (
      SELECT 1 FROM `sys_menu` AS child
      WHERE child.`parent_id` = folder.`menu_id`
        AND COALESCE(child.`component`, '') <> 'mcp/market/index'
        AND COALESCE(child.`perms`, '') NOT LIKE 'mcp:market:%'
  );

DELETE role_menu FROM `sys_role_menu` AS role_menu
JOIN `retired_ai_admin_menus` AS retired ON retired.`menu_id` = role_menu.`menu_id`;

SET @retired_menu_pattern = (
    SELECT CONCAT('(^|,)(', GROUP_CONCAT(`menu_id` ORDER BY `menu_id` SEPARATOR '|'), ')(?=,|$)')
    FROM `retired_ai_admin_menus`
);
UPDATE `sys_tenant_package`
SET `menu_ids` = TRIM(BOTH ',' FROM REGEXP_REPLACE(`menu_ids`, @retired_menu_pattern, ''))
WHERE `menu_ids` REGEXP @retired_menu_pattern;

DELETE menu FROM `sys_menu` AS menu
JOIN `retired_ai_admin_menus` AS retired ON retired.`menu_id` = menu.`menu_id`;

DROP TEMPORARY TABLE `retired_ai_admin_menus`;
COMMIT;
