---
name: ai-module-add
description: 为 ruoyi-ai 的 ruoyi-chat / ruoyi-aiflow / ruoyi-workflow 模块新增 Langchain4j agent、tool、workflow 节点或 MCP 工具时使用的脚手架技能。封装项目约定的注册流程、命名规范、租户过滤与权限标注，避免重复样板代码。
disable-model-invocation: true
---

# ai-module-add

为 RuoYi-AI 项目添加 AI 模块元素的标准化脚手架。

## 何时使用

- 在 `ruoyi-modules/ruoyi-chat` 新增一个 Langchain4j AI agent、tool、知识库检索器或对话模板。
- 在 `ruoyi-modules/ruoyi-aiflow` 新增可视化工作流节点（模型调用 / 邮件 / 人工审核 / 自定义）。
- 在 `ruoyi-modules/ruoyi-workflow` 新增 Warm-Flow BPMN 节点定义。
- 把项目内的 service 包装成 MCP tool（暴露给上游客户端调用）。

调用方式：用户明确说"加个 AI agent" / "新加一个工作流节点" / "把这个 service 暴露成 MCP 工具"等场景时使用。

## 输入约定

调用前请用户提供：

1. **目标模块**：`chat` / `aiflow` / `workflow`（决定代码目录）。
2. **元素类型**：`agent` / `tool` / `node` / `mcp-tool`。
3. **元素名**：业务中文名 + 英文 slug（例：「天气查询」→ `weatherQuery`）。
4. **触发场景**：用户场景描述（用于 system prompt 与 README）。

如果调用时缺少以上信息，必须先询问，不要猜测。

## 项目约定（必须遵守）

### 包结构

```
ruoyi-modules/ruoyi-{module}/src/main/java/org/ruoyi/{module}/
├── controller/         # REST 入口，仅做参数校验与装配
├── service/            # 业务编排，不直接持有 Langchain4j 资源
│   └── impl/
├── domain/             # 与数据库交互的实体、VO、BO
├── mapper/             # MyBatis-Plus mapper
├── agent/              # 仅 chat 模块：Langchain4j AiService 接口与装配
├── tool/               # 仅 chat 模块：@Tool 注解方法 + 注册
├── node/               # 仅 aiflow / workflow 模块：节点实现
└── config/             # Bean 注册、Langchain4j wiring
```

### Langchain4j agent 必备模板

```java
public interface WeatherAgent {
    @SystemMessage(fromResource = "prompts/weather-agent-system.txt")
    @UserMessage("{{userQuery}}")
    String chat(@V("userQuery") String userQuery,
                @MemoryId String sessionId,
                @V("locale") String locale);
}
```

- `@SystemMessage` 文案放 `src/main/resources/prompts/<agent>.txt`，**禁止硬编码中文长字符串在 Java 源码里**。
- `MemoryId` 必须是 `String`（Sa-Token 登录态字段类型）。
- 对话历史由 Langchain4j `MessageWindowChatMemory` 自动管理，**不要**自己写 List<Message>。
- 工具暴露通过 `@Tool(name = "...", value = "...")` 标注方法，工具类用 `@Component` 注册到 Spring 容器，由 `ToolProvider` 自动扫描。

### MCP 工具暴露

`ruoyi-chat` 的 MCP server 注册位于 `org.ruoyi.chat.mcp.McpServerConfig`。新增 tool：

1. 在 `tool/` 包新增 `@Component` 类，方法标 `@Tool`。
2. 在 `McpServerConfig` 的 `mcpServer(McpServerTransportProvider, ...)` Bean 里把工具类加进 `List.of(...)`。
3. **必须**给工具加权限检查（`@SaCheckPermission` 或方法内显式 `StpUtil.checkPermission`），不能仅靠路径拦截。

### 工作流节点（aiflow）

- 实现 `AiFlowNodeExecutor` 接口，节点类型常量在 `AiFlowNodeType` 枚举里加。
- 入参 / 出参序列化统一用 `JsonNode`，**不要**自己造 DTO 跨节点传递。
- 长任务（>5s）必须开异步线程池，禁止阻塞 SSE 流。

### 工作流节点（workflow / Warm-Flow）

- 自定义节点实现 `WarmFlowInterceptor` 或注册 `FlowService`。
- 节点 ID 全局唯一，前缀用模块缩写（`chat-`、`flow-`）。

## 命名规范

| 元素 | 命名 |
|---|---|
| Agent 接口 | `<业务>Agent`（例：`WeatherAgent`） |
| Tool 类 | `<业务>Tools`（例：`WeatherTools`） |
| Service | `I<业务>Service` + `impl.<业务>ServiceImpl` |
| Controller | `<业务>Controller` |
| 节点类型枚举值 | `<模块>_<业务>`（例：`CHAT_WEATHER`） |

## 禁止清单

- ❌ 在 agent 代码里 `new ObjectMapper()` 或手动拼 JSON，用 Langchain4j 内置序列化。
- ❌ 把 model API key 写在 Java 源码或 `application.yml`，一律走「模型管理」后台配置。
- ❌ 工具方法不带 `MemoryId` / `sessionId` 隔离，会导致多租户串数据。
- ❌ 绕过 `@SaCheckPermission`（演示模式下 `demo.excludes` 已含部分路径，但权限注解不能省）。
- ❌ 节点内部用 `Thread.sleep` 模拟异步，必须用 `@Async` 或线程池。
- ❌ 新增 Service 不写 `@Tag("dev")` 单测。

## 输出交付物

完成调用后必须产出：

1. 新建的所有文件清单（含相对路径）。
2. 涉及的 `application.yml` / `pom.xml` 改动（如有）。
3. 一段「如何在前端/工作流里触发这个新元素」的说明（10 行内）。
4. 待办测试清单：`@Tag("dev")` Service 单测 + `@SpringBootTest` 集成测试（如果涉及外部依赖）。