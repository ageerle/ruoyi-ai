package org.ruoyi.workflow.workflow;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.serializer.std.ObjectStreamStateSerializer;
import org.ruoyi.common.core.exception.base.BaseException;
import org.ruoyi.workflow.entity.WorkflowComponent;
import org.ruoyi.workflow.entity.WorkflowEdge;
import org.ruoyi.workflow.entity.WorkflowNode;
import org.ruoyi.workflow.enums.ErrorEnum;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;
import static org.ruoyi.workflow.workflow.WfComponentNameEnum.HUMAN_FEEDBACK;

/**
 * 负责构建工作流运行所依赖的状态图�?
 */
@Slf4j
public class WorkflowGraphBuilder {

    private final Map<Long, WorkflowComponent> componentIndex;
    private final Map<String, WorkflowNode> nodeIndex;
    private final Map<String, List<WorkflowEdge>> edgesBySource;
    private final Map<String, List<WorkflowEdge>> edgesByTarget;
    private final WorkflowNodeRunner nodeRunner;
    private final WfState wfState;

    private final ObjectStreamStateSerializer<WfNodeState> stateSerializer = new ObjectStreamStateSerializer<>(WfNodeState::new);
    private final Map<String, List<StateGraph<WfNodeState>>> stateGraphNodes = new HashMap<>();
    private final Map<String, List<StateGraph<WfNodeState>>> stateGraphEdges = new HashMap<>();
    private final Map<String, String> rootToSubGraph = new HashMap<>();
    private final Map<String, GraphCompileNode> nodeToParallelBranch = new HashMap<>();

    public WorkflowGraphBuilder(
            List<WorkflowComponent> components,
            List<WorkflowNode> nodes,
            List<WorkflowEdge> edges,
            WorkflowNodeRunner nodeRunner,
            WfState wfState) {
        this.componentIndex = components.stream()
                .collect(Collectors.toMap(WorkflowComponent::getId, Function.identity(), (origin, ignore) -> origin));
        this.nodeIndex = nodes.stream()
                .collect(Collectors.toMap(WorkflowNode::getUuid, Function.identity(), (origin, ignore) -> origin));
        this.edgesBySource = edges.stream().collect(Collectors.groupingBy(WorkflowEdge::getSourceNodeUuid));
        this.edgesByTarget = edges.stream().collect(Collectors.groupingBy(WorkflowEdge::getTargetNodeUuid));
        this.nodeRunner = nodeRunner;
        this.wfState = wfState;
    }

    public StateGraph<WfNodeState> build(WorkflowNode startNode) throws GraphStateException {
        CompileNode rootCompileNode = new CompileNode();
        rootCompileNode.setId(startNode.getUuid());
        buildCompileNode(rootCompileNode, startNode);

        StateGraph<WfNodeState> mainStateGraph = new StateGraph<>(stateSerializer);
        wfState.addEdge(START, startNode.getUuid());
        buildStateGraph(null, mainStateGraph, rootCompileNode);
        return mainStateGraph;
    }

    private void buildCompileNode(CompileNode parentNode, WorkflowNode node) {
        log.info("buildCompileNode, parentNode:{}, node:{}, title:{}", parentNode.getId(), node.getUuid(), node.getTitle());
        CompileNode newNode;
        List<String> upstreamNodeUuids = getUpstreamNodeUuids(node.getUuid());
        if (upstreamNodeUuids.isEmpty()) {
            log.error("节点{}没有上游节点", node.getUuid());
            newNode = parentNode;
        } else if (upstreamNodeUuids.size() == 1) {
            String upstreamUuid = upstreamNodeUuids.get(0);
            boolean pointToParallel = pointToParallelBranch(upstreamUuid);
            if (pointToParallel) {
                String rootId = node.getUuid();
                GraphCompileNode graphCompileNode = getOrCreateGraphCompileNode(rootId);
                appendToNextNodes(parentNode, graphCompileNode);
                newNode = graphCompileNode;
            } else if (parentNode instanceof GraphCompileNode graphCompileNode) {
                newNode = CompileNode.builder().id(node.getUuid()).conditional(false).nextNodes(new ArrayList<>()).build();
                graphCompileNode.appendToLeaf(newNode);
            } else {
                newNode = CompileNode.builder().id(node.getUuid()).conditional(false).nextNodes(new ArrayList<>()).build();
                appendToNextNodes(parentNode, newNode);
            }
        } else {
            newNode = CompileNode.builder().id(node.getUuid()).conditional(false).nextNodes(new ArrayList<>()).build();
            GraphCompileNode parallelBranch = nodeToParallelBranch.get(parentNode.getId());
            appendToNextNodes(Objects.requireNonNullElse(parallelBranch, parentNode), newNode);
        }

        if (newNode == null) {
            log.error("节点:{}不存�?", node.getUuid());
            return;
        }
        for (String downstream : getDownstreamNodeUuids(node.getUuid())) {
            WorkflowNode downstreamNode = nodeIndex.get(downstream);
            if (downstreamNode != null) {
                buildCompileNode(newNode, downstreamNode);
            }
        }
    }

    private void buildStateGraph(CompileNode upstreamCompileNode,
                                 StateGraph<WfNodeState> stateGraph,
                                 CompileNode compileNode) throws GraphStateException {
        log.info("buildStateGraph, upstream:{}, node:{}", upstreamCompileNode, compileNode.getId());
        String stateGraphNodeUuid = compileNode.getId();
        if (upstreamCompileNode == null) {
            addNodeToStateGraph(stateGraph, stateGraphNodeUuid);
            addEdgeToStateGraph(stateGraph, START, compileNode.getId());
        } else {
            if (compileNode instanceof GraphCompileNode graphCompileNode) {
                String stateGraphId = graphCompileNode.getId();
                CompileNode root = graphCompileNode.getRoot();
                String rootId = root.getId();
                String existSubGraphId = rootToSubGraph.get(rootId);

                if (StringUtils.isBlank(existSubGraphId)) {
                    StateGraph<WfNodeState> subgraph = new StateGraph<>(stateSerializer);
                    addNodeToStateGraph(subgraph, rootId);
                    addEdgeToStateGraph(subgraph, START, rootId);
                    for (CompileNode child : root.getNextNodes()) {
                        buildStateGraph(root, subgraph, child);
                    }
                    addEdgeToStateGraph(subgraph, graphCompileNode.getTail().getId(), END);
                    stateGraph.addNode(stateGraphId, subgraph.compile());
                    rootToSubGraph.put(rootId, stateGraphId);
                    stateGraphNodeUuid = stateGraphId;
                } else {
                    stateGraphNodeUuid = existSubGraphId;
                }
            } else {
                addNodeToStateGraph(stateGraph, stateGraphNodeUuid);
            }

            if (Boolean.FALSE.equals(upstreamCompileNode.getConditional())) {
                addEdgeToStateGraph(stateGraph, upstreamCompileNode.getId(), stateGraphNodeUuid);
            }
        }

        List<CompileNode> nextNodes = compileNode.getNextNodes();
        if (nextNodes.size() > 1) {
            boolean conditional = nextNodes.stream().noneMatch(item -> item instanceof GraphCompileNode);
            compileNode.setConditional(conditional);
            for (CompileNode nextNode : nextNodes) {
                buildStateGraph(compileNode, stateGraph, nextNode);
            }
            if (conditional) {
                List<String> targets = nextNodes.stream().map(CompileNode::getId).toList();
                Map<String, String> mappings = new HashMap<>();
                for (String target : targets) {
                    mappings.put(target, target);
                }
                stateGraph.addConditionalEdges(
                        stateGraphNodeUuid,
                        edge_async(state -> state.data().get("next").toString()),
                        mappings
                );
            }
        } else if (nextNodes.size() == 1) {
            for (CompileNode nextNode : nextNodes) {
                buildStateGraph(compileNode, stateGraph, nextNode);
            }
        } else {
            addEdgeToStateGraph(stateGraph, stateGraphNodeUuid, END);
        }
    }

    private GraphCompileNode getOrCreateGraphCompileNode(String rootId) {
        GraphCompileNode exist = nodeToParallelBranch.get(rootId);
        if (exist == null) {
            GraphCompileNode graphCompileNode = new GraphCompileNode();
            graphCompileNode.setId("parallel_" + rootId);
            graphCompileNode.setRoot(CompileNode.builder().id(rootId).conditional(false).nextNodes(new ArrayList<>()).build());
            nodeToParallelBranch.put(rootId, graphCompileNode);
            exist = graphCompileNode;
        }
        return exist;
    }

    private List<String> getUpstreamNodeUuids(String nodeUuid) {
        return edgesByTarget.getOrDefault(nodeUuid, List.of())
                .stream()
                .map(WorkflowEdge::getSourceNodeUuid)
                .toList();
    }

    private List<String> getDownstreamNodeUuids(String nodeUuid) {
        return edgesBySource.getOrDefault(nodeUuid, List.of())
                .stream()
                .map(WorkflowEdge::getTargetNodeUuid)
                .toList();
    }

    private boolean pointToParallelBranch(String nodeUuid) {
        return edgesBySource.getOrDefault(nodeUuid, List.of())
                .stream()
                .filter(edge -> StringUtils.isBlank(edge.getSourceHandle()))
                .count() > 1;
    }

    private void addNodeToStateGraph(StateGraph<WfNodeState> stateGraph, String stateGraphNodeUuid) throws GraphStateException {
        List<StateGraph<WfNodeState>> stateGraphList = stateGraphNodes.computeIfAbsent(stateGraphNodeUuid, k -> new ArrayList<>());
        boolean exist = stateGraphList.stream().anyMatch(item -> item == stateGraph);
        if (exist) {
            log.info("state graph node exist,stateGraphNodeUuid:{}", stateGraphNodeUuid);
            return;
        }
        log.info("addNodeToStateGraph,node uuid:{}", stateGraphNodeUuid);
        WorkflowNode wfNode = getNodeByUuid(stateGraphNodeUuid);
        stateGraph.addNode(stateGraphNodeUuid, node_async(state -> nodeRunner.run(wfNode, state)));
        stateGraphList.add(stateGraph);

        WorkflowComponent component = componentIndex.get(wfNode.getWorkflowComponentId());
        if (component == null) {
            throw new BaseException(ErrorEnum.A_PARAMS_ERROR.getInfo());
        }
        if (HUMAN_FEEDBACK.getName().equals(component.getName())) {
            wfState.addInterruptNode(stateGraphNodeUuid);
        }
    }

    private void addEdgeToStateGraph(StateGraph<WfNodeState> stateGraph, String source, String target) throws GraphStateException {
        String key = source + "_" + target;
        List<StateGraph<WfNodeState>> stateGraphList = stateGraphEdges.computeIfAbsent(key, k -> new ArrayList<>());
        boolean exist = stateGraphList.stream().anyMatch(item -> item == stateGraph);
        if (exist) {
            log.info("state graph edge exist,source:{},target:{}", source, target);
            return;
        }
        log.info("addEdgeToStateGraph,source:{},target:{}", source, target);
        stateGraph.addEdge(source, target);
        stateGraphList.add(stateGraph);
    }

    private WorkflowNode getNodeByUuid(String nodeUuid) {
        WorkflowNode workflowNode = nodeIndex.get(nodeUuid);
        if (workflowNode == null) {
            throw new BaseException(ErrorEnum.A_WF_NODE_NOT_FOUND.getInfo());
        }
        return workflowNode;
    }

    private void appendToNextNodes(CompileNode compileNode, CompileNode newNode) {
        boolean exist = compileNode.getNextNodes().stream().anyMatch(item -> item.getId().equals(newNode.getId()));
        if (!exist) {
            compileNode.getNextNodes().add(newNode);
        }
    }
}
