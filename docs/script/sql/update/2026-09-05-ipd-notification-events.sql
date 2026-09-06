-- =====================================================================
-- OPS-05 站内通知与可执行待办事件接口（AC-TEAM-01/05/06/07/13、AC-GATE-05/09/13/17、AC-DEL-04/05/07）
-- 单表 outbox：发布（publish，幂等）与投递（dispatchPending，可轮询消费端）解耦；
--   kind=FYI（跨组知会）/ ACTION（可执行审批行动）分流；
--   delivery_status: PENDING→SENT；失败退避重试（retry_count，5·2^(n-1) 分钟）至 DEAD；
--   channel 一期固定 MOCK（勿当真实发送），真实渠道后续卡新增 NotificationChannel 实现。
-- 租户：登记 tenant.excludes（单企业私有部署，无多租户语义）
-- 幂等：表已存在则跳过（手工执行前请确认环境）；日期：2026-09-05
-- =====================================================================

create table if not exists notification_events
(
    id              bigint        not null comment '主键（雪花）',
    receiver_id     bigint        not null comment '接收者（persons.id；收件箱仅本人可见 AC-TEAM-01）',
    event_type      varchar(50)   not null comment '事件类型（目录见 NotificationService.Types：BID_INVITED|BID_WON|BID_LOST|BID_EXPIRING_SOON|BID_SELECT_OVERDUE|BID_CONDITIONS_CHANGED|GATE_REJECTED|GATE_SIGN_SOON|G5_REVIEW_TODO|GATE_CONDITION_OVERDUE|DEL_CROSS_GROUP_CC|DEL_REJECTED|DEL_REVIEW_OVERDUE）',
    kind            varchar(8)    not null default 'FYI' comment 'FYI=跨组知会 / ACTION=可执行审批行动（两者严格分开）',
    source_type     varchar(40)   not null comment '业务来源表：bid_invitations|gates|gate_reviews|deletion_requests|projects',
    source_id       bigint        not null comment '业务来源行ID',
    dedup_key       varchar(160)  not null comment '幂等去重键 = source_type:event_type:source_id:receiver_id',
    title           varchar(200)  not null comment '标题（页03 站内信列表）',
    content         varchar(1000) null comment '正文',
    action_url      varchar(300)  null comment '行动跳转链接（kind=ACTION 时填写）',
    channel         varchar(20)   not null default 'MOCK' comment '投递渠道（一期 MOCK，标识清楚防误当真实发送）',
    delivery_status varchar(16)   not null default 'PENDING' comment 'PENDING|SENT|FAILED|DEAD',
    retry_count     int           not null default 0 comment '已重试次数（达上限转 DEAD）',
    next_retry_at   datetime      null comment '下次可投递时间（指数退避；PENDING 为 NULL）',
    read_flag       char(1)       not null default '0' comment '已读：0未读 1已读',
    read_at         datetime      null comment '已读时间',
    create_dept     bigint        null,
    create_by       bigint        null,
    create_time     datetime      null default CURRENT_TIMESTAMP,
    update_by       bigint        null,
    update_time     datetime      null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    tenant_id       varchar(20)   null default '000000',
    del_flag        char(1)       null default '0',
    remark          varchar(500)  null,
    primary key (id),
    unique key uk_notify_dedup (dedup_key),
    key idx_notify_inbox (receiver_id, read_flag, create_time),
    key idx_notify_dispatch (delivery_status, next_retry_at)
) engine = innodb
  default charset = utf8mb4
  collate = utf8mb4_general_ci
  comment = 'IPD 站内通知与可执行待办事件（OPS-05 outbox）';

-- 租户共享登记（与 26 张基线表同策略）：ruoyi-admin/src/main/resources/application.yml → tenant.excludes 增一行 notification_events

-- 真库最小权限（与 ipd_app@127.0.0.1 表级 GRANT 模型对齐）
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ipd_dev.notification_events TO 'ipd_app'@'127.0.0.1';
