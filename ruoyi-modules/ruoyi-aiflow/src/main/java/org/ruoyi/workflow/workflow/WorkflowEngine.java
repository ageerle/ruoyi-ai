package org.ruoyi.workflow.workflow;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.bsc.async.AsyncGenerator;
import org.bsc.langgraph4j.*;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.bsc.langgraph4j.langchain4j.generators.StreamingChatGenerator;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.StateSnapshot;
import org.bsc.langgraph4j.streaming.StreamingOutput;
import org.ruoyi.common.core.exception.base.BaseException;
import org.ruoyi.workflow.base.NodeInputConfigTypeHandler;
import org.ruoyi.workflow.dto.workflow.WfRuntimeNodeDto;
import org.ruoyi.workflow.dto.workflow.WfRuntimeResp;
import org.ruoyi.workflow.entity.*;
import org.ruoyi.workflow.enums.ErrorEnum;
import org.ruoyi.workflow.helper.SSEEmitterHelper;
import org.ruoyi.workflow.service.WorkflowRuntimeNodeService;
import org.ruoyi.workflow.service.WorkflowRuntimeService;
import org.ruoyi.workflow.util.JsonUtil;
import org.ruoyi.workflow.workflow.data.NodeIOData;
import org.ruoyi.workflow.workflow.def.WfNodeIO;
import org.ruoyi.workflow.workflow.def.WfNodeParamRef;
import org.ruoyi.workflow.workflow.node.AbstractWfNode;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.function.Function;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.ruoyi.workflow.cosntant.AdiConstant.WorkflowConstant.*;
import static org.ruoyi.workflow.enums.ErrorEnum.*;

@Slf4j
public class WorkflowEngine {
    private final Workflow workflow;
    private final List<WorkflowComponent> components;
    private final List<WorkflowNode> wfNodes;
    private final List<WorkflowEdge> wfEdges;
    private final SSEEmitterHelper sseEmitterHelper;
    private final WorkflowRuntimeService workflowRuntimeService;
    private final WorkflowRuntimeNodeService workflowRuntimeNodeService;
    private CompiledGraph<WfNodeState> app;
    private SseEmitter sseEmitter;
    private User user;
    private WfState wfState;
    private WfRuntimeResp wfRuntimeResp;

    public WorkflowEngine(
            Workflow workflow,
            SSEEmitterHelper sseEmitterHelper,
            List<WorkflowComponent> components,
            List<WorkflowNode> nodes,
            List<WorkflowEdge> wfEdges,
            WorkflowRuntimeService workflowRuntimeService,
            WorkflowRuntimeNodeService workflowRuntimeNodeService) {
        this.workflow = workflow;
        this.sseEmitterHelper = sseEmitterHelper;
        this.components = components;
        this.wfNodes = nodes;
        this.wfEdges = wfEdges;
        this.workflowRuntimeService = workflowRuntimeService;
        this.workflowRuntimeNodeService = workflowRuntimeNodeService;
    }

    public void run(User user, List<ObjectNode> userInputs, SseEmitter sseEmitter) {
        this.user = user;
        this.sseEmitter = sseEmitter;
        log.info("WorkflowEngine run,userId:{},workflowUuid:{},userInputs:{}", user.getId(), workflow.getUuid(), userInputs);
        if (!this.workflow.getIsEnable()) {
            sseEmitterHelper.sendErrorAndComplete(user.getId(), sseEmitter, ErrorEnum.A_WF_DISABLED.getInfo());
            throw new BaseException(ErrorEnum.A_WF_DISABLED.getInfo());
        }

        Long workflowId = this.workflow.getId();
        this.wfRuntimeResp = workflowRuntimeService.create(user, workflowId);
        this.sseEmitterHelper.startSse(user, sseEmitter, JsonUtil.toJson(wfRuntimeResp));

        String runtimeUuid = this.wfRuntimeResp.getUuid();
        try {
            Pair<WorkflowNode, Set<WorkflowNode>> startAndEnds = findStartAndEndNode();
            WorkflowNode startNode = startAndEnds.getLeft();
            List<NodeIOData> wfInputs = getAndCheckUserInput(userInputs, startNode);
            this.wfState = new WfState(user, wfInputs, runtimeUuid);
            workflowRuntimeService.updateInput(this.wfRuntimeResp.getId(), wfState);


            WorkflowGraphBuilder graphBuilder = new WorkflowGraphBuilder(
                    components,
                    wfNodes,
                    wfEdges,
                    this::runNode,
                    this.wfState);
            StateGraph<WfNodeState> mainStateGraph = graphBuilder.build(startNode);

            MemorySaver saver = new MemorySaver();
            CompileConfig compileConfig = CompileConfig.builder().checkpointSaver(saver)
                    .interruptBefore(wfState.getInterruptNodes().toArray(String[]::new))
                    .build();
            app = mainStateGraph.compile(compileConfig);
            RunnableConfig invokeConfig = RunnableConfig.builder().build();
            exe(invokeConfig, false);
        } catch (Exception e) {
            errorWhenExe(e);
        }
    }

    private void exe(RunnableConfig invokeConfig, boolean resume) {
        //不使用langgraph4j state的update相关方法，无需传入input
        AsyncGenerator<NodeOutput<WfNodeState>> outputs = app.stream(resume ? null : Map.of(), invokeConfig);
        streamingResult(wfState, outputs, sseEmitter);

        StateSnapshot<WfNodeState> stateSnapshot = app.getState(invokeConfig);
        String nextNode = stateSnapshot.config().nextNode().orElse("");
        //还有下个节点，表示进入中断状态，等待用户输入后继续执�?
        if (StringUtils.isNotBlank(nextNode) && !nextNode.equalsIgnoreCase(END)) {
            String intTip = WorkflowUtil.getHumanFeedbackTip(nextNode, wfNodes);
            //将等待输入信息[事件与提示词]发送到到客户端
            SSEEmitterHelper.parseAndSendPartialMsg(sseEmitter, "[NODE_WAIT_FEEDBACK_BY_" + nextNode + "]", intTip);
            InterruptedFlow.RUNTIME_TO_GRAPH.put(wfState.getUuid(), this);
            //更新状�?
            wfState.setProcessStatus(WORKFLOW_PROCESS_STATUS_WAITING_INPUT);
            workflowRuntimeService.updateOutput(wfRuntimeResp.getId(), wfState);
        } else {
            WorkflowRuntime updatedRuntime = workflowRuntimeService.updateOutput(wfRuntimeResp.getId(), wfState);
            sseEmitterHelper.sendComplete(user.getId(), sseEmitter, updatedRuntime.getOutput());
            InterruptedFlow.RUNTIME_TO_GRAPH.remove(wfState.getUuid());
        }
    }

    /**
     * 中断流程等待用户输入时，会进行暂停状态，用户输入后调用本方法执行流程剩余部分
     *
     * @param userInput 用户输入
     */
    public void resume(String userInput) {
        RunnableConfig invokeConfig = RunnableConfig.builder().build();
        try {
            app.updateState(invokeConfig, Map.of(HUMAN_FEEDBACK_KEY, userInput), null);
            exe(invokeConfig, true);
        } catch (Exception e) {
            errorWhenExe(e);
        } finally {
            //有可能多次接收人机交互，待整个流程完全执行后才能删除
            if (wfState.getProcessStatus() != WORKFLOW_PROCESS_STATUS_WAITING_INPUT) {
                InterruptedFlow.RUNTIME_TO_GRAPH.remove(wfState.getUuid());
            }
        }
    }

    private void errorWhenExe(Exception e) {
        log.error("error", e);
        String errorMsg = e.getMessage();
        if (errorMsg.contains("parallel node doesn't support conditional branch")) {
            errorMsg = "并行节点中不能包含条件分�?";
        }
        sseEmitterHelper.sendErrorAndComplete(user.getId(), sseEmitter, errorMsg);
        workflowRuntimeService.updateStatus(wfRuntimeResp.getId(), WORKFLOW_PROCESS_STATUS_FAIL, errorMsg);
    }

    private Map<String, Object> runNode(WorkflowNode wfNode, WfNodeState nodeState) {
        Map<String, Object> resultMap = new HashMap<>();
        try {
            WorkflowComponent wfComponent = components.stream().filter(item -> item.getId().equals(wfNode.getWorkflowComponentId())).findFirst().orElseThrow();
            AbstractWfNode abstractWfNode = WfNodeFactory.create(wfComponent, wfNode, wfState, nodeState);
            //节点实例
            WfRuntimeNodeDto runtimeNodeDto = workflowRuntimeNodeService.createByState(user, wfNode.getId(), wfRuntimeResp.getId(), nodeState);
            wfState.getRuntimeNodes().add(runtimeNodeDto);

            SSEEmitterHelper.parseAndSendPartialMsg(sseEmitter, "[NODE_RUN_" + wfNode.getUuid() + "]", JsonUtil.toJson(runtimeNodeDto));

            NodeProcessResult processResult = abstractWfNode.process((is) -> {
                workflowRuntimeNodeService.updateInput(runtimeNodeDto.getId(), nodeState);
                List<NodeIOData> nodeIODataList = nodeState.getInputs();
//                if (!wfNode.getWorkflowComponentId().equals(1L)) {
//                    String inputConfig = wfNode.getInputConfig();
//                    WfNodeInputConfig nodeInputConfig = NodeInputConfigTypeHandler.fillNodeInputConfig(inputConfig);
//                    List<WfNodeParamRef> refInputs = nodeInputConfig.getRefInputs();
//                    Set<String> nameSet = CollStreamUtil.toSet(refInputs, WfNodeParamRef::getNodeParamName);
//                    if (CollUtil.isNotEmpty(nameSet)) {
//                        nodeIODataList = nodeIODataList.stream().filter(item -> nameSet.contains(item.getName()))
//                                .collect(Collectors.toList());
//                    } else {
//                        nodeIODataList = nodeIODataList.stream().filter(item -> item.getName().contains("input"))
//                                .collect(Collectors.toList());
//                    }
//                }
                for (NodeIOData input : nodeIODataList) {
                    String inputConfig = wfNode.getInputConfig();
                    WfNodeInputConfig nodeInputConfig = NodeInputConfigTypeHandler.fillNodeInputConfig(inputConfig);
                    List<WfNodeParamRef> refInputs = nodeInputConfig.getRefInputs();
                    if (CollUtil.isNotEmpty(refInputs) && "input".equals(input.getName())) {
                        continue;
                    }
                    SSEEmitterHelper.parseAndSendPartialMsg(sseEmitter, "[NODE_INPUT_" + wfNode.getUuid() + "]", JsonUtil.toJson(input));
                }
            }, (is) -> {
                workflowRuntimeNodeService.updateOutput(runtimeNodeDto.getId(), nodeState);
                //并行节点内部的节点执行结束后，需要主动向客户端发送输出结�?
                String nodeUuid = wfNode.getUuid();
                List<NodeIOData> nodeOutputs = nodeState.getOutputs();
                for (NodeIOData output : nodeOutputs) {
                    log.info("callback node:{},output:{}", nodeUuid, output.getContent());
                    SSEEmitterHelper.parseAndSendPartialMsg(sseEmitter, "[NODE_OUTPUT_" + nodeUuid + "]", JsonUtil.toJson(output));
                }
            });
            if (StringUtils.isNotBlank(processResult.getNextNodeUuid())) {
                resultMap.put("next", processResult.getNextNodeUuid());
            }
        } catch (Exception e) {
            log.error("Node run error", e);
            throw new BaseException(ErrorEnum.B_WF_RUN_ERROR.getInfo());
        }
        resultMap.put("name", wfNode.getTitle());
        //langgraph4j state中的data不做数据存储，只存储元数�?
        StreamingChatGenerator<AgentState> generator = wfState.getNodeToStreamingGenerator().get(wfNode.getUuid());
        if (null != generator) {
            resultMap.put("_streaming_messages", generator);
            return resultMap;
        }
        return resultMap;
    }

    /**
     * 流式输出结果
     *
     * @param outputs    输出
     * @param sseEmitter sse emitter
     */
    private void streamingResult(WfState wfState, AsyncGenerator<NodeOutput<WfNodeState>> outputs, SseEmitter sseEmitter) {
        for (NodeOutput<WfNodeState> out : outputs) {
            if (out instanceof StreamingOutput<WfNodeState> streamingOutput) {
                String node = streamingOutput.node();
                String chunk = streamingOutput.chunk();
                log.info("node:{},chunk:{}", node, chunk);
                Map<String, String> strMap = new HashMap<>();
                strMap.put("ck", chunk);
//                SSEEmitterHelper.parseAndSendPartialMsg(sseEmitter, "[NODE_CHUNK_" + node + "]", strMap.toString());
                SSEEmitterHelper.parseAndSendPartialMsg(sseEmitter, "[NODE_CHUNK_" + node + "]", chunk);
            } else {
                AbstractWfNode abstractWfNode = wfState.getCompletedNodes().stream()
                        .filter(item -> item.getNode().getUuid().endsWith(out.node())).findFirst().orElse(null);
                if (null != abstractWfNode) {
                    WfRuntimeNodeDto runtimeNodeDto = wfState.getRuntimeNodeByNodeUuid(out.node());
                    if (null != runtimeNodeDto) {
                        workflowRuntimeNodeService.updateOutput(runtimeNodeDto.getId(), abstractWfNode.getState());
                        wfState.setOutput(abstractWfNode.getState().getOutputs());
                    } else {
                        log.warn("Can not find runtime node, node uuid:{}", out.node());
                    }
                } else {
                    log.warn("Can not find node state,node uuid:{}", out.node());
                }
            }
        }
    }

    /**
     * 校验用户输入并组装成工作流的输入
     *
     * @param userInputs 用户输入
     * @param startNode  开始节点定�?
     * @return 正确的用户输入列�?
     */
    private List<NodeIOData> getAndCheckUserInput(List<ObjectNode> userInputs, WorkflowNode startNode) {
        WfNodeInputConfig wfNodeInputConfig = NodeInputConfigTypeHandler.fillNodeInputConfig(startNode.getInputConfig());
        List<WfNodeIO> defList = wfNodeInputConfig.getUserInputs();
        defList = CollStreamUtil.toList(defList, Function.identity());
        List<NodeIOData> wfInputs = new ArrayList<>();
        for (WfNodeIO paramDefinition : defList) {
            String paramNameFromDef = paramDefinition.getName();
            boolean requiredParamMissing = paramDefinition.getRequired();
            for (ObjectNode userInput : userInputs) {
                NodeIOData nodeIOData = WfNodeIODataUtil.createNodeIOData(userInput);
                if (!paramNameFromDef.equalsIgnoreCase(nodeIOData.getName())) {
                    continue;
                }
                Integer dataType = nodeIOData.getContent().getType();
                if (null == dataType) {
                    throw new BaseException(A_WF_INPUT_INVALID.getInfo());
                }
                requiredParamMissing = false;
                boolean valid = paramDefinition.checkValue(nodeIOData);
                if (!valid) {
                    log.error("用户输入无效,workflowId:{}", startNode.getWorkflowId());
                    throw new BaseException(ErrorEnum.A_WF_INPUT_INVALID.getInfo());
                }
                wfInputs.add(nodeIOData);
            }
            if (requiredParamMissing) {
                log.error("在流程定义中必填的参数没有传进来,name:{}", paramNameFromDef);
                throw new BaseException(A_WF_INPUT_MISSING.getInfo());
            }
        }
        return wfInputs;
    }

    /**
     * 查找开始及结束节点 <br/>
     * 开始节点只能有一个，结束节点可能多个
     *
     * @return 开始节点及结束节点列表
     */
    public Pair<WorkflowNode, Set<WorkflowNode>> findStartAndEndNode() {
        WorkflowNode startNode = null;
        Set<WorkflowNode> endNodes = new HashSet<>();
        for (WorkflowNode node : wfNodes) {
            Optional<WorkflowComponent> wfComponent = components.stream().filter(item -> item.getId().equals(node.getWorkflowComponentId())).findFirst();
            if (wfComponent.isPresent() && WfComponentNameEnum.START.getName().equals(wfComponent.get().getName())) {
                if (null != startNode) {
                    throw new BaseException(ErrorEnum.A_WF_MULTIPLE_START_NODE.getInfo());
                }
                startNode = node;
            } else if (wfComponent.isPresent() && WfComponentNameEnum.END.getName().equals(wfComponent.get().getName())) {
                endNodes.add(node);
            }
        }
        if (null == startNode) {
            log.error("没有开始节点, workflowId:{}", wfNodes.get(0).getWorkflowId());
            throw new BaseException(ErrorEnum.A_WF_START_NODE_NOT_FOUND.getInfo());
        }
        //Find all end nodes
        wfNodes.forEach(item -> {
            String nodeUuid = item.getUuid();
            boolean source = false;
            boolean target = false;
            for (WorkflowEdge edgeDef : wfEdges) {
                if (edgeDef.getSourceNodeUuid().equals(nodeUuid)) {
                    source = true;
                } else if (edgeDef.getTargetNodeUuid().equals(nodeUuid)) {
                    target = true;
                }
            }
            if (!source && target) {
                endNodes.add(item);
            }
        });
        log.info("start node:{}", startNode);
        log.info("end nodes:{}", endNodes);
        if (endNodes.isEmpty()) {
            log.error("没有结束节点,workflowId:{}", startNode.getWorkflowId());
            throw new BaseException(A_WF_END_NODE_NOT_FOUND.getInfo());
        }
        return Pair.of(startNode, endNodes);
    }


    public CompiledGraph<WfNodeState> getApp() {
        return app;
    }
}
