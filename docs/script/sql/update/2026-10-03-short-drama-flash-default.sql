-- Existing Atlas keys/hosts remain intact. Default priority is maintained through model management.
-- Apply after 2026-10-02-chat-model-sort-order.sql.
UPDATE chat_model SET sort_order = 10
WHERE provider_code = 'atlas' AND category = 'chat' AND model_name = 'bytedance/doubao-seed-2.1-pro-260628' AND sort_order = 0;
UPDATE chat_model SET sort_order = 0
WHERE provider_code = 'atlas' AND category = 'chat' AND model_name = 'deepseek-ai/deepseek-v4.1-flash';
