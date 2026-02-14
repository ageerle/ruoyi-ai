package org.ruoyi.workflow.workflow;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.langchain4j.generators.StreamingChatGenerator;
import org.bsc.langgraph4j.state.AgentState;
import org.ruoyi.common.chat.Service.IChatModelService;
import org.ruoyi.common.chat.Service.IChatService;
import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.entity.chat.ChatContext;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.factory.ChatServiceFactory;
import org.ruoyi.workflow.base.NodeInputConfigTypeHandler;
import org.ruoyi.workflow.entity.WorkflowNode;
import org.ruoyi.workflow.enums.WfIODataTypeEnum;
import org.ruoyi.workflow.util.JsonUtil;
import org.ruoyi.workflow.workflow.data.NodeIOData;
import org.ruoyi.workflow.workflow.data.NodeIODataContent;
import org.ruoyi.workflow.workflow.def.WfNodeParamRef;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

import static org.ruoyi.workflow.cosntant.AdiConstant.WorkflowConstant.DEFAULT_OUTPUT_PARAM_NAME;

@Slf4j
@Component
public class WorkflowUtil {

    @Resource
    private ChatServiceFactory chatServiceFactory;

    @Resource
    private IChatModelService chatModelService;

    public static String renderTemplate(String template, List<NodeIOData> values) {
        // 🔒 关键修复：如果 template 为 null，直接返回 null 或空字符串
        if (template == null) {
            return null; // 或 return ""; 根据业务需求
        }

        String result = template;

        // 防御 values 为 null
        if (values == null) {
            return result;
        }

        for (NodeIOData next : values) {
            if (next == null || next.getName() == null) {
                continue;
            }

            String name = next.getName();
            NodeIODataContent<?> dataContent = next.getContent();
            if (dataContent == null || dataContent.getValue() == null) {
                // 变量值为 null，替换为空字符串
                result = result.replace("{" + name + "}", "");
                continue;
            }

            String replacement;
            if (dataContent.getType().equals(WfIODataTypeEnum.FILES.getValue())) {
                @SuppressWarnings("unchecked")
                List<String> value = (List<String>) dataContent.getValue();
                replacement = String.join(",", value);
            } else if (dataContent.getType().equals(WfIODataTypeEnum.OPTIONS.getValue())) {
                @SuppressWarnings("unchecked")
                Map<String, Object> value = (Map<String, Object>) dataContent.getValue();
                replacement = value.toString();
            } else {
                replacement = dataContent.getValue().toString();
            }

            result = result.replace("{" + name + "}", replacement);
        }

        return result;
    }

    public static String getHumanFeedbackTip(String nodeUuid, List<WorkflowNode> wfNodes) {
        WorkflowNode wfNode = wfNodes.stream()
            .filter(item -> item.getUuid().equals(nodeUuid))
            .findFirst().orElse(null);
        if (null == wfNode) {
            return "";
        }
        String wfNodeNodeConfig = wfNode.getNodeConfig();
        if (StrUtil.isBlank(wfNodeNodeConfig)) {
            return "";
        }
        Map<String, Object> map = JsonUtil.toMap(wfNodeNodeConfig);
        Object tip = map.getOrDefault("tip", "");
        return String.valueOf(tip);
    }

    public void streamingInvokeLLM(WfState wfState, WfNodeState state, WorkflowNode node, String modelName,
                                   List<SystemMessage> systemMessage) {
        log.info("stream invoke, modelName: {}", modelName);

        // 根据模型名称查询模型信息
        ChatModelVo chatModelVo = chatModelService.selectModelByName(modelName);
        if (chatModelVo == null) {
            throw new IllegalArgumentException("模型不存在: " + modelName);
        }

        // 根据模型名称找到模型实体
        String modelVoCategory = chatModelVo.getCategory();
        // 根据 category 获取对应的 ChatService（不使用计费代理，工作流场景单独计费）
        IChatService chatService = chatServiceFactory.getOriginalService(modelVoCategory);

        StreamingChatGenerator<AgentState> streamingGenerator = StreamingChatGenerator.builder()
            .mapResult(response -> {
                String responseTxt = response.aiMessage().text();
                log.info("llm response:{}", responseTxt);

                // 传递所有输入数据 + 添加 LLM 输出
                wfState.getNodeStateByNodeUuid(node.getUuid()).ifPresent(item -> {
                    List<NodeIOData> outputs = new ArrayList<>(item.getInputs());
                    NodeIOData output = NodeIOData.createByText(DEFAULT_OUTPUT_PARAM_NAME, "", responseTxt);
                    outputs.add(output);
                    item.setOutputs(outputs);
                });

                return Map.of("completeResult", response.aiMessage().text());
            })
            .startingNode(node.getUuid())
            .startingState(state)
            .build();

        // 获取用户信息和Token以及SSe连接对象（对话接口需要使用）
        Long userId = wfState.getUserId();
        String tokenValue = wfState.getTokenValue();
        SseEmitter sseEmitter = wfState.getSseEmitter();
        StreamingChatResponseHandler handler = streamingGenerator.handler();

        // 构建 ruoyi-ai 的 ChatRequest
        List<ChatMessage> chatMessages = new ArrayList<>();
        addUserMessage(node, state.getInputs(), chatMessages);
        chatMessages.addAll(systemMessage);

        // 定义模型调用对象
        ChatRequest chatRequest = new ChatRequest();
        // 目前工作流深度思考成员变量只能写死
        chatRequest.setEnableThinking(false);
        chatRequest.setModel(modelName);
        chatRequest.setChatMessages(chatMessages);

        //构建聊天对话上下文参数
        ChatContext chatContext = ChatContext.builder()
            .chatModelVo(chatModelVo)
            .chatRequest(chatRequest)
            .emitter(sseEmitter)
            .userId(userId)
            .tokenValue(tokenValue)
            .handler(handler)
            .build();

        // 使用工作流专用方法
        chatService.chat(chatContext);
        wfState.getNodeToStreamingGenerator().put(node.getUuid(), streamingGenerator);
    }

    /**
     * 添加用户信息
     *
     * @param node        节点
     * @param userMessage 用户信息
     */
    private void addUserMessage(WorkflowNode node, List<NodeIOData> userMessage, List<ChatMessage> messages) {
        if (CollUtil.isEmpty(userMessage)) {
            return;
        }
        WfNodeInputConfig nodeInputConfig = NodeInputConfigTypeHandler.fillNodeInputConfig(node.getInputConfig());
        List<WfNodeParamRef> refInputs = nodeInputConfig.getRefInputs();
        Set<String> nameSet = CollStreamUtil.toSet(refInputs, WfNodeParamRef::getName);
        // 构建消息列表
        List<UserMessage> messageList = buildMessageList(userMessage, nameSet);
        // 如果没有找到匹配的消息，尝试使用input字段
        if (CollUtil.isEmpty(messageList)) {
            messageList = buildMessageList(userMessage, Set.of("input"));
        }
        messages.addAll(messageList);
    }

    /**
     * 组装message对象
     *
     * @param role
     * @param value
     * @return
     */
    private UserMessage getMessage(String role, String value) {
        log.info("Creating message with role: {}, content: {}", role, value);
        return new UserMessage(value);
    }

    /**
     * 构建消息列表
     */
    private List<UserMessage> buildMessageList(List<NodeIOData> userMessage, Set<String> nameSet) {
        return userMessage.stream()
            .filter(item -> item != null && item.getName() != null)
            // 兼容默认输出参数的人机交互
            .filter(item -> nameSet.contains(item.getName()))
            .map(item -> getMessage("user", item.getContent().getValue().toString())).toList();
    }
}
