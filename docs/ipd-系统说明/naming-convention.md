# 命名约定

> 本文件是**新增**的工作手册。规定新建 IPD 业务代码的命名风格，确保本仓库仓库的 IPD 改造与开发说明书 + RuoYi-AI 基线共存而不冲突。

---

## 一、原则

| 维度 | 决策 | 理由 |
|---|---|---|
| **已存在表** | **不改**（包括字段名、索引、约束） | 改基线表会破坏 RuoYi-AI 功能 |
| **新建表 / 新字段** | 严格遵循开发说明书（英文原名） | 与 v3 一致；避免命名混乱 |
| **新 controller** | `/api/v1/...` 前缀（开发说明书 §9.2 要求） | 与老 RuoYi controller 分离 |
| **新响应类** | `ApiV1Response<T>`（不与 `R<T>` 混用） | code=0 成功 vs 老 code=200 |
| **Java 包名** | `org.ruoyi.admin.controller.api.v1.*`（ruoyi-admin 子包） | 避免污染 ruoyi-system / ruoyi-chat 老代码 |
| **Java 类名** | 业务名 + 后缀（`Controller` / `Service` / `Bo` / `Vo`） | 与 RuoYi-AI 一致 |

---

## 二、表命名（开发说明书原文）

### 2.1 组织与人员

| 表名 | Java Entity | Java Mapper |
|---|---|---|
| `persons` | `Person` | `PersonMapper` |
| `product_groups` | `ProductGroup` | `ProductGroupMapper` |
| `project_members` | `ProjectMember` | `ProjectMemberMapper` |

### 2.2 产品与项目

| 表名 | Java Entity |
|---|---|
| `products` | `Product` |
| `projects` | `Project` |
| `project_stages` | `ProjectStage` |

### 2.3 IPD 与动作

| 表名 | Java Entity |
|---|---|
| `stage_actions` | `StageAction` |
| `deliverables` | `Deliverable` |
| `sop_templates` | `SopTemplate` |

### 2.4 Gate（⭐ v3 新增）

| 表名 | Java Entity |
|---|---|
| `gate_review_elements` | `GateReviewElement` |
| `gate_element_results` | `GateElementResult` |
| `gates` | `Gate` |
| `gate_reviews` | `GateReview` |

### 2.5 招标与需求

| 表名 | Java Entity |
|---|---|
| `bid_invitations` | `BidInvitation` |
| `bid_responses` | `BidResponse` |
| `requirements` | `Requirement` |
| `requirement_changes` | `RequirementChange` |

### 2.6 审核与审计

| 表名 | Java Entity |
|---|---|
| `deletion_requests` | `DeletionRequest` |
| `audit_logs` | `AuditLog` |

### 2.7 激励

| 表名 | Java Entity |
|---|---|
| `kpi_records` | `KpiRecord` |
| `allowance_ledgers` | `AllowanceLedger` |
| `bonus_pools` | `BonusPool` |

### 2.8 配置与 AI

| 表名 | Java Entity |
|---|---|
| `system_configs` | `SystemConfig` |
| `ai_documents` | `AiDocument` |

### 2.9 移交 + 模板

| 表名 | Java Entity |
|---|---|
| `handover_records` | `HandoverRecord` |
| `cert_templates` | `CertTemplate` |

---

## 三、字段命名

### 3.1 snake_case（数据库列名）

| 场景 | 命名 | 示例 |
|---|---|---|
| 业务主键 | `id` | `id` |
| 外键 | `<entity>_id` | `project_id`、`user_id` |
| 时间字段 | `<action>_time` 或 `<action>_at` | `create_time`、`submitted_at` |
| 业务截止 | `deadline` | `gate.submission_deadline` |
| 系统计算时间戳 | `due_at` | `gate.sign_due_at` |
| 状态枚举 | `status`（用字典值，不用 enum） | `project.status = 'DRAFT'` |
| 金额字段 | `<noun>` (DECIMAL(18,2)) | `bonus_pool`、`sales_target` |
| 比例字段 | `<noun>_<ratio>` | `contribution_ratio` |
| 布尔字段 | `is_<adj>` 或 `<verb>_ed` | `is_active`、`is_blocking` |
| 责任人 | `assignee_id` / `responsible_id` | — |
| 创建/更新人 | `create_by` / `update_by`（沿用 RuoYi-AI） | — |
| 删除标志 | `del_flag`（沿用 RuoYi-AI） | `0` 正常 / `1` 删除 |
| 备注 | `remark` | `VARCHAR(500)` |

### 3.2 Java 字段（camelCase，MyBatis-Plus 自动映射）

| snake_case | Java camelCase |
|---|---|
| `project_id` | `projectId` |
| `create_time` | `createTime` |
| `bonus_pool` | `bonusPool` |
| `is_blocking` | `isBlocking` |
| `due_at` | `dueAt` |
| `del_flag` | `delFlag` |

### 3.3 业务字段命名（开发说明书强制）

来自开发说明书 `_公共规范.md §八`：

- **`deadline`**：业务级截止（如提交评审意见、招标截止）
- **`dueAt`**：动作级时间戳（如签署到期，按 `gate.signDeadlineDays` 计算）
- **`revisionId`**：SOP 版本号字段（**不混用** `work_items.revision`）
- **`demand`**：游客通过需求门户提报的外部需求（code 形如 `D-2026-XXX`）
- **`requirement`**：内部立项后的需求单
- **`guest_demand`**：审计日志 `entityType`（专指游客需求归档后写入审计）

---

## 四、Java 包结构

### 4.1 新建 IPD 业务（ruoyi-admin 子包）

```
org.ruoyi.admin.controller.api.v1
├── ProjectController          # /api/v1/projects
├── StageActionController      # /api/v1/stage-actions
├── GateReviewController       # /api/v1/gates/:id/reviews
├── BidInvitationController    # /api/v1/bid-invitations
├── BidResponseController      # /api/v1/bid-invitations/:id/respond
├── HandoverController         # /api/v1/handovers
├── RequirementController      # /api/v1/requirements
├── DeletionRequestController  # /api/v1/deletion-requests
├── AuditLogController         # /api/v1/audit-logs
├── BonusPoolController        # /api/v1/bonus/pools
├── AllowanceController        # /api/v1/allowance/ledgers
└── AdminHandoverController    # /api/v1/admin/handover

org.ruoyi.admin.service.api.v1
├── IProjectService
├── ProjectServiceImpl
└── ...

org.ruoyi.admin.domain.api.v1
├── Project
├── ProjectBo
├── ProjectVo
└── ...

org.ruoyi.admin.mapper.api.v1
├── ProjectMapper
└── ...

org.ruoyi.admin.config.api.v1
├── ApiV1WebMvcConfig           # 注册 /api/v1/* 拦截器
└── ApiV1ErrorHandler           # 全局异常 → ApiV1Response 错误码
```

### 4.2 复用 RuoYi 现有包

```
org.ruoyi.common.*            # 沿用（27 个子模块）
org.ruoyi.system.*            # 沿用（RBAC）
org.ruoyi.chat.*              # 沿用（AI 核心）
```

**注意**：不在 RuoYi 现有包里加 IPD 业务代码，避免污染。**新增一律放在 `ruoyi-admin` 的 `api.v1.*` 子包下**。

---

## 五、API 路径命名

### 5.1 路径前缀（新接口必须）

```
/api/v1/<resource>            # 资源列表 / 创建
/api/v1/<resource>/:id        # 单个资源操作
/api/v1/<resource>/:id/<action>  # 资源动作
```

### 5.2 资源路径清单（开发说明书 §9.2）

| 方法 | 路径 | 用途 | 命名约定 |
|---|---|---|---|
| GET / POST | `/api/v1/projects` | 项目列表/创建 | 复数 |
| POST | `/api/v1/projects/:id/legacy-import` | 存量导入 | kebab-case |
| GET | `/api/v1/projects/:id/stages` | 阶段列表 | kebab-case |
| PATCH | `/api/v1/stage-actions/:id` | 更新动作 | kebab-case |
| POST | `/api/v1/stage-actions/:id/deliverables` | 上传交付物 | kebab-case |
| POST | `/api/v1/projects/:id/stage-advance` | 阶段跳转 | kebab-case |
| GET | `/api/v1/projects/:id/gates` | Gate 列表 | — |
| POST | `/api/v1/gates/:id/reviews` | 提交 Gate 评审 | kebab-case |
| POST | `/api/v1/gates/:id/arbitrate` | 升级仲裁 | — |
| POST | `/api/v1/bid-invitations` | 发起招标 | kebab-case |
| POST | `/api/v1/bid-invitations/:id/respond` | 应标 | — |
| POST | `/api/v1/bid-invitations/:id/select` | 遴选 | — |
| POST | `/api/v1/handovers` | 项目移交 | — |
| GET / POST | `/api/v1/kpi/...` | KPI | — |
| GET | `/api/v1/allowance/ledgers` | 津贴台账 | — |
| POST | `/api/v1/bonus/calculate` | 奖金池核算 | — |
| GET | `/api/v1/bonus/pools/:id` | 奖金池详情 | — |
| POST | `/api/v1/requirements` | 游客提交（免鉴权） | — |
| GET | `/api/v1/requirements/track/:code` | 游客查询 | — |
| POST | `/api/v1/deletion-requests` | 发起删除申请 | — |
| POST | `/api/v1/deletion-requests/:id/review` | 审核 | — |
| GET | `/api/v1/audit-logs` | 分层查询 | — |
| GET | `/api/v1/audit-logs/verify` | hash 链校验 | — |
| POST | `/api/v1/admin/handover` | 超管权限移交 | — |

### 5.3 集成接口

```
POST /api/v1/integration/hr/sync          # HR 人员同步（一期 Mock）
GET  /api/v1/auth/wecom/url              # 企微 OAuth（一期 Mock）
GET  /api/v1/auth/wecom/callback
POST /api/v1/auth/wecom/bind
POST /api/v1/auth/wecom/unbind
POST /api/v1/ai/generate                 # AI 调用
```

---

## 六、Springdoc OpenAPI 注解

### 6.1 通用注解

```java
@ApiV1RestController             // 项目自定义组合注解
@RequestMapping("/api/v1/projects")
public class ProjectController {

    @Operation(summary = "项目列表", tags = {"IPD · 项目"})
    @GetMapping
    public ApiV1Response<TableDataInfo<ProjectVo>> list(@Valid ProjectQuery query) {
        // ...
    }

    @Operation(summary = "新建项目（触发招标）", tags = {"IPD · 项目"})
    @Parameters({
        @Parameter(name = "templateType", description = "HARDWARE / SOFTWARE / SOLUTION"),
        @Parameter(name = "targetMarkets", description = "目标市场（多选，自动带出国别认证）")
    })
    @PostMapping
    public ApiV1Response<ProjectVo> create(@Valid @RequestBody ProjectBo bo) {
        // ...
    }
}
```

### 6.2 错误响应示例

```java
@ApiResponses({
    @ApiResponse(responseCode = "0", description = "成功"),
    @ApiResponse(responseCode = "40001", description = "业务规则阻断（如：阶段门禁未过）"),
    @ApiResponse(responseCode = "50001", description = "资源不存在"),
    @ApiResponse(responseCode = "90001", description = "系统内部错误")
})
```

---

## 七、Controller / Service 命名

### 7.1 Controller

| 类型 | 命名 | 路径 |
|---|---|---|
| 资源 CRUD | `<Resource>Controller` | `/api/v1/<resource>` |
| 业务动作 | `<Resource><Action>Controller` | `/api/v1/<resource>/<action>` |
| 集成接口 | `Integration<Source>Controller` | `/api/v1/integration/<source>` |
| 公共查询 | `<Domain>QueryController` | `/api/v1/<domain>/...` |

### 7.2 Service

| 类型 | 命名 |
|---|---|
| 接口 | `I<Domain>Service` |
| 实现 | `<Domain>ServiceImpl` |
| 工厂 | `<Domain>Factory` |
| Provider | `<Domain>Provider` |

### 7.3 BO / VO / DTO

| 类型 | 命名 | 用途 |
|---|---|---|
| `Bo` | `<Action><Domain>Bo` | 业务对象（请求参数） |
| `Vo` | `<Domain>Vo` | 视图对象（响应数据） |
| `Dto` | `<Domain>Dto` | 数据传输对象（层间传递） |
| `Query` | `<Domain>Query` | 查询参数 |

**示例**：
- `ProjectCreateBo` — 创建项目请求
- `ProjectVo` — 项目详情响应
- `StageActionQuery` — 阶段动作查询参数
- `GateReviewSubmitDto` — Gate 评审提交数据

---

## 八、错误响应（不删除现有 R<T>）

### 8.1 ApiV1Response 类（新建）

```java
@Data
public class ApiV1Response<T> {
    private int code;           // 0 = 成功
    private String message;     // 错误信息
    private T data;             // 响应数据
    private String timestamp;    // ISO8601 时间戳
    private String traceId;      // 链路追踪 ID

    public static <T> ApiV1Response<T> ok(T data) {
        ApiV1Response<T> r = new ApiV1Response<>();
        r.setCode(0);
        r.setMessage("ok");
        r.setData(data);
        r.setTimestamp(Instant.now().toString());
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    public static <T> ApiV1Response<T> error(int code, String message) {
        ApiV1Response<T> r = new ApiV1Response<>();
        r.setCode(code);
        r.setMessage(message);
        r.setTimestamp(Instant.now().toString());
        return r;
    }
}
```

**位置**：`ruoyi-admin/src/main/java/org/ruoyi/admin/domain/api/v1/ApiV1Response.java`（新建，不污染 RuoYi-AI 现有 `R.java`）

### 8.2 错误码段（新建 enum）

```java
public enum ApiErrorCode {
    SUCCESS(0, "ok"),

    // 1xxxx: 参数校验错误
    PARAM_INVALID(10001, "参数无效"),
    PARAM_MISSING(10002, "必填参数缺失"),

    // 2xxxx: 认证错误
    AUTH_UNAUTHORIZED(20001, "未登录"),
    AUTH_TOKEN_EXPIRED(20002, "Token 过期"),

    // 3xxxx: 权限不足
    PERM_DENIED(30001, "权限不足"),

    // 4xxxx: 业务规则阻断（核心）
    BIZ_GATE_BLOCKED(40001, "阶段门禁未通过"),
    BIZ_DUAL_SIGN_PENDING(40002, "等待对方签署"),
    BIZ_PROJECT_OVER_LIMIT(40003, "项目数超限"),

    // 5xxxx: 资源不存在
    RESOURCE_NOT_FOUND(50001, "资源不存在"),
    RESOURCE_CONFLICT(50002, "资源状态冲突"),

    // 9xxxx: 系统内部错误
    SYS_INTERNAL_ERROR(90001, "系统内部错误");

    private final int code;
    private final String defaultMessage;

    public int getCode() { return code; }
    public String getDefaultMessage() { return defaultMessage; }
}
```

**位置**：`ruoyi-admin/src/main/java/org/ruoyi/admin/enums/ApiErrorCode.java`（新建）

---

## 九、与 RuoYi-AI 基线的边界

| 维度 | RuoYi-AI 现状 | 新 IPD 代码 |
|---|---|---|
| 包路径 | `org.ruoyi.*`（已有） | `org.ruoyi.admin.controller.api.v1.*`（新建子包） |
| Controller | `@RestController` | `@ApiV1RestController`（自定义组合注解） |
| 响应 | `R<T>` | `ApiV1Response<T>` |
| 路径 | `/system/xxx`、`/chat/send` | `/api/v1/...` |
| Service 接口 | `IXxxService` | `IXxxService`（**沿用**） |
| Service 实现 | `XxxServiceImpl` | `XxxServiceImpl`（**沿用**） |
| Mapper | `XxxMapper extends BaseMapper<Xxx>` | **沿用** |
| 错误码段 | 业务码（不固定段） | 1xxxx / 2xxxx / 3xxxx / 4xxxx / 5xxxx / 9xxxx |

---

## 十、变更日志

| 日期 | 变更 |
|---|---|
| 2026-09-04 | 初始版（Claude Code 自动生成） |

---

**最后更新**：2026-09-04