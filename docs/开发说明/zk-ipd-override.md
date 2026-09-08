# ZK-IPD 设计稿对照与裁决索引（2026-09-07）

> **本文件定位**：ZK-IPD 设计稿与本仓二开 SSOT 的**裁决登记索引**，不是 SSOT 切换声明。
>
> **真实 SSOT 现状**（2026-09-07 盘点）：
> 1. `/Users/mac/Documents/ruoyi-ai/README-IPD-OVERRIDE.md`（23 KB，2026-09-07 更新）——自称"优先级高于根 README"，是**当前二开第一道门**，未退场。
> 2. `/Users/mac/Documents/ruoyi-ai/docs/开发说明/开发说明书.md`（8 KB / 7122 行，spec/）——目标 IPD 系统产品设计。
> 3. `/Users/mac/Documents/ZK-IPD/产品流程细化管理工具 2/` ——**新设计稿对照源**，包含：
>    - `AGENTS.md`（59 行 durable workflow decisions，2026-09-03）
>    - `IPD产品经理管理系统·最终完整版AI开发Prompt（全规则闭环无遗留疑问）.md`（182 行，2026-09-02）
>    - `README.md`（2026-09-02）
> 4. `/Users/mac/Documents/ruoyi-ai/docs/ipd-系统说明/开发计划-看板镜像.md`（436 KB）——工程执行与验收看板。

## 一、ZK-IPD AGENTS 行 → 本仓文件映射（已对齐裁决）

| ZK AGENTS 行号 | 主题 | 本仓对应文件 | 现状 | 裁决 |
|---|---|---|---|---|
| L20 | 三角色权限（超管 / 产品组长 / 产品经理） | `docs/开发说明/开发说明书.md` + 后端 RBAC | ✅ 已实现 | 不动 |
| L21 | 五大 Gate（Go/No-Go / 差异化确认 / 开发双周评审 / GTM 就绪 / 90 天复盘） | `ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/service/gate/` | ✅ 后端 5 Gate | 不动 |
| L22 | 项目可见性强制 API | `service/ProjectService.java` 权限注解 | ✅ 已实现 | 不动 |
| L23 | 项目经理删除软删 + 产品组长审批 | `controller/IpdDeletionController.java` + `service/DeletionService.java` | ✅ P0-9.1 run8 79/79 ALLPASS | 不动 |
| L24 | 超管唯一性 + 交接审计 | `service/SuperAdminService.java` + 审计链 | ✅ DEF-4 修复存续 | 不动 |
| L25 | PM/组长只读同步源（L1-L5） | `service/HrSyncService.java` + `controller/HrSyncController.java` | 🟡 Mock 占位 | 登记为"二期替换" |
| L26 | 合并工作台为"我的工作台" | `apps/web-antd/src/views/ipd/workbench/index.vue` | ✅ 已合并 | 不动 |
| L27 | 阶段/模板顺序 + 字段级质量检查 | `service/StageEngine.java` + `service/ActionCatalog.java` | 🟡 SOP V2 37 vs 后端 69 冲突 | 登记为待裁决 P2 项 |
| **L28** | **"阶段确认"替换 Gate/Reviewer 术语（UI）** | `apps/web-antd/src/api/ipd/gate-review.ts` 等 | ⚠️ 前端术语未改 | **裁决：改文案层（视图 + 测试断言），Java 包名保留** |
| L29 | 需求附件 + 六阶段动作绑定 | `service/RequirementService.java` | ✅ 已实现 | 不动 |
| L30 | 需求变更 + 阶段豁免双方独立审批 | `service/ChangeRequestService.java` | ✅ P0-9.1 验证 | 不动 |
| L31 | 项目移交原子化（PM→PM） | `service/HandoverService.java` | ⚠️ `archiveCompletedHandover` + `getMonthlyAttribution` 端点缺失 | 登记 D-2 待补 |
| L32 | PM 有项目未移交不能停用 | `service/DeactivationGuard.java` | ✅ 已实现 | 不动 |
| L33 | Windows 启动器 `打开IPD工作台.cmd` | macOS 仓不适用（基线 RuoYi-AI） | n/a | 不动 |
| L34 | Dual-PM 可选叠加层 | `service/DualPmService.java` | ✅ P2-7 部分实现 | 不动 |
| L35 | Dual-PM 责任边界 | `service/DualPmService.java` | ✅ 已实现 | 不动 |
| **L36** | **Concept Go/No-Go / 需求变更 / 90 天复盘不可豁免；普通豁免双组长+超管** | `service/GateEngine.java` | ⚠️ 后端"超管放行"分支残留 | **裁决：保留"未配置从严"机制，不删超管放行（避免误伤未配置环境）** |
| L37 | KPI 60% 功能 + 40% 共享 | `service/KpiScoringService.java` | ✅ P3-8 部分实现 | 不动 |
| L38 | 津贴/负反馈/贡献/奖金一锁定不可覆盖 | `service/BonusPoolService.java` + `service/KpiSharedConfirmService.java` | ✅ P3-7 ALLPASS | 不动 |
| L39 | SOP 质量字段级定位 | `service/QualityCheckService.java` | ✅ 已实现 | 不动 |
| L40 | PM/组长只读同步 + L1-L5 仅在锁定新项目津贴基础 | `service/HrSyncService.java` | 🟡 Mock | 同 L25 |
| L41 | 招募发起人范围 | `service/RecruitmentService.java` | ✅ 已实现 | 不动 |
| L42 | KPI 证据附件 + 审计红标 | `service/KpiEvidenceService.java` + 审计模块 | ✅ 已实现 | 不动 |
| L43 | 项目创建双路径 | `service/ProjectService.java` | ✅ 已实现 | 不动 |
| **L44** | **SOP V2 = 固定 37 动作目录** | `service/ActionCatalog.java`（后端 69 动作） | ⚠️ 37 vs 69 冲突 | **裁决：先用 69 兼容（ZK V2 是新版设计），待后端按 ZK V2 重写后切换；登记 P2 待裁决** |
| L45 | 保存动作前先保存修订 | `service/RevisionService.java` | ✅ 已实现 | 不动 |
| L46 | 全流程时间线钻取 + 文档库全局 | `controller/TimelineController.java` + `controller/DocumentLibraryController.java` | 🟡 后端已建，前端消费待核 | 登记前端消费状态 |
| L47 | 需求变更 + KPI 协作绩效入口 | `service/RequirementChangeService.java` | ✅ 已实现 | 不动 |
| L48 | 产品持久化主数据 + 需求合并 | `service/ProductService.java` + `service/DemandService.java` | 🟡 后端已建 | 不动 |
| L49 | 市场 PM 招募 + 责任矩阵原子化 | `service/RecruitmentService.java` | ✅ 已实现 | 不动 |
| L50 | 企业微信 OAuth + 演示模式 | `controller/IpdAuthController.java` | ⚠️ 缺 `/wecom/bind` `/wecom/scan` `/wecom/unbind` 三端点 | 登记 B-5 待补 |
| L51 | 产品空间 vs 项目空间分离 | `controller/ProductSpaceController.java` + `controller/ProjectSpaceController.java` | 🟡 后端已建 | 不动 |
| L52 | 产品可见性 + R&D 上下文最小化 | `service/ProductVisibilityService.java` | ✅ 已实现 | 不动 |
| L53 | 游客附件预览 + 需求变更附件可达 | `controller/DemandAttachmentController.java` | ✅ 已实现 | 不动 |
| L54 | 项目圈上下文协作线程 | `controller/ProjectCircleController.java` | ⚠️ ZK L57 已删，本仓后端 7 端点残留 | **裁决：删 ProjectCircleController + 前端 circle.vue + project-circle.ts/api/路由** |
| **L57** | **Project Circle is removed from the product** | 同 L54 | 同 L54 | 同 L54 |
| **L58** | **Governance Center is removed** | `apps/web-antd/src/views/ipd/workbench/index.vue` | ⚠️ 前端含"治理中心"字样 | **裁决：删治理中心字样，删除审批+无产出提醒内嵌到工作台主体** |
| L59 | 招募模式选择卡 | `apps/web-antd/src/views/ipd/recruitment/index.vue` | 🟡 待验 | 不动 |

## 二、本仓未对齐 ZK 的项（已识别，未裁决）

| 编号 | 描述 | 文件 | 影响 |
|---|---|---|---|
| GAP-1 | 前端术语未从 Gate/Reviewer → "阶段确认" 改文案层 | `apps/web-antd/src/api/ipd/gate-review.ts` + `gate-element.ts` + `gate-element-result.ts` + `gate-material.ts` + 对应 `.test.ts` | C-1 范围 |
| GAP-2 | 后端 SOP V2 = 69 动作 vs ZK V2 = 37 动作 | `service/ActionCatalog.java` + `test/.../ActionCatalogTest.java` | D-5 全量回归影响 |
| GAP-3 | 后端 `/wecom/bind` `/wecom/scan` `/wecom/unbind` 三端点缺失 | `controller/IpdAuthController.java` + `service/PersonService.java` | B-5 范围 |
| GAP-4 | 后端 HandoverController 缺 `POST /archive` + `GET /monthly-attribution` | `controller/HandoverController.java` | D-2 范围 |
| GAP-5 | 后端 ProjectCircleController 7 端点残留 | `controller/ProjectCircleController.java` | B-1 / C-2 范围 |
| GAP-6 | 前端 project/detail/circle.vue 残留 | `apps/web-antd/src/views/ipd/project/detail/circle.vue` | C-2 范围 |
| GAP-7 | 前端治理中心字样残留 | `apps/web-antd/src/views/ipd/workbench/index.vue` | C-3 范围 |
| GAP-8 | 前端 bonus.ts 缺 `freezeBonusPool` + `distributeBonusPool` | `apps/web-antd/src/api/ipd/bonus.ts` | C-5 范围 |
| GAP-9 | 后端 WorkbenchService 缺 `getOverdueList` | `service/WorkbenchService.java` | C-6 范围 |
| GAP-10 | HrSyncController Mock 占位 | `controller/HrSyncController.java` | B-6 范围（仅登记） |
| GAP-11 | 看板镜像 P2-3/P2-4/P2-5 行错绑证据（JVM/logback/docs） | `开发计划-看板镜像.md` | B-7 范围 |

## 三、不在本仓范围内的项

- ZK L33（Windows 启动器 `.cmd`）——本仓为 macOS 后端基线 RuoYi-AI，不适用。
- ZK L34 Dual-PM 可选叠加层——已部分实现，验收卡 P2-7 覆盖。

## 四、本索引维护规则

- 本文件只登记"对照关系 + 裁决状态"，不改 SSOT 优先级。
- SSOT 优先级变更必须先动 `README-IPD-OVERRIDE.md`，再回头同步本索引。
- `docs/开发说明/` 仍只允许**勘误级更新**（错字 / 失效引用 / 数字对齐），本索引是登记文件不属于产品设计。
- 任何 GAP 关闭后必须把对应行的"现状"从 ⚠️ 改为 ✅，并在 `docs/ipd-系统说明/log.md` 登记。

## 五、登记人 / 时间

- 登记：EvoX 会话 `plan-fab94db1` revision 3
- 依据证据：ZK-IPD `AGENTS.md` 59 行 + `README-IPD-OVERRIDE.md` 23 KB + 验收目录 11 个 json
- 登记时间：2026-09-07
