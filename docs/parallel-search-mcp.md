# Parallel Search MCP（智能体联网搜索）

Parallel 可以作为智能体的可选联网搜索工具，无需 Parallel 账号或 API Key。
它使用现有的 REMOTE 工具和智能体「关联工具」选择器，不更改已有 Bing
工具、智谱工作流搜索节点或现有智能体的工具选择。

## 配置

1. 在管理端打开「工具管理」，新增一个工具，名称填写 `Parallel Search`，
   类型选择 `REMOTE`，状态选择 `ENABLED`。
2. 配置 JSON 填写：

   ```json
   {
     "baseUrl": "https://search.parallel.ai/mcp"
   }
   ```

3. 保存并使用「测试连接」验证 Streamable HTTP 连接。
4. 编辑需要联网搜索的智能体，在「关联工具」中选择 `Parallel Search` 并保存。
   保留或移除其他关联工具由管理员决定；不会自动替换已有搜索工具。
5. 在聊天界面选择该智能体，明确要求它联网查询并注明来源。
   智能体可以调用 `web_search` 搜索，使用 `web_fetch` 读取公开网页。

取消智能体的工具关联，或在工具管理中将其设为 `DISABLED`，即可停止为其提供这些操作。
只有关联了此工具的智能体才会获得这些操作。原有「用户明确要求浏览器查询」
限制仍然生效。启用后，智能体可在该请求中选择和调用工具。

## 数据与限制

调用时，搜索关键词、目标描述、请求的网页 URL 和工具参数会发送给 Parallel。
连接的 User-Agent 使用稳定项目标识 `ruoyi-ai`，不包含用户、设备或安装标识。
匿名接口有速率限制；无需 API Key 不代表无限使用或服务等级保证。
此配置仅使用匿名 Search MCP，不使用 OAuth、付费 REST API 或模型账号。
不要在 URL 中添加 API Key 或查询参数。

参考：[Parallel Search MCP 文档](https://docs.parallel.ai/integrations/mcp/search-mcp)。
