-- 加宽 chat_provider.provider_icon 字段 (对应 issue IHPUDA)
-- 背景：文件系统使用 minio 私有桶时，厂商图标存的是带签名的临时访问 URL，
--       长度常超过 255，导致「Data too long for column 'provider_icon'」。
-- MODIFY COLUMN 可重复执行。

ALTER TABLE `chat_provider`
    MODIFY COLUMN `provider_icon` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '厂商图标';
