CREATE TABLE test_demo
(
    id          bigint            NOT NULL,
    tenant_id   nvarchar(20)      DEFAULT ('000000') NULL,
    dept_id     bigint            NULL,
    user_id     bigint            NULL,
    order_num   int DEFAULT ((0)) NULL,
    test_key    nvarchar(255)     NULL,
    value       nvarchar(255)     NULL,
    version     int DEFAULT ((0)) NULL,
    create_dept bigint            NULL,
    create_time datetime2(0)      NULL,
    create_by   bigint            NULL,
    update_time datetime2(0)      NULL,
    update_by   bigint            NULL,
    del_flag    int DEFAULT ((0)) NULL,
    CONSTRAINT PK__test_dem__3213E83F176051C8 PRIMARY KEY CLUSTERED (id)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'test_demo',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
    'MS_Description', N'租户id',
    'SCHEMA', N'dbo',
    'TABLE', N'test_demo',
    'COLUMN', N'tenant_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'部门id',
     'SCHEMA', N'dbo',
     'TABLE', N'test_demo',
     'COLUMN', N'dept_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'用户id',
     'SCHEMA', N'dbo',
     'TABLE', N'test_demo',
     'COLUMN', N'user_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'排序号',
     'SCHEMA', N'dbo',
     'TABLE', N'test_demo',
     'COLUMN', N'order_num'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'key键',
     'SCHEMA', N'dbo',
     'TABLE', N'test_demo',
     'COLUMN', N'test_key'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'值',
     'SCHEMA', N'dbo',
     'TABLE', N'test_demo',
     'COLUMN', N'value'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'版本',
     'SCHEMA', N'dbo',
     'TABLE', N'test_demo',
     'COLUMN', N'version'
GO

EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建部门' ,
    'SCHEMA', N'dbo',
    'TABLE', N'test_demo',
    'COLUMN', N'create_dept'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'test_demo',
     'COLUMN', N'create_time'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建人',
     'SCHEMA', N'dbo',
     'TABLE', N'test_demo',
     'COLUMN', N'create_by'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'更新时间',
     'SCHEMA', N'dbo',
     'TABLE', N'test_demo',
     'COLUMN', N'update_time'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'更新人',
     'SCHEMA', N'dbo',
     'TABLE', N'test_demo',
     'COLUMN', N'update_by'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'删除标志',
     'SCHEMA', N'dbo',
     'TABLE', N'test_demo',
     'COLUMN', N'del_flag'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'测试单表',
     'SCHEMA', N'dbo',
     'TABLE', N'test_demo'
GO

CREATE TABLE test_tree
(
    id          bigint               NOT NULL,
    tenant_id   nvarchar(20)         DEFAULT ('000000') NULL,
    parent_id   bigint DEFAULT ((0)) NULL,
    dept_id     bigint               NULL,
    user_id     bigint               NULL,
    tree_name   nvarchar(255)        NULL,
    version     int    DEFAULT ((0)) NULL,
    create_dept bigint               NULL,
    create_time datetime2(0)         NULL,
    create_by   bigint               NULL,
    update_time datetime2(0)         NULL,
    update_by   bigint               NULL,
    del_flag    int    DEFAULT ((0)) NULL,
    CONSTRAINT PK__test_tre__3213E83FC75A1B63 PRIMARY KEY CLUSTERED (id)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
        ON [PRIMARY]
)
ON [PRIMARY]
GO

EXEC sp_addextendedproperty
     'MS_Description', N'主键',
     'SCHEMA', N'dbo',
     'TABLE', N'test_tree',
     'COLUMN', N'id'
GO

EXEC sp_addextendedproperty
    'MS_Description', N'租户id',
    'SCHEMA', N'dbo',
    'TABLE', N'test_tree',
    'COLUMN', N'tenant_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'父id',
     'SCHEMA', N'dbo',
     'TABLE', N'test_tree',
     'COLUMN', N'parent_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'部门id',
     'SCHEMA', N'dbo',
     'TABLE', N'test_tree',
     'COLUMN', N'dept_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'用户id',
     'SCHEMA', N'dbo',
     'TABLE', N'test_tree',
     'COLUMN', N'user_id'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'值',
     'SCHEMA', N'dbo',
     'TABLE', N'test_tree',
     'COLUMN', N'tree_name'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'版本',
     'SCHEMA', N'dbo',
     'TABLE', N'test_tree',
     'COLUMN', N'version'
GO

EXEC sys.sp_addextendedproperty
    'MS_Description', N'创建部门' ,
    'SCHEMA', N'dbo',
    'TABLE', N'test_tree',
    'COLUMN', N'create_dept'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建时间',
     'SCHEMA', N'dbo',
     'TABLE', N'test_tree',
     'COLUMN', N'create_time'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'创建人',
     'SCHEMA', N'dbo',
     'TABLE', N'test_tree',
     'COLUMN', N'create_by'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'更新时间',
     'SCHEMA', N'dbo',
     'TABLE', N'test_tree',
     'COLUMN', N'update_time'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'更新人',
     'SCHEMA', N'dbo',
     'TABLE', N'test_tree',
     'COLUMN', N'update_by'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'删除标志',
     'SCHEMA', N'dbo',
     'TABLE', N'test_tree',
     'COLUMN', N'del_flag'
GO

EXEC sp_addextendedproperty
     'MS_Description', N'测试树表',
     'SCHEMA', N'dbo',
     'TABLE', N'test_tree'
GO

INSERT sys_user VALUES (3, N'000000', 108, N'test', N'本部门及以下 密码666666', N'sys_user', N'', N'', N'0', NULL, N'$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', N'0', N'0', N'127.0.0.1', getdate(), 103, 1, getdate(), 3, getdate(), NULL);
GO
INSERT sys_user VALUES (4, N'000000', 102, N'test1', N'仅本人 密码666666', N'sys_user', N'', N'', N'0', NULL, N'$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', N'0', N'0', N'127.0.0.1', getdate(), 103, 1, getdate(), 4, getdate(), NULL);
GO

INSERT sys_menu VALUES (5, N'测试菜单', 0, 5, N'demo', NULL, 1, 0, N'M', N'0', N'0', NULL, N'star', 103, 1, getdate(), NULL, NULL, N'');
GO

INSERT sys_menu VALUES (1500, N'测试单表', 5, 1, N'demo', N'demo/demo/index', 1, 0, N'C', N'0', N'0', N'demo:demo:list', N'#', 103, 1, getdate(), NULL, NULL, N'测试单表菜单');
GO
INSERT sys_menu VALUES (1501, N'测试单表查询', 1500, 1, N'#', N'', 1, 0, N'F', N'0', N'0', N'demo:demo:query', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (1502, N'测试单表新增', 1500, 2, N'#', N'', 1, 0, N'F', N'0', N'0', N'demo:demo:add', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (1503, N'测试单表修改', 1500, 3, N'#', N'', 1, 0, N'F', N'0', N'0', N'demo:demo:edit', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (1504, N'测试单表删除', 1500, 4, N'#', N'', 1, 0, N'F', N'0', N'0', N'demo:demo:remove', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (1505, N'测试单表导出', 1500, 5, N'#', N'', 1, 0, N'F', N'0', N'0', N'demo:demo:export', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO

INSERT sys_menu VALUES (1506, N'测试树表', 5, 1, N'tree', N'demo/tree/index', 1, 0, N'C', N'0', N'0', N'demo:tree:list', N'#', 103, 1, getdate(), NULL, NULL, N'测试树表菜单');
GO
INSERT sys_menu VALUES (1507, N'测试树表查询', 1506, 1, N'#', N'', 1, 0, N'F', N'0', N'0', N'demo:tree:query', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (1508, N'测试树表新增', 1506, 2, N'#', N'', 1, 0, N'F', N'0', N'0', N'demo:tree:add', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (1509, N'测试树表修改', 1506, 3, N'#', N'', 1, 0, N'F', N'0', N'0', N'demo:tree:edit', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (1510, N'测试树表删除', 1506, 4, N'#', N'', 1, 0, N'F', N'0', N'0', N'demo:tree:remove', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO
INSERT sys_menu VALUES (1511, N'测试树表导出', 1506, 5, N'#', N'', 1, 0, N'F', N'0', N'0', N'demo:tree:export', N'#', 103, 1, getdate(), NULL, NULL, N'');
GO

INSERT sys_role VALUES (3, N'000000', N'本部门及以下', N'test1', 3, N'4', 1, 1, N'0', N'0', 103, 1, getdate(), 1, NULL, NULL);
GO
INSERT sys_role VALUES (4, N'000000', N'仅本人', N'test2', 4, N'5', 1, 1, N'0', N'0', 103, 1, getdate(), 1, NULL, NULL);
GO

INSERT sys_role_menu VALUES (3, 1);
GO
INSERT sys_role_menu VALUES (3, 5);
GO
INSERT sys_role_menu VALUES (3, 100);
GO
INSERT sys_role_menu VALUES (3, 101);
GO
INSERT sys_role_menu VALUES (3, 102);
GO
INSERT sys_role_menu VALUES (3, 103);
GO
INSERT sys_role_menu VALUES (3, 104);
GO
INSERT sys_role_menu VALUES (3, 105);
GO
INSERT sys_role_menu VALUES (3, 106);
GO
INSERT sys_role_menu VALUES (3, 107);
GO
INSERT sys_role_menu VALUES (3, 108);
GO
INSERT sys_role_menu VALUES (3, 500);
GO
INSERT sys_role_menu VALUES (3, 501);
GO
INSERT sys_role_menu VALUES (3, 1001);
GO
INSERT sys_role_menu VALUES (3, 1002);
GO
INSERT sys_role_menu VALUES (3, 1003);
GO
INSERT sys_role_menu VALUES (3, 1004);
GO
INSERT sys_role_menu VALUES (3, 1005);
GO
INSERT sys_role_menu VALUES (3, 1006);
GO
INSERT sys_role_menu VALUES (3, 1007);
GO
INSERT sys_role_menu VALUES (3, 1008);
GO
INSERT sys_role_menu VALUES (3, 1009);
GO
INSERT sys_role_menu VALUES (3, 1010);
GO
INSERT sys_role_menu VALUES (3, 1011);
GO
INSERT sys_role_menu VALUES (3, 1012);
GO
INSERT sys_role_menu VALUES (3, 1013);
GO
INSERT sys_role_menu VALUES (3, 1014);
GO
INSERT sys_role_menu VALUES (3, 1015);
GO
INSERT sys_role_menu VALUES (3, 1016);
GO
INSERT sys_role_menu VALUES (3, 1017);
GO
INSERT sys_role_menu VALUES (3, 1018);
GO
INSERT sys_role_menu VALUES (3, 1019);
GO
INSERT sys_role_menu VALUES (3, 1020);
GO
INSERT sys_role_menu VALUES (3, 1021);
GO
INSERT sys_role_menu VALUES (3, 1022);
GO
INSERT sys_role_menu VALUES (3, 1023);
GO
INSERT sys_role_menu VALUES (3, 1024);
GO
INSERT sys_role_menu VALUES (3, 1025);
GO
INSERT sys_role_menu VALUES (3, 1026);
GO
INSERT sys_role_menu VALUES (3, 1027);
GO
INSERT sys_role_menu VALUES (3, 1028);
GO
INSERT sys_role_menu VALUES (3, 1029);
GO
INSERT sys_role_menu VALUES (3, 1030);
GO
INSERT sys_role_menu VALUES (3, 1031);
GO
INSERT sys_role_menu VALUES (3, 1032);
GO
INSERT sys_role_menu VALUES (3, 1033);
GO
INSERT sys_role_menu VALUES (3, 1034);
GO
INSERT sys_role_menu VALUES (3, 1035);
GO
INSERT sys_role_menu VALUES (3, 1036);
GO
INSERT sys_role_menu VALUES (3, 1037);
GO
INSERT sys_role_menu VALUES (3, 1038);
GO
INSERT sys_role_menu VALUES (3, 1039);
GO
INSERT sys_role_menu VALUES (3, 1040);
GO
INSERT sys_role_menu VALUES (3, 1041);
GO
INSERT sys_role_menu VALUES (3, 1042);
GO
INSERT sys_role_menu VALUES (3, 1043);
GO
INSERT sys_role_menu VALUES (3, 1044);
GO
INSERT sys_role_menu VALUES (3, 1045);
GO
INSERT sys_role_menu VALUES (3, 1500);
GO
INSERT sys_role_menu VALUES (3, 1501);
GO
INSERT sys_role_menu VALUES (3, 1502);
GO
INSERT sys_role_menu VALUES (3, 1503);
GO
INSERT sys_role_menu VALUES (3, 1504);
GO
INSERT sys_role_menu VALUES (3, 1505);
GO
INSERT sys_role_menu VALUES (3, 1506);
GO
INSERT sys_role_menu VALUES (3, 1507);
GO
INSERT sys_role_menu VALUES (3, 1508);
GO
INSERT sys_role_menu VALUES (3, 1509);
GO
INSERT sys_role_menu VALUES (3, 1510);
GO
INSERT sys_role_menu VALUES (3, 1511);
GO
INSERT sys_role_menu VALUES (4, 5);
GO
INSERT sys_role_menu VALUES (4, 1500);
GO
INSERT sys_role_menu VALUES (4, 1501);
GO
INSERT sys_role_menu VALUES (4, 1502);
GO
INSERT sys_role_menu VALUES (4, 1503);
GO
INSERT sys_role_menu VALUES (4, 1504);
GO
INSERT sys_role_menu VALUES (4, 1505);
GO
INSERT sys_role_menu VALUES (4, 1506);
GO
INSERT sys_role_menu VALUES (4, 1507);
GO
INSERT sys_role_menu VALUES (4, 1508);
GO
INSERT sys_role_menu VALUES (4, 1509);
GO
INSERT sys_role_menu VALUES (4, 1510);
GO
INSERT sys_role_menu VALUES (4, 1511);
GO

INSERT sys_user_role VALUES (3, 3);
GO
INSERT sys_user_role VALUES (4, 4);
GO

INSERT test_demo VALUES (1, N'000000', 102, 4, 1, N'测试数据权限', N'测试', 0, 103, getdate(), 1, NULL, NULL, 0);
GO
INSERT test_demo VALUES (2, N'000000', 102, 3, 2, N'子节点1', N'111', 0, 103, getdate(), 1, NULL, NULL, 0);
GO
INSERT test_demo VALUES (3, N'000000', 102, 3, 3, N'子节点2', N'222', 0, 103, getdate(), 1, NULL, NULL, 0);
GO
INSERT test_demo VALUES (4, N'000000', 108, 4, 4, N'测试数据', N'demo', 0, 103, getdate(), 1, NULL, NULL, 0);
GO
INSERT test_demo VALUES (5, N'000000', 108, 3, 13, N'子节点11', N'1111', 0, 103, getdate(), 1, NULL, NULL, 0);
GO
INSERT test_demo VALUES (6, N'000000', 108, 3, 12, N'子节点22', N'2222', 0, 103, getdate(), 1, NULL, NULL, 0);
GO
INSERT test_demo VALUES (7, N'000000', 108, 3, 11, N'子节点33', N'3333', 0, 103, getdate(), 1, NULL, NULL, 0);
GO
INSERT test_demo VALUES (8, N'000000', 108, 3, 10, N'子节点44', N'4444', 0, 103, getdate(), 1, NULL, NULL, 0);
GO
INSERT test_demo VALUES (9, N'000000', 108, 3, 9, N'子节点55', N'5555', 0, 103, getdate(), 1, NULL, NULL, 0);
GO
INSERT test_demo VALUES (10, N'000000', 108, 3, 8, N'子节点66', N'6666', 0, 103, getdate(), 1, NULL, NULL, 0);
GO
INSERT test_demo VALUES (11, N'000000', 108, 3, 7, N'子节点77', N'7777', 0, 103, getdate(), 1, NULL, NULL, 0);
GO
INSERT test_demo VALUES (12, N'000000', 108, 3, 6, N'子节点88', N'8888', 0, 103, getdate(), 1, NULL, NULL, 0);
GO
INSERT test_demo VALUES (13, N'000000', 108, 3, 5, N'子节点99', N'9999', 0, 103, getdate(), 1, NULL, NULL, 0);
GO

INSERT test_tree VALUES (1, N'000000', 0, 102, 4, N'测试数据权限', 0, 103, getdate(), 1, NULL, NULL, 0);
GO
INSERT test_tree VALUES (2, N'000000', 1, 102, 3, N'子节点1', 0, 103, getdate(), 1, NULL, NULL, 0);
GO
INSERT test_tree VALUES (3, N'000000', 2, 102, 3, N'子节点2', 0, 103, getdate(), 1, NULL, NULL, 0);
GO
INSERT test_tree VALUES (4, N'000000', 0, 108, 4, N'测试树1', 0, 103, getdate(), 1, NULL, NULL, 0);
GO
INSERT test_tree VALUES (5, N'000000', 4, 108, 3, N'子节点11', 0, 103, getdate(), 1, NULL, NULL, 0);
GO
INSERT test_tree VALUES (6, N'000000', 4, 108, 3, N'子节点22', 0, 103, getdate(), 1, NULL, NULL, 0);
GO
INSERT test_tree VALUES (7, N'000000', 4, 108, 3, N'子节点33', 0, 103, getdate(), 1, NULL, NULL, 0);
GO
INSERT test_tree VALUES (8, N'000000', 5, 108, 3, N'子节点44', 0, 103, getdate(), 1, NULL, NULL, 0);
GO
INSERT test_tree VALUES (9, N'000000', 6, 108, 3, N'子节点55', 0, 103, getdate(), 1, NULL, NULL, 0);
GO
INSERT test_tree VALUES (10, N'000000', 7, 108, 3, N'子节点66', 0, 103, getdate(), 1, NULL, NULL, 0);
GO
INSERT test_tree VALUES (11, N'000000', 7, 108, 3, N'子节点77', 0, 103, getdate(), 1, NULL, NULL, 0);
GO
INSERT test_tree VALUES (12, N'000000', 10, 108, 3, N'子节点88', 0, 103, getdate(), 1, NULL, NULL, 0);
GO
INSERT test_tree VALUES (13, N'000000', 10, 108, 3, N'子节点99', 0, 103, getdate(), 1, NULL, NULL, 0);
GO
