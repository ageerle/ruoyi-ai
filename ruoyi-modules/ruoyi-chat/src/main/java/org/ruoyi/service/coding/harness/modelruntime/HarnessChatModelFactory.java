package org.ruoyi.service.coding.harness.modelruntime;

import dev.langchain4j.model.chat.StreamingChatModel;
import org.ruoyi.service.coding.harness.model.HarnessRunState;
import org.ruoyi.service.coding.harness.model.HarnessSessionState;

public interface HarnessChatModelFactory {
    StreamingChatModel create(HarnessSessionState session, HarnessRunState run);

    /**
     * Creates a bounded action model for turns whose tool call is mechanically mandatory.
     * Implementations that cannot safely disable thinking keep the normal model and advertise
     * that required tool choice is unsupported.
     */
    default ActionModel createRequiredActionModel(HarnessSessionState session,
                                                  HarnessRunState run) {
        boolean thinking = run.modelRoute() != null && run.modelRoute().thinkingEnabled();
        return new ActionModel(create(session, run), thinking, false);
    }

    /**
     * Escalates one narration-only mandatory-action turn without re-enabling long reasoning.
     * Provider integrations may keep the same action model when no stronger configured route
     * exists; the processor still bounds this recovery to one attempt per plan revision.
     */
    default ActionModel createEscalatedActionModel(HarnessSessionState session,
                                                   HarnessRunState run) {
        return createRequiredActionModel(session, run);
    }

    record ActionModel(StreamingChatModel model, boolean thinkingEnabled,
                       boolean requiredToolChoiceSupported) {
        public ActionModel {
            if (model == null) {
                throw new IllegalArgumentException("Action model is required");
            }
        }
    }
}
