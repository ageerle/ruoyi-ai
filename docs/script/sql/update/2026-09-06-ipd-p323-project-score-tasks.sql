-- P3-2.3 上市 30/90 日项目绩效待办
-- 幂等：information_schema 检查后建表/索引。

SET @table_exists := (
  SELECT COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project_score_tasks'
);
SET @sql := IF(@table_exists = 0, 'CREATE TABLE project_score_tasks (
  id BIGINT NOT NULL COMMENT ''待办主键'',
  project_id BIGINT NOT NULL COMMENT ''项目 ID'',
  person_id BIGINT NOT NULL COMMENT ''被评/待办 PM 人员 ID'',
  target_type VARCHAR(24) NOT NULL COMMENT ''SELF_SCORING|LEADER_REVIEW'',
  due_at DATETIME NOT NULL COMMENT ''任务截止日'',
  launch_date_snapshot DATETIME NOT NULL COMMENT ''创建/重排时上市日期'',
  status VARCHAR(16) NOT NULL DEFAULT ''PENDING'' COMMENT ''PENDING|DONE|CANCELLED'',
  action_url VARCHAR(500) DEFAULT NULL,
  create_dept BIGINT DEFAULT NULL, create_by BIGINT DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by BIGINT DEFAULT NULL, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  tenant_id VARCHAR(20) DEFAULT ''000000'',
  del_flag CHAR(1) DEFAULT ''0'',
  PRIMARY KEY (id),
  UNIQUE KEY uk_score_task_target (project_id, person_id, target_type, tenant_id),
  KEY idx_score_task_due (status, due_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT=''上市后项目绩效待办（P3-2.3）''', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 回滚（人工维护脚本，不自动执行）：
-- DROP TABLE project_score_tasks;
