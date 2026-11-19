-- Existing installations: apply before starting the version with database-managed skills.
-- agent_info.skill_names retains the stable names stored by older versions.
CREATE TABLE IF NOT EXISTS agent_skill (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
    name varchar(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '技能标识',
    description varchar(1024) NOT NULL COMMENT '技能描述',
    content longtext NOT NULL COMMENT '技能指令',
    status char(1) NOT NULL DEFAULT '0' COMMENT '0启用 1停用',
    create_dept bigint DEFAULT NULL,
    create_by bigint DEFAULT NULL,
    create_time datetime DEFAULT NULL,
    update_by bigint DEFAULT NULL,
    update_time datetime DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_agent_skill_tenant_name (tenant_id, name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '智能体技能';

INSERT IGNORE INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query_param,
     is_frame, is_cache, menu_type, visible, status, perms, icon,
     create_dept, create_by, create_time, remark)
VALUES
    (2099111900010000100, '技能管理', 0, 5, 'agent/skill', 'agent/skill/index', '', 1, 0, 'C', '0', '0', 'agent:skill:list', 'mdi:book-cog-outline', 103, 1, NOW(), '智能体技能管理'),
    (2099111900010000101, '技能详情', 2099111900010000100, 1, '#', '', '', 1, 0, 'F', '0', '0', 'agent:skill:query', '#', 103, 1, NOW(), ''),
    (2099111900010000102, '技能新增', 2099111900010000100, 2, '#', '', '', 1, 0, 'F', '0', '0', 'agent:skill:add', '#', 103, 1, NOW(), ''),
    (2099111900010000103, '技能修改', 2099111900010000100, 3, '#', '', '', 1, 0, 'F', '0', '0', 'agent:skill:edit', '#', 103, 1, NOW(), ''),
    (2099111900010000104, '技能删除', 2099111900010000100, 4, '#', '', '', 1, 0, 'F', '0', '0', 'agent:skill:remove', '#', 103, 1, NOW(), '');
