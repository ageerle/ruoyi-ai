-- P2-5.1 Gate 提交与逐项判定附件快照（激活 gate_element_results 死表，QA-04-D1）
-- apply 前：p1-ddl-apply-check.py 复核；仅在隔离开发环境执行
-- 1) gates.element_snapshot 提交时冻结的要素定义快照（longtext 与审计载荷同口径，DEF-6 备注沿用）
alter table gates
    add column element_snapshot longtext null comment 'P2-5.1 提交时冻结的要素定义快照 JSON（后续编辑/停用不影响在途评审）' after gate_coefficient;

-- 2) gate_element_results.evidence_ref 判定证据附件引用（FAIL/否决必填，AC-GATE-02）
alter table gate_element_results
    add column evidence_ref varchar(500) null comment '判定证据附件引用（FAIL 必填 AC-GATE-02）' after condition_note;
