package org.ruoyi.service.coding.harness.loop;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.service.chat.impl.provider.doubao.DoubaoStreamingChatModel;
import org.ruoyi.service.coding.harness.model.*;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class DoubaoReasoningProvenanceTest {
    private HarnessRunState run(String model) {
        HarnessRunState run = mock(HarnessRunState.class);
        HarnessModelRoute route = mock(HarnessModelRoute.class);
        when(run.modelRoute()).thenReturn(route);
        when(route.thinkingEnabled()).thenReturn(true);
        when(route.selectedModel()).thenReturn(model);
        when(run.sessionId()).thenReturn("session");
        when(run.runId()).thenReturn("run");
        return run;
    }
    private HarnessMessage map(HarnessRunState run, Map<String,Object> attributes) {
        AiMessage ai = AiMessage.builder().attributes(attributes).toolExecutionRequests(List.of(
            ToolExecutionRequest.builder().id("call1").name("read_file").arguments("{}").build())).build();
        return new HarnessAssistantMessageMapper().map(ChatResponse.builder().aiMessage(ai)
            .finishReason(dev.langchain4j.model.output.FinishReason.TOOL_EXECUTION).build(),
            run,"effect",1,true,1);
    }
    @Test void providerOmissionIsPreservedWithWarningAndWithoutFabricatedReasoning() {
        HarnessRunState run = run("bytedance/doubao-seed-2.1-pro-260628");
        HarnessMessage message = map(run,Map.of(DoubaoStreamingChatModel.REASONING_ABSENT_ATTRIBUTE,true));
        assertNull(message.thinking());
        assertEquals(true,message.metadata().get("doubaoReasoningAbsent"));
        assertNotNull(message.metadata().get("protocolWarning"));
        assertFalse(message.metadata().containsKey("doubaoEncryptedContent"));
        assertNull(DurableHarnessRunProcessor.thinkingReplayViolation(run,List.of(message)));
    }
    @Test void missingProvenanceStillRejectsDroppedReasoning() {
        HarnessRunState run = run("bytedance/doubao-seed-2.1-pro-260628");
        assertNotNull(DurableHarnessRunProcessor.thinkingReplayViolation(run,List.of(map(run,Map.of()))));
    }
    @Test void omissionMarkerDoesNotRelaxOtherProviders() {
        HarnessRunState run = run("deepseek-v4-flash");
        assertNotNull(DurableHarnessRunProcessor.thinkingReplayViolation(run,List.of(map(run,
            Map.of(DoubaoStreamingChatModel.REASONING_ABSENT_ATTRIBUTE,true)))));
    }
    @Test void encryptedPayloadSurvivesExactly() {
        HarnessRunState run = run("bytedance/doubao-seed-2.1-pro-260628");
        HarnessMessage message = map(run,Map.of(DoubaoStreamingChatModel.ENCRYPTED_CONTENT_ATTRIBUTE,"opaque-unmodified"));
        assertEquals("opaque-unmodified",message.metadata().get("doubaoEncryptedContent"));
        assertNull(DurableHarnessRunProcessor.thinkingReplayViolation(run,List.of(message)));
    }
}
