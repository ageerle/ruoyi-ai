-- 为 chat_model 表添加 provider_name 字段
-- 变更日期: 2025-10-04
-- 负责人: Robust_H
-- 说明: 嵌入模型供应商 （用于实现动态选择嵌入模型实现类）
ALTER TABLE `ruoyi-ai`.chat_model
    ADD COLUMN `provider_name` varchar(20) DEFAULT NULL COMMENT '模型供应商' AFTER `model_name`;

-- 修改 knowledge_info 中的 ‘embedding_model_name’ 为 ‘embedding_model_id’
-- 变更日期: 2025-10-04
-- 负责人: Robust_H
-- 说明: 用于区分多个供应商实现同一嵌入模型的情况
ALTER TABLE `ruoyi-ai`.knowledge_info
    ADD COLUMN `embedding_model_id` bigint DEFAULT NULL COMMENT '模型id' AFTER `embedding_model_name`;
