-- 补充工作流节点消息模板配置 (对应 issue IJX5VV)
-- 背景：NodeMessageTemplateEnum 依赖以下 9 个 sys_config 键，缺失时
--       WorkflowMessageUtil.getNodeMessageTemplate 会抛出「请先配置该节点的响应模板」。
-- 这批配置在历史提交 20d531c0 中存在，SQL 脚本合并重命名时遗失，此处恢复。
-- 幂等：按 config_key + tenant_id 判重，可重复执行。

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
SELECT 2027206492573335554, '000000', '人机交互节点响应模板', 'node.humanFeedback.template', '👤 人机交互节点：等待用户操作 - ', 'Y', 103, 1, '2026-02-27 10:17:46', 1, '2026-02-27 10:17:46', NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'node.humanFeedback.template' AND `tenant_id` = '000000');

INSERT INTO `sys_config` (`config_id`, `tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2027208880369647617, '000000', '条件分支节点响应模板', 'node.switch.template', '🔀 条件分支节点：触发 -> 跳转到节点 ', 'Y', 103, 1, '2026-02-27 10:27:15', 1, '2026-02-27 10:35:54', NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'node.switch.template' AND `tenant_id` = '000000');

INSERT INTO `sys_config` (`config_id`, `tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2027213914603995137, '000000', '大模型回答节点响应模板', 'node.llmAnswer.template', '🤖 LLM 节点 生成回答：', 'Y', 103, 1, '2026-02-27 10:47:16', 1, '2026-02-27 10:52:40', NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'node.llmAnswer.template' AND `tenant_id` = '000000');

INSERT INTO `sys_config` (`config_id`, `tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2027214387000066050, '000000', '关键词提取响应模板', 'node.keywordExtractor.template', '🔑 关键词提取节点 处理完成 ： ', 'Y', 103, 1, '2026-02-27 10:49:08', 1, '2026-02-27 10:52:08', NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'node.keywordExtractor.template' AND `tenant_id` = '000000');

INSERT INTO `sys_config` (`config_id`, `tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 2027217577397391361, '000000', '工作流异常响应模板', 'node.exception.template', '🛑 工作流发生异常：', 'N', 103, 1, '2026-02-27 11:01:49', 1, '2026-02-27 11:02:01', NULL
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key` = 'node.exception.template' AND `tenant_id` = '000000');
