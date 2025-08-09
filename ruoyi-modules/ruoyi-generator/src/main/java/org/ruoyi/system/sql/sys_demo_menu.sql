-- 菜单 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1954175368920645632, 'dome管理', '2000', '1', 'sysDemo', 'system/sysDemo/index', 1, 0, 'C', '0', '0', 'system:sysDemo:list', '#', 103, 1, sysdate(), null, null, 'dome管理菜单');

-- 按钮 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1954175368920645633, 'dome管理查询', 1954175368920645632, '1',  '#', '', 1, 0, 'F', '0', '0', 'system:sysDemo:query',        '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1954175368920645634, 'dome管理新增', 1954175368920645632, '2',  '#', '', 1, 0, 'F', '0', '0', 'system:sysDemo:add',          '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1954175368920645635, 'dome管理修改', 1954175368920645632, '3',  '#', '', 1, 0, 'F', '0', '0', 'system:sysDemo:edit',         '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1954175368920645636, 'dome管理删除', 1954175368920645632, '4',  '#', '', 1, 0, 'F', '0', '0', 'system:sysDemo:remove',       '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1954175368920645637, 'dome管理导出', 1954175368920645632, '5',  '#', '', 1, 0, 'F', '0', '0', 'system:sysDemo:export',       '#', 103, 1, sysdate(), null, null, '');
