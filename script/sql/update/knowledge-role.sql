/*
 Navicat Premium Dump SQL

 Source Server         : mysql-local-study
 Source Server Type    : MySQL
 Source Server Version : 80405 (8.4.5)
 Source Host           : 100.168.0.1:3500
 Source Schema         : ruoyi-ai

 Target Server Type    : MySQL
 Target Server Version : 80405 (8.4.5)
 File Encoding         : 65001

 Date: 20/07/2025 10:01:42
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for knowledge_role
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_role`;
CREATE TABLE `knowledge_role`  (
                                   `id` bigint NOT NULL COMMENT '知识库角色id',
                                   `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '知识库角色name',
                                   `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                                   `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                                   `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                                   `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                   `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                                   `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                   `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
                                   `group_id` bigint NULL DEFAULT NULL COMMENT '知识库角色组id',
                                   PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识库角色表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for knowledge_role_group
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_role_group`;
CREATE TABLE `knowledge_role_group`  (
                                         `id` bigint NOT NULL COMMENT '知识库角色组id',
                                         `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '知识库角色组name',
                                         `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                                         `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                                         `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                                         `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                         `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                                         `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                         `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
                                         PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识库角色组表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for knowledge_role_relation
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_role_relation`;
CREATE TABLE `knowledge_role_relation`  (
                                            `id` bigint NOT NULL COMMENT 'id',
                                            `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
                                            `create_dept` bigint NULL DEFAULT NULL COMMENT '创建部门',
                                            `create_by` bigint NULL DEFAULT NULL COMMENT '创建者',
                                            `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                            `update_by` bigint NULL DEFAULT NULL COMMENT '更新者',
                                            `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                            `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
                                            `knowledge_role_id` bigint NULL DEFAULT NULL COMMENT '知识库角色id',
                                            `knowledge_id` bigint NULL DEFAULT NULL COMMENT '知识库id',
                                            PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识库角色与知识库关联表' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;


-- 菜单
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1946483381643743233, '知识库角色管理', 1775500307898949634, '12', 'knowledgeRole', 'system/knowledgeRole/index', NULL, 1, 0, 'C', '0', '0', NULL, 'ri:user-3-fill', 103, 1, '2025-07-19 16:41:17', NULL, NULL, '知识库角色管理');

-- 用户表添加字段
ALTER TABLE sys_user
    ADD COLUMN `krole_group_type` VARCHAR(50) COMMENT '关联知识库角色/角色组',
ADD COLUMN `krole_group_id` TEXT COMMENT '关联知识库角色/角色组id';

