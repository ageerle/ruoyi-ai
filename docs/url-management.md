# URL 管理

URL 管理用于维护 Copilot 新建编程任务页的常用链接。管理端入口为“系统管理 → URL管理”（`/system/url`），支持新增、编辑、批量删除、名称和状态筛选及分页。

## 安装与权限

已有数据库执行 [增量脚本](script/sql/update/2026-09-01-sys-url.sql)，可重复执行。新安装使用包含 URL 表、菜单及初始链接的全量 `docs/script/sql/ruoyi-ai.sql`。脚本创建 `sys_url` 表和菜单；普通管理角色需分配对应菜单及按钮权限。

| 接口 | 权限 | 用途 |
| --- | --- | --- |
| `GET /system/url/list` | `system:url:list` | 名称、状态筛选和分页 |
| `GET /system/url/{urlId}` | `system:url:query` | 编辑回填 |
| `POST /system/url` | `system:url:add` | 新增 |
| `PUT /system/url` | `system:url:edit` | 编辑 |
| `DELETE /system/url/{urlIds}` | `system:url:remove` | 删除，多个 ID 以逗号分隔 |
| `GET /system/url/shortcuts` | `coding:harness:use` 或 `system:url:list` | 当前租户的已启用链接 |

链接地址仅允许带有效主机名的 HTTP(S) 地址。状态 `0` 表示启用，`1` 表示停用；列表和快捷入口按 `sortOrder`、`urlId` 升序排列。数据查询和修改遵循当前租户隔离。

## 使用

在管理端填写名称、地址、说明、顺序和状态后保存。Copilot 点击“新建编程任务”会刷新快捷入口；停用的链接不显示。卡片显示名称、说明和外链标识，并以 `noopener,noreferrer` 在新标签页打开。加载中、空列表和请求失败均有提示，失败时可重试。

后端实现位于 `ruoyi-modules/ruoyi-system` 的 `SysUrlController`、`SysUrlServiceImpl`；管理端位于 `ruoyi-admin/apps/web-antd/src/views/system/url`；Copilot 使用 `UrlShortcutCards.vue` 和 `useUrlShortcuts.ts`。

## 2026-09-08 最终验收

- 后端打包、管理端构建和 Copilot 构建通过；Copilot 构建包含类型检查。
- URL 接口 14/14，覆盖 CRUD、分页、排序、输入校验、权限和租户隔离。
- Copilot 单元测试 18/18，卡片状态与点击检查 7/7，EXTERNAL 计划工具回归 2/2。
- 增量 SQL 连续执行两次通过，全量 SQL 的 URL 模块在临时隔离库通过。
- 浏览器完成新增、编辑、筛选、停用隐藏、重新启用刷新和卡片说明检查；临时数据已清理。
- 管理端全项目仍有 103 项既有类型诊断，URL 模块为 0 项。

本轮还修复了 EXTERNAL 模式隐藏 `plan_step` 的框架问题，使模型能在证据校验后完成计划。原生运行和计划均已到达 `COMPLETED`。本轮为本地验收；未包含远程部署。新标签页打开参数通过组件测试确认，本轮未新增真实弹窗或手机尺寸实测结论。
