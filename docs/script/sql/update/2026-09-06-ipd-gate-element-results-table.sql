-- W4-Defence-G1 / AC-GATE-15 G1 否决项硬阻断落库（DDL 幂等）
-- 卡：B-FIX-PACK-1 子卡（看板 task id 2e4dfcfc-728c-4163-a071-a08b3bafbf9a）
-- 范围：gate_element_results 物理表（DROP-and-CREATE 不可逆 → 走 ALTER/ADD 幂等改造）
-- apply 前：p1-ddl-apply-check.py 复核；仅在隔离开发环境执行

-- 0) information_schema 幂等：本卡不重建表（2026-09-04-ipd-p0-tables.sql L261 已建实体表 gate_element_results）
--    本卡仅补足列：evidence_ref / condition_note / responsible_person_id / closed_evidence / leftover_status
--    / sign_due_at / element_snapshot，凡 information_schema.COLUMNS 已存在则跳过。

-- 1) gate_element_results.evidence_ref 判定证据附件引用（FAIL 必填 AC-GATE-02）
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'gate_element_results'
      AND COLUMN_NAME = 'evidence_ref'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE gate_element_results ADD COLUMN evidence_ref varchar(500) NULL COMMENT ''判定证据附件引用（FAIL 必填 AC-GATE-02）'' AFTER condition_note',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) gate_element_results.responsible_person_id CONDITIONAL 责任人（AC-GATE-16）
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'gate_element_results'
      AND COLUMN_NAME = 'responsible_person_id'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE gate_element_results ADD COLUMN responsible_person_id bigint NULL COMMENT ''条件遗留责任人（AC-GATE-16）'' AFTER leftover_item',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) gate_element_results.closed_evidence 关闭凭证（仅责任人/超管可关）
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'gate_element_results'
      AND COLUMN_NAME = 'closed_evidence'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE gate_element_results ADD COLUMN closed_evidence varchar(500) NULL COMMENT ''关闭遗留凭证（close 时必填）'' AFTER leftover_due_at',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) gate_element_results.sign_due_at 签署期限（BR-GATE-04 3 自然日；submit/reopen 起算）
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'gate_element_results'
      AND COLUMN_NAME = 'sign_due_at'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE gate_element_results ADD COLUMN sign_due_at datetime NULL COMMENT ''签署期限（BR-GATE-04）'' AFTER del_flag',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5) gate_element_results.element_snapshot 要素快照（提交时冻结，避免要素停用影响在途）
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'gate_element_results'
      AND COLUMN_NAME = 'element_snapshot'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE gate_element_results ADD COLUMN element_snapshot longtext NULL COMMENT ''要素定义快照 JSON（提交时冻结）'' AFTER sign_due_at',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6) 索引 gate_id + element_id 唯一约束：同一 Gate 同一要素只能有一条 result 行（幂等）
SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'gate_element_results'
      AND INDEX_NAME = 'uk_ger_gate_element'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE gate_element_results ADD UNIQUE KEY uk_ger_gate_element (gate_id, element_id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 7) 索引：遗留项扫描（OPEN + due < now）
SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'gate_element_results'
      AND INDEX_NAME = 'idx_ger_leftover'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE gate_element_results ADD KEY idx_ger_leftover (leftover_status, leftover_due_at)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
