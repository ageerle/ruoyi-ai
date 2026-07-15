package org.ruoyi.service.shortdrama.impl;

import org.ruoyi.service.shortdrama.composition.AspectRatio;
import org.ruoyi.service.shortdrama.composition.TransitionType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

record ShortDramaVideoComposeJob(
    Long projectId,
    String jobId,
    String tenantId,
    TransitionType transitionType,
    BigDecimal transitionDurationSeconds,
    AspectRatio aspectRatio,
    List<Long> storyboardIds
) {

    ShortDramaVideoComposeJob {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(jobId, "jobId");
        Objects.requireNonNull(transitionType, "transitionType");
        Objects.requireNonNull(transitionDurationSeconds, "transitionDurationSeconds");
        Objects.requireNonNull(aspectRatio, "aspectRatio");
        storyboardIds = List.copyOf(Objects.requireNonNull(storyboardIds, "storyboardIds"));
    }
}
