ALTER TABLE `chat_model`
    ADD COLUMN `api_url` varchar(50) NULL COMMENT '请求后缀' AFTER `api_key`;

INSERT INTO `chat_config` (`id`, `category`, `config_name`, `config_value`, `config_dict`, `create_dept`, `create_time`, `create_by`, `update_by`, `update_time`, `remark`, `version`, `del_flag`, `update_ip`, `tenant_id`) VALUES (1779450794872414211, 'chat', 'apiUrl', 'v1/chat/completions', 'API 请求后缀', 103, '2024-04-14 18:05:05', '1', '1', '2025-04-23 22:29:04', NULL, NULL, '0', NULL, 0);
