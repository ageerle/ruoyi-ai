-- =============================================
-- 流程编排搜索节点配置脚本
-- =============================================
-- 说明：本脚本用于添加搜索节点的系统配置
-- 执行前请确保 sys_config 表存在
-- =============================================

-- 搜索节点模板配置
INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_by, create_time, update_by, update_time)
VALUES ('搜索节点模板', 'node.googleSearch.template', '正在搜索相关内容...', 'Y', '搜索节点的响应模板，用于网络搜索功能', 'admin', NOW(), 'admin', NOW())
ON DUPLICATE KEY UPDATE config_value = '正在搜索相关内容...', update_time = NOW();

-- =============================================
-- 验证配置是否添加成功
-- =============================================
SELECT config_id, config_name, config_key, config_value, config_type, remark
FROM sys_config
WHERE config_key IN (
    'node.googleSearch.template'
)
ORDER BY config_id;
