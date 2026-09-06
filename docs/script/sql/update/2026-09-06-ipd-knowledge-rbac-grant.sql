-- 2026-09-06 AI-REG-01 遗留销项①：非超管 IPD 映射账号（ipd-leader/ipd-market/ipd-rd）授权知识库菜单与按钮权限码
-- 背景：sys_menu 知识库按钮只有 system:info:query/add/edit/remove/export 5 个（KnowledgeInfoController 还用 system:info:list，
--       KnowledgeAttachController 用 system:attach:*，KnowledgeFragmentController 用 system:fragment:*，三者均无对应菜单行）；
--       ipd_pm(900201)/ipd_group_leader(900202) 两角色 sys_role_menu 为 0 行——换票后平台菜单空、资料库只见壳。
-- 动作：A. 补 13 个 F 按钮菜单（挂知识管理 C=2006681261898813441 下，menu_id 3447-3459 已核空闲）；
--       B. 两角色绑定「对话管理目录 → 知识管理 → 全部知识库按钮」共 20 个菜单（2x20=40 行）。
-- 幂等：可重复执行（菜单按 NOT EXISTS 防重，角色绑定先删后插）。
-- 关联：docs/ipd-系统说明/前端对接/AI平台功能回归-设计-20260906.md §遗留登记 1

-- A. 补按钮菜单
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
SELECT 2006681261898813447, '知识库列表', 2006681261898813441, 6, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:info:list', '#', 103, 1, NOW(), 'AI-REG-01 遗留①补：列表接口权限码' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms='system:info:list' AND parent_id=2006681261898813441);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
SELECT 2006681261898813448, '知识文档列表', 2006681261898813441, 7, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:attach:list', '#', 103, 1, NOW(), 'AI-REG-01 遗留①补' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms='system:attach:list' AND parent_id=2006681261898813441);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
SELECT 2006681261898813449, '知识文档查询', 2006681261898813441, 8, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:attach:query', '#', 103, 1, NOW(), 'AI-REG-01 遗留①补' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms='system:attach:query' AND parent_id=2006681261898813441);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
SELECT 2006681261898813450, '知识文档新增', 2006681261898813441, 9, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:attach:add', '#', 103, 1, NOW(), 'AI-REG-01 遗留①补' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms='system:attach:add' AND parent_id=2006681261898813441);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
SELECT 2006681261898813451, '知识文档修改', 2006681261898813441, 10, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:attach:edit', '#', 103, 1, NOW(), 'AI-REG-01 遗留①补' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms='system:attach:edit' AND parent_id=2006681261898813441);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
SELECT 2006681261898813452, '知识文档删除', 2006681261898813441, 11, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:attach:remove', '#', 103, 1, NOW(), 'AI-REG-01 遗留①补' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms='system:attach:remove' AND parent_id=2006681261898813441);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
SELECT 2006681261898813453, '知识文档导出', 2006681261898813441, 12, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:attach:export', '#', 103, 1, NOW(), 'AI-REG-01 遗留①补' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms='system:attach:export' AND parent_id=2006681261898813441);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
SELECT 2006681261898813454, '知识片段列表', 2006681261898813441, 13, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:fragment:list', '#', 103, 1, NOW(), 'AI-REG-01 遗留①补' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms='system:fragment:list' AND parent_id=2006681261898813441);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
SELECT 2006681261898813455, '知识片段查询', 2006681261898813441, 14, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:fragment:query', '#', 103, 1, NOW(), 'AI-REG-01 遗留①补' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms='system:fragment:query' AND parent_id=2006681261898813441);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
SELECT 2006681261898813456, '知识片段新增', 2006681261898813441, 15, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:fragment:add', '#', 103, 1, NOW(), 'AI-REG-01 遗留①补' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms='system:fragment:add' AND parent_id=2006681261898813441);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
SELECT 2006681261898813457, '知识片段修改', 2006681261898813441, 16, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:fragment:edit', '#', 103, 1, NOW(), 'AI-REG-01 遗留①补' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms='system:fragment:edit' AND parent_id=2006681261898813441);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
SELECT 2006681261898813458, '知识片段删除', 2006681261898813441, 17, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:fragment:remove', '#', 103, 1, NOW(), 'AI-REG-01 遗留①补' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms='system:fragment:remove' AND parent_id=2006681261898813441);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
SELECT 2006681261898813459, '知识片段导出', 2006681261898813441, 18, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:fragment:export', '#', 103, 1, NOW(), 'AI-REG-01 遗留①补' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms='system:fragment:export' AND parent_id=2006681261898813441);

-- B. 角色绑定（先删后插保证幂等）：ipd_pm=900201 / ipd_group_leader=900202
--    绑定链路：对话管理目录(2000209300188356609) → 知识管理 C(2006681261898813441) → 既有 5 F(3442-3446) + 新 13 F(3447-3459)
DELETE rm FROM sys_role_menu rm
WHERE rm.role_id IN (900201, 900202)
  AND rm.menu_id IN (2000209300188356609, 2006681261898813441,
      2006681261898813442, 2006681261898813443, 2006681261898813444, 2006681261898813445, 2006681261898813446,
      2006681261898813447, 2006681261898813448, 2006681261898813449, 2006681261898813450, 2006681261898813451,
      2006681261898813452, 2006681261898813453, 2006681261898813454, 2006681261898813455, 2006681261898813456,
      2006681261898813457, 2006681261898813458, 2006681261898813459);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON (
  m.menu_id = 2000209300188356609              -- 对话管理目录
  OR m.menu_id = 2006681261898813441           -- 知识管理 C
  OR (m.parent_id = 2006681261898813441 AND m.menu_type = 'F' AND m.status = '0')  -- 知识管理下全部启用按钮（含既有5+新13）
)
WHERE r.role_id IN (900201, 900202);
