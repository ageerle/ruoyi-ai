# SQL 脚本说明

## 全新安装

执行 [ruoyi-ai.sql](ruoyi-ai.sql) 即可创建 `ruoyi-ai` 主库和 `snail_job` 调度库，并导入初始化数据。

```sh
mysql -uroot -p < ruoyi-ai.sql
```

主 SQL 已合并下表列出的基础更新，以及原始想法、技能绑定、剧本打磨字段、模型优先级和 PC 登录超时结构。新安装仍需执行下面“后续功能更新”中注明的补充脚本。[snail_job_mysql.sql](snail_job_mysql.sql) 仅用于单独初始化调度库，执行主 SQL 后无需再执行它。

全量初始化包含 `DROP TABLE` 和调度库的 `DROP DATABASE`，已有数据库升级应使用对应的增量脚本。

## 已有数据库升级

连接目标主库后，按文件名升序执行尚未应用的更新脚本；不要根据文件重命名重复执行已应用的更新。

- `2026-06-15-trace.sql` 原名为 `update-0615-trace.sql`，内容未变；其中菜单插入没有判重，不应重复执行。
- `2026-08-30-mcp-market-tool-tenant.sql` 是一次性迁移，执行前检查脚本内的预检要求；部分执行失败后应按实际表结构续跑。
- 其余脚本包含重复执行保护，具体适用条件见各脚本注释。

## 命名约定

更新脚本统一采用 `YYYY-MM-DD-功能描述.sql`，功能描述使用小写英文和连字符，例如 `2026-09-01-sys-url.sql`。保留已有更新日期，同日脚本按文件名升序执行，并确保依赖顺序一致。

全量入口保留 `ruoyi-ai.sql`，独立调度库入口保留 `snail_job_mysql.sql`。

## 主 SQL 合并清单

截至 2026-09-01，`update` 目录的 9 个脚本均已体现在主 SQL 的最终表结构和初始化数据中。结构变更直接并入建表语句，重复配置按最终值保留一份；旧库回填及兼容逻辑保留在增量脚本中。

| 更新脚本 | 主 SQL 中对应内容 |
| --- | --- |
| [2026-06-15-trace.sql](update/2026-06-15-trace.sql) | `trace_run`、`trace_node` 表、租户字段和索引，以及链路追踪菜单 |
| [2026-07-20-knowledge-fragment-fid.sql](update/2026-07-20-knowledge-fragment-fid.sql) | 附件 `file_hash`、片段 `fid`、可空的 `doc_id` 和知识库租户索引 |
| [2026-07-21-chat-provider-icon-length.sql](update/2026-07-21-chat-provider-icon-length.sql) | `chat_provider.provider_icon` 扩展为 `varchar(1000)` |
| [2026-07-21-sys-config-node-template.sql](update/2026-07-21-sys-config-node-template.sql) | 7 个工作流节点消息模板配置 |
| [2026-07-24-register-default-role-and-missing-menus.sql](update/2026-07-24-register-default-role-and-missing-menus.sql) | 默认注册角色、角色菜单关联、默认角色配置，以及会话、附件和片段权限 |
| [2026-07-29-sys-config-node-template-all.sql](update/2026-07-29-sys-config-node-template-all.sql) | 工作流全部 8 个模板，前 7 个配置与上一批模板合并去重 |
| [2026-07-29-zhipu-web-search-node.sql](update/2026-07-29-zhipu-web-search-node.sql) | 智谱网络搜索节点及其消息模板；内部组件名保留 `Google` 以兼容已有流程 |
| [2026-08-30-mcp-market-tool-tenant.sql](update/2026-08-30-mcp-market-tool-tenant.sql) | `mcp_market_tool` 租户、审计字段和 `idx_tenant_market` 索引 |
| [2026-09-01-sys-url.sql](update/2026-09-01-sys-url.sql) | `sys_url` 表、5 个菜单权限和 2 条公开链接 |

后续增加更新脚本时，同步修改主 SQL 中对应的表结构或初始化数据，并更新本清单及主 SQL 文件头。

## 后续功能更新

以下脚本统一位于 `update/`。已有数据库按日期升级，保留原始输入、已批准媒体和提供商密钥；不要执行全量初始化。

| 脚本 | 内容与新安装要求 |
| --- | --- |
| [知识库检索节点](update/2026-09-11-knowledge-retrieval-node.sql) | 补充节点注册，新安装也需执行 |
| [短剧视觉资产](update/2026-09-30-short-drama-visual-asset.sql) | 补充异步视觉资产表，新安装也需执行 |
| [剧本打磨与 Seedance 2.5](update/2026-09-30-script-refinement-seedance-25.sql) | 打磨字段已并入全量结构；新安装执行以补充视频模型记录 |
| [技能绑定](update/2026-10-01-short-drama-skill-bindings.sql) | 项目审美与导演绑定，幂等；全量结构已包含 |
| [模型优先级](update/2026-10-02-chat-model-sort-order.sql) | 幂等增加优先级列，保留已自定义优先级；全量结构已包含 |
| [PC 登录空闲时间](update/2026-10-02-pc-login-idle-timeout.sql) | 仅调整原 30 分钟配置；全量默认已更新，已签发令牌需重新登录 |
| [文字模型默认优先级](update/2026-10-03-short-drama-flash-default.sql) | 先应用模型优先级迁移，再调整已有 Flash / Seed 模型排序，不新增模型或密钥 |
| [原始创作输入](update/2026-10-03-short-drama-original-idea.sql) | 幂等增加独立原文列，不用生成概要回填；全量结构已包含 |
