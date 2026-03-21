package org.ruoyi.workflow.dto.workflow;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WorkflowResp {
    private Long id;
    private String uuid;
    private String title;
    private String remark;
    private Boolean isPublic;
    private Long userId;
    private String userUuid;
    private String userName;
    private List<WfNodeDto> nodes;
    private List<WfEdgeReq> edges;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
