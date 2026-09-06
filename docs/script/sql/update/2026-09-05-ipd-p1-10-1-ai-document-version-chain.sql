-- P1-10.1：AI 文档原始输出与不可丢失版本链（BR-AI-03 / AC-AI-04、AC-AI-06）
-- 适用库：ipd_dev（本地隔离环境；禁止迁移生产，由 DBA 按发布窗口执行）
-- 前置：ai_documents 表已存在（P0-2 基线 26 表），当前 0 行，补列为无损 ALTER。

-- 1) 版本不可变锚点：写入时计算 sha256(content) hex（64 字符），历史行内容与摘要永不更新
ALTER TABLE ai_documents
    ADD COLUMN content_sha256 CHAR(64) NULL COMMENT '内容摘要 sha256 hex（P1-10.1 版本不可变锚点，写入时计算）' AFTER token_completion;

-- 2) 人工审核落名：BR-AI-03 AI 输出须人工审核后生效
ALTER TABLE ai_documents
    ADD COLUMN reviewed_by BIGINT NULL COMMENT '审核人ID' AFTER version_no,
    ADD COLUMN reviewed_at DATETIME NULL COMMENT '审核时间' AFTER reviewed_by;

-- 3) 线性链防分叉：一个父版本最多一个子版本（唯一索引）；
--    v1 的 parent_version_id 为 NULL，MySQL 唯一索引允许多个 NULL，多条链的 v1 互不冲突；
--    并发双写同一父版本时第二个 insert 撞键 → service 映射 STATE_CONFLICT 明确报错。
ALTER TABLE ai_documents
    ADD UNIQUE INDEX uk_ai_doc_parent (parent_version_id);

-- 回滚（仅本地隔离环境验证用）：
-- ALTER TABLE ai_documents
--     DROP INDEX uk_ai_doc_parent,
--     DROP COLUMN reviewed_at,
--     DROP COLUMN reviewed_by,
--     DROP COLUMN content_sha256;
