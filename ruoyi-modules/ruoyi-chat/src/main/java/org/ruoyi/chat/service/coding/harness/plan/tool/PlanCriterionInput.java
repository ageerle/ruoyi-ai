package org.ruoyi.chat.service.coding.harness.plan.tool;

import dev.langchain4j.model.output.structured.Description;

/**
 * Model-authored criterion proposal; the Java plan aggregate becomes authoritative on save.
 * Mechanically closable criteria use either type {@code PROCESS_EXIT}, expected
 * {@code exitCode=0}, and a canonical {@code execute_process:{...}} evidence key, or the
 * shell-free type {@code FILE_MUTATION}, expected {@code success}, and a canonical
 * {@code workspace_file:relative/path} key.
 */
public record PlanCriterionInput(
    @Description("Stable criterion id referenced by plan steps") String id,
    @Description("Exactly PROCESS_EXIT or FILE_MUTATION") String type,
    @Description("Exactly exitCode=0 for PROCESS_EXIT, or success for FILE_MUTATION")
    String expected,
    @Description("Canonical finite execute_process JSON key (never a server/watch command), or workspace_file:relative/path inside allowedMutationRoots")
    String evidenceKey
) { }
