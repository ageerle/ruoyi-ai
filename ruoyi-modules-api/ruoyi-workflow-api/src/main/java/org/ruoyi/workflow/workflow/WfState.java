package org.ruoyi.workflow.workflow;

import lombok.Getter;
import lombok.Setter;
import org.bsc.langgraph4j.langchain4j.generators.StreamingChatGenerator;
import org.bsc.langgraph4j.state.AgentState;
import org.ruoyi.workflow.dto.workflow.WfRuntimeNodeDto;
import org.ruoyi.workflow.entity.User;
import org.ruoyi.workflow.entity.WorkflowNode;
import org.ruoyi.workflow.workflow.data.NodeIOData;
import org.ruoyi.workflow.workflow.node.AbstractWfNode;

import java.util.*;

import static org.ruoyi.workflow.cosntant.AdiConstant.WorkflowConstant.WORKFLOW_PROCESS_STATUS_READY;

/**
 * 工作流实例状态 | workflow instance state
 */
@Setter
@Getter
public class WfState {

    private String uuid;
    private User user;
    private String processingNodeUuid;

    //Source node uuid => target node uuid list
    private Map<String, List<String>> edges = new HashMap<>();
    private Map<String, List<String>> conditionalEdges = new HashMap<>();

    //Source node uuid => streaming chat generator
    private Map<String, StreamingChatGenerator<AgentState>> nodeToStreamingGenerator = new HashMap<>();

    /**
     * 已运行节点列表
     */
    private List<AbstractWfNode> completedNodes = new LinkedList<>();

    private List<WfRuntimeNodeDto> runtimeNodes = new ArrayList<>();

    /**
     * 工作流接收到的输入（也是开始节点的输入参数）
     */
    private List<NodeIOData> input;

    /**
     * 工作流执行结束后的输出
     */
    private List<NodeIOData> output = new ArrayList<>();
    private Integer processStatus = WORKFLOW_PROCESS_STATUS_READY;

    /**
     * 人机交互节点
     */
    private Set<String> interruptNodes = new HashSet<>();

    public WfState(User user, List<NodeIOData> input, String uuid) {
        this.input = input;
        this.user = user;
        this.uuid = uuid;
    }

    /**
     * 获取最新的输出结果
     *
     * @return 参数列表
     */
    public List<NodeIOData> getLatestOutputs() {
        WfNodeState upstreamState = completedNodes.get(completedNodes.size() - 1).getState();
        return upstreamState.getOutputs();
    }

    public Optional<WfNodeState> getNodeStateByNodeUuid(String nodeUuid) {
        return this.completedNodes.stream().filter(item -> item.getNode().getUuid().equals(nodeUuid)).map(AbstractWfNode::getState).findFirst();
    }

    /**
     * 新增一条边
     * 并行执行分支的情况下会出现一个 source node 对应多个 target node
     *
     * @param sourceNodeUuid 开始节点
     * @param targetNodeUuid 目标节点
     */
    public void addEdge(String sourceNodeUuid, String targetNodeUuid) {
        List<String> targetNodeUuids = edges.computeIfAbsent(sourceNodeUuid, k -> new ArrayList<>());
        targetNodeUuids.add(targetNodeUuid);
    }

    /**
     * 新增一条边
     * 按条件执行的分支会出现一个 source node 对应多个 target node 的情况
     *
     * @param sourceNodeUuid 开始节点
     * @param targetNodeUuid 目标节点
     */
    public void addConditionalEdge(String sourceNodeUuid, String targetNodeUuid) {
        List<String> targetNodeUuids = conditionalEdges.computeIfAbsent(sourceNodeUuid, k -> new ArrayList<>());
        targetNodeUuids.add(targetNodeUuid);
    }

    public List<NodeIOData> getIOByNodeUuid(String nodeUuid) {
        List<NodeIOData> result = new ArrayList<>();
        Optional<AbstractWfNode> optional = completedNodes.stream().filter(node -> nodeUuid.equals(node.getNode().getUuid())).findFirst();
        if (optional.isEmpty()) {
            return result;
        }
        result.addAll(optional.get().getState().getInputs());
        result.addAll(optional.get().getState().getOutputs());
        return result;
    }

    public WfRuntimeNodeDto getRuntimeNodeByNodeUuid(String wfNodeUuid) {
        WorkflowNode wfNode = getCompletedNodes().stream()
                .map(AbstractWfNode::getNode)
                .filter(node -> node.getUuid().equals(wfNodeUuid))
                .findFirst()
                .orElse(null);
        if (null == wfNode) {
            return null;
        }
        return getRuntimeNodes().stream()
                .filter(item -> item.getNodeId().equals(wfNode.getId()))
                .findFirst()
                .orElse(null);
    }

    public void addInterruptNode(String nodeUuid) {
        this.interruptNodes.add(nodeUuid);
    }
}
