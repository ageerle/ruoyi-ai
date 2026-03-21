package org.ruoyi.workflow.workflow;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.entity.User;
import org.ruoyi.common.chat.service.workFlow.IWorkFlowStarterService;
import org.ruoyi.common.core.exception.base.BaseException;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.sse.core.SseEmitterManager;
import org.ruoyi.workflow.entity.*;
import org.ruoyi.workflow.helper.SSEEmitterHelper;
import org.ruoyi.workflow.service.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.ruoyi.common.chat.enums.ErrorEnum.*;

@Slf4j
@Service
public class WorkflowStarter implements IWorkFlowStarterService {

    @Lazy
    @Resource
    private WorkflowStarter self;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private WorkflowNodeService workflowNodeService;

    @Resource
    private WorkflowEdgeService workflowEdgeService;

    @Resource
    private WorkflowComponentService workflowComponentService;

    @Resource
    private WorkflowRuntimeService workflowRuntimeService;

    @Resource
    private WorkflowRuntimeNodeService workflowRuntimeNodeService;

    @Resource
    private SSEEmitterHelper sseEmitterHelper;

    @Resource
    private SseEmitterManager sseEmitterManager;

    public SseEmitter streaming(User user, String workflowUuid, List<ObjectNode> userInputs, Long sessionId) {
        // 获取用户ID
        Long userId = LoginHelper.getUserId();
        // 获取登录Token
        String tokenValue = StpUtil.getTokenValue();
        // 根据用户ID和Token连接SSE对象
        SseEmitter sseEmitter = sseEmitterManager.connect(userId, tokenValue);
        if (!sseEmitterHelper.checkOrComplete(user, sseEmitter)) {
            return sseEmitter;
        }
        Workflow workflow = workflowService.getByUuid(workflowUuid);
        if (null == workflow) {
            sseEmitterHelper.sendErrorAndComplete(user.getId(), sseEmitter, A_WF_NOT_FOUND.getInfo());
            return sseEmitter;
        } else if (Boolean.FALSE.equals(workflow.getIsEnable())) {
            sseEmitterHelper.sendErrorAndComplete(user.getId(), sseEmitter, A_WF_DISABLED.getInfo());
            return sseEmitter;
        }
        self.asyncRun(user, workflow, userInputs, sseEmitter, userId, tokenValue, sessionId);
        return sseEmitter;
    }

    @Async
    public void asyncRun(User user, Workflow workflow, List<ObjectNode> userInputs, SseEmitter sseEmitter, Long userId, String tokenValue, Long sessionId) {
        log.info("WorkflowEngine run,userId:{},workflowUuid:{},userInputs:{}", user.getId(), workflow.getUuid(), userInputs);
        List<WorkflowComponent> components = workflowComponentService.getAllEnable();
        List<WorkflowNode> nodes = workflowNodeService.lambdaQuery()
                .eq(WorkflowNode::getWorkflowId, workflow.getId())
                .eq(WorkflowNode::getIsDeleted, false)
                .list();
        List<WorkflowEdge> edges = workflowEdgeService.lambdaQuery()
                .eq(WorkflowEdge::getWorkflowId, workflow.getId())
                .eq(WorkflowEdge::getIsDeleted, false)
                .list();
        WorkflowEngine workflowEngine = new WorkflowEngine(workflow,
                sseEmitterHelper, components, nodes, edges,
                workflowRuntimeService, workflowRuntimeNodeService);
        workflowEngine.run(user, userInputs, sseEmitter, userId, tokenValue, sessionId);
    }

    @Async
    public void resumeFlow(String runtimeUuid, String userInput, SseEmitter sseEmitter) {
        WorkflowEngine workflowEngine = InterruptedFlow.RUNTIME_TO_GRAPH.get(runtimeUuid);
        if (null == workflowEngine) {
            log.error("工作流恢复执行时失败,runtime:{}", runtimeUuid);
            throw new BaseException(A_WF_RESUME_FAIL.getInfo());
        }
        // 如果SSE连接对象不为空传入该对象（Chat调用工作流对话使用）
        if (null != sseEmitter){
            workflowEngine.setSseEmitter(sseEmitter);
            // 为了让每个节点都可以发送模板消息 保持SSE对象一致（以防出现向已关闭的SSE对象发送消息）
            workflowEngine.getWfState().setSseEmitter(sseEmitter);
        }
        workflowEngine.resume(userInput);
    }
}
