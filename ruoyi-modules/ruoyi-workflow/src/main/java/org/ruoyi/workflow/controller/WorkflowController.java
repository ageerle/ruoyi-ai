package org.ruoyi.workflow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.workflow.base.ThreadContext;
import org.ruoyi.workflow.dto.workflow.*;
import org.ruoyi.workflow.entity.WorkflowComponent;
import org.ruoyi.workflow.service.WorkflowComponentService;
import org.ruoyi.workflow.service.WorkflowService;
import org.ruoyi.workflow.workflow.WorkflowStarter;
import org.ruoyi.workflow.workflow.node.switcher.OperatorEnum;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workflow")
@Validated
public class WorkflowController {

    @Resource
    private WorkflowStarter workflowStarter;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private WorkflowComponentService workflowComponentService;

    @PostMapping("/add")
    public R<WorkflowResp> add(@RequestBody @Validated WfAddReq addReq) {
        return R.ok(workflowService.add(addReq.getTitle(), addReq.getRemark(), addReq.getIsPublic()));
    }

    @PostMapping("/set-public/{wfUuid}")
    public R setPublic(@PathVariable String wfUuid, @RequestParam(defaultValue = "true") Boolean isPublic) {
        workflowService.setPublic(wfUuid, isPublic);
        return R.ok();
    }

    @PostMapping("/update")
    public R<WorkflowResp> update(@RequestBody @Validated WorkflowUpdateReq req) {
        return R.ok(workflowService.update(req));
    }

    @PostMapping("/del/{uuid}")
    public R delete(@PathVariable String uuid) {
        workflowService.softDelete(uuid);
        return R.ok();
    }

    @PostMapping("/enable/{uuid}")
    public R enable(@PathVariable String uuid, @RequestParam Boolean enable) {
        workflowService.enable(uuid, enable);
        return R.ok();
    }

    @PostMapping("/base-info/update")
    public R<WorkflowResp> updateBaseInfo(@RequestBody @Validated WfBaseInfoUpdateReq req) {
        return R.ok(workflowService.updateBaseInfo(req.getUuid(), req.getTitle(), req.getRemark(), req.getIsPublic()));
    }

    @Operation(summary = "流式响应")
    @PostMapping(value = "/run", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sseAsk(@RequestBody WorkflowRunReq runReq) {
        return workflowStarter.streaming(ThreadContext.getCurrentUser(), runReq.getUuid(), runReq.getInputs());
    }

    @GetMapping("/mine/search")
    public R<Page<WorkflowResp>> searchMine(@RequestParam(defaultValue = "") String keyword,
                                            @RequestParam(required = false) Boolean isPublic,
                                            @NotNull @Min(1) Integer currentPage,
                                            @NotNull @Min(10) Integer pageSize) {
        return R.ok(workflowService.search(keyword, isPublic, null, currentPage, pageSize));
    }

    /**
     * 获取当前用户可访问的工作流详情
     *
     * @param uuid 工作流唯一标识
     * @return 工作流详情
     */
    @GetMapping("/{uuid}")
    public R<WorkflowResp> getDetail(@PathVariable String uuid) {
        return R.ok(workflowService.getDetail(uuid));
    }

    /**
     * 搜索公开工作流
     *
     * @param keyword     搜索关键词
     * @param currentPage 当前页数
     * @param pageSize    每页数量
     * @return 工作流列表
     */
    @GetMapping("/public/search")
    public R<Page<WorkflowResp>> searchPublic(@RequestParam(defaultValue = "") String keyword,
                                              @NotNull @Min(1) Integer currentPage,
                                              @NotNull @Min(10) Integer pageSize) {
        return R.ok(workflowService.searchPublic(keyword, currentPage, pageSize));
    }


    /**
     * 搜索公开工作流
     *
     * @param keyword     搜索关键词
     * @param currentPage 当前页数
     * @param pageSize    每页数量
     * @return 工作流列表
     */
    @GetMapping("/search")
    public R<Page<WorkflowResp>> search(@RequestParam(defaultValue = "") String keyword,
                                        @NotNull @Min(1) Integer currentPage,
                                        @NotNull @Min(10) Integer pageSize) {
        return R.ok(workflowService.search(keyword, currentPage, pageSize));
    }

    /**
     * 获取公开工作流详情
     *
     * @param uuid 工作流唯一标识
     * @return 工作流详情
     */
    @GetMapping("/public/{uuid}")
    public R<WorkflowResp> getPublicDetail(@PathVariable String uuid) {
        return R.ok(workflowService.getPublicDetail(uuid));
    }

    @GetMapping("/public/operators")
    public R<List<Map<String, String>>> searchPublic() {
        List<Map<String, String>> result = new ArrayList<>();
        for (OperatorEnum operator : OperatorEnum.values()) {
            result.add(Map.of("name", operator.getName(), "desc", operator.getDesc()));
        }
        return R.ok(result);
    }

    @GetMapping("/public/component/list")
    public R<List<WorkflowComponent>> component() {
        return R.ok(workflowComponentService.getAllEnable());
    }
}
