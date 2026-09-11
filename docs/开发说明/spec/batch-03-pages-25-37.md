# 25. 项目详情-需求与变更

## 1. 页面元信息

- 编号：25
- 名称：项目详情-需求与变更
- URL 路径：`/changes`
- 页面性质：项目详情页签“需求与变更”
- 主用角色：市场PM、研发PM
- 可见角色：所有可访问当前项目的登录用户
- 可操作角色：项目负责人可发起变更；产品组长、超级管理员可审批
- 优先级：P2
- 关联规则：BR-GATE-07（需求变更双签）、BR-GATE-03（双签并行签署，互不可见）、BR-GATE-04（双签期限3个自然日）、BR-REQ-05（需求生命周期）、BR-ORG-05（组长可视范围）
- 现有实现：部分（`ChangesPage` App.jsx:304-319、`ChangeModal` App.jsx:327-330、`DecisionChainPanel` ClosurePages.jsx:104-111、`ChangeClosurePanel` ClosurePages.jsx:121-125；服务端 `/api/changes`、`/api/requirements`、`/api/collaboration` 已实现）

## 2. 页面布局

- 页面外层使用 `<PageFrame title="变更管理">`（App.jsx:312）。
- 顶部沿用全局 `<Shell>` 项目选择器（App.jsx:124）。
- 当前路由未挂载 `<StageRail>`（App.jsx:126-130 条件渲染）。
- 副标题固定："先选择变更项目，再加载该项目的需求基线；双PM项目进入五节点决策链"。
- 顶部操作区右侧 `<button className="primary-button">` 触发 `ChangeModal`。
- 页面展示项目选择区（`<select>` 列出 boot.projects）。
- 指标卡显示全部变更、待双重审批、已批准、累计排期影响。
- 主内容区 `change-cards` 卡片列表，`<article className="change-card-head">` 展示项目、关联、状态、影响。
- 卡片显示产品组长审批状态与超级管理员审批状态。
- 卡片点击触发 `setSelectedChange(item)`，下方渲染 `<DecisionChainPanel subjectType="change_request">`。
- 变更被批准后下方渲染 `<ChangeClosurePanel change={selectedChange}>`。
- 实施闭环步骤：基线修订 → 实施结果 → 市场PM验证 → 研发PM验证 → 关闭。
- 变更申请附件可在批准前查看。
- 双PM项目显示五节点协同链（market_pm→rd_pm→market_lead→rd_lead→super_admin），非双PM项目显示产品组长与超级管理员两个节点。

## 3. 字段模型

| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---:|---|---|---|---|
| `changeId` | string | 是 | UUID | URL | 无 | 必须存在 |
| `code` | string | 是 | `CR-001` | `change_requests.code` | 无 | 项目内唯一 |
| `projectId` | string | 是 | UUID | 项目选择器 | 当前项目 | 可见范围校验 |
| `requirementId` | string | 否 | UUID | `requirements` | 空 | 必须属于当前项目 |
| `title` | string | 是 | 4–160字符 | `ChangeModal` form.title | 空 | minLength=4 |
| `reason` | string | 是 | 8–2000字符 | `ChangeModal` form.reason | 空 | minLength=8 |
| `impactScope` | string | 是 | 8–2000字符 | `ChangeModal` form.impactScope | 空 | minLength=8 |
| `impactScheduleDays` | integer | 是 | 0–365 | `ChangeModal` form.impactScheduleDays | 0 | z.number().int().min(0).max(365) |
| `impactCost` | number | 是 | 0–100000万元 | `ChangeModal` form.impactCost | 0 | z.number().min(0).max(100000) |
| `status` | enum | 是 | `pending/approved/rejected` | `change_requests.status` | `pending` | 状态机控制 |
| `leadDecision` | enum | 是 | `pending/approved/rejected` | `change_approvals` | `pending` | 产品组长节点 |
| `adminDecision` | enum | 是 | `pending/approved/rejected` | `change_approvals` | `pending` | 超级管理员节点 |
| `decisionComment` | string | 条件 | ≥4字符 | 决策接口 body.comment | 空 | minLength=4 |
| `attachments` | array | 否 | 附件对象 | `change_implementation_evidence` | 空 | 通过 `/api/changes/:id/evidence` 关联 |

## 4. API 接口清单

- `GET /api/changes?projectId=<projectId>` — server.mjs:965
  - 用途：返回指定项目变更单及附件数量、领导决策、超级管理员决策。
  - 错误码：30004（项目不可见）；权限：项目可访问用户；审计：读取不写。
- `GET /api/requirements?projectId=<projectId>` — server.mjs:921
  - 用途：返回需求列表与状态汇总供选择关联。
  - 错误码：30004；权限：项目可访问；审计：读取不写。
- `POST /api/changes?projectId=<projectId>` — server.mjs:979
  - 请求体：`{title, requirementId?, reason, impactScope, impactScheduleDays, impactCost}`。
  - 响应体：`{id, code, status:"pending"}`。
  - 错误码：10001 参数不完整；30001 无编辑权限；50001 关联需求不存在。
  - 权限：项目负责人或超级管理员；审计：entityType=change_request, action=submit。
- `POST /api/attachments?projectId=<projectId>` — server.mjs:1675
  - 请求体：multipart {file, category:"change", description}。
  - 错误码：10001 空文件；10901 类型不支持；权限：项目可编辑；审计：entityType=attachment, action=create。
- `POST /api/changes/:id/evidence` — closure.mjs:392
  - 请求体：`{description, attachmentId?}`。
  - 错误码：10001；50001；权限：项目可访问且未终结；审计：entityType=change_request, action=submit。
- `GET /api/collaboration?projectId=<projectId>` — server.mjs:1820
  - 响应体：`{decisions:[{id, subject_type:"change_request", ...}], biweeklyReviews}`。
  - 错误码：30004；权限：项目可访问；审计：读取不写。
- `POST /api/collaboration/decisions/:id/sign` — server.mjs:1834
  - 请求体：`{decision, comment}`。
  - 错误码：30001 非当前签署人；50002 节点已处理。
  - 权限：当前 `signer_id` 必须等于登录用户；审计：entityType=joint_decision, action=sign。
- `POST /api/changes/:id/decision` — server.mjs:1005（仅非双PM项目）
  - 请求体：`{decision:"approved|rejected", comment}`。
  - 错误码：50002 双PM项目禁止直审；30001 越权。
  - 权限：产品组长、超级管理员，且属于项目所属产品组；审计：entityType=change_request, action=approve|reject。
- `GET /api/changes/:id/implementation` — closure.mjs:389
  - 响应体：`{implementation, evidence}`；权限：项目可访问；审计：读取不写。
- `POST /api/changes/:id/baseline` — closure.mjs:390
  - 请求体：`{note}`；响应体：`{ok, revision}`。
  - 错误码：50002 尚未批准；10001 说明过短；权限：变更申请人或超级管理员；审计：entityType=change_request, action=update。
- `POST /api/changes/:id/implementation` — closure.mjs:391
  - 请求体：`{note}`；错误码：10001；30001 非责任人；权限：`change_implementations.owner_id`；审计：entityType=change_request, action=update。
- `POST /api/changes/:id/verify` — closure.mjs:393
  - 请求体：`{decision, comment}`；错误码：50002；30001 非责任PM；权限：市场PM或研发PM；审计：entityType=change_request, action=approve|reject。
- `POST /api/changes/:id/close` — closure.mjs:394
  - 响应体：`{ok, status:"closed"}`；错误码：50002 双PM未全部验证；权限：项目可访问；审计：entityType=change_request, action=closeout。

## 5. 闭环剧本

| 环节 | v3 状态机 | 内容 |
|---|---|---|
| ① 发起 | `SUBMITTED` | 项目负责人在 `/changes` 选项目与需求，填写变更标题、原因、影响范围、排期与成本，上传证据后调用 `POST /api/changes`。 |
| ② 处理 | `PENDING` | 双PM项目服务端调用 `createJointDecision` 生成五节点 `change_request` 协同决策（BR-GATE-03 并行签署，互不可见）；非双PM项目创建 `change_approvals` 产品组长与超级管理员两节点。 |
| ③ 审核 | `PENDING→APPROVED/REJECTED` | 五节点依次签署，期限 3 个自然日（`gate.signDeadlineDays`），到时未签按主导方意见弃权执行（BR-GATE-04）；**超期 3 个自然日（gate.signDeadlineDays）未签署 → 自动转 ABSTAINED_TIMEOUT，按主导方意见执行（v3 BR-GATE-04），并写审计 entityType=gate, action=sign（自动弃权）**。 |
| ④ 结果 | `APPROVED` | 进入实施闭环：基线修订→实施结果→双PM验证→关闭；任何节点驳回进入 `REJECTED` 不可执行（BR-GATE-07 未闭环阻断阶段出口）。 |
| ⑤ 记录 | — | 写入 `change_request / submit`、`/ sign`、`/ approve|reject`、`/ closeout`、`/ update` 等审计，entityType=`requirement_change`。 |
| ⑥ 归档 | `ARCHIVED` | 变更历史、附件、验证记录永久保留；不得物理删除；已结项项目下只读。 |

## 6. 现状 vs v3 差距（量化）

- **缺失字段（3 个）**：v3 要求但当前代码未存储 `leader_approver_id`（产品组长审批人单独字段）、`admin_approver_id`（超级管理员审批人单独字段）、`closed_by`/`closed_at`（`change_implementations` 已具备但前端未读取展示）。
- **缺失接口（1 个）**：v3 要求变更单详情独立路由 `GET /api/changes/:id`，当前仅有 `GET /api/changes?projectId` 列表（30004 项目不可见）。
- **缺失状态机环节（2 个）**：v3 BR-GATE-07 要求"未闭环变更单阻断阶段出口"，当前 `StageConfirmPage` 未读取 `change_requests.status=pending` 进行门禁校验；五节点签署 3 个自然日超期弃权机制当前没有调度任务（`gate.signDeadlineDays` 参数已读取但未触发）。
- **缺失定时任务（1 个）**：v3 要求变更签署期限到期自动弃权，无对应 cron job。
- **验收用例覆盖度**：v3 验收清单 AC-CHG-01~07 共 7 条，当前页面实现覆盖 5 条（提交、审批、实施、验证、关闭），缺失 2 条（签署超期弃权、未闭环阻断）。

## 7. 验收用例

**用例 1：双PM项目提交变更**
```text
Given 当前用户是市场项目负责人且可编辑当前项目
When 用户选择关联需求并提交完整变更影响
Then 服务端生成 CR-xxx 和五节点 change_request 决策
And 变更状态为 pending
And 写入 entityType=requirement_change, action=submit
```

**用例 2：变更节点否决**
```text
Given 某双PM项目变更正在进行五节点签署
When 当前签署人选择驳回并填写意见
Then 变更状态立即变为 rejected
And 后续节点不得继续签署
And 写入当前节点和否决原因的审计记录（entityType=requirement_change, action=reject）
```

**用例 3：实施完成验证**
```text
Given 变更已 approved 且基线修订已生成
When 实施责任人提交实施说明
Then 两名责任PM分别收到 change_verify 任务
And 任意一方验证不通过时实施状态回到 in_progress
And 双PM均通过后才能调用 close
```

**用例 4：非双PM项目兼容审批**
```text
Given 当前项目没有双PM配对
When 所属产品组长和超级管理员依次批准
Then 变更状态变为 approved
And 允许生成基线修订
And 审计记录 action 使用 approve|reject
```

**用例 5：未闭环阻断阶段出口**
```text
Given 当前阶段存在 status=pending 的变更单
When 项目负责人尝试提交阶段确认
Then 返回 40001 阶段门禁未通过
And 阻断信息列出所有未闭环变更编号
And 不得进入下一阶段
```

---

# 26. 需求变更单详情

## 1. 页面元信息

- 编号：26
- 名称：需求变更单详情
- URL 路径：建议新增 `/changes?change=<changeId>`；当前为 `ChangesPage` 卡片展开
- 页面性质：变更单下钻详情、签署与实施验证入口
- 主用角色：市场PM、研发PM、产品组长、超级管理员
- 可见角色：所有可访问项目的登录用户
- 优先级：P2
- 关联规则：BR-GATE-07、BR-GATE-03、BR-GATE-04、BR-REQ-05
- 现有实现：部分（`DecisionChainPanel` ClosurePages.jsx:104-111 与 `ChangeClosurePanel` ClosurePages.jsx:121-125 已在 `ChangesPage` 内联渲染，无独立 URL 路由、无 `GET /api/changes/:id` 单条接口）

## 2. 页面布局

- 页面外层 `<PageFrame title="需求变更单详情">`（建议新增 `ChangeDetailPage`）。
- 顶部面包屑：`变更管理 / <项目名称> / CR-xxx`。
- 页面标题显示变更单编号与标题。
- 标题右侧 `<i className="status-pill">`。
- 头部展示项目编号、项目名称、关联需求、创建人、创建时间。
- 中部左右两列。
- 左列：变更事实（原因、影响范围、排期影响、成本影响、申请附件）。
- 右列：责任确认链（市场PM、研发PM、市场组长、研发组长、超级管理员）。
- 当前节点显示"待处理"高亮，已完成节点显示签署人和意见。
- 被驳回节点显示红色标记。
- 底部展示实施闭环时间线。
- 当前用户为签署人时显示审批意见编辑器。
- 当前用户有权限时显示上传申请证据按钮。
- 双PM验证未完成时关闭按钮禁用。
- 详情视图应在刷新后保持 `?change=<id>`。

## 3. 字段模型

| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---:|---|---|---|---|
| `changeId` | string | 是 | UUID | URL参数 | 无 | 必须存在 |
| `code` | string | 是 | `CR-xxx` | `change_requests.code` | 无 | 只读 |
| `title` | string | 是 | 4–160字符 | `change_requests.title` | 无 | 创建后不可改标题 |
| `projectId` | string | 是 | UUID | `change_requests.project_id` | 无 | 可见范围校验 |
| `requesterId` | string | 是 | UUID | `change_requests.requester_id` | 无 | 只读 |
| `requesterName` | string | 是 | 姓名 | `users` | 无 | 只读 |
| `requirement` | object | 否 | 需求对象 | `requirements` | 空 | 必须属于当前项目 |
| `reason` | string | 是 | 8–2000字符 | `change_requests.reason` | 空 | 必填 |
| `impactScope` | string | 是 | 8–2000字符 | `change_requests.impact_scope` | 空 | 必填 |
| `impactScheduleDays` | integer | 是 | 0–365 | `change_requests.impact_schedule_days` | 无 | 非负 |
| `impactCost` | number | 是 | 万元 | `change_requests.impact_cost` | 无 | 非负 |
| `status` | enum | 是 | `pending/approved/rejected` | `change_requests.status` | `pending` | 只读 |
| `decisionChain` | array | 是 | 五节点对象 | `joint_decision_signatures` | 空 | 按顺序展示 |
| `currentApprover` | object | 否 | 签署节点 | `joint_decisions` | 空 | 只能有一个 |
| `implementation` | object | 否 | 实施对象 | `change_implementations` | 空 | approved后才存在 |
| `evidence` | array | 否 | 附件对象 | `change_implementation_evidence` | 空 | 按创建时间倒序 |
| `verificationNote` | string | 否 | 验证意见 | 实施记录 | 空 | 双PM均需填写 |
| `closedAt` | datetime | 否 | ISO时间 | 实施记录 | 空 | 只读 |

## 4. API 接口清单

- `GET /api/changes?projectId=<projectId>` — server.mjs:965
- `GET /api/changes/:id` — 缺失（🔴 新增）
  - 用途：返回单个变更单完整详情。
  - 响应体：`{change, requirement, project, requester, decisionChain, implementation, evidence}`。
  - 错误码：50001；30001；权限：调用 `visibleProject`；审计：读取不写。
- `GET /api/collaboration/decisions/:id/detail` — closure.mjs:386
- `POST /api/collaboration/decisions/:id/sign` — server.mjs:1834
- `POST /api/changes/:id/evidence` — closure.mjs:392
- `GET /api/changes/:id/implementation` — closure.mjs:389
- `POST /api/changes/:id/baseline` — closure.mjs:390
- `POST /api/changes/:id/implementation` — closure.mjs:391
- `POST /api/changes/:id/verify` — closure.mjs:393
- `POST /api/changes/:id/close` — closure.mjs:394
- `GET /api/audit-logs?entityType=requirement_change&entityId=<id>` — server.mjs:2209
  - 用途：读取变更单审计摘要。
  - 错误码：30001；权限：审计范围允许；审计：读取不写。

## 5. 闭环剧本

| 环节 | 内容 |
|---|---|
| ① 发起 | 用户在 `ChangesPage` 点击 CR-xxx 或访问 `/changes?change=<id>`。 |
| ② 处理 | 详情组件加载变更单、关联需求、申请附件和五节点决策；当前节点只展示一次。 |
| ③ 审核 | 签署人填写具体证据、影响或整改意见后批准或驳回；申请人不能自审（BR-GATE-03）；**超期 3 个自然日（gate.signDeadlineDays）未签署 → 自动转 ABSTAINED_TIMEOUT，按主导方意见执行（v3 BR-GATE-04），并写审计 entityType=gate, action=sign（自动弃权）**。 |
| ④ 结果 | 全部通过后出现实施入口；任一节点否决后状态变为已驳回。 |
| ⑤ 记录 | 详情页记录当前节点、签署人、时间和意见，并显示在审计摘要中；审计：entityType=requirement_change, action=view_detail / sign / approve / reject。 |
| ⑥ 归档 | 实施和验证均完成后关闭详情状态；历史意见、附件和审计永久保留。 |

## 6. 现状 vs v3 差距（量化）

- **缺失接口（1 个）**：`GET /api/changes/:id` 单条详情接口；当前仅能通过列表 + 客户端筛选获得。
- **缺失页面（1 个）**：`ChangeDetailPage` 独立组件；当前依赖 `ChangesPage` 的 `selectedChange` 客户端状态，刷新后丢失选中状态。
- **缺失状态机环节（1 个）**：URL `?change=<changeId>` 深链接未实现，刷新页面无法定位变更单。
- **缺失字段（2 个）**：`dual_pm_project`（当前 `ChangesPage` 通过 `getDualAssignment` 计算但未存储）、`block_stage_exit`（未闭环阻断阶段出口未在接口返回）。
- **验收用例覆盖度**：v3 验收清单 AC-CHG-08~12 共 5 条详情场景，当前覆盖 0 条。

## 7. 验收用例

**用例 1：URL 直达详情**
```text
Given 服务端存在 CR-001
When 用户访问 /changes?change=CR对应的UUID
Then 页面自动选中 CR-001
And 刷新浏览器后仍保持当前变更
And 不显示其他项目的变更内容
```

**用例 2：当前节点签署**
```text
Given 当前用户是 market_pm 节点且状态 pending
When 用户填写"证据完整"并确认
Then 下一节点变为可处理
And 当前节点显示 approved、签署人和时间
And 审计日志记录 decision 和 comment（entityType=requirement_change, action=sign）
```

**用例 3：验证未完成**
```text
Given 变更已批准并有实施记录
When 只有市场PM完成验证
Then 关闭按钮仍禁用
And 研发PM未验证状态持续显示
And 页面不能修改历史验证结果
```

**用例 4：申请附件追溯**
```text
Given 变更申请阶段已上传证据
When 用户打开详情附件列表
Then 显示原始文件名、大小、上传人和说明
And 附件链接可打开原文件
And 证据不会在关闭后删除
```

**用例 5：重复签署**
```text
Given 当前节点已经 approved
When 用户再次点击确认或驳回
Then 返回 50002 节点已处理
And 节点状态不发生变化
And 不产生第二条重复审计
```

---

# 27. 项目移交

## 1. 页面元信息

- 编号：27
- 名称：项目移交
- URL 路径：`/handoffs`
- 页面性质：单项目、批量项目及长期产品责任移交工作台
- 主用角色：项目负责人、接任市场PM、接任研发PM、产品组长、超级管理员
- 可见角色：所有登录用户
- 可操作角色：项目当前负责人可发起；接任人和治理节点可确认；超级管理员可监督
- 优先级：P2
- 关联规则：BR-HAND-01（单/批量移交）、BR-HAND-02（数据跟随）、BR-HAND-03（津贴按接手时评级锁定）、BR-HAND-04（审计留痕）、BR-USER-06（离职处置先移交后禁用）、BR-ORG-01~06、BR-ADM-02
- 现有实现：完整（`HandoffWorkbench` ClosurePages.jsx:38-58 + `BatchHandoffPanel` FinalRulesPages.jsx:49-58 + server.mjs:2045、server.mjs:2121/2135 单项目闭环 + final-rules.mjs:334~337 批量闭环）

## 2. 页面布局

- `/handoffs` 同时挂载 `<HandoffWorkbench>` 与 `<BatchHandoffPanel>`（App.jsx:127）。
- 顶部显示 `<StageRail>`（App.jsx:127）。
- `<HandoffWorkbench>` 副标题说明独立收件箱、原子转移、历史保留。
- 左侧"待我接收"列表卡片显示项目、发起人、接任人、状态、创建时间。
- 左侧第二组"我发起的"移交。
- 右侧详情显示人员关系、未完成动作/资料/待决确认/AI会话数。
- 责任确认链展示接收人、跨组组长、超级管理员。
- 当前用户为接收人时显示处理意见编辑器。
- 项目移交完成后显示"继续移交产品长期责任"按钮。
- 发起区仅对活动项目当前负责人显示。
- 接任人选择器显示姓名、工号、组、等级、在管项目数。
- 已有待移交时显示只读 banner。
- 已结项项目只读，不显示发起按钮。
- 批量移交面板显示项目、专业、选接任人。
- 同一项目和同一专业线在草稿中只允许出现一次。
- 批量发起前先调用预览接口。
- 任一子流程预检失败时不得创建批次。
- 批量批次卡显示所有子流程和当前治理节点。
- 任意子流程驳回时批次整体驳回，不得部分转移。

## 3. 字段模型

| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---:|---|---|---|---|
| `handoffId` | string | 是 | UUID | URL/接口 | 空 | 只能打开可见移交 |
| `projectId` | string | 是 | UUID | `projects` | 当前项目 | 必须活动 |
| `fromUserId` | string | 是 | UUID | `handoffs.from_user_id` | 无 | 当前项目负责人 |
| `toUserId` | string | 是 | UUID | 接任人目录 | 空 | 在岗、角色匹配 |
| `handoffScope` | enum | 是 | `project/product_only` | `handoffs` | `project` | 不可直接改历史 |
| `includeProduct` | boolean | 否 | true/false | 移交表单 | false | 开启后增加治理节点 |
| `note` | string | 否 | ≤1000字符 | 移交表单 | 默认说明 | 记录责任范围 |
| `status` | enum | 是 | `pending/accepted/rejected/cancelled` | `handoffs.status` | `pending` | 状态机 |
| `scope.openWorkItems` | integer | 是 | ≥0 | 工作项统计 | 无 | 只读 |
| `scope.documents` | integer | 是 | ≥0 | 产物统计 | 无 | 只读 |
| `scope.pendingReviews` | integer | 是 | ≥0 | 评审统计 | 无 | 只读 |
| `scope.aiConversations` | integer | 是 | ≥0 | AI会话统计 | 无 | 只读 |
| `currentStep` | integer | 是 | ≥1 | `handoff_approvals` | 1 | 仅当前节点可处理 |
| `approvalRole` | enum | 是 | `recipient/from_market_lead/to_market_lead/market_lead/admin` | 治理链 | 空 | 角色固定 |
| `decision` | enum | 是 | `pending/approved/rejected` | 治理链 | `pending` | 重复处理返回 50002 |
| `comment` | string | 否 | ≥2字符 | 治理节点 | 空 | 必填 |
| `batchTitle` | string | 是 | 2–200字符 | 批量表单 | 空 | 必填 |
| `batchNote` | string | 是 | ≥4字符 | 批量表单 | 空 | 必填 |
| `batchStatus` | enum | 是 | `pending/completed/rejected` | `handoff_batches` | `pending` | 原子批次 |
| `leg.track` | enum | 是 | `market/rd` | 批量表单 | `market` | 只能匹配专业线 |
| `leg.projectId` | string | 是 | UUID | 批量表单 | 当前项目 | 项目必须活动 |
| `leg.toUserId` | string | 是 | UUID | 接任人目录 | 空 | 不能等于原负责人 |
| `sourceHandoffId` | string | 否 | UUID | 产品长期责任移交 | 空 | 仅产品责任场景 |

## 4. API 接口清单

- `GET /api/handovers/inbox` — closure.mjs:305
- `GET /api/projects/:id/handoff-candidates` — closure.mjs:309
- `GET /api/handovers/:id/preview` — closure.mjs:321
- `POST /api/handovers` — server.mjs:2045
- `POST /api/handovers/:id/decision` — closure.mjs:346
- `POST /api/handovers/:id/cancel` — closure.mjs:322
- `POST /api/handovers/:id/product-continuation` — closure.mjs:323
- `GET /api/handoff-batches` — final-rules.mjs:335
- `POST /api/handoff-batches/preview` — final-rules.mjs:334
- `POST /api/handoff-batches` — final-rules.mjs:336
- `POST /api/handoff-batches/:batchId/legs/:legId/decision` — final-rules.mjs:337
- `GET /api/workflow/tasks?bucket=pending` — closure.mjs:269
- `POST /api/admin/people-sync/run` — extensions.mjs:367
- `GET /api/admin/transfer-readiness?toUserId=...` — server.mjs:647（移交准备度）

## 5. 闭环剧本

| 环节 | v3 状态机 | 内容 |
|---|---|---|
| ① 发起 | `DRAFT` | 项目负责人选择接任人、说明、是否包含产品长期责任；批量模式先加入多个子流程。 |
| ② 处理 | `PENDING` | 服务端统计未完成动作、资料、待决确认和AI会话，创建审批链并投递接收任务。 |
| ③ 审核 | `PENDING→ACCEPTED/REJECTED` | 接任人确认范围；跨组时原、新市场组长确认；包含产品责任时由组长和超级管理员锁定。 |
| ④ 结果 | `ACCEPTED` | 原子更新项目负责人、双PM、市场/研发任务责任和权限；批量任一子流程拒绝则批次不生效。 |
| ⑤ 记录 | — | 写入 `handover / create|approve|reject|handover`、`handoff_batch / create|complete|reject` 审计；审计：entityType=handover, action=create / approve / reject / handoff；entityType=handoff_batch, action=create / complete / reject。 |
| ⑥ 归档 | `COMPLETED` | 移交记录和历史资料永久保留；原负责人转为只读或历史角色；产品责任单独追踪。 |

## 6. 现状 vs v3 差距（量化）

- **缺失字段（3 个）**：v3 BR-HAND-03 要求 `lockedLevel`、`lockedAmount` 在 `ProjectMember` 按接手时评级快照锁定（db.mjs:312-325 已设计 `ProjectMember.lockedLevel/lockedAmount`），当前 `handoff.finalize` 仅写入 `dual_pm_assignments.market_level_locked`，未写入 `ProjectMember` 表；v3 BR-USER-06 要求 `account_status` 在 `FROZEN_PENDING_HANDOVER` 态，当前代码未在 `applyPeople`/`handoff` 流程中触发该状态；批量移交缺少 `leg.from_user_id`（`final-rules.mjs:336` 自动从 dual_pm_assignments 推断但未持久化）。
- **缺失接口（2 个）**：v3 BR-USER-06 要求 `POST /api/admin/handoff-batches/assign-frozen-status`（设置冻结态）和 `POST /api/admin/handoff-batches/unfreeze`（解冻），当前代码无；v3 BR-HAND-03 要求 `GET /api/projects/:id/handover-allowance-preview`（按接手时评级锁定预览），当前无。
- **缺失定时任务（1 个）**：v3 BR-USER-06 要求 15 个自然日（`handover.resignDeadlineDays`）逾期升级超级管理员，无 cron job。
- **缺失状态机环节（1 个）**：v3 BR-USER-06 冻结态仅有移交权限；当前代码无登录态限制（`auth` 中间件未检查 `account_status`）。
- **验收用例覆盖度**：v3 验收清单 AC-HAND-01~06 共 6 条，当前实现 4 条（单项目、批量、长期产品、取消），缺失 2 条（冻结态、超期升级）。

## 7. 验收用例

**用例 1：单项目市场责任移交**
```text
Given 项目负责人选择同组织在岗市场PM
When 提交项目移交且不包含产品责任
Then 系统生成接收人及必要的组长确认链
And 接收人确认后项目 owner_id 更新为接任人
And ProjectMember.lockedLevel 按接手时评级快照锁定
And 未完成动作负责人和项目成员权限原子更新
```

**用例 2：退回后禁止部分生效**
```text
Given 批量批次包含项目和研发/市场子流程
When 任意子流程组长选择拒绝
Then 批次状态为 rejected
And 所有项目责任保持原值
And 不产生部分转移或部分奖金变更
```

**用例 3：产品长期责任续接**
```text
Given 项目移交已 accepted 且未包含产品责任
When 原负责人点击继续移交产品长期责任
Then 系统按同一接任人创建 product_only 移交
And 项目历史和研发责任不变
And 产品 owner_market_pm_id 仅在全部节点完成后更新
```

**用例 4：离职冻结态**
```text
Given 第三方人员同步识别员工 RESIGNED
When 系统生成待移交任务
Then 账号进入 FROZEN_PENDING_HANDOVER
And auth 中间件拒绝除移交以外的所有接口
And 全部项目完成后才变为 DISABLED
And 超过15个自然日自动升级超级管理员并记录审计（entityType=handover, action=escalate）
```

**用例 5：津贴按接手评级锁定**
```text
Given 接任人为 L3 且 L3 升级为 L4
When 移交完成
Then 项目津贴台账使用接手时评级 L3
And 在研项目后续津贴保持 L3 不变
And 新挂载项目按 L4
```

---

# 28. 组织架构

## 1. 页面元信息

- 编号：28
- 名称：组织架构
- URL 路径：建议新增 `/organization` 或作为 `/admin?tab=organization`
- 页面性质：系统管理子页
- 主用角色：超级管理员
- 可见角色：超级管理员
- 优先级：P2
- 关联规则：BR-ORG-01~06（主组/协同组/组长权限）、BR-USER-02（人员同步）、BR-USER-07（L1-L5 API 权威源）、BR-HAND-02（数据跟随）
- 现有实现：缺失（`/identity-sync` 是人员同步配置页、`AdminPage` 仅展示只读成员表，未提供产品组、协同组、跨组可见范围的组织树和权限验证）

## 2. 页面布局

- 页面标题"组织架构"。
- 副标题说明组织数据由第三方人员API同步，禁止本地修改。
- 顶部显示"第三方托管 · 禁止本地修改" banner。
- 页面展示组织同步时间和下次计划时间。
- 左侧产品组树显示产品组名称、组长、成员数、主组/协同组标识。
- 点击产品组后右侧显示成员表格。
- 右侧成员表显示姓名、工号、角色、专业线、组内关系、等级、在管项目数。
- 页面显示组长的直属关系和跨组项目可见范围。
- 页面提供产品组搜索。
- 页面提供按市场/研发/组长角色筛选。
- 页面提供"仅显示异常同步项"筛选。
- 页面显示组织关系图或折叠树。
- 页面不显示本地新增产品组按钮。
- 页面不显示本地新增或修改组长的按钮。
- 页面不显示代理组长或代审人入口（BR-ORG-04）。
- 页面不提供个人L1–L5等级编辑（BR-USER-07）。
- 页面提供"查看同步日志"入口。

## 3. 字段模型

| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---:|---|---|---|---|
| `groupId` | string | 是 | UUID | `product_groups` | 无 | 唯一、只读 |
| `groupName` | string | 是 | 产品组/产品线名称 | `product_groups` | 无 | API返回、不可本地改 |
| `leadUserId` | string | 是 | UUID | `product_groups.lead_user_id` | 无 | 必须为第三方在岗组长 |
| `leadName` | string | 是 | 姓名 | `users` | 无 | 只读 |
| `leadEmployeeNo` | string | 否 | 工号 | `user_profiles` | 空 | 只读 |
| `memberUserId` | string | 是 | UUID | `product_group_members` | 无 | 只能有一个主组 |
| `memberRole` | enum | 是 | `lead/member` | `product_group_members` | `member` | 来自同步 |
| `professionalTrack` | enum | 是 | `market/rd` | `pm_profiles` | 无 | 不得跨角色（BR-USER-08） |
| `currentLevel` | enum | 是 | `L1..L5` | 第三方API/同步表 | 无 | 超级管理员不可修改 |
| `employmentStatus` | enum | 是 | `active/resigned/pending_inactive` | 第三方API | 无 | 离职进入待移交态 |
| `directLeaderExternalId` | string | 否 | 外部组长ID | 人员API | 空 | 必须能解析为组长 |
| `projectCount` | integer | 是 | ≥0 | 项目统计 | 0 | 按权限计算 |
| `sourceManaged` | boolean | 是 | true/false | `users` | true | 组织页必须只读 |
| `sourceUpdatedAt` | datetime | 是 | ISO时间 | `external_identities` | 无 | 用于差异检查 |
| `lastSyncedAt` | datetime | 是 | ISO时间 | `identity_source_providers` | 无 | 同步成功时间 |
| `syncStatus` | enum | 是 | `healthy/stale/failed` | 同步任务 | `healthy` | 失败显示异常 |
| `isMainGroup` | boolean | 否 | true/false | 项目关系 | false | 由市场PM所在组推导 |
| `isCrossGroup` | boolean | 否 | true/false | 项目关系 | false | 双PM项目可能为true |

## 4. API 接口清单

- `GET /api/admin/organization` — 缺失（🔴 新增）
- `GET /api/admin/organization/groups/:groupId` — 缺失（🔴 新增）
- `GET /api/admin/organization/tree` — 缺失（🔴 新增）
- `GET /api/admin/organization/sync-status` — 缺失（🔴 新增）
- `GET /api/admin/identity-source` — extensions.mjs:259
- `POST /api/admin/identity-source/test` — extensions.mjs:278
- `POST /api/admin/people-sync/preview` — extensions.mjs:284
- `POST /api/admin/people-sync/confirm` — extensions.mjs:361
- `POST /api/admin/people-sync/run` — extensions.mjs:367
- `GET /api/admin/people-sync/history` — extensions.mjs:387
- `GET /api/admin/members` — server.mjs:397
- `POST /api/admin/product-groups` — 明确禁用（30001）
- `PUT /api/admin/product-groups/:id/lead` — 明确禁用（30001）

## 5. 闭环剧本

| 环节 | 内容 |
|---|---|
| ① 发起 | 超级管理员进入组织架构页，选择查看产品组、成员或同步状态。 |
| ② 处理 | 页面按产品组和角色加载组织树，不执行本地人员写入。 |
| ③ 审核 | 第三方API提供组长、成员、专业线和等级；页面展示同步差异和异常。 |
| ④ 结果 | 确认同步后，产品组和成员关系按API结果更新；不同步则保持原数据。 |
| ⑤ 记录 | 同步任务、人员变更、等级差异和离职状态写入审计 `user / update`、`people_sync_run / complete`；审计：entityType=user, action=update / disable / handover_freeze；entityType=people_sync_run, action=complete / partial_failed。 |
| ⑥ 归档 | 组织关系和外部身份映射保留历史；离职人员进入待移交态，不直接物理删除。 |

## 6. 现状 vs v3 差距（量化）

- **缺失字段（10 个）**：v3 BR-ORG-01~06 要求 `group_id`、`lead_user_id`、`group_name`、`member_count`、`project_count`、`is_main_group`、`is_cross_group`、`member_role`、`professional_track`、`currentLevel`（其中 `group_id`、`member_role` 已在 `product_group_members` 表中存在，但前端页面无对应字段展示）。
- **缺失接口（4 个）**：`GET /api/admin/organization`、`GET /api/admin/organization/groups/:groupId`、`GET /api/admin/organization/tree`、`GET /api/admin/organization/sync-status`。
- **缺失页面（1 个）**：`<OrganizationPage>` 完整组件；当前仅有 `AdminPage` `members` tab 的简化表。
- **缺失状态机环节（1 个）**：v3 BR-USER-07 要求超级管理员不修改个人等级，但当前无显式权限校验层（API 直接 `updateMember` 修改接口仍存在）。
- **验收用例覆盖度**：v3 验收清单 AC-ORG-01~04 共 4 条，当前实现 0 条。

## 7. 验收用例

**用例 1：识别错误页面**
```text
Given 当前用户不是超级管理员
When 访问 /organization
Then 返回 30001 权限不足
And 不显示人员同步配置
And 不允许调用任何组织写接口
```

**用例 2：组织树展示**
```text
Given 第三方API已同步两个产品组及组长
When 超级管理员进入组织架构页
Then 页面显示产品组、组长、成员数量和同步时间
And 组长字段显示为"第三方同步"
And 不显示代理组长或本地编辑入口
```

**用例 3：等级只读**
```text
Given 某人员API等级为L4而历史系统显示L3
When 超级管理员打开人员详情
Then 页面显示当前API等级L4和历史差异
And 不提供修改按钮
And 差异和同步结果进入审计（entityType=user, action=sync_diff）
```

**用例 4：离职待移交**
```text
Given 同步结果包含 employmentStatus=RESIGNED
When 确认同步
Then 账号进入 FROZEN_PENDING_HANDOVER
And 页面显示待移交项目数
And 不直接删除账号或历史项目
And 写入 people_sync.completed 审计（entityType=people_sync_run, action=complete）
```

**用例 5：本地修改被拒绝**
```text
Given 超级管理员尝试调用产品组新增或修改组长接口
When 服务端校验发现数据必须由API提供
Then 返回 30001 权限不足
And 本地数据不变
And 页面提示请修改第三方人员源
```

---

# 29. KPI 考核-功能 KPI

## 1. 页面元信息

- 编号：29
- 名称：KPI 考核-功能 KPI
- URL 路径：`/performance?tab=kpi`
- 页面性质：双PM项目功能指标评分
- 主用角色：市场PM、研发PM
- 可见角色：项目成员、产品组长、超级管理员
- 优先级：P3
- 关联规则：BR-KPI-01（功能60%+共担40%）、BR-KPI-02（市场PM功能四项各15%）、BR-KPI-03（研发PM功能四项各15%）、BR-KPI-06~07、BR-INC-07（绩效系数映射）
- 现有实现：部分（`PerformancePage` App.jsx:415-430 与 `<KpiDrawer>` App.jsx:433-451；server.mjs:1141 `GET /api/performance/overview`、server.mjs:1162 `PUT /api/performance/kpis/:projectId/:metricCode`、server.mjs:1234 `review` 已实现；`automaticSharedScore` 已实现）

## 2. 页面布局

- `PerformancePage` 顶部保留双PM责任卡（市场PM、研发PM、组长、等级、规则版本、项目分）。
- 页面 tab "项目KPI" 进入本页。
- 页面标题显示"12项项目KPI正式评分入口"。
- 标题说明责任PM填报、功能KPI直属组长复核、共担KPI双组长确认。
- 表格列显示指标、对象、权重、原始/目标、得分、状态、操作。
- 市场行使用市场责任色，研发行使用研发责任色。
- 共担KPI在本页仅展示只读摘要，编辑入口在第30页。
- 点击"填报/查看"打开 `<KpiDrawer>`。
- 抽屉显示评分口径。
- 抽屉显示原始值、目标值、功能KPI建议分。
- 原始值和目标值为必填数字。
- 功能KPI建议分范围0–100。
- 证据说明必须填写或上传至少一份附件（BR-KPI-06）。
- 支持多文件上传 PDF、Word、Excel、PPT 和图片。
- 已复核状态禁止删除证据。
- 提交后显示"已提交，等待组长复核"。
- 被驳回后显示具体原因。
- 组长可调用复核接口选择批准或驳回。

## 3. 字段模型

| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---:|---|---|---|---|
| `projectId` | string | 是 | UUID | boot | 当前项目 | 项目可见 |
| `metricCode` | string | 是 | `PERFORMANCE_METRICS.code` | KPI定义 | 空 | 必须是已发布指标 |
| `metricName` | string | 是 | 中文名称 | KPI定义 | 空 | 只读 |
| `metricCategory` | enum | 是 | `functional/shared` | `kpi_scores` | `functional` | 只读 |
| `professionalTrack` | enum | 是 | `market/rd` | KPI定义 | 空 | 与当前用户匹配 |
| `weight` | number | 是 | 0–1 | KPI定义 | 0.15 | 不允许用户改 |
| `rawValue` | number | 是 | 实测值 | 抽屉表单 | 0 | 必填、数字 |
| `targetValue` | number | 是 | 目标值 | 抽屉表单 | KPI默认目标 | 必填、数字 |
| `score` | number | 条件 | 0–100 | KPI抽屉/复核 | 无 | 功能KPI必填 |
| `evidenceNote` | string | 条件 | ≤2000字符 | KPI抽屉 | 空 | 与附件至少一项（BR-KPI-06） |
| `status` | enum | 是 | `draft/submitted/reviewed/rejected` | `kpi_scores.status` | `draft` | 状态机 |
| `reviewedBy` | string | 否 | UUID | `kpi_scores.reviewed_by` | 空 | 仅复核人 |
| `evidence` | array | 否 | 附件对象 | `attachments` | 空 | 未复核可删 |
| `updatedAt` | datetime | 是 | ISO时间 | 服务端 | 当前时间 | 只读 |
| `sourceMode` | enum | 是 | `hybrid/system/manual` | KPI定义 | `hybrid` | 只读 |

## 4. API 接口清单

- `GET /api/performance/overview?projectId=<projectId>` — server.mjs:1141
- `PUT /api/performance/kpis/:projectId/:metricCode` — server.mjs:1162
- `GET /api/performance/kpis/:projectId/:metricCode/evidence` — server.mjs:1190
- `POST /api/performance/kpis/:projectId/:metricCode/evidence` — server.mjs:1199
- `DELETE /api/performance/kpis/:projectId/:metricCode/evidence/:attachmentId` — server.mjs:1222
- `POST /api/performance/kpis/:projectId/review` — server.mjs:1234
- `POST /api/performance/cycles/generate` — closure.mjs:407

## 5. 闭环剧本

| 环节 | 内容 |
|---|---|
| ① 发起 | 责任PM进入 `/performance?tab=kpi`，选择本人专业线功能指标并打开 `KpiDrawer`。 |
| ② 处理 | 填写原始值、目标值、建议分、证据说明或上传证据，提交为 `submitted`。 |
| ③ 审核 | 对应产品组长在复核接口中批准或驳回；被驳回记录具体意见。 |
| ④ 结果 | 批准后状态为 `reviewed`，项目综合得分刷新；驳回后返回责任PM修改。 |
| ⑤ 记录 | 写入 `kpi_record / submit`、`attachment / create|delete`、`kpi_record / approve|reject` 审计；审计：entityType=kpi_record, action=submit / approve / reject；entityType=attachment, action=create / delete。 |
| ⑥ 归档 | 复核后的KPI参与后续项目绩效和奖金计算；历史修订和证据保留。 |

## 6. 现状 vs v3 差距（量化）

- **缺失字段（1 个）**：`source_mode` 当前固定 `hybrid`，v3 BR-KPI-02/03 要求功能KPI达标线（≤15% 需求变更率、≤30 天窗口偏差、≤15 天上市准时率、≤20% 一次性实现率）的字段化校验（当前仅在 description 中）。
- **缺失接口（1 个）**：`POST /api/performance/cycles/generate` 已实现但前端无周期选择 UI；v3 BR-KPI-08 期望项目级评定周期同步显示。
- **缺失状态机环节（1 个）**：v3 要求功能KPI四类指标达标线（需求变更率 ≤15%、窗口偏差 ≤30 天、上市准时率 ≤15 天、一次性实现率 ≤20%）作为写入校验，当前仅在 `score` 字段允许任意 0–100 输入，无 `metric_spec.target_min` 与 `actual_value` 自动比对。
- **缺失周期管理**：v3 BR-KPI-06 项目绩效综合得分 = 功能KPI×60% + 共担KPI×40%，当前页面无综合分显示与公式溯源。
- **验收用例覆盖度**：v3 验收清单 AC-KPI-01~08 共 8 条，当前覆盖 5 条，缺失 3 条（达标线判定、周期生成、综合分计算）。

## 7. 验收用例

**用例 1：功能KPI填报**
```text
Given 当前用户是市场PM且项目已启用双PM
When 打开 market_requirement_accuracy 并提交原始值、目标值、建议分和证据说明
Then 指标状态变为 submitted
And 证据说明或附件至少一项被保存
And 当前用户不能再次修改为另一组责任人
```

**用例 2：组长复核通过**
```text
Given 市场功能KPI已 submitted
When 市场产品组长调用 review decision=approved
Then 指标状态变为 reviewed
And 项目综合得分重新计算
And 写入 performance.kpi_reviewed 审计（entityType=kpi_record, action=approve）
```

**用例 3：达标线校验**
```text
Given 需求变更率超过 15% 的指标
When 责任PM提交功能KPI
Then 返回 40009 KPI评分缺失并提示实际值超阈值
And 不得进入 submitted 状态
And 抽屉保留输入
```

**用例 4：非责任人查看**
```text
Given 当前用户是研发PM
When 查看市场PM功能KPI
And 用户不是超级管理员
Then 抽屉所有字段只读
And 不显示提交按钮
And 证据仍可按项目范围查看
```

**用例 5：已复核证据不可删除**
```text
Given KPI已 reviewed 且证据上传人是当前用户
When 删除证据
Then 返回 50002 节点已处理
And 文件和KPI状态不变
And 审计不产生删除记录
```

---

# 30. KPI 考核-共担 KPI 归集

## 1. 页面元信息

- 编号：30
- 名称：KPI 考核-共担 KPI 归集
- URL 路径：建议 `/performance?tab=kpi&category=shared` 或新增 `/performance/shared`
- 页面性质：K01–K04 月度归集、季度对账、上市后六个月终算
- 主用角色：产品组长（`kpi.collectorRole=GROUP_LEADER`）、超级管理员
- 协作角色：市场PM、研发PM（提供数据或查看结果）
- 优先级：P3
- 关联规则：BR-KPI-01（功能60%+共担40%）、BR-KPI-04（共担四项）、BR-KPI-05（产品组长录入、次月第 5 工作日 18:00 截止）、BR-KPI-06~07、BR-USER-06
- 现有实现：缺失（当前仅有 `automaticSharedScore` 公式，db.mjs:214-221 与 `kpi_scores` 表（db.mjs:494-501），但没有产品组长归集业务页、归集任务、定时任务与逾期升级）

## 2. 页面布局

- 页面顶部显示项目、双PM、上市日期和当前归集周期。
- 显示"次月第5个工作日18:00前截止"规则提示。
- 截止时间从 `kpi.monthlyDeadlineDay=5`（v3 §7 参数）读取并按 Asia/Shanghai 时区计算。
- 周期选择器提供月份。
- 页面显示四个归集卡：K01 销量/出货、K02 渠道、K03 NPS、K04 场景。
- 每张卡显示目标值、实际值、达成率、建议分、状态、证据数量。
- 产品组长角色显示"录入/复核"按钮。
- 其他角色默认只读。
- 进入录入弹窗后显示本周期产品组归集范围。
- 录入弹窗显示实际数据来源和统计口径。
- K01 输入实际回款（`bonus.salesSource=RECEIPT`）。
- K02 输入实际签约渠道或集成商数量。
- K03 输入 NPS 原始值和有效样本数（`kpi.npsMinSample` 默认 ≥30）。
- K04 输入实际落地场景数和有效验收记录。
- 页面显示 NPS 样本量不足警告。
- 页面显示证据编号和附件说明。
- 产品组长提交后进入"待复核"。
- 复核人查看证据并填写复核意见。
- 同一周期同一指标只能存在一条归集记录。
- 截止前显示正常 banner；截止后显示逾期红色 banner。
- 逾期第 1 天通知产品组长（`kpi.reminderDay1=1`）。
- 逾期第 3 天升级超级管理员（`kpi.reminderDay3=3`）。
- 页面显示季度对账状态。
- 上市后六个月终算时显示"终算中/已锁定"。

## 3. 字段模型

| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---:|---|---|---|---|
| `projectId` | string | 是 | UUID | 当前项目 | 无 | 项目可写或可审核 |
| `period` | string | 是 | `YYYY-MM` | 归集周期 | 当前上月 | 唯一周期 |
| `metricCode` | enum | 是 | `shared_sales/shared_channels/shared_nps/shared_scenarios` | KPI定义 | 空 | 仅K01–K04 |
| `targetValue` | number | 是 | 项目基线目标 | `dual_pm_assignments` | 无 | 正数 |
| `rawValue` | number | 是 | 实际值 | 产品组长 | 0 | 数字、单位明确 |
| `achievementRate` | number | 只读 | % | 服务端 | 0 | `rawValue/targetValue*100` |
| `score` | number | 只读 | 0–100 | 自动公式 | 0 | 按指标公式 |
| `sampleSize` | integer | 条件 | ≥0 | K03问卷 | 0 | NPS 建议 ≥30 |
| `source` | enum | 是 | `manual/erp/crm/acceptance` | 归集表单 | `manual` | 记录来源 |
| `evidenceNote` | string | 条件 | ≥3字符 | 归集表单 | 空 | K03/K04 必填 |
| `attachmentCount` | integer | 只读 | ≥0 | 证据表 | 0 | 附件计数 |
| `status` | enum | 是 | `draft/submitted/reviewed/rejected/locked` | 归集表 | `draft` | 状态机 |
| `submittedBy` | string | 否 | UUID | 归集记录 | 空 | 产品组长 |
| `submittedAt` | datetime | 否 | ISO时间 | 服务端 | 空 | 首次提交时间 |
| `reviewedBy` | string | 否 | UUID | 归集记录 | 空 | 复核产品组长 |
| `reviewedAt` | datetime | 否 | ISO时间 | 服务端 | 空 | 审核时间 |
| `deadlineAt` | datetime | 是 | Asia/Shanghai 截止 | 参数/任务 | 空 | 次月第5工作日18:00 |
| `reminderLevel` | enum | 是 | `none/day1/day3` | 定时任务 | `none` | 逾期升级 |
| `quarterlyStatus` | enum | 是 | `pending/reconciled/exception` | 对账任务 | 空 | 季度对账 |
| `isLocked` | boolean | 是 | true/false | 终算记录 | false | 锁定后不可改 |

## 4. API 接口清单

- `GET /api/performance/overview?projectId=<projectId>` — server.mjs:1141
- `GET /api/performance/shared-kpis/collect?projectId=<projectId>&period=<YYYY-MM>` — 缺失（🔴 新增）
- `POST /api/performance/shared-kpis/collect` — 缺失（🔴 新增）
- `PUT /api/performance/shared-kpis/collect/:id` — 缺失（🔴 新增）
- `POST /api/performance/shared-kpis/collect/:id/review` — 缺失（🔴 新增）
- `GET /api/performance/shared-kpis/:id/evidence` — 缺失（🔴 新增）
- `POST /api/performance/shared-kpis/scan-deadline` — 缺失（🔴 新增定时任务）
- `GET /api/performance/shared-kpis/reconcile?quarter=<YYYY-Qn>` — 缺失（🔴 新增）
- `POST /api/performance/shared-kpis/lock` — 缺失（🔴 新增）
- `GET /api/admin/system-config/kpi` — 缺失（🔴 新增配置接口）

## 5. 闭环剧本

| 环节 | 内容 |
|---|---|
| ① 发起 | 每月归集窗口打开后，系统按产品组和项目创建 K01–K04 周期记录，投递产品组长任务。 |
| ② 处理 | 产品组长填入实际值、数据来源、样本量、证据说明和附件；服务端计算达成率和自动分（`automaticSharedScore`）。 |
| ③ 审核 | 产品组长复核统计周期、单位和证据；截止日为次月第 5 个工作日 18:00（`kpi.monthlyDeadlineDay`），逾期升级超级管理员。 |
| ④ 结果 | 复核通过后进入季度对账和上市六个月终算，锁定后供 KPI、津贴和奖金使用。 |
| ⑤ 记录 | 写入 `kpi_record / submit|update|approve|reject|lock`、`/ deadline_reminder`、`/ deadline_escalated` 审计；审计：entityType=kpi_record, action=submit / update / approve / reject / lock / deadline_reminder / deadline_escalated。 |
| ⑥ 归档 | 锁定记录作为绩效结算依据，不允许覆盖或删除；历史来源、样本和证据永久保留。 |

> ⚙️ 跨页守护（D.6.6 第 4 条）：共担 KPI 月度归集（BR-KPI-05 次月第 5 工作日 18:00）—— 30 共担 KPI 归集页 cron 任务 `kpi_monthly_collect_cron`（每月 1 日 09:00 创建周期 + 5 日 18:00 截止扫描）服务端执行：`deadlineAt < now() AND submitted=false` → 自动升级超级管理员（`entityType=kpi_record, action=deadline_escalated`）并写入通知；锁定后供 31 项目绩效评定 + 34 奖金池 + 33 津贴台账取数。

## 6. 现状 vs v3 差距（量化）

- **缺失字段（10 个）**：`period`、`deadlineAt`、`reminderLevel`、`quarterlyStatus`、`isLocked`、`collectorRole`（产品组长角色映射）、`nps_min_sample`、`monthly_deadline_day`、`source_mode`（K01 回款源）、`window_id`。
- **缺失接口（9 个）**：上方 API 清单中标注"缺失（🔴 新增）"的全部 9 个。
- **缺失定时任务（1 个）**：v3 BR-KPI-05 要求每月扫描即将截止和已逾期周期，目前无 cron job。
- **缺失状态机环节（2 个）**：v3 BR-KPI-05 逾期升级未实现；季度对账状态机未实现。
- **验收用例覆盖度**：v3 验收清单 AC-KPI-09~14 共 6 条共担场景，当前覆盖 0 条。

## 7. 验收用例

**用例 1：产品组长录入**
```text
Given 当前用户是项目所属产品组长且周期未截止
When 提交 K01–K04 实际值和数据来源
Then 归集记录状态为 submitted
And K01/K02/K04 按实际值/目标值计算达成率
And K03 在有效样本数不足 30 时显示阻断警告
```

**用例 2：截止时间计算**
```text
Given 归集周期为 2026-08
When 系统计算截止时间
Then 截止时间为 2026年9月第5个工作日18:00
And 页面按 Asia/Shanghai 时区显示
And 不以自然日或周末简单替代
```

**用例 3：逾期升级**
```text
Given 周期已超过截止时间 1 个自然日
When 定时任务扫描
Then 产品组长收到 day1 提醒
And 超过第3天仍未完成时超级管理员收到升级提醒
And 写入对应 reminder|escalated 审计（entityType=kpi_record, action=deadline_reminder / deadline_escalated）
```

**用例 4：季度对账**
```text
Given 同一指标存在月份间差异
When 产品组长打开季度对账
Then 页面列出实际值、目标值、达成率和评分差异
And 产品组长可以填写对账备注
And 未锁定数据可退回修改
```

**用例 5：六个月终算锁定**
```text
Given 项目已过上市后 6 个月且归集已复核
When 超级管理员执行终算
Then 记录状态变为 locked
And 后续修改接口返回 50002 节点已处理
And 共担KPI结果进入项目绩效和奖金计算
```

---

# 31. 项目绩效评定

## 1. 页面元信息

- 编号：31
- 名称：项目绩效评定
- URL 路径：建议 `/performance?tab=project-review`
- 页面性质：项目维度综合得分评定
- 主用角色：市场PM、研发PM、市场产品组长、研发产品组长
- 可见角色：双PM项目成员、产品组长、超级管理员
- 优先级：P3
- 关联规则：BR-KPI-08（评定对象=项目概念→发布；时点=上市后 30 日内；权重自评 20% + 市场组长 40% + 研发组长 40%；和恒 100%）、BR-KPI-06（综合得分=功能×60%+共担×40%）、BR-GATE-05（90 天复盘）、BR-INC-07（绩效系数映射）
- 现有实现：缺失（当前 PerformancePage 与 KpiDrawer 无项目评定页面、无权重字段、无四类评分卡、无锁定模型）

## 2. 页面布局

- 页面顶部显示项目名称、项目等级和上市日期。
- 页面显示评估区间"概念阶段 → 发布阶段"。
- 页面显示评定截止日和 G5 最迟复盘日期（`kpi.reviewDaysAfterLaunch=30`、`gate.reviewDays90=90`）。
- 页面展示评定对象：市场和研发双PM。
- 市场侧评定卡显示市场PM自评、市场组长评、研发组长评。
- 研发侧评定卡显示研发PM自评、市场组长评、研发组长评。
- 每张评定卡显示分项得分、权重、证据说明和提交状态。
- 页面固定显示权重：自评 20%、市场组长 40%、研发组长 40%（`kpi.reviewWeights`）。
- 权重合计必须为 100%（前端联动校验）。
- 页面提供KPI功能得分和共担KPI得分的只读摘要。
- 页面提供进入 KPI 明细页的链接。
- 市场PM只编辑自己的自评分。
- 市场产品组长只编辑市场侧对双PM的评分。
- 研发产品组长只编辑研发侧对双PM的评分。
- 超级管理员可以查看，必要时代为锁定但不能编辑评分。
- 页面显示"尚未到上市后30日""可开始""已逾期"。
- 页面显示评定规则配置来源。
- 页面显示计算过程而非仅显示最终结果。
- 页面底部显示暂存、提交和锁定按钮。
- 暂存仅保存草稿，不产生最终得分。
- 提交前校验四个评定卡是否完整。
- 全部四类评分完成后可提交。
- 任一评分被驳回时只允许责任人修改。
- 锁定后所有字段只读。

## 3. 字段模型

| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---:|---|---|---|---|
| `projectId` | string | 是 | UUID | 当前项目 | 无 | 已启用双PM |
| `ruleVersion` | string | 是 | 绩效规则版本 | 双PM配对 | 无 | 锁定版本 |
| `reviewWindow` | object | 是 | concept→publish | 项目阶段 | 无 | 只读 |
| `launchDate` | date | 是 | L08上市日期 | `dual_pm_assignments` | 无 | 不可随意修改 |
| `reviewDeadline` | datetime | 是 | 上市后30日 | 系统计算 | 无 | 逾期提醒 |
| `latestGateDeadline` | datetime | 是 | 上市后90日 | G5规则 | 无 | 最迟完成 |
| `marketSelfScore` | number | 是 | 0–100 | 市场PM | 0 | 必填 |
| `marketLeaderScore` | number | 是 | 0–100 | 市场产品组长 | 0 | 必填 |
| `rdLeaderMarketScore` | number | 是 | 0–100 | 研发产品组长 | 0 | 必填 |
| `rdSelfScore` | number | 是 | 0–100 | 研发PM | 0 | 必填 |
| `rdLeaderRdScore` | number | 是 | 0–100 | 研发产品组长 | 0 | 必填 |
| `marketCompositeScore` | number | 只读 | 0–100 | 服务端 | 0 | `20/40/40` |
| `rdCompositeScore` | number | 只读 | 0–100 | 服务端 | 0 | `20/40/40` |
| `status` | enum | 是 | `draft/submitted/approved/locked/rejected` | 项目绩效表 | `draft` | 状态机 |
| `evidenceNote` | string | 否 | 文本 | 评定表 | 空 | 评分依据 |
| `comment` | string | 条件 | 文本 | 评定表 | 空 | 驳回必须填写 |
| `submittedBy` | string | 否 | UUID | 评分人 | 空 | 只读 |
| `lockedAt` | datetime | 否 | ISO时间 | 锁定人 | 空 | 锁定后只读 |
| `performanceCoefficient` | number | 只读 | 0/0.3/0.6/0.8/1 | `bonus.performanceTiers` | 0 | 由综合分映射 |

## 4. API 接口清单

- `GET /api/performance/project-reviews/:projectId` — 缺失（🔴 新增）
- `PUT /api/performance/project-reviews/:projectId/draft` — 缺失（🔴 新增）
- `POST /api/performance/project-reviews/:projectId/submit` — 缺失（🔴 新增）
- `POST /api/performance/project-reviews/:projectId/decision` — 缺失（🔴 新增）
- `POST /api/performance/project-reviews/:projectId/lock` — 缺失（🔴 新增）
- `GET /api/performance/overview?projectId=<projectId>` — server.mjs:1141
- `GET /api/admin/system-config/kpi` — 缺失（🔴 新增配置接口）

## 5. 闭环剧本

| 环节 | 内容 |
|---|---|
| ① 发起 | 项目上市后 30 日内（`kpi.reviewDaysAfterLaunch=30`），市场PM、研发PM 和各自产品组长收到项目绩效评定任务。 |
| ② 处理 | 四类角色分别填写自评或评定分、证据说明和意见，服务端按 20/40/40 计算双PM综合分。 |
| ③ 审核 | 产品组长对相应评分卡批准或驳回；驳回必须填写原因，责任人修改后重提；**评分卡审批超期 3 个自然日（gate.signDeadlineDays）未签署 → 自动转 ABSTAINED_TIMEOUT，按主导方意见执行（v3 BR-GATE-04），并写审计 entityType=gate, action=sign（自动弃权）**。 |
| ④ 结果 | 全部评分卡通过后形成市场PM和研发PM综合得分，映射为成功奖金绩效系数（`bonus.performanceTiers`）。综合得分计算结果 → 进入 34 奖金池核算页；`bonus.performanceTiers` 与 4 个算例见附录 D.6.5 |
| ⑤ 记录 | 写入 `kpi_record / submit|approve|reject|lock` 审计；审计：entityType=kpi_record, action=submit / approve / reject / lock。 |
| ⑥ 归档 | 锁定结果作为奖金计算输入，保留规则版本、评分人、时间和证据。 |

## 6. 现状 vs v3 差距（量化）

- **缺失字段（15 个）**：`reviewWeights`（20/40/40）、`ruleVersion`、`reviewWindow`、`launchDate`、`reviewDeadline`、`latestGateDeadline`、`marketSelfScore`、`marketLeaderScore`、`rdLeaderMarketScore`、`rdSelfScore`、`rdLeaderRdScore`、`marketCompositeScore`、`rdCompositeScore`、`performanceCoefficient`、`lockedAt`。
- **缺失接口（5 个）**：上 API 清单中标注"缺失（🔴 新增）"的全部 5 个。
- **缺失表（1 个）**：`project_performance_reviews` 表（项目绩效评定模型）。
- **缺失定时任务（1 个）**：v3 BR-KPI-08 上市后 30 日提醒与 90 日升级无 cron job。
- **缺失状态机环节（2 个）**：v3 BR-KPI-08 自评/组长评四类评分状态机未实现；权重合计 100% 校验未实现。
- **验收用例覆盖度**：v3 验收清单 AC-KPI-15~21 共 7 条，当前覆盖 0 条。

## 7. 验收用例

**用例 1：权重校验**
```text
Given 项目绩效评定使用市场组长和研发组长
When 页面载入默认权重
Then 显示自评 20%、市场组长 40%、研发组长 40%
And 权重合计必须为 100%
And 修改任何权重都不允许直接提交
```

**用例 2：综合得分计算**
```text
Given 市场PM自评80、市场组长90、研发组长85
When 评分卡全部通过
Then 市场PM综合分 = 80×20%+90×40%+85×40% = 86
And 综合分参与成功奖金绩效系数映射
And 结果显示完整计算过程
```

**用例 3：超期提醒**
```text
Given 项目已上市超过30日但评定未锁定
When 定时任务扫描
Then 向四位评定责任人生成提醒任务
And 超过 G5 最迟90日仍未锁定时升级超级管理员
And 写入 deadline_reminder 审计（entityType=kpi_record, action=deadline_reminder）
```

**用例 4：锁定后追溯**
```text
Given 项目绩效综合分已经锁定
When 用户尝试修改评分
Then 返回 50002 节点已处理
And 页面显示锁定人、锁定时间和规则版本
And 奖金计算使用锁定快照
```

**用例 5：组长驳回**
```text
Given 产品组长对整体方案不认可
When 调用 decision=rejected 并填写意见
Then 状态回到 draft|rejected
And 方案提交人和维度责任人收到通知
And 不得进入奖金计算
```

---

# 32. 项目详情-KPI 考核

## 1. 页面元信息

- 编号：32
- 名称：项目详情-KPI 考核
- URL 路径：建议 `/projects/:projectId/performance`；当前可复用 `/performance?projectId=<projectId>&tab=kpi`
- 页面性质：项目详情中的 KPI 汇总子页
- 主用角色：项目成员
- 可见角色：所有可查看项目范围的用户
- 优先级：P3
- 关联规则：BR-KPI-01、BR-KPI-04~07、BR-ORG-05
- 现有实现：部分（`PerformancePage` App.jsx:415-430 已基于 `boot.project.id` 渲染 KPI 表格；服务端 `/api/performance/overview` 完整；缺少项目详情上下文面包屑、嵌套 URL、独立详情组件）

## 2. 页面布局

- 页面外层使用 `<PageFrame title="项目详情 / KPI考核">`。
- 顶部面包屑显示项目空间 → 当前项目 → KPI考核。
- 面包屑上级可点，项目名称和页签末级不可点。
- 顶部显示项目名称、项目编号、产品、当前阶段。
- 页面显示双PM负责人卡。
- 负责人卡显示市场PM、研发PM、等级、规则版本。
- 页面显示功能 KPI 60% 和共担 KPI 40% 的权重提示（`kpi.functionalWeight=0.6`、`kpi.sharedWeight=0.4`）。
- 页面显示KPI完成度和综合得分。
- 页面显示"功能KPI填报""共担KPI归集""项目绩效评定"三个入口。
- 点击功能KPI进入第29页内容。
- 点击共担KPI进入第30页内容。
- 点击项目绩效评定进入第31页内容。
- 页面显示每个指标的当前状态。
- 页面显示原始值、目标值、得分和权重。
- 页面显示证据数量。
- 项目KPI表格与 `/performance?tab=kpi` 复用同一数据模型。
- 页面提供"查看证据"操作。
- 项目详情页只读用户进入时显示只读 banner。
- 项目未启用双PM时显示空状态和配置提示。
- 页面显示KPI数据截止时间。
- 页面不直接展示津贴和奖金金额。
- 页面底部可显示 KPI 完成度对项目绩效的影响说明。

## 3. 字段模型

| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---:|---|---|---|---|
| `projectId` | string | 是 | UUID | URL/boot | 当前项目 | 必须属于可见范围 |
| `projectCode` | string | 是 | 项目编号 | `projects` | 无 | 只读 |
| `projectName` | string | 是 | 项目名称 | `projects` | 无 | 只读 |
| `assignment` | object | 是 | 双PM对象 | `dual_pm_assignments` | 无 | 页面启用条件 |
| `functionalWeight` | number | 是 | 0.6 | 系统参数 `kpi.functionalWeight` | 0.6 | 禁止改 |
| `sharedWeight` | number | 是 | 0.4 | 系统参数 `kpi.sharedWeight` | 0.4 | 禁止改 |
| `kpis` | array | 是 | 指标列表 | `kpi_scores` | 空 | 按市场/研发/共担分组 |
| `kpi.metricCode` | string | 是 | KPI编码 | KPI定义 | 无 | 只读 |
| `kpi.metricCategory` | enum | 是 | `functional/shared` | `kpi_scores` | 无 | 只读 |
| `kpi.weight` | number | 是 | 0–1 | KPI定义 | 无 | 合计校验 |
| `kpi.rawValue` | number | 否 | 数值 | `kpi_scores` | 空 | 依指标类型 |
| `kpi.targetValue` | number | 否 | 数值 | `kpi_scores` | 空 | 依指标类型 |
| `kpi.score` | number | 否 | 0–100 | `kpi_scores` | 空 | 0–100 |
| `kpi.status` | enum | 是 | KPI状态 | `kpi_scores` | `draft` | 状态机 |
| `kpi.evidenceCount` | integer | 是 | ≥0 | 附件统计 | 0 | 只读 |
| `functionalScore` | number | 只读 | 0–100 | 服务端 | 0 | 60%部分 |
| `sharedScore` | number | 只读 | 0–100 | 服务端 | 0 | 40%部分 |
| `compositeScore` | number | 只读 | 0–100 | `projectScore` | 0 | 60/40综合 |
| `canEdit` | boolean | 是 | true/false | `access` | false | 责任人或超级管理员 |
| `isReadOnly` | boolean | 是 | true/false | 项目状态/角色 | false | 页面banner条件 |

## 4. API 接口清单

- `GET /api/performance/overview?projectId=<projectId>` — server.mjs:1141
- `GET /api/projects/:id` — server.mjs 公共能力
- `GET /api/performance/kpis/:projectId/:metricCode/evidence` — server.mjs:1190
- `PUT /api/performance/kpis/:projectId/:metricCode` — server.mjs:1162
- `POST /api/performance/kpis/:projectId/review` — server.mjs:1234
- `GET /api/performance/cycles?projectId=<projectId>` — closure.mjs:408

## 5. 闭环剧本

| 环节 | 内容 |
|---|---|
| ① 发起 | 用户从项目详情概览点击"KPI考核"或从导航进入指定项目KPI页。 |
| ② 处理 | 页面按项目读取功能KPI、共担KPI和综合分，责任人打开抽屉填报。 |
| ③ 审核 | 功能KPI由直属组长复核，共担KPI进入第30页归集流程。 |
| ④ 结果 | 页面显示最新综合得分，低于 60 或缺证据的指标显示预警。 |
| ⑤ 记录 | 页面复用 KPI 提交、证据和复核审计；审计：entityType=kpi_record, action=submit / approve / reject；entityType=attachment, action=create。 |
| ⑥ 归档 | 项目KPI快照和证据跟随项目归档，项目完成后只读。 |

## 6. 现状 vs v3 差距（量化）

- **缺失字段（3 个）**：`functionalScore`、`sharedScore`、`compositeScore` 三项综合分在服务端 `projectScore` 计算但前端未渲染（`PerformancePage` 仅显示 `data.scores.market.score`、`data.scores.rd.score`）。
- **缺失页面（1 个）**：`ProjectKpiSummary` 嵌套于项目详情下的独立组件。
- **缺失接口（1 个）**：`GET /api/projects/:projectId/performance/summary` 专属摘要（避免读取 `boot.project.id` 切换）。
- **缺失面包屑/嵌套路由**：当前 PerformancePage 直接挂载 `/performance`，不区分项目子页签。
- **验收用例覆盖度**：v3 验收清单 AC-KPI-22~25 共 4 条项目KPI汇总，当前覆盖 1 条（综合分），缺失 3 条。

## 7. 验收用例

**用例 1：项目上下文下钻**
```text
Given 用户正在查看项目A并点击项目KPI
When 进入 /projects/A/performance
Then 页面显示项目A名称、编号和双PM
And 所有API只请求projectId=A
And 刷新浏览器后仍停留在项目A
```

**用例 2：项目得分计算**
```text
Given 功能KPI得分和共担KPI得分已经复核
When 用户打开项目KPI汇总
Then 页面显示功能 60%、共担 40% 的计算说明
And 项目综合分与项目绩效计算使用同一快照
And 不能由客户端自行重新计算覆盖服务端结果
```

**用例 3：只读项目**
```text
Given 当前用户只能查看项目而不能编辑
When 打开 KPI 抽屉
Then 原始值、目标值、证据和按钮均只读
And 仍可查看附件和历史状态
And 不调用写入接口
```

**用例 4：未启用双PM**
```text
Given 项目没有 dual_pm_assignments
When 打开项目KPI页
Then 显示"当前项目尚未启用双PM"空状态
And 提示超级管理员或产品组长完成配对
And 不显示伪造的KPI数据
```

**用例 5：综合分低于60预警**
```text
Given 项目综合分 = 55
When 用户打开项目KPI汇总
Then 页面顶部显示红色预警
And 显示功能KPI与共担KPI缺项列表
And 链接到对应填报页
```

---

# 33. 激励管理-津贴台账

## 1. 页面元信息

- 编号：33
- 名称：激励管理-津贴台账
- URL 路径：`/performance?tab=allowance`
- 页面性质：月度津贴核算、复核、封顶和停发快照
- 主用角色：市场PM、研发PM、产品组长、超级管理员
- 可见角色：所有可查看绩效项目的人员
- 优先级：P3
- 关联规则：BR-INC-02（津贴锁定）、BR-INC-03（叠加与封顶 2×）、BR-INC-11（无实质产出停发）、BR-INC-12（退出停发）、BR-INC-13（升降级）、BR-INC-15（台账定位）
- 现有实现：部分（`PerformancePage` App.jsx:415-430 allowance tab；server.mjs:1327 `POST /api/performance/allowances/:projectId/generate`、server.mjs:1370 `decision`；缺少连续两个月无实质产出停发单、考核分数 <60 停发规则前端提示、津贴与跨项目封顶累计计算展示）

## 2. 页面布局

- 页面标题"月度津贴快照"。
- 顶部显示当前项目、双PM、当前绩效周期。
- 周期选择器默认当前月份。
- 生成按钮仅对产品组长或超级管理员显示。
- 表格显示周期、人员、锁定等级、基础额度、战略系数、项目分、负反馈、核算额、封顶额、状态。
- 人员行显示市场PM或研发PM。
- 等级字段显示 `lockedLevel`（`ProjectMember.lockedLevel`），不显示API当前等级以免历史快照漂移（v3 BR-INC-02）。
- 基础额度显示 L1–L5 固定映射（`allowance.L1..L5`）。
- 项目分来自项目 KPI 综合分。
- 负反馈显示"正常、减半、停发"。
- 页面显示公式说明：基础额 × 津贴系数 × 负反馈因子。
- 页面显示多项目叠加说明。
- 封顶额显示当前等级基础额的 2 倍（`allowance.capMultiplier=2`）。
- 状态为 `draft` 时显示"待复核"，`reviewed` 显示"已复核"，`locked` 显示"已锁定"。
- 被退回状态显示原因。
- 产品组长可对本人专业线记录复核。
- 超级管理员可复核和锁定。
- 页面显示三项目以上备案提示（`allowance.projectCountThreshold=3`）。
- 页面显示项目分低于 60 的停发原因（`allowance.scoreStopThreshold=60`）。
- 页面增加"连续两个月无实质产出"区域。
- 停发区域显示扫描日期、连续无产出天数、最近产出时间和确认状态。
- 页面不直接修改锁定记录。
- 页面不显示真实工资系统状态。
- 页面提供导出当前筛选结果入口。

## 3. 字段模型

| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---:|---|---|---|---|
| `period` | string | 是 | `YYYY-MM` | 页面筛选 | 当前月 | 正则校验 |
| `projectId` | string | 是 | UUID | 当前项目 | 无 | 项目可见 |
| `userId` | string | 是 | UUID | 双PM | 无 | 市场/研发负责人 |
| `professionalTrack` | enum | 是 | `market/rd` | 双PM分配 | 无 | 固定角色 |
| `lockedLevel` | enum | 是 | `L1..L5` | 绑定时快照 | 无 | 历史不可改 |
| `baseAmount` | number | 是 | 1000–3000 | `allowanceLevels` | 无 | 固定额 |
| `allowanceCoeff` | number | 是 | 项目配置 | `dual_pm_assignments` | 无 | 读取配置 |
| `projectScore` | number | 是 | 0–100 | `projectScore` | 无 | 低于60时停发 |
| `feedbackStatus` | enum | 是 | `正常/减半/停发` | 负反馈 | 正常 | 由责任映射计算 |
| `feedbackFactor` | number | 只读 | 1/0.5/0 | 负反馈 | 1 | 不允许手动改 |
| `calculatedAmount` | number | 只读 | 元 | 服务端 | 0 | 两位小数 |
| `otherProjectAmount` | number | 只读 | 元 | 其他项目台账 | 0 | 用于 2× 封顶 |
| `maxMonthly` | number | 只读 | 元 | 当前等级2倍 | 0 | 基础额×2 |
| `cappedAmount` | number | 只读 | 元 | 服务端 | 0 | 不超过封顶 |
| `status` | enum | 是 | `draft/reviewed/locked/rejected` | `allowance_settlements` | `draft` | 锁定后只读 |
| `outputSnapshot` | object | 否 | 实质产出快照 | 扫描任务 | 空 | 记录最近产出 |
| `stopOrderStatus` | enum | 是 | `none/pending/approved/rejected` | 停发单 | `none` | 超级管理员确认 |
| `lockedAt` | datetime | 否 | ISO时间 | 服务端 | 空 | 锁定时间 |

## 4. API 接口清单

- `GET /api/performance/overview?projectId=<projectId>` — server.mjs:1141
- `POST /api/performance/allowances/:projectId/generate` — server.mjs:1327
- `POST /api/performance/allowances/:id/decision` — server.mjs:1370
- `GET /api/performance/allowances?projectId=<projectId>&period=<YYYY-MM>` — 缺失（🔴 新增统一查询）
- `GET /api/performance/substantive-output` — final-rules.mjs:348
- `POST /api/performance/allowance-stop-orders` — 缺失（🔴 新增）
- `POST /api/performance/allowance-stop-orders/:id/decision` — 缺失（🔴 新增）
- `GET /api/performance/capacity-approvals` — final-rules.mjs:350

## 5. 闭环剧本

| 环节 | 内容 |
|---|---|
| ① 发起 | 产品组长选择月份生成两名双PM津贴核算。 |
| ② 处理 | 服务端读取绑定时等级、项目津贴系数、项目分、负反馈和其他项目已核算金额。 |
| ③ 审核 | 对应产品组长复核；超级管理员锁定。低于 60 分或主责负反馈时停发，连带方减半（BR-INC-11）。 |
| ④ 结果 | 锁定快照写入项目台账，后续月份只读；连续无实质产出生成待确认停发单。 |
| ⑤ 记录 | 写入 `allowance_ledger / generate|approve|lock|reject`、`allowance_stop_order / create|decide` 审计；审计：entityType=allowance_ledger, action=generate / approve / lock / reject；entityType=allowance_stop_order, action=create / decide。 |
| ⑥ 归档 | 台账和停发单永久保留；已锁定记录不得覆盖，项目完成后只读。 |

> ⚙️ 跨页守护（D.6.6 第 3 条）：60 天无产出停发津贴（BR-INC-11）—— 33 津贴台账页 cron 任务 `no_output_check_cron`（每日 02:00）服务端扫描：`lastOutputAt < now() - 60 days` AND `noOutputMonths >= 2` → 自动创建 `allowance_stop_order`（`entityType=allowance_stop_order, action=create`）等待产品组长决定；停发决定写 `action=decide`；低于 60 分场景由 29 功能KPI 同步触发停发。

## 6. 现状 vs v3 差距（量化）

- **缺失字段（5 个）**：`outputSnapshot`、`stopOrderStatus`、`noOutputMonths`、`cappedAmount`（生成时已计算但前端未展示）、`otherProjectAmount`（2×封顶累计输入未展示）。
- **缺失接口（3 个）**：`GET /api/performance/allowances` 统一查询、`POST /api/performance/allowance-stop-orders`、`POST /api/performance/allowance-stop-orders/:id/decision`。
- **缺失定时任务（1 个）**：v3 BR-INC-11 连续两个月无实质产出扫描（final-rules.mjs:346 `scanOutputs` 已实现但未生成停发单）。
- **缺失状态机环节（1 个）**：v3 BR-INC-11 待确认停发单状态机未实现。
- **验收用例覆盖度**：v3 验收清单 AC-INC-12~19 共 8 条，当前覆盖 4 条（生成、复核、锁定、公式），缺失 4 条（停发单、封顶累计、退出停发、升降级）。

## 7. 验收用例

**用例 1：项目分低于60**
```text
Given 项目综合分为 55 分
When 产品组长生成津贴
Then market_paid 和 rd_paid 均按规则停发
And calculated_amount 和 capped_amount 为 0
And 页面显示项目分低于 60 的原因
```

**用例 2：主责和连带责任**
```text
Given 某负反馈主责为市场PM、研发PM为连带
When 生成本月津贴
Then 市场PM反馈因子为 0
And 研发PM反馈因子为 0.5
And 两者分别显示停发和减半
And 奖金计算复用同一负反馈记录
```

**用例 3：多项目2倍封顶**
```text
Given 个人L4且同时在两个项目获得津贴
When 生成第二个项目津贴
Then 个人月度总额不超过 L4 基础额度的 2 倍
And 超出部分在本项目 capped_amount 中扣减
And 页面显示 otherProjectAmount 和 maxMonthly
```

**用例 4：锁定保护**
```text
Given 津贴状态为 locked
When 用户再次调用 decision=locked
Then 返回 50002 节点已处理
And 台账不改变
And 不重复生成结算金额
```

**用例 5：无实质产出停发**
```text
Given 连续60个自然日没有动作状态、交付物、记录或Gate事件
When 每月1日扫描
Then 系统生成待确认停发单而不是自动停发
And 超级管理员确认后才执行
And 写入扫描、停发单和确认审计（entityType=allowance_ledger, action=scan / stop / confirm）
```

---

# 34. 激励管理-奖金池核算

## 1. 页面元信息

- 编号：34
- 名称：激励管理-奖金池核算
- URL 路径：`/performance`，使用 `PerformanceV31Panel` 下方回款面板；建议新增 `/performance?tab=bonus`
- 页面性质：回款台账、六个月核算窗口、奖金池和成功奖金生成
- 主用角色：市场PM、研发PM、市场产品组长、研发产品组长、超级管理员
- 可见角色：项目相关人员
- 优先级：P3
- 关联规则：BR-INC-04（奖金池=目标销售额×5%×差异化系数）、BR-INC-04b（达成率按回款）、BR-INC-05（差异化系数S=1.5/A=1.0/B=0.8）、BR-INC-06（6档阶梯区间制，禁止等值判定）、BR-INC-07（绩效系数5档映射）、BR-INC-08（每人成功奖金公式）、BR-INC-14（退款不回溯）
- 现有实现：部分（`PerformanceV31Panel` FinalRulesPages.jsx:19-30 含回款台账、窗口、战略等级变更、回款复核；server.mjs:1420 `POST /api/performance/bonuses/:projectId/generate` 已实现 `pool = base × 0.05 × bonus_pool_coeff` 和 `salesLadderCoefficient`、`performanceCoefficient`；缺失奖金池计算引擎、6档阶梯区间匹配显式化、4 个算例 A/B/C/D、负反馈系数与个人奖金的完整计算 `feedbackCoeff` 显式映射、绩效系数来源（项目分锁定）、贡献度联动）

## 2. 页面布局

- 页面显示项目、目标销售额、实际上市日期、六个月回款窗口。
- 窗口未形成时显示实际上市日期缺失提示。
- 回款录入区供双PM登记实际回款或退款。
- 录入字段包括金额、发生日期、客户/说明。
- 每条回款必须上传银行回单、账单或其他证据。
- 回款状态显示草稿、已提交、已纳入或已排除窗口。
- 窗口外退款保留记录但不回溯奖金（BR-INC-14）。
- 页面显示目标回款、周期内净回款和核算状态。
- 生成六个月净回款核算后进入双组长复核。
- 双组长批准后由超级管理员锁定。
- 页面显示战略等级 S/A/B 及差异化系数。
- 页面显示达成率和阶梯系数。
- 页面显示功能/共担KPI综合得分和绩效系数。
- 页面显示贡献度市场/研发比例。
- 页面显示负反馈系数。
- 页面显示两人奖金草稿。
- 页面显示总额、公式输入、计算过程和版本。
- 生成按钮仅超级管理员可用。
- 锁定时必须两笔奖金同时存在。
- 页面提供算例 A/B/C/D 结果展示。
- 算例 A 默认展示：500万目标、S级、90%回款、55%/45%贡献、市场绩效0=80。

## 3. 字段模型

| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---:|---|---|---|---|
| `projectId` | string | 是 | UUID | 当前项目 | 无 | 活动双PM项目 |
| `actualLaunchDate` | date | 是 | L08上市日期 | `dual_pm_assignments` | 无 | 未设置禁止计算 |
| `windowStart` | date | 是 | 上市日 | 服务端 | 无 | 只读 |
| `windowEnd` | date | 是 | 上市后6个月 | 服务端 `bonus.monthsWindow=6` | 无 | 只读 |
| `targetSales` | number | 是 | 元 | 立项基线 | 无 | 大于0 |
| `targetReceipts` | number | 是 | 元 | V3.1目标口径 | `targetSales` | 大于0 |
| `entryType` | enum | 是 | `receipt/refund` | 回款表单 | `receipt` | 只接受两值 |
| `amount` | number | 是 | 元 | 回款表单 | 0 | 正数 |
| `occurredOn` | date | 是 | `YYYY-MM-DD` | 回款表单 | 空 | 需在窗口内或保留记录 |
| `description` | string | 是 | ≥4字符 | 回款表单 | 空 | 客户、回款单号等 |
| `inBonusWindow` | boolean | 只读 | true/false | 服务端 | true | 窗口判定 |
| `evidenceCount` | integer | 只读 | ≥0 | `receipt_evidence` | 0 | 草稿提交前必须≥1 |
| `netReceipts` | number | 只读 | 元 | 核算表 | 0 | 回款减窗口内退款 |
| `strategicLevel` | enum | 是 | `S/A/B` | `dual_pm_assignments` | `A` | 变更需审批 |
| `differentialCoefficient` | number | 是 | S=1.5/A=1.0/B=0.8 | 系统参数 `bonus.coefficient.{S,A,B}` | 无 | 后台可配 |
| `achievementRate` | number | 只读 | % | `netReceipts/targetReceipts*100` | 0 | 非负 |
| `ladderCoefficient` | number | 只读 | 0/0.3/0.6/0.8/1.0/1.2 | 系统参数 `bonus.achievementTiers` | 0 | 6 档区间匹配 |
| `projectScore` | number | 是 | 0–100 | 项目绩效 | 0 | 锁定快照 |
| `performanceCoefficient` | number | 只读 | 0/0.3/0.6/0.8/1.0 | `bonus.performanceTiers` | 0 | 项目分映射 |
| `contributionPct` | number | 是 | 市场40–65、研发35–60 | 贡献度 | 无 | 合计100 |
| `feedbackCoefficient` | number | 只读 | 0/0.5/1 | 负反馈 | 1 | 最低锁定值 |
| `bonusPool` | number | 只读 | 元 | 计算引擎 | 0 | 目标销售额×5%×系数 |
| `calculatedAmount` | number | 只读 | 元 | 计算引擎 | 0 | 两人分别计算 |
| `ruleVersion` | string | 是 | `performance-v31` | `dual_pm_assignments` | 无 | 锁定版本 |
| `calculationTrace` | object | 是 | 输入、步骤、结果 | 新增计算表 | 空 | 不可变 |
| `status` | enum | 是 | `draft/locked` | `bonus_settlements` | `draft` | 两笔必须成对 |

## 4. API 接口清单

- `GET /api/performance/receipts/:projectId` — final-rules.mjs:307
- `POST /api/performance/receipts/:projectId` — final-rules.mjs:308
- `POST /api/performance/receipts/:id/evidence` — final-rules.mjs:309
- `POST /api/performance/receipts/:id/submit` — final-rules.mjs:310
- `POST /api/performance/receipts/:projectId/settlement` — final-rules.mjs:311
- `POST /api/performance/receipts/:projectId/settlement-decision` — final-rules.mjs:312
- `POST /api/performance/bonus-calculate/preview` — 缺失（🔴 新增）
- `POST /api/performance/bonus-calculate/confirm` — 缺失（🔴 新增）
- `POST /api/performance/bonuses/:projectId/generate` — server.mjs:1420
- `POST /api/performance/bonuses/:id/lock` — server.mjs:1456
- `GET /api/admin/system-config/bonus` — 缺失（🔴 新增）

## 5. 闭环剧本

| 环节 | 内容 |
|---|---|
| ① 发起 | 实际上市日期后双PM登记窗口内回款、退款和证据；页面显示六个月窗口。 |
| ② 处理 | 服务端计算窗口内净回款、达成率、阶梯系数和奖金池；退款只冲减窗口内净额（BR-INC-14）。 |
| ③ 审核 | 双组长复核回款台账，超级管理员锁定；贡献度和项目绩效必须同时锁定后才能生成个人奖金。 |
| ④ 结果 | 生成市场PM和研发PM两笔草稿，按贡献比例、绩效系数、负反馈系数计算金额。 |
| ⑤ 记录 | 写入 `receipt / create`、`receipt_settlement / generate|decide|lock`、`bonus_pool / generate|lock` 审计；审计：entityType=receipt, action=create / update；entityType=receipt_settlement, action=generate / decide / lock；entityType=bonus_pool, action=generate / preview / confirm / lock。 |
| ⑥ 归档 | 两笔奖金锁定后不可修改；周期外退款和锁定后结果保持历史不变。 |

**完整公式（v3 BR-INC-04~08）：**

```text
奖金池 = 立项时上市 6 个月目标销售额
       × bonus.poolRate（默认 0.05）
       × 项目差异化系数（`bonus.coefficient.{S,A,B}`）

达成率 = 六个月窗口内净实际回款 ÷ 目标回款 × 100%

阶梯系数（6 档区间制，按阈值从高到低逐档匹配，禁 `===` 与浮点容差） = {
  0     ，达成率 < 50%
  0.3   ，50% ≤ 达成率 < 70%
  0.6   ，70% ≤ 达成率 < 85%
  0.8   ，85% ≤ 达成率 < 100%
  1.0   ，100% ≤ 达成率 ≤ 120%
  1.2   ，达成率 > 120%
}

绩效系数（5 档映射） = {
  1.0   ，综合得分 ≥ 95
  0.8   ，85 ≤ 综合得分 < 95
  0.6   ，70 ≤ 综合得分 < 85
  0.3   ，60 ≤ 综合得分 < 70
  0     ，综合得分 < 60
}

每人成功奖金 =
  奖金池
  × 阶梯系数
  × 贡献度比例（市场 40-65% / 研发 35-60%，和恒 100%）
  × 绩效系数
  × 负反馈系数（0 / 0.5 / 1，锁定取最低）
```

**算例 A 手工核算（与 v3 P3 验收一致）：**

```text
输入：
目标销售额 = 5,000,000 元
项目等级 = S
差异化系数 = 1.5
六个月净回款 = 4,500,000 元
达成率 = 4,500,000 ÷ 5,000,000 × 100% = 90%
阶梯系数 = 0.8（85% ≤ 90% < 100%）
市场贡献度 = 55%
研发贡献度 = 45%
市场项目分 = 85
研发项目分 = 85
绩效系数 = 0.8（85 ≤ 85 < 95）
无负反馈，负反馈系数 = 1

奖金池 = 5,000,000 × 5% × 1.5 = 375,000 元

市场PM奖金 = 375,000 × 0.8 × 55% × 0.8 × 1 = 132,000 元
研发PM奖金 = 375,000 × 0.8 × 45% × 0.8 × 1 = 108,000 元
```

**算例 B 手工核算（验证 1.2 档）：**

```text
输入：达成率 = 130% → 阶梯系数 = 1.2；项目分 ≥ 95 → 绩效系数 = 1.0

市场PM奖金 = 375,000 × 1.2 × 55% × 1.0 × 1 = 247,500 元（v3 P3 验收 24.75 万）
研发PM奖金 = 375,000 × 1.2 × 45% × 1.0 × 1 = 202,500 元
```

**算例 C 手工核算（验证 70% 档）：**

```text
输入：目标 500 万、窗口内回款 350 万 → 达成率 = 70% → 阶梯系数 = 0.6

奖金池 = 375,000 元
市场PM奖金 = 375,000 × 0.6 × 55% × 0.8 × 1 = 99,000 元
研发PM奖金 = 375,000 × 0.6 × 45% × 0.8 × 1 = 81,000 元
```

**算例 D 手工核算（验证 120% 端点，禁等值判定）：**

```text
输入 1：达成率 = 120% → 阶梯系数 = 1.0（100% ≤ 120% ≤ 120%）
  市场PM奖金 = 375,000 × 1.0 × 55% × 1.0 × 1 = 206,250 元（v3 P3 验收 20.625 万）

输入 2：达成率 = 120.1% → 阶梯系数 = 1.2（> 120%）
  市场PM奖金 = 375,000 × 1.2 × 55% × 1.0 × 1 = 247,500 元（v3 P3 验收 24.75 万）

输入 3：达成率 = 99.99% → 阶梯系数 = 0.8（85% ≤ 99.99% < 100%）
  校验：1.0 档下界为 100%，不含 99.99%（验证禁止 `===` 与浮点容差）
```

## 6. 现状 vs v3 差距（量化）

- **缺失字段（4 个）**：`ProjectMember.lockedLevel`（v3 BR-INC-02 按接手时评级快照）、`ProjectMember.lockedAmount`（v3 BR-INC-02 锁定津贴额度）、`bonusEligible`（v3 BR-INC-12 退出标志）、`calculationTrace`（v3 计算输入/步骤/结果完整快照）。
- **缺失接口（3 个）**：`POST /api/performance/bonus-calculate/preview`、`POST /api/performance/bonus-calculate/confirm`、`GET /api/admin/system-config/bonus`。
- **缺失状态机环节（3 个）**：v3 BR-INC-04 阶梯区间匹配必须按阈值从高到低逐档匹配（当前 `salesLadderCoefficient` 已使用排序数组但缺少显式区间边界提示）；v3 BR-INC-07 绩效系数必须由项目锁定分映射（当前 `performanceCoefficient(score, rules)` 已实现映射但前端未展示项目分来源）；v3 BR-INC-08 必须成对锁定（当前 server 已实现成对锁定但需前端显示）。
- **缺失前端算例展示**：当前 `PerformanceV31Panel` 未提供 A/B/C/D 四个算例的可视化验证。
- **验收用例覆盖度**：v3 验收清单 AC-INC-17a~17h 共 8 条 + AC-INC-29 算例，当前覆盖 3 条，缺失 5 条 + 4 个算例。

## 7. 验收用例

**用例 1：算例A端到端核算**
```text
Given 目标500万、S=1.5、净回款450万、项目分85、贡献55/45
When 执行奖金计算预览
Then 奖金池为 375000 元
And 阶梯系数 = 0.8
And 绩效系数 = 0.8
And 市场奖金 = 132000 元、研发奖金 = 108000 元
And calculationTrace 逐项保存输入和结果
```

**用例 2：六档阶梯边界**
```text
Given 达成率分别为 99.99%、100%、120%、120.1%
When 执行阶梯计算
Then 阶梯系数分别为 0.8、1.0、1.0、1.2
And 不使用 equalityTolerance 或浮点等值判断
And 99.99% 不会被误判为 1.0
```

**用例 3：负反馈系数**
```text
Given 某负反馈已锁定且主责为市场PM、连带为研发PM
When 生成奖金
Then negative feedback factor 取两笔记录中最低值
And 市场PM奖金按 0 处理或按规则降低
And 研发PM按连带规则减半
And 结果保留负反馈引用 ID
```

**用例 4：窗口外退款**
```text
Given 退款发生在窗口结束后
When 生成六个月净回款
Then 该退款只留台账记录
And net_receipts 不被回溯扣减
And 已锁定 奖金结果不变
```

**用例 5：成对锁定**
```text
Given 市场或研发奖金仅有一笔草稿
When 超级管理员执行锁定
Then 返回 50002 节点已处理
And 两笔必须同时生成后成对锁定
And 锁定记录包含规则版本、计算版本和完整参数
```

---

# 35. 贡献度评定

## 1. 页面元信息

- 编号：35
- 名称：贡献度评定
- URL 路径：`/performance?tab=contribution`
- 页面性质：上市后 90 天贡献度五维度评定
- 主用角色：市场PM、研发PM、市场产品组长、研发产品组长
- 可见角色：双PM项目相关人员
- 优先级：P3
- 关联规则：BR-INC-09（五维度贡献、市场 40-65% / 研发 35-60%、和恒 100%）、BR-KPI-08、BR-GATE-05（90天复盘）、BR-AUD-01
- 现有实现：部分（`PerformancePage` App.jsx:415-430 contribution tab 已实现 `marketPct/rdPct/dimensions` 输入；server.mjs:1389 `PUT /api/performance/contributions/:projectId`、server.mjs:1405 `confirm`；缺失五维度权重固定（25/25/20/20/10）、五维度各自证据字段、四方确认状态机、超级管理员锁定）

## 2. 页面布局

- 页面标题"上市90天贡献度"。
- 副标题"双方提案 · 双组长确认"。
- 顶部显示实际上市日期、90天复盘截止日。
- 页面显示市场PM和研发PM比例输入。
- 市场比例范围 40–65%（`bonus.contribution.marketRange`）。
- 研发比例范围 35–60%（`bonus.contribution.rdRange`）。
- 研发比例由 100% 减市场比例自动计算（前端联动校验）。
- 页面固定显示五维度：立项主导、差异化创新、上市节奏、市场结果、协同领导力。
- 五维度显示各自权重：25%、25%、20%、20%、10%。
- 每个维度显示评分输入和证据说明。
- 评分输入范围 0–100。
- 页面显示市场PM和研发PM各自的维度依据。
- 市场侧依据偏市场价值、客户和 GTM。
- 研发侧依据偏技术、交付、质量和稳定性。
- 页面显示总分和五维权重加权结果。
- 页面显示当前提交状态。
- 提交方案按钮由双PM共同操作。
- 页面显示各自产品组长的确认按钮。
- 当前责任人可以暂存未确认方案。
- 任一方确认后显示确认人和时间。
- 全部确认后显示 `locked`。
- 被驳回时显示意见并允许修改后重提。
- 页面不提供修改历史确认。
- 页面不提供直接按职位等分。
- 页面不提供超出范围比例。
- 页面不提供技术委员会角色。
- 页面显示贡献度结果对第 34 页奖金的影响说明。
- 页面不直接显示个人奖金金额。
- 页面建议显示计算版本和来源。

## 3. 字段模型

| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---:|---|---|---|---|
| `projectId` | string | 是 | UUID | 当前项目 | 无 | 双PM项目 |
| `launchDate` | date | 是 | 上市日期 | 双PM配对 | 无 | 90天计算锚点 |
| `reviewDeadline` | datetime | 是 | 上市后90日 | 系统计算 | 无 | 逾期提醒 |
| `marketPct` | number | 是 | 40–65 | 双PM表单 | 55 | 与rdPct合计100 |
| `rdPct` | number | 是 | 35–60 | 服务端计算 | 45 | 范围校验 |
| `dimensions` | object | 是 | 五维对象 | 双PM表单 | 空 | 每个维度有依据 |
| `dimensions.initiation` | number | 是 | 0–100 | 评分表 | 0 | 权重25% |
| `dimensions.differentiation` | number | 是 | 0–100 | 评分表 | 0 | 权重25% |
| `dimensions.launch` | number | 是 | 0–100 | 评分表 | 0 | 权重20% |
| `dimensions.market` | number | 是 | 0–100 | 评分表 | 0 | 权重20% |
| `dimensions.collaboration` | number | 是 | 0–100 | 评分表 | 0 | 权重10% |
| `evidenceNote` | string | 否 | 评分依据 | 评分表 | 空 | 建议留证 |
| `status` | enum | 是 | `draft/submitted/locked/rejected` | `contribution_reviews` | `draft` | 状态机 |
| `marketPmConfirmed` | boolean | 是 | true/false | 确认表 | false | 市场PM确认 |
| `rdPmConfirmed` | boolean | 是 | true/false | 确认表 | false | 研发PM确认 |
| `marketLeadConfirmed` | boolean | 是 | true/false | 确认表 | false | 市场组长确认 |
| `rdLeadConfirmed` | boolean | 是 | true/false | 确认表 | false | 研发组长确认 |
| `dimensionEvidence` | array | 否 | 维度证据 | 新增证据表 | 空 | 支持附件 |
| `lockedAt` | datetime | 否 | ISO时间 | 服务端 | 空 | 锁定只读 |

## 4. API 接口清单

- `PUT /api/performance/contributions/:projectId` — server.mjs:1389
- `POST /api/performance/contributions/:projectId/confirm` — server.mjs:1405
- `GET /api/performance/overview?projectId=<projectId>` — server.mjs:1141
- `POST /api/performance/contributions/:projectId/decision` — 缺失（🔴 新增）
- `POST /api/performance/contribution-evidence` — 缺失（🔴 新增）
- `GET /api/admin/system-config/bonus` — 缺失（🔴 新增）

## 5. 闭环剧本

| 环节 | 内容 |
|---|---|
| ① 发起 | 项目上市后 90 天复盘窗口开始，双PM收到贡献度评定任务。 |
| ② 处理 | 双PM共同填写市场/研发比例、五维度评分和证据；服务端校验 40–65、35–60 及合计 100。 |
| ③ 审核 | 双PM和各自产品组长分别确认；任一意见不一致则记录驳回和整改依据。市场 40-65% / 研发 35-60% 和恒 100%；锁定后用于 34 奖金池核算（算例见附录 D.6.5） |
| ④ 结果 | 四方确认后贡献度方案锁定，市场/研发比例作为第 34 页奖金分配输入。 |
| ⑤ 记录 | 写入 `kpi_record / submit|approve|reject|lock` 审计；审计：entityType=kpi_record, action=submit / approve / reject / lock。 |
| ⑥ 归档 | 锁定后比例和五维评分不可覆盖；作为奖金池和成功奖金的永久结算依据。 |

## 6. 现状 vs v3 差距（量化）

- **缺失字段（5 个）**：`dimensionEvidence` 数组、`dimensionWeights`（五维固定权重 25/25/20/20/10）、`reviewDeadline`（90天计算锚点）、`lockedAt`、`calculationVersion`。
- **缺失接口（3 个）**：`POST /api/performance/contributions/:projectId/decision`（组长驳回）、`POST /api/performance/contribution-evidence`（维度证据上传）、`GET /api/admin/system-config/bonus`（参数键 `bonus.contribution.{marketRange, rdRange}`）。
- **缺失状态机环节（2 个）**：v3 BR-INC-09 四方确认状态机未实现（仅有 `confirm` 接口）；五维权重合计校验未实现。
- **缺失表（1 个）**：`contribution_dimension_evidence` 表（维度证据存储）。
- **验收用例覆盖度**：v3 验收清单 AC-INC-22~27 共 6 条，当前覆盖 3 条，缺失 3 条（维度证据、四方锁定、超级管理员复核）。

## 7. 验收用例

**用例 1：比例联动**
```text
Given 双PM在市场比例输入 55
When 表单重新渲染
Then 研发比例自动变为 45
And 市场比例不能超过 65 或低于 40
And 研发比例不能超过 60 或低于 35
```

**用例 2：五维度校验**
```text
Given 用户提交五维度评分但证据字段缺失或维度越界
When 调用 PUT /api/performance/contributions
Then 返回 10001 参数校验错误
And 不覆盖已有贡献度方案
And 错误明确指出维度键和范围
```

**用例 3：四方锁定**
```text
Given 市场PM、研发PM、市场组长、研发组长均已确认
When 第四个确认完成
Then 状态变为 locked
And contribution lock 时间写入
And 奖金计算使用 marketPct/rdPct
```

**用例 4：组长驳回**
```text
Given 产品组长对整体方案不认可
When 调用 decision=rejected 并填写意见
Then 状态回到 draft|rejected
And 方案提交人和维度责任人收到通知
And 不得进入奖金计算
```

**用例 5：历史追溯**
```text
Given 贡献度已锁定
When 用户查看奖金计算输入
Then 页面显示市场/研发比例、五维快照、锁定时间和规则版本
And 不显示"待确认"或临时方案
```

---

# 36. 负反馈执行

## 1. 页面元信息

- 编号：36
- 名称：负反馈执行
- URL 路径：`/performance?tab=feedback`
- 页面性质：四种触发情形、责任认定、整改和奖金/津贴影响
- 主用角色：产品组长、超级管理员
- 发起角色：当前项目可见用户
- 优先级：P3
- 关联规则：BR-INC-10（四种触发情形、主责停发、连带减半）、BR-INC-11（连续 2 个月无实质产出停发）、BR-INC-12（退出）
- 现有实现：部分（`PerformancePage` App.jsx:415-430 feedback tab 仅实现发起表单；server.mjs:1248 `POST /api/performance/feedback` 已实现四种 `feedbackType` 与责任映射（market_window_missed→双主责、requirement_rework→市场主责/研发连带、parameter_copy→研发主责/市场连带、quality_incident→研发主责/市场连带）；缺失负反馈详情抽屉、双组长确认、整改提交/复核、关闭、奖金系数 0/0.5/1 调整、津贴影响自动联动）

## 2. 页面布局

- 页面标题"负反馈执行"。
- 页面显示事件台账和发起区域。
- 发起表单包含事件类型、事实说明、证据编号或附件说明。
- 四种事件类型以中文名称显示。
- 页面显示每种类型的主责方和连带方。
- 事件台账显示项目、触发类型、责任、奖金系数、状态、创建时间。
- 产品组长和超级管理员看到当前待处理节点。
- 事件详情展开或打开 `NegativeFeedbackDrawer`。
- 详情显示双组长和超级管理员确认链。
- 详情显示津贴影响：主责停发、连带减半。
- 详情显示奖金影响：贡献度系数降低或取消分配资格。
- 详情显示整改说明、整改证据和复核状态。
- 页面不提供申请人直接确认。
- 页面不提供删除负反馈。
- 页面不提供修改责任映射。
- 页面显示市场窗口事件的双PM共同主责。
- 页面显示需求返工超标的市场PM主责、研发PM连带。
- 页面显示参数堆砌/对标抄袭的研发PM主责、市场PM连带。
- 页面显示质量事故的研发PM主责、市场PM连带。
- 页面显示"主责方"与"连带方"标签。
- 页面显示奖金系数可选值 0、0.5、1。
- 页面显示超级管理员锁定状态。
- 页面显示整改任务和关闭按钮。
- 页面不把负反馈作为独立绩效分数。
- 页面不直接修改已锁定奖金历史结果。
- 页面提供按类型、项目、状态筛选。
- 页面提供证据附件查看。
- 页面不提供前端硬编码责任关系；责任映射由服务端或配置返回。

## 3. 字段模型

| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---:|---|---|---|---|
| `id` | string | 是 | UUID | `negative_feedbacks` | 无 | 服务端生成 |
| `projectId` | string | 是 | UUID | 当前项目 | 无 | 项目可见 |
| `feedbackType` | enum | 是 | 四种类型 | 表单/API | 空 | 必填 |
| `description` | string | 是 | ≥6字符 | 表单 | 空 | 事实说明 |
| `evidenceNote` | string | 是 | ≥3字符 | 表单 | 空 | 证据编号/说明 |
| `evidence` | array | 否 | 附件 | 附件表 | 空 | 可追加 |
| `marketResponsibility` | enum | 是 | `primary/linked/none` | 服务端映射 | 空 | 由类型决定 |
| `rdResponsibility` | enum | 是 | `primary/linked/none` | 服务端映射 | 空 | 由类型决定 |
| `bonusFactor` | number | 是 | 0/0.5/1 | 双组长/超级管理员 | 1 | 复核时确定 |
| `marketLeadDecision` | enum | 是 | `pending/approved/rejected` | 确认表 | `pending` | 组长节点 |
| `rdLeadDecision` | enum | 是 | `pending/approved/rejected` | 确认表 | `pending` | 组长节点 |
| `adminDecision` | enum | 是 | `pending/approved/rejected` | 确认表 | `pending` | 超级管理员节点 |
| `status` | enum | 是 | `pending/lock/remediation_review/resolved/rejected` | 负反馈 | `pending` | 状态机 |
| `resolved` | boolean | 是 | true/false | 负反馈 | false | 关闭后只读 |
| `remediationNote` | string | 条件 | ≥10字符 | 双PM | 空 | 整改必填 |
| `remediationEvidence` | string | 条件 | ≥3字符 | 双PM | 空 | 整改必填 |
| `remediationSubmittedAt` | datetime | 否 | ISO时间 | 服务端 | 空 | 记录 |
| `marketLeadRemediationDecision` | enum | 是 | `pending/approved/rejected` | 整改确认 | `pending` | 组长复核 |
| `rdLeadRemediationDecision` | enum | 是 | `pending/approved/rejected` | 整改确认 | `pending` | 组长复核 |
| `closedBy` | string | 否 | UUID | 超级管理员 | 空 | 只读 |
| `closedAt` | datetime | 否 | ISO时间 | 服务端 | 空 | 只读 |
| `allowanceImpact` | object | 只读 | 停发/减半/正常 | 绩效台账 | 空 | 关联人员 |
| `bonusImpact` | object | 只读 | 0/0.5/1 | 奖金结算 | 空 | 关联结算 |

## 4. API 接口清单

- `GET /api/performance/feedback?projectId=<projectId>` — 缺失（🔴 新增）
- `POST /api/performance/feedback` — server.mjs:1248
- `POST /api/performance/feedback/:id/decision` — server.mjs:1263
- `POST /api/performance/feedback/:id/remediation` — server.mjs:1288
- `POST /api/performance/feedback/:id/remediation-review` — server.mjs:1303
- `POST /api/performance/feedback/:id/close` — server.mjs:1317
- `GET /api/performance/feedback/:id/attachments` — 缺失（🔴 新增）
- `GET /api/performance/feedback/:id` — 缺失（🔴 新增）
- `POST /api/performance/feedback/:id/attachments` — 缺失（🔴 新增）
- `GET /api/admin/system-config/performance/feedback` — 缺失（🔴 新增）

## 5. 闭环剧本

| 环节 | 内容 |
|---|---|
| ① 发起 | 任何项目可见用户选择四种触发类型之一，填写事实说明和证据，发起负反馈。 |
| ② 处理 | 服务端按类型写入市场PM和研发PM主责/连带关系，初始奖金系数为 1，进入双组长确认。 |
| ③ 审核 | 市场、研发产品组长分别确认，超级管理员在双组长后锁定；可选择奖金系数 0、0.5 或 1 并填写意见。 |
| ④ 结果 | 锁定后主责方津贴停发、连带方津贴减半；奖金计算使用负反馈系数和责任结果。 |
| ⑤ 记录 | 写入 `negative_feedback / create|approve|reject|lock|remediation|remediation_reviewed|closed` 审计；审计：entityType=negative_feedback, action=create / approve / reject / lock / remediation / remediation_reviewed / closed。 |
| ⑥ 归档 | 负反馈及结果永久保留；已锁定的奖金系数和津贴影响不可被后续操作覆盖。 |

**v3 BR-INC-10 四种触发映射：**

| 触发情形 | 市场PM责任 | 研发PM责任 | 规则动作 |
|---|---|---|---|
| `market_window_missed` | primary | primary | 双PM共同主责；主责方津贴停发，奖金系数按锁定结果 |
| `requirement_rework` | primary | linked | 市场PM主责，研发PM连带；主责停发、连带减半 |
| `parameter_copy` | linked | primary | 研发PM主责，市场PM连带；主责停发、连带减半 |
| `quality_incident` | linked | primary | 研发PM主责，市场PM连带；主责停发、连带减半 |

## 6. 现状 vs v3 差距（量化）

- **缺失字段（10 个）**：`bonusFactor`（默认 1，需在 `decision` 接口赋值）、`marketLeadRemediationDecision`、`rdLeadRemediationDecision`、`remediationNote`、`remediationEvidence`、`remediationSubmittedAt`、`closedBy`、`closedAt`、`allowanceImpact`、`bonusImpact`（部分已在 db.mjs 表结构中存在但前端未读取展示）。
- **缺失接口（4 个）**：`GET /api/performance/feedback?projectId`、`GET /api/performance/feedback/:id`、`GET /api/performance/feedback/:id/attachments`、`POST /api/performance/feedback/:id/attachments`、`GET /api/admin/system-config/performance/feedback`。
- **缺失状态机环节（2 个）**：v3 BR-INC-10 整改提交/复核/关闭三阶段状态机当前仅服务端子流程存在，前端无详情；v3 BR-INC-11 连续两个月无实质产出停发联动未实现。
- **缺失责任映射显式化**：当前服务端在 `create` 时硬编码映射（server.mjs:1254），缺少配置表 `performance_feedback_type_config`。
- **验收用例覆盖度**：v3 验收清单 AC-INC-30~35 共 6 条，当前覆盖 2 条（创建、初始责任映射），缺失 4 条（双组长确认、整改、关闭、津贴/奖金影响）。

## 7. 验收用例

**用例 1：错过市场窗口**
```text
Given 用户选择 market_window_missed 并提交事实与证据
When 服务端创建负反馈
Then marketResponsibility=primary
And rdResponsibility=primary
And 双PM共同主责，不得被误判为连带
And 初始状态为 pending 并等待双组确认
```

**用例 2：需求返工超标**
```text
Given 用户选择 requirement_rework
When 系统写入责任映射
Then 市场PM=primary
And 研发PM=linked
And 津贴显示市场停发、研发减半
And 奖金系数由后续确认写入
```

**用例 3：参数堆砌或对标抄袭**
```text
Given 用户选择 parameter_copy
When 系统写入责任映射
Then 研发PM=primary
And 市场PM=linked
And 页面显示研发主责、市场连带
And 不允许申请人修改责任标签
```

**用例 4：质量事故**
```text
Given 用户选择 quality_incident 且提交证据
When 双组长和超级管理员依次确认
Then 记录状态最终为 lock
And 研发主责方津贴停发、市场连带方连降
And 奖金计算引用该负反馈的 bonus_factor
```

**用例 5：整改关闭**
```text
Given 负反馈已 lock 且 resolved=0
When 双PM提交整改说明和证据
Then 状态进入 remediation_review
And 两位组长均 approved 后超级管理员才能 close
And close 操作返回最终 bonusFactor 并写入审计（entityType=negative_feedback, action=closed）
```

---

# 37. 项目详情-激励台账

## 1. 页面元信息

- 编号：37
- 名称：项目详情-激励台账
- URL 路径：建议 `/projects/:projectId/incentives`；当前可由 `/performance?tab=overview` 和 `/performance?tab=allowance`、`/performance?tab=contribution` 组合访问
- 页面性质：项目级KPI、津贴、奖金、贡献度和发放记录汇总
- 主用角色：市场PM、研发PM
- 可见角色：所有项目可见用户
- 优先级：P3
- 关联规则：BR-INC-15（台账定位，不是正式工资单）、BR-KPI-08、BR-INC-04~09、BR-AUD-01
- 现有实现：部分（`PerformancePage` App.jsx:415-430 overview + allowance + contribution tab 已聚合；server.mjs:1141 `GET /api/performance/overview` 已聚合 assignment/kpis/scores/feedbacks/allowances/contribution/bonuses；缺失项目详情内嵌的独立激励台账 URL、奖金池计算输入摘要、负反馈/奖金/贡献度的因果关系时间线、导出接口）

## 2. 页面布局

- 页面外层使用 `<PageFrame>`。
- 页面标题显示项目名称和"激励台账"。
- 顶部面包屑为项目空间 → 当前项目 → 激励台账。
- 顶部显示项目综合分、市场/研发双PM、规则版本。
- 页面显示"立项锁定基线"。
- 基线区显示目标销售额、目标渠道、目标NPS、目标场景、战略等级、上市日期。
- 基线字段全部只读。
- 基线修改需要通过正式变更流程，不由本页直接编辑。
- 页面显示KPI综合得分卡。
- KPI区显示功能KPI得分、共担KPI得分、缺项和复核状态。
- 页面提供进入第32页的链接。
- 页面显示津贴台账摘要。
- 津贴区显示周期、市场PM、研发PM、基础额、封顶额、状态。
- 页面提供进入第33页的链接。
- 页面显示贡献度区。
- 贡献度区显示市场/研发比例、五维结果和锁定状态。
- 页面提供进入第35页的链接。
- 页面显示奖金区。
- 奖金区显示目标销售额、净回款、达成率、奖金池、梯度和个人金额。
- 页面提供进入第34页的链接。
- 页面显示负反馈摘要。
- 负反馈区显示事件类型、主责/连带、奖金系数和状态。
- 页面提供进入第36页的链接。
- 页面显示项目结算状态。
- 状态包括KPI未复核、津贴未锁定、贡献度未锁定、奖金未锁定、已结项。
- 页面显示"可结项/暂不可结项"摘要。
- 可结项条件复用 `GET /api/performance/closeout-readiness/:projectId`。
- 页面显示最后更新时间。
- 页面提供导出当前项目台账入口。
- 导出内容只包含当前项目和当前权限范围。
- 页面不提供直接编辑已锁定金额。
- 页面不提供直接修改目标销售额。
- 页面不提供删除台账。
- 页面不提供修改KPI历史。
- 页面建议提供按时间线排序的结算事件。
- 时间线显示提交、复核、锁定、停发和奖金生成事件。

## 3. 字段模型

| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---:|---|---|---|---|
| `projectId` | string | 是 | UUID | URL/boot | 无 | 必须属于可见范围 |
| `projectName` | string | 是 | 项目名称 | `projects` | 无 | 只读 |
| `projectCode` | string | 是 | 项目编号 | `projects` | 无 | 只读 |
| `assignment` | object | 是 | 双PM配对 | `dual_pm_assignments` | 无 | 页面启用条件 |
| `ruleVersion` | string | 是 | 绩效规则版本 | 配对表 | 无 | 锁定版本 |
| `strategicLevel` | enum | 是 | `S/A/B` | 配对表 | 无 | 变更需审批 |
| `targetSales` | number | 是 | 元 | 配对表 | 0 | 大于0 |
| `targetChannels` | number | 是 | 数量 | 配对表 | 0 | 立项基线 |
| `targetNps` | number | 是 | 分值 | 配对表 | 0 | 立项基线 |
| `targetScenarios` | number | 是 | 数量 | 配对表 | 0 | 立项基线 |
| `actualLaunchDate` | date | 否 | 上市日期 | 配对表 | 空 | 后置指标锚点 |
| `scores` | object | 是 | 市场/研发综合分 | `projectScore` | 0 | 60/40或当前规则 |
| `allowances` | array | 否 | 津贴快照 | `allowance_settlements` | 空 | 按周期倒序 |
| `contribution` | object | 否 | 贡献度记录 | `contribution_reviews` | 空 | 未锁定前可编辑 |
| `contribution.marketPct` | number | 条件 | 40–65 | 贡献度表 | 空 | 与rd合计100 |
| `contribution.rdPct` | number | 条件 | 35–60 | 贡献度表 | 空 | 与market合计100 |
| `contribution.status` | enum | 是 | `draft/submitted/locked` | 贡献度表 | `draft` | 状态机 |
| `bonuses` | array | 否 | | `bonus_settlements` | 空 | 必须两条 |
| `bonus.bonusPool` | number | 只读 | 元 | 奖金表 | 0 | 目标销售额×5%×系数 |
| `bonus.netReceipts` | number | 只读 | 元 | 回款核算 | 0 | 周期内回款减退款 |
| `bonus.achievementRate` | number | 只读 | % | 奖金表 | 0 | 非负 |
| `ladderCoefficient` | number | 只读 | 0–1.2 | 奖金表 | 0 | 按系统参数 `bonus.achievementTiers` 6 档区间匹配 |
| `bonus.calculatedAmount` | number | 只读 | 元 | 奖金表 | 0 | 两位小数 |
| `bonus.status` | enum | 是 | `draft/locked` | 奖金表 | `draft` | 成对锁定 |
| `feedbacks` | array | 否 | 负反馈记录 | `negative_feedbacks` | 空 | 只读 |
| `settlementStatus` | enum | 是 | `not_ready/ready/locked` | readiness计算 | `not_ready` | 结项门槛 |
| `isReadOnly` | boolean | 是 | true/false | 项目/产品状态 | false | 结项或退市后true |
| `updatedAt` | datetime | 是 | ISO时间 | 多表最新时间 | 空 | 只读 |

## 4. API 接口清单

- `GET /api/performance/overview?projectId=<projectId>` — server.mjs:1141
- `GET /api/performance/closeout-readiness/:projectId` — closure.mjs:409
- `GET /api/performance/receipts/:projectId` — final-rules.mjs:307
- `GET /api/performance/allowances?projectId=<projectId>&period=<YYYY-MM>` — 缺失（🔴 新增统一查询）
- `GET /api/performance/contributions/:projectId` — 缺失（🔴 新增详情查询）
- `GET /api/performance/bonuses/:projectId` — 缺失（🔴 新增统一查询）
- `GET /api/performance/feedback?projectId=<projectId>` — 缺失（🔴 新增查询）
- `GET /api/audit-logs?projectId=<projectId>&entityType=project` — server.mjs:2209
- `GET /api/projects/:id/incentives/export` — 缺失（🔴 新增）
- `GET /api/admin/system-config/performance` — 缺失（🔴 新增）

## 5. 闭环剧本

| 环节 | 内容 |
|---|---|
| ① 发起 | 用户从项目详情点击"激励台账"，或从绩效入口进入指定项目。 |
| ② 处理 | 页面汇总基线、KPI、津贴、贡献度、回款、奖金和负反馈，按项目依赖顺序展示。 |
| ③ 审核 | 各模块按原闭环复核：KPI由组长、贡献度由四方、津贴由组长/超级管理员、奖金由双组长/超级管理员。 |
| ④ 结果 | 页面显示当前可结项状态；若KPI、津贴、贡献度或奖金未锁定，结项准备度不通过。 |
| ⑤ 记录 | 台账读取不写审计；导出和任何参数展示配置写审计；审计：entityType=incentive_ledger_export, action=export；entityType=allowance_stop_order, action=create / decide。 |
| ⑥ 归档 | 项目结项或产品退市后，所有激励字段只读，导出和历史查看仍可用。 |

## 6. 现状 vs v3 差距（量化）

- **缺失字段（2 个）**：`settlementStatus`（结项准备度摘要字段）、`updatedAt`（聚合字段）。
- **缺失接口（5 个）**：`GET /api/performance/allowances` 统一查询、`GET /api/performance/contributions/:projectId` 详情查询、`GET /api/performance/bonuses/:projectId` 统一查询、`GET /api/performance/feedback?projectId` 查询、`GET /api/projects/:id/incentives/export` 导出接口。
- **缺失页面（1 个）**：`<ProjectIncentivesPage>` 独立组件；当前依赖 `/performance` 全局 tab。
- **缺失因果关系时间线**：v3 BR-INC-15 要求台账聚合与因果回溯，目前各 tab 独立，无统一时间线。
- 🔴 必须引用附录 D.6.5 4 个算例作为验收标准
- **验收用例覆盖度**：v3 验收清单 AC-INC-36~40 共 5 条台账要求，当前覆盖 1 条，缺失 4 条（项目聚合、奖金摘要、导出、跨模块追溯）。

## 7. 验收用例

**用例 1：项目级汇总**
```text
Given 项目A已启用双PM且存在部分KPI、津贴、贡献度、奖金数据
When 用户进入 /projects/A/incentives
Then 页面只显示项目A的数据
And 各模块状态与独立页面一致
And 不把项目B数据混入汇总
```

**用例 2：结项准备度**
```text
Given KPI未全部 reviewed、津贴未全部 locked 或奖金未成对 locked
When 打开结项准备度
Then readiness 显示 not_ready
And 页面列出具体缺项
And 不能调用项目结项接口
```

**用例 3：只读结项项目**
```text
Given 项目状态为 completed
When 用户打开激励台账
Then 所有金额、比例和状态只读
And 导出和历史查看仍可用
And 页面不显示编辑或重新生成按钮
```

**用例 4：跨模块追溯**
```text
Given 奖金已锁定
When 用户点击奖金金额或达成率
Then 可以看到回款核算、绩效系数、贡献度和规则版本
And 不要求用户分别打开多个全局tab
And 台账中保留各模块记录ID
```

**用例 5：审计导出**
```text
Given 用户具有项目相关审计导出权限
When 点击导出项目激励台账
Then 导出内容按项目和当前权限过滤
And 生成导出审计
And 不包含密码、Token、密钥或附件原文
```