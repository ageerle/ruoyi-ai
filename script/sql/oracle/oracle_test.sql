create table test_demo (
    id          number(20)      not null,
    tenant_id   varchar2(20)    default '000000',
    dept_id     number(20)      default null,
    user_id     number(20)      default null,
    order_num   number(10)      default 0,
    test_key    varchar2(255)   default null,
    value       varchar2(255)   default null,
    version     number(10)      default 0,
    create_dept number(20)      default null,
    create_time date,
    create_by   number(20)      default null,
    update_time date,
    update_by   number(20)      default null,
    del_flag    number(2)       default 0
);

alter table test_demo add constraint pk_test_demo primary key (id);

comment on table  test_demo              is '测试单表';
comment on column test_demo.id           is '主键';
comment on column test_demo.tenant_id    is '租户编号';
comment on column test_demo.dept_id      is '部门id';
comment on column test_demo.user_id      is '用户id';
comment on column test_demo.order_num    is '排序号';
comment on column test_demo.test_key     is 'key键';
comment on column test_demo.value        is '值';
comment on column test_demo.version      is '版本';
comment on column test_demo.create_dept  is '创建部门';
comment on column test_demo.create_time  is '创建时间';
comment on column test_demo.create_by    is '创建人';
comment on column test_demo.update_time  is '更新时间';
comment on column test_demo.update_by    is '更新人';
comment on column test_demo.del_flag     is '删除标志';

create table test_tree (
    id          number(20)      not null,
    tenant_id   varchar2(20)    default '000000',
    parent_id   number(20)      default 0,
    dept_id     number(20)      default null,
    user_id     number(20)      default null,
    tree_name   varchar2(255)   default null,
    version     number(10)      default 0,
    create_dept number(20)      default null,
    create_time date,
    create_by   number(20)      default null,
    update_time date,
    update_by   number(20)      default null,
    del_flag    number(2)       default 0
);

alter table test_tree add constraint pk_test_tree primary key (id);

comment on table  test_tree              is '测试树表';
comment on column test_tree.id           is '主键';
comment on column test_tree.tenant_id    is '租户编号';
comment on column test_tree.parent_id    is '父id';
comment on column test_tree.dept_id      is '部门id';
comment on column test_tree.user_id      is '用户id';
comment on column test_tree.tree_name    is '值';
comment on column test_tree.version      is '版本';
comment on column test_tree.create_dept  is '创建部门';
comment on column test_tree.create_time  is '创建时间';
comment on column test_tree.create_by    is '创建人';
comment on column test_tree.update_time  is '更新时间';
comment on column test_tree.update_by    is '更新人';
comment on column test_tree.del_flag     is '删除标志';

insert into sys_user(user_id, tenant_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, create_dept, create_by, create_time, update_by, update_time, remark) values (3, '000000', 108, 'test', '本部门及以下 密码666666', 'sys_user', '', '', '0', null, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', '127.0.0.1', sysdate, 103, 1, sysdate, 3, sysdate, null);
insert into sys_user(user_id, tenant_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, create_dept, create_by, create_time, update_by, update_time, remark) values (4, '000000', 102, 'test1', '仅本人 密码666666', 'sys_user', '', '', '0', null, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', '127.0.0.1', sysdate, 103, 1, sysdate, 4, sysdate, null);

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) values (5, '测试菜单', 0, 5, 'demo', null, 1, 0, 'M', '0', '0', null, 'star', 103, 1, sysdate, 1, sysdate, '');

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) values (1500, '测试单表', 5, 1, 'demo', 'demo/demo/index', 1, 0, 'C', '0', '0', 'demo:demo:list', '#', 103, 1, sysdate, null, null, '测试单表菜单');
insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) values (1501, '测试单表查询', 1500, 1, '#', '', 1, 0, 'F', '0', '0', 'demo:demo:query', '#', 103, 1, sysdate, null, null, '');
insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) values (1502, '测试单表新增', 1500, 2, '#', '', 1, 0, 'F', '0', '0', 'demo:demo:add', '#', 103, 1, sysdate, null, null, '');
insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) values (1503, '测试单表修改', 1500, 3, '#', '', 1, 0, 'F', '0', '0', 'demo:demo:edit', '#', 103, 1, sysdate, null, null, '');
insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) values (1504, '测试单表删除', 1500, 4, '#', '', 1, 0, 'F', '0', '0', 'demo:demo:remove', '#', 103, 1, sysdate, null, null, '');
insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) values (1505, '测试单表导出', 1500, 5, '#', '', 1, 0, 'F', '0', '0', 'demo:demo:export', '#', 103, 1, sysdate, null, null, '');

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) values (1506, '测试树表', 5, 1, 'tree', 'demo/tree/index', 1, 0, 'C', '0', '0', 'demo:tree:list', '#', 103, 1, sysdate, null, null, '测试树表菜单');
insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) values (1507, '测试树表查询', 1506, 1, '#', '', 1, 0, 'F', '0', '0', 'demo:tree:query', '#', 103, 1, sysdate, null, null, '');
insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) values (1508, '测试树表新增', 1506, 2, '#', '', 1, 0, 'F', '0', '0', 'demo:tree:add', '#', 103, 1, sysdate, null, null, '');
insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) values (1509, '测试树表修改', 1506, 3, '#', '', 1, 0, 'F', '0', '0', 'demo:tree:edit', '#', 103, 1, sysdate, null, null, '');
insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) values (1510, '测试树表删除', 1506, 4, '#', '', 1, 0, 'F', '0', '0', 'demo:tree:remove', '#', 103, 1, sysdate, null, null, '');
insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) values (1511, '测试树表导出', 1506, 5, '#', '', 1, 0, 'F', '0', '0', 'demo:tree:export', '#', 103, 1, sysdate, null, null, '');

insert into sys_role(role_id, tenant_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_dept, create_by, create_time, update_by, update_time, remark) values (3, '000000', '本部门及以下', 'test1', 3, '4', 1, 1, '0', '0', 103, 1, sysdate, null, null, null);
insert into sys_role(role_id, tenant_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_dept, create_by, create_time, update_by, update_time, remark) values (4, '000000', '仅本人',      'test2', 4, '5', 1, 1, '0', '0', 103, 1, sysdate, null, null, null);

insert into sys_role_menu(role_id, menu_id) values (3, 1);
insert into sys_role_menu(role_id, menu_id) values (3, 5);
insert into sys_role_menu(role_id, menu_id) values (3, 100);
insert into sys_role_menu(role_id, menu_id) values (3, 101);
insert into sys_role_menu(role_id, menu_id) values (3, 102);
insert into sys_role_menu(role_id, menu_id) values (3, 103);
insert into sys_role_menu(role_id, menu_id) values (3, 104);
insert into sys_role_menu(role_id, menu_id) values (3, 105);
insert into sys_role_menu(role_id, menu_id) values (3, 106);
insert into sys_role_menu(role_id, menu_id) values (3, 107);
insert into sys_role_menu(role_id, menu_id) values (3, 108);
insert into sys_role_menu(role_id, menu_id) values (3, 500);
insert into sys_role_menu(role_id, menu_id) values (3, 501);
insert into sys_role_menu(role_id, menu_id) values (3, 1001);
insert into sys_role_menu(role_id, menu_id) values (3, 1002);
insert into sys_role_menu(role_id, menu_id) values (3, 1003);
insert into sys_role_menu(role_id, menu_id) values (3, 1004);
insert into sys_role_menu(role_id, menu_id) values (3, 1005);
insert into sys_role_menu(role_id, menu_id) values (3, 1006);
insert into sys_role_menu(role_id, menu_id) values (3, 1007);
insert into sys_role_menu(role_id, menu_id) values (3, 1008);
insert into sys_role_menu(role_id, menu_id) values (3, 1009);
insert into sys_role_menu(role_id, menu_id) values (3, 1010);
insert into sys_role_menu(role_id, menu_id) values (3, 1011);
insert into sys_role_menu(role_id, menu_id) values (3, 1012);
insert into sys_role_menu(role_id, menu_id) values (3, 1013);
insert into sys_role_menu(role_id, menu_id) values (3, 1014);
insert into sys_role_menu(role_id, menu_id) values (3, 1015);
insert into sys_role_menu(role_id, menu_id) values (3, 1016);
insert into sys_role_menu(role_id, menu_id) values (3, 1017);
insert into sys_role_menu(role_id, menu_id) values (3, 1018);
insert into sys_role_menu(role_id, menu_id) values (3, 1019);
insert into sys_role_menu(role_id, menu_id) values (3, 1020);
insert into sys_role_menu(role_id, menu_id) values (3, 1021);
insert into sys_role_menu(role_id, menu_id) values (3, 1022);
insert into sys_role_menu(role_id, menu_id) values (3, 1023);
insert into sys_role_menu(role_id, menu_id) values (3, 1024);
insert into sys_role_menu(role_id, menu_id) values (3, 1025);
insert into sys_role_menu(role_id, menu_id) values (3, 1026);
insert into sys_role_menu(role_id, menu_id) values (3, 1027);
insert into sys_role_menu(role_id, menu_id) values (3, 1028);
insert into sys_role_menu(role_id, menu_id) values (3, 1029);
insert into sys_role_menu(role_id, menu_id) values (3, 1030);
insert into sys_role_menu(role_id, menu_id) values (3, 1031);
insert into sys_role_menu(role_id, menu_id) values (3, 1032);
insert into sys_role_menu(role_id, menu_id) values (3, 1033);
insert into sys_role_menu(role_id, menu_id) values (3, 1034);
insert into sys_role_menu(role_id, menu_id) values (3, 1035);
insert into sys_role_menu(role_id, menu_id) values (3, 1036);
insert into sys_role_menu(role_id, menu_id) values (3, 1037);
insert into sys_role_menu(role_id, menu_id) values (3, 1038);
insert into sys_role_menu(role_id, menu_id) values (3, 1039);
insert into sys_role_menu(role_id, menu_id) values (3, 1040);
insert into sys_role_menu(role_id, menu_id) values (3, 1041);
insert into sys_role_menu(role_id, menu_id) values (3, 1042);
insert into sys_role_menu(role_id, menu_id) values (3, 1043);
insert into sys_role_menu(role_id, menu_id) values (3, 1044);
insert into sys_role_menu(role_id, menu_id) values (3, 1045);
insert into sys_role_menu(role_id, menu_id) values (3, 1500);
insert into sys_role_menu(role_id, menu_id) values (3, 1501);
insert into sys_role_menu(role_id, menu_id) values (3, 1502);
insert into sys_role_menu(role_id, menu_id) values (3, 1503);
insert into sys_role_menu(role_id, menu_id) values (3, 1504);
insert into sys_role_menu(role_id, menu_id) values (3, 1505);
insert into sys_role_menu(role_id, menu_id) values (3, 1506);
insert into sys_role_menu(role_id, menu_id) values (3, 1507);
insert into sys_role_menu(role_id, menu_id) values (3, 1508);
insert into sys_role_menu(role_id, menu_id) values (3, 1509);
insert into sys_role_menu(role_id, menu_id) values (3, 1510);
insert into sys_role_menu(role_id, menu_id) values (3, 1511);
insert into sys_role_menu(role_id, menu_id) values (4, 5);
insert into sys_role_menu(role_id, menu_id) values (4, 1500);
insert into sys_role_menu(role_id, menu_id) values (4, 1501);
insert into sys_role_menu(role_id, menu_id) values (4, 1502);
insert into sys_role_menu(role_id, menu_id) values (4, 1503);
insert into sys_role_menu(role_id, menu_id) values (4, 1504);
insert into sys_role_menu(role_id, menu_id) values (4, 1505);
insert into sys_role_menu(role_id, menu_id) values (4, 1506);
insert into sys_role_menu(role_id, menu_id) values (4, 1507);
insert into sys_role_menu(role_id, menu_id) values (4, 1508);
insert into sys_role_menu(role_id, menu_id) values (4, 1509);
insert into sys_role_menu(role_id, menu_id) values (4, 1510);
insert into sys_role_menu(role_id, menu_id) values (4, 1511);

insert into sys_user_role(user_id, role_id) values (3, 3);
insert into sys_user_role(user_id, role_id) values (4, 4);

insert into test_demo(id, tenant_id, dept_id, user_id, order_num, test_key, value, version, create_dept, create_time, create_by, update_time, update_by, del_flag) values (1, '000000', 102, 4, 1, '测试数据权限', '测试', 0, 103, sysdate, 1, null, null, 0);
insert into test_demo(id, tenant_id, dept_id, user_id, order_num, test_key, value, version, create_dept, create_time, create_by, update_time, update_by, del_flag) values (2, '000000', 102, 3, 2, '子节点1', '111', 0, 103, sysdate, 1, null, null, 0);
insert into test_demo(id, tenant_id, dept_id, user_id, order_num, test_key, value, version, create_dept, create_time, create_by, update_time, update_by, del_flag) values (3, '000000', 102, 3, 3, '子节点2', '222', 0, 103, sysdate, 1, null, null, 0);
insert into test_demo(id, tenant_id, dept_id, user_id, order_num, test_key, value, version, create_dept, create_time, create_by, update_time, update_by, del_flag) values (4, '000000', 108, 4, 4, '测试数据', 'demo', 0, 103, sysdate, 1, null, null, 0);
insert into test_demo(id, tenant_id, dept_id, user_id, order_num, test_key, value, version, create_dept, create_time, create_by, update_time, update_by, del_flag) values (5, '000000', 108, 3, 13, '子节点11', '1111', 0, 103, sysdate, 1, null, null, 0);
insert into test_demo(id, tenant_id, dept_id, user_id, order_num, test_key, value, version, create_dept, create_time, create_by, update_time, update_by, del_flag) values (6, '000000', 108, 3, 12, '子节点22', '2222', 0, 103, sysdate, 1, null, null, 0);
insert into test_demo(id, tenant_id, dept_id, user_id, order_num, test_key, value, version, create_dept, create_time, create_by, update_time, update_by, del_flag) values (7, '000000', 108, 3, 11, '子节点33', '3333', 0, 103, sysdate, 1, null, null, 0);
insert into test_demo(id, tenant_id, dept_id, user_id, order_num, test_key, value, version, create_dept, create_time, create_by, update_time, update_by, del_flag) values (8, '000000', 108, 3, 10, '子节点44', '4444', 0, 103, sysdate, 1, null, null, 0);
insert into test_demo(id, tenant_id, dept_id, user_id, order_num, test_key, value, version, create_dept, create_time, create_by, update_time, update_by, del_flag) values (9, '000000', 108, 3, 9, '子节点55', '5555', 0, 103, sysdate, 1, null, null, 0);
insert into test_demo(id, tenant_id, dept_id, user_id, order_num, test_key, value, version, create_dept, create_time, create_by, update_time, update_by, del_flag) values (10, '000000', 108, 3, 8, '子节点66', '6666', 0, 103, sysdate, 1, null, null, 0);
insert into test_demo(id, tenant_id, dept_id, user_id, order_num, test_key, value, version, create_dept, create_time, create_by, update_time, update_by, del_flag) values (11, '000000', 108, 3, 7, '子节点77', '7777', 0, 103, sysdate, 1, null, null, 0);
insert into test_demo(id, tenant_id, dept_id, user_id, order_num, test_key, value, version, create_dept, create_time, create_by, update_time, update_by, del_flag) values (12, '000000', 108, 3, 6, '子节点88', '8888', 0, 103, sysdate, 1, null, null, 0);
insert into test_demo(id, tenant_id, dept_id, user_id, order_num, test_key, value, version, create_dept, create_time, create_by, update_time, update_by, del_flag) values (13, '000000', 108, 3, 5, '子节点99', '9999', 0, 103, sysdate, 1, null, null, 0);

insert into test_tree(id, tenant_id, parent_id, dept_id, user_id, tree_name, version, create_dept, create_time, create_by, update_time, update_by, del_flag) values (1, '000000', 0, 102, 4, '测试数据权限', 0, 103, sysdate, 1, null, null, 0);
insert into test_tree(id, tenant_id, parent_id, dept_id, user_id, tree_name, version, create_dept, create_time, create_by, update_time, update_by, del_flag) values (2, '000000', 1, 102, 3, '子节点1', 0, 103, sysdate, 1, null, null, 0);
insert into test_tree(id, tenant_id, parent_id, dept_id, user_id, tree_name, version, create_dept, create_time, create_by, update_time, update_by, del_flag) values (3, '000000', 2, 102, 3, '子节点2', 0, 103, sysdate, 1, null, null, 0);
insert into test_tree(id, tenant_id, parent_id, dept_id, user_id, tree_name, version, create_dept, create_time, create_by, update_time, update_by, del_flag) values (4, '000000', 0, 108, 4, '测试树1', 0, 103, sysdate, 1, null, null, 0);
insert into test_tree(id, tenant_id, parent_id, dept_id, user_id, tree_name, version, create_dept, create_time, create_by, update_time, update_by, del_flag) values (5, '000000', 4, 108, 3, '子节点11', 0, 103, sysdate, 1, null, null, 0);
insert into test_tree(id, tenant_id, parent_id, dept_id, user_id, tree_name, version, create_dept, create_time, create_by, update_time, update_by, del_flag) values (6, '000000', 4, 108, 3, '子节点22', 0, 103, sysdate, 1, null, null, 0);
insert into test_tree(id, tenant_id, parent_id, dept_id, user_id, tree_name, version, create_dept, create_time, create_by, update_time, update_by, del_flag) values (7, '000000', 4, 108, 3, '子节点33', 0, 103, sysdate, 1, null, null, 0);
insert into test_tree(id, tenant_id, parent_id, dept_id, user_id, tree_name, version, create_dept, create_time, create_by, update_time, update_by, del_flag) values (8, '000000', 5, 108, 3, '子节点44', 0, 103, sysdate, 1, null, null, 0);
insert into test_tree(id, tenant_id, parent_id, dept_id, user_id, tree_name, version, create_dept, create_time, create_by, update_time, update_by, del_flag) values (9, '000000', 6, 108, 3, '子节点55', 0, 103, sysdate, 1, null, null, 0);
insert into test_tree(id, tenant_id, parent_id, dept_id, user_id, tree_name, version, create_dept, create_time, create_by, update_time, update_by, del_flag) values (10, '000000', 7, 108, 3, '子节点66', 0, 103, sysdate, 1, null, null, 0);
insert into test_tree(id, tenant_id, parent_id, dept_id, user_id, tree_name, version, create_dept, create_time, create_by, update_time, update_by, del_flag) values (11, '000000', 7, 108, 3, '子节点77', 0, 103, sysdate, 1, null, null, 0);
insert into test_tree(id, tenant_id, parent_id, dept_id, user_id, tree_name, version, create_dept, create_time, create_by, update_time, update_by, del_flag) values (12, '000000', 10, 108, 3, '子节点88', 0, 103, sysdate, 1, null, null, 0);
insert into test_tree(id, tenant_id, parent_id, dept_id, user_id, tree_name, version, create_dept, create_time, create_by, update_time, update_by, del_flag) values (13, '000000', 10, 108, 3, '子节点99', 0, 103, sysdate, 1, null, null, 0);
