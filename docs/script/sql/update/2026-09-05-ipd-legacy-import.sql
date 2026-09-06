-- P1-9.1 / BR-PROD-03：存量导入 LEGACY 元数据 + 动作「历史缺失」标记（不伪造 DONE）
-- 运行账号需 ALTER 权限（mysql-migrator / root）；随后 GRANT 给 ipd_app 若缺列权限。

ALTER TABLE projects
    ADD COLUMN declared_stage VARCHAR(16) NULL COMMENT '存量申报当前阶段（展示/补齐目标）' AFTER current_stage,
    ADD COLUMN legacy_effective_at DATETIME NULL COMMENT '存量导入生效日' AFTER source,
    ADD COLUMN missing_history_ack CHAR(1) NULL DEFAULT '0' COMMENT '历史缺失声明 1=已确认' AFTER legacy_effective_at,
    ADD COLUMN catchup_status VARCHAR(16) NULL COMMENT 'IN_PROGRESS|COMPLETE 补齐状态' AFTER missing_history_ack;

ALTER TABLE stage_actions
    ADD COLUMN history_mark VARCHAR(32) NULL COMMENT 'HISTORICAL_MISSING=历史缺失（不伪造DONE）' AFTER status;
