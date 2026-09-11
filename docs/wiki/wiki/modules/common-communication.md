---
topic: modules/common-communication
title: ruoyi-common 通信层（web / sse / websocket / oss / mail / sms / social）
updated: 2026-09-04
raw:
  - raw/common-source/security-config.md
---

# ruoyi-common 通信层

覆盖 7 个通信 / 集成模块：web / sse / websocket / oss / mail / sms / social。共 58 个 Java 文件。

参见：[modules/common.md § 27 子模块清单](../modules/common.md)。

## ruoyi-common-web（20 文件）

Web MVC 配置 + XSS 过滤 + i18n + Undertow 配置。

参见 [modules/admin.md § Undertow 配置](../modules/admin.md)。

**关键类**：

- `WebMvcConfig` —— 拦截器注册 / 静态资源映射 / CORS
- `ResourcesConfig` —— 静态资源 + Swagger UI
- `GlobalExceptionHandler` —— 全局异常（参见 [modules/common-core-utilities.md](../modules/common-core-utilities.md)）
- `I18nConfig` + `I18nLocaleResolver` —— 国际化
- `UndertowConfig` —— Undertow 定制
- `CaptchaConfig` —— 图形验证码
- `DemoAutoConfiguration` —— 演示模式拦截器（参见 [system-listener-runner.md § Demo mode](../modules/system-listener-runner.md)）

**默认排除路径**（`security.excludes`）：

```yaml
security:
  excludes:
    - /*.html
    - /**/*.html
    - /**/*.css
    - /**/*.js
    - /favicon.ico
    - /error
    - /*/api-docs
    - /warm-flow-ui/config
    - /workflow/run
```

## ruoyi-common-sse（8 文件）

Server-Sent Events 服务端。

参见 [modules/chat.md § SSE 流式响应](../modules/chat.md)。

**关键类**：

- `SseMessageUtils` —— 静态发送 SSE 消息
- `SseMessageDto` —— 消息 DTO
- `SseEmitterServer` —— Emitter 注册表
- `SseController` —— 客户端订阅入口

**默认配置**：

```yaml
sse:
  enabled: true
  path: /resource/sse
```

**典型用法**（chat 模块推送 LLM 流式输出）：

```java
SseMessageUtils.sendMessage(userId, new SseMessageDto("chat", "data..."));
```

## ruoyi-common-websocket（9 文件）

WebSocket 服务端（默认关）。

参见 [claude-md.md § websocket.enabled](../raw/project-skeleton/claude-md.md)。

**关键类**：

- `WebSocketServer` —— Netty / Spring WebSocket 服务端
- `WebSocketMessage` —— 消息封装
- `WebSocketSessionManager` —— Session 管理

**默认配置**：

```yaml
websocket:
  enabled: false     # 默认关
  path: /resource/websocket
  allowedOrigins: '*'
```

**与 SSE 的选择**：

| 维度 | SSE | WebSocket |
|---|---|---|
| 协议 | HTTP/1.1 | 独立升级协议 |
| 方向 | 单向（服务器 → 客户端） | 双向 |
| 代理 | 友好 | 需要额外配置 |
| 适用 | LLM 流式输出、AI 通知 | 协同编辑、实时游戏 |

**默认 SSE** 是更轻量的选择。WebSocket 仅在必须双向通信时开。

## ruoyi-common-oss（8 文件）

AWS S3 / MinIO 对象存储。

参见 [claude-md.md § MinIO 端口](../raw/project-skeleton/claude-md.md)。

**关键类**：

- `OssClient` —— S3 客户端封装（基于 AWS SDK v2）
- `OssProperties` —— 配置绑定
- `SysOssService` —— OSS 业务（存 `sys_oss` 表 + 调 S3 上传）

**默认配置**：

```yaml
oss:
  endpoint: http://127.0.0.1:29000     # MinIO
  access-key: minioadmin
  secret-key: minioadmin
  bucket: ruoyi-ai
```

**与 MinIO 配合**：docker compose 已起 MinIO（端口 `29000` / `29090`），OSS 上传后通过 presigned URL 访问。

## ruoyi-common-mail（3 文件）

邮件发送。

**关键类**：

- `MailService` —— 简单邮件发送
- `MailProperties` —— SMTP 配置

**默认配置**：

```yaml
mail:
  host: smtp.example.com
  port: 465
  username: noreply@example.com
  password: ${MAIL_PASSWORD:}
  ssl: true
```

**典型用法**（aiflow 节点 `EmailSend`）：

```java
mailService.sendText("user@example.com", "标题", "正文");
mailService.sendHtml("user@example.com", "标题", "<p>HTML 正文</p>");
```

## ruoyi-common-sms（3 文件）

短信发送（基于 sms4j）。

**关键类**：

- `SmsService` —— 短信发送
- `SmsProperties` —— 多厂商配置

**支持厂商**：阿里云、腾讯云、华为云、京东云、移动云等（sms4j 支持 20+ 厂商）。

## ruoyi-common-social（13 文件）

社交登录（基于 JustAuth）。

参见 [claude-md.md § JustAuth](../raw/project-skeleton/claude-md.md)。

**支持的平台**：GitHub / Gitee / 微信 / QQ / 支付宝 / 百度 / 钉钉 / 飞书等。

**关键类**：

- `SocialUtils` —— 第三方登录 URL 生成 + 回调处理
- `SocialProperties` —— 多平台 keys
- `AuthController.bindCallback` —— 绑定回调

## 与 chat 模块的关系

7 个通信模块全部支撑 chat / aiflow 模块：

- **SSE**：LLM 流式输出
- **WebSocket**：协同场景
- **OSS**：文件上传（聊天附件、AI 生成图片 / 视频）
- **Mail**：aiflow 邮件节点 + 通知
- **SMS**：登录验证码
- **Social**：第三方登录

参见：[claude-md.md § 排除路径 / 端口表](../raw/project-skeleton/claude-md.md)。