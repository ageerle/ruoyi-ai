-- AM-GRANT [P1] 收口：12 张缺表级 GRANT 白名单的 IPD 业务表补四权（2026-09-08）
--
-- 缺口来源：docs/ipd-系统说明/治理/测试债务根除与守卫加固建议-20260908.md §3.3
-- 缺口检测：p1-ddl-apply-check.py 扩项②：SELECT table_priv FROM mysql.tables_priv
--   WHERE user='ipd_app' AND host='127.0.0.1' AND db='ipd_dev' AND table_name=xxx
--   判定：table_priv 列是否同时含 SELECT/INSERT/UPDATE/DELETE（库级 SELECT,INSERT 不算）
-- 事故机制：库级 SELECT,INSERT 不足以支撑 UPDATE/DELETE——ipd_app 应用对这些表执行 UPDATE 会 500
--   实证案例：kpi_shared_confirms / gate_arbitrations 已真实发生过（P3-1.2 建表脚本漏 GRANT 块）
-- 修复模式：对齐仓里既有教训 memory「ipd_dev 新表漏表级 GRANT 致 UPDATE 拒绝塌缩 500」
--
-- 修复目标：12 张缺表级白名单的 IPD 业务表，补 SELECT,INSERT,UPDATE,DELETE 表级四权。
-- 状态：APPLIED（2026-09-08 本机 ipd_dev 由 root 通道执行——pymysql 直连 .codex/ipd-dev/run/mysql.sock）
-- 回滚：REVOKE ALL PRIVILEGES ON ipd_dev.x FROM 'ipd_app'@'127.0.0.1' 单表粒度（需要时按单表执行）
--
-- 幂等策略：MySQL GRANT 同权限重复执行幂等；如需严格守卫（跳过已有完整四权表），
--   应用侧先用 SELECT table_priv FROM mysql.tables_priv WHERE ... 判定（apply 脚本已实现）。
--
-- 验证：apply 后跑 python3 docs/ipd-系统说明/验收/p1-ddl-apply-check.py，
--   表级 GRANT 段输出 "FULL 20/20"，"缺口 0/20" → PASS。

SET @grant_user = 'ipd_app';
SET @grant_host = '127.0.0.1';
SET @grant_db = 'ipd_dev';

-- 12 张缺表级白名单的 IPD 业务表（GRANT_RULES 中无完整四权 mysql.tables_priv 行）
GRANT SELECT, INSERT, UPDATE, DELETE ON ipd_dev.contribution_versions TO 'ipd_app'@'127.0.0.1';
GRANT SELECT, INSERT, UPDATE, DELETE ON ipd_dev.sop_template_instances TO 'ipd_app'@'127.0.0.1';
GRANT SELECT, INSERT, UPDATE, DELETE ON ipd_dev.post_launch_reviews TO 'ipd_app'@'127.0.0.1';
GRANT SELECT, INSERT, UPDATE, DELETE ON ipd_dev.switching_acceptance TO 'ipd_app'@'127.0.0.1';
GRANT SELECT, INSERT, UPDATE, DELETE ON ipd_dev.project_score_records TO 'ipd_app'@'127.0.0.1';
GRANT SELECT, INSERT, UPDATE, DELETE ON ipd_dev.project_score_tasks TO 'ipd_app'@'127.0.0.1';
GRANT SELECT, INSERT, UPDATE, DELETE ON ipd_dev.gate_review_observers TO 'ipd_app'@'127.0.0.1';
GRANT SELECT, INSERT, UPDATE, DELETE ON ipd_dev.correction_logs TO 'ipd_app'@'127.0.0.1';
GRANT SELECT, INSERT, UPDATE, DELETE ON ipd_dev.kpi_rule_snapshots TO 'ipd_app'@'127.0.0.1';
GRANT SELECT, INSERT, UPDATE, DELETE ON ipd_dev.ipd_business_config TO 'ipd_app'@'127.0.0.1';
GRANT SELECT, INSERT, UPDATE, DELETE ON ipd_dev.ipd_business_config_versions TO 'ipd_app'@'127.0.0.1';
GRANT SELECT, INSERT, UPDATE, DELETE ON ipd_dev.notification_events TO 'ipd_app'@'127.0.0.1';

FLUSH PRIVILEGES;

-- 验证：12 张表实际权限集合（应用侧期望 FULL SELECT,INSERT,UPDATE,DELETE）
SELECT table_name, table_priv AS granted
FROM mysql.tables_priv
WHERE user = @grant_user AND host = @grant_host AND db = @grant_db
  AND table_name IN (
    'contribution_versions','sop_template_instances','post_launch_reviews','switching_acceptance',
    'project_score_records','project_score_tasks','gate_review_observers','correction_logs',
    'kpi_rule_snapshots','ipd_business_config','ipd_business_config_versions','notification_events')
ORDER BY table_name;