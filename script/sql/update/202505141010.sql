ALTER TABLE `knowledge_attach`
ADD COLUMN `pic_status` tinyint(1) NOT NULL DEFAULT 10 COMMENT '拆解图片状态10未开始，20进行中，30已完成' AFTER `oss_id`,
ADD COLUMN `pic_anys_status` tinyint(1) NOT NULL DEFAULT 10 COMMENT '分析图片状态10未开始，20进行中，30已完成' AFTER `pic_status`,
ADD COLUMN `vector_status` tinyint(1) NOT NULL DEFAULT 10 COMMENT '写入向量数据库状态10未开始，20进行中，30已完成' AFTER `pic_anys_status`,
DROP PRIMARY KEY,
ADD PRIMARY KEY (`id`) USING BTREE;
