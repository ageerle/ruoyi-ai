# Fork 源 + 外部资源清单

> 本文件是**新增**的工作手册。说明本仓库的 fork 来源 + `docs/开发说明/` 引用的 10+ 个外部资源（这些资源**不在本仓库**中），以及后续填充策略。

---

## 一、Fork 源说明

### 1.1 本仓库 Git Remote

```bash
$ git remote -v
origin  https://github.com/ageerle/ruoyi-ai.git (fetch)
origin  https://github.com/ageerle/ruoyi-ai.git (push)
```

### 1.2 Fork 链

```
ageerle/ruoyi-ai                  ← 原 RuoYi-AI 开源仓库
  └── wilson323/ruoyi-ai          ← 二开基线（被开发说明书引用）
       └── 本仓库（IPD 改造中）   ← 由 Gavin 主导改造
```

### 1.3 三者关系

| 仓库 | 类型 | 状态 | 与本仓库关系 |
|---|---|---|---|
| `ageerle/ruoyi-ai` | 原 RuoYi-AI 开源 | 持续维护 | **祖先**（代码源头） |
| `wilson323/ruoyi-ai` | 二开基线（fork + 定制） | 未知（开发说明书引用） | **祖先**（开发说明书以它为基线） |
| 本仓库 | 二开 IPD 系统 | 改造中 | **当前**（改造目标） |

### 1.4 文档表述与实际差异

| 文档表述 | 实际 |
|---|---|
| 开发说明书：「基于 `wilson323/ruoyi-ai` 二次开发」 | git remote 是 `ageerle/ruoyi-ai` |
| README：「Enterprise-Grade AI Assistant Platform」（原始 RuoYi-AI） | 本仓库即将改造为 IPD 系统（README-IPD-OVERRIDE.md 已说明） |

### 1.5 与上游同步策略

**当前状态**：未与上游同步

**建议**：
1. **P0 阶段前**：评估与 `wilson323/ruoyi-ai` 的差异（如有定制），决定 merge 策略
2. **每季度**：与 `wilson323/ruoyi-ai` 同步 AI 增强类更新（Langchain4j / chat 模块）
3. **不反向贡献**：IPD 业务不在开源范围内，留在本仓库

---

## 二、外部资源清单（10+ 个）

`docs/开发说明/` 全文引用了以下外部资源，**但本仓库目前没有任何一个**。

### 2.1 核心文档（7 个）

| 资源 | 来源 | 用途 | 引用位置 | 现状 |
|---|---|---|---|---|
| `IPD系统_AI开发主Prompt_v3.md` | 产品 v3 原文 | **唯一权威规格**（181 行 / 11.5 KB） | 开发说明书 §1、§3、§5、§8、§11 | ✅ **已填充**（2026-09-04 从 ZK-IPD `产品流程细化管理工具 2/` 复制） |
| `IPD系统_六阶段标准动作清单_v3.md` | 产品 v3 | 69 动作 seed 数据源 | 开发说明书 §5.4 BR-IPD-02 | ❌ 缺失 |
| `IPD系统_五大Gate评审要素_v1.md` | 产品 v1 | 33 项要素 + 14 否决项 seed | 开发说明书 §5.6 BR-GATE-01b | ❌ 缺失 |
| `IPD系统_验收清单.md` | 产品 | 237 条 AC（P0–P4 归类） | 开发说明书 §11 各阶段验收 | ❌ 缺失 |
| `IPD系统_开发执行规则_AI必读.md` | 产品 | 11 条硬约束 + 状态机速查 | 开发说明书 §3 | ❌ 缺失 |
| `IPD系统_冲突裁决与最终待确认清单.md` | 产品 | 4 条裁决原则 + 6 项裁定结论 | 开发说明书 §3、§14.1 | ❌ 缺失 |
| `IPD系统_待确认决策表_v2.md` | 产品 | 33 项决策原始记录 | 开发说明书 §5、§8 | ❌ 缺失 |

### 2.2 设计规范（2 个）

| 资源 | 来源 | 用途 | 引用位置 | 现状 |
|---|---|---|---|---|
| `assets/公共规范-通用.md` | RuoYi-AI 设计规范 | type 骨架（按钮 / 表格 / 表单 token） | `_公共规范.md` §一 | ❌ 缺失 |
| `design-specs/后台-RuoYi-AI.md` | RuoYi-AI 设计规范 | Element Plus 组件表 | `_公共规范.md` §六 | ❌ 缺失 |

### 2.3 演示数据（1 个）

| 资源 | 来源 | 用途 | 引用位置 | 现状 |
|---|---|---|---|---|
| `mock-data.js` | 前端原型 | 演示数据（13 角色 + 3 项目 + 算例金额） | 开发说明书 §13 + _导航地图 §6 | ❌ 缺失 |

---

## 三、本仓库提供的外部资源骨架

为减少对原文档的依赖，本目录 `外部资源/` 下提供 10 个**骨架文件**，每个含：

1. 文件用途说明（来自开发说明书）
2. 关键章节大纲（从开发说明书提取）
3. 引用位置（在哪些开发说明书段落被引用）
4. 填充提示（Gavin 决策 / 等待填充）

骨架文件清单：

```
docs/ipd-系统说明/外部资源/
├── IPD系统_AI开发主Prompt_v3.md       (骨架)
├── IPD系统_六阶段标准动作清单_v3.md   (骨架)
├── IPD系统_五大Gate评审要素_v1.md     (骨架)
├── IPD系统_验收清单.md               (骨架)
├── IPD系统_开发执行规则_AI必读.md    (骨架)
├── IPD系统_冲突裁决与最终待确认清单.md (骨架)
├── IPD系统_待确认决策表_v2.md         (骨架)
├── assets_公共规范-通用.md            (骨架)
├── design-specs_后台-RuoYi-AI.md       (骨架)
└── mock-data.js                       (骨架)
```

**填充顺序建议**：

1. **优先填充**（业务核心）：
 - `IPD系统_六阶段标准动作清单_v3.md`（69 动作定义）
 - `IPD系统_五大Gate评审要素_v1.md`（33 项要素 + 14 否决项）
 - `mock-data.js`（演示数据 seed）

2. **次优先**（架构约束）：
 - `IPD系统_开发执行规则_AI必读.md`（11 条硬约束）
 - `IPD系统_冲突裁决与最终待确认清单.md`（4 条原则）

3. **最后填充**（参考类）：
 - `IPD系统_AI开发主Prompt_v3.md`（1369 行 v3 原文，体积大）
 - `IPD系统_验收清单.md`（237 条 AC）
 - `IPD系统_待确认决策表_v2.md`（33 项决策原始记录）

4. **UI 设计**（最后）：
 - `assets_公共规范-通用.md`（type 骨架）
 - `design-specs_后台-RuoYi-AI.md`（组件表）

---

## 四、各资源填补决策表

| 资源 | 来源 | 决策依赖 | 等待方 | 预期时间 |
|---|---|---|---|---|
| v3 主 Prompt | Gavin / 产品 | 是否有权限访问原文 | Gavin | 二开启动前 |
| 69 动作清单 | v3 prompt 派生 | 等 v3 prompt 提供 | Gavin | P1 前 |
| 33 项 Gate 要素 | v1 prompt | 独立于 v3 | Gavin | P1 前 |
| mock-data.js | 团队 | 沿用 v3 prompt 中的算例数据 | Claude Code 可生成 | P0 阶段 |
| 验收清单 | 团队 | 衍生自 v3 prompt | Claude Code 可生成 | P1 末 |
| 11 条硬约束 | v3 派生 | 等 v3 prompt | Gavin | P0 前 |
| 冲突裁决 | 团队 | 决策原则 + 6 项裁定 | Gavin | P0 前 |
| 33 项决策表 | 团队 | 历史决策 | Gavin | 任何时候 |
| assets 公共规范 | RuoYi-AI / Vben | 沿用 Vben 默认 | 自动可填充 | P0 前 |
| design-specs | RuoYi-AI / Element Plus | 沿用 Element Plus | 自动可填充 | P0 前 |

---

## 五、与本仓库的关联

### 5.1 引用的 wiki 路径

开发说明书里部分约定在 wiki 里有落地：

| 约定 | 落地 |
|---|---|
| 多租户实现 | `docs/wiki/wiki/cross-cutting/multi-tenant-design.md` |
| 部署方式 | `docs/wiki/wiki/cross-cutting/deployment-guide.md` |
| 系统架构 | `docs/wiki/wiki/cross-cutting/architecture-overview.md` |
| 模块说明 | `docs/wiki/wiki/modules/*.md` |

**改造时**：新建 IPD controller 时可以参考这些 wiki 了解 RuoYi-AI 基线。

### 5.2 引用的子包路径

| 文档功能 | 本仓库对应实现 |
|---|---|
| 多租户 | `ruoyi-common/ruoyi-common-tenant` |
| 数据权限 | `ruoyi-common/ruoyi-common-mybatis`（`DataPermissionInterceptor`） |
| Sa-Token | `ruoyi-common/ruoyi-common-satoken` + `ruoyi-common/ruoyi-common-security` |
| 字典 | `ruoyi-common/ruoyi-common-core`（`SysDictType` / `SysDictData`） |
| WebSocket / SSE | `ruoyi-common/ruoyi-common-websocket` / `ruoyi-common/ruoyi-common-sse` |
| OSS（MinIO） | `ruoyi-common/ruoyi-common-oss` |
| Excel 导入导出 | `ruoyi-common/ruoyi-common-excel` |
| 分布式锁 | `ruoyi-common/ruoyi-common-redis`（Lock4j + Redisson） |
| 限流 | `ruoyi-common/ruoyi-common-ratelimiter` |
| 幂等 | `ruoyi-common/ruoyi-common-idempotent` |
| 字典翻译 | `ruoyi-common/ruoyi-common-translation` |
| 操作日志 | `ruoyi-common/ruoyi-common-log` |
| 定时任务 | `ruoyi-common/ruoyi-common-job`（SnailJob 客户端） |
| 短信 | `ruoyi-common/ruoyi-common-sms` |
| 邮件 | `ruoyi-common/ruoyi-common-mail` |
| 社交登录 | `ruoyi-common/ruoyi-common-social` |
| WebSocket | `ruoyi-common/ruoyi-common-websocket` |
| 多租户 | `ruoyi-common/ruoyi-common-tenant` |
| 加密 | `ruoyi-common/ruoyi-common-encrypt`（RSA API 加密 + AES 字段加密） |
| 脱敏 | `ruoyi-common/ruoyi-common-sensitive` |
| 链路追踪 | `ruoyi-common/ruoyi-common-trace` |
| Springdoc | `ruoyi-common/ruoyi-common-doc` |

---

## 六、维护规则

### 6.1 何时更新本文档

| 触发 | 更新内容 |
|---|---|
| Fork 源变更 | update §1.4 + §1.5 |
| 新增外部资源引用 | update §2 |
| 新骨架文件 | update §三 |
| 资源状态变更（如 mock-data.js 已填充） | update §四 |

### 6.2 owner

- Claude Code（自动）+ Gavin（决策）

---

## 七、附录：开发说明书引用的其他非文档资源

开发说明书还引用了以下**代码 / 配置资源**，已在本仓库存在：

| 资源 | 本仓库位置 | 状态 |
|---|---|---|
| `sys_dict_type` / `sys_dict_data` 表 | 已有（MyBatis-Plus 自动建表） | ✅ |
| `SysUser` 实体 | `ruoyi-modules/ruoyi-system/src/main/java/org/ruoyi/system/domain/SysUser.java` | ✅ |
| `BaseEntity` | `ruoyi-common/ruoyi-common-chat/src/main/java/org/ruoyi/common/chat/entity/BaseEntity.java` | ✅ |
| `@SaCheckLogin` / `@SaCheckRole` / `@SaCheckPermission` / `@SaCheckDataScope` | `ruoyi-common/ruoyi-common-satoken` + `ruoyi-common/ruoyi-common-security` | ✅ |
| `@Lock4j` 分布式锁 | `ruoyi-common/ruoyi-common-redis` | ✅ |
| `@RateLimiter` 限流 | `ruoyi-common/ruoyi-common-ratelimiter` | ✅ |
| `@ExcelProperty` / `@ExcelDictFormat` | `ruoyi-common/ruoyi-common-excel` | ✅ |
| `@Sensitive` 脱敏 | `ruoyi-common/ruoyi-common-sensitive` | ✅ |
| `@Translation` 字典翻译 | `ruoyi-common/ruoyi-common-translation` | ✅ |
| `@Log` 操作日志 | `ruoyi-common/ruoyi-common-log` | ✅ |
| `@SnailJob` 分布式任务 | `ruoyi-common/ruoyi-common-job` | ✅ |
| `@Idempotent` 幂等 | `ruoyi-common/ruoyi-common-idempotent` | ✅ |
| `@TenantLine` 多租户 | `ruoyi-common/ruoyi-common-mybatis` | ✅ |
| `MybatisPlusConfig` | `ruoyi-common/ruoyi-common-mybatis/src/main/java/org/ruoyi/common/mybatis/config/MybatisPlusConfig.java` | ✅ |

**新增 IPD 业务时直接用上面这些注解**，不需要新建。

---

**最后更新**：2026-09-04