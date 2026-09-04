---
topic: modules/chat-agents-catalog
title: ruoyi-chat — 21 个 Langchain4j Agent 目录
updated: 2026-09-04
raw:
  - raw/chat-source/agents-catalog.md
  - raw/chat-source/chit-chat-agent.md
  - raw/chat-source/chat-service-factory.md
---

# ruoyi-chat — Langchain4j Agent 目录

21+ 个 Langchain4j agent 接口，按职责分类。本篇是 [modules/chat.md](../modules/chat.md) 的 agent 索引扩展。

参见 [agents-catalog.md](../raw/chat-source/agents-catalog.md) 完整文件清单。

## Agent 类型一览（顶层）

7 个核心 agent（按业务领域）：

| Agent | 职责 | 主要工具 / 能力 | 触发场景 |
|---|---|---|---|
| **ChitChatAgent** | 闲聊兜底 | 无工具 | Supervisor 默认落脚点；问候、日常对话 |
| **SqlAgent** | 数据库查询 | 数据库 schema 读取、SQL 生成、执行 | 「查一下用户表中昨天注册的」 |
| **WebSearchAgent** | 联网搜索 | 智智 Web Search API | 实时信息查询（新闻 / 价格） |
| **EchartsAgent** | 图表生成 | Echarts 配置生成 | 数据可视化 |
| **ChartGenerationAgent** | 业务图表 | 图表数据准备 | BI 场景 |
| **SkillsAgent** | 技能调用 | Skills 路由（参见 `SkillsPathResolver`） | 复杂任务分派 |
| **ShortDramaScriptAgent** | 短剧脚本 | 长上下文生成、多角色对话 | 内容创作 |

参见 [chit-chat-agent.md](../raw/chat-source/chit-chat-agent.md) 示例接口定义。

## Agent 配置子包（`agent/config/`）

包括：

- `AgentMysqlConfig` —— MySQL 数据源配置（Agent 专用，与业务库隔离）

## Agent 接口模板（统一约定）

所有 agent 接口遵循：

```java
public interface XxxAgent {
    @SystemMessage("""
        角色：...
        要求：
        - ...
    """)
    @UserMessage("{{query}}")
    @Agent("Agent 描述（给 Supervisor 路由用）")
    String chat(@V("query") String query);
}
```

**关键约定**（详见 [.claude/skills/ai-module-add/SKILL.md](../../.claude/skills/ai-module-add/SKILL.md)）：

1. `@SystemMessage` 文案必须放 `resources/prompts/<agent>.txt`，**禁止硬编码长字符串**
2. `@MemoryId String sessionId` 隔离会话
3. `String chat(@V("query") String query)` 是统一签名
4. `@Agent("...")` 描述必须准确——Supervisor 路由依赖

## Supervisor 编排模式

多个 agent 通过 Supervisor 模式组合：

```
                  ┌─────────────────┐
   用户 query ──► │  Supervisor     │
                  │  (Langgraph4j)  │
                  └────────┬────────┘
                           │
        ┌──────────────────┼──────────────────┐
        ▼                  ▼                  ▼
   ┌─────────┐       ┌──────────┐       ┌─────────┐
   │ ChitChat│       │   Web    │       │   SQL   │
   │  Agent  │       │  Search  │       │  Agent  │
   │ (兜底)  │       │  Agent   │       │         │
   └─────────┘       └──────────┘       └─────────┘
        │                  │                  │
        └──────────────────┴──────────────────┘
                           │
                           ▼
                  Supervisor 汇总输出
```

**Supervisor 决策依据**：
- 用户 query 关键词
- `@Agent("...")` 描述
- 历史 context

**Supervisor 的核心价值**：避免把简单问题派给复杂 agent（省 token + 准确率）。

## Agent 注册与发现

agent 接口用 `@AiService` 注解或 Java Config 注册到 Spring 容器：

```java
@Bean
public ChatLanguageModel chatModel(...) { ... }

@Bean
public ChitChatAgent chitChatAgent(ChatLanguageModel model) {
    return AiServices.builder(ChitChatAgent.class)
        .chatLanguageModel(model)
        .build();
}
```

**注意**：项目里 agent 是**接口**（不是 class）—— Langchain4j 在运行时生成实现类。

## 添加新 Agent 流程

参考 [.claude/skills/ai-module-add/SKILL.md](../../.claude/skills/ai-module-add/SKILL.md)：

1. 在 `org.ruoyi.agent` 创建接口 `XxxAgent`
2. 写 `@SystemMessage` 文案 → `resources/prompts/xxx-agent.txt`
3. 在 Spring Config 注册（`@Bean` 方法）
4. 若 Supervisor 需要路由 → 更新 Supervisor 的判断逻辑
5. 写 `@Tag("dev")` 单测（用 [gen-test](../../.claude/skills/gen-test/SKILL.md) skill）

## Agent 测试

Agent 单测用 Langchain4j 提供的 `AgentTester` 或 mock `ChatLanguageModel`：

```java
@Tag("dev")
class ChitChatAgentTest {
    @Test
    void chat_respondsInSameLanguage() {
        // mock ChatLanguageModel 返回固定响应
        // 验证 agent 接口被正确调用
    }
}
```

## 与 MCP 工具的关系

Agent 通过 `@Tool` 注解的方法**调用 MCP 工具**：

```java
public interface WeatherAgent {
    @Tool("查询天气")
    String getWeather(@P("城市") String city);
}
```

Agent 工具通过 `LangChain4jMcpToolProviderService` 加载（参见 [modules/chat.md § MCP 工具暴露核心](../modules/chat.md)）。

## 已知约束

- **prompt 长度**：每个 agent 的 system prompt < 2000 tokens，否则每次请求都浪费
- **工具数量**：单 agent 暴露 > 20 个工具时，LLM 决策准确率下降
- **多租户**：agent 工具方法必须 `@MemoryId String tenantId` 隔离
- **流式响应**：agent 默认返回 `String`，需要 streaming 时用 `TokenStream`
- **Supervisor 路由决策**：完全依赖 `@Agent` 描述准确 + LLM 判断，需充分测试