ALTER TABLE `knowledge_attach`
ADD COLUMN `oss_id` bigint(20) NOT NULL COMMENT '对象存储主键' AFTER `remark`,
ADD COLUMN `pic_status` tinyint(1) NOT NULL DEFAULT 10 COMMENT '拆解图片状态10未开始，20进行中，30已完成' AFTER `oss_id`,
ADD COLUMN `pic_anys_status` tinyint(1) NOT NULL DEFAULT 10 COMMENT '分析图片状态10未开始，20进行中，30已完成' AFTER `pic_status`,
ADD COLUMN `vector_status` tinyint(1) NOT NULL DEFAULT 10 COMMENT '写入向量数据库状态10未开始，20进行中，30已完成' AFTER `pic_anys_status`,
DROP PRIMARY KEY,
ADD PRIMARY KEY (`id`) USING BTREE;

ALTER TABLE `knowledge_attach`
MODIFY COLUMN `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '备注' AFTER `update_time`;

/*
 Navicat Premium Data Transfer

 Source Server         : localhost-57
 Source Server Type    : MySQL
 Source Server Version : 50731 (5.7.31)
 Source Host           : localhost:3306
 Source Schema         : ruoyi-ai

 Target Server Type    : MySQL
 Target Server Version : 50731 (5.7.31)
 File Encoding         : 65001

 Date: 19/05/2025 15:22:09
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for knowledge_attach_pic
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_attach_pic`;
CREATE TABLE `knowledge_attach_pic` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `kid` varchar(50) NOT NULL COMMENT '知识库id',
  `aid` varchar(50) NOT NULL COMMENT '附件id',
  `doc_name` varchar(500) DEFAULT NULL COMMENT '文档名称',
  `doc_type` varchar(50) NOT NULL COMMENT '文档类型',
  `content` longtext COMMENT '文档内容',
  `page_num` int(5) DEFAULT '0' COMMENT '所在页数',
  `index_num` int(5) DEFAULT '0' COMMENT '所在页index',
  `pic_anys_status` int(5) NOT NULL DEFAULT '10' COMMENT '分析图片状态10未开始，20进行中，30已完成',
  `oss_id` bigint(20) NOT NULL COMMENT '对象存储主键',
  `create_dept` varchar(255) DEFAULT NULL COMMENT '部门',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` text COMMENT '备注',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1922929659800637443 DEFAULT CHARSET=utf8mb4 COMMENT='知识库附件图片列表';

SET FOREIGN_KEY_CHECKS = 1;
