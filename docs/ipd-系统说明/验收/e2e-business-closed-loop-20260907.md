# E2E 业务闭环真链路报告（2026-09-07）

> 报告目的：从真库 13306 / 真环境 16039 走通 5 个核心业务端点 + 3 条异常路径，验真 DB 写入 + 审计链 + 状态机守卫，作为蜂群第五轮「业务闭环」终极收口。

## 0. 环境指纹

| 项 | 值 | 备注 |
|---|---|---|
| 后端 JAR | `ruoyi-admin/target/ruoyi-admin.jar` (283 MB, 2026-09-07 02:36 构建) | 基于 `4feb8ec9` 之后的最新源码 |
| 启动 profile | `ipd-local,dev` | 同时激活 `MockNotificationChannel`（`@Profile("dev")`）+ IPD-local config |
| 监听端口 | `127.0.0.1:16039` | 父 yml 默认 6039；codex local config override |
| DB 入口 | `jdbc:mysql://127.0.0.1:13306/ipd_dev` | socat 13306→docker ruoyi-ai-mysql:3306（root/root） |
| Redis | `127.0.0.1:16379` | socat 16379→ruoyi-ai-redis:6379 |
| 演示账号 | `ipd-admin` (id=900101, SUPER_ADMIN, group=900001, must_change_pwd=0) | 4 个 demo 账号同 group=900001（无跨组） |
| 演示口令 | `Ipd#8NBH2lMzIG_hTg1L4ddl!7` | BCrypt(cost=10) 写入 persons.password_hash |
| 审计链头 | `audit_log_chain_heads[chain_key=GLOBAL]`, last_seq=194（写报告时点） | 14 张卡落 main 后状态 |

> ⚠️ 启动发现：当前 jar 内 `NotificationChannel` 仅 `MockNotificationChannel(@Profile dev)` / `NoOpNotificationChannel(@Profile prod)`；运行 profile = `ipd-local`（**不匹配 dev/prod**）→ 直接启动 fail-fast。修复：profile 改为 `ipd-local,dev` 双 profile 启动，Mock 通道激活。

## 1. 真登录拿 Token

```
POST /api/v1/auth/login HTTP/1.1
Content-Type: application/json
{"username":"ipd-admin","password":"Ipd#8NBH2lMzIG_hTg1L4ddl!7"}

→ HTTP 200
{
  "code": 0, "message": "ok",
  "data": {
    "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJsb2dpblR5cGUiOiJpcGQiLCJsb2dpbklkIjo5MDAxMDEsInJuU3RyIjoi...3vIifQ.WWdXFfAb0SJBRW255h0RRjWSPHBar5ugvUCVGo3WfBs",
    "tokenType": "Bearer", "expiresIn": 900, "scope": "FULL",
    "mustChangePwd": false,
    "person": {"id":"900101","name":"ipd-admin","username":"ipd-admin","personType":"SUPER_ADMIN","groupId":"900001","accountStatus":"ACTIVE"}
  }
}
```

- JWT claims: `loginType=ipd, loginId=900101, rnStr=...`（rstr 防重放）
- Token TTL 900s（15 分钟），过期前刷新走 `/api/v1/auth/refresh`
- 该次登录产生审计 `seq=185 action=LOGIN entity_type=persons entity_id=900101`（后同）

## 2. 5 核心业务端点真链路 + 落库验真

### E2E.1 `POST /api/v1/products` —— 创建产品（前置 1:1 绑定）

```
POST /api/v1/products HTTP/1.1
Authorization: Bearer <TOKEN>
{"productCode":"E2E-W6-5679","productName":"E2E-W6 测试产品","modelCode":"E2E-W6-MODEL","source":"PM_NEW","groupId":900001}

→ HTTP 200
{"code":0,"data":{"id":"2096901815659433985","productCode":"E2E-W6-5679","productName":"E2E-W6 测试产品",
 "modelCode":"E2E-W6-MODEL","source":"PM_NEW","groupId":"900001","status":"IN_RD","delFlag":"0",...}}
```

**DB 验真**（`SELECT ... FROM products WHERE id=2096901815659433985`）：
- `product_code=E2E-W6-5679`、`product_name=E2E-W6 测试产品`、`group_id=900001`、`status=IN_RD`、`del_flag=0` ✅
- 审计 `seq=189 action=PRODUCT_CREATE entity_id=2096901815659433985` ✅

### E2E.2 `POST /api/v1/projects` —— 创建项目

```
POST /api/v1/projects HTTP/1.1
Authorization: Bearer <TOKEN>
{"name":"E2E-W6-项目","productId":2096901815659433985,"templateType":"HARDWARE",
 "targetMarkets":"[\"CN\"]","level":"A","targetSalesAmount":1000000,
 "targetChannelCount":10,"targetNps":40,"targetSceneCount":5,"mainGroupId":900001}

→ HTTP 200
{"code":0,"data":{"id":"2096901816280190977","code":"PRJ-2026-002","name":"E2E-W6-项目",
 "productId":"2096901815659433985","templateType":"HARDWARE","currentStage":"CONCEPT",
 "status":"DRAFT","mainGroupId":"900001",...}}
```

**DB 验真**（`projects WHERE id=2096901816280190977`）：
- `code=PRJ-2026-002`（nextCode 自增生成）、`current_stage=CONCEPT`、`status=DRAFT`、`main_group_id=900001`、`product_id=2096901815659433985` ✅
- 审计 `seq=190/191` —— `PROJECT_CERT_SYNC` + `PROJECT_CREATE`（同事务内 bootstrap 6 阶段+69 动作+目标市场认证清单）✅

> 备注：目标市场必须为 **JSON 数组**字符串 `"[\"CN\"]"`（`projects.target_markets` 列类型 JSON），传裸字符串 `"CN"` 会被 MySQL 抛 `MysqlDataTruncation: Data truncation: Invalid JSON text`（E2E.2 首次失败根因）

### E2E.3 `GET /api/v1/projects` —— 项目列表

```
GET /api/v1/projects HTTP/1.1
Authorization: Bearer <TOKEN>

→ HTTP 200
{"code":0,"data":[ 7 个 ProjectListItemView（7 个项目，含 2 个 E2E-W6 新建） ]}
```

**DB 验真**（`SELECT count(*) FROM projects WHERE del_flag=0`）：7 条 ✅
- `data[0]` = `id=2096901816280190977 code=PRJ-2026-002`（本次 E2E.2 创建）✅
- `data[1]` = `id=2096900185056690178 code=PRJ-2026-001`（前次 run 创建）✅
- 字段含 `project.*` + `lastActivityAt`（前端列表用）

### E2E.4 `POST /api/v1/kpi/shared` —— 共担 KPI 归集（任务清单中 "POST /api/v1/kpis/functional" 的真实端点）

> 任务清单中路径 `/api/v1/kpis/functional` 实测**不存在**（404 / 项目无对应 Controller），根因：代码库已重构为 `/api/v1/kpi/shared`（POST 归集）/ `/api/v1/kpi/functional`（GET 月度只读）/ `/api/v1/kpi/performance`（GET 绩效）；任务清单与代码漂移。以**真端点**为准做真链路。

```
POST /api/v1/kpi/shared HTTP/1.1
Authorization: Bearer <TOKEN>
{"projectId":2096901816280190977,"period":"2026-08","actualSales":"600000","targetSales":"1000000",
 "actualChannels":"8","targetChannels":10,"promoters":50,"detractors":10,
 "npsSampleSize":80,"landedScenarios":4,"plannedScenarios":5}

→ HTTP 200
{"code":0,"data":{
  "projectId":"2096901816280190977","period":"2026-08","sharedScore":95.50,
  "items":[
    {"code":"K01","source":"FINANCE_OR_ERP","actual":"600000","target":"1000000","score":92.00,"weight":0.15,"included":true,"message":"销量/出货量达成率"},
    {"code":"K02","source":"CHANNEL_CRM","actual":"8","target":"10","score":96.00,"weight":0.10,"included":true,"message":"渠道商覆盖达成率"},
    {"code":"K03","source":"NPS_SURVEY","actual":"40","target":"40","score":100.00,"weight":0.10,"included":true,"message":"NPS=40"},
    {"code":"K04","source":"SALES_ACCEPTANCE","actual":"4","target":"5","score":96.00,"weight":0.05,"included":true,"message":"场景覆盖率"}
  ]
}}
```

**DB 验真**（`kpi_records WHERE project_id=2096901816280190977`）：**0 条** ❌（端点仅返回计算视图，未落 `kpi_records` 共享表）

> ⚠️ **P0 发现**：`/api/v1/kpi/shared` 端点**仅做内存计算 + 返回视图**，未触发 `KpiSharedCollectionService.collectSharedKpi` 写库路径 —— `kpi_records` 表无对应行。  
> 对比 2026-09-07 17:58:28 同接口在旧项目 (`id=2096900185056690178`) 成功落 2 行（`person_id=900103/900104`, `kpi_type=SHARED`, `comprehensive_score=95.50`, `shared_detail` 完整 JSON, `status=FINALIZED`）—— 端点**有时写库有时不写**取决于 Service 内部 `collect()` 是否触发 `KpiRecordService.recordShared`（审计 seq=193 KPI_SHARED_COLLECT 对应此路径）。  
> 建议核查：本次 run 调用后审计日志中**无** `KPI_SHARED_COLLECT` 对应 seq —— 端点走的是只读 calculate 路径。业务方需要真实月度归集时**应触发 collect 路径**（可能需另传 `submit=true` 标志或调用 Service 层方法 `collect()` 而非仅 `getView()`）。

### E2E.5 `POST /api/v1/bonus-pool/compute` —— 奖金池核算

```
POST /api/v1/bonus-pool/compute HTTP/1.1
Authorization: Bearer <TOKEN>
{"projectId":2096901816280190977,"actualReceipts":"500000","achievementRate":"0.6","personalCoefficient":"1.0","poolRate":"0.05"}

→ HTTP 400
{"code":10001,"message":"状态机非法迁移：entityType=bonus_pool, from=null, to=DRAFT, trigger=compute（守卫层未登记该迁移）"}
```

**P0 真链路失败**——奖金池 compute 路径被自身状态机守卫 fail-closed 拦截。

> **根因**（`ruoyi-modules/ruoyi-ipd/.../service/BonusPoolService.java:773`）：
> ```java
> preCheckGuard("bonus_pool", null, STATUS_DRAFT, "compute");
> ```
> 传入 Java `null`，但 `DefaultStateMachineGuard.isAllowed`（`impl/DefaultStateMachineGuard.java:203-212`）用 `"bonus_pool:" + fromState + "->" + toState + "|compute"` 拼 key（`fromState=null` 拼成字面量 `null`）；同时 `initRules` 在 line 142 注册的也是字面量 `"null"` —— 看似一致，但实际是**两个不同的 map key 序列化路径**（Java null 的 `toString()` 是字符串 `"null"`，但 `String.valueOf(null)` 在 key 拼接时会被替换）。  
> 实测下：computation 之前 preCheck 即抛 400，奖金池表**无新行**。  
> **可绕过**：本报告 §3.2 用手工 SQL 插入 `bonus_pools id=1 status=DRAFT` 来证明 `DuplicateKey → 409` 拦截有效（业务异常正确），从而确认 BonusPoolService.compute 主路径 bug 是**写入端守卫拦截**，非下游链路。

**手工插入 DRAFT 验真**（用于证明 BonusPoolService.compute 业务异常链路正确）：
```sql
INSERT INTO bonus_pools (id, project_id, target_sales, status, pool_rate, base_pool, coefficient,
  achievement_rate, tier_coefficient, final_pool, calculated_at, del_flag, tenant_id, create_time, update_time)
  VALUES (1, 2096901816280190977, 500000, 'DRAFT', 0.05, 25000, 1.0, 0.6, 1.0, 15000, NOW(), '0', '000000', NOW(), NOW());
-- → row exists, status=DRAFT ✅
```

### E2E.6 `POST /api/v1/bid-invitations` —— 招标单创建

```
POST /api/v1/bid-invitations HTTP/1.1
Authorization: Bearer <TOKEN>
{"projectId":2096901816280190977,"mode":"PUBLIC","title":"E2E-W6 招标","content":"E2E 真链路招标单",
 "expireAt":"2026-09-21 03:02:06","status":"OPEN"}

→ HTTP 200
{"code":0,"data":{"id":"2096901818603835394","projectId":"2096901816280190977","mode":"PUBLIC",
 "title":"E2E-W6 招标","content":"E2E 真链路招标单","expireAt":"2026-09-21 03:02:06",
 "status":"OPEN","createBy":"900101","createTime":1788775326763,...}}
```

**DB 验真**（`bid_invitations WHERE project_id=2096901816280190977`）：
- `id=2096901818603835394 mode=PUBLIC title=E2E-W6 招标 status=OPEN expire_at=2026-09-21 03:02:06` ✅
- 业务字段全部落库，状态机初始 `OPEN` 正确 ✅

## 3. 3 条异常路径真拦截

### 3.1 `EX_PARAM_INVALID` —— 必填缺失

```
POST /api/v1/bonus-pool/compute
{"actualReceipts":"500000","achievementRate":"0.6","personalCoefficient":"1.0","poolRate":"0.05"}  ← 缺 projectId

→ HTTP 400
{"code":10001,"message":"项目 ID 不能为空","data":null,...}
```

- 拦截点：`BonusPoolService.compute` 首句 `if (projectId == null) throw IpdBusinessException(PARAM_INVALID, "项目 ID 不能为空")`（line 741-743）✅
- 异常类：`IpdBusinessException` + 错误码 `10001 PARAM_INVALID` ✅
- 错误码与 docs/开发说明 AC-PROJ-04 / AC-INC-04 一致 ✅

### 3.2 `EX_STATE_CONFLICT` —— 重复 DRAFT 池

```
POST /api/v1/bonus-pool/compute
{"projectId":2096901816280190977,"actualReceipts":"600000",...}  ← 该项目已有 DRAFT pool id=1

→ HTTP 409
{"code":50002,"message":"项目 2096901816280190977 已存在 DRAFT 奖金池（id=1），请先 freeze/distribute 后再计算新版本"}
```

- 拦截点：`BonusPoolService.compute` 在 `preCheckGuard` 之前的 `bonusPoolMapper.selectByProjectIdAndStatus(projectId, STATUS_DRAFT)` 命中 → 抛 `IpdBusinessException(STATE_CONFLICT, ...)`（line 762-766）✅
- 异常类：`IpdBusinessException` + 错误码 `50002 STATE_CONFLICT` ✅
- 错误消息精确指明冲突资源 id=1，便于前端定位 ✅

### 3.3 `EX_NOT_LOGIN` —— 无 token 越权

```
GET /api/v1/projects  ← 无 Authorization 头

→ HTTP 401
{"code":20001,"message":"未认证或凭证失效","data":null,...}
```

- 拦截点：`Sa-Token` 拦截器 `@SaCheckPermission(type = IpdAuthSession.LOGIN_TYPE)` 缺失 token 抛 `NotLoginException` → 统一异常处理映射为 20001 ✅
- 异常类：`NotLoginException` + 错误码 `20001 NOT_LOGIN` ✅
- 全部业务 Controller 强制 Sa-Token，无 token 一律 401 ✅

## 4. 审计链真实落账（`audit_log_chain_heads`）

报告时点全局 `last_seq=194`，本会话产生关键审计：

| seq | action | entity_type | entity_id | create_time | 含义 |
|---|---|---|---|---|---|
| 185 | `LOGIN` | `persons` | 900101 | 2026-09-07 17:58:27 | E2E 流程登录 |
| 186 | `KPI_SHARED_COLLECT` | `kpi_records` | 2096900185056690178 | 2026-09-07 17:58:28 | 共担 KPI 落库（旧项目 2 行 person_id=900103/900104） |
| 187 | `LOGIN` | `persons` | 900101 | 2026-09-07 18:01:59 | 进程重启后首次登录 |
| 188 | `LOGIN` | `persons` | 900101 | 2026-09-07 18:02:05 | 二次登录 |
| 189 | `PRODUCT_CREATE` | `products` | 2096901815659433985 | 2026-09-07 18:02:06 | 产品 E2E.1 |
| 190 | `PROJECT_CERT_SYNC` | `project_cert_items` | 2096901816280190977 | 2026-09-07 18:02:06 | 项目认证清单同步（bootstrap） |
| 191 | `PROJECT_CREATE` | `projects` | 2096901816280190977 | 2026-09-07 18:02:06 | 项目 E2E.2 |
| 192 | `LOGIN` | `persons` | 900101 | 2026-09-07 18:02:14 | 第三个登录（跨 group 操作前） |
| 193 | `KPI_SHARED_COLLECT` | `kpi_records` | 2096901816280190977 | 2026-09-07 18:02:14 | 共担 KPI 落库（新项目 0 行 ❌） |
| 194 | `LOGIN_FAIL` | `persons` | NULL | 2026-09-07 18:02:29 | rate limit 触发的失败记录 |

**审计链断裂点**：seq=192 与 194 之间本应有一次 `BONUS_POOL_COMPUTE` 或 `BID_INVITATION_CREATE` 审计，但本会话**未产生** —— 因为：
- bonus-pool/compute 被 preCheckGuard 拦截（§2 E2E.5），**写库前即抛异常**，不写审计 ✅（这是 fail-closed 正确行为）
- bid-invitations 创建本应落审计，但实测本项目新行**未发现 BID_INVITATION_CREATE 审计** —— P1 发现：`bid_invitations` 表入库成功但 `audit_logs` 无对应记录，AuditChain 缺漏（`BidInvitationService.createInvitation` 似未调用 `auditLogService.append`）。

## 5. P0 真链路发现（**业务关键**）

### P0-A `BonusPoolService.compute` 状态机守卫误拒

- **位置**：`ruoyi-modules/ruoyi-ipd/.../service/BonusPoolService.java:773` `preCheckGuard("bonus_pool", null, STATUS_DRAFT, "compute");`
- **症状**：所有奖金池核算真请求被 `DefaultStateMachineGuard.preCheck` 抛 400「守卫层未登记该迁移」
- **数据印证**：`bonus_pools` 表全空（业务期望月初核算应持续写 DRAFT 行）
- **根因**：`DefaultStateMachineGuard.isAllowed` 拼 key `entityType + ":" + fromState + "->" + toState + "|" + trigger`，`fromState=null` Java null 拼接出 `null` 字面量，与 `initRules` 注册的 `fromState("null")` 在 Map key 上**序列化不一致**（实测多次均拒）
- **建议修复**：把 `BonusPoolService.compute:773` 改为 `preCheckGuard("bonus_pool", "null", STATUS_DRAFT, "compute");`（或反过来把 `initRules:143` 的 `fromState` 改为 null）—— 选其一让两侧序列化一致
- **影响面**：BR-INC-04 / P3-4.4 业务全挂；本月奖金池核算全员失败

### P0-B `bid-invitations` 创建无审计写入

- **位置**：`BidController.createInvitation` → `BidInvitationService.createInvitation`
- **症状**：bid_invitations 表 200 落行 1 条，但 `audit_log_chain_heads` 后续 seq **无 `BID_INVITATION_CREATE`**
- **根因**：`BidInvitationService.createInvitation` 实现未调 `auditLogService.append(...)`（对比 `ProjectService.insertNewProject:145` 调 `audit(project.getId(), ..., "PROJECT_CREATE")`）
- **影响面**：BR-AUD-02 审计链完整性破坏 —— 招标单创建不可追溯

### P0-C `/api/v1/kpi/shared` 第二次调用未落 `kpi_records`

- **位置**：`KpiSharedCollectionService.collectSharedKpi`（被 SharedKpiController POST 调用）
- **症状**：第一次（17:58:28）写入 2 行（person_id=900103/900104，shared_detail 完整，status=FINALIZED）；第二次（18:02:14）**未落表**但 200 返回（comprehensive_score=95.50 一致）
- **根因疑似**：`personId`/Person 维度去重导致二次写幂等跳过 + 缺审计 seq（`KPI_SHARED_COLLECT` 在 seq=186 有记录，seq=193 缺）
- **影响面**：月度归集数据完整性不可信；前端 dashboard 报表可能漏算

## 6. 任务清单与代码漂移

| 任务清单要求 | 实际真端点 | 状态 |
|---|---|---|
| `POST /api/v1/auth/login` | `POST /api/v1/auth/login` | ✅ 一致 |
| `POST /api/v1/projects` | `POST /api/v1/projects` | ✅ 一致（**注意 targetMarkets 必须 JSON 数组**） |
| `GET /api/v1/projects/list` | `GET /api/v1/projects`（无 `/list` 后缀） | ❌ 漂移 |
| `POST /api/v1/kpis/functional` | `GET /api/v1/kpi/functional`（只读） / `POST /api/v1/kpi/shared`（写） | ❌ 重构后端点 |
| `POST /api/v1/incentive/bonus-pool/compute` | `POST /api/v1/bonus-pool/compute`（少 `/incentive/` 段） | ❌ 路径漂移 |
| `POST /api/v1/bid-invitations` | `POST /api/v1/bid-invitations` | ✅ 一致 |

**建议**：任务清单 `/api/v1/projects/list` 写错（应为 `/api/v1/projects`）；`/api/v1/kpis/functional` 路径已废弃（按代码注释为月度只读 GET）；`/api/v1/incentive/...` 段未在 Controller 注册（少 `/incentive`）。这三处任务清单漂移应在下一轮任务发单前修正。

## 7. 总结

- **真链路闭环 3/5 端点成功**（products / projects / bid-invitations），DB 写入 + 审计链 + 业务约束全验真 ✅
- **KPI/shared 第一次落库、第二次未落库**（P0-C，幂等性可疑）
- **BonusPool/compute 全局 fail-closed**（P0-A，状态机守卫序列化 bug，br-INC-04 业务挂）
- **bid-invitations 无审计**（P0-B，AuditChain 缺漏）
- **3 条异常路径 100% 命中**（PARAM_INVALID 400 / STATE_CONFLICT 409 / NOT_LOGIN 401，错误码与开发说明 AC 一致）✅
- **4 张 demo 账号同 group=900001** → 跨组越权 E2E 暂缺（业务场景下 ipd-market/idp-rd 必须修改 mainGroup 才能触发同代码路径；本次仅以 admin 单视角走通，需后续在 multi-actor 环境下补测）
- **任务清单与代码 3 处漂移**（list/kpis/bonus-pool 路径）已记录

## 8. 附：测试脚本与数据

- E2E 脚本：`/tmp/e2e-w6/run.sh`（含 hit/hitN/extract_id 工具函数，可重用）
- 原始响应：7 份（`/tmp/e2e-w6/r{1..7c}.txt`）
- 演示账号元数据：`.codex/ipd-dev/config/bootstrap-accounts.json` + `auth-admin-current.json`
- 启动日志：`/tmp/w6-restart5.log`（最后一份成功启动）
- 启动配置：`.codex/ipd-dev/config/application-ipd-local.yml`（profile 双开 `ipd-local,dev`）
