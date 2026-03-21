package org.ruoyi.workflow.dto.workflow;

import lombok.Data;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Data
public class WorkflowResumeReq {
    private String feedbackContent;
    private SseEmitter sseEmitter;
}
