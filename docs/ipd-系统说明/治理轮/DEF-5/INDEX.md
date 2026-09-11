# DEF-5 决策索引（owner 拍板专用，2026-09-07）

> 子 agent 已产出完整决策支持材料但**未执行真库变更**。本卡保留 todo 等 owner 拍板执行窗口。

## 产出材料（worktree 隔离路径，不在主仓）

| 文件 | 路径 |
|---|---|
| 决策方案（含停写时间线） | `.claude/worktrees/agent-ad567d6ddbe92ca65/docs/ipd-系统说明/治理轮/DEF-5/def5-plan.md` |
| 安全 patch（137 张 GRANT + REVOKE） | `.claude/worktrees/agent-ad567d6ddbe92ca65/docs/ipd-系统说明/治理轮/DEF-5/def5-grants.sql.patch` |
| 验证脚本（前置/后置/烟雾/全量） | `.claude/worktrees/agent-ad567d6ddbe92ca65/docs/ipd-系统说明/治理轮/DEF-5/def5-verify.sh` |

## Owner 必看的 3 件事（按优先级降序）

### #1 ⭐⭐⭐⭐⭐ rebuild-chain 与 G-02 强只追加的工程张力（最大决策点）

- `AuditLogMapper.updateChainHash()` 是 audit_logs 上**唯一**的 UPDATE 调用方
- 修复后 ipd_app 在 audit_logs 上无 UPDATE 权限 → `/api/v1/audit-logs/rebuild-chain` 必然 SQL error 1142
- **三条路径**：
  - **方案 ① 临时 GRANT**（本 patch 默认）—— 超管 rebuild 前 GRANT UPDATE，rebuild 后立即 REVOKE；脚本忘 revoke 由每日 23:50 cron 兜底
  - **方案 ② 独立 ipd_admin 用户 + 双数据源**（推荐中期）—— 但需 java 代码改动（出本任务边界）
  - **方案 ③ DBA 直连**—— 关闭 app 内 rebuild 端点

### #2 ⭐⭐⭐⭐ 方案 ① 的脚本化与审计
- def5-grants.sql.patch 末尾附 def5-rebuild-grant.sh 模板（未单独产出文件，按 plan §5.2 描述）
- **必须配每日 23:50 cron 自动 REVOKE + 报警**（MySQL 5.7+ EVENT SCHEDULER），否则 G-02 漂移

### #3 ⭐⭐⭐ host pattern 收紧（Wave 19 立项）
- 当前 `ipd_app@'%'` 比 `@127.0.0.1` 宽
- 实测连接来源是 `ipd_app@192.168.65.1`（socat 转发副作用），收紧需先排查 socat 拓扑
- 本 patch **保留 `@'%'`**，未触此议题

## Patch 结构（137 张表三档分级）

| 档 | 表 | grant | 数量 |
|---|---|---|---|
| A 强只追加 | `audit_logs` | SELECT, INSERT | 1 |
| B 分配器 | `audit_log_chain_heads` | SELECT, INSERT, UPDATE | 1 |
| C 业务可写 | 其余 135 张 | SELECT, INSERT, UPDATE, DELETE | 135 |

最后一步 `REVOKE ALL PRIVILEGES, GRANT OPTION ON ipd_dev.* FROM 'ipd_app'@'%'` + `FLUSH PRIVILEGES`。

## 停写窗口（plan §4）

总时长 ≤45 分钟（凌晨 02:00-03:00）。10 个步骤从窗口通告→后端停→DB 全连接检查→执行 patch→后置验证→重启→业务烟雾→DEF-4 全量→全量回归→通告解除。**rollback 预案 ≤5 分钟**（重 GRANT 库级 ALL 是 O(1)，REVOKE 137 张表是 O(N) 循环 ≈2s）。

## 状态

- 看板 todo（081fbd58）保留，等 owner 拍板执行窗口
- 子 agent 在 worktree 隔离跑未 commit、未连真库、未改 java
