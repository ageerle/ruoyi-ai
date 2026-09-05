# ABC 依次执行 — 全量 @Tag(dev) 验证 + 缺陷 A/B/C·DEF-1 复评（2026-09-05）

> **会话角色**：第二方 QA / 独立复核泳道（遵 AUD-GOV-01 登记的 SSOT-L311「不改 Java 源码」+ OPS-09 单一写入者）
> **执行时间**：2026-09-05 15:13–15:19 PDT
> **基线 HEAD**：A 运行时 `a39d1d9d` → 运行中兄弟前移至 `b7985d16`（P0-4.1 ApiV1Response.timestamp UTC）
> **边界**：仅新增/提交本会话证据报告；**不写 Java 源码、不写看板镜像**（均他泳道在途/单写）；**不重启共享 app**
> **环境**：live 16039（STALE，见 §3）、MySQL 13306、active builds 0（错峰干净）、脏跟踪 27→31（兄弟权限/产品泳道热写中）

## 0. 结论速览

| 项 | 结果 | 判定 |
|---|---|---|
| **A** 全量 @Tag(dev) 单模块验证 | Tests run **324 / Failures 1 / Errors 0 / Skipped 18** @15:14:04 | ✅ 契约层全绿；1 失败=**WIP 假红**（非 HEAD 回归） |
| **A** 契约/安全测试 | Sec01 13/13·Sec02 7/7·SecApi02 4/4·Api01 4/4·Api03 5/5·P054 9/9·StpType 2/2 | ✅ 兄弟收口非假绿（独立复现佐证） |
| **环境** live 16039 app | `GET /audit-logs/scope`→`code:404 No endpoint` | ⚠ **STALE**（早于 cfdbca20 达 1.5h）→ 阻断真库 HTTP 复验 |
| **B** 缺陷A-system-config | 脏树已补 3 码（未提交） | ◐ 修复中（兄弟权限泳道在途） |
| **B** 缺陷A-audit | `ipd:audit-log:list/verify/export` **仍缺** Catalog | ❌ **OPEN**（老 3 端点全员 500） |
| **B** 缺陷B advice | assignableTypes 仍 7 控制器；advice 无 NotPermission handler | ❌ **OPEN**（拒绝塌缩 500/90001） |
| **B** DEF-1 | GateElementService.audit 明文写 JSON 列 | ❌ OPEN → ✅ **FIXED**（§8：b74f46bf 并发修复+4 单测绿佐证） |
| **B** DEF-2 | NOAUTH 空体→500 非 401 | ❌ **OPEN**（=缺陷B2） |
| **B** 缺陷C | 旧探针脚本错用户名 | ✅ 已被 `QA-03-matrix-v5.py` 取代 |
| **C** SEC-API-02 ✅done | 交付文件 Catalog 仍缺 audit-log 码 | ⚠ **部分假绿**（提请复评） |
| **C** QA-03 ⬜待认领 | 报告(84fac6e8)+脚本(2c7b69be)已提交 61/61 | ⚠ **看板滞后**（假红：已做未记功） |

---

## 1. A — 全量 @Tag(dev) 单模块验证

**命令**（错峰/离线/无 -am 无 clean，遵 AGENTS.md 复核纪律）：
```
export PATH="$HOME/tools/maven/bin:$PATH"; export JAVA_HOME="$HOME/tools/jdk-17/Contents/Home"
mvn -o -pl ruoyi-modules/ruoyi-ipd test
```
- START 15:13:54（active builds 0，错峰干净）→ END 15:14:04（增量编译，10s）
- 汇总：**`Tests run: 324, Failures: 1, Errors: 0, Skipped: 18`** → BUILD FAILURE（仅 1 断言失败）
- Skipped 18 = `P131DatabaseIntegrationTest`（真库集成，条件跳过）——符合预期，非假绿（其余 306 项 tests run>0）

**唯一失败 = WIP 假红（非 HEAD 回归）**：
```
ProductServiceTest.createOk:87  expected: "ACTIVE" but was: "IN_RD"
```
- 根因：兄弟**未提交**的 `ProductService.java`（脏）实现 AC-PROD-06/07「超管导入→在售 ST_ON_SALE；PM 新增→在研 ST_IN_RD」，把 PM_NEW 默认状态由 `ACTIVE` 改为 `IN_RD`；而 `ProductServiceTest.java`（**未脏**，L83 `@DisplayName("PM_NEW 正常创建 + 默认 ACTIVE")`、L87 断言 `"ACTIVE"`）尚未同步更新。
- 判定：本运行编译的是**脏工作树**（含兄弟在途 WIP），故失败反映未提交的半成品状态；**在提交态 HEAD `b7985d16`，ProductService.create 仍设 "ACTIVE"，该测试通过**。属 AGENTS.md/记忆所载「假红陷阱」教科书实例。
- 处置：**不改测试、不改源码**（他泳道在途）；建议兄弟 P1-1.2/AC-PROD-06/07 收口时同步更新 `ProductServiceTest.createOk` 期望为 `IN_RD`，并在**提交后净窗口**错峰重跑全量确认归零。

## 2. A 补充 — 契约/安全测试全绿（佐证兄弟收口非假绿）

| 测试 | 结果 | 对应卡/意义 |
|---|---|---|
| `Sec01AcceptanceTest` | 13/13 | SEC-01 命名验收重建（ab7d8fa7）覆盖 AC-AUTH-08/HR-07/08/GLB-11 |
| `Sec02AcceptanceTest` | 7/7 | SEC-02 六身份矩阵（**mock 层**，不覆盖真实 HTTP，见 §4.3 盲点） |
| `SecApi02StpInterfaceTest` | 4/4 | SEC-API-02 StpInterface 桥接 |
| `Api01AcceptanceTest` | 4/4 | API-01 包络（仅覆盖 assignableTypes 内控制器） |
| `Api03AcceptanceTest` | 5/5 | API-03 IpdAuthInputException→400（MockMvc 真实 dispatch 穿真实 advice） |
| `P054AcceptanceTest` | 9/9 | P0-5.4 `/scope`+`/export/scope`（**不含**老 list/verify/export，见 §4.2） |
| `IpdAuthSessionStpTypeTest` | 2/2 | loginType=ipd 会话 |

→ 兄弟各泳道收口在**契约/mock 层**均真实绿；但 §4 三缺陷恰在这些测试的**盲点**（真实 HTTP 全链 + JSON 列 + 目录码），故 mock 绿 ≠ HTTP 闭环仍成立。

## 3. 关键环境发现 — live 16039 app STALE（阻断真库 HTTP 复验）

**探针**（read-only，15:16）：
```
GET http://127.0.0.1:16039/api/v1/audit-logs/scope  (无 token)
→ HTTP 200  body: {"code":404,"msg":"No endpoint GET /api/v1/audit-logs/scope."}
```
- `/scope` 端点由 `cfdbca20`（P0-5.4）新增；live app **无此端点** → 证明 app 早于 cfdbca20。
- 时间对照：app 进程启动 **13:17:03**；`cfdbca20`=14:44:23、`a2d07f19`(DTO 修复)=13:59:53、HEAD `b7985d16`=15:1x。app 落后 HEAD ≥4 个代码提交、约 2h。
- **影响**：对 16039 的任何真库 HTTP 探针都在测**旧码**，无法复验当前缺陷修复态。兄弟 P0-5.4 是在**独立实例 16041**（14:25 从 cfdbca20 起）验收的，非 16039。
- **移交**：真库 HTTP 端到端复验须待 **owner 从当前 HEAD 重启共享 app**（或起专用实例）后进行；本轮以「源码级确定性追踪 + MockMvc 契约测试 + QA-03/7367cfd0 既有实证」替代 live 复验，诚实标注 live 复验为 **BLOCKED（stale app）**。

## 4. B — 缺陷只读复核 + 修复规格（不改 Java，遵 lane）

> 修复方案与记忆 `0b9120df`（缺陷模式）一致；以下为 owner 泳道**待应用规格**，本会话不落笔到热树。

### 4.1 缺陷A-system-config — ◐ 修复中（脏树，未提交）
`IpdRolePermissionCatalog` 脏 diff 已加 `ipd:system-config:read`(READ_SET)、`:list`+`:update`(ADMIN_WRITE)。兄弟权限泳道在途，未提交。**不碰**。

### 4.2 缺陷A-audit — ❌ STILL OPEN
- `AuditLogController` 老端点 `GET /`(L57)、`/verify`(L69)、`/export`(L81) 携 `@SaCheckPermission("ipd:audit-log:list/verify/export")`；但 Catalog **无任何 `ipd:audit-log:*` 码** → 全角色（含 SUPER_ADMIN）AOP 层抛 `NotPermissionException`。
- P0-5.4 新增的 `/scope`(L98)、`/export/scope`(L114) **不带**注解权限，用 `requireInternal()`+service 角色范围，故**可用**；P0-5.4 看板明示「不争 sibling 已有的 list/verify/export 3 端点」→ 老 3 端点未修。
- **后果**：AC-AUD-02（`/api/v1/audit-logs/verify`）**仍无法 live 通过**（叠加 §4.3 → 500）。
- **修复规格**：Catalog 增 `AUDIT` 集 `{"ipd:audit-log:list","ipd:audit-log:verify","ipd:audit-log:export"}`，仅并入 `SUPER_ADMIN`（对齐老端点 `requireAdmin()` 语义）；或若老端点已被 `/scope` 取代则删除之。

### 4.3 缺陷B advice 白名单 — ❌ STILL OPEN（双实证）
- `IpdPermissionExceptionHandler.assignableTypes` 仍仅 7 控制器（IpdAuth/Product/Project/StageAction/CertTemplate/GateElement/DeletionRequest），**缺 AuditLog/SystemConfig/CoefficientChange/LaunchDateChange**。
- `IpdServiceExceptionAdvice`（basePackages=`org.ruoyi.ipd.controller`，覆盖全部）handler 集 = IpdBusinessException/ServiceException/MethodArgumentNotValid/NoHandlerFound/**IpdAuthInputException**(API-03 新增)/Exception.class——**无 NotPermission/NotRole**。
- ∴ 上述 4 控制器的 `NotPermissionException` 落 `Exception.class` → **500/90001**，而非 403/30001。
- **双实证**：QA-03 `DEF-3`（system-config:list→500，HEAD a6b407d9 真机）+ 本会话 `7367cfd0`（audit-logs→500）。**当前无任何测试锁定此路径**（§2 盲点：Sec02 是 mock、Api01 仅覆盖白名单内控制器）。
- **修复规格（二选一，建议后者更 robust）**：①assignableTypes 补 4 控制器；②`IpdServiceExceptionAdvice` 增全局 `@ExceptionHandler(NotPermissionException.class)`+`(NotRoleException.class)`→403/FORBIDDEN(30001)，未来新控制器自动覆盖。并补 `MissingServletRequestParameterException`/`HttpMessageNotReadableException`→400（解 DEF-2/B2）。

### 4.4 DEF-1 GateElement 审计明文写 JSON 列 — ❌ STILL OPEN（影响面更广）
- `GateElementService.audit()`：`.afterData(detail)` 中 `detail` 为**明文串**——`create`=`gateCode+"/"+elementCode`（如 "G1/E01"）、`update`=`elementCode`、`disable`=`elementCode+" disabled"`；`audit_logs.after_data` 是 MySQL **JSON 列** → `MysqlDataTruncation: Invalid JSON text` → 500 + 同事务回滚。
- **影响面 = create + update + disable 全部要素写路径**（QA-03 仅测到 create；update/disable 用 id=0 触发 400 未达 audit，valid id 必同样 500）。`GateElementService.java` **未脏** → HEAD 与工作树均未修。
- `GateElementServiceTest 5/5` 绿 = mock 掉 auditLogService，**不触真 JSON 列** → mock 绿 ≠ HTTP 闭环。
- **修复规格**：`audit()` 的 afterData 改写**合法 JSON**（如 Jackson 序列化 `{"gate":"G1","element":"E01"}` 或 JSON 字符串字面量），保持 P0-5 审计 prev_hash/curr_hash 链不断；补真库 HTTP 正例（超管建/改/停要素→200 且 audit_logs 落 1 行可解析 JSON）。

### 4.5 DEF-2 NOAUTH 空体→500 非 401 — ❌ STILL OPEN（=缺陷B2）
`@Valid`/消息不可读先于鉴权 AOP 触发且落 500 兜底。随 §4.3 修复（HttpMessageNotReadable→400 + 未认证 401 兜底）一并解。

### 4.6 缺陷C 旧探针脚本 — ✅ 已取代
`验收/SEC-02-real-http-probe.sh` 仍含错用户名 `MARKET_PM_A/B`（假绿源）；已被 `验收/QA-03-matrix-v5.py`（真实种子名，61/61，脚本 2c7b69be 归仓）取代。建议旧脚本标注 deprecated 或删除，防误用。

## 5. C — 看板复评（不写看板，提请协调器）

### 5.1 SEC-API-02（✅done）→ ⚠ 部分假绿
交付物 `IpdRolePermissionCatalog`（commit 1ec31f5d）**仍缺 audit-log 码**（§4.2）；system-config 码在脏树未提交。桥接/StpInterface 本体 done 可立，但**目录完整性缺口应挂 SEC-02/缺陷A 跟踪**，不宜以「Catalog 已交付」视为权限目录全绿。

### 5.2 QA-03（⬜待认领）→ ⚠ 看板滞后（假红：已做未记功）
QA-03 报告（`84fac6e8`，矩阵 61/61 + 审计 5 项确认 + DEF-1/2/3）与脚本（`2c7b69be`）**均已提交**，但镜像 QA-03 行仍 `⬜ 待认领；未实施/未验收`。提请协调器据已提交证据将其推进至 done/inreview（本会话不写镜像，遵单写）。

### 5.3 P0-5.1 / P0-5.4 scope 厘清
- P0-5.1（✅done）明示「HTTP 验链 API 仍属 P0-5.4」，其自身 scope（验链算法 + 大整数无损 JSON 存储）done 可立。
- P0-5.4（✅done，14:25@16041 验收 `/scope`）**未含**老 `/verify`；叠加 §4.2+§4.3，**AC-AUD-02 `/verify` 至今未 live 通过**。提请：AC-AUD-02 的 live 闭环挂到缺陷A-audit+B 修复后，不由 P0-5.1/P0-5.4 现有 done 覆盖。

### 5.4 DEF-1 建卡提案（提请协调器登记）
- **标题**：GateElement 审计 afterData 明文写 JSON 列致 create/update/disable 全 500 回滚
- **级别**：P1 功能阻断（超管核心配置接口 100% 不可用，上线以来从未成功写入）
- **AC**：超管 POST/PUT/disable gate-elements 合法体→200；audit_logs 落对应行且 after_data 为可解析 JSON；prev/curr hash 链不断
- **root cause/修复**：见 §4.4；**证据**：QA-03 §5 DEF-1 + 本报告源码追踪 + GateElementServiceTest mock 绿对照

## 6. 遗留风险与移交

1. **live app STALE**（§3）：真库 HTTP 复验 BLOCKED，须 owner 从 HEAD 重启共享 app/起专用实例；重启前所有 16039 探针结论仅代表旧码。
2. **WIP 假红**（§1）：`ProductServiceTest.createOk` 待兄弟 AC-PROD-06/07 收口同步更新测试后，净窗口错峰重跑全量确认归零；本会话不改他泳道测试/源码。
3. **缺陷 A-audit/B/DEF-1/DEF-2 仍 OPEN**：修复规格已备（§4），归 owner 泳道应用；应用后须补**真实 HTTP 正/反例**锁定（当前无测试覆盖，§2 盲点），防再次 mock 绿假关。
4. **看板复评**（§5）：SEC-API-02 部分假绿、QA-03 滞后、AC-AUD-02 未 live 闭环、DEF-1 建卡——均提请协调器在 drift=False 净窗口据已提交证据处置。
5. **热树持续**（脏 27→31，HEAD 运行中前移 a39d1d9d→b7985d16）：本会话全程只读 + 隔离提交，未写 Java/镜像/他卡，遵 OPS-09。

## 7. 附录 — 可复现命令与时间戳

```
# A 全量验证（15:13:54–15:14:04，324/1/0/18）
mvn -o -pl ruoyi-modules/ruoyi-ipd test         # 日志 .codex/ipd-dev/a-fullrun-devtag.log（gitignored）
# 假红根因
sed -n '1,20p' ruoyi-modules/ruoyi-ipd/target/surefire-reports/org.ruoyi.ipd.service.ProductServiceTest.txt
git diff -- .../service/ProductService.java      # 兄弟 WIP：setStatus ACTIVE→IN_RD
# STALE 探针（15:16）
curl -s http://127.0.0.1:16039/api/v1/audit-logs/scope   # → code:404 No endpoint
# 缺陷 A-audit / B 源码定位
grep -n "audit-log" .../security/IpdRolePermissionCatalog.java          # 无匹配 = 缺码
grep -n "assignableTypes\|Controller.class" .../security/IpdPermissionExceptionHandler.java  # 仅 7
grep -nE "ExceptionHandler" .../advice/IpdServiceExceptionAdvice.java   # 无 NotPermission
# DEF-1 源码定位
grep -nA6 "private void audit" .../service/GateElementService.java      # afterData(明文)
```
时间戳：A 运行 15:13:54–15:14:04 PDT；STALE 探针 ~15:16；本报告写就 15:18–15:19 PDT；HEAD `b7985d16`；active builds 0（错峰）；脏跟踪 31（兄弟在途，未碰）。

---

## 8. ADDENDUM（15:20–15:22 PDT）— DEF-1 已由 b74f46bf 并发修复 + 第二方独立验证

本报告 §4.4 基于 **15:16 源码读取**（当时 DEF-1 OPEN）。写就期间（15:16→15:19）兄弟提交 **`b74f46bf fix(ipd): DEF-1 GateElement 审计 afterData 改走 AuditEventData.json +4 回归单测`**，恰与本会话 §4.4 修复规格**独立收敛**。第二方独立验证：

**① 修复正确性（源码级）**
- `GateElementService.audit()`：`.afterData(detail)` → `.afterData(AuditEventData.json("detail", detail))`。
- 新增 `AuditEventData.json(Object... pairs)`：LinkedHashMap + Jackson `writeValueAsString` → 合法 JSON `{"detail":"G1/E01"}`（修复前纯文本 `G1/E01`）→ 满足 MySQL JSON 列。与 §4.4 规格一致。
- b74f46bf 仅动 `GateElementService.java` + `GateElementAuditJsonTest.java` 两文件。

**② 回归测试佐证（错峰单类，独立复现）**
```
mvn -o -pl ruoyi-modules/ruoyi-ipd -Dtest=GateElementAuditJsonTest test
→ Tests run: 4, Failures: 0, Errors: 0, Skipped: 0  BUILD SUCCESS @15:21:36（active builds 0）
```
- `GateElementAuditJsonTest`（@Tag dev）虽属 Mockito 层，但 `assertValidJson()` 用**真实 `ObjectMapper.readTree(afterData)`** 断言 afterData 为合法 JSON——锁死的正是 DEF-1 违反的契约，**非空洞 mock 绿**（对照：旧 `GateElementServiceTest` 完全 mock 掉 auditLogService、从不校验 afterData，故 5/5 绿却漏 DEF-1）。sibling 亦诚实标注「mock 层；真机 HTTP 证据见 QA-03 报告」。

**③ 残留缺口（诚实标注）**
- 单测证明 afterData 是合法 JSON（MySQL JSON 列接受的充要条件），但**未做修复后真库 HTTP 端到端复验**（超管 POST /gate-elements→200 + audit_logs 落 1 行）。该复验被 §3 STALE app 阻断（16039 早于 b74f46bf）；建议 owner 重启 app 后用 `QA-03-matrix-v5.py` 重跑 gate-element create 格作最终闭环。
- **判定更新**：DEF-1 由 ❌OPEN → ✅**FIXED（源码正确 + 4 单测绿佐证 + 契约锁死）**，仅余「真库 HTTP 端到端复验」待 app 重启（PARTIAL-closure）。

**④ 缺陷 A-audit / B 未受 b74f46bf 触及**（未动 Catalog/advice/AuditLogController）→ 仍 ❌OPEN，§4.2/§4.3 结论不变。

---

## 9. ADDENDUM（15:28–15:37 PDT）— 「继续」授权落地：缺陷B IMPLEMENTED+VERIFIED / A-audit BLOCKED / DEF-1 全闭环

owner 指令「继续」= 授予本第二方泳道**写授权**修 §4.3 缺陷B（仅此缺陷，覆盖 AUD-GOV-01「不改 Java 源码」lane 规则）。基线 HEAD `7b13a409`（较 §8 又前移：兄弟 `86f2a045` P0-4.1 timestamp 改 String、`7b13a409` DEF-1 回归证据）。

**① 缺陷B 修复（已落盘 + 隔离提交 `6628ab3b`）**
- `IpdServiceExceptionAdvice.java` +43 行，新增 5 个全局 `@ExceptionHandler`（`basePackages=org.ruoyi.ipd.controller` 覆盖全部控制器，补 `IpdPermissionExceptionHandler.assignableTypes` 7 控制器白名单之外的缺口）：

| 异常 | 修复后映射 | 修复前 |
|---|---|---|
| `IpdPermissionException` | 按 `e.getHttpStatus()`/`getErrorCode()` 忠实映射（401/20001、403/30001…） | 落 `Exception.class` → 500/90001 |
| `NotPermissionException` | 403 + 30001 | 500/90001 |
| `NotRoleException` | 403 + 30001 | 500/90001 |
| `HttpMessageNotReadableException` | 400 + 10001（DEF-2） | 500/90001 |
| `MissingServletRequestParameterException` | 400 + 10001 | 500/90001 |

- 提交隔离核验：`git diff --cached` = 恰 2 文件（advice +43 / 新测试 +152），未扫入兄弟 35 脏文件；secret 自检 clean。

**② 验收测试（真 HTTP dispatch，防空洞 mock 绿）**
- 新增 `DefectBAdviceAcceptanceTest`（`@Tag("dev")`，`standaloneSetup(真实 AuditLogController).setControllerAdvice(真实 IpdServiceExceptionAdvice)`）——受试者特意选**非白名单**控制器 AuditLogController，正是缺陷B 现场。
```
mvn -o -pl ruoyi-modules/ruoyi-ipd -Dtest=DefectBAdviceAcceptanceTest test
→ Tests run: 5, Failures: 0, Errors: 0, Skipped: 0  BUILD SUCCESS @15:33:56（active builds 0）
  日志实证："[IPD] request body not readable: JSON parse error" → DEF-2 走真实分发命中 handleNotReadable
```
- 5 例：`IpdPermissionException(401)`→401/20001、`(403)`→403/30001、`NotPermission`→403/30001、`NotRole`→403/30001、body 不可读→400/10001。**Skipped=0** 排除假绿静默跳过。

**③ 全量回归被兄弟 WIP 阻断（诚实标注 + 静态零风险证）**
- 修后欲跑 `-Dtest='*AcceptanceTest'` 全量回归，@15:35:21 与 @15:36:37 两次 **BUILD FAILURE：`LegacyImportService.java:134-136` 找不到 `StageAction.setRemark/getRemark`**。
- **归因（非我）**：`LegacyImportService.java`=**未跟踪新文件(??)**、`StageAction.java`=**脏(M) 且无 remark 访问器** → 兄弟正加「历史导入」特性、消费者先于生产者字段落盘的 WIP 半成品；我的 `6628ab3b` 仅动 advice+test 两文件，零涉及。属记忆 0fee5448「WIP 源码错配型假红」子类。
- **静态回归零风险证**（替代被阻断的动态跑）：全仓仅 `Api01AcceptanceTest` 断言 500，用 `ServiceException(未知码)` + **纯 `RuntimeException`**（均非我 5 类，仍走未改动的 `handleServiceException`/`handleUnexpected`）；`Sec01/Sec02` 断言的是 `IpdPermissionException` **对象本身**（非 advice HTTP 映射，未改 IpdPermission）；`P064AcceptanceTest` 用**自带局部 stub advice** 断言 `IpdPermissionException(401/403)`→401/403，**与我的生产映射完全一致 = 独立佐证**。→ 无一现存测试对我重映射的 5 类断言 500，回归风险 = 0。
- 残留：待兄弟补 `StageAction.remark` 使主源可编译后，复跑 `mvn -o -pl ruoyi-modules/ruoyi-ipd -Dtest='*AcceptanceTest' test` 作动态终证（PARTIAL-closure）。

**④ 缺陷 A-audit 仍 BLOCKED**：`IpdRolePermissionCatalog.java` 全程 **脏(M)**（兄弟 system-config 三码 WIP 未提交），我的 audit-log 补码 hunk 与其同处 ADMIN_WRITE 区、`git add -p` 不可分离 → 编辑会扫入其未提交改动，违单一写入者纪律。延后至 Catalog 净窗口或归 owner。§4.2 结论不变。

**⑤ DEF-1 升级为全闭环**：兄弟 `7b13a409`「矩阵 v6 **62/62**、超管建要素 **HTTP 200** + 审计 **JSON_VALID=3/3**」已补上 §8③ 所缺的**真库 HTTP 端到端复验**（app 已重启载新码，不再 STALE）。→ DEF-1 由 §8 的 ✅FIXED(PARTIAL-closure) 升级为 ✅**FULL-CLOSED**。

**⑥ 判定更新（本节为最新事实源，取代 §0/§4.2/§4.3 对应行）**

| 项 | §0–§8 旧判 | §9 新判 |
|---|---|---|
| 缺陷B（advice 权限拒绝落 500） | ❌OPEN（§4.3 双实证） | ✅**FIXED+VERIFIED**（6628ab3b，5/5 真 HTTP 绿） |
| DEF-2（body 不可读落 500） | ❌OPEN | ✅**FIXED**（handleNotReadable→400，测试锁死） |
| DEF-1（GateElement 审计明文写 JSON 列） | ✅FIXED / PARTIAL-closure（§8） | ✅**FULL-CLOSED**（7b13a409 真库 HTTP 62/62+JSON_VALID 3/3） |
| 缺陷A-audit（Catalog 缺 audit-log 码） | ❌OPEN | ❌OPEN-**BLOCKED**（Catalog 脏，延后/移交 owner） |
| 全量 *AcceptanceTest 回归 | — | ⚠**PARTIAL**（兄弟 WIP 编译阻断；静态零风险证已附，动态终证待主源可编译） |

**⑦ 回归动态终证补记（15:39:44）— PARTIAL → COMPLETE**
③ 的编译阻断在 15:36→15:39 间被兄弟自愈（`StageAction` 补上 remark 访问器，与 `LegacyImportService` 转为 WIP-一致、主源可编译；二者仍脏/未跟踪但不再冲突）。错峰复跑全量：
```
mvn -o -pl ruoyi-modules/ruoyi-ipd -Dtest='*AcceptanceTest' test
→ Tests run: 189, Failures: 0, Errors: 0, Skipped: 0  BUILD SUCCESS @15:39:44（active builds 0）
```
- 含 `DefectBAdviceAcceptanceTest` 5/5、`Api01` 4/4、`P064` 10/10、`Sec01` 13/13、`P054` 9/9 等 22 个验收类全绿；**Skipped=0** 排除假绿。③ 的静态零风险证被动态终证坐实——缺陷B advice 改动对 189 条现存验收零回归。
- **判定再更新**：§9⑥ 表「全量 *AcceptanceTest 回归」⚠PARTIAL → ✅**COMPLETE（189/189 绿）**。缺陷B 收口证据链完整：单类 5/5（15:33:56）+ 全量 189/189（15:39:44）双时间戳。
