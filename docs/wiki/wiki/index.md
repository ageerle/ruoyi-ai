# Knowledge Base Index

RuoYi-AI 项目知识库索引。基于 karpathy-llm-wiki 工作流生成：源材料在 `raw/`，编译后的结构化文章在 `wiki/` 下。

## 文章清单（按主题分组）

### modules/ — 各功能模块详解

| 文章 | 主题 | Updated |
|---|---|---|
| [modules/admin.md](modules/admin.md) | ruoyi-admin — Spring Boot 主应用入口 | 2026-09-04 |
| [modules/admin-source.md](modules/admin-source.md) | ruoyi-admin 源码层（启动 / Controller / Config） | 2026-09-04 |
| [modules/chat.md](modules/chat.md) | ruoyi-chat — Langchain4j AI 核心模块 | 2026-09-04 |
| [modules/chat-agents-catalog.md](modules/chat-agents-catalog.md) | ruoyi-chat — 21 个 Agent 目录 + Supervisor 编排 | 2026-09-04 |
| [modules/chat-mcp-tools.md](modules/chat-mcp-tools.md) | ruoyi-chat — 内置 MCP 工具详解（含 ExecuteCommand 安全风险） | 2026-09-04 |
| [modules/chat-multimodal.md](modules/chat-multimodal.md) | ruoyi-chat — 多模态（视频 / 音频 / 图像 / Embedding） | 2026-09-04 |
| [modules/aiflow.md](modules/aiflow.md) | ruoyi-aiflow — 可视化 AI 工作流引擎 | 2026-09-04 |
| [modules/system.md](modules/system.md) | ruoyi-system — RBAC 与系统管理 | 2026-09-04 |
| [modules/system-rbac-deep-dive.md](modules/system-rbac-deep-dive.md) | ruoyi-system — RBAC 实体关系 + 权限注解 + 数据权限 | 2026-09-04 |
| [modules/system-listener-runner.md](modules/system-listener-runner.md) | ruoyi-system — 事件监听器与启动任务 | 2026-09-04 |
| [modules/workflow.md](modules/workflow.md) | ruoyi-workflow — Warm-Flow BPMN 引擎 | 2026-09-04 |
| [modules/generator.md](modules/generator.md) | ruoyi-generator — 代码生成器 | 2026-09-04 |
| [modules/common.md](modules/common.md) | ruoyi-common — 27 个共享库 | 2026-09-04 |
| [modules/common-core-utilities.md](modules/common-core-utilities.md) | common — 核心工具层（core / json / doc / excel） | 2026-09-04 |
| [modules/common-security-auth.md](modules/common-security-auth.md) | common — 安全认证层（security / satoken / encrypt / sensitive） | 2026-09-04 |
| [modules/common-data.md](modules/common-data.md) | common — 数据层（mybatis / redis / tenant / trace） | 2026-09-04 |
| [modules/common-communication.md](modules/common-communication.md) | common — 通信层（web / sse / websocket / oss / mail / sms / social） | 2026-09-04 |
| [modules/common-business.md](modules/common-business.md) | common — 业务能力层（log / job / ratelimiter / idempotent / translation） | 2026-09-04 |

### cross-cutting/ — 跨模块主题

| 文章 | 主题 | Updated |
|---|---|---|
| [cross-cutting/architecture-overview.md](cross-cutting/architecture-overview.md) | 系统架构总览 + 模块拓扑 + AI 双引擎 | 2026-09-04 |
| [cross-cutting/multi-tenant-design.md](cross-cutting/multi-tenant-design.md) | 多租户隔离设计（机制 / 边界 / checklist） | 2026-09-04 |
| [cross-cutting/deployment-guide.md](cross-cutting/deployment-guide.md) | 部署指南（3 种方式 + 配套服务 + 故障排查） | 2026-09-04 |

### automation/ — Claude Code 自动化栈

| 文章 | 主题 | Updated |
|---|---|---|
| [automation/claude-code-setup.md](automation/claude-code-setup.md) | Claude Code 自动化栈使用手册 | 2026-09-04 |

## 统计

- **raw 文件数**：60
- **wiki 文章数**：22
- **raw 总大小**：~450 KB
- **wiki 总大小**：~210 KB
- **覆盖模块**：admin / chat / aiflow / system / workflow / generator / common / extend
- **覆盖主题**：架构 / 多租户 / 部署 / 自动化
- **git 跟踪**：commit 8004cd03（+ 新增待提交）
- **CI 集成**：.github/workflows/wiki-lint.yml（自动验证）

## 主题目录（raw 源材料）

| 目录 | 文件数 | 主题 |
|---|---|---|
| `raw/project-skeleton/` | 7 | 项目骨架（pom / application*.yml / CLAUDE.md / README） |
| `raw/admin-source/` | 7 | ruoyi-admin 源码（启动 / controller / config / logback） |
| `raw/chat-source/` | 8 | ruoyi-chat 核心（agent / controller / MCP tool / factory / RAG trace） |
| `raw/aiflow-source/` | 8 | ruoyi-aiflow 核心（controller / entity / 引擎 / factory） |
| `raw/system-source/` | 8 | ruoyi-system 核心（controller / listener / runner / entity） |
| `raw/workflow-source/` | 2 | ruoyi-workflow 核心（controller / service） |
| `raw/generator-source/` | 2 | ruoyi-generator 核心（controller / service） |
| `raw/common-source/` | 6 | ruoyi-common 关键库（security / mybatis / web / satoken / chat） |
| `raw/extend-source/` | 2 | ruoyi-extend（monitor-admin / snailjob-server） |
| `raw/docker-source/` | 3 | docker-compose 配置 + Dockerfile |

## 操作日志

见 [log.md](log.md)。

## 使用说明

每篇文章顶部有 YAML frontmatter：

```yaml
topic: modules/chat
title: ruoyi-chat — Langchain4j AI 核心
raw:
  - raw/chat-source/chit-chat-agent.md
  - raw/chat-source/chat-controller.md
  ...
```

`raw:` 字段列出该文章引用的事实来源。所有数据点都能追溯到 `raw/` 里的 verbatim 原文。

**新增文章**：参考 `.agents/skills/karpathy-llm-wiki/SKILL.md` 的 article-template。

**验证完整性**：跑 `node .claude/helpers/wiki-lint.cjs`（已写入 wiki 根目录）。