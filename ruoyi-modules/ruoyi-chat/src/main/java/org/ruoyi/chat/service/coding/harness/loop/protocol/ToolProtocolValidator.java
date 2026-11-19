package org.ruoyi.chat.service.coding.harness.loop.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ruoyi.chat.service.coding.harness.model.HarnessMessage;
import org.ruoyi.chat.service.coding.harness.model.HarnessMessageRole;
import org.ruoyi.chat.service.coding.harness.model.HarnessToolCall;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Validates the provider tool protocol and projects concurrently completed results back into
 * assistant source order. CONTROL records are deliberately invisible to model adjacency.
 */
public final class ToolProtocolValidator {

    private final ObjectMapper objectMapper;

    public ToolProtocolValidator() {
        this(new ObjectMapper());
    }

    public ToolProtocolValidator(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper").copy()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }

    public ToolProtocolValidation validate(List<HarnessMessage> ledgerMessages) {
        List<HarnessMessage> messages = ledgerMessages == null ? List.of() : ledgerMessages;
        List<HarnessMessage> modelMessages = new ArrayList<>();
        List<ToolBatchProjection> batches = new ArrayList<>();
        List<ToolProtocolViolation> violations = new ArrayList<>();
        Set<String> seenCallIds = new HashSet<>();
        Set<String> seenResultIds = new HashSet<>();
        MutableBatch open = null;

        for (HarnessMessage message : messages) {
            if (message == null) {
                add(violations, null, ToolProtocolViolationCode.NULL_MESSAGE, -1, null,
                    "Ledger contains a null message");
                continue;
            }
            if (message.role() == HarnessMessageRole.CONTROL) {
                continue;
            }

            if (open != null && message.role() == HarnessMessageRole.TOOL) {
                acceptResult(open, message, seenResultIds, violations);
                if (open.allExpectedResultsPresent()) {
                    finish(open, modelMessages, batches);
                    open = null;
                }
                continue;
            }

            if (open != null) {
                ToolProtocolViolationCode code = switch (message.role()) {
                    case USER -> ToolProtocolViolationCode.USER_DURING_OPEN_BATCH;
                    case ASSISTANT -> ToolProtocolViolationCode.ASSISTANT_DURING_OPEN_BATCH;
                    case SYSTEM -> ToolProtocolViolationCode.SYSTEM_DURING_OPEN_BATCH;
                    case CONTROL, TOOL -> throw new IllegalStateException("Unexpected role branch");
                };
                add(violations, open, code, message.sequence(), null,
                    message.role() + " message interrupted an unresolved tool batch");
                finish(open, modelMessages, batches);
                open = null;
            }

            switch (message.role()) {
                case SYSTEM, USER -> {
                    if (message.content() == null) {
                        add(violations, null, ToolProtocolViolationCode.INVALID_MESSAGE_CONTENT,
                            message.sequence(), null,
                            message.role() + " message has null content");
                    }
                    modelMessages.add(message);
                }
                case ASSISTANT -> {
                    if (message.toolCalls().isEmpty()) {
                        if (isBlank(message.content()) && isBlank(message.thinking())) {
                            add(violations, null,
                                ToolProtocolViolationCode.EMPTY_ASSISTANT_MESSAGE,
                                message.sequence(), null,
                                "Assistant message has no text, thinking, or tool calls");
                        }
                        modelMessages.add(message);
                    } else {
                        open = startBatch(message, seenCallIds, violations);
                    }
                }
                case TOOL -> {
                    ToolProtocolViolationCode code = seenResultIds.contains(message.toolCallId())
                        ? ToolProtocolViolationCode.DUPLICATE_RESULT
                        : ToolProtocolViolationCode.ORPHAN_RESULT;
                    seenResultIds.add(message.toolCallId());
                    add(violations, null, code, message.sequence(), message.toolCallId(),
                        code == ToolProtocolViolationCode.DUPLICATE_RESULT
                            ? "Tool call already has a result"
                            : "Tool result has no adjacent assistant call batch");
                    modelMessages.add(message);
                }
                case CONTROL -> throw new IllegalStateException("CONTROL must be filtered");
            }
        }

        if (open != null) {
            finish(open, modelMessages, batches);
        }

        boolean validBatches = batches.stream().allMatch(ToolBatchProjection::complete);
        HarnessMessageRole tailRole = modelMessages.isEmpty()
            ? null : modelMessages.get(modelMessages.size() - 1).role();
        boolean atRequestBoundary = tailRole == HarnessMessageRole.USER
            || tailRole == HarnessMessageRole.TOOL;
        boolean allowsNextModelRequest = violations.isEmpty() && validBatches && atRequestBoundary;
        return new ToolProtocolValidation(modelMessages, batches, violations,
            allowsNextModelRequest);
    }

    private MutableBatch startBatch(HarnessMessage assistant, Set<String> seenCallIds,
                                    List<ToolProtocolViolation> allViolations) {
        MutableBatch batch = new MutableBatch(assistant);
        for (HarnessToolCall call : assistant.toolCalls()) {
            if (!seenCallIds.add(call.toolCallId())) {
                add(allViolations, batch, ToolProtocolViolationCode.DUPLICATE_CALL_ID,
                    assistant.sequence(), call.toolCallId(),
                    "Tool call id must be unique in the transcript");
            }
            if (!validArguments(call.arguments())) {
                // A unique call id is still pairable. The processor must write an explicit
                // invalid_tool_call result instead of poisoning the whole session or inventing a
                // model request boundary. The result is checked in acceptResult before the batch
                // can become valid.
                batch.invalidArgumentCallIds.add(call.toolCallId());
            }
            batch.expected.putIfAbsent(call.toolCallId(), call);
        }
        return batch;
    }

    private void acceptResult(MutableBatch batch, HarnessMessage result,
                              Set<String> seenResultIds,
                              List<ToolProtocolViolation> allViolations) {
        String callId = result.toolCallId();
        if (!seenResultIds.add(callId)) {
            add(allViolations, batch, ToolProtocolViolationCode.DUPLICATE_RESULT,
                result.sequence(), callId, "Tool call already has a result");
            return;
        }
        HarnessToolCall source = batch.expected.get(callId);
        if (source == null) {
            add(allViolations, batch, ToolProtocolViolationCode.ORPHAN_RESULT,
                result.sequence(), callId,
                "Tool result does not belong to the open assistant batch");
            return;
        }
        if (!Objects.equals(source.toolName(), result.toolName())) {
            add(allViolations, batch, ToolProtocolViolationCode.TOOL_NAME_MISMATCH,
                result.sequence(), callId,
                "Tool result name does not match its source call");
        }
        if (result.content() == null || result.toolName() == null
            || result.toolName().isBlank()) {
            add(allViolations, batch, ToolProtocolViolationCode.INVALID_TOOL_RESULT,
                result.sequence(), callId,
                "Tool result requires a tool name and non-null content");
        }
        if (batch.invalidArgumentCallIds.contains(callId)
            && !isInvalidArgumentRejection(result)) {
            add(allViolations, batch, ToolProtocolViolationCode.INVALID_TOOL_CALL_ARGUMENTS,
                result.sequence(), callId,
                "Malformed tool arguments require an explicit invalid_tool_call rejection");
        }
        batch.results.put(callId, result);
    }

    private boolean isInvalidArgumentRejection(HarnessMessage result) {
        return result.toolError()
            && "invalid_tool_call".equals(result.metadata().get("code"))
            && "INVALID".equals(result.metadata().get("syntheticReason"));
    }

    private boolean validArguments(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return false;
        }
        try {
            JsonNode value = objectMapper.readTree(arguments);
            return value != null && value.isObject();
        } catch (JsonProcessingException exception) {
            return false;
        }
    }

    private void finish(MutableBatch batch, List<HarnessMessage> modelMessages,
                        List<ToolBatchProjection> batches) {
        List<HarnessMessage> orderedResults = new ArrayList<>();
        List<HarnessToolCall> missingCalls = new ArrayList<>();
        Set<String> projectedIds = new HashSet<>();
        for (HarnessToolCall call : batch.assistant.toolCalls()) {
            if (!projectedIds.add(call.toolCallId())) {
                continue;
            }
            HarnessMessage result = batch.results.get(call.toolCallId());
            if (result == null) {
                missingCalls.add(call);
            } else {
                orderedResults.add(result);
            }
        }
        ToolBatchProjection projection = new ToolBatchProjection(batch.assistant,
            batch.assistant.toolCalls(), orderedResults, missingCalls, batch.violations);
        batches.add(projection);
        modelMessages.add(batch.assistant);
        modelMessages.addAll(orderedResults);
    }

    private void add(List<ToolProtocolViolation> allViolations, MutableBatch batch,
                     ToolProtocolViolationCode code, long sequence, String callId,
                     String detail) {
        ToolProtocolViolation violation = new ToolProtocolViolation(code, sequence, callId, detail);
        allViolations.add(violation);
        if (batch != null) {
            batch.violations.add(violation);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static final class MutableBatch {
        private final HarnessMessage assistant;
        private final Map<String, HarnessToolCall> expected = new LinkedHashMap<>();
        private final Map<String, HarnessMessage> results = new LinkedHashMap<>();
        private final Set<String> invalidArgumentCallIds = new HashSet<>();
        private final List<ToolProtocolViolation> violations = new ArrayList<>();

        private MutableBatch(HarnessMessage assistant) {
            this.assistant = assistant;
        }

        private boolean allExpectedResultsPresent() {
            return !expected.isEmpty() && results.keySet().containsAll(expected.keySet());
        }
    }
}
