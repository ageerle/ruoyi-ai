package org.ruoyi.service.coding.harness.app;

import org.ruoyi.service.coding.harness.model.HarnessApprovalPolicy;
import org.ruoyi.service.coding.harness.model.HarnessPermissionMode;
import org.ruoyi.service.coding.harness.model.HarnessThinkingLevel;
import org.ruoyi.service.coding.harness.model.HarnessVerificationMode;
import org.ruoyi.service.coding.harness.model.WorkspaceManifest;

import java.util.UUID;

public record CreateHarnessSessionCommand(
    String workspacePath,
    String model,
    HarnessPermissionMode permissionMode,
    HarnessApprovalPolicy approvalPolicy,
    String title,
    String idempotencyKey,
    WorkspaceManifest workspaceManifest,
    HarnessThinkingLevel thinkingLevel,
    HarnessVerificationMode verificationMode
) {

    public CreateHarnessSessionCommand(String workspacePath, String model,
                                       HarnessPermissionMode permissionMode, String title) {
        this(workspacePath, model, permissionMode, HarnessApprovalPolicy.ON_REQUEST,
            title, UUID.randomUUID().toString(), null,
            HarnessThinkingLevel.DOUBAO_DEFAULT, HarnessVerificationMode.DEFAULT);
    }

    public CreateHarnessSessionCommand(String workspacePath, String model,
                                       HarnessPermissionMode permissionMode, String title,
                                       String idempotencyKey) {
        this(workspacePath, model, permissionMode, HarnessApprovalPolicy.ON_REQUEST,
            title, idempotencyKey, null,
            HarnessThinkingLevel.DOUBAO_DEFAULT, HarnessVerificationMode.DEFAULT);
    }

    /** Backward-compatible constructor for callers created before workspace manifests existed. */
    public CreateHarnessSessionCommand(String workspacePath, String model,
                                       HarnessPermissionMode permissionMode,
                                       HarnessApprovalPolicy approvalPolicy, String title,
                                       String idempotencyKey) {
        this(workspacePath, model, permissionMode, approvalPolicy, title, idempotencyKey, null,
            HarnessThinkingLevel.DOUBAO_DEFAULT, HarnessVerificationMode.DEFAULT);
    }

    /**
     * Backward-compatible constructor for callers (existing tests/services) that supply an
     * explicit {@link WorkspaceManifest} but predate thinking level and verification mode.
     * New fields default to HIGH thinking and AGENT verification.
     */
    public CreateHarnessSessionCommand(String workspacePath, String model,
                                       HarnessPermissionMode permissionMode,
                                       HarnessApprovalPolicy approvalPolicy, String title,
                                       String idempotencyKey,
                                       WorkspaceManifest workspaceManifest) {
        this(workspacePath, model, permissionMode, approvalPolicy, title, idempotencyKey,
            workspaceManifest, HarnessThinkingLevel.DOUBAO_DEFAULT,
            HarnessVerificationMode.DEFAULT);
    }

    public CreateHarnessSessionCommand {
        approvalPolicy = approvalPolicy == null ? HarnessApprovalPolicy.ON_REQUEST : approvalPolicy;
        workspaceManifest = workspaceManifest == null
            ? WorkspaceManifest.legacySingleProject() : workspaceManifest;
        thinkingLevel = thinkingLevel == null
            ? HarnessThinkingLevel.DOUBAO_DEFAULT : thinkingLevel;
        verificationMode = verificationMode == null
            ? HarnessVerificationMode.DEFAULT : verificationMode;
        if (idempotencyKey == null || idempotencyKey.isBlank()
            || idempotencyKey.length() > 256) {
            throw new IllegalArgumentException("idempotencyKey is required and must not exceed 256 characters");
        }
        idempotencyKey = idempotencyKey.strip();
    }
}
