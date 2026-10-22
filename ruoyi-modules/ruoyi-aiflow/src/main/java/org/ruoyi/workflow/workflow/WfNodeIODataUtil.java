package org.ruoyi.workflow.workflow;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.ruoyi.common.chat.enums.ErrorEnum;
import org.ruoyi.common.core.exception.base.BaseException;
import org.ruoyi.workflow.enums.WfIODataTypeEnum;
import org.ruoyi.workflow.util.JsonUtil;
import org.ruoyi.workflow.workflow.data.NodeIOData;
import org.ruoyi.workflow.workflow.data.NodeIODataFilesContent;
import org.ruoyi.workflow.workflow.def.*;

import java.util.*;

import static org.ruoyi.workflow.cosntant.AdiConstant.IMAGE_EXTENSIONS;
import static org.ruoyi.workflow.cosntant.AdiConstant.WorkflowConstant.DEFAULT_INPUT_PARAM_NAME;
import static org.ruoyi.workflow.cosntant.AdiConstant.WorkflowConstant.DEFAULT_OUTPUT_PARAM_NAME;

public class WfNodeIODataUtil {

    public static final Map<WfIODataTypeEnum, Class<? extends WfNodeIO>> INPUT_TYPE_TO_NODE_IO_DEF = new HashMap<>();

    static {
        INPUT_TYPE_TO_NODE_IO_DEF.put(WfIODataTypeEnum.TEXT, WfNodeIOText.class);
        INPUT_TYPE_TO_NODE_IO_DEF.put(WfIODataTypeEnum.BOOL, WfNodeIOBool.class);
        INPUT_TYPE_TO_NODE_IO_DEF.put(WfIODataTypeEnum.NUMBER, WfNodeIONumber.class);
        INPUT_TYPE_TO_NODE_IO_DEF.put(WfIODataTypeEnum.OPTIONS, WfNodeIOOptions.class);
        INPUT_TYPE_TO_NODE_IO_DEF.put(WfIODataTypeEnum.FILES, WfNodeIOFiles.class);

    }

    public static NodeIOData createNodeIOData(ObjectNode data) {
        JsonNode nameObj = data.get("name");
        JsonNode content = data.get("content");
        if (null == nameObj || null == content) {
            throw new BaseException(ErrorEnum.A_PARAMS_ERROR.getInfo());
        }
        String name = nameObj.asText();
        Integer type = content.get("type").asInt();
        String title = content.get("title").asText();
        JsonNode value = content.get("value");
        NodeIOData result = null;
        if (WfIODataTypeEnum.TEXT.getValue().equals(type)) {
            result = NodeIOData.createByText(name, title, value.asText());
        } else if (WfIODataTypeEnum.NUMBER.getValue().equals(type)) {
            result = NodeIOData.createByNumber(name, title, value.asDouble());
        } else if (WfIODataTypeEnum.BOOL.getValue().equals(type)) {
            result = NodeIOData.createByBool(name, title, value.asBoolean());
        } else if (WfIODataTypeEnum.FILES.getValue().equals(type)) {
            if (value.isArray()) {
                List<String> fileUrls = new ArrayList<>();
                Iterator<JsonNode> iterator = value.elements();
                while (iterator.hasNext()) {
                    fileUrls.add(iterator.next().asText());
                }
                result = NodeIOData.createByFiles(name, title, fileUrls);
            }
        } else if (WfIODataTypeEnum.OPTIONS.getValue().equals(type)) {
            if (value instanceof ObjectNode) {
                result = NodeIOData.createByOptions(name, title, JsonUtil.toMap(value));
            }
        }
        return result;
    }

    /**
     * 1.如果没有名称为 output 的输出参数，则需要新增 <br/>
     * 2.判断是否已经有文本类型的输出参数，如果有，则复制该参数并将参数名改为 output <br/>
     * 3.如果没有文本类型的参数，则复制第一个参数，并将参数名改为 output
     *
     * @param inputs 输入参数列表
     * @return 输出参数列表
     */
    public static List<NodeIOData> changeInputsToOutputs(List<NodeIOData> inputs) {
        if (CollectionUtils.isEmpty(inputs)) {
            return new ArrayList<>();
        }
        List<NodeIOData> result = CollUtil.newCopyOnWriteArrayList(inputs);

        boolean outputExist = false;
        NodeIOData defaultInputName = null, txtExist = null, first = null;
        for (NodeIOData nodeIOData : result) {
            if (null == first) {
                first = nodeIOData;
            }
            if (DEFAULT_OUTPUT_PARAM_NAME.equals(nodeIOData.getName())) {
                outputExist = true;
            } else if (DEFAULT_INPUT_PARAM_NAME.equals(nodeIOData.getName())) {
                defaultInputName = nodeIOData;
            } else if (null == txtExist && WfIODataTypeEnum.TEXT.getValue().equals(nodeIOData.getContent().getType())) {
                txtExist = nodeIOData;
            }
        }

        if (outputExist) {
            return result;
        }
        // 必须"复制后改名"而非原地改名：inputs 中的 NodeIOData 对象同时被 wfState.getInput()、
        // 开始节点自身的 state.getInputs() 以及运行时输入记录引用，原地 setName 会销毁原始参数名，
        // 下游节点按名引用（LLM 模板变量 {{xxx}}、条件分支取值、getFirstInputText）将全部取不到用户输入。
        // 同理不能直接 add 原对象（会得到两条指向同一实例的重复参数），应 add 改名后的副本。
        if (null != defaultInputName) {
            result.add(copyAndRenameToOutput(defaultInputName));
        } else if (null != txtExist) {
            result.add(copyAndRenameToOutput(txtExist));
        } else if (null != first) {
            result.add(copyAndRenameToOutput(first));
        }
        return result;
    }

    /**
     * 复制输入参数，并将副本的名称改为默认输出参数名（output）
     * <p>
     * 使用深拷贝（与 {@code AbstractWfNode#createByReferParam} 一致）：
     * 副本与原始参数的 content 相互独立，避免 EndNode 的
     * {@link #changeFilesContentToMarkdown} 等原地修改 content 的逻辑互相污染。
     *
     * @param source 被复制的输入参数
     * @return 名称为 output 的副本
     */
    private static NodeIOData copyAndRenameToOutput(NodeIOData source) {
        NodeIOData copy = SerializationUtils.clone(source);
        copy.setName(DEFAULT_OUTPUT_PARAM_NAME);
        return copy;
    }

    /**
     * 将输入输出中的文件url转成markdown格式的文件地址<br/>
     * 将变量渲染到模板时使用该方法，其他情况交由前端处理
     *
     * @param ioDataList 输入输出列表
     */
    public static void changeFilesContentToMarkdown(List<NodeIOData> ioDataList) {
        ioDataList.forEach(input -> {
            if (input.getContent() instanceof NodeIODataFilesContent filesContent) {
                List<String> newValues = new ArrayList<>();
                for (String s : filesContent.getValue()) {
                    if (IMAGE_EXTENSIONS.contains(s.substring(s.lastIndexOf(".") + 1))) {
                        newValues.add("![" + filesContent.getTitle() + "](" + s + ")");
                    } else {
                        newValues.add("[" + filesContent.getTitle() + "](" + s + ")");
                    }
                }
                filesContent.setValue(newValues);
            }
        });
    }
}
