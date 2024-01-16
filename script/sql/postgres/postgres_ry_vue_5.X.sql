-- ----------------------------
-- 租户表
-- ----------------------------
drop table if exists sys_tenant;
create table if not exists sys_tenant
(
    id                int8,
    tenant_id         varchar(20)   not null,
    contact_user_name varchar(20)   default null::varchar,
    contact_phone     varchar(20)   default null::varchar,
    company_name      varchar(50)   default null::varchar,
    license_number    varchar(30)   default null::varchar,
    address           varchar(200)  default null::varchar,
    intro             varchar(200)  default null::varchar,
    domain            varchar(200)  default null::varchar,
    remark            varchar(200)  default null::varchar,
    package_id        int8,
    expire_time       timestamp,
    account_count     int4          default -1,
    status            char          default '0'::bpchar,
    del_flag          char          default '0'::bpchar,
    create_dept       int8,
    create_by         int8,
    create_time       timestamp,
    update_by         int8,
    update_time       timestamp,
    constraint "pk_sys_tenant" primary key (id)
);


comment on table   sys_tenant                    is '租户表';
comment on column  sys_tenant.tenant_id          is '租户编号';
comment on column  sys_tenant.contact_phone      is '联系电话';
comment on column  sys_tenant.company_name       is '企业名称';
comment on column  sys_tenant.company_name       is '联系人';
comment on column  sys_tenant.license_number     is '统一社会信用代码';
comment on column  sys_tenant.address            is '地址';
comment on column  sys_tenant.intro              is '企业简介';
comment on column  sys_tenant.domain             is '域名';
comment on column  sys_tenant.remark             is '备注';
comment on column  sys_tenant.package_id         is '租户套餐编号';
comment on column  sys_tenant.expire_time        is '过期时间';
comment on column  sys_tenant.account_count      is '用户数量（-1不限制）';
comment on column  sys_tenant.status             is '租户状态（0正常 1停用）';
comment on column  sys_tenant.del_flag           is '删除标志（0代表存在 2代表删除）';
comment on column  sys_tenant.create_dept        is '创建部门';
comment on column  sys_tenant.create_by          is '创建者';
comment on column  sys_tenant.create_time        is '创建时间';
comment on column  sys_tenant.update_by          is '更新者';
comment on column  sys_tenant.update_time        is '更新时间';


-- ----------------------------
-- 初始化-租户表数据
-- ----------------------------

insert into sys_tenant values(1, '000000', '管理组', '15888888888', 'XXX有限公司', null, null, '多租户通用后台管理管理系统', null, null, null, null, -1, '0', '0', 103, 1, now(), null, null);


-- ----------------------------
-- 租户套餐表
-- ----------------------------
drop table if exists sys_tenant_package;
create table if not exists sys_tenant_package
(
    package_id          int8,
    package_name        varchar(20)     default ''::varchar,
    menu_ids            varchar(3000)   default ''::varchar,
    remark              varchar(200)    default ''::varchar,
    menu_check_strictly bool            default true,
    status              char            default '0'::bpchar,
    del_flag            char            default '0'::bpchar,
    create_dept         int8,
    create_by           int8,
    create_time         timestamp,
    update_by           int8,
    update_time         timestamp,
    constraint "pk_sys_tenant_package" primary key (package_id)
);


comment on table   sys_tenant_package                    is '租户套餐表';
comment on column  sys_tenant_package.package_id         is '租户套餐id';
comment on column  sys_tenant_package.package_name       is '套餐名称';
comment on column  sys_tenant_package.menu_ids           is '关联菜单id';
comment on column  sys_tenant_package.remark             is '备注';
comment on column  sys_tenant_package.status             is '状态（0正常 1停用）';
comment on column  sys_tenant_package.del_flag           is '删除标志（0代表存在 2代表删除）';
comment on column  sys_tenant_package.create_dept        is '创建部门';
comment on column  sys_tenant_package.create_by          is '创建者';
comment on column  sys_tenant_package.create_time        is '创建时间';
comment on column  sys_tenant_package.update_by          is '更新者';
comment on column  sys_tenant_package.update_time        is '更新时间';


-- ----------------------------
-- 1、部门表
-- ----------------------------
drop table if exists sys_dept;
create table if not exists sys_dept
(
    dept_id     int8,
    tenant_id   varchar(20) default '000000'::varchar,
    parent_id   int8        default 0,
    ancestors   varchar(500)default ''::varchar,
    dept_name   varchar(30) default ''::varchar,
    order_num   int4        default 0,
    leader      varchar(20) default null::varchar,
    phone       varchar(11) default null::varchar,
    email       varchar(50) default null::varchar,
    status      char        default '0'::bpchar,
    del_flag    char        default '0'::bpchar,
    create_dept int8,
    create_by   int8,
    create_time timestamp,
    update_by   int8,
    update_time timestamp,
    constraint "sys_dept_pk" primary key (dept_id)
);

comment on table sys_dept               is '部门表';
comment on column sys_dept.dept_id      is '部门ID';
comment on column sys_dept.tenant_id    is '租户编号';
comment on column sys_dept.parent_id    is '父部门ID';
comment on column sys_dept.ancestors    is '祖级列表';
comment on column sys_dept.dept_name    is '部门名称';
comment on column sys_dept.order_num    is '显示顺序';
comment on column sys_dept.leader       is '负责人';
comment on column sys_dept.phone        is '联系电话';
comment on column sys_dept.email        is '邮箱';
comment on column sys_dept.status       is '部门状态（0正常 1停用）';
comment on column sys_dept.del_flag     is '删除标志（0代表存在 2代表删除）';
comment on column sys_dept.create_dept  is '创建部门';
comment on column sys_dept.create_by    is '创建者';
comment on column sys_dept.create_time  is '创建时间';
comment on column sys_dept.update_by    is '更新者';
comment on column sys_dept.update_time  is '更新时间';

-- ----------------------------
-- 初始化-部门表数据
-- ----------------------------
insert into sys_dept values(100, '000000', 0,   '0',          'XXX科技',   0, '疯狂的狮子Li', '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null);
insert into sys_dept values(101, '000000', 100, '0,100',      '深圳总公司', 1, '疯狂的狮子Li', '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null);
insert into sys_dept values(102, '000000', 100, '0,100',      '长沙分公司', 2, '疯狂的狮子Li', '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null);
insert into sys_dept values(103, '000000', 101, '0,100,101',  '研发部门',   1, '疯狂的狮子Li', '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null);
insert into sys_dept values(104, '000000', 101, '0,100,101',  '市场部门',   2, '疯狂的狮子Li', '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null);
insert into sys_dept values(105, '000000', 101, '0,100,101',  '测试部门',   3, '疯狂的狮子Li', '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null);
insert into sys_dept values(106, '000000', 101, '0,100,101',  '财务部门',   4, '疯狂的狮子Li', '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null);
insert into sys_dept values(107, '000000', 101, '0,100,101',  '运维部门',   5, '疯狂的狮子Li', '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null);
insert into sys_dept values(108, '000000', 102, '0,100,102',  '市场部门',   1, '疯狂的狮子Li', '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null);
insert into sys_dept values(109, '000000', 102, '0,100,102',  '财务部门',   2, '疯狂的狮子Li', '15888888888', 'xxx@qq.com', '0', '0', 103, 1, now(), null, null);

-- ----------------------------
-- 2、用户信息表
-- ----------------------------
drop table if exists sys_user;
create table if not exists sys_user
(
    user_id     int8,
    tenant_id   varchar(20)  default '000000'::varchar,
    dept_id     int8,
    user_name   varchar(30)  not null,
    nick_name   varchar(30)  not null,
    user_type   varchar(10)  default 'sys_user'::varchar,
    email       varchar(50)  default ''::varchar,
    phonenumber varchar(11)  default ''::varchar,
    sex         char         default '0'::bpchar,
    avatar      int8,
    password    varchar(100) default ''::varchar,
    status      char         default '0'::bpchar,
    del_flag    char         default '0'::bpchar,
    login_ip    varchar(128) default ''::varchar,
    login_date  timestamp,
    create_dept int8,
    create_by   int8,
    create_time timestamp,
    update_by   int8,
    update_time timestamp,
    remark      varchar(500) default null::varchar,
    constraint "sys_user_pk" primary key (user_id)
);

comment on table sys_user               is '用户信息表';
comment on column sys_user.user_id      is '用户ID';
comment on column sys_user.tenant_id    is '租户编号';
comment on column sys_user.dept_id      is '部门ID';
comment on column sys_user.user_name    is '用户账号';
comment on column sys_user.nick_name    is '用户昵称';
comment on column sys_user.user_type    is '用户类型（sys_user系统用户）';
comment on column sys_user.email        is '用户邮箱';
comment on column sys_user.phonenumber  is '手机号码';
comment on column sys_user.sex          is '用户性别（0男 1女 2未知）';
comment on column sys_user.avatar       is '头像地址';
comment on column sys_user.password     is '密码';
comment on column sys_user.status       is '帐号状态（0正常 1停用）';
comment on column sys_user.del_flag     is '删除标志（0代表存在 2代表删除）';
comment on column sys_user.login_ip     is '最后登陆IP';
comment on column sys_user.login_date   is '最后登陆时间';
comment on column sys_user.create_dept  is '创建部门';
comment on column sys_user.create_by    is '创建者';
comment on column sys_user.create_time  is '创建时间';
comment on column sys_user.update_by    is '更新者';
comment on column sys_user.update_time  is '更新时间';
comment on column sys_user.remark       is '备注';

-- ----------------------------

-- 初始化-用户信息表数据
-- ----------------------------
insert into sys_user values(1, '000000', 103, 'admin', '疯狂的狮子Li', 'sys_user', 'crazyLionLi@163.com', '15888888888', '1', null, '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', now(), 103, 1, now(), null, null, '管理员');
insert into sys_user values(2, '000000', 105, 'lionli', '疯狂的狮子Li', 'sys_user', 'crazyLionLi@qq.com',  '15666666666', '1', null, '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', now(), 103, 1, now(), null, null, '测试员');


-- ----------------------------
-- 3、岗位信息表
-- ----------------------------
drop table if exists sys_post;
create table if not exists sys_post
(
    post_id     int8,
    tenant_id   varchar(20) default '000000'::varchar,
    post_code   varchar(64) not null,
    post_name   varchar(50) not null,
    post_sort   int4        not null,
    status      char        not null,
    create_dept int8,
    create_by   int8,
    create_time timestamp,
    update_by   int8,
    update_time timestamp,
    remark      varchar(500) default null::varchar,
    constraint "sys_post_pk" primary key (post_id)
);

comment on table sys_post               is '岗位信息表';
comment on column sys_post.post_id      is '岗位ID';
comment on column sys_post.tenant_id    is '租户编号';
comment on column sys_post.post_code    is '岗位编码';
comment on column sys_post.post_name    is '岗位名称';
comment on column sys_post.post_sort    is '显示顺序';
comment on column sys_post.status       is '状态（0正常 1停用）';
comment on column sys_post.create_dept  is '创建部门';
comment on column sys_post.create_by    is '创建者';
comment on column sys_post.create_time  is '创建时间';
comment on column sys_post.update_by    is '更新者';
comment on column sys_post.update_time  is '更新时间';
comment on column sys_post.remark       is '备注';

-- ----------------------------
-- 初始化-岗位信息表数据
-- ----------------------------
insert into sys_post values(1, '000000', 'ceo',  '董事长',    1, '0', 103, 1, now(), null, null, '');
insert into sys_post values(2, '000000', 'se',   '项目经理',  2, '0', 103, 1, now(), null, null, '');
insert into sys_post values(3, '000000', 'hr',   '人力资源',  3, '0', 103, 1, now(), null, null, '');
insert into sys_post values(4, '000000', 'user', '普通员工',  4, '0', 103, 1, now(), null, null, '');

-- ----------------------------
-- 4、角色信息表
-- ----------------------------
drop table if exists sys_role;
create table if not exists sys_role
(
    role_id             int8,
    tenant_id           varchar(20)  default '000000'::varchar,
    role_name           varchar(30)  not null,
    role_key            varchar(100) not null,
    role_sort           int4         not null,
    data_scope          char         default '1'::bpchar,
    menu_check_strictly bool         default true,
    dept_check_strictly bool         default true,
    status              char         not null,
    del_flag            char         default '0'::bpchar,
    create_dept         int8,
    create_by           int8,
    create_time         timestamp,
    update_by           int8,
    update_time         timestamp,
    remark              varchar(500) default null::varchar,
    constraint "sys_role_pk" primary key (role_id)
);

comment on table sys_role                       is '角色信息表';
comment on column sys_role.role_id              is '角色ID';
comment on column sys_role.tenant_id            is '租户编号';
comment on column sys_role.role_name            is '角色名称';
comment on column sys_role.role_key             is '角色权限字符串';
comment on column sys_role.role_sort            is '显示顺序';
comment on column sys_role.data_scope           is '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）';
comment on column sys_role.menu_check_strictly  is '菜单树选择项是否关联显示';
comment on column sys_role.dept_check_strictly  is '部门树选择项是否关联显示';
comment on column sys_role.status               is '角色状态（0正常 1停用）';
comment on column sys_role.del_flag             is '删除标志（0代表存在 2代表删除）';
comment on column sys_role.create_dept          is '创建部门';
comment on column sys_role.create_by            is '创建者';
comment on column sys_role.create_time          is '创建时间';
comment on column sys_role.update_by            is '更新者';
comment on column sys_role.update_time          is '更新时间';
comment on column sys_role.remark               is '备注';

-- ----------------------------
-- 初始化-角色信息表数据
-- ----------------------------
insert into sys_role values('1', '000000', '超级管理员',  'superadmin',  1, '1', 't', 't', '0', '0', 103, 1, now(), null, null, '超级管理员');
insert into sys_role values('2', '000000', '普通角色',    'common', 2, '2', 't', 't', '0', '0', 103, 1, now(), null, null, '普通角色');


-- ----------------------------
-- 5、菜单权限表
-- ----------------------------
drop table if exists sys_menu;
create table if not exists sys_menu
(
    menu_id     int8,
    menu_name   varchar(50) not null,
    parent_id   int8         default 0,
    order_num   int4         default 0,
    path        varchar(200) default ''::varchar,
    component   varchar(255) default null::varchar,
    query_param varchar(255) default null::varchar,
    is_frame    char         default '1'::bpchar,
    is_cache    char         default '0'::bpchar,
    menu_type   char         default ''::bpchar,
    visible     char         default '0'::bpchar,
    status      char         default '0'::bpchar,
    perms       varchar(100) default null::varchar,
    icon        varchar(100) default '#'::varchar,
    create_dept int8,
    create_by   int8,
    create_time timestamp,
    update_by   int8,
    update_time timestamp,
    remark      varchar(500) default ''::varchar,
    constraint "sys_menu_pk" primary key (menu_id)
);

comment on table sys_menu               is '菜单权限表';
comment on column sys_menu.menu_id      is '菜单ID';
comment on column sys_menu.menu_name    is '菜单名称';
comment on column sys_menu.parent_id    is '父菜单ID';
comment on column sys_menu.order_num    is '显示顺序';
comment on column sys_menu.path         is '路由地址';
comment on column sys_menu.component    is '组件路径';
comment on column sys_menu.query_param  is '路由参数';
comment on column sys_menu.is_frame     is '是否为外链（0是 1否）';
comment on column sys_menu.is_cache     is '是否缓存（0缓存 1不缓存）';
comment on column sys_menu.menu_type    is '菜单类型（M目录 C菜单 F按钮）';
comment on column sys_menu.visible      is '显示状态（0显示 1隐藏）';
comment on column sys_menu.status       is '菜单状态（0正常 1停用）';
comment on column sys_menu.perms        is '权限标识';
comment on column sys_menu.icon         is '菜单图标';
comment on column sys_menu.create_dept  is '创建部门';
comment on column sys_menu.create_by    is '创建者';
comment on column sys_menu.create_time  is '创建时间';
comment on column sys_menu.update_by    is '更新者';
comment on column sys_menu.update_time  is '更新时间';
comment on column sys_menu.remark       is '备注';

-- ----------------------------
-- 初始化-菜单信息表数据
-- ----------------------------
-- 一级菜单
insert into sys_menu values('1', '系统管理', '0', '1', 'system',           null, '', '1', '0', 'M', '0', '0', '', 'system',   103, 1, now(), null, null, '系统管理目录');
insert into sys_menu values('6', '系统管理', '0', '2', 'tenant',           null, '', '1', '0', 'M', '0', '0', '', 'chart',    103, 1, now(), null, null, '租户管理目录');
insert into sys_menu values('2', '系统监控', '0', '3', 'monitor',          null, '', '1', '0', 'M', '0', '0', '', 'monitor',  103, 1, now(), null, null, '系统监控目录');
insert into sys_menu values('3', '系统工具', '0', '4', 'tool',             null, '', '1', '0', 'M', '0', '0', '', 'tool',     103, 1, now(), null, null, '系统工具目录');
insert into sys_menu values('4', 'PLUS官网', '0', '5', 'https://gitee.com/dromara/RuoYi-Vue-Plus', null, '', '0', '0', 'M', '0', '0', '', 'guide',    103, 1, now(), null, null, 'RuoYi-Vue-Plus官网地址');
-- 二级菜单
insert into sys_menu values('100',  '用户管理',     '1',   '1', 'user',             'system/user/index',            '', '1', '0', 'C', '0', '0', 'system:user:list',            'user',          103, 1, now(), null, null, '用户管理菜单');
insert into sys_menu values('101',  '角色管理',     '1',   '2', 'role',             'system/role/index',            '', '1', '0', 'C', '0', '0', 'system:role:list',            'peoples',       103, 1, now(), null, null, '角色管理菜单');
insert into sys_menu values('102',  '菜单管理',     '1',   '3', 'menu',             'system/menu/index',            '', '1', '0', 'C', '0', '0', 'system:menu:list',            'tree-table',    103, 1, now(), null, null, '菜单管理菜单');
insert into sys_menu values('103',  '部门管理',     '1',   '4', 'dept',             'system/dept/index',            '', '1', '0', 'C', '0', '0', 'system:dept:list',            'tree',          103, 1, now(), null, null, '部门管理菜单');
insert into sys_menu values('104',  '岗位管理',     '1',   '5', 'post',             'system/post/index',            '', '1', '0', 'C', '0', '0', 'system:post:list',            'post',          103, 1, now(), null, null, '岗位管理菜单');
insert into sys_menu values('105',  '字典管理',     '1',   '6', 'dict',             'system/dict/index',            '', '1', '0', 'C', '0', '0', 'system:dict:list',            'dict',          103, 1, now(), null, null, '字典管理菜单');
insert into sys_menu values('106',  '参数设置',     '1',   '7', 'config',           'system/config/index',          '', '1', '0', 'C', '0', '0', 'system:config:list',          'edit',          103, 1, now(), null, null, '参数设置菜单');
insert into sys_menu values('107',  '通知公告',     '1',   '8', 'notice',           'system/notice/index',          '', '1', '0', 'C', '0', '0', 'system:notice:list',          'message',       103, 1, now(), null, null, '通知公告菜单');
insert into sys_menu values('108',  '日志管理',     '1',   '9', 'log',              '',                             '', '1', '0', 'M', '0', '0', '',                            'log',           103, 1, now(), null, null, '日志管理菜单');
insert into sys_menu values('109',  '在线用户',     '2',   '1', 'online',           'monitor/online/index',         '', '1', '0', 'C', '0', '0', 'monitor:online:list',         'online',        103, 1, now(), null, null, '在线用户菜单');
insert into sys_menu values('113',  '缓存监控',     '2',   '5', 'cache',            'monitor/cache/index',          '', '1', '0', 'C', '0', '0', 'monitor:cache:list',          'redis',         103, 1, now(), null, null, '缓存监控菜单');
insert into sys_menu values('114',  '表单构建',     '3',   '1', 'build',            'tool/build/index',             '', '1', '0', 'C', '0', '0', 'tool:build:list',             'build',         103, 1, now(), null, null, '表单构建菜单');
insert into sys_menu values('115',  '代码生成',     '3',   '2', 'gen',              'tool/gen/index',               '', '1', '0', 'C', '0', '0', 'tool:gen:list',               'code',          103, 1, now(), null, null, '代码生成菜单');
insert into sys_menu values('121',  '租户管理',     '6',   '1', 'tenant',           'system/tenant/index',          '', '1', '0', 'C', '0', '0', 'system:tenant:list',          'list',          103, 1, now(), null, null, '租户管理菜单');
insert into sys_menu values('122',  '租户套餐管理',  '6',   '2', 'tenantPackage',    'system/tenantPackage/index',   '', '1', '0', 'C', '0', '0', 'system:tenantPackage:list',   'form',          103, 1, now(), null, null, '租户套餐管理菜单');

-- springboot-admin监控
insert into sys_menu values('117',  'Admin监控',   '2',   '5',  'Admin',            'monitor/admin/index',         '', '1', '0', 'C', '0', '0', 'monitor:admin:list',          'dashboard',     103, 1, now(), null, null, 'Admin监控菜单');
-- oss菜单
insert into sys_menu values('118',  '文件管理',     '1',   '10', 'oss',              'system/oss/index',            '', '1', '0', 'C', '0', '0', 'system:oss:list',             'upload',        103, 1, now(), null, null, '文件管理菜单');
-- xxl-job-admin控制台
insert into sys_menu values('120',  '任务调度中心',  '2',   '5',  'XxlJob',           'monitor/xxljob/index',        '', '1', '0', 'C', '0', '0', 'monitor:xxljob:list',         'job',           103, 1, now(), null, null, 'Xxl-Job控制台菜单');

-- 三级菜单
insert into sys_menu values('500',  '操作日志', '108', '1', 'operlog',    'monitor/operlog/index',    '', '1', '0', 'C', '0', '0', 'monitor:operlog:list',    'form',          103, 1, now(), null, null, '操作日志菜单');
insert into sys_menu values('501',  '登录日志', '108', '2', 'logininfor', 'monitor/logininfor/index', '', '1', '0', 'C', '0', '0', 'monitor:logininfor:list', 'logininfor',    103, 1, now(), null, null, '登录日志菜单');
-- 用户管理按钮
insert into sys_menu values('1001', '用户查询', '100', '1',  '', '', '', '1', '0', 'F', '0', '0', 'system:user:query',          '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1002', '用户新增', '100', '2',  '', '', '', '1', '0', 'F', '0', '0', 'system:user:add',            '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1003', '用户修改', '100', '3',  '', '', '', '1', '0', 'F', '0', '0', 'system:user:edit',           '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1004', '用户删除', '100', '4',  '', '', '', '1', '0', 'F', '0', '0', 'system:user:remove',         '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1005', '用户导出', '100', '5',  '', '', '', '1', '0', 'F', '0', '0', 'system:user:export',         '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1006', '用户导入', '100', '6',  '', '', '', '1', '0', 'F', '0', '0', 'system:user:import',         '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1007', '重置密码', '100', '7',  '', '', '', '1', '0', 'F', '0', '0', 'system:user:resetPwd',       '#', 103, 1, now(), null, null, '');
-- 角色管理按钮
insert into sys_menu values('1008', '角色查询', '101', '1',  '', '', '', '1', '0', 'F', '0', '0', 'system:role:query',          '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1009', '角色新增', '101', '2',  '', '', '', '1', '0', 'F', '0', '0', 'system:role:add',            '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1010', '角色修改', '101', '3',  '', '', '', '1', '0', 'F', '0', '0', 'system:role:edit',           '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1011', '角色删除', '101', '4',  '', '', '', '1', '0', 'F', '0', '0', 'system:role:remove',         '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1012', '角色导出', '101', '5',  '', '', '', '1', '0', 'F', '0', '0', 'system:role:export',         '#', 103, 1, now(), null, null, '');
-- 菜单管理按钮
insert into sys_menu values('1013', '菜单查询', '102', '1',  '', '', '', '1', '0', 'F', '0', '0', 'system:menu:query',          '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1014', '菜单新增', '102', '2',  '', '', '', '1', '0', 'F', '0', '0', 'system:menu:add',            '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1015', '菜单修改', '102', '3',  '', '', '', '1', '0', 'F', '0', '0', 'system:menu:edit',           '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1016', '菜单删除', '102', '4',  '', '', '', '1', '0', 'F', '0', '0', 'system:menu:remove',         '#', 103, 1, now(), null, null, '');
-- 部门管理按钮
insert into sys_menu values('1017', '部门查询', '103', '1',  '', '', '', '1', '0', 'F', '0', '0', 'system:dept:query',          '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1018', '部门新增', '103', '2',  '', '', '', '1', '0', 'F', '0', '0', 'system:dept:add',            '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1019', '部门修改', '103', '3',  '', '', '', '1', '0', 'F', '0', '0', 'system:dept:edit',           '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1020', '部门删除', '103', '4',  '', '', '', '1', '0', 'F', '0', '0', 'system:dept:remove',         '#', 103, 1, now(), null, null, '');
-- 岗位管理按钮
insert into sys_menu values('1021', '岗位查询', '104', '1',  '', '', '', '1', '0', 'F', '0', '0', 'system:post:query',          '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1022', '岗位新增', '104', '2',  '', '', '', '1', '0', 'F', '0', '0', 'system:post:add',            '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1023', '岗位修改', '104', '3',  '', '', '', '1', '0', 'F', '0', '0', 'system:post:edit',           '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1024', '岗位删除', '104', '4',  '', '', '', '1', '0', 'F', '0', '0', 'system:post:remove',         '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1025', '岗位导出', '104', '5',  '', '', '', '1', '0', 'F', '0', '0', 'system:post:export',         '#', 103, 1, now(), null, null, '');
-- 字典管理按钮
insert into sys_menu values('1026', '字典查询', '105', '1', '#', '', '', '1', '0', 'F', '0', '0', 'system:dict:query',          '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1027', '字典新增', '105', '2', '#', '', '', '1', '0', 'F', '0', '0', 'system:dict:add',            '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1028', '字典修改', '105', '3', '#', '', '', '1', '0', 'F', '0', '0', 'system:dict:edit',           '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1029', '字典删除', '105', '4', '#', '', '', '1', '0', 'F', '0', '0', 'system:dict:remove',         '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1030', '字典导出', '105', '5', '#', '', '', '1', '0', 'F', '0', '0', 'system:dict:export',         '#', 103, 1, now(), null, null, '');
-- 参数设置按钮
insert into sys_menu values('1031', '参数查询', '106', '1', '#', '', '', '1', '0', 'F', '0', '0', 'system:config:query',        '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1032', '参数新增', '106', '2', '#', '', '', '1', '0', 'F', '0', '0', 'system:config:add',          '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1033', '参数修改', '106', '3', '#', '', '', '1', '0', 'F', '0', '0', 'system:config:edit',         '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1034', '参数删除', '106', '4', '#', '', '', '1', '0', 'F', '0', '0', 'system:config:remove',       '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1035', '参数导出', '106', '5', '#', '', '', '1', '0', 'F', '0', '0', 'system:config:export',       '#', 103, 1, now(), null, null, '');
-- 通知公告按钮
insert into sys_menu values('1036', '公告查询', '107', '1', '#', '', '', '1', '0', 'F', '0', '0', 'system:notice:query',        '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1037', '公告新增', '107', '2', '#', '', '', '1', '0', 'F', '0', '0', 'system:notice:add',          '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1038', '公告修改', '107', '3', '#', '', '', '1', '0', 'F', '0', '0', 'system:notice:edit',         '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1039', '公告删除', '107', '4', '#', '', '', '1', '0', 'F', '0', '0', 'system:notice:remove',       '#', 103, 1, now(), null, null, '');
-- 操作日志按钮
insert into sys_menu values('1040', '操作查询', '500', '1', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:operlog:query',      '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1041', '操作删除', '500', '2', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:operlog:remove',     '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1042', '日志导出', '500', '4', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:operlog:export',     '#', 103, 1, now(), null, null, '');
-- 登录日志按钮
insert into sys_menu values('1043', '登录查询', '501', '1', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:logininfor:query',   '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1044', '登录删除', '501', '2', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:logininfor:remove',  '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1045', '日志导出', '501', '3', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:logininfor:export',  '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1050', '账户解锁', '501', '4', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:logininfor:unlock',  '#', 103, 1, now(), null, null, '');
-- 在线用户按钮
insert into sys_menu values('1046', '在线查询', '109', '1', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:online:query',       '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1047', '批量强退', '109', '2', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:online:batchLogout', '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1048', '单条强退', '109', '3', '#', '', '', '1', '0', 'F', '0', '0', 'monitor:online:forceLogout', '#', 103, 1, now(), null, null, '');
-- 代码生成按钮
insert into sys_menu values('1055', '生成查询', '115', '1', '#', '', '', '1', '0', 'F', '0', '0', 'tool:gen:query',             '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1056', '生成修改', '115', '2', '#', '', '', '1', '0', 'F', '0', '0', 'tool:gen:edit',              '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1057', '生成删除', '115', '3', '#', '', '', '1', '0', 'F', '0', '0', 'tool:gen:remove',            '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1058', '导入代码', '115', '2', '#', '', '', '1', '0', 'F', '0', '0', 'tool:gen:import',            '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1059', '预览代码', '115', '4', '#', '', '', '1', '0', 'F', '0', '0', 'tool:gen:preview',           '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1060', '生成代码', '115', '5', '#', '', '', '1', '0', 'F', '0', '0', 'tool:gen:code',              '#', 103, 1, now(), null, null, '');
-- oss相关按钮
insert into sys_menu values('1600', '文件查询', '118', '1', '#', '', '', '1', '0', 'F', '0', '0', 'system:oss:query',        '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1601', '文件上传', '118', '2', '#', '', '', '1', '0', 'F', '0', '0', 'system:oss:upload',       '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1602', '文件下载', '118', '3', '#', '', '', '1', '0', 'F', '0', '0', 'system:oss:download',     '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1603', '文件删除', '118', '4', '#', '', '', '1', '0', 'F', '0', '0', 'system:oss:remove',       '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1604', '配置添加', '118', '5', '#', '', '', '1', '0', 'F', '0', '0', 'system:oss:add',          '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1605', '配置编辑', '118', '6', '#', '', '', '1', '0', 'F', '0', '0', 'system:oss:edit',         '#', 103, 1, now(), null, null, '');
-- 租户管理相关按钮
insert into sys_menu values('1606', '租户查询', '121', '1', '#', '', '', '1', '0', 'F', '0', '0', 'system:tenant:query',   '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1607', '租户新增', '121', '2', '#', '', '', '1', '0', 'F', '0', '0', 'system:tenant:add',     '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1608', '租户修改', '121', '3', '#', '', '', '1', '0', 'F', '0', '0', 'system:tenant:edit',    '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1609', '租户删除', '121', '4', '#', '', '', '1', '0', 'F', '0', '0', 'system:tenant:remove',  '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1610', '租户导出', '121', '5', '#', '', '', '1', '0', 'F', '0', '0', 'system:tenant:export',  '#', 103, 1, now(), null, null, '');
-- 租户套餐管理相关按钮
insert into sys_menu values('1611', '租户套餐查询', '122', '1', '#', '', '', '1', '0', 'F', '0', '0', 'system:tenantPackage:query',   '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1612', '租户套餐新增', '122', '2', '#', '', '', '1', '0', 'F', '0', '0', 'system:tenantPackage:add',     '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1613', '租户套餐修改', '122', '3', '#', '', '', '1', '0', 'F', '0', '0', 'system:tenantPackage:edit',    '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1614', '租户套餐删除', '122', '4', '#', '', '', '1', '0', 'F', '0', '0', 'system:tenantPackage:remove',  '#', 103, 1, now(), null, null, '');
insert into sys_menu values('1615', '租户套餐导出', '122', '5', '#', '', '', '1', '0', 'F', '0', '0', 'system:tenantPackage:export',  '#', 103, 1, now(), null, null, '');


-- ----------------------------
-- 6、用户和角色关联表  用户N-1角色
-- ----------------------------
drop table if exists sys_user_role;
create table if not exists sys_user_role
(
    user_id int8 not null,
    role_id int8 not null,
    constraint sys_user_role_pk primary key (user_id, role_id)
);

comment on table sys_user_role              is '用户和角色关联表';
comment on column sys_user_role.user_id     is '用户ID';
comment on column sys_user_role.role_id     is '角色ID';

-- ----------------------------
-- 初始化-用户和角色关联表数据
-- ----------------------------
insert into sys_user_role values ('1', '1');
insert into sys_user_role values ('2', '2');


-- ----------------------------
-- 7、角色和菜单关联表  角色1-N菜单
-- ----------------------------
drop table if exists sys_role_menu;
create table if not exists sys_role_menu
(
    role_id int8 not null,
    menu_id int8 not null,
    constraint sys_role_menu_pk primary key (role_id, menu_id)
);

comment on table sys_role_menu              is '角色和菜单关联表';
comment on column sys_role_menu.role_id     is '角色ID';
comment on column sys_role_menu.menu_id     is '菜单ID';

-- ----------------------------
-- 初始化-角色和菜单关联表数据
-- ----------------------------
insert into sys_role_menu values ('2', '1');
insert into sys_role_menu values ('2', '2');
insert into sys_role_menu values ('2', '3');
insert into sys_role_menu values ('2', '4');
insert into sys_role_menu values ('2', '100');
insert into sys_role_menu values ('2', '101');
insert into sys_role_menu values ('2', '102');
insert into sys_role_menu values ('2', '103');
insert into sys_role_menu values ('2', '104');
insert into sys_role_menu values ('2', '105');
insert into sys_role_menu values ('2', '106');
insert into sys_role_menu values ('2', '107');
insert into sys_role_menu values ('2', '108');
insert into sys_role_menu values ('2', '109');
insert into sys_role_menu values ('2', '110');
insert into sys_role_menu values ('2', '111');
insert into sys_role_menu values ('2', '112');
insert into sys_role_menu values ('2', '113');
insert into sys_role_menu values ('2', '114');
insert into sys_role_menu values ('2', '115');
insert into sys_role_menu values ('2', '116');
insert into sys_role_menu values ('2', '500');
insert into sys_role_menu values ('2', '501');
insert into sys_role_menu values ('2', '1000');
insert into sys_role_menu values ('2', '1001');
insert into sys_role_menu values ('2', '1002');
insert into sys_role_menu values ('2', '1003');
insert into sys_role_menu values ('2', '1004');
insert into sys_role_menu values ('2', '1005');
insert into sys_role_menu values ('2', '1006');
insert into sys_role_menu values ('2', '1007');
insert into sys_role_menu values ('2', '1008');
insert into sys_role_menu values ('2', '1009');
insert into sys_role_menu values ('2', '1010');
insert into sys_role_menu values ('2', '1011');
insert into sys_role_menu values ('2', '1012');
insert into sys_role_menu values ('2', '1013');
insert into sys_role_menu values ('2', '1014');
insert into sys_role_menu values ('2', '1015');
insert into sys_role_menu values ('2', '1016');
insert into sys_role_menu values ('2', '1017');
insert into sys_role_menu values ('2', '1018');
insert into sys_role_menu values ('2', '1019');
insert into sys_role_menu values ('2', '1020');
insert into sys_role_menu values ('2', '1021');
insert into sys_role_menu values ('2', '1022');
insert into sys_role_menu values ('2', '1023');
insert into sys_role_menu values ('2', '1024');
insert into sys_role_menu values ('2', '1025');
insert into sys_role_menu values ('2', '1026');
insert into sys_role_menu values ('2', '1027');
insert into sys_role_menu values ('2', '1028');
insert into sys_role_menu values ('2', '1029');
insert into sys_role_menu values ('2', '1030');
insert into sys_role_menu values ('2', '1031');
insert into sys_role_menu values ('2', '1032');
insert into sys_role_menu values ('2', '1033');
insert into sys_role_menu values ('2', '1034');
insert into sys_role_menu values ('2', '1035');
insert into sys_role_menu values ('2', '1036');
insert into sys_role_menu values ('2', '1037');
insert into sys_role_menu values ('2', '1038');
insert into sys_role_menu values ('2', '1039');
insert into sys_role_menu values ('2', '1040');
insert into sys_role_menu values ('2', '1041');
insert into sys_role_menu values ('2', '1042');
insert into sys_role_menu values ('2', '1043');
insert into sys_role_menu values ('2', '1044');
insert into sys_role_menu values ('2', '1045');
insert into sys_role_menu values ('2', '1050');
insert into sys_role_menu values ('2', '1046');
insert into sys_role_menu values ('2', '1047');
insert into sys_role_menu values ('2', '1048');
insert into sys_role_menu values ('2', '1055');
insert into sys_role_menu values ('2', '1056');
insert into sys_role_menu values ('2', '1057');
insert into sys_role_menu values ('2', '1058');
insert into sys_role_menu values ('2', '1059');
insert into sys_role_menu values ('2', '1060');

-- ----------------------------
-- 8、角色和部门关联表  角色1-N部门
-- ----------------------------
drop table if exists sys_role_dept;
create table if not exists sys_role_dept
(
    role_id int8 not null,
    dept_id int8 not null,
    constraint sys_role_dept_pk primary key (role_id, dept_id)
);

comment on table sys_role_dept              is '角色和部门关联表';
comment on column sys_role_dept.role_id     is '角色ID';
comment on column sys_role_dept.dept_id     is '部门ID';

-- ----------------------------
-- 初始化-角色和部门关联表数据
-- ----------------------------
insert into sys_role_dept values ('2', '100');
insert into sys_role_dept values ('2', '101');
insert into sys_role_dept values ('2', '105');


-- ----------------------------
-- 9、用户与岗位关联表  用户1-N岗位
-- ----------------------------
drop table if exists sys_user_post;
create table if not exists sys_user_post
(
    user_id int8 not null,
    post_id int8 not null,
    constraint sys_user_post_pk primary key (user_id, post_id)
);

comment on table sys_user_post              is '用户与岗位关联表';
comment on column sys_user_post.user_id     is '用户ID';
comment on column sys_user_post.post_id     is '岗位ID';

-- ----------------------------
-- 初始化-用户与岗位关联表数据
-- ----------------------------
insert into sys_user_post values ('1', '1');
insert into sys_user_post values ('2', '2');


-- ----------------------------
-- 10、操作日志记录
-- ----------------------------
drop table if exists sys_oper_log;
create table if not exists sys_oper_log
(
    oper_id        int8,
    tenant_id      varchar(20)   default '000000'::varchar,
    title          varchar(50)   default ''::varchar,
    business_type  int4          default 0,
    method         varchar(100)  default ''::varchar,
    request_method varchar(10)   default ''::varchar,
    operator_type  int4          default 0,
    oper_name      varchar(50)   default ''::varchar,
    dept_name      varchar(50)   default ''::varchar,
    oper_url       varchar(255)  default ''::varchar,
    oper_ip        varchar(128)  default ''::varchar,
    oper_location  varchar(255)  default ''::varchar,
    oper_param     varchar(2000) default ''::varchar,
    json_result    varchar(2000) default ''::varchar,
    status         int4          default 0,
    error_msg      varchar(2000) default ''::varchar,
    oper_time      timestamp,
    cost_time      int8          default 0,
    constraint sys_oper_log_pk primary key (oper_id)
);

create index idx_sys_oper_log_bt ON sys_oper_log (business_type);
create index idx_sys_oper_log_s ON sys_oper_log (status);
create index idx_sys_oper_log_ot ON sys_oper_log (oper_time);

comment on table sys_oper_log                   is '操作日志记录';
comment on column sys_oper_log.oper_id          is '日志主键';
comment on column sys_oper_log.tenant_id        is '租户编号';
comment on column sys_oper_log.title            is '模块标题';
comment on column sys_oper_log.business_type    is '业务类型（0其它 1新增 2修改 3删除）';
comment on column sys_oper_log.method           is '方法名称';
comment on column sys_oper_log.request_method   is '请求方式';
comment on column sys_oper_log.operator_type    is '操作类别（0其它 1后台用户 2手机端用户）';
comment on column sys_oper_log.oper_name        is '操作人员';
comment on column sys_oper_log.dept_name        is '部门名称';
comment on column sys_oper_log.oper_url         is '请求URL';
comment on column sys_oper_log.oper_ip          is '主机地址';
comment on column sys_oper_log.oper_location    is '操作地点';
comment on column sys_oper_log.oper_param       is '请求参数';
comment on column sys_oper_log.json_result      is '返回参数';
comment on column sys_oper_log.status           is '操作状态（0正常 1异常）';
comment on column sys_oper_log.error_msg        is '错误消息';
comment on column sys_oper_log.oper_time        is '操作时间';
comment on column sys_oper_log.cost_time        is '消耗时间';

-- ----------------------------
-- 11、字典类型表
-- ----------------------------
drop table if exists sys_dict_type;
create table if not exists sys_dict_type
(
    dict_id     int8,
    tenant_id   varchar(20)  default '000000'::varchar,
    dict_name   varchar(100) default ''::varchar,
    dict_type   varchar(100) default ''::varchar,
    status      char         default '0'::bpchar,
    create_dept int8,
    create_by   int8,
    create_time timestamp,
    update_by   int8,
    update_time timestamp,
    remark      varchar(500) default null::varchar,
    constraint sys_dict_type_pk primary key (dict_id)
);

create unique index sys_dict_type_index1 ON sys_dict_type (tenant_id, dict_type);

comment on table sys_dict_type                  is '字典类型表';
comment on column sys_dict_type.dict_id         is '字典主键';
comment on column sys_dict_type.tenant_id       is '租户编号';
comment on column sys_dict_type.dict_name       is '字典名称';
comment on column sys_dict_type.dict_type       is '字典类型';
comment on column sys_dict_type.status          is '状态（0正常 1停用）';
comment on column sys_dict_type.create_dept     is '创建部门';
comment on column sys_dict_type.create_by       is '创建者';
comment on column sys_dict_type.create_time     is '创建时间';
comment on column sys_dict_type.update_by       is '更新者';
comment on column sys_dict_type.update_time     is '更新时间';
comment on column sys_dict_type.remark          is '备注';

insert into sys_dict_type values(1, '000000', '用户性别', 'sys_user_sex',        '0', 103, 1, now(), null, null, '用户性别列表');
insert into sys_dict_type values(2, '000000', '菜单状态', 'sys_show_hide',       '0', 103, 1, now(), null, null, '菜单状态列表');
insert into sys_dict_type values(3, '000000', '系统开关', 'sys_normal_disable',  '0', 103, 1, now(), null, null, '系统开关列表');
insert into sys_dict_type values(6, '000000', '系统是否', 'sys_yes_no',          '0', 103, 1, now(), null, null, '系统是否列表');
insert into sys_dict_type values(7, '000000', '通知类型', 'sys_notice_type',     '0', 103, 1, now(), null, null, '通知类型列表');
insert into sys_dict_type values(8, '000000', '通知状态', 'sys_notice_status',   '0', 103, 1, now(), null, null, '通知状态列表');
insert into sys_dict_type values(9, '000000', '操作类型', 'sys_oper_type',       '0', 103, 1, now(), null, null, '操作类型列表');
insert into sys_dict_type values(10, '000000', '系统状态', 'sys_common_status',   '0', 103, 1, now(), null, null, '登录状态列表');


-- ----------------------------
-- 12、字典数据表
-- ----------------------------
drop table if exists sys_dict_data;
create table if not exists sys_dict_data
(
    dict_code   int8,
    tenant_id   varchar(20)  default '000000'::varchar,
    dict_sort   int4         default 0,
    dict_label  varchar(100) default ''::varchar,
    dict_value  varchar(100) default ''::varchar,
    dict_type   varchar(100) default ''::varchar,
    css_class   varchar(100) default null::varchar,
    list_class  varchar(100) default null::varchar,
    is_default  char         default 'N'::bpchar,
    status      char         default '0'::bpchar,
    create_dept int8,
    create_by   int8,
    create_time timestamp,
    update_by   int8,
    update_time timestamp,
    remark      varchar(500) default null::varchar,
    constraint sys_dict_data_pk primary key (dict_code)
);

comment on table sys_dict_data                  is '字典数据表';
comment on column sys_dict_data.dict_code       is '字典编码';
comment on column sys_dict_type.tenant_id       is '租户编号';
comment on column sys_dict_data.dict_sort       is '字典排序';
comment on column sys_dict_data.dict_label      is '字典标签';
comment on column sys_dict_data.dict_value      is '字典键值';
comment on column sys_dict_data.dict_type       is '字典类型';
comment on column sys_dict_data.css_class       is '样式属性（其他样式扩展）';
comment on column sys_dict_data.list_class      is '表格回显样式';
comment on column sys_dict_data.is_default      is '是否默认（Y是 N否）';
comment on column sys_dict_data.status          is '状态（0正常 1停用）';
comment on column sys_dict_data.create_dept     is '创建部门';
comment on column sys_dict_data.create_by       is '创建者';
comment on column sys_dict_data.create_time     is '创建时间';
comment on column sys_dict_data.update_by       is '更新者';
comment on column sys_dict_data.update_time     is '更新时间';
comment on column sys_dict_data.remark          is '备注';

insert into sys_dict_data values(1, '000000', 1,  '男',       '0',       'sys_user_sex',        '',   '',        'Y', '0', 103, 1, now(), null, null, '性别男');
insert into sys_dict_data values(2, '000000', 2,  '女',       '1',       'sys_user_sex',        '',   '',        'N', '0', 103, 1, now(), null, null, '性别女');
insert into sys_dict_data values(3, '000000', 3,  '未知',     '2',       'sys_user_sex',        '',   '',        'N', '0', 103, 1, now(), null, null, '性别未知');
insert into sys_dict_data values(4, '000000', 1,  '显示',     '0',       'sys_show_hide',       '',   'primary', 'Y', '0', 103, 1, now(), null, null, '显示菜单');
insert into sys_dict_data values(5, '000000', 2,  '隐藏',     '1',       'sys_show_hide',       '',   'danger',  'N', '0', 103, 1, now(), null, null, '隐藏菜单');
insert into sys_dict_data values(6, '000000', 1,  '正常',     '0',       'sys_normal_disable',  '',   'primary', 'Y', '0', 103, 1, now(), null, null, '正常状态');
insert into sys_dict_data values(7, '000000', 2,  '停用',     '1',       'sys_normal_disable',  '',   'danger',  'N', '0', 103, 1, now(), null, null, '停用状态');
insert into sys_dict_data values(12, '000000', 1,  '是',       'Y',       'sys_yes_no',          '',   'primary', 'Y', '0', 103, 1, now(), null, null, '系统默认是');
insert into sys_dict_data values(13, '000000', 2,  '否',       'N',       'sys_yes_no',          '',   'danger',  'N', '0', 103, 1, now(), null, null, '系统默认否');
insert into sys_dict_data values(14, '000000', 1,  '通知',     '1',       'sys_notice_type',     '',   'warning', 'Y', '0', 103, 1, now(), null, null, '通知');
insert into sys_dict_data values(15, '000000', 2,  '公告',     '2',       'sys_notice_type',     '',   'success', 'N', '0', 103, 1, now(), null, null, '公告');
insert into sys_dict_data values(16, '000000', 1,  '正常',     '0',       'sys_notice_status',   '',   'primary', 'Y', '0', 103, 1, now(), null, null, '正常状态');
insert into sys_dict_data values(17, '000000', 2,  '关闭',     '1',       'sys_notice_status',   '',   'danger',  'N', '0', 103, 1, now(), null, null, '关闭状态');
insert into sys_dict_data values(29, '000000', 99, '其他',     '0',       'sys_oper_type',       '',   'info',    'N', '0', 103, 1, now(), null, null, '其他操作');
insert into sys_dict_data values(18, '000000', 1,  '新增',     '1',       'sys_oper_type',       '',   'info',    'N', '0', 103, 1, now(), null, null, '新增操作');
insert into sys_dict_data values(19, '000000', 2,  '修改',     '2',       'sys_oper_type',       '',   'info',    'N', '0', 103, 1, now(), null, null, '修改操作');
insert into sys_dict_data values(20, '000000', 3,  '删除',     '3',       'sys_oper_type',       '',   'danger',  'N', '0', 103, 1, now(), null, null, '删除操作');
insert into sys_dict_data values(21, '000000', 4,  '授权',     '4',       'sys_oper_type',       '',   'primary', 'N', '0', 103, 1, now(), null, null, '授权操作');
insert into sys_dict_data values(22, '000000', 5,  '导出',     '5',       'sys_oper_type',       '',   'warning', 'N', '0', 103, 1, now(), null, null, '导出操作');
insert into sys_dict_data values(23, '000000', 6,  '导入',     '6',       'sys_oper_type',       '',   'warning', 'N', '0', 103, 1, now(), null, null, '导入操作');
insert into sys_dict_data values(24, '000000', 7,  '强退',     '7',       'sys_oper_type',       '',   'danger',  'N', '0', 103, 1, now(), null, null, '强退操作');
insert into sys_dict_data values(25, '000000', 8,  '生成代码', '8',       'sys_oper_type',       '',   'warning', 'N', '0', 103, 1, now(), null, null, '生成操作');
insert into sys_dict_data values(26, '000000', 9,  '清空数据', '9',       'sys_oper_type',       '',   'danger',  'N', '0', 103, 1, now(), null, null, '清空操作');
insert into sys_dict_data values(27, '000000', 1,  '成功',     '0',       'sys_common_status',   '',   'primary', 'N', '0', 103, 1, now(), null, null, '正常状态');
insert into sys_dict_data values(28, '000000', 2,  '失败',     '1',       'sys_common_status',   '',   'danger',  'N', '0', 103, 1, now(), null, null, '停用状态');


-- ----------------------------
-- 13、参数配置表
-- ----------------------------
drop table if exists sys_config;
create table if not exists sys_config
(
    config_id    int8,
    tenant_id    varchar(20)  default '000000'::varchar,
    config_name  varchar(100) default ''::varchar,
    config_key   varchar(100) default ''::varchar,
    config_value varchar(500) default ''::varchar,
    config_type  char         default 'N'::bpchar,
    create_dept  int8,
    create_by    int8,
    create_time  timestamp,
    update_by    int8,
    update_time  timestamp,
    remark       varchar(500) default null::varchar,
    constraint sys_config_pk primary key (config_id)
);

comment on table sys_config                 is '参数配置表';
comment on column sys_config.config_id      is '参数主键';
comment on column sys_config.tenant_id      is '租户编号';
comment on column sys_config.config_name    is '参数名称';
comment on column sys_config.config_key     is '参数键名';
comment on column sys_config.config_value   is '参数键值';
comment on column sys_config.config_type    is '系统内置（Y是 N否）';
comment on column sys_config.create_dept    is '创建部门';
comment on column sys_config.create_by      is '创建者';
comment on column sys_config.create_time    is '创建时间';
comment on column sys_config.update_by      is '更新者';
comment on column sys_config.update_time    is '更新时间';
comment on column sys_config.remark         is '备注';

insert into sys_config values(1, '000000', '主框架页-默认皮肤样式名称',     'sys.index.skinName',            'skin-blue',     'Y', 103, 1, now(), null, null, '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow' );
insert into sys_config values(2, '000000', '用户管理-账号初始密码',         'sys.user.initPassword',         '123456',        'Y', 103, 1, now(), null, null, '初始化密码 123456' );
insert into sys_config values(3, '000000', '主框架页-侧边栏主题',           'sys.index.sideTheme',           'theme-dark',    'Y', 103, 1, now(), null, null, '深色主题theme-dark，浅色主题theme-light' );
insert into sys_config values(5, '000000', '账号自助-是否开启用户注册功能',   'sys.account.registerUser',      'false',         'Y', 103, 1, now(), null, null, '是否开启注册用户功能（true开启，false关闭）');
insert into sys_config values(11, '000000', 'OSS预览列表资源开关',          'sys.oss.previewListResource',   'true',          'Y', 103, 1, now(), null, null, 'true:开启, false:关闭');


-- ----------------------------
-- 14、系统访问记录
-- ----------------------------
drop table if exists sys_logininfor;
create table if not exists sys_logininfor
(
    info_id        int8,
    tenant_id      varchar(20)  default '000000'::varchar,
    user_name      varchar(50)  default ''::varchar,
    ipaddr         varchar(128) default ''::varchar,
    login_location varchar(255) default ''::varchar,
    browser        varchar(50)  default ''::varchar,
    os             varchar(50)  default ''::varchar,
    status         char         default '0'::bpchar,
    msg            varchar(255) default ''::varchar,
    login_time     timestamp,
    constraint sys_logininfor_pk primary key (info_id)
);

create index idx_sys_logininfor_s ON sys_logininfor (status);
create index idx_sys_logininfor_lt ON sys_logininfor (login_time);

comment on table sys_logininfor                 is '系统访问记录';
comment on column sys_logininfor.info_id        is '访问ID';
comment on column sys_logininfor.tenant_id      is '租户编号';
comment on column sys_logininfor.user_name      is '用户账号';
comment on column sys_logininfor.ipaddr         is '登录IP地址';
comment on column sys_logininfor.login_location is '登录地点';
comment on column sys_logininfor.browser        is '浏览器类型';
comment on column sys_logininfor.os             is '操作系统';
comment on column sys_logininfor.status         is '登录状态（0成功 1失败）';
comment on column sys_logininfor.msg            is '提示消息';
comment on column sys_logininfor.login_time     is '访问时间';

-- ----------------------------
-- 17、通知公告表
-- ----------------------------
drop table if exists sys_notice;
create table if not exists sys_notice
(
    notice_id      int8,
    tenant_id      varchar(20)  default '000000'::varchar,
    notice_title   varchar(50)  not null,
    notice_type    char         not null,
    notice_content text,
    status         char         default '0'::bpchar,
    create_dept    int8,
    create_by      int8,
    create_time    timestamp,
    update_by      int8,
    update_time    timestamp,
    remark         varchar(255) default null::varchar,
    constraint sys_notice_pk primary key (notice_id)
);

comment on table sys_notice                 is '通知公告表';
comment on column sys_notice.notice_id      is '公告ID';
comment on column sys_notice.tenant_id      is '租户编号';
comment on column sys_notice.notice_title   is '公告标题';
comment on column sys_notice.notice_type    is '公告类型（1通知 2公告）';
comment on column sys_notice.notice_content is '公告内容';
comment on column sys_notice.status         is '公告状态（0正常 1关闭）';
comment on column sys_notice.create_dept    is '创建部门';
comment on column sys_notice.create_by      is '创建者';
comment on column sys_notice.create_time    is '创建时间';
comment on column sys_notice.update_by      is '更新者';
comment on column sys_notice.update_time    is '更新时间';
comment on column sys_notice.remark         is '备注';

-- ----------------------------
-- 初始化-公告信息表数据
-- ----------------------------
insert into sys_notice values('1', '000000', '温馨提醒：2018-07-01 新版本发布啦', '2', '新版本内容', '0', 103, 1, now(), null, null, '管理员');
insert into sys_notice values('2', '000000', '维护通知：2018-07-01 系统凌晨维护', '1', '维护内容',   '0', 103, 1, now(), null, null, '管理员');


-- ----------------------------
-- 18、代码生成业务表
-- ----------------------------
drop table if exists gen_table;
create table if not exists gen_table
(
    table_id          int8,
    table_name        varchar(200)  default ''::varchar,
    table_comment     varchar(500)  default ''::varchar,
    sub_table_name    varchar(64)   default ''::varchar,
    sub_table_fk_name varchar(64)   default ''::varchar,
    class_name        varchar(100)  default ''::varchar,
    tpl_category      varchar(200)  default 'crud'::varchar,
    package_name      varchar(100)  default null::varchar,
    module_name       varchar(30)   default null::varchar,
    business_name     varchar(30)   default null::varchar,
    function_name     varchar(50)   default null::varchar,
    function_author   varchar(50)   default null::varchar,
    gen_type          char          default '0'::bpchar not null,
    gen_path          varchar(200)  default '/'::varchar,
    options           varchar(1000) default null::varchar,
    create_dept       int8,
    create_by         int8,
    create_time       timestamp,
    update_by         int8,
    update_time       timestamp,
    remark            varchar(500)  default null::varchar,
    constraint gen_table_pk primary key (table_id)
);

comment on table gen_table is '代码生成业务表';
comment on column gen_table.table_id is '编号';
comment on column gen_table.table_name is '表名称';
comment on column gen_table.table_comment is '表描述';
comment on column gen_table.sub_table_name is '关联子表的表名';
comment on column gen_table.sub_table_fk_name is '子表关联的外键名';
comment on column gen_table.class_name is '实体类名称';
comment on column gen_table.tpl_category is '使用的模板（CRUD单表操作 TREE树表操作）';
comment on column gen_table.package_name is '生成包路径';
comment on column gen_table.module_name is '生成模块名';
comment on column gen_table.business_name is '生成业务名';
comment on column gen_table.function_name is '生成功能名';
comment on column gen_table.function_author is '生成功能作者';
comment on column gen_table.gen_type is '生成代码方式（0zip压缩包 1自定义路径）';
comment on column gen_table.gen_path is '生成路径（不填默认项目路径）';
comment on column gen_table.options is '其它生成选项';
comment on column gen_table.create_dept is '创建部门';
comment on column gen_table.create_by is '创建者';
comment on column gen_table.create_time is '创建时间';
comment on column gen_table.update_by is '更新者';
comment on column gen_table.update_time is '更新时间';
comment on column gen_table.remark is '备注';

-- ----------------------------
-- 19、代码生成业务表字段
-- ----------------------------
drop table if exists gen_table_column;
create table if not exists gen_table_column
(
    column_id      int8,
    table_id       int8,
    column_name    varchar(200) default null::varchar,
    column_comment varchar(500) default null::varchar,
    column_type    varchar(100) default null::varchar,
    java_type      varchar(500) default null::varchar,
    java_field     varchar(200) default null::varchar,
    is_pk          char         default null::bpchar,
    is_increment   char         default null::bpchar,
    is_required    char         default null::bpchar,
    is_insert      char         default null::bpchar,
    is_edit        char         default null::bpchar,
    is_list        char         default null::bpchar,
    is_query       char         default null::bpchar,
    query_type     varchar(200) default 'EQ'::varchar,
    html_type      varchar(200) default null::varchar,
    dict_type      varchar(200) default ''::varchar,
    sort           int4,
    create_dept    int8,
    create_by      int8,
    create_time    timestamp,
    update_by      int8,
    update_time    timestamp,
    constraint gen_table_column_pk primary key (column_id)
);

comment on table gen_table_column is '代码生成业务表字段';
comment on column gen_table_column.column_id is '编号';
comment on column gen_table_column.table_id is '归属表编号';
comment on column gen_table_column.column_name is '列名称';
comment on column gen_table_column.column_comment is '列描述';
comment on column gen_table_column.column_type is '列类型';
comment on column gen_table_column.java_type is 'JAVA类型';
comment on column gen_table_column.java_field is 'JAVA字段名';
comment on column gen_table_column.is_pk is '是否主键（1是）';
comment on column gen_table_column.is_increment is '是否自增（1是）';
comment on column gen_table_column.is_required is '是否必填（1是）';
comment on column gen_table_column.is_insert is '是否为插入字段（1是）';
comment on column gen_table_column.is_edit is '是否编辑字段（1是）';
comment on column gen_table_column.is_list is '是否列表字段（1是）';
comment on column gen_table_column.is_query is '是否查询字段（1是）';
comment on column gen_table_column.query_type is '查询方式（等于、不等于、大于、小于、范围）';
comment on column gen_table_column.html_type is '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）';
comment on column gen_table_column.dict_type is '字典类型';
comment on column gen_table_column.sort is '排序';
comment on column gen_table_column.create_dept is '创建部门';
comment on column gen_table_column.create_by is '创建者';
comment on column gen_table_column.create_time is '创建时间';
comment on column gen_table_column.update_by is '更新者';
comment on column gen_table_column.update_time is '更新时间';

-- ----------------------------
-- OSS对象存储表
-- ----------------------------
drop table if exists sys_oss;
create table if not exists sys_oss
(
    oss_id        int8,
    tenant_id     varchar(20)  default '000000'::varchar,
    file_name     varchar(255) default ''::varchar not null,
    original_name varchar(255) default ''::varchar not null,
    file_suffix   varchar(10)  default ''::varchar not null,
    url           varchar(500) default ''::varchar not null,
    create_dept   int8,
    create_by     int8,
    create_time   timestamp,
    update_by     int8,
    update_time   timestamp,
    service       varchar(20)  default 'minio'::varchar,
    constraint sys_oss_pk primary key (oss_id)
);

comment on table sys_oss                    is 'OSS对象存储表';
comment on column sys_oss.oss_id            is '对象存储主键';
comment on column sys_oss.tenant_id         is '租户编码';
comment on column sys_oss.file_name         is '文件名';
comment on column sys_oss.original_name     is '原名';
comment on column sys_oss.file_suffix       is '文件后缀名';
comment on column sys_oss.url               is 'URL地址';
comment on column sys_oss.create_by         is '上传人';
comment on column sys_oss.create_dept       is '创建部门';
comment on column sys_oss.create_time       is '创建时间';
comment on column sys_oss.update_by         is '更新者';
comment on column sys_oss.update_time       is '更新时间';
comment on column sys_oss.service           is '服务商';

-- ----------------------------
-- OSS对象存储动态配置表
-- ----------------------------
drop table if exists sys_oss_config;
create table if not exists sys_oss_config
(
    oss_config_id int8,
    tenant_id     varchar(20)  default '000000'::varchar,
    config_key    varchar(20)  default ''::varchar not null,
    access_key    varchar(255) default ''::varchar,
    secret_key    varchar(255) default ''::varchar,
    bucket_name   varchar(255) default ''::varchar,
    prefix        varchar(255) default ''::varchar,
    endpoint      varchar(255) default ''::varchar,
    domain        varchar(255) default ''::varchar,
    is_https      char         default 'N'::bpchar,
    region        varchar(255) default ''::varchar,
    access_policy char(1)      default '1'::bpchar not null,
    status        char         default '1'::bpchar,
    ext1          varchar(255) default ''::varchar,
    create_dept   int8,
    create_by     int8,
    create_time   timestamp,
    update_by     int8,
    update_time   timestamp,
    remark        varchar(500) default ''::varchar,
    constraint sys_oss_config_pk primary key (oss_config_id)
);

comment on table sys_oss_config                 is '对象存储配置表';
comment on column sys_oss_config.oss_config_id  is '主建';
comment on column sys_oss_config.tenant_id      is '租户编码';
comment on column sys_oss_config.config_key     is '配置key';
comment on column sys_oss_config.access_key     is 'accessKey';
comment on column sys_oss_config.secret_key     is '秘钥';
comment on column sys_oss_config.bucket_name    is '桶名称';
comment on column sys_oss_config.prefix         is '前缀';
comment on column sys_oss_config.endpoint       is '访问站点';
comment on column sys_oss_config.domain         is '自定义域名';
comment on column sys_oss_config.is_https       is '是否https（Y=是,N=否）';
comment on column sys_oss_config.region         is '域';
comment on column sys_oss_config.access_policy  is '桶权限类型(0=private 1=public 2=custom)';
comment on column sys_oss_config.status         is '是否默认（0=是,1=否）';
comment on column sys_oss_config.ext1           is '扩展字段';
comment on column sys_oss_config.create_dept    is '创建部门';
comment on column sys_oss_config.create_by      is '创建者';
comment on column sys_oss_config.create_time    is '创建时间';
comment on column sys_oss_config.update_by      is '更新者';
comment on column sys_oss_config.update_time    is '更新时间';
comment on column sys_oss_config.remark         is '备注';

insert into sys_oss_config values (1, '000000', 'minio',  'ruoyi',            'ruoyi123',        'ruoyi',             '', '127.0.0.1:9000',                      '','N', '',            '1', '0', '', 103, 1, now(), 1, now(), null);
insert into sys_oss_config values (2, '000000', 'qiniu',  'XXXXXXXXXXXXXXX',  'XXXXXXXXXXXXXXX', 'ruoyi',             '', 's3-cn-north-1.qiniucs.com',           '','N', '',            '1', '1', '', 103, 1, now(), 1, now(), null);
insert into sys_oss_config values (3, '000000', 'aliyun', 'XXXXXXXXXXXXXXX',  'XXXXXXXXXXXXXXX', 'ruoyi',             '', 'oss-cn-beijing.aliyuncs.com',         '','N', '',            '1', '1', '', 103, 1, now(), 1, now(), null);
insert into sys_oss_config values (4, '000000', 'qcloud', 'XXXXXXXXXXXXXXX',  'XXXXXXXXXXXXXXX', 'ruoyi-1250000000',  '', 'cos.ap-beijing.myqcloud.com',         '','N', 'ap-beijing',  '1', '1', '', 103, 1, now(), 1, now(), null);
insert into sys_oss_config values (5, '000000', 'image',  'ruoyi',            'ruoyi123',        'ruoyi',             'image', '127.0.0.1:9000',                 '','N', '',            '1', '1', '', 103, 1, now(), 1, now(), NULL);

-- 字符串自动转时间 避免框架时间查询报错问题
create or replace function cast_varchar_to_timestamp(varchar) returns timestamptz as $$
select to_timestamp($1, 'yyyy-mm-dd hh24:mi:ss');
$$ language sql strict ;

create cast (varchar as timestamptz) with function cast_varchar_to_timestamp as IMPLICIT;
