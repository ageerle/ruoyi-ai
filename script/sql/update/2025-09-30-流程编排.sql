CREATE TABLE t_workflow
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid        VARCHAR(32)  NOT NULL DEFAULT '',
    title       VARCHAR(100) NOT NULL DEFAULT '',
    remark      TEXT         NOT NULL,
    user_id     BIGINT       NOT NULL DEFAULT 0,
    is_public   TINYINT(1) NOT NULL DEFAULT 0,
    is_enable   TINYINT(1) NOT NULL DEFAULT 1,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted  TINYINT(1) NOT NULL DEFAULT 0
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='工作流定义（用户定义的工作流）| Workflow Definition';


CREATE TABLE t_workflow_node
(
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid                  VARCHAR(32)  NOT NULL DEFAULT '',
    workflow_id           BIGINT       NOT NULL DEFAULT 0,
    workflow_component_id BIGINT       NOT NULL DEFAULT 0,
    user_id               BIGINT       NOT NULL DEFAULT 0,
    title                 VARCHAR(100) NOT NULL DEFAULT '',
    remark                VARCHAR(500) NOT NULL DEFAULT '',
    input_config          JSON         NOT NULL DEFAULT ('{}'),
    node_config           JSON         NOT NULL DEFAULT ('{}'),
    position_x            DOUBLE       NOT NULL DEFAULT 0,
    position_y            DOUBLE       NOT NULL DEFAULT 0,
    create_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted            TINYINT(1) NOT NULL DEFAULT 0,
    INDEX                 idx_workflow_node_workflow_id (workflow_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='工作流定义的节点 | Node of Workflow Definition';


CREATE TABLE t_workflow_edge
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid             VARCHAR(32) NOT NULL DEFAULT '',
    workflow_id      BIGINT      NOT NULL DEFAULT 0,
    source_node_uuid VARCHAR(32) NOT NULL DEFAULT '',
    source_handle    VARCHAR(32) NOT NULL DEFAULT '',
    target_node_uuid VARCHAR(32) NOT NULL DEFAULT '',
    create_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted       TINYINT(1) NOT NULL DEFAULT 0,
    INDEX            idx_workflow_edge_workflow_id (workflow_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;


CREATE TABLE t_workflow_runtime
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid          VARCHAR(32)  NOT NULL DEFAULT '',
    user_id       BIGINT       NOT NULL DEFAULT 0,
    workflow_id   BIGINT       NOT NULL DEFAULT 0,
    input         JSON         NOT NULL DEFAULT ('{}'),
    output        JSON         NOT NULL DEFAULT ('{}'),
    status        SMALLINT     NOT NULL DEFAULT 1 COMMENT '执行状态，1：就绪，2：执行中，3：成功，4：失败',
    status_remark VARCHAR(250) NOT NULL DEFAULT '' COMMENT '状态备注',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted    TINYINT(1) NOT NULL DEFAULT 0,
    INDEX         idx_workflow_runtime_workflow_id (workflow_id),
    INDEX         idx_workflow_runtime_user_id (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='工作流实例（运行时）| Workflow Runtime';


CREATE TABLE t_workflow_runtime_node
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid                VARCHAR(32)  NOT NULL DEFAULT '',
    user_id             BIGINT       NOT NULL DEFAULT 0,
    workflow_runtime_id BIGINT       NOT NULL DEFAULT 0,
    node_id             BIGINT       NOT NULL DEFAULT 0,
    input               JSON         NOT NULL DEFAULT ('{}'),
    output              JSON         NOT NULL DEFAULT ('{}'),
    status              SMALLINT     NOT NULL DEFAULT 1 COMMENT '执行状态，1：进行中，2：失败，3：成功',
    status_remark       VARCHAR(250) NOT NULL DEFAULT '' COMMENT '状态备注',
    create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted          TINYINT(1) NOT NULL DEFAULT 0,
    INDEX               idx_runtime_node_runtime_id (workflow_runtime_id),
    INDEX               idx_runtime_node_node_id (node_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT ='工作流实例（运行时）- 节点 | Workflow Runtime Node';


CREATE TABLE t_workflow_component
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid          VARCHAR(32)  DEFAULT ''                NOT NULL,
    name          VARCHAR(32)  DEFAULT ''                NOT NULL,
    title         VARCHAR(100) DEFAULT ''                NOT NULL,
    remark        TEXT                                   NOT NULL,
    display_order INT          DEFAULT 0                 NOT NULL,
    is_enable     TINYINT(1) DEFAULT 0 NOT NULL,
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted    TINYINT(1) DEFAULT 0 NOT NULL,
    INDEX         idx_display_order (display_order)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
    COMMENT '工作流组件库 | Workflow Component';


-- workflow
-- 如果不定义输入的变量名，则默认设置为input
-- 如果不定义输出的变量名，则默认设置为output
insert into t_workflow_component(uuid, name, title, remark, is_enable)
values (replace(uuid(), '-', ''), 'Start', '开始', '流程由此开始', true);
insert into t_workflow_component(uuid, name, title, remark, is_enable)
values (replace(uuid(), '-', ''), 'End', '结束', '流程由此结束', true);
insert into t_workflow_component(uuid, name, title, remark, is_enable)
values (replace(uuid(), '-', ''), 'Answer', '生成回答', '调用大语言模型回答问题', true);
insert into t_workflow_component(uuid, name, title, remark, display_order, is_enable)
values (replace(uuid(), '-', ''), 'Dalle3', 'DALL-E 3 画图', '调用Dall-e-3生成图片', 11, false);
insert into t_workflow_component(uuid, name, title, remark, display_order, is_enable)
values (replace(uuid(), '-', ''), 'DocumentExtractor', '文档提取', '从文档中提取信息', 4, false);
insert into t_workflow_component(uuid, name, title, remark, display_order, is_enable)
values (replace(uuid(), '-', ''), 'KeywordExtractor', '关键词提取',
        '从内容中提取关键词，Top N指定需要提取的关键词数量', 5, false);
insert into t_workflow_component(uuid, name, title, remark, is_enable)
values (replace(uuid(), '-', ''), 'KnowledgeRetrieval', '知识检索', '从知识库中检索信息，需选中知识库',
        true);
insert into t_workflow_component(uuid, name, title, remark, is_enable)
values (replace(uuid(), '-', ''), 'Switcher', '条件分支', '根据设置的条件引导执行不同的流程', false);
insert into t_workflow_component(uuid, name, title, remark, is_enable)
values (replace(uuid(), '-', ''), 'Classifier', '内容归类',
        '使用大语言模型对输入信息进行分析并归类，根据类别调用对应的下游节点', false);
insert into t_workflow_component(uuid, name, title, remark, display_order, is_enable)
values (replace(uuid(), '-', ''), 'Template', '模板转换',
        '将多个变量合并成一个输出内容', 10, true);
insert into t_workflow_component(uuid, name, title, remark, display_order, is_enable)
values (replace(uuid(), '-', ''), 'Google', 'Google搜索', '从Google中检索信息', 13, false);
insert into t_workflow_component(uuid, name, title, remark, display_order, is_enable)
values (replace(uuid(), '-', ''), 'FaqExtractor', '常见问题提取',
        '从内容中提取出常见问题及对应的答案，Top N为提取的数量',
        6, false);
insert into t_workflow_component(uuid, name, title, remark, display_order, is_enable)
values (replace(uuid(), '-', ''), 'Tongyiwanx', '通义万相-画图', '调用文生图模型生成图片', 12, false);
insert into t_workflow_component(uuid, name, title, remark, display_order, is_enable)
values (replace(uuid(), '-', ''), 'HumanFeedback', '人机交互',
        '中断执行中的流程并等待用户的输入，用户输入后继续执行后续流程', 10, false);
insert into t_workflow_component(uuid, name, title, remark, display_order, is_enable)
values (replace(uuid(), '-', ''), 'MailSend', '邮件发送', '发送邮件到指定邮箱', 10, false);
insert into t_workflow_component(uuid, name, title, remark, display_order, is_enable)
values (replace(uuid(), '-', ''), 'HttpRequest', 'Http请求',
        '通过Http协议发送请求，可将其他组件的输出作为参数，也可设置常量作为参数。', 10, false);


INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (1976160997656043521, '流程管理', 0, 1, 'flow', '', null, 1, 0, 'M', '0', '0', null, 'ph:user-fill', null, null, '2025-10-09 13:41:12', 1, '2025-10-20 20:59:25', '');
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark) VALUES (1976161221409579010, '工作流编排', 1976160997656043521, 0, 'workflow', 'workflow/index', null, 1, 0, 'C', '0', '0', null, 'ph:user-fill', null, null, '2025-10-09 13:42:05', 1, '2025-10-20 20:59:16', '');

