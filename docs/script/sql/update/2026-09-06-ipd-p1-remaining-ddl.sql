-- P1 应做剩余 6 项 + 端点契约 4 大类缺口 — DDL 一并落地
-- 前置：本批触达 3 列增量 + 0 表新建，全部为列添加（幂等 ALTER）
--
-- 字段语义：
--   bid_invitations.confirm_token        varchar(8)  P1-5.2 遴选二次确认 token（6 字符随机 + 截断安全裕度）
--   bid_invitations.confirm_token_expires datetime    P1-5.2 token 24h 过期
--   projects.last_activity_at            datetime    P1-9.2 存量分段起算（max of stage_action/deliverable/kpi/gate update_time）
--   gate_reviews.gate_code               varchar(20) P1-9.2/§5.3 G3 评审自动创建（独立于 gate_element_results）
--
-- Round 9 P1-5.2：selectResponse 落选通知二次确认——operator 调 /select 时须带 confirmToken（招标单 confirm_token 字段值），
-- 后端比对 + 24h 过期校验。前端流程：调用方先展示「将向 N 名应标者发送落选通知」预演，前端从 /pre-select-token 拿 token + 24h 过期，
-- 用户在 UI 上勾选「已确认」后，将 token 提交到 /select。
--
-- Round 9 P1-9.2：存量 14 天场景复核——projects.last_activity_at 由 trigger / scan 写，本卡只加列。

alter table bid_invitations
    add column confirm_token varchar(8) null comment 'P1-5.2 遴选二次确认 token（6 字符随机）' after selected_response_id,
    add column confirm_token_expires datetime null comment 'P1-5.2 token 24h 过期' after confirm_token;

alter table projects
    add column last_activity_at datetime null comment 'P1-9.2 最近活动日（max of stage_action/deliverable/kpi/gate update_time）' after legacy_effective_at;

-- Round 9 §5.3 G3 评审自动创建入口：gate_reviews 表加 gate_code 列（若已存在则跳过）
-- 注：原始 gate_reviews 表已由更早的 DDL 建好，本卡仅在缺列时增量加。
-- 使用 INFORMATION_SCHEMA 防护：MySQL 8 / 5.7 均支持。
SET @sql_add_gate_code := (
    SELECT IF(
        (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'gate_reviews'
              AND COLUMN_NAME = 'gate_code') > 0,
        'SELECT ''gate_reviews.gate_code 已存在'' AS msg',
        'ALTER TABLE gate_reviews ADD COLUMN gate_code varchar(20) NULL COMMENT ''Gate 编码（G1/G2/G3/G4/G5），用于入口 ?gateCode= 路由'' AFTER project_id'
    )
);
PREPARE stmt FROM @sql_add_gate_code;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 索引：last_activity_at 用于「临界 3 天」扫描（created_at + last_activity_at）
CREATE INDEX idx_projects_last_activity_at ON projects (last_activity_at);
CREATE INDEX idx_gate_reviews_gate_code ON gate_reviews (gate_code);