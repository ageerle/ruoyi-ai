-- 一次性补全工作流节点消息模板配置 (NodeMessageTemplateEnum 全部 8 个键)
-- 背景：节点执行时 WorkflowMessageUtil.getNodeMessageTemplate 从 sys_config 读取展示模板,
--       历史库中这批配置缺失, 导致运行工作流抛「请先配置该节点的响应模板」。
--       其中前 7 个见 2026-07-21-sys-config-node-template.sql,
--       Google Search 为此前从未入库的节点模板。
-- 说明：代码已增加内置默认模板兜底, 本脚本为可选, 执行后模板可在 系统管理-配置管理 中自定义。
-- 幂等：按 config_key + tenant_id 判重, 可重复执行。

INSERT INTO `sys_config` (`config_id`, `tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2027192921483309058, '000000', 'HTTP请求节点响应模板', 'node.httpRequest.template', '✅ HTTP请求节点：结束响应 - ', 'Y', 103, 1, '2026-02-27 09:23:51', 1, '2026-02-27 09:31:41', NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'node.httpRequest.template' AND `tenant_id` = '000000');

INSERT INTO `sys_config` (`config_id`, `tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2027193296990957569, '000000', '文生图节点响应模板', 'node.image.template', '🎨 文生图节点：结束响应 - 图片URL: ', 'Y', 103, 1, '2026-02-27 09:25:20', 1, '2026-02-27 09:31:52', NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'node.image.template' AND `tenant_id` = '000000');

INSERT INTO `sys_config` (`config_id`, `tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2027193820393959425, '000000', '发送邮箱节点响应模板', 'node.mailsend.template', '📧 发送邮箱节点：结束响应 - ', 'Y', 103, 1, '2026-02-27 09:27:25', 1, '2026-02-27 09:32:05', NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'node.mailsend.template' AND `tenant_id` = '000000');

INSERT INTO `sys_config` (`config_id`, `tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2027194134438277122, '000000', '结束节点响应模板', 'node.end.template', '🔚 流程已执行完毕，如果您有其他需求，请随时重新发起请求。', 'Y', 103, 1, '2026-02-27 09:28:40', 1, '2026-02-27 09:32:53', NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'node.end.template' AND `tenant_id` = '000000');

INSERT INTO `sys_config` (`config_id`, `tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2027208880369647617, '000000', '条件分支节点响应模板', 'node.switch.template', '🔀 条件分支节点：触发 -> 跳转到节点 ', 'Y', 103, 1, '2026-02-27 10:27:15', 1, '2026-02-27 10:35:54', NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'node.switch.template' AND `tenant_id` = '000000');

INSERT INTO `sys_config` (`config_id`, `tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2027213914603995137, '000000', '大模型回答节点响应模板', 'node.llmAnswer.template', '🤖 LLM 节点 生成回答：', 'Y', 103, 1, '2026-02-27 10:47:16', 1, '2026-02-27 10:52:40', NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'node.llmAnswer.template' AND `tenant_id` = '000000');

INSERT INTO `sys_config` (`config_id`, `tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2027217577397391361, '000000', '工作流异常响应模板', 'node.exception.template', '🛑 工作流发生异常：', 'N', 103, 1, '2026-02-27 11:01:49', 1, '2026-02-27 11:02:01', NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'node.exception.template' AND `tenant_id` = '000000');

INSERT INTO `sys_config` (`config_id`, `tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2084157200000000003, '000000', '网络搜索节点响应模板', 'node.googleSearch.template', '🔍 网络搜索节点处理完成：', 'Y', 103, 1, '2026-07-29 19:40:00', 1, '2026-07-29 19:40:00', NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'node.googleSearch.template' AND `tenant_id` = '000000');
