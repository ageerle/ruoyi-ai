package org.ruoyi.service.coding.harness.prompt;

import org.ruoyi.service.coding.harness.model.HarnessPermissionMode;
import org.ruoyi.service.coding.harness.model.WorkspaceManifest;
import org.ruoyi.service.coding.harness.tool.ToolDescriptor;

import java.util.List;

public record HarnessPromptContext(
    String workspace,
    WorkspaceManifest workspaceManifest,
    HarnessPermissionMode permissionMode,
    String originalRequirement,
    String responseLanguage,
    String projectInstructions,
    String planProjection,
    String budgetProjection,
    List<ToolDescriptor> tools,
    List<HarnessSkillMetadata> skills,
    boolean externalVerification
) {
    public HarnessPromptContext {
        workspaceManifest = workspaceManifest == null
            ? WorkspaceManifest.legacySingleProject() : workspaceManifest;
        tools = tools == null ? List.of() : List.copyOf(tools);
        skills = skills == null ? List.of() : List.copyOf(skills);
        if (workspace == null || workspace.isBlank() || permissionMode == null
            || originalRequirement == null || originalRequirement.isBlank()
            || responseLanguage == null || responseLanguage.isBlank()) {
            throw new IllegalArgumentException(
                "Prompt context requires workspace, permission mode, original requirement, "
                    + "and response language");
        }
    }

    /** Backward-compatible adapter for callers created before external verification mode existed. */
    public HarnessPromptContext(String workspace, WorkspaceManifest workspaceManifest,
                                HarnessPermissionMode permissionMode,
                                String originalRequirement, String responseLanguage,
                                String projectInstructions, String planProjection,
                                String budgetProjection, List<ToolDescriptor> tools,
                                List<HarnessSkillMetadata> skills) {
        this(workspace, workspaceManifest, permissionMode, originalRequirement, responseLanguage,
            projectInstructions, planProjection, budgetProjection, tools, skills, false);
    }

    /** Backward-compatible adapter for the former single-project prompt context. */
    public HarnessPromptContext(String workspace, HarnessPermissionMode permissionMode,
                                String originalRequirement, String responseLanguage,
                                String projectInstructions, String planProjection,
                                String budgetProjection, List<ToolDescriptor> tools,
                                List<HarnessSkillMetadata> skills) {
        this(workspace, WorkspaceManifest.legacySingleProject(), permissionMode,
            originalRequirement, responseLanguage, projectInstructions, planProjection,
            budgetProjection, tools, skills, false);
    }
}
