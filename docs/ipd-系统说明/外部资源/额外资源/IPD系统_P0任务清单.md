---
source: file:///Users/mac/Documents/ZK-IPD/产品流程细化管理工具/IPD系统_P0任务清单.md
collected: 2026-09-04
published: 2026-09-03 11:14 (file mtime)
topic: P0 阶段具体任务清单
original_size:    15187 bytes /      351 行
source_path: IPD系统_P0任务清单.md
---

# IPD 产品经理管理系统 — P0 底座 任务清单

> 版本：v1　日期：2026-09-03
> 配套：`IPD系统_AI开发主Prompt_v3.md`（规格）、`IPD系统_验收清单.md`（P0 门 27 条）、`IPD系统_开发执行规则_AI必读.md`（项目红线）
> 工作流：使用 **`workbuddy-dev-workflow`** skill 派发；派发前先读本文件第 2 节的共享文件清单
> 环境：本机免 Docker，先双击 `F:\devtools\start-dev-env.bat`，连接参数见 `F:\devtools\README.md`

---

## 1. 怎么用

1. **先读**：主 Prompt §1 技术规格（TS-01 ~ TS-04）、§2 数据模型；项目规则文件第 2 节十条硬约束
2. **T-01 ~ T-03 由主代理自己做**（共享文件，不派子代理）
3. **T-04 起可派子代理**，按第 3 节的并行表派发；每个子代理的 `FILES` 必须互斥
4. 每个任务完成 → 跑 `VERIFY` → 对照 `AC` 打勾 → 提交 → 再派下一个
5. 全部完成 → 过 P0 门（第 6 节）→ `git tag v0.1-P0`

---

## 2. 共享文件清单（主代理独占，子代理**禁止**写）

| 文件 | 原因 |
|---|---|
| `apps/api/prisma/schema.prisma` | 全表唯一来源，多子代理改必冲突 |
| `apps/api/prisma/migrations/**` | migration 顺序敏感 |
| `apps/api/prisma/seed.ts` | 参数默认值唯一来源 |
| `apps/api/src/config/**` | 全局配置 |
| `apps/web/src/locales/zh-CN.ts` | 文案唯一来源（G-07） |
| `apps/web/src/types/**` | 前后端类型契约 |
| 根 `package.json` / `pnpm-workspace.yaml` / `tsconfig*.json` / `.env*` | 工程配置 |
| `docker/docker-compose.yml` | 仅供部署，开发期不运行 |

子代理需要新表/新字段/新文案/新类型 → **回报主代理，由主代理改**，不要在子代理里自行补。

---

## 3. 依赖与并行

```
阶段 A（串行 · 主代理）
  T-01 ──> T-02 ──> T-03
                     │
阶段 B（并行 · 后端，最多 3 路）    │
  T-04 ┬                          │
  T-05 ┼── 均 DEP: T-03 ──────────┘
  T-06 ┤
  T-07 ┤
  T-08 ┘
                     
阶段 C（并行 · 前端，与 B 同时）  
  T-09 ──> T-10   （DEP: T-01，接口用 mock，不阻塞后端）

阶段 D（依赖 B + C）
  T-11 (DEP: T-05, T-07)  ──> T-12 (DEP: T-11, T-09)
  T-13 (DEP: T-06, T-09)
```

**可并行组合**：T-04/T-05/T-06/T-07/T-08 之间 FILES 已隔离，可同时派；T-09 可与它们并行。
**必须串行**：T-01→T-02→T-03；T-11→T-12。

---

## 4. 任务详情

### T-01 monorepo 骨架与环境配置

```
FILES:  根 package.json, pnpm-workspace.yaml, tsconfig.base.json, .gitignore,
        .env.example, docker/docker-compose.yml, docker/nginx.conf,
        apps/api/（NestJS 空壳）, apps/web/（Vite+React 空壳）
STEPS:
  1. pnpm workspace 初始化；apps/api 用 NestJS CLI 生成空壳，apps/web 用 Vite react-ts 模板
  2. .env.example + .env（二者职责不同，不可混为一谈）：
     - .env.example（**入库**）：只写 <...> 占位符，**绝不写真实密码**——模板进版本库，凭据不入库。
       须含 compose 用 ${VAR:?} 强校验的全部变量：
       POSTGRES_DB / POSTGRES_USER / POSTGRES_PASSWORD / IPD_APP_PASSWORD / REDIS_PASSWORD
       注释里用**假密码**演示 @ → %40 编码规则
     - .env（**gitignore，不入库**）：本机开发真实值，由主代理生成，使 T-02/T-03 开箱即用。
       真实凭据来源 F:\devtools\README.md
     - DATABASE_URL=ipd_app（运行时，非超级用户）｜DIRECT_URL=postgres（仅迁移）
       双账号分离见主 Prompt **TS-01c** 与 **G-11**
  3. docker/docker-compose.yml 编写完整（postgres:15-alpine / redis:7-alpine / minio / api / web / nginx）
     —— 仅用于生产部署，README 中注明开发期不执行 docker compose up
  4. .gitignore 排除 node_modules/dist/.env/uploads
VERIFY: pnpm install 成功 → pnpm -F api build 通过 → pnpm -F web build 通过
AC:     AC-ENV-01, AC-ENV-05
DEP:    无
```

> ⚠️ 本任务**不派子代理**。

---

### T-02 Prisma 全量表定义

```
FILES:  apps/api/prisma/schema.prisma
STEPS:
  1. 按主 Prompt §2 数据模型建全部 20 张表；所有表含 id/created_at/updated_at/deleted_at/created_by/updated_by
  2. 金额字段一律 DECIMAL(18,2)，禁止 float；日期一律 TIMESTAMPTZ
  3. 审计日志表 **不设 updated_at**（只追加设计，AC-AUD-07）
  4. 枚举落 PG enum 或字典表，禁止代码内硬编码
VERIFY: pnpm prisma validate 通过 → pnpm prisma format 无 diff
AC:     AC-CFG-01（配合 T-05）
DEP:    T-01
```

> ⚠️ 本任务**不派子代理**。

---

### T-03 migration + seed + 审计表权限

```
FILES:  apps/api/prisma/migrations/**, apps/api/prisma/seed.ts
STEPS:
  1. 生成初始 migration（prisma migrate 走 DIRECT_URL / postgres）
  2. migration 中追加 SQL，针对**运行时账号**收权（AC-AUD-01）：
       REVOKE UPDATE, DELETE ON audit_logs FROM ipd_app;
     —— 注意：revoke 对象是 ipd_app，不是 postgres（超级用户无视 revoke）
  3. seed：默认超管账号（独立凭证，非 PM 姓名规则）、默认产品组、角色字典、
     系统参数表全部默认值（按主 Prompt §7 参数表逐条写入，**51 行**基线
     （2026-09-03 实测修正：原写 37 项系旧口径），另含 11 个 schema 注释裁定的
     「建议新增键」，合计 62 键，一项不能漏）
     —— seed 属运行时写入，**用 DATABASE_URL / ipd_app 执行**，禁止走 DIRECT_URL（G-11）
VERIFY: pnpm prisma migrate deploy 成功 → db:constraints 成功 → seed 执行成功（跑两遍验幂等）
        → psql 查 system_configs 有 62 行 → db:verify 全部通过
        → **以 ipd_app 身份执行 UPDATE audit_logs SET ... 必须被拒绝**
AC:     AC-ENV-02, AC-AUD-01, AC-AUD-07
DEP:    T-02
```

> ⚠️ 本任务**不派子代理**。
> ⚠️ seed 里的参数默认值**必须与主 Prompt §7 参数表逐行一致**，包括 2026-09-03 裁定的
> `bonus.achievementTiers` 6 档区间制（JSON 以 `min:null` 表最高档兜底）。
> 📌 实施修订（2026-09-04，T-03 落地时裁定）：
> ① 交付物为 `prisma/seed.js`（CommonJS，免构建），非本清单原写的 seed.ts —— 零新依赖、
>    Node 直跑，功能等价；
> ② STEP 2 的 REVOKE **不写入 migration**（全新环境 ipd_app 角色可能尚不存在，会让
>    migrate deploy 直接失败），由 post-migrate-constraints.sql §7.2 承担，实测验证等效；
> ③ 三张模板表中仅 system_configs.configKey 是 partial unique，Prisma upsert 会 42P10，
>    seed 对模板表统一「先查后写」；
> ④ 密码哈希按主 Prompt §6 的 bcrypt(12)（bcryptjs），非 scrypt。

---

### T-04 统一响应 / 异常过滤 / 参数校验管道

```
FILES:  apps/api/src/common/response/*, apps/api/src/common/filters/*,
        apps/api/src/common/pipes/*, apps/api/src/common/interceptors/response.interceptor.ts
STEPS:
  1. 全局响应拦截器：包装为 { code, message, data, timestamp, traceId }
  2. 全局异常过滤器：按主 Prompt TS-09 业务错误码映射（2xxxx 认证 / 3xxxx 权限 / 4xxxx 参数 / 5xxxx 业务）
  3. 全局 ValidationPipe：whitelist + transform + forbidNonWhitelisted
VERIFY: 单测：任意返回值被正确包装；抛 BusinessException(code) 映射为对应 HTTP 状态
AC:     AC-ENV-04（前端依赖此结构）
DEP:    T-01
```

---

### T-05 参数服务（零硬编码，涉配置红线）

```
FILES:  apps/api/src/modules/system-config/*, apps/api/src/common/config/parameter.service.ts
STEPS:
  1. ParameterService：get<T>(key) / getAll() / set()，带内存缓存 + 变更失效
  2. 缓存失效策略：改参数后立即生效，无需重启（AC-CFG-03）
  3. 提供后台参数查询接口（超管可读写，其余角色只读）
VERIFY: 单测：修改参数后 get() 立即返回新值（不重启）
        静态检查：全局搜索 1000/1500/2000/2500/3000/0.05/1.5/7/2 等字面量 → 零命中
AC:     AC-CFG-01, AC-CFG-02, AC-CFG-03
DEP:    T-03
```

> 🔴 **这是 P0 最关键的任务**。所有涉钱系数必须经此服务取值，禁止任何字面量。
> 后续 P3 的奖金/阶梯/权重全部依赖它，此处偷懒会导致 P3 整体返工。

---

### T-06 审计切面 @Audit() + hash 链

```
FILES:  apps/api/src/common/audit/*, apps/api/src/modules/audit-log/*
STEPS:
  1. @Audit(action, entityType) 装饰器 + 全局拦截器：写操作自动落 audit_logs
  2. hash 链：每条记录 hash = SHA256(prevHash + 本条规范化 JSON)，写入 prev_hash 字段
  3. 用户信息从 request.user 取（由 T-07 填充；本任务定义契约，不实现认证）
  4. audit_logs 表无 updated_at，且 PG 层已 revoke UPDATE/DELETE（T-03 完成）
VERIFY: 单测：连续写 3 条 → hash 链可校验；篡改中间一条 → 校验报出断裂点位置
AC:     AC-AUD-02, AC-AUD-03, AC-AUD-07
DEP:    T-03
```

> TDD 必做。hash 链是本项目的合规底座，必须看到测试先失败。

---

### T-07 JWT 认证 + RBAC 守卫

```
FILES:  apps/api/src/modules/auth/*, apps/api/src/common/guards/*
STEPS:
  1. 账密登录（超管独立凭证 + PM 账号=姓名）
  2. JWT Access 15min + Refresh 7d；Refresh 可续期，登出失效
  3. RolesGuard + 数据域守卫：普通PM 仅本人数据 / 组长本组 / 超管全局（AC-AUTH-08~11）
  4. 企微 Mock 登录：已绑定签发 JWT，未绑定拒绝（AC-AUTH-04/05）
  5. 首次登录强制改密：`mustChangePwd` 为真时除改密接口外全部拒绝（AC-AUTH-02）
VERIFY: 单测：改密前访问任意接口返回 2xxxx 强制改密码
        集成：普通PM 调 /api/v1/admin/handover → 3xxxx 且无任何数据变更
AC:     AC-AUTH-01 ~ AC-AUTH-11
DEP:    T-03
```

> 🔴 越权用例（AC-AUTH-08/09）必须写成真实集成测试，**不得只测正常路径**。

---

### T-08 字典接口

```
FILES:  apps/api/src/modules/dict/*
STEPS:
  1. 字典分组查询接口（角色、项目等级、阶段、动作状态、删除审核状态等）
  2. 前端从本接口取枚举，禁止硬编码（TS-03）
VERIFY: 接口返回全部字典分组；前端类型由主代理同步进 apps/web/src/types
AC:     AC-CFG-01（枚举不硬编码）
DEP:    T-03
```

---

### T-09 前端骨架

```
FILES:  apps/web/src/{main.tsx,App.tsx,router/*,layouts/*,services/request.ts,
        stores/*,utils/*}, apps/web/src/components/**
STEPS:
  1. 布局：侧边导航 + 顶栏（Ant Design 5）
  2. 路由 + 权限路由守卫（按 T-07 返回的角色过滤菜单）
  3. 请求封装：统一错误处理、401 跳登录、2xxxx 跳改密、traceId 透传
  4. 字典缓存（Zustand + TanStack Query）
  5. i18n 骨架（文案抽离，G-07）
VERIFY: pnpm -F web build 通过 → 断网时页面显示友好错误而非白屏
AC:     AC-ENV-04
DEP:    T-01
```

> 文案一律引用 `locales/zh-CN.ts`，该文件由主代理维护；子代理只写调用不写内容。

---

### T-10 登录页 / 强制改密页 / 企微 Mock / 登出

```
FILES:  apps/web/src/pages/auth/**
STEPS:
  1. 登录页（账密 + 企微 Mock 扫码入口）
  2. 强制改密页：未改密前路由守卫拦截到其他所有页面
  3. 登出清 token 与字典缓存
VERIFY: 手工：新账号登录 → 强制改密页 → 改密后进入首页；未改密直接输 URL 被拦回
AC:     AC-AUTH-01, AC-AUTH-02, AC-AUTH-04, AC-AUTH-05, AC-AUTH-07
DEP:    T-09, T-07
```

---

### T-11 通用删除审核引擎（后端）

```
FILES:  apps/api/src/modules/deletion/*
STEPS:
  1. 通用引擎：任意实体可接入（entityType + entityId），后续模块直接复用
  2. 两级审核（BR-DEL-01）：组长初审 → 超管终审；普通数据仅一级（BR-DEL-02）
  3. 时效：各级 2 个工作日（`deletion.reviewDays`），逾期提醒并升级（BR-DEL-04）
  4. 撤回：提交后 24 小时内申请人可撤回，写审计（BR-DEL-04）
  5. 驳回：通知申请人，保存理由（BR-DEL-05）
  6. 通过后软删除 + 移入归档区；超管二次确认彻底清除，清除动作再写审计（BR-DEL-03）
  7. 任何人无直接删除权限（BR-DEL-06）
VERIFY: 单测：两级全通过才置 deletedAt；一级数据只需组长；超 24h 不可撤回
AC:     AC-DEL-01 ~ AC-DEL-08（见验收清单 P4，P0 先跑通引擎本身）
DEP:    T-05, T-07
```

> 🔴 状态机必须 TDD。期限、撤回时限全部走参数服务，禁止字面量。

---

### T-12 删除审核页面（前端）

```
FILES:  apps/web/src/pages/deletion/**
STEPS:
  1. 我的申请（含撤回按钮，超 24h 置灰）
  2. 待我审核（组长视图 / 超管视图）
  3. 归档区（超管可见，含二次确认彻底清除）
VERIFY: 手工：发起申请 → 组长审 → 超管审 → 数据进入归档区且业务列表不可见
AC:     AC-DEL-01 ~ AC-DEL-08
DEP:    T-11, T-09
```

---

### T-13 审计日志查询页 + hash 链校验接口

```
FILES:  apps/web/src/pages/audit/**, apps/api/src/modules/audit-log/verify.controller.ts
STEPS:
  1. 查询页：按时间/操作人/实体/动作筛选，分页
  2. 分层导出（AC-AUD-04/05）：普通PM 仅本人相关 / 组长本组 / 超管全局；游客无权限
  3. hash 链校验接口 `/api/v1/audit-logs/verify`：返回是否通过 + 断裂点位置
VERIFY: 手工：篡改一条日志 → 校验接口报出断裂点索引
AC:     AC-AUD-02 ~ AC-AUD-06
DEP:    T-06, T-09
```

---

## 5. P0 的 TDD 边界

| 必做 TDD（先写失败测试） | 不做 TDD |
|---|---|
| 参数服务取值与缓存失效（T-05） | Prisma schema / migration / seed |
| 审计 hash 链与断裂检测（T-06） | DTO、纯 CRUD controller |
| 权限守卫与越权拦截（T-07） | 页面样式与布局 |
| 删除审核状态机（T-11） | i18n 资源、工程配置 |
| 响应包装与错误码映射（T-04） | 依赖安装、脚手架 |

---

## 6. P0 门（全部通过才能进 P1）

1. `pnpm -F api test` 全绿；`pnpm -F web build`、`pnpm -F api build` 通过
2. 验收清单 P0 章节 **27 条**逐条打勾（含新增 AC-ENV-05/06）
3. 全局检索涉钱系数字面量 → 零命中
4. 全局检索 Redis 6.2+ 专有命令 → 零命中
5. 手工跑通主链路：启动 → 超管登录 → 建 PM 账号 → PM 登录被强制改密 → 发起删除申请走完两级审核 → 审计日志可查且 hash 链校验通过
6. `git tag v0.1-P0`

---

## 7. 已知风险

| 风险 | 应对 |
|---|---|
| T-05 参数服务做成硬编码 | P3 全面返工。T-05 完成后立即做静态检查（AC-CFG-01） |
| T-03 seed 参数漏项或数值与 §7 不一致 | 用脚本比对 seed 与参数表行数与键值，不要肉眼核对 |
| 子代理自行补表/改 schema | 违反第 2 节。reveiw 时重点查 `git diff --stat` 是否触及共享文件 |
| Redis 5.0 下 BullMQ 不可用 | P0 不引入 BullMQ（无定时任务），风险推迟到 P2；届时若不可用先升 Redis |
| 并行子代理改同一共享文件 | 第 2 节清单 + review 时查 diff 范围 |
