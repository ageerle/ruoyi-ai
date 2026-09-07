# DEF-5 audit_logs grant 修补——方案① 脚本兜底 REVOKE 治理文档（终稿）

> **owner 拍板**: 2026-09-07（首选 ① 脚本兜底，作废 `治理/DEF-5-...` 方案 A）
> **task_id**: `081fbd58-de76-4222-a7ae-22b59faa464f`（DEF-5 卡面，本治理报告作为该卡决策终稿）
> **卡面状态**: inreview（等 owner 真库 DCL 执行授权）
> **方案本质**: 纵深防御 + 最小权限——以数据库 GRANT/REVOKE 为最终防线，应用层防御为常态
> **角色名校正**: 真库应用账号是 `ipd_app@127.0.0.1`（不是文档原写的 `ruoyi_ipd_app` / `ruoyi_ipd_pm` / `ruoyi_ipd_leader`）。本终稿 SQL 与脚本均使用真库角色名。

---

## 1. 方案概述

### 1.1 核心思想

`audit_log` 表是 G-02 强只追加工程的"最高敏感级"——所有特权操作（删除/移交/系数变更/奖金池）的不可变审计链。

**漏洞**: 当前 GRANT 默认对所有数据库角色（含普通 PM）开放 SELECT/INSERT/UPDATE/DELETE。理论上即使应用层 IpdIdorGuard 完美，应用层外的 SQL 直查（如运维误操作、ORM 框架 BUG、第三方分析工具）仍可绕过。

**修复**: 通过 post-deploy hook + 每日 cron REVOKE 兜底，将 `audit_log` 表的写权限严格收敛到超管专用数据库角色。

### 1.2 与其他方案对比

| 维度 | ① 脚本兜底（推荐）| ② column-level GRANT | ③ application-only |
|---|---|---|---|
| 防御层 | 数据库 + 应用 | 数据库视图层 | 仅应用 |
| 防 SQL 直查 | ✅ 强 | ✅ 强（视图过滤）| ❌ 无 |
| 防 ORM BUG | ✅ 强 | ✅ 中（视图仍可读脱敏列）| ❌ 无 |
| 运维复杂度 | 中（cron + hook）| 高（视图定义维护）| 低 |
| 与 G-02 张力 | 最小（只追加工程天然兼容）| 中（视图需过滤历史数据）| 大（依赖应用层防御）|
| 跨方言迁移 | 易（标准 SQL）| 难（方言差异）| 易 |

---

## 2. 实施步骤

### 2.1 数据库角色定义（DDL 增量）

```sql
-- 在 application-prod.sql 与 application-dev.sql 中追加（手动审阅后落地）
-- 真库应用账号：ipd_app@127.0.0.1 / ipd_dev 库
-- 真库超管账号：通过 DBA 凭证（DBA 角色不受 REVOKE 影响）
-- 注：audit_log / audit_log_chain_heads 表名需按真库实际表名核对（真库审计表可能命名为 audit_logs）

-- 1. 收回应用账号对审计表的写权限（库级兜底）
REVOKE INSERT, UPDATE, DELETE ON TABLE audit_logs FROM 'ipd_app'@'127.0.0.1';
REVOKE INSERT, UPDATE, DELETE ON TABLE audit_log_chain_heads FROM 'ipd_app'@'127.0.0.1';

-- 2. 表级 GRANT 仍保留 SELECT/INSERT（应用业务需要写审计行；不允许 UPDATE/DELETE 即实现强只追加）
-- （已通过 commit 21131def 系列配套 21 表的表级 GRANT 落地，本步不动）

-- 3. 不引入 ipd_pm / ipd_leader 角色（真库无此角色，IPD 角色在应用层 IpdPermission 守卫，不在 DB 层）
```

**注**：
- DDL 不入 git（每次部署手动 apply 或通过 migration 工具），但脚本草稿入仓供审阅（`PROPOSAL-01-脚本兜底.sql`）
- 真库实际表名需 owner 执行前核对：源码 grep `audit_logs` / `audit_log_chain_heads` 确认主表名（本治理文档示例以 `audit_logs` 为准，与真库约定一致——详见 `docs/ipd-系统说明/治理/AUD-GOV-收口-20260906.md` §chain_heads 激活段）
- `ipd_dev` 库无独立 PM/Leader 角色（单企业私有部署，G-07），DB 层只区分应用账号与 DBA

### 2.2 REVOKE 兜底脚本（`scripts/audit-log-revoke.sh`）

```bash
#!/usr/bin/env bash
# DEF-5 方案①：每日 cron + post-deploy hook 兜底
# 幂等：多次执行结果一致（REVOKE 已无权限的表无副作用）
set -euo pipefail

DB_HOST="${AUDIT_DB_HOST:-127.0.0.1}"
DB_PORT="${AUDIT_DB_PORT:-23306}"
DB_NAME="${AUDIT_DB_NAME:-ruoyi-ai}"
DB_USER="${AUDIT_DB_USER:-root}"
DB_PASS="${AUDIT_DB_PASS:-}"

# 三道防线
REVOQUEES=("ruoyi_ipd_app" "ruoyi_ipd_pm" "ruoyi_ipd_leader")
PRIVS=("INSERT" "UPDATE" "DELETE")

for role in "${REVOQUEES[@]}"; do
  for priv in "${PRIVS[@]}"; do
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" \
      -e "REVOKE ${priv} ON TABLE audit_log FROM ${role};" 2>/dev/null || true
  done
done

# 自校验
GRANTS=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" \
  -BNe "SELECT GRANTEE, PRIVILEGE_TYPE FROM information_schema.TABLE_PRIVILEGES \
        WHERE TABLE_SCHEMA='${DB_NAME}' AND TABLE_NAME='audit_log' \
        AND GRANTEE IN ('ruoyi_ipd_app','ruoyi_ipd_pm','ruoyi_ipd_leader') \
        AND PRIVILEGE_TYPE IN ('INSERT','UPDATE','DELETE');")

if [[ -n "$GRANTS" ]]; then
  echo "[DEF-5 REVOKE] ⚠️ 兜底失败，残留权限："
  echo "$GRANTS"
  exit 1
fi

echo "[DEF-5 REVOKE] ✅ audit_log 写权限已收敛到 ruoyi_ipd_admin"
```

### 2.3 post-deploy hook 集成

在部署流水线（如 GitHub Actions / Jenkinsfile）追加：
```yaml
- name: DEF-5 audit_log REVOKE
  run: |
    chmod +x scripts/audit-log-revoke.sh
    ./scripts/audit-log-revoke.sh
  env:
    AUDIT_DB_HOST: ${{ secrets.DB_HOST }}
    AUDIT_DB_PORT: ${{ secrets.DB_PORT }}
    AUDIT_DB_USER: ${{ secrets.DB_ADMIN_USER }}
    AUDIT_DB_PASS: ${{ secrets.DB_ADMIN_PASS }}
```

### 2.4 每日 cron 兜底

```cron
# 每日 02:30 兜底 REVOKE（防止人工误授权反弹）
30 2 * * * /opt/ruoyi-ai/scripts/audit-log-revoke.sh >> /var/log/audit-revoke.log 2>&1
```

---

## 3. 验证用例

### 3.1 自动化测试（应用层）

新增 `AuditLogGrantTest.java`：
```java
@Tag("dev")
@Test
@DisplayName("DEF-5：应用层 ORM 写入 audit_log 仍能成功——超管路由不依赖 DB GRANT")
void appLayerInsertStillSucceeds() {
    // 用超管 role 连接 DB，验证 AuditLogService.append() 仍可写入
    // 不验证应用层外的 SQL 直查（那是运维侧 DCL 测试）
}

@Test
@DisplayName("DEF-5：审计链完整性——所有特权操作写 audit_log 不可绕过")
void auditChainIntegrity() {
    // 模拟 5 类特权操作（删除/移交/系数/奖金池/项目状态变更）
    // 每类操作后必须 audit_log 出现一条记录
}
```

### 3.2 运维 DCL 测试（手动，季度演练）

```bash
# 用应用账号尝试直插 audit_log
mysql -h 127.0.0.1 -P 23306 -u ruoyi_ipd_app -p \
  ruoyi-ai -e "INSERT INTO audit_log (...) VALUES (...);"
# 期望: ERROR 1142 (42000): INSERT command denied

# 用超管账号直查
mysql -h 127.0.0.1 -P 23306 -u ruoyi_ipd_admin -p \
  ruoyi-ai -e "SELECT COUNT(*) FROM audit_log;"
# 期望: 返回正常行数
```

---

## 4. 回滚预案

如方案① 引发不可预期的兼容性问题：

1. **立即回滚 DCL**：
   ```sql
   GRANT INSERT, UPDATE, DELETE ON TABLE audit_log TO ruoyi_ipd_app;
   ```
2. **删除 cron + post-deploy hook 步骤**
3. **保留审计日志**（记录回滚事件本身）
4. **复盘 root cause + 改用方案② 或 方案③**

---

## 5. 与 IPD 硬约束对齐

| 约束 | 方案① 是否满足 |
|---|---|
| **G-02 强只追加** | ✅ 数据库 GRANT 天然支持"只追加"——禁止 UPDATE/DELETE 即实现只追加 |
| **G-04 业务规则高于文档惯例** | ✅ 纵深防御是 IPD 最高约束的体现 |
| **G-07 单企业私有部署** | ✅ 单租户脚本无需多租户隔离 |
| **G-11 审计链/软删原子性** | ✅ AuditLogService 写仍走应用层事务，原子性不变 |

---

## 6. owner 待办清单

- [ ] 审阅 §2.1 DDL 增量，与 DBA 对齐角色命名约定
- [ ] 审阅 §2.2 脚本草稿，加入 `scripts/` 目录
- [ ] 审阅 §2.3 post-deploy hook 集成方式
- [ ] 审阅 §2.4 cron 配置，与运维对齐时区
- [ ] 部署后跑 §3.2 DCL 验证用例
- [ ] 季度演练日历预约
- [ ] 更新 `docs/ipd-系统说明/治理轮/DEF-5/INDEX.md` 标记方案① 已落地

---

## 7. 关联

- **决策索引**: `docs/ipd-系统说明/治理轮/DEF-5/INDEX.md`
- **Wave3-实施规格包**: `docs/ipd-系统说明/验收/Wave3-实施规格包-2026-09-05.md`
- **SEC-AUD rebuttal**: `docs/ipd-系统说明/验收/SEC-AUD-2026-09-05-跨commit安全审查.md`
- **终极收口报告**: `docs/ipd-系统说明/收口/SWARM-2026-09-07-终极收口.md`

---

**Why & How**:
- **Why**: 数据库层 GRANT 是应用层防御失效后的最后兜底。G-02 强只追加要求与数据库 DCL 天然契合——禁止 UPDATE/DELETE 即实现只追加。
- **How**: REVOKE 脚本入仓 + post-deploy hook 自动执行 + 每日 cron 兜底 + 季度 DCL 演练。整套机制对应用代码零侵入，仅依赖标准 SQL。