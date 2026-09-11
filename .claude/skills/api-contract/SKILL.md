---
name: api-contract
description: 后端修改 controller 后，自动生成 OpenAPI 增量 diff + 列出受影响的接口与字段 + 输出给前端（ruoyi-web / ruoyi-admin）的变更通知草稿。封装"改一个接口要通知几个前端仓库"这件重复劳动。
disable-model-invocation: true
---

# api-contract

RuoYi-AI 后端改动后端接口时的契约同步工具。

## 何时使用

- 新增 / 修改 / 删除 controller 里的 endpoint
- 改 request / response DTO（VO / BO / Form）
- 改枚举或错误码（前端要联动）
- 调整 OpenAPI 分组（springdoc.group-configs）

调用方式：用户明确说"改 API 了，生成变更通知" / "同步前端" 时使用。

## 输入约定

调用前请提供：

1. **改动范围**：本次提交涉及的所有 controller / DTO 文件相对路径。
2. **前端仓库**（可选）：默认 `ruoyi-web` + `ruoyi-admin`，可指定子集。
3. **发布目标版本**（可选）：用于变更日志 commit message。

## 工作流程

### 第 1 步：定位改动

```bash
# 用户提供路径后扫描
git diff --name-only HEAD~1 -- 'ruoyi-*/src/main/java/**/controller/**' 'ruoyi-*/src/main/java/**/domain/**'
```

### 第 2 步：抽取接口契约

对每个改动文件，按以下结构提取：

```yaml
- method: GET | POST | PUT | DELETE | PATCH
  path: /system/user/list
  group: 3.系统模块   # springdoc group
  auth: SaCheckLogin + SaCheckPermission("system:user:list")
  request:
    type: SysUserQuery (Form)
    fields: [...]
  response:
    type: TableDataInfo<SysUserVo>
    fields: [...]
  deprecated: false
  since: 3.1.0
```

抽取方法：

- 路径注解（`@GetMapping` / `@PostMapping` + 值）
- 方法签名（参数 + 返回类型）
- `@SaCheck*` 注解 → auth
- `@Valid` + `@NotNull` 等 → request 必填校验
- `@Tag`（springdoc）/ `@Operation` → 描述

### 第 3 步：对比旧版本（推荐）

```bash
# 启动 springdoc 静态导出（见下方），对比 v(N-1) 与 v(N) 的 openapi.json
diff <(jq '.paths | keys[]' openapi-v3.0.0.json | sort) \
     <(jq '.paths | keys[]' openapi-v3.1.0.json | sort)
```

### 第 4 步：分类输出

按 BREAKING / NEW / CHANGE 三类输出：

- **BREAKING**（前端必须改）：
  - 删除 endpoint
  - 删除或重命名字段
  - 字段类型变更（如 `Long` → `String`）
  - 必填字段从可选变必填
  - 枚举值移除
  - 路径变更

- **NEW**（新增，前端可选接入）：
  - 新 endpoint
  - 新可选字段
  - 新枚举值

- **CHANGE**（行为变更，需对齐）：
  - 字段语义变化（注释里说明）
  - 默认值变更
  - 排序 / 分页规则调整
  - 鉴权要求变化

## 输出格式

```markdown
## API 契约变更通知（v3.0.0 → v3.1.0）

**改动范围**: 12 个 controller，8 个 DTO
**生成时间**: 2026-09-04

### ⛔ BREAKING（前端必须改）

#### 1. `/system/user/list` 请求参数变更
- **删除字段**: `deptId` → 改用 `deptIds: List<Long>` 替代
- **影响前端**:
  - `ruoyi-web/src/api/system/user.ts` 的 `listUser` 调用
  - `ruoyi-admin/src/api/system/user.ts` 的 `listUser` 调用
- **迁移代码**:
  ```typescript
  // 旧
  const data = await listUser({ deptId: 5 });
  // 新
  const data = await listUser({ deptIds: [5] });
  ```

#### 2. `/chat/send` 响应体字段重命名
- `tokenUsage.totalTokens` → `tokenUsage.total`
- ...

### 🆕 NEW（前端可选接入）

#### 1. `/chat/agent/{agentId}/invoke` 新增
- POST 接口，参数：`{ input: string, sessionId?: string }`
- 响应：`{ taskId: string, status: 'PENDING' | 'RUNNING' | 'DONE' }`
- 鉴权：`SaCheckPermission("chat:agent:invoke")`
- 前端建议：在 `ruoyi-web/src/views/chat/` 加 "异步任务" 入口

### 🔄 CHANGE（行为对齐）

#### 1. `/workflow/run` 默认并发数从 5 改为 10
- ...
```

## 工具与命令

### 导出 OpenAPI 静态 JSON（可选）

在 `application.yml` 加（dev 环境）：

```yaml
springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
```

启动应用后：

```bash
curl http://localhost:6039/v3/api-docs > openapi-current.json
```

按 group 分别拉：

```bash
for group in 1.演示模块 2.通用模块 3.系统模块 4.代码生成模块 5.工作流模块 6.MCP模块; do
  encoded=$(echo -n "$group" | jq -sRr @uri)
  curl "http://localhost:6039/v3/api-docs/$encoded" > "openapi-${group}.json"
done
```

### 解析 Java 注解（推荐用 jqr）

```bash
# 列出某个 controller 的所有 endpoint
grep -E "@(Get|Post|Put|Delete|Patch)Mapping" \
  ruoyi-system/src/main/java/org/ruoyi/system/controller/SysUserController.java
```

### 对比 OpenAPI diff

```bash
# 仅看 path 差异
diff <(jq -S '.paths | keys[]' old.json | sort) \
     <(jq -S '.paths | keys[]' new.json | sort)

# 看字段级 schema 变化
diff <(jq -S '.components.schemas' old.json) \
     <(jq -S '.components.schemas' new.json)
```

## 跨仓库通知草稿

调用完成后自动产出可粘贴到 GitHub Issue / 飞书 / 邮件的草稿：

```markdown
## 【后端 API 变更】v3.1.0 → v3.2.0

### 影响范围
- BREAKING: 3 处
- NEW: 5 处
- CHANGE: 2 处

### 详细变更
（接上面分类输出）

### 必读仓库
- [ ] ruoyi-web
- [ ] ruoyi-admin
- [ ] ruoyi-uniapp（如有变动）
- [ ] ruoyi-copilot（如有变动）

### 预计发布时间
TBD（请回复确认）
```

## 禁止清单

- ❌ 跳过 BREAKING 章节直接列 NEW（前端会漏改导致线上故障）。
- ❌ 把非契约变更（如纯内部 service 重构）混入通知。
- ❌ 不带 diff 直接发通知（前端无法评估影响面）。
- ❌ 改动未经用户确认就推送飞书 / GitHub。

## 输出交付物

调用完成后必须产出：

1. 完整的 API 变更分类报告（BREAKING / NEW / CHANGE）。
2. 影响的前端调用点列表（含文件路径）。
3. 跨仓库通知草稿（Markdown，可直接粘贴）。
4. 建议的发布顺序（如先开后端还是先后端）。