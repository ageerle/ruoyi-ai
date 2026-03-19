//package org.ruoyi.service.chat.handler;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.ruoyi.common.chat.base.ThreadContext;
//import org.ruoyi.common.chat.domain.dto.request.WorkFlowRunner;
//import org.ruoyi.common.chat.entity.chat.ChatContext;
//import org.ruoyi.common.chat.service.workFlow.IWorkFlowStarterService;
//import org.ruoyi.common.core.utils.ObjectUtils;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//
///**
// * 工作流对话处理器
// * <p>
// * 处理 enableWorkFlow=true 的场景，启动工作流对话
// *
// * @author ageerle@163.com
// * @date 2025/12/13
// */
//@Slf4j
//@Component
//@Order(2)
//@RequiredArgsConstructor
//public class WorkflowChatHandler implements ChatHandler {
//
//    private final IWorkFlowStarterService workFlowStarterService;
//
//    @Override
//    public boolean supports(ChatContext context) {
//        Boolean enableWorkFlow = context.getChatRequest().getEnableWorkFlow();
//        return enableWorkFlow != null && enableWorkFlow;
//    }
//
//    @Override
//    public SseEmitter handle(ChatContext context) {
//        log.info("处理工作流对话，用户: {}, 会话: {}",
//            context.getUserId(), context.getChatRequest().getSessionId());
//
//        WorkFlowRunner runner = context.getChatRequest().getWorkFlowRunner();
//        if (ObjectUtils.isEmpty(runner)) {
//            log.warn("工作流参数为空");
//            return context.getEmitter();
//        }
//
//        return workFlowStarterService.streaming(
//            ThreadContext.getCurrentUser(),
//            runner.getUuid(),
//            runner.getInputs(),
//            context.getChatRequest().getSessionId()
//        );
//    }
//}
