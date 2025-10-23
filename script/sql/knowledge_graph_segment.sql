-- 知识图谱片段表
-- 用于记录从文档中抽取图谱时的文本片段信息
CREATE TABLE IF NOT EXISTS `knowledge_base_graph_segment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `uuid` VARCHAR(64) NOT NULL COMMENT '片段UUID',
    `kb_uuid` VARCHAR(64) NOT NULL COMMENT '知识库UUID',
    `kb_item_uuid` VARCHAR(64) COMMENT '知识库条目UUID',
    `doc_uuid` VARCHAR(64) COMMENT '文档UUID',
    `segment_text` TEXT COMMENT '片段文本内容',
    `chunk_index` INT DEFAULT 0 COMMENT '片段索引（第几个片段）',
    `total_chunks` INT DEFAULT 1 COMMENT '总片段数',
    `extraction_status` TINYINT DEFAULT 0 COMMENT '抽取状态：0-待处理 1-处理中 2-已完成 3-失败',
    `entity_count` INT DEFAULT 0 COMMENT '抽取的实体数量',
    `relation_count` INT DEFAULT 0 COMMENT '抽取的关系数量',
    `token_used` INT DEFAULT 0 COMMENT '消耗的token数',
    `error_message` VARCHAR(500) COMMENT '错误信息',
    `user_id` BIGINT COMMENT '用户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark` VARCHAR(500) COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uuid` (`uuid`),
    KEY `idx_kb_uuid` (`kb_uuid`),
    KEY `idx_kb_item_uuid` (`kb_item_uuid`),
    KEY `idx_doc_uuid` (`doc_uuid`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识图谱片段表';
