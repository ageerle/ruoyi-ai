package org.ruoyi.workflow.workflow;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.entity.User;
import org.ruoyi.common.chat.service.workFlow.IWorkFlowStarterService;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.sse.core.SseEmitterManager;
import org.ruoyi.common.tenant.helper.TenantHelper;
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
        // 获取登录Token（仅透传给 WfState，工作流 SSE 通过 emitter 直发，不串台）
        String tokenValue = StpUtil.getTokenValue();
        // 获取当前租户ID（@Async 线程不继承请求线程的租户上下文，需显式透传）
        String tenantId = TenantHelper.getTenantId();
        // 根据会话ID连接SSE对象（每会话一个连接，避免同用户多会话串台）
        SseEmitter sseEmitter = sseEmitterManager.connect(String.valueOf(sessionId));
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
        self.asyncRun(user, workflow, userInputs, sseEmitter, userId, tokenValue, sessionId, tenantId);
        return sseEmitter;
    }

    @Async
    public void asyncRun(User user, Workflow workflow, List<ObjectNode> userInputs, SseEmitter sseEmitter, Long userId, String tokenValue, Long sessionId, String tenantId) {
        // @Async 线程不继承请求线程的租户上下文, 显式设置, 避免租户缓存/隔离逻辑异常
        if (tenantId != null) {
            TenantHelper.setDynamic(tenantId);
        }
        try {
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
        } finally {
            TenantHelper.clearDynamic();
        }
    }
}
