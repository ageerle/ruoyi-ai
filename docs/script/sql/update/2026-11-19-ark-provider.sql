START TRANSACTION;

-- 火山方舟预置走 Agent Plan 的 OpenAI 兼容接口，密钥由各模型单独配置。
INSERT INTO `chat_provider`
    (`provider_name`, `provider_code`, `provider_desc`, `api_host`, `status`, `sort_order`,
     `create_dept`, `create_by`, `create_time`, `remark`, `del_flag`, `tenant_id`)
SELECT '火山方舟', 'ark', '火山方舟 Agent Plan，兼容 OpenAI 对话接口',
       'https://ark.cn-beijing.volces.com/api/plan/v3', '0', 10, 103, '1', NOW(),
       '使用 Agent Plan 专属 API Key', '0', 0
WHERE NOT EXISTS (
    SELECT 1 FROM `chat_provider`
    WHERE `provider_code` = 'ark' AND `tenant_id` = 0 AND `del_flag` = '0'
);

-- 将原先通过通用 OpenAI 入口使用 Agent Plan 的对话模型归到新厂商。
UPDATE `chat_model` AS model
JOIN `chat_provider` AS provider
  ON provider.`provider_code` = 'ark'
 AND provider.`tenant_id` = model.`tenant_id`
 AND provider.`del_flag` = '0'
 AND provider.`status` = '0'
SET model.`provider_code` = 'ark'
WHERE model.`provider_code` = 'custom_api'
  AND model.`tenant_id` = 0
  AND model.`category` = 'chat'
  AND TRIM(TRAILING '/' FROM model.`api_host`) IN (
      'https://ark.cn-beijing.volces.com/api/plan/v3',
      'https://ark.cn-beijing.volces.com/api/plan/v3/chat/completions'
  );

COMMIT;
