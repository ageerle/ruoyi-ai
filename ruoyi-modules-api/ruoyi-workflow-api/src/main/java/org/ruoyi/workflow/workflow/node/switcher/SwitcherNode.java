package org.ruoyi.workflow.workflow.node.switcher;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.workflow.entity.WorkflowComponent;
import org.ruoyi.workflow.entity.WorkflowNode;
import org.ruoyi.workflow.workflow.NodeProcessResult;
import org.ruoyi.workflow.workflow.WfNodeState;
import org.ruoyi.workflow.workflow.WfState;
import org.ruoyi.workflow.workflow.data.NodeIOData;
import org.ruoyi.workflow.workflow.node.AbstractWfNode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 条件分支节点
 * 根据配置的条件规则，选择不同的分支路径执行
 */
@Slf4j
public class SwitcherNode extends AbstractWfNode {

    public SwitcherNode(WorkflowComponent wfComponent, WorkflowNode nodeDef, WfState wfState, WfNodeState nodeState) {
        super(wfComponent, nodeDef, wfState, nodeState);
    }

    @Override
    public NodeProcessResult onProcess() {
        try {
            SwitcherNodeConfig config = checkAndGetConfig(SwitcherNodeConfig.class);
            List<NodeIOData> inputs = state.getInputs();

            log.info("条件分支节点处理中，分支数量: {}",
                    config.getCases() != null ? config.getCases().size() : 0);

            // 按顺序评估每个分支
            if (config.getCases() != null) {
                for (int i = 0; i < config.getCases().size(); i++) {
                    SwitcherCase switcherCase = config.getCases().get(i);
                    log.info("评估分支 {}: uuid={}, 运算符={}",
                            i + 1, switcherCase.getUuid(), switcherCase.getOperator());

                    if (evaluateCase(switcherCase, inputs)) {
                        // 检查目标节点UUID是否为空
                        if (StringUtils.isBlank(switcherCase.getTargetNodeUuid())) {
                            log.warn("分支 {} 匹配但目标节点UUID为空，跳过到下一个分支", i + 1);
                            continue;
                        }

                        log.info("分支 {} 匹配，跳转到节点: {}",
                                i + 1, switcherCase.getTargetNodeUuid());

                        // 构造输出：只保留 output 和其他非 input 参数 + 添加分支匹配信息
                        List<NodeIOData> outputs = new java.util.ArrayList<>();

                        // 过滤输入：排除 input 参数（与 output 冗余），保留其他参数
                        inputs.stream()
                                .filter(item -> !"input".equals(item.getName()))
                                .forEach(outputs::add);

                        // 如果没有 output 参数，从 input 创建 output（便于后续节点使用）
                        boolean hasOutput = outputs.stream().anyMatch(item -> "output".equals(item.getName()));
                        if (!hasOutput) {
                            inputs.stream()
                                    .filter(item -> "input".equals(item.getName()))
                                    .findFirst()
                                    .ifPresent(inputParam -> {
                                        String title = inputParam.getContent() != null && inputParam.getContent().getTitle() != null
                                                ? inputParam.getContent().getTitle() : "";
                                        NodeIOData outputParam = NodeIOData.createByText("output", title, inputParam.valueToString());
                                        outputs.add(outputParam);
                                        log.debug("从输入创建输出参数供下游节点使用");
                                    });
                        }

                        outputs.add(NodeIOData.createByText("matched_case", "switcher", String.valueOf(i + 1)));
                        outputs.add(NodeIOData.createByText("case_uuid", "switcher", switcherCase.getUuid()));
                        outputs.add(NodeIOData.createByText("target_node", "switcher", switcherCase.getTargetNodeUuid()));

                        // WorkflowEngine 会自动将 nextNodeUuid 放入 resultMap 的 "next" 键中
                        return NodeProcessResult.builder()
                                .content(outputs)
                                .nextNodeUuid(switcherCase.getTargetNodeUuid())
                                .build();
                    }
                }
            }

            // 所有分支都不满足，使用默认分支
            log.info("没有分支匹配，使用默认分支: {}", config.getDefaultTargetNodeUuid());

            if (StringUtils.isBlank(config.getDefaultTargetNodeUuid())) {
                log.warn("默认目标节点UUID为空，工作流可能在此停止");
            }

            String defaultTarget = config.getDefaultTargetNodeUuid() != null ?
                    config.getDefaultTargetNodeUuid() : "";

            // 构造输出：只保留 output 和其他非 input 参数 + 添加默认分支信息
            List<NodeIOData> outputs = new java.util.ArrayList<>();

            // 过滤输入：排除 input 参数（与 output 冗余），保留其他参数
            inputs.stream()
                    .filter(item -> !"input".equals(item.getName()))
                    .forEach(outputs::add);

            // 如果没有 output 参数，从 input 创建 output（便于后续节点使用）
            boolean hasOutput = outputs.stream().anyMatch(item -> "output".equals(item.getName()));
            if (!hasOutput) {
                inputs.stream()
                        .filter(item -> "input".equals(item.getName()))
                        .findFirst()
                        .ifPresent(inputParam -> {
                            String title = inputParam.getContent() != null && inputParam.getContent().getTitle() != null
                                    ? inputParam.getContent().getTitle() : "";
                            NodeIOData outputParam = NodeIOData.createByText("output", title, inputParam.valueToString());
                            outputs.add(outputParam);
                            log.debug("从输入创建输出参数供下游节点使用");
                        });
            }

            outputs.add(NodeIOData.createByText("matched_case", "switcher", "default"));
            outputs.add(NodeIOData.createByText("target_node", "switcher", defaultTarget));

            // WorkflowEngine 会自动将 nextNodeUuid 放入 resultMap 的 "next" 键中
            return NodeProcessResult.builder()
                    .content(outputs)
                    .nextNodeUuid(config.getDefaultTargetNodeUuid())
                    .build();

        } catch (Exception e) {
            log.error("处理条件分支节点失败: {}", node.getUuid(), e);

            List<NodeIOData> errorOutputs = List.of(
                    NodeIOData.createByText("status", "switcher", "error"),
                    NodeIOData.createByText("error", "switcher", e.getMessage())
            );

            return NodeProcessResult.builder()
                    .content(errorOutputs)
                    .error(true)
                    .message("条件分支节点错误: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 评估单个分支的条件
     *
     * @param switcherCase 分支配置
     * @param inputs       输入数据
     * @return 是否满足条件
     */
    private boolean evaluateCase(SwitcherCase switcherCase, List<NodeIOData> inputs) {
        if (switcherCase.getConditions() == null || switcherCase.getConditions().isEmpty()) {
            log.warn("分支 {} 没有条件，跳过", switcherCase.getUuid());
            return false;
        }

        String operator = switcherCase.getOperator();
        boolean isAnd = "and".equalsIgnoreCase(operator);

        log.debug("使用 {} 逻辑评估 {} 个条件",
                operator, switcherCase.getConditions().size());

        for (SwitcherCase.Condition condition : switcherCase.getConditions()) {
            boolean conditionResult = evaluateCondition(condition, inputs);
            log.debug("条件结果: {} (参数: {}, 运算符: {}, 值: {})",
                    conditionResult, condition.getNodeParamName(),
                    condition.getOperator(), condition.getValue());

            if (isAnd && !conditionResult) {
                // AND 逻辑：任何一个条件不满足就返回 false
                return false;
            } else if (!isAnd && conditionResult) {
                // OR 逻辑：任何一个条件满足就返回 true
                return true;
            }
        }
        // AND 逻辑：所有条件都满足返回 true
        // OR 逻辑：所有条件都不满足返回 false
        return isAnd;
    }

    /**
     * 评估单个条件
     *
     * @param condition 条件配置
     * @param inputs    输入数据
     * @return 是否满足条件
     */
    private boolean evaluateCondition(SwitcherCase.Condition condition, List<NodeIOData> inputs) {
        try {
            log.info("评估条件 - 节点UUID: {}, 参数名: {}, 运算符: {}, 期望值: {}",
                    condition.getNodeUuid(), condition.getNodeParamName(),
                    condition.getOperator(), condition.getValue());

            // 获取实际值
            String actualValue = getValueFromInputs(condition.getNodeUuid(),
                    condition.getNodeParamName(), inputs);

            if (actualValue == null) {
                log.warn("无法找到节点: {}, 参数: {} 的值 - 可用输入: {}",
                        condition.getNodeUuid(), condition.getNodeParamName(),
                        inputs.stream().map(NodeIOData::getName).toList());
                actualValue = "";
            }

            log.info("获取到的实际值: '{}' (类型: {})", actualValue, actualValue.getClass().getSimpleName());

            String expectedValue = condition.getValue() != null ? condition.getValue() : "";
            OperatorEnum operator = OperatorEnum.getByName(condition.getOperator());

            if (operator == null) {
                log.warn("未知运算符: {}，视为false", condition.getOperator());
                return false;
            }

            boolean result = evaluateOperator(operator, actualValue, expectedValue);
            log.info("条件评估结果: {} (实际值='{}', 运算符={}, 期望值='{}')",
                    result, actualValue, operator, expectedValue);

            return result;

        } catch (Exception e) {
            log.error("评估条件时出错: {}", condition, e);
            return false;
        }
    }

    /**
     * 从输入数据中获取指定节点的参数值
     */
    private String getValueFromInputs(String nodeUuid, String paramName, List<NodeIOData> inputs) {
        log.debug("从节点UUID '{}' 搜索参数 '{}'", nodeUuid, paramName);
        
        String result = null;

        // 首先尝试从当前输入中查找
        log.debug("检查当前输入 (数量: {})", inputs.size());
        for (NodeIOData input : inputs) {
            log.debug("  - 输入: 名称='{}', 值='{}'", input.getName(), input.valueToString());
            if (paramName.equals(input.getName())) {
                result = input.valueToString();
            }
        }
        
        if (result != null) {
            log.info("在当前输入中找到参数 '{}': '{}'", paramName, result);
            return result;
        }

        // 如果当前输入中没有，尝试从工作流状态中查找指定节点的输出
        if (StringUtils.isNotBlank(nodeUuid)) {
            List<NodeIOData> nodeOutputs = wfState.getIOByNodeUuid(nodeUuid);
            log.debug("检查节点 '{}' 的输出 (数量: {})", nodeUuid, nodeOutputs.size());
            for (NodeIOData output : nodeOutputs) {
                log.debug("  - 输出: 名称='{}', 值='{}'", output.getName(), output.valueToString());
                if (paramName.equals(output.getName())) {
                    result = output.valueToString();
                }
            }
            
            if (result != null) {
                log.info("在节点 '{}' 的输出中找到参数 '{}': '{}'", nodeUuid, paramName, result);
                return result;
            }
        } else {
            log.debug("节点UUID为空，跳过工作流状态搜索");
        }

        // 特殊处理：如果找的是 'output' 但没找到，尝试找 'input'
        if ("output".equals(paramName)) {
            log.debug("未找到参数 'output'，尝试查找 'input'");
            String inputValue = getValueFromInputs(nodeUuid, "input", inputs);
            if (inputValue != null) {
                return inputValue;
            }
        }

        log.warn("在输入或节点 '{}' 的输出中未找到参数 '{}'", nodeUuid, paramName);
        return null;
    }

    /**
     * 根据运算符评估条件
     */
    private boolean evaluateOperator(OperatorEnum operator, String actualValue, String expectedValue) {
        switch (operator) {
            case CONTAINS:
                return actualValue.contains(expectedValue);

            case NOT_CONTAINS:
                return !actualValue.contains(expectedValue);

            case START_WITH:
                return actualValue.startsWith(expectedValue);

            case END_WITH:
                return actualValue.endsWith(expectedValue);

            case EMPTY:
                return StringUtils.isBlank(actualValue);

            case NOT_EMPTY:
                return StringUtils.isNotBlank(actualValue);

            case EQUAL:
                return actualValue.equals(expectedValue);

            case NOT_EQUAL:
                return !actualValue.equals(expectedValue);

            case GREATER:
            case GREATER_OR_EQUAL:
            case LESS:
            case LESS_OR_EQUAL:
                return evaluateNumericComparison(operator, actualValue, expectedValue);

            default:
                log.warn("不支持的运算符: {}", operator);
                return false;
        }
    }

    /**
     * 评估数值比较
     */
    private boolean evaluateNumericComparison(OperatorEnum operator, String actualValue, String expectedValue) {
        try {
            BigDecimal actual = new BigDecimal(actualValue.trim());
            BigDecimal expected = new BigDecimal(expectedValue.trim());
            int comparison = actual.compareTo(expected);

            switch (operator) {
                case GREATER:
                    return comparison > 0;
                case GREATER_OR_EQUAL:
                    return comparison >= 0;
                case LESS:
                    return comparison < 0;
                case LESS_OR_EQUAL:
                    return comparison <= 0;
                default:
                    return false;
            }
        } catch (NumberFormatException e) {
            log.warn("无法解析数字进行比较: 实际值={}, 期望值={}",
                    actualValue, expectedValue);
            return false;
        }
    }
}
