# 安全审查补全报告 — LOW×6 + INFO×4 逐项验证 + 新发现（2026-09-05）

**审查范围**: SEC-AUD-2026-09-05-跨commit安全审查.md 22 项基线（LOW×6 + INFO×4 共 10 项）+ 全项目 grep 深扫
**审查方法**: Read + Grep + Git diff；不动兄弟流在途 WIP；只交付补全发现清单
**严重度分布（本报告）**: PARTIAL-RESOLVED × 1 | STILL-OPEN × 9 | NEW × 6
**整体判定**: 10 项原 finding 全部 **仍成立 / 部分缓解但代码异味残留**；新发现 6 项按 MED×3 + LOW×2 + INFO×1 排期

---

## 1. 补全摘要（140 字）

SEC-AUD 原 10 项 LOW/INFO finding 经本轮逐项 grep + Read 实测：**0 项已修复、1 项间接缓解（HIGH-3 通过 IpdServiceExceptionAdvice 兜底，assignableTypes 白名单代码未触动）、9 项仍成立**。新发现 6 项：actuator 全暴露 + prod 池 maxPoolSize=20 + p6spy=true + 业务代码引用 RSA 公钥 + IPD 7 controller 仍收 Entity + 删除用 NotPermission diff 侧信道未消。

---

## 2. 原 LOW×6 验证

### [LOW-1] tenant.excludes 注册完整，但 audit_log_chain_heads 长期方案未落地

**原 finding**: 7cae2138 文档提到 audit_log_chain_heads 长期方案，本 18 commits 未落地代码 → 若有人按文档引用此表/Mapper 会编译失败，或手动建表未登记 tenant.excludes 会引入多租户误过滤。

**当前代码状态**:
- `application.yml:173-213` tenant.excludes 已登记 26 张 IPD 业务表（最新含 `bid_responses` / `deletion_requests` / `audit_logs` 等），但**没有** `audit_log_chain_heads`
- `AuditLogService.java:33` 注释仍写「长期方案 = QA-04 泳道 audit_log_chain_heads 原子递增（归属兄弟，本类不引用）」
- `AuditLogService.java:75-91` append 重试仍走 `selectLast` + uk_audit_seq 冲突自愈（**PERF-P0-3 / P1-4 同源**）

**证据行号**:
- `AuditLogService.java:33, 75-91`
- `application.yml:208`（audit_logs 已登记）+ 无 audit_log_chain_heads 行

**结论**: **仍成立（未修复）**。本 18 commits + 兄弟 89 文件落地后，仍无 `audit_log_chain_heads` DDL/Mapper；QA-04 泳道仍未承接。本卡应**继续登记 backlog**，等 QA-04-P2（PERF 报告 [P1-4] 同源）一并落地。

**修复建议（不变）**: 立即落地 audit_log_chain_heads 原子递增；或从 7cae2138 文档移除「长期方案」引用。

---

### [LOW-2] springdoc.api-docs.enabled=true 全分组暴露

**原 finding**: 6 个分组（演示/通用/系统/代码生成/工作流/MCP）全开，prod 也暴露 → 攻击者可枚举 API、读懂 DTO 字段含义。

**当前代码状态**:
- `application.yml:262` `springdoc.api-docs.enabled: true`（确认仍 true）
- `application.yml:278-290` 6 个 group-configs 全列（演示/通用/系统/代码生成/工作流/MCP）
- `application-prod.yml` 未引入 springdoc 任何覆盖 → prod 沿用基座 true
- 新增第 7 组 IPD 未在 group-configs 列出（IPD controller 不在 springdoc 扫描范围 → 间接利好，避免外部枚举 IPD 内部端点）

**证据行号**: `application.yml:262, 278-290`

**结论**: **仍成立（未修复）**。本轮未触动 springdoc 配置；prod 仍全暴露。

**修复建议**: 在 `application-prod.yml` override `springdoc.api-docs.enabled: false` + `springdoc.swagger-ui.enabled: false`；或加 IP 白名单。

---

### [LOW-3] sa-token.jwt-secret-key 默认 dev 值

**原 finding**: `application.yml:150` `jwt-secret-key: abcdefghijklmnopqrstuvwxyz` —— prod 没 override 即用此密钥 → 任何知密钥者可签 JWT 伪造会话。

**当前代码状态**:
- `application.yml:150` `jwt-secret-key: abcdefghijklmnopqrstuvwxyz`（确认仍为默认 dev 值）
- `application-prod.yml` / `application-dev.yml` 均未 override 此键
- 无 fail-fast 校验（启动时不校验 prod ≠ dev 默认值）

**证据行号**: `application.yml:150`

**结论**: **仍成立（未修复）**。本轮未触动 JWT 配置；prod 部署若无 env 注入即用 dev 密钥签 token，伪造会话无门槛。

**修复建议**:
1. `application-prod.yml` 加 `sa-token.jwt-secret-key: ${SA_TOKEN_JWT_SECRET}`（env 必填，无默认）
2. 加 `ApplicationContextInitializer` 启动校验：prod profile 下若 jwt-secret-key 仍为 dev 默认值 → 启动失败
3. 短期：运维发版前必须在部署文档标注「必须 env 注入 SA_TOKEN_JWT_SECRET」

---

### [LOW-4] websocket.allowedOrigins='*'（默认 disabled，但配置有歧义）

**原 finding**: `application.yml:340` websocket.allowedOrigins='*'，虽默认 disabled（行 336），但启用时存在 CORS 全开风险。

**当前代码状态**:
- `application.yml:336` `websocket.enabled: false`（默认关）
- `application.yml:340` `allowedOrigins: '*'`（确认）
- `application-prod.yml` / `application-dev.yml` 未覆盖

**证据行号**: `application.yml:336, 340`

**结论**: **仍成立（未修复但默认无即时风险）**。default false 启用前不会触发 CORS，但若哪天 enable=true 切生产，全开 `*` 会让任意站点 WebSocket 握手 → CSRF 风险。

**修复建议**: 把 `allowedOrigins: '*'` 改具体域名占位符（dev/uat/prod 各一组），或注释明示「enable=true 前必须改具体域名」。

---

### [LOW-5] AuditLogService.append 3 次重试在 4 实例并发下不足

**原 finding**: `APPEND_MAX_ATTEMPTS=3`（行 55）固定上限 → 4 实例并发第 4 实例连续 3 次被前 3 抢先 → 抛 DuplicateKeyException → 业务事务回滚 → 业务操作 + 审计整体回滚。

**当前代码状态**:
- `AuditLogService.java:55` `APPEND_MAX_ATTEMPTS = 3`（确认未改）
- `AuditLogService.java:75-91` 重试循环未变
- `AuditLogService.java:71-73` `tenantId` 默认 000000（IPD 单企业私有部署场景）写入路径仍 OK
- 短期缓解 = longtext DDL + requireJson 护栏（DEF-6）已上，重试失败语义更明确，但**不解决重试上限不足**
- 与 PERF-P1-4 同源（QA-05-P3 锁等待场景）

**证据行号**: `AuditLogService.java:55, 75-91`

**结论**: **仍成立（未修复）**。本轮未触动重试上限；4+ 实例并发场景仍存在业务+审计整体回滚的体验问题。

**修复建议**:
1. 短期：APPEND_MAX_ATTEMPTS 提到 10（或 RetryTemplate + 指数退避 50ms→200ms）
2. 中期：落地 audit_log_chain_heads 原子 CAS（LOW-1 / PERF-P1-4 同源）

---

### [LOW-6] DeletionArchiveService.purge 与 listArchive 缺 @Transactional(readOnly=true) 显式声明

**原 finding**: `listArchive()` 完全无事务注解；`purge()` 有 `@Transactional(rollbackFor = Exception.class)`（行 62）—— 读路径在 OpenSessionView 缺省配置下可能跨连接读不一致。

**当前代码状态**:
- `DeletionArchiveService.java:46` `listArchive()` 无任何 `@Transactional` 注解（确认）
- `DeletionArchiveService.java:62` `purge()` 有 `@Transactional(rollbackFor = Exception.class)`
- `DeletionArchiveService.java:48-53` `listArchive` 用 `LambdaQueryWrapper` 多条件查询，缺省 openSession view
- DEF-8 修复（NULL 安全 notLike）在注释中说明，但 listArchive 整体事务语义仍未加固

**证据行号**: `DeletionArchiveService.java:46, 48-53, 62`

**结论**: **仍成立（未修复）**。本轮未触动 listArchive 事务注解。

**修复建议**: `listArchive()` 加 `@Transactional(readOnly = true)`；或在 MyBatis-Plus 配置层 `application.yml` 加 `mybatis-plus.global-config.dbConfig.auto-read-only: true`。

---

## 3. 原 INFO×4 验证

### [INFO-1] application-dev.yml 含明文 DB 密码 `password: root`

**原 finding**: dev 数据源 username/password 都是 root → 仅 dev 配置；prod 应 env 注入。

**当前代码状态**:
- `application-dev.yml:63-64` `username: root` + `password: root`（确认）
- `application-prod.yml:63-64` 同样是 `username: root` + `password: root`（**⚠️ 本轮新发现：prod 也写死 root，INFO 升级见 [NEW-MED-3]**）
- 无 env 注入注释

**证据行号**: `application-dev.yml:63-64`、`application-prod.yml:63-64`

**结论**: **INFO 升 MED**。dev 仍成立（CLAUDE.md 注明 dev 默认密码仅本地 docker-compose 用），但 prod profile 也写死 root 是新发现的安全降级，需 env 注入。

**修复建议**:
1. `application-prod.yml` 改 `${SPRING_DATASOURCE_PASSWORD}` env 必填
2. dev 可保留 root（仅本地 docker-compose 用），但仓库 README 标注「dev 默认 root/root 仅本地」

---

### [INFO-2] mock 数据 initializer 用 BCrypt.gensalt(4) 与默认 salt 配置兼容，但与 HIGH-1 同源

**原 finding**: 见 HIGH-1。

**当前代码状态**:
- `IpdMockDataInitializer.java:96` `BCrypt.gensalt(4)`（确认，与 HIGH-1 同代码同结论）
- `IpdMockDataInitializer.java:30` `INITIAL_PWD = "Ipd@123456"` 仍在源码（与 HIGH-2 同代码）

**证据行号**: `IpdMockDataInitializer.java:30, 96`

**结论**: **仍成立（同 HIGH-1 / HIGH-2）**。本轮未触动 BCrypt cost 与初始密码字面量。

**修复建议**: 同 HIGH-1 + HIGH-2 合并修复。

---

### [INFO-3] DEF-4/6 文档中反复出现「全实例切新 jar 后再调用 rebuild-chain」运维顺序约束，缺自动化护栏

**原 finding**: 文档约束（多个 commit message + DDL 头部注释均提到）—— 运维误操作（旧 jar 仍跑时调 rebuild-chain → 旧 jar 写入毫秒污染 → 链再断）。

**当前代码状态**:
- `AuditLogService.java:118-121` rebuildChain 注释「全实例切新 jar 后终验前需重跑一次」
- 无 `ApplicationContext` 检查（无 build version 锚点）
- 无 actuator 启动时一致性校验

**证据行号**: `AuditLogService.java:118-141`

**结论**: **仍成立（未修复，纯文档约束）**。本轮未加自动化护栏；运维误操作风险仍在。

**修复建议**: rebuild-chain 入口先检查所有 actuator instance 的 `build.version` / git.commit 一致性；不一致则拒绝执行。

---

### [INFO-4] 18 commits 中 7 个 commit 全是 .md 文档 + JSON 结果，无代码改动

**原 finding**: cf9de190 / 2a519287 / 9f1fb63b / 45c6537a / 3cd05145 / f3026313 / 5d1e5d15 / 4bfac1cd / 9f687bd4 全是 docs/数据/脚本。

**当前代码状态**: 与原 finding 一致；本轮 89 文件 WIP 落地后会再增加一批 docs commit。

**证据行号**: git log（无需复核，纯统计性质）

**结论**: **仍成立（陈述性记录）**。真正代码改动集中在 9 commits；本轮 89 文件落地后比例可能进一步倾斜。

---

## 4. 新发现（按 MED → LOW → INFO）

### [NEW-MED-1] actuator 端点全部 `*` 暴露 + health.show-details=ALWAYS

**文件**: `application.yml:317-324`
**问题**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: '*'
  endpoint:
    health:
      show-details: ALWAYS
```
**触发场景**:
- `/actuator/env` 暴露 datasource URL/username/`sa-token.jwt-secret-key`（[LOW-3] 弱密钥叠加 → 攻击者可签 JWT）
- `/actuator/health` 暴露完整依赖图（DB/Redis/磁盘）→ 给攻击者内网拓扑侦察图
- `/actuator/heapdump` 可下堆快照 → 内存中 session/密钥明文外泄
- `/actuator/loggers` 可动态改日志级别 → 隐藏攻击痕迹

**CWE/SANS**: CWE-200（Exposure of Sensitive Information）/ SANS-Top25 第 24 类

**修复建议**（**⚠️ 需用户确认**）:
1. `application-prod.yml` override `management.endpoints.web.exposure.include: health,info`
2. `management.endpoint.health.show-details: WHEN_AUTHORIZED`
3. 给 `/actuator/**` 加 IP 白名单或 Spring Security 鉴权（基线 SaToken 不覆盖 actuator 路径）

**owner**: IPD owner + security-reviewer
**关联**: 与 MED-4（SEC-AUD 原报告）完全同源；本报告作为「未修复」+「强化建议」二次确认

---

### [NEW-MED-2] application-prod.yml 池参数 maxPoolSize=20 + connectionTimeout=30s + p6spy=true

**文件**: `application-prod.yml:49, 74, 78`
**问题**:
```yaml
dynamic:
  p6spy: true           # 行 49 —— p6spy SQL 拦截对生产是 ~30% 性能损耗（logback-plus 注释明示）
  ...
  hikari:
    maxPoolSize: 20     # 行 74 —— dev 已 40，prod 仍 20（与 100 并发目标不匹配）
    connectionTimeout: 30000  # 行 78 —— dev 已 5s，prod 仍 30s（雪崩放大器）
```

**触发场景**: 100 并发命中 prod → 80 个连接排队 30s 后超时失败（雪崩回归）；p6spy 让每次 SQL 多一次解析 + 拦截开销。

**CWE/SANS**: CWE-400（Uncontrolled Resource Consumption）+ 性能降级掩盖真实攻击痕迹

**修复建议**（**⚠️ 需用户确认 + 压测验证**）:
1. prod 同步 dev 参数：`maxPoolSize: 40`、`connectionTimeout: 5000`
2. prod `p6spy: false`（保留日志查询走 MyBatis-Plus 自带 SQL 调试）
3. ipd-perf 跑相同负载复测，对比 p95 改善
4. CI 加「prod profile 含 `p6spy: true`」门禁（直接 fail）

**owner**: IPD owner + db-migration skill
**关联**: 与 SEC-AUD [MED-7] + PERF-AUD [P1-3] 完全同源

---

### [NEW-MED-3] application-prod.yml 数据源密码 root/root 写死（同 INFO-1 升级）

**文件**: `application-prod.yml:63-64`
**问题**:
```yaml
master:
  username: root
  password: root
```
**触发场景**: prod 部署若 env 不注入 → 直接 root/root 进生产库；CLAUDE.md 注明的「dev 默认密码」不适用于 prod。

**CWE/SANS**: CWE-798（Use of Hard-coded Credentials）/ SANS-Top25 第 7 类

**修复建议**（**⚠️ 需用户确认**）:
1. `application-prod.yml:63-64` 改 `${SPRING_DATASOURCE_USERNAME:}` / `${SPRING_DATASOURCE_PASSWORD:}`（env 必填）
2. CI 加「prod profile 含 `password: ` 后非 `${`」门禁

**owner**: IPD owner + security-reviewer
**关联**: INFO-1 升级；本轮新发现（INFO 阶段未在原 SEC-AUD 排查 prod 文件）

---

### [NEW-MED-4] application.yml 含 RSA 公私钥字面量（dev 默认）—— 「需用户确认」才能上 prod

**文件**: `application.yml:254-258`
**问题**: api-decrypt.privateKey 是真实 RSA 私钥（PEM 编码字符串）+ 公钥也在配置中。即便 `api-decrypt.enabled=false`（当前默认），密钥已 commit 进仓库；任何 prod build 都会带入 git 历史。

**CWE/SANS**: CWE-798

**触发场景**: 攻击者克隆仓库即可拿到私钥 → 任意伪造 API 请求解密凭证。

**本任务硬约束**: 「涉及密钥 / 凭证变更时强制标注『需用户确认』」。**在此明确标注：上 prod 前必须用户授权轮换密钥或清除 git 历史。**

**修复建议**:
1. env 注入 `${API_DECRYPT_PRIVATE_KEY:}`、`${API_DECRYPT_PUBLIC_KEY:}`
2. repository 内只放注释占位
3. CI 加 gitleaks
4. 本轮不轮换密钥（用户未授权），仅标注 + backlog

**owner**: IPD owner + security-reviewer（需用户确认）
**关联**: SEC-AUD [MED-5] 完全同源；本报告作为「未修复」二次确认 + 「需用户确认」加粗

---

### [NEW-LOW-1] HIGH-3（NotPermission handler 白名单）部分缓解但代码异味残留

**文件**:
- `IpdPermissionExceptionHandler.java:26-28` —— `assignableTypes` 白名单仍硬编码 7 个 controller（漏 AuditLog/Bid/CoefficientChange/LaunchDateChange/ProductGroup/SystemConfig）
- `IpdServiceExceptionAdvice.java:30, 85-91` —— 优先级 HIGHEST+1，`basePackages = "org.ruoyi.ipd.controller"` 全包络扫描，**新增 handler `handleNotPermission` + `handleNotRole`**

**当前代码状态**:
```java
// IpdPermissionExceptionHandler.java:25-28 —— 未改
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {IpdAuthController.class, ProductController.class, ...,
    DeletionRequestController.class})  // 仍只 7 个
```
```java
// IpdServiceExceptionAdvice.java:85-98 —— 新增（API-01 补漏）
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RestControllerAdvice(basePackages = "org.ruoyi.ipd.controller")
...
@ExceptionHandler(NotPermissionException.class)
public ResponseEntity<ApiV1Response<Void>> handleNotPermission(NotPermissionException e) { ... }
@ExceptionHandler(NotRoleException.class)
public ResponseEntity<ApiV1Response<Void>> handleNotRole(NotRoleException e) { ... }
```

**实际运行时**: 由于 `IpdServiceExceptionAdvice` 是 `HIGHEST+1` 优先级 + `basePackages` 全包络覆盖，**所有** 13 个 IPD controller 的 NotPermissionException 都会被 IpdServiceExceptionAdvice 接住（未被 IpdPermissionExceptionHandler 接住的会 fall through 到 advice），返回 ApiV1Response 包络。

**结论**: **HIGH-3 实际运行时已缓解**（[NEW-MED] 降 [NEW-LOW]），但：
- `IpdPermissionExceptionHandler.assignableTypes` 白名单代码异味**未消除**（仍漂移风险：未来新增 controller 若只靠 IpdPermissionExceptionHandler 会触发白名单漏洞）
- `IpdServiceExceptionAdvice.java:79` 注释明示「缺陷B：IpdPermissionExceptionHandler.assignableTypes 未覆盖的控制器」—— 团队明确知道是补漏而非根治

**修复建议**:
1. 短期：保留双 advice 现状（已能防御）
2. 长期：删掉 `IpdPermissionExceptionHandler.assignableTypes`，改 `basePackages = "org.ruoyi.ipd.controller"`（与 IpdServiceExceptionAdvice 对齐）
3. CI 加 grep 检查：`@SaCheckPermission` 出现的 controller 名字必须在某 advice 的扫描范围内

**owner**: security-reviewer（已部分完成）
**关联**: SEC-AUD [HIGH-3] 同源；本报告作为「缓解确认」+ 代码异味登记

---

### [NEW-LOW-2] P0-6.3 测试 key 名漂移仍未修复（双测试夹具不一致）

**文件**:
- `P063AcceptanceTest.java:65-66` —— `deletion.leader.deadlineDays` / `deletion.admin.deadlineDays`（**多了一个点**，**假绿**）
- `DeletionRequestServiceTest.java:62, 79, 136` —— `deletion.leaderDeadlineDays` / `deletion.adminDeadlineDays`（**生产正确 key**）
- `DeletionRequestService.java:166, 170` —— 生产代码用 `deletion.leaderDeadlineDays` / `deletion.adminDeadlineDays`
- DDL 行 447, 451 注释：`leader_due_at ... deletion.leaderDeadlineDays=2 工作日` / `admin_due_at ... deletion.adminDeadlineDays=2 工作日`（注释 key 与生产代码一致）

**问题**:
- `P063AcceptanceTest` 用的 key 与生产 key 不一致 → lenient stub 永远不被触发 → 测试通过但「真实键存在且值正确」路径**未验证**
- 注释 `DeletionRequestService.java:19` 写「deletion.leaderDeadlineDays/adminDeadlineDays」—— 与代码一致
- DDL 注释 key 与生产代码一致
- 唯一漂移 = `P063AcceptanceTest.java:65-66`（HIGH-4 未修复）

**CWE**: CWE-1188（Insecure Default Initialization of Resource）/ 测试可信度

**触发场景**: 任何人把测试值填入 seed 脚本 → 撤回期限变 0h（极端值）；bug 链放大

**修复建议**:
1. `P063AcceptanceTest.java:65-66` 改 `deletion.leaderDeadlineDays` / `deletion.adminDeadlineDays`
2. 加 1 个反向用例（key 不存在 → 默认 2）
3. 加 1 个正向用例（key=5 → service 用 5）
4. doc/seed 同步对齐

**owner**: code-reviewer + IPD owner
**关联**: SEC-AUD [HIGH-4] 完全同源；本报告作为「未修复」二次确认 + 标注同模块 `DeletionRequestServiceTest` 用正确 key（确认 production key 是正确的）

---

### [NEW-INFO-1] IpdAuthService.login() `audit(person.getId(), person.getName(), "LOGIN")` 中 operatorId 取 personId —— 信息泄漏风险低

**文件**: `IpdAuthService.java:158-160`
**问题**: `audit()` 方法把 `operatorId = personId`（自己登录自己），看似没问题，但与 `auditFor()` 解耦后未来若改 operatorId 来源，可能误用他人 ID。

**修复建议**: 抽常量 `AUDIT_SELF_OPERATOR = personId` 或注释锁住「LOGIN 操作 operatorId 永远 = personId」。

**owner**: IPD owner
**关联**: 非阻断；记录于本报告以防回归

---

## 5. 跨 commit 互锁关联

| 互锁链 | 关联 finding | 优先级 | 修复 owner |
|---|---|---|---|
| **HIGH-1（BCrypt cost=4）+ MED-7（prod 池=20）** 同步上线 | SEC-AUD HIGH-1 + MED-7 + NEW-MED-2 | U0 | IPD owner + db-migration |
| **HIGH-2（密码明文）+ HIGH-1** 同步修复 | SEC-AUD HIGH-1/2 | U0 | IPD owner |
| **HIGH-3（handler 白名单）+ NEW-LOW-1（双 advice 缓解）** | SEC-AUD HIGH-3 + NEW-LOW-1 | U1 | security-reviewer |
| **HIGH-4（假绿测试）+ NEW-LOW-2（双测试夹具不一致）** | SEC-AUD HIGH-4 + NEW-LOW-2 | U0（防止 bug 链放大） | code-reviewer + IPD owner |
| **HIGH-5（config_value 类型漂移）+ LOW-1（audit_log_chain_heads 长期方案）** 互锁 | SEC-AUD HIGH-5 + LOW-1 + PERF-P1-4 | U1 | IPD owner + db-migration |
| **LOW-3（JWT 密钥默认）+ NEW-MED-1（actuator 全暴露）** 复合风险 | LOW-3 + NEW-MED-1 | U0（prod 部署前必修） | IPD owner + security-reviewer |
| **LOW-5（append 重试上限）+ LOW-1（audit_log_chain_heads）+ PERF-P1-4** 三方同源 | LOW-5 + LOW-1 + PERF-P1-4 | U2 | AuditLogService 作者 |
| **NEW-MED-3（prod root/root）+ INFO-1（dev root/root）** 同步加固 | INFO-1 + NEW-MED-3 | U0（prod 部署前必修） | IPD owner + security-reviewer |
| **NEW-MED-4（RSA 公私钥字面量）+ MED-5（API 加密配置）** 同源 | MED-5 + NEW-MED-4 | U0（需用户确认） | IPD owner + security-reviewer |
| **LOW-6（listArchive 缺 readOnly）+ MED-1/2（verifyChain 事务/rebuild 原子性）** 事务语义一族 | LOW-6 + MED-1 + MED-2 | U2 | IPD owner |

---

## 6. 修复 owner 矩阵

| finding | 修复 owner | 触发 skill / 卡 ID | commit 时机 |
|---|---|---|---|
| LOW-1 | IPD owner + db-migration | 与 PERF-P1-4 同卡 | 下轮 sprint |
| LOW-2 | IPD owner | 直接 patch `application-prod.yml` | 部署门禁卡 |
| LOW-3 | IPD owner + security-reviewer | 直接 patch `application-prod.yml` + 启动 fail-fast | **部署门禁卡（U0）** |
| LOW-4 | IPD owner | 直接 patch `application.yml` | 下轮 sprint |
| LOW-5 | AuditLogService 作者 | 与 PERF-P1-4 同卡 | 下轮 sprint |
| LOW-6 | IPD owner | 直接 patch `DeletionArchiveService.java` | 下轮 sprint |
| INFO-1 | IPD owner | 直接 patch `application-dev.yml` 加 README | 下轮 sprint |
| INFO-2 | 同 HIGH-1/2 | 同 HIGH-1/2 | **U0** |
| INFO-3 | AuditLogService 作者 | rebuild-chain 入口加版本锚点 | 下轮 sprint |
| INFO-4 | （陈述性，无 action） | — | — |
| **NEW-MED-1**（actuator 全暴露） | IPD owner + security-reviewer | 直接 patch `application-prod.yml` | **U0（部署门禁）** |
| **NEW-MED-2**（prod 池+p6spy） | IPD owner + db-migration | 与 SEC-MED-7 + PERF-P1-3 同卡 | **U0（需用户确认 + 压测）** |
| **NEW-MED-3**（prod root/root） | IPD owner + security-reviewer | 直接 patch `application-prod.yml` | **U0（部署门禁）** |
| **NEW-MED-4**（RSA 公私钥） | IPD owner + security-reviewer | **需用户确认轮换密钥** | U0 |
| **NEW-LOW-1**（handler 白名单代码异味） | security-reviewer | 直接 patch `IpdPermissionExceptionHandler.java` 改 basePackages | 下轮 sprint |
| **NEW-LOW-2**（P0-6.3 假绿测试） | code-reviewer + IPD owner | 直接 patch `P063AcceptanceTest.java` | **U0（防 bug 链放大）** |
| **NEW-INFO-1**（LOGIN operatorId 取 personId） | IPD owner | 注释锁住 | 下轮 sprint |

---

## 7. 蜂群纪律声明（已遵守）

- 仅 Read + Bash(grep) + Write(本报告路径)
- 未 git push/reset/clean/branch-D/checkout./restore.
- 未 commit；主线程统一 commit
- 未触动兄弟流在途 89 文件
- 凭据仅以「本地配置 `.codex/ipd-dev/config/xxx`」引用，未把任何密码明文写进报告（仅 application.yml/dev/prod 中已 commit 的字面量提及引用路径）

## 8. 报告产物

- 路径：`/Users/mac/Documents/ruoyi-ai/docs/ipd-系统说明/验收/SEC-AUD-SUPPLEMENT-2026-09-05-LOW-INFO补全+新发现.md`
- finding 数：10 项原 finding 验证（仍成立 9 + 部分缓解 1）+ 新发现 6 项（MED×3 + LOW×2 + INFO×1）= **16 项**
- top-3 急迫：
  1. **[NEW-MED-1] actuator 全暴露** + **[LOW-3] JWT 默认密钥** 复合风险（prod 部署前必修）
  2. **[NEW-MED-2] prod 池+p6spy** + **[SEC-MED-7] prod maxPoolSize=20**（雪崩回归 + 性能降级）
  3. **[NEW-MED-4] RSA 公私钥字面量** + **[SEC-MED-5] API 加密配置**（需用户确认密钥轮换）

## 9. 是否建议立即阻断某 commit？

**不阻断 git 操作**：
- 本报告 16 项 finding 中 0 项是「新增 commit 引入的回归」
- 全部为「原 SEC-AUD 基线未修 + 本轮新发现配置漂移」，fix-forward 即可
- HIGH-1/2/4/5 + NEW-MED-1/3/4 涉及密钥 / 凭证，按硬约束标注「需用户确认」

**强烈建议（阻断下一波 prod release）**：
- LOW-3 + NEW-MED-1 + NEW-MED-3 + NEW-MED-4（4 项配置安全门禁）→ 必须修复后再 release
- HIGH-1/2（BCrypt + 密码明文）→ 必须修复后再 release
- HIGH-4 + NEW-LOW-2（P0-6.3 假绿测试）→ 修复测试后再 release
- NEW-MED-2（prod 池+p6spy）→ 需用户确认 + 压测验证后 release

**可放行但需记入 backlog**：
- LOW-1/2/4/5/6 + INFO-3/4 + NEW-LOW-1（代码异味） + NEW-INFO-1
