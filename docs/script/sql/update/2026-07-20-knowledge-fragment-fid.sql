-- RAG metadata migration (MySQL 8). Safe to execute repeatedly.
ALTER TABLE `knowledge_attach`
    ADD COLUMN IF NOT EXISTS `file_hash` varchar(64) NULL DEFAULT NULL COMMENT '文件SHA-256摘要' AFTER `doc_id`,
    MODIFY COLUMN `doc_id` varchar(32) NULL DEFAULT NULL COMMENT '文档ID';

SET @add_file_hash = IF(EXISTS(
    SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE()
    AND table_name = 'knowledge_attach' AND index_name = 'uk_knowledge_file_hash'),
    'SELECT 1', 'ALTER TABLE `knowledge_attach` ADD UNIQUE INDEX `uk_knowledge_file_hash` (`knowledge_id`, `file_hash`)');
PREPARE stmt FROM @add_file_hash; EXECUTE stmt; DEALLOCATE PREPARE stmt;

ALTER TABLE `knowledge_fragment`
    ADD COLUMN IF NOT EXISTS `fid` varchar(32) NULL DEFAULT NULL COMMENT '向量库片段ID' AFTER `id`,
    MODIFY COLUMN `doc_id` varchar(32) NULL DEFAULT NULL COMMENT '文档ID';

UPDATE `knowledge_fragment`
SET `fid` = LOWER(MD5(CONCAT('knowledge_fragment:', `id`)))
WHERE `fid` IS NULL OR `fid` = '';

SET @drop_idx_fid = IF(EXISTS(
    SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE()
    AND table_name = 'knowledge_fragment' AND index_name = 'idx_fid'),
    'ALTER TABLE `knowledge_fragment` DROP INDEX `idx_fid`', 'SELECT 1');
PREPARE stmt FROM @drop_idx_fid; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @add_uk_fid = IF(EXISTS(
    SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE()
    AND table_name = 'knowledge_fragment' AND index_name = 'uk_fid'),
    'SELECT 1', 'ALTER TABLE `knowledge_fragment` ADD UNIQUE INDEX `uk_fid` (`fid`)');
PREPARE stmt FROM @add_uk_fid; EXECUTE stmt; DEALLOCATE PREPARE stmt;

ALTER TABLE `knowledge_fragment` MODIFY COLUMN `fid` varchar(32) NOT NULL COMMENT '向量库片段ID';

SET @add_tenant_user = IF(EXISTS(
    SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE()
    AND table_name = 'knowledge_info' AND index_name = 'idx_tenant_user'),
    'SELECT 1', 'ALTER TABLE `knowledge_info` ADD INDEX `idx_tenant_user` (`tenant_id`, `user_id`)');
PREPARE stmt FROM @add_tenant_user; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @add_tenant_share = IF(EXISTS(
    SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE()
    AND table_name = 'knowledge_info' AND index_name = 'idx_tenant_share'),
    'SELECT 1', 'ALTER TABLE `knowledge_info` ADD INDEX `idx_tenant_share` (`tenant_id`, `share`)');
PREPARE stmt FROM @add_tenant_share; EXECUTE stmt; DEALLOCATE PREPARE stmt;
