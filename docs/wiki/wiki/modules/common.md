---
topic: modules/common
title: ruoyi-common — 27 个共享库
updated: 2026-09-04
raw:
  - raw/common-source/security-config.md
  - raw/common-source/mybatis-plus-config.md
  - raw/common-source/global-exception-handler.md
  - raw/common-source/login-helper.md
  - raw/common-source/role-type-enum.md
  - raw/common-source/audio-service-factory.md
---

# ruoyi-common — 27 个共享库

`ruoyi-common` 由 27 个子模块组成，提供所有功能模块复用的基础能力。总 342 个 Java 文件。

参见：[pom-xml.md § ruoyi-common-bom](../raw/project-skeleton/pom-xml.md)、[claude-md.md § Module Layout — common](../raw/project-skeleton/claude-md.md)。

## 27 个子模块清单

| 模块 | 文件数 | 作用 |
|---|---|---|
| ruoyi-common-bom | 1 | BOM 依赖管理 |
| ruoyi-common-core | 高 | 核心工具（SpringUtils / StringUtils / R / Page 等） |
| ruoyi-common-web | 中 | Web MVC、异常处理、i18n、Undertow 配置 |
| ruoyi-common-security | 中 | Sa-Token 集成、注解鉴权 |
| ruoyi-common-satoken | 中 | LoginHelper + StpUtil 封装 |
| ruoyi-common-mybatis | 中 | MyBatis-Plus 配置、多租户拦截器 |
| ruoyi-common-redis | 中 | Redisson、分布式锁 |
| ruoyi-common-cache | 低 | 缓存注解封装 |
| ruoyi-common-chat | 中 | Langchain4j 适配、BaseEntity、RoleType |
| ruoyi-common-encrypt | 低 | mybatis-encryptor、api-decrypt |
| ruoyi-common-excel | 低 | FastExcel 集成 |
| ruoyi-common-job | 低 | SnailJob 客户端封装 |
| ruoyi-common-json | 低 | Jackson 配置 |
| ruoyi-common-log | 低 | 操作日志（oper_log） |
| ruoyi-common-mail | 低 | 邮件发送 |
| ruoyi-common-oss | 中 | AWS S3 / MinIO 适配 |
| ruoyi-common-ratelimiter | 低 | 限流注解 |
| ruoyi-common-sensitive | 低 | 数据脱敏 |
| ruoyi-common-sms | 低 | sms4j 集成 |
| ruoyi-common-social | 低 | JustAuth 社交登录 |
| ruoyi-common-sse | 中 | SSE 服务端 |
| ruoyi-common-tenant | 中 | 多租户上下文 |
| ruoyi-common-trace | 中 | 链路追踪 |
| ruoyi-common-translation | 低 | 字典翻译 |
| ruoyi-common-idempotent | 低 | 幂等性注解 |
| ruoyi-common-doc | 低 | Springdoc 集成 |
| ruoyi-common-websocket | 中 | WebSocket 服务端 |

## 关键模块详情

### ruoyi-common-security + ruoyi-common-satoken

```java
@Configuration
public class SecurityConfig {
    // 注册 Sa-Token 拦截器
}
```

参见：[security-config](../raw/common-source/security-config)。

Sa-Token 关键类：

- `StpUtil` —— 当前用户登录态操作（login / logout / getLoginId / checkPermission）
- `LoginHelper` —— 项目封装的便捷方法
- `@SaCheckLogin` / `@SaCheckRole` / `@SaCheckPermission` / `@SaCheckDataScope`

参见：[login-helper](../raw/common-source/login-helper)。

### ruoyi-common-mybatis

```java
@Configuration
public class MybatisPlusConfig {
    // 多租户拦截器
    // 分页插件
    // 乐观锁插件
    // 数据权限插件
}
```

参见：[mybatis-plus-config](../raw/common-source/mybatis-plus-config)。

**关键拦截器**：
- `MybatisTenantInterceptor` —— 自动加 `tenant_id` WHERE 条件
- `DataPermissionInterceptor` —— 自动加数据权限范围
- `PaginationInnerInterceptor` —— 分页

### ruoyi-common-web

`GlobalExceptionHandler` —— 全局异常处理，把各种异常（`ServiceException` / `NotPermissionException` / 校验异常 / IO 异常）统一转为 `R` 响应。

参见：[global-exception-handler](../raw/common-source/global-exception-handler)。

### ruoyi-common-chat

提供 AI 模块的基础类：

- `BaseEntity` —— 所有 entity 的基类，含 `id` / `createTime` / `updateTime` / `createBy` / `updateBy` / `delFlag` / `remark` / `tenantId`
- `User` —— AI 上下文中的用户对象
- `RoleType` / `UserStatusEnum` / `ErrorEnum` —— 公共枚举
- `AudioServiceFactory` / `VideoServiceFactory` —— 多模态服务 factory 模式

参见：[role-type-enum](../raw/common-source/role-type-enum)、[audio-service-factory](../raw/common-source/audio-service-factory)。

## 多租户 + 数据权限

详见 [cross-cutting/multi-tenant-design.md](../cross-cutting/multi-tenant-design.md)。

## 引用约定

`ruoyi-common-bom` 是 BOM（依赖管理），其他子模块通过 `dependencyManagement` 统一版本。功能模块（admin / chat / system 等）通过 `<dependency>` 引用具体子模块：

```xml
<dependency>
    <groupId>org.ruoyi</groupId>
    <artifactId>ruoyi-common-web</artifactId>
</dependency>
```

参见：[pom-xml.md § dependencyManagement](../raw/project-skeleton/pom-xml.md)。