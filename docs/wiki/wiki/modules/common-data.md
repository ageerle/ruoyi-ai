---
topic: modules/common-data
title: ruoyi-common 数据层（mybatis / redis / tenant / trace）
updated: 2026-09-04
raw:
  - raw/common-source/mybatis-plus-config.md
  - raw/common-source/TenantHelper.md
---

# ruoyi-common 数据层

覆盖 4 个数据相关模块：MyBatis-Plus、Redis、多租户、链路追踪。

参见：[modules/common.md § 27 子模块清单](../modules/common.md)、[cross-cutting/multi-tenant-design.md](../cross-cutting/multi-tenant-design.md)。

## ruoyi-common-mybatis（19 文件）

MyBatis-Plus 配置 + 拦截器 + 自动填充。

参见 [mybatis-plus-config.md](../raw/common-source/mybatis-plus-config.md)。

**3 个核心拦截器**：

| 拦截器 | 作用 |
|---|---|
| `PaginationInnerInterceptor` | 自动分页（`PageHelper` 替代） |
| `TenantLineInnerInterceptor` | 自动加 `WHERE tenant_id` |
| `DataPermissionInterceptor` | 自动加数据权限范围 |
| `OptimisticLockerInnerInterceptor` | 乐观锁 |

**关键配置**：

```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor(TenantConfig tenantConfig) {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
    interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantHandler() {
        @Override
        public Expression getTenantId() {
            return new StringValue(TenantHelper.getTenantId());
        }
    }));
    return interceptor;
}
```

**自动填充**（`create_time` / `update_time` / `create_by` / `update_by`）：

- 实现 `MetaObjectHandler`
- 在 `@TableField(fill = FieldFill.INSERT)` / `FieldFill.INSERT_UPDATE` 自动注入

参见 [claude-md.md § Key Conventions — 多租户](../raw/project-skeleton/claude-md.md)。

## ruoyi-common-redis（11 文件）

Redisson 集成 + 分布式锁 + 缓存。

**关键类**：

- `RedissonConfig` —— Redisson 客户端配置（连接池 / 序列化）
- `RedisUtils` —— 静态 Redis 操作（`get` / `set` / `hGet` / `setNx` 等）
- `RedisLock` —— 分布式锁（基于 `setNx + Lua`）

**Redisson 自动装配**（参见 [pom-xml.md § redisson](../raw/project-skeleton/pom-xml.md)）：

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
</dependency>
```

**Lock4j 分布式锁**（参见 [claude-md.md § Lock4j](../raw/project-skeleton/claude-md.md)）：

```java
@Lock4j(key = "order:create", expire = 30000)
public void createOrder(OrderBo bo) { ... }
```

## ruoyi-common-tenant（9 文件）

多租户上下文传播。

参见 [TenantHelper.md](../raw/common-source/TenantHelper.md)。

**关键类**：

- `TenantHelper` —— ThreadLocal 包装（`setTenantId` / `getTenantId` / `clear`）
- `TenantConfig` —— 排除表配置
- `TenantContext` —— Spring 上下文集成
- `TenantFilter` —— HTTP 请求级自动 setTenantId
- `TenantContextHolder` —— 备用 holder

**典型用法**（参见 [cross-cutting/multi-tenant-design.md](../cross-cutting/multi-tenant-design.md)）：

```java
// HTTP 请求：自动 set（filter）
// 异步线程：手动 set + finally clear
TenantHelper.setTenantId(tenantId);
try {
    userMapper.selectList(null);
} finally {
    TenantHelper.clear();
}
```

## ruoyi-common-trace（20 文件）

分布式链路追踪 —— 把每个请求的调用链写入 MySQL 表，供查询分析。

参见 [claude-md.md § trace.enabled](../raw/project-skeleton/claude-md.md)。

**关键类**：

- `TraceContext` —— 链路上下文（traceId / spanId / parentSpanId）
- `TraceInterceptor` —— 拦截 HTTP 请求 / mapper 调用
- `TracePayloadBuilder`（在 chat 模块）—— RAG 链路 payload

**两张表**：

```sql
trace_run  -- 一次请求 / 一次会话（trace_id, tenant_id, start_time, end_time, status）
trace_node -- 一个 span（trace_id, span_id, parent_span_id, name, type, start_time, duration_ms, payload）
```

**重要**：`trace_run` 和 `trace_node` 在 `tenant.excludes` 白名单里（参见 [application-yml.md § tenant.excludes](../raw/project-skeleton/application-yml.md)）—— 因为 trace writer 在异步线程写，没有 tenant 上下文。

**启用**：`trace.enabled: true`（默认开）；关闭后所有埋点透传，零性能开销。

## 与多租户隔离的关系

3 个数据层模块互相依赖：

```
TenantHelper (ThreadLocal)
    ↓
TenantLineInnerInterceptor (自动加 WHERE tenant_id)
    ↓
MetaObjectHandler (自动填充 create_by / tenant_id)
    ↓
TraceInterceptor (记录谁访问了哪个租户的数据)
```

任何修改都必须联动测试这 4 个组件，参见 [cross-cutting/multi-tenant-design.md § 修改 checklist](../cross-cutting/multi-tenant-design.md)。