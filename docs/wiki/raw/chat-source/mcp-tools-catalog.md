---
source: file:///Users/mac/Documents/ruoyi-ai/ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/mcp/tools/
collected: 2026-09-04
published: 2026-09-04
topic: chat-source
---

# 内置 MCP 工具清单（6 个）

`org.ruoyi.mcp.tools` 包下的内置 MCP 工具（暴露给 Langchain4j agents）：

## 完整文件清单

```
DeleteFileTool
EditFileTool
ExecuteCommandTool
ListDirectoryTool
ReadFileTool
WriteFileTool
```

## 完整目录路径

```
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/mcp/tools/DeleteFileTool.java
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/mcp/tools/EditFileTool.java
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/mcp/tools/ExecuteCommandTool.java
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/mcp/tools/ListDirectoryTool.java
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/mcp/tools/ReadFileTool.java
ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/mcp/tools/WriteFileTool.java
```

## 注册位置

通过 `LangChain4jMcpToolProviderService` 加载（参见 raw/chat-source/mcp-tool-provider-service.md）。
