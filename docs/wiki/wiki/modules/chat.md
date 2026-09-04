---
topic: modules/chat
title: ruoyi-chat — Langchain4j AI 核心模块
updated: 2026-09-04
raw:
  - raw/chat-source/chit-chat-agent.md
  - raw/chat-source/chat-controller.md
  - raw/chat-source/mcp-tool-provider-service.md
  - raw/chat-source/chat-service-factory.md
  - raw/chat-source/rag-trace-node-types.md
  - raw/chat-source/rag-trace-payload-builder.md
  - raw/chat-source/vector-store-properties.md
  - raw/chat-source/mcp-sse-config.md
---

# ruoyi-chat — Langchain4j AI 核心

`ruoyi-chat` 是 RuoYi-AI 的**核心 AI 模块**，承担：

- 多 agent 编排（Supervisor 模式）
- Langchain4j 工具暴露与 MCP server 集成
- 多模型适配（DeepSeek / Zhipu / OpenAI / MIMO / Bailian）
- RAG 与向量库桥接
- 聊天会话与消息持久化
- 多模态（文本 / 图片 / 视频生成）

540 个 Java 文件，结构如下：

```
org.ruoyi.chat/
├── controller/
│   ├── chat/        # 主聊天 controller（ChatController / ChatSessionController / ChatMessageController）
│   ├── agent/       # Agent 注册 controller
│   └── mcp/         # MCP 工具市场 controller
├── service/         # 343 个业务 service（按 model/chat/agent/video/embed 等分子包）
├── agent/           # 21 个 Langchain4j @Agent 接口
├── mcp/
│   ├── tools/       # 内置 MCP 工具（EditFile / DeleteFile / ReadFile / WriteFile / ListDirectory / ExecuteCommand）
│   └── service/core/   # LangChain4jMcpToolProviderService + BuiltinToolProvider
├── factory/         # 5 个 factory（Chat / Rerank / VectorStore / Embedding / ResourceLoader）
├── domain/          # 85 个 entity / bo / vo / dto
├── mapper/          # MyBatis-Plus mapper
├── config/          # Spring 配置（VectorStoreProperties / McpSseConfig / SkillsPathResolver / SystemToolInitializer）
├── argtrace/        # RAG 链路追踪（最近从 chat.* 移到 argtrace.*，参见 commit 9d439d1d）
├── observability/   # 可观测性
└── websocket/           # WebSocket 处理
```

参见：[pom-xml.md § modules](../raw/project-skeleton/pom-xml.md)、[claude-md.md § Module Layout](../raw/project-skeleton/claude-md.md)。

## 入口 — ChatController

```java
@Controller
@RequestMapping("/chat")
public class ChatController {
    private final ChatServiceFacade chatService;

    @PostMapping("/send")
    @ResponseBody
    public SseEmitter sseChat(@RequestBody @Valid ChatRequest chatRequest) {
        return chatService.sseChat(chatRequest);
    }
}
```

`/chat/send` 是唯一的 chat 入口，**返回 `SseEmitter`**（Server-Sent Events）。SSE 而非 WebSocket 是项目默认选择（参见 `application.yml` 的 `sse.enabled: true`、`websocket.enabled: false`）——SSE 单向流、HTTP/1.1 长连接、自动重连，配合 LLM streaming 输出天然契合。

实际业务在 `ChatServiceFacade`（不是直接实现）。

参见：[chat-controller.md](../raw/chat-source/chat-controller.md)、[application-yml.md § sse](../raw/project-skeleton/application-yml.md)。

## Agent 接口 — 以 ChitChatAgent 为例

```java
public interface ChitChatAgent {
    @SystemMessage("""
        你是一个友好、自然的对话助手,负责问候、闲聊和常识性问答。
        要求:
        - 用与用户相同的语言回答,简洁自然
        - 不要编造需要实时数据或专业工具才能得到的事实
        - 如果用户的问题实际需要联网搜索、查数据库、执行技能或生成图表,直接说明这超出你的职责,
          让用户重新描述需求
    """)
    @UserMessage("{{query}}")
    @Agent("闲聊兜底助手:仅用于问候、日常闲聊和不需要联网搜索、数据库、技能或图表的通用问题")
    String chat(@V("query") String query);
}
```

**Langchain4j @Agent 模式**的核心约定：

| 注解 | 作用 |
|---|---|
| `@SystemMessage("...")` | 系统 prompt（角色、约束、行为） |
| `@UserMessage("{{var}}")` | 用户消息模板，`{{var}}` 占位 |
| `@Agent("...")` | agent 描述（给 Supervisor 路由用） |
| `@V("var")` | 模板变量绑定 |

**ChitChatAgent 的特殊定位**：作为 Supervisor 编排的「兜底」agent——当用户问题不需要联网 / 数据库 / 图表 / 技能时落地到这里，避免简单问题被路由到重型 agent 浪费 token。

21 个 agent 包括：ChitChat / ShortDramaScript / Sql / Skills / Echarts / WebSearch / ChartGeneration 等，每个都有明确职责边界。

参见：[chit-chat-agent.md](../raw/chat-source/chit-chat-agent.md)、[claude-md.md § AI Stack](../raw/project-skeleton/claude-md.md)。

## MCP 工具暴露核心 — LangChain4jMcpToolProviderService

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class LangChain4jMcpToolProviderService {
    private static final int MAX_FAILURE_COUNT = 3;
    private static final long DISABLE_DURATION = 5 * 60 * 1000;
    private final McpToolMapper mcpToolMapper;
    private final ObjectMapper objectMapper;
    private final BuiltinToolRegistry builtinToolRegistry;
    private final Map<Long, McpClient> activeClients = new ConcurrentHashMap<>();
    private final Map<Long, Boolean> toolHealthStatus = new ConcurrentHashMap<>();
    private final Map<Long, Integer> toolFailureCount = new ConcurrentHashMap<>();
    private final Map<Long, Long> toolDisabledUntil = new ConcurrentHashMap<>();
}
```

**三大职责**：

1. **统一工具注册**：从数据库 `mcp_tool` 表读工具配置，把 BUILTIN / LOCAL / REMOTE 三类工具统一转成 Langchain4j `ToolProvider`
2. **健康检查**：跟踪每个工具的失败次数（`MAX_FAILURE_COUNT=3`）
3. **自动熔断**：连续失败 3 次 → 工具被禁用 5 分钟（`DISABLE_DURATION=5min`）

**关键发现**：`BuiltinToolRegistry` 注入说明内置工具（`EditFile` / `ReadFile` 等）通过**注册表模式**管理，而不是直接 new——这与 commit history「fix: expose builtin tools to LangChain4j agents」一致。

参见：[mcp-tool-provider-service.md](../raw/chat-source/mcp-tool-provider-service.md)、[claude-md.md § AI Stack — MCP tools](../raw/project-skeleton/claude-md.md)。

## 内置 MCP 工具 — 文件操作 6 件套

`mcp/tools/` 下 6 个内置 MCP 工具暴露给 Langchain4j agents：

| 工具类 | 用途 |
|---|---|
| `EditFileTool` | 编辑文件 |
| `DeleteFileTool` | 删除文件 |
| `ReadFileTool` | 读取文件 |
| `WriteFileTool` | 写入文件 |
| `ListDirectoryTool` | 列出目录 |
| `ExecuteCommandTool` | 执行 shell 命令 |

**`ExecuteCommandTool` 是高危工具**——给 AI agent shell 执行能力。生产环境必须：
- 加白名单（只能执行特定命令）
- 加审计日志
- 加超时与资源限制
- 加沙箱（独立用户 / 容器）

参见：[claude-md.md § Key Conventions — Coding harness](../raw/project-skeleton/claude-md.md)（coding.harness.tools.execute-process.enabled=true 的同源风险）。

## Factory 模式 — ChatServiceFactory

```java
@Component
public class ChatServiceFactory implements ApplicationContextAware {
    private final Map<String, AbstractChatService> chatServiceMap = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        Map<String, AbstractChatService> serviceMap = ctx.getBeansOfType(AbstractChatService.class);
        for (AbstractChatService service : serviceMap.values()) {
            chatServiceMap.put(service.getProviderName(), service);
        }
    }

    public AbstractChatService getOriginalService(String category) {
        AbstractChatService service = chatServiceMap.get(category);
        if (service == null) throw new IllegalArgumentException("不支持的模型类别: " + category);
        return service;
    }
}
```

**`ApplicationContextAware` + 自动收集所有 `AbstractChatService` 子类**——按 `providerName` 路由。

模型适配层（DeepSeek / Zhipu / OpenAI 等）每个实现 `AbstractChatService`，启动时自动注册到 factory，前端传 `provider` 时按名查表。

类似 factory 模式还用于：`RerankModelFactory`、`VectorStoreStrategyFactory`、`EmbeddingModelFactory`、`ResourceLoaderFactory`——都是「按类型路由」的可插拔架构。

参见：[chat-service-factory.md](../raw/chat-source/chat-service-factory.md)。

## 向量库配置 — VectorStoreProperties + McpSseConfig

- `VectorStoreProperties` 绑定 `application.yml` 的 `vector-store.*` 段，支持 weaviate / milvus / qdrant 三选一
- `McpSseConfig` 配置 MCP server 的 SSE 传输（HTTP-based MCP 而非 stdio）

参见：[vector-store-properties.md](../raw/chat-source/vector-store-properties.md)、[mcp-sse-config.md](../raw/chat-source/mcp-sse-config.md)、[application-yml.md § vector-store](../raw/project-skeleton/application-yml.md)。

## RAG 链路追踪 — argtrace 包

**近期重构**：trace 相关代码从 `org.ruoyi.chat.*` 移到 `org.ruoyi.argtrace.*`（参见 commit `9d439d1d: refactor: move RAG trace classes to argtrace package`）。

- `RagTraceNodeTypes` —— 节点类型常量（retrieval / rerank / generation / tool_call 等）
- `RagTracePayloadBuilder` —— 构造 trace payload，写入 `trace_run` / `trace_node` 表

**注意**：`trace_run` 和 `trace_node` 在 `tenant.excludes` 白名单里（参见 [application-yml.md § tenant.excludes](../raw/project-skeleton/application-yml.md)），因为 trace writer 跑在异步线程上，不传递租户上下文——这是设计上故意放行的共享表。

## 多租户 + 多会话隔离

`ChatSessionOwnershipGuard`（service 层）确保用户只能访问自己的会话，避免越权读其他租户的会话。这是项目里**业务安全**的关键防线，参见 [claude-md.md § Key Conventions — Multi-tenant](../raw/project-skeleton/claude-md.md)。