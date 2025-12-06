package org.ruoyi.workflow.workflow;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import dev.langchain4j.data.message.UserMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.langchain4j.generators.StreamingChatGenerator;
import org.bsc.langgraph4j.state.AgentState;
import org.ruoyi.chat.factory.ChatServiceFactory;
import org.ruoyi.chat.service.chat.IChatService;
import org.ruoyi.common.chat.entity.chat.Message;
import org.ruoyi.common.chat.request.ChatRequest;
import org.ruoyi.workflow.base.NodeInputConfigTypeHandler;
import org.ruoyi.workflow.entity.WorkflowNode;
import org.ruoyi.workflow.enums.WfIODataTypeEnum;
import org.ruoyi.workflow.util.JsonUtil;
import org.ruoyi.workflow.workflow.data.NodeIOData;
import org.ruoyi.workflow.workflow.data.NodeIODataContent;
import org.ruoyi.workflow.workflow.def.WfNodeParamRef;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.ruoyi.workflow.cosntant.AdiConstant.WorkflowConstant.DEFAULT_OUTPUT_PARAM_NAME;

@Slf4j
@Component
public class WorkflowUtil {

    @Resource
    private ChatServiceFactory chatServiceFactory;

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

    public void streamingInvokeLLM(WfState wfState, WfNodeState state, WorkflowNode node, String category,
                                   String modelName, List<UserMessage> systemMessage) {
        log.info("stream invoke, category: {}, modelName: {}", category, modelName);

        // 根据 category 获取对应的 ChatService（不使用计费代理，工作流场景单独计费）
        IChatService chatService = chatServiceFactory.getOriginalService(category);

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

        // 构建 ruoyi-ai 的 ChatRequest
        List<Message> messages = new ArrayList<>();

        addUserMessage(node, state.getInputs(), messages);

        addSystemMessage(systemMessage, messages);

        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setModel(modelName);
        chatRequest.setMessages(messages);

        // 使用工作流专用方法
        chatService.chat(chatRequest, streamingGenerator.handler());
        wfState.getNodeToStreamingGenerator().put(node.getUuid(), streamingGenerator);
    }

    /**
     * 添加用户信息
     *
     * @param node
     * @param messages
     */
    private void addUserMessage(WorkflowNode node, List<NodeIOData> userMessage, List<Message> messages) {
        if (CollUtil.isEmpty(userMessage)) {
            return;
        }

        WfNodeInputConfig nodeInputConfig = NodeInputConfigTypeHandler.fillNodeInputConfig(node.getInputConfig());

        List<WfNodeParamRef> refInputs = nodeInputConfig.getRefInputs();

        Set<String> nameSet = CollStreamUtil.toSet(refInputs, WfNodeParamRef::getName);

        userMessage.stream().filter(item -> nameSet.contains(item.getName()))
                .map(item -> getMessage("user", item.getContent().getValue())).forEach(messages::add);

        if (CollUtil.isNotEmpty(messages)) {
            return;
        }

        userMessage.stream().filter(item -> "input".equals(item.getName()))
                .map(item -> getMessage("user", item.getContent().getValue())).forEach(messages::add);
    }

    /**
     * 组装message对象
     *
     * @param role
     * @param value
     * @return
     */
    private Message getMessage(String role, Object value) {
        log.info("Creating message with role: {}, content: {}", role, value); // 🔥
        Message message = new Message();
        message.setRole(role);
        message.setContent(value);
        return message;
    }

    /**
     * 添加系统信息
     *
     * @param systemMessage
     * @param messages
     */
    private void addSystemMessage(List<UserMessage> systemMessage, List<Message> messages) {
        log.info("addSystemMessage received: {}", systemMessage); // 🔥 加这一行

        if (CollUtil.isEmpty(systemMessage)) {
            return;
        }
        systemMessage.stream()
                .map(userMsg -> getMessage("system", userMsg.singleText()))
                .forEach(messages::add);
    }
}
