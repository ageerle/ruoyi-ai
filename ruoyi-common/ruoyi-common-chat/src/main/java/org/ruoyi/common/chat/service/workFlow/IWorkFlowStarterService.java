package org.ruoyi.common.chat.service.workFlow;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ruoyi.common.chat.entity.User;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 工作流启动Service接口
 *
 * @author Zengxb
 * @date 2026-02-24
 */
public interface IWorkFlowStarterService {

    /**
     * 启动工作流
     * @param user 用户
     * @param workflowUuid 工作流UUID
     * @param userInputs 用户输入信息
     * @return 流式输出结果
     */
    SseEmitter streaming(User user, String workflowUuid, List<ObjectNode> userInputs, Long sessionId);

    /**
     * 恢复工作流
      * @param runtimeUuid 运行时UUID
     * @param userInput 用户输入
     * @param sseEmitter SSE连接对象
     */
    void resumeFlow(String runtimeUuid, String userInput, SseEmitter sseEmitter);
}
