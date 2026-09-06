-- P2-5.3 Gate 条件遗留项：责任人与关闭证据两列（AC-GATE-16/17，BR-GATE-04）
-- 前置：2026-09-06-ipd-p251-gate-element-results.sql 已建表并预留
--   leftover_item / leftover_due_at / leftover_status 三列；本卡补责任人闭环字段。
-- 语义：
--   responsible_person_id  条件遗留责任人（CONDITIONAL 判定时必填，AC-GATE-16）
--   closed_evidence        关闭凭证（close 时必填，AC「关闭需证据」）
-- leftover_status 取值：OPEN（未关闭）/ CLOSED（已关闭）；遗留查询只依赖本表，
-- 要素后续停用/删除不消除遗留（AC-GATE-17 防线）。

alter table gate_element_results
    add column responsible_person_id bigint null comment '条件遗留责任人（AC-GATE-16 CONDITIONAL 必填）' after leftover_item,
    add column closed_evidence varchar(500) null comment '遗留关闭凭证（close 必填）' after leftover_due_at;

-- 既有 CONDITIONAL 历史行（无责任人）保持 NULL：老数据不回填，提交阻断按逾期口径只拦
-- leftover_due_at 已过且未关闭的行，与新判定必填口径不冲突。
