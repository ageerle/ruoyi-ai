package org.ruoyi.service.coding.harness.plan.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.ruoyi.service.coding.harness.model.HarnessOwner;

/** Per-run bound tool facade; it never trusts owner/session identifiers from model arguments. */
public final class HarnessPlanTools {

    private final HarnessPlanCommandService service;
    private final HarnessOwner owner;
    private final String sessionId;
    private final String runId;

    HarnessPlanTools(HarnessPlanCommandService service, HarnessOwner owner,
                     String sessionId, String runId) {
        this.service = service;
        this.owner = owner;
        this.sessionId = sessionId;
        this.runId = runId;
    }

    @Tool(name = "plan_create", value = {
        "Create or idempotently replace a structured task plan and acceptance contract.",
        "Call once when the current authoritative plan projection is none. If a taskId already",
        "exists, do not call again unless current mode is PLAN after authenticated revision feedback.",
        "Mechanical criteria currently require type PROCESS_EXIT and expected exitCode=0;",
        "evidenceKey is required and must bind one exact canonical execute_process invocation,",
        "for example execute_process:{\"executable\":\"mvn\",\"argv\":[\"test\"],\"cwd\":\".\"}.",
        "Canonical identity is executable, argv, cwd; cwd defaults to . and timeoutMs is an",
        "execution safety deadline deliberately excluded from evidence identity.",
        "PROCESS_EXIT must bind a finite command. Never bind a dev/static server, watch mode,",
        "or another long-lived command: it cannot satisfy exitCode=0 without being stopped.",
        "Use a finite syntax/test command and a bounded probe for runtime server behavior.",
        "The other exact form is type FILE_MUTATION, expected success, and evidenceKey",
        "workspace_file:relative/path. That path must be within allowedMutationRoots.",
        "Never require FILE_MUTATION for a test, fixture, or instruction file that must stay",
        "unchanged. For one requested production file and one check, create one step and bind",
        "that step only to the production-file mutation and exact check criteria.",
        "Never bind one aggregate test/check criterion to multiple steps in the same dependency",
        "chain: bind it only to the last dependent step; intermediate steps may have no criteria.",
        "If the immutable request names a Required test/check command, bind that exact executable",
        "and argv as PROCESS_EXIT evidence; never replace it with npm test or another wrapper.",
        "Do not narrow plural or aggregate requirements in planMarkdown or step obligations.",
        "For API input limits, enumerate each named field and the raw request/body or collection",
        "growth boundary separately; a limit on one decoded field is not a request-size limit.",
        "Oversize rejection must be a client-observable JSON 4xx, never a socket destroy.",
        "For frontend loading/error/empty/content states, plan a cross-layer visibility contract:",
        "the selectors toggled by JavaScript must exist in HTML and have effective CSS behavior.",
        "Use for complex work before mutation. The returned taskId/revision/hash require",
        "authenticated control-plane approval; this tool cannot approve its own plan."
    })
    public PlanToolResult create(
        @P(value = "Structured draft, steps, verification criteria and mutation scope",
            required = true) PlanDraftInput draft) {
        return service.createPlan(owner, sessionId, runId, draft);
    }

    @Tool(name = "plan_step", value = {
        "Transition one authoritative plan step with optimistic expectedRevision.",
        "Use only in BUILD and only with the revision in the current authoritative projection;",
        "never use an earlier tool result revision and never call plan_step in VERIFY.",
        "BLOCK or FAIL requires evidenceIds containing failed mechanical evidence that matches",
        "this step; analysis, output truncation, or model prose is never blocker evidence.",
        "COMPLETE requires ids of successful persisted evidence satisfying every bound criterion.",
        "The runtime normally records successful first-party evidence and advances bound steps",
        "automatically; call this only when the returned plan projection still requires it."
    })
    public PlanToolResult step(
        @P(value = "Step transition command", required = true) PlanStepCommand command) {
        return service.updateStep(owner, sessionId, runId, command);
    }

    @Tool(name = "plan_record_tool_evidence", value = {
        "Turn a persisted execute_process result into mechanical PROCESS_EXIT evidence.",
        "Java binds the unique earlier assistant call to its result and derives type, command key,",
        "actual exit outcome, success, digest and provenance; the model supplies only toolCallId",
        "and expectedRevision and cannot relabel read output, change the command key, or assert pass.",
        "Successful execute_process/write_file/replace_text calls are normally recorded by the",
        "runtime automatically; call this only if the returned plan projection still lacks evidence."
    })
    public PlanToolResult evidence(
        @P(value = "Durable tool-result evidence command", required = true)
        PlanEvidenceCommand command) {
        return service.recordToolEvidence(owner, sessionId, runId, command);
    }

    @Tool(name = "plan_verify", value = {
        "Enter, fail, or complete authoritative verification with optimistic expectedRevision.",
        "Use only the revision in the current authoritative projection, never an earlier result.",
        "COMPLETE is rejected unless successful persisted evidence satisfies every criterion and",
        "a fresh final-review tool result exists. Choose COMPLETE or FAIL only after reasoning",
        "over that result; the runtime never auto-completes merely because a file was read.",
        "For BEGIN and FAIL, evidenceIds are ignored; only COMPLETE accepts persisted evidence ids.",
        "Any failed execute_process after the current VERIFY boundary makes COMPLETE illegal;",
        "call FAIL, repair in BUILD, and begin a clean verification revision.",
        "FAIL returns structured work to BUILD, invalidates stale successful evidence, and reopens",
        "the last completed step for an explicit retry and repair."
    })
    public PlanToolResult verify(
        @P(value = "Verification transition command", required = true)
        PlanVerificationCommand command) {
        return service.verify(owner, sessionId, runId, command);
    }
}
