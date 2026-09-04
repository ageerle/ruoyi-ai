# Wiki Log


## 2026-09-04

### batch-1: project-skeleton
- **ingest**: pom.xml, application*.yml (3), CLAUDE.md, README.md, README_ZH.md
- **compile**: wiki/modules/admin.md（基于 7 个 raw 文件）
- **raw files**: raw/project-skeleton/ (7 files, ~95 KB)

### batch-2: admin-source
- **ingest**: ruoyi-admin 全部 6 个 Java 文件 + logback-plus.xml
- **compile**: wiki/modules/admin-source.md（启动 / Controller / Config 详解）
- **raw files**: raw/admin-source/ (7 files, ~31 KB)

### batch-3: chat-source
- **ingest**: ruoyi-chat 8 个核心（ChitChatAgent / ChatController / MCP tool provider / ChatServiceFactory / RAG trace / VectorStoreProperties / McpSseConfig）
- **compile**: wiki/modules/chat.md（覆盖 21 agent、6 内置 MCP 工具、5 factory、Supervisor 模式、RAG 追踪）
- **raw files**: raw/chat-source/ (8 files, ~34 KB)

### batch-4: aiflow-source
- **ingest**: ruoyi-aiflow 8 个核心（WorkflowController / RuntimeController / 3 entity / WorkflowEngine / WfNodeFactory / GraphBuilder）
- **compile**: wiki/modules/aiflow.md（图驱动引擎、节点工厂、状态机）
- **raw files**: raw/aiflow-source/ (8 files, ~42 KB)

### batch-5: system-source
- **ingest**: system 8 文件（3 controller + listener + runner + entity）
- **compile**: wiki/modules/system.md（RBAC、23 controller、监听器、启动任务、CMS）

### batch-6: workflow + generator
- **ingest**: workflow 2 + generator 2 文件
- **compile**: wiki/modules/workflow.md + wiki/modules/generator.md

### batch-7: common 关键库
- **ingest**: common 6 文件（security / mybatis / web / satoken / chat）
- **compile**: wiki/modules/common.md（27 子模块清单）

### batch-8: extend + docker
- **ingest**: extend 2 + docker 3 文件
- **状态**: raw 已就位，wiki 待写（合并到 cross-cutting/deployment-guide.md）

### batch-9: cross-cutting + automation
- **compile**: 
  - wiki/cross-cutting/architecture-overview.md（系统架构总览 + 模块拓扑）
  - wiki/cross-cutting/multi-tenant-design.md（多租户隔离机制 + 边界）
  - wiki/cross-cutting/deployment-guide.md（3 种部署方式 + 配套服务）
  - wiki/automation/claude-code-setup.md（Claude Code 自动化栈使用手册）

### lint 验证
- **脚本**: wiki-lint.cjs（自写 78 行，简化版 check_evidence.py）
- **结果**: 79 通过 / 0 失败 / 0 孤立 raw
- **修复**: 5 篇 wiki 文章的 raw 引用缺 .md 后缀 + lint 脚本 listRaw bug

### batch-10: wiki 扩展
- **ingest**: 3 个聚合 raw（agents-catalog / mcp-tools-catalog / rbac-entities）
- **compile**: 4 篇扩展 wiki
  - wiki/modules/system-rbac-deep-dive.md（26 实体 / 4 权限注解 / 5 级数据权限）
  - wiki/modules/system-listener-runner.md（ApplicationRunner / 2 listener / 启动期异常处理）
  - wiki/modules/chat-agents-catalog.md（7 个核心 agent / Supervisor 模式 / 添加流程）
  - wiki/modules/chat-mcp-tools.md（6 个内置工具 / ExecuteCommand 安全风险 / BuiltinToolProvider 模式）
- **lint 结果**: 99 通过 / 0 失败 / 0 孤立 raw
- **git**: 77 文件暂存（未自动 commit）
