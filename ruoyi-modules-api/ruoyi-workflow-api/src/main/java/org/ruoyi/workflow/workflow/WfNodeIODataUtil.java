package org.ruoyi.workflow.workflow;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.collections4.CollectionUtils;
import org.ruoyi.common.core.exception.base.BaseException;
import org.ruoyi.workflow.enums.ErrorEnum;
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
        if (null != defaultInputName) {
            defaultInputName.setName(DEFAULT_OUTPUT_PARAM_NAME);
        } else if (null != txtExist) {
            txtExist.setName(DEFAULT_OUTPUT_PARAM_NAME);
        } else if (null != first) {
            first.setName(DEFAULT_OUTPUT_PARAM_NAME);
        }
        result.add(inputs.get(0));
        return result;
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
