---
topic: modules/generator
title: ruoyi-generator — 代码生成器
updated: 2026-09-04
raw:
  - raw/generator-source/gen-controller.md
  - raw/generator-source/i-gen-table-service.md
---

# ruoyi-generator — 代码生成器

`ruoyi-generator` 是**代码生成器模块**（13 个 Java 文件，最轻量）。基于 Velocity + 数据库元数据，自动生成 entity / mapper / service / controller / vue 模板。

参见：[pom-xml.md § velocity](../raw/project-skeleton/pom-xml.md)。

## 核心思路

```
数据库表（gen_table）
   ↓
Velocity 模板（vm/java/...)
   ↓
代码（Entity / Mapper / Service / Controller / VO）
```

1. 导入数据库表结构到 `gen_table` 表
2. 配置生成参数（包名、模块名、作者等）
3. 选择模板（默认有 4-5 套）
4. 批量生成代码 zip

## 关键 Controller

参见：[gen-controller](../raw/generator-source/gen-controller)。

入口是 `GenController`，路由 `/tool/gen`，提供：

- 导入表：`/importTable`
- 同步表结构：`/synchDb`
- 生成代码：`/download?tableIds=1,2,3`
- 删除表：`/remove`
- 修改生成配置：`/edit`

## 关键 Service

参见：[i-gen-table-service](../raw/generator-source/i-gen-table-service)。

`IGenTableService` 提供：

- `selectGenTableList`（分页查询）
- `selectDbTableList`（从 information_schema 查数据库表）
- `importGenTable`（导入表到 `gen_table`）
- `generatorCode`（调用 Velocity 生成）

## 模板位置

Velocity 模板在 `resources/templates/` 下，按目录组织：

```
vm/
├── java/
│   ├── Domain.java.vm       # Entity
│   ├── Mapper.java.vm       # Mapper interface
│   ├── Service.java.vm      # IxxxService
│   ├── ServiceImpl.java.vm  # xxxServiceImpl
│   ├── Controller.java.vm   # Controller
│   ├── Bo.java.vm           # Business Object
│   └── Vo.java.vm           # View Object
├── vue/                     # Vue 3 + Element Plus
│   ├── index.vue.vm
│   └── api.js.vm
└── sql/
    └── menu.sql.vm          # 菜单 SQL
```

**注意**：模板细节没有现成 raw 文件，本节基于通用 RuoYi 框架惯例，具体以仓库实际为准。

## 生成策略

| 生成目标 | 是否生成 |
|---|---|
| Java Entity（含 Swagger 注解、MyBatis-Plus 注解） | ✅ |
| MyBatis-Plus Mapper interface + XML | ✅ |
| Service interface + Impl | ✅ |
| Controller（含 @SaCheckPermission 自动注解） | ✅ |
| VO / BO / DTO | ✅ |
| Vue 3 页面（index.vue + api.js） | ✅ |
| 菜单 SQL（插入 sys_menu 记录） | ✅ |
| 数据库 DDL | ❌（由 MyBatis-Plus 自动建表） |

## 关键约束

- 表必须有主键（雪花 `ASSIGN_ID`）
- 表必须有 `create_time` / `update_time`（自动填充）
- 表必须有 `del_flag`（逻辑删除）
- 字段类型映射：MySQL → Java / TS 严格对照（`tinyint(1)` → `Boolean`）

## 与 db-migration skill 的关系

db-migration skill（我们装的 `.claude/skills/db-migration/SKILL.md`）封装的是**手工迁移**流程（DDL + 实体同步）。generator 用于**新表**的快速生成。两者配合：

1. 用 generator 跑出新表的 entity + mapper + service + controller
2. 用 db-migration 风格的 DDL 脚本落库
3. 用 generator 生成的菜单 SQL 注册到 sys_menu

参见：[claude-md.md § Skills — db-migration / ai-module-add](../raw/project-skeleton/claude-md.md)。