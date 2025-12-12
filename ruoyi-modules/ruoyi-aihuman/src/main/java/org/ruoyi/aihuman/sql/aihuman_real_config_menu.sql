-- 菜单 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible,
                      status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values (1980480880138051584, '真人交互数字人配置', '2000', '1', 'aihumanRealConfig', 'aihuman/aihumanRealConfig/index',
        1, 0, 'C', '0', '0', 'aihuman:aihumanRealConfig:list', '#', 103, 1, sysdate(), null, null,
        '真人交互数字人配置菜单');

-- 按钮 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible,
                      status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values (1980480880138051585, '真人交互数字人配置查询', 1980480880138051584, '1', '#', '', 1, 0, 'F', '0', '0',
        'aihuman:aihumanRealConfig:query', '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible,
                      status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values (1980480880138051586, '真人交互数字人配置新增', 1980480880138051584, '2', '#', '', 1, 0, 'F', '0', '0',
        'aihuman:aihumanRealConfig:add', '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible,
                      status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values (1980480880138051587, '真人交互数字人配置修改', 1980480880138051584, '3', '#', '', 1, 0, 'F', '0', '0',
        'aihuman:aihumanRealConfig:edit', '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible,
                      status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values (1980480880138051588, '真人交互数字人配置删除', 1980480880138051584, '4', '#', '', 1, 0, 'F', '0', '0',
        'aihuman:aihumanRealConfig:remove', '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible,
                      status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values (1980480880138051589, '真人交互数字人配置导出', 1980480880138051584, '5', '#', '', 1, 0, 'F', '0', '0',
        'aihuman:aihumanRealConfig:export', '#', 103, 1, sysdate(), null, null, '');
