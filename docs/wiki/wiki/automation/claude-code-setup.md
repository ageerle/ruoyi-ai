---
topic: automation/claude-code-setup
title: Claude Code 自动化栈使用手册
updated: 2026-09-04
raw:
  - raw/project-skeleton/claude-md.md
  - raw/admin-source/ruoyi-ai-application.md
  - raw/project-skeleton/pom-xml.md
---

# Claude Code 自动化栈使用手册

本项目挂了 4 层 Claude Code 自动化：Skill（用户主动调用）、Subagent（Claude 自动调度）、Hook（机器执行）、MCP Server（外部能力）。另由 Ruflo 提供多智能体协同底座。

参见：[claude-md.md](../raw/project-skeleton/claude-md.md) § 自动化栈使用手册。

## Skills（用户主动调用，4 个项目自定义 + 30+ ruflo 内置）

### 项目自定义 skill

| Skill | 调用 | 适用场景 |
|---|---|---|
| `/ai-module-add` | user-only | 在 `ruoyi-chat` / `ruoyi-aiflow` 加新 Langchain4j agent、tool、workflow 节点或 MCP 工具 |
| `/gen-test` | user-only | 按 `@Tag("dev")` Surefire 过滤规范生成 Service / Controller 单测 |
| `/api-contract` | user-only | 改了 controller / DTO 后生成 OpenAPI 增量 diff + 给前端变更通知 |
| `/db-migration` | user-only | 新增业务表 / 加字段 / 加索引 / 新增 snailjob 任务 / 登记租户共享表 |

每个 skill 详见 `.claude/skills/<name>/SKILL.md`。

### ruflo 内置 30+ skill

默认不主动调，按名调用。`swarm-orchestration` / `v3-swarm-coordination` / `sparc-methodology` 是重型武器。

## Subagents（Claude 自动调度，4 个并发审查）

按改动范围触发，**边界不重叠**：

| Agent | 触发 | 审查 | 交给谁 |
|---|---|---|---|
| `code-reviewer` | 改任何 Java | 架构、可读性、并发、错误处理、Spring 用法 | AI / 安全 / 性能 |
| `langchain4j-agent-reviewer` | 改 `ruoyi-chat` / `ruoyi-aiflow` | prompt 注入、`@Tool` 暴露、token、MCP | 性能 / 安全 / 架构 |
| `security-reviewer` | 改 controller / service / config | 多租户、Sa-Token、加密、XSS、SQL 注入、密钥 | AI / 性能 / 架构 |
| `performance-analyzer` | 改 mapper / service / AI 模块 | SQL N+1、Redis、连接池、JVM、Langchain4j 性能 | 架构 / 安全 / AI |

每个 agent 详见 `.claude/agents/<name>.md`。

### 调度规则

- 改 1-2 行代码 → 不派 agent
- 改一个 controller 写操作 → `security-reviewer` + `code-reviewer`（2 个）
- 改 Langchain4j 模块 → `langchain4j-agent-reviewer`
- 模块级重构（>5 文件）→ 2-3 个 agent 并发

## Hooks（机器执行，最硬约束）

| Hook | 类型 | 触发 | 行为 |
|---|---|---|---|
| `sensitive-field-guard.cjs` | PreToolUse | Write/Edit/MultiEdit | 阻断 `.env*` / `application-prod.yml` / PEM 私钥；警告 JWT / 明文 password |
| `pom-edit-hint.cjs` | PostToolUse | Write/Edit/MultiEdit 命中 pom.xml | 提示 5 类同步项（langchain4j BOM / annotation processor / grpc / flatten / surefire） |

详见 `.claude/helpers/*.cjs` + `.claude/settings.json`。

## MCP Servers（项目级 scope）

| Server | 命令 | 用途 | 状态 |
|---|---|---|---|
| `context7` | `npx -y @upstash/context7-mcp` | 实时查 Langchain4j / Spring Boot 文档 | ✅ 可用 |
| `github` | `npx -y @modelcontextprotocol/server-github` | 操作 issues / PRs / actions | ⚠️ 需 GitHub PAT |

注册位置：`~/.claude.json → projects[本项目路径].mcpServers`。

## 漂移自检命令

每次改完配置跑一遍：

```bash
# 1. 文件存在 + 语法
for f in .claude/skills/{ai-module-add,api-contract,db-migration,gen-test}/SKILL.md .claude/agents/*.md; do [ -f "$f" ] && echo "✅ $f"; done
node --check .claude/helpers/*.cjs

# 2. settings.json 合法
node -e "JSON.parse(require('fs').readFileSync('.claude/settings.json','utf8'))"

# 3. MCP 注册
node -e 'const j=require("/Users/mac/.claude.json").projects["/Users/mac/Documents/ruoyi-ai"].mcpServers||{}; console.log(Object.keys(j))'

# 4. 4 agent 边界节齐全
for a in code-reviewer langchain4j-agent-reviewer performance-analyzer security-reviewer; do
  grep -q "^## 边界" .claude/agents/$a.md && echo "✅ $a" || echo "❌ $a 缺边界节"
done
```

## Ruflo 多智能体协同底座

项目使用 Ruflo V3 做多智能体协调（通过 `/ruflo-core:init-project` 初始化）。

**启用时机**：

- ✅ 跨 3+ 模块的功能开发、API 大改、安全审计、性能基准
- ❌ 单文件 / 单行改动、配置修改、问答

启用步骤：

```bash
npx claude-flow swarm init --topology hierarchical-mesh --max-agents 8
npx claude-flow hooks route --task "<任务描述>"
```

参见：[claude-md.md § Ruflo 多智能体协同底座](../raw/project-skeleton/claude-md.md)。