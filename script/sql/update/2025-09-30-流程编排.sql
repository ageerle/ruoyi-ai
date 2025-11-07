CREATE TABLE `t_workflow`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
    `uuid`        varchar(32)  NOT NULL DEFAULT 'uuid',
    `title`       varchar(100) NOT NULL DEFAULT '标题',
    `user_id`     bigint(20) NOT NULL DEFAULT '0' COMMENT '用户ID',
    `is_public`   tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否公开',
    `is_enable`   tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`      text COMMENT '备注',
    `is_deleted`  tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除 默认0不删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=119 DEFAULT CHARSET=utf8mb4 COMMENT='工作流定义（用户定义的工作流）| Workflow Definition';



CREATE TABLE `t_workflow_node`
(
    `id`                    bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uuid`                  varchar(32)  NOT NULL DEFAULT '' COMMENT '节点唯一标识',
    `workflow_id`           bigint(20) NOT NULL DEFAULT '0' COMMENT '所属工作流定义 id',
    `workflow_component_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '引用的组件 id',
    `user_id`               bigint(20) NOT NULL DEFAULT '0' COMMENT '创建人',
    `title`                 varchar(100) NOT NULL DEFAULT '' COMMENT '节点标题',
    `remark`                varchar(500) NOT NULL DEFAULT '' COMMENT '节点备注',
    `input_config`          json         NOT NULL COMMENT '输入参数模板，例：{"params":[{"name":"user_define_param01","type":"string"}]}',
    `node_config`           json                  DEFAULT NULL COMMENT '节点执行配置，例：{"params":[{"prompt":"Summarize the following content:{user_define_param01}"}]}',
    `position_x`            double       NOT NULL DEFAULT '0' COMMENT '画布 x 坐标',
    `position_y`            double       NOT NULL DEFAULT '0' COMMENT '画布 y 坐标',
    `create_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`            tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 正常，1 已删',
    PRIMARY KEY (`id`),
    KEY                     `idx_workflow_node_workflow_id` (`workflow_id`)
) ENGINE=InnoDB AUTO_INCREMENT=269 DEFAULT CHARSET=utf8mb4 COMMENT='工作流定义的节点 | Node of Workflow Definition';



CREATE TABLE `t_workflow_runtime_node`
(
    `id`                  bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uuid`                varchar(32)  NOT NULL DEFAULT '' COMMENT '节点运行实例唯一标识',
    `user_id`             bigint(20) NOT NULL DEFAULT '0' COMMENT '创建人',
    `workflow_runtime_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '所属运行实例 id',
    `node_id`             bigint(20) NOT NULL DEFAULT '0' COMMENT '对应工作流定义里的节点 id',
    `input`               json                  DEFAULT NULL COMMENT '节点本次输入数据',
    `output`              json                  DEFAULT NULL COMMENT '节点本次输出数据',
    `status`              smallint(6) NOT NULL DEFAULT '1' COMMENT '节点执行状态：1 进行中，2 失败，3 成功',
    `status_remark`       varchar(250) NOT NULL DEFAULT '' COMMENT '状态补充说明，如失败堆栈',
    `create_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`          tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 正常，1 已删',
    PRIMARY KEY (`id`),
    KEY                   `idx_runtime_node_runtime_id` (`workflow_runtime_id`),
    KEY                   `idx_runtime_node_node_id` (`node_id`)
) ENGINE=InnoDB AUTO_INCREMENT=805 DEFAULT CHARSET=utf8mb4 COMMENT='工作流实例（运行时）- 节点 | Workflow Runtime Node';



CREATE TABLE `t_workflow_edge`
(
    `id`               bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uuid`             varchar(32) NOT NULL DEFAULT '' COMMENT '边唯一标识',
    `workflow_id`      bigint(20) NOT NULL DEFAULT '0' COMMENT '所属工作流定义 id',
    `source_node_uuid` varchar(32) NOT NULL DEFAULT '' COMMENT '起始节点 uuid',
    `source_handle`    varchar(32) NOT NULL DEFAULT '' COMMENT '起始锚点标识',
    `target_node_uuid` varchar(32) NOT NULL DEFAULT '' COMMENT '目标节点 uuid',
    `create_time`      datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`       tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 正常，1 已删',
    PRIMARY KEY (`id`),
    KEY                `idx_workflow_edge_workflow_id` (`workflow_id`)
) ENGINE=InnoDB AUTO_INCREMENT=199 DEFAULT CHARSET=utf8mb4 COMMENT='工作流定义的边 | Edge of Workflow Definition';



CREATE TABLE `t_workflow_component`
(
    `id`            bigint(20) NOT NULL AUTO_INCREMENT,
    `uuid`          varchar(32)  NOT NULL DEFAULT '',
    `name`          varchar(32)  NOT NULL DEFAULT '',
    `title`         varchar(100) NOT NULL DEFAULT '',
    `remark`        text         NOT NULL,
    `display_order` int(11) NOT NULL DEFAULT '0',
    `is_enable`     tinyint(1) NOT NULL DEFAULT '0',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `is_deleted`    tinyint(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY             `idx_display_order` (`display_order`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COMMENT='工作流组件库 | Workflow Component';


CREATE TABLE `t_workflow_runtime`
(
    `id`            bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uuid`          varchar(32)  NOT NULL DEFAULT '' COMMENT '运行实例唯一标识',
    `user_id`       bigint(20) NOT NULL DEFAULT '0' COMMENT '启动人',
    `workflow_id`   bigint(20) NOT NULL DEFAULT '0' COMMENT '对应工作流定义 id',
    `input`         json                  DEFAULT NULL COMMENT '运行输入，例：{"userInput01":"text01","userInput02":true,"userInput03":10,"userInput04":["selectedA","selectedB"],"userInput05":["https://a.com/a.xlsx","https://a.com/b.png"]}',
    `output`        json                  DEFAULT NULL COMMENT '运行输出，成功或失败的结果',
    `status`        smallint(6) NOT NULL DEFAULT '1' COMMENT '执行状态：1 就绪，2 执行中，3 成功，4 失败',
    `status_remark` varchar(250) NOT NULL DEFAULT '' COMMENT '状态补充说明，如失败原因',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 正常，1 已删',
    PRIMARY KEY (`id`),
    KEY             `idx_workflow_runtime_workflow_id` (`workflow_id`),
    KEY             `idx_workflow_runtime_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=297 DEFAULT CHARSET=utf8mb4 COMMENT='工作流实例（运行时）| Workflow Runtime';


-- workflow
-- 如果不定义输入的变量名，则默认设置为input
-- 如果不定义输出的变量名，则默认设置为output
insert into t_workflow_component(uuid, name, title, remark, is_enable)
values (replace(uuid(), '-', ''), 'Start', '开始', '流程由此开始', true);
insert into t_workflow_component(uuid, name, title, remark, is_enable)
values (replace(uuid(), '-', ''), 'End', '结束', '流程由此结束', true);
insert into t_workflow_component(uuid, name, title, remark, is_enable)
values (replace(uuid(), '-', ''), 'Answer', '生成回答', '调用大语言模型回答问题', true);



INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by,
                      update_time, remark)
VALUES (1976160997656043521, '流程管理', 0, 1, 'flow', '', null, 1, 0, 'M', '0', '0', null, 'ph:user-fill', null, null,
        '2025-10-09 13:41:12', 1, '2025-10-20 20:59:25', '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by,
                      update_time, remark)
VALUES (1976161221409579010, '工作流编排', 1976160997656043521, 0, 'workflow', 'workflow/index', null, 1, 0, 'C', '0',
        '0', null, 'ph:user-fill', null, null, '2025-10-09 13:42:05', 1, '2025-10-20 20:59:16', '');

