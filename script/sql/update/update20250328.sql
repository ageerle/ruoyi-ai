ALTER TABLE `chat_message`
    ADD COLUMN `role` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '对话角色' AFTER `content`;
