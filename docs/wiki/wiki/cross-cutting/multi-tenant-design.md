---
topic: cross-cutting/multi-tenant-design
title: 多租户隔离设计
updated: 2026-09-04
raw:
  - raw/project-skeleton/application-yml.md
  - raw/common-source/mybatis-plus-config.md
  - raw/system-source/entity-sys-user.md
  - raw/chat-source/mcp-tool-provider-service.md
---

# 多租户隔离设计

RuoYi-AI **默认开启多租户**（`tenant.enable=true`），所有业务表自动加 `tenant_id` 过滤条件。本节讲解实现机制与边界。

## 总览

```
┌────────────────────────────────────────┐
│  HTTP 请求                              │
│  Header: Authorization = Sa-Token     │
└──────────────┬─────────────────────────┘
               │
               ▼
┌────────────────────────────────────────┐
│  Sa-Token 拦截器                        │
│  - 解析 token →  StpUtil.getLoginId()   │
│  - 提取 tenant_id → TenantHelper.set()  │
└──────────────┬─────────────────────────┘
               │
               ▼
┌────────────────────────────────────────┐
│  MyBatis-Plus 多租户拦截器              │
│  - 自动在 SQL 加 WHERE tenant_id = ?   │
│  - tenant.excludes 白名单跳过          │
└──────────────┬─────────────────────────┘
               │
               ▼
┌────────────────────────────────────────┐
│  MySQL（按 tenant_id 物理隔离逻辑）     │
└────────────────────────────────────────┘
```

## 关键配置

参见 [application-yml.md § tenant](../raw/project-skeleton/application-yml.md)：

```yaml
tenant:
  enable: true
  excludes:
    - sys_menu              # 菜单跨租户共享
    - sys_tenant            # 租户表本身
    - sys_tenant_package
    - sys_role_dept
    - sys_role_menu
    - sys_user_post
    - sys_user_role
    - sys_client
    - sys_oss_config
    - flow_spel             # Warm-Flow 表达式
    - trace_run             # 链路追踪（异步线程写，无 tenant 上下文）
    - trace_node            # 同上
```

## 实现机制

### 1. tenant_id 字段

所有 entity 必须继承 `BaseEntity`（参见 [modules/common.md § chat](../modules/common.md)），自动获得：

```java
@TableField("tenant_id")
private String tenantId;
```

参见：[entity-sys-user](../raw/system-source/entity-sys-user) 示例。

### 2. MyBatis-Plus 多租户拦截器

参见：[mybatis-plus-config](../raw/common-source/mybatis-plus-config)。

```java
@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantHandler() {
            @Override
            public Expression getTenantId() {
                return new StringValue(TenantHelper.getTenantId());
            }
            @Override
            public boolean ignoreTable(String tableName) {
                return tenantConfig.getExcludes().contains(tableName);
            }
        }));
        return interceptor;
    }
}
```

**机制**：每次 mapper 调用前，拦截器自动改写 SQL，加 `WHERE tenant_id = ?`，从 `TenantHelper`（基于 ThreadLocal）取当前请求的 tenant id。

### 3. 上下文传播

```java
TenantHelper.setTenantId("000001");    // 设置
String tid = TenantHelper.getTenantId(); // 获取
TenantHelper.clear();                    // 清理（必须在 finally）
```

**默认 tenant**：`"000000"`（超级管理员，跨租户）。

### 4. 异步线程上下文丢失

`@Async` / `ThreadPoolTaskExecutor` / SnailJob 任务 / MQ 消费者：**ThreadLocal 不传递**——必须在调用 mapper 前手动 set：

```java
@Async
public void asyncTask() {
    TenantHelper.setTenantId(originalTenantId);
    try {
        userMapper.selectList(null);   // 正常过滤
    } finally {
        TenantHelper.clear();
    }
}
```

**这就是**为什么 `trace_run` / `trace_node` 在 `tenant.excludes` 里：链路追踪 writer 跑在异步线程上，没有 tenant 上下文。

## 边界 / 例外

### 不参与多租户过滤的表

`tenant.excludes` 列表——通常是：

| 表 | 原因 |
|---|---|
| `sys_menu` | 菜单是租户共享的（不然切换租户看不到菜单） |
| `sys_role` / `sys_role_menu` / `sys_user_role` | 角色定义本身可共享 |
| `sys_tenant` / `sys_tenant_package` | 租户管理元数据 |
| `sys_oss_config` | OSS 配置是系统级 |
| `flow_spel` | Warm-Flow 表达式 |
| `trace_run` / `trace_node` | 异步线程追踪写入 |

**新增共享表**：在 `application.yml` 的 `tenant.excludes` 数组加一行。**漏加 = 跨租户串数据**（P0 事故）。

### 用户级数据权限 vs 租户级隔离

两者**不冲突**：

- **租户**：粗粒度，按 `tenant_id` 切（公司 A / 公司 B 不能互看）
- **数据权限**：细粒度，在同一租户内按部门 / 本人过滤（销售部 vs 财务部）

数据权限通过 `@SaCheckDataScope` 注解 + `DataPermissionInterceptor` 拦截器实现。详见 [security-reviewer agent 文档](../../.claude/agents/security-reviewer.md)。

### 异步任务 / 定时任务

每个 SnailJob / `@Scheduled` / MQ 消费者任务，**必须**显式 set tenant id：

```java
@Scheduled(cron = "0 0 2 * * ?")
public void dailyCleanup() {
    // 遍历所有租户
    for (String tenantId : tenantMapper.selectAllTenantIds()) {
        TenantHelper.setTenantId(tenantId);
        try {
            // 执行清理
        } finally {
            TenantHelper.clear();
        }
    }
}
```

## Chat 模块的租户处理

参见 [mcp-tool-provider-service.md](../raw/chat-source/mcp-tool-provider-service.md)：Langchain4j agent 调用 MCP 工具时，工具方法必须显式接 `@MemoryId String tenantId` 隔离：

```java
@Tool
public String readUserDoc(@MemoryId String tenantId, String docId) {
    // tenantId 在 Langchain4j 调用时自动从 chat memory 注入
    return docService.read(tenantId, docId);
}
```

**漏接 tenantId = 跨租户读数据**（P0 安全事故）。

## 测试多租户

测试类必须显式 set tenant：

```java
@Tag("dev")
class SysUserServiceTest {
    @BeforeEach
    void setUp() {
        TenantHelper.setTenantId("000000"); // 测试租户
    }
    @AfterEach
    void tearDown() {
        TenantHelper.clear();
    }
}
```

## 修改 checklist

改动涉及多租户时，必须 review：

- [ ] 新表有 `tenant_id` 字段
- [ ] entity 继承 `BaseEntity` 含 `tenantId`
- [ ] mapper 接口方法都经过多租户拦截器（默认是，除非显式 `@InterceptorIgnore(tenantLine = "true")`）
- [ ] 异步任务 / 定时任务显式 set tenant
- [ ] `@Tool` 方法接 `@MemoryId String tenantId`
- [ ] 跨租户共享表登记到 `tenant.excludes`
- [ ] 测试用 BeforeEach setTenant

漏任何一项都可能导致**跨租户数据泄漏**——最高优先级审查项。