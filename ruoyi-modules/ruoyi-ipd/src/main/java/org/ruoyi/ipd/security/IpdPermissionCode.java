package org.ruoyi.ipd.security;

/**
 * IPD 模块权限标识（SEC-01）。
 * 命名对齐 RuoYi：{@code ipd:资源:动作}。Controller 优先使用字符串字面量。
 */
public interface IpdPermissionCode {

    String OPERATION_MODULE_PROJECT = "ipd:project:list";
    String OPERATION_MODULE_PROJECT_QUERY = "ipd:project:query";
    String OPERATION_MODULE_PROJECT_CREATE = "ipd:project:add";
    String OPERATION_MODULE_PROJECT_STATUS_CHANGE = "ipd:project:edit";
    String OPERATION_MODULE_PROJECT_ADVANCE_STAGE = "ipd:project:edit";

    String OPERATION_PRODUCT_GROUP = "ipd:product:list";
    String OPERATION_PRODUCT_GROUP_CREATE = "ipd:product:add";
    String OPERATION_PRODUCT_GROUP_BIND_PROJECT = "ipd:product:edit";
    String OPERATION_PRODUCT_GROUP_STATUS_CHANGE = "ipd:product:edit";
    String OPERATION_PRODUCT_GROUP_TERMINATE = "ipd:product:edit";

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
}
