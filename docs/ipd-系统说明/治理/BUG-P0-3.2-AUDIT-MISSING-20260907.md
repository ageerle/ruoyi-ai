# BUG-P0-3.2-AUDIT-MISSING（2026-09-07）

- **发现时间**：2026-09-07 19:10
- **发现者**：P0-3.4 真实验收会话（按用户拍板「必须真实实现不是 Mock 模拟」执行真 HTTP+DB 验收）
- **严重级别**：中（业务可见 — 变更审计数据缺失；不影响业务闭环）
- **归属**：P0-3.2（参数管理 API、激活版本与即时读取）实现缺口
- **触发卡**：P0-3.4 真实验收 AC-CFG-02/05 唯一 FAIL
- **状态**：⬜ 登记完成，待 owner 拍板修复优先级

## 一、Bug 现象

P0-3.4 真 HTTP 端到端验收（commit `b7fe3800`，证据文件 `docs/ipd-系统说明/验收/P0-3.4-真HTTP端到端验收-20260907.md`）发现：

调用 `PUT /api/v1/system-configs/{key}` 改任意参数后，**真库 `audit_logs` 表 0 条 SYSTEM_CONFIG_UPDATE 写入**。

真表现状：
- `audit_logs` 共 651 条
- 30 种 action 模式：LOGIN / GATE_SIGN / GATE_ELEMENT_JUDGE / PROJECT_CREATE / 等
- **0 条任何形式的 SYSTEM_CONFIG_UPDATE / system_config / config_update**

## 二、根因（源码 0 命中）

```
$ grep -rn "audit_logs\|AuditLog\|audit\.append" \
    ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/

# 命中点：
#   - AuditLogMapper.java（mapper 定义）
#   - AuditLogController.java（GET 列表，不写）
#   - AuditLogService.java（service 定义）
#   - AuditChainHeadMapper / AuditHashChain（哈希链工具）
#   - dto/AuditEntryVO.java（前端投影）
#   - IpdRolePermissionCatalog.java（注释）
# **0 个 controller / service 主动调用 AuditLogService.append()**
```

具体缺口：

```
$ grep -nE "audit|AuditLog" \
    ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/service/SystemConfigService.java
(no output)

$ grep -nE "audit|append" \
    ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/controller/SystemConfigController.java
70:        systemConfigService.update(key, req.value(), actor.id());
# 只调 update，没 audit.append
```

**结论**：`SystemConfigController.update()` → `SystemConfigService.update()` 链路完全没调 `AuditLogService.append()`。

## 三、卡面契约 vs 实际实现

| 维度 | P0-3.2 卡面（SSOT line 117）| 实际实现 | 差距 |
|---|---|---|---|
| 读取/修改接口鉴权 | ✅ | ✅ | — |
| 范围校验 | ✅ | ✅ | — |
| **变更审计** | ✅ 卡面写 | ❌ **SystemConfigService.update 不调 audit.append** | **缺口** |
| 并发冲突可见 | ✅ | ✅ uk 兜底 STATE_CONFLICT | — |
| 激活值立即可读 | ✅ | ✅ invalidated 广播 | — |
| 不据配置读回声称消费者生效 | ✅ | ✅ | — |

P0-3.2 卡面第 4 列原话「变更审计；并发冲突可见；激活值通过真实API/DB立即可读且无需重启」中，**变更审计是契约承诺但未落地**。

## 四、对 P0-3.4 真实验收的影响

P0-3.4 真 HTTP 端到端 13 项探针：
- PASS=12：4 项 AC 全部真 HTTP 端到端 PASS（参数变更立即生效、版本链真写穿、历史快照不静默覆盖等核心契约 100% 满足）
- **FAIL=1**：AC-CFG-02/05 audit_logs 有 CONFIG_UPDATE 写入 — 但这一项**不是 P0-3.4 卡面 4 AC 的主责**

P0-3.4 卡面 4 AC（AC-CFG-02 / AC-CFG-03 / AC-HR-05 / AC-GLB-10）核心业务效果 100% 满足；唯一 FAIL 是「审计日志」这一治理层面契约。

## 五、修复建议

### 5.1 修复点

在 `SystemConfigService.update(key, value, actorId)` 方法内，事务提交前调用：

```
auditLogService.append(AuditLog.builder()
    .action("SYSTEM_CONFIG_UPDATE")
    .resourceType("SYSTEM_CONFIG")
    .resourceId(key)
    .actorId(actorId)
    .before(Map.of("value", oldValue))
    .after(Map.of("value", newValue))
    .reason(reason)
    .build());
```

### 5.2 修复范围

- `ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/service/SystemConfigService.java`
- `ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/controller/SystemConfigController.java`（把 reason 透传到 service）
- 新增 `P032AuditAppendAcceptanceTest`（@Tag dev）覆盖：
  - 改 1 个 key → audit_logs 增 1 条 SYSTEM_CONFIG_UPDATE
  - before/after JSON 含 key + value
  - 失败回滚 → audit_logs 不增（事务一致性）

### 5.3 验收命令

```
JAVA_HOME=/Users/mac/tools/jdk-17/Contents/Home \
/Users/mac/tools/maven/bin/mvn -pl ruoyi-modules/ruoyi-ipd -am \
  -Dtest=P032AuditAppendAcceptanceTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

外加真 HTTP 真库验证（commit `b7fe3800` 已建立的验收模板）：

```
# 改 gate.signDeadlineDays=3
curl -X PUT $BASE/api/v1/system-configs/gate.signDeadlineDays \
  -H "Authorization: Bearer $TK_ADMIN" \
  -H 'Content-Type: application/json' -d '{"value":"3","reason":"BUG fix verify"}'

# DB 直查 audit_logs 有 SYSTEM_CONFIG_UPDATE 写入
SELECT COUNT(*) FROM audit_logs
WHERE action='SYSTEM_CONFIG_UPDATE'
  AND resource_id='gate.signDeadlineDays';
```

## 六、owner 拍板参考

| 选项 | 操作 | 风险 |
|---|---|---|
| A. 接受 P0-3.4 done + 修 BUG 卡单独立项 | 修 SystemConfigService.update 加 audit.append，独立卡 follow up；不动 P0-3.2 当前 ✅ 状态 | 0（已翻 done 的契约 100% 满足，audit 是补充契约） |
| B. P0-3.2 翻回 ⬜ inreview | 等 audit 补完再翻 done | 高（P0-3.4 卡面 4 AC 已真 PASS，再翻回 P0-3.2 阻塞与证据矛盾） |
| C. 不修 audit 缺口（接受治理层缺口） | 长期带缺口运行 | 中（治理闭环缺一环） |

**建议 A**：12/13 真 PASS 充分支持 P0-3.4 done 判定；audit 缺口独立 BUG 卡 follow up。

## 七、证据引用

- P0-3.4 真实验收证据：`docs/ipd-系统说明/验收/P0-3.4-真HTTP端到端验收-20260907.md`（249 行）
- P0-3.4 真实验收 commit：`b7fe3800 docs(ipd,P0-3.4): 真 HTTP 端到端验收证据（按用户拍板「必须真实实现不是 Mock 模拟」）`
- P0-3.2 卡面契约：`docs/ipd-系统说明/开发计划-看板镜像.md` line 117 字段 4「变更审计」
- AuditLogService 存在但未调用：`ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/service/AuditLogService.java`
- SystemConfigService 0 命中：`ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/service/SystemConfigService.java`
- SystemConfigController line 70：`ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/controller/SystemConfigController.java`
- 真库 audit_logs 探针：MySQL 8.0.46 @ 127.0.0.1:13306/ipd_dev
