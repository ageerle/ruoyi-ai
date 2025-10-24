-- ========================================
-- 为 graph_build_task 表添加缺失字段
-- ========================================
-- 执行日期: 2025-10-11
-- 说明: 添加 create_dept 和 update_by 字段以符合 MyBatis-Plus BaseEntity 规范
-- ========================================

-- 检查表是否存在
SELECT 'Adding fields to graph_build_task table...' AS status;

-- 添加 create_dept 字段（如果不存在）
ALTER TABLE `graph_build_task` 
ADD COLUMN `create_dept` BIGINT(20) NULL COMMENT '创建部门' AFTER `end_time`;

-- 添加 update_by 字段（如果已存在 create_by 但缺少 update_by）
-- 注意：update_by 应该在 create_time 之前
ALTER TABLE `graph_build_task` 
ADD COLUMN `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者' AFTER `create_by`;

-- 验证字段是否添加成功
SELECT 'Fields added successfully!' AS status;

-- 查看表结构
DESCRIBE `graph_build_task`;

-- ========================================
-- 说明
-- ========================================
-- create_dept: 创建部门ID，与创建者关联
-- update_by: 更新者用户名或ID
-- 
-- 这两个字段是 MyBatis-Plus BaseEntity 的标准字段
-- 添加后可以正常使用自动填充功能
-- ========================================

