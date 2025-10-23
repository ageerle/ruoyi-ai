-- =============================================
-- 知识图谱菜单配置SQL
-- 执行此脚本后，图谱管理菜单将显示在系统中
-- =============================================

-- 注意：请根据实际情况修改以下内容：
-- 1. parent_id: 运营管理的菜单ID（需要先查询获取）
-- 2. order_num: 菜单排序号
-- 3. create_by: 创建人

-- =============================================
-- 第一步：查询运营管理的菜单ID
-- =============================================
-- SELECT menu_id FROM sys_menu WHERE menu_name = '运营管理' AND parent_id = 0;
-- 假设查询结果为: 2000（请根据实际情况修改）

SET @operator_menu_id = (SELECT menu_id FROM sys_menu WHERE menu_name = '运营管理' AND parent_id = 0 LIMIT 1);

-- =============================================
-- 第二步：插入图谱管理目录
-- =============================================
INSERT INTO sys_menu (
    menu_id,
    menu_name, 
    parent_id, 
    order_num, 
    path, 
    component, 
    is_frame, 
    is_cache, 
    menu_type, 
    visible, 
    status, 
    perms, 
    icon,
    create_dept,
    create_by, 
    create_time, 
    update_by, 
    update_time, 
    remark
) VALUES (
    1950000000000000001,           -- 菜单ID（使用雪花ID规则）
    '图谱管理',                    -- 菜单名称
    @operator_menu_id,             -- 父菜单ID（运营管理）
    15,                            -- 排序号（在知识库管理之后）
    'graph',                       -- 路由地址
    NULL,                          -- 组件路径（目录为空）
    1,                             -- 是否外链（0否 1是）
    0,                             -- 是否缓存（0缓存 1不缓存）
    'M',                           -- 菜单类型（M目录 C菜单 F按钮）
    '0',                           -- 显示状态（0显示 1隐藏）
    '0',                           -- 菜单状态（0正常 1停用）
    NULL,                          -- 权限标识
    'carbon:chart-relationship',   -- 菜单图标
    103,                           -- 创建部门
    1,                             -- 创建者（用户ID）
    NOW(),                         -- 创建时间
    1,                             -- 更新者（用户ID）
    NOW(),                         -- 更新时间
    '知识图谱管理目录'              -- 备注
);

-- 设置图谱管理目录ID
SET @graph_menu_id = 1950000000000000001;

-- =============================================
-- 第三步：插入图谱实例管理菜单
-- =============================================
INSERT INTO sys_menu (
    menu_id,
    menu_name, 
    parent_id, 
    order_num, 
    path, 
    component, 
    is_frame, 
    is_cache, 
    menu_type, 
    visible, 
    status, 
    perms, 
    icon,
    create_dept,
    create_by, 
    create_time, 
    update_by, 
    update_time, 
    remark
) VALUES (
    1950000000000000002,                               -- 菜单ID
    '图谱实例',                                        -- 菜单名称
    @graph_menu_id,                                    -- 父菜单ID（图谱管理）
    1,                                                 -- 排序号
    'graphInstance',                                   -- 路由地址
    'operator/graphInstance/index',                    -- 组件路径
    1,                                                 -- 是否外链
    0,                                                 -- 是否缓存
    'C',                                               -- 菜单类型
    '0',                                               -- 显示状态
    '0',                                               -- 菜单状态
    'operator:graph:list',                             -- 权限标识
    'ant-design:node-index-outlined',                  -- 菜单图标
    103,                                               -- 创建部门
    1,                                                 -- 创建者（用户ID）
    NOW(),                                             -- 创建时间
    1,                                                 -- 更新者（用户ID）
    NOW(),                                             -- 更新时间
    '图谱实例管理菜单'                                  -- 备注
);

-- 设置图谱实例菜单ID
SET @graph_instance_menu_id = 1950000000000000002;

-- =============================================
-- 第四步：插入图谱实例管理的按钮权限
-- =============================================

-- 查询按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
VALUES (1950000000000000003, '图谱实例查询', @graph_instance_menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'operator:graph:query', '#', 103, 1, NOW(), '');

-- 新增按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
VALUES (1950000000000000004, '图谱实例新增', @graph_instance_menu_id, 2, '#', '', 1, 0, 'F', '0', '0', 'operator:graph:add', '#', 103, 1, NOW(), '');

-- 编辑按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
VALUES (1950000000000000005, '图谱实例编辑', @graph_instance_menu_id, 3, '#', '', 1, 0, 'F', '0', '0', 'operator:graph:edit', '#', 103, 1, NOW(), '');

-- 删除按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
VALUES (1950000000000000006, '图谱实例删除', @graph_instance_menu_id, 4, '#', '', 1, 0, 'F', '0', '0', 'operator:graph:remove', '#', 103, 1, NOW(), '');

-- 导出按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
VALUES (1950000000000000007, '图谱实例导出', @graph_instance_menu_id, 5, '#', '', 1, 0, 'F', '0', '0', 'operator:graph:export', '#', 103, 1, NOW(), '');

-- 构建按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
VALUES (1950000000000000008, '图谱构建', @graph_instance_menu_id, 6, '#', '', 1, 0, 'F', '0', '0', 'operator:graph:build', '#', 103, 1, NOW(), '');

-- 重建按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
VALUES (1950000000000000009, '图谱重建', @graph_instance_menu_id, 7, '#', '', 1, 0, 'F', '0', '0', 'operator:graph:rebuild', '#', 103, 1, NOW(), '');

-- =============================================
-- 第五步：插入图谱可视化菜单
-- =============================================
INSERT INTO sys_menu (
    menu_id,
    menu_name, 
    parent_id, 
    order_num, 
    path, 
    component, 
    is_frame, 
    is_cache, 
    menu_type, 
    visible, 
    status, 
    perms, 
    icon,
    create_dept,
    create_by, 
    create_time, 
    update_by, 
    update_time, 
    remark
) VALUES (
    1950000000000000010,                               -- 菜单ID
    '图谱可视化',                                      -- 菜单名称
    @graph_menu_id,                                    -- 父菜单ID（图谱管理）
    2,                                                 -- 排序号
    'graphVisualization',                              -- 路由地址
    'operator/graphVisualization/index',               -- 组件路径
    1,                                                 -- 是否外链
    0,                                                 -- 是否缓存
    'C',                                               -- 菜单类型
    '0',                                               -- 显示状态
    '0',                                               -- 菜单状态
    'operator:graph:view',                             -- 权限标识
    'carbon:chart-network',                            -- 菜单图标
    103,                                               -- 创建部门
    1,                                                 -- 创建者（用户ID）
    NOW(),                                             -- 创建时间
    1,                                                 -- 更新者（用户ID）
    NOW(),                                             -- 更新时间
    '图谱可视化菜单'                                    -- 备注
);

-- =============================================
-- 第六步：插入图谱检索测试菜单
-- =============================================
INSERT INTO sys_menu (
    menu_id,
    menu_name, 
    parent_id, 
    order_num, 
    path, 
    component, 
    is_frame, 
    is_cache, 
    menu_type, 
    visible, 
    status, 
    perms, 
    icon,
    create_dept,
    create_by, 
    create_time, 
    update_by, 
    update_time, 
    remark
) VALUES (
    1950000000000000011,                               -- 菜单ID
    '图谱检索测试',                                    -- 菜单名称
    @graph_menu_id,                                    -- 父菜单ID（图谱管理）
    3,                                                 -- 排序号
    'graphRAG',                                        -- 路由地址
    'operator/graphRAG/index',                         -- 组件路径
    1,                                                 -- 是否外链
    0,                                                 -- 是否缓存
    'C',                                               -- 菜单类型
    '0',                                               -- 显示状态
    '0',                                               -- 菜单状态
    'operator:graph:retrieve',                         -- 权限标识
    'carbon:search-advanced',                          -- 菜单图标
    103,                                               -- 创建部门
    1,                                                 -- 创建者（用户ID）
    NOW(),                                             -- 创建时间
    1,                                                 -- 更新者（用户ID）
    NOW(),                                             -- 更新时间
    '图谱检索测试菜单'                                  -- 备注
);

-- 设置图谱检索测试菜单ID
SET @graph_rag_menu_id = 1950000000000000011;

-- =============================================
-- 第七步：插入图谱检索测试的按钮权限
-- =============================================

-- 实体抽取按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
VALUES (1950000000000000012, '实体抽取', @graph_rag_menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'operator:graph:extract', '#', 103, 1, NOW(), '');

-- 文本入库按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
VALUES (1950000000000000013, '文本入库', @graph_rag_menu_id, 2, '#', '', 1, 0, 'F', '0', '0', 'operator:graph:ingest', '#', 103, 1, NOW(), '');

-- =============================================
-- 完成提示
-- =============================================
SELECT '图谱管理菜单配置完成！' AS message;
SELECT '请刷新浏览器页面，菜单将显示在"运营管理"下' AS tip;

-- =============================================
-- 查询结果验证
-- =============================================
SELECT 
    m1.menu_name AS '一级菜单',
    m2.menu_name AS '二级菜单',
    m3.menu_name AS '三级菜单/按钮',
    m3.perms AS '权限标识',
    m3.path AS '路由地址',
    m3.component AS '组件路径'
FROM sys_menu m1
LEFT JOIN sys_menu m2 ON m2.parent_id = m1.menu_id
LEFT JOIN sys_menu m3 ON m3.parent_id = m2.menu_id
WHERE m1.menu_name = '运营管理' 
  AND m2.menu_name = '图谱管理'
ORDER BY m2.order_num, m3.order_num;

