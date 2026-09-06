-- P2-5.4 Gate 超时、延期、重发与仲裁轮次（AC-GATE-06~10/21；BR-GATE-04/05/06）
-- 1) gates 加签署期限与延长次数（AC-GATE-08 弃权判定锚点 / AC-GATE-21 最多 3 次）
-- 2) 新表 gate_arbitrations：双PM 冲突仲裁与超管终裁意见（AC-GATE-10）
--    独立表原因：两位组长的 reviewerType 同为 GROUP_LEADER，gate_reviews 的
--    uk_gr_gate_type_round(gate_id, reviewer_type, round) 会拦截第二位组长，
--    仲裁意见按人去重(gate_id, round, arbitrator_id) 走本表。
-- apply 后核对：show columns from gates like 'sign_%'; show columns from gate_arbitrations;

alter table gates
    add column sign_due_at datetime null comment '签署期限（BR-GATE-04 3 自然日；submit/reopen 起算，超管可延长 AC-GATE-21）' after started_at,
    add column sign_extension_count int not null default 0 comment '签署期限已延长次数（AC-GATE-21 上限 3）' after sign_due_at;

create table gate_arbitrations (
    id               bigint       not null comment '主键（雪花算法）',
    gate_id          bigint       not null comment 'Gate 实例ID',
    round            int          not null comment '评审轮次（与冲突发生轮对齐）',
    arbitrator_type  varchar(16)  not null comment 'GROUP_LEADER|SUPER_ADMIN',
    arbitrator_id    bigint       not null comment '仲裁人/终裁人ID',
    decision         varchar(16)  not null comment 'APPROVE|REJECT',
    opinion          varchar(1000) null comment '仲裁/终裁意见',
    create_dept      bigint       null,
    create_by        bigint       null,
    create_time      datetime     default current_timestamp,
    update_by        bigint       null,
    update_time      datetime     default current_timestamp on update current_timestamp,
    tenant_id        varchar(20)  default '000000',
    del_flag         char(1)      default '0',
    remark           varchar(500) null,
    primary key (id),
    unique key uk_ga_gate_round_arb (gate_id, round, arbitrator_id),
    key idx_ga_gate (gate_id)
) engine = InnoDB default charset = utf8mb4 comment ='Gate 冲突仲裁与超管终裁意见（P2-5.4 AC-GATE-10）';
