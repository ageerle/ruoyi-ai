---
topic: modules/common-business
title: ruoyi-common 业务能力层（log / job / ratelimiter / idempotent / translation）
updated: 2026-09-04
raw:
  - raw/common-source/login-helper.md
  - raw/common-source/global-exception-handler.md
---

# ruoyi-common 业务能力层

覆盖 5 个业务能力横切关注点模块：操作日志、定时任务、限流、幂等、字典翻译。共 27 个 Java 文件。

参见：[modules/common.md § 27 子模块清单](../modules/common.md)。

## ruoyi-common-log（7 文件）

操作日志 —— 记录所有写操作的审计轨迹。

参见 [modules/system-listener-runner.md § UserActionListener](../modules/system-listener-runner.md)。

**关键类**：

- `@Log` 注解 + AOP 切面 —— 自动记录 `@Log` 标注的方法
- `SysOperLog` 实体 —— `sys_oper_log` 表
- `OperLogEvent` + listener —— 异步写日志

**典型用法**：

```java
@Log(title = "用户管理", businessType = BusinessType.INSERT)
@PostMapping
public R<Void> add(@RequestBody SysUserBo user) {
    // AOP 自动记录：方法名、参数、用户、IP、耗时、结果
}
```

**`sys_oper_log` 表字段**：

| 字段 | 含义 |
|---|---|
| `title` | 业务模块 |
| `business_type` | 操作类型（INSERT / UPDATE / DELETE / GRANT / IMPORT / EXPORT / FORCE / GENCODE / CLEAN） |
| `method` | 方法名 |
| `request_method` | HTTP 方法 |
| `oper_url` | 请求 URL |
| `oper_ip` | 操作 IP |
| `oper_param` | 请求参数（截断敏感字段） |
| `json_result` | 响应 JSON |
| `status` | 0 成功 / 1 失败 |
| `error_msg` | 错误信息 |
| `oper_time` | 耗时（ms） |

## ruoyi-common-job（1 文件）

SnailJob 客户端封装。

参见 [claude-md.md § SnailJob](../raw/project-skeleton/claude-md.md)。

**关键类**：

- `@SnailJob` 注解 —— 标记分布式任务
- `JobExecutor` 接口 —— 任务执行
- `JobArgs` —— 任务参数

**典型用法**：

```java
@SnailJob(name = "清理过期会话", cron = "0 0 2 * * ?")
@Component
public class ChatSessionCleanupJob implements JobExecutor {
    @Override
    public SnailJobResult execute(JobArgs args) {
        int count = chatSessionService.cleanExpired(LocalDate.now().minusDays(30));
        return new SnailJobResult(true, "清理 " + count + " 条会话");
    }
}
```

**server 端**：`ruoyi-snailjob-server` 是独立的 Spring Boot 应用（参见 [modules/admin.md § 配套独立服务](../modules/admin.md)）。

## ruoyi-common-ratelimiter（4 文件）

限流。

参见 [modules/admin.md § RateLimiter 配置](../modules/admin.md)。

**关键类**：

- `@RateLimiter` 注解 —— 标记限流方法
- `RateLimiterHandler` —— AOP 拦截
- `RedisLimitScript` —— Redis Lua 限流脚本

**典型用法**：

```java
@RateLimiter(key = "auth:login", count = 5, time = 60, limitType = LimitType.IP)
@PostMapping("/login")
public R<?> login(@RequestBody LoginBody body) { ... }
```

**参数**：

- `key`：限流 key（支持 SpEL）
- `count`：窗口内允许次数
- `time`：窗口大小（秒）
- `limitType`：`IP` / `GLOBAL`

**实现**：Redis Lua 脚本（原子计数）+ 滑动窗口。

## ruoyi-common-idempotent（3 文件）

幂等性 —— 防止重复提交（防止用户在慢响应时点多次提交按钮）。

**关键类**：

- `@Idempotent` 注解 —— 标记幂等方法
- `IdempotentHandler` —— 拦截
- `IdempotentKeyResolver` —— key 解析

**典型用法**：

```java
@Idempotent(key = "#orderBo.userId", expire = 60)
@PostMapping("/submit")
public R<?> submitOrder(@RequestBody OrderBo orderBo) { ... }
```

**机制**：第一次请求 → 记 Redis key + 执行业务；60 秒内重复请求 → 返回「请勿重复提交」。

**与限流的区别**：
- 限流：限「速率」（X 次 / 时间）
- 幂等：限「重复」（同一操作只执行一次）

## ruoyi-common-translation（12 文件）

字典翻译 —— VO 字段自动从字典翻译。

**关键类**：

- `@Translation` 注解 —— 标记翻译字段
- `TranslationHandler` —— 拦截返回值
- `DictTranslationService` —— 字典查询 + 翻译

**典型用法**：

```java
public class SysUserVo {
    @Translation(type = TranslationType.DICT, key = "sys_user_sex")
    private String sexLabel;
}
```

返回前端时 `sexLabel` 自动从 `sys_user_sex` 字典查 `0=男,1=女,2=未知`。

**与 `@ExcelDictFormat` 的关系**：Excel 导入导出用 `@ExcelDictFormat`（Excel 层面）；VO 返回前端用 `@Translation`（API 层面）。

## 5 模块协作模式

以「删除用户」为例：

1. `@Log` 记录操作（log 模块）
2. `@RateLimiter` 限制频繁调用（ratelimiter 模块）
3. `@SaCheckPermission` 校验权限（security 模块 + satoken 模块）
4. `@DataScope` 控制数据范围（mybatis 模块）
5. `@Idempotent` 防止重复（idempotent 模块）
6. 事务回滚 + `GlobalExceptionHandler` 兜底（core 模块）
7. 操作后异步写 `sys_oper_log`（log 模块的 listener）

一个接口涉及 5-7 个 common 子模块的协作。

## 已知约束

- **限流 key 必须唯一**：避免不同业务互限
- **幂等 key 必须稳定**：同一操作的 key 应该相同（用业务主键）
- **日志表膨胀**：长期运行后 `sys_oper_log` 会很大，建议定时归档
- **任务幂等**：SnailJob 任务必须自己保证幂等（框架只保证调度不重复）
- **翻译字段性能**：每次查询都查字典，**建议字典放 Redis**