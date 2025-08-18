-- 菜单 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1957435675864506368, '员工分配', '2000', '1', 'storeEmployee', 'store/storeEmployee/index', 1, 0, 'C', '0', '0', 'store:storeEmployee:list', '#', 103, 1, sysdate(), null, null, '员工分配菜单');

-- 按钮 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1957435675864506369, '员工分配查询', 1957435675864506368, '1',  '#', '', 1, 0, 'F', '0', '0', 'store:storeEmployee:query',        '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1957435675864506370, '员工分配新增', 1957435675864506368, '2',  '#', '', 1, 0, 'F', '0', '0', 'store:storeEmployee:add',          '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1957435675864506371, '员工分配修改', 1957435675864506368, '3',  '#', '', 1, 0, 'F', '0', '0', 'store:storeEmployee:edit',         '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1957435675864506372, '员工分配删除', 1957435675864506368, '4',  '#', '', 1, 0, 'F', '0', '0', 'store:storeEmployee:remove',       '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1957435675864506373, '员工分配导出', 1957435675864506368, '5',  '#', '', 1, 0, 'F', '0', '0', 'store:storeEmployee:export',       '#', 103, 1, sysdate(), null, null, '');
