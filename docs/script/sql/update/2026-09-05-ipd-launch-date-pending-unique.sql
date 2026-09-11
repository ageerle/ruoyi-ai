-- P1（owner 2026-09-05 指令项1a + 项1b）：launch_date_change_requests 加「部分唯一索引」等价约束 + 乐观锁列
--
-- 本脚本一次交付两件，缺一不可：
--   · 项1a：uk_ldcr_pending_project → 杜绝并发提议产生两条在途申请
--   · 项1b：version 列           → 使 LaunchDateChangeRequest#version(@Version) 生效，
--                                  第二签并发只放行 1 次（同 P1-4.3 / R8-P0-9 机制）
--
-- 重要：本表只存在于 docs/script/sql/update/2026-09-05-ipd-launch-date-change-requests.sql，
--   基线 2026-09-04-ipd-p0-tables.sql（26 表）不含它 —— 新环境若不按序执行 update/ 则整张双签表缺失。
--   该基线缺口已单独登记待 owner 裁决（本轮不改他人基线文件）。
--
-- 病症：LaunchDateChangeService.propose 用 selectCount(status=PENDING_SECOND)==0 预检后再 insert，
--       两者不在同一原子临界区。并发两次提议都读到 0 → 同一 project_id 落下两条 PENDING_SECOND，
--       AC-INC-33「同一项目同时只允许一个在途双签」被破坏，且第二签确认时无法判定该条是哪一次提议。
--       表上原有 key idx_ldcr_project_status (project_id, status) 是**普通索引**（见
--       2026-09-05-ipd-launch-date-change-requests.sql:28），不构成任何约束。
--
-- 方案：MySQL 无 PostgreSQL 的 partial unique index，等价实现 = STORED 生成列 + UNIQUE KEY。
--       生成列只在「仍处在待第二签且未软删」时等于 project_id，否则为 NULL；
--       唯一索引允许多个 NULL，因此只对活跃在途申请生效，历史 CONFIRMED/REJECTED 行不受影响。
--
-- 前置检查（必须先跑）：活库里若已存在并发产生的重复行，本脚本的 ADD UNIQUE KEY 会直接失败——
--       这是**故意的**。重复申请行是审计事实，脚本不自动删（删=销毁审计轨迹，见
--       治理轮/全局废弃冗余清理-2026-09-05.md L5）。请人工裁决保留哪一条后再重跑本脚本。
SELECT project_id, COUNT(*) AS dup_pending
FROM launch_date_change_requests
WHERE status = 'PENDING_SECOND'
  AND IFNULL(del_flag, '0') = '0'
GROUP BY project_id
HAVING COUNT(*) > 1;

-- 1) 生成列 pending_project_id（幂等：列已存在则跳过）
SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'launch_date_change_requests'
      AND column_name = 'pending_project_id'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE launch_date_change_requests ADD COLUMN pending_project_id bigint GENERATED ALWAYS AS (IF(`status` = ''PENDING_SECOND'' AND IFNULL(`del_flag`,''0'') = ''0'', `project_id`, NULL)) STORED COMMENT ''部分唯一索引等价列：仅活跃在途申请=project_id，否则 NULL''',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 唯一索引 uk_ldcr_pending_project（幂等：索引已存在则跳过）
SET @idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'launch_date_change_requests'
      AND index_name = 'uk_ldcr_pending_project'
);
SET @sql := IF(@idx_exists = 0,
    'CREATE UNIQUE INDEX uk_ldcr_pending_project ON launch_date_change_requests(pending_project_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) 乐观锁列 version（幂等：列已存在则跳过）——对应 @Version，并发第二签只放行 1 次
SET @ver_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'launch_date_change_requests'
      AND column_name = 'version'
);
SET @sql := IF(@ver_exists = 0,
    'ALTER TABLE launch_date_change_requests ADD COLUMN version int NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号（P1 项1b 并发第二签）'' AFTER remark',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 兼容历史行：NOT NULL DEFAULT 0 已在列定义兜底，此 UPDATE 仅做明示归一（同 P1-4.3 脚本做法）
UPDATE launch_date_change_requests SET version = 0 WHERE version IS NULL;

-- 执行后校验（期望：index_name 一行 + column_name 两行 pending_project_id/version）
SELECT index_name, column_name, seq_in_index, non_unique
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'launch_date_change_requests'
  AND index_name = 'uk_ldcr_pending_project'
ORDER BY seq_in_index;

SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'launch_date_change_requests'
  AND column_name IN ('pending_project_id', 'version')
ORDER BY column_name;

-- ROLLBACK（环境异常回滚用；先删索引再删列）
-- DROP INDEX uk_ldcr_pending_project ON launch_date_change_requests;
-- ALTER TABLE launch_date_change_requests DROP COLUMN pending_project_id;
-- ALTER TABLE launch_date_change_requests DROP COLUMN version;
