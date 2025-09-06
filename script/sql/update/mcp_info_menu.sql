-- 菜单 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1954103099019309056, 'MCP', '2000', '1', 'mcpInfo', 'operator/mcpInfo/index', 1, 0, 'C', '0', '0', 'operator:mcpInfo:list', '#', 103, 1, sysdate(), null, null, 'MCP菜单');

-- 按钮 SQL
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1954103099019309057, 'MCP查询', 1954103099019309056, '1',  '#', '', 1, 0, 'F', '0', '0', 'operator:mcpInfo:query',        '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1954103099019309058, 'MCP新增', 1954103099019309056, '2',  '#', '', 1, 0, 'F', '0', '0', 'operator:mcpInfo:add',          '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1954103099019309059, 'MCP修改', 1954103099019309056, '3',  '#', '', 1, 0, 'F', '0', '0', 'operator:mcpInfo:edit',         '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1954103099019309060, 'MCP删除', 1954103099019309056, '4',  '#', '', 1, 0, 'F', '0', '0', 'operator:mcpInfo:remove',       '#', 103, 1, sysdate(), null, null, '');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values(1954103099019309061, 'MCP导出', 1954103099019309056, '5',  '#', '', 1, 0, 'F', '0', '0', 'operator:mcpInfo:export',       '#', 103, 1, sysdate(), null, null, '');


-- mcp_info ddl
CREATE TABLE `mcp_info` (
                            `mcp_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
                            `server_name` varchar(50) DEFAULT NULL COMMENT '服务器名称',
                            `transport_type` varchar(255) DEFAULT NULL COMMENT '链接方式',
                            `command` varchar(255) DEFAULT NULL COMMENT 'Command',
                            `arguments` varchar(255) DEFAULT NULL COMMENT 'Args',
                            `env` varchar(255) DEFAULT NULL COMMENT 'Env',
                            `status` tinyint(1) DEFAULT NULL COMMENT '是否启用',
                            `create_dept` bigint(20) DEFAULT NULL COMMENT '创建部门',
                            `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
                            `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                            `remark` varchar(255) DEFAULT NULL COMMENT '备注',
                            PRIMARY KEY (`mcp_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4;

INSERT INTO `ruoyi-ai`.`sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1954098808913211393, '000000', 0, 'STDIO', 'STDIO', 'mcp_transport_type', NULL, '', 'N', '0', NULL, NULL, '2025-08-09 16:33:56', 1, '2025-08-09 16:34:19', NULL);
INSERT INTO `ruoyi-ai`.`sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1954098960432443394, '000000', 1, 'SSE', 'SSE', 'mcp_transport_type', NULL, '', 'N', '0', NULL, NULL, '2025-08-09 16:34:32', NULL, '2025-08-09 16:34:32', NULL);
INSERT INTO `ruoyi-ai`.`sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1954099421436784642, '000000', 2, 'HTTP', 'HTTP', 'mcp_transport_type', NULL, '', 'N', '0', NULL, NULL, '2025-08-09 16:36:22', NULL, '2025-08-09 16:36:22', NULL);
INSERT INTO `ruoyi-ai`.`sys_dict_type` (`dict_id`, `tenant_id`, `dict_name`, `dict_type`, `status`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1954098639622713345, '000000', 'mcp链接方式', 'mcp_transport_type', '0', NULL, NULL, '2025-08-09 16:33:16', NULL, '2025-08-09 16:33:16', NULL);


INSERT INTO `ruoyi-ai`.`mcp_info` (`mcp_id`, `server_name`, `transport_type`, `command`, `arguments`, `env`, `status`, `description`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1, 'howtocook-mcp', 'STDIO', 'npx', '["-y", "howtocook-mcp"]', NULL, 1, NULL, NULL, NULL, '2025-08-11 17:19:25', 1, '2025-08-11 18:24:22', NULL);
