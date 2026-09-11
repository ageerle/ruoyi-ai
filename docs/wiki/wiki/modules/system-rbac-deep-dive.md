---
topic: modules/system-rbac-deep-dive
title: ruoyi-system — RBAC 权限体系深入
updated: 2026-09-04
raw:
  - raw/system-source/rbac-entities.md
  - raw/system-source/entity-sys-user.md
  - raw/system-source/sys-user-controller.md
  - raw/system-source/sys-role-controller.md
  - raw/system-source/sys-menu-controller.md
  - raw/system-source/entity-cms-content.md
  - raw/common-source/login-helper.md
---

# ruoyi-system RBAC 权限体系

本篇深入 [modules/system.md](../modules/system.md) 的 RBAC 模型，覆盖实体关系、权限注解、数据权限范围、CMS 模块。

参见：[rbac-entities](../raw/system-source/rbac-entities.md)、[entity-sys-user](../raw/system-source/entity-sys-user.md)。

## 26 个实体分组

参见 [rbac-entities.md](../raw/system-source/rbac-entities.md) 完整清单，按职责分 7 类：

- **RBAC 核心**：SysUser / SysRole / SysMenu / SysDept / SysPost
- **中间表**：SysUserRole / SysRoleMenu / SysUserPost / SysRoleDept
- **系统配置与日志**：SysConfig / SysDict{Type,Data} / SysNotice / SysOperLog / SysLogininfor
- **OSS / 客户端**：SysOss / SysOssConfig / SysOssExt / SysClient
- **多租户**：SysTenant / SysTenantPackage
- **社交 / 缓存**：SysSocial / SysCache
- **AI / CMS**：ChatConfig / CmsContent

## 实体关系图

```
                          ┌──────────────┐
                          │   SysDept    │ (树形，ancestors 字段)
                          └──────┬───────┘
                                 │ 1:N
                                 ▼
┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│   SysPost    │◄──┐     │   SysUser    │◄────────┤ SysUserPost  │
└──────────────┘   │     └──┬───────┬───┘   N:N   └──────────────┘
                   │        │       │
                   │  N:N   │       │ N:N
                   └────────┤       ├─────────┐
                            │       │         │
                            ▼       ▼         ▼
                    ┌──────────┐ ┌──────────┐ ┌──────────┐
                    │ SysRole  │ │ SysRole  │ │ SysUser  │
                    │  (perms) │ │  (depts) │ │   Role   │
                    └────┬─────┘ └──────────┘ └──────────┘
                         │ N:N
                         ▼
                  ┌──────────────┐
                  │   SysMenu    │ (树形，perms 权限标识)
                  │ (component)  │
                  └──────────────┘

┌──────────────────────────────────────────────────────────────────┐
│  SysTenant (主) ─────────► SysTenantPackage (套餐，限制角色数)  │
└──────────────────────────────────────────────────────────────────┘
```

## 权限注解 4 件套

参见：[login-helper](../raw/common-source/login-helper.md)。

```java
@SaCheckLogin           // 1. 要求登录（默认所有 controller 都加）
@SaCheckRole("admin")   // 2. 要求角色
@SaCheckPermission("system:user:list")  // 3. 要求权限标识
@SaCheckDataScope       // 4. 要求数据权限范围（按部门过滤）
```

`@SaCheckPermission` 的 `value` 对应 SysMenu 的 `perms` 字段。

## 5 级数据权限范围

通过 `@SaCheckDataScope` + `DataPermissionInterceptor` 实现，存于 SysRole.dataScope：

| 取值 | 含义 | SQL 改写 |
|---|---|---|
| `1` (ALL) | 全部数据 | 无 WHERE 追加 |
| `2` (CUSTOM) | 自定义部门 | WHERE dept_id IN (用户自定义部门集合) |
| `3` (DEPT) | 本部门 | WHERE dept_id = 当前用户部门 |
| `4` (DEPT_AND_CHILD) | 本部门及下级 | WHERE dept_id IN (本部门 + 所有下级) |
| `5` (SELF) | 仅本人 | WHERE create_by = 当前用户 |

详见 [security-reviewer agent 文档](../../.claude/agents/security-reviewer.md) § P0 边界。

## SysMenu 的关键字段

```java
public class SysMenu {
    private String menuName;       // 菜单名（前端展示）
    private String perms;          // 权限标识（如 "system:user:list"）
    private String path;           // 路由路径
    private String component;      // 前端组件路径
    private String icon;           // 图标
    private Long parentId;       // 父菜单（树形）
    private Integer menuType;     // 类型：M（目录）/ C（菜单）/ F（按钮）
    private Integer visible;      // 是否显示
    private Integer status;        // 状态
    private String query;          // 路由参数
}
```

**菜单类型**：`M`（目录，包含子菜单）/ `C`（菜单，实际页面）/ `F`（按钮，权限控制点）

按钮型菜单（`F`）的 `perms` 是 `@SaCheckPermission` 检查的目标。

## 用户导入 — SysUserImportListener

参见 [sys-user-import-listener](../raw/system-source/sys-user-import-listener.md)。

基于 EasyExcel 的 `ReadListener<SysUserImportVo>`：

```java
@Slf4j
public class SysUserImportListener implements ReadListener<SysUserImportVo> {
    @Override
    public void invoke(SysUserImportVo user, AnalysisContext ctx) {
        // 单条处理：校验、加密密码、设置部门、insert
        userMapper.insert(user);
    }
    @Override
    public void doAfterAllAnalysed(AnalysisContext ctx) {
        // 完成后清理
    }
}
```

**性能瓶颈**：逐行入库（不是批量）。导入 1 万用户 ≈ 30 秒 + 1 万次 SQL。可改造点：批量缓存 → `insertBatch`。

## 用户行为审计 — UserActionListener

参见 [user-action-listener](../raw/system-source/user-action-listener.md)。

监听关键操作：
- 登录成功 / 失败
- 登出
- 密码修改
- 实名认证
- 租户切换

事件源：Spring `ApplicationEventPublisher` 发 `UserActionEvent`，listener 接 event → 写 `sys_oper_log` 表。

参见：[claude-md.md § Key Conventions — Demo mode](../raw/project-skeleton/claude-md.md)（演示模式拦截写操作）。

## 启动期任务 — SystemApplicationRunner

参见 [system-application-runner](../raw/system-source/system-application-runner.md)。

```java
@Component
public class SystemApplicationRunner implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        // 1. 缓存字典到 Redis
        // 2. 加载系统配置到内存
        // 3. 初始化租户清单
        // 4. 注册 nacos / consul 服务（如果有）
    }
}
```

实现 `ApplicationRunner`，**在 Spring Boot 完全启动后**立即执行。常用于缓存预热、配置加载。

**与 `@PostConstruct` 的区别**：
- `@PostConstruct`：bean 初始化时执行（早，可能依赖未就绪）
- `ApplicationRunner.run`：所有 bean 初始化完成后执行（晚，依赖就绪）

## CMS — CmsContent

参见 [entity-cms-content](../raw/system-source/entity-cms-content.md)。

最近新增的 CMS 模块（commit history: `feat: add coding harness runtime and CMS module`），与 AI 内容生成配合：

```java
public class CmsContent {
    private Long id;
    private String title;
    private String content;       // Markdown / 富文本
    private String contentType;   // article / script / dialogue
    private Long authorId;
    private String status;        // draft / published / archived
    private String tenantId;
    private Date createTime;
    private Date updateTime;
}
```

可能用途：
- AI 生成短剧脚本（与 `ShortDramaScriptAgent` 配合）
- AI 生成文章 / 博客
- 客服话术模板

**与 chat 模块的边界**：chat 是「生成」，system/CMS 是「存储 + 管理」。

## SysCache 与缓存策略

`SysCache` 实体是项目内 Redis 缓存的**统一封装**：

- `cacheKey`：缓存键（业务前缀）
- `cacheValue`：序列化值（JSON / String）
- `expire`：过期时间
- `region`：缓存域（系统配置 / 字典 / 用户权限等）

替代方案：`@Cacheable` 注解（`spring-boot-starter-cache`），更轻量。SysCache 是项目早期就有的手动缓存层。