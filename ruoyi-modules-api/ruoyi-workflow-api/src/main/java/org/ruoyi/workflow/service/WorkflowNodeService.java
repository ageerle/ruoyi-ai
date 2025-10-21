package org.ruoyi.workflow.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.ruoyi.common.core.exception.base.BaseException;
import org.ruoyi.workflow.dto.workflow.WfNodeDto;
import org.ruoyi.workflow.entity.Workflow;
import org.ruoyi.workflow.entity.WorkflowComponent;
import org.ruoyi.workflow.entity.WorkflowNode;
import org.ruoyi.workflow.enums.ErrorEnum;
import org.ruoyi.workflow.enums.WfIODataTypeEnum;
import org.ruoyi.workflow.mapper.WorkflowNodeMapper;
import org.ruoyi.workflow.util.JsonUtil;
import org.ruoyi.workflow.util.MPPageUtil;
import org.ruoyi.workflow.util.UuidUtil;
import org.ruoyi.workflow.workflow.WfComponentNameEnum;
import org.ruoyi.workflow.workflow.WfNodeInputConfig;
import org.ruoyi.workflow.workflow.def.WfNodeIOText;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class WorkflowNodeService extends ServiceImpl<WorkflowNodeMapper, WorkflowNode> {

    @Lazy
    @Resource
    private WorkflowNodeService self;

    @Resource
    private WorkflowComponentService workflowComponentService;

    public WorkflowNode getStartNode(long workflowId) {
        return baseMapper.getStartNode(workflowId);
    }

    public List<WfNodeDto> listDtoByWfId(long workflowId) {
        List<WorkflowNode> workflowNodeList = ChainWrappers.lambdaQueryChain(baseMapper)
                .eq(WorkflowNode::getWorkflowId, workflowId)
                .eq(WorkflowNode::getIsDeleted, false)
                .list();
        workflowNodeList.forEach(this::checkAndDecrypt);
        return MPPageUtil.convertToList(workflowNodeList, WfNodeDto.class, (source, target) -> {
            target.setInputConfig(JsonUtil.toBean(source.getInputConfig(), ObjectNode.class));
            target.setNodeConfig(JsonUtil.toBean(source.getNodeConfig(), ObjectNode.class));
            return target;
        });
    }

    public WorkflowNode getByUuid(long workflowId, String uuid) {
        WorkflowNode node = ChainWrappers.lambdaQueryChain(baseMapper)
                .eq(WorkflowNode::getWorkflowId, workflowId)
                .eq(WorkflowNode::getUuid, uuid)
                .eq(WorkflowNode::getIsDeleted, false)
                .last("limit 1")
                .one();
        checkAndDecrypt(node);
        return node;
    }

    public List<WorkflowNode> listByWorkflowId(Long workflowId) {
        List<WorkflowNode> list = ChainWrappers.lambdaQueryChain(baseMapper)
                .eq(WorkflowNode::getWorkflowId, workflowId)
                .eq(WorkflowNode::getIsDeleted, false)
                .list();
        list.forEach(this::checkAndDecrypt);
        return list;
    }

    public List<WorkflowNode> copyByWorkflowId(long workflowId, long targetWorkflowId) {
        List<WorkflowNode> result = new ArrayList<>();
        self.listByWorkflowId(workflowId).forEach(node -> {
            result.add(self.copyNode(targetWorkflowId, node));
        });
        return result;
    }

    public WorkflowNode copyNode(Long targetWorkflowId, WorkflowNode sourceNode) {
        WorkflowNode newNode = new WorkflowNode();
        BeanUtils.copyProperties(sourceNode, newNode, "id", "createTime", "updateTime");
        newNode.setWorkflowId(targetWorkflowId);
        baseMapper.insert(newNode);

        return ChainWrappers.lambdaQueryChain(baseMapper)
                .eq(WorkflowNode::getWorkflowId, targetWorkflowId)
                .eq(WorkflowNode::getUuid, newNode.getUuid())
                .eq(WorkflowNode::getIsDeleted, false)
                .last("limit 1")
                .one();
    }

    @Transactional
    public void createOrUpdateNodes(Long workflowId, List<WfNodeDto> nodes) {
        List<Object> uuidList = new ArrayList<>();
        for (WfNodeDto node : nodes) {
            WorkflowNode newOrUpdate = new WorkflowNode();
            BeanUtils.copyProperties(node, newOrUpdate);
            newOrUpdate.setInputConfig(JsonUtil.toJson(node.getInputConfig()));
            newOrUpdate.setNodeConfig(JsonUtil.toJson(node.getNodeConfig()));
            newOrUpdate.setWorkflowId(workflowId);
            checkAndEncrypt(newOrUpdate);
            WorkflowNode old = self.getByUuid(workflowId, node.getUuid());
            if (null != old) {
                log.info("更新节点,uuid:{},title:{}", node.getUuid(), node.getTitle());
                newOrUpdate.setId(old.getId());
            } else {
                log.info("新增节点,uuid:{},title:{}", node.getUuid(), node.getTitle());
                newOrUpdate.setId(null);
            }
            uuidList.add(node.getUuid());
            self.saveOrUpdate(newOrUpdate);
        }
        ChainWrappers.lambdaUpdateChain(baseMapper)
                .eq(WorkflowNode::getWorkflowId, workflowId)
                .notIn(CollUtil.isNotEmpty(uuidList), WorkflowNode::getUuid, uuidList)
                .set(WorkflowNode::getIsDeleted, true)
                .update();
    }

    private void checkAndEncrypt(WorkflowNode workflowNode) {
        WorkflowComponent component = workflowComponentService.getAllEnable()
                .stream()
                .filter(item -> item.getId().equals(workflowNode.getWorkflowComponentId()))
                .findFirst()
                .orElse(null);
        if (null == component) {
            log.error("节点不存在,uuid:{},title:{}", workflowNode.getUuid(), workflowNode.getTitle());
            throw new BaseException(ErrorEnum.A_PARAMS_ERROR.getInfo());
        }
        if (component.getName().equals(WfComponentNameEnum.MAIL_SEND.getName())) {

            //加密（目前暂时只在数据库层做加密，前后端交互时数据加解密待定）
//            MailSendNodeConfig mailSendNodeConfig = JsonUtil.fromJson(workflowNode.getNodeConfig(), MailSendNodeConfig.class);
//            if (null != mailSendNodeConfig && null != mailSendNodeConfig.getSender() && null != mailSendNodeConfig.getSender().getPassword()) {
//                String password = mailSendNodeConfig.getSender().getPassword();
//                String encrypt = AesUtil.encrypt(password);
//                mailSendNodeConfig.getSender().setPassword(encrypt);
//                workflowNode.setNodeConfig(JsonUtil.toJson(mailSendNodeConfig));
//            }
        }
    }

    private void checkAndDecrypt(WorkflowNode workflowNode) {
        if (null == workflowNode) {
            log.warn("节点不存在");
            return;
        }
        WorkflowComponent component = workflowComponentService.getAllEnable()
                .stream()
                .filter(item -> item.getId().equals(workflowNode.getWorkflowComponentId()))
                .findFirst()
                .orElse(null);
        if (null == component) {
            log.error("节点不存在,uuid:{},title:{}", workflowNode.getUuid(), workflowNode.getTitle());
            throw new BaseException(ErrorEnum.A_PARAMS_ERROR.getInfo());
        }
        if (component.getName().equals(WfComponentNameEnum.MAIL_SEND.getName())) {
//            MailSendNodeConfig mailSendNodeConfig = JsonUtil.fromJson(workflowNode.getNodeConfig(), MailSendNodeConfig.class);
//            if (null != mailSendNodeConfig && null != mailSendNodeConfig.getSender() && null != mailSendNodeConfig.getSender().getPassword()) {
//                String password = mailSendNodeConfig.getSender().getPassword();
//                if (StringUtils.isNotBlank(password)) {
//                    String decrypt = AesUtil.decrypt(password);
//                    mailSendNodeConfig.getSender().setPassword(decrypt);
//                }
//                workflowNode.setNodeConfig(JsonUtil.toJson(mailSendNodeConfig));
//            }
        }
    }

    @Transactional
    public void deleteNodes(Long workflowId, List<String> uuids) {
        if (CollectionUtils.isEmpty(uuids)) {
            return;
        }
        for (String uuid : uuids) {
            WorkflowNode old = self.getByUuid(workflowId, uuid);
            if (null == old) {
                continue;
            }
            if (!old.getWorkflowId().equals(workflowId)) {
                log.error("节点不属于指定的工作流,删除失败,workflowId:{},node workflowId:{}", workflowId, workflowId);
                throw new BaseException(ErrorEnum.A_PARAMS_ERROR.getInfo());
            }
            if (workflowComponentService.getStartComponent().getId().equals(old.getWorkflowComponentId())) {
                log.warn("开始节点不能删除,uuid:{}", old.getUuid());
                continue;
            }
            ChainWrappers.lambdaUpdateChain(baseMapper)
                    .eq(WorkflowNode::getWorkflowId, workflowId)
                    .eq(WorkflowNode::getUuid, uuid)
                    .set(WorkflowNode::getIsDeleted, true)
                    .update();
        }

    }

    /**
     * user_inputs:
     * [
     * {
     * "uuid": "12bc919774aa4e779d97e3dd9c836e11",
     * "name": "var_user_input",
     * "title": "用户输入",
     * "type": 1,
     * "required": true,
     * "max_length": 1000
     * }
     * ]
     *
     * @param workflow 工作流定义
     */
    public WorkflowNode createStartNode(Workflow workflow) {
        WfNodeIOText wfNodeIOText = new WfNodeIOText();
        wfNodeIOText.setUuid(UuidUtil.createShort());
        wfNodeIOText.setType(WfIODataTypeEnum.TEXT.getValue());
        wfNodeIOText.setName("var_user_input");
        wfNodeIOText.setTitle("用户输入");
        wfNodeIOText.setRequired(false);
        wfNodeIOText.setMaxLength(1000);
        WfNodeInputConfig nodeInputConfig = new WfNodeInputConfig();
        nodeInputConfig.setUserInputs(List.of(wfNodeIOText));
        nodeInputConfig.setRefInputs(new ArrayList<>());
        WorkflowComponent startComponent = workflowComponentService.getStartComponent();
        WorkflowNode node = new WorkflowNode();
        node.setWorkflowComponentId(startComponent.getId());
        node.setWorkflowId(workflow.getId());
        node.setRemark("用户输入");
        node.setUuid(UuidUtil.createShort());
        node.setTitle("开始");
        node.setInputConfig(JsonUtil.toJson(nodeInputConfig));
        node.setNodeConfig("{}");
        baseMapper.insert(node);
        return node;
    }
}
