-- 菜单 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible,
                      status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values (1971546066781597696, '数字人信息管理', '2000', '1', 'aihumanInfo', 'aihuman/aihumanInfo/index', 1, 0, 'C', '0',
        '0', 'aihuman:aihumanInfo:list', '#', 103, 1, sysdate(), null, null, '数字人信息管理菜单');

-- 按钮 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible,
                      status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values (1971546066781597697, '数字人信息管理查询', 1971546066781597696, '1', '#', '', 1, 0, 'F', '0', '0',
        'aihuman:aihumanInfo:query', '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible,
                      status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values (1971546066781597698, '数字人信息管理新增', 1971546066781597696, '2', '#', '', 1, 0, 'F', '0', '0',
        'aihuman:aihumanInfo:add', '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible,
                      status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values (1971546066781597699, '数字人信息管理修改', 1971546066781597696, '3', '#', '', 1, 0, 'F', '0', '0',
        'aihuman:aihumanInfo:edit', '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible,
                      status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values (1971546066781597700, '数字人信息管理删除', 1971546066781597696, '4', '#', '', 1, 0, 'F', '0', '0',
        'aihuman:aihumanInfo:remove', '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible,
                      status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values (1971546066781597701, '数字人信息管理导出', 1971546066781597696, '5', '#', '', 1, 0, 'F', '0', '0',
        'aihuman:aihumanInfo:export', '#', 103, 1, sysdate(), null, null, '');
