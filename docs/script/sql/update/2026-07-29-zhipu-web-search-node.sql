-- 智谱 Web Search 工作流扩展节点
-- 内部组件名继续使用 Google，以兼容现有前端组件和已保存流程。

UPDATE `t_workflow_component`
SET `title` = '网络搜索',
    `remark` = '调用智谱 Web Search 检索互联网信息',
    `display_order` = 40,
    `is_enable` = 1,
    `is_deleted` = 0,
    `update_time` = NOW()
WHERE `name` = 'Google'
  AND `tenant_id` = '000000';

INSERT INTO `t_workflow_component`
    (`uuid`, `name`, `title`, `remark`, `display_order`, `is_enable`,
     `create_time`, `update_time`, `is_deleted`, `tenant_id`)
SELECT
    'a7f8c2d44e5b4c83a9d6f103c2b47e18',
    'Google',
    '网络搜索',
    '调用智谱 Web Search 检索互联网信息',
    40,
    1,
    NOW(),
    NOW(),
    0,
    '000000'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM `t_workflow_component`
    WHERE `name` = 'Google'
      AND `tenant_id` = '000000'
);

UPDATE `sys_config`
SET `config_name` = '网络搜索节点响应模板',
    `config_value` = '🔍 网络搜索节点处理完成：',
    `update_time` = NOW()
WHERE `config_key` = 'node.googleSearch.template'
  AND `tenant_id` = '000000';

INSERT INTO `sys_config`
    (`config_id`, `tenant_id`, `config_name`, `config_key`, `config_value`,
     `config_type`, `create_dept`, `create_by`, `create_time`, `update_by`,
     `update_time`, `remark`)
SELECT
    2084157200000000003,
    '000000',
    '网络搜索节点响应模板',
    'node.googleSearch.template',
    '🔍 网络搜索节点处理完成：',
    'Y',
    103,
    1,
    NOW(),
    1,
    NOW(),
    '智谱 Web Search 工作流扩展节点'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM `sys_config`
    WHERE `config_key` = 'node.googleSearch.template'
      AND `tenant_id` = '000000'
);
