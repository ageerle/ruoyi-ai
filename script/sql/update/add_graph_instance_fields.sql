-- 为 knowledge_graph_instance 表添加新字段
-- 用于支持图谱实例管理的扩展功能
-- 执行日期: 2025-01-11

-- 添加 LLM 模型名称字段
ALTER TABLE knowledge_graph_instance 
ADD COLUMN model_name VARCHAR(100) DEFAULT NULL COMMENT 'LLM模型名称' AFTER config;

-- 添加实体类型字段
ALTER TABLE knowledge_graph_instance 
ADD COLUMN entity_types VARCHAR(500) DEFAULT NULL COMMENT '实体类型（逗号分隔）' AFTER model_name;

-- 添加关系类型字段
ALTER TABLE knowledge_graph_instance 
ADD COLUMN relation_types VARCHAR(500) DEFAULT NULL COMMENT '关系类型（逗号分隔）' AFTER entity_types;

