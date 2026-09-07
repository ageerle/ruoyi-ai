package org.ruoyi.ipd.security;

/**
 * IPD 模块权限标识（SEC-01）。
 * 命名对齐 RuoYi：{@code ipd:资源:动作}。Controller 必须引用本接口常量——
 * 裸字面量散落在改名时会漏改（2026-09-06 第五批治理决议，常量化收口）。
 */
public interface IpdPermissionCode {

    String OPERATION_MODULE_PROJECT = "ipd:project:list";
    String OPERATION_MODULE_PROJECT_QUERY = "ipd:project:query";
    String OPERATION_MODULE_PROJECT_CREATE = "ipd:project:add";
    String OPERATION_MODULE_PROJECT_STATUS_CHANGE = "ipd:project:edit";

    String OPERATION_PRODUCT_GROUP = "ipd:product:list";
    String OPERATION_PRODUCT_GROUP_CREATE = "ipd:product:add";
    String OPERATION_PRODUCT_GROUP_BIND_PROJECT = "ipd:product:edit";

    String OPERATION_STAGE_ACTION = "ipd:stage-action:list";
    String OPERATION_STAGE_ACTION_EXECUTE = "ipd:stage-action:edit";
    String OPERATION_STAGE_ACTION_DELIVERABLE = "ipd:stage-action:add";
    String OPERATION_STAGE_ACTION_INSTANTIATE = "ipd:stage-action:add";

    String OPERATION_CERT_TEMPLATE = "ipd:cert-template:list";
    String OPERATION_CERT_TEMPLATE_CREATE = "ipd:cert-template:add";
    String OPERATION_CERT_TEMPLATE_DELETE = "ipd:cert-template:remove";

    String OPERATION_GATE_ELEMENT = "ipd:gate-element:list";
    String OPERATION_GATE_ELEMENT_CREATE = "ipd:gate-element:add";
    String OPERATION_GATE_ELEMENT_UPDATE = "ipd:gate-element:edit";
    String OPERATION_GATE_ELEMENT_DISABLE = "ipd:gate-element:remove";
    String OPERATION_GATE_ELEMENT_PUBLISH = "ipd:gate-element:publish";
    String OPERATION_GATE_ELEMENT_ARCHIVE = "ipd:gate-element:archive";
    String OPERATION_GATE_ELEMENT_COPY = "ipd:gate-element:copy";
    String OPERATION_GATE_ELEMENT_REVERT = "ipd:gate-element:revert";

    String OPERATION_DELETION_REQUEST_ARCHIVE = "ipd:deletion-request:archive";
    String OPERATION_DELETION_REQUEST_PURGE = "ipd:deletion-request:purge";
    String OPERATION_DELETION_REQUEST_SUBMIT = "ipd:deletion-request:submit";
    String OPERATION_DELETION_REQUEST_LEADER = "ipd:deletion-request:leader";
    String OPERATION_DELETION_REQUEST_ADMIN = "ipd:deletion-request:admin";

    String OPERATION_GATE_REVIEW = "ipd:gate-review:list";
    String OPERATION_GATE_REVIEW_INITIATE = "ipd:gate-review:add";
    String OPERATION_GATE_REVIEW_APPROVE = "ipd:gate-review:edit";

    /** AC-INC-15c：双PM 联合提议系数 */
    String OPERATION_COEFFICIENT_PROPOSE = "ipd:coefficient:propose";
    /** AC-INC-15c：产品组长确认系数 */
    String OPERATION_COEFFICIENT_CONFIRM = "ipd:coefficient:confirm";

    /** OPS-05：站内通知收件箱（本人） */
    String OPERATION_NOTIFICATION_READ = "ipd:notification:read";
    /** OPS-05：outbox 消费端手动触发（仅超管） */
    String OPERATION_NOTIFICATION_DISPATCH = "ipd:notification:dispatch";

    /** P1-10.1：AI 文档版本链读取 */
    String OPERATION_AI_DOCUMENT = "ipd:ai-document:list";
    /** P1-10.1：登记 AI 原始输出 v1（生成入口 P4-2 接管） */
    String OPERATION_AI_DOCUMENT_CREATE = "ipd:ai-document:add";
    /** P1-10.1：人工改版（基准非最新版 409） */
    String OPERATION_AI_DOCUMENT_REVISE = "ipd:ai-document:edit";
    /** P1-10.1：人工审核通过（BR-AI-03） */
    String OPERATION_AI_DOCUMENT_REVIEW = "ipd:ai-document:review";

    /** P4-2.1：AI 模型配置读（内部全员；密钥永不回显） */
    String OPERATION_AI_MODEL = "ipd:ai-model:list";
    /** P4-2.1：AI 模型配置写（仅超管） */
    String OPERATION_AI_MODEL_EDIT = "ipd:ai-model:edit";

    /** SOP 模板写（仅超管） */
    String OPERATION_SOP_TEMPLATE_EDIT = "ipd:sop-template:edit";
    /** SOP 模板读（内部全员） */
    String OPERATION_SOP_TEMPLATE = "ipd:sop-template:list";

    /** P3-1.x：KPI 考核查询（内部四角色） */
    String OPERATION_KPI_QUERY = "ipd:kpi:query";

    /** P3-4.4：奖金池查询 */
    String OPERATION_BONUS_POOL_QUERY = "ipd:bonus-pool:query";
    /** P3-4.4：奖金池计算 */
    String OPERATION_BONUS_POOL_COMPUTE = "ipd:bonus-pool:compute";
    /** P3-4.4：奖金池冻结 */
    String OPERATION_BONUS_POOL_FREEZE = "ipd:bonus-pool:freeze";
    /** P3-4.4：奖金池分配 */
    String OPERATION_BONUS_POOL_DISTRIBUTE = "ipd:bonus-pool:distribute";

    /** P3-6.2：贡献度查询 */
    String OPERATION_CONTRIBUTION_QUERY = "ipd:contribution:query";
    /** P3-6.2：贡献度保存 */
    String OPERATION_CONTRIBUTION_SAVE = "ipd:contribution:save";
    /** P3-6.2：贡献度确认 */
    String OPERATION_CONTRIBUTION_CONFIRM = "ipd:contribution:confirm";

    /** P3-8.2：负反馈查询 */
    String OPERATION_NEGATIVE_FEEDBACK_QUERY = "ipd:negative-feedback:query";
    /** P3-8.2：负反馈录入 */
    String OPERATION_NEGATIVE_FEEDBACK_CREATE = "ipd:negative-feedback:create";
    /** P3-8.2：负反馈认定/解除 */
    String OPERATION_NEGATIVE_FEEDBACK_DECIDE = "ipd:negative-feedback:decide";

    /** P3-7.1：切换验收 run / get / list（内部全员可读；写操作 service 二次校验） */
    String OPERATION_SWITCHING_ACCEPTANCE_QUERY = "ipd:switching-acceptance:query";
    /** P3-7.1：切换验收 lock / unlock（仅 SUPER_ADMIN，走 requireAdmin） */
    String OPERATION_SWITCHING_ACCEPTANCE_ADMIN = "ipd:switching-acceptance:admin";

    /** SEC-02：审计日志 */
    String OPERATION_AUDIT_LOG_LIST = "ipd:audit-log:list";
    String OPERATION_AUDIT_LOG_VERIFY = "ipd:audit-log:verify";
    String OPERATION_AUDIT_LOG_EXPORT = "ipd:audit-log:export";

    /** AC-COMP-01/04/05：合规读 */
    String OPERATION_COMPLIANCE_READ = "ipd:compliance:read";
    /** AC-COMP-02/03：合规写 */
    String OPERATION_COMPLIANCE_WRITE = "ipd:compliance:write";

    /** 系统参数 */
    String OPERATION_SYSTEM_CONFIG_LIST = "ipd:system-config:list";
    String OPERATION_SYSTEM_CONFIG_READ = "ipd:system-config:read";
    String OPERATION_SYSTEM_CONFIG_UPDATE = "ipd:system-config:update";

    /** 产品查询 */
    String OPERATION_PRODUCT_QUERY = "ipd:product:query";


    /** P2-3.1：校验型创建招标单（MARKET_PM/GROUP_LEADER/SUPER_ADMIN） */
    String OPERATION_BID_INVITATION_CREATE = "ipd:bid-invitation:create";
    /** P2-3.3：超管指派 */
    String OPERATION_BID_INVITATION_ADMIN_ASSIGN = "ipd:bid-invitation:admin-assign";

    /** HIGH-3.1：撤销已接受移交（24h 内；RLD_BACK 终态 + 副作用回滚） */
    String OPERATION_HANDOVER_CANCEL = "ipd:handover:cancel";
}
