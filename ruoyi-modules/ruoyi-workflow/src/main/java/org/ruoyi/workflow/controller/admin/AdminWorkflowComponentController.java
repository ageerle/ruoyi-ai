package org.ruoyi.workflow.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.workflow.dto.workflow.WfComponentReq;
import org.ruoyi.workflow.dto.workflow.WfComponentSearchReq;
import org.ruoyi.workflow.entity.WorkflowComponent;
import org.ruoyi.workflow.service.WorkflowComponentService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/workflow/component")
@Validated
public class AdminWorkflowComponentController {
    @Resource
    private WorkflowComponentService workflowComponentService;

    @PostMapping("/search")
    public R<Page<WorkflowComponent>> search(@RequestBody WfComponentSearchReq searchReq, @NotNull @Min(1) Integer currentPage, @NotNull @Min(10) Integer pageSize) {
        return R.ok(workflowComponentService.search(searchReq, currentPage, pageSize));
    }

    @PostMapping("/enable")
    public R enable(@RequestParam String uuid, @RequestParam Boolean isEnable) {
        workflowComponentService.enable(uuid, isEnable);
        return R.ok();
    }

    @PostMapping("/del/{uuid}")
    public R del(@PathVariable String uuid) {
        workflowComponentService.deleteByUuid(uuid);
        return R.ok();
    }


    @PostMapping("/addOrUpdate")
    public R<WorkflowComponent> addOrUpdate(@Validated @RequestBody WfComponentReq req) {
        return R.ok(workflowComponentService.addOrUpdate(req));
    }

}
