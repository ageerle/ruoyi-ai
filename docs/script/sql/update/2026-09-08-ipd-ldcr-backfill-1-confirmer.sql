-- =====================================================================
-- 2026-09-08-ipd-ldcr-backfill-1-confirmer.sql
-- 目的:R11 / A1 修复后,补齐存量 PENDING_SECOND 死路 1 行的 confirmer_id/confirmer_role
--      (原设计 propose 时刻未预落第二签人,R10 收口时确认 1 行待回填)
-- 来源:docs/ipd-系统说明/缺表4类-14问澄清-20260908.md §存量死路
--
-- ⚠ 草稿状态:待 owner 拍板再 apply;本文件首行为注释,无 DDL 副作用。
-- 拍板后由 root@socket 通道执行,UPDATE 前必做 in-list 确认:
--   SELECT id, proposer_id, proposer_role, project_id, status, confirmer_id
--   FROM launch_date_change_requests
--   WHERE id=2096324844730716161 FOR UPDATE;
-- =====================================================================

-- 1) 死路 1 行核实(应用前必跑,确认行仍在 PENDING_SECOND)
--    期望返回 1 行:proposer_id=900101 SUPER_ADMIN, confirmer_id IS NULL
SELECT id, project_id, proposer_id, proposer_role, confirmer_id, confirmer_role, status, del_flag
FROM launch_date_change_requests
WHERE id = 2096324844730716161
  AND del_flag = '0'
  AND status = 'PENDING_SECOND';

-- 2) 候选回填目标交叉验证
--    主组 900001 内 RD_PM=900104(ipd-rd) 同组/角色白名单内/与 proposer 不同人
SELECT id, name, person_type, group_id, account_status, employment_status
FROM persons
WHERE id IN (900101, 900104)
  AND del_flag = '0'
  AND account_status = 'ACTIVE'
  AND employment_status = 'ACTIVE';

-- 3) 回填 UPDATE(乐观:仅更新原仍为 PENDING_SECOND + confirmer_id IS NULL 的行)
UPDATE launch_date_change_requests
SET confirmer_id   = 900104,
    confirmer_role = 'RD_PM',
    update_by      = 900101,
    update_time    = NOW()
WHERE id = 2096324844730716161
  AND del_flag = '0'
  AND status = 'PENDING_SECOND'
  AND confirmer_id IS NULL;

-- 4) 回填后回读核验(应用后必跑)
--    期望返回:confirmer_id=900104, confirmer_role='RD_PM'
SELECT id, proposer_id, proposer_role, confirmer_id, confirmer_role, status
FROM launch_date_change_requests
WHERE id = 2096324844730716161;

-- 5) 索引/约束防回归核验(应用后必跑)
--    期望:uk_ldcr_pending_project 索引仍存在,无新增冲突行
SELECT INDEX_NAME, COLUMN_NAME, NON_UNIQUE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'ipd_dev'
  AND TABLE_NAME = 'launch_date_change_requests'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

SELECT COUNT(*) AS conflict_rows
FROM launch_date_change_requests
WHERE status = 'PENDING_SECOND'
  AND del_flag = '0'
  AND project_id = 2096324843405316098;
-- 期望:conflict_rows = 1
