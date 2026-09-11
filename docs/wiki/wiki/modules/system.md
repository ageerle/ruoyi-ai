---
topic: modules/system
title: ruoyi-system — RBAC 与系统管理
updated: 2026-09-04
raw:
  - raw/system-source/sys-user-controller.md
  - raw/system-source/sys-role-controller.md
  - raw/system-source/sys-menu-controller.md
  - raw/system-source/sys-user-import-listener.md
  - raw/system-source/user-action-listener.md
  - raw/system-source/system-application-runner.md
  - raw/system-source/entity-sys-user.md
  - raw/system-source/entity-cms-content.md
---

# ruoyi-system — RBAC 与系统管理

`ruoyi-system` 是项目的**系统管理模块**，承担 4 类核心职责：

1. **RBAC**（用户 / 角色 / 菜单 / 部门 / 岗位 / 字典）
2. **认证与登录**（`SysLoginService`）
3. **系统配置**（`SysConfig` / `SysOssConfig` / `SysDict`）
4. **CMS**（`CmsContent`，最近新增）

181 个 Java 文件，包结构：

```
org.ruoyi.system/
├── controller/             # 23 个 REST controller
│   ├── monitor/            # 监控（在线用户、操作日志）
│   └── system/             # RBAC / CMS / 社交 / 客户端
├── service/                # 53 个业务 service（含 IxxxService + impl）
├── domain/                 # 78 个实体（entity / bo / vo / dto）
├── mapper/                 # 23 个 MyBatis-Plus mapper
├── listener/               # 2 个事件监听器
│   ├── SysUserImportListener.java   # EasyExcel 用户导入监听
│   └── UserActionListener.java      # 用户行为审计
├── runner/
│   └── SystemApplicationRunner.java # 启动期任务
└── utils/
```

参见：[pom-xml.md § modules](../raw/project-skeleton/pom-xml.md)、[claude-md.md § Module Layout](../raw/project-skeleton/claude-md.md)。

## RBAC 模型

5 个核心实体：

| 实体 | 作用 |
|---|---|
| `SysUser` | 用户（含租户字段、登录失败计数） |
| `SysRole` | 角色（含权限字符串、数据权限范围） |
| `SysMenu` | 菜单（含权限标识 `perms`，路由、组件路径） |
| `SysDept` | 部门（树形结构） |
| `SysPost` | 岗位（与用户多对多） |

中间表：`SysUserRole`、`SysRoleMenu`、`SysUserPost`、`SysRoleDept`。

参见：[entity-sys-user](../raw/system-source/entity-sys-user)。

### 权限校验机制

- `@SaCheckLogin`：要求登录
- `@SaCheckRole("admin")`：要求角色
- `@SaCheckPermission("system:user:list")`：要求权限标识（菜单的 `perms` 字段）
- `@SaCheckDataScope`：要求数据权限范围（all / 自定义 / 本部门 / 本部门及下级 / 本人）

`Sa-Token` 配置参见 [application-yml.md § sa-token](../raw/project-skeleton/application-yml.md)。

## 23 个 controller 全景

```
SysUserController          用户管理（增删改查、导入导出、密码重置）
SysRoleController          角色管理
SysMenuController          菜单管理（树形）
SysDeptController          部门管理（树形）
SysPostController          岗位管理
SysDictTypeController      字典类型
SysDictDataController      字典数据
SysConfigController        参数配置
SysNoticeController        通知公告
SysOssController           OSS 对象存储
SysOssConfigController     OSS 配置
SysSocialController        社交账号绑定
SysClientController        客户端管理
SysTenantController        租户管理（开关在 application.yml tenant.enable）
SysTenantPackageController  租户套餐
SysProfileController       个人中心（改密、改资料）
SysRegisterController      注册
SysCaptchaController       验证码
ChatConfigController       聊天配置
CmsContentController       CMS 内容
SysUserOnlineController    在线用户
SysOperLogController       操作日志
SysLoginLogController      登录日志
```

参见：[sys-user-controller](../raw/system-source/sys-user-controller)、[sys-role-controller](../raw/system-source/sys-role-controller)、[sys-menu-controller](../raw/system-source/sys-menu-controller)。

## 关键事件监听器

### SysUserImportListener（EasyExcel 集成）

```java
public class SysUserImportListener implements ReadListener<SysUserImportVo> {
    // 每读到一行就处理（可批量优化）
    @Override
    public void invoke(SysUserImportVo user, AnalysisContext context) { ... }
}
```

基于 EasyExcel 的 `ReadListener`，导入用户 Excel 时**逐行处理**（不是全量读完后批量）。注意：项目里可能是单条入库，未做批量优化——大文件导入可能慢。

参见：[sys-user-import-listener](../raw/system-source/sys-user-import-listener)。

### UserActionListener（用户行为审计）

```java
public class UserActionListener { ... }
```

监听用户登录、登出、密码修改、敏感操作等，写入 `sys_oper_log` 表。

参见：[user-action-listener](../raw/system-source/user-action-listener)。

## 启动期任务 — SystemApplicationRunner

```java
@Component
public class SystemApplicationRunner implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        // 缓存字典、加载配置、初始化租户等
    }
}
```

实现 `ApplicationRunner`，Spring Boot 启动完成后立即执行。常用于：
- 加载字典到 Redis
- 预热配置
- 初始化多租户数据
- 注册自定义组件

参见：[system-application-runner](../raw/system-source/system-application-runner)。

## CMS（最近新增）

`CmsContent.java`（参见 [entity-cms-content](../raw/system-source/entity-cms-content)）+ `CmsContentController` 是**最近新增**的内容管理模块（commit history 显示 `feat: add coding harness runtime and CMS module`）。

设计目标可能是 AI 生成内容的存储（如 AI 生成的短剧脚本、文章等），与 `ruoyi-chat` 的内容生成配合使用。

## 多租户隔离

`SysUser` 实体含 `tenantId` 字段（继承自 `BaseEntity`）。所有 user / role / menu 查询都自动加 `tenantId` 过滤（MyBatis-Plus 多租户拦截器）。

**例外**：`sys_menu` / `sys_tenant` / `sys_role_menu` 等在 `tenant.excludes` 白名单里（参见 [application-yml.md § tenant.excludes](../raw/project-skeleton/application-yml.md)）——菜单需要跨租户共享（不然切换租户看不到菜单）。

## 与 ruoyi-chat 的边界

`ruoyi-system` 是**业务系统层**，`ruoyi-chat` 是**AI 层**。两者的关系：

- 系统用户 → 在 `SysUser` 里有 `userId`
- 聊天会话 → 在 chat 模块里，但 `userId` 字段关联到 `SysUser.id`
- AI 配额 / 计费 → 在 system 里通过 `SysConfig` 配置，chat 模块读取

**注意权限**：`SysLoginService` 处理登录（admin 模块的 `AuthController` 调用），chat 模块的 SSE 接口在 `demo.excludes` 白名单里允许写操作。