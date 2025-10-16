package org.ruoyi.workflow.workflow;

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
import org.ruoyi.workflow.entity.WorkflowNode;
import org.ruoyi.workflow.enums.WfIODataTypeEnum;
import org.ruoyi.workflow.util.JsonUtil;
import org.ruoyi.workflow.workflow.data.NodeIOData;
import org.ruoyi.workflow.workflow.data.NodeIODataContent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.ruoyi.workflow.cosntant.AdiConstant.WorkflowConstant.DEFAULT_OUTPUT_PARAM_NAME;

@Slf4j
@Component
public class WorkflowUtil {

    @Resource
    private ChatServiceFactory chatServiceFactory;

    @SuppressWarnings("unchecked")
    public static String renderTemplate(String template, List<NodeIOData> values) {
        String result = template;
        for (NodeIOData next : values) {
            String name = next.getName();
            NodeIODataContent<?> dataContent = next.getContent();
            if (dataContent.getType().equals(WfIODataTypeEnum.FILES.getValue())) {
                List<String> value = (List<String>) dataContent.getValue();
                result = result.replace("{" + name + "}", String.join(",", value));
            } else if (dataContent.getType().equals(WfIODataTypeEnum.OPTIONS.getValue())) {
                Map<String, Object> value = (Map<String, Object>) dataContent.getValue();
                result = result.replace("{" + name + "}", value.toString());
            } else {
                result = result.replace("{" + name + "}", dataContent.getValue().toString());
            }
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

    public void streamingInvokeLLM(WfState wfState, WfNodeState state, WorkflowNode node, String modelPlatform,
                                   String modelName, List<UserMessage> msgs) {
        log.info("stream invoke, modelPlatform: {}, modelName: {}", modelPlatform, modelName);

        // 根据 modelPlatform 获取对应的 ChatService（不使用计费代理，工作流场景单独计费）
        IChatService chatService = chatServiceFactory.getOriginalService(modelPlatform);

        StreamingChatGenerator<AgentState> streamingGenerator = StreamingChatGenerator.builder()
                .mapResult(response -> {
                    String responseTxt = response.aiMessage().text();
                    log.info("llm response:{}", responseTxt);
                    NodeIOData output = NodeIOData.createByText(DEFAULT_OUTPUT_PARAM_NAME, "", responseTxt);
                    wfState.getNodeStateByNodeUuid(node.getUuid()).ifPresent(item -> item.getOutputs().add(output));
                    return Map.of("completeResult", response.aiMessage().text());
                })
                .startingNode(node.getUuid())
                .startingState(state)
                .build();

        // 构建 ruoyi-ai 的 ChatRequest
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setModel(modelName);

        List<Message> messages = new ArrayList<>();
        for (UserMessage userMsg : msgs) {
            Message message = new Message();
            message.setContent(userMsg.singleText());
            message.setRole("user");
            messages.add(message);
        }
        chatRequest.setMessages(messages);
        // 使用工作流专用方法
        chatService.chat(chatRequest, streamingGenerator.handler());
        wfState.getNodeToStreamingGenerator().put(node.getUuid(), streamingGenerator);
    }
}
