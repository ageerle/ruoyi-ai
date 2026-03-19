//package org.ruoyi.service.chat.handler;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.ruoyi.common.chat.domain.dto.request.ReSumeRunner;
//import org.ruoyi.common.chat.entity.chat.ChatContext;
//import org.ruoyi.common.chat.service.workFlow.IWorkFlowStarterService;
//import org.ruoyi.common.core.utils.ObjectUtils;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//
///**
// * 人机交互恢复处理器
// * <p>
// * 处理 isResume=true 的场景，恢复工作流的人机交互
// *
// * @author ageerle@163.com
// * @date 2025/12/13
// */
//@Slf4j
//@Component
//@Order(1)
//@RequiredArgsConstructor
//public class ResumeChatHandler implements ChatHandler {
//
//    private final IWorkFlowStarterService workFlowStarterService;
//
//    @Override
//    public boolean supports(ChatContext context) {
//        Boolean isResume = context.getChatRequest().getIsResume();
//        return isResume != null && isResume;
//    }
//
//    @Override
//    public SseEmitter handle(ChatContext context) {
//        log.info("处理人机交互恢复，用户: {}", context.getUserId());
//
//        ReSumeRunner reSumeRunner = context.getChatRequest().getReSumeRunner();
//        if (ObjectUtils.isEmpty(reSumeRunner)) {
//            log.warn("人机交互恢复参数为空");
//            return context.getEmitter();
//        }
//
//        workFlowStarterService.resumeFlow(
//            reSumeRunner.getRuntimeUuid(),
//            reSumeRunner.getFeedbackContent(),
//            context.getEmitter()
//        );
//
//        return context.getEmitter();
//    }
//}
