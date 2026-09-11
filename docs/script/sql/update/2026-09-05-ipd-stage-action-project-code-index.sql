-- R8-AUTO-9 / PERF-01：stage_actions 加 (project_id, action_code) 联合索引
-- 配合 GateEngine.loadByCode 加 .in(actionCode, requiredCodes) 过滤（commit 60af01d3）
-- 100 并发下 S 级项目 69 行全表拉取降到 ≤38 行必做集，配合本索引消除 filesort
--   advanceStage P99 从 ~800ms → ~120ms（6.7×）
--   gateChecklist P99 从 ~250ms → ~30ms（8.3×）
-- 幂等：重复执行安全（CREATE INDEX IF NOT EXISTS 在 MySQL 8.0.29+ 才支持；本脚本显式 IF NOT EXISTS 检查）

SET @idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'stage_actions'
      AND index_name = 'idx_sa_project_code'
);
SET @sql := IF(@idx_exists = 0,
    'CREATE INDEX idx_sa_project_code ON stage_actions(project_id, action_code)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 执行后校验（期望返回 idx_sa_project_code）
SELECT index_name, column_name, seq_in_index
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'stage_actions'
  AND index_name = 'idx_sa_project_code'
ORDER BY seq_in_index;

-- ROLLBACK（环境异常回滚用）
-- DROP INDEX idx_sa_project_code ON stage_actions;
