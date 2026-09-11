---
topic: modules/common-security-auth
title: ruoyi-common 安全与认证层（security / satoken / encrypt / sensitive）
updated: 2026-09-04
raw:
  - raw/common-source/security-config.md
  - raw/common-source/login-helper.md
  - raw/common-source/SensitiveService.md
---

# ruoyi-common 安全与认证层

覆盖 4 个安全相关模块：安全配置、Sa-Token 封装、加解密、数据脱敏。

参见：[modules/common.md § 27 子模块清单](../modules/common.md)、[security-config.md](../raw/common-source/security-config.md)。

## ruoyi-common-security（3 文件）

Sa-Token 集成 + Web 安全配置。

参见 [security-config.md](../raw/common-source/security-config.md)。

**关键类**：

- `SecurityConfig` —— 注册 Sa-Token 拦截器、注解处理器
- `SaTokenInterceptor` —— 拦截 HTTP 请求，校验 token
- `WebMvcSecurityConfig` —— Web MVC 安全配置

**Sa-Token 拦截器**：

```java
@Configuration
public class SecurityConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor())
            .addPathPatterns("/**")
            .excludePathPatterns(...);
    }
}
```

**excludePathPatterns**（来自 application.yml `security.excludes`）：

- 静态资源（`/*.html`、`/static/**`）
- API docs（`/v3/api-docs/**`）
- 登录相关（`/login`、`/auth/**`、`/captcha/**`）
- 工作流 UI（`/warm-flow-ui/config`）

## ruoyi-common-satoken（5 文件）

Sa-Token 静态操作封装。

参见 [login-helper.md](../raw/common-source/login-helper.md)。

**关键类**：

- `LoginHelper` —— 项目封装的便捷方法（`getUserId()` / `getUsername()` / `getTenantId()`）
- `StpUtil`（来自 Sa-Token）—— 原始 API（`login()` / `logout()` / `getLoginId()` / `checkPermission()`）
- `SaTokenContext` —— 上下文

**典型用法**：

```java
// 获取当前登录用户
Long userId = LoginHelper.getUserId();
String tenantId = LoginHelper.getTenantId();

// 权限检查
StpUtil.checkPermission("system:user:list");

// 数据权限范围
@SaCheckDataScope  // 注解方式

// 角色检查
StpUtil.checkRole("admin");
```

## ruoyi-common-encrypt（23 文件）

API 加解密 + MyBatis 字段加密。

参见 [claude-md.md § Key Conventions — Sa-Token](../raw/project-skeleton/claude-md.md)（api-decrypt 配置）。

**两类加密**：

| 类型 | 配置 | 作用 |
|---|---|---|
| **api-decrypt** | `api-decrypt.enabled` | HTTP 请求 / 响应 RSA 加密（防止中间人窃听） |
| **mybatis-encryptor** | `mybatis-encryptor.enable` | 数据库字段 AES 加密（敏感字段落库前加密） |

**api-decrypt 用法**：

```java
@ApiEncrypt  // 请求解密 + 响应加密
@PostMapping("/secure")
public R<?> secureEndpoint(@RequestBody SensitiveRequest req) { ... }
```

**mybatis-encryptor 用法**：

```java
@TableField("id_card")
@EncryptField(algorithm = "AES")
private String idCard;
```

**密钥管理**：`api-decrypt.privateKey` / `publicKey` 在 application.yml，**生产必须替换**（[claude-md.md § api-decrypt](../raw/project-skeleton/claude-md.md) 有 dev 默认 key）。

## ruoyi-common-sensitive（4 文件）

数据脱敏 —— 返回给前端前自动改写敏感字段。

参见 [SensitiveService.md](../raw/common-source/SensitiveService.md)。

**脱敏策略**：

| 类型 | 示例 | 改写后 |
|---|---|---|
| 手机号 | `13812345678` | `138****5678` |
| 身份证 | `110101199003078888` | `110101********8888` |
| 邮箱 | `user@example.com` | `u***@example.com` |
| 银行卡 | `6222021234567890` | `622202********7890` |
| 姓名 | `张三` | `张*` |

**用法**：

```java
@Sensitive(strategy = SensitiveStrategy.PHONE)
private String phone;
```

返回前端时自动脱敏（日志打印也脱敏）。

## 已知约束

- **api-decrypt 性能开销**：RSA 加解密 ≈ 1-5ms / 请求。生产环境建议关闭非敏感接口
- **mybatis-encryptor 不能加密主键**：会破坏 MyBatis-Plus 雪花算法
- **Sensitive 不抗爬虫**：脱敏是展示层，数据库存原值——爬虫直接读 DB 就能拿原值。**关键字段必须叠加 mybatis-encryptor**
- **多租户 + 脱敏**：返回前要确认 tenantId 过滤（避免把 A 租户的脱敏数据返回给 B 租户）

参见：[security-reviewer agent 文档 § 密钥管理](../../.claude/agents/security-reviewer.md)。