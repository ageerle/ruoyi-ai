package org.ruoyi.service.coding.harness.loop.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.ruoyi.service.coding.harness.model.HarnessRunState;
import org.ruoyi.service.coding.harness.model.HarnessSessionState;
import org.ruoyi.service.coding.harness.modelruntime.HarnessAnalysisDelegate;

/** Run-bound collaboration facade; multiple calls in one model turn execute concurrently. */
public final class HarnessDelegateTools {

    private final HarnessAnalysisDelegate delegate;
    private final HarnessSessionState session;
    private final HarnessRunState run;

    public HarnessDelegateTools(HarnessAnalysisDelegate delegate, HarnessSessionState session,
                                HarnessRunState run) {
        this.delegate = delegate;
        this.session = session;
        this.run = run;
    }

    @Tool(name = "delegate_task", value = {
        "Ask one read-only analysis worker a bounded independent question using only evidence you supply. " +
            "For parallel collaboration, emit 2-3 delegate_task calls in the same assistant tool turn with disjoint scopes. " +
            "Do not use for trivial work, mutations, repository discovery, or work the parent will repeat."
    })
    public String delegateTask(
        @P(name = "role", value = "Short role such as backend reviewer or frontend contract reviewer", required = true)
        String role,
        @P(name = "task", value = "One concrete, independently answerable analysis question", required = true)
        String task,
        @P(name = "evidence", value = "Focused source/tool evidence already collected by the parent", required = true)
        String evidence
    ) {
        return delegate.execute(session, run, role, task, evidence);
    }
}
