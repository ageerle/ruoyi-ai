-- ----------------------------
-- Add MiniMax provider
-- ----------------------------
INSERT INTO `chat_provider` (`id`, `provider_name`, `provider_code`, `provider_icon`, `provider_desc`, `api_host`, `status`, `sort_order`, `create_dept`, `create_time`, `create_by`, `update_by`, `update_time`, `remark`, `version`, `del_flag`, `update_ip`, `tenant_id`)
VALUES (2010000000000000001, 'MiniMax', 'minimax', NULL, 'MiniMax大模型服务，支持M2.7、M2.5等模型', 'https://api.minimax.io/v1', '0', 6, NULL, NOW(), '1', '1', NOW(), 'MiniMax厂商', NULL, '0', NULL, 0);

-- ----------------------------
-- Add MiniMax chat models
-- ----------------------------
INSERT INTO `chat_model` (`id`, `category`, `model_name`, `provider_code`, `model_describe`, `model_dimension`, `model_show`, `api_host`, `api_key`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `tenant_id`)
VALUES (2010000000000000002, 'chat', 'MiniMax-M2.7', 'minimax', 'MiniMax-M2.7', NULL, 'Y', 'https://api.minimax.io/v1', '', NULL, 1, NOW(), 1, NOW(), 'MiniMax最新旗舰模型M2.7，支持1M上下文窗口', 0);

INSERT INTO `chat_model` (`id`, `category`, `model_name`, `provider_code`, `model_describe`, `model_dimension`, `model_show`, `api_host`, `api_key`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `tenant_id`)
VALUES (2010000000000000003, 'chat', 'MiniMax-M2.5', 'minimax', 'MiniMax-M2.5', NULL, 'Y', 'https://api.minimax.io/v1', '', NULL, 1, NOW(), 1, NOW(), 'MiniMax M2.5模型，204K上下文窗口', 0);

INSERT INTO `chat_model` (`id`, `category`, `model_name`, `provider_code`, `model_describe`, `model_dimension`, `model_show`, `api_host`, `api_key`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `tenant_id`)
VALUES (2010000000000000004, 'chat', 'MiniMax-M2.5-highspeed', 'minimax', 'MiniMax-M2.5-highspeed', NULL, 'Y', 'https://api.minimax.io/v1', '', NULL, 1, NOW(), 1, NOW(), 'MiniMax M2.5高速版，204K上下文窗口，更低延迟', 0);

-- ----------------------------
-- Add MiniMax embedding model
-- ----------------------------
INSERT INTO `chat_model` (`id`, `category`, `model_name`, `provider_code`, `model_describe`, `model_dimension`, `model_show`, `api_host`, `api_key`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `tenant_id`)
VALUES (2010000000000000005, 'vector', 'embo-01', 'minimax', 'embo-01', 1536, 'N', 'https://api.minimax.io/v1', '', NULL, 1, NOW(), 1, NOW(), 'MiniMax embo-01嵌入模型，1536维度', 0);
