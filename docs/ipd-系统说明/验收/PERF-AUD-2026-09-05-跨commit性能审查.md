# 性能审查报告 — 跨 commit（2026-09-05 16:00 起，19 commits）

> **报告性质**：主线程补落盘（performance-analyzer agent 按 ROOT_SYSTEM_POLICY 硬约束未写盘，本报告为其原文归档）
> **审查范围**: 19 commit（d9c24227 / a52035aa / 5d1e5d15 / 3e1babbb / 9f687bd4 / 436262b0 / f3026313 / 359c657f / d10d3bdc / 45c6537a / 9f1fb63b / b61c7f35 / 5b95a9d0 / 3cd05145 / 2a519287 / c23fd1f2 / 7cae2138 / cf9de190 / 4bfac1cd）
> **严重度统计**：P0 × 4 | P1 × 6 | P2 × 5（**新增** 15 条；**重复已发车卡** 0 条）

---

## [P0-1] `DeletionRequestService.escalateOverdueLeaderReview` N+1 写放大 + 缺索引

- **文件**: `/Users/mac/Documents/ruoyi-ai/ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/service/DeletionRequestService.java:127-138`
- **commit**: d10d3bdc（P0-6.3 新增）
- **现象**:
  1. 全表 `selectList(eq status LEADER_REVIEW, lt leaderDueAt now)` —— `deletion_requests` 仅有 `idx_dr_entity (entity_type, entity_id)`（DDL 行 460），**缺 (status, leader_due_at) 复合索引**
  2. 对每条 overdue 都 `updateById` + `auditLogService.append`，每条 append = 2 SQL（selectLast + insert）→ **O(3N) SQL**
  3. `listOverdueAdminReview`（L141）同样缺索引
- **触发场景**: 每日定时任务；逾期 100 条 → 100 select + 100 update + 100 append = **300 SQL**；单事务持有 deletion_requests 表
- **修复建议**（owner = DeletionRequestService 作者）:
  1. DDL 加 `idx_dr_status_leader_due (status, leader_due_at)`
  2. 批量 `update deletion_requests set status='ADMIN_REVIEW', admin_due_at=? where status='LEADER_REVIEW' and leader_due_at < now() and id IN (...)`，分页 500/批
  3. 升级审计合并为 1 条总审计 + JSON 列表载荷（"批量升级 N 条，seqs=[..]"）

---

## [P0-2] `BidInvitationService.expireOverdue` N+1 + 全表扫 + 逻辑冗余

- **文件**: `/Users/mac/Documents/ruoyi-ai/ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/service/BidInvitationService.java:78-95`
- **commit**: d10d3bdc（P0-6.3 新增）
- **现象**:
  1. `selectList(eq status OPEN, lt expireAt now)` —— `bid_invitations` 仅有 `idx_bi_project (project_id)`（DDL 行 412），**缺 (status, expire_at) 复合索引**
  2. 每条 OPEN 过期招标单都 `bidResponseMapper.selectCount(eq invitationId)` → N 次 SELECT
  3. **L91 行 `responseCount == 0 ? "EXPIRED" : "EXPIRED"` 三元恒等**（无论 count 结果都 EXPIRED），属于冗余代码 + 误导读 code 人
- **触发场景**: 每日定时；100 条 OPEN 过期 = 200 SQL；事务持有 bid_invitations+bid_responses 两表
- **修复建议**:
  1. DDL 加 `idx_bi_status_expire (status, expire_at)`
  2. 批量 `update bid_invitations set status='EXPIRED' where id IN (...) and status='OPEN' and expire_at < now()`
  3. 删除 L91 三元冗余，直接 `inv.setStatus("EXPIRED")`
  4. 如确需 response count，改用 `where exists (select 1 from bid_responses where invitation_id = bid_invitations.id)` 一次性 join

---

## [P0-3] `audit_logs` 按 operator_id 范围查询缺覆盖索引

- **文件**: `/Users/mac/Documents/ruoyi-ai/ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/service/AuditLogService.java:151-169`
- **DDL**: `docs/script/sql/update/2026-09-04-ipd-p0-tables.sql:485-508`
- **现象**: `listByOperatorIds` `WHERE operator_id IN (...) ORDER BY seq DESC LIMIT m,n` —— `audit_logs` 仅有 `uk_audit_seq (seq)` 唯一索引，**缺 (operator_id, seq) 复合索引**；深分页走 filesort
- **实测**: QA-05 报告 p95=1640ms（pageSize=200，深页）；500 操作人 × 50 万行（隔离库 ipd_perf）
- **修复建议**（owner = AuditLogService 作者，**需用户确认 + 压测验证**）:
  1. DDL 加 `idx_audit_op_seq (operator_id, seq DESC)`（注意 InnoDB 索引 8KB 页，seq 用 bigint 8B 即可）
  2. 深分页改为 keyset：`WHERE operator_id IN (...) AND seq < :cursor ORDER BY seq DESC LIMIT :n`（避免 OFFSET 1000000 类扫描）
  3. **建议在 ipd_perf 库跑 EXPLAIN 取证**: 覆盖前后 `EXPLAIN FORMAT=JSON SELECT ... WHERE operator_id IN (1,2,3) ORDER BY seq DESC LIMIT 200, 200;`

---

## [P0-4] `BidResponseService.listByRdPm` 缺索引 + 不分页

- **文件**: `/Users/mac/Documents/ruoyi-ai/ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/service/BidResponseService.java:81-87`
- **DDL**: `docs/script/sql/update/2026-09-04-ipd-p0-tables.sql:415-432`
- **现象**: `bid_responses` 仅有 `idx_br_invitation (invitation_id)`，**缺 (rd_pm_id, create_time) 复合索引**；`listByRdPm` 全表扫
- **触发场景**: 研发 PM 个人工作台 — 每人 50+ 应标，1 次查询 ~ 全表；多人并发更糟
- **修复建议**: DDL 加 `idx_br_rd_pm (rd_pm_id, create_time DESC)`；查询加分页（pageSize 上限 200）

---

## [P1-1] `ReceiptLedgerService` 全部查询无分页 + 全内存过滤窗口

- **文件**: `/Users/mac/Documents/ruoyi-ai/ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/service/ReceiptLedgerService.java:89-120`
- **现象**:
  1. `calculateAchievementRate`（L89）`selectList` 全表 + Java 循环 `isInWindow` 过滤 → 数据量大时 OOM 风险 + CPU 浪费
  2. `listByProject`（L114）不分页，返回全部回款明细
  3. **DDL 文件缺失**: `grep "create table.*receipt" /Users/mac/Documents/ruoyi-ai/docs/script/sql/update/*.sql` 返回空 → 索引状态未知（**a52035aa QA-04-D2 应已建，请补审**）
- **修复建议**:
  1. SQL 端 WHERE 过滤窗口: `WHERE project_id=? AND source='RECEIPT' AND receipt_month >= window_start AND receipt_month <= window_end`
  2. 加 `(project_id, source, receipt_month)` 复合索引
  3. `listByProject` 加分页（pageSize 上限 200）

---

## [P1-2] `BidResponseService.listByInvitation` 不分页

- **文件**: `BidResponseService.java:70-76`
- **现象**: 单招标 100+ 应标时全量返回；与 [P0-4] 同根

---

## [P1-3] `application-prod.yml` Hikari 参数与 p6spy 隐患

- **文件**: `/Users/mac/Documents/ruoyi-ai/ruoyi-admin/src/main/resources/application-prod.yml:72-86`
- **现象**:
  1. `connectionTimeout: 30000` —— dev 改为 5000（d9c24227），但 prod 沿用 30s；QA-05-P1 注释明确「30s 排队超时是高并发雪崩放大器」
  2. `p6spy: true`（行 49）—— dev/prod 都开；logback-plus 注释明确「不建议生产环境使用」
  3. `maxPoolSize: 20`（行 74）—— dev 已 40，prod 沿用 20 与 100 并发目标不匹配
- **触发场景**: 生产部署后并发 > 20 即排队雪崩 + 30s 超时雪崩放大；p6spy 额外 ~30% 性能损耗
- **修复建议**（**⚠️ 需用户确认 + 压测验证**）:
  1. prod 同步 dev 参数: `maxPoolSize: 40, connectionTimeout: 5000`
  2. prod `p6spy: false`（保留日志查询走 MyBatis-Plus 自带 SQL 调试）
  3. ipd-perf 跑相同负载复测，对比 p95 改善

---

## [P1-4] `AuditLogService.append` 写路径仍是 2 SQL/写 + 多实例竞态重试

- **文件**: `/Users/mac/Documents/ruoyi-ai/ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/service/AuditLogService.java:75-90`
- **commit**: 7cae2138 / c23fd1f2 / 5b95a9d0（DEF-4 三修复）
- **现象**: 每条 append = `selectLast` (ORDER BY seq DESC LIMIT 1) + `insert`，**uk_audit_seq 冲突时最多 6 SQL**（重试 3 次 × 2）
- **触发**: 100 并发写审计 → 200+ SQL；多实例共库竞态时重试雪崩
- **注释自承**: 「长期方案 = QA-04 泳道 audit_log_chain_heads 原子递增（归属兄弟，本类不引用）」—— **兄弟卡未落地，仍走 selectLast**
- **修复建议**:
  1. 合并兄弟卡推进 `audit_log_chain_heads`（单行原子 `UPDATE chain_heads SET seq=seq+1, curr_hash=? WHERE key='audit'`，无竞争）
  2. 当前临时缓解: `SELECT seq, curr_hash FROM audit_logs WHERE seq = (SELECT MAX(seq) FROM audit_logs)` 子查询合并 1 SQL（仍走 uk_audit_seq 反向）

---

## [P1-5] `ProjectBootstrapService.bootstrap` 同步 6 阶段 + 69 动作 = 75+ SQL

- **文件**: `/Users/mac/Documents/ruoyi-ai/ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/service/ProjectBootstrapService.java:56-57`
- **commit**: d4836573（P1-3 动作清单 v3）
- **现象**: 注释「PERF-01 取号已 synchronized 保护」，但 `selectAllStageIdsForBootstrap` + `selectAllActionIdsForBootstrap` + 75 次单条 insert
- **触发**: 每个项目创建 = 75+ SQL；批量创建项目（季度启动 N 个）= 75N SQL
- **修复建议**: 改 MyBatis-Plus `saveBatch(list)`（需 `sqlSessionFactory.openSession(ExecutorType.BATCH)`，参见 IPERF-01 已发车卡）

---

## [P1-6] `CoefficientChangeService` / `LaunchDateChangeService` 单 update + audit 模式

- **文件**: `/Users/mac/Documents/ruoyi-ai/ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/service/CoefficientChangeService.java:101,131` + `LaunchDateChangeService.java:97,142`
- **现象**: 每次变更 = 2 selectById + 1 update + 1 audit append = 4 SQL；调用 [P1-4] 的 append 写路径
- **建议**: 低频路径不强制修；可后续在 `audit` 框架层合并

---

## [P2-1] `DeletionRequestService.escalateOverdueLeaderReview` 串行处理

- 见 [P0-1]；批量 update 仍单事务持有，可能阻塞 deletion_requests 表写

---

## [P2-2] `ReceiptLedgerService.calculateAchievementRate` 全内存循环过滤

- 见 [P1-1]；SQL 层窗口过滤缺失

---

## [P2-3] 应用 jar 启动参数未在仓库固定

- **现象**: 无 `Dockerfile` / `scripts/start.sh` 固定 `-Xmx` / GC 参数；QA-05 实例用 `-Xmx2g` 但 50 万审计 + 5 万 product 下应评估
- **建议**（**需用户确认**）: 加 `scripts/start.sh` 固定 `-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./logs/`

---

## [P2-4] `logback-plus.xml` 全局 INFO + 无 profile 区分

- **文件**: `/Users/mac/Documents/ruoyi-ai/ruoyi-admin/src/main/resources/logback-plus.xml:121`
- **现象**: `<root level="info">` 是基线，但 prod 应默认 WARN + chat/ruoyi 包级 WARN；当前 dev/prod 共享
- **建议**: 用 `logback-spring.xml` + `<springProfile name="prod">` 切 WARN

---

## [P2-5] `SystemConfigService.cache` 单 JVM ConcurrentHashMap 内存无界

- **文件**: `/Users/mac/Documents/ruoyi-ai/ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/service/SystemConfigService.java:34`
- **现象**: 注释自承「多实例部署时需升级 Redis 广播失效」—— 当前单实例 OK；若扩多实例将出幻读（一个实例写，其他实例缓存 stale）
- **建议**: 前瞻 Redis pub/sub 失效（需用户确认部署形态）

---

## 与已发车 [QA-05-P1]/[QA-05-P2]/[QA-05-P3] 去重结论

| 已发车卡 | 覆盖范围 | 本报告**新增** vs **重复** |
|---|---|---|
| **QA-05-P1**（d9c24227）dev Hikari 池 = 40 | application.yml + application-dev.yml | **重复 0%；新增 [P1-3]**（prod 未跟进，p6spy 未关） |
| **QA-05-P2**（9f687bd4）深分页 p95=1640ms | 容量验收报告，未触发代码修复 | **重复 0%；新增 [P0-3]**（operator_id 覆盖索引缺失是 P2 报告已识别但未建卡） |
| **QA-05-P3**（同报告）锁等待 | audit hash 链 ORDER BY DESC LIMIT 1 串行化 | **重复 0%；新增 [P1-4]**（DEF-4 c23fd1f2 移除 FOR UPDATE 后审计写仍是 2 SQL/写，brother 卡 audit_log_chain_heads 未落地） |

**本报告全部 15 条 finding 均为新增**；3 张已发车卡仅记录在 ipd_perf 数据基线，**未触发代码/索引/连接池变更**。

---

## Top-3 急迫瓶颈

1. **[P0-1] DeletionRequestService.escalateOverdueLeaderReview** —— 每日定时 N+1 写放大 + 缺索引；100 条逾期 = 300 SQL，且单事务持有 deletion_requests
2. **[P0-2] BidInvitationService.expireOverdue** —— 每日定时 N+1 + 全表扫 + 逻辑冗余；100 条 OPEN 过期 = 200 SQL + L91 三元恒等
3. **[P0-3] audit_logs operator_id 覆盖索引缺失** —— QA-05 已实测 p95=1640ms 的深分页瓶颈；P2 报告未跟进建卡；建议 ipd_perf 跑 EXPLAIN 取证后立刻建 `idx_audit_op_seq (operator_id, seq DESC)`

---

## 蜂群纪律声明（已遵守）

- 仅 Read/Grep/Bash 只读操作
- 未 git push/reset/clean/branch-D/checkout./restore.
- 未 commit；主线程统一 commit
- ipd_perf 库 EXPLAIN 取证建议给主线程（需在隔离库执行，不污染 ipd_dev）
- ipd_dev 仅 Read，未发起任何 DDL/DML

## 报告产物

- 路径: `/Users/mac/Documents/ruoyi-ai/docs/ipd-系统说明/验收/PERF-AUD-2026-09-05-跨commit性能审查.md`
- finding 数: P0 × 4 | P1 × 6 | P2 × 5 = **15 项**
- top-3: P0-1（删除审核升级 N+1） / P0-2（招标过期 N+1 + L91 三元冗余） / P0-3（audit operator_id 覆盖索引）

## 责任划分（fix-forward owner）

| finding | 修复 owner |
|---|---|
| P0-1/2/3/4 | IPD owner + db-migration skill |
| P1-1/2/3 | IPD owner + db-migration skill |
| P1-4/5 | AuditLogService / ProjectBootstrapService 作者 |
| P1-6 | CoefficientChangeService / LaunchDateChangeService 作者 |
| P2-* | 各模块 owner |