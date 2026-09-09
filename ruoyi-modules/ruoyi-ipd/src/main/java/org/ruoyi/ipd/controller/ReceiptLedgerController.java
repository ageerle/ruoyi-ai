package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.ReceiptLedger;
import org.ruoyi.ipd.dto.ReceiptLedgerCreateReq;
import org.ruoyi.ipd.dto.ReceiptRefundReq;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.service.AuditLogService;
import org.ruoyi.ipd.service.ReceiptLedgerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 销售回款台账 HTTP 端点（P3-4.1 AC-INC-16b/16c/16d/31/31b/32）。
 * 2026-09-08 缺口补齐：recordReceipt / recordRefund / listByProject 此前无 controller
 * 挂载，receipt_ledger 表 0 行（服务存在但写入路径未接线）。
 * 权限同 BonusPool 域：录入/冲减同 COMPUTE（管理员动作），查询同 QUERY。
 */
@RestController
@RequestMapping("/api/v1/receipt-ledgers")
@RequiredArgsConstructor
public class ReceiptLedgerController {

    private final IpdPermission permission;
    private final ReceiptLedgerService service;
    private final AuditLogService auditLogService;

    /** 月度回款录入（AC-INC-16c）：凭证可空，source/窗口由服务端定死。 */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_BONUS_POOL_COMPUTE, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping
    public ApiV1Response<ReceiptLedger> create(@Valid @RequestBody ReceiptLedgerCreateReq req) {
        IpdActor actor = permission.requireAdmin();
        ReceiptLedger ledger = new ReceiptLedger();
        ledger.setProjectId(req.projectId());
        ledger.setReceiptMonth(req.receiptMonth());
        ledger.setReceiptAmount(req.receiptAmount());
        ledger.setRefundAmount(req.refundAmount());
        ledger.setVoucherUrl(req.voucherUrl());
        ledger.setVoucherHash(req.voucherHash());
        ledger.setSource("RECEIPT");
        ReceiptLedger saved = service.recordReceipt(ledger);
        audit(actor, "RECEIPT_CREATE", saved.getId(),
            "回款录入 " + req.receiptMonth() + " 金额 " + req.receiptAmount());
        return ApiV1Response.ok(saved);
    }

    /** 退款冲减（AC-INC-31/31b）：窗口内当期冲减，窗口外拒绝回溯。 */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_BONUS_POOL_COMPUTE, type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/{projectId}/refunds")
    public ApiV1Response<ReceiptLedger> refund(@PathVariable Long projectId,
                                               @Valid @RequestBody ReceiptRefundReq req) {
        IpdActor actor = permission.requireAdmin();
        ReceiptLedger saved = service.recordRefund(projectId, req.month(), req.refundAmount());
        audit(actor, "RECEIPT_REFUND", saved.getId(),
            "退款冲减 " + req.month() + " 金额 " + req.refundAmount());
        return ApiV1Response.ok(saved);
    }

    /** 按项目查询回款台账（达成率口径明细，AC-INC-16b）。 */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_BONUS_POOL_QUERY, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping("/by-project/{projectId}")
    public ApiV1Response<List<ReceiptLedger>> listByProject(@PathVariable Long projectId) {
        permission.requireInternal();
        return ApiV1Response.ok(service.listByProject(projectId));
    }

    private void audit(IpdActor actor, String action, Long entityId, String reason) {
        if (actor == null) {
            return;
        }
        auditLogService.append(AuditLog.builder()
            .operatorId(actor.id())
            .operatorName(actor.name())
            .operatorRole(actor.role())
            .action(action)
            .entityType("receipt_ledger")
            .entityId(entityId)
            .reason(reason)
            .build());
    }
}
