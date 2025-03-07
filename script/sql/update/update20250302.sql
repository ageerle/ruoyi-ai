ALTER TABLE `knowledge_info` ADD COLUMN `share` tinyint(4) NULL DEFAULT NULL COMMENT '是否公开知识库（0 否 1是）' AFTER `kname`;
