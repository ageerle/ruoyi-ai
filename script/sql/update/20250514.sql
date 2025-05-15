ALTER TABLE `knowledge_info`
ADD COLUMN `system_prompt` varchar(255) NULL COMMENT '系统提示词' AFTER `vector_model`;

ALTER TABLE `knowledge_info`
    CHANGE COLUMN `vector` `vector_model_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '向量库' AFTER `text_block_size`,
    CHANGE COLUMN `vector_model` `embedding_model_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '向量模型' AFTER `vector_model_name`;
