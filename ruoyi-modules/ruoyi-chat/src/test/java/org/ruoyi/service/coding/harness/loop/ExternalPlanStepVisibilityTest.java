package org.ruoyi.service.coding.harness.loop;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.service.coding.harness.loop.tool.HarnessToolRegistry;
import org.ruoyi.service.coding.harness.model.*;
import org.ruoyi.service.coding.harness.plan.ExecutionMode;
import org.ruoyi.service.coding.harness.plan.PlanAggregate;
import org.ruoyi.service.coding.harness.plan.PlanTaskStep;
import org.ruoyi.service.coding.harness.plan.tool.HarnessPlanCommandService;
import org.ruoyi.service.coding.harness.runtime.HarnessRunRequest;
import org.ruoyi.service.coding.harness.tool.ToolCapability;
import org.ruoyi.service.coding.harness.tool.ToolDescriptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ExternalPlanStepVisibilityTest {
    @Test
    void externalImplementationCanExplicitlyFinishItsActiveStep() {
        var registry = effectiveRegistry(HarnessVerificationMode.EXTERNAL);
        assertTrue(registry.descriptor("plan_step").isPresent());
        assertTrue(registry.descriptor("write_file").isPresent());
        assertTrue(registry.descriptor("plan_verify").isPresent());
    }

    @Test
    void agentVerificationRetainsAutomaticProgressionPolicy() {
        var registry = effectiveRegistry(HarnessVerificationMode.AGENT);
        assertTrue(registry.descriptor("write_file").isPresent());
        assertTrue(registry.descriptor("plan_verify").isPresent());
        assertFalse(registry.descriptor("plan_step").isPresent());
    }

    private HarnessToolRegistry effectiveRegistry(HarnessVerificationMode mode) {
        var processor = mock(DurableHarnessRunProcessor.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(processor, "planCommands", mock(HarnessPlanCommandService.class));
        var run = mock(HarnessRunState.class, RETURNS_DEEP_STUBS);
        var session = mock(HarnessSessionState.class);
        var plan = mock(PlanAggregate.class, RETURNS_DEEP_STUBS);
        var step = mock(PlanTaskStep.class);
        when(session.verificationMode()).thenReturn(mode);
        when(run.permissionMode()).thenReturn(HarnessPermissionMode.WORKSPACE_WRITE);
        when(run.executionPlan()).thenReturn(plan);
        when(plan.mode()).thenReturn(ExecutionMode.BUILD);
        when(plan.inProgressStep()).thenReturn(Optional.of(step));
        when(plan.contract().criteria()).thenReturn(List.of());
        when(plan.evidence()).thenReturn(List.of());
        when(step.acceptanceCriterionIds()).thenReturn(List.of());
        var registry = HarnessToolRegistry.builder(new ObjectMapper()).registerAnnotated(
            new Tools(), List.of(descriptor("plan_step"), descriptor("plan_verify"), descriptor("write_file"))).build();
        return ReflectionTestUtils.invokeMethod(processor, "effectiveRegistry",
            new HarnessRunRequest(new HarnessOwner("000000", 1L), "session", "run"),
            run, session, registry, false, false);
    }

    private ToolDescriptor descriptor(String name) {
        return new ToolDescriptor(name, Set.of(ToolCapability.CONTROL), true, 1000,
            1024, 1024, true, "Test tool visibility only");
    }

    static class Tools {
        @Tool(name = "plan_step", value = "Complete an active step")
        public String step() { return "test"; }
        @Tool(name = "plan_verify", value = "Enter verification")
        public String verify() { return "test"; }
        @Tool(name = "write_file", value = "Write implementation")
        public String write() { return "test"; }
    }
}
