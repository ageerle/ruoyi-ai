SET @supports_vision_sql = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE chat_model ADD COLUMN supports_vision TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''支持识图''',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'chat_model' AND column_name = 'supports_vision'
);
PREPARE supports_vision_stmt FROM @supports_vision_sql;
EXECUTE supports_vision_stmt;
DEALLOCATE PREPARE supports_vision_stmt;

START TRANSACTION;

-- Preserve the models from the legacy list before replacing it with one default.
UPDATE chat_model AS model
JOIN sys_config AS config
  ON config.tenant_id = CAST(model.tenant_id AS CHAR)
  OR (model.tenant_id = 0 AND config.tenant_id = '000000')
SET model.supports_vision = 1
WHERE config.config_key = 'chat.image.visionModels'
  AND (config.config_name <> '聊天默认视觉模型' OR INSTR(config.config_value, ',') > 0)
  AND model.category = 'chat'
  AND FIND_IN_SET(
      CAST(model.model_name AS BINARY),
      CAST(REGEXP_REPLACE(TRIM(config.config_value), '[[:space:]]*,[[:space:]]*', ',') AS BINARY)
  ) > 0;

UPDATE sys_config
SET config_name = '聊天默认视觉模型',
    config_value = SUBSTRING_INDEX(
        TRIM(LEADING ',' FROM REGEXP_REPLACE(TRIM(COALESCE(config_value, '')), '[[:space:]]*,[[:space:]]*', ',')),
        ',', 1
    ),
    remark = '填写已开启「支持识图」的对话模型名称。当前模型未开启时，带图消息由此模型回答。'
WHERE config_key = 'chat.image.visionModels'
  AND (config_name <> '聊天默认视觉模型' OR INSTR(config_value, ',') > 0);

INSERT INTO `sys_config` (
    `config_id`, `tenant_id`, `config_name`, `config_key`,
    `config_value`, `config_type`, `create_dept`, `create_by`, `create_time`, `remark`
)
SELECT 2123320236999876410, '000000', '聊天默认视觉模型', 'chat.image.visionModels',
       '', 'N', 103, 1, NOW(), '填写已开启「支持识图」的对话模型名称。当前模型未开启时，带图消息由此模型回答。'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_config`
    WHERE `tenant_id` = '000000' AND `config_key` = 'chat.image.visionModels'
);

COMMIT;
