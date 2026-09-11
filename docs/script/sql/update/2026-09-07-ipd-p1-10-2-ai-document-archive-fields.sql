-- P1-10.2：AI 文档审核→归档门禁 + 版本对比（AC-AI-03 / AC-AI-05，BR-AI-02 / BR-AI-06）
-- 适用库：ipd_dev（本地隔离环境；禁止迁移生产，由 DBA 按发布窗口执行）
-- 前置：ai_documents 表已存在（P0-2 基线 + P1-10.1 增列），当前 0 行，本 ALTER 无损。

-- 1) 拒绝原因——BR-AI-03 审核拒绝落 audit；与 review/reject 同步落 review_comment
ALTER TABLE ai_documents
    ADD COLUMN review_comment VARCHAR(1000) NULL COMMENT '审核/拒绝备注（P1-10.2）' AFTER reviewed_at;

-- 2) 归档落名——archived_at / archived_by 仅 ARCHIVED 行非空
ALTER TABLE ai_documents
    ADD COLUMN archived_at DATETIME NULL COMMENT '归档时间（P1-10.2，AC-AI-03）' AFTER review_comment,
    ADD COLUMN archived_by BIGINT NULL COMMENT '归档操作者 ID（P1-10.2，BR-AI-06 审计身份可信）' AFTER archived_at;

-- 3) 状态索引——运营侧按状态筛选（如查全部 ARCHIVED 行）
ALTER TABLE ai_documents
    ADD INDEX idx_ai_doc_status (status);

-- 回滚（仅本地隔离环境验证用）：
-- ALTER TABLE ai_documents
--     DROP INDEX idx_ai_doc_status,
--     DROP COLUMN archived_by,
--     DROP COLUMN archived_at,
--     DROP COLUMN review_comment;