ALTER TABLE `knowledge_attach`
ADD COLUMN `oss_id` bigint(20) NOT NULL COMMENT '对象存储主键' AFTER `remark`,
ADD COLUMN `pic_status` tinyint(1) NOT NULL DEFAULT 10 COMMENT '拆解图片状态10未开始，20进行中，30已完成' AFTER `oss_id`,
ADD COLUMN `pic_anys_status` tinyint(1) NOT NULL DEFAULT 10 COMMENT '分析图片状态10未开始，20进行中，30已完成' AFTER `pic_status`,
ADD COLUMN `vector_status` tinyint(1) NOT NULL DEFAULT 10 COMMENT '写入向量数据库状态10未开始，20进行中，30已完成' AFTER `pic_anys_status`,
DROP PRIMARY KEY,
ADD PRIMARY KEY (`id`) USING BTREE;

ALTER TABLE `knowledge_attach`
MODIFY COLUMN `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '备注' AFTER `update_time`;
