/*
 Navicat Premium Dump SQL

 Source Server         : ruoyi-ai
 Source Server Type    : MySQL
 Source Server Version : 50740 (5.7.40-log)
 Source Host           : 120.0.0.1:3306
 Source Schema         : ruoyi-ai

 Target Server Type    : MySQL
 Target Server Version : 50740 (5.7.40-log)
 File Encoding         : 65001

 Date: 11/02/2025 16:06:27
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for chat_app_store
-- ----------------------------
DROP TABLE IF EXISTS `chat_app_store`;
CREATE TABLE `chat_app_store`  (
  `id` bigint(20) NOT NULL COMMENT 'id',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '描述',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'logo',
  `app_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '地址',
  `create_dept` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '部门',
  `create_by` bigint(20) NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '应用商店' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of chat_app_store
-- ----------------------------
INSERT INTO `chat_app_store` VALUES (1, '知识库', '创建属于自己的本地知识库', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2025/02/11/9178bd7126b0478b9713e18844de58d4.png', '/knowledge', '', NULL, NULL, NULL, NULL, NULL);
INSERT INTO `chat_app_store` VALUES (2, '绘画', '开启创意绘画之旅', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2025/02/11/e8b4ff15af6945d09accb59f5dd6279b.png', '/draw', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `chat_app_store` VALUES (3, '翻译', '提供精准高效的语言翻译服务', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2025/02/11/3b5e87263c004ba389d6af8d43552770.png', '/fanyi', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `chat_app_store` VALUES (4, '音乐创作', '激发音乐创作潜能', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2025/02/11/a761c32e823945d29daeaeaf45a6dfe9.png', '/music', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `chat_app_store` VALUES (5, '智能PPT', '一键生成专业 PPT', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2025/02/11/8de63c7a2d5e4c22bc8121a3c9e0fec1.png', '/ppt', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `chat_app_store` VALUES (6, '文生视频', '将文字内容转化为生动视频', 'http://panda-1253683406.cos.ap-guangzhou.myqcloud.com/panda/2025/02/11/15d878c58db248afa886032efb292467.png', '/video', NULL, NULL, NULL, NULL, NULL, NULL);

SET FOREIGN_KEY_CHECKS = 1;
