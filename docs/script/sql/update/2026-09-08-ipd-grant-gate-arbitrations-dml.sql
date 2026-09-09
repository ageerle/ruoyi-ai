-- 2026-09-08 WB-17-1 真活验证暴露的授权缺口（gate 999101 链路实证）：
-- ipd_dev 授权模型 = 库级 SELECT,INSERT + 逐表 CRUD 白名单（128 张表），
-- gate_arbitrations 建表时漏登记表级授权 ⇒ INSERT 走库级权限成功、
-- UPDATE 被拒（HTTP 500: UPDATE command denied to user 'ipd_app'）。
-- 旧 arbitrate 为 INSERT-only 从未触发；2026-09-08 改 UPDATE 语义（预落行落决策）后首次暴露。
-- 与 gates / gate_reviews / gate_element_results / gate_review_elements 同款四表权限对齐。
-- GRANT 幂等（重复执行无害）；账号 host 模式为 127.0.0.1（mysql.user 实查）。
GRANT SELECT, INSERT, UPDATE, DELETE ON ipd_dev.gate_arbitrations TO 'ipd_app'@'127.0.0.1';
-- 回读校验（应输出 1 行表级 GRANT）：
-- SHOW GRANTS FOR 'ipd_app'@'127.0.0.1';
