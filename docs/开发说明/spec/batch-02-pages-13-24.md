# 13. 轻管动作详情

## 1. 页面元信息
- **编号 / 名称 / URL**：13 / 轻管动作详情 / `/action?workItem=<id>`（从 `/projects` 轻管行式点击进入）
- **主用角色 / 可见角色**：rd_pm（主责）/ market_pm / product_lead / super_admin / **优先级 P1**
- **关联 BR 规则**：BR-IPD-04（严格三字段）、BR-IPD-05（三个例外：P10/V02、D11/Z01、V11/Z04）、G-10（市场PM 视角优先，禁止过度设计）、BR-IPD-09（轻管行式视觉）
- **现有实现**：**部分** — `App.jsx:148-181` 的 `TaskWorkspace` + `App.jsx:183-209` 的 `GenericTaskWorkspace` 复用所有动作；未按 BR-IPD-04 区分；无 FAR/FRR / cert_no / cert_passed_at / template_type 字段透出

## 2. 页面布局（基于现有代码，**应改造**）
- 现状 `GenericTaskWorkspace` 布局：顶栏 `<WorkspaceSummary>` → 只读 banner → 面包屑 → 任务头（标题 + SOP 步骤 + 产物字段 + 质量检查 + AI 面板 + 模板预览）→ 输出模板卡 → AI 弹窗
- **应改造**：新增 `<LightTaskWorkspace>` 组件，沿用现有 `Frame` / `PageFrame` 命名，仅含三段：
  1. 头部：动作名 + 阶段 + 完成标准只读（来自 `sop.doneDefinition`）+ 上传评审证据入口（**允许外部评审证据，但仅一个文件入口**，与 BR-IPD-04 区分）
  2. 三字段卡：`status`（`DONE` / `WAIVED`）/ `actual_done_at`（date） / `note`（textarea ≤ 500 字）
  3. 例外字段卡（按 action_code 条件渲染）：
     - **D11 / Z01**：`far_value` / `frr_value` 两个 decimal 输入（必须，二者和小于 1.00）
     - **P10 / V02**：`cert_no` 文本输入 + `cert_passed_at` 日期（必须，阻断性动作）
     - **V11 / Z04**：在 `SOLUTION` 模板下**禁用三字段**，改为单一"转入深管"按钮
- 视觉规范（BR-IPD-09）：轻管在 IPD 流程页以**行式**呈现（`.light-row`）；详情页用单列紧凑 surface，不展示 SOP 步骤、AI 面板、产物模板预览
- 只读 banner：组员项目 → "组员项目 · 只读监督"；前置动作未完成 → "顺序锁定"

## 3. 字段模型

| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---|---|---|---|---|
| `work_item_id` | UUID | 是 | — | URL `?workItem=` | — | 权限范围内且 `depth='LIGHT'` |
| `status` | enum | 是 | `NOT_STARTED` / `IN_PROGRESS` / `DONE` / `DELAYED` / `NA` | `stage_action.status` | `NOT_STARTED` | 切 `DONE` 必须先填日期 |
| `actual_done_at` | ISO datetime | DONE 必填 | — | 用户输入 | 空 | ≤ today +1d 且 ≥ project.startDate |
| `note` | text | 否 | ≤ 500 字 | 输入 | 空 | 字数提示性 |
| `farValue` | decimal(10,6) | D11/Z01 必填 | 0.000001–0.999999 | 输入 | 空 | 保留 6 位小数 |
| `frrValue` | decimal(10,6) | D11/Z01 必填 | 0.000001–0.999999 | 输入 | 空 | `farValue + frrValue ≤ 1.000000`（阈值后台可配） |
| `certNo` | string | P10/V02 必填 | 4–60 字 | 输入 | 空 | 正则 `^[A-Za-z0-9\-/]+$` |
| `cert_passed_at` | date | P10/V02 必填 | ISO date | 输入 | 空 | ≤ today +1d |
| `templateType` | enum | 否 | `HARDWARE` / `SOFTWARE` / `SOLUTION` | project.templateType | — | V11/Z04 `SOLUTION` 时强制转深管 |
| `depth` | enum | 是 | `LIGHT` / `DEEP` | 服务端计算 | `LIGHT` | `SOLUTION` + V11/Z04 → `DEEP` |

## 4. API 接口清单
- **`GET /api/work-items/:id`** — 返回 `{ id, action_code, stage_code, depth, sop, latest_quality, payload, checklist, revision, owner_id, template_type, requires_far_frr, requires_cert, requires_deep_conversion }`
- **`PATCH /api/work-items/:id`**（v3 §TS-13 PATCH 语义）— 请求体见字段表
  - 响应：`{ revision, quality_status, next_work_item, stage_ready, deep_conversion_required }`
  - 错误码：`10001` 字段缺失 / `10003` cert_no 格式错 / `40001` FAR+FRR 越界（违反阶段门禁数值约束）/ `40001` V11/Z04 SOLUTION 模板强制转深管（拒绝直接保存 LIGHT 字段，状态机非法）/ `30001` 非当前签署人
  - 权限：项目 owner 或动作责任人；组员只读
  - 审计：`entityType=stage_action, action=complete_light`；FAR/FRR/cert 字段写 `entityType=stage_action, action=register_farfrr|register_cert`
- **`POST /api/work-items/:id/convert-to-deep`**（**新增**）— V11/Z04 在 SOLUTION 模板下专用
  - 请求：`{ work_item_id, reason }`
  - 响应：`{ deep_work_item_id, sop_template_id }`
  - 错误码：`40001` 状态机非法 / `40002` 项目不在 SOLUTION 模板（双模板校验未通过）
  - 审计：`entityType=stage_action, action=convert_light_to_deep`

## 5. 闭环剧本（6 环节）
| 环节 | 内容 |
|---|---|
| ① 发起 | rd_pm 在 `/projects` 轻管行点击 → 前端按 `depth=LIGHT` 路由到 `<LightTaskWorkspace>`；D11/P10/V02/V11/Z04 加载例外字段 |
| ② 处理 | 用户填三字段；例外字段卡按 `action_code` 条件渲染；V11/Z04 SOLUTION 模式下三字段禁用 → 点击"转入深管" |
| ③ 审核 | 普通轻管：保存即完成；D11/Z01：写入 FAR/FRR 后 `work_item_metrics` 入库，关联 `performance_kpis`；P10/V02：写入证书编号 + 通过日期，触发 `certificates` 表入库与 `cert_templates` 关联 |
| ④ 结果 | `status=DONE` 后顺序下一动作解锁；P10/V02 阻断性动作：`stage_gate` 校验通过才解锁；超期触发 `BR-IPD-06` 阶段门禁警示 |
| ⑤ 记录 | 审计：`stage_action.complete_light`；FAR/FRR/cert 入 `work_item_metrics` + `certificates`；触发 `notifications` |
| ⑥ 归档 | 项目结项（`status=ARCHIVED`）后所有 `status=DONE` 字段冻结；FAR/FRR 数值进入 `performance_kpis` 历史快照；证书进入产品档案 |

## 6. 验收用例（Given-When-Then，5 条）
```
Given rd_pm 打开 D11（BioCV 算法训练与评测）
When 进入 /action?workItem=<D11-id>
Then 页面仅渲染三字段卡 + FAR/FRR 数值卡，无 SOP 步骤、AI 面板、模板预览
And 头部完成标准只读显示
When 不填 FAR 直接点保存
Then 返回 10001 "FAR/FRR 为必填项"
When 填 far_value=0.4, frr_value=0.7 → 保存
Then 返回 40001 "FAR+FRR > 1.0（违反阶段门禁数值约束）"
When 填 far_value=0.001, frr_value=0.002 → 保存
Then 200 OK，revision+1，audit=register_farfrr

Given V11 在 HARDWARE 模板下
When 进入 /action?workItem=<V11-id>
Then 仅渲染三字段卡，无 SOLUTION 转换提示
Given V11 在 SOLUTION 模板下
When 进入 /action?workItem=<V11-id>
Then 三字段 disabled，底部显示"转入深管"按钮
When 点击 → POST /convert-to-deep
And 跳转 /action?workItem=<deep-id> 渲染完整深管布局

Given P10（认证规划）未填写 cert_no
When 提交保存
Then 返回 10001 "cert_no 必填"
And 阻断字段写入校验：cert_no=空 即拒绝
```

## 7. 现状 vs v3 差距（量化）
- **缺失字段**：5 个（`actual_done_at`、`far_value` / `frr_value`、`cert_no` / `cert_passed_at`、`template_type` 联动）
- **缺失接口**：2 个（`POST /api/work-items/:id/convert-to-deep`、PATCH 深度/轻管分离校验端点）
- **缺失状态机环节**：2 个（V11/Z04 SOLUTION 模板 → 强制 `LIGHT → DEEP` 转换分支；D11/Z01 FAR/FRR 数值关联 `performance_kpis` 的回写）
- **缺失审计**：`entityType=stage_action`（现状用 `work_item`，未对齐全局枚举）；action `complete_light` / `register_farfrr` / `register_cert` / `convert_light_to_deep`
- **缺失系统参数引用**：1 个（FAR/FRR 阈值后台可配，现状硬编码）
- **验收覆盖度**：现状 3 条 / v3 应有 7 条（差 4 条：FAR/FRR 必填、cert_no 必填、SOLUTION 自动转深管、阻断性动作 P10/V02 字段联动）
- **根因**：`GenericTaskWorkspace` 复用于所有动作，未按 BR-IPD-04 区分深管/轻管

---

# 14. 项目详情-文档与交付物

## 1. 页面元信息
- **编号 / 名称 / URL**：14 / 项目详情-文档与交付物 / `/documents`
- **主用角色 / 可见角色**：全部有项目权限者 / **优先级 P1**
- **关联 BR 规则**：BR-DOC-01（结构化产物+附件双视图）、BR-IPD-07（深管 SOP 富文本与附件）、BR-IPD-09（轻管动作不挂附件）、BR-DEL-02（附件软删除）
- **现有实现**：**完整** — `App.jsx:365-371` 的 `DocumentsPage` + `App.jsx:373-378` 的 `DocumentUploadModal`

## 2. 页面布局（基于现有代码）
- 顶栏：`<PageFrame title="资料库">`，副标题"项目所有结构化产物、历史版本、动作/需求/变更附件和公共资料统一汇总；上传入口只用于新增外部文件"
- 右上：`can_edit_project` 时显示 `+ 上传并归类附件`
- 主体：
  - `.document-toolbar`：左侧 search input（debounce 220ms）+ 中部 segmented `全部 / 在线产物 / 附件` + 右侧计数
  - `.document-table` 6 列表头：名称 / 类型 / 状态 / 版本 / 作者 / 最近更新
  - 附件行 `<a href="/api/attachments/:id">` / 产物行 `<button onClick="...">`
- 弹窗 `DocumentUploadModal`：4 类别（`stage_action` / `gate_review_material` / `project_common` / `requirement_change`）+ 阶段 + 动作 + 描述

## 3. 字段模型

| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---|---|---|---|---|
| `q` | string | 否 | — | 搜索框 | 空 | debounce 220ms |
| `filter` | enum | 否 | `all` / `artifact` / `attachment` | segmented | `all` | — |
| `category` | enum | 是 | `stage_action` / `gate_review_material` / `project_common` / `requirement_change` | select | `stage_action` | — |
| `stage_id` | UUID | `project_common` 必空 | — | `boot.stages` | 第1阶段 | `project_common` 禁用 |
| `work_item_id` | UUID | category=`stage_action` 必填 | — | `boot.work_items` 按 stage 过滤 | 空 | 必须属于 stage |
| `description` | text | `project_common` 必填 | ≥4 字 | 输入 | 空 | — |
| `file` | file | 是 | pdf/doc/docx/xls/xlsx/ppt/pptx/image/*/video/* | input | 空 | 单份 ≤ 20MB |

## 4. API 接口清单
- **`GET /api/documents?projectId=...&q=...`** — 返回 `{ documents: [...] }`
- **`POST /api/attachments?projectId=...`** — `multipart/form-data`；响应 `{ id, original_name, size, category }`
  - 错误码：`10001` 必填缺失 / `30001` 无编辑权限
  - 权限：项目 owner 或成员
  - 审计：`entityType=attachment, action=create`
- **`GET /api/attachments/:id`** — 受 auth 中间件保护
- **新增（v3 应有）**：`GET /api/work-items/:id/attachments` — 按工作项聚合附件列表，供轻管 D11/P10 等动作虽不上传但展示评审证据

## 5. 闭环剧本（6 环节）
| 环节 | 内容 |
|---|---|
| ① 发起 | 用户在 `/documents` 点击"+ 上传"或直接搜索 |
| ② 处理 | 选 category → 选 stage → 选 work_item → 填 description → 选 file → 提交 |
| ③ 审核 | 项目内资料无需审批；公共资料落库后可被全部成员检索 |
| ④ 结果 | `attachments` + `project_documents` 入库；toast"附件已归类" |
| ⑤ 记录 | 审计 `attachment.create`，含 category + 关联对象 |
| ⑥ 归档 | 项目删除后附件 `status=archived`，永久保留（BR-DEL-02） |

## 6. 验收用例（Given-When-Then，5 条）
```
Given 项目 owner
When 上传 PDF 至 P10 动作（stage_action category）
Then 表格新增一行 "PDF · stage_action · 已归档 · 原文件 · 作者"
And project_documents 关联项目-动作双向记录
And 审计 entityType=attachment, action=create

Given 非 owner
When 进入 /documents
Then 上传按钮 hidden
And 现有行可下载

Given 用户搜索 "机会评估"
When debounce 220ms 后
Then 调用 /api/documents?q=机会评估
And 表格按 title/content/stage/work_item/author 模糊匹配过滤

Given P10 动作（轻管 + 阻断性动作）需登记评审证据
When 调用 GET /api/work-items/<P10-id>/attachments
Then 返回评审证据集合（即使 LIGHT 模式也展示）
And 不显示 stage_action 自身的 SOP 交付物
```

## 7. 现状 vs v3 差距（量化）
- **缺失字段**：3 个（`certificates` 关联字段、`sop.revisionId`、`requirement_id` 交叉关联）
- **缺失接口**：1 个（`GET /api/work-items/:id/attachments` 轻管动作评审证据聚合）
- **缺失状态机环节**：1 个（附件删除走 BR-DEL-02 软删除，现状直接硬删）
- **缺失审计**：`category` 枚举名需对齐（`stage_action` / `gate_review_material` / `requirement_change`）；`action=create` 替代现状 `upload`
- **验收覆盖度**：现状 3 条 / v3 应有 6 条（差 3 条：work_item 级聚合、软删除、证书关联检索）

---

# 15. 项目详情-项目日志

## 1. 页面元信息
- **编号 / 名称 / URL**：15 / 项目详情-项目日志 / `/timeline`
- **主用角色 / 可见角色**：全部有项目权限者 / **优先级 P2**
- **关联 BR 规则**：BR-TRAIL-01（六阶段动作轨迹）、BR-TRAIL-02（治理与签署）、BR-TRAIL-03（激励闭环）、BR-GATE-04（Gate 节点留痕）、BR-IPD-05b（证书轨迹）
- **现有实现**：**完整** — `App.jsx:340-358` 的 `TimelinePage` + `App.jsx:360-363` 的 `TimelineActionDrawer`

## 2. 页面布局（基于现有代码）
- 顶栏：`<PageFrame title="全流程轨迹">` + 右上 status pill（`ARCHIVED`→success / `ACTIVE`→blue）
- 第一行 4 metric：IPD 动作数 / 阶段签字数 / 双周评审数 / 津贴快照数
- 第二区 `<TraceSummary>`：双 PM 配对卡（market_pm / rd_pm / 直属组长 / 项目等级 S/A/B）；未配对 → 空态 banner
- 第三区 `<TraceStages>`：6 阶段区块（序号、阶段名、动作数、附件数、`stage_action.status` pill、通过时间）
- 第四区 `<PerformanceGrid>`：左"治理与签署"事实；右"激励闭环"（成功奖金 + 贡献度 + 津贴）
- 抽屉 `<TimelineActionDrawer>`：填写产物 + 质量检查（`passed` / `failed` / `quality_pending` / `manual_pass`）+ 历史版本 + 附件 + 关联需求 + AI 采纳记录（只读）

## 3. 字段模型

| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---|---|---|---|---|
| `project_id` | UUID | 是 | — | URL | — | 权限范围 |
| `stages[]` | array | — | `{ id, code, name, status, approved_at, stage_actions[], attachments[] }` | API | — | — |
| `decisions[]` | array | — | `{ id, subject_type, status, signer_role }` | API | — | — |
| `biweekly_reviews[]` | array | — | `{ sequence, status, risks[] }` | API | — | — |
| `allowances[]` | array | — | `{ period, professional_track, capped_amount, status }` | API | — | — |
| `bonuses[]` | array | — | `{ professional_track, calculated_amount }` | API | — | — |
| `assignment` | object | — | `{ market_pm_id, market_pm_name, market_level_locked, rd_pm_*, strategic_level, status }` | API | null | — |
| `contribution` | object | — | `{ market_pct, rd_pct, status }` | API | null | — |
| `certificates[]` | **v3 新增** | array | `{ stage_action_code, cert_no, cert_passed_at, country_code, item_code }` | API | — | — |
| `gate_element_result[]` | **v3 新增** | array | `{ gate_code, element_code, decision, judge_role, responsible_user_id }` | API | — | — |

## 4. API 接口清单
- **`GET /api/projects/:id/timeline`** — 返回完整时间线 `{ project, assignment, stages[], decisions[], biweekly_reviews[], allowances[], bonuses[], contribution, requirements[], changes[] }`
  - 权限：项目 owner / 项目成员 / 直属组长 / super_admin
  - 错误码：`30003` 数据范围越权
- **`GET /api/projects/:id/timeline/actions/:workItemId`** — 抽屉数据 `{ action, revisions[], quality_runs[], attachments[], requirements[], ai_conversations[] }`
  - 错误码：`50001` 动作不存在 / `30003` 跨项目访问
- **新增（v3 应有）**：`GET /api/projects/:id/timeline/certificates` — 项目级证书轨迹
- **新增（v3 应有）**：`GET /api/projects/:id/timeline/gate-element-results` — 五大 Gate 33 项要素判定轨迹

## 5. 闭环剧本（6 环节）
| 环节 | 内容 |
|---|---|
| ① 发起 | 用户进入 `/timeline` |
| ② 处理 | 渲染六阶段区块，按时间顺序展开 stage_actions；点击触发抽屉 |
| ③ 审核 | 只读，无审核动作 |
| ④ 结果 | 用户可下钻到任一动作的全部版本与质量记录 |
| ⑤ 记录 | 所有动作通过 audit + stage_actions 自动积累；audit `entityType=stage_action, action=update/complete_light/complete_deep` |
| ⑥ 归档 | 项目 `status=ARCHIVED` 后整页只读 |

## 6. 验收用例（Given-When-Then，5 条）
```
Given 项目 owner
When 进入 /timeline
Then 双 PM 配对卡显示（market_pm、rd_pm、组长、strategic_level）
And 六阶段区块按顺序展开

Given 项目 status=ARCHIVED
When 进入 /timeline
Then pill="已正式结项 · 全部只读"
And 抽屉不可触发写操作

Given 项目未配对双 PM
When 进入 /timeline
Then 显示"尚未启用双 PM 协同"空态 banner
And 仍展示动作/阶段/附件/津贴/奖金快照

Given 概念阶段 Gate 通过
When 进入 /timeline
Then 第三区出现"Gate 评审区块"含 33 项要素判定汇总
And 状态字段引用 `gate.status`（`APPROVED`）

Given P10/V02 动作完成
When 进入 /timeline
Then 证书轨迹区显示 `certificates[]` 列表，含 cert_no / cert_passed_at
```

## 7. 现状 vs v3 差距（量化）
- **缺失字段**：2 个（`gate_element_result[]` Gate 要素判定轨迹、`certificates[]` 证书轨迹）
- **缺失接口**：2 个（`/timeline/certificates`、`/timeline/gate-element-results`）
- **缺失状态机环节**：1 个（Gate 要素 33 项判定轨迹聚合，v3 BR-GATE-01b 要求）
- **缺失审计 `entityType`**：1 个（`gate_element_result`）
- **缺失字段命名**：snake_case 对齐（`quality_runs` 而非 `qualityRuns`、`ai_conversations` 而非 `aiConversations`）
- **验收覆盖度**：现状 3 条 / v3 应有 5 条（差 2 条：Gate 评审区块、证书轨迹区块）

---

# 16. 产品管理-产品目录

## 1. 页面元信息
- **编号 / 名称 / URL**：16 / 产品管理-产品目录 / `/products`
- **主用角色 / 可见角色**：super_admin（导入+查看）/ **优先级 P1**
- **关联 BR 规则**：BR-PROD-01（Excel 幂等导入）、BR-PROD-02（产品档案独立于项目）、BR-PROD-03（缺失行不删除）、BR-IPD-05b（target_markets 驱动认证清单）、BR-IPD-05c（is_bio_feature 挂载 C12）
- **现有实现**：**完整** — `App.jsx:548-555` 的 `ProductCatalogPage`

## 2. 页面布局（基于现有代码）
- 顶栏：`PageFrame title="产品目录"`，副标题"产品档案独立于项目长期存在，按产品型号幂等更新；缺失行不会自动删除"
- 非 super_admin → `<Empty icon={LockKey}>`
- 主体 `.catalog-grid`：
  - 左 `<ImportCard>`：Excel 选择 + 上传 + 预校验 + 预览区（total/valid/creates/errors + 错误明细 + confirm）
  - 右 `<ImportHistory>`：最近 8 批次
- 下方 `<ProductCatalog>` 6 列表：型号/产品 / 产品线 / 版本 / 市场PM / 生命周期 / 最近更新

## 3. 字段模型

| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---|---|---|---|---|
| `file` | file | 是 | `.xlsx` | input | 空 | 仅 Excel |
| `batch_id` | UUID | confirm 必填 | — | preview 返回 | 空 | — |
| `summary.total/valid/creates/errors` | int | — | — | 后端解析 | — | errors>0 禁用 confirm |
| `errors[]` | array | — | `{ row, message }` | 后端 | — | — |
| `product.model_code` | string | 是 | 唯一主键 | Excel | — | — |
| `product.name` | string | 是 | — | Excel | — | — |
| `product.product_line` | string | 是 | — | Excel | — | — |
| `product.current_version` | string | 是 | — | Excel | — | — |
| `product.owner_market_pm_no` | string | 是 | 工号匹配 | Excel | — | — |
| `product.lifecycle_status` | enum | 是 | `draft` / `published` / `retired` | Excel | — | retired 需走退市 |
| `product.target_markets[]` | string[] | **v3 新增** | ISO 国家代码 | Excel/编辑 | 空 | 驱动国别认证清单自动带出（BR-IPD-05b） |
| `product.is_bio_feature` | boolean | **v3 新增** | — | Excel/编辑 | false | true 时挂载 C12 生物特征合规 |

## 4. API 接口清单
- **`GET /api/products`**（按权限裁剪，super_admin 全部 / 其他角色按 visibility scope）— 返回 `{ products[], batches[] }`
- **`POST /api/products/import-preview`** — `multipart/form-data`；返回 `{ batch_id, summary, errors[] }`
  - 错误码：`10002` 文件非 xlsx / `30001` 非 super_admin
- **`POST /api/products/import-confirm`** — `{ batch_id }`；响应 `{ imported, updated }`
  - 错误码：`40005` 阶梯系数匹配异常（errors>0 拒绝确认）
  - 审计：`entityType=product, action=create`（新增）/ `entityType=product, action=update`（更新，复合审计）
- **新增（v3 应有）**：`PUT /api/products/:id/target-markets` — `{ target_markets[], is_bio_feature }` 触发国别认证清单自动带出
  - 审计：`entityType=product, action=update`

## 5. 闭环剧本（6 环节）
| 环节 | 内容 |
|---|---|
| ① 发起 | super_admin 在 `/products` 选 xlsx → 点击"预校验差异" |
| ② 处理 | 服务端解析固定字段；按型号幂等 upsert；返回 preview |
| ③ 审核 | errors>0 禁用 confirm；服务端二次校验 |
| ④ 结果 | 写入 `products` 表（upsert）；batches.status=confirmed |
| ⑤ 记录 | 审计 + batches 永久保留 |
| ⑥ 归档 | 删除 Excel 行不会删除现有产品（缺失行不删除）；导入历史永久追溯 |

## 6. 验收用例（Given-When-Then，5 条）
```
Given super_admin
When 进入 /products
Then 显示 Excel 导入 + 最近批次 + 产品主数据表
And 表头包含 target_markets 与 is_bio_feature 列

Given 上传 50 行（49 有效 + 1 缺型号）
When 预校验
Then summary={total:50, valid:49, creates:20, errors:1}
And 错误明细列出第 50 行
And confirm 按钮 disabled

Given confirm 成功
Then /api/products/import-confirm 返回 imported/updated
And 批次 status=confirmed
And 审计 product.create × 20 + product.update × 29

Given 修改某产品 target_markets 由"沙特" →"沙特+阿联酋"
When 保存
Then 自动触发 PUT /api/products/:id/target-markets
And 预览"已新增 3 项阿联酋认证（SABER 等）"
```

## 7. 现状 vs v3 差距（量化）
- **缺失字段**：2 个（`target_markets[]`、`is_bio_feature`）
- **缺失接口**：1 个（`PUT /api/products/:id/target-markets`）
- **缺失状态机环节**：1 个（产品 target_markets 变更 → cert_templates 自动带出 → 触发 P10/V02 评审关联）
- **缺失审计 `action`**：`bulk_upsert` 不在全局枚举，需复合写 `create`/`update`
- **验收覆盖度**：现状 3 条 / v3 应有 5 条（差 2 条：target_markets/is_bio_feature 列展示、cert-templates 自动联动）

---

# 17. 产品新增/编辑

## 1. 页面元信息
- **编号 / 名称 / URL**：17 / 产品新增·编辑 / 模态弹窗（`/products` 表格行"编辑"触发）
- **主用角色 / 可见角色**：super_admin / **优先级 P1**
- **关联 BR 规则**：BR-PROD-04（产品长期负责人锁定）、BR-PROD-05（在售型号维护）、BR-PROD-06（退市独立）、BR-IPD-05b（target_markets 驱动认证清单）
- **现有实现**：**新增** — 当前仅 `/products` 表格只读展示，无 `<ProductEditModal>`

## 2. 页面布局（**应新增**）
- 触发：super_admin 在 `/products` 表格行右侧"⋯"→"编辑"
- 弹窗 `<ProductEditModal>`：
  - 顶栏：`Package` 图标 + "编辑产品档案" + 副标题"产品档案长期存在，姓名与产品型号由第三方人员主数据/Excel 导入锁定"
  - 字段：`model_code`（readOnly）/ `name` / `product_line` / `current_version` / `version_date` / `owner_market_pm_id`（market only）/ `lifecycle_status` / **`target_markets[]`**（多选 ISO 国家）/ **`is_bio_feature`**（checkbox）/ `notes`
  - **`target_markets` 变更后**：`POST /api/products/:id/cert-templates/derive` 触发预览 → toast"国别认证清单已重新带出"
  - 当 `lifecycle_status=retired`：附加"产品退市"按钮 → 跳转 `/api/products/:id/retirement`

## 3. 字段模型

| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---|---|---|---|---|
| `id` | UUID | 是 | — | URL | — | — |
| `model_code` | string | 是 | 唯一主键 | DB | 不可编辑 | 只读展示 |
| `name` | string | 是 | 1–120 | 输入 | 当前值 | 必填 |
| `product_line` | string | 否 | — | 输入 | 当前值 | — |
| `current_version` | string | 否 | — | 输入 | 当前值 | 正则 `[A-Z0-9.\-]+` |
| `version_date` | date | 否 | ISO date | 输入 | 当前值 | ≤ today+1d |
| `owner_market_pm_id` | UUID | 否 | market 角色 | `/api/users?role=market_pm` | 当前值 | 不允许改为非市场PM |
| `lifecycle_status` | enum | 是 | `draft` / `published` / `retired` | select | `published` | retired 必走退市 |
| `target_markets[]` | string[] | 是 | ISO 国家代码 | multi-select | 当前值 | v3 BR-IPD-05b |
| `isBioFeature` | boolean | 是 | — | checkbox | 当前值 | true 挂载 C12 |
| `notes` | text | 否 | ≤500 字 | 输入 | 当前值 | — |

## 4. API 接口清单
- **`GET /api/products/:id`**（**新增**）— 回填表单
  - 权限：super_admin 或项目 owner
- **`PUT /api/products/:id`**（**新增**）— 请求同字段表
  - 错误码：`30001` 非 super_admin / `40002` 双签未完成（owner 非市场PM）/ `50002` retired 未走退市流状态冲突
  - 审计：`entityType=product, action=update`，before/after
- **`POST /api/products/:id/cert-templates/derive`**（**新增**）— target_markets 变更后触发
  - 响应 `{ derived_items: [{ cert_item_id, applies_to, required: true/false }] }`
  - 审计：`entityType=product, action=update`（含 derive 子动作）
- **`POST /api/products/:id/retirement`** — 已存在

## 5. 闭环剧本（6 环节）
| 环节 | 内容 |
|---|---|
| ① 发起 | super_admin 在 `/products` 行点击"编辑" |
| ② 处理 | 弹窗打开 → 拉取最新 product → 渲染表单 → 修改字段 |
| ③ 审核 | 非 super_admin 不可编辑；retired 必须先走退市流 |
| ④ 结果 | 保存 → 列表 reload → toast"产品档案已更新" |
| ⑤ 记录 | 审计 `product.update`；target_markets 变更触发 cert_templates derive |
| ⑥ 归档 | 退市后需求/项目/资料统一归档；编辑历史可追溯 |

## 6. 验收用例（Given-When-Then，5 条）
```
Given super_admin
When 点击"编辑"某产品行
Then 弹出 ProductEditModal，model_code disabled
And 字段按当前档案回填；target_markets 多选已选中当前国别

When 修改 version 为 v2.0 并保存
Then 调用 PUT /api/products/:id
And target_markets 未变时跳过 cert-templates derive

Given 修改 target_markets 由"沙特" →"沙特+阿联酋"
When 保存
Then 自动 POST /api/products/:id/cert-templates/derive
And 预览"已新增 3 项阿联酋认证（SABER 等）"
And 审计 entityType=product, action=update

Given 非 super_admin 尝试通过 URL 直调 PUT
Then 返回 30001 "无权限"

Given 修改 lifecycle_status → retired 但未走退市流
Then 返回 50002 "状态冲突"
```

## 7. 现状 vs v3 差距（量化）
- **缺失字段**：2 个（`target_markets[]`、`is_bio_feature`）
- **缺失接口**：3 个（`GET /api/products/:id`、`PUT /api/products/:id`、`POST /api/products/:id/cert-templates/derive`）
- **缺失状态机环节**：2 个（target_markets 变更触发 cert-templates 联动；retired 强制走退市前置）
- **缺失 UI 组件**：1 个（`<ProductEditModal>`）
- **验收覆盖度**：现状 2 条 / v3 应有 5 条（差 3 条：target_markets 编辑、cert-templates 联动预览、retired 退市前置）

---

# 18. 国别认证清单模板库

## 1. 页面元信息
- **编号 / 名称 / URL**：18 / 国别认证清单模板库 / `/admin/cert-templates`（归属超级管理员 `/admin` 子页）
- **主用角色 / 可见角色**：super_admin（维护）/全部 product_manager（只读查阅）/ **优先级 P2**
- **关联 BR 规则**：BR-IPD-05b（国别 ↔ 认证项映射，按 target_markets 自动带出）、BR-IPD-05（P10/V02 法规认证保留为阻断性动作）、BR-PROD-07（产品档案 target_markets 驱动认证清单）
- **现有实现**：**完全缺失** — server.mjs 无 `cert_templates` / `cert_template_items` 表；`KeyGatePanel` 未引用认证清单；`App.jsx` 无对应路由；`/admin` 5 个 tab 不含 `cert`

## 2. 页面布局（**应新增**）
- 路由：`/admin/cert-templates`，归属 super_admin `/admin` 的第 6 个 tab
- 顶栏：`PageFrame title="国别认证清单模板库"`，副标题"维护国别对应的强制认证项；项目 target_markets 匹配后自动应用到 P10/V02 动作"
- 主体双栏 `.cert-grid`：
  - 左 `<CountryList>`：国别表（沙特 / 阿联酋 / 印度 / 欧盟 / 美国 / 中国 等），每行 ISO code / 中文名 / 当前版本 / 模板项数 / 操作（编辑 / 复制为新版本）
  - 右 `<TemplateEditor>`：选中国别后展示认证项 checklist — 表头：code / title / 类型（mandatory=否决 / recommended）/ 适用动作（P10 / V02 / both）/ 必填证据字段 / 顺序 — 行内操作：新增 / 删除 / 调顺序
- 顶部操作：新建国别 / 复制当前模板为新版本 / 导出 JSON
- **目标市场关联展示**：右侧底部嵌一段"本模板已应用于 X 个产品 target_markets"

## 3. 字段模型

| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---|---|---|---|---|
| `country.id` | UUID | 是 | — | DB | — | — |
| `country.code` | string | 是 | ISO 3166-1 | select | — | 唯一 |
| `country.name_zh` | string | 是 | 1–60 | input | — | — |
| `country.version` | int | 是 | ≥1 自增 | DB | 1 | 复制递增 |
| `country.effective_from` | date | 是 | ISO date | input | today | — |
| `item.code` | string | 是 | 模板内唯一 | input | — | 例 `SABER-SASO-IECEE` |
| `item.title` | string | 是 | 1–80 | input | — | — |
| `item.requirement_type` | enum | 是 | `mandatory` / `recommended` | select | `mandatory` | mandatory = 否决项 |
| `item.applies_to` | enum | 是 | `P10` / `V02` / `both` | select | `both` | — |
| `item.evidence_field` | enum | 否 | `cert_no` / `far_frr` / `passed_on` | select | `cert_no` | — |
| `item.order_index` | int | 否 | — | drag | — | — |

## 4. API 接口清单
- **`GET /api/admin/cert-templates`**（**新增**）— `{ countries: [{ id, code, name_zh, version, item_count, effective_from, applied_product_count }] }`
- **`GET /api/admin/cert-templates/:countryId`**（**新增**）— `{ country, items[] }`
- **`POST /api/admin/cert-templates/:countryId/copy`**（**新增**）— `{ effective_from }` → version+1，旧版保留
  - 审计：`entityType=system_config, action=archive`（模板旧版本归档）
- **`POST /api/admin/cert-templates/:countryId/items`**（**新增**）
  - 审计：`entityType=system_config, action=create`
- **`PUT /api/admin/cert-templates/:countryId/items/:itemId`**（**新增**）
  - 审计：`entityType=system_config, action=update`
- **`DELETE /api/admin/cert-templates/:countryId/items/:itemId`**（**新增**）— 软删除
  - 审计：`entityType=system_config, action=delete`
- **`GET /api/cert-templates/lookup?countryCode=...&actionCode=...`**（**新增**）— P10/V02 集成
  - 权限：受 auth 中间件保护
- **`POST /api/products/:id/cert-templates/derive`**（**新增**）— 产品 target_markets 变更后批量触发 derive
  - 审计：`entityType=product, action=update`（含 derive 子动作）

## 5. 闭环剧本（6 环节）
| 环节 | 内容 |
|---|---|
| ① 发起 | super_admin 进入 `/admin/cert-templates`；选国别；编辑认证项 |
| ② 处理 | 表单变更实时 local state；保存触发 POST/PUT/DELETE |
| ③ 审核 | 仅 super_admin 维护；变更不影响已 ARCHIVED 项目 |
| ④ 结果 | 生效后 P10/V02 动作调用 lookup 自动带出 |
| ⑤ 记录 | 每次写操作写 `audit_log`（entityType=system_config，action=create/update/delete/archive） |
| ⑥ 归档 | 模板旧版本保留；项目 P10/V02 历史结果通过 cert_no 永久关联 |

## 6. 验收用例（Given-When-Then，5 条）
```
Given super_admin
When 进入 /admin/cert-templates
Then 列出沙特/阿联酋/印度等国别，每行带版本号与项数
And 选中沙特后右栏展示 mandatory 认证项 SABER、IECEE 等

Given 项目 target_markets 含沙特且当前动作 P10
When rd_pm 进入 P10 动作
Then 表单"认证项"下拉自动带出沙特模板 mandatory 项
And 保存的 P10 完成记录关联 cert_template_item_id

Given 沙特模板需要更新认证项
When super_admin 复制为新版本 v2
Then v1 仍可查询（项目历史回溯）
And v2 自 effective_from 起对新项目生效
And 审计 entityType=system_config, action=archive

Given super_admin 删除某项认证
When 调用 DELETE
Then 软删除（is_deleted=true），旧项目仍可查
And 审计 entityType=system_config, action=delete
```

## 7. 现状 vs v3 差距（量化）
- **缺失字段**：6 个（`code`、`name_zh`、`version`、`effective_from`、`applies_to`、`evidence_field`）
- **缺失接口**：7 个（GET 列表 / GET 详情 / POST copy / POST/PUT/DELETE item / POST derive / GET lookup）
- **缺失状态机环节**：3 个（country 复制为新版本、P10/V02 lookup 自动带出、产品 target_markets 变更触发 derive）
- **缺失数据表**：2 张（`cert_templates`、`cert_template_items`）
- **缺失路由**：1 个（`/admin/cert-templates` 整体缺失）
- **缺失审计 `entityType`**：1 个（`system_config`）
- **验收覆盖度**：现状 0 条 / v3 应有 6 条
- **根因**：server.mjs 全文无 cert_* 路由；`/admin` 5 tab 无 cert 子页

---

# 19. 招标组队-招标单列表

## 1. 页面元信息
- **编号 / 名称 / URL**：19 / 招标组队-招标单列表 / `/recruitments`（对应 API `/api/bid-invitations`）
- **主用角色 / 可见角色**：market_pm（创建）/ rd_pm（应标）/ product_lead（确认）/ super_admin（治理）/ **优先级 P2**
- **关联 BR 规则**：BR-REC-01（密封应标）、BR-REC-02（双组长确认）、BR-REC-03（状态机 `OPEN`→`SELECTED`→`EXPIRED`/`CLOSED`）、BR-TEAM-04（招募条件版本控制）
- **现有实现**：**完整** — `App.jsx:594-602` 的 `RecruitmentsPage` + `FinalRulesPages.jsx:70-78` 的 `RecruitmentLifecyclePanel`

## 2. 页面布局（基于现有代码）
- 顶栏：`<PageFrame title="研发PM招募">`，副标题"市场PM先发布需求、研发PM密封应标，双组长依次确认后才可转为正式双PM项目"
- 右上：market_pm 或 super_admin 显示"+ 发起研发招募"
- 第一行 4 metric：进行中（OPEN）/ 待我应标 / 待转项目（SELECTED 待 approve）/ 已转项目
- 主体双栏 `.recruitment-layout`：
  - 左 `<RecruitmentList>`：状态 pill + 候选数 + 应标数
  - 右 `<RecruitmentDetail>`：hero + brief + flow（六阶段可视化）+ candidate-grid
  - 按状态分支：`OPEN` 邀请者出现应标编辑器；`OPEN`+market_pm+accept 出现"选定"；任意+pendingApproval 出现 approval-callout；ready_to_convert 出现补充经营目标表单
- 底部 `<RecruitmentLifecyclePanel>`：条件版本与到期处理

## 3. 字段模型

| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---|---|---|---|---|
| `bid_invitation.status` | enum | 是 | `OPEN`/`SELECTED`/`EXPIRED`/`CLOSED` | DB | `OPEN` | 状态机不可逆 |
| `bid_invitation.code` | string | 是 | 唯一 | DB | — | — |
| `bid_invitation.strategic_level` | enum | 是 | `S`/`A`/`B` | DB | `A` | — |
| `bid_invitation.deadline` | datetime | 是 | — | DB | now+7d | ≥ now（系统参数 `bid.expireWarnDays=3` 提醒） |
| `bid_response.solution_summary` | text | accept 必填 | ≥40 字 | input | — | — |
| `bid_response.estimated_days` | int | accept 必填 | 1–365 | input | 90 | — |
| `bid_response.resource_commitment` | text | accept 必填 | 1–200 字 | input | 模板 | — |
| `bid_response.major_risks` | text | accept 必填 | ≥20 字 | input | — | — |
| `bid_response.decision` | enum | 是 | `accept`/`reject` | input | `accept` | reject 不留痕 |
| `bid_response.reconfirm_required` | boolean | 否 | — | DB | false | 条件版本变更后 true |
| `select.reason` | text | 选定必填 | ≥10 字 | prompt | 模板 | — |
| `convert.project_code` | string | 转项目必填 | 唯一 | input | — | — |
| `convert.target_sales/channels/nps/scenarios` | int | 转项目必填 | ≥0 | input | 5000000/20/45/4 | — |

## 4. API 接口清单
- **`GET /api/bid-invitations`** — `{ bid_invitations: [...] }`
- **`GET /api/bid-invitations/:id`** — `{ ..., targets[], responses[], approvals[] }`
- **`POST /api/bid-invitations`**（创建）
- **`POST /api/bid-invitations/:id/publish`** — `OPEN`
  - 审计：`entityType=bid_invitation, action=submit`
- **`POST /api/bid-invitations/:id/responses`** — 应标 `{ decision, solution_summary, estimated_days, resource_commitment, major_risks }`
  - 错误码：`30001` 不在邀请名单 / `40001` 状态机非法（status≠OPEN）
  - 审计：`entityType=bid_response, action=accept`（reject 不留痕 → 不写 response 表，仅写 audit `bid_response, action=reject`）
- **`POST /api/bid-invitations/:id/select`** — `{ rd_pm_id, reason }`
  - 错误码：`30001` 非发起人 / `40002` 候选未 accept / `40001` 状态非 OPEN
  - 审计：`entityType=bid_invitation, action=select`
- **`POST /api/bid-invitations/:id/approvals`** — `{ decision, comment }`
  - 审计：`entityType=bid_invitation, action=approve|reject`
- **`PUT /api/bid-invitations/:id/terms`** — 条件版本变更 `v2+`
  - 审计：`entityType=bid_invitation, action=update`
- **`POST /api/bid-invitations/:id/close`** — `OPEN/SELECTED → CLOSED`
  - 审计：`entityType=bid_invitation, action=archive`
- 公共错误码：`40005` 阶梯系数匹配异常（strategic_level 校验）

## 5. 闭环剧本（6 环节）
| 环节 | 内容 |
|---|---|
| ① 发起 | market_pm 在 `/recruitments` 点击"+ 发起研发招募" |
| ② 处理 | `RecruitmentCreateModal` 填字段 + 选定向/公开 + 选 rd_pm 候选 → 创建草稿 |
| ③ 审核 | 草稿可修改；publish → 应标 → select → market_lead → rd_lead → convert_project |
| ④ 结果 | 每步更新 status 与 approvals；状态机不可逆；通知投递 |
| ⑤ 记录 | 审计写状态变更；密封应标版本号自增；reconfirm_required 标记 |
| ⑥ 归档 | CLOSED 后所有 response/version 永久保留；convert_project 后 `status=SELECTED + project_id`，仍可查 |

## 6. 验收用例（Given-When-Then，5 条）
```
Given market_pm
When 进入 /recruitments
Then "+ 发起研发招募"按钮可见
And 左侧清单按状态分组

Given 招募 status=OPEN 且当前是被邀请 rd_pm
When 提交 accept 应标
Then POST /api/bid-invitations/:id/responses with decision=accept
And 候选人卡片状态变"已应标"
And 审计 entityType=bid_response, action=accept

Given 招募 status=SELECTED 且当前是发起人
When 提交补充经营目标并转项目
Then POST /api/projects/from-recruitment
And 项目自动绑定招募中锁定的需求、rd_pm、双组长
And 审计 entityType=project, action=create

Given 招募已应标且发起人修改条件版本 v2
When PUT /api/bid-invitations/:id/terms
Then 已应标 response.reconfirm_required=true
And UI 提示该候选人需重新确认
```

## 7. 现状 vs v3 差距（量化）
- **缺失字段**：1 个（`response.reconfirm_required` 条件版本变更提示 UI 未透出）
- **缺失接口**：0（核心接口齐全，**待重命名为 `/api/bid-invitations/*`**）
- **缺失状态机环节**：1 个（terms 版本变更后已应标自动转 reconfirm_required=true）
- **缺失审计 `entityType`**：2 个（`bid_invitation`、`bid_response`，现状用 `recruitment` / `recruitment_bid`）
- **缺失审计 `action`**：`submit`、`select`、`approve`、`archive`（现状缺失枚举对齐）
- **缺失系统参数引用**：1 个（`bid.expireWarnDays=3`、`bid.selectDeadlineDays=7`、`bid.assignAfterDays=30`）
- **验收覆盖度**：现状 3 条 / v3 应有 5 条（差 2 条：reconfirm 提示、转项目后 status=SELECTED 仍可查）

---

# 20. 发起招标

## 1. 页面元信息
- **编号 / 名称 / URL**：20 / 发起招标 / `/recruitments` 弹窗 `<RecruitmentCreateModal>`（App.jsx:612-635）
- **主用角色 / 可见角色**：market_pm（创建）/ super_admin（代创建）/ **优先级 P2**
- **关联 BR 规则**：BR-REC-CREATE-01（market_pm 发起）、BR-REC-CREATE-02（一对一/一对多/全 rd_pm 公开）、BR-REC-CREATE-03（截止日期默认+7 天）
- **现有实现**：**完整**

## 2. 页面布局（基于现有代码）
- 触发：`/recruitments` 右上 `+ 发起研发招募`
- 弹窗 `<RecruitmentCreateModal>`：
  - 顶栏：Handshake 图标 + "发起研发PM招募" + 副标题"字段最小要求、候选状态与错误位置均明确展示"
  - 字段顺序：market_pm 发起人 → 招募模式（targeted/public）→ 新产品/需求名称 → 客户问题 → 应用场景 → 核心功能 → 产品类型（`HARDWARE`/`SOFTWARE`/`SOLUTION`）/目标上市/市场窗口/strategic_level → 截止日期 → 候选 rd_pm
  - 错误：每个字段下方 field-error；底部 form-error
  - 底部：取消 + 主按钮"创建招募草稿"

## 3. 字段模型

| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---|---|---|---|---|
| `market_pm_id` | UUID | 是 | 市场PM | `/api/projects/create-options` | 当前用户 | — |
| `recruitment_mode` | enum | 是 | `targeted`/`public` | radio | `targeted` | public 时 rd_pm_ids 强制空 |
| `title` | string | 是 | ≥4 字 | input | — | 长度 |
| `customer_problem` | text | 是 | ≥4 字 | textarea | — | 长度 |
| `application_scenario` | text | 是 | ≥4 字 | textarea | — | 长度 |
| `core_features` | text | 是 | ≥4 字 | textarea | — | 长度 |
| `product_type` | enum | 是 | `HARDWARE`/`SOFTWARE`/`SOLUTION` | select | `SOLUTION` | — |
| `target_launch_date` | date | 是 | ISO date | date | — | ≥today |
| `market_window` | string | 是 | 1–60 字 | input | — | 长度 |
| `strategic_level` | enum | 是 | `S`/`A`/`B` | select | `A` | — |
| `deadline` | date | 是 | ISO date | date | today+7 | ≥today（系统参数 `bid.expireWarnDays=3` 提醒） |
| `rd_pm_ids[]` | UUID[] | targeted 必填 | rd_pm | fieldset | — | ≥1 |

## 4. API 接口清单
- **`POST /api/bid-invitations`** — 请求体同字段表
  - 响应：`{ id, code, status: 'OPEN' }`
  - 错误码：`10001` 字段缺失 / `40001` 截止日期早于今天（deadline 校验）/ `30001` 非 market_pm
  - 审计：`entityType=bid_invitation, action=create`
- **`GET /api/projects/create-options`**（已存在）— `{ market_pms[], rd_pms[], stages[] }`

## 5. 闭环剧本（6 环节）
| 环节 | 内容 |
|---|---|
| ① 发起 | `/recruitments` 点击"+ 发起研发招募" |
| ② 处理 | 弹窗表单 → 实时校验 → 提交创建草稿 |
| ③ 审核 | 草稿无需审批 |
| ④ 结果 | 创建成功 toast；自动 load 新招募详情 |
| ⑤ 记录 | 审计 `bid_invitation.create`；`bid_invitation_targets` 批量插入 |
| ⑥ 归档 | 草稿可任意修改至发布前；发布后只能调整截止日期与关闭 |

## 6. 验收用例（Given-When-Then，5 条）
```
Given market_pm
When 点击"+ 发起研发招募"
Then 弹窗打开，market_pm 发起人字段自动锁定为自己
And 截止日期默认 today+7

Given 完整字段+至少1 rd_pm
When 点击"创建招募草稿"
Then POST /api/bid-invitations
And 弹窗关闭，跳转新招募详情
And status=OPEN

Given 字段未达最小长度
Then 按钮 disabled，字段下方显示红色 field-error

Given recruitment_mode=public
Then rd_pm_ids 字段 disabled，强制空
And 截止日期检查 deadline < today → 40001
```

## 7. 现状 vs v3 差距（量化）
- **缺失字段**：0
- **缺失接口**：0（**待迁移到 `/api/bid-invitations`**）
- **缺失状态机环节**：0
- **缺失审计 `entityType`**：1 个（`bid_invitation`，现状用 `recruitment`）
- **缺失审计 `action`**：`create` 替代现状默认 audit
- **缺失系统参数引用**：1 个（`bid.expireWarnDays=3`、`bid.selectDeadlineDays=7`、`bid.assignAfterDays=30`）
- **验收覆盖度**：现状 3 条 / v3 应有 4 条（差 1 条：public 模式下 rd_pm_ids 强制空校验）

---

# 21. 应标

## 1. 页面元信息
- **编号 / 名称 / URL**：21 / 应标 / `/recruitments` 详情内 `<form.bid-editor>`（App.jsx:602 段落）
- **主用角色 / 可见角色**：被邀请 rd_pm / **优先级 P2**
- **关联 BR 规则**：BR-REC-BID-01（密封应标）、BR-REC-BID-02（拒绝不留痕）、BR-REC-BID-03（同一 rd_pm 仅一份最新应标）、BR-TEAM-04（条件版本变更后 reconfirm_required）
- **现有实现**：**完整**

## 2. 页面布局（基于现有代码）
- 位置：右侧详情面板，仅当 `selected.status === "OPEN" && invited` 时出现
- `<form.bid-editor>`：H3"我的密封应标" + "其他候选人不可见" + 技术方案摘要 + 预计周期 + 资源投入 + 主要风险 + 拒绝/接受按钮

## 3. 字段模型

| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---|---|---|---|---|
| `decision` | enum | 是 | `accept`/`reject` | 按钮 | — | — |
| `solution_summary` | text | accept 必填 | ≥40 字 | textarea | — | — |
| `estimated_days` | int | accept 必填 | 1–365 | number | 90 | — |
| `resource_commitment` | text | accept 必填 | 1–200 字 | text | 模板 | — |
| `major_risks` | text | accept 必填 | ≥20 字 | textarea | — | — |
| `reconfirm_acknowledged` | boolean | reconfirm 时必填 | — | checkbox | false | 接受 v2 条件 |

## 4. API 接口清单
- **`POST /api/bid-invitations/:id/responses`** — 请求见字段表
  - 错误码：`30001` 不在邀请名单 / `40001` 状态机非法（status≠OPEN）/ `40002` reconfirm_required 未确认新条件
  - 审计：`entityType=bid_response, action=accept|reject`；reject 仅写 `audit_log`

## 5. 闭环剧本（6 环节）
| 环节 | 内容 |
|---|---|
| ① 发起 | rd_pm 进入 `/recruitments` 并选中被邀请招募 |
| ② 处理 | 填写方案/周期/资源/风险；点"接受"或"拒绝" |
| ③ 审核 | accept 时密封方案仅发起人可见；reject 不留痕 |
| ④ 结果 | accept → invitation_status='accept', responses 表写入；reject → invitation_status='reject' |
| ⑤ 记录 | 审计 `bid_response.accept` / 仅 audit 写 reject |
| ⑥ 归档 | 应标版本号自增；terms 版本变更后已应标自动标 `reconfirm_required=true` |

## 6. 验收用例（Given-When-Then，5 条）
```
Given 候选 rd_pm
When 填写方案摘要 ≥40 字、周期、资源、风险 →接受并提交
Then POST /api/bid-invitations/:id/responses with decision=accept
And 候选人卡片状态变"已应标"

Given 候选 rd_pm
When 点"拒绝"
Then POST /api/bid-invitations/:id/responses with decision=reject
And 候选人卡片状态变"已拒绝"
And bid_responses 表无任何方案字段记录

Given response.reconfirm_required=true
When 候选 rd_pm 进入详情
Then 编辑器顶部 banner"招募条件已变更，请重新确认后提交"
And "接受"按钮前置勾选"我已阅读 v2 条件"

Given 未勾选 reconfirm_acknowledged 但提交
Then 返回 40002 "reconfirm_required 未确认新条件"
```

## 7. 现状 vs v3 差距（量化）
- **缺失字段**：1 个（`reconfirm_acknowledged` reconfirm_required 时的二次确认勾选）
- **缺失接口**：0
- **缺失状态机环节**：1 个（terms 版本变更后 response 自动进入 reconfirm_required 状态）
- **缺失审计 `entityType`**：1 个（`bid_response`，现状用 `recruitment_bid`）
- **缺失审计 `action`**：`accept`、`reject`（现状使用 `submit` 不符合全局枚举）
- **验收覆盖度**：现状 2 条 / v3 应有 4 条（差 2 条：reconfirm banner、勾选确认）

---

# 22. 遴选

## 1. 页面元信息
- **编号 / 名称 / URL**：22 / 遴选 / `/recruitments` 详情 `<CandidateGrid>` 内候选人卡片
- **主用角色 / 可见角色**：发起人 market_pm（market_pm_id=current_user.id）/ **优先级 P2**
- **关联 BR 规则**：BR-REC-SEL-01（仅发起人）、BR-REC-SEL-02（未中选通知）、BR-REC-SEL-03（状态机 `OPEN`→`SELECTED`）
- **现有实现**：**完整**

## 2. 页面布局（基于现有代码）
- 位置：`<CandidateGrid>` 每张候选人卡 → `<SealedBid>` 右下"选定该研发PM"按钮
- 触发条件：`market_pm_id === currentUser.id && status === "OPEN" && response.decision === "accept"`
- `window.prompt("填写选择理由", "...")` → ≥10 字（**应改 `<Modal>` 以便审计可追溯**）
- 状态机：`OPEN` → `SELECTED`；生成 approvals 链（market_lead + rd_lead）

## 3. 字段模型

| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---|---|---|---|---|
| `rd_pm_id` | UUID | 是 | 目标候选人 | props | — | 必须对应 response |
| `reason` | text | 是 | ≥10 字 | `<Modal>` | 模板 | — |

## 4. API 接口清单
- **`POST /api/bid-invitations/:id/select`** — `{ rd_pm_id, reason }`；响应 `{ status: 'SELECTED', award_approval_id }`
  - 错误码：`30001` 非发起人 / `40002` 候选未 accept / `40001` 状态非 OPEN
  - 审计：`entityType=bid_invitation, action=select`
- **`POST /api/bid-invitations/:id/approvals`** — 双组长确认
  - 审计：`entityType=bid_invitation, action=approve|reject`

## 5. 闭环剧本（6 环节）
| 环节 | 内容 |
|---|---|
| ① 发起 | market_pm 在 `<CandidateGrid>` 点击"选定该研发PM" |
| ② 处理 | `<Modal>` 输入选择理由 ≥10 字 |
| ③ 审核 | 状态迁移 `OPEN` → `SELECTED`；自动生成 market_lead 审批节点 |
| ④ 结果 | 招募 status=SELECTED；中选 rd_pm 通知；未中选通知 |
| ⑤ 记录 | 审计 `bid_invitation.select` |
| ⑥ 归档 | 中选进入双组长确认；未中选 invitation_status 保留但不再激活 |

## 6. 验收用例（Given-When-Then，5 条）
```
Given market_pm + 招募 status=OPEN + 候选人 response.decision=accept
When 点击"选定该研发PM" + 理由 ≥10 字
Then POST /api/bid-invitations/:id/select
And 招募 status=SELECTED
And 其他候选人 invitation_status 不变但收"未中选"通知

Given 非发起人
Then 候选人卡片不显示"选定该研发PM"按钮

Given 输入理由 < 10 字
Then 按钮 disabled，弹窗字段下方显示红色 field-error
```

## 7. 现状 vs v3 差距（量化）
- **缺失字段**：0
- **缺失接口**：0
- **缺失状态机环节**：0
- **缺失审计 `entityType`**：1 个（`bid_invitation`，现状用 `recruitment`）
- **缺失审计 `action`**：`select`、`approve` 需对齐全局枚举
- **缺失 UI 组件**：1 个（`<Modal>` 替代 `window.prompt`，便于审计可追溯）
- **验收覆盖度**：现状 2 条 / v3 应有 3 条（差 1 条：reason `<Modal>` 替代 `prompt`）

---

# 23. 项目详情-Gate 评审

## 1. 页面元信息
- **编号 / 名称 / URL**：23 / 项目详情-Gate 评审 / `/reviews` + 嵌入式 `KeyGatePanel`（App.jsx:401）
- **主用角色 / 可见角色**：market_pm + rd_pm + product_lead + super_admin / **优先级 P2**
- **关联 BR 规则**：BR-GATE-01（五节点联合签署）、BR-GATE-02（强制附件）、BR-GATE-04（**`gate.signDeadlineDays=3`** + **`gate.signExtendMaxTimes=3`** + **`gate.reviewRoundEscalation=3`**）、BR-GATE-01b（33 项要素骨架）
- **现有实现**：**部分** — `KeyGatePanel`（FinalRulesPages.jsx:11-18）+ `DecisionChainPanel` 已实现五节点签署；**缺失3 天弃权倒计时、超期按主导方意见执行、super_admin 延长3 次**

## 2. 页面布局（基于现有代码，**应增强**）
- 顶栏：`<PageFrame title="阶段确认">`，副标题"用于确认一个 IPD 阶段是否具备进入下一阶段的条件，防止缺项、无证据或未决风险被带入后续工作"
- 右上：`<button className="primary-button">提交{stage.name}阶段确认</button>`（仅 can_edit_project 时）
- 主体双栏 `.review-layout`：
  - 左：阶段确认事项 + `<DecisionChainPanel>` 五节点链
  - 右：确认条件 checklist + 当前阶段状态卡
- 开发阶段追加 `<BiweeklyPanel>`
- **`<KeyGatePanel>` 增强**：
  - 五大门卡 header 增 `signDeadlineDays=3` 倒计时（如 status=`PENDING`，dueAt-now <=3d）
  - 超期态（status=`ABSTAINED_TIMEOUT`）：自动按主导方意见执行并显示 banner"已超期，按主导方意见自动通过"
  - super_admin 可点击"延长 3 天"，最多 `gate.signExtendMaxTimes=3` 次（计数 +审计）

## 3. 字段模型

| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---|---|---|---|---|
| `gate.id` | UUID | 是 | — | DB | — | — |
| `gate.title` | string | 是 | — | DB | — | — |
| `gate.stageCode` | enum | 是 | `concept`/`plan`/`develop`/`verify`/`launch` | DB | — | — |
| `gate.version` | int | 是 | ≥1 | DB | 1 | — |
| `gate.status` | enum | 是 | `DRAFT`/`PENDING`/`APPROVED`/`REJECTED`/`ABSTAINED_TIMEOUT` | DB | `DRAFT` | — |
| `vetoMode` | bool | 是 | — | DB | false | — |
| `gate.materials[]` | array | — | `{ id, material_type, original_name }` | DB | — | submit 前 2 类必备 |
| `gate.signatures[]` | array | — | `{ signer_role, signer_id, decision }` | DB | — | — |
| `gate.arbitrations[]` | array | — | `{ market_lead_id, market_decision, rd_lead_id, rd_decision }` | DB | — | — |
| `dueAt` | datetime | submit 时必填 | now + `gate.signDeadlineDays` (=3) | DB | now+3d | — |
| `gate.extensionCount` | int | — | 0–`gate.signExtendMaxTimes` (=3) | DB | 0 | >3 时 `40001` 状态机非法 |
| `gate.leadSide` | enum | 超期必填 | `market`/`rd` | 计算 | — | 超期按 leadSide 自动通过 |
| `gate.reviewRound` | int | — | 0–`gate.reviewRoundEscalation` (=3)，超级管理员升至 `gate.reviewRoundSuperAdmin` (=5) | DB | 0 | — |

## 4. API 接口清单
- **`GET /api/key-gates?projectId=...`**
- **`POST /api/key-gates/:id/materials`** — type=`minutes`/`review_material`
- **`POST /api/key-gates/:id/submit`** — 仅 market_pm；前置 2 类材料齐
  - 审计：`entityType=gate, action=submit`
- **`POST /api/key-gates/:id/sign`** — `{ decision, comment }`
  - 审计：`entityType=gate, action=sign`
- **`POST /api/key-gates/:id/arbitrate`** — 双组长仲裁
  - 审计：`entityType=gate, action=arbitrate`
- **`POST /api/key-gates/:id/reopen`** — REJECTED 后整改发起新版本
  - 审计：`entityType=gate, action=reassign`
- **`POST /api/reviews/:id/decision`** — 阶段评审单层决策
  - 审计：`entityType=gate, action=approve|reject`
- **新增（v3 BR-GATE-04）**：`POST /api/key-gates/:id/extend`** — super_admin 专用；body `{ reason }`；限制 extension_count < `gate.signExtendMaxTimes`(=3)
  - 错误码：`40001` 已达延长上限（超出 signExtendMaxTimes）
  - 审计：`entityType=gate, action=update`
- **新增（v3 BR-GATE-04）**：`POST /api/cron/auto-approve-overdue-gates`** — 定时任务入口；扫描 dueAt<now 且 status=`PENDING` 的 Gate，按 leadSide 自动通过 → status=`ABSTAINED_TIMEOUT`
  - 审计：`entityType=gate, action=approve`（含 auto 标记）
- 公共错误码：`40001` 阶段门禁未过 / `40002` 双签未完成 / `40004` 评级快照不一致

## 5. 闭环剧本（6 环节）
| 环节 | 内容 |
|---|---|
| ① 发起 | 双PM 完成本阶段 → market_pm 上传会议纪要+评审材料 → 点击"提交 Gate" |
| ② 处理 | 服务端校验材料齐全 → 创建五节点签名（market_pm→rd_pm→market_lead→rd_lead→super_admin）→ dueAt=now+`gate.signDeadlineDays`(3d) |
| ③ 审核 | 五节点依次收到待办；任意节点驳回 → status=`REJECTED`；双方组长结论不一致 → arbitrate；超期未签 → 定时任务按 leadSide 自动通过 → status=`ABSTAINED_TIMEOUT` |
| ④ 结果 | 全部 approved → status=`APPROVED`，触发阶段门禁解锁；rejected → 整改后 reopen |
| ⑤ 记录 | 每次操作写 `key_gate_signatures` / `arbitrations` + audit_log；超期自动通过写 audit `gate.approve（auto）` |
| ⑥ 归档 | 通过的 Gate 永久归档；vetoMode=true 双PM否决不可覆盖 |

## 6. 验收用例（Given-When-Then，5 条）
```
Given market_pm 完成概念阶段
When 进入 /reviews
Then KeyGatePanel 顶部展示"概念阶段 Gate 第1版"
And 两个上传位均为空

When 上传纪要 + 材料 + 提交 Gate
Then POST /api/key-gates/:id/submit
And dueAt=now+3d（= `gate.signDeadlineDays`）
And 五节点 mini-chain 显示 pending
And 审计 entityType=gate, action=submit

Given 当前节点签署后第4天仍 PENDING
When 定时任务触发
Then POST /api/cron/auto-approve-overdue-gates 自动通过
And leadSide=market 时按市场组长意见执行
And 状态 → ABSTAINED_TIMEOUT
And 审计 entityType=gate, action=approve（auto 标记）

Given super_admin 延长已达3 次（= `gate.signExtendMaxTimes`）
When 再点"延长 3 天"
Then 返回 40001 "已达 signExtendMaxTimes 延长上限"
```

## 7. 现状 vs v3 差距（量化）
- **缺失字段**：3 个（`dueAt`、`extensionCount`、`leadSide`）
- **缺失接口**：2 个（`POST /api/key-gates/:id/extend`、`POST /api/cron/auto-approve-overdue-gates`）
- **缺失状态机环节**：2 个（3 天弃权倒计时、超期按主导方意见自动通过 → `ABSTAINED_TIMEOUT`）
- **缺失定时任务**：1 个（cron 扫描 dueAt<now 且 PENDING）
- **缺失系统参数引用**：3 个（`gate.signDeadlineDays=3`、`gate.signExtendMaxTimes=3`、`gate.reviewRoundEscalation=3`、`gate.reviewRoundSuperAdmin=5`）
- **缺失审计 `entityType`**：2 个（`gate` 整单流转、`gate_element_result` 要素判定，现状未对齐；`gate_review` 已废弃并入 `gate`）
- **缺失审计 `action`**：`submit`、`sign`、`arbitrate`、`reassign`、`approve`
- **验收覆盖度**：现状 2 条 / v3 应有 6 条（差 4 条：3 天倒计时、超期自动通过、super_admin 延长 3 次、仲裁升级流程）

---

# 24. Gate 评审详情

## 1. 页面元信息
- **编号 / 名称 / URL**：24 / Gate 评审详情 / `/reviews/gate/:gateId`（独立抽屉/子页）+ `KeyGatePanel` 卡片 `Eye` 图标触发
- **主用角色 / 可见角色**：market_pm + rd_pm + product_lead + super_admin / **优先级 P2**
- **关联 BR 规则**：BR-GATE-01b（**33 项要素三态判定表**：G1 七项/5 否决 + G2 六项/4 否决 + G3 五项/0 否决 + G4 八项/3 否决 + G5 七项/2 否决 = **33 项/14 否决**）、BR-GATE-02（否决项硬阻断）、BR-GATE-03（双签互不可见）、BR-GATE-04（⚠️ 项必填责任人+关闭期限 + 遗留项自动生成待办）
- **现有实现**：**完全缺失 33 项要素判定表** — `KeyGatePanel` 仅显示五节点签署 + 双PM否决模式 + 仲裁；现状无 GateDetailDrawer，无要素判定表，无 ⚠️ 项必填责任人+关闭期限

## 2. 页面布局（**应新增**）
- 触发：`KeyGatePanel` 卡片右上 `Eye` 图标 → 打开 `<GateDetailDrawer>`
- 抽屉结构：
  - 顶栏：标题 + 阶段 + 第 N 版 + status pill（`DRAFT`/`PENDING`/`APPROVED`/`REJECTED`/`ABSTAINED_TIMEOUT`）+ 3 天倒计时（`gate.signDeadlineDays=3`）
  - 上半区：会议纪要 + 评审材料附件列表
  - **核心 — 33 项要素判定表** `<GateElementTable>`：
    - 序号 / 要素代码 / 要素名称 / 否决项标记 / **当前用户判定**（三态：`passed` / `conditional` / `failed`）/ 他人判定（互不可见）/ 证据编号 / 备注
    - **⚠️ `conditional` 项必须**填责任人与关闭期限（v3 BR-GATE-04）
    - **❌ 否决项 + `failed`**触发 →整体 Gate 硬阻断，"确认本节点"按钮置灰
  - 下半区：五节点 mini-chain（本人未签突出）
  - 底部：当前签署人"驳回 / 确认本节点"；仲裁态双组长仲裁按钮

## 3. 字段模型

| 字段名 | 类型 | 必填 | 取值/枚举 | 数据源 | 默认 | 校验 |
|---|---|---|---|---|---|---|
| `gate_review_element.code` | string | 是 | — | DB | — | G1-1..G1-7 / G2-1..G2-6 / G3-1..G3-5 / G4-1..G4-8 / G5-1..G5-7（共 **33**） |
| `gate_review_element.title` | string | 是 | — | DB | — | — |
| `gate_review_element.is_veto` | bool | 是 | — | DB | false | 否决项失败 →整体否决 |
| `gate_review_element.applies_to_gate` | enum | 是 | `G1`/`G2`/`G3`/`G4`/`G5` | DB | — | — |
| `gate_element_result.id` | UUID | 是 | — | DB | — | — |
| `gate_element_result.gate_id` | UUID | 是 | — | DB | — | — |
| `gate_element_result.element_code` | string | 是 | — | DB | — | — |
| `gate_element_result.judge_id` | UUID | 是 | — | session | — | 当前签署人 |
| `gate_element_result.decision` | enum | 是 | `pending`/`passed`/`conditional`/`failed` | select | `pending` | 三态 |
| `gate_element_result.responsible_user_id` | UUID | conditional 必填 | — | `/api/users?role=*` | — | ⚠️ 项必填责任人 |
| `gate_element_result.close_deadline` | date | conditional 必填 | ≥today | date | — | ⚠️ 项必填关闭期限 |
| `gate_element_result.evidence_note` | text | 否 | ≤500 | input | — | — |
| `gate_element_result.comment` | text | 否 | ≤500 | input | — | — |
| `dueAt` | datetime | — | — | DB | now+3d | — |
| `vetoMode` | bool | — | — | DB | false | — |
| `overallDecision` | enum | — | `passed`/`failed`/`conditional` | 计算 | — | 任何否决项 failed → failed |

## 4. API 接口清单
- **`GET /api/key-gates/:id/elements`**（**新增**）— `{ elements: [{ code, title, is_veto, my_judgment, others_judgments: [{ judge_role, decision }] }], dueAt, vetoMode, my_role, my_pending }`
  - 权限：受 auth 中间件保护
- **`POST /api/key-gates/:id/elements/:code/judge`**（**新增**）— `{ decision, responsible_user_id?, close_deadline?, evidence_note?, comment? }`
  - 错误码：`30001` 非当前签署人 / `40001` 已过 dueAt / `40002` ⚠️ 项缺责任人/关闭期限 / `40002` 否决项失败需 comment 必填 / `10003` decision 非法值
  - 审计：`entityType=gate_element_result, action=update`
- **`GET /api/key-gates/:id/elements/all`**（**新增**，super_admin 专属）— 返回全部签署人判定（双签互不可见仅对双组长生效，super_admin 可见全部）
- **`POST /api/key-gates/:id/elements/:code/legacy-todo`**（**新增**）— conditional 项关闭期限到达自动生成待办
  - 响应 `{ task_id }`
  - 审计：`entityType=gate_element_result, action=update`（含遗留项生成子动作）
- **`GET /api/key-gates/:id`**（已存在）
- **`POST /api/key-gates/:id/sign` / `/arbitrate` / `/reopen`**（已存在）
  - 审计分别：`gate.sign` / `gate.arbitrate` / `gate.reassign`

## 5A. 33 项 Gate 要素三态判定表（v3 BR-GATE-01b 严格落表）

> **数据源**：`IPD系统_五大Gate评审要素_v1.md`（v3 §3.6 详细展开）。本表是判定表的**单一事实源**，后续 §3 字段 / §4 API / §5 闭环剧本 / §6 验收用例 全部以此为准。

| 要素 | Gate | 通过标准 | 主答 | 否决项 | 必现页面 |
|---|---|---|---|---|---|
| G1-1 市场机会真实性 | G1 | ≥ 5 家目标客户一手验证；或 ≥ 1 家客户书面意向（二选一） | 市场PM | — | 24 |
| G1-2 市场规模与目标设定 | G1 | 自下而上推导（客户数×客单×渗透率）；或可比产品对标 | 市场PM | ❌ 四项基准值缺失 | 24 |
| G1-3 竞争格局与差异化 | G1 | 主要竞品 ≥ 3 家；差异点 ≥ 2 项且竞品 6 个月内难复制 | 市场PM | — | 24 |
| G1-4 技术可行性 | G1 | 预研结论"可行"或"有条件可行"+明确条件 | 研发PM | ❌ 结论"不可行" | 24 |
| G1-5 商业性 | G1 | 毛利率 ≥ 产品线门槛（`gate.grossMarginThreshold` 待补）；销量预测与产能匹配 | 市场PM | ❌ 毛利率为负 | 24 |
| G1-6 合规与知识产权 | G1 | 认证清单已确认且无不可逾越障碍；FTO 无高风险专利；Z03 合规审查已通过 | 市场PM+研发PM | ❌ Z03 合规审查未通过 / 认证存在不可逾越障碍 | 24 + 18 |
| G1-7 资源与组队 | G1 | 双PM 均已确认承接 | 产品组长 | ❌ 研发PM 未到位 | 24 |
| G2-1 PRD 完整性 | G2 | 需求条目化率 100%；每条含验收标准 | 市场PM | ❌ PRD 未上传 | 24 |
| G2-2 需求优先级与版本规划 | G2 | 首版 Must 需求可支撑核心场景闭环 | 市场PM | — | 24 |
| G2-3 差异化卖点可交付性 | G2 | 每个卖点有研发侧"可实现 + 成本增量"确认 | 研发PM | ❌ 存在卖点被判定为不可实现 | 24 |
| G2-4 价值定价与毛利复核 | G2 | 毛利率 ≥ 门槛（`gate.grossMarginThreshold` 待补） | 市场PM | ❌ 毛利率低于门槛且无定价调整方案 | 24 |
| G2-5 技术方案与里程碑 | G2 | 里程碑日期完整（EVT/DVT/PVT/GTM） | 研发PM | ❌ 里程碑日期缺失 | 24 |
| G2-6 认证与法规清单 | G2 | 目标市场强制认证全部列入且周期匹配 | 研发PM | ❌ 强制认证缺失或周期冲突 | 24 + 18 |
| G3-1 进度与里程碑 | G3 | 偏差 ≤ 5 工作日，或已制定追赶计划 | 研发PM | — | 24 |
| G3-2 场景完整度 | G3 | 每个核心场景端到端可走通（**市场PM 参与的核心理由**） | 市场PM | — | 24 |
| G3-3 需求变更情况 | G3 | 累计变更率 ≤ 15%（预警线 12%） | 市场PM | — | 24 |
| G3-4 技术风险与阻塞 | G3 | 无 P0 级阻塞；有则已升级并明确责任人 | 研发PM | — | 24 |
| G3-5 成本与合规跟踪 | G3 | 成本偏差 ≤ 5%；认证进度正常 | 研发PM | — | 24 |
| G4-1 产品就绪 | G4 | DVT/V02/V03/V06 全部通过；无 P0 缺陷 | 研发PM | ❌ V02 认证未通过 / V06 量产准入未通过 | 24 |
| G4-2 质量与缺陷 | G4 | P0 清零，P1 ≤ 3 且有 workaround | 研发PM | ❌ P0 未清零 | 24 |
| G4-3 供应与备货 | G4 | L05 完成；备货满足首批订单预测 | 研发PM | — | 24 |
| G4-4 价格与渠道体系 | G4 | L02 发布；目标渠道覆盖率 ≥ 60% | 市场PM | — | 24 |
| G4-5 销售工具与培训 | G4 | L03 齐备；核心渠道培训覆盖率 ≥ 80% | 市场PM | — | 24 |
| G4-6 本地化与合规落地 | G4 | V07 + Z05 逐项打勾；目标市场强制认证已取得 | 市场PM | ❌ 目标市场强制认证未取得 / Z05 未完成 | 24 |
| G4-7 售后与支持 | G4 | V08 已就绪；备件 + 服务话术 + 退换货政策 | 市场PM | — | 24 |
| G4-8 GTM 方案可执行性 | G4 | 首批目标客户名单 ≥ 10 家且已分配责任人 | 市场PM | — | 24 |
| G5-1 销售达成情况 | G5 | 90 天累计达成率 ≥ 25%（**预警线非否决项**） | 市场PM | — | 24 |
| G5-2 渠道与场景覆盖 | G5 | 渠道覆盖 ≥ 50% 目标；场景覆盖 ≥ 50% 目标 | 市场PM | — | 24 |
| G5-3 客户反馈与质量 | G5 | P0 问题 100% 闭环；NPS 达目标或已制定改进计划 | 市场PM | ❌ P0 问题未闭环 | 24 |
| G5-4 需求准确率复盘 | G5 | 变更率统计完成；根因已归类 | 市场PM | — | 24 |
| G5-5 上市准时性与窗口命中 | G5 | 偏差已计量并归因（≤15 天准时率 / ≤30 天窗口命中） | 市场PM+研发PM | — | 24 |
| G5-6 利润与成本复盘 | G5 | 偏差 ≤ 5 个百分点或已归因 | 市场PM | — | 24 |
| G5-7 迭代与生命周期决策 | G5 | 已形成明确决议（加速放量 / 迭代 / 限售 / 停产） | 双PM+产品组长 | ❌ 未形成决议 | 24 |

**否决项汇总（14 项）**：
- G1 否决 5 项：G1-2、G1-4、G1-5、G1-6（含 2 个 ❌ 子项：Z03 / 认证障碍）、G1-7
- G2 否决 4 项：G2-1、G2-3、G2-4、G2-5、G2-6（注：v3 文件记为 4 否决；含 G2-3 不可实现 + G2-6 强制认证缺失共 2 个核心）
- G3 否决 0 项（过程 Gate，连续 2 次 P0 阻塞自动上升组长，详见 D.6.4）
- G4 否决 3 项：G4-1（含 V02 / V06 两个子 ❌）、G4-2、G4-6（含强制认证 / Z05 两个子 ❌）
- G5 否决 2 项：G5-3、G5-7

**判定三态枚举**：`pending` / `passed`（✅）/ `conditional`（⚠️）/ `failed`（❌）

**双签机制**：
- G1 / G5：**双签否决**，任一方 ❌ 即驳回（v3 §3.6 头部"双签否决"列 ✅）
- G2 / G3 / G4：**流程门禁但非双签否决**——G3 是过程 Gate；G2 / G4 任一否决项 failed 触发硬阻断，但不进入"双签驳回"流程
- 14 项 ❌ 任一命中 → 整体 status=`REJECTED`，UI "确认本节点"按钮置灰
- ⚠️ 项（conditional）必填 `responsible_user_id` + `close_deadline`（≥ today），否则保存返回 `40002`
- 关闭期限到达自动生成待办 `POST /api/key-gates/:id/elements/:code/legacy-todo`

**字段映射**（§3 字段模型扩展）：
- `gate_review_element.code` ∈ {G1-1..G1-7, G2-1..G2-6, G3-1..G3-5, G4-1..G4-8, G5-1..G5-7}（33 项）
- `gate_review_element.is_veto` 按上表 ❌ 列置 true
- `gate_review_element.applies_to_gate` 锁 5 个 Gate 之一
- `gate_element_result.decision` 三态 = `passed` / `conditional` / `failed`
- `gate_element_result.responsible_user_id` + `close_deadline` ⇐ conditional 必填

**互不可见规则**（v3 BR-GATE-04）：
- `GET /api/key-gates/:id/elements` 服务端按 role 过滤 `my_judgment` / `others_judgments`
- 双签方（市场PM / 研发PM）仅在双方均 `signedAt IS NOT NULL` 时互相可见判定
- 产品组长 / super_admin 可见全部（仲裁场景）
- 任一方未签时，仅返回当前用户自己草稿，对方 decision 字段隐藏

---

## 5. 闭环剧本（6 环节）
| 环节 | 内容 |
|---|---|
| ① 发起 | 当前签署人在 `/reviews/gate/:id` 打开 33 项要素表 |
| ② 处理 | 逐项判定 `passed`/`conditional`/`failed`；conditional 必填责任人+关闭期限；否决项 failed 必填 comment |
| ③ 审核 | 全部判定后点"确认本节点" → 服务端校验：任何否决项 failed → 整体 status=`REJECTED`；所有项 passed 或 conditional → 进入下一节点 |
| ④ 结果 | 节点签名 + 通知下一节点；超期未签 → 自动 `ABSTAINED_TIMEOUT` |
| ⑤ 记录 | 每条 judgment 写入 `gate_element_result`；遗留项关闭期限到达自动生成待办并通知责任人 |

> ⚙️ 跨页守护（D.6.6 第 5 条）：Gate ⚠️ 项必填责任人+关闭期限（BR-GATE-01b）—— 24 Gate 评审详情页 `POST /api/key-gates/:id/elements/:code/judge` 服务端校验：decision=conditional 时 `responsible_user_id` + `close_deadline` 必填，缺一返回 `40002`；关闭期限到达 cron 触发 `legacy-todo` 生成待办并通知责任人。

#### API 层互不可见规则（v3 BR-GATE-04 关键实现）

| API | | 过滤逻辑 |
| `GET /api/gates/:id/reviews` | | 服务端按当前登录用户角色过滤：
| | | - market_pm：仅返回自己提交的 + 对方若 `signedAt IS NOT NULL` 才显示对方（已签署可见）
| | | - rd_pm：同上对称
| | | - market_lead / rd_lead：可见全部（仲裁用）
| | | - super_admin：可见全部
| | | - 双方都未提交时，仅返回当前用户自己的草稿（隐藏对方 decision 字段）
| `POST /api/gates/:id/reviews` | | 写入 decision + signedAt + opinion；触发下一节点签署任务 |
| 超期检测 | | 服务端 cron 每小时扫描 dueAt < now()；signedAt IS NULL → 状态转 ABSTAINED_TIMEOUT |
| ⑥ 归档 | Gate 通过后所有 judgment 永久保留；双签互不可见由查询时按 role 过滤 |

## 6. 验收用例（Given-When-Then，6 条）
```
Given 当前是第 2 节点 rd_pm
When 在 /reviews/gate/:id 抽屉打开 33 项要素表
Then 表格展示 33 行（G1-1..G1-7、G2-1..G2-6、G3-1..G3-5、G4-1..G4-8、G5-1..G5-7）共 **14 行为否决项**（G1=5 + G2=4 + G3=0 + G4=3 + G5=2）
And 仅展示自己的判定列；他人判定列为"他人判定（互不可见）"
And 右上显示 3 天倒计时（= `gate.signDeadlineDays`）

Given 判定否决项 G1-5（"技术可行性"）为 failed
When 点"确认本节点"
Then 提示"否决项判定失败将导致整体驳回"
And 保存后整门 status=REJECTED，进入整改
And 审计 entityType=gate_element_result, action=update

Given 判定 G2-1（"PRD 完整性"）为 conditional 但未填责任人
When 保存
Then 返回 40002 "⚠️ 项必须填写责任人与关闭期限"

Given 当前是 super_admin
When 进入 /reviews/gate/:id
Then 表格额外展示全部签署人的判定列（双签互不可见仅对双组长生效）

Given conditional 项关闭期限到达
When 定时任务触发
Then 自动 POST /api/key-gates/:id/elements/:code/legacy-todo
And 生成待办 + 通知责任人
And 审计 entityType=gate_element_result, action=update
```

## 7. 现状 vs v3 差距（**关键差距**，量化）
- **缺失字段**：11 个（`gate_review_element[].code/title/is_veto/applies_to_gate`、`gate_element_result.decision 三态扩展`、`responsible_user_id`、`close_deadline`、`evidence_note`、`comment`、`gate.overallDecision`）
- **缺失接口**：4 个（`GET /elements`、`POST /elements/:code/judge`、`GET /elements/all`、`POST /elements/:code/legacy-todo`）
- **缺失数据表**：2 张（`gate_review_elements` 33 项 + `gate_element_result` 判定 + 遗留项跟踪）
- **缺失状态机环节**：4 个（33 项要素 seed、⚠️ 项责任人+期限必填、否决项硬阻断、双签互不可见过滤）
- **缺失定时任务**：1 个（conditional 关闭期限到达自动生成待办）
- **缺失页面**：1 个（`<GateDetailDrawer>` 与路由 `/reviews/gate/:gateId`）
- **缺失系统参数引用**：3 个（`gate.signDeadlineDays`、`gate.signExtendMaxTimes`、`gate.reviewRoundEscalation`）
- **缺失审计 `entityType`**：1 个（`gate_element_result`）
- **缺失审计 `action`**：`update`（含遗留项生成子动作）
- **验收覆盖度**：现状 0 条 / v3 应有 8 条
- **根因**：`KeyGatePanel` 当前仅展示五节点签署 + 仲裁，**完全缺失 33 项要素判定表骨架**——这是 v3 BR-GATE-01b 的核心