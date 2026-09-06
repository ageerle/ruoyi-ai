-- =====================================================================
-- P1-3.3 SOP 模板管理和实例版本快照（页46；BR-IPD-07；AC-IPD-20）
-- V10（生物特征合规验证）初始 PUBLISHED v1：内容含「算法公平性」与「偏见测试」要求
-- 幂等：已存在在库 V10 模板则跳过；日期：2026-09-05；隔离库人工 apply
-- =====================================================================

INSERT INTO sop_templates (id, action_code, title, content, version, status, tenant_id, del_flag)
SELECT 9001001, 'V10', 'V10 生物特征合规验证 SOP（v1）',
  '一、目的：验证生物特征算法满足上市合规要求。\n二、必检项：\n1. 算法公平性评估：按人群分组复核 FAR/FRR 分布，差异超阈值须整改；\n2. 偏见测试：跨 demographic 分组执行偏见测试并留存报告；\n3. FAR/FRR 实测值录入 D11 动作字段。\n三、输出：合规验证报告（含算法公平性与偏见测试结论）。',
  1, 'PUBLISHED', '000000', '0'
WHERE NOT EXISTS (SELECT 1 FROM sop_templates WHERE action_code = 'V10' AND del_flag = '0');
