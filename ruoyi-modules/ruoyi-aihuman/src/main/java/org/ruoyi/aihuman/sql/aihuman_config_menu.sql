-- 菜单 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible,
                      status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values (1971582278942666752, '交互数字人配置', '2000', '1', 'aihumanConfig', 'aihuman/aihumanConfig/index', 1, 0, 'C',
        '0', '0', 'aihuman:aihumanConfig:list', '#', 103, 1, sysdate(), null, null, '交互数字人配置菜单');

-- 按钮 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible,
                      status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values (1971582278942666753, '交互数字人配置查询', 1971582278942666752, '1', '#', '', 1, 0, 'F', '0', '0',
        'aihuman:aihumanConfig:query', '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible,
                      status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values (1971582278942666754, '交互数字人配置新增', 1971582278942666752, '2', '#', '', 1, 0, 'F', '0', '0',
        'aihuman:aihumanConfig:add', '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible,
                      status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values (1971582278942666755, '交互数字人配置修改', 1971582278942666752, '3', '#', '', 1, 0, 'F', '0', '0',
        'aihuman:aihumanConfig:edit', '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible,
                      status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values (1971582278942666756, '交互数字人配置删除', 1971582278942666752, '4', '#', '', 1, 0, 'F', '0', '0',
        'aihuman:aihumanConfig:remove', '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible,
                      status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values (1971582278942666757, '交互数字人配置导出', 1971582278942666752, '5', '#', '', 1, 0, 'F', '0', '0',
        'aihuman:aihumanConfig:export', '#', 103, 1, sysdate(), null, null, '');
