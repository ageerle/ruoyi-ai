-- 2026-09-08 D 批次机制 2 扩项（p1-ddl-apply-check.py 新增 GRANT_RULES）首跑抓出的 12 张表级授权缺口。
-- 与同日 gate_arbitrations 漏授权同构：ipd_dev 授权模型 = 库级 SELECT,INSERT + 逐表 CRUD 白名单，
-- 建表 SQL 不带 GRANT 块时新表不进白名单 ⇒ INSERT 静默走库级成功、UPDATE/DELETE 被拒（HTTP 500）。
-- 证据：python3 docs/ipd-系统说明/验收/p1-ddl-apply-check.py --dbs ipd_dev --strict
--   → 表级GRANT: 缺口 12/20（2026-09-08 复跑实证，明细见 ddl-apply-check-result-20260908.json）。
-- 注意：notification_events 的 UPDATE 链路疑似从未真活走通（INSERT 一直走库级）——补授权后
--   首次真活可能暴露隐藏缺陷，属预期，不算本 SQL 的回归。
-- GRANT 幂等（重复执行无害）；账号 host 模式为 127.0.0.1（mysql.user 实查）。
-- 未 apply：等 owner 授权后由 DBA/人工执行（本仓 SQL 登记不等于生效，apply 后须回读核验）。
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
-- 回读校验（应见上列 12 表各 1 行含 Select,Insert,Update,Delete）：
--   SELECT table_name, table_priv FROM mysql.tables_priv
--   WHERE user='ipd_app' AND host='127.0.0.1' AND db='ipd_dev' ORDER BY table_name;
-- 复验门禁（应转 FULL 20/20、exit 0）：
--   python3 docs/ipd-系统说明/验收/p1-ddl-apply-check.py --dbs ipd_dev --strict
