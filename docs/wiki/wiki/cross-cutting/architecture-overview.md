---
topic: cross-cutting/architecture-overview
title: 系统架构总览
updated: 2026-09-04
raw:
  - raw/project-skeleton/pom-xml.md
  - raw/project-skeleton/application-yml.md
  - raw/admin-source/ruoyi-ai-application.md
  - raw/chat-source/chat-controller.md
  - raw/system-source/sys-user-controller.md
  - raw/aiflow-source/workflow-engine.md
---

# 系统架构总览

RuoYi-AI 是一个**企业级 AI 助手平台**，后端基于 Spring Boot 3.5.8 + Java 17 + Langchain4j 1.17.2 + Langgraph4j 1.8.20，前端分离（独立仓库）。

## 顶层架构

```
┌────────────────────────────────────────────────────────┐
│                      用户 / 浏览器                       │
│      ┌────────────────┐  ┌────────────────┐              │
│      │  ruoyi-web     │  │  ruoyi-admin   │              │
│      │  用户端 (Vue3) │  │ 管理后台 (Vue3) │              │
│      └────────┬───────┘  └────────┬───────┘              │
└───────────────┼──────────────────┼─────────────────────┘
                │ HTTPS / SSE / WebSocket
                ▼
┌────────────────────────────────────────────────────────┐
│           Spring Boot 3.5.8 (ruoyi-admin, port 6039)    │
│  ┌──────────────────────────────────────────────────┐  │
│  │  ruoyi-modules/                                   │  │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────┐ │  │
│  │  │ ruoyi-chat   │ │ ruoyi-aiflow │ │ ruoyi-   │ │  │
│  │  │  Langchain4j │ │  自研图引擎  │ │ system   │ │  │
│  │  │  AI 核心     │ │  AI 编排     │ │ RBAC     │ │  │
│  │  └──────────────┘ └──────────────┘ └──────────┘ │  │
│  │  ┌──────────────┐ ┌──────────────────────────┐  │  │
│  │  │ ruoyi-       │ │ ruoyi-generator           │  │  │
│  │  │ workflow     │ │ 代码生成                  │  │  │
│  │  │ Warm-Flow    │ │                          │  │  │
│  │  └──────────────┘ └──────────────────────────┘  │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │  ruoyi-common/ (27 子模块)                       │  │
│  │  共享：security/mybatis/web/chat/redis/sse/...   │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
                │ JDBC / Redis / HTTP / gRPC
                ▼
┌────────────────────────────────────────────────────────┐
│  数据 / 中间件                                           │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │ MySQL    │ │ Redis    │ │ 向量库   │ │ MinIO    │  │
│  │ 23306    │ │ 26379    │ │ (任选一) │ │ 29000    │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘  │
└────────────────────────────────────────────────────────┘

外部 AI 模型（通过 application.yml 模型管理配置）：
- DeepSeek / Zhipu / OpenAI / MIMO / Bailian
- Dify / Coze / FastGPT / RAGFlow 平台集成
```

参见：[pom-xml.md](../raw/project-skeleton/pom-xml.md)、[modules/admin.md](../modules/admin.md)、[modules/chat.md](../modules/chat.md)。

## 模块拓扑

```
                 ┌─────────────────────┐
                 │   ruoyi-admin       │
                 │   Spring Boot 主    │
                 └──────────┬──────────┘
                            │ 依赖（jar）
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ ruoyi-system │    │  ruoyi-chat  │    │ ruoyi-aiflow │
│   RBAC       │    │  AI 核心     │    │  AI 编排     │
└──────────────┘    └──────────────┘    └──────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ ruoyi-       │    │  ruoyi-      │    │  ruoyi-      │
│ workflow     │    │  generator   │    │  common/*    │
│ Warm-Flow    │    │  代码生成    │    │  27 共享库   │
└──────────────┘    └──────────────┘    └──────────────┘
```

**关键点**：`ruoyi-extend/`（monitor-admin + snailjob-server）是**独立 Spring Boot 应用**，**不**被 admin 依赖——它们独立部署。

参见：[modules/admin.md](../modules/admin.md)、[claude-md.md § Module Layout](../raw/project-skeleton/claude-md.md)。

## AI 双引擎

项目有**两套工作流引擎**，分工清晰：

| 引擎 | 适用 | 技术 |
|---|---|---|
| `ruoyi-workflow` | 传统审批 | Warm-Flow（BPMN 2.0） |
| `ruoyi-aiflow` | AI 处理管道 | 自研图驱动 |

详见 [modules/workflow.md](../modules/workflow.md) 和 [modules/aiflow.md](../modules/aiflow.md)。

## AI 核心技术栈

- **Langchain4j**：`1.17.2`（stable + community + beta 三套 BOM 严格对齐）
- **Langgraph4j**：`1.8.20`（Supervisor 模式 agent 编排）
- **向量库**：Weaviate（默认）/ Milvus / Qdrant，可通过 `vector-store.type` 切换
- **多模型**：通过「模型管理」后台配置，支持 DeepSeek / Zhipu / OpenAI 等
- **MCP 协议**：内置 6 个文件操作工具 + 自定义 MCP 服务接入

参见：[modules/chat.md](../modules/chat.md)、[claude-md.md § AI Stack](../raw/project-skeleton/claude-md.md)。

## 通信协议

- **HTTP / HTTPS**：REST API（Spring MVC）
- **SSE**：默认流式响应通道（`/resource/sse`），适合 LLM streaming 输出
- **WebSocket**：可选（默认关），用于需要双向通信场景
- **MySQL TCP**：数据库连接（HikariCP 连接池）

参见：[application-yml.md § sse / websocket](../raw/project-skeleton/application-yml.md)。

## 数据持久层

- **MyBatis-Plus** + 多租户拦截器 + 数据权限拦截器 + 分页插件
- **Redis**：缓存、分布式锁（Redisson）
- **向量库**：Weaviate / Milvus / Qdrant（RAG 与 embedding）
- **MinIO**：对象存储（OSS / 头像 / 文件上传）

参见：[modules/common.md § mybatis / redis / oss](../modules/common.md)。

## 安全 / 鉴权

- **Sa-Token + JWT** 双层认证
- **API RSA 加解密**（可选，`api-decrypt.enabled`）
- **多租户过滤**（自动）
- **数据权限范围**（`@SaCheckDataScope`）
- **XSS 过滤**（全局）

详见 [cross-cutting/multi-tenant-design.md](../cross-cutting/multi-tenant-design.md)。