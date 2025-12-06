# 数据库开发规范

## 1. 建表规约

### 1.1 表命名规范

**【强制】** 表名、字段名必须使用小写字母或数字，禁止出现数字开头，禁止两个下划线中间只出现数字。

**【强制】** 表名采用 `业务-模块-功能` 的命名方式，使用下划线分隔。

**正例：**

```sql
-- 聊天业务-配置模块-配置功能
chat_config

-- 聊天业务-消息模块-消息功能  
chat_message

-- 聊天业务-模型模块-模型功能
chat_model

-- 系统业务-用户模块-用户功能
sys_user

-- 系统业务-角色模块-角色功能
sys_role

-- 知识库业务-角色模块-角色功能
knowledge_role
```

**反例：**

```sql
-- 不规范的命名
chatConfig
ChatMessage
chat-model
user1
2user
user__info
```

### 1.2 字段命名规范

**【强制】** 字段名必须使用小写字母或数字，使用下划线分隔。

**【推荐】** 表达是与否概念的字段，必须使用 `is_xxx` 的方式命名，数据类型是 `char(1)`，1 表示是，0 表示否。

**正例：** `is_deleted`、`is_enabled`

## 2. 公共字段规范

### 2.1 必备公共字段

**【推荐】** 表增加以下公共字段：

| 字段名           | 类型             | 默认值            | 说明            | 是否必须  |
|---------------|----------------|----------------|---------------|-------|
| `id`          | `bigint(20)`   | AUTO_INCREMENT | 主键ID          | 是     |
| `create_time` | `datetime`     | NULL           | 创建时间          | 是     |
| `update_time` | `datetime`     | NULL           | 更新时间          | 是     |
| `create_by`   | `bigint(20)`   | NULL           | 创建者ID         | 是     |
| `update_by`   | `bigint(20)`   | NULL           | 更新者ID         | 是     |
| `create_dept` | `bigint(20)`   | NULL           | 创建部门ID        | 是     |
| `del_flag`    | `char(1)`      | '0'            | 删除标志（0存在 1删除） | 推荐    |
| `tenant_id`   | `varchar(20)`  | '000000'       | 租户编号          | 多租户必须 |
| `remark`      | `varchar(500)` | NULL           | 备注            | 是     |
| `version`     | `int(11)`      | NULL           | 版本号（乐观锁）      | 可选    |

### 2.2 公共字段说明

- **`id`**: 主键，使用雪花算法生成的 bigint 类型
- **`create_time`**: 记录创建时间，便于数据追踪和审计
- **`update_time`**: 记录最后更新时间，便于数据同步和缓存失效
- **`create_by`**: 创建者用户ID，便于权限控制和数据追溯
- **`update_by`**: 更新者用户ID，便于操作审计
- **`del_flag`**: 逻辑删除标志，0表示正常，1表示删除
- **`tenant_id`**: 租户隔离字段，支持多租户架构
- **`remark`**: 备注信息，便于业务说明

## 3. SQL 更新管理规范

### 3.1 目录结构

```
script/
├── sql/
│   ├── ruoyi-ai.sql          # 初始化SQL文件
│   └── update/               # 增量更新SQL目录
│       ├── 2024-05-24-chat-message-billing-type.sql
│       ├── 2024-07-13-chat-model-priority.sql
│       └── 2024-08-15-knowledge-role-bak.sql
└── deploy/
    └── deploy/
        └── mysql-init/
            └── ruoyi-ai.sql  # Docker初始化SQL（与主文件同步）
```

### 3.2 更新SQL规范

**【强制】** 增量更新SQL文件必须放在 `script/sql/update/` 目录下。

**【强制】** 更新SQL文件命名格式：`YYYY-MM-DD-功能描述.sql`

**正例：**

```
2024-05-24-chat-message-billing-type.sql
2024-07-13-chat-model-priority.sql
2024-08-15-knowledge-role-backup.sql
```

**【强制】** 每个更新SQL文件必须包含：

- 文件头部注释说明变更内容
- 变更日期和负责人
- 具体的DDL/DML语句

**正例：**

```sql
-- 为 chat_message 表添加 billing_type 字段
-- 变更日期: 2024-05-24
-- 负责人: 张三
-- 说明: 支持消息计费类型区分

ALTER TABLE chat_message
    ADD COLUMN billing_type char NULL COMMENT '计费类型（1-token计费，2-次数计费，null-普通消息）';
```

### 3.3 部署流程

#### 3.3.1 首次部署

**【强制】** 首次初始化项目只需要执行：

```bash
mysql -u root -p database_name < script/sql/ruoyi-ai.sql
```

#### 3.3.2 增量更新

**【强制】** 代码更新时，按日期顺序执行 `script/sql/update/` 下的补丁SQL：

```bash
# 按文件名日期顺序执行
mysql -u root -p database_name < script/sql/update/2024-05-24-chat-message-billing-type.sql
mysql -u root -p database_name < script/sql/update/2024-07-13-chat-model-priority.sql
```

#### 3.3.3 同步更新

**【强制】** 当数据库发生变化时，必须同时完成以下操作：

1. 在 `script/sql/update/` 下添加增量SQL补丁
2. 将变更同步更新到初始化文件 `script/sql/ruoyi-ai.sql`

---
>
> 最后更新时间：2025-11-07
