# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

## Before exploring, read these

- **`CONTEXT.md`** at the repo root, or
- **`CONTEXT-MAP.md`** at the repo root if it exists: it points at one `CONTEXT.md` per context. Read each one relevant to the topic.
- **`docs/adr/`**: read ADRs that touch the area you're about to work in. In multi-context repos, also check `src/<context>/docs/adr/` for context-scoped decisions.

If any of these files don't exist, **proceed silently**. Don't flag their absence; don't suggest creating them upfront. The `/domain-modeling` skill (reached via `/grill-with-docs` and `/improve-codebase-architecture`) creates them lazily when terms or decisions actually get resolved.

## File structure

Single-context repo (most repos):

```
/
├── CONTEXT.md
├── docs/adr/
│   ├── 0001-event-sourced-orders.md
│   └── 0002-postgres-for-write-model.md
└── src/
```

Multi-context repo (presence of `CONTEXT-MAP.md` at the root):

```
/
├── CONTEXT-MAP.md
├── CONTEXT-<context>.md (or src/<context>/CONTEXT.md)
├── docs/adr/                 (shared decisions)
└── src/<context>/docs/adr/  (context-scoped decisions)
```

## 本仓库当前状态（2026-09-04）

- **Layout**: Single-context（已确认）
- **`CONTEXT.md`**: ❌ 不存在（按 skill 指引「proceed silently」——不强制创建）
- **`CONTEXT-MAP.md`**: ❌ 不存在
- **`docs/adr/`**: ❌ 不存在
- **`docs/ipd-系统说明/`**: ✅ 存在（IPD 二开工作手册）
- **`docs/wiki/`**: ✅ 存在（21 篇 wiki + 60 raw）

## 本仓库的实际文档地图（替代 CONTEXT.md）

由于 `CONTEXT.md` 不存在，engineering skills 应该读以下文件作为 domain context：

### 项目总览
- `README.md` —— RuoYi-AI 基线介绍
- `README-IPD-OVERRIDE.md` —— IPD 改造方向总览（**优先级最高**）
- `CLAUDE.md` —— Claude Code 项目级规范
- `AGENTS.md` —— mattpocock-skills 装的 agent 规范

### 改造设计
- `docs/开发说明/开发说明书.md` —— 1305 行 + 11 条硬约束（G-01~G-11）+ 业务规则 + 数据模型 + API 契约 + 49 页规格
- `docs/开发说明/spec/_导航地图.md` —— 49 页清单 + 跳转关系 + 权限矩阵
- `docs/开发说明/spec/_公共规范.md` —— UI / 视觉 / 文案 / 术语「宪法」

### 改造工程（新增）
- `docs/ipd-系统说明/开发文档一致性报告.md` —— 文档一致性审计（§六 为终局口径）
- `docs/ipd-系统说明/改造检查清单.md` —— 静态检查 + CI 集成
- `docs/ipd-系统说明/type-mapping.md` —— PostgreSQL → MySQL 字段类型映射
- `docs/ipd-系统说明/naming-convention.md` —— 表命名 + 字段命名 + API 路径

### 外部资源（原文已填充，2026-09-04）
- `docs/ipd-系统说明/外部资源/`：7 个核心（v3 Prompt / 六阶段清单 v3 / 五大Gate要素 / 验收清单 v2.1 = 237 条 AC / AI必读 / 冲突裁决 / 决策表 v2）+ 前端规范 2 件 + `mock-data.js`（Q2 = 回款口径）
- 清单与适配状态见 `fork-原与外部资源清单.md`

### RuoYi-AI 基线知识
- `docs/wiki/wiki/index.md` —— 21 篇文章清单
- `docs/wiki/wiki/modules/*.md` —— 各模块详解
- `docs/wiki/wiki/cross-cutting/*.md` —— 跨模块主题（架构 / 多租户 / 部署）

### 自动化栈
- `.claude/settings.json` —— hooks + 权限
- `.claude/skills/<name>/SKILL.md` —— 4 个项目自定义 skill
- `.claude/agents/<name>.md` —— 4 个 subagent
- `.claude/helpers/*.cjs` —— 3 个 hook 脚本
- `.claude/hooks/*.sh` —— 1 个 bash hook
- `.agents/skills/<name>/SKILL.md` —— 43 个 mattpocock skill
- `.claude-flow/CAPABILITIES.md` —— Ruflo 配置

## 何时应该创建 CONTEXT.md

按 mattpocock skill 设计：**懒加载**——只在 `/domain-modeling` 真正开始建模领域时才创建。如果 `/grill-with-docs` 触发需求调研，或 `/improve-codebase-architecture` 扫描到架构改进机会，`/domain-modeling` 会创建对应的 `CONTEXT.md`。

---

**最后更新**：2026-09-04（setup-matt-pocock-skills 自动生成）