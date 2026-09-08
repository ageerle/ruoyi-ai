-- DEF-5 PROPOSAL-01 脚本兜底 —— DDL 草稿（2026-09-07 终稿）
-- 配合 PROPOSAL-01-脚本兜底.md §2.1
-- 本文件不入 git（gitignored docs/script/sql/update/ 体系外，仅供审阅）
-- 执行需 owner 现场授权，且必须走 .codex/ipd-dev/config/mysql-client.cnf 隔离库凭证

-- ============================================================================
-- 0. 角色前提（真库现状）
-- ============================================================================
-- 应用账号：ipd_app@127.0.0.1（库 ipd_dev）
-- 超管账号：DBA（root 或迁移专用账号）
-- 表级 GRANT 模型：21 张业务表已有 SELECT/INSERT/UPDATE/DELETE（commit 21131def 系列）
-- 库级 GRANT 模型：默认 *.* 含 SELECT/INSERT/UPDATE/DELETE（即"库级兜底 DML"）—— 本文件要堵这个兜底

-- ============================================================================
-- 1. 应用账号库级 INSERT 兜底收回（仅对核心审计表）
-- ============================================================================
-- 注：audit_logs / audit_log_chain_heads 表名按真库实际核对

-- 1.1 收回 audit_logs 库级 INSERT（应用业务已走表级 GRANT，仍可写）
--     库级 INSERT 收回后，audit_logs 仅接受来自显式表级 GRANT 的写入
--     攻击者即使有 ipd_app 凭证也无法通过未授权路径植入伪造审计行
REVOKE INSERT ON ipd_dev.audit_logs FROM 'ipd_app'@'127.0.0.1';

-- 1.2 收回 audit_log_chain_heads 库级 INSERT（关键：防 GLOBAL 锚点伪造）
--     表级 GRANT 是 S/I/U，业务可任意 INSERT，攻击者植入伪造 GLOBAL 锚行 → 整条审计链锚点失守
--     收回库级 INSERT 后，chain_heads 仅接受应用代码路径显式写入（AuditLogService.append）
REVOKE INSERT ON ipd_dev.audit_log_chain_heads FROM 'ipd_app'@'127.0.0.1';

-- ============================================================================
-- 2. 应用账号库级 UPDATE/DELETE 兜底收回（与 commit e9e6631d Q6 一致）
-- ============================================================================
-- 已 commit e9e6631d（Q6）执行过 REVOKE UPDATE,DELETE，本文件作为完整兜底再列一次（幂等可重跑）

REVOKE UPDATE, DELETE ON ipd_dev.audit_logs FROM 'ipd_app'@'127.0.0.1';
REVOKE UPDATE, DELETE ON ipd_dev.audit_log_chain_heads FROM 'ipd_app'@'127.0.0.1';

-- ============================================================================
-- 3. rebuild-chain 端点临时 GRANT UPDATE（超管 rebuild 时使用）
-- ============================================================================
-- AuditLogMapper.updateChainHash() 是 audit_logs 上唯一 UPDATE 调用方
-- 超管 rebuild chain 时临时 GRANT UPDATE，rebuild 后立即 REVOKE
-- def5-rebuild-grant.sh 由 PROPOSAL-01 §2.2 文档内嵌 bash 片段实现（本文件不单独产脚本）

-- GRANT UPDATE ON ipd_dev.audit_logs TO 'ipd_app'@'127.0.0.1';
-- （rebuild 后立即执行 ↓）
-- REVOKE UPDATE ON ipd_dev.audit_logs FROM 'ipd_app'@'127.0.0.1';

-- ============================================================================
-- 4. 自校验（执行 §1-2 后跑，确认残留 0）
-- ============================================================================
-- SELECT GRANTEE, TABLE_NAME, PRIVILEGE_TYPE
-- FROM information_schema.TABLE_PRIVILEGES
-- WHERE TABLE_SCHEMA = 'ipd_dev'
--   AND TABLE_NAME IN ('audit_logs', 'audit_log_chain_heads')
--   AND GRANTEE = "'ipd_app'@'127.0.0.1'"
--   AND PRIVILEGE_TYPE IN ('INSERT', 'UPDATE', 'DELETE')
-- ORDER BY TABLE_NAME, PRIVILEGE_TYPE;
--
-- 期望：空结果集（0 行）

-- ============================================================================
-- 5. 业务回归探针（执行 §1-2 后跑，确认业务仍可写）
-- ============================================================================
-- 用 ipd_app 身份 INSERT audit_logs：
--   INSERT INTO audit_logs (event_type, actor_id, payload, prev_hash, hash, seq, tenant_id, create_time)
--   VALUES ('DEF5_PROBE', 1, '{}', '', 'def5probe', 0, '000000', NOW());
-- 期望：1 行成功（来自表级 GRANT，不来自库级兜底）
--
-- 然后再 ROLLBACK / DELETE：
--   DELETE FROM audit_logs WHERE event_type = 'DEF5_PROBE';
-- 期望：ERROR 1142 (42000): DELETE command denied to user 'ipd_app'@'127.0.0.1'

-- ============================================================================
-- 6. 执行前确认清单（owner 自查）
-- ============================================================================
-- [ ] 已确认真库表名（audit_logs / audit_log_chain_heads）—— 不是 audit_log
-- [ ] 已确认应用账号 ipd_app@127.0.0.1 与库 ipd_dev 实际存在
-- [ ] 已确认 .codex/ipd-dev/config/mysql-client.cnf 凭证仅本机隔离库使用
-- [ ] 已确认 commit e9e6631d Q6 REVOKE UPDATE/DELETE 已生效（防止本次与上次冲突）
-- [ ] 已通知主协调会话：本卡 DCL 执行将进入 [U0] 阻塞窗口（rebuild chain 端点 UPDATE 临时 GRANT 需授权）
-- [ ] 已确认 rebuild-chain 端点的 PROD/PRE 切换窗口（建议在 OPS-04 维护窗口内执行）

-- ============================================================================
-- 7. 回滚预案（与 PROPOSAL-01 §4 对齐）
-- ============================================================================
-- 如方案 ① 引发不可预期的兼容性问题，立即回滚：
--
-- GRANT INSERT, UPDATE, DELETE ON ipd_dev.audit_logs TO 'ipd_app'@'127.0.0.1';
-- GRANT INSERT, UPDATE, DELETE ON ipd_dev.audit_log_chain_heads TO 'ipd_app'@'127.0.0.1';
--
-- 然后查 commit e9e6631d 决策回填（Q6 拍 P 路径）

-- ============================================================================
-- 8. 与 IPD 硬约束对齐（与 PROPOSAL-01 §5 对齐）
-- ============================================================================
-- G-02 强只追加 ✅ 数据库 GRANT 天然支持：禁止 UPDATE/DELETE 即实现只追加
-- G-04 业务规则高于文档惯例 ✅ 纵深防御是 IPD 最高约束的体现
-- G-07 单企业私有部署 ✅ 单租户脚本无需多租户隔离
-- G-11 审计链/软删原子性 ✅ AuditLogService 写仍走应用层事务，原子性不变
