# PostgreSQL → MySQL 字段类型映射表

> 本文件是**新增**的工作手册。开发说明书按 PostgreSQL 设计（`TIMESTAMPTZ`、`UUID`、`JSONB`、`enum`），本仓库基线用 MySQL 8.0。本表提供**完整**的字段类型映射与适配决策。

---

## 一、基础字段映射表

### 1.1 主键与字符串

| PostgreSQL 类型 | MySQL 类型 | 决策 | 决策依据 |
|---|---|---|---|
| `UUID` 主键 | **`BIGINT(20) NOT NULL AUTO_INCREMENT`** + 雪花 ID 生成器 | ✅ **沿用 BIGINT 雪花**（不用 UUID 字符串） | 性能更好 + 与 RuoYi-AI 基线一致；UUID 字符串索引性能差且占用空间大 |
| `VARCHAR(n)` | `VARCHAR(n)` | ✅ 直接对应 | — |
| `TEXT` | `TEXT` 或 `MEDIUMTEXT`（> 64KB） | ✅ 直接对应 | MySQL `TEXT` 最大 64KB；超过用 `MEDIUMTEXT`（16MB） |
| `CHAR(n)` | `CHAR(n)` | ✅ 直接对应 | — |

### 1.2 数字与金额

| PostgreSQL 类型 | MySQL 类型 | 决策 | 决策依据 |
|---|---|---|---|
| `INTEGER` / `INT4` | `INT` 或 `BIGINT` | ✅ 直接对应 | — |
| `BIGINT` / `INT8` | `BIGINT(20)` | ✅ 直接对应 | — |
| `SMALLINT` / `INT2` | `SMALLINT` | ✅ 直接对应 | — |
| `DECIMAL(p, s)` | `DECIMAL(p, s)` | ✅ 直接对应 | **禁止 float / double**（开发说明书 §6.3） |
| `NUMERIC(p, s)` | `DECIMAL(p, s)` | ✅ 直接对应 | MySQL 等价 |
| `REAL` / `FLOAT4` | `FLOAT` | ⚠️ 不推荐 | 用 `DECIMAL` 替代 |
| `DOUBLE PRECISION` / `FLOAT8` | `DOUBLE` | ⚠️ 不推荐 | 用 `DECIMAL` 替代 |
| `SERIAL` / `BIGSERIAL` | `BIGINT NOT NULL AUTO_INCREMENT` | ✅ 等价 | 自增序列 |
| `BOOLEAN` | `TINYINT(1)`（0/1）或 `CHAR(1)` | ⚠️ 项目用 `CHAR(1) DEFAULT '0'` | RuoYi-AI 风格（参见 `SysXxx.delFlag`） |
| `REAL[]` / 数组 | JSON 或 关联表 | ⚠️ 不直接对应 | 用中间表（一对多） |

### 1.3 时间字段

| PostgreSQL 类型 | MySQL 类型 | 决策 | 决策依据 |
|---|---|---|---|
| `TIMESTAMPTZ`（带时区） | **`DATETIME`**（不带时区） | ✅ **存 UTC 业务时间** | MySQL `DATETIME` 不带时区；应用层做时区转换 |
| `TIMESTAMP`（不带时区） | `DATETIME` | ✅ 直接对应 | — |
| `DATE` | `DATE` | ✅ 直接对应 | — |
| `TIME` | `TIME` | ✅ 直接对应 | — |
| `INTERVAL` | `BIGINT`（存秒数） 或 `VARCHAR(64)` | ⚠️ 用秒数 | MySQL 无原生 INTERVAL；推荐存秒数（业务计算方便） |
| `now()` 默认值 | `DEFAULT CURRENT_TIMESTAMP` | ✅ 直接对应 | MySQL 等价 |

**时间存储约定（重要）**：

| 场景 | MySQL 字段 | 默认值 |
|---|---|---|
| 创建时间 | `create_time DATETIME` | `DEFAULT CURRENT_TIMESTAMP` |
| 更新时间 | `update_time DATETIME` | `DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` |
| 业务时间锚点（如上市日期 L08） | `launch_date DATE` 或 `DATETIME` | `NULL`（业务录入，非自动） |
| 截止时间（如 gate 签署 dueAt） | `due_at DATETIME` | `NULL` |

### 1.4 JSON / 结构化字段

| PostgreSQL 类型 | MySQL 类型 | 决策 | 决策依据 |
|---|---|---|---|
| `JSONB` | **`JSON`** | ✅ **用 MySQL 5.7+ 原生 JSON** | 功能对等；查询略弱（无 JSONB 索引运算符）但够用 |
| `JSON` | `JSON` | ✅ 直接对应 | — |
| `JSONB[]` | `JSON` + JSON 函数 | ⚠️ 不推荐 | 用关联表替代 |

### 1.5 枚举与字典

| PostgreSQL 类型 | MySQL 类型 | 决策 | 决策依据 |
|---|---|---|---|
| `ENUM` 类型（CREATE TYPE） | **`VARCHAR(32)` 或 `CHAR(1)`** + 字典表 | ✅ **用字典表 `sys_dict_data`** | RuoYi-AI 已有 `sys_dict_data` + `sys_dict_type`；便于超管后台维护（G-05） |
| 自定义 `enum` 类型 | 应用层 `@EnumValue` + MyBatis-Plus 枚举映射 | ✅ 通用模式 | — |

**字典表使用规范**：

```
sys_dict_type（字典类型）
  - id PK
  - dict_type VARCHAR(100) UNIQUE  -- 例如 'project.status'、'bonus.tier'
  - dict_name VARCHAR(100)
  - status CHAR(1) DEFAULT '0'

sys_dict_data（字典值）
  - id PK
  - dict_type VARCHAR(100)  -- 关联 sys_dict_type.dict_type
  - dict_label VARCHAR(100)
  - dict_value VARCHAR(100)
  - is_default CHAR(1)
  - status CHAR(1) DEFAULT '0'
```

### 1.6 其他特殊类型

| PostgreSQL 类型 | MySQL 类型 | 决策 | 决策依据 |
|---|---|---|---|
| `BYTEA`（二进制） | `BLOB` 或 `MEDIUMBLOB` | ✅ 直接对应 | 文件存储推荐 MinIO（`sys_oss` 表）而不是 DB |
| `CIDR` / `INET`（IP） | `VARCHAR(45)` | ✅ 用字符串 | MySQL 无原生 IP 类型 |
| `MACADDR` | `VARCHAR(17)` | ✅ 用字符串 | — |
| `GEOMETRY` / `GEOGRAPHY` | `GEOMETRY`（MySQL 8.0+） | ⚠️ 不推荐 | 地理信息存 PostGIS 或专用服务 |
| `JSON PATH` | `VARCHAR(255)` 或 `JSON` | ⚠️ 不推荐 | MySQL 无原生 JSONPath |

---

## 二、公共字段约定（每张表都有）

```
id              BIGINT(20) NOT NULL AUTO_INCREMENT   -- 雪花算法（BaseEntity.id）
tenant_id       VARCHAR(20) DEFAULT '000000'         -- 多租户（BaseEntity.tenantId）
create_time     DATETIME DEFAULT CURRENT_TIMESTAMP
update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
create_by       BIGINT(20) DEFAULT NULL
update_by       BIGINT(20) DEFAULT NULL
del_flag        CHAR(1) DEFAULT '0'                   -- 逻辑删除（BaseEntity.delFlag）
remark          VARCHAR(500) DEFAULT NULL             -- 备注（BaseEntity.remark）
```

**Java 端 `BaseEntity`（已在 `ruoyi-common-chat` 实现）**：

```java
@Data
public class BaseEntity implements Serializable {
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

    @TableField(value = "del_flag", fill = FieldFill.INSERT)
    @TableLogic
    private String delFlag;

    @TableField(value = "remark")
    private String remark;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private String tenantId;
}
```

---

## 三、关键决策详解

### 3.1 为什么不用 UUID？

| 维度 | BIGINT 雪花 | UUID v4 |
|---|---|---|
| 存储大小 | 8 字节 | 16 字节 + 索引 20+ |
| 索引性能 | ✅ 优 | ⚠️ 差（随机分布，B+树分裂严重） |
| 跨表关联 | ✅ 直接 `=` 比较 | ⚠️ 字符串比较 + 索引前缀截断 |
| 安全（不可猜测） | ✅ 64 位随机 | ✅ 128 位随机 |
| 与 RuoYi-AI 一致 | ✅ | ❌ 需新建生成器 |
| 已有 `IdType.ASSIGN_ID` | ✅ | ❌ 需 `IdType.ASSIGN_UUID` |

**结论**：沿用 BIGINT 雪花。仅在对外暴露的不可猜测 ID（如游客查询码 `BR-REQ-03` 的 8 位查询码）单独用 `SecureRandom` 生成。

### 3.2 为什么用 DATETIME 不带时区？

| 维度 | `DATETIME`（不带时区） | `TIMESTAMP`（带时区，4 字节到 2038） | `TIMESTAMPTZ`（PG 带时区） |
|---|---|---|---|
| 时区处理 | 应用层 | 数据库自动转换 | 数据库自动转换 |
| 2038 问题 | ✅ 无 | ⚠️ 有（4 字节到 2038） | ✅ 无（8 字节） |
| MySQL 支持 | ✅ 原生 | ✅ 原生 | ❌ 无（用 DATETIME 替代） |
| 应用层可控 | ✅ | ⚠️ 受 DB 影响 | — |

**结论**：用 `DATETIME`，应用层统一 UTC 存储 + 业务时区转换。MySQL `TIMESTAMP` 有 2038 年问题。

### 3.3 为什么 JSON 而不是 TEXT 存 JSON 字符串？

| 维度 | `JSON` | `TEXT` |
|---|---|---|
| 数据校验 | ✅ MySQL 自动校验 JSON 合法性 | ❌ 不校验 |
| 查询能力 | ✅ `JSON_EXTRACT`、`->` 运算符 | ⚠️ 需应用层解析 |
| 索引能力 | ✅ 可建 JSON 多值索引 | ⚠️ 全文索引 |
| 与 PostgreSQL JSONB 兼容性 | ✅ 字段语义对等 | ⚠️ 类型不一致 |

**结论**：用 MySQL 5.7+ 原生 `JSON` 类型。

### 3.4 为什么不直接用 MySQL ENUM？

| 维度 | MySQL `ENUM` | 字典表 `sys_dict_data` |
|---|---|---|
| 修改枚举值 | ⚠️ `ALTER TABLE`（锁表） | ✅ 后台 UI 改 |
| 添加新枚举值 | ⚠️ `ALTER TABLE` | ✅ 后台 UI 加 |
| 国际化 | ⚠️ 需另存 label | ✅ dict_label 支持 i18n |
| 超管维护 | ❌ 需要 DDL 权限 | ✅ 后台即可 |
| 与文档 G-05 一致 | ❌ 违反 | ✅ 完全一致 |

**结论**：**禁止 MySQL ENUM**，全部走字典表 `sys_dict_data`。

---

## 四、特殊场景适配

### 4.1 涉钱字段

所有金额一律 `DECIMAL(18, 2)`，**禁止 float / double / real / numeric**（开发说明书 §6.3）：

```sql
bonus_pool      DECIMAL(18, 2) NOT NULL DEFAULT 0
allowance       DECIMAL(18, 2) NOT NULL DEFAULT 0
sales_actual    DECIMAL(18, 2) NOT NULL DEFAULT 0
level_coefficient DECIMAL(5, 2) NOT NULL DEFAULT 1.0
```

**前端显示**：`font-variant-numeric: tabular-nums`（开发说明书 _公共规范.md §四）

### 4.2 时间范围字段（gate 签署 dueAt / 任务到期）

```sql
due_at          DATETIME DEFAULT NULL     -- 系统计算（不让人工填）
deadline        DATETIME DEFAULT NULL     -- 业务截止（人工填）
```

### 4.3 字典值字段（状态 / 类型 / 级别）

```sql
-- 不存 enum 字符串本身，而是字典的 dict_label
status          VARCHAR(32) NOT NULL DEFAULT 'DRAFT'
project_level   VARCHAR(8)   -- 'S' / 'A' / 'B'

-- 字典类型
INSERT INTO sys_dict_type (dict_type, dict_name) VALUES
  ('project.status', '项目状态'),
  ('project.level', '项目等级'),
  ('bonus.tier', '奖金阶梯档位'),
  ('gate.decision', 'Gate 要素判定');  -- ✅/⚠️/❌
```

### 4.4 i18n 文本字段（开发说明书 G-07）

```sql
title_zh_cn     VARCHAR(200)  -- 中文
title_en_us     VARCHAR(200) DEFAULT NULL  -- 预留，二期填
description     JSON  -- {"zh-CN": "...", "en-US": null}
```

**注意**：一期只填 `zh-CN`（G-07），但字段预留扩展能力。

### 4.5 国别认证清单（cert_templates）

```sql
country_code    VARCHAR(8)   -- 'SA' / 'US' / 'CN' / 'JP'
auth_type       VARCHAR(32)  -- 'SABER' / 'FCC' / 'CCC' / 'PSE'
required        TINYINT(1)   -- 0 否 / 1 是
sort            INT          -- 显示顺序
```

---

## 五、迁移工具

### 5.1 DDL 生成

Velocity 模板（沿用 `ruoyi-generator`）：

```java
// 推荐放在 ruoyi-generator 的 templates/mysql/
// 模板：vm/sql/table-ddl.sql.vm

CREATE TABLE \${tableName} (
    id              BIGINT(20) NOT NULL AUTO_INCREMENT,
    tenant_id       VARCHAR(20) DEFAULT '000000' COMMENT '租户ID',
    \${columns}    -- 由 entity 自动生成
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT(20) DEFAULT NULL COMMENT '创建人',
    update_by       BIGINT(20) DEFAULT NULL COMMENT '更新人',
    del_flag        CHAR(1) DEFAULT '0' COMMENT '删除标志',
    remark          VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '\${tableComment}';
```

### 5.2 索引建议

```sql
-- 主键（自动）
-- 租户（多租户拦截器需要）
KEY idx_tenant_id (tenant_id)

-- 业务查询字段
KEY idx_user_id (user_id)
KEY idx_create_time (create_time)

-- 唯一约束
UNIQUE KEY uk_session_id (session_id, tenant_id)
```

---

## 六、变更日志

| 日期 | 变更 | 说明 |
|---|---|---|
| 2026-09-04 | 初始版 | Claude Code 在 drift audit 完成后自动生成 |

---

**owner**：Claude Code + 后端工程师
**更新触发**：新增 IPD 业务表时

---

**最后更新**：2026-09-04