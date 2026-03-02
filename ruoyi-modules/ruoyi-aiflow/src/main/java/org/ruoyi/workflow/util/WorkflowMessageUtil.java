package org.ruoyi.workflow.util;

import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.enums.RoleType;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.workflow.entity.WorkflowNode;
import org.ruoyi.workflow.helper.SSEEmitterHelper;
import org.ruoyi.workflow.workflow.WfState;
import org.ruoyi.workflow.workflow.WorkflowUtil;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 工作流消息工具类
 *
 * @author Zengxb
 * @date 2026-02-26
 */
@Slf4j
public class WorkflowMessageUtil {


    /**
     * 通知并存储消息(对话使用)
     * @param wfState 工作流实例状态
     * @param sseEmitter SSE连接对象
     * @param node 工作流节点
     * @param message 消息
     */
    public static void notifyAndStoreMessage(WfState wfState, SseEmitter sseEmitter, WorkflowNode node, String message){
        saveWorkflowMessage(wfState, message);
        sendEmitterMessage(sseEmitter, node, message);
    }


    /**
     * 获取节点的响应模板
     * @param configKey 参数Key
     * @return 返回模板样式
     */
    public static String getNodeMessageTemplate(String configKey){
        ConfigService configService = SpringUtil.getBean(ConfigService.class);
        String configValue = configService.getConfigValue(configKey);
        if (StringUtils.isEmpty(configValue)) {
            throw new ServiceException("请先配置该节点的响应模板");
        }
        return configValue;
    }

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

    /**
     * 发送SSE消息
     * @param sseEmitter 连接对象
     * @param node 工作流定义
     * @param message 消息
     */
    public static void sendEmitterMessage(SseEmitter sseEmitter, WorkflowNode node, String message) {
        String nodeUuid = node.getUuid();
        SSEEmitterHelper.parseAndSendPartialMsg(sseEmitter,"[NODE_CHUNK_" + nodeUuid + "]", message);
    }

}
