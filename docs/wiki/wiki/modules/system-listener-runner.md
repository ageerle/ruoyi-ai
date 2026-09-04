---
topic: modules/system-listener-runner
title: ruoyi-system — 事件监听器与启动任务
updated: 2026-09-04
raw:
  - raw/system-source/sys-user-import-listener.md
  - raw/system-source/user-action-listener.md
  - raw/system-source/system-application-runner.md
---

# ruoyi-system — 事件监听器与启动任务

本篇聚焦 system 模块的 2 个 listener + 1 个 runner，扩展 [modules/system.md](../modules/system.md) 的细节。

## 启动期任务（ApplicationRunner）

### SystemApplicationRunner

参见 [system-application-runner](../raw/system-source/system-application-runner.md)。

```java
@Component
public class SystemApplicationRunner implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        // Spring Boot 启动完成后立即执行
    }
}
```

**典型执行内容**：

| 任务 | 实现 | 备注 |
|---|---|---|
| 缓存字典 | `dictService.loadAll() → Redis` | 启动期一次性预热 |
| 加载配置 | `configService.loadAll() → Caffeine` | 本地缓存，避免每次访问 MySQL |
| 初始化租户 | `tenantService.init()` | 创建默认租户 / 套餐绑定 |
| 注册服务发现 | `nacos.register()` | 如果用 nacos / consul |
| 加载 MCP 工具列表 | `mcpToolService.refresh()` | LangChain4j 工具注册 |
| 预热 Langchain4j embedding | `embeddingModel.warmup()` | 减少首次调用延迟 |

**注意**：`run()` 内**禁止**做耗时长的同步操作（会卡住启动）。重任务应放到 SnailJob 异步执行。

### 与 CommandLineRunner 的区别

- `CommandLineRunner`：原始 `String[] args`，简单场景
- `ApplicationRunner`：封装为 `ApplicationArguments`，可解析 `--key=value` 形式的参数

## 事件监听器（ApplicationListener）

### SysUserImportListener（EasyExcel 集成）

参见 [sys-user-import-listener](../raw/system-source/sys-user-import-listener.md)。

```java
@Slf4j
public class SysUserImportListener implements ReadListener<SysUserImportVo> {
    private final SysUserMapper userMapper;
    private final TenantHelper tenantHelper;
    private final SysConfigService configService;

    @Override
    public void invoke(SysUserImportVo user, AnalysisContext ctx) {
        // 单条处理
        validate(user);
        encryptPassword(user);
        userMapper.insert(user);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext ctx) {
        // 清理 / 汇总
    }
}
```

**职责**：
1. 校验 Excel 导入的每行数据
2. 加密密码（`BCryptPasswordEncoder`）
3. 自动填充部门 / 岗位
4. 单条入库

**性能优化建议**：
- 默认每条 SQL，N 万用户 = N 万次 round-trip
- 改造：缓存 500 条 → `userService.insertBatch(batchList)`

**错误处理**：当前实现可能在某条失败时中断整个导入。建议加 try-catch + 错误行记录 + 跳过后继续。

### UserActionListener（用户行为审计）

参见 [user-action-listener](../raw/system-source/user-action-listener.md)。

```java
@Component
public class UserActionListener {
    @EventListener
    public void onUserLogin(UserLoginEvent event) {
        SysLogininfor log = new SysLogininfor();
        log.setUserName(event.getUsername());
        log.setStatus(event.isSuccess() ? "0" : "1");
        log.setMsg(event.getMessage());
        log.setIpaddr(event.getIp());
        loginInfoMapper.insert(log);
    }

    @EventListener
    @Async
    public void onOperLog(SysOperLogEvent event) {
        // 异步写操作日志
        operLogMapper.insert(event.getLog());
    }
}
```

**两类事件**：

| 事件 | 写入表 | 同步 / 异步 |
|---|---|---|
| `UserLoginEvent` | `sys_logininfor` | 同步（登录结果反馈） |
| `SysOperLogEvent` | `sys_oper_log` | 异步（性能考虑） |

**多租户考虑**：listener 在 `@Transactional` / 异步线程里，**TenantHelper 必须显式 set**。

### 自定义事件发布

业务侧触发：

```java
applicationEventPublisher.publishEvent(new UserLoginEvent(this, username, success, message, ip));
```

事件类实现 `ApplicationEvent` 即可，自动被 listener 接住。

## 关键事件清单

| 事件 | 触发位置 | Listener | 写入 |
|---|---|---|---|
| `UserLoginEvent` | AuthController.login() | UserActionListener | sys_logininfor |
| `UserLogoutEvent` | AuthController.logout() | UserActionListener | sys_logininfor |
| `SysOperLogEvent` | @Log 注解 / AOP 拦截 | UserActionListener | sys_oper_log |
| `UserImportEvent` | Excel 上传 | SysUserImportListener | sys_user（按行） |

## 与 AOP 切面的边界

事件监听（`@EventListener`） vs AOP 切面（`@Around`）：

| 维度 | EventListener | AOP |
|---|---|---|
| 触发方式 | 显式 `publishEvent` | 自动拦截匹配方法 |
| 耦合度 | 松耦合（事件类型匹配即可） | 紧耦合（依赖包路径） |
| 适合 | 「业务完成后做 X」 | 「方法执行前后做 X」 |
| 性能 | 微小开销 | 反射开销 |

操作日志既可走 EventListener，也可走 `@Log` AOP 切面。项目里两种都用：关键路径走 AOP（`@Log` 注解），通用路径走 event。

## 启动期异常处理

```java
@Override
public void run(ApplicationArguments args) {
    try {
        // 关键启动逻辑
    } catch (Exception e) {
        log.error("启动期任务失败", e);
        // 决定：throw 还是 swallow？
        // 大多数情况 swallow，避免阻止应用启动
    }
}
```

**经验**：启动期失败不应该阻塞应用启动（让用户能登录到管理界面查问题）。只对**致命依赖**（如数据库连接）throw，让 Spring Boot 退出码非 0，方便 systemd 重启。

参见：[claude-md.md § Key Conventions — Demo mode](../raw/project-skeleton/claude-md.md)（演示模式会拦截写入）。