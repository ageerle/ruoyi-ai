---
name: db-migration
description: 为 ruoyi-ai 数据库变更提供标准化流程。封装「新增业务表 / 加字段 / 加索引 / 新增 snailjob 任务 / 跨租户共享表登记」的完整模板与回滚脚本生成，避免漏改 entity / mapper / SQL / 租户白名单。
disable-model-invocation: true
---

# db-migration

RuoYi-AI 数据库变更标准化技能。

## 何时使用

- 新增业务表（必须新建 entity + mapper + SQL）
- 给已有表加字段 / 加索引 / 改类型
- 新增 / 修改 SnailJob 分布式定时任务
- 跨租户共享表（多租户过滤白名单）变更
- 涉及 `tenant.excludes` / `demo.excludes` 配置变更

调用方式：用户明确说"加个表" / "改字段" / "加索引" / "登记共享表" 时使用。

## 输入约定

调用前请提供：

1. **变更类型**：`add-table` / `add-column` / `add-index` / `add-job` / `register-shared`。
2. **业务名**：英文表名 / 字段名 / 任务名（snake_case）。
3. **业务描述**：中文说明（用于注释 / 文档）。
4. **目标模块**：变更归属哪个 `ruoyi-modules/*` 或 `ruoyi-common/*`。
5. **租户策略**：`tenant`（默认加租户列）/ `shared`（不租户隔离，需登记 `tenant.excludes`）/ `system`（系统表，已在白名单）。

## 项目约定（必须遵守）

### 表命名

- 表名小写 + 下划线：`sys_user`、`chat_session`、`knowledge_doc`
- 模块前缀：`sys_*`（系统）、`chat_*`（chat 模块）、`flow_*`（workflow 模块）、`aiflow_*`（aiflow 模块）、`trace_*`（trace 模块）
- 主键：`id BIGINT`，雪花算法（MyBatis-Plus `ASSIGN_ID`）
- 必备字段：

```sql
create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
create_by    BIGINT       DEFAULT NULL              COMMENT '创建人',
update_by    BIGINT       DEFAULT NULL              COMMENT '更新人',
tenant_id    VARCHAR(20)  DEFAULT '000000'          COMMENT '租户ID',
del_flag     CHAR(1)      DEFAULT '0'               COMMENT '删除标志（0正常 1删除）',
remark       VARCHAR(500) DEFAULT NULL              COMMENT '备注',
```

### Entity 必备字段

```java
@TableName(value = "your_table", autoResultMap = true)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class YourEntity extends BaseEntity {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField("tenant_id")
    private String tenantId;

    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    @TableField("remark")
    private String remark;

    // 业务字段 ...
}
```

`BaseEntity` 位于 `ruoyi-common-core`，已含 id + 4 个时间字段，子类只需加业务字段。

### Mapper

```java
public interface YourMapper extends BaseMapper<YourEntity> {
    // 单表 CRUD 由 BaseMapper 提供，无需手写
    // 复杂查询才写 XML（resources/mapper/your/YourMapper.xml）
}
```

### 必备配置文件

#### 1. `tenant.excludes`（如选 `shared`）

`ruoyi-admin/src/main/resources/application.yml:148-161` 的 `tenant.excludes` 数组追加新表名。

#### 2. `demo.excludes`（如写操作需绕过演示模式）

`application.yml:296-308` 的 `demo.excludes` 追加新 controller 路径。

#### 3. 多数据源（如用 `dynamic-datasource`）

新表归属哪个数据源在 `application-*.yml` 的 `spring.datasource.dynamic.datasource.master` 配置里检查，必要时加新数据源。

#### 4. 代码生成（ruoyi-generator）

如使用 `ruoyi-generator` 生成代码，表名要登记到 `gen_table` 表。

## 模板

### 场景 A：新增业务表

#### 输出文件清单

1. `docs/sql/migration/V<version>__add_<table>.sql` —— DDL 脚本
2. `ruoyi-modules/<module>/src/main/java/org/ruoyi/<module>/domain/<Entity>.java` —— 实体
3. `ruoyi-modules/<module>/src/main/java/org/ruoyi/<module>/mapper/<Entity>Mapper.java` —— mapper
4. `ruoyi-modules/<module>/src/main/resources/mapper/<module>/<Entity>Mapper.xml` —— 复杂 SQL
5. `application.yml` —— 必要时加 `tenant.excludes` / `demo.excludes`
6. `ruoyi-modules/<module>/src/main/java/org/ruoyi/<module>/service/I<Entity>Service.java` + impl —— 业务层
7. `ruoyi-modules/<module>/src/main/java/org/ruoyi/<module>/controller/<Entity>Controller.java` —— REST 入口
8. `@Tag("dev")` Service 单测（按 `gen-test` skill）

#### DDL 模板

```sql
-- V3.1.0__add_chat_session.sql
-- 业务：会话主表（用于 chat 模块持久化对话历史）
-- 变更：新增
-- 影响范围：chat 模块；租户隔离；演示模式白名单需登记

DROP TABLE IF EXISTS chat_session;
CREATE TABLE chat_session (
    id           BIGINT       NOT NULL                  COMMENT '主键',
    session_id   VARCHAR(64)  NOT NULL                  COMMENT '会话ID',
    user_id      BIGINT       NOT NULL                  COMMENT '用户ID',
    agent_id     BIGINT       DEFAULT NULL              COMMENT 'Agent ID',
    title        VARCHAR(200) DEFAULT '新对话'           COMMENT '会话标题',
    status       CHAR(1)      DEFAULT '0'               COMMENT '状态（0正常 1关闭）',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by    BIGINT       DEFAULT NULL              COMMENT '创建人',
    update_by    BIGINT       DEFAULT NULL              COMMENT '更新人',
    tenant_id    VARCHAR(20)  DEFAULT '000000'           COMMENT '租户ID',
    del_flag     CHAR(1)      DEFAULT '0'                COMMENT '删除标志',
    remark       VARCHAR(500) DEFAULT NULL               COMMENT '备注',
    PRIMARY KEY (id),
    UNIQUE KEY uk_session_id (session_id, tenant_id),
    KEY idx_user_id (user_id),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '聊天会话表';
```

### 场景 B：加字段

```sql
-- V3.1.1__add_chat_session_metadata.sql
ALTER TABLE chat_session
    ADD COLUMN metadata JSON DEFAULT NULL COMMENT '元数据（自定义 KV）' AFTER status,
    ADD COLUMN token_used INT DEFAULT 0 COMMENT '累计 token 消耗' AFTER metadata;
```

Entity 同步：

```java
@TableField("metadata")
private String metadata;  // JSON 字符串

@TableField("token_used")
private Integer tokenUsed;
```

### 场景 C：加索引

```sql
-- V3.1.2__idx_chat_session_tenant_status.sql
CREATE INDEX idx_chat_session_tenant_status ON chat_session (tenant_id, status);
```

**注意**：MyBatis-Plus 自动建表只加 `@TableField` 字段，**不会自动加索引**。索引必须走 DDL 脚本。

### 场景 D：登记共享表

`application.yml` 改 `tenant.excludes`：

```yaml
tenant:
  excludes:
    # ... 现有列表 ...
    - chat_session_demo     # 演示数据，跨租户共享
```

同步登记 `sa-token` 的角色权限（如果新表带权限控制）：

```sql
INSERT INTO sys_menu (menu_name, perms, menu_type, ...) VALUES ('聊天会话', 'chat:session:list', 'F', ...);
```

### 场景 E：新增 SnailJob 任务

1. 注册任务组 / 任务：`@SnailJob` 注解 + 实现类
2. SQL 插入（`snail_job_*` 表由 SnailJob 自动建，**通常不用手动**）

```java
@SnailJob(name = "清理过期会话", cron = "0 0 2 * * ?")
@Component
public class ChatSessionCleanupJob implements JobExecutor {
    @Override
    public SnailJobResult execute(JobArgs args) {
        // ... 业务逻辑
        return new SnailJobResult(true, "清理 N 条");
    }
}
```

## 回滚脚本

每个变更脚本**必须**配 `V<version>__rollback_<name>.sql`：

```sql
-- V3.1.0__rollback_add_chat_session.sql
DROP TABLE IF EXISTS chat_session;
```

字段变更回滚：

```sql
ALTER TABLE chat_session
    DROP COLUMN metadata,
    DROP COLUMN token_used;
```

## 必做检查清单

每次变更交付前确认：

- [ ] Entity 已加 `extends BaseEntity`
- [ ] `@TableLogic` 字段已加（如果表需要逻辑删除）
- [ ] Mapper 已 `extends BaseMapper<Entity>`
- [ ] 主键策略 `IdType.ASSIGN_ID`
- [ ] 多租户字段 `tenant_id` 已加（如果表需租户隔离）
- [ ] 已更新 `tenant.excludes`（如果选 shared）
- [ ] 已更新 `demo.excludes`（如果新 controller 写操作）
- [ ] 索引已加（高频查询字段）
- [ ] SQL 注释完整（COMMENT）
- [ ] 回滚脚本已写
- [ ] Service 单测已加（按 `gen-test` skill）

## 禁止清单

- ❌ 用 `@TableField(exist = false)` 跳过字段映射（真没加字段就该加 DDL）
- ❌ Entity 不 `extends BaseEntity`（破坏统一基类约定）
- ❌ 表名加 `t_` 前缀（项目统一不加）
- ❌ 主键用 `AUTO_INCREMENT`（必须雪花）
- ❌ 用 `tinyint` / `int` 存布尔值（项目统一用 `char(1) '0'|'1'`）
- ❌ 删表 / 删字段不写回滚脚本
- ❌ 改 `tenant.excludes` 不通知用户（影响多租户隔离，事故级别 P0）
- ❌ 直接在生产数据库 `ALTER TABLE`（必须走评审 + 备份）

## 输出交付物

调用完成后必须产出：

1. 完整文件清单（DDL、Entity、Mapper、Service、Controller、配置）。
2. DDL 脚本 + 回滚脚本。
3. 必做检查清单的自检结果。
4. 影响范围报告（哪些模块、哪些接口、哪些前端）。
5. 灰度建议（如适用）。