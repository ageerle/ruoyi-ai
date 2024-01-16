CREATE TABLE sys_tenant
(
    id                    bigint                          NOT NULL,
    tenant_id             nvarchar(20)                    NOT NULL,
    contact_user_name     nvarchar(20)                    NULL,
    contact_phone         nvarchar(20)                    NULL,
    company_name          nvarchar(50)                    NULL,
    license_number        nvarchar(30)                    NULL,
    address               nvarchar(200)                   NULL,
    intro                 nvarchar(200)                   NULL,
    domain                nvarchar(200)                   NULL,
    remark                nvarchar(200)                   NULL,
    package_id            bigint                          NULL,
    expire_time           datetime2(7)                    NULL,
    account_count         int             DEFAULT ((-1))  NULL,
    status                nchar(1)        DEFAULT ('0')   NULL,
    del_flag              nchar(1)        DEFAULT ('0')   NULL,
    create_dept           bigint                          NULL,
    create_by             bigint                          NULL,
    create_time           datetime2(7)                    NULL,
    update_by             bigint                          NULL,
    update_time           datetime2(7)                    NULL,
    CONSTRAINT PK__sys_tenant__B21E8F2427725F8A PRIMARY KEY CLUSTERED (id)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sys.sp_addextendedproperty
    'MS_Description', N'id' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant',
    'COLUMN', N'id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'租户编号' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant',
    'COLUMN', N'tenant_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'联系人' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant',
    'COLUMN', N'contact_user_name'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'联系电话' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant',
    'COLUMN', N'contact_phone'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'企业名称' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant',
    'COLUMN', N'company_name'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'统一社会信用代码' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant',
    'COLUMN', N'license_number'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'地址' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant',
    'COLUMN', N'address'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'企业简介' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant',
    'COLUMN', N'intro'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'域名' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant',
    'COLUMN', N'domain'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'备注' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant',
    'COLUMN', N'remark'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'租户套餐编号' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant',
    'COLUMN', N'package_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'过期时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant',
    'COLUMN', N'expire_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'用户数量（-1不限制）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant',
    'COLUMN', N'account_count'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'租户状态（0正常 1停用）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant',
    'COLUMN', N'status'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'删除标志（0代表存在 2代表删除）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant',
    'COLUMN', N'del_flag'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建部门' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant',
    'COLUMN', N'create_dept'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant',
    'COLUMN', N'create_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant',
    'COLUMN', N'create_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant',
    'COLUMN', N'update_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant',
    'COLUMN', N'update_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'租户表' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant'
GO

INSERT sys_tenant VALUES (1, N'000000', N'管理组', N'15888888888', N'XXX有限公司', NULL, NULL, N'多租户通用后台管理管理系统', NULL, NULL, NULL, NULL, -1, N'0', N'0', 103, 1, getdate(), NULL, NULL)
GO


CREATE TABLE sys_tenant_package
(
    package_id            bigint                          NOT NULL,
    package_name          nvarchar(20)                    NOT NULL,
    menu_ids              nvarchar(20)                    NULL,
    remark                nvarchar(200)                   NULL,
    menu_check_strictly   tinyint         DEFAULT ((1))   NULL,
    status                nchar(1)        DEFAULT ('0')   NULL,
    del_flag              nchar(1)        DEFAULT ('0')   NULL,
    create_dept           bigint                          NULL,
    create_by             bigint                          NULL,
    create_time           datetime2(7)                    NULL,
    update_by             bigint                          NULL,
    update_time           datetime2(7)                    NULL,
    CONSTRAINT PK__sys_tenant_package__B21E8F2427725F8A PRIMARY KEY CLUSTERED (package_id)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sys.sp_addextendedproperty
    'MS_Description', N'租户套餐id' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant_package',
    'COLUMN', N'package_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'套餐名称' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant_package',
    'COLUMN', N'package_name'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'关联菜单id' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant_package',
    'COLUMN', N'menu_ids'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'备注' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant_package',
    'COLUMN', N'remark'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'租户状态（0正常 1停用）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant_package',
    'COLUMN', N'status'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'删除标志（0代表存在 2代表删除）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant_package',
    'COLUMN', N'del_flag'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建部门' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant_package',
    'COLUMN', N'create_dept'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant_package',
    'COLUMN', N'create_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant_package',
    'COLUMN', N'create_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant_package',
    'COLUMN', N'update_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant_package',
    'COLUMN', N'update_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'租户套餐表' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_tenant_package'
GO


CREATE TABLE gen_table
(
    table_id          bigint                         NOT NULL,
    table_name        nvarchar(200) DEFAULT ''       NULL,
    table_comment     nvarchar(500) DEFAULT ''       NULL,
    sub_table_name    nvarchar(64)                   NULL,
    sub_table_fk_name nvarchar(64)                   NULL,
    class_name        nvarchar(100) DEFAULT ''       NULL,
    tpl_category      nvarchar(200) DEFAULT ('crud') NULL,
    package_name      nvarchar(100)                  NULL,
    module_name       nvarchar(30)                   NULL,
    business_name     nvarchar(30)                   NULL,
    function_name     nvarchar(50)                   NULL,
    function_author   nvarchar(50)                   NULL,
    gen_type          nchar(1)      DEFAULT ('0')    NULL,
    gen_path          nvarchar(200) DEFAULT ('/')    NULL,
    options           nvarchar(1000)                 NULL,
    create_dept       bigint                         NULL,
    create_by         bigint                         NULL,
    create_time       datetime2(7)                   NULL,
    update_by         bigint                         NULL,
    update_time       datetime2(7)                   NULL,
    remark            nvarchar(500)                  NULL,
    CONSTRAINT PK__gen_tabl__B21E8F2427725F8A PRIMARY KEY CLUSTERED (table_id)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sys.sp_addextendedproperty
    'MS_Description', N'编号' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table',
    'COLUMN', N'table_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'表名称' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table',
    'COLUMN', N'table_name'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'表描述' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table',
    'COLUMN', N'table_comment'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'关联子表的表名' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table',
    'COLUMN', N'sub_table_name'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'子表关联的外键名' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table',
    'COLUMN', N'sub_table_fk_name'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'实体类名称' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table',
    'COLUMN', N'class_name'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'使用的模板（crud单表操作 tree树表操作）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table',
    'COLUMN', N'tpl_category'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'生成包路径' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table',
    'COLUMN', N'package_name'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'生成模块名' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table',
    'COLUMN', N'module_name'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'生成业务名' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table',
    'COLUMN', N'business_name'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'生成功能名' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table',
    'COLUMN', N'function_name'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'生成功能作者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table',
    'COLUMN', N'function_author'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'生成代码方式（0zip压缩包 1自定义路径）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table',
    'COLUMN', N'gen_type'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'生成路径（不填默认项目路径）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table',
    'COLUMN', N'gen_path'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'其它生成选项' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table',
    'COLUMN', N'options'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建部门' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table',
    'COLUMN', N'create_dept'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table',
    'COLUMN', N'create_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table',
    'COLUMN', N'create_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table',
    'COLUMN', N'update_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table',
    'COLUMN', N'update_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'备注' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table',
    'COLUMN', N'remark'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'代码生成业务表' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table'
GO

CREATE TABLE gen_table_column
(
    column_id      bigint                       NOT NULL,
    table_id       bigint                       NULL,
    column_name    nvarchar(200)                NULL,
    column_comment nvarchar(500)                NULL,
    column_type    nvarchar(100)                NULL,
    java_type      nvarchar(500)                NULL,
    java_field     nvarchar(200)                NULL,
    is_pk          nchar(1)                     NULL,
    is_increment   nchar(1)                     NULL,
    is_required    nchar(1)                     NULL,
    is_insert      nchar(1)                     NULL,
    is_edit        nchar(1)                     NULL,
    is_list        nchar(1)                     NULL,
    is_query       nchar(1)                     NULL,
    query_type     nvarchar(200) DEFAULT ('EQ') NULL,
    html_type      nvarchar(200)                NULL,
    dict_type      nvarchar(200) DEFAULT ''     NULL,
    sort           int                          NULL,
    create_dept    bigint                       NULL,
    create_by      bigint                       NULL,
    create_time    datetime2(7)                 NULL,
    update_by      bigint                       NULL,
    update_time    datetime2(7)                 NULL,
    CONSTRAINT PK__gen_tabl__E301851F2E68B4E8 PRIMARY KEY CLUSTERED (column_id)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sys.sp_addextendedproperty
    'MS_Description', N'编号' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table_column',
    'COLUMN', N'column_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'归属表编号' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table_column',
    'COLUMN', N'table_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'列名称' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table_column',
    'COLUMN', N'column_name'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'列描述' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table_column',
    'COLUMN', N'column_comment'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'列类型' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table_column',
    'COLUMN', N'column_type'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'JAVA类型' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table_column',
    'COLUMN', N'java_type'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'JAVA字段名' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table_column',
    'COLUMN', N'java_field'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'是否主键（1是）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table_column',
    'COLUMN', N'is_pk'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'是否自增（1是）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table_column',
    'COLUMN', N'is_increment'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'是否必填（1是）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table_column',
    'COLUMN', N'is_required'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'是否为插入字段（1是）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table_column',
    'COLUMN', N'is_insert'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'是否编辑字段（1是）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table_column',
    'COLUMN', N'is_edit'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'是否列表字段（1是）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table_column',
    'COLUMN', N'is_list'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'是否查询字段（1是）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table_column',
    'COLUMN', N'is_query'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'查询方式（等于、不等于、大于、小于、范围）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table_column',
    'COLUMN', N'query_type'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table_column',
    'COLUMN', N'html_type'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'字典类型' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table_column',
    'COLUMN', N'dict_type'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'排序' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table_column',
    'COLUMN', N'sort'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建部门' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table_column',
    'COLUMN', N'create_dept'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table_column',
    'COLUMN', N'create_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table_column',
    'COLUMN', N'create_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table_column',
    'COLUMN', N'update_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table_column',
    'COLUMN', N'update_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'代码生成业务表字段' ,
    'SCHEMA', N'dbo',
    'TABLE', N'gen_table_column'
GO

CREATE TABLE sys_config
(
    config_id    bigint                      NOT NULL,
    tenant_id    nvarchar(20)  DEFAULT '000000'  NULL,
    config_name  nvarchar(100) DEFAULT ''    NULL,
    config_key   nvarchar(100) DEFAULT ''    NULL,
    config_value nvarchar(500) DEFAULT ''    NULL,
    config_type  nchar(1)      DEFAULT ('N') NULL,
    create_dept  bigint                      NULL,
    create_by    bigint                      NULL,
    create_time  datetime2(7)                NULL,
    update_by    bigint                      NULL,
    update_time  datetime2(7)                NULL,
    remark       nvarchar(500)               NULL,
    CONSTRAINT PK__sys_conf__4AD1BFF182643682 PRIMARY KEY CLUSTERED (config_id)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sys.sp_addextendedproperty
    'MS_Description', N'参数主键' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_config',
    'COLUMN', N'config_id'
GO
EXEC sys.sp_addextendedproperty
     'MS_Description', N'租户编号' ,
     'SCHEMA', N'dbo',
     'TABLE', N'sys_config',
     'COLUMN', N'tenant_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'参数名称' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_config',
    'COLUMN', N'config_name'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'参数键名' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_config',
    'COLUMN', N'config_key'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'参数键值' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_config',
    'COLUMN', N'config_value'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'系统内置（Y是 N否）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_config',
    'COLUMN', N'config_type'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建部门' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_config',
    'COLUMN', N'create_dept'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_config',
    'COLUMN', N'create_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_config',
    'COLUMN', N'create_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_config',
    'COLUMN', N'update_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_config',
    'COLUMN', N'update_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'备注' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_config',
    'COLUMN', N'remark'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'参数配置表' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_config'
GO

INSERT sys_config VALUES (1, N'000000', N'主框架页-默认皮肤样式名称', N'sys.index.skinName', N'skin-blue', N'Y', 103, 1, getdate(), NULL, NULL, N'蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow')
GO
INSERT sys_config VALUES (2, N'000000', N'用户管理-账号初始密码', N'sys.user.initPassword', N'123456', N'Y', 103, 1, getdate(), NULL, NULL, N'初始化密码 123456')
GO
INSERT sys_config VALUES (3, N'000000', N'主框架页-侧边栏主题', N'sys.index.sideTheme', N'theme-dark', N'Y', 103, 1, getdate(), NULL, NULL, N'深色主题theme-dark，浅色主题theme-light')
GO
INSERT sys_config VALUES (5, N'000000', N'账号自助-是否开启用户注册功能', N'sys.account.registerUser', N'false', N'Y', 103, 1, getdate(), NULL, NULL, N'是否开启注册用户功能（true开启，false关闭）')
GO
INSERT sys_config VALUES (11, N'000000', N'OSS预览列表资源开关', N'sys.oss.previewListResource', N'true', N'Y', 103, 1, getdate(), NULL, NULL, N'true:开启, false:关闭');
GO

CREATE TABLE sys_dept
(
    dept_id     bigint                     NOT NULL,
    tenant_id   nvarchar(20) DEFAULT ('000000') NULL,
    parent_id   bigint       DEFAULT ((0)) NULL,
    ancestors   nvarchar(500)DEFAULT ''    NULL,
    dept_name   nvarchar(30) DEFAULT ''    NULL,
    order_num   int          DEFAULT ((0)) NULL,
    leader      nvarchar(20)               NULL,
    phone       nvarchar(11)               NULL,
    email       nvarchar(50)               NULL,
    status      nchar(1)     DEFAULT ('0') NULL,
    del_flag    nchar(1)     DEFAULT ('0') NULL,
    create_dept bigint                     NULL,
    create_by   bigint                     NULL,
    create_time datetime2(7)               NULL,
    update_by   bigint                     NULL,
    update_time datetime2(7)               NULL,
    CONSTRAINT PK__sys_dept__DCA659747DE13804 PRIMARY KEY CLUSTERED (dept_id)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sys.sp_addextendedproperty
    'MS_Description', N'部门id' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dept',
    'COLUMN', N'dept_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'租户编号' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dept',
    'COLUMN', N'tenant_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'父部门id' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dept',
    'COLUMN', N'parent_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'祖级列表' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dept',
    'COLUMN', N'ancestors'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'部门名称' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dept',
    'COLUMN', N'dept_name'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'显示顺序' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dept',
    'COLUMN', N'order_num'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'负责人' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dept',
    'COLUMN', N'leader'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'联系电话' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dept',
    'COLUMN', N'phone'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'邮箱' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dept',
    'COLUMN', N'email'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'部门状态（0正常 1停用）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dept',
    'COLUMN', N'status'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'删除标志（0代表存在 2代表删除）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dept',
    'COLUMN', N'del_flag'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建部门' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dept',
    'COLUMN', N'create_dept'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dept',
    'COLUMN', N'create_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dept',
    'COLUMN', N'create_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dept',
    'COLUMN', N'update_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dept',
    'COLUMN', N'update_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'部门表' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dept'
GO

INSERT sys_dept VALUES (100, N'000000', 0, N'0', N'XXX科技', 0, N'疯狂的狮子Li', N'15888888888', N'xxx@qq.com', N'0', N'0', 103, 1, getdate(), NULL, NULL)
GO
INSERT sys_dept VALUES (101, N'000000', 100, N'0,100', N'深圳总公司', 1, N'疯狂的狮子Li', N'15888888888', N'xxx@qq.com', N'0', N'0', 103, 1, getdate(), NULL, NULL)
GO
INSERT sys_dept VALUES (102, N'000000', 100, N'0,100', N'长沙分公司', 2, N'疯狂的狮子Li', N'15888888888', N'xxx@qq.com', N'0', N'0', 103, 1, getdate(), NULL, NULL)
GO
INSERT sys_dept VALUES (103, N'000000', 101, N'0,100,101', N'研发部门', 1, N'疯狂的狮子Li', N'15888888888', N'xxx@qq.com', N'0', N'0', 103, 1, getdate(), NULL, NULL)
GO
INSERT sys_dept VALUES (104, N'000000', 101, N'0,100,101', N'市场部门', 2, N'疯狂的狮子Li', N'15888888888', N'xxx@qq.com', N'0', N'0', 103, 1, getdate(), NULL, NULL)
GO
INSERT sys_dept VALUES (105, N'000000', 101, N'0,100,101', N'测试部门', 3, N'疯狂的狮子Li', N'15888888888', N'xxx@qq.com', N'0', N'0', 103, 1, getdate(), NULL, NULL)
GO
INSERT sys_dept VALUES (106, N'000000', 101, N'0,100,101', N'财务部门', 4, N'疯狂的狮子Li', N'15888888888', N'xxx@qq.com', N'0', N'0', 103, 1, getdate(), NULL, NULL)
GO
INSERT sys_dept VALUES (107, N'000000', 101, N'0,100,101', N'运维部门', 5, N'疯狂的狮子Li', N'15888888888', N'xxx@qq.com', N'0', N'0', 103, 1, getdate(), NULL, NULL)
GO
INSERT sys_dept VALUES (108, N'000000', 102, N'0,100,102', N'市场部门', 1, N'疯狂的狮子Li', N'15888888888', N'xxx@qq.com', N'0', N'0', 103, 1, getdate(), NULL, NULL)
GO
INSERT sys_dept VALUES (109, N'000000', 102, N'0,100,102', N'财务部门', 2, N'疯狂的狮子Li', N'15888888888', N'xxx@qq.com', N'0', N'0', 103, 1, getdate(), NULL, NULL)
GO

CREATE TABLE sys_dict_data
(
    dict_code   bigint                      NOT NULL,
    tenant_id   nvarchar(20)  DEFAULT ('000000') NULL,
    dict_sort   int           DEFAULT ((0)) NULL,
    dict_label  nvarchar(100) DEFAULT ''    NULL,
    dict_value  nvarchar(100) DEFAULT ''    NULL,
    dict_type   nvarchar(100) DEFAULT ''    NULL,
    css_class   nvarchar(100)               NULL,
    list_class  nvarchar(100)               NULL,
    is_default  nchar(1)      DEFAULT ('N') NULL,
    status      nchar(1)      DEFAULT ('0') NULL,
    create_dept bigint                      NULL,
    create_by   bigint                      NULL,
    create_time datetime2(7)                NULL,
    update_by   bigint                      NULL,
    update_time datetime2(7)                NULL,
    remark      nvarchar(500)               NULL,
    CONSTRAINT PK__sys_dict__19CBC34B661AF3B3 PRIMARY KEY CLUSTERED (dict_code)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sys.sp_addextendedproperty
    'MS_Description', N'字典编码' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_data',
    'COLUMN', N'dict_code'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'字典编码' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_data',
    'COLUMN', N'tenant_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'字典排序' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_data',
    'COLUMN', N'dict_sort'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'字典标签' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_data',
    'COLUMN', N'dict_label'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'字典键值' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_data',
    'COLUMN', N'dict_value'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'字典类型' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_data',
    'COLUMN', N'dict_type'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'样式属性（其他样式扩展）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_data',
    'COLUMN', N'css_class'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'表格回显样式' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_data',
    'COLUMN', N'list_class'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'是否默认（Y是 N否）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_data',
    'COLUMN', N'is_default'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'状态（0正常 1停用）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_data',
    'COLUMN', N'status'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建部门' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_data',
    'COLUMN', N'create_dept'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_data',
    'COLUMN', N'create_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_data',
    'COLUMN', N'create_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_data',
    'COLUMN', N'update_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_data',
    'COLUMN', N'update_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'备注' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_data',
    'COLUMN', N'remark'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'字典数据表' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_data'
GO

INSERT sys_dict_data VALUES (1, N'000000', 1, N'男', N'0', N'sys_user_sex', N'', N'', N'Y', N'0', 103, 1, getdate(), NULL, NULL, N'性别男')
GO
INSERT sys_dict_data VALUES (2, N'000000', 2, N'女', N'1', N'sys_user_sex', N'', N'', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'性别女')
GO
INSERT sys_dict_data VALUES (3, N'000000', 3, N'未知', N'2', N'sys_user_sex', N'', N'', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'性别未知')
GO
INSERT sys_dict_data VALUES (4, N'000000', 1, N'显示', N'0', N'sys_show_hide', N'', N'primary', N'Y', N'0', 103, 1, getdate(), NULL, NULL, N'显示菜单')
GO
INSERT sys_dict_data VALUES (5, N'000000', 2, N'隐藏', N'1', N'sys_show_hide', N'', N'danger', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'隐藏菜单')
GO
INSERT sys_dict_data VALUES (6, N'000000', 1, N'正常', N'0', N'sys_normal_disable', N'', N'primary', N'Y', N'0', 103, 1, getdate(), NULL, NULL, N'正常状态')
GO
INSERT sys_dict_data VALUES (7, N'000000', 2, N'停用', N'1', N'sys_normal_disable', N'', N'danger', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'停用状态')
GO
INSERT sys_dict_data VALUES (8, N'000000', 1, N'正常', N'0', N'sys_job_status', N'', N'primary', N'Y', N'0', 103, 1, getdate(), NULL, NULL, N'正常状态')
GO
INSERT sys_dict_data VALUES (9, N'000000', 2, N'暂停', N'1', N'sys_job_status', N'', N'danger', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'停用状态')
GO
INSERT sys_dict_data VALUES (10, N'000000', 1, N'默认', N'DEFAULT', N'sys_job_group', N'', N'', N'Y', N'0', 103, 1, getdate(), NULL, NULL, N'默认分组')
GO
INSERT sys_dict_data VALUES (11, N'000000', 2, N'系统', N'SYSTEM', N'sys_job_group', N'', N'', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'系统分组')
GO
INSERT sys_dict_data VALUES (12, N'000000', 1, N'是', N'Y', N'sys_yes_no', N'', N'primary', N'Y', N'0', 103, 1, getdate(), NULL, NULL, N'系统默认是')
GO
INSERT sys_dict_data VALUES (13, N'000000', 2, N'否', N'N', N'sys_yes_no', N'', N'danger', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'系统默认否')
GO
INSERT sys_dict_data VALUES (14, N'000000', 1, N'通知', N'1', N'sys_notice_type', N'', N'warning', N'Y', N'0', 103, 1, getdate(), NULL, NULL, N'通知')
GO
INSERT sys_dict_data VALUES (15, N'000000', 2, N'公告', N'2', N'sys_notice_type', N'', N'success', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'公告')
GO
INSERT sys_dict_data VALUES (16, N'000000', 1, N'正常', N'0', N'sys_notice_status', N'', N'primary', N'Y', N'0', 103, 1, getdate(), NULL, NULL, N'正常状态')
GO
INSERT sys_dict_data VALUES (17, N'000000', 2, N'关闭', N'1', N'sys_notice_status', N'', N'danger', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'关闭状态')
GO
INSERT sys_dict_data VALUES (29, N'000000', 99, N'其他', N'0', N'sys_oper_type', N'', N'info', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'其他操作');
GO
INSERT sys_dict_data VALUES (18, N'000000', 1, N'新增', N'1', N'sys_oper_type', N'', N'info', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'新增操作')
GO
INSERT sys_dict_data VALUES (19, N'000000', 2, N'修改', N'2', N'sys_oper_type', N'', N'info', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'修改操作')
GO
INSERT sys_dict_data VALUES (20, N'000000', 3, N'删除', N'3', N'sys_oper_type', N'', N'danger', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'删除操作')
GO
INSERT sys_dict_data VALUES (21, N'000000', 4, N'授权', N'4', N'sys_oper_type', N'', N'primary', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'授权操作')
GO
INSERT sys_dict_data VALUES (22, N'000000', 5, N'导出', N'5', N'sys_oper_type', N'', N'warning', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'导出操作')
GO
INSERT sys_dict_data VALUES (23, N'000000', 6, N'导入', N'6', N'sys_oper_type', N'', N'warning', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'导入操作')
GO
INSERT sys_dict_data VALUES (24, N'000000', 7, N'强退', N'7', N'sys_oper_type', N'', N'danger', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'强退操作')
GO
INSERT sys_dict_data VALUES (25, N'000000', 8, N'生成代码', N'8', N'sys_oper_type', N'', N'warning', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'生成操作')
GO
INSERT sys_dict_data VALUES (26, N'000000', 9, N'清空数据', N'9', N'sys_oper_type', N'', N'danger', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'清空操作')
GO
INSERT sys_dict_data VALUES (27, N'000000', 1, N'成功', N'0', N'sys_common_status', N'', N'primary', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'正常状态')
GO
INSERT sys_dict_data VALUES (28, N'000000', 2, N'失败', N'1', N'sys_common_status', N'', N'danger', N'N', N'0', 103, 1, getdate(), NULL, NULL, N'停用状态')
GO

CREATE TABLE sys_dict_type
(
    dict_id     bigint                      NOT NULL,
    tenant_id   nvarchar(20)  DEFAULT ('000000') NULL,
    dict_name   nvarchar(100) DEFAULT ''    NULL,
    dict_type   nvarchar(100) DEFAULT ''    NULL,
    status      nchar(1)      DEFAULT ('0') NULL,
    create_dept bigint                      NULL,
    create_by   bigint                      NULL,
    create_time datetime2(7)                NULL,
    update_by   bigint                      NULL,
    update_time datetime2(7)                NULL,
    remark      nvarchar(500)               NULL,
    CONSTRAINT PK__sys_dict__3BD4186C409C5391 PRIMARY KEY CLUSTERED (dict_id)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
)
ON [PRIMARY]
GO

CREATE NONCLUSTERED INDEX sys_dict_type_index1 ON sys_dict_type (tenant_id, dict_type)
GO

EXEC sys.sp_addextendedproperty
    'MS_Description', N'字典主键' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_type',
    'COLUMN', N'dict_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'字典主键' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_type',
    'COLUMN', N'tenant_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'字典名称' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_type',
    'COLUMN', N'dict_name'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'字典类型' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_type',
    'COLUMN', N'dict_type'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'状态（0正常 1停用）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_type',
    'COLUMN', N'status'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建部门' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_type',
    'COLUMN', N'create_dept'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_type',
    'COLUMN', N'create_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_type',
    'COLUMN', N'create_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_type',
    'COLUMN', N'update_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_type',
    'COLUMN', N'update_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'备注' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_type',
    'COLUMN', N'remark'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'字典类型表' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_dict_type'
GO

INSERT sys_dict_type VALUES (1, N'000000', N'用户性别', N'sys_user_sex', N'0', 103, 1, getdate(), NULL, NULL, N'用户性别列表')
GO
INSERT sys_dict_type VALUES (2, N'000000', N'菜单状态', N'sys_show_hide', N'0', 103, 1, getdate(), NULL, NULL, N'菜单状态列表')
GO
INSERT sys_dict_type VALUES (3, N'000000', N'系统开关', N'sys_normal_disable', N'0', 103, 1, getdate(), NULL, NULL, N'系统开关列表')
GO
INSERT sys_dict_type VALUES (4, N'000000', N'任务状态', N'sys_job_status', N'0', 103, 1, getdate(), NULL, NULL, N'任务状态列表')
GO
INSERT sys_dict_type VALUES (5, N'000000', N'任务分组', N'sys_job_group', N'0', 103, 1, getdate(), NULL, NULL, N'任务分组列表')
GO
INSERT sys_dict_type VALUES (6, N'000000', N'系统是否', N'sys_yes_no', N'0', 103, 1, getdate(), NULL, NULL, N'系统是否列表')
GO
INSERT sys_dict_type VALUES (7, N'000000', N'通知类型', N'sys_notice_type', N'0', 103, 1, getdate(), NULL, NULL, N'通知类型列表')
GO
INSERT sys_dict_type VALUES (8, N'000000', N'通知状态', N'sys_notice_status', N'0', 103, 1, getdate(), NULL, NULL, N'通知状态列表')
GO
INSERT sys_dict_type VALUES (9, N'000000', N'操作类型', N'sys_oper_type', N'0', 103, 1, getdate(), NULL, NULL, N'操作类型列表')
GO
INSERT sys_dict_type VALUES (10, N'000000', N'系统状态', N'sys_common_status', N'0', 103, 1, getdate(), NULL, NULL, N'登录状态列表')
GO

CREATE TABLE sys_logininfor
(
    info_id        bigint                      NOT NULL,
    tenant_id      nvarchar(20)  DEFAULT ('000000') NULL,
    user_name      nvarchar(50)  DEFAULT ''    NULL,
    ipaddr         nvarchar(128) DEFAULT ''    NULL,
    login_location nvarchar(255) DEFAULT ''    NULL,
    browser        nvarchar(50)  DEFAULT ''    NULL,
    os             nvarchar(50)  DEFAULT ''    NULL,
    status         nchar(1)      DEFAULT ('0') NULL,
    msg            nvarchar(255) DEFAULT ''    NULL,
    login_time     datetime2(7)                NULL,
    CONSTRAINT PK__sys_logi__3D8A9C1A1854AE10 PRIMARY KEY CLUSTERED (info_id)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
)
ON [PRIMARY]
GO

CREATE NONCLUSTERED INDEX idx_sys_logininfor_s ON sys_logininfor (status)
GO
CREATE NONCLUSTERED INDEX idx_sys_logininfor_lt ON sys_logininfor (login_time)
GO

EXEC sys.sp_addextendedproperty
    'MS_Description', N'访问ID' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_logininfor',
    'COLUMN', N'info_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'租户编号' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_logininfor',
    'COLUMN', N'tenant_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'用户账号' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_logininfor',
    'COLUMN', N'user_name'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'登录IP地址' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_logininfor',
    'COLUMN', N'ipaddr'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'登录地点' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_logininfor',
    'COLUMN', N'login_location'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'浏览器类型' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_logininfor',
    'COLUMN', N'browser'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'操作系统' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_logininfor',
    'COLUMN', N'os'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'登录状态（0成功 1失败）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_logininfor',
    'COLUMN', N'status'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'提示消息' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_logininfor',
    'COLUMN', N'msg'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'访问时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_logininfor',
    'COLUMN', N'login_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'系统访问记录' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_logininfor'
GO

CREATE TABLE sys_menu
(
    menu_id     bigint                      NOT NULL,
    menu_name   nvarchar(50)                NOT NULL,
    parent_id   bigint        DEFAULT ((0)) NULL,
    order_num   int           DEFAULT ((0)) NULL,
    path        nvarchar(200) DEFAULT ''    NULL,
    component   nvarchar(255)               NULL,
    query_param nvarchar(255)               NULL,
    is_frame    int           DEFAULT ((1)) NULL,
    is_cache    int           DEFAULT ((0)) NULL,
    menu_type   nchar(1)      DEFAULT ''    NULL,
    visible     nchar(1)      DEFAULT ((0)) NULL,
    status      nchar(1)      DEFAULT ((0)) NULL,
    perms       nvarchar(100)               NULL,
    icon        nvarchar(100) DEFAULT ('#') NULL,
    create_dept bigint                      NULL,
    create_by   bigint                      NULL,
    create_time datetime2(7)                NULL,
    update_by   bigint                      NULL,
    update_time datetime2(7)                NULL,
    remark      nvarchar(500) DEFAULT ''    NULL,
    CONSTRAINT PK__sys_menu__4CA0FADCF8545C58 PRIMARY KEY CLUSTERED (menu_id)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sys.sp_addextendedproperty
    'MS_Description', N'菜单ID' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_menu',
    'COLUMN', N'menu_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'菜单名称' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_menu',
    'COLUMN', N'menu_name'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'父菜单ID' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_menu',
    'COLUMN', N'parent_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'显示顺序' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_menu',
    'COLUMN', N'order_num'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'路由地址' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_menu',
    'COLUMN', N'path'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'组件路径' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_menu',
    'COLUMN', N'component'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'路由参数' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_menu',
    'COLUMN', N'query_param'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'是否为外链（0是 1否）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_menu',
    'COLUMN', N'is_frame'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'是否缓存（0缓存 1不缓存）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_menu',
    'COLUMN', N'is_cache'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'菜单类型（M目录 C菜单 F按钮）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_menu',
    'COLUMN', N'menu_type'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'显示状态（0显示 1隐藏）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_menu',
    'COLUMN', N'visible'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'菜单状态（0正常 1停用）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_menu',
    'COLUMN', N'status'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'权限标识' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_menu',
    'COLUMN', N'perms'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'菜单图标' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_menu',
    'COLUMN', N'icon'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建部门' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_menu',
    'COLUMN', N'create_dept'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_menu',
    'COLUMN', N'create_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_menu',
    'COLUMN', N'create_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_menu',
    'COLUMN', N'update_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_menu',
    'COLUMN', N'update_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'备注' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_menu',
    'COLUMN', N'remark'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'菜单权限表' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_menu'
GO

INSERT sys_menu VALUES (1, N'系统管理', 0, 1, N'system', NULL, N'', 1, 0, N'M', N'0', N'0', N'', N'system', 103, 1, getdate(), NULL, NULL, N'系统管理目录')
GO
INSERT sys_menu VALUES (6, N'租户管理', 0, 2, N'tenant', NULL, N'', 1, 0, N'M', N'0', N'0', N'', N'chart', 103, 1, getdate(), NULL, NULL, N'租户管理目录')
GO
INSERT sys_menu VALUES (2, N'系统监控', 0, 3, N'monitor', NULL, N'', 1, 0, N'M', N'0', N'0', N'', N'monitor', 103, 1, getdate(), NULL, NULL, N'系统监控目录')
GO
INSERT sys_menu VALUES (3, N'系统工具', 0, 4, N'tool', NULL, N'', 1, 0, N'M', N'0', N'0', N'', N'tool', 103, 1, getdate(), NULL, NULL, N'系统工具目录')
GO
INSERT sys_menu VALUES (4, N'PLUS官网', 0, 5, N'https://gitee.com/dromara/RuoYi-Vue-Plus', null, N'', 0, 0, N'M', N'0', N'0', N'', N'guide', 103, 1, getdate(), null, null, N'RuoYi-Vue-Plus官网地址');
GO
INSERT sys_menu VALUES (100, N'用户管理', 1, 1, N'user', N'system/user/index', N'', 1, 0, N'C', N'0', N'0', N'system:user:list', N'user', 103, 1, getdate(), NULL, NULL, N'用户管理菜单')
GO
INSERT sys_menu VALUES (101, N'角色管理', 1, 2, N'role', N'system/role/index', N'', 1, 0, N'C', N'0', N'0', N'system:role:list', N'peoples', 103, 1, getdate(), NULL, NULL, N'角色管理菜单')
GO
INSERT sys_menu VALUES (102, N'菜单管理', 1, 3, N'menu', N'system/menu/index', N'', 1, 0, N'C', N'0', N'0', N'system:menu:list', N'tree-table', 103, 1, getdate(), NULL, NULL, N'菜单管理菜单')
GO
INSERT sys_menu VALUES (103, N'部门管理', 1, 4, N'dept', N'system/dept/index', N'', 1, 0, N'C', N'0', N'0', N'system:dept:list', N'tree', 103, 1, getdate(), NULL, NULL, N'部门管理菜单')
GO
INSERT sys_menu VALUES (104, N'岗位管理', 1, 5, N'post', N'system/post/index', N'', 1, 0, N'C', N'0', N'0', N'system:post:list', N'post', 103, 1, getdate(), NULL, NULL, N'岗位管理菜单')
GO
INSERT sys_menu VALUES (105, N'字典管理', 1, 6, N'dict', N'system/dict/index', N'', 1, 0, N'C', N'0', N'0', N'system:dict:list', N'dict', 103, 1, getdate(), NULL, NULL, N'字典管理菜单')
GO
INSERT sys_menu VALUES (106, N'参数设置', 1, 7, N'config', N'system/config/index', N'', 1, 0, N'C', N'0', N'0', N'system:config:list', N'edit', 103, 1, getdate(), NULL, NULL, N'参数设置菜单')
GO
INSERT sys_menu VALUES (107, N'通知公告', 1, 8, N'notice', N'system/notice/index', N'', 1, 0, N'C', N'0', N'0', N'system:notice:list', N'message', 103, 1, getdate(), NULL, NULL, N'通知公告菜单')
GO
INSERT sys_menu VALUES (108, N'日志管理', 1, 9, N'log', N'', N'', 1, 0, N'M', N'0', N'0', N'', N'log', 103, 1, getdate(), NULL, NULL, N'日志管理菜单')
GO
INSERT sys_menu VALUES (109, N'在线用户', 2, 1, N'online', N'monitor/online/index', N'', 1, 0, N'C', N'0', N'0', N'monitor:online:list', N'online', 103, 1, getdate(), NULL, NULL, N'在线用户菜单')
GO
INSERT sys_menu VALUES (113, N'缓存监控', 2, 5, N'cache', N'monitor/cache/index', N'', 1, 0, N'C', N'0', N'0', N'monitor:cache:list', N'redis', 103, 1, getdate(), NULL, NULL, N'缓存监控菜单')
GO
INSERT sys_menu VALUES (114, N'表单构建', 3, 1, N'build', N'tool/build/index', N'', 1, 0, N'C', N'0', N'0', N'tool:build:list', N'build', 103, 1, getdate(), NULL, NULL, N'表单构建菜单')
GO
INSERT sys_menu VALUES (115, N'代码生成', 3, 2, N'gen', N'tool/gen/index', N'', 1, 0, N'C', N'0', N'0', N'tool:gen:list', N'code', 103, 1, getdate(), NULL, NULL, N'代码生成菜单')
GO
INSERT sys_menu VALUES (121, N'租户管理', 6, 1, N'tenant', N'system/tenant/index', N'', 1, 0, N'C', N'0', N'0', N'system:tenant:list', N'code', 103, 1, getdate(), NULL, NULL, N'租户管理菜单')
GO
INSERT sys_menu VALUES (122, N'租户套餐管理', 6, 2, N'tenantPackage', N'system/tenantPackage/index', N'', 1, 0, N'C', N'0', N'0', N'system:tenantPackage:list', N'code', 103, 1, getdate(), NULL, NULL, N'租户套餐管理菜单')
GO
INSERT sys_menu VALUES (117, N'Admin监控', 2, 5, N'Admin', N'monitor/admin/index', N'', 1, 0, N'C', N'0', N'0', N'monitor:admin:list', N'dashboard', 103, 1, getdate(), NULL, NULL, N'Admin监控菜单');
GO
INSERT sys_menu VALUES (118, N'文件管理', 1, 10, N'oss', N'system/oss/index', N'', 1, 0, N'C', '0', N'0', N'system:oss:list', N'upload', 103, 1, getdate(), NULL, NULL, N'文件管理菜单');
GO
INSERT sys_menu VALUES (120, N'任务调度中心', 2, 5, N'XxlJob', N'monitor/xxljob/index', N'', 1, 0, N'C', N'0', N'0', N'monitor:xxljob:list', N'job', 103, 1, getdate(), NULL, NULL, N'Xxl-Job控制台菜单');
GO
INSERT sys_menu VALUES (500, N'操作日志', 108, 1, N'operlog', N'monitor/operlog/index', N'', 1, 0, N'C', N'0', N'0', N'monitor:operlog:list', N'form', 103, 1, getdate(), NULL, NULL, N'操作日志菜单')
GO
INSERT sys_menu VALUES (501, N'登录日志', 108, 2, N'logininfor', N'monitor/logininfor/index', N'', 1, 0, N'C', N'0', N'0', N'monitor:logininfor:list', N'logininfor', 103, 1, getdate(), NULL, NULL, N'登录日志菜单')
GO
INSERT sys_menu VALUES (1001, N'用户查询', 100, 1, N'', N'', N'', 1, 0, N'F', N'0', N'0', N'system:user:query', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1002, N'用户新增', 100, 2, N'', N'', N'', 1, 0, N'F', N'0', N'0', N'system:user:add', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1003, N'用户修改', 100, 3, N'', N'', N'', 1, 0, N'F', N'0', N'0', N'system:user:edit', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1004, N'用户删除', 100, 4, N'', N'', N'', 1, 0, N'F', N'0', N'0', N'system:user:remove', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1005, N'用户导出', 100, 5, N'', N'', N'', 1, 0, N'F', N'0', N'0', N'system:user:export', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1006, N'用户导入', 100, 6, N'', N'', N'', 1, 0, N'F', N'0', N'0', N'system:user:import', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1007, N'重置密码', 100, 7, N'', N'', N'', 1, 0, N'F', N'0', N'0', N'system:user:resetPwd', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1008, N'角色查询', 101, 1, N'', N'', N'', 1, 0, N'F', N'0', N'0', N'system:role:query', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1009, N'角色新增', 101, 2, N'', N'', N'', 1, 0, N'F', N'0', N'0', N'system:role:add', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1010, N'角色修改', 101, 3, N'', N'', N'', 1, 0, N'F', N'0', N'0', N'system:role:edit', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1011, N'角色删除', 101, 4, N'', N'', N'', 1, 0, N'F', N'0', N'0', N'system:role:remove', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1012, N'角色导出', 101, 5, N'', N'', N'', 1, 0, N'F', N'0', N'0', N'system:role:export', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1013, N'菜单查询', 102, 1, N'', N'', N'', 1, 0, N'F', N'0', N'0', N'system:menu:query', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1014, N'菜单新增', 102, 2, N'', N'', N'', 1, 0, N'F', N'0', N'0', N'system:menu:add', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1015, N'菜单修改', 102, 3, N'', N'', N'', 1, 0, N'F', N'0', N'0', N'system:menu:edit', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1016, N'菜单删除', 102, 4, N'', N'', N'', 1, 0, N'F', N'0', N'0', N'system:menu:remove', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1017, N'部门查询', 103, 1, N'', N'', N'', 1, 0, N'F', N'0', N'0', N'system:dept:query', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1018, N'部门新增', 103, 2, N'', N'', N'', 1, 0, N'F', N'0', N'0', N'system:dept:add', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1019, N'部门修改', 103, 3, N'', N'', N'', 1, 0, N'F', N'0', N'0', N'system:dept:edit', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1020, N'部门删除', 103, 4, N'', N'', N'', 1, 0, N'F', N'0', N'0', N'system:dept:remove', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1021, N'岗位查询', 104, 1, N'', N'', N'', 1, 0, N'F', N'0', N'0', N'system:post:query', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1022, N'岗位新增', 104, 2, N'', N'', N'', 1, 0, N'F', N'0', N'0', N'system:post:add', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1023, N'岗位修改', 104, 3, N'', N'', N'', 1, 0, N'F', N'0', N'0', N'system:post:edit', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1024, N'岗位删除', 104, 4, N'', N'', N'', 1, 0, N'F', N'0', N'0', N'system:post:remove', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1025, N'岗位导出', 104, 5, N'', N'', N'', 1, 0, N'F', N'0', N'0', N'system:post:export', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1026, N'字典查询', 105, 1, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:dict:query', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1027, N'字典新增', 105, 2, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:dict:add', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1028, N'字典修改', 105, 3, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:dict:edit', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1029, N'字典删除', 105, 4, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:dict:remove', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1030, N'字典导出', 105, 5, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:dict:export', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1031, N'参数查询', 106, 1, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:config:query', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1032, N'参数新增', 106, 2, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:config:add', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1033, N'参数修改', 106, 3, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:config:edit', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1034, N'参数删除', 106, 4, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:config:remove', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1035, N'参数导出', 106, 5, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:config:export', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1036, N'公告查询', 107, 1, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:notice:query', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1037, N'公告新增', 107, 2, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:notice:add', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1038, N'公告修改', 107, 3, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:notice:edit', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1039, N'公告删除', 107, 4, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:notice:remove', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1040, N'操作查询', 500, 1, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'monitor:operlog:query', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1041, N'操作删除', 500, 2, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'monitor:operlog:remove', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1042, N'日志导出', 500, 4, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'monitor:operlog:export', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1043, N'登录查询', 501, 1, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'monitor:logininfor:query', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1044, N'登录删除', 501, 2, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'monitor:logininfor:remove', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1045, N'日志导出', 501, 3, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'monitor:logininfor:export', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1050, N'账户解锁', 501, 4, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'monitor:logininfor:unlock',  N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1046, N'在线查询', 109, 1, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'monitor:online:query', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1047, N'批量强退', 109, 2, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'monitor:online:batchLogout', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1048, N'单条强退', 109, 3, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'monitor:online:forceLogout', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1055, N'生成查询', 115, 1, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'tool:gen:query', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1056, N'生成修改', 115, 2, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'tool:gen:edit', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1057, N'生成删除', 115, 3, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'tool:gen:remove', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1058, N'导入代码', 115, 2, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'tool:gen:import', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1059, N'预览代码', 115, 4, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'tool:gen:preview', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_menu VALUES (1060, N'生成代码', 115, 5, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'tool:gen:code', N'#', 103, 1, getdate(), NULL, NULL, N'')
GO
-- oss相关按钮
INSERT sys_menu VALUES (1600, N'文件查询', 118, 1, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:oss:query', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (1601, N'文件上传', 118, 2, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:oss:upload', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (1602, N'文件下载', 118, 3, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:oss:download', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (1603, N'文件删除', 118, 4, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:oss:remove', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (1604, N'配置添加', 118, 5, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:oss:add', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (1605, N'配置编辑', 118, 6, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:oss:edit', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO
-- 租户管理相关按钮
INSERT sys_menu VALUES (1606, N'租户查询', 121, 1, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:tenant:query', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (1607, N'租户新增', 121, 2, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:tenant:add', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (1608, N'租户修改', 121, 3, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:tenant:edit', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (1609, N'租户删除', 121, 4, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:tenant:remove', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (1610, N'租户导出', 121, 5, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:tenant:export', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO
-- 租户套餐管理相关按钮
INSERT sys_menu VALUES (1611, N'租户套餐查询', 122, 1, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:tenantPackage:query', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (1612, N'租户套餐新增', 122, 2, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:tenantPackage:add', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (1613, N'租户套餐修改', 122, 3, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:tenantPackage:edit', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (1614, N'租户套餐删除', 122, 4, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:tenantPackage:remove', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (1615, N'租户套餐导出', 122, 5, N'#', N'', N'', 1, 0, N'F', N'0', N'0', N'system:tenantPackage:export', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO

CREATE TABLE sys_notice
(
    notice_id      bigint                     NOT NULL,
    tenant_id      nvarchar(20) DEFAULT ('000000') NULL,
    notice_title   nvarchar(50)               NOT NULL,
    notice_type    nchar(1)                   NOT NULL,
    notice_content nvarchar(max)              NULL,
    status         nchar(1)     DEFAULT ('0') NULL,
    create_dept    bigint                     NULL,
    create_by      bigint                     NULL,
    create_time    datetime2(7)               NULL,
    update_by      bigint                     NULL,
    update_time    datetime2(7)               NULL,
    remark         nvarchar(255)              NULL,
    CONSTRAINT PK__sys_noti__3E82A5DB0EC94801 PRIMARY KEY CLUSTERED (notice_id)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
)
ON [PRIMARY]
TEXTIMAGE_ON [PRIMARY]
GO

EXEC sys.sp_addextendedproperty
    'MS_Description', N'公告ID' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_notice',
    'COLUMN', N'notice_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'租户编号' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_notice',
    'COLUMN', N'tenant_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'公告标题' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_notice',
    'COLUMN', N'notice_title'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'公告类型（1通知 2公告）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_notice',
    'COLUMN', N'notice_type'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'公告内容' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_notice',
    'COLUMN', N'notice_content'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'公告状态（0正常 1关闭）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_notice',
    'COLUMN', N'status'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建部门' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_notice',
    'COLUMN', N'create_dept'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_notice',
    'COLUMN', N'create_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_notice',
    'COLUMN', N'create_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_notice',
    'COLUMN', N'update_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_notice',
    'COLUMN', N'update_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'备注' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_notice',
    'COLUMN', N'remark'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'通知公告表' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_notice'
GO

INSERT sys_notice VALUES (1, N'000000', N'温馨提醒：2018-07-01 若依新版本发布啦', N'2', N'新版本内容', N'0', 103, 1, getdate(), NULL, NULL, N'管理员')
GO
INSERT sys_notice VALUES (2, N'000000', N'维护通知：2018-07-01 若依系统凌晨维护', N'1', N'维护内容', N'0', 103, 1, getdate(), NULL, NULL, N'管理员')
GO

CREATE TABLE sys_oper_log
(
    oper_id        bigint                       NOT NULL,
    tenant_id      nvarchar(20)   DEFAULT ('000000') NULL,
    title          nvarchar(50)   DEFAULT ''    NULL,
    business_type  int            DEFAULT ((0)) NULL,
    method         nvarchar(100)  DEFAULT ''    NULL,
    request_method nvarchar(10)   DEFAULT ''    NULL,
    operator_type  int            DEFAULT ((0)) NULL,
    oper_name      nvarchar(50)   DEFAULT ''    NULL,
    dept_name      nvarchar(50)   DEFAULT ''    NULL,
    oper_url       nvarchar(255)  DEFAULT ''    NULL,
    oper_ip        nvarchar(128)  DEFAULT ''    NULL,
    oper_location  nvarchar(255)  DEFAULT ''    NULL,
    oper_param     nvarchar(2000) DEFAULT ''    NULL,
    json_result    nvarchar(2000) DEFAULT ''    NULL,
    status         int            DEFAULT ((0)) NULL,
    error_msg      nvarchar(2000) DEFAULT ''    NULL,
    oper_time      datetime2(7)                 NULL,
    cost_time      bigint         DEFAULT ((0)) NULL,
    CONSTRAINT PK__sys_oper__34723BF9BD954573 PRIMARY KEY CLUSTERED (oper_id)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
)
ON [PRIMARY]
GO

CREATE NONCLUSTERED INDEX idx_sys_oper_log_bt ON sys_oper_log (business_type)
GO
CREATE NONCLUSTERED INDEX idx_sys_oper_log_s ON sys_oper_log (status)
GO
CREATE NONCLUSTERED INDEX idx_sys_oper_log_ot ON sys_oper_log (oper_time)
GO

EXEC sys.sp_addextendedproperty
    'MS_Description', N'日志主键' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oper_log',
    'COLUMN', N'oper_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'租户编号' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oper_log',
    'COLUMN', N'tenant_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'模块标题' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oper_log',
    'COLUMN', N'title'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'业务类型（0其它 1新增 2修改 3删除）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oper_log',
    'COLUMN', N'business_type'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'方法名称' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oper_log',
    'COLUMN', N'method'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'请求方式' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oper_log',
    'COLUMN', N'request_method'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'操作类别（0其它 1后台用户 2手机端用户）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oper_log',
    'COLUMN', N'operator_type'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'操作人员' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oper_log',
    'COLUMN', N'oper_name'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'部门名称' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oper_log',
    'COLUMN', N'dept_name'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'请求URL' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oper_log',
    'COLUMN', N'oper_url'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'主机地址' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oper_log',
    'COLUMN', N'oper_ip'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'操作地点' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oper_log',
    'COLUMN', N'oper_location'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'请求参数' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oper_log',
    'COLUMN', N'oper_param'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'返回参数' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oper_log',
    'COLUMN', N'json_result'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'操作状态（0正常 1异常）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oper_log',
    'COLUMN', N'status'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'错误消息' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oper_log',
    'COLUMN', N'error_msg'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'操作时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oper_log',
    'COLUMN', N'oper_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'消耗时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oper_log',
    'COLUMN', N'cost_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'操作日志记录' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oper_log'
GO

CREATE TABLE sys_post
(
    post_id     bigint                          NOT NULL,
    tenant_id   nvarchar(20) DEFAULT ('000000') NULL,
    post_code   nvarchar(64)                    NOT NULL,
    post_name   nvarchar(50)                    NOT NULL,
    post_sort   int                             NOT NULL,
    status      nchar(1)                        NOT NULL,
    create_dept bigint                          NULL,
    create_by   bigint                          NULL,
    create_time datetime2(7)                    NULL,
    update_by   bigint                          NULL,
    update_time datetime2(7)                    NULL,
    remark      nvarchar(500)                   NULL,
    CONSTRAINT PK__sys_post__3ED7876668E2D081 PRIMARY KEY CLUSTERED (post_id)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sys.sp_addextendedproperty
    'MS_Description', N'岗位ID' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_post',
    'COLUMN', N'post_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'租户编号' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_post',
    'COLUMN', N'tenant_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'岗位编码' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_post',
    'COLUMN', N'post_code'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'岗位名称' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_post',
    'COLUMN', N'post_name'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'显示顺序' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_post',
    'COLUMN', N'post_sort'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'状态（0正常 1停用）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_post',
    'COLUMN', N'status'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建部门' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_post',
    'COLUMN', N'create_dept'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_post',
    'COLUMN', N'create_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_post',
    'COLUMN', N'create_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_post',
    'COLUMN', N'update_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_post',
    'COLUMN', N'update_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'备注' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_post',
    'COLUMN', N'remark'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'岗位信息表' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_post'
GO

INSERT sys_post VALUES (1, N'000000', N'ceo', N'董事长', 1, N'0', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_post VALUES (2, N'000000', N'se', N'项目经理', 2, N'0', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_post VALUES (3, N'000000', N'hr', N'人力资源', 3, N'0', 103, 1, getdate(), NULL, NULL, N'')
GO
INSERT sys_post VALUES (4, N'000000', N'user', N'普通员工', 4, N'0', 103, 1, getdate(), NULL, NULL, N'')
GO

CREATE TABLE sys_role
(
    role_id             bigint                     NOT NULL,
    tenant_id           nvarchar(20) DEFAULT ('000000') NULL,
    role_name           nvarchar(30)               NOT NULL,
    role_key            nvarchar(100)              NOT NULL,
    role_sort           int                        NOT NULL,
    data_scope          nchar(1)     DEFAULT ('1') NULL,
    menu_check_strictly tinyint      DEFAULT ((1)) NULL,
    dept_check_strictly tinyint      DEFAULT ((1)) NULL,
    status              nchar(1)                   NOT NULL,
    del_flag            nchar(1)     DEFAULT ('0') NULL,
    create_dept         bigint                     NULL,
    create_by           bigint                     NULL,
    create_time         datetime2(7)               NULL,
    update_by           bigint                     NULL,
    update_time         datetime2(7)               NULL,
    remark              nvarchar(500)              NULL,
    CONSTRAINT PK__sys_role__760965CCF9383145 PRIMARY KEY CLUSTERED (role_id)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sys.sp_addextendedproperty
    'MS_Description', N'角色ID' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_role',
    'COLUMN', N'role_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'租户编号' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_role',
    'COLUMN', N'tenant_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'角色名称' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_role',
    'COLUMN', N'role_name'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'角色权限字符串' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_role',
    'COLUMN', N'role_key'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'显示顺序' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_role',
    'COLUMN', N'role_sort'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_role',
    'COLUMN', N'data_scope'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'菜单树选择项是否关联显示' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_role',
    'COLUMN', N'menu_check_strictly'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'部门树选择项是否关联显示' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_role',
    'COLUMN', N'dept_check_strictly'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'角色状态（0正常 1停用）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_role',
    'COLUMN', N'status'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'删除标志（0代表存在 2代表删除）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_role',
    'COLUMN', N'del_flag'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建部门' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_role',
    'COLUMN', N'create_dept'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_role',
    'COLUMN', N'create_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_role',
    'COLUMN', N'create_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_role',
    'COLUMN', N'update_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_role',
    'COLUMN', N'update_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'备注' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_role',
    'COLUMN', N'remark'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'角色信息表' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_role'
GO

INSERT sys_role VALUES (1, N'000000', N'超级管理员', N'superadmin', 1, N'1', 1, 1, N'0', N'0', 103, 1, getdate(), NULL, NULL, N'超级管理员')
GO
INSERT sys_role VALUES (2, N'000000', N'普通角色', N'common', 2, N'2', 1, 1, N'0', N'0', 103, 1, getdate(), NULL, NULL, N'普通角色')
GO

CREATE TABLE sys_role_dept
(
    role_id bigint NOT NULL,
    dept_id bigint NOT NULL,
    CONSTRAINT PK__sys_role__2BC3005BABBCA08A PRIMARY KEY CLUSTERED (role_id, dept_id)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sys.sp_addextendedproperty
    'MS_Description', N'角色ID' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_role_dept',
    'COLUMN', N'role_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'部门ID' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_role_dept',
    'COLUMN', N'dept_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'角色和部门关联表' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_role_dept'
GO

INSERT sys_role_dept VALUES (2, 100)
GO
INSERT sys_role_dept VALUES (2, 101)
GO
INSERT sys_role_dept VALUES (2, 105)
GO

CREATE TABLE sys_role_menu
(
    role_id bigint NOT NULL,
    menu_id bigint NOT NULL,
    CONSTRAINT PK__sys_role__A2C36A6187BA4B17 PRIMARY KEY CLUSTERED (role_id, menu_id)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sys.sp_addextendedproperty
    'MS_Description', N'角色ID' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_role_menu',
    'COLUMN', N'role_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'菜单ID' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_role_menu',
    'COLUMN', N'menu_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'角色和菜单关联表' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_role_menu'
GO

INSERT sys_role_menu VALUES (2, 1)
GO
INSERT sys_role_menu VALUES (2, 2)
GO
INSERT sys_role_menu VALUES (2, 3)
GO
INSERT sys_role_menu VALUES (2, 100)
GO
INSERT sys_role_menu VALUES (2, 101)
GO
INSERT sys_role_menu VALUES (2, 102)
GO
INSERT sys_role_menu VALUES (2, 103)
GO
INSERT sys_role_menu VALUES (2, 104)
GO
INSERT sys_role_menu VALUES (2, 105)
GO
INSERT sys_role_menu VALUES (2, 106)
GO
INSERT sys_role_menu VALUES (2, 107)
GO
INSERT sys_role_menu VALUES (2, 108)
GO
INSERT sys_role_menu VALUES (2, 109)
GO
INSERT sys_role_menu VALUES (2, 110)
GO
INSERT sys_role_menu VALUES (2, 111)
GO
INSERT sys_role_menu VALUES (2, 112)
GO
INSERT sys_role_menu VALUES (2, 113)
GO
INSERT sys_role_menu VALUES (2, 114)
GO
INSERT sys_role_menu VALUES (2, 115)
GO
INSERT sys_role_menu VALUES (2, 116)
GO
INSERT sys_role_menu VALUES (2, 500)
GO
INSERT sys_role_menu VALUES (2, 501)
GO
INSERT sys_role_menu VALUES (2, 1001)
GO
INSERT sys_role_menu VALUES (2, 1002)
GO
INSERT sys_role_menu VALUES (2, 1003)
GO
INSERT sys_role_menu VALUES (2, 1004)
GO
INSERT sys_role_menu VALUES (2, 1005)
GO
INSERT sys_role_menu VALUES (2, 1006)
GO
INSERT sys_role_menu VALUES (2, 1007)
GO
INSERT sys_role_menu VALUES (2, 1008)
GO
INSERT sys_role_menu VALUES (2, 1009)
GO
INSERT sys_role_menu VALUES (2, 1010)
GO
INSERT sys_role_menu VALUES (2, 1011)
GO
INSERT sys_role_menu VALUES (2, 1012)
GO
INSERT sys_role_menu VALUES (2, 1013)
GO
INSERT sys_role_menu VALUES (2, 1014)
GO
INSERT sys_role_menu VALUES (2, 1015)
GO
INSERT sys_role_menu VALUES (2, 1016)
GO
INSERT sys_role_menu VALUES (2, 1017)
GO
INSERT sys_role_menu VALUES (2, 1018)
GO
INSERT sys_role_menu VALUES (2, 1019)
GO
INSERT sys_role_menu VALUES (2, 1020)
GO
INSERT sys_role_menu VALUES (2, 1021)
GO
INSERT sys_role_menu VALUES (2, 1022)
GO
INSERT sys_role_menu VALUES (2, 1023)
GO
INSERT sys_role_menu VALUES (2, 1024)
GO
INSERT sys_role_menu VALUES (2, 1025)
GO
INSERT sys_role_menu VALUES (2, 1026)
GO
INSERT sys_role_menu VALUES (2, 1027)
GO
INSERT sys_role_menu VALUES (2, 1028)
GO
INSERT sys_role_menu VALUES (2, 1029)
GO
INSERT sys_role_menu VALUES (2, 1030)
GO
INSERT sys_role_menu VALUES (2, 1031)
GO
INSERT sys_role_menu VALUES (2, 1032)
GO
INSERT sys_role_menu VALUES (2, 1033)
GO
INSERT sys_role_menu VALUES (2, 1034)
GO
INSERT sys_role_menu VALUES (2, 1035)
GO
INSERT sys_role_menu VALUES (2, 1036)
GO
INSERT sys_role_menu VALUES (2, 1037)
GO
INSERT sys_role_menu VALUES (2, 1038)
GO
INSERT sys_role_menu VALUES (2, 1039)
GO
INSERT sys_role_menu VALUES (2, 1040)
GO
INSERT sys_role_menu VALUES (2, 1041)
GO
INSERT sys_role_menu VALUES (2, 1042)
GO
INSERT sys_role_menu VALUES (2, 1043)
GO
INSERT sys_role_menu VALUES (2, 1044)
GO
INSERT sys_role_menu VALUES (2, 1045)
GO
INSERT sys_role_menu VALUES (2, 1046)
GO
INSERT sys_role_menu VALUES (2, 1047)
GO
INSERT sys_role_menu VALUES (2, 1048)
GO
INSERT sys_role_menu VALUES (2, 1049)
GO
INSERT sys_role_menu VALUES (2, 1050)
GO
INSERT sys_role_menu VALUES (2, 1051)
GO
INSERT sys_role_menu VALUES (2, 1052)
GO
INSERT sys_role_menu VALUES (2, 1053)
GO
INSERT sys_role_menu VALUES (2, 1054)
GO
INSERT sys_role_menu VALUES (2, 1055)
GO
INSERT sys_role_menu VALUES (2, 1056)
GO
INSERT sys_role_menu VALUES (2, 1057)
GO
INSERT sys_role_menu VALUES (2, 1058)
GO
INSERT sys_role_menu VALUES (2, 1059)
GO
INSERT sys_role_menu VALUES (2, 1060)
GO

CREATE TABLE sys_user
(
    user_id     bigint                             NOT NULL,
    tenant_id   nvarchar(20)  DEFAULT ('000000')   NULL,
    dept_id     bigint                             NULL,
    user_name   nvarchar(30)                       NOT NULL,
    nick_name   nvarchar(30)                       NOT NULL,
    user_type   nvarchar(10)  DEFAULT ('sys_user') NULL,
    email       nvarchar(50)  DEFAULT ''           NULL,
    phonenumber nvarchar(11)  DEFAULT ''           NULL,
    sex         nchar(1)      DEFAULT ('0')        NULL,
    avatar      bigint                             NULL,
    password    nvarchar(100) DEFAULT ''           NULL,
    status      nchar(1)      DEFAULT ('0')        NULL,
    del_flag    nchar(1)      DEFAULT ('0')        NULL,
    login_ip    nvarchar(128) DEFAULT ''           NULL,
    login_date  datetime2(7)                       NULL,
    create_dept bigint                             NULL,
    create_by   bigint                             NULL,
    create_time datetime2(7)                       NULL,
    update_by   bigint                             NULL,
    update_time datetime2(7)                       NULL,
    remark      nvarchar(500)                      NULL,
    CONSTRAINT PK__sys_user__B9BE370F79170B6A PRIMARY KEY CLUSTERED (user_id)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sys.sp_addextendedproperty
    'MS_Description', N'用户ID' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user',
    'COLUMN', N'user_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'租户编号' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user',
    'COLUMN', N'tenant_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'部门ID' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user',
    'COLUMN', N'dept_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'用户账号' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user',
    'COLUMN', N'user_name'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'用户昵称' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user',
    'COLUMN', N'nick_name'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'用户类型（sys_user系统用户）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user',
    'COLUMN', N'user_type'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'用户邮箱' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user',
    'COLUMN', N'email'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'手机号码' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user',
    'COLUMN', N'phonenumber'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'用户性别（0男 1女 2未知）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user',
    'COLUMN', N'sex'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'头像地址' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user',
    'COLUMN', N'avatar'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'密码' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user',
    'COLUMN', N'password'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'帐号状态（0正常 1停用）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user',
    'COLUMN', N'status'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'删除标志（0代表存在 2代表删除）' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user',
    'COLUMN', N'del_flag'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'最后登录IP' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user',
    'COLUMN', N'login_ip'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'最后登录时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user',
    'COLUMN', N'login_date'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建部门' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user',
    'COLUMN', N'create_dept'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user',
    'COLUMN', N'create_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user',
    'COLUMN', N'create_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新者' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user',
    'COLUMN', N'update_by'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'更新时间' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user',
    'COLUMN', N'update_time'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'备注' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user',
    'COLUMN', N'remark'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'用户信息表' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user'
GO

INSERT sys_user VALUES (1, 103, N'000000', N'admin', N'疯狂的狮子Li', N'sys_user', N'crazyLionLi@163.com', N'15888888888', N'1', NULL, N'$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', N'0', N'0', N'127.0.0.1', getdate(), 103, 1, getdate(), NULL, NULL, N'管理员')
GO
INSERT sys_user VALUES (2, 105, N'000000', N'lionli', N'疯狂的狮子Li', N'sys_user', N'crazyLionLi@qq.com', N'15666666666', N'1', NULL, N'$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', N'0', N'0', N'127.0.0.1', getdate(), 103, 1, getdate(), NULL, NULL, N'测试员')
GO

CREATE TABLE sys_user_post
(
    user_id bigint NOT NULL,
    post_id bigint NOT NULL,
    CONSTRAINT PK__sys_user__CA534F799C04589B PRIMARY KEY CLUSTERED (user_id, post_id)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sys.sp_addextendedproperty
    'MS_Description', N'用户ID' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user_post',
    'COLUMN', N'user_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'岗位ID' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user_post',
    'COLUMN', N'post_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'用户与岗位关联表' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user_post'
GO

INSERT sys_user_post VALUES (1, 1)
GO
INSERT sys_user_post VALUES (2, 2)
GO

CREATE TABLE sys_user_role
(
    user_id bigint NOT NULL,
    role_id bigint NOT NULL,
    CONSTRAINT PK__sys_user__6EDEA153FB34D8F0 PRIMARY KEY CLUSTERED (user_id, role_id)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sys.sp_addextendedproperty
    'MS_Description', N'用户ID' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user_role',
    'COLUMN', N'user_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'角色ID' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user_role',
    'COLUMN', N'role_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'用户和角色关联表' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_user_role'
GO

INSERT sys_user_role VALUES (1, 1)
GO
INSERT sys_user_role VALUES (2, 2)
GO

CREATE TABLE sys_oss
(
    oss_id        bigint                          NOT NULL,
    tenant_id     nvarchar(20)  DEFAULT ('000000') NULL,
    file_name     nvarchar(255) DEFAULT ''        NOT NULL,
    original_name nvarchar(255) DEFAULT ''        NOT NULL,
    file_suffix   nvarchar(10)  DEFAULT ''        NOT NULL,
    url           nvarchar(500)                   NOT NULL,
    create_dept   bigint                          NULL,
    create_time   datetime2(7)                    NULL,
    create_by     bigint                          NULL,
    update_time   datetime2(7)                    NULL,
    update_by     bigint                          NULL,
    service       nvarchar(20)  DEFAULT ('minio') NOT NULL,
    CONSTRAINT PK__sys_oss__91241EA442389F0D PRIMARY KEY CLUSTERED (oss_id)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sp_addextendedproperty
    'MS_Description', N'对象存储主键',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss',
    'COLUMN', N'oss_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'租户编号' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss',
    'COLUMN', N'tenant_id'
GO
EXEC sp_addextendedproperty
    'MS_Description', N'文件名',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss',
    'COLUMN', N'file_name'
GO
EXEC sp_addextendedproperty
    'MS_Description', N'原名',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss',
    'COLUMN', N'original_name'
GO
EXEC sp_addextendedproperty
    'MS_Description', N'文件后缀名',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss',
    'COLUMN', N'file_suffix'
GO
EXEC sp_addextendedproperty
    'MS_Description', N'URL地址',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss',
    'COLUMN', N'url'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建部门' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss',
    'COLUMN', N'create_dept'
GO
EXEC sp_addextendedproperty
    'MS_Description', N'创建时间',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss',
    'COLUMN', N'create_time'
GO
EXEC sp_addextendedproperty
    'MS_Description', N'上传人',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss',
    'COLUMN', N'create_by'
GO
EXEC sp_addextendedproperty
    'MS_Description', N'更新时间',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss',
    'COLUMN', N'update_time'
GO
EXEC sp_addextendedproperty
    'MS_Description', N'更新人',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss',
    'COLUMN', N'update_by'
GO
EXEC sp_addextendedproperty
    'MS_Description', N'服务商',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss',
    'COLUMN', N'service'
GO
EXEC sp_addextendedproperty
    'MS_Description', N'OSS对象存储表',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss'
GO

CREATE TABLE sys_oss_config
(
    oss_config_id bigint                      NOT NULL,
    tenant_id     nvarchar(20)  DEFAULT ('000000') NULL,
    config_key    nvarchar(20)  DEFAULT ''    NOT NULL,
    access_key    nvarchar(255) DEFAULT ''    NULL,
    secret_key    nvarchar(255) DEFAULT ''    NULL,
    bucket_name   nvarchar(255) DEFAULT ''    NULL,
    prefix        nvarchar(255) DEFAULT ''    NULL,
    endpoint      nvarchar(255) DEFAULT ''    NULL,
    domain        nvarchar(255) DEFAULT ''    NULL,
    is_https      nchar(1)      DEFAULT ('N') NULL,
    region        nvarchar(255) DEFAULT ''    NULL,
    access_policy nchar(1)      DEFAULT ('1') NOT NULL,
    status        nchar(1)      DEFAULT ('1') NULL,
    ext1          nvarchar(255) DEFAULT ''    NULL,
    create_dept   bigint                      NULL,
    create_by     bigint                      NULL,
    create_time   datetime2(7)                NULL,
    update_by     bigint                      NULL,
    update_time   datetime2(7)                NULL,
    remark        nvarchar(500)               NULL,
    CONSTRAINT PK__sys_oss___BFBDE87009ED2882 PRIMARY KEY CLUSTERED (oss_config_id)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sp_addextendedproperty
    'MS_Description', N'主建',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss_config',
    'COLUMN', N'oss_config_id'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'租户编号' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss_config',
    'COLUMN', N'tenant_id'
GO
EXEC sp_addextendedproperty
    'MS_Description', N'配置key',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss_config',
    'COLUMN', N'config_key'
GO
EXEC sp_addextendedproperty
    'MS_Description', N'accessKey',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss_config',
    'COLUMN', N'access_key'
GO
EXEC sp_addextendedproperty
    'MS_Description', N'秘钥',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss_config',
    'COLUMN', N'secret_key'
GO
EXEC sp_addextendedproperty
    'MS_Description', N'桶名称',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss_config',
    'COLUMN', N'bucket_name'
GO
EXEC sp_addextendedproperty
    'MS_Description', N'前缀',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss_config',
    'COLUMN', N'prefix'
GO
EXEC sp_addextendedproperty
    'MS_Description', N'访问站点',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss_config',
    'COLUMN', N'endpoint'
GO
EXEC sp_addextendedproperty
     'MS_Description', N'自定义域名',
     'SCHEMA', N'dbo',
     'TABLE', N'sys_oss_config',
     'COLUMN', N'domain'
GO
EXEC sp_addextendedproperty
    'MS_Description', N'是否https（Y=是,N=否）',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss_config',
    'COLUMN', N'is_https'
GO
EXEC sp_addextendedproperty
    'MS_Description', N'域',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss_config',
    'COLUMN', N'region'
GO
EXEC sp_addextendedproperty
     'MS_Description', N'桶权限类型(0=private 1=public 2=custom)',
     'SCHEMA', N'dbo',
     'TABLE', N'sys_oss_config',
     'COLUMN', N'access_policy'
GO
EXEC sp_addextendedproperty
    'MS_Description', N'是否默认（0=是,1=否）',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss_config',
    'COLUMN', N'status'
GO
EXEC sp_addextendedproperty
    'MS_Description', N'扩展字段',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss_config',
    'COLUMN', N'ext1'
GO
EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建部门' ,
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss_config',
    'COLUMN', N'create_dept'
GO
EXEC sp_addextendedproperty
    'MS_Description', N'创建者',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss_config',
    'COLUMN', N'create_by'
GO
EXEC sp_addextendedproperty
    'MS_Description', N'创建时间',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss_config',
    'COLUMN', N'create_time'
GO
EXEC sp_addextendedproperty
    'MS_Description', N'更新者',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss_config',
    'COLUMN', N'update_by'
GO
EXEC sp_addextendedproperty
    'MS_Description', N'更新时间',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss_config',
    'COLUMN', N'update_time'
GO
EXEC sp_addextendedproperty
    'MS_Description', N'备注',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss_config',
    'COLUMN', N'remark'
GO
EXEC sp_addextendedproperty
    'MS_Description', N'对象存储配置表',
    'SCHEMA', N'dbo',
    'TABLE', N'sys_oss_config'
GO

INSERT INTO sys_oss_config VALUES (N'1', N'000000', N'minio', N'ruoyi',            N'ruoyi123',        N'ruoyi',            N'', N'127.0.0.1:9000',                    N'',N'N', N'',           N'1', N'0', N'', 103, 1, getdate(), 1, getdate(), NULL)
GO
INSERT INTO sys_oss_config VALUES (N'2', N'000000', N'qiniu', N'XXXXXXXXXXXXXXXX', N'XXXXXXXXXXXXXXX', N'ruoyi',            N'', N's3-cn-north-1.qiniucs.com',         N'',N'N', N'',           N'1', N'1', N'', 103, 1, getdate(), 1, getdate(), NULL)
GO
INSERT INTO sys_oss_config VALUES (N'3', N'000000', N'aliyun', N'XXXXXXXXXXXXXXX', N'XXXXXXXXXXXXXXX', N'ruoyi',            N'', N'oss-cn-beijing.aliyuncs.com',       N'',N'N', N'',           N'1', N'1', N'', 103, 1, getdate(), 1, getdate(), NULL)
GO
INSERT INTO sys_oss_config VALUES (N'4', N'000000', N'qcloud', N'XXXXXXXXXXXXXXX', N'XXXXXXXXXXXXXXX', N'ruoyi-1250000000', N'', N'cos.ap-beijing.myqcloud.com',       N'',N'N', N'ap-beijing', N'1', N'1', N'', 103, 1, getdate(), 1, getdate(), NULL)
GO
INSERT INTO sys_oss_config VALUES (N'5', N'000000', N'image',  N'ruoyi',           N'ruoyi123',        N'ruoyi',            N'image', N'127.0.0.1:9000',               N'',N'N', N'',           N'1', N'1', N'', 103, 1, getdate(), 1, getdate(), NULL)
GO
