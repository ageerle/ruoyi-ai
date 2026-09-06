-- =====================================================================
-- P0 域#35 项目协作圈（ZK-D2+ 后端真实现，对齐原型 project_followers /
-- project_circle_posts / project_circle_comments）
-- 3 新表：圈成员（FOLLOWER|COMMENTER|CONTRIBUTOR）/ 动态（2-3000 字，可关联业务对象）/
--   评论（2-2000 字，parentId 楼中楼）
-- 语义：可见性=项目成员或圈成员；管理人=超管/市场PM负责人/主组组长；ARCHIVED 只读
-- 幂等：create table if not exists；日期：2026-09-06；隔离库人工 apply
-- =====================================================================

create table if not exists project_followers
(
    id          bigint       not null comment '主键（雪花）',
    project_id  bigint       not null comment '项目',
    user_id     bigint       not null comment '人员（persons.id）',
    circle_role varchar(16)  not null default 'FOLLOWER' comment '圈角色 FOLLOWER|COMMENTER|CONTRIBUTOR',
    added_by    bigint       null comment '添加人',
    create_dept bigint null, create_by bigint null,
    create_time datetime null default CURRENT_TIMESTAMP,
    update_by   bigint null, update_time datetime null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id   varchar(20) null default '000000',
    del_flag    char(1) null default '0',
    remark      varchar(500) null,
    primary key (id),
    unique key uk_followers_project_user (project_id, user_id),
    key idx_followers_user (user_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD 项目协作圈成员';

create table if not exists project_circle_posts
(
    id          bigint        not null comment '主键（雪花）',
    project_id  bigint        not null comment '项目',
    author_id   bigint        not null comment '作者（persons.id）',
    object_type varchar(40)   null comment '关联对象类型（需求/变更/闸门等，可空）',
    object_id   bigint        null comment '关联对象ID（可空）',
    content     varchar(3000) not null comment '动态内容（2-3000 字）',
    create_dept bigint null, create_by bigint null,
    create_time datetime null default CURRENT_TIMESTAMP,
    update_by   bigint null, update_time datetime null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id   varchar(20) null default '000000',
    del_flag    char(1) null default '0',
    remark      varchar(500) null,
    primary key (id),
    key idx_pcp_project (project_id, create_time),
    key idx_pcp_author (author_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD 项目协作圈动态';

create table if not exists project_circle_comments
(
    id          bigint        not null comment '主键（雪花）',
    post_id     bigint        not null comment '动态（project_circle_posts.id）',
    author_id   bigint        not null comment '作者（persons.id）',
    parent_id   bigint        null comment '父评论（楼中楼，可空）',
    content     varchar(2000) not null comment '评论内容（2-2000 字）',
    create_dept bigint null, create_by bigint null,
    create_time datetime null default CURRENT_TIMESTAMP,
    update_by   bigint null, update_time datetime null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id   varchar(20) null default '000000',
    del_flag    char(1) null default '0',
    remark      varchar(500) null,
    primary key (id),
    key idx_pcc_post (post_id, create_time),
    key idx_pcc_author (author_id)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='IPD 项目协作圈评论（楼中楼）';

-- 回读校验（apply 后人工核对三表在位）
show tables like 'project_%circle%';
show tables like 'project_followers';
