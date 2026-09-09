-- =====================================================================
-- 2026-09-09-ipd-grant-c-batch-4-dml.sql
-- c-batch-4 五张新表的表级授权登记件（GRANT CI 门禁扫描源）。
-- 背景：c-batch-4 已于 2026-09-09 经 owner「以上全部都要完整执行」授权 apply 到
--   ipd_dev（5 表 + products 2 列），本文件把随行的表级 GRANT 落成登记件。
-- 授权模型：库级 SELECT,INSERT + 逐表 CRUD 白名单（与 2026-09-08-ipd-grant-12-tables-dml.sql 同构）。
-- GRANT 幂等（重复执行无害）；账号 host 模式为 127.0.0.1（mysql.user 实查）。
-- =====================================================================

GRANT SELECT, INSERT, UPDATE, DELETE ON ipd_dev.gate_waivers TO 'ipd_app'@'127.0.0.1';
GRANT SELECT, INSERT, UPDATE, DELETE ON ipd_dev.rd_replacements TO 'ipd_app'@'127.0.0.1';
GRANT SELECT, INSERT, UPDATE, DELETE ON ipd_dev.rd_replacement_approvals TO 'ipd_app'@'127.0.0.1';
GRANT SELECT, INSERT, UPDATE, DELETE ON ipd_dev.product_retirements TO 'ipd_app'@'127.0.0.1';
GRANT SELECT, INSERT, UPDATE, DELETE ON ipd_dev.multi_project_capacity_approvals TO 'ipd_app'@'127.0.0.1';
