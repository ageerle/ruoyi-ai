package org.ruoyi.workflow.workflow;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.workflow.enums.WfIODataTypeEnum;
import org.ruoyi.workflow.workflow.data.NodeIOData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link WfNodeIODataUtil#changeInputsToOutputs} 单元测试。
 * <p>
 * 聚焦 2026-10 修复的缺陷：原实现把选中参数「原地改名」为 output 后又 add 了原对象本身，
 * 导致 ①开始节点输入参数名被销毁、②输出列表出现两条指向同一实例的重复参数，
 * 下游 LLM 节点因此拿到空 prompt（详见 PR 关联 issue）。
 */
@Tag("dev")
class WfNodeIODataUtilTest {

    private static final String DEFAULT_OUTPUT = "output";
    private static final String DEFAULT_INPUT = "input";

    private NodeIOData text(String name, String value) {
        return NodeIOData.createByText(name, name, value);
    }

    private Optional<NodeIOData> byName(List<NodeIOData> list, String name) {
        return list.stream().filter(item -> name.equals(item.getName())).findFirst();
    }

    @Nested
    @DisplayName("新增 output 参数时保留原始输入参数")
    class KeepsOriginalParams {

        @Test
        @DisplayName("默认 input 参数：原参数保留，新增同名值 output 副本")
        void defaultInputParamIsKeptAndOutputCopyAdded() {
            List<NodeIOData> inputs = new ArrayList<>();
            inputs.add(text(DEFAULT_INPUT, "你好"));

            List<NodeIOData> outputs = WfNodeIODataUtil.changeInputsToOutputs(inputs);

            assertEquals(2, outputs.size());
            // 原始 input 参数未被改名、未被销毁
            assertTrue(byName(outputs, DEFAULT_INPUT).isPresent());
            assertEquals("你好", byName(outputs, DEFAULT_INPUT).orElseThrow().valueToString());
            // 新增的 output 副本携带同样的值
            assertTrue(byName(outputs, DEFAULT_OUTPUT).isPresent());
            assertEquals("你好", byName(outputs, DEFAULT_OUTPUT).orElseThrow().valueToString());
            // output 是副本，不是原对象
            assertNotSame(inputs.get(0), byName(outputs, DEFAULT_OUTPUT).orElseThrow());
        }

        @Test
        @DisplayName("自定义名参数（仓库自带样例的 var_user_input）：行为同上")
        void customNamedParamIsKeptAndOutputCopyAdded() {
            List<NodeIOData> inputs = new ArrayList<>();
            inputs.add(text("var_user_input", "帮我总结一下这篇文章"));

            List<NodeIOData> outputs = WfNodeIODataUtil.changeInputsToOutputs(inputs);

            assertEquals(2, outputs.size());
            assertTrue(byName(outputs, "var_user_input").isPresent());
            assertTrue(byName(outputs, DEFAULT_OUTPUT).isPresent());
            assertEquals("帮我总结一下这篇文章", byName(outputs, DEFAULT_OUTPUT).orElseThrow().valueToString());
        }

        @Test
        @DisplayName("多参数：全部原始参数保留，仅新增一个 output")
        void multipleParamsAllKeptAndSingleOutputAdded() {
            List<NodeIOData> inputs = new ArrayList<>();
            inputs.add(text("question", "问题"));
            inputs.add(NodeIOData.createByNumber("count", "数量", 3.0));

            List<NodeIOData> outputs = WfNodeIODataUtil.changeInputsToOutputs(inputs);

            assertEquals(3, outputs.size());
            assertTrue(byName(outputs, "question").isPresent());
            assertTrue(byName(outputs, "count").isPresent());
            // 只有一个 output，且取自第一个文本类型参数
            assertEquals(1, outputs.stream().filter(item -> DEFAULT_OUTPUT.equals(item.getName())).count());
            assertEquals("问题", byName(outputs, DEFAULT_OUTPUT).orElseThrow().valueToString());
        }

        @Test
        @DisplayName("无文本参数时复制第一个参数")
        void firstParamCopiedWhenNoTextParam() {
            List<NodeIOData> inputs = new ArrayList<>();
            inputs.add(NodeIOData.createByNumber("count", "数量", 7.0));
            inputs.add(NodeIOData.createByBool("flag", "开关", true));

            List<NodeIOData> outputs = WfNodeIODataUtil.changeInputsToOutputs(inputs);

            assertEquals(3, outputs.size());
            assertTrue(byName(outputs, DEFAULT_OUTPUT).isPresent());
            assertEquals("7.0", byName(outputs, DEFAULT_OUTPUT).orElseThrow().valueToString());
        }

        @Test
        @DisplayName("原始输入列表不被修改（修复原地改名缺陷）")
        void inputListIsNotMutated() {
            List<NodeIOData> inputs = new ArrayList<>();
            inputs.add(text("var_user_input", "原文"));

            WfNodeIODataUtil.changeInputsToOutputs(inputs);

            assertEquals(1, inputs.size());
            assertEquals("var_user_input", inputs.get(0).getName());
            assertEquals("原文", inputs.get(0).valueToString());
        }

        @Test
        @DisplayName("副本与原参数 content 相互独立（深拷贝）")
        void outputCopyHasIndependentContent() {
            List<NodeIOData> inputs = new ArrayList<>();
            inputs.add(text(DEFAULT_INPUT, "原文"));

            List<NodeIOData> outputs = WfNodeIODataUtil.changeInputsToOutputs(inputs);

            NodeIOData copy = byName(outputs, DEFAULT_OUTPUT).orElseThrow();
            assertNotSame(inputs.get(0).getContent(), copy.getContent());
        }
    }

    @Nested
    @DisplayName("已存在 output 参数时保持原样")
    class ExistingOutputUntouched {

        @Test
        @DisplayName("不重复新增、不改名")
        void listReturnedUnchanged() {
            List<NodeIOData> inputs = new ArrayList<>();
            inputs.add(text(DEFAULT_OUTPUT, "已有输出"));
            inputs.add(text(DEFAULT_INPUT, "用户输入"));

            List<NodeIOData> outputs = WfNodeIODataUtil.changeInputsToOutputs(inputs);

            assertEquals(2, outputs.size());
            assertEquals("已有输出", byName(outputs, DEFAULT_OUTPUT).orElseThrow().valueToString());
            assertEquals("用户输入", byName(outputs, DEFAULT_INPUT).orElseThrow().valueToString());
        }
    }

    @Nested
    @DisplayName("边界输入")
    class EdgeCases {

        @Test
        @DisplayName("空列表返回空列表")
        void emptyInputReturnsEmptyList() {
            assertTrue(WfNodeIODataUtil.changeInputsToOutputs(new ArrayList<>()).isEmpty());
        }

        @Test
        @DisplayName("null 返回空列表")
        void nullInputReturnsEmptyList() {
            assertTrue(WfNodeIODataUtil.changeInputsToOutputs(null).isEmpty());
        }
    }

    @Nested
    @DisplayName("下游节点消费链路回归（开始节点 → LLM 节点）")
    class DownstreamConsumption {

        /**
         * 复现仓库自带样例工作流（docs/script/sql/ruoyi-ai.sql 中 workflow 119）：
         * 开始节点 user_inputs=[var_user_input]（TEXT，无 prologue），
         * 下游「生成回答」节点 prompt 模板为空。
         * <p>
         * 这里按 {@code AbstractWfNode#initInput} 与 {@code AbstractWfNode#getFirstInputText}
         * 的等价逻辑消费开始节点输出：修复前输出列表是两条同实例的 output，
         * 经下游改名后变成两条 input，getFirstInputText 因 size>1 排除 input 而返回空串，
         * LLM 收到空 prompt、用户输入被静默丢弃。
         */
        @Test
        @DisplayName("LLM 节点应拿到用户输入作为 prompt")
        void llmNodeReceivesUserInputAsPrompt() {
            List<NodeIOData> startInputs = new ArrayList<>();
            startInputs.add(text("var_user_input", "帮我总结一下这篇文章"));

            List<NodeIOData> startOutputs = WfNodeIODataUtil.changeInputsToOutputs(startInputs);

            // 等价 AbstractWfNode#initInput：上游 output 改名 input，其余按节点声明的入参过滤
            // （样例 LLM 节点未声明任何入参，故仅保留 output 转成的 input）
            List<NodeIOData> llmInputs = new ArrayList<>();
            for (NodeIOData io : startOutputs) {
                if (DEFAULT_OUTPUT.equals(io.getName())) {
                    NodeIOData renamed = SerializationUtils.clone(io);
                    renamed.setName(DEFAULT_INPUT);
                    llmInputs.add(renamed);
                }
            }

            // 等价 AbstractWfNode#getFirstInputText
            String prompt;
            Optional<NodeIOData> outputParam = llmInputs.stream()
                    .filter(item -> DEFAULT_OUTPUT.equals(item.getName()))
                    .findFirst();
            if (outputParam.isPresent()) {
                prompt = outputParam.orElseThrow().valueToString();
            } else if (llmInputs.size() > 1) {
                prompt = llmInputs.stream()
                        .filter(item -> WfIODataTypeEnum.TEXT.getValue().equals(item.getContent().getType())
                                && !DEFAULT_INPUT.equals(item.getName()))
                        .map(NodeIOData::valueToString)
                        .findFirst()
                        .orElse("");
            } else {
                prompt = llmInputs.get(0).valueToString();
            }

            assertEquals("帮我总结一下这篇文章", prompt);
        }
    }
}
