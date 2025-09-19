ALTER TABLE `knowledge_attach`
MODIFY COLUMN `oss_id` bigint(20) NULL DEFAULT NULL COMMENT '对象存储ID' AFTER `doc_type`;