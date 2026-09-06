-- =====================================================================
-- IPD 产品经理管理系统 P0 底座表结构（v3 TS-05 全部 26 张实体表）
-- 依据：docs/ipd-系统说明/外部资源/IPD系统_AI开发主Prompt_v3.md TS-05/TS-06
--       + docs/开发说明/开发说明书.md §6（公共字段 §6.3）
-- 适配：docs/ipd-系统说明/type-mapping.md（PG→MySQL：BIGINT 雪花主键 /
--       DATETIME 存 UTC / DECIMAL 金额禁 float / CHAR(1) 布尔 / JSON 数组）
-- 回写：2026-09-05 QA-04-D2——6 幻影列（ai_documents/allowance_ledgers/bonus_pools/kpi_records）
--       + 3 线上漂移列（audit_logs.hash_version、system_configs.source_ref/validation_rule），
--       增量迁移见同目录 2026-09-05-ipd-qa04d2-*.sql
-- 租户：单企业私有部署，全表带 tenant_id 默认 '000000'，统一登记 tenant.excludes
-- 日期：2026-09-04
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. persons 人员（v3 TS-06 全字段；角色固定不可跨 B7；组长随 API 同步 A3）
-- ---------------------------------------------------------------------
create table persons
(
    id               bigint       not null                comment '主键（雪花）',
    name             varchar(64)  not null                comment '姓名',
    employee_no      varchar(64)  null                    comment '工号',
    person_type      varchar(32)  not null                comment '人员类型 MARKET_PM|RD_PM|GROUP_LEADER|SUPER_ADMIN',
    group_id         bigint       null                    comment '所属产品组',
    level            varchar(8)   null                    comment '职级 L1..L5（API 唯一权威源 B6）',
    level_source     varchar(16)  null                    comment '等级来源（仅 API）',
    level_updated_at datetime     null                    comment '等级最近变更时间（B6 新旧额度判定）',
    account_status   varchar(32)  not null default 'ACTIVE' comment '账号状态 ACTIVE|FROZEN_PENDING_HANDOVER|DISABLED|RESIGNED',
    employment_status varchar(16) not null default 'ACTIVE' comment '在职状态 ACTIVE|RESIGNED',
    wecom_user_id    varchar(64)  null                    comment '企微绑定ID',
    wecom_bound_at   datetime     null                    comment '企微绑定时间',
    username         varchar(64)  null                    comment '登录用户名（=姓名）',
    password_hash    varchar(128) null                    comment '密码哈希',
    must_change_pwd  char(1)      not null default '1'    comment '首登强制改密（1是 0否）',
    last_login_at    datetime     null                    comment '最近登录时间',
    create_dept      bigint       null                    comment '创建部门',
    create_by        bigint       null                    comment '创建人',
    create_time      datetime     null default CURRENT_TIMESTAMP comment '创建时间',
    update_by        bigint       null                    comment '更新人',
    update_time      datetime     null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP comment '更新时间',
    tenant_id        varchar(20)  null default '000000'   comment '租户ID',
    del_flag         char(1)      null default '0'        comment '删除标志（0正常 1删除）',
    remark           varchar(500) null                    comment '备注',
    primary key (id),
    unique key uk_persons_employee_no (employee_no),
    unique key uk_persons_username (username)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD 人员（市场PM/研发PM/产品组长/超管）';

-- 2. product_groups 产品组
create table product_groups
(
    id               bigint       not null                comment '主键（雪花）',
    group_name       varchar(64)  not null                comment '产品组名称',
    leader_person_id bigint       null                    comment '组长（随 HR API 同步）',
    parent_id        bigint       null                    comment '上级组（预留）',
    description      varchar(500) null                    comment '描述',
    create_dept      bigint       null, create_by bigint null,
    create_time      datetime     null default CURRENT_TIMESTAMP,
    update_by        bigint       null, update_time datetime null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id        varchar(20)  null default '000000',
    del_flag         char(1)      null default '0',
    remark           varchar(500) null,
    primary key (id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD 产品组（组织架构）';

-- 3. products 产品（与项目 1:1，BR-PROD-01/Q5；三路来源）
create table products
(
    id               bigint       not null                comment '主键（雪花）',
    product_code     varchar(64)  null                    comment '产品编码',
    product_name     varchar(128) not null                comment '产品名称',
    model_code       varchar(64)  null                    comment '在售型号编码（超管导入）',
    source           varchar(32)  not null default 'PM_NEW' comment '来源 ADMIN_IMPORT|PM_NEW|GUEST_OTHER',
    project_id       bigint       null                    comment '关联项目（1:1 唯一）',
    group_id         bigint       null                    comment '归属产品组',
    status           varchar(16)  not null default 'ACTIVE' comment '状态 ACTIVE|INACTIVE',
    create_dept      bigint       null, create_by bigint null,
    create_time      datetime     null default CURRENT_TIMESTAMP,
    update_by        bigint       null, update_time datetime null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id        varchar(20)  null default '000000',
    del_flag         char(1)      null default '0',
    remark           varchar(500) null,
    primary key (id),
    unique key uk_products_project (project_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD 产品（项目与需求上层实体）';

-- 4. projects 项目（v3 TS-06 全字段）
create table projects
(
    id                       bigint        not null       comment '主键（雪花）',
    code                     varchar(32)   not null       comment '项目编码 PRJ-YYYY-NNN 自动生成',
    name                     varchar(128)  not null       comment '项目名称',
    product_id               bigint        not null       comment '归属产品（1:1 唯一，Q5）',
    template_type            varchar(16)   not null       comment '模板类型 HARDWARE|SOFTWARE|SOLUTION',
    target_markets           json          null           comment '目标市场国家/地区代码数组（M1 驱动认证清单）',
    level                    varchar(8)    not null       comment '项目级别 S|A|B',
    level_coefficient        decimal(5,2)  null           comment '差异化系数（立项录入 BR-INC-05）',
    level_coefficient_reason varchar(500)  null           comment '系数定值理由（S/B 必填，写审计）',
    target_sales_amount      decimal(18,2) null           comment '立项目标销售额（奖金池基数 BR-INC-04）',
    target_channel_count     int           null           comment '立项目标渠道商数',
    target_nps               int           null           comment '立项 NPS 目标',
    target_scene_count       int           null           comment '立项目标场景数',
    launch_date              datetime      null           comment '上市日期（后置指标起算原点 BR-IPD-08）',
    current_stage            varchar(16)   not null default 'CONCEPT' comment '当前阶段 CONCEPT|PLAN|DEV|VALID|LAUNCH|LIFECYCLE',
    lifecycle_status         varchar(16)   null           comment '生命周期 ON_SALE|LIMITED|EOL|ARCHIVED',
    source                   varchar(16)   not null default 'NEW' comment '来源 NEW|LEGACY（存量导入）',
    status                   varchar(16)   not null default 'DRAFT' comment '状态 DRAFT|TEAMING|ACTIVE|SUSPENDED|ARCHIVED',
    main_group_id            bigint        null           comment '主组=市场PM 所在产品组（BR-ORG-01）',
    create_dept              bigint        null, create_by bigint null,
    create_time              datetime      null default CURRENT_TIMESTAMP,
    update_by                bigint        null, update_time datetime null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id                varchar(20)   null default '000000',
    del_flag                 char(1)       null default '0',
    remark                   varchar(500)  null,
    primary key (id),
    unique key uk_projects_code (code),
    unique key uk_projects_product (product_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD 项目（核心实体）';

-- 5. project_members 项目成员（双PM 绑定 + 评级快照；v3 TS-06）
create table project_members
(
    id             bigint        not null       comment '主键（雪花）',
    project_id     bigint        not null       comment '项目',
    person_id      bigint        not null       comment '人员',
    role           varchar(16)   not null       comment '角色 MARKET_PM|RD_PM（固定不可跨 B7）',
    locked_level   varchar(8)    not null       comment '绑定时评级快照（BR-INC-02）',
    locked_amount  decimal(10,2) not null       comment '锁定月度津贴额',
    join_date      datetime      not null       comment '加入日期',
    exit_date      datetime      null           comment '退出日期',
    exit_reason    varchar(16)   null           comment '退出原因 TRANSFER|VOLUNTARY|LOW_PERF',
    bonus_eligible char(1)       not null default '1' comment '奖金资格（放弃置 0，BR-INC-09）',
    create_dept    bigint        null, create_by bigint null,
    create_time    datetime      null default CURRENT_TIMESTAMP,
    update_by      bigint        null, update_time datetime null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id      varchar(20)   null default '000000',
    del_flag       char(1)       null default '0',
    remark         varchar(500)  null,
    primary key (id),
    key idx_pm_project (project_id),
    key idx_pm_person (person_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD 项目成员（双PM 绑定+评级快照）';

-- 6. project_stages 六阶段实例
create table project_stages
(
    id            bigint      not null comment '主键（雪花）',
    project_id    bigint      not null comment '项目',
    stage_code    varchar(16) not null comment '阶段 CONCEPT|PLAN|DEV|VALID|LAUNCH|LIFECYCLE',
    stage_name    varchar(64) null     comment '阶段名（概念/计划/开发/验证/发布/生命周期）',
    sort_order    int         not null default 0 comment '顺序',
    status        varchar(16) not null default 'NOT_STARTED' comment '状态 NOT_STARTED|IN_PROGRESS|DONE',
    gate_id       bigint      null     comment '出口 Gate 实例',
    started_at    datetime    null,
    completed_at  datetime    null,
    create_dept   bigint null, create_by bigint null,
    create_time   datetime null default CURRENT_TIMESTAMP,
    update_by     bigint null, update_time datetime null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id     varchar(20) null default '000000',
    del_flag      char(1) null default '0',
    remark        varchar(500) null,
    primary key (id),
    key idx_stages_project (project_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD 项目六阶段实例';

-- 7. stage_actions 阶段动作实例（69 动作；v3 TS-06 全字段）
create table stage_actions
(
    id             bigint        not null comment '主键（雪花）',
    project_id     bigint        not null comment '项目',
    stage_id       bigint        not null comment '阶段实例',
    action_code    varchar(8)    not null comment '动作编号 C01/P01/D05/C12/D11/V10...（69 个）',
    action_name    varchar(128)  not null comment '动作名称',
    owner_role     varchar(16)   not null comment '责任角色 MARKET_PM|RD_PM|BOTH',
    depth          varchar(8)    not null comment '管理深度 DEEP|LIGHT（BR-IPD-03/04）',
    status         varchar(16)   not null default 'NOT_STARTED' comment '状态 NOT_STARTED|IN_PROGRESS|DONE|DELAYED|NA',
    is_blocking    char(1)       not null default '0' comment '是否阻断跳阶（1是）',
    actual_done_at datetime      null     comment '实际完成时间（轻管核心字段 BR-IPD-05）',
    far_value      decimal(10,6) null     comment 'D11/Z01 BioCV 误识率 FAR',
    frr_value      decimal(10,6) null     comment 'D11/Z01 BioCV 拒识率 FRR',
    cert_no        varchar(64)   null     comment 'V02 认证证书编号',
    cert_passed_at datetime      null     comment 'V02 认证通过日期',
    algo_type      varchar(16)   null     comment 'BioCV 算法 FINGERPRINT|FACE|PALM|VEIN|MULTI',
    is_bio_feature char(1)       not null default '0' comment '涉生物特征（驱动 C12 强制挂载）',
    due_date       datetime      null,
    sop_id         bigint        null,
    create_dept    bigint null, create_by bigint null,
    create_time    datetime null default CURRENT_TIMESTAMP,
    update_by      bigint null, update_time datetime null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id      varchar(20) null default '000000',
    del_flag       char(1) null default '0',
    remark         varchar(500) null,
    primary key (id),
    key idx_sa_project (project_id),
    key idx_sa_stage (stage_id),
    key idx_sa_code (action_code)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD 阶段动作实例（69 动作，深管/轻管）';

-- 8. deliverables 交付物
create table deliverables
(
    id           bigint       not null comment '主键（雪花）',
    action_id    bigint       not null comment '所属动作实例',
    project_id   bigint       not null comment '项目（冗余，便于查询）',
    file_name    varchar(255) not null comment '文件名',
    oss_id       bigint       null     comment 'OSS 存储 ID（MinIO，ruoyi-common-oss）',
    file_url     varchar(500) null,
    file_size    bigint       null,
    uploaded_by  bigint       null,
    uploaded_at  datetime     null,
    create_dept  bigint null, create_by bigint null,
    create_time  datetime null default CURRENT_TIMESTAMP,
    update_by    bigint null, update_time datetime null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id    varchar(20) null default '000000',
    del_flag     char(1) null default '0',
    remark       varchar(500) null,
    primary key (id),
    key idx_deliv_action (action_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD 交付物（附件关联）';

-- 9. sop_templates SOP 模板
create table sop_templates
(
    id           bigint        not null comment '主键（雪花）',
    action_code  varchar(8)    not null comment '绑定动作编号（深管 42 动作）',
    title        varchar(128)  not null,
    content      mediumtext    null     comment 'SOP 富文本',
    version      int           not null default 1 comment '版本',
    status       varchar(16)   not null default 'DRAFT' comment '状态 DRAFT|PUBLISHED|ARCHIVED',
    create_dept  bigint null, create_by bigint null,
    create_time  datetime null default CURRENT_TIMESTAMP,
    update_by    bigint null, update_time datetime null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id    varchar(20) null default '000000',
    del_flag     char(1) null default '0',
    remark       varchar(500) null,
    primary key (id),
    key idx_sop_action (action_code)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD SOP 模板（标准作业程序+版本）';

-- 10. gate_review_elements Gate 评审要素定义（33 项，超管可增删改）
create table gate_review_elements
(
    id               bigint       not null comment '主键（雪花）',
    gate_code        varchar(8)   not null comment '适用 Gate G1..G5',
    element_code     varchar(16)  not null comment '要素编号',
    element_name     varchar(128) not null comment '要素名',
    pass_standard    text         null     comment '通过标准',
    is_veto          char(1)      not null default '0' comment '是否否决项（14 项，命中无法提交通过）',
    sort_order       int          not null default 0,
    enabled          char(1)      not null default '1',
    create_dept      bigint null, create_by bigint null,
    create_time      datetime null default CURRENT_TIMESTAMP,
    update_by        bigint null, update_time datetime null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id        varchar(20) null default '000000',
    del_flag         char(1) null default '0',
    remark           varchar(500) null,
    primary key (id),
    key idx_gre_gate (gate_code)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD Gate 评审要素定义（33 项+14 否决项）';

-- 11. gate_element_results Gate 要素判定
create table gate_element_results
(
    id               bigint       not null comment '主键（雪花）',
    gate_id          bigint       not null comment 'Gate 实例',
    element_id       bigint       not null comment '要素定义',
    result           varchar(16)  not null comment '判定 PASS|CONDITIONAL|FAIL（✅/⚠️/❌）',
    condition_note   varchar(500) null     comment '带条件通过说明',
    leftover_item    varchar(500) null     comment '遗留项跟踪',
    leftover_due_at  datetime     null,
    leftover_status  varchar(16)  null     comment '遗留项状态 OPEN|CLOSED',
    create_dept      bigint null, create_by bigint null,
    create_time      datetime null default CURRENT_TIMESTAMP,
    update_by        bigint null, update_time datetime null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id        varchar(20) null default '000000',
    del_flag         char(1) null default '0',
    remark           varchar(500) null,
    primary key (id),
    key idx_ger_gate (gate_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD Gate 要素逐项判定（含遗留项跟踪）';

-- 12. cert_templates 国别认证清单模板库（M1）
create table cert_templates
(
    id              bigint       not null comment '主键（雪花）',
    country_code    varchar(8)   not null comment '国家/地区代码（如 SA）',
    country_name    varchar(64)  not null comment '国家名（如 沙特阿拉伯）',
    cert_name       varchar(128) not null comment '认证名（如 SABER）',
    cert_authority  varchar(128) null,
    requirement_desc varchar(500) null,
    is_mandatory    char(1)      not null default '1',
    create_dept     bigint null, create_by bigint null,
    create_time     datetime null default CURRENT_TIMESTAMP,
    update_by       bigint null, update_time datetime null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id       varchar(20) null default '000000',
    del_flag        char(1) null default '0',
    remark          varchar(500) null,
    primary key (id),
    key idx_ct_country (country_code)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD 国别认证清单模板库（目标市场自动带出）';

-- 13. gates Gate 实例（五大联合 Gate）
create table gates
(
    id               bigint        not null comment '主键（雪花）',
    project_id       bigint        not null comment '项目',
    gate_code        varchar(8)    not null comment 'G1..G5',
    status           varchar(24)   not null default 'PENDING' comment '状态 PENDING|APPROVED|REJECTED|ABSTAINED_TIMEOUT',
    current_round    int           not null default 1 comment '当前评审轮次（BR-GATE-06：第3轮组长列席/第5轮超管介入）',
    gate_coefficient decimal(5,2)  null     comment 'Gate 系数（G1 双签决定，bonus.coefficientDecider）',
    planned_at       datetime      null,
    started_at       datetime      null,
    concluded_at     datetime      null,
    create_dept      bigint null, create_by bigint null,
    create_time      datetime null default CURRENT_TIMESTAMP,
    update_by        bigint null, update_time datetime null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id        varchar(20) null default '000000',
    del_flag         char(1) null default '0',
    remark           varchar(500) null,
    primary key (id),
    key idx_gates_project (project_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD Gate 实例（五大联合评审）';

-- 14. gate_reviews Gate 双签记录（v3 TS-06 全字段）
create table gate_reviews
(
    id            bigint       not null comment '主键（雪花）',
    gate_id       bigint       not null comment 'Gate 实例',
    reviewer_type varchar(16)  not null comment '签署方 MARKET_PM|RD_PM|GROUP_LEADER|SUPER_ADMIN',
    reviewer_id   bigint       not null,
    decision      varchar(16)  null     comment '决定 APPROVE|REJECT|ABSTAIN',
    opinion       varchar(1000) null    comment '意见（双方都提交前互不可见）',
    signed_at     datetime     null,
    due_at        datetime     not null comment '签署期限（BR-GATE-04，3 天超时弃权）',
    round         int          not null default 1 comment '评审轮次',
    create_dept   bigint null, create_by bigint null,
    create_time   datetime null default CURRENT_TIMESTAMP,
    update_by     bigint null, update_time datetime null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id     varchar(20) null default '000000',
    del_flag      char(1) null default '0',
    remark        varchar(500) null,
    primary key (id),
    unique key uk_gr_gate_type_round (gate_id, reviewer_type, round)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD Gate 评审双签记录（每方一条）';

-- 15. requirements 需求池
create table requirements
(
    id             bigint        not null comment '主键（雪花）',
    product_id     bigint        null     comment '产品（游客「其他」可空路由）',
    project_id     bigint        null,
    source         varchar(16)   not null default 'PORTAL_GUEST' comment '来源 PORTAL_GUEST|INTERNAL',
    submitter_name varchar(64)   null,
    contact        varchar(128)  null,
    title          varchar(200)  not null,
    content        text          null,
    query_code     varchar(32)   not null comment '查询码（游客凭码查进度）',
    status         varchar(16)   not null default 'SUBMITTED' comment 'SUBMITTED|ACCEPTED|EVALUATING|SCHEDULED|PROCESSING|CLOSED|ARCHIVED',
    market_pm_id   bigint        null,
    rd_pm_id       bigint        null,
    routed_at      datetime      null     comment '按产品路由双PM 时间',
    create_dept    bigint null, create_by bigint null,
    create_time    datetime null default CURRENT_TIMESTAMP,
    update_by      bigint null, update_time datetime null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id      varchar(20) null default '000000',
    del_flag       char(1) null default '0',
    remark         varchar(500) null,
    primary key (id),
    unique key uk_req_query_code (query_code),
    key idx_req_product (product_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD 需求池（免登录提交+查询码）';

-- 16. requirement_changes 需求变更单（双签否决对象 BR-GATE-07）
create table requirement_changes
(
    id             bigint       not null comment '主键（雪花）',
    requirement_id bigint       not null,
    project_id     bigint       null,
    change_type    varchar(32)  null,
    before_snapshot json        null,
    after_snapshot  json        null,
    reason         varchar(500) null,
    status         varchar(16)  not null default 'DRAFT' comment 'DRAFT|PENDING_SIGN|APPROVED|REJECTED',
    create_dept    bigint null, create_by bigint null,
    create_time    datetime null default CURRENT_TIMESTAMP,
    update_by      bigint null, update_time datetime null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id      varchar(20) null default '000000',
    del_flag       char(1) null default '0',
    remark         varchar(500) null,
    primary key (id),
    key idx_rc_requirement (requirement_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD 需求变更单（双签否决）';

-- 17. bid_invitations 招标单（BR-TEAM）
create table bid_invitations
(
    id                  bigint       not null comment '主键（雪花）',
    project_id          bigint       not null,
    mode                varchar(16)  not null default 'PUBLIC' comment '招标方式 ONE_TO_ONE|PUBLIC',
    target_person_id    bigint       null     comment '一对一指定研发PM',
    title               varchar(128) not null,
    content             text         null,
    expire_at           datetime     null     comment '有效期（bid.expireWarnDays=3 预警）',
    status              varchar(16)  not null default 'OPEN' comment 'OPEN|SELECTED|EXPIRED|CLOSED',
    selected_response_id bigint      null,
    create_dept         bigint null, create_by bigint null,
    create_time         datetime null default CURRENT_TIMESTAMP,
    update_by           bigint null, update_time datetime null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id           varchar(20) null default '000000',
    del_flag            char(1) null default '0',
    remark              varchar(500) null,
    primary key (id),
    key idx_bi_project (project_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD 招标单（组队招标）';

-- 18. bid_responses 应标记录（不留痕的拒绝不记录）
create table bid_responses
(
    id             bigint       not null comment '主键（雪花）',
    invitation_id  bigint       not null,
    rd_pm_id       bigint       null     comment '研发PM ID（应标时可为空，遴选后回填）',
    response_note  varchar(500) null,
    status         varchar(16)  not null default 'PENDING' comment 'PENDING|ACCEPTED|REJECTED|WITHDRAWN',
    responded_at   datetime     null,
    create_dept    bigint null, create_by bigint null,
    create_time    datetime null default CURRENT_TIMESTAMP,
    update_by      bigint null, update_time datetime null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id      varchar(20) null default '000000',
    del_flag       char(1) null default '0',
    remark         varchar(500) null,
    primary key (id),
    key idx_br_invitation (invitation_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD 应标记录（研发PM 应标）';

-- 19. deletion_requests 删除申请（差异化审核 BR-DEL，2+2 工作日）
create table deletion_requests
(
    id                bigint       not null comment '主键（雪花）',
    entity_type       varchar(32)  not null comment '目标实体类型',
    entity_id         bigint       not null comment '目标实体ID',
    entity_snapshot   json         null     comment '删除前快照',
    reason            varchar(500) not null comment '删除理由',
    requester_id      bigint       not null,
    status            varchar(24)  not null default 'DRAFT' comment 'DRAFT|LEADER_REVIEW|ADMIN_REVIEW|DELETED|REJECTED',
    leader_id         bigint       null,
    leader_decision   varchar(16)  null     comment 'APPROVE|REJECT',
    leader_decided_at datetime     null,
    leader_due_at     datetime     null     comment '组长审核期限（deletion.leaderDeadlineDays=2 工作日）',
    admin_id          bigint       null,
    admin_decision    varchar(16)  null,
    admin_decided_at  datetime     null,
    admin_due_at      datetime     null     comment '超管审核期限（deletion.adminDeadlineDays=2 工作日）',
    executed_at       datetime     null     comment '实际执行软删除时间',
    create_dept       bigint null, create_by bigint null,
    create_time       datetime null default CURRENT_TIMESTAMP,
    update_by         bigint null, update_time datetime null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id         varchar(20) null default '000000',
    del_flag          char(1) null default '0',
    remark            varchar(500) null,
    primary key (id),
    key idx_dr_entity (entity_type, entity_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD 删除申请（两级审核，禁直接物理删除 G-02）';

-- 20. handover_records 移交记录（项目移交+超管移交 BR-HAND/BR-ADM）
create table handover_records
(
    id              bigint       not null comment '主键（雪花）',
    handover_type   varchar(16)  not null comment '类型 PROJECT|SUPER_ADMIN|BATCH',
    from_person_id  bigint       not null,
    to_person_id    bigint       null     comment '承接人',
    project_id      bigint       null,
    scope           json         null     comment '移交范围（角色独立移交：市场PM/研发PM 各自数据跟随）',
    status          varchar(16)  not null default 'DRAFT' comment 'DRAFT|CONFIRMED|COMPLETED',
    confirmed_at    datetime     null,
    completed_at    datetime     null,
    create_dept     bigint null, create_by bigint null,
    create_time     datetime null default CURRENT_TIMESTAMP,
    update_by       bigint null, update_time datetime null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id       varchar(20) null default '000000',
    del_flag        char(1) null default '0',
    remark          varchar(500) null,
    primary key (id),
    key idx_hr_from (from_person_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD 移交记录（先移交后禁用 BR-HAND）';

-- 21. audit_logs 审计日志（只追加 + hash 链，v3 TS-06/TS-08）
-- ⚠️ 例外：无 update_time / 无 del_flag / 无业务更新（只追加，破坏即校验失败 AC-AUD-01）
create table audit_logs
(
    id            bigint      not null comment '主键（雪花）',
    seq           bigint      not null auto_increment comment '全局递增序号（hash 链顺序锚）',
    operator_id   bigint      null,
    operator_name varchar(64) null,
    operator_role varchar(32) null,
    action        varchar(32) not null comment 'CREATE|UPDATE|DELETE|APPROVE|REJECT|HANDOVER|LOGIN...',
    entity_type   varchar(32) not null,
    entity_id     bigint      null,
    before_data   json        null,
    after_data    json        null,
    reason        varchar(500) null,
    prev_hash     char(64)    not null comment '前条 hash（链首为 64 个 0）',
    curr_hash     char(64)    not null comment 'SHA256(prevHash+本条内容)',
    ip_address    varchar(64) null,
    tenant_id     varchar(20) null default '000000',
    create_time   datetime    null default CURRENT_TIMESTAMP comment '创建时间（唯一时间字段，只追加）',
    hash_version  int         null     comment 'NULL=legacy-v1,2=canonical-json-v2（QA-04-D2 回写线上形态）',
    primary key (id),
    unique key uk_audit_seq (seq)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD 审计日志（只追加+SHA256 hash 链，AC-AUD-01）';

-- 22. kpi_records KPI 记录
create table kpi_records
(
    id                  bigint        not null comment '主键（雪花）',
    project_id          bigint        null,
    person_id           bigint        not null,
    kpi_type            varchar(16)   not null comment 'FUNCTIONAL|SHARED（共担四项归集由产品组长）',
    period              varchar(7)    not null comment '考核周期 YYYY-MM（每月 5 日截止 kpi.monthlyDeadlineDay）',
    functional_score    decimal(5,2)  null,
    shared_detail       json          null     comment '共担四项明细',
    comprehensive_score decimal(5,2)  null     comment '综合得分（kpi.functionalWeight=0.6/0.4）',
    segment             varchar(16)   null     comment '在研分段',
    scored_by           bigint        null,
    scored_at           datetime      null     comment '评分日期（QA-04-D2 补列）',
    status              varchar(16)   not null default 'DRAFT',
    create_dept         bigint null, create_by bigint null,
    create_time         datetime null default CURRENT_TIMESTAMP,
    update_by           bigint null, update_time datetime null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id           varchar(20) null default '000000',
    del_flag            char(1) null default '0',
    remark              varchar(500) null,
    primary key (id),
    key idx_kpi_person_period (person_id, period)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD KPI 记录（功能+共担）';

-- 23. allowance_ledgers 月度津贴台账（BR-INC-02/03）
create table allowance_ledgers
(
    id             bigint        not null comment '主键（雪花）',
    person_id      bigint        not null,
    project_id     bigint        not null,
    month          varchar(7)    not null comment '台账月份 YYYY-MM',
    locked_level   varchar(8)    not null comment '评级（绑定时锁定）',
    base_amount    decimal(10,2) not null comment 'allowance.L1..L5 基准',
    final_amount   decimal(10,2) not null comment '终额（多项目叠加、2 倍封顶 capMultiplier）',
    cap_applied    char(1)       not null default '0' comment '是否触发封顶',
    stop_reason    varchar(255)  null     comment '停发原因（<60 分/无产出 noOutput.days=60）',
    stop_start_date datetime     null     comment '停发开始日期（QA-04-D2 补列）',
    create_dept    bigint null, create_by bigint null,
    create_time    datetime null default CURRENT_TIMESTAMP,
    update_by      bigint null, update_time datetime null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id      varchar(20) null default '000000',
    del_flag       char(1) null default '0',
    remark         varchar(500) null,
    primary key (id),
    key idx_al_person_month (person_id, month)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD 月度津贴台账（锁定评级/叠加/封顶/停发）';

-- 24. bonus_pools 项目奖金池（BR-INC-04~09，涉钱必须 TDD）
create table bonus_pools
(
    id                bigint        not null comment '主键（雪花）',
    project_id        bigint        not null comment '项目（1:1）',
    target_sales      decimal(18,2) not null comment '目标销售额（回款口径 salesSource=RECEIPT）',
    pool_rate         decimal(5,4)  not null default 0.0500 comment '奖金池比例（bonus.poolRate）',
    base_pool         decimal(18,2) null     comment '基础奖金池=目标销售额×5%',
    coefficient       decimal(5,2)  null     comment '项目系数（S/A/B，G1 双签决定）',
    achievement_rate  decimal(5,2)  null     comment '回款达成率%',
    tier_coefficient  decimal(5,2)  null     comment '达成率 6 档阶梯系数',
    final_pool        decimal(18,2) null     comment '最终奖金池',
    distributions     json          null     comment '个人分配结果（五维贡献+绩效系数）',
    status            varchar(16)   not null default 'DRAFT' comment 'DRAFT|CONFIRMED|DISTRIBUTED',
    calculated_at     datetime      null     comment '计算完成时间（QA-04-D2 补列）',
    distributed_at    datetime      null     comment '发放时间（QA-04-D2 补列）',
    create_dept       bigint null, create_by bigint null,
    create_time       datetime null default CURRENT_TIMESTAMP,
    update_by         bigint null, update_time datetime null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id         varchar(20) null default '000000',
    del_flag          char(1) null default '0',
    remark            varchar(500) null,
    primary key (id),
    unique key uk_bp_project (project_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD 项目奖金池（目标销售额×5%×系数→6档阶梯→分配）';

-- 25. ai_documents AI 文档（BR-AI，版本链）
create table ai_documents
(
    id                bigint       not null comment '主键（雪花）',
    project_id        bigint       not null,
    doc_type          varchar(32)  null,
    title             varchar(200) not null,
    content           mediumtext   null,
    model             varchar(64)  null     comment '生成模型',
    token_prompt      int          null,
    token_completion  int          null,
    status            varchar(16)  not null default 'GENERATED' comment 'GENERATED|REVIEWED|ARCHIVED（未审核不可归档）',
    parent_version_id bigint       null     comment '版本链父文档',
    version_no        int          not null default 1,
    reviewed_by       bigint       null     comment '审核人ID（QA-04-D2 补列）',
    reviewed_at       datetime     null     comment '审核时间（QA-04-D2 补列）',
    create_dept       bigint null, create_by bigint null,
    create_time       datetime null default CURRENT_TIMESTAMP,
    update_by         bigint null, update_time datetime null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id         varchar(20) null default '000000',
    del_flag          char(1) null default '0',
    remark            varchar(500) null,
    primary key (id),
    key idx_ai_project (project_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD AI 文档（生成+人工审核+版本链+token 统计）';

-- 26. system_configs 系统参数（G-05 全部可配置，禁止硬编码；种子见同目录 seed 脚本）
create table system_configs
(
    id            bigint       not null comment '主键（雪花）',
    config_key    varchar(64)  not null comment '参数键（如 bonus.salesSource / allowance.L3 / gate.signDeadlineDays）',
    config_value  varchar(500) not null comment '参数值',
    value_type    varchar(16)  not null default 'STRING' comment 'STRING|NUMBER|JSON|BOOL',
    default_value varchar(500) null     comment '出厂默认（便于重置）',
    description   varchar(255) null,
    create_dept   bigint null, create_by bigint null,
    create_time   datetime null default CURRENT_TIMESTAMP,
    update_by     bigint null, update_time datetime null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id     varchar(20) null default '000000',
    del_flag      char(1) null default '0',
    remark        varchar(500) null,
    validation_rule json      null     comment '校验规则（QA-04-D2 回写线上形态）',
    source_ref    varchar(1024) null    comment '来源引用（QA-04-D2 回写线上形态）',
    primary key (id),
    unique key uk_sc_key (config_key)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD 系统参数（6 项涉钱参数等全部可配置 G-05）';