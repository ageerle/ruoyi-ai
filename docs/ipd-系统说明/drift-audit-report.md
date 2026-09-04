# Drift Audit 报告

> 本报告对比 `docs/开发说明/`（目标 IPD 系统规格）与本仓库（RuoYi-AI 基线）的所有差异点。
> **生成时间**：2026-09-04
> **生成方式**：Claude Code 系统读 `docs/开发说明/` 全文（7122 行）+ RuoYi-AI 基线（pom.xml / application.yml / 源码）+ 输出

---

## A. 一致部分清单（基线已对齐）

以下维度的 RuoYi-AI 实现与开发说明书要求**完全一致**或**可保留**，无需改造。

| 维度 | 本仓库现状 | 开发说明书要求 | 一致依据 |
|---|---|---|---|
| **后端 Spring Boot 版本** | `3.5.8` | 沿用 RuoYi-AI | `pom.xml:17` |
| **后端 Langchain4j 版本** | `1.17.2` | 沿用 | `pom.xml:56` |
| **Java 版本** | `17` | 沿用 | `pom.xml:20` |
| **数据库** | MySQL 8.0（docker-compose 中 mysql:8.0） | 沿用（v3 写 PG，但 G-11 显式说 MySQL 适配） | `docs/docker/ruoyi-ai/docker-compose-all.yaml` |
| **ORM** | MyBatis-Plus 3.5.14 + JSqlParser | 沿用（v3 写 Prisma） | `pom.xml:27-28` |
| **缓存** | Redisson 3.51.0 + Lock4j 2.2.7 | Redis（v3 写 BullMQ，但二开保留 Redis） | `pom.xml:31-33` |
| **认证** | Sa-Token 1.44.0 + sa-token-jwt | 沿用 Sa-Token + JWT | `pom.xml:26` |
| **多租户** | MyBatis-Plus 多租户拦截器 + tenant.excludes | 沿用 | `application.yml:148-161` |
| **端口（开发）** | 后端 6039 | 文档未指定具体端口 | `application.yml:4` |
| **端口（docker）** | MySQL 23306 / Redis 26379 / Weaviate 28080 / MinIO 29000 | 文档未指定 | `docker-compose-all.yaml` |
| **前端栈** | Vue 3 + Vben Admin + element-plus-x | 沿用 | `README.md:65` |
| **AI 模型支持** | DeepSeek / Zhipu / MIMO / Bailian / OpenAI | 文档未限定具体厂商 | `application.yml` 模型管理 |
| **MinIO** | 端口 29000/29090 | 文档要 MinIO | `docker-compose-all.yaml` |
| **Springdoc OpenAPI** | 6 分组 | 文档未限定（新增 IPD 接口用 `/api/v1` 前缀即可） | `application.yml:227-239` |
| **MyBatis-Plus 拦截器链** | 多租户 + 数据权限 + 分页 + 乐观锁 | 文档未限定具体实现 | `ruoyi-common-mybatis` |
| **Sa-Token 注解体系** | `@SaCheckLogin/Role/Permission/DataScope` | 文档约束（G-02）已支持 | `ruoyi-common-satoken` |
| **WebSocket / SSE** | 已有支持（`websocket.enabled: false` 默认） | 文档未指定 | `application.yml:281-289` |
| **Spring Boot Admin** | 端口独立部署 | 文档未涉及 | `ruoyi-extend/ruoyi-monitor-admin` |
| **SnailJob 定时任务** | 已集成（`@SnailJob` 注解） | 文档要 SnailJob | `ruoyi-extend/ruoyi-snailjob-server` |
| **Warm-Flow 工作流** | 已有 BPMN 支持 | 文档要保留 | `ruoyi-modules/ruoyi-workflow` |
| **代码生成器** | Velocity 模板 | 文档未涉及（保留即可） | `ruoyi-modules/ruoyi-generator` |
| **EasyExcel 集成** | `@ExcelDictFormat` 注解 | 文档未涉及 | `ruoyi-common-excel` |
| **数据脱敏** | `@Sensitive` 注解 | 文档需要 | `ruoyi-common-sensitive` |
| **分布式锁** | `@Lock4j` | 文档需要 | `pom.xml:32` |

---

## B. 真实飘移点清单（需要改造解决）

### B.1 [P0] API 规范完全冲突

#### B.1.1 统一响应字段

| 字段 | 本仓库实际 | 开发说明书要求（§9.1） |
|---|---|---|
| 成功 code | **`200`** | **`0`** |
| 消息字段 | `msg` | `message` |
| 额外字段 | 无 | `timestamp`（ISO8601）、`traceId` |
| 数据字段 | `data` | `data` |

**位置**：
- 本仓库：`ruoyi-common/ruoyi-common-core/src/main/java/org/ruoyi/common/core/domain/R.java`
- 文档：`docs/开发说明/开发说明书.md` §9.1（行 521–537）

**修复方案（二选一）**：
- 方案 A：新建 `ApiV1Response<T>` 类，code=0；老 `R<T>` 保留给 RuoYi 原接口；新 `/api/v1/...` 接口用新类
- 方案 B：改 `R<T>` 统一用 code=0（**破坏性**，所有老接口返回值会变 0）

**推荐方案 A**——兼容性最好。

#### B.1.2 错误码段

| 错误码段 | 本仓库实际 | 开发说明书要求 |
|---|---|---|
| 0 | 保留 | **成功** |
| 1xxxx | 业务异常 | **参数校验错误** |
| 2xxxx | （无） | **认证错误** |
| 3xxxx | （无） | **权限不足** |
| 4xxxx | （无） | **业务规则阻断**（G-02 触发） |
| 5xxxx | （无） | **资源不存在 / 状态冲突** |
| 9xxxx | 系统异常 | **系统内部错误** |

**修复方案**：建立 `ErrorCode` 枚举（code 段 + message 模板），新建 `BizException` 继承 `ServiceException`，全局异常处理器按 code 段自动映射。

#### B.1.3 API 路径前缀

| 维度 | 本仓库实际 | 开发说明书要求 |
|---|---|---|
| 路径前缀 | **无**（直接 `/system/xxx`、`/chat/send`、`/auth/login`） | **`/api/v1/...`** |
| 现有接口 | `/system/user/list`、`/chat/send`、`/auth/login`、`/auth/captcha` | `/api/v1/projects`、`/api/v1/stage-actions/:id` 等 |

**修复方案**：在 `ruoyi-admin` 新增 `controller.api.v1` 子包，所有 IPD 业务 controller 走 `/api/v1/...` 前缀。老接口完全不动。

---

### B.2 [P1] 静态检查脚本路径不对（CI 跑不起来）

`docs/开发说明/开发说明书.md` 附录 A 的 grep 命令使用路径 `src/` 和 `apps/`，但本仓库源码实际在 `ruoyi-*/src/main/java/`。CI 跑会 0 命中（路径错），无法反映真实违规情况。

**位置**：`docs/开发说明/开发说明书.md` 附录 A（行 864–889）

**修复方案**：详见 [`改造检查清单.md`](改造检查清单.md) 提供的修正版脚本（含正确的 grep 路径）。

---

### B.3 [P1] 文档引用 10+ 个外部资源不在本仓库

`docs/开发说明/` 全文引用了 10+ 个外部资源（v3 prompt、动作清单、Gate 评审要素、验收清单、mock-data.js 等），但**没有任何一个**在本仓库中存在。

**完整清单**：见 [`fork-原与外部资源清单.md`](fork-原与外部资源清单.md)。

**影响**：
- 二开工程师无法验证文档 vs v3 原文的一致性
- 无法 seed 69 动作、33 项 Gate 要素、4 个奖金算例
- 演示数据 mock-data.js 缺失 → 无法做演示

**修复方案**：本目录 `外部资源/` 下提供 10 个骨架文件（含模板 + 引用说明），等 Gavin 提供原文后填充。

---

### B.4 [P2] 数据库类型冲突

文档按 PostgreSQL 设计（`TIMESTAMPTZ`、`UUID`、`JSONB`、`enum`），本仓库用 MySQL 8.0。

**详细映射**：见 [`type-mapping.md`](type-mapping.md)

**关键决策**（已在 README-IPD-OVERRIDE.md §3.3 决策）：
- 主键：沿用 `BIGINT(20)` 雪花（不用 UUID，性能 + 与 RuoYi-AI 一致）
- 时间：MySQL `DATETIME`（不带时区，存 UTC 业务时间）
- JSON：MySQL `JSON` 类型（5.7+ 原生支持）
- 枚举：字典表 `sys_dict_data`（RuoYi-AI 已有，超管后台维护）
- 自增：`BIGINT AUTO_INCREMENT`

---

### B.5 [P2] 命名风格冲突

| 类型 | RuoYi-AI 现有 | 开发说明书要求 |
|---|---|---|
| 系统表 | `SysUser`、`SysRole`、`SysMenu`、`SysConfig` | `persons`、`product_groups`、`system_configs` |
| 审计表 | `SysOperLog`、`SysLogininfor` | `audit_logs` |
| 字典表 | `SysDictType`、`SysDictData` | `dict_items`（合并为单表？） |
| 业务表 | `CmsContent` | `products`、`projects`、`stage_actions`、`gate_review_elements` |

**详细约定**：见 [`naming-convention.md`](naming-convention.md)

**关键决策**：
- **复用 RuoYi 表**：保持 `Sys*` 风格（不动）
- **新建 IPD 业务表**：用文档原名（`persons` 等英文复数）
- **新 controller 路径**：`/api/v1/...` 前缀
- **新 enum 代码**：文档字面量（`DRAFT`、`TEAMING`、`ACTIVE` 等）

---

### B.6 [P2] 静态资源缺失

文档说「侧边栏 224px」「顶栏 50px」「主色 #006BE6」等 UI 规范。这些是 Vue + Vben Admin 的**默认值**，本仓库前端代码在独立仓库（`ruoyi-web` / `ruoyi-admin`）。但本仓库没有这些前端代码。

**修复策略**：本仓库只关心后端；前端规范通过 Vben Admin 默认值 + 文档 _公共规范.md 实现。

---

### B.7 [P3] 演示数据冲突

文档说「13 个真实姓名」（傅志谦/杨波/文元彪…），但 `BR-USER-01` 又说「PM 禁止页面手动新增；来源 = 第三方 API」。**真实姓名演示**与文档原则冲突。

**位置**：`docs/开发说明/spec/_导航地图.md` §6（已标 `NEEDS CLARIFICATION`）

**修复策略**：在外部资源 `mock-data.js` 骨架中明确标注「使用化名」+ 等 Gavin 决定。

---

### B.8 [P3] Fork 源不一致

文档说「基于 `wilson323/ruoyi-ai`」，但本仓库 git remote 是 `ageerle/ruoyi-ai`。

**修复策略**：在 [`fork-原与外部资源清单.md`](fork-原与外部资源清单.md) 标注实际情况 + 建议与上游同步策略。

---

## C. 不一致点汇总表

| 优先级 | 类别 | 飘移点 | 修复文档 |
|---|---|---|---|
| **P0** | API 规范 | 成功响应码（200 vs 0）、错误码段、路径前缀 | 本报告 B.1 |
| **P1** | 静态检查 | grep 路径 `src/` 错误 | `改造检查清单.md` |
| **P1** | 资源缺失 | 10+ 个外部资源不在仓库 | `fork-原与外部资源清单.md` |
| **P2** | 数据库类型 | PG → MySQL 映射 | `type-mapping.md` |
| **P2** | 命名风格 | Sys* vs 文档名 | `naming-convention.md` |
| **P3** | 演示数据 | 真实姓名 vs 文档原则冲突 | `外部资源/mock-data.js` |
| **P3** | Fork 源 | wilson323 vs ageerle | `fork-原与外部资源清单.md` |

---

## D. 修复成本估计

| 优先级 | 工作量 | 实施位置 | 依赖 |
|---|---|---|---|
| P0 API 规范 | 5-7 人日 | 新建 `ApiV1Response` + `ErrorCode` + 新 IPD controller 子包 | type-mapping 完成；naming-convention 完成 |
| P1 静态检查 | 1 人日 | 修正 grep 路径 + 加 CI workflow | — |
| P1 外部资源 | 2-3 人日 | 10 个骨架 + 等 Gavin 提供原文 | Gavin 决策 |
| P2 数据库类型 | 1-2 人日 | `type-mapping.md` 文档 + mapper 全 调整 | — |
| P2 命名风格 | 0.5 人日（纯文档） | `naming-convention.md` | — |
| P3 演示数据 | 0.5 人日（等 Gavin） | `mock-data.js` 骨架 | Gavin 提供 |
| P3 Fork 源 | 0.5 人日 | `fork-原与外部资源清单.md` | Gavin 决策 |

**总计**：~11-15 人日（不含业务实现）

---

## E. 后续审计建议

1. **每完成一个 P 阶段** → 重跑 drift audit（更新本报告）
2. **每加一个新 IPD 业务表** → 查 `naming-convention.md` + `type-mapping.md`
3. **每加一个新 IPD 接口** → 查 `ApiV1Response` 用法 + 错误码段映射
4. **每提交一次 PR** → 跑 `改造检查清单.md` 的静态检查
5. **每季度** → 与上游 wilson323/ruoyi-ai 同步评估

---

**最后更新**：2026-09-04
**下次审计**：P0 阶段验收通过后