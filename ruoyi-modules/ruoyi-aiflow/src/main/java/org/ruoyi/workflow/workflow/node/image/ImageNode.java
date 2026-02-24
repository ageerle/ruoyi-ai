package org.ruoyi.workflow.workflow.node.image;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.workflow.entity.WorkflowComponent;
import org.ruoyi.workflow.entity.WorkflowNode;
import org.ruoyi.workflow.util.SpringUtil;
import org.ruoyi.workflow.workflow.NodeProcessResult;
import org.ruoyi.workflow.workflow.WfNodeState;
import org.ruoyi.workflow.workflow.WfState;
import org.ruoyi.workflow.workflow.WorkflowUtil;
import org.ruoyi.workflow.workflow.data.NodeIOData;
import org.ruoyi.workflow.workflow.node.AbstractWfNode;

import static org.ruoyi.workflow.cosntant.AdiConstant.WorkflowConstant.NODE_PROCESS_STATUS_SUCCESS;

/**
 * 【节点】文生图 <br/>
 * 节点内容固定格式：ImageNodeConfig
 */
@Slf4j
public class ImageNode extends AbstractWfNode {

    public ImageNode(WorkflowComponent wfComponent, WorkflowNode nodeDef, WfState wfState, WfNodeState nodeState) {
        super(wfComponent, nodeDef, wfState, nodeState);
    }

    /**
     * nodeConfig格式：
     * {"prompt": "{input}","model_name":"wan2.5-t2i-preview","size":"1024*1024"}
     *
     * @return 图片地址URL
     */
    @Override
    public NodeProcessResult onProcess() {
        ImageNodeConfig nodeConfigObj = checkAndGetConfig(ImageNodeConfig.class);
        String inputText = getFirstInputText();
        log.info("Image node config:{}", nodeConfigObj);
        String prompt = inputText;
        if (StringUtils.isNotBlank(nodeConfigObj.getPrompt())) {
            prompt = WorkflowUtil.renderTemplate(nodeConfigObj.getPrompt(), state.getInputs());
        }
        log.info("Image prompt:{}", prompt);
        // 获取工作流实例
        WorkflowUtil workflowUtil = SpringUtil.getBean(WorkflowUtil.class);
        // 获取模型名称
        String modelName = nodeConfigObj.getModelName();
        // 获取图片大小
        String size = nodeConfigObj.getSize();
        // 获取随机数种子
        Integer seed = nodeConfigObj.getSeed();
        // 调用LLM生成图片（后续可以将图片保存到OSS中）
        String imageUrl = workflowUtil.buildTextToImage(modelName, prompt, size, seed);
        // 创建节点参数对象
        NodeIOData nodeIOData = NodeIOData.createByText("output", "image", imageUrl);
        // 添加到输出列表以便给后续节点使用
        state.getOutputs().add(nodeIOData);
        // 设置为成功状态
        state.setProcessStatus(NODE_PROCESS_STATUS_SUCCESS);
        return new NodeProcessResult();
    }
}
