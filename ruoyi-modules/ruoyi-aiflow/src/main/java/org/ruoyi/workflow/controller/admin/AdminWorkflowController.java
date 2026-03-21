package org.ruoyi.workflow.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.workflow.dto.workflow.WfSearchReq;
import org.ruoyi.workflow.dto.workflow.WorkflowResp;
import org.ruoyi.workflow.service.WorkflowService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/workflow")
@Validated
public class AdminWorkflowController {

    @Resource
    private WorkflowService workflowService;

    @PostMapping("/search")
    public R<Page<WorkflowResp>> search(@RequestBody WfSearchReq req,
                                        @RequestParam @NotNull @Min(1) Integer currentPage,
                                        @RequestParam @NotNull @Min(10) Integer pageSize) {
        return R.ok(workflowService.search(req.getTitle(), req.getIsPublic(),
                req.getIsEnable(), currentPage, pageSize));
    }

    @PostMapping("/enable")
    public R enable(@RequestParam String uuid, @RequestParam Boolean isEnable) {
        workflowService.enable(uuid, isEnable);
        return R.ok();
    }
}
