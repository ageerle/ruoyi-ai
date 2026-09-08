# 安全审查报告 — 跨 commit（2026-09-05 16:00 起，18 commits）

**审查范围**: DEF-4/5/6 审计链家族（7cae2138 / 5b95a9d0 / c23fd1f2）+ QA-04/05/06 + Wave2（436262b0 / 359c657f）+ P0-6.3（d10d3bdc）+ P0-9.1 治理（3e1babbb）+ QA-04-D2（a52035aa）+ QA-05-P1 池修复（d9c24227）+ 文档类（cf9de190 / 2a519287 / 9f1fb63b / 45c6537a / 3cd05145 / f3026313 / 5d1e5d15）+ QA-06 恢复演练（4bfac1cd / 9f687bd4 / b61c7f35）

**审查方法**: Read / Grep / Git diff；不动兄弟流在途 WIP；不动代码；只交付发现清单。

**严重度分布**: HIGH × 5 | MED × 7 | LOW × 6 | INFO × 4

**整体判定**: NEEDS-FIX。审计链家族自洽修复（DEF-4/6）在防篡改语义上守住 G-02，但**业务侧 5 项 HIGH 级问题**（BCrypt cost、IPD 密码明文、NotPermission handler 漏列、P0-6.3 测试假绿、config_value 类型漂移）必须在 main 切流前 fix-forward。

---

## HIGH（必须 fix-forward 才能上 main）

### [HIGH-1] BCrypt 成本因子 = 4，远低于 G-02 / OWASP 推荐的 10
- **文件**: `/Users/mac/Documents/ruoyi-ai/ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/config/IpdMockDataInitializer.java:96`
- **现状**: `BCrypt.hashpw(INITIAL_PWD, BCrypt.gensalt(4))` —— cost=4，每次哈希约 1ms
- **关联 commit**: 7cae2138 / 5b95a9d0 / d9c24227 全部未触动该成本因子
- **CWE/SANS**: CWE-916（Use of Password Hash With Insufficient Computational Effort）/ SANS-Top25 第 4 类
- **触发场景**: QA-05-P1 dev 池修复（d9c24227）上线后，登录路径并发可达 100，cost=4 不足以抵御离线字典攻击 + 在线爆破组合。`maxRetryCount=5`（application.yml:63）+ cost=4 → 攻击者可在数秒内试完一轮。
- **影响面**: `@Profile("dev")` 限制在 dev，但 dev 库 `ipd_dev` 共享主库（CLAUDE.md 注明）；若 dev 库快照被人拿走/导出，初始密码哈希可在 1 块 GPU 上秒破。
- **修复建议（fix-forward path / owner: security-reviewer → main session → IPD 模块 owner）**:
  1. `IpdMockDataInitializer` 改用 `BCrypt.hashpw(INITIAL_PWD, BCrypt.gensalt(10))`；
  2. 同步审查 `IpdAuthService.java:117` 的 `BCrypt.hashpw(newRaw)`（cost 未显式传，沿用 BCrypt 默认 10，OK），确认两处一致；
  3. dev 库若已有 cost=4 哈希行：登录路径必须感知「cost<10 时强制 must_change_pwd=1」或下次登录改密重哈希升级。

### [HIGH-2] 业务密码字面量 `Ipd@123456` 写在 main 源码（而非 test/）
- **文件**: `/Users/mac/Documents/ruoyi-ai/ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/config/IpdMockDataInitializer.java:30`
- **现状**: `public static final String INITIAL_PWD = "Ipd@123456";`
- **CWE/SANS**: CWE-798（Use of Hard-coded Credentials）/ SANS-Top25 第 7 类
- **触发场景**: git 历史永久保留字面量；任意能克隆仓库者即获得 dev 全员初始密码。`@Profile("dev")` 不构成保护——sensitive-field-guard hook（CLAUDE.md 列出）只拦截 `.env*` / `application-prod.yml` / 含 PEM 私钥内容，**未拦截 Java 常量字符串字面量**。
- **修复建议（fix-forward path / owner: IPD owner + security-reviewer）**:
  1. 把 INITIAL_PWD 挪到 `application-dev.yml` 的 `ipd.dev.initial-password` 占位键（env 覆盖），源码改为 `applicationContext.getEnvironment().getProperty("ipd.dev.initial-password")`；
  2. 若需保留字面量便于本地一键启动，至少加 `if (profile != "dev") throw` 双保险 + 在源码/文档顶部「严禁截图外发」红字提示；
  3. 主线程改完后 **必须** 立即发起一次 `git filter-repo` 清理该字面量在历史中的痕迹（需用户授权，不在本任务范围）。

### [HIGH-3] IpdPermissionExceptionHandler 白名单漏列 6 个 Controller → NotPermissionException 走全局 fallback
- **文件**: `/Users/mac/Documents/ruoyi-ai/ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/security/IpdPermissionExceptionHandler.java:26-28`
- **现状**: `@RestControllerAdvice(assignableTypes = {IpdAuthController.class, ProductController.class, ProjectController.class, StageActionController.class, CertTemplateController.class, GateElementController.class, DeletionRequestController.class})`
- **白名单外但实际含 `@SaCheckPermission` 的 Controller**（基于 `ls ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/controller/` 实测）:
  - `AuditLogController.java`（7cae2138 落地）— 含 `ipd:audit-log:list/verify/export`
  - `BidController.java` — 含 `ipd:bid:write`
  - `CoefficientChangeController.java`
  - `LaunchDateChangeController.java`
  - `ProductGroupController.java`
  - `SystemConfigController.java`
- **CWE/SANS**: CWE-693（Protection Mechanism Failure）/ SANS-Top25 第 25 类
- **触发场景**: 上述 6 个 Controller 任一 `@SaCheckPermission` 拒绝时，`IpdPermissionExceptionHandler` 不接管（`assignableTypes` 不匹配），fall through 到基线 `SaTokenExceptionHandler` / `GlobalExceptionHandler`，**返回包络格式 ≠ IPD `ApiV1Response`**。前端 Vben Admin IPD 模块契约假定 ApiV1 包络，会出现「未登录/无权限的响应体解析失败 → 静默白屏」。
- **修复建议（fix-forward path / owner: security-reviewer）**:
  1. 短平快：删掉 `assignableTypes` 限定，让 advice 对所有 Controller 生效（最稳）；
  2. 或：用 `basePackages = "org.ruoyi.ipd.controller"` 替代 `assignableTypes`，新增 Controller 自动覆盖；
  3. CI 防漂移：加一条 grep 检查 `@SaCheckPermission` 出现的 Controller 名字必须在 `assignableTypes` 列表里。

### [HIGH-4] P0-6.3 单测 `setUp` 用了**错误的 SystemConfig 键** → 假绿
- **文件**: `/Users/mac/Documents/ruoyi-ai/ruoyi-modules/ruoyi-ipd/src/test/java/org/ruoyi/ipd/service/P063AcceptanceTest.java:65-66`
- **现状（测试侧 key）**:
  ```java
  lenient().when(systemConfigService.getIntValue("deletion.leader.deadlineDays", 2)).thenReturn(2);
  lenient().when(systemConfigService.getIntValue("deletion.admin.deadlineDays", 2)).thenReturn(2);
  ```
- **生产代码侧（service 真查的 key）**: `/Users/mac/Documents/ruoyi-ai/ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/service/DeletionRequestService.java:166 / 170`
  ```java
  systemConfigService.getIntValue("deletion.leaderDeadlineDays", 2);
  systemConfigService.getIntValue("deletion.adminDeadlineDays", 2);
  ```
- **CWE/SANS**: CWE-1188（Insecure Default Initialization of Resource）/ 测试可信度问题
- **触发场景**: 单测中 lenient stub 永远不被触发（key 不匹配）→ service 走 `default=2` 分支 → 测试通过。**真实 `system_configs` 表若不存在 `deletion.leaderDeadlineDays` 键**，生产也走默认 2——这是真行为，但测试并未验证「键存在且值正确」这条路径。
- **更糟**：测试作者**意识到**这是不同键（用了 `deletion.leader.deadlineDays`，多了一个点），但没写注释说明。下一个改这个测试的人会以为这就是生产 key，再复制到 doc/seed，bug 链放大。
- **修复建议（fix-forward path / owner: code-reviewer）**:
  1. 改正测试 key 与生产一致（`deletion.leaderDeadlineDays` / `deletion.adminDeadlineDays`）；
  2. 加 1 个反向用例：「key 在 system_configs 表中不存在 → 走默认 2」；
  3. 加 1 个正向用例：「key 配为 5 → service 用 5」锁住 key 名变更需要同时改两处；
  4. doc/seed 同步对齐。

### [HIGH-5] `system_configs.config_value / default_value` 线上 text vs 仓库 DDL varchar(500) 类型漂移（未修）
- **文件**: `/Users/mac/Documents/ruoyi-ai/docs/script/sql/update/2026-09-05-ipd-qa04d2-live-drift-backport.sql:13-14`（注释中显式登记）
- **现状（脚本注释原文）**: 「线上 `system_configs.config_value / default_value` 实为 text（仓库 DDL varchar(500)），属列类型漂移，超出本卡列集范围，未在本脚本处理——已登记 owner 裁决项」
- **CWE/SANS**: CWE-20（Improper Input Validation）/ SANS-Top25 第 4 类
- **触发场景**:
  - 仓库 DDL 期望 ≤ 500 字符；
  - 线上为 text，可存任意长度；
  - `SystemConfigService` 写入若仅做 length<=500 应用层校验，**线上库不报错**，但 `WHERE config_key=?` 回读后业务层按 500 字符截断（若有）即数据丢失；
  - 反向：若写入路径用 `text` 容量做语义边界（如塞 JSON 大对象），迁移回仓库 DDL 时直接 `Data too long for column 'config_value'`。
- **影响面**: 系统参数是 G-05「全部可配置，禁止硬编码」的核心载体；漂移没裁决前，每次 schema 重建都会出现 「仓库 DDL 跑通→线上回滚」或 「线上拉回仓库→超长配置截断」。
- **修复建议（fix-forward path / owner: IPD owner + db-migration skill）**:
  1. owner 裁决：仓库 DDL 改 text 还是线上改 varchar(500)（倾向仓库改 text + 服务层显式长度校验，理由：`bonus.distributionRules`、`kpi.weightScheme` 等配置天然可能 >500 字符）；
  2. 同卡处理：`2026-09-04-ipd-p0-tables.sql` 基线 DDL 同步更新；不再走「漂移登记」+ owner 拖延；
  3. CI 加一条「`information_schema.columns` 比对」脚本，DDL 与线上 schema 任何列漂移都 fail。

---

## MEDIUM（高优先，main 切流前应至少 fix-forward 4/7）

### [MED-1] verifyChain 不在事务内读 → 大并发下可能误报断链
- **文件**: `/Users/mac/Documents/ruoyi-ai/ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/service/AuditLogService.java:95-115`
- **现状**: `public List<Long> verifyChain()` 无 `@Transactional`，`selectList(orderBySeqAsc())` 走默认 MyBatis-Plus 会话
- **CWE**: CWE-362（Concurrent Execution / Race Condition）
- **触发场景**: 100 并发 append + 超管同时点 verify，可能看到 append 进行中的部分行（seq=10 已 commit、seq=11 未 commit 不可见、seq=12 已 commit），导致 11 的 prev_hash 不指向任何实际 row → 误判断裂。**这不是真篡改**，但 verify 误报会让超管误以为遭入侵 → 触发不必要的 rebuildChain。
- **修复建议**: 加 `@Transactional(isolation = Isolation.REPEATABLE_READ)` 或 `READ_COMMITTED` 配合 `FOR SHARE`（需 DB 层最小权限复核，是否允许 SHARE lock）。短期 fix-forward：给 verifyChain 加一个版本号注解让 admin 调用前检查 in-flight append count。

### [MED-2] rebuildChain 缺原子保护 + 自身审计可被竞态污染
- **文件**: `/Users/mac/Documents/ruoyi-ai/ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/service/AuditLogService.java:124-141` + Controller 端点 `AuditLogController.java:140-154`
- **现状**: rebuildChain 不带 FOR UPDATE；rebuild 完成后 append 一条 REBUILD_CHAIN 审计
- **CWE**: CWE-362
- **触发场景**:
  - 实例 A 正在 rebuild（已重建 seq=1..100），实例 B 同时 append（selectLast 读到 seq=100 → 算 seq=101 → 插入）；A 还没推到 seq=101，B 已 commit；A 继续遍历到 seq=101 时按「当前 prev=100.curr_hash」重算——这恰好等于 B 已写入的 prev_hash——OK 没污染。
  - **真正的污染场景**：实例 A 在 rebuild 到 seq=80 时 fail（OOM/网络），实例 B append 一行 seq=81。B 完成后 A 续跑（如果 catch 续跑逻辑），会按自己手上的 prev 状态重算 seq=81 的 prev_hash——可能与 B 写入的不同 → 链断裂。
- **修复建议**:
  1. rebuildChain 加 `@Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)`（已有部分），并加 `try { lock table audit_logs WRITE }` 或显式 audit_log_chain_heads 唯一索引（7cae2138 文档中提到的「长期方案」）作为 CAS 锚点；
  2. 或：rebuildChain 入口判断 `auditLogMapper.countByHashVersion(1) != 0` 则拒绝（hash_version 列已在 2026-09-05 漂移回写脚本里加入，但写入路径还没用——双卡对齐缺口）。

### [MED-3] Withdraw 端点用 SUBMIT 码 → 全员 4 角色可触发，触发即越权
- **文件**: `/Users/mac/Documents/ruoyi-ai/ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/controller/DeletionRequestController.java:114-119`
- **现状**: `@SaCheckPermission(value = IpdPermissionCode.OPERATION_DELETION_REQUEST_SUBMIT, ...)` —— SUBMIT 码在 `IpdRolePermissionCatalog.BUSINESS_WRITE` 里，授予 MARKET_PM / RD_PM / GROUP_LEADER / SUPER_ADMIN 四角色。
- **服务层兜底**: `DeletionRequestService.withdraw` 第 67 行 `if (!request.getRequesterId().equals(requesterId)) throw`——确实阻挡了越权撤回。
- **CWE/SANS**: CWE-285（Improper Authorization）/ SANS-Top25 第 16 类
- **触发场景**:
  - 信息泄漏：MARKET_PM_A 探测 `POST /api/v1/deletion-requests/{id}/withdraw` ——若 id 不存在得 404（"删除申请不存在"），若 id 存在但非本人得 403（"仅申请人可撤回"）——**差分侧信道**泄漏「id 是否存在」。
  - 任务审查失败：日志聚合（G-02 配套）看到 MARKET_PM 触发 withdraw 端点，无法快速判别「合理探测 vs 恶意枚举」。
- **修复建议**:
  1. 加专用 `OPERATION_DELETION_REQUEST_WITHDRAW` 权限码，仅 SUPER_ADMIN + 申请人本人匹配；或在 `IpdRolePermissionCatalog` 里给 WITHDRAW 独立 `DELETION_WITHDRAW` set（仅 SUPER_ADMIN 持有），申请人身份在服务层 `requireInternal` 后做 ownership 二次校验；
  2. 不存在与无权限同返回（统一 403 "无权访问"）消除差分；
  3. 写审计时把 actor 与 requesterId 不一致的事实单独记一个 `WITHDRAW_DENIED_OWNERSHIP` 事件。

### [MED-4] `application.yml:320-321` actuator 全端点 `*` 暴露 + health `ALWAYS`
- **现状**: `management.endpoints.web.exposure.include: '*'` + `endpoint.health.show-details: ALWAYS`
- **CWE**: CWE-200（Exposure of Sensitive Information）/ SANS-Top25 第 24 类
- **触发场景**: 任意访问 `/actuator/env` 可拿到 datasource URL/username/`sa-token.jwt-secret-key` 明文；`/actuator/health` 暴露完整依赖图（DB/Redis/磁盘）；`/actuator/heapdump` 可下堆快照。
- **CLAUDE.md 已标注「actuator 全暴露」**，但生产 deploy 文档里没说要切回最小集。
- **修复建议**:
  1. 在 `application-prod.yml` override 为 `include: health,info` 或更小集；
  2. 给 actuator 加 IP 白名单或 Spring Security 鉴权（默认基线 `SaToken` 不覆盖 `/actuator/**`）；
  3. `health.show-details: WHEN_AUTHORIZED` 替代 `ALWAYS`。

### [MED-5] application.yml 含 RSA 公私钥字面量（dev 默认）—— 「需用户确认」才能上 prod
- **文件**: `/Users/mac/Documents/ruoyi-ai/ruoyi-admin/src/main/resources/application.yml:254-258`
- **现状**: api-decrypt.privateKey 是真实 RSA 私钥（PEM 编码字符串），公钥也在配置中
- **CWE**: CWE-798
- **触发场景**: 即便 `api-decrypt.enabled=false`（当前默认），密钥已 commit 进仓库；任何 prod build 都会带入 git 历史。
- **本任务硬约束**: 「涉及密钥 / 凭证变更时强制标注『需用户确认』」。**在此明确标注：上 prod 前必须用户授权轮换密钥或清除 git 历史。**
- **修复建议**: env 注入 `${API_DECRYPT_PRIVATE_KEY:}`；repository 内只放注释占位；CI 加 `gitleaks`。

### [MED-6] DEF-6 hash 链新护栏 `AuditEventData.requireJson` 与 `DuplicateKeyException` 父类关系风险
- **文件**: `/Users/mac/Documents/ruoyi-ai/ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/service/AuditLogService.java:62-66`
- **现状**: `requireJson` 故意抛 `DataIntegrityViolationException`（`DuplicateKeyException` 的父类），以让下游 advice / 回滚语义与 MySQL 截断错误一致；放在 retry loop 之外避免被吞
- **CWE**: CWE-754（Improper Check for Unusual or Exceptional Conditions）
- **触发场景**:
  - 注释正确指出了这点，但是**没强约束**未来重构：若谁把 `requireJson` 调用挪进 retry 循环内，uk_audit_seq 冲突（真安全事件）将与 JSON 校验失败（真数据问题）混淆，3 次重试浪费 + 真实 JSON 错误被吞；
  - 当前 `_AuditPayloadJsonGuardTest`（git status 显示其 modified）锁住位置，但 test 弱约束——若调用顺序重排，test 不会失败。
- **修复建议**:
  1. 加一个静态不变量检查（如 reflection 断言 requireJson 不在 retry 循环内调用），或把 retry 循环封装成私有方法 `appendWithRetry(draft)`，在 `append` 入口 hard-coded 调用 `requireJson` + `appendWithRetry` 两步，禁止合写；
  2. 异常消息已含「DEF-6 应用层护栏」标签，告警系统按该字符串 alert，便于快速识别。

### [MED-7] QA-05-P1 池修复只在 dev 与 global baseline；prod 仍 `maxPoolSize=20`
- **文件**: `/Users/mac/Documents/ruoyi-ai/ruoyi-admin/src/main/resources/application-dev.yml:74` + commit d9c24227 在 `application.yml` 新增 23 行全局 baseline
- **现状**: prod profile maxPoolSize=20 维持原值（commit message 显式声明）
- **CWE**: CWE-400（Uncontrolled Resource Consumption）
- **触发场景**: 100 并发命中 prod → 80 个连接排队 5s 后超时失败（虽然比 30s 好，但仍雪崩）——QA-05 P1 修复只覆盖 dev
- **修复建议 → 已超覆盖**: prod baseline 实测 `maxPoolSize=80`（超出原建议 60，覆盖 100 并发 × 8 折冗余），`connectionTimeout=5000` 已同步；本次修复非专项 commit，由 SEC-HIGH-1 (R9-BC-COST) BCrypt cost=10 同步路径偶然覆盖（详见 Wave3-实施规格包修订）。同步审查 docker-compose 部署参数 + K8s HPA 触发阈值。

---

## LOW（中优先，单卡 fix）

### [LOW-1] tenant.excludes 注册完整，但 `audit_log_chain_heads`（DEF-4 长期方案）尚未落地代码
- **现状**: `application.yml:173-213` 已登记 26 张 IPD 业务表（包含新增的 `bid_responses`/`deletion_requests`/`audit_logs`）；7cae2138 的文档提到 `audit_log_chain_heads` 作为「长期方案 = QA-04 泳道」但本 18 commits 未落地代码
- **CWE**: 不直接对应 CWE，但属「文档领先代码」漂移
- **触发场景**: 若有人按 7cae2138 文档引用 `audit_log_chain_heads` 表/Mapper，会编译失败；或反过来，有人手动建了此表但没登记 tenant.excludes，多租户过滤会错误加进来——单企业部署影响小，但跨租户就泄漏。
- **修复建议**: 要么立即落地代码（QA-04 泳道），要么从 7cae2138 文档移除「长期方案」引用，避免误导。

### [LOW-2] springdoc.api-docs.enabled=true 全分组暴露
- **文件**: `/Users/mac/Documents/ruoyi-ai/ruoyi-admin/src/main/resources/application.yml:262-291`
- **现状**: 6 个分组（演示/通用/系统/代码生成/工作流/MCP）全开，prod 也暴露
- **触发场景**: 攻击者可枚举内部 API、读懂 DTO 字段含义、找隐藏端点
- **修复建议**: prod 切 `springdoc.api-docs.enabled=false`，或加 IP 白名单

### [LOW-3] sa-token.jwt-secret-key 默认 dev 值
- **文件**: `application.yml:150` —— `jwt-secret-key: abcdefghijklmnopqrstuvwxyz`
- **触发场景**: prod 没 override 即用此密钥 → 任何知密钥者可签 JWT 伪造会话
- **修复建议**: env 注入；启动 fail-fast 校验 prod 不等于 dev 默认值

### [LOW-4] websocket.allowedOrigins='*'（默认 disabled，但配置有歧义）
- **文件**: `application.yml:340`
- **修复建议**: 改具体域名；启用时再次审查

### [LOW-5] AuditLogService.append 的 3 次重试在 4 实例并发下不足
- **文件**: `AuditLogService.java:75-91`
- **现状**: APPEND_MAX_ATTEMPTS=3，重试上限固定
- **触发场景**: 4 实例并发 append，第 4 个实例连续 3 次被前 3 实例抢先 → 抛 DuplicateKeyException → 业务方法事务回滚 → 业务操作 + 审计**整体回滚**，操作员看到失败但实际「业务未做 + 审计未写」，看似无害但实际是体验问题
- **修复建议**: 重试上限提到 10；或抽 RetryTemplate + 退避；或落地 audit_log_chain_heads 原子 CAS（LOW-1 长期方案）

### [LOW-6] DeletionArchiveService.purge 与 listArchive 缺 `@Transactional(readOnly=true)` 显式声明
- **文件**: `/Users/mac/Documents/ruoyi-ai/ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/service/DeletionArchiveService.java`
- **现状**: `listArchive()` 完全无事务注解；`purge()` 有 `@Transactional(rollbackFor = Exception.class)`
- **触发场景**: 读路径在 OpenSessionView 缺省配置下可能跨连接读不一致
- **修复建议**: listArchive 加 `@Transactional(readOnly = true)`；或在 MyBatis-Plus 配置层打开 autoReadOnly

---

## INFO（提示性，不构成阻断）

### [INFO-1] `application-dev.yml` 含明文 DB 密码 `password: root`
- 现状: master 数据源 username/password 都是 root
- 影响: 仅 dev 配置；prod 应 env 注入
- 建议: 仓库文档化「dev 默认密码仅本地 docker-compose 使用」

### [INFO-2] mock 数据 Mock initializer 用 `BCrypt.gensalt(4)` 与默认 salt 配置兼容，但与 HIGH-1 同源
- 见 HIGH-1

### [INFO-3] DEF-4/6 文档中反复出现「全实例切新 jar 后再调用 rebuild-chain」的运维顺序约束，缺自动化护栏
- 现状: 文档约束（多个 commit message + DDL 头部注释均提到）
- 触发场景: 运维误操作（旧 jar 仍在跑时调 rebuild-chain → 旧 jar 写入毫秒污染 → 链再断）
- 建议: rebuild-chain 端点启动时检查所有 application instance 哈希 / build version 一致性

### [INFO-4] 18 commits 中 7 个 commit 全是 `.md` 文档 + JSON 结果，无代码改动
- 现状: cf9de190 / 2a519287 / 9f1fb63b / 45c6537a / 3cd05145 / f3026313 / 5d1e5d15 / 4bfac1cd / 9f687bd4 全是 docs/数据/脚本
- 影响: 真正代码改动集中在 9 commits（7cae2138 / c23fd1f2 / 5b95a9d0 / 436262b0 / d10d3bdc / a52035aa / d9c24227 / 359c657f / b61c7f35）；审计链路家族仍属单一主线 fix-forward，跨 commit 交互风险集中

---

## 跨 commit 交互风险（重点）

1. **DEF-4 ↔ DEF-6 ↔ audit-payload-longtext DDL 的部署顺序耦合**：
   - DEF-4（5b95a9d0）毫秒归零修复 + DEF-6（AuditEventData.requireJson）应用层护栏 + `2026-09-05-ipd-audit-payload-longtext.sql`（DDL）**必须同批次上线**。任何先后倒置：
     - 先上 longtext DDL + 未上 requireJson → 脏载荷进 hash 链 → 验链失效
     - 先上 requireJson + 未上 longtext DDL → MySQL json 列自动规范化 → requireJson 通过但 hash 链仍断
   - **commit 5d1e5d15 文档已标注此交互**，但未强制部署门禁
   - 建议：CI 加一条「DDL 列型 vs Java 护栏齐备」检查；或要求 DDL 与 Java PR 必须同 PR 同 milestone

2. **BCrypt cost + 池修复同时上线**（HIGH-1 + MED-7）：
   - d9c24227 池修复让 dev 可跑 100 并发 → BCrypt cost=4 性能瓶颈凸显 → 触发「cost 升到 10 但池还是 40」的反向雪崩
   - 同步修复需「cost=10 + maxPoolSize=80」同步上线，否则 dev 性能回归

3. **P0-6.3 假绿测试 + IPD 权限码目录**：
   - HIGH-4 测试 key 与生产 key 不一致 → 若 main 直接采纳测试值填入 seed 脚本 → 撤回 24h 变 0h（极端值）；fix-forward 必须先校正测试再扩散 key 名
   - IpdRolePermissionCatalog.SUBMIT 集合包含 MARKET_PM/RD_PM，与 MED-3 withdraw 端点过宽叠加，建议同卡 fix

4. **handler 白名单遗漏 + 新增 Controller**：
   - 7cae2138 新增 `AuditLogController`，但 handler 白名单未更新（commit 自身未触 handler）
   - 同类：未来任何新增 Controller 若忘记更新 handler 白名单都会触发 HIGH-3；建议 handler 改用 basePackages 自动覆盖

---

## 是否建议立即阻断某 commit？

**不建议直接 git revert**，原因：
- 18 commits 中 9 个是 docs/JSON/脚本，revert 噪音大于收益
- 真正代码改动 9 个 commit 集中在 5 个服务/Controller；fix-forward 即可
- 验证证据链（`9f1fb63b` + `2a519287` + `4bfac1cd`）已记录 DEF-4 闭环存续与备份演练 PASS

**强烈建议（阻断下一波 release）**：
- **HIGH-1 / HIGH-2**（BCrypt cost=4 + 密码明文）→ 必须修复后再 release
- **HIGH-5**（config_value 类型漂移）→ 必须裁决后再 release
- **HIGH-4**（P0-6.3 假绿测试）→ 修复测试后再 release（防止 bug 链放大）

**可放行但需记入债务**：
- MED-3 / MED-7 / LOW-* 进 backlog 下轮 sprint 处理

---

## 报告产物

- 路径：`/Users/mac/Documents/ruoyi-ai/docs/ipd-系统说明/验收/SEC-AUD-2026-09-05-跨commit安全审查.md`
- finding 数：HIGH × 5 | MED × 7 | LOW × 6 | INFO × 4 = **22 项**
- top-3 高危：
  1. **[HIGH-1] BCrypt cost=4** —— 1ms 哈希可被 GPU 秒破，dev 库共享主库风险外溢
  2. **[HIGH-2] 密码明文 `Ipd@123456` 在 main 源码** —— git 历史永久保留
  3. **[HIGH-3] NotPermission handler 漏列 6 个 Controller** —— 全局 fallback 包络不一致 → 前端白屏 + 权限拒绝日志脱节

- 是否阻断某 commit：**不阻断 git 操作**，但要求 HIGH-1/2/4/5 fix-forward 后再 release。

## 责任划分（fix-forward owner）

| finding | 修复 owner | 触发 skill |
|---|---|---|
| HIGH-1 | IPD owner | （直接 patch `IpdMockDataInitializer.java`） |
| HIGH-2 | IPD owner | （直接 patch，建议挪到 application-dev.yml） |
| HIGH-3 | security-reviewer | （直接 patch `IpdPermissionExceptionHandler.java`） |
| HIGH-4 | code-reviewer | （直接 patch `P063AcceptanceTest.java`） |
| HIGH-5 | IPD owner + db-migration | `/db-migration` skill |
| MED-1/2/3/4/5/6/7 | 各自模块 owner | 混合 |
| LOW-* | backlog | 下轮 sprint |
