package org.ruoyi.chat.service.coding.harness.plan;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Versioned, length-prefixed canonical encoding independent of JSON mapper configuration. */
final class CanonicalPlanHasher {

    private static final String FORMAT_V1 = "ruoyi-coding-plan-aggregate-v1";
    private static final String FORMAT_V2 = "ruoyi-coding-plan-aggregate-v2";

    private CanonicalPlanHasher() {
    }

    static String hash(PlanAggregate aggregate) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(buffer)) {
                writeString(out, aggregate.schemaVersion() >= 2 ? FORMAT_V2 : FORMAT_V1);
                out.writeInt(aggregate.schemaVersion());
                writeString(out, aggregate.taskId().toString());
                out.writeLong(aggregate.revision());
                writeString(out, aggregate.mode().name());
                writeString(out, aggregate.reviewState().name());
                writeContract(out, aggregate.contract());
                writeString(out, aggregate.originalRequest());
                writeString(out, aggregate.planMarkdown());
                if (aggregate.schemaVersion() >= 2) {
                    writeSteps(out, aggregate.steps());
                }
                writeFeedback(out, aggregate.feedbackHistory());
                writeEvidence(out, aggregate.evidence());
                writeReceipts(out, aggregate.approvalReceipts());
                writeString(out, aggregate.blockedFromMode() == null
                    ? null : aggregate.blockedFromMode().name());
                writeString(out, aggregate.blockedReason());
                writeString(out, aggregate.failureReason());
                out.writeLong(aggregate.createdAt());
                out.writeLong(aggregate.updatedAt());
            }
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(buffer.toByteArray()));
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to calculate canonical plan hash", e);
        }
    }

    private static void writeSteps(DataOutputStream out, List<PlanTaskStep> steps)
        throws IOException {
        out.writeInt(steps.size());
        for (PlanTaskStep step : steps) {
            writeString(out, step.stepId());
            writeString(out, step.title());
            writeString(out, step.instructions());
            writeString(out, step.status().name());
            writeStrings(out, step.dependencyIds());
            writeStrings(out, step.acceptanceCriterionIds());
            writeStrings(out, step.completionEvidenceIds());
            writeString(out, step.statusReason());
            out.writeInt(step.attempt());
        }
    }

    private static void writeContract(DataOutputStream out, TaskContract contract) throws IOException {
        writeString(out, contract.contractId().toString());
        writeString(out, contract.kind());
        writeString(out, contract.normalizedGoal());
        out.writeInt(contract.criteria().size());
        for (AcceptanceCriterion criterion : contract.criteria()) {
            writeString(out, criterion.id());
            writeString(out, criterion.type());
            writeString(out, criterion.expected());
            writeString(out, criterion.evidenceKey());
        }
        writeSortedStrings(out, contract.allowedMutationRoots());
        writeSortedStrings(out, contract.forbiddenOperations());
    }

    private static void writeFeedback(DataOutputStream out, List<PlanFeedback> feedback)
        throws IOException {
        out.writeInt(feedback.size());
        for (PlanFeedback item : feedback) {
            writeString(out, item.feedbackId());
            writeString(out, item.content());
            out.writeLong(item.createdAt());
        }
    }

    private static void writeEvidence(DataOutputStream out, List<ExecutionEvidence> evidence)
        throws IOException {
        out.writeInt(evidence.size());
        for (ExecutionEvidence item : evidence) {
            writeString(out, item.evidenceId());
            writeString(out, item.type());
            writeString(out, item.canonicalKey());
            writeString(out, item.digest());
            out.writeBoolean(item.successful());
            writeString(out, item.summary());
            writeStringMap(out, item.attributes());
            out.writeLong(item.observedAt());
        }
    }

    private static void writeReceipts(DataOutputStream out,
                                      Map<String, PlanApprovalReceipt> receipts) throws IOException {
        List<Map.Entry<String, PlanApprovalReceipt>> entries = receipts.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .toList();
        out.writeInt(entries.size());
        for (Map.Entry<String, PlanApprovalReceipt> entry : entries) {
            PlanApprovalReceipt receipt = entry.getValue();
            writeString(out, entry.getKey());
            writeString(out, receipt.taskId().toString());
            out.writeLong(receipt.expectedRevision());
            writeString(out, receipt.expectedHash());
            out.writeLong(receipt.approvedRevision());
            out.writeLong(receipt.approvedAt());
        }
    }

    private static void writeSortedStrings(DataOutputStream out, Set<String> values)
        throws IOException {
        List<String> sorted = values.stream().sorted(Comparator.naturalOrder()).toList();
        out.writeInt(sorted.size());
        for (String value : sorted) {
            writeString(out, value);
        }
    }

    private static void writeStrings(DataOutputStream out, List<String> values)
        throws IOException {
        out.writeInt(values.size());
        for (String value : values) {
            writeString(out, value);
        }
    }

    private static void writeStringMap(DataOutputStream out, Map<String, String> values)
        throws IOException {
        List<Map.Entry<String, String>> sorted = values.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .toList();
        out.writeInt(sorted.size());
        for (Map.Entry<String, String> entry : sorted) {
            writeString(out, entry.getKey());
            writeString(out, entry.getValue());
        }
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        if (value == null) {
            out.writeInt(-1);
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }
}
