package org.ruoyi.workflow.util;

import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.enums.RoleType;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.workflow.workflow.WfState;
import org.ruoyi.workflow.workflow.WorkflowUtil;

/**
 * 工作流消息工具类
 *
 * @author Zengxb
 * @date 2026-02-26
 */
@Slf4j
public class WorkflowMessageUtil {

    /**
     * 保存工作流消息公共方法（对话使用）
     * @param wfState 工作流实例状态
     * @param message 消息
     */
    public static void saveWorkflowMessage(WfState wfState, String message) {
        Long sessionId = wfState.getSessionId();
        Long userId = wfState.getUserId();

        if (sessionId != null && userId != null) {
            ChatRequest chatRequest = new ChatRequest();
            chatRequest.setSessionId(sessionId);
            WorkflowUtil workflowUtil = SpringUtils.getBean(WorkflowUtil.class);
            workflowUtil.saveChatMessage(chatRequest, userId, message, RoleType.WORKFLOW.getName(), new ChatModelVo());
        }
    }

}
