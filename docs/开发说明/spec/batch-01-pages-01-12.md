# 01. 登录页

### 1. 页面元信息
- 编号 / 名称 / URL：01 / 登录页 / `/` 或 `/login`
- 主用角色：未登录访问者（product_manager / product_lead / super_admin / 游客）
- 可见角色：所有未登录访问者
- 优先级：P0
- 关联 BR：BR-USER-04 / BR-USER-05 / BR-AUD-01
- 现有实现：部分（`LoginPage` in `App.jsx:63-82`，账号密码 + 企微 Mock Tab 完整；缺 refreshToken、缺冻结态拦截、缺 MFA/captcha）

### 2. 页面布局
- 区块顺序：左 brand 区（`login-brand` 含产品阶段点）→ 右 login-panel（双 Tab `account` / `wecom`）→ 底部游客需求入口 → 演示账号 5 按钮
- 主要组件：`<LoginPage>`、`<GuestDemandModal>`、`<RegisterPage>`（`/register` 邀请注册）
- 关键视觉元素：姓名账号 Tab、企微扫码 Tab、5 个演示账号快捷（傅志谦/段进科/程龙/杨波/肖敬龙）、企微本地演示 8 账号、游客需求入口按钮

### 3. 字段模型
| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---|---|---|---|---|
| `mode` | string | — | `account` / `wecom` | local | `account` | — |
| `username` | string | 是 | 姓名或「姓名-工号」 | local | `傅志谦` | 非空、≤64 |
| `password` | string | 是 | — | local | `IPD@2026` | bcrypt 12 round |
| 🔴 `captchaToken` | string | 视配置 | — | client | `""` | 5 次失败后强制 |
| 🔴 `wecomUserId` | string | 条件 | — | wecom OAuth callback | — | 须先 bind |
| 🔴 `mfaCode` | string | 条件 | 6 位数字 | local | — | 启用 MFA 时 |
| 🔴 `deviceFingerprint` | string | 否 | — | client | — | 写审计 |
| `error` / `busy` | — | — | — | local | `""` / false | — |

### 4. API 接口清单
- `POST /api/auth/login`（现有）
  - 请求：`{username, password}`；🔴 响应需补 `{accessToken(15min), refreshToken(7d), user}`
  - 错误码：20001 未登录 / 20002 token过期 / 20003 refresh失效 / 30001 无权限 / 50001 账号不存在
  - 权限要求：未登录
  - 审计写入：`entityType=user, action=login_success / login_failed / login_blocked_frozen`
- 🔴 `POST /api/auth/refresh`（v3 缺）
- 🔴 `GET /api/auth/wecom/url`（v3 缺，Mock 返回本地模拟页）
- 🔴 `GET /api/auth/wecom/callback?code=`（v3 缺）
- 🔴 `POST /api/auth/wecom/bind` / `unbind`（v3 缺）
- `POST /api/auth/wecom/start`（现有）
- `POST /api/auth/wecom/demo/login`（现有，仅本地演示）
- `GET /api/auth/wecom/demo/accounts`（现有）

### 5. 闭环剧本
| 环节 | 内容 |
|---|---|
| ① 发起 | 用户在 `/login` 提交账号密码 → `POST /api/auth/login`；或企微 Tab 跳 `GET /api/auth/wecom/url` → Mock 授权 → callback → bind 后签发 JWT |
| ② 处理 | 服务端 bcrypt(12) 校验 username/passwordHash；校验 `accountStatus`（ACTIVE 放行，FROZEN_PENDING_HANDOVER 仅移交可达，DISABLED 返回 30001） |
| ③ 审核 | 无审批；但服务端必写审计 `entityType=user, action=login_success/failed/blocked_frozen` 含 IP+UA+deviceFingerprint |
| ④ 结果 | 登录成功 → `AppRoot` 调 `/api/auth/me` → 若 `mustChangePwd=true` 强制渲染 `FirstLoginPage`；否则调 `/api/bootstrap` 进入工作台 |
| ⑤ 记录 | audit_logs 仅 INSERT（PG 层 REVOKE UPDATE,DELETE FROM ipd_app，TS-08/G-11）；含 prevHash+currHash SHA256 链 |
| ⑥ 归档 | 失败登录累计 5 次锁定账号 30min 并通知产品组长；refreshToken 7d 到期前 1 天提醒；登出销毁服务端会话 |

### 6. 现状 vs v3 差距
- **缺失字段（4 项）**：`captchaToken` / `wecomUserId` / `mfaCode` / `deviceFingerprint`
- **缺失接口（5 项）**：`/api/auth/refresh` / `/api/auth/wecom/url` / `/api/auth/wecom/callback` / `/api/auth/wecom/bind` / `/api/auth/wecom/unbind`
- **缺失状态机环节（3 个）**：① 冻结态拦截分支（30001 + 仅移交页面白名单）② MFA/captchaToken 升级 ③ refreshToken 续签与注销
- **验收覆盖度**：现状 3 条 AC / v3 应有 6 条 AC（缺冻结态 + MFA + refreshToken + 企微 callback 4 条）

### 7. 验收用例
```
Given 临时密码用户 mustChangePwd=true
When 提交正确账号密码
Then POST /api/auth/login 返回 mustChangePwd=true 且 accessToken+refreshToken
And 写入审计 entityType=user, action=login_success, after={mustChangePwd:true}
And AppRoot 渲染 FirstLoginPage 而非工作台
```
```
Given accountStatus=FROZEN_PENDING_HANDOVER 状态人员登录
When 提交账号密码
Then 返回 30001；审计 entityType=user, action=login_blocked_frozen
And 仅可访问 /handovers（移交权限白名单）
```

---

# 02. 首次登录强制改密

### 1. 页面元信息
- 编号 / 名称 / URL：02 / 首次登录强制改密 / 无独立 URL，`AppRoot` 在 `me.mustChangePwd===true` 时强制渲染
- 主用角色：临时密码首次登录的任意角色（product_manager / product_lead / super_admin）
- 优先级：P0
- 关联 BR：BR-USER-04
- 现有实现：完整（`FirstLoginPage` in `App.jsx:99-117`；缺密码历史去重、缺密码强度实时反馈）

### 2. 页面布局
- 区块顺序：左 brand（首次登录安全设置文案）→ 右 login-panel（managed-profile-card 只读身份卡 + 三字段表单 + 退出次按钮）
- 主要组件：`<FirstLoginPage>`、`<managed-profile-card>`

### 3. 字段模型
| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---|---|---|---|---|
| `currentPassword` | string | 是 | — | local | `IPD@2026` | 与服务端 hash 一致 |
| `newPassword` | string | 是 | — | local | `""` | ≥8、与 current 不同、与最近 3 次不同 |
| `confirmPassword` | string | 是 | — | local | `""` | = newPassword |
| 🔴 `passwordStrength` | number | — | 0-100 | client 计算 | — | 含大小写+数字+符号 |
| 🔴 `oldPasswordHistoryHash` | string[] | 否 | — | server | — | 排除最近 3 次 |
| `fieldErrors.*` | string | — | — | API/客户端 | — | — |

### 4. API 接口清单
- `POST /api/auth/change-password`（现有）
  - 请求：`{currentPassword, newPassword}`；响应：`{user: {..., mustChangePwd: false}}`
  - 错误码：10001 字段缺失 / 10003 格式错 / 20001 未登录 / 50001 账号不存在
  - 审计写入：`entityType=user, action=password_changed`，before/after 含 hash
- `POST /api/auth/logout`（现有）
- 🔴 `GET /api/auth/password-policy`（v3 缺）

### 5. 闭环剧本
| 环节 | 内容 |
|---|---|
| ① 发起 | `me.mustChangePwd=true` 触发 `AppRoot` 渲染 `FirstLoginPage` |
| ② 处理 | 客户端校验三项；调用 `/api/auth/password-policy` 取最新策略 |
| ③ 审核 | 无审批；服务端校验密码历史（最近 3 次去重） |
| ④ 结果 | `/api/auth/change-password` 成功后 `mustChangePwd=false`、更新 `passwordHash + passwordUpdatedAt`、记录 passwordHistory；`onComplete` 触发 `/api/bootstrap` |
| ⑤ 记录 | `entityType=user, action=password_changed`，before/after 含 hash；失败写 `action=password_change_failed` |
| ⑥ 归档 | 旧密码哈希立即作废；新密码加入 history；保留 90 天失败尝试滚动窗口 |

### 6. 现状 vs v3 差距
- **缺失字段（2 项）**：`passwordStrength` / `oldPasswordHistoryHash`
- **缺失接口（1 项）**：`/api/auth/password-policy`
- **缺失状态机环节（3 个）**：① 密码历史去重校验 ② 密码强度客户端实时反馈 ③ 密码修改后 24h 内不可再次修改
- **验收覆盖度**：现状 3 条 AC / v3 应有 5 条 AC（缺"近 3 次去重"+"密码强度可视化"2 条）

### 7. 验收用例
```
Given 临时账号首次登录 mustChangePwd=true
When 输入正确临时密码 + 合法新密码 + 确认
Then POST /api/auth/change-password 返回 mustChangePwd=false
And 审计写入 entityType=user, action=password_changed
And 自动进入工作台
```
```
Given 新密码与最近 3 次中某次重复
When 提交
Then 返回 10003 格式错；fieldErrors.newPassword 显示「请勿使用近 3 次已用过的密码」
And 审计 entityType=user, action=password_change_failed
```

---

# 03. 工作台

### 1. 页面元信息
- 编号 / 名称 / URL：03 / 工作台 / `/workspace`
- 主用角色：全部已登录
- 可见角色：product_manager / product_lead / super_admin
- 优先级：P0
- 关联 BR：BR-WF-01 / BR-WF-03 / BR-HAND-04 / BR-GATE-04 / BR-TEAM-06 / BR-KPI-05 / BR-INC-11
- 现有实现：完整（`WorkflowCenter` in `ClosurePages.jsx:20-36` + `GovernancePage` in `FinalRulesPages.jsx:42-48`）

### 2. 页面布局
- 区块顺序：顶栏（`<Frame>` title=greeting）→ metric-strip（4 卡）→ workflow-tabs（5 bucket）→ workflow-layout（左任务列表 + 右 my-current-task 侧栏）→ governance 区
- 主要组件：`<Frame>`、`<WorkflowCenter>`、`<GovernancePage>`、`<Metric>`、`<Blank>`
- 关键视觉元素：5 bucket tab（pending/initiated/overdue/completed/saved）；按 project_name 分组的 task-project-card

### 3. 字段模型
| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---|---|---|---|---|
| `bucket` | string | — | pending/initiated/overdue/completed/saved | local | `pending` | — |
| `data.tasks` | array | — | taskTypeNames 17 类枚举 | `/api/workflow/tasks?bucket=` | `[]` | — |
| `data.stats` | object | — | `{pending,overdue,unread,completed}` | 同上 | `{}` | — |
| `saved` | array | — | saved items | `/api/saved-items` | `[]` | — |
| 🔴 `pendingType` | object | — | 17 类细分枚举 | server | — | 见下 |
| 🔴 `noOutputAlerts` | array | — | 60 天无产出提醒 | server | `[]` | BR-INC-11 |
| `loading` / `nextAction` | — | — | — | local / boot | true / — | — |

🔴 17 类 taskType：`stage_sign` / `key_gate` / `key_gate_arbitration` / `deletion_review` / `waiver_review` / `handover` / `rd_replacement` / `contribution_confirm` / `receipt_review` / `retirement_review` / `strategic_change` / `capacity_approval` / `kpi_fill` / `change_implementation` / `change_verify` / `bonus_lock` / `closeout`

### 4. API 接口清单
- `GET /api/workflow/tasks?bucket=`（现有）
- `GET /api/saved-items`（现有）
- `GET /api/bootstrap?projectId=...`（现有）
- 🔴 `GET /api/workflow/tasks?bucket=overdue&type=&limit=`（v3 缺 type 过滤）
- 🔴 `GET /api/performance/no-output-alerts`（v3 缺，BR-INC-11）
- 🔴 `POST /api/notifications/read-all`（v3 缺统一通知中心）
- 审计写入：`entityType=workflow, action=view_bucket / click_task / push_overdue`

### 5. 闭环剧本
| 环节 | 内容 |
|---|---|
| ① 发起 | 登录后 `AppRoot` 触发 `/api/bootstrap` → `<WorkflowCenter>` 挂载 → 拉 `/api/workflow/tasks?bucket=pending` |
| ② 处理 | 服务端按 user + role 聚合 17 类待办（stage_sign / key_gate / key_gate_arbitration / deletion_review / waiver_review / handover / rd_replacement / contribution_confirm / receipt_review / retirement_review / strategic_change / capacity_approval / kpi_fill / change_implementation / change_verify / bonus_lock / closeout） |
| ③ 审核 | 工作台本身不审批；点击任务卡进入业务详情（`<GenericTaskWorkspace>` / `<HandoffWorkbench>` / `<PerformanceV31Panel>` / `<CapacityApprovalPanel>`） |
| ④ 结果 | `navigate(item.deep_link)`；状态变更后服务端再次推送新 task 至 pending 桶 |
| ⑤ 记录 | bucket 切换写 `entityType=workflow, action=view_bucket`；任务点击写 `action=click_task` |
| ⑥ 归档 | `saved` bucket 收藏对象进入 `saved_items` 表；完成态任务 7 天后归档保留痕迹 |

### 6. 现状 vs v3 差距
- **缺失字段（2 项）**：`pendingType` 17 类细分 / `noOutputAlerts` 数组
- **缺失接口（3 项）**：`/api/workflow/tasks?type=` type 过滤 / `/api/performance/no-output-alerts` / 统一通知接口
- **缺失状态机环节（9 类）**：缺 key_gate_arbitration / waiver_review / deletion_review / retirement_review / strategic_change / capacity_approval / receipt_review / bonus_lock / closeout 9 类任务投递
- **验收覆盖度**：现状 3 条 AC / v3 应有 5 条 AC（缺 no_output_alert 自动投递 + 17 类细分 2 条）

### 7. 验收用例
```
Given product_manager 登录后命中 /workspace
When WorkflowCenter 挂载
Then GET /api/workflow/tasks?bucket=pending 返回任务列表与 stats
And 4 张 metric 卡渲染：待我处理 / 临期超期 / 未读通知 / 已完成
```
```
Given 某轻管动作 D11 BioCV 评测到期前 3 天
When 系统扫描
Then 该任务投递到 overdue 桶并标 priority=high
And 审计 entityType=task, action=push_overdue, after={taskId, dueDate}
```

---

# 04. 删除审核-我的申请

### 1. 页面元信息
- 编号 / 名称 / URL：04 / 删除审核-我的申请 / `/admin`（deletion-review 子区）+ `/projects` 中 `DeleteProjectModal` 入口（`App.jsx:261-265`）
- 主用角色：发起删除申请的 product_manager / product_lead / super_admin
- 可见角色：全部（仅看自己的申请）
- 优先级：P1
- 关联 BR：BR-DEL-01 / BR-DEL-03 / BR-DEL-04 / BR-DEL-06 / G-02
- 现有实现：完整（`DeleteProjectModal` in `App.jsx:261-265` + `ProjectsPage` 中 `deletion-review` section）

### 2. 页面布局
- 区块顺序：项目空间 → 「申请删除」次按钮 → 弹窗（`DeleteProjectModal`）→ 提交后回到 `/projects` 加载 `/api/deletion-requests`
- 主要组件：`<DeleteProjectModal>`、`<deletion-review>`
- 关键视觉元素：删除原因 textarea 必填 ≥6 字符；按钮 `danger-action` 红色

### 3. 字段模型
| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---|---|---|---|---|
| `targetType` | enum | 是 | `project` / `gate_record` / `requirement` / `deliverable` | modal state | `project` | — |
| `targetId` | string | 是 | 目标 id | props | — | — |
| `reason` | string | 是 | — | modal state | `""` | minLength=6 |
| `replacementNote` | string | 否 | — | modal state | 默认 | — |
| 🔴 `criticality` | enum | 是 | `critical` / `normal` | server 推断 | `critical` | BR-DEL-01/02 |
| 🔴 `withdrawDeadline` | DateTime | — | submit + deletion.withdrawHours=24h | server | — | BR-DEL-04 |
| 🔴 `attachments` | File[] | 否 | — | client | `[]` | 替代资料凭证 |

### 4. API 接口清单
- `POST /api/projects/:id/delete-request`（现有）
- `GET /api/deletion-requests`（现有）
- `POST /api/deletion-requests/:id/decision`（现有）
- 🔴 `POST /api/deletion-requests/:id/withdraw`（v3 缺，BR-DEL-04 24h 撤回）
- 🔴 `GET /api/deletion-requests?targetType=gate_record|requirement|deliverable|project&status=pending`（v3 缺，BR-DEL-01 通用化）
- 🔴 `POST /api/deletion-requests/:id/escalate`（v3 缺，超期升级超级管理员）
- 审计写入：`entityType=deletion_request, action=submit / withdraw / approve / reject / archive / escalate`

### 5. 闭环剧本
| 环节 | 内容 |
|---|---|
| ① 发起 | `/projects` 点「申请删除」→ `DeleteProjectModal` → `POST /api/projects/:id/delete-request` |
| ② 处理 | 服务端推断 criticality=critical 走 BR-DEL-01 双层；写 deletion_requests；设置 `withdrawDeadline = now + deletion.withdrawHours=24h` |
| ③ 审核 | `deletion.leaderDeadlineDays=2` 组长初审 → `deletion.adminDeadlineDays=2` 超级管理员终审；状态 LEADER_REVIEW → ADMIN_REVIEW → DELETED/REJECTED |
| ④ 结果 | 两级通过 → `deletedAt` 置位（软删除）→ 移入归档区；申请人 24h 内可撤回 |
| ⑤ 记录 | submit / withdraw / approve / reject / archive / escalate 全链路写 audit_logs，before/after 含 reason 与 status |
| ⑥ 归档 | 项目保留在 boot.projects 但 lifecycle=ARCHIVED；ProjectCircle/Requirements 只读；附 archive 区 |

### 6. 现状 vs v3 差距
- **缺失字段（3 项）**：`criticality` / `withdrawDeadline` / `attachments`
- **缺失接口（3 项）**：`/api/deletion-requests/:id/withdraw` / `/api/deletion-requests?targetType=` / `/api/deletion-requests/:id/escalate`
- **缺失状态机环节（3 个）**：① 撤回窗口（24h）② 超期升级 ③ 归档区二次彻底清除
- **验收覆盖度**：现状 3 条 AC / v3 应有 5 条 AC（缺"24h 撤回"+"超期升级"2 条）
- **现状不足**：`targetType` 仅支持 `project`，v3 要求通用化（gate_record / requirement / deliverable）

### 7. 验收用例
```
Given 项目 owner 发起删除申请
When POST /api/projects/:id/delete-request 返回 status=LEADER_REVIEW
And 系统写入 entityType=deletion_request, action=submit, before={status:ACTIVE}, after={status:LEADER_REVIEW}
And 设置 withdrawDeadline = submit_at + 24h
```
```
Given 申请提交后 12h 内申请人点击撤回
When POST /api/deletion-requests/:id/withdraw
Then status=REJECTED（withdrawn）；项目恢复 ACTIVE
And 审计 entityType=deletion_request, action=withdraw
```

---

# 05. 删除审核-待我审核

### 1. 页面元信息
- 编号 / 名称 / URL：05 / 删除审核-待我审核 / `/admin`（deletion-review 子区）
- 主用角色：product_lead（初审）、super_admin（终审）
- 可见角色：`product_lead` 看本组、`super_admin` 看全部
- 优先级：P1
- 关联 BR：BR-DEL-01 / BR-DEL-02 / BR-DEL-04 / BR-DEL-05
- 现有实现：完整（`ProjectsPage` 中 `deletion-review` section）

### 2. 页面布局
- 区块顺序：scope-banner → project-summary → stage-list → 底部「项目删除审核」section
- 主要组件：`<deletion-review>`、`<DecisionChainPanel>` 思路复用
- 关键视觉元素：`deletion-row` 三列；criticality 标识

### 3. 字段模型
| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---|---|---|---|---|
| `deletions` | array | — | deletion_request | `/api/deletion-requests` | `[]` | — |
| `item.status` | enum | — | DRAFT/LEADER_REVIEW/ADMIN_REVIEW/DELETED/REJECTED | server | — | — |
| `item.requesterName` / `item.reason` | string | — | — | server | — | — |
| `item.leadDecision` / `item.adminDecision` | enum | — | PENDING/APPROVED/REJECTED | server | — | — |
| 🔴 `item.criticality` | enum | — | critical/normal | server | `critical` | — |
| 🔴 `item.leadDeadline` | DateTime | — | submit + deletion.leaderDeadlineDays=2 工作日 | server | — | BR-DEL-04 |
| 🔴 `item.adminDeadline` | DateTime | — | lead_approved + deletion.adminDeadlineDays=2 工作日 | server | — | BR-DEL-04 |

### 4. API 接口清单
- `GET /api/deletion-requests`（现有）
- `POST /api/deletion-requests/:id/decision`（现有）
- 🔴 `GET /api/deletion-requests?approverRole=lead&status=pending`（v3 缺角色过滤）
- 🔴 `GET /api/deletion-requests/:id/audit-trail`（v3 缺）
- 审计写入：`entityType=deletion_request, action=approve / reject / archive / escalate`

### 5. 闭环剧本
| 环节 | 内容 |
|---|---|
| ① 发起 | product_manager 提交删除申请 → 通知 product_lead；product_lead 进入 `/admin` 加载 `/api/deletion-requests?approverRole=lead&status=pending` |
| ② 处理 | 渲染待我处理 `deletion-row`；product_lead 可 approve 或 reject；criticality=normal 仅一级 |
| ③ 审核 | product_lead 2 工作日内审批（`deletion.leaderDeadlineDays=2`）；通过后投递 super_admin；super_admin 2 工作日内终审（`deletion.adminDeadlineDays=2`） |
| ④ 结果 | approve：项目 lifecycle=ARCHIVED；reject：status=REJECTED；超期升级 super_admin |
| ⑤ 记录 | `entityType=deletion_request, action=approve/reject/archive`，before/after 含 status |
| ⑥ 归档 | 软删除项目仍出现在 `boot.projects` 但 lifecycle=ARCHIVED；archive 区仅 super_admin 可二次彻底清除 |

### 6. 现状 vs v3 差距
- **缺失字段（3 项）**：`criticality` / `leadDeadline` / `adminDeadline`
- **缺失接口（2 项）**：`/api/deletion-requests?approverRole=` / `/api/deletion-requests/:id/audit-trail`
- **缺失状态机环节（2 个）**：① deadline 倒计时显示 ② 超期升级触发器
- **验收覆盖度**：现状 3 条 AC / v3 应有 5 条 AC（缺"deadline 倒计时"+"超期自动升级"2 条）
- **现状不足**：`decision` 接口同时承担两个 role，缺乏阶段推进判断

### 7. 验收用例
```
Given product_lead 身份进入 /admin
When GET /api/deletion-requests?approverRole=lead&status=pending 返回 pending 项
Then deletion-review 区域渲染对应行；显示 criticality 标识与 leadDeadline 倒计时
And 按钮仅在 canReviewDeletion=true 时出现
```
```
Given product_lead 2 个工作日未审
When 定时任务扫描
Then 升级 super_admin 并写 entityType=deletion_request, action=escalate
```

---

# 06. 审计日志

### 1. 页面元信息
- 编号 / 名称 / URL：06 / 审计日志 / `/admin`（audit tab，`<AuditPanel>` in `App.jsx:478-484`）+ 项目级 `/timeline`（`TimelinePage` in `App.jsx:340-358`）
- 主用角色：super_admin（全量）+ product_lead（本组）+ product_manager（仅本人相关）+ 游客（无权限）
- 可见角色：分层可见
- 优先级：P0
- 关联 BR：BR-AUD-01 ~ BR-AUD-04 / BR-ORG-06 / TS-08
- 现有实现：部分（缺 hash 链校验接口）

### 2. 页面布局
- 区块顺序：
  - 全局审计（`/admin`）：filters → audit-list → drawer 详情
  - 项目轨迹（`/timeline`）：metric-strip → 双PM 责任摘要 → 阶段列表 → 性能/激励区 → drawer
- 主要组件：`<AuditPanel>`、`<TimelinePage>`、`<TimelineActionDrawer>`

### 3. 字段模型
| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---|---|---|---|---|
| `filters.projectId` / `actorId` / `actionType` / `dateFrom` / `dateTo` | string/date | 否 | — | local | `""` | — |
| 🔴 `filters.entityType` | string | 否 | enum | local | `""` | v3 缺 |
| 🔴 `data.logs[].prevHash` / `currHash` | string | — | SHA256 | server | — | hash 链 |
| 🔴 `data.logs[].operatorRole` | enum | — | super_admin / product_lead / product_manager / market_pm / rd_pm / guest | server | — | v3 缺 |
| 🔴 `data.verifyResult` | object | — | `{brokenAt, repairedAt}` | `/api/audit-logs/verify` | null | hash 校验 |

### 4. API 接口清单
- `GET /api/audit-logs?projectId=&actorId=&actionType=&dateFrom=&dateTo=&entityType=&scope=`（v3 路径前缀 `/api/audit-logs/*`）
- 🔴 `GET /api/audit-logs/verify`（v3 缺，BR-AUD-02 hash 链校验）
- `GET /api/audit-logs/export?...`（v3 路径）
- `GET /api/projects/:id/timeline`（现有）
- `GET /api/projects/:id/timeline/actions/:workItemId`（现有）
- 🔴 `GET /api/audit-logs?scope=mine|team|all`（v3 缺，BR-AUD-03 分层）
- 审计写入：`entityType=audit_log, action=verify_hash_chain`

### 5. 闭环剧本
| 环节 | 内容 |
|---|---|
| ① 发起 | 任意业务动作落库时服务端同步写 `audit_logs`（含 hash 链） |
| ② 处理 | `/admin` audit tab → `GET /api/audit-logs?scope=mine|team|all&...` 按 filter 拉取 |
| ③ 审核 | super_admin / product_lead 可导出 CSV；不可修改/删除；super_admin 可调 `/api/audit-logs/verify` 校验 hash 链 |
| ④ 结果 | drawer 展示详情；导出 CSV；verify 返回断裂点或全链 OK |
| ⑤ 记录 | 每次查看/导出/校验再写一条 `entityType=audit_log, action=verify_hash_chain`（审计的审计） |
| ⑥ 归档 | 项目结项后 `/timeline` 显示「全部只读」；super_admin 可配置归档策略（默认 3 年后转冷存储，BR-AUD-04） |

### 6. 现状 vs v3 差距
- **缺失字段（5 项）**：`entityType` 过滤 / `prevHash` / `currHash` / `operatorRole` / `verifyResult`
- **缺失接口（2 项）**：`/api/audit-logs/verify` / `?scope=mine|team|all` 分层
- **缺失状态机环节（3 个）**：① hash 链断裂点定位 ② 归档策略配置入口 ③ 审计的审计（view/export 自写审计）
- **验收覆盖度**：现状 3 条 AC / v3 应有 5 条 AC（缺 hash 链断裂定位 + 审计自审计 2 条）
- **现状不足**：缺统一 `@Audit()` 装饰器切面（TS-03）

### 7. 验收用例
```
Given super_admin 进入 /admin 切到 audit tab
When 调用 GET /api/audit-logs?scope=all&entityType=deletion_request&dateFrom=2026-09-01
Then 返回 logs；分页结构 {list,total,page,size}
And 列表按时间倒序渲染；每行包含 prevHash+currHash
```
```
Given 任何角色点击"校验 hash 链"
When 调用 GET /api/audit-logs/verify
Then 返回 {ok: true, total, brokenAt: null} 或 {ok: false, brokenAt: seq}
And 写入 entityType=audit_log, action=verify_hash_chain
```

---

# 07. 我的项目-列表

### 1. 页面元信息
- 编号 / 名称 / URL：07 / 我的项目-列表 / `/projects`
- 主用角色：全部已登录
- 可见角色：按 `boot.access.projectScope` 过滤
- 优先级：P0
- 关联 BR：BR-PROJ-01 / BR-PROJ-02 / BR-ORG-06 / BR-PROD-01
- 现有实现：完整（`ProjectsPage` in `App.jsx:241-249`）

### 2. 页面布局
- 区块顺序：`<PageFrame>` → `scope-banner` → `catchup-banner` → `project-summary`（4 卡 + KPI 入口）→ `stage-list`（六阶段折叠面板）→ `CloseoutReadiness`（超级管理员）→ `other-projects`
- 主要组件：`<ProjectsPage>`、`<CloseoutReadiness>`、`<CreateProjectModal>`、`<DeleteProjectModal>`、`<RdReplacementDeepLink>`

### 3. 字段模型
| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---|---|---|---|---|
| `expanded` / `createOpen` / `deleteOpen` | bool/string | — | — | local | false / concept | — |
| `deletions` | array | — | deletion_request | `/api/deletion-requests` | `[]` | — |
| `canCreate` / `canReviewDeletion` | bool | — | — | derived | — | — |
| `scopeText` | string | — | all / team / mine | server | — | — |
| 🔴 `project.targetSalesAmount` | decimal | 是 | 元 | server | — | bonus.poolBase=TARGET_SALES |
| 🔴 `project.targetChannelCount` | int | 是 | — | server | — | K02 |
| 🔴 `project.targetNps` | int | 是 | 0-100 | server | — | K03 |
| 🔴 `project.targetSceneCount` | int | 是 | — | server | — | K04 |
| 🔴 `project.launchDate` | DateTime | — | — | server | null | bonus.launchAnchor=L08_ACTION |
| 🔴 `project.levelCoefficient` | decimal | — | — | server | — | C09 提议 |
| 🔴 `project.levelCoefficientReason` | string | — | — | server | — | S/B 必填 |
| 🔴 `project.targetMarkets` | string[] | — | 国家代码 | server | `[]` | M1 认证带出 |
| 🔴 `project.source` | enum | — | NEW / LEGACY | server | — | — |
| 🔴 `project.mainGroupId` | string | — | — | server | — | BR-ORG-01 |

### 4. API 接口清单
- `GET /api/projects?scope=`（现有，由 `/api/bootstrap` 提供）
- `GET /api/deletion-requests`（现有）
- `POST /api/deletion-requests/:id/decision`（现有）
- `GET /api/projects/create-options`（现有）
- `GET /api/projects/:id/closeout-readiness`（现有）
- 🔴 `GET /api/projects/:id`（v3 缺独立详情）
- 🔴 `GET /api/projects/:id/members`（v3 缺双PM 绑定）
- 🔴 `GET /api/projects/:id/cert-checklist?markets=`（v3 缺 BR-IPD-05b）
- 🔴 `POST /api/projects/:id/stage-advance`（v3 缺 BR-IPD-06 门禁校验）

### 5. 闭环剧本
| 环节 | 内容 |
|---|---|
| ① 发起 | 顶栏下拉切换项目 → `refresh(projectId)` → `/api/bootstrap` 重新加载 |
| ② 处理 | 阶段列表点击展开 → 按 workItem 渲染状态点；super_admin 可触发 `CloseoutReadiness` 校验 |
| ③ 审核 | 创建/删除按钮触发 modal；product_lead / super_admin 进入 deletion-review 处理 |
| ④ 结果 | 切换项目后侧栏/顶栏/工作台同步刷新；进入 `/action?workItem=...` 走深管动作 |
| ⑤ 记录 | `entityType=project, action=switched_context / view_summary`；删除决策写 `action=approve/reject` |
| ⑥ 归档 | `other-projects` 列出 ARCHIVED 但禁用点击；super_admin 可正式结项（status=ARCHIVED） |

> ⚙️ 跨页守护（D.6.6 第 2 条）：14 天场景定义复核倒计时（BR-PROD-04）—— 07 项目列表页 `GET /api/projects?catchup=true` 返回字段 `sceneReviewDeadlineAt`：存量补录项目的 banner 显示倒计时（14 天），超期未复核显示红点 ⚠️ 并触发升级；状态变化写入审计 `entityType=project, action=update`（补录状态子动作）。

### 6. 现状 vs v3 差距
- **缺失字段（10 项）**：`targetSalesAmount` / `targetChannelCount` / `targetNps` / `targetSceneCount` / `launchDate` / `levelCoefficient` / `levelCoefficientReason` / `targetMarkets` / `source` / `mainGroupId`
- **缺失接口（4 项）**：`/api/projects/:id` / `/api/projects/:id/members` / `/api/projects/:id/cert-checklist` / `/api/projects/:id/stage-advance`
- **缺失状态机环节（4 个）**：① 阶段门禁校验（BR-IPD-06，40001）② 目标市场 → 认证清单自动带出（BR-IPD-05b）③ 四个立项基准值"修改需双签"标注 ④ 1:1 产品-项目关系（BR-PROD-01，bonus.multiProjectSplit=NONE）
- **验收覆盖度**：现状 3 条 AC / v3 应有 5 条 AC（缺"目标市场认证带出"+"立项基准值锁定"2 条）

### 7. 验收用例
```
Given product_manager 登录
When 进入 /projects
Then scope-banner 显示"仅我的项目"；仅显示 owner_id=自己 的项目
And 摘要四宫格显示 targetSalesAmount/targetChannelCount/targetNps/targetSceneCount + "修改需双签"标注
```
```
Given 项目选择目标市场含 SA
When 加载项目详情
Then GET /api/projects/:id/cert-checklist?markets=SA 返回 SABER/SASO 认证项
```

---

# 08. 新建项目

### 1. 页面元信息
- 编号 / 名称 / URL：08 / 新建项目 / `/projects?create=1` + `<CreateProjectModal>` in `App.jsx:251-258`（mode="recruitment"）
- 主用角色：market_pm / product_lead / super_admin
- 可见角色：满足 `canCreate` 的用户
- 优先级：P1
- 关联 BR：BR-PROJ-03 / BR-PROD-02 / BR-PROD-01 / BR-TEAM-01 ~ 10
- 现有实现：完整（`CreateProjectModal` mode="recruitment"）

### 2. 页面布局
- 区块顺序：项目空间 → 「新建项目」primary按钮 → 弹窗（双入口选择 → 招募选择器 / 已在研表单 → 四宫格目标值 → security-note → 主按钮）
- 主要组件：`<CreateProjectModal>`、`<Empty>`、`<SealCheck>`、`recruitment-picker`

### 3. 字段模型（mode=recruitment）
| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---|---|---|---|---|
| `mode` | enum | — | `""` / `recruitment` / `catch_up` | local | `""` | — |
| `recruitmentId` | string | 是 | recruitment.id | local | `initialRecruitmentId` | status=ready_to_convert |
| `form.code` | string | 是 | PRJ-YYYY-NNN | local | `""` | unique |
| 🔴 `form.productId` | string | 是 | product.id | server | — | 1:1 BR-PROD-01 |
| 🔴 `form.templateType` | enum | 是 | HARDWARE / SOFTWARE / SOLUTION | local | — | BR-PROD-02 |
| 🔴 `form.targetMarkets` | string[] | 是 | 国家代码 | local | `[]` | min 1 |
| 🔴 `form.level` | enum | 是 | S / A / B | local | `A` | bonus.coefficient 映射 |
| 🔴 `form.levelCoefficient` | decimal | 是 | — | local | — | bonus.coefficient.S=1.5/A=1.0/B=0.8 |
| 🔴 `form.levelCoefficientReason` | string | 条件 | — | local | — | S/B 必填（bonus.coefficientDecider=G1_DUAL_SIGN） |
| `form.targetSales` | decimal | 是 | — | local | 5000000 | >0 |
| `form.targetChannels` | int | 是 | — | local | 20 | >0 |
| `form.targetNps` | int | 是 | 0-100 | local | 40 | — |
| `form.targetScenarios` | int | 是 | — | local | 4 | >0 |
| 🔴 `form.marketPmId` / `rdPmId` | string | 是 | — | server | — | hr.allowCrossRole=false 互斥（BR-TEAM-08） |

### 4. API 接口清单
- `GET /api/projects/create-options`（现有）
- `POST /api/projects/from-recruitment`（现有）
- 🔴 `POST /api/projects`（v3 缺统一创建入口）
- 🔴 `GET /api/products?type=inResearch`（v3 缺，BR-PROD-01 三路来源）
- 🔴 `POST /api/bid-invitations`（v3 缺 BR-TEAM-01）
- 🔴 `GET /api/bid-invitations/:id/responses`（v3 缺）
- 🔴 `POST /api/bid-invitations/:id/select`（v3 缺 BR-TEAM-05）
- 审计写入：`entityType=project, action=create / update`

### 5. 闭环剧本
| 环节 | 内容 |
|---|---|
| ① 发起 | `/projects?create=1` 触发 modal 自动打开；选择 mode=recruitment |
| ② 处理 | 选择 readyRecruitments → 锁定 customer_problem/target_launch_date/strategic_level；服务端校验 hr.allowCrossRole=false 保证 marketPm≠rdPm（BR-TEAM-08） |
| ③ 审核 | 无审批；服务端在原招募原子事务中校验 ready_to_convert 状态 |
| ④ 结果 | 创建项目 lifecycle=ACTIVE；自动生成六阶段 + 69 动作 instances；选 targetMarkets 后调 `/api/projects/:id/cert-checklist` 带出认证项；返回 projectId → `refresh(projectId)` |
| ⑤ 记录 | `entityType=project, action=create / update`，before/after 含 recruitmentId、projectCode、targetSales、levelCoefficient |
| ⑥ 归档 | 关联招募 status=CLOSED（保持完整历史）；项目绑定 ProjectMember 评级快照（BR-INC-02） |

### 6. 现状 vs v3 差距
- **缺失字段（8 项）**：`productId` / `templateType` / `targetMarkets` / `level` / `levelCoefficient` / `levelCoefficientReason` / `marketPmId` / `rdPmId`
- **缺失接口（4 项）**：`/api/products` / `/api/bid-invitations` / `/api/bid-invitations/:id/responses` / `/api/bid-invitations/:id/select`
- **缺失状态机环节（4 个）**：① BR-PROD-01 1:1 产品绑定（bonus.multiProjectSplit=NONE）② BR-PROD-02 三模板分支 ③ BR-TEAM-01 招标组队完整链路 ④ BR-INC-05 系数定值双签（bonus.coefficientDecider=G1_DUAL_SIGN）
- **验收覆盖度**：现状 3 条 AC / v3 应有 5 条 AC（缺"招标→应标→遴选→中标通知"4 步 + 角色互斥 40003）

### 7. 验收用例
```
Given market_pm 选择 ready_to_convert=true 的招募
When 提交 form 含 productId/templateType=HARDWARE/targetMarkets=[SA, EU]
Then POST /api/projects/from-recruitment 返回 projectId
And 项目自动绑定 recruited 的 market_pm / rd_pm / 产品类型 / 战略等级
And 调用 /api/projects/:id/cert-checklist 返回 SABER/CE/RoHS/REACH/GDPR 5 项
And 审计 entityType=project, action=create
```
```
Given 同一 PM 同时被选为 marketPm 和 rdPm
When 提交
Then 返回 40003 角色冲突；审计 entityType=project, action=create_failed_self_dual
```

---

# 09. 存量项目导入

### 1. 页面元信息
- 编号 / 名称 / URL：09 / 存量项目导入 / `/projects?create=1` 弹窗内 `mode=catch_up` 路径
- 主用角色：market_pm / product_lead / super_admin
- 可见角色：canCreate 用户
- 优先级：P1
- 关联 BR：BR-PROD-03 / BR-PROD-04
- 现有实现：完整（`CreateProjectModal` mode=catch_up）

### 2. 页面布局
- 区块顺序：弹窗 → 选择 mode=catch_up → 填写 name/code/marketPm/rdPm/declaredCurrentStageCode + 产品类型/上市/市场窗口 + 四项目标值 → 提交
- 主要组件：`<CreateProjectModal>`、`<Empty>`
- 关键视觉元素：`declaredCurrentStageCode` 下拉；提示"必须从概念阶段开始依次补齐和确认"

### 3. 字段模型
| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---|---|---|---|---|
| `form.name` | string | 是 | — | local | `""` | minLength=4 |
| `form.code` | string | 是 | PRJ-YYYY-NNN | local | `""` | unique |
| 🔴 `form.productId` | string | 是 | product.id | server | — | 1:1 |
| 🔴 `form.templateType` | enum | 是 | HARDWARE / SOFTWARE / SOLUTION | local | HARDWARE | — |
| 🔴 `form.targetMarkets` | string[] | 是 | — | local | `[]` | min 1 |
| `form.productType` / `targetLaunchDate` | string/date | 是 | — | local | 默认 | — |
| `form.marketPmId` / `form.rdPmId` | string | 是 | — | local | — | hr.allowCrossRole=false 互斥 |
| `form.declaredCurrentStageCode` | enum | 是 | concept/plan/develop/verify/launch/lifecycle | local | develop | BR-PROD-03 |
| 🔴 `form.missingHistoryAck` | bool | 是 | — | local | false | 历史缺失声明 |
| `form.strategicLevel` / `form.marketWindow` | enum/string | 是 | S/A/B / — | local | A / 默认 | — |
| `form.targetSales/Channels/Nps/Scenarios` | decimal/int | 是 | — | local | 默认 | — |

### 4. API 接口清单
- `POST /api/projects/catch-up`（现有）
- 🔴 `POST /api/projects/:id/legacy-import`（v3 缺独立接口，BR-PROD-03）
- 🔴 `POST /api/projects/:id/catchup-stage-completed`（v3 缺补齐阶段回调）
- 🔴 `POST /api/projects/:id/biweekly-reviews`（v3 缺 BR-IPD-03 补录双周评审）
- 审计写入：`entityType=project, action=create / update`

### 5. 闭环剧本
| 环节 | 内容 |
|---|---|
| ① 发起 | `/projects?create=1` 弹窗选择「已在研项目补录」 |
| ② 处理 | 提交后服务端创建项目 stages 全量，但 catchup_status=IN_PROGRESS；用户必须从 concept 开始补；缺失节点标"历史缺失" |
| ③ 审核 | 无审批；BR-PROD-04 三切入点（需求变更双签/上市节奏/复盘）自动生效 |
| ④ 结果 | 项目创建；页面显示 catchup-banner 直到 catchup_status=COMPLETE；market_pm 补入后 2 周内必须完成「场景定义复核」系统自动生成待办并倒计时 |
| ⑤ 记录 | `entityType=project, action=create / update`；每完成一个阶段写 `action=update` |
| ⑥ 归档 | catchup_status=COMPLETE 后 banner 自动隐藏；功能KPI 补入前不追溯、补入后全量考核；共担KPI 不分段不打折；月度津贴从次月起按评级固定额发放（BR-PROD-04） |

> ⚙️ 跨页守护（D.6.6 第 2 条）：14 天场景定义复核倒计时（BR-PROD-04）—— 09 存量项目导入页 `POST /api/projects/:id/catchup-milestone` 服务端校验：market_pm 补入后 cron 启动 14 天（=`handover.resignDeadlineDays` 同源但独立）倒计时，超期未完成场景定义复核 → 自动生成待办 + 升级产品组长；同时在 07 项目列表 banner 红点提示。

### 6. 现状 vs v3 差距
- **缺失字段（4 项）**：`productId` / `templateType` / `targetMarkets` / `missingHistoryAck`
- **缺失接口（3 项）**：`/api/projects/:id/legacy-import` / `/api/projects/:id/catchup-stage-completed` / `/api/projects/:id/biweekly-reviews`
- **缺失状态机环节（3 个）**：① 场景定义复核 14 天倒计时（BR-PROD-04）② 补录完成 → 津贴次月起算 ③ 共担KPI 不分段不打折
- **验收覆盖度**：现状 3 条 AC / v3 应有 5 条 AC（缺"场景定义复核倒计时"+"津贴次月起算"2 条）
- **现状不足**：未实现 missingHistoryAck 历史缺失标记与已过节点阻断豁免联动

### 7. 验收用例
```
Given market_pm 在弹窗选 catch_up
When 填写 name/code/marketPm/rdPm/declaredCurrentStageCode=develop/missingHistoryAck=true
Then POST /api/projects/catch-up 返回 projectId
And 审计 entityType=project, action=create
And /projects 显示 catchup-banner："实际阶段：develop"
And 系统自动生成"场景定义复核"待办，倒计时 14 天
```
```
Given catch_up 项目补齐至 declared 阶段
When 服务端把 catchup_status 切到 COMPLETE
Then catchup-banner 自动隐藏；流程进入正常推进
And 月度津贴从次月起按评级固定额发放（BR-PROD-04）
```

---

# 10. 项目详情-项目概览

### 1. 页面元信息
- 编号 / 名称 / URL：10 / 项目详情-项目概览 / `/projects`（项目摘要 + scope-banner + catchup-banner）
- 主用角色：全部已登录
- 可见角色：按 boot.access.projectScope
- 优先级：P1
- 关联 BR：BR-PROJ-05 / BR-DUAL-01 / BR-PERF-08 / BR-INC-04 / BR-IPD-08
- 现有实现：完整（`ProjectsPage` 中 `scope-banner` + `catchup-banner` + `project-summary`）

### 2. 页面布局
- 区块顺序：scope-banner → catchup-banner → project-summary（4 卡 + KPI 入口）→ stage-list → 治理区
- 主要组件：`<ProjectsPage>` 内嵌 `<RdReplacementDeepLink>`
- 关键视觉元素：项目摘要 4 宫格 + 四个立项基准值 + 上市日期（标注"修改需双签"）+「进入项目KPI评分」入口

### 3. 字段模型
| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---|---|---|---|---|
| `boot.project.*` | object | — | — | `/api/bootstrap` | — | — |
| 🔴 `project.targetSalesAmount` | decimal | 是 | 元 | server | — | 显示 + "修改需双签" |
| 🔴 `project.targetChannelCount` | int | 是 | — | server | — | 同上 |
| 🔴 `project.targetNps` | int | 是 | 0-100 | server | — | 同上 |
| 🔴 `project.targetSceneCount` | int | 是 | — | server | — | 同上 |
| 🔴 `project.launchDate` | DateTime | — | — | server | null | bonus.launchAnchor=L08_ACTION 锁定 |
| 🔴 `project.levelCoefficient` | decimal | — | — | server | — | C09 提议 |
| 🔴 `project.levelCoefficientReason` | string | — | — | server | — | S/B 必填 |
| 🔴 `project.targetMarkets` | string[] | — | 国家代码 | server | `[]` | 驱动 cert-checklist |
| 🔴 `project.isBioFeature` | bool | — | — | server | false | C12 全等级阻断自动挂载触发 |
| `boot.access.canEditProject` / `projectScope` | bool/string | — | — | derived / server | — | — |

### 4. API 接口清单
- `GET /api/bootstrap?projectId=...`（现有）
- 🔴 `GET /api/projects/:id/cert-checklist?markets=`（v3 缺 BR-IPD-05b）
- 🔴 `PATCH /api/projects/:id/level-coefficient`（v3 缺 BR-INC-05 系数修改走双签）
- `GET /api/rd-replacements/:replacementId`（v3 路径前缀）
- `GET /api/projects/:id/handoff-candidates`（现有）

### 5. 闭环剧本
| 环节 | 内容 |
|---|---|
| ① 发起 | 顶栏切换项目 → `refresh(projectId)` → `/api/bootstrap` 重载 |
| ② 处理 | scope-banner 根据 projectScope 渲染；catchup-banner 按条件渲染；摘要四宫格 + 四个立项基准值 + 上市日期（标注"修改需双签"） |
| ③ 审核 | 不涉及审批；组员项目以 read-only 模式展示；点击 KPI 入口 → /performance 走 BR-KPI 模块 |
| ④ 结果 | 点击 KPI 入口 → /performance；点击 stage-list 行 → /action?workItem=...；点击 coefficient 触发 PATCH 走 BR-INC-05 双签（bonus.coefficientDecider=G1_DUAL_SIGN） |
| ⑤ 记录 | `entityType=project, action=view_summary` 每次切换项目写一条；系数修改写 `action=update, before/after` |
| ⑥ 归档 | 项目 lifecycle=ARCHIVED 时摘要卡切换为只读；上市日期修改需双签 + 审计 |

### 6. 现状 vs v3 差距
- **缺失字段（9 项）**：`targetSalesAmount` / `targetChannelCount` / `targetNps` / `targetSceneCount` / `launchDate` / `levelCoefficient` / `levelCoefficientReason` / `targetMarkets` / `isBioFeature`
- **缺失接口（2 项）**：`/api/projects/:id/cert-checklist` / `PATCH /api/projects/:id/level-coefficient`
- **缺失状态机环节（3 个）**：① 上市日期锁定修改需双签（BR-IPD-08）② 系数修改走双签（BR-INC-05 / bonus.coefficientDecider=G1_DUAL_SIGN）③ isBioFeature=true 自动挂载 C12（BR-IPD-02）
- **验收覆盖度**：现状 3 条 AC / v3 应有 6 条 AC（缺"上市日期双签修改"+"系数双签修改"+"生物特征自动挂载 C12"3 条）

### 7. 验收用例
```
Given product_manager 命中 /projects
When bootstrap 加载完成
Then scope-banner 渲染"仅我的项目"；owner_name = 当前 PM
And 摘要四宫格显示 4 个立项基准值 + launchDate（如有）
And 每个基准值右上角有"修改需双签"图标
```
```
Given S 级项目系数 1.5，用户点击修改为 1.8
When PATCH /api/projects/:id/level-coefficient
Then 触发 market_pm + rd_pm + product_lead 三节点确认链（BR-INC-05）
And 审计 entityType=project, action=update, before=1.5, after=1.8
```

---

# 11. 项目详情-IPD 流程

### 1. 页面元信息
- 编号 / 名称 / URL：11 / 项目详情-IPD 流程 / `/projects` 中 `stage-list` + `workitem-list`
- 主用角色：全部已登录
- 可见角色：项目 owner / 协同 PM / product_lead 监督 / super_admin
- 优先级：P0（系统核心页）
- 关联 BR：BR-IPD-01 ~ BR-IPD-09 / BR-DUAL-02 / BR-GATE-01 ~ 04 / BR-IPD-05b / BR-IPD-06
- 现有实现：部分（69 动作 seed 缺失，深管/轻管视觉未严格区分）

### 2. 页面布局
- 区块顺序：scope-banner → catchup-banner → project-summary → **stage-list（六阶段折叠面板，DEEP 卡片 / LIGHT 行式严格区分）** → CloseoutReadiness → other-projects
- 主要组件：`<StageConfirmPage>`、`<DecisionChainPanel>`、`<KeyGatePanel>`、`<BiweeklyPanel>`、`<GenericTaskWorkspace>`
- 关键视觉元素：stage-progress 进度条；workitem 状态点 done/locked/open；DEEP 动作卡片式带交付物图标，LIGHT 行式仅显示状态与日期 + D11 额外显示 FAR/FRR

### 3. 字段模型
| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---|---|---|---|---|
| `expanded` | enum | — | concept/plan/develop/verify/launch/lifecycle | local | concept | — |
| `stage.code` | enum | — | concept/plan/develop/verify/launch/lifecycle | server | — | 6 阶段固定 |
| `stage.status` / `gateStatus` | enum | — | DRAFT/PENDING/APPROVED/REJECTED | server | — | — |
| `workItem.status` | enum | — | NOT_STARTED/IN_PROGRESS/DONE/DELAYED/NA | server | — | — |
| `workItem.depth` | enum | — | DEEP / LIGHT | server | — | 决定校验逻辑 BR-IPD-03/04 |
| `workItem.isBlocking` | bool | — | — | server | — | BR-IPD-06 38/25/14 分级 |
| 🔴 `workItem.farValue` | decimal(10,6) | 条件 | — | server | null | D11/Z01 强制 |
| 🔴 `workItem.frrValue` | decimal(10,6) | 条件 | — | server | null | D11/Z01 强制 |
| 🔴 `workItem.certNo` | string | 条件 | — | server | null | V02 |
| 🔴 `workItem.certPassedAt` | DateTime | 条件 | — | server | null | V02 |
| 🔴 `workItem.algoType` | enum | 条件 | FINGERPRINT/FACE/PALM/VEIN/MULTI | server | null | D11 |
| 🔴 `workItem.isBioFeature` | bool | — | — | server | false | 触发 C12 挂载 |
| 🔴 `workItem.targetMarkets` | string[] | — | — | server | [] | 决定 cert-checklist 带出 |

### 4. API 接口清单
- `GET /api/bootstrap?projectId=...`（现有）
- `POST /api/key-gates/:stageId/submit`（现有）
- `POST /api/key-gates/:stageId/waive`（现有）
- `POST /api/waivers/:id/decision`（现有）
- `POST /api/reviews/:id/decision`（现有）
- `POST /api/key-gates/:gateId/materials?materialType=...`（现有）
- `POST /api/key-gates/:gateId/submit /sign /arbitrate /reopen`（现有）
- 🔴 `GET /api/projects/:id/stages`（v3 缺独立阶段接口）
- 🔴 `PATCH /api/stage-actions/:id`（v3 缺统一动作更新，深管/轻管字段差异校验）
- 🔴 `POST /api/stage-actions/:id/deliverables`（v3 缺 BR-IPD-03 强制上传）
- 🔴 `POST /api/projects/:id/stage-advance`（v3 缺 BR-IPD-06 门禁校验，40001）
- 🔴 `GET /api/projects/:id/cert-checklist`（v3 缺 BR-IPD-05b 认证清单）
- 🔴 `GET /api/key-gates/:id/elements?gateCode=G1-G5`（v3 缺 BR-GATE-01b 33 项要素）
- 🔴 `POST /api/key-gates/:id/element-results`（v3 缺三态判定 + 否决项硬阻断）
- 🔴 `POST /api/key-gates/:id/arbitrate`（v3 缺 BR-GATE-06 仲裁）
- 审计写入：`entityType=stage_action / gate / gate_element_result, action=submit / sign / arbitrate / judge / update / approve / reject`

### 5. 闭环剧本
| 环节 | 内容 |
|---|---|
| ① 发起 | 项目 owner 完成所有 workItem 后点「提交阶段确认」 |
| ② 处理 | `POST /api/key-gates/:stageId/submit` → 服务端校验本阶段全部 workItem 已 DONE 且 gateStatus=ready（DEEP 强制交付物 / LIGHT 仅三字段 / D11 FAR+FRR 数值 / V02 证书号+日期） |
| ③ 审核 | 双PM阶段确认：market_pm → rd_pm → product_lead → product_lead(r&d) → super_admin 五节点（gate.signDeadlineDays=3 自然日 BR-GATE-04）；关键 Gate 还需会议纪要 + 评审材料 + 33 项要素三态判定（命中 ❌ 阻断）；**超期 3 个自然日（gate.signDeadlineDays）未签署 → 自动转 ABSTAINED_TIMEOUT，按主导方意见执行（v3 BR-GATE-04），并写审计 entityType=gate, action=sign（自动弃权）** |
| ④ 结果 | 通过：stage.status=APPROVED，next stage 激活；⚠️ 项生成待办、关闭前不允许进入下个 Gate；驳回：回到 PENDING 等待整改 |
| ⑤ 记录 | submit/sign/arbitrate/element_decided/reopen 全链路写入；before/after 含 stage.status、gate.elementResults、signatures |
| ⑥ 归档 | 阶段 archived → 决策链保留；进入下一阶段；G5 90 天复盘（gate.reviewDays90=90）生成项目绩效综合得分（BR-KPI-08） |

> ⚙️ 跨页守护（D.6.6 第 1 条）：未闭环变更单阻断阶段出口（BR-GATE-07）—— 11 IPD 流程页 `POST /api/key-gates/:stageId/submit` 服务端校验：当前阶段 `unclosed_change_count = COUNT(requirement_change WHERE stage=X AND status NOT IN ('CLOSED','WITHDRAWN'))` 必须为 0，否则返回 `40001` 阶段门禁未过；变更单管理在 25 需求与变更 + 26 变更单详情。

### 6. 现状 vs v3 差距
- **缺失字段（7 项）**：`farValue` / `frrValue` / `certNo` / `certPassedAt` / `algoType` / `isBioFeature` / `targetMarkets`
- **缺失接口（7 项）**：stage-actions CRUD 3 项 + cert-checklist + gates/elements 2 项 + gates/arbitrate + gate element-results（合计 7）
- **缺失状态机环节（5 个）**：① BR-IPD-06 阶段门禁校验（38/25/14 分级，错误码 40001）② BR-IPD-05b 认证清单自动带出 ③ BR-GATE-01b 33 项要素三态判定 ④ BR-GATE-06 仲裁升级 ⑤ C12 全等级阻断自动挂载 ⑥ **BR-GATE-04 超期 ABSTAINED_TIMEOUT 自动弃权（gate.signDeadlineDays=3 自然日，无 cron 触发）**
- **验收覆盖度**：现状 3 条 AC / v3 应有 6 条 AC（缺"命中否决项硬阻断"+"D11 FAR/FRR 强制"+"C12 自动挂载"3 条）
- **现状不足**：69 动作未 seed 完整；DEEP/LIGHT 字段校验逻辑未分离；视觉未严格区分 DEEP 卡片/LIGHT 行式

### 7. 验收用例
```
Given 项目当前阶段为 develop 且 workItem 已全部 DONE（含 DEEP 交付物）
When 项目 owner 提交阶段确认
Then POST /api/key-gates/:stageId/submit 创建五节点决策链（gate.signDeadlineDays=3 → dueAt）
And 审计 entityType=gate, action=submit
And 工作台投递任务给 market_pm 第一节点
```
```
Given G1 评审命中否决项（如客户验证 < gate.g1.minCustomerVerifications=5 家无书面意向）
When 试图点击"通过"
Then 按钮置灰并高亮命中项；POST /api/key-gates/:id/element-results 阻断提交
And 系统提示命中 BR-GATE-01b 否决项；返回 40001
```
```
Given D11 BioCV 评测动作完成但未登记 FAR/FRR
When 提交 DONE
Then 返回 40004 缺数值字段；UI 显示"必须登记实测 FAR/FRR 数值"
```
```
Given G2 五节点签署中（market_pm 已签，后续节点 3 自然日未签）
When 系统触发 gate.signDeadlineDays=3 超期 cron（v3 BR-GATE-04）
Then 未签节点自动转 ABSTAINED_TIMEOUT，gate.leadSide（market/rd）主导方意见执行
And 审计 entityType=gate, action=sign（自动弃权），status pill 由 PENDING → ABSTAINED_TIMEOUT
```

---

# 12. 深管动作详情

### 1. 页面元信息
- 编号 / 名称 / URL：12 / 深管动作详情 / `/action?workItem={id}`（`GenericTaskWorkspace` in `App.jsx:183-209`）
- 主用角色：项目 owner / 双PM
- 可见角色：项目可见者只读
- 优先级：P0
- 关联 BR：BR-IPD-03 / BR-IPD-04 / BR-IPD-05 / BR-IPD-07 / BR-QUALITY-01 / BR-AI-01 / BR-G-10
- 现有实现：部分（DEEP/LIGHT 统一处理，未严格按 G-10 区分）

### 2. 页面布局
- 区块顺序：read-only-banner → WorkspaceSummary → task-breadcrumb → task-quick-actions（收藏按钮）→ task-head → coach-grid（左 SOP / 中央 work-canvas / 右 AI+质量）
- 主要组件：`<GenericTaskWorkspace>`、`<WorkspaceSummary>`、`<SopStep>`、`<AiModal>`、`<QualityPanel>`
- 关键视觉元素：SOP 步骤折叠；字段按 sop.fields 渲染；质量检查面板双层；模板应用 modal；**DEEP 强制 SOP + 交付物 + 阻断性动作；LIGHT 仅三字段无附件入口（G-10）**
- banner：read-only-banner / locked（顺序锁定）/ quality-stale-banner

### 3. 字段模型
| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---|---|---|---|---|
| `source.id` | string | — | workItem.id | URL param | — | — |
| `source.depth` | enum | — | DEEP / LIGHT | server | DEEP | BR-IPD-03/04 决定校验逻辑 |
| `source.payload` | object | — | field-key→任意 | server | `{}` | — |
| `source.checklist` | array | — | `{label, done}` | server | — | — |
| `source.revision` | int | — | — | server | — | — |
| 🔴 `source.actionCode` | string | — | C01-C12/P01-P13/D01-D11/V01-V12/L01-L08/LC01-LC09 | server | — | 69 动作 |
| 🔴 `source.isBlocking` | bool | — | — | server | false | BR-IPD-06 |
| 🔴 `source.farValue` / `frrValue` | decimal(10,6) | 条件 | — | server | null | D11 必填 |
| 🔴 `source.certNo` / `certPassedAt` | string/date | 条件 | — | server | null | V02 |
| `source.sop.fields` | array | — | `{key,label,type,required,minChars,evidenceRequired,expectedStructure}` | server | 三默认 | — |
| 🔴 `payload.summary` / `evidence` / `riskNotes` | string | DEEP 必填 | — | local | `""` | minChars ≥ 80 |
| 🔴 `lightPayload.status` | enum | LIGHT 必填 | NOT_STARTED/IN_PROGRESS/DONE/NA | local | — | — |
| 🔴 `lightPayload.actualDoneAt` | DateTime | LIGHT 条件 | — | local | null | status=DONE 时必填 |
| 🔴 `lightPayload.remark` | string | LIGHT 选填 | — | local | `""` | ≤200 字 |
| `qualityStale` | bool | — | — | local | false | 内容改后置 true |

### 4. API 接口清单
- `GET /api/bootstrap?projectId=...`（现有）
- `PUT /api/work-items/:id`（现有）
- `POST /api/work-items/:id/quality-check`（现有）
- `POST /api/work-items/:id/quality-runs/:runId/review`（现有）
- `POST /api/ai/context-preview?workItemId=&fieldKey=`（现有）
- `POST /api/ai/run`（现有）
- `POST /api/ai/outputs/:conversationId/decision`（现有）
- `POST /api/attachments?projectId=...`（现有）
- `POST /api/saved-items`（现有）
- 🔴 `PATCH /api/stage-actions/:id`（v3 缺统一动作更新，DEEP/LIGHT 字段差异校验）
- 🔴 `POST /api/stage-actions/:id/deliverables`（v3 缺 BR-IPD-03 DEEP 强制交付物）
- 🔴 `POST /api/stage-actions/:id/sop-preview`（v3 缺 BR-IPD-07 SOP 强制展示）
- 审计写入：`entityType=stage_action / work_item, action=update / complete_deep / complete_light / register_farfrr / register_cert / accept / reject`

### 5. 闭环剧本
| 环节 | 内容 |
|---|---|
| ① 发起 | `/action?workItem={id}` → `<TaskWorkspace>` 找到 source → `<GenericTaskWorkspace>` 渲染；按 depth 分支（DEEP/LIGHT） |
| ② 处理 | DEEP：编辑 payload + 上传 ≥1 交付物 → qualityStale=true → save → `PATCH /api/stage-actions/:id` + `POST .../deliverables` → 触发质量检查；LIGHT：仅编辑 status/actualDoneAt/remark 三字段；UI 无附件入口（G-10） |
| ③ 审核 | DEEP：质量 failed 阻塞 nextWorkItem；quality_pending 进入组长人工语义复核（manual_pass）；LIGHT：无质量检查 |
| ④ 结果 | DEEP：通过 → nextWorkItem 推送到工作台（action=stage_sign）；stageReady=true 时提示可提交阶段确认；LIGHT：保存即完成（action=complete_light） |
| ⑤ 记录 | DEEP：entityType=stage_action, action=update / register_farfrr / register_cert；LIGHT：entityType=stage_action, action=complete_light |
| ⑥ 归档 | payload 与 revisions 永久保存；qualityRuns 与 aiConversations 进 timeline drawer；D11 FAR/FRR 与 V02 certNo/certPassedAt 单独字段归档 |

### 6. 现状 vs v3 差距
- **缺失字段（10 项）**：`depth` / `actionCode` / `isBlocking` / `farValue` / `frrValue` / `certNo` / `certPassedAt` / `lightPayload.status/actualDoneAt/remark` 等
- **缺失接口（3 项）**：`PATCH /api/stage-actions/:id` / `POST .../deliverables` / `POST .../sop-preview`
- **缺失状态机环节（5 个）**：① G-10 DEEP/LIGHT 字段校验逻辑分离（现状统一处理）② D11 FAR/FRR 强制（action=register_farfrr）③ V02 certNo/certPassedAt 强制（action=register_cert）④ DEEP SOP 强制展示 ⑤ 阻断性动作门禁联动 stage-advance（40001）
- **验收覆盖度**：现状 3 条 AC / v3 应有 6 条 AC（缺"LIGHT 三字段无附件入口"+"D11 强制 FAR/FRR"+"DEEP 交付物缺失阻断"3 条）
- **现状不足**：未实现 G-10 严格三字段（LIGHT UI 仍保留评审证据上传入口）；69 动作未 seed；DEEP 阻断性动作未联动 stage-advance 校验

### 7. 验收用例
```
Given 用户 owner 进入 /action?workItem=W001（DEEP 动作）
When 加载 GenericTaskWorkspace
Then payload/checklist/sop.fields 渲染正确；只读项目显示 read-only-banner
And SOP 强制展示（左侧 SopStep 折叠）；右上角有"上传交付物"按钮（BR-IPD-03）
And qualityStale=false；quality.status 展示
```
```
Given owner 编辑后点击"保存并系统自检"
When PATCH /api/stage-actions/W001 返回 nextWorkItem
Then 通知"已完成双层质量检查，正在进入下一动作"；650ms 后 navigate(/action?workItem=...)
And 审计 entityType=stage_action, action=update, after={revision:2}
```
```
Given 用户进入 D11 BioCV 评测动作
When 编辑 payload 但未填 farValue/frrValue 就 save
Then 返回 40004 缺 FAR/FRR；UI 红色提示"必须登记实测 FAR/FRR 数值"
And 审计 entityType=stage_action, action=update 触发 register_farfrr 缺值校验
```
```
Given 用户进入 P05 轻管动作
When 进入 /action?workItem=P05
Then UI 仅显示 3 字段（status / actualDoneAt / remark）；无"上传评审证据"按钮
And 提交后 entityType=stage_action, action=complete_light
```

---

# 全局汇总（12 页 v3 差距量化）

| 维度 | 现状 | v3 要求 | 缺口 |
|---|---|---|---|
| 缺失字段 | 0（全部用现有） | 见各页 | **约 66 项**（captchaToken/refreshToken/层级/state/wecom/auth/criticality/deadline/cert/markets/bioFeature/coefficient/far/frr/certNo/certPassedAt/lightPayload/actionCode/isBlocking/depth 等） |
| 缺失接口 | 现状代码已实现约 28 个 | 见各页 | **约 34 项**（refresh/wecom OAuth 3 项 / audit verify+scope / 通用 deletion 3 项 / projects 详情+cert+stage-advance / bids 4 项 / legacy-import / catchup callback / stage-actions CRUD / gate elements+arbitrate） |
| 缺失状态机环节 | 现状仅覆盖基础登录、看板、删除双层、IPD 主线 | v3 全部 BR-xxx | **约 45+ 环节**（BR-PROD-01 三路来源、BR-TEAM-01~10 招标组队、BR-GATE-01b 33 项要素三态、BR-KPI-05 共担归集、BR-INC-04b 回款口径、BR-INC-05 双签定值、BR-INC-11 无产出停发、BR-IPD-05b 认证带出、BR-IPD-06 门禁分级、BR-USER-06 离职先移交后禁用等） |
| 验收覆盖度 | 36 条 AC | 60 条 AC | **缺 24 条 AC** |

# 全局一致性锚点

- 角色：`super_admin` / `product_lead` / `product_manager` / `market_pm` / `rd_pm`
- 状态（人）：`ACTIVE` / `FROZEN_PENDING_HANDOVER` / `DISABLED` / `RESIGNED`
- 状态（项目）：`DRAFT` / `TEAMING` / `ACTIVE` / `SUSPENDED` / `ARCHIVED`
- 状态（动作）：`NOT_STARTED` / `IN_PROGRESS` / `DONE` / `DELAYED` / `NA`
- 质量：`passed` / `failed` / `quality_pending` / `manual_pass`
- 错误码：1xxxx 参数校验 / 2xxxx 认证 / 3xxxx 权限 / **4xxxx 业务阻断（40001 门禁 / 40002 双签 / 40003 超 3 项目 / 40004 评级快照 / 40005 阶梯系数）** / 5xxxx 资源 / 9xxxx 系统
- 路径前缀：`/api/auth/*` / `/api/admin/*` / `/api/projects/*` / `/api/work-items/*` / `/api/key-gates/*` / `/api/requirements/*` / `/api/changes/*` / `/api/bid-invitations/*` / `/api/handovers/*` / `/api/kpi/*` / `/api/performance/*` / `/api/bonus/*` / `/api/allowance/*` / `/api/deletion-requests/*` / `/api/audit-logs/*` / `/api/ai/*` / `/api/notifications/*` / `/api/attachments/*` / `/api/documents/*` / `/api/products/*` / `/api/recruitments/*` / `/api/collaboration/*` / `/api/public/*` / `/api/saved-items/*` / `/api/workflow/*` / `/api/search/*` / `/api/identity-sync/*`
- 关键参数：`gate.signDeadlineDays=3` / `deletion.leaderDeadlineDays=2` / `deletion.adminDeadlineDays=2` / `deletion.withdrawHours=24` / `gate.g1.minCustomerVerifications=5` / `bonus.poolBase=TARGET_SALES` / `bonus.salesSource=RECEIPT` / `bonus.performanceScoreStrategy=PROJECT_SCORE` / `bonus.multiProjectSplit=NONE` / `bonus.launchAnchor=L08_ACTION` / `bonus.coefficientDecider=G1_DUAL_SIGN` / `kpi.reviewWeights={self:0.2,marketLeader:0.4,rdLeader:0.4}` / `kpi.monthlyDeadlineDay=5` / `hr.allowCrossRole=false` / `hr.levelSource=API_ONLY`
