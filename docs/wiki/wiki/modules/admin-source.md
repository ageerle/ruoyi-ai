---
topic: modules/admin-source
title: ruoyi-admin 源码层 — 启动 / Controller / Config
updated: 2026-09-04
raw:
  - raw/admin-source/ruoyi-ai-application.md
  - raw/admin-source/ruoyi-ai-servlet-initializer.md
  - raw/admin-source/mapper-conflict-resolver.md
  - raw/admin-source/index-controller.md
  - raw/admin-source/auth-controller.md
  - raw/admin-source/captcha-controller.md
  - raw/admin-source/logback-plus-xml.md
---

# ruoyi-admin 源码层

`ruoyi-admin` 模块虽然是整个项目的「主应用」，但源码层非常薄——只有 6 个 Java 文件 + 1 个 logback 配置。所有业务逻辑都在 `ruoyi-modules/*` 里，admin 只做：启动 + 鉴权 / 首页 + 验证码 + 日志 + 一个 MapStruct 冲突解决器。

## 启动类 — RuoYiAIApplication

```java
@SpringBootApplication
public class RuoYiAIApplication {
    public static void main(String[] args) {
        killPortProcess(6039);                  // ← 自杀式腾端口
        SpringApplication application = new SpringApplication(RuoYiAIApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
    }
}
```

**关键细节**：

1. **`killPortProcess(6039)`** 必须在 `SpringApplication.run()` 之前调用，否则端口被占 → 启动失败。Windows 风格用 `netstat -ano` + `taskkill /F /PID`；macOS/Linux 直接抛异常被 catch + 静默跳过（无副作用）。
2. **`BufferingApplicationStartup(2048)`** 启用 Spring Boot 启动期事件缓冲（2048 条），用于 actuator 暴露 startup 端点，便于排查启动慢的 bean。

参见：[ruoyi-ai-application.md](../raw/admin-source/ruoyi-ai-application.md)、[claude-md.md § Key Conventions — Port 6039 auto-kill](../raw/project-skeleton/claude-md.md)。

## War 包支持 — RuoYiAIServletInitializer

```java
public class RuoYiAIServletInitializer extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(RuoYiAIApplication.class);
    }
}
```

继承 `SpringBootServletInitializer`，让项目能打成 war 包部署到外部 Tomcat（默认打 jar）。部署方式见 [readme-md.md § Docker Deployment](../raw/project-skeleton/readme-md.md)。

## MapStruct Plus 冲突解决器 — MapperConflictResolver

```java
@Configuration
public class MapperConflictResolver implements BeanDefinitionRegistryPostProcessor {
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        for (String beanName : registry.getBeanDefinitionNames()) {
            if (beanName.equals("chatMessageBoToChatMessageMapperImpl")) {
                BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
                String beanClassName = beanDefinition.getBeanClassName();
                if (beanClassName != null && beanClassName.startsWith("org.ruoyi.domain.bo.chat")) {
                    log.warn("Removing conflicting bean definition: {}", beanClassName);
                    registry.removeBeanDefinition(beanName);
                }
            }
        }
    }
}
```

**这是项目里非常关键的「修补」组件**——修复 MapStruct Plus 在生成 `IConvert` 实现时的 bug：

- 当存在同名 BO 类（如 `chatMessageBo` 在两个包下），MapStruct Plus 可能生成两个 `chatMessageBoToChatMessageMapperImpl` bean
- Spring 启动时第二个 bean 抛 `BeanDefinitionOverrideException`
- 这个 resolver 在 `BeanDefinitionRegistryPostProcessor` 阶段移除冲突的 bean 定义，保留正确的那个（`org.ruoyi.domain.bo.chat` 包下的显然是意外的版本）

**经验**：扩展 chat 模块时，如果遇到 `BeanDefinitionOverrideException`，先看这个 resolver 是否覆盖到对应 mapper 名，可能需要扩展匹配条件。

参见：[mapper-conflict-resolver.md](../raw/admin-source/mapper-conflict-resolver.md)。

## 首页 — IndexController

```java
@SaIgnore
@RestController
public class IndexController {
    @GetMapping("/")
    public String index() {
        return StringUtils.format("欢迎使用{}后台管理框架，请通过前端地址访问。",
                                   SpringUtils.getApplicationName());
    }
}
```

`/` 路径返回欢迎语，强制 `@SaIgnore` 跳过登录校验。**前端单独部署**（`ruoyi-web` / `ruoyi-admin` 是独立仓库），后端只暴露 API；`/` 这个端点用于快速确认服务是否启动。

参见：[index-controller.md](../raw/admin-source/index-controller.md)。

## 认证入口 — AuthController

```java
@SaIgnore
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final SocialProperties socialProperties;
    private final SysLoginService loginService;
    ...
}
```

`@SaIgnore` 表示整个 controller **不参与 Sa-Token 登录态校验**——因为这里本来就是登录入口。

**关键依赖**：
- `SocialProperties` + `AuthRequest`：来自 JustAuth + 自定义配置，社交登录（微信 / GitHub / Gitee）
- `SysLoginService`：实际登录业务，**位于 `ruoyi-modules/ruoyi-system`**（不在 admin）
- `LoginHelper`：来自 `ruoyi-common-satoken`，封装 Sa-Token 操作
- `TenantHelper`：来自 `ruoyi-common-tenant`，多租户上下文

**关键能力**：
- 账号密码登录
- 短信登录
- 社交 OAuth 登录
- 第三方登录回调处理
- 限流（`@RateLimiter`）
- API 加密（`@ApiEncrypt`）

参见：[auth-controller.md](../raw/admin-source/auth-controller.md)。

## 验证码 — CaptchaController

`@SaIgnore` 跳过登录校验，路由 `/captcha`，提供图形 / 算术验证码生成与校验。配置来自 `application.yml`：

```yaml
captcha:
  enable: false        # 默认禁用
  type: MATH           # 类型：math（算术）/ char（字符）
  category: CIRCLE     # 干扰线类型：line / circle / shear
  numberLength: 1
  charLength: 4
```

参见：[captcha-controller.md](../raw/admin-source/captcha-controller.md)、[application-yml.md § captcha](../raw/project-skeleton/application-yml.md)。

## 日志 — logback-plus.xml

项目自定义的 logback 配置，**不在 classpath 默认的 logback-spring.xml 体系里**——直接叫 `logback-plus.xml` 是项目惯例。

参见：[logback-plus-xml.md](../raw/admin-source/logback-plus-xml.md)、[application-yml.md § logging.config](../raw/project-skeleton/application-yml.md)。

## 模块边界

`ruoyi-admin` 唯一负责：

| 职责 | 实现 |
|---|---|
| 启动 | `RuoYiAIApplication` + `RuoYiAIServletInitializer` |
| 首页 | `IndexController` |
| 登录 | `AuthController`（业务逻辑下沉到 system 模块的 `SysLoginService`） |
| 验证码 | `CaptchaController` |
| Bean 冲突修复 | `MapperConflictResolver` |
| 日志格式 | `logback-plus.xml` |

**所有 REST controller / service 都不在 admin**——admin 只是壳 + 登录入口。这是 RuoYi 系列脚手架的典型设计：壳层薄，业务模块厚。