# 38. 需求门户-游客提交

### 1. 页面元信息
- **编号 / URL**：38 / 登录页弹窗（可拆为 `/portal/submit`）
- **角色 / 优先级**：游客（未登录）/ **P4**
- **BR 关联**：BR-REQ-01（免登录）、**BR-REQ-02b（产品三情形）**、BR-REQ-02、**BR-REQ-03（8 位查询码）**、**BR-REQ-04（自动路由）**
- **现状**：部分（App.jsx:84-97 `GuestDemandModal` + 登录页底部 CTA）

### 2. 页面布局
- **沿用**：`<LoginPage>` → CTA「客户需求反馈」→ `<GuestDemandModal onClose>`
- **弹层 `.create-modal.guest-demand-modal`**：Head（图标+标题+关闭）→ Form（5 label：客户名称 / 反馈人 / 产品型号 / 功能需求 / 附件 + honeypot `website`）→ 错误区 → 操作按钮
- **关键元素**：产品型号字段联动 `.product-suggestions` 下拉（最多 8 项 + 底部固定「其他/未找到」按钮）；附件列表 `.guest-file-list`；成功态 `.guest-success`（CheckCircle + `<strong>{result.code}</strong>` + 附件上传成功计数）
- **v3 必须强化**：成功页需把 **8 位查询码以显著字号（≥28px）独立呈现** + 复制按钮 + 提示「请截图或抄写保存，系统不再展示此码」

### 3. 字段模型

| 字段 | 类型 | 必填 | 校验 |
|---|---|---|---|
| customerName | string | ✅ | minLength 2, maxLength 120 |
| feedbackPerson | string | ✅ | minLength 2, maxLength 80 |
| contact | string | — | v3 BR-REQ-02 联系方式 |
| productId | uuid\|null | — | 服务端校验存在 |
| rawModel | string | ✅ | 第 ③ 类可空 |
| functionalRequirement | string | ✅ | minLength 6, maxLength 4000 |
| website | string | — | honeypot 必为空 |
| files | File[] | — | ≤ 5 份，每份 ≤ 20MB |

### 4. API 接口清单（双轨）

| 现状 | v3 缺失 🔴 |
|---|---|
| `POST /api/public/demands` 缺查询码生成与展示 | 🔴 响应必须返回 `code`（**8 位大写字母+数字**，正则 `^[A-Z0-9]{8}$`） |
| `POST /api/public/demands/:id/attachments?token=` | 保留 |
| — | 🔴 **新增** `POST /api/public/demands/:code/supplement`（BR-REQ-03 受理前可补充） |
| — | 🔴 **新增** `POST /api/public/demands/:code/withdraw`（BR-REQ-03 24h 内可撤回） |
| — | 🔴 **新增** `GET /api/public/products` 必须返回 `status` 字段区分在售/在研/其他 |
| — | 🔴 服务端按 `productId` 自动解析该产品负责双 PM，写 `assignedMarketPmId/assignedRdPMId`（BR-REQ-04 自动路由） |
| — | 🔴 错误码 4xxxx：40010 查询码格式 / 40011 速率限制 / 40012 附件超限 / 40401 产品已下架 |

### 5. 闭环剧本（v3 BR-REQ-01/02b/03/04）

| 环节 | 内容 |
|---|---|
| ① 发起 | 游客在登录页底部点击「客户需求反馈」→ 弹出 GuestDemandModal；选产品三情形之一（v3 BR-REQ-02b） |
| ② 处理 | `POST /api/public/demands` → 服务端生成 `code = base36(randomBytes(5))`（v3 BR-REQ-03）+ `uploadToken`；按 productId 解析双 PM → 落库 `assignedMarketPmId/assignedRdPMId` |
| ③ 审核 | 无审批；产品 PM 在 40 页需求池可对 ③ 类需求认领（v3 BR-REQ-02b 第 ③ 条） |
| ④ 结果 | 成功页大字显示 `code` + 复制按钮；附件逐份上传并显示计数 |
| ⑤ 记录 | audit entityType=`guest_demand`, action=`submit`；IP hash + UA hash；「待指派」类需求额外 action=`unassigned_overdue` |
| ⑥ 归档 | 第 ③ 类需求 5 工作日（`req.guestOtherAssignDays=5`）未指派 → 提醒 product_lead 兜底（v3 第 ③ 条衍生） |

### 6. 验收用例

```
Given 游客在登录页点击「客户需求反馈」
When  选择在售产品「ZK-X100」、填客户名称/反馈人/功能需求、可选 3 份附件
Then  POST /api/public/demands 返回 code 为 8 位大写字母+数字（正则 ^[A-Z0-9]{8}$）
And   成功页 code 字号 ≥28px 并提供「复制」按钮
And   需求表 assigned_market_pm_id/assigned_rd_pm_id 自动写入该产品负责的双 PM
And   audit_logs 写入 action=submit, detail.ipHash, uaHash
```

```
Given 产品型号字段输入「ZZZ-999」并选「其他/未找到」
When  提交
Then  raw_model="ZZZ-999", product_id=NULL, status="SUBMITTED", assigned_pm_id=NULL 进入「待指派」池
And   5 工作日后仍为 SUBMITTED → 系统推送提醒到所属 product_lead
```

```
Given 同一 IP 1 小时内提交 11 次
When  第 11 次请求
Then  返回 40011 速率限制；前端提示「请稍后再试」
```

```
Given honeypot 字段填入了非空字符串
When  提交
Then  服务端 400 拒绝，写入 audit action=spam_rejected
```

### 7. 现状 vs v3 差距（量化）

| 维度 | 现状 | v3 要求 | 差距 |
|---|---|---|---|
| 字段 | customerName/feedbackPerson/productId/rawModel/functionalRequirement/website/files | v3 还要求 `contact`（BR-REQ-02 明确）、`assignedMarketPmId/assignedRdPMId`（BR-REQ-04） | 🔴 缺 3 字段 |
| 状态 | 无显式状态字段（隐含 new） | v3 BR-REQ-05：`SUBMITTED` 初始态 | 🔴 缺状态字段 |
| 查询码 | 已返回 `result.code` 但未明确 8 位规格 | v3 BR-REQ-03：**8 位** + base36 + 正则 `^[A-Z0-9]{8}$` | ⚠️ 已近满足，需补正则校验 + UI 强化 |
| 三情形 | 部分实现（在售 + 其他） | v3 BR-REQ-02b：在售 / 在研 / 其他 | 🔴 缺「在研产品」选择源（PM 登录后新增的产品） |
| 自动路由 | 现状无 | v3 BR-REQ-04 | 🔴 完全缺失 |
| 接口 | 2 个（创建 + 附件） | v3 应有 5 个（含查询、撤回、补充） | 🔴 缺 3 接口 |
| 验收用例 | 现状 3 条 | v3 应覆盖 8 条（含速率、撤回、补充） | 🔴 缺 5 条 |
| 错误码 | 现状无 4xxxx 编码 | v3 TS-09 业务码段 4xxxx | 🔴 完全缺失 |
| 待指派兜底 | 无 | v3 `req.guestOtherAssignDays=5` 衍生 | 🔴 完全缺失 |

---

# 39. 需求门户-查询进度

### 1. 页面元信息
- **编号 / URL**：39 / **应新增** `/portal/track`
- **角色 / 优先级**：游客（凭 8 位查询码）/ **P4**
- **BR 关联**：**BR-REQ-09（凭码查询）**、**BR-REQ-03（受理前可补充/撤回）**、BR-USER-09（8 位查询码）
- **现状**：**完全缺失**

### 2. 页面布局
- **应新增组件**：`<GuestTrackPage>`（不套 Shell，公开路由）
- **同构登录页**：`.login-page` + `.login-brand` + `.login-panel`
- **结构**：顶部 8 位输入框（mask `AAA-AA-AAA`）→ 状态徽章（SUBMITTED/ACCEPTED/EVALUATING/SCHEDULED/PROCESSING/CLOSED/ARCHIVED/WITHDRAWN）+ 时间线 + 附件清单（仅文件名/大小）→ 受理前操作区（24h 撤回按钮）
- **数据范围 banner**：「本系统仅展示您提交需求的处理进度，不公开内部负责人」

### 3. 字段模型

| 字段 | 类型 | 必填 | 校验 |
|---|---|---|---|
| code | string | ✅ | 路径参数 `^[A-Z0-9]{8}$` |
| supplementNote | string | — | minLength 6, maxLength 2000 |
| withdrawReason | string | — | minLength 6 |

### 4. API 接口清单

| 路径 | 方法 | 用途 |
|---|---|---|
| 🔴 `/api/public/demands/:code` | GET | 脱敏视图：code, status, customerName(末位脱敏), timeline, attachments, canSupplement, canWithdraw, withdrawDeadlineAt |
| 🔴 `/api/public/demands/:code/supplement` | POST | 补充说明（受理前可） |
| 🔴 `/api/public/demands/:code/withdraw` | POST | 24h 内撤回 |

**错误码**：404 码不存在 / 409 状态不允许操作 / 410 已撤回 / 40011 速率限制（每码每分钟 ≤6 次）

### 5. 闭环剧本（v3 BR-REQ-03/09）

| 环节 | 内容 |
|---|---|
| ① 发起 | 游客凭 38 页 8 位码访问 `/portal/track?code=…` |
| ② 处理 | `GET /api/public/demands/:code` 脱敏视图（屏蔽 owner_name 全文） |
| ③ 审核 | 无；变更由 40/41 页 PM 操作驱动 |
| ④ 结果 | 状态徽章 + 时间线 timeline（submitted→accepted→evaluating→scheduled→processing→closed→archived） |
| ⑤ 记录 | audit entityType=`guest_demand`, action=`track/supplement/withdraw` |
| ⑥ 归档 | WITHDRAWN 状态不再可查；6 个月后只读清理 |

### 6. 验收用例

```
Given 游客凭 AB12CD34 访问 /portal/track
Then  显示当前状态徽章 + 时间线；不暴露反馈人原文
And   audit action=track
```

```
Given status=SUBMITTED 且提交未超 24h
When  撤回并填理由
Then  status=WITHDRAWN；410 不再可查
And   audit action=withdraw
```

```
Given status=ACCEPTED
When  尝试补充
Then  返回 409「状态不允许操作」
```

### 7. 现状 vs v3 差距（量化）

| 维度 | 现状 | v3 要求 | 差距 |
|---|---|---|---|
| 页面 | 0 | 1 独立页 /portal/track | 🔴 缺 1 整页 |
| API | 0 | 3（查/补/撤） | 🔴 缺 3 |
| 字段 | 0 | 3 | 🔴 缺 3 |
| 状态枚举 | 0 | 8 态（含 WITHDRAWN） | 🔴 缺 8 |
| 24h 撤回窗口 | 无 | BR-REQ-03 必填 | 🔴 缺 |
| 脱敏规则 | 无 | 屏蔽 owner_name/电话末位 | 🔴 缺 |
| 错误码 4xxxx | 无 | v3 TS-09 | 🔴 缺 |
| AC | 0 条 | 应有 4 条 | 🔴 缺 4 |

---

# 40. 需求池

### 1. 页面元信息
- **编号 / URL**：40 / `/requirements` tab=product
- **角色 / 优先级**：`product_manager`（本产品）/ `product_lead`（本组）/ `super_admin`（全组织 + 待指派）/ **P1**
- **BR 关联**：**BR-PROD-01（产品三路来源）**、**BR-REQ-02b（产品三情形）**、**BR-REQ-04（自动路由）**、**BR-REQ-05（7 态）**、**BR-REQ-06（删除双层审核）**
- **现状**：部分（App.jsx:276-284 `ProductDemandsPanel`，6 态）
- **术语边界说明**（见 `_公共规范.md` 第八节「补充术语」）：本页管理的对象是 **`demand`**（游客/内部提报、挂在 *product* 上、尚未立项），接口前缀因此为 `/api/demands/*`；页面路由复用 `/requirements` 仅为导航分组，**不代表对象类型是 `requirement`**。立项后挂在 project 下的需求单见页 41（`/api/requirements/:id`）。

### 2. 页面布局
- **沿用**：`<PageFrame>` → `<section-switch>` → `<ProductDemandsPanel>`
- **4 项指标卡**：需求总量 / 待分析（SUBMITTED）/ 未匹配产品 / 已进入项目
- **`.business-toolbar`**：**v3 7 态**筛选 + 超级管理员产品下拉 + 「待指派」专属筛选
- **`.product-demand-list`**：来源徽章（GUEST/INTERNAL/LEGACY）+ 状态 pill + 行内操作
- **顶部新增**「待指派」卡片（第 ③ 类需求，5 工作日兜底）

### 3. 字段模型

| 字段 | 类型 | 必填 | 枚举/校验 |
|---|---|---|---|
| status | enum | ✅ | v3 BR-REQ-05：SUBMITTED/ACCEPTED/EVALUATING/SCHEDULED/PROCESSING/CLOSED/ARCHIVED |
| source | enum | ✅ | GUEST/INTERNAL/LEGACY |
| productId | uuid\|null | — | 第 ③ 类可空 |
| assignedMarketPmId / assignedRdPmId | uuid | — | 服务端按 productId 自动路由（BR-REQ-04） |
| priority | enum | — | P0/P1/P2 |
| linkedWorkItemId | uuid | — | PROCESSING 时必填 |
| projectId | uuid | — | PROCESSING 时必填 |

### 4. API 接口清单

| 现状 | v3 缺失 🔴 |
|---|---|
| `GET /api/demands?productId=` | 增 status/source/assignedMarketPmId 多参 |
| `POST /api/demands/:id/triage` | 状态机按 v3 7 态校验 |
| `POST /api/demands/:id/link-project` | 保留 |
| — | 🔴 `POST /api/demands/:id/accept`（SUBMITTED→ACCEPTED） |
| — | 🔴 `POST /api/demands/:id/evaluate` |
| — | 🔴 `POST /api/demands/:id/schedule` |
| — | 🔴 `POST /api/demands/:id/process` body=`{projectId,workItemId}` |
| — | 🔴 `POST /api/demands/:id/close` |
| — | 🔴 `POST /api/demands/:id/archive` |
| — | 🔴 `POST /api/demands/:id/claim`（超级管理员认领待指派） |
| — | 🔴 `GET /api/demands/unassigned`（待指派池） |

**错误码**：40010 查询码格式 / 40011 速率限制 / 40006 产品项目 1:1 违反 / 5xxxx 资源不存在

### 5. 闭环剧本（v3 BR-REQ-04/05/06）

| 环节 | 内容 |
|---|---|
| ① 发起 | `product_manager` 进入 tab=product；按 status/source/product 筛选 |
| ② 处理 | SUBMITTED 由 PM 7 天内 accept/reject；游客「其他」类入「待指派」 |
| ③ 审核 | EVALUATING→SCHEDULED 需评估证据；PROCESSING 必关联 workItem |
| ④ 结果 | 7 态流转写 audit action=`accept/evaluate/schedule/process/claim`；通知当事人 + 申请人 |
| ⑤ 记录 | audit entityType=`demand`（**非 `requirement`** — 本页对象未立项，术语边界见 `_公共规范.md` 第八节「补充术语」；游客来源的归档写入用 `guest_demand`），完整记录 7 态迁移 + 责任人 + 时间戳 |
| ⑥ 归档 | ARCHIVED 仅可查；删除走 BR-DEL-01 双层审核（`deletion.leaderDeadlineDays=2` + `deletion.adminDeadlineDays=2`） |

### 6. 验收用例

```
Given status=SUBMITTED 产品需求
When  PM 点击「受理」
Then  status=ACCEPTED；audit action=accept, from=SUBMITTED, to=ACCEPTED
And   第 ③ 类触发「待指派 → 已认领」流程
```

```
Given 游客「其他」需求 5 工作日（req.guestOtherAssignDays=5）仍 SUBMITTED
When  系统定时扫描
Then  推送提醒至所属 product_lead；audit action=unassigned_overdue
```

```
Given PROCESSING 需求要求删除
When  发起删除申请
Then  走 BR-DEL-01 双层审核（product_lead 2 工作日 + super_admin 2 工作日）
```

### 7. 现状 vs v3 差距（量化）

| 维度 | 现状 | v3 要求 | 差距 |
|---|---|---|---|
| 状态机 | 6 态 | 7 态 | 🔴 缺 1 态 |
| 三路来源 | 2 | 3 | 🔴 缺 1 |
| 自动路由 | 无 | BR-REQ-04 必填 | 🔴 完全缺失 |
| 待指派兜底 | 无 | 5 工作日 | 🔴 完全缺失 |
| API | 3 | 10 | 🔴 缺 7 |
| AC | 3 条 | 应有 8 条 | 🔴 缺 5 |
| 错误码 4xxxx | 无 | v3 TS-09 | 🔴 缺 |

---

# 41. 需求详情-处理

### 1. 页面元信息
- **编号 / URL**：41 / `/requirements` tab=project（建议升级 `/requirements/:id`）
- **角色 / 优先级**：项目负责人 / `super_admin` / **P1**
- **BR 关联**：**BR-REQ-05（7 态状态机）**、**BR-REQ-07（未关联动作禁止创建）**、BR-GATE-07（变更双签）、BR-DEL-01
- **现状**：部分（App.jsx:286-302 `ProjectRequirementsPanel` + `RequirementModal`）

### 2. 页面布局
- **沿用**：`<ProjectRequirementsPanel embedded>` + `<RequirementModal>`
- **v3 升级**：详情抽屉含 description/acceptanceCriteria 全文 + workItem 链接 + 状态迁移历史 + 7 态操作按钮
- **顶部 4 项指标卡**：全部 / EVALUATING / BASELINED / P0
- **行内操作**：按 v3 7 态转换按钮；删除走 BR-DEL-01 双层审核弹窗

### 3. 字段模型

| 字段 | 类型 | 必填 | 校验 |
|---|---|---|---|
| title | string | ✅ | minLength 4 |
| description | string | ✅ | minLength 8 |
| priority | enum | ✅ | P0/P1/P2 |
| acceptanceCriteria | string | ✅ | minLength 8 |
| stageCode | enum | ✅ | concept/plan/develop/verify/launch/lifecycle |
| workItemId | uuid | ✅ | 服务端校验同项目（v3 BR-REQ-07） |
| status | enum | — | v3 BR-REQ-05 7 态 |
| deletionRequestId | uuid | — | 删除审核关联 |

### 4. API 接口清单

| 现状 | v3 缺失 🔴 |
|---|---|
| `GET /api/requirements?projectId=` | status 单态过滤 |
| `POST /api/requirements?projectId=` | 保留 |
| `POST /api/requirements/:id/status` | 状态机 7 态校验 |
| — | 🔴 `GET /api/requirements/:id` 详情 |
| — | 🔴 `GET /api/requirements/:id/history` 状态迁移历史 |
| — | 🔴 `POST /api/requirements/:id/deletion-request`（触发 BR-DEL-01） |

### 5. 闭环剧本（v3 BR-REQ-05/07 + BR-GATE-07）

| 环节 | 内容 |
|---|---|
| ① 发起 | 项目负责人录入，**必关联 workItem**（BR-REQ-07） |
| ② 处理 | CANDIDATE → EVALUATING → BASELINED → 与项目 IPD 节点联动 |
| ③ 审核 | 评估/基线变更由 product_lead 确认；变更走 BR-GATE-07 五节点 |
| ④ 结果 | PROCESSING 同步通知双 PM；CLOSED 后禁编辑 |
| ⑤ 记录 | audit entityType=`requirement`, action=`update` |
| ⑥ 归档 | 删除走 BR-DEL-01 双层审核（2 + 2 工作日） |

### 6. 验收用例

```
Given 项目负责人选 stage=plan、workItem=P03
When  提交必填
Then  code=REQ-001, status=SUBMITTED；audit action=create
```

```
Given status=CLOSED
When  尝试修改
Then  返回 409「已闭环不可编辑」
```

### 7. 现状 vs v3 差距（量化）

| 维度 | 现状 | v3 要求 | 差距 |
|---|---|---|---|
| 状态机 | 4 态 | 7 态 | 🔴 缺 3 态 |
| 详情页 | 行内 5 列 | 独立抽屉 | 🔴 缺 |
| 历史表 | 无 | 必存 | 🔴 缺 |
| workItem 全程 | 录入时 | BR-REQ-07 必填 | ⚠️ 部分 |
| AC | 3 条 | 应有 5 条 | 🔴 缺 2 |

---

# 42. AI 文档助手

### 1. 页面元信息
- **编号 / URL**：42 / 双入口：① `<AiModal>`（App.jsx:225-239）② `/admin/ai-config`（**应新增**）
- **角色 / 优先级**：`product_manager`/`product_lead`（运行）/ `super_admin`（配置）/ **P4**
- **BR 关联**：**BR-AI-01（模型配置）**、BR-AI-02、BR-AI-03、BR-AI-04、BR-AI-05（调用审计）、BR-AI-06
- **现状**：部分（运行态完成；管理态缺失）

### 2. 页面布局
- **运行态 AiModal**：Head + 隐私 banner + Skill binding + 上下文源多选 + 生成按钮 + 结果区（provider/fallback 徽章 + 三按钮）
- **管理态 `/admin/ai-config`（应新增）**：Provider 列表 + 同步日志 + 默认切换

### 3. 字段模型

**运行态**：

| 字段 | 类型 | 必填 | 校验 |
|---|---|---|---|
| action | enum | ✅ | AI生成初稿/AI完善内容/AI检查质量/SOP补漏检查 |
| selectedSources | uuid[] | ✅ | ≥1 |
| workItemId | uuid | ✅ | — |

**管理态（应新增 12 字段）**：

| 字段 | 类型 | 必填 | 校验 |
|---|---|---|---|
| name | string | ✅ | minLength 2 |
| providerType | enum | ✅ | openai/anthropic/azure/qwen-local |
| endpoint | url | ✅ | url |
| apiKey | string | ✅ | minLength 20；AES 加密 |
| model | string | ✅ | — |
| temperature | number | — | 0–2 |
| maxTokens | number | — | ≥ 1 |
| timeoutMs | number | — | ≥ 1000 |
| retryTimes | number | — | 0–3 |
| monthlyTokenBudget | number | — | ≥ 0 |
| rpmLimit | number | — | ≥ 1 |
| enabled / isDefault | bool | — | 默认 true / 唯一 |

### 4. API 接口清单

| 现状 | v3 缺失 🔴 |
|---|---|
| `GET /api/ai/context-preview` | 保留 |
| `POST /api/ai/run` | **必须记录 token 消耗到 audit**（BR-AI-05） |
| `POST /api/ai/outputs/:id/decision` | 保留 |
| — | 🔴 `GET/POST /api/admin/ai-providers[/:id]` |
| — | 🔴 `POST /api/admin/ai-providers/:id/rotate-key` |
| — | 🔴 `POST /api/admin/ai-providers/:id/set-default` |
| — | 🔴 `POST /api/admin/ai-providers/:id/sync` |
| — | 🔴 `GET /api/admin/ai-call-logs` |

### 5. 闭环剧本（v3 BR-AI-01/02/03/05）

| 环节 | 内容 |
|---|---|
| ① 发起 | 工作动线点 AI / `super_admin` 在 /admin/ai-config |
| ② 处理 | 服务端按 workItem.sop 绑 Skill；v1 落 `ai_document.version_chain` |
| ③ 审核 | PM 人工审核；通过后入正式交付物 |
| ④ 结果 | 拒绝/采纳/替换 三态；token 写 audit |
| ⑤ 记录 | `ai_document` + `ai_call_logs` + audit entityType=`ai_document`, action=`generate` |
| ⑥ 归档 | 历史版本不可删；轮换密钥立即生效 |

### 6. 验收用例

```
Given Provider=本地通义千问、isDefault=true
When  PM 触发 AI
Then  /api/ai/run 调本地模型 mode=provider；audit 含 tokenUsage, latencyMs
```

```
Given 月 token 9000/预算 8000
When  下一次调用
Then  返回 4xxxx「月度 token 超限」+ 自动 fallback 模式
```

```
Given 用户对 AI 输出点「拒绝并归档」
Then  POST decision body=rejected；workItem.payload 不变
And audit action=delete
```

### 7. 现状 vs v3 差距（量化）

| 维度 | 现状 | v3 要求 | 差距 |
|---|---|---|---|
| Provider 管理页 | 缺 | /admin/ai-config | 🔴 缺 1 整页 |
| 模型字段 | 5 | 12 | 🔴 缺 7 |
| Token 预算 | 仅数值 | 必填 + fallback | ⚠️ 部分 |
| 调用审计 token | 缺 | 必填 | 🔴 缺 |
| 错误码 4xxxx | 缺 | v3 TS-09 | 🔴 缺 |
| AC | 2 条 | 应有 6 条 | 🔴 缺 4 |

---

# 43. 删除审核-归档区

### 1. 页面元信息
- **编号 / URL**：43 / `/admin/deletion-archive`（**应新增**独立子路由）
- **角色 / 优先级**：`super_admin` / **P4**
- **BR 关联**：**BR-DEL-03（逻辑删除+归档）**、BR-DEL-04、BR-DEL-05、BR-DEL-06
- **现状**：部分（GovernancePage 行内；独立归档页缺失）

### 2. 页面布局
- **应新增**：`<DeletionArchivePage>`
- **结构**：橙色告警 banner + 多参筛选 + 列表 + 「彻底清除」按钮（仅 `DELETED && clearedAt IS NULL`）+ 二次确认弹窗（输入 targetId 末 6 位 + clearedReason ≥6 字）

### 3. 字段模型

| 字段 | 类型 | 必填 | 校验 |
|---|---|---|---|
| targetType | enum | ✅ | project/requirement/attachment/product/recruitment |
| targetId | uuid | ✅ | — |
| reason | string | ✅ | minLength 6 |
| status | enum | — | DRAFT/LEADER_REVIEW/ADMIN_REVIEW/DELETED/REJECTED |
| confirmTail | string | ✅（清除时） | = targetId 末 6 位 |
| retentionDays | number | — | 默认 30 |
| clearedReason | string | ✅（清除时） | minLength 6 |

### 4. API 接口清单

| 现状 | v3 缺失 🔴 |
|---|---|
| `GET /api/deletion-archive` | 多参过滤 status/targetType/from/to |
| `POST /api/deletion-archive/:id/clear` body=`{confirmTail,clearedReason}` | 保留 |
| `POST /api/deletion-archive/:id/restore` | 保留 |
| — | 🔴 `GET /api/deletion-archive/:id/hash-chain` |
| — | 🔴 清除动作本身再写一条 audit（v3 BR-DEL-03 显式） |

### 5. 闭环剧本（v3 BR-DEL-03/04）

| 环节 | 内容 |
|---|---|
| ① 发起 | `POST /api/deletion-requests` → DRAFT |
| ② 处理 | `product_lead` 初审 2 工作日（deletion.leaderDeadlineDays=2）→ LEADER_REVIEW |
| ③ 审核 | `super_admin` 终审 2 工作日（deletion.adminDeadlineDays=2）→ ADMIN_REVIEW |
| ④ 结果 | 通过后 status=DELETED；recoverable_until=now+30 天 |
| ⑤ 记录 | 三层审计：deletion_request / archive / hash_chain |
| ⑥ 归档 | 保留期可恢复；`super_admin`「彻底清除」→ 二次确认 + 写 audit entityType=`deletion_request`, action=`delete` |

### 6. 验收用例

```
Given 已 DELETED 且过 31 天
When  super_admin 输入 targetId 末 6 位 + clearedReason
Then  POST clear 返回 200，hashChainId 非空
And   audit action=delete 含 hashChainId
```

```
Given 保留期未满
When  点「恢复」
Then  status 恢复为原状态；audit action=restore
```

### 7. 现状 vs v3 差距（量化）

| 维度 | 现状 | v3 要求 | 差距 |
|---|---|---|---|
| 独立归档页 | 缺（仅行内） | /admin/deletion-archive | 🔴 缺 1 整页 |
| 二次确认 | prompt 密码 | targetId 末 6 位 | 🔴 缺 |
| Hash 链 | 无 | v3 TS-08 | 🔴 完全缺失 |
| 清除再审计 | 部分 | v3 显式 | ⚠️ 强化 |
| AC | 0 条独立 | 应有 4 条 | 🔴 缺 4 |

---

# 44. 人员管理

### 1. 页面元信息
- **编号 / URL**：44 / `/identity-sync`
- **角色 / 优先级**：`super_admin` / **P4**
- **BR 关联**：**BR-USER-01（来源第三方 API）**、BR-USER-02（`hr.syncCron=0 2 1 * *`）、BR-USER-06（**7 步闭环**）、**BR-USER-07（`hr.levelSource=API_ONLY`）**、BR-USER-08（`hr.allowCrossRole=false`）
- **现状**：完整（App.jsx:132 `<IdentitySyncPage>`）

### 2. 页面布局
- **组件**：`<IdentitySyncPage>`
- **结构**：4 项指标卡 + 三栏（同步配置 / 成员列表含 `accountStatus`/`levelSource` / 邀请列表）+ 行操作（同步 / L1-L5 比对 / 离职 7 步流程）

### 3. 字段模型

| 字段 | 类型 | 必填 | 校验 |
|---|---|---|---|
| syncEndpoint | url | ✅ | url |
| syncPeriod | enum | — | hourly/daily/manual |
| resignUserId | uuid | ✅（离职） | 必须在岗 |
| successorId | uuid | ✅（离职） | 同组织在岗 |
| handoverDeadline | date | — | 默认 +15 日（handover.resignDeadlineDays） |
| accountStatus | enum | — | ACTIVE/FROZEN_PENDING_HANDOVER/DISABLED/RESIGNED |
| levelSource | enum | — | API_ONLY（v3 BR-USER-07，超级管理员不可改） |

### 4. API 接口清单

| 现状 | v3 缺失 🔴 |
|---|---|
| `GET /api/admin/members` | 保留 |
| `GET /api/admin/sync-config` | 含 cron=`0 2 1 * *` |
| `POST /api/admin/sync-config/run-now` | 保留 |
| `POST /api/admin/members/:id/resign` | **触发 v3 BR-USER-06 完整 7 步** |
| — | 🔴 `GET /api/admin/sync-records` |
| — | 🔴 `POST /api/admin/members/:id/freeze-handover` |
| — | 🔴 `POST /api/admin/handover-batch` |
| — | 🔴 `POST /api/admin/members/:id/confirm-handover`（→ DISABLED） |

### 5. 闭环剧本（v3 BR-USER-06 7 步）

| 环节 | 内容 |
|---|---|
| ① 触发 | API 同步识别离职 → accountStatus=`FROZEN_PENDING_HANDOVER`（仅保留移交权限） |
| ② 冻结 | 仅保留移交权限；不发津贴 |
| ③ 任务 | 自动生成「待移交任务」→ 推送本人 + 双方 product_lead + super_admin |
| ④ 移交 | 本人或 product_lead/super_admin 代操作；单项目/批量 |
| ⑤ 禁用 | 全部项目移交完 → accountStatus=`DISABLED`；解绑企微 |
| ⑥ 升级 | 15 日（handover.resignDeadlineDays）未完成 → 每日提醒 super_admin |
| ⑦ 记录 | audit entityType=`user`, action=`freeze`；月初 PM 领当月全额津贴（v3 BR-INC-11） |

### 6. 验收用例

```
Given PM 离职触发冻结
Then  accountStatus=FROZEN_PENDING_HANDOVER；保留移交权限
And   待移交任务推送给双方 product_lead + super_admin
```

```
Given 该 PM 名下 3 个项目
When  一键批量移交完成
Then  accountStatus=DISABLED；企微解绑
And   月初 PM 仍可领当月全额津贴
And   audit action=handover
```

### 7. 现状 vs v3 差距（量化）

| 维度 | 现状 | v3 要求 | 差距 |
|---|---|---|---|
| `FROZEN_PENDING_HANDOVER` 状态 | 缺 | v3 BR-USER-06 必填 | 🔴 缺 1 状态 |
| 离职 7 步 | 简化为 1 接口 | 7 步 | 🔴 缺 4 步 |
| 15 日升级 | 无 | v3 衍生 | 🔴 缺定时 |
| 月初津贴补发 | 无 | v3 BR-INC-11 | 🔴 缺规则 |
| `hr.levelSource=API_ONLY` 校验 | 无 | v3 BR-USER-07 | 🔴 缺校验 |
| cron `0 2 1 * *` | 无 | v3 BR-USER-02 | 🔴 缺定时 |
| AC | 2 条 | 应有 5 条 | 🔴 缺 3 |

---

# 45. 参数配置

### 1. 页面元信息
- **编号 / URL**：45 / `/admin/system-config`（**应新增**）
- **角色 / 优先级**：`super_admin` / **P4**
- **BR 关联**：**G-05（参数可配置）**、**G-08（涉钱 6 项必为配置开关）**
- **现状**：**完全缺失**

### 2. 页面布局
- **应新增**：`<SystemConfigPage>`
- **三 Tab**：① 参数总览（v3 §7 ~50 项分组）② **6 项涉钱参数高亮**（`bonus.poolBase` `bonus.salesSource` `bonus.performanceScoreStrategy` `bonus.multiProjectSplit` `bonus.launchAnchor` `bonus.coefficientDecider`）③ 数据范围（PM/组长/超级管理员可见范围，只读）

### 3. 字段模型（v3 §7 关键参数，与全局一致性表严格对齐）

| 参数键 | 默认值 | 决策 |
|---|---|---|
| `allowance.L1..L5` | 1000/1500/2000/2500/3000 | B6 |
| `allowance.capMultiplier` | 2 | V3.1 |
| `allowance.projectCountThreshold` | 3 | A4 |
| `allowance.noOutputMonths` | 2 | V3.1 |
| `allowance.scoreStopThreshold` | 60 | V3.1 |
| `allowance.levelEffectiveRule` | `BY_BIND_TIME` | B6 |
| `bonus.poolRate` | 0.05 | V3.1 |
| 🔴 `bonus.poolBase` | `TARGET_SALES` | Q1 |
| 🔴 `bonus.salesSource` | `RECEIPT` | Q2 |
| 🔴 `bonus.performanceScoreStrategy` | `PROJECT_SCORE` | Q3 |
| 🔴 `bonus.multiProjectSplit` | `NONE` | Q5 |
| 🔴 `bonus.launchAnchor` | `L08_ACTION` | Q6 |
| 🔴 `bonus.coefficientDecider` | `G1_DUAL_SIGN` | Q4 |
| `bonus.coefficient.S/A/B` | 1.5/1.0/0.8 | E21 |
| `bonus.achievementTiers` | 6 档区间 | E22 |
| `bonus.performanceTiers` | 5 档 | E23 |
| `bonus.contribution.marketRange` | 40-65 | V3.1 |
| `bonus.contribution.rdRange` | 35-60 | V3.1 |
| `bonus.monthsWindow` | 6 | V3.1 |
| `kpi.functionalWeight` | 0.6 | V3.1 |
| `kpi.sharedWeight` | 0.4 | V3.1 |
| `kpi.reviewWeights` | `{self:0.2, marketLeader:0.4, rdLeader:0.4}` | Q3 衍生 |
| `kpi.monthlyDeadlineDay` | 5 | I5 衍生 |
| `kpi.collectorRole` | `GROUP_LEADER` | I5 |
| `kpi.reviewDaysAfterLaunch` | 30 | 建议 |
| `gate.g1.minCustomerVerifications` | 5 | I3 衍生 |
| `gate.signDeadlineDays` | **3** | D17 |
| `gate.signExtendMaxTimes` | 3 | D17 |
| `gate.reviewRoundEscalation` | 3 | D18 |
| `gate.reviewRoundSuperAdmin` | 5 | D18 |
| `gate.reviewDays90` | 90 | V3.1 |
| `bid.expireWarnDays` | 3 | C12 |
| `bid.selectDeadlineDays` | 7 | C12 |
| `bid.assignAfterDays` | 30 | C13 |
| `handover.resignDeadlineDays` | 15 | B8 |
| `handover.freezeBeforeDisable` | true | B8 |
| `deletion.leaderDeadlineDays` | **2** | F29 |
| `deletion.adminDeadlineDays` | **2** | F29 |
| `deletion.withdrawHours` | 24 | F29 |
| `noOutput.days` | 60 | E27 |
| `hr.syncCron` | `0 2 1 * *` | B6 |
| `hr.syncLeaderRole` | true | A3 |
| `hr.enableProxyLeader` | **false** | A3 |
| `hr.allowCrossRole` | **false** | B7 |
| `hr.levelSource` | `API_ONLY` | B6 |
| `req.guestOtherAssignDays` | 5 | G31 |
| `ai.monthlyTokenBudget` | 配置 | — |

### 4. API 接口清单（应新增全部）

- 🔴 `GET /api/admin/system-config` → `{ current, history }`
- 🔴 `PUT /api/admin/system-config` body=`{params}` → `{draftId}`
- 🔴 `POST /api/admin/system-config/:draftId/publish` → `{publishedVersion}`
- 🔴 `POST /api/admin/system-config/revert` body=`{targetVersionId}` 紧急回滚

### 5. 闭环剧本（v3 G-05/08）

| 环节 | 内容 |
|---|---|
| ① 发起 | super_admin 改参数 → 保存为 draft |
| ② 处理 | PUT 不覆盖 current；旧 published 不动 |
| ③ 审核 | 二次确认弹窗显示 diff；输入「确认发布」 |
| ④ 结果 | publish → 旧 published → archived；新 published 生效 |
| ⑤ 记录 | audit entityType=`system_config`, action=`update` |
| ⑥ 归档 | 旧版本只读；revert 紧急回滚写 audit |

### 6. 验收用例

```
Given super_admin 把 gate.signDeadlineDays 从 3 改为 5
When  发布
Then  后续 Gate 按 5 天算到期；audit before=3, after=5
```

```
Given 已有 draft 未发布
When  再次保存
Then  返回 409「请先发布或废弃已有 draft」
```

```
Given 切换 bonus.poolBase TARGET_SALES → ACTUAL_SALES
When  发布
Then  6 项涉钱参数变更写专用审计 action=money_param_switched
```

### 7. 现状 vs v3 差距（量化）

| 维度 | 现状 | v3 要求 | 差距 |
|---|---|---|---|
| 参数页 | 缺 | /admin/system-config | 🔴 缺 1 整页 |
| 参数数量 | 0 | ~50 项 | 🔴 缺 ~50 |
| 6 项涉钱开关 | 无 | v3 G-08 红线 | 🔴 完全缺失 |
| Draft/Publish | 无 | 必保留历史 | 🔴 完全缺失 |
| 紧急回滚 | 无 | revert 接口 | 🔴 完全缺失 |
| AC | 0 条 | 应有 4 条 | 🔴 缺 4 |

---

# 46. SOP 模板

### 1. 页面元信息
- **编号 / URL**：46 / `/admin/sop-templates`（**应新增**）
- **角色 / 优先级**：`super_admin` / **P4**
- **BR 关联**：**BR-IPD-07（SOP 绑定 + 版本号 + 在研保持原版本）**、BR-IPD-02（69 个动作）、BR-IPD-03/04
- **现状**：**完全缺失**

### 2. 页面布局
- **应新增**：`<SOPTemplatePage>`
- **三栏**：左=版本列表（published 高亮 + draft/archived）；中=Stage × Action 二维表；右=SOP 编辑器

### 3. 字段模型（v3 BR-IPD-07）

| 字段 | 类型 | 必填 | 校验 |
|---|---|---|---|
| code | string | ✅ | version 内唯一 |
| stage | enum | ✅ | concept/plan/develop/verify/launch/lifecycle |
| name | string | ✅ | minLength 2 |
| depth | enum | ✅ | DEEP/LIGHT |
| isBlocking | bool | — | — |
| steps[].title/operation/output | string | ✅ | minLength 2/4/4 |
| fields[].key | string | ✅ | camelCase |
| fields[].type | enum | ✅ | text/textarea/number/select |
| outputTemplate.sections[].fieldKey | string | ✅ | 必须在 fields 中 |

### 4. API 接口清单（应新增）

- 🔴 `GET /api/admin/sop-versions`
- 🔴 `GET /api/admin/sop-versions/:id`
- 🔴 `POST /api/admin/sop-versions/:id/copy` → draftId
- 🔴 `PUT /api/admin/sop-templates/:templateId`（仅 draft）
- 🔴 `POST /api/admin/sop-versions/:id/publish`
- 🔴 `POST /api/admin/sop-versions/:id/revert`

### 5. 闭环剧本（v3 BR-IPD-07）

| 环节 | 内容 |
|---|---|
| ① 发起 | super_admin 复制当前 published 为 draft |
| ② 处理 | 编辑单个动作 SOP；保存 draft |
| ③ 审核 | 发布前校验 fields 唯一、sections 引用存在 |
| ④ 结果 | 新建项目 sopVersionId 指向新 published；**在研项目保持原 sop_json**（v3 关键） |
| ⑤ 记录 | audit entityType=`work_item`, action=`update` |
| ⑥ 归档 | archived 只读 |

### 6. 验收用例

```
Given super_admin 复制 v3 为 v4 draft 改 P03 minChars=80→120
When  发布
Then  新建项目 sopVersionId=4；既有项目 P03 保持 v3
And   audit action=update 含 diff
```

```
Given draft 中 sections[0].fieldKey 不在 fields 中
When  发布
Then  返回 400 指明错误 index/fieldKey
```

### 7. 现状 vs v3 差距（量化）

| 维度 | 现状 | v3 要求 | 差距 |
|---|---|---|---|
| SOP 编辑页 | 缺 | /admin/sop-templates | 🔴 缺 1 整页 |
| 69 个动作 seed | 部分 | v3 BR-IPD-02 +5 个 v3 新增（Z01-Z05） | ⚠️ 需补 Z01-Z05 |
| 版本号管理 | 无 | v3 BR-IPD-07 必填 | 🔴 完全缺失 |
| 在研保持原版本 | 无 | v3 关键约束 | 🔴 完全缺失 |
| AC | 0 条 | 应有 4 条 | 🔴 缺 4 |

---

# 47. Gate 评审要素

### 1. 页面元信息
- **编号 / URL**：47 / `/admin/gate-elements`（**应新增**）
- **角色 / 优先级**：`super_admin` / **P4**
- **BR 关联**：**BR-GATE-01b（33 项要素 + 14 项否决项可增删改）**、BR-GATE-02、BR-GATE-04（双签 `gate.signDeadlineDays=3`）
- **现状**：**完全缺失**独立管理页（数据齐 v1 文件）

### 2. 页面布局
- **应新增**：`<GateElementsPage>`
- **三列**：左=五大 Gate；中=要素表（code/label/isVeto/gateCodes/threshold/evidenceRequired）；右=详情编辑器
- **顶部 CTA**：新增要素 / 复制 Gate / 历史恢复
- **关键元素**：否决项红色标记 + 双签确认勾选 + 毛利率门槛

### 3. 字段模型（v3 BR-GATE-01b）

| 字段 | 类型 | 必填 | v3 要求 |
|---|---|---|---|
| code | string | ✅ | 唯一 |
| gateCode | enum[] | ✅ | G1/G2/G3/G4/G5 |
| label | string | ✅ | minLength 4 |
| isVeto | bool | — | 否决项硬阻断（共 14 项） |
| vetoDualRequired | bool | — | isVeto=true 时必为 true |
| thresholdJson | json | — | `{type:"min", field:"grossMargin", value:0.3}` |
| evidenceRequired | bool | — | — |
| requiredAttachmentTypes | enum[] | — | minutes/review_material/spec |
| judgmentStates | enum | — | ✅ 通过 / ⚠️ 带条件（责任人+期限）/ ❌ 不通过 |

**33 项要素分布**：G1 七项（含 5 否决）/ G2 六项（含 4 否决）/ G3 五项 / G4 八项（含 3 否决）/ G5 七项（含 2 否决）= **33 项 + 14 否决**

### 4. API 接口清单（应新增）

- 🔴 `GET /api/admin/gate-elements?gateCode=`
- 🔴 `POST /api/admin/gate-elements`
- 🔴 `PUT /api/admin/gate-elements/:id`（仅 draft）
- 🔴 `DELETE /api/admin/gate-elements/:id`（仅 draft）
- 🔴 `POST /api/admin/gate-element-versions/:id/publish`
- 🔴 `POST /api/admin/gate-element-versions/:id/revert`

### 5. 闭环剧本（v3 BR-GATE-01b）

| 环节 | 内容 |
|---|---|
| ① 发起 | super_admin 选 Gate → 「新增要素」 |
| ② 处理 | 填要素 → 保存 draft |
| ③ 审核 | 发布前校验：isVeto=true ⇒ vetoDualRequired=true；thresholdJson 合法 |
| ④ 结果 | 新建项目启动 Gate 时按新要素清单 |
| ⑤ 记录 | audit entityType=`gate_element_result`, action=`update` |
| ⑥ 归档 | 旧 published archived；否决项变更不影响已发起评审（快照） |

### 6. 验收用例

```
Given super_admin 新增 G1「毛利率达标」thresholdJson={grossMarginMin:0.3}, isVeto=true
When  发布
Then  后续 G1 评审该要素默认未通过
And   必须双签确认才能通过
And   audit action=update
```

```
Given 已 published 要素
When  直接编辑 label
Then  返回 409「请先复制到 draft」
```

### 7. 现状 vs v3 差距（量化）

| 维度 | 现状 | v3 要求 | 差距 |
|---|---|---|---|
| Gate 要素后台 | 缺 | /admin/gate-elements | 🔴 缺 1 整页 |
| 33 项 seed | 数据齐 v1 | 缺页面 | ⚠️ 缺 UI |
| 三态判定（✅⚠️❌） | 部分 | 显式 + ⚠️ 必填责任人+期限 | 🔴 缺 ⚠️ 跟踪 |
| 否决项硬阻断 | 实现 | v3 保持 | ✓ |
| 14 项否决项标注 | 未配置 | v3 BR-GATE-01b | 🔴 缺 |
| AC | 1 条 | 应有 4 条 | 🔴 缺 3 |

---

# 48. AI 模型配置

### 1. 页面元信息
- **编号 / URL**：48 / `/admin/ai-config`（与 42 页配置态合并）
- **角色 / 优先级**：`super_admin` / **P4**
- **BR 关联**：**BR-AI-01（provider/endpoint/apiKey/model/temperature/maxTokens/超时重试/月度 token 预算/限流）**
- **现状**：**完全缺失**

### 2. 页面布局
- **应新增**：`<AIConfigPage>`
- **三段**：① Provider 列表（11 字段全）② 当前默认 Provider ③ 同步/调用日志

### 3. 字段模型（v3 BR-AI-01 完整）

| 字段 | 类型 | 必填 | 校验 |
|---|---|---|---|
| name | string | ✅ | minLength 2 |
| providerType | enum | ✅ | openai/anthropic/azure/qwen-local |
| endpoint | url | ✅ | url |
| apiKey | string | ✅ | minLength 20；AES 加密 |
| model | string | ✅ | — |
| temperature | number | — | 0–2 |
| maxTokens | number | — | ≥ 1 |
| timeoutMs | number | — | ≥ 1000 |
| retryTimes | number | — | 0–3 |
| monthlyTokenBudget | number | — | ≥ 0 |
| rpmLimit | number | — | ≥ 1 |
| enabled / isDefault | bool | — | 默认 true / 唯一 |

### 4. API 接口清单（应新增）

- 🔴 `GET /api/admin/ai-providers`
- 🔴 `POST /api/admin/ai-providers`
- 🔴 `PUT /api/admin/ai-providers/:id`
- 🔴 `POST /api/admin/ai-providers/:id/rotate-key`
- 🔴 `POST /api/admin/ai-providers/:id/set-default`
- 🔴 `POST /api/admin/ai-providers/:id/sync`
- 🔴 `GET /api/admin/ai-call-logs`

### 5. 闭环剧本（v3 BR-AI-01）

| 环节 | 内容 |
|---|---|
| ① 发起 | super_admin 新增 Provider |
| ② 处理 | AES 加密 apiKey；写 audit |
| ③ 审核 | set-default 切换默认 |
| ④ 结果 | /api/ai/run 按默认路由；预算超限 fallback |
| ⑤ 记录 | ai_call_logs + audit entityType=`system_config`, action=`update` |
| ⑥ 归档 | 轮换密钥立即生效 |

### 6. 验收用例

```
Given super_admin 新增本地通义千问 + 设默认
When  PM 触发 AI
Then  /api/ai/run 实际调用本地模型；audit 含 tokenUsage, latencyMs
```

```
Given 月预算超限
When  下一次调用
Then  返回 4xxxx + fallback 提示
```

### 7. 现状 vs v3 差距（量化）

| 维度 | 现状 | v3 要求 | 差距 |
|---|---|---|---|
| 配置页 | 缺 | /admin/ai-config | 🔴 缺 1 整页 |
| 字段数 | 0 | 12 | 🔴 缺 12 |
| API Key 加密 | 无 | v3 必填 | 🔴 缺 |
| 月预算 fallback | 无 | v3 必填 | 🔴 缺 |
| 调用日志 | 无 | v3 BR-AI-05 | 🔴 缺 |
| AC | 0 条 | 应有 4 条 | 🔴 缺 4 |

---

# 49. 超级管理员移交

### 1. 页面元信息
- **编号 / URL**：49 / `/admin` 内嵌 `<SuperAdminSuccessionPanel>`（FinalRulesPages.jsx:80-88）
- **角色 / 优先级**：`super_admin`（仅当前超级管理员可见）/ **P4**
- **BR 关联**：**BR-ADM-01**、**BR-ADM-02**、**BR-ADM-03（旧账号禁止登录）**
- **现状**：完整（server.mjs:647-698 `/api/admin/transfer-readiness` + `/api/admin/transfer`）

### 2. 页面布局
- **沿用**：`<SuperAdminSuccessionPanel>` 双栏 `.succession-grid`
- **左列表单**：接任 product_lead 下拉 + 承接产品组的 replacement_lead 下拉 + 确认短语输入 + 红色 `danger-action` 按钮
- **右列**：准备度 4 项 checklist（current_projects / current_tasks / successor / group_replacement）

### 3. 字段模型

| 字段 | 类型 | 必填 | 校验 |
|---|---|---|---|
| toUserId | uuid | ✅ | role=`product_lead`, status=active |
| replacementLeadId | uuid\|"" | — | 若 targetGroup 存在必填 |
| confirmation | string | ✅ | **字面量等于 "确认移交管理员"**（v3 BR-ADM-02） |
| currentPassword | string | ✅ | 服务端 verifyPassword |

### 4. API 接口清单

- ✅ `GET /api/admin/members`（server.mjs:397）
- ✅ `GET /api/admin/transfer-readiness?toUserId=&replacementLeadId?`（server.mjs:647）
- ✅ `POST /api/admin/transfer`（server.mjs:663）body=`{toUserId, replacementLeadId?, confirmation, currentPassword}`
- **错误码**：400 confirmation 不匹配（"确认移交管理员"）/ 403 密码错 / 409 接任人不是 product_lead / 409 原超级管理员仍有未清事项

### 5. 闭环剧本（v3 BR-ADM-01/02/03）

| 环节 | 内容 |
|---|---|
| ① 发起 | 当前 super_admin 选接任组长 → 系统轮询 readiness |
| ② 处理 | 用户输入「确认移交管理员」+ 二次 prompt 当前密码 |
| ③ 审核 | BEGIN IMMEDIATE：① 原超级管理员 role=`product_manager`, status=`inactive` ② DELETE 原超级管理员 sessions ③ 接任人 role=`super_admin` ④ DELETE 接任人 sessions ⑤ UPDATE product_groups.lead_user_id |
| ④ 结果 | `count(role='super_admin' and status='active') === 1` 校验 |
| ⑤ 记录 | audit entityType=`user`, action=`reassign_super_admin`, detail.formerAdminDisabled=true |
| ⑥ 归档 | 原超级管理员 sessions 立即撤销；所有写操作返回 401（v3 BR-ADM-03） |

### 6. 验收用例

```
Given 当前 super_admin A 仍有 1 个活动项目
When  选接任人 B
Then  readiness.ready=false，checks[0].passed=false
And   「执行原子移交」按钮 disabled
```

```
Given A 无活动项目无待办，B 是 product_lead 且负责 G1
When  输入 replacementLeadId=C + confirmation="确认移交管理员" + 当前密码
Then  POST transfer 返回 200，newAdmin.name=B
And   A 后续请求 401；B 登录 role=super_admin
And   G1.lead_user_id=C；唯一超级管理员校验通过
And   audit action=reassign_super_admin, detail.formerAdminDisabled=true
```

```
Given confirmation 多打一个空格
When  点击执行
Then  前端 button.disabled 仍为 true（confirmation!==预期字面量）
And   服务端即便被绕过也会返回 400
```

```
Given A 有项目但无待办（混合场景）
When  选接任人 B
Then  readiness.ready=false（current_projects 未通过）
And   不能绕过 checks 进入 transfer
```

### 7. 现状 vs v3 差距（量化）

| 维度 | 现状 | v3 要求 | 差距 |
|---|---|---|---|
| 实现度 | ✅完整 | BR-ADM-01/02/03 已对齐 | ✓ |
| 确认短语 | "确认移交管理员" 已实现 | v3 同样要求 | ✓ |
| 原子事务 | BEGIN IMMEDIATE 已实现 | v3 必填 | ✓ |
| 旧账号禁用 | sessions DELETE 已实现 | v3 BR-ADM-03 | ✓ |
| Readiness 4 项 | 已实现 | v3 必填 | ✓ |
| AC | 2 条 | 应有 4 条 | ⚠️ 已补 2 条边界（confirmation 多空格 / 混合项目-待办场景） |