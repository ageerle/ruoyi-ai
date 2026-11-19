package org.ruoyi.chat.service.coding.harness.plan;

/** A durable user feedback item associated with a plan revision. */
public record PlanFeedback(
    String feedbackId,
    String content,
    long createdAt
) {

    public PlanFeedback {
        if (feedbackId == null || feedbackId.isBlank()) {
            throw new IllegalArgumentException("feedbackId must not be blank");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Plan feedback content must not be blank");
        }
        if (createdAt <= 0) {
            throw new IllegalArgumentException("Plan feedback createdAt must be positive");
        }
        feedbackId = feedbackId.strip();
        content = content.strip();
    }
}
