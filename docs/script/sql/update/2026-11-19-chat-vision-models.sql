INSERT INTO `sys_config` (
    `config_id`, `tenant_id`, `config_name`, `config_key`,
    `config_value`, `config_type`, `create_dept`, `create_by`, `create_time`, `remark`
)
SELECT 2123320236999876410, '000000', '聊天图片理解模型', 'chat.image.visionModels',
       '', 'N', 103, 1, NOW(), '填写支持图片输入的对话模型名称，用英文逗号分隔。当前模型在列表中时直接使用，否则使用第一项。'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_config`
    WHERE `tenant_id` = '000000' AND `config_key` = 'chat.image.visionModels'
);
