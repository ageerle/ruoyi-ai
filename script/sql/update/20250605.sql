
/*
 上传图片后会自动查找分类为image的模型,要使用图片识别功能,需要执行这条sql并配置key信息
*/


INSERT INTO `chat_model` (`id`, `tenant_id`, `category`, `model_name`, `model_describe`, `model_price`, `model_type`, `model_show`, `system_prompt`, `api_host`, `api_key`, `api_url`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1930184891812147202, '000000', 'image', 'qwen/qwen2.5-vl-72b-instruct', 'qwen/qwen2.5-vl-72b-instruct', 0.003, '2', '0', NULL, 'https://api.ppinfra.com/v3/openai/chat/completions', 'xx', NULL, 103, 1, '2025-06-04 16:48:34', 1, '2025-06-04 16:48:34', '视觉模型');
