package org.ruoyi.service.coding.harness.model;

import java.util.UUID;

/** Durable session metadata. The canonical workspace is immutable after creation. */
public record HarnessSessionState(
    int schemaVersion,
    String sessionId,
    String tenantId,
    Long userId,
    String workspace,
    WorkspaceManifest workspaceManifest,
    String model,
    HarnessPermissionMode permissionMode,
    HarnessApprovalPolicy approvalPolicy,
    String title,
    String activeRunId,
    long createdAt,
    long updatedAt,
    long revision,
    HarnessThinkingLevel thinkingLevel,
    HarnessVerificationMode verificationMode,
    long pinnedAt,
    long deletedAt
) {

    public static final int CURRENT_SCHEMA_VERSION = 5;

    public HarnessSessionState {
        approvalPolicy = approvalPolicy == null ? HarnessApprovalPolicy.ON_REQUEST : approvalPolicy;
        workspaceManifest = workspaceManifest == null
            ? WorkspaceManifest.legacySingleProject() : workspaceManifest;
        // Doubao 思考等级默认 high；其他模型不使用该字段。旧快照缺字段按 high 归一化。
        thinkingLevel = thinkingLevel == null
            ? HarnessThinkingLevel.DOUBAO_DEFAULT : thinkingLevel;
        verificationMode = verificationMode == null
            ? HarnessVerificationMode.DEFAULT : verificationMode;
        if (schemaVersion < 1 || sessionId == null || sessionId.isBlank()
            || tenantId == null || tenantId.isBlank() || userId == null || userId <= 0
            || workspace == null || workspace.isBlank() || permissionMode == null
            || createdAt <= 0 || updatedAt <= 0 || revision < 0 || pinnedAt < 0 || deletedAt < 0) {
            throw new IllegalArgumentException("Invalid Harness session state");
        }
    }

    /** Legacy snapshots and callers have no sidebar metadata. */
    public HarnessSessionState(int schemaVersion, String sessionId, String tenantId, Long userId,
                               String workspace, WorkspaceManifest workspaceManifest,
                               String model, HarnessPermissionMode permissionMode,
                               HarnessApprovalPolicy approvalPolicy, String title, String activeRunId,
                               long createdAt, long updatedAt, long revision,
                               HarnessThinkingLevel thinkingLevel, HarnessVerificationMode verificationMode) {
        this(schemaVersion, sessionId, tenantId, userId, workspace, workspaceManifest, model,
            permissionMode, approvalPolicy, title, activeRunId, createdAt, updatedAt, revision,
            thinkingLevel, verificationMode, 0, 0);
    }

    /** Backward-compatible constructor for persisted/test callers created before approval policy existed. */
    public HarnessSessionState(int schemaVersion, String sessionId, String tenantId, Long userId,
                               String workspace, String model, HarnessPermissionMode permissionMode,
                               String title, String activeRunId, long createdAt, long updatedAt,
                               long revision) {
        this(schemaVersion, sessionId, tenantId, userId, workspace,
            WorkspaceManifest.legacySingleProject(), model, permissionMode,
            HarnessApprovalPolicy.ON_REQUEST, title, activeRunId, createdAt, updatedAt, revision,
            HarnessThinkingLevel.DOUBAO_DEFAULT, HarnessVerificationMode.DEFAULT);
    }

    /** Backward-compatible constructor for callers created before workspace manifests existed. */
    public HarnessSessionState(int schemaVersion, String sessionId, String tenantId, Long userId,
                               String workspace, String model, HarnessPermissionMode permissionMode,
                               HarnessApprovalPolicy approvalPolicy, String title,
                               String activeRunId, long createdAt, long updatedAt, long revision) {
        this(schemaVersion, sessionId, tenantId, userId, workspace,
            WorkspaceManifest.legacySingleProject(), model, permissionMode, approvalPolicy,
            title, activeRunId, createdAt, updatedAt, revision,
            HarnessThinkingLevel.DOUBAO_DEFAULT, HarnessVerificationMode.DEFAULT);
    }

    /**
     * Backward-compatible constructor for callers (tests and persisted projections) that supply
     * an explicit {@link WorkspaceManifest} but predate thinking level and verification mode.
     */
    public HarnessSessionState(int schemaVersion, String sessionId, String tenantId, Long userId,
                               String workspace, WorkspaceManifest workspaceManifest,
                               String model, HarnessPermissionMode permissionMode,
                               HarnessApprovalPolicy approvalPolicy, String title,
                               String activeRunId, long createdAt, long updatedAt, long revision) {
        this(schemaVersion, sessionId, tenantId, userId, workspace, workspaceManifest,
            model, permissionMode, approvalPolicy, title, activeRunId, createdAt, updatedAt,
            revision, HarnessThinkingLevel.DOUBAO_DEFAULT, HarnessVerificationMode.DEFAULT);
    }

    public static HarnessSessionState create(HarnessOwner owner, String workspace, String model,
                                             HarnessPermissionMode permissionMode, String title, long now) {
        return create(owner, workspace, model, permissionMode,
            HarnessApprovalPolicy.ON_REQUEST, title, now);
    }

    public static HarnessSessionState create(HarnessOwner owner, String workspace, String model,
                                             HarnessPermissionMode permissionMode,
                                             HarnessApprovalPolicy approvalPolicy,
                                             String title, long now) {
        return createWithId(UUID.randomUUID().toString(), owner, workspace, model,
            permissionMode, approvalPolicy, title, now);
    }

    public static HarnessSessionState createWithId(String sessionId, HarnessOwner owner,
                                                   String workspace, String model,
                                                   HarnessPermissionMode permissionMode,
                                                   String title, long now) {
        return createWithId(sessionId, owner, workspace, model, permissionMode,
            HarnessApprovalPolicy.ON_REQUEST, title, now, null);
    }

    public static HarnessSessionState createWithId(String sessionId, HarnessOwner owner,
                                                   String workspace, String model,
                                                   HarnessPermissionMode permissionMode,
                                                   HarnessApprovalPolicy approvalPolicy,
                                                   String title, long now) {
        return new HarnessSessionState(CURRENT_SCHEMA_VERSION, sessionId,
            owner.tenantId(), owner.userId(), workspace,
            WorkspaceManifest.legacySingleProject(), model,
            permissionMode == null ? HarnessPermissionMode.READ_ONLY : permissionMode,
            approvalPolicy, title, null, now, now, 0,
            HarnessThinkingLevel.DOUBAO_DEFAULT, HarnessVerificationMode.DEFAULT);
    }

    public static HarnessSessionState createWithId(String sessionId, HarnessOwner owner,
                                                   String workspace, String model,
                                                   HarnessPermissionMode permissionMode,
                                                   HarnessApprovalPolicy approvalPolicy,
                                                   String title, long now,
                                                   WorkspaceManifest workspaceManifest) {
        return createWithId(sessionId, owner, workspace, model, permissionMode, approvalPolicy,
            title, now, workspaceManifest, HarnessThinkingLevel.DOUBAO_DEFAULT,
            HarnessVerificationMode.DEFAULT);
    }

    /** Full canonical factory used by the application service for new sessions. */
    public static HarnessSessionState createWithId(String sessionId, HarnessOwner owner,
                                                   String workspace, String model,
                                                   HarnessPermissionMode permissionMode,
                                                   HarnessApprovalPolicy approvalPolicy,
                                                   String title, long now,
                                                   WorkspaceManifest workspaceManifest,
                                                   HarnessThinkingLevel thinkingLevel,
                                                   HarnessVerificationMode verificationMode) {
        return new HarnessSessionState(CURRENT_SCHEMA_VERSION, sessionId,
            owner.tenantId(), owner.userId(), workspace,
            workspaceManifest == null ? WorkspaceManifest.legacySingleProject()
                : workspaceManifest,
            model, permissionMode == null ? HarnessPermissionMode.READ_ONLY : permissionMode,
            approvalPolicy, title, null, now, now, 0,
            thinkingLevel == null ? HarnessThinkingLevel.DOUBAO_DEFAULT : thinkingLevel,
            verificationMode == null ? HarnessVerificationMode.DEFAULT : verificationMode);
    }

    public HarnessOwner owner() {
        return new HarnessOwner(tenantId, userId);
    }

    public HarnessSessionState withActiveRun(String runId, long now) {
        return new HarnessSessionState(schemaVersion, sessionId, tenantId, userId, workspace,
            workspaceManifest, model, permissionMode, approvalPolicy, title, runId, createdAt,
            now, revision, thinkingLevel, verificationMode, pinnedAt, deletedAt);
    }

    public HarnessSessionState withTitle(String newTitle, long now) {
        return new HarnessSessionState(schemaVersion, sessionId, tenantId, userId, workspace,
            workspaceManifest, model, permissionMode, approvalPolicy, newTitle, activeRunId,
            createdAt, now, revision, thinkingLevel, verificationMode, pinnedAt, deletedAt);
    }

    public HarnessSessionState withRevision(long newRevision) {
        return new HarnessSessionState(schemaVersion, sessionId, tenantId, userId, workspace,
            workspaceManifest, model, permissionMode, approvalPolicy, title, activeRunId,
            createdAt, updatedAt, newRevision, thinkingLevel, verificationMode, pinnedAt, deletedAt);
    }

    /** Sidebar changes do not alter the execution revision or conversation activity time. */
    public HarnessSessionState withSidebarState(long newPinnedAt, long newDeletedAt) {
        return new HarnessSessionState(schemaVersion, sessionId, tenantId, userId, workspace,
            workspaceManifest, model, permissionMode, approvalPolicy, title, activeRunId,
            createdAt, updatedAt, revision, thinkingLevel, verificationMode, newPinnedAt, newDeletedAt);
    }
}
