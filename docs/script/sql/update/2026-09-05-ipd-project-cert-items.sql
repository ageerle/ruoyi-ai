-- P1-7.1：项目级认证清单快照（目标市场带出 + 手工补充；版本保留；已完成不静默重置）
-- 幂等：表已存在则跳过
-- Round 8 / R8-P0-9：changeStatus 乐观锁必需 version 列（MyBatis-Plus @Version）
-- Round 8 / R8-P0-8：delFlag 已用 @TableLogic 注解，应用层自动过滤 del_flag='1'

CREATE TABLE IF NOT EXISTS project_cert_items
(
    id               bigint        not null comment '主键（雪花）',
    project_id       bigint        not null comment '归属项目',
    template_id      bigint        null comment '来源 cert_templates.id；手工可空',
    country_code     varchar(8)    not null comment '国家/地区代码',
    country_name     varchar(64)   not null comment '国家名',
    cert_name        varchar(128)  not null comment '认证名',
    cert_authority   varchar(128)  null,
    requirement_desc varchar(500)  null,
    is_mandatory     char(1)       not null default '1',
    source           varchar(16)   not null default 'AUTO' comment 'AUTO|MANUAL',
    status           varchar(24)   not null default 'PENDING' comment 'PENDING|IN_PROGRESS|DONE|NA',
    catalog_version  varchar(64)   not null comment '带出时模板快照版本',
    create_dept      bigint        null,
    create_by        bigint        null,
    create_time      datetime      null default CURRENT_TIMESTAMP,
    update_by        bigint        null,
    update_time      datetime      null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id        varchar(20)   null default '000000',
    del_flag         char(1)       null default '0',
    version          int           not null default 0 comment 'Round 8 / R8-P0-9：乐观锁（MyBatis-Plus @Version）',
    remark           varchar(500)  null,
    primary key (id),
    unique key uk_pci_project_cert (project_id, country_code, cert_name),
    key idx_pci_project (project_id),
    key idx_pci_project_mand (project_id, is_mandatory, country_code, id) comment 'Round 8 / R8-PERF-08：orderBy 覆盖索引'
) engine = innodb
  default charset = utf8mb4
  collate = utf8mb4_general_ci
  comment = 'IPD 项目认证清单（AC-PROD-10/11/12）';

-- ALTER 增量（已存在表的旧实例需补 version 列）
-- ALTER TABLE project_cert_items ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁（MyBatis-Plus @Version）' AFTER del_flag;

-- ROLLBACK（环境异常回滚用）
-- ALTER TABLE project_cert_items DROP COLUMN version;
-- ALTER TABLE project_cert_items DROP INDEX idx_pci_project_mand;

-- GRANT SELECT, INSERT, UPDATE, DELETE ON ipd_dev.project_cert_items TO 'ipd_app'@'127.0.0.1';
