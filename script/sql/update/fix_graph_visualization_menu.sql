-- =============================================
-- 修复图谱可视化菜单配置
-- 日期: 2025-10-13
-- 说明: 确保图谱可视化菜单正确配置，支持独立访问
-- =============================================

-- 1. 检查图谱可视化菜单是否存在
SELECT 
    menu_id,
    menu_name,
    parent_id,
    path,
    component,
    visible,
    status,
    menu_type
FROM sys_menu 
WHERE menu_name = '图谱可视化' OR path = 'graphVisualization';

-- 2. 如果菜单不存在，插入菜单
-- 注意：如果已存在，此语句会因主键冲突而失败，这是正常的
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
)
SELECT 
    1950000000000000010,                               -- 菜单ID
    '图谱可视化',                                      -- 菜单名称
    (SELECT menu_id FROM sys_menu WHERE menu_name = '图谱管理' LIMIT 1),  -- 父菜单ID
    2,                                                 -- 排序号
    'graphVisualization',                              -- 路由地址
    'operator/graphVisualization/index',               -- 组件路径
    1,                                                 -- 是否外链（1=否）
    0,                                                 -- 是否缓存（0=缓存）
    'C',                                               -- 菜单类型（C=菜单）
    '0',                                               -- 显示状态（0=显示）
    '0',                                               -- 菜单状态（0=正常）
    'operator:graph:view',                             -- 权限标识
    'carbon:chart-network',                            -- 菜单图标
    103,                                               -- 创建部门
    1,                                                 -- 创建者（用户ID）
    NOW(),                                             -- 创建时间
    1,                                                 -- 更新者（用户ID）
    NOW(),                                             -- 更新时间
    '图谱可视化菜单'                                    -- 备注
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE menu_id = 1950000000000000010
);

-- 3. 更新现有菜单（如果已存在）
UPDATE sys_menu 
SET 
    path = 'graphVisualization',
    component = 'operator/graphVisualization/index',
    visible = '0',
    status = '0',
    menu_type = 'C',
    is_frame = 1,
    is_cache = 0,
    update_by = 1,
    update_time = NOW()
WHERE menu_name = '图谱可视化';

-- 4. 验证菜单配置
SELECT 
    menu_id,
    menu_name,
    parent_id,
    path,
    component,
    visible AS '显示状态(0=显示)',
    status AS '菜单状态(0=正常)',
    menu_type AS '菜单类型(C=菜单)',
    perms AS '权限标识'
FROM sys_menu 
WHERE menu_name = '图谱可视化';

-- 5. 检查父菜单
SELECT 
    m1.menu_id,
    m1.menu_name,
    m1.path,
    m2.menu_name AS parent_name,
    m2.path AS parent_path
FROM sys_menu m1
LEFT JOIN sys_menu m2 ON m1.parent_id = m2.menu_id
WHERE m1.menu_name = '图谱可视化';

-- =============================================
-- 执行说明
-- =============================================
-- 1. 在 MySQL 客户端或 Navicat 中执行此 SQL
-- 2. 检查输出，确认菜单配置正确
-- 3. 重新登录系统以刷新菜单权限
-- 4. 访问 http://localhost:5666/#/operator/graphVisualization?id=xxx&knowledgeId=xxx
-- =============================================

