package org.ruoyi.workflow.controller;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.domain.dto.StartProcessReturnDTO;
import org.ruoyi.common.core.domain.dto.UserDTO;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.web.core.BaseController;
import org.dromara.warm.flow.core.entity.Node;
import org.dromara.warm.flow.orm.entity.FlowNode;
import org.ruoyi.workflow.common.ConditionalOnEnable;
import org.ruoyi.workflow.domain.bo.*;
import org.ruoyi.workflow.domain.vo.FlowHisTaskVo;
import org.ruoyi.workflow.domain.vo.FlowTaskVo;
import org.ruoyi.workflow.service.IFlwTaskService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 任务管理 控制层
 *
 * @author may
 */
@ConditionalOnEnable
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/task")
public class FlwTaskController extends BaseController {

    private final IFlwTaskService flwTaskService;

    /**
     * 启动任务
     *
     * @param startProcessBo 启动流程参数
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/startWorkFlow")
    public R<StartProcessReturnDTO> startWorkFlow(@Validated(AddGroup.class) @RequestBody StartProcessBo startProcessBo) {
        StartProcessReturnDTO startProcessReturn = flwTaskService.startWorkFlow(startProcessBo);
        return R.ok("提交成功", startProcessReturn);
    }

    /**
     * 办理任务
     *
     * @param completeTaskBo 办理任务参数
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/completeTask")
    public R<Void> completeTask(@Validated(AddGroup.class) @RequestBody CompleteTaskBo completeTaskBo) {
        return toAjax(flwTaskService.completeTask(completeTaskBo));
    }

    /**
     * 查询当前用户的待办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @GetMapping("/pageByTaskWait")
    public TableDataInfo<FlowTaskVo> pageByTaskWait(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        return flwTaskService.pageByTaskWait(flowTaskBo, pageQuery);
    }

    /**
     * 查询当前用户的已办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */

    @GetMapping("/pageByTaskFinish")
    public TableDataInfo<FlowHisTaskVo> pageByTaskFinish(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        return flwTaskService.pageByTaskFinish(flowTaskBo, pageQuery);
    }

    /**
     * 查询待办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @GetMapping("/pageByAllTaskWait")
    public TableDataInfo<FlowTaskVo> pageByAllTaskWait(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        return flwTaskService.pageByAllTaskWait(flowTaskBo, pageQuery);
    }

    /**
     * 查询已办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @GetMapping("/pageByAllTaskFinish")
    public TableDataInfo<FlowHisTaskVo> pageByAllTaskFinish(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        return flwTaskService.pageByAllTaskFinish(flowTaskBo, pageQuery);
    }

    /**
     * 查询当前用户的抄送
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @GetMapping("/pageByTaskCopy")
    public TableDataInfo<FlowTaskVo> pageByTaskCopy(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        return flwTaskService.pageByTaskCopy(flowTaskBo, pageQuery);
    }

    /**
     * 根据taskId查询代表任务
     *
     * @param taskId 任务id
     */
    @GetMapping("/getTask/{taskId}")
    public R<FlowTaskVo> getTask(@PathVariable Long taskId) {
        return R.ok(flwTaskService.selectById(taskId));
    }

    /**
     * 获取下一节点信息
     *
     * @param bo 参数
     */
    @PostMapping("/getNextNodeList")
    public R<List<FlowNode>> getNextNodeList(@RequestBody FlowNextNodeBo bo) {
        return R.ok(flwTaskService.getNextNodeList(bo));
    }

    /**
     * 终止任务
     *
     * @param bo 参数
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/terminationTask")
    public R<Boolean> terminationTask(@RequestBody FlowTerminationBo bo) {
        return R.ok(flwTaskService.terminationTask(bo));
    }

    /**
     * 任务操作
     *
     * @param bo            参数
     * @param taskOperation 操作类型，委派 delegateTask、转办 transferTask、加签 addSignature、减签 reductionSignature
     */
    @Log(title = "任务管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/taskOperation/{taskOperation}")
    public R<Void> taskOperation(@Validated @RequestBody TaskOperationBo bo, @PathVariable String taskOperation) {
        return toAjax(flwTaskService.taskOperation(bo, taskOperation));
    }

    /**
     * 修改任务办理人
     *
     * @param taskIdList 任务id
     * @param userId     办理人id
     */
    @Log(title = "任务管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping("/updateAssignee/{userId}")
    public R<Void> updateAssignee(@RequestBody List<Long> taskIdList, @PathVariable String userId) {
        return toAjax(flwTaskService.updateAssignee(taskIdList, userId));
    }

    /**
     * 驳回审批
     *
     * @param bo 参数
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/backProcess")
    public R<Void> backProcess(@Validated({AddGroup.class}) @RequestBody BackProcessBo bo) {
        return toAjax(flwTaskService.backProcess(bo));
    }

    /**
     * 获取可驳回的前置节点
     *
     * @param taskId       任务id
     * @param nowNodeCode  当前节点
     */
    @GetMapping("/getBackTaskNode/{taskId}/{nowNodeCode}")
    public R<List<Node>> getBackTaskNode(@PathVariable Long taskId, @PathVariable String nowNodeCode) {
        return R.ok(flwTaskService.getBackTaskNode(taskId, nowNodeCode));
    }

    /**
     * 获取当前任务的所有办理人
     *
     * @param taskId 任务id
     */
    @GetMapping("/currentTaskAllUser/{taskId}")
    public R<List<UserDTO>> currentTaskAllUser(@PathVariable Long taskId) {
        return R.ok(flwTaskService.currentTaskAllUser(List.of(taskId)));
    }

    /**
     * 催办任务
     *
     * @param bo 参数
     * @return 结果
     */
    @PostMapping("/urgeTask")
    public R<Void> urgeTask(@RequestBody FlowUrgeTaskBo bo) {
        return toAjax(flwTaskService.urgeTask(bo));
    }


}
