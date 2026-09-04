# ⚠️ 项目改造方向说明（README-IPD-OVERRIDE）

> **本文优先级高于根目录 `README.md`**——本仓库正从 RuoYi-AI 改造为 **IPD 产品经理管理系统**，所有二开工程师必须先读本文件。

---

## 一、为什么有这个文件

本仓库原始来源是 [`wilson323/ruoyi-ai`](https://github.com/wilson323/ruoyi-ai)（一份开源的「企业级 AI 助手平台」Spring Boot + Langchain4j 项目）。在此基线上，二开团队（用户身份：Gavin）将整个项目改造为一个全新的产品——**「IPD 产品经理管理系统」**，服务于熵基科技内部市场PM/研发PM 的产品研发协同与激励核算。

**改造不是另起炉灶，是 fork 后深度改造**。这是为什么仓库里同时存在：

| 内容 | 说明 |
|---|---|
| **RuoYi-AI 原始代码**（约 1274 个 Java 文件） | 基线，原封不动保留 + 部分复用 |
| **`docs/开发说明/`**（约 7122 行文档） | 目标 IPD 系统的二次开发说明书（产品设计） |
| **`docs/ipd-系统说明/`**（本次新增） | 二开漂移审计 + 改造检查清单 + 适配说明 |
| **`docs/wiki/`**（约 60 个 raw + 21 篇 wiki） | RuoYi-AI 基线的知识库（karpathy-llm-wiki 工作流生成） |
| **本文件（README-IPD-OVERRIDE.md）** | 二开工程师的第一道门 |

**访问者分两类，对应不同读法**：
- 想了解 RuoYi-AI 基线 → 直接读 `README.md`
- 想做 IPD 系统二开 → **必须先读本文件**，再读 `docs/开发说明/`，最后查 `docs/ipd-系统说明/`

---

## 二、改造前后对比（一句话版）

| | **改造前（RuoYi-AI）** | **改造后（IPD 系统）** |
|---|---|---|
| **产品定位** | 企业级 AI 助手平台（多 agent + RAG + 多模型） | 单企业私有部署的 IPD 产品工作平台 |
| **核心场景** | AI 聊天、知识库、AI 工作流编排 | 69 动作 IPD 流程、五大 Gate 双签、KPI 考核、奖金池核算 |
| **用户** | 任何企业 | 市场PM / 研发PM / 产品组长 / 超管 / 游客 |
| **业务核心** | Langchain4j agent + 知识库 + 模型适配 | 双 PM 协同 + 奖金公式 + 审计 hash 链 |
| **前端** | Vue 3 + Vben Admin + element-plus-x（保留） | 同上 + 49 页 IPD 业务页面（待开发） |
| **后端** | Spring Boot 3.5.8 + MyBatis-Plus（保留） | 同上 + IPD 业务表 + 新业务 controller |

---

## 三、改造范围与策略

### 3.1 三层文件分类

| 层 | 处理方式 | 数量 | 示例 |
|---|---|---|---|
| **保留原封不动** | 完全沿用 RuoYi-AI 实现 | 约 90% 的 Java 文件 | `ruoyi-common-core`、`ruoyi-common-mybatis`、`ruoyi-common-redis`、`ruoyi-common-satoken` 等 27 个 common 模块 |
| **改造 / 扩展** | 在原有基础上增加新方法、新 controller、新 service | 部分 | `ruoyi-system` 加 IPD 业务表与 controller；`ruoyi-chat` 加 IPD 文档助手 |
| **全新开发** | 在新模块或新包下从零实现 | 部分 | `ruoyi-ipd`（建议新建模块，含 IPD 业务 20 表 + 49 页对应的 controller/service） |

### 3.2 推荐模块拆分（建议方案）

| 模块名 | 来源 | 说明 |
|---|---|---|
| `ruoyi-admin` | 保留 + 微调 | 端口 6039；增加 `/api/v1` 前缀的 IPD controller；保留 RuoYi AI 助手相关 controller（演示用） |
| `ruoyi-system` | 保留 + 大幅扩展 | RBAC 已有；增加产品组、IPD 业务表（建议新建子包 `system.ipd`） |
| `ruoyi-chat` | 保留 + 扩展 | AI 基础保留；增加 IPD 文档助手 agent 与文档版本链 |
| **`ruoyi-ipd`**（建议新建） | 全新 | 69 动作 / 五大 Gate / KPI / 奖金池 / 需求池 / 删除审核等 IPD 业务核心 |
| `ruoyi-workflow` | 保留 | Warm-Flow 保留；二期可考虑集成 |
| `ruoyi-aiflow` | 保留 | 可与 `ruoyi-ipd` 集成做 IPD 流程的可视化编辑器 |
| `ruoyi-generator` | 保留 | 沿用 |
| `ruoyi-common` | 保留 + 复用 | 27 个子模块全部沿用 |

### 3.3 数据库迁移策略

本仓库用 MySQL 8.0（保留 RuoYi-AI 基线）。开发说明书按 PostgreSQL 设计的字段（`TIMESTAMPTZ`、`UUID`、`JSONB`、`enum`）需做适配。详见 [`docs/ipd-系统说明/type-mapping.md`](docs/ipd-系统说明/type-mapping.md)。

**关键决策**：

| v3 设计 | MySQL 适配 | 决策依据 |
|---|---|---|
| `UUID` 主键 | **沿用 `BIGINT(20)` 雪花算法**（RuoYi-AI 基线） | 与现有 RuoYi 体系一致；UUID 索引性能差 |
| `TIMESTAMPTZ` | **`DATETIME`**（不带时区，存 UTC 业务时间） | MySQL 默认行为；应用层做时区转换 |
| `JSONB` | **`JSON`**（MySQL 5.7+ 原生 JSON） | 功能对等；查询略弱但够用 |
| `enum` | **字典表 `sys_dict_data`**（RuoYi-AI 已有） | 便于超管后台维护，符合 G-05 参数可配置原则 |
| `BIGSERIAL` | **`BIGINT AUTO_INCREMENT`** | MySQL 等价物 |

### 3.4 命名风格策略

| 类型 | 命名规则 | 决策依据 |
|---|---|---|
| **复用 RuoYi 表** | `SysXxx` 风格（沿用 RuoYi-AI 命名） | 已存在不动 |
| **新建 IPD 业务表** | 文档原名（`persons`、`products`、`projects`、`stage_actions` 等英文复数） | 文档术语表强制；与 v3 一致 |
| **新 controller 路径** | `/api/v1/...` 前缀（开发说明书 §9 要求） | 与现有 RuoYi 路径分离，避免冲突 |
| **统一响应** | 新增 `R<T>` 子类 `ApiV1Response`：`code=0` 成功（开发说明书 §9.1） | 与现有 `R<T>`（`code=200` 成功）共存；老接口用老的，新接口用新的 |
| **enum 代码** | 文档字面量（`DRAFT`、`TEAMING`、`ACTIVE`、`SUSPENDED`、`ARCHIVED`） | 与 v3 TS-07 一致 |

---

## 四、改造文档地图

```
仓库根
├── README.md                            ← 原始 RuoYi-AI 介绍（改造前基线）
├── README-IPD-OVERRIDE.md               ← 本文件：改造方向说明（你在这里）
│
├── docs/
│   ├── docker/                          ← Docker Compose 配置（保留）
│   ├── image/                           ← 项目图片（保留）
│   ├── script/                          ← 脚本（保留）
│   │
│   ├── 开发说明/                          ← 目标 IPD 系统的二次开发说明书（产品设计）
│   │   ├── 开发说明书.md                 主文档（1305 行）：11 条硬约束 + 业务规则 + 数据模型 + API + 49 页 + P0–P4
│   │   └── spec/
│   │       ├── _导航地图.md              49 页清单 + 跳转关系 + 权限矩阵
│   │       ├── _公共规范.md              UI / 视觉 / 文案 / 术语（宪法）
│   │       ├── batch-01 ~ batch-04      49 页的页面级规格
│   │       ├── 附录-D6-跨页状态机联动.md
│   │       └── 页级规格模板.md
│   │
│   ├── ipd-系统说明/                      ← 本次新增：二开漂移审计 + 改造指南（**这是你做改造时的工作手册**）
│   │   ├── README.md                    本目录总览（怎么读、各文件用途）
│   │   ├── drift-audit-report.md        drift 审计报告（哪些一致 / 哪些冲突）
│   │   ├── 改造检查清单.md              静态检查脚本路径修正 + CI 集成
│   │   ├── type-mapping.md               PostgreSQL → MySQL 类型映射表
│   │   ├── naming-convention.md          Sys* vs 文档名的命名约定
│   │   ├── fork-原与外部资源清单.md       本仓库源 + 10+ 个外部资源清单
│   │   └── 外部资源/                      外部资源骨架（详见各文件）
│   │       ├── IPD系统_AI开发主Prompt_v3.md
│   │       ├── IPD系统_六阶段标准动作清单_v3.md
│   │       ├── IPD系统_五大Gate评审要素_v1.md
│   │       ├── IPD系统_验收清单.md
│   │       ├── IPD系统_开发执行规则_AI必读.md
│   │       ├── IPD系统_冲突裁决与最终待确认清单.md
│   │       ├── IPD系统_待确认决策表_v2.md
│   │       ├── assets_公共规范-通用.md
│   │       ├── design-specs_后台-RuoYi-AI.md
│   │       └── mock-data.js
│   │
│   └── wiki/                             ← RuoYi-AI 基线的知识库（karpathy-llm-wiki 生成）
│       ├── raw/                          60 个不可变源文件
│       ├── wiki/                         21 篇结构化文章（按模块/主题分组）
│       └── wiki-lint.cjs                 自写 lint 脚本
│
├── ruoyi-admin/                         Spring Boot 主应用（保留 + 扩展）
├── ruoyi-common/                        27 个共享库（保留）
├── ruoyi-extend/                        辅助应用（保留）
├── ruoyi-modules/                       功能模块（保留 + 扩展）
│   ├── ruoyi-system/                    系统管理（保留 + 新增 IPD 子包）
│   ├── ruoyi-chat/                      AI 核心（保留 + IPD 文档助手扩展）
│   ├── ruoyi-aiflow/                     AI 工作流（保留）
│   ├── ruoyi-workflow/                  传统工作流（保留）
│   └── ruoyi-generator/                  代码生成（保留）
│
└── CLAUDE.md                             Claude Code 上下文（项目级规范）
```

---

## 五、改造阶段（与开发说明书 P0–P4 对齐）

### 阶段总览

| 阶段 | 内容 | 文档出处 | 工作量估计 |
|---|---|---|---|
| **P0 底座** | monorepo / 后端基础 / 前端布局 / 认证 / 通用删除引擎 / 审计日志 + hash 链 | 开发说明书 §11 P0 | 中（沿用 RuoYi 多数） |
| **P1 IPD 主干** | 产品 / 项目 CRUD / 69 动作 seed / 阶段可视化 / SOP / 交付物 / 门禁 / 国别认证 / 存量导入 | §11 P1 | 大（核心业务） |
| **P2 双PM 机制** | 组织架构 / 人员管理 / 招标组队 / 双 PM 绑定 / 五大 Gate 双签 / 需求变更 / 移交 / 超管移交 | §11 P2 | 大 |
| **P3 绩效与激励** | KPI / 项目绩效 / 津贴 / 奖金池 / 负反馈 / 退出升降级 / 报表 | §11 P3 | 中（⭐涉钱必须 TDD） |
| **P4 外围闭环** | 需求门户 / AI 文档助手 / 工作台 / 全局报表 / 权限矩阵回归 / 审计覆盖检查 | §11 P4 | 小 |

### 跨阶段硬约束（11 条）

来自 `docs/开发说明/开发说明书.md §3` 的 G-01 ~ G-11，**任一违反即返工**：

| 编号 | 约束 | 落地点 |
|---|---|---|
| **G-01** | 业务闭环：发起 → 处理 → 审核 → 结果 → 记录 → 归档 六环节 | 所有流程页必须有出口按钮 + 归档区 |
| **G-02** | 权限隔离：三层角色（超管 / 产品组长 / 普通 PM）严格隔离；**编辑可自主，删除必须走申请审核** | 删按钮全跳「发起删除申请」 |
| **G-03** | 审计留痕：新增 / 编辑 / 审核 / 驳回 / 删除 / 移交 / 参数修改 / 人员同步 / 登录凭证变更 全部写审计；日志只追加不可改不可删 | `@Audit()` + DB 层 `REVOKE UPDATE, DELETE` |
| **G-04** | 制度优先：本规则与 v3 冲突时以 v3 为准 | 开发说明书 §14 已按 v3 裁定 |
| **G-05** | 参数可配置：所有系数 / 阈值 / 期限 / 额度落库为参数表，禁止硬编码 | `system_configs` 表 + `ConfigService` |
| **G-06** | 无绝对化承诺：禁止「100% 闭环」「无遗留疑问」文案；未知状态显式展示「待补充」「未配置」 | UI 走 `el-empty` + 方向性提示 |
| **G-07** | 中文界面：全中文，文案抽离 i18n，一期只填 `zh-CN` | 前端 `locales/zh-CN.ts` |
| **G-08** | 涉钱参数必须可切换 | 6 项奖金参数全部走 `system_configs` |
| **G-09** | 角色体系不扩展：仅 普通 PM / 产品组长 / 超管 / 游客；不新增代理审批委托 | 不做委托；离职由组长 / 超管代操作 |
| **G-10** | 市场 PM 视角优先：研发 PM 主导阶段（开发 / 验证）默认轻管 | 23 个轻管动作详情只用 3 个输入框 |
| **G-11** | 数据库双账号分离：`DATABASE_URL` 用 `ipd_app`（非超级用户）；`DIRECT_URL` 仅迁移用 | MySQL：`ipd_app`（运行时）+ 迁移专用（仅 Flyway / Liquibase） |

### 阶段验证（每个 P 阶段完成必须跑）

| P | 验证清单 |
|---|---|
| P0 | 启动 → 超管登录 → 创建 PM → 首次强制改密 → 删除申请走完两级审核 → 审计可查 + hash 链通过 |
| P1 | 新建硬件项目 → 69 动作自动挂载 → 深管不传交付物无法完成 → 轻管只填三字段 → 阻断动作未完成跳阶被拒 → 选沙特自动带 SABER → BioCV 强制挂 C12 |
| P2 | 招标 → 应标 → 遴选 → 组队 → G1 要素逐项判定（否决无法通过）→ 双签互不可见 → 3 天超期弃权 → 冲突仲裁 → 离职冻结 + 移交 → 项目移交数据完整 |
| P3 | KPI 打分 → 综合得分 → 津贴叠加封顶 → 4 个算例必过（A: 13.2 / B: 24.75 / C: 70% → 0.6 / D: 120% → 1.0 / 120.1% → 1.2 / 99.99% → 0.8）→ 静态检查 0 命中 |
| P4 | 游客提交需求 → 按产品路由 → 凭码查进度 → 需求变更双签 → AI 生成 → 未审核不可归档 → 版本链回溯 → 三类角色权限边界 |

---

## 六、改造工程师必读路径

### 6.1 第一次接触本仓库的工程师

**第 1 步**：读本文件（README-IPD-OVERRIDE.md），5 分钟，了解改造背景

**第 2 步**：读 `docs/开发说明/spec/_公共规范.md`，10 分钟，理解 UI / 文案 / 术语「宪法」

**第 3 步**：读 `docs/开发说明/spec/_导航地图.md`，10 分钟，理解 49 页 + 跳转关系 + 权限矩阵

**第 4 步**：读 `docs/开发说明/开发说明书.md`（主文档），60 分钟，理解业务规则 + 数据模型 + API + 构建顺序

**第 5 步**：读 `docs/ipd-系统说明/drift-audit-report.md`，20 分钟，知道哪些一致 / 哪些冲突

**第 6 步**：根据要做的工作，读对应模块的 `docs/wiki/wiki/modules/*.md` 理解 RuoYi-AI 基线

**第 7 步**：动手前再读一遍 `docs/ipd-系统说明/改造检查清单.md`，确认没漏掉任何静态检查项

### 6.2 老 RuoYi-AI 工程师转过来的注意点

- 你熟悉的 `SysXxx` 命名风格保留给 RuoYi-AI 原有模块；**新 IPD 业务表用文档原名**（`persons`、`projects` 等）
- 你熟悉的 `R<T>{code=200}` 保留给 RuoYi-AI 接口；**新 IPD 接口用 `ApiV1Response{code=0}`**
- 你熟悉的 `/system/xxx`、`/chat/send` 路径保留；**新 IPD 接口走 `/api/v1/...`**
- 你的 MyBatis-Plus / Redisson / Sa-Token 经验 100% 适用
- 你的 Langchain4j 经验部分适用（IPD 文档助手扩展即可）

### 6.3 新工程师（无 RuoYi 经验）

- 先按 6.1 读完 6 个文件
- 然后看 `docs/wiki/wiki/index.md` 了解 RuoYi-AI 基线模块
- 最后跑 `mvn spring-boot:run -pl ruoyi-admin` 起项目，浏览 Swagger UI

---

## 七、与上游基线的关系

### 7.1 Fork 源

- **基线**：[wilson323/ruoyi-ai](https://github.com/wilson323/ruoyi-ai)（项目 README § 1 引用）
- **原始 RuoYi-AI**：[ageerle/ruoyi-ai](https://github.com/ageerle/ruoyi-ai)（本仓库 git remote）

⚠️ **本仓库与原 wilson323/ruoyi-ai 的同步问题**：
- 此 fork 在 2026-09-04 独立开发
- 后续与上游同步需要评估：RuoYi-AI 的 AI 能力（Langchain4j）部分同步；IPD 业务部分反向贡献给上游
- 同步策略待 Gavin 决定（建议建 `merge-upstream` 分支保护主分支）

### 7.2 二开范围 vs 上游 RuoYi-AI 的兼容性

| 改动 | 是否破坏 RuoYi-AI 兼容 | 说明 |
|---|---|---|
| 新增 `ruoyi-ipd` 模块 | ✅ 不破坏 | 完全独立模块 |
| 在 `ruoyi-system` 加 IPD 子包 | ⚠️ 微破坏 | 文件结构增加但不删除现有文件 |
| 新增 `/api/v1` 前缀 | ✅ 不破坏 | 老接口（`/system/xxx`）保留 |
| 新增 `ApiV1Response` | ✅ 不破坏 | 与 `R<T>` 共存 |
| 加 `application-v1.yml` 或类似 | ⚠️ 需谨慎 | profile 配置不要影响老接口 |

---

## 八、本仓库自动化栈（改造效率工具）

为了让二开更稳更快，本仓库装了 Claude Code 自动化栈：

| 类型 | 内容 | 文件 |
|---|---|---|
| **CLAUDE.md** | 项目级规范（已重写为「本项目当前是 IPD 二开基线」视角） | `CLAUDE.md` |
| **Skills（4 个项目自定义）** | ai-module-add / gen-test / api-contract / db-migration | `.claude/skills/` |
| **Subagents（4 个）** | code-reviewer / langchain4j-agent-reviewer / performance-analyzer / security-reviewer | `.claude/agents/` |
| **Hooks（2 个）** | sensitive-field-guard.cjs / pom-edit-hint.cjs | `.claude/helpers/` |
| **MCP Servers** | context7（实时文档） / github（待配 PAT） | `~/.claude.json → projects[本仓库].mcpServers` |
| **CI 集成** | wiki-lint.yml（每次 wiki 变动自动验证） | `.github/workflows/wiki-lint.yml` |

**使用建议**：
- 加新业务表 → 用 `/db-migration` skill（自动按 v3 字段类型生成）
- 加新 IPD 接口 → 用 `/api-contract` skill（自动生成 BREAKING / NEW / CHANGE 报告）
- 改任何 Java 文件 → Claude 自动派 subagent 并发审查（无需手动调）
- 写敏感配置 → hook 自动阻断 `.env` / `application-prod.yml` / PEM 私钥

---

## 九、联系与升级

- **Gavin**（Gavin 二开 owner）：负责业务决策、待确认项裁定、阶段验收
- **本文件维护者**：Claude Code（每次重大改造完成后自动 update）
- **修改触发**：任何与本文件不一致的实际改造 → 必须先 update 本文件

---

## 十、附录：开发说明书 / ipd-系统说明 / wiki 三层文档的关系

| 层 | 角色 | 何时读 | 谁写 |
|---|---|---|---|
| **开发说明书/**（已有） | 产品设计 / 业务规则 | 改造前必读 | Gavin + 产品 |
| **ipd-系统说明/**（新增） | 漂移审计 / 改造指南 / 适配说明 | 改造中随时查 | Claude Code + 工程师 |
| **wiki/**（已有） | RuoYi-AI 基线知识库 | 复用 RuoYi 功能时 | Claude Code 自动生成 |

**三层文档的核心区别**：
- `开发说明书/` 是「**应该建成什么样**」（设计文档，不可漂移）
- `ipd-系统说明/` 是「**怎么从基线改过来**」（改造指南，随改造更新）
- `wiki/` 是「**RuoYi-AI 基线是什么**」（事实记录，半年一次更新）

---

**本文件最后更新**：2026-09-04，由 Claude Code 在 drift audit 完成后自动生成。

**下次更新触发**：P0 阶段验收通过 / 改造方向重大调整 / 上游 wilson323/ruoyi-ai 重大更新。