-- P1-6.1 Gate要素定义版本生命周期（页47 draft/publish/archive）
-- 库：ipd_dev；迁移人：reviewer-qoder @2026-09-06；owner 授权：用户拍板「清理垃圾数据、补 P1-6.1 缺口」
-- 关联卡：P1-6.1（缺口6 DB element_code 唯一索引 + 生命周期列；G2-6 语义按 DOC-05 翻正）

-- 1) 生命周期列：存量 33 条正牌种子默认 published/version=1（即当前生效定义）
ALTER TABLE gate_review_elements
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'published'
        COMMENT '生命周期: draft/published/archived（页47）；draft 与 archived 对业务不可见' AFTER enabled,
    ADD COLUMN version INT NOT NULL DEFAULT 1
        COMMENT '发布版本号：新建草稿=0，每次 publish 递增' AFTER status,
    ADD COLUMN veto_dual_required CHAR(1) NOT NULL DEFAULT '0'
        COMMENT '双否决位：1=该否决项命中需双签确认（定义层标记，评审侧 P2-5.2 消费）' AFTER is_veto,
    ADD COLUMN threshold_json VARCHAR(512) NULL
        COMMENT '阈值配置 JSON 对象（键非空、值均为整数），如 {"minCustomerVerifications":3}' AFTER pass_standard;

-- 2) element_code 全局唯一（并发唯一兜底；服务层已有预检）
--    前置核验：element_code 现存 43 行（33 正牌 + 10 已停用测试残留）零重复后本索引方可建
ALTER TABLE gate_review_elements
    ADD UNIQUE INDEX uk_gate_element_code (element_code);

-- 3) G2-6 语义按 DOC-05（外部资源/IPD系统_五大Gate评审要素_v1.md L103）翻正：
--    「认证与法规清单确认」命中条件=强制认证缺失或周期冲突，属否决位（❌）
--    翻正后全库否决位 14→15，与规格逐元素对齐（G1=5/G2=5/G3=0/G4=3/G5=2）
--    注意：AC-GLB-12 原文「14项否决项」基线需由 P1-6.2 按 DOC-05 复核为 15
UPDATE gate_review_elements SET is_veto = '1' WHERE element_code = 'G2-6' AND is_veto = '0';

-- 回滚参考（如需）：
-- ALTER TABLE gate_review_elements DROP INDEX uk_gate_element_code;
-- ALTER TABLE gate_review_elements
--     DROP COLUMN status, DROP COLUMN version,
--     DROP COLUMN veto_dual_required, DROP COLUMN threshold_json;
-- UPDATE gate_review_elements SET is_veto = '0' WHERE element_code = 'G2-6';
