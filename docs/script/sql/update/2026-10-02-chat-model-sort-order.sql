-- Idempotent schema upgrade; preserve credentials and explicitly customized priorities.
SET @sort_order_sql = (SELECT IF(COUNT(*) = 0,
 'ALTER TABLE chat_model ADD COLUMN sort_order INT NOT NULL DEFAULT 100 COMMENT ''默认模型优先级，升序'' AFTER category',
 'SELECT 1') FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'chat_model' AND column_name = 'sort_order');
PREPARE sort_order_stmt FROM @sort_order_sql;
EXECUTE sort_order_stmt;
DEALLOCATE PREPARE sort_order_stmt;
UPDATE chat_model SET sort_order = 0 WHERE provider_code = 'atlas' AND sort_order = 100 AND model_name IN ('bytedance/doubao-seed-2.1-pro-260628', 'openai/gpt-image-2.5-flare/text-to-image', 'bytedance/seedance-2.0-mini/reference-to-video');
