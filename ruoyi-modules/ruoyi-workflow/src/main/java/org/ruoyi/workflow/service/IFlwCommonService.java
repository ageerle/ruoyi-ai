package org.ruoyi.workflow.service;

import org.ruoyi.common.core.domain.dto.UserDTO;

import java.util.List;

/**
 * 通用 工作流服务
 *
 * @author LionLi
 */
public interface IFlwCommonService {

    /**
     * 发送消息
     *
     * @param flowName    流程定义名称
     * @param instId      实例id
     * @param messageType 消息类型
     * @param message     消息内容，为空则发送默认配置的消息内容
     */
    void sendMessage(String flowName, Long instId, List<String> messageType, String message);

    /**
     * 发送消息
     *
     * @param messageType 消息类型
     * @param message     消息内容
     * @param subject     邮件标题
     * @param userList    接收用户
     */
    void sendMessage(List<String> messageType, String message, String subject, List<UserDTO> userList);

    /**
     * 申请人节点编码
     *
     * @param definitionId 流程定义id
     * @return 申请人节点编码
     */
    String applyNodeCode(Long definitionId);
}
