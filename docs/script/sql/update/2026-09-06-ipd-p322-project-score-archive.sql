-- P3-2.2 项目绩效不可变归档与规则快照
-- project_scores 是历史 P3-6 贡献度占位表，语义已冲突；本表专用于 20/40/40 项目绩效。
-- 幂等：information_schema 检查后 ALTER，重复执行不报 1060/1061/1022。

SET @table_exists := (
  SELECT COUNT(*)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'project_score_records'
);
SET @sql := IF(@table_exists = 0, 'CREATE TABLE project_score_records (
  id BIGINT NOT NULL COMMENT ''归档主键'',
  project_id BIGINT NOT NULL COMMENT ''项目 ID'',
  person_id BIGINT NOT NULL COMMENT ''被评 PM 人员 ID'',
  pm_role VARCHAR(16) NOT NULL COMMENT ''MARKET_PM|RD_PM'',
  component_type VARCHAR(24) NOT NULL COMMENT ''SELF|MARKET_LEADER|RD_LEADER'',
  score DECIMAL(5,2) NOT NULL COMMENT ''0~100 评分'',
  version_no INT NOT NULL COMMENT ''组件追加版本'',
  rule_version INT NOT NULL COMMENT ''kpi.reviewWeights 版本'',
  rule_snapshot JSON NOT NULL COMMENT ''不可变规则 JSON'',
  status VARCHAR(16) NOT NULL DEFAULT ''ARCHIVED'' COMMENT ''ARCHIVED'',
  author_id BIGINT NOT NULL COMMENT ''提交人'',
  author_role VARCHAR(24) NOT NULL COMMENT ''提交人角色'',
  reason VARCHAR(500) DEFAULT NULL COMMENT ''更正说明'',
  submitted_at DATETIME NOT NULL COMMENT ''提交时间'',
  create_dept BIGINT DEFAULT NULL, create_by BIGINT DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by BIGINT DEFAULT NULL, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  tenant_id VARCHAR(20) DEFAULT ''000000'',
  del_flag CHAR(1) DEFAULT ''0'',
  PRIMARY KEY (id),
  UNIQUE KEY uk_score_record_component_version (project_id, person_id, pm_role, component_type, version_no, tenant_id),
  KEY idx_score_record_settlement (project_id, person_id, version_no),
  KEY idx_score_record_rule (rule_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT=''项目绩效不可变归档（P3-2.2）''', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 回滚（人工维护脚本，不自动执行）：
-- DROP TABLE project_score_records;
