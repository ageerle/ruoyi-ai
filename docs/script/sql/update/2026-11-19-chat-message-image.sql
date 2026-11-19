CREATE TABLE IF NOT EXISTS `chat_message_image` (
    `message_id` BIGINT NOT NULL PRIMARY KEY,
    `mime_type` VARCHAR(32) NOT NULL,
    `image_data` MEDIUMBLOB NOT NULL,
    `recognized_text` MEDIUMTEXT NULL,
    CONSTRAINT `fk_chat_message_image_message`
        FOREIGN KEY (`message_id`) REFERENCES `chat_message` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息原图和识别文字';
