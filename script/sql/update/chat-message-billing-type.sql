-- 为 chat_message 表添加 billing_type 字段
ALTER TABLE chat_message
    ADD COLUMN billing_type char NULL COMMENT '计费类型（1-token计费，2-次数计费，null-普通消息）';

