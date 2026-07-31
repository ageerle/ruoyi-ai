package org.ruoyi.workflow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.workflow.dto.workflow.WfRuntimeNodeDto;
import org.ruoyi.workflow.dto.workflow.WfRuntimeResp;
import org.ruoyi.workflow.service.WorkflowRuntimeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workflow/runtime")
@Validated
public class WorkflowRuntimeController {

    @Resource
    private WorkflowRuntimeService workflowRuntimeService;

    @GetMapping("/page")
    public R<Page<WfRuntimeResp>> search(@RequestParam String wfUuid,
                                         @NotNull @Min(1) Integer currentPage,
                                         @NotNull @Min(10) Integer pageSize) {
        return R.ok(workflowRuntimeService.page(wfUuid, currentPage, pageSize));
    }

    @GetMapping("/nodes/{runtimeUuid}")
    public R<List<WfRuntimeNodeDto>> listByRuntimeId(@PathVariable String runtimeUuid) {
        return R.ok(workflowRuntimeService.listByRuntimeUuid(runtimeUuid));
    }

    @PostMapping("/clear")
    public R<Boolean> clear(@RequestParam(defaultValue = "") String wfUuid) {
        return R.ok(workflowRuntimeService.deleteAll(wfUuid));
    }

    @PostMapping("/del/{wfRuntimeUuid}")
    public R<Boolean> delete(@PathVariable String wfRuntimeUuid) {
        return R.ok(workflowRuntimeService.softDelete(wfRuntimeUuid));
    }
}
