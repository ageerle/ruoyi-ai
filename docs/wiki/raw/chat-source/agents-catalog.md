---
source: file:///Users/mac/Documents/ruoyi-ai/ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/agent/
collected: 2026-09-04
published: 2026-09-04
topic: chat-source
---

# Langchain4j Agent 目录清单（21+）

`org.ruoyi.agent` 包下的所有 Langchain4j agent 接口文件（按职责分组）：

## 顶层 agent（接口 + @SystemMessage + @UserMessage）

ChartGenerationAgent
ChitChatAgent
EchartsAgent
ShortDramaScriptAgent
SkillsAgent
SqlAgent
WebSearchAgent

## config 子包（agent 配置）

AgentMysqlConfig
AgentMysqlProperties

## 完整目录路径

```
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/agent/ChartGenerationAgent.java
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/agent/ChitChatAgent.java
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/agent/config/AgentMysqlConfig.java
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/agent/config/AgentMysqlProperties.java
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/agent/domain/ColumnInfo.java
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/agent/domain/Condition.java
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/agent/domain/Query.java
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/agent/domain/Result.java
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/agent/domain/SchemaResult.java
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/agent/domain/TableListResult.java
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/agent/domain/TableStructure.java
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/agent/EchartsAgent.java
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/agent/manager/TableSchemaInitializer.java
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/agent/manager/TableSchemaManager.java
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/agent/ShortDramaScriptAgent.java
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/agent/SkillsAgent.java
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/agent/SqlAgent.java
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/agent/tool/ExecuteSqlQueryTool.java
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/agent/tool/QueryAllTablesTool.java
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/agent/tool/QueryTableSchemaTool.java
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/agent/WebSearchAgent.java
```
