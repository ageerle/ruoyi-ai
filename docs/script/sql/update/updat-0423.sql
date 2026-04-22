-- 为知识库信息表新增检索配置字段 (剔除了已存在的重排字段)
ALTER TABLE knowledge_info
    ADD COLUMN similarity_threshold DOUBLE DEFAULT 0.5 COMMENT '相似度阈值'
AFTER retrieve_limit;

ALTER TABLE knowledge_info ADD COLUMN enable_hybrid tinyint(1) DEFAULT 0 COMMENT '是否启用混合检索';
ALTER TABLE knowledge_info ADD COLUMN hybrid_alpha double DEFAULT 0.5 COMMENT '混合检索权重比例 (0.0=纯向量, 1.0=纯关键词)';

-- 为知识片段表增加全文索引及关联ID
ALTER TABLE knowledge_fragment ADD COLUMN knowledge_id bigint COMMENT '知识库ID';
ALTER TABLE knowledge_fragment ADD FULLTEXT INDEX ft_content (content) WITH PARSER ngram;

-- 为知识库附件表增加解析状态字段
ALTER TABLE `knowledge_attach` ADD COLUMN `status` TINYINT DEFAULT 0 COMMENT '解析状态: 0待解析, 1解析中, 2已解析, 3解析失败';
