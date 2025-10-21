package org.ruoyi.workflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.common.core.exception.base.BaseException;
import org.ruoyi.workflow.dto.workflow.WfComponentReq;
import org.ruoyi.workflow.dto.workflow.WfComponentSearchReq;
import org.ruoyi.workflow.entity.WorkflowComponent;
import org.ruoyi.workflow.enums.ErrorEnum;
import org.ruoyi.workflow.mapper.WorkflowComponentMapper;
import org.ruoyi.workflow.util.PrivilegeUtil;
import org.ruoyi.workflow.util.UuidUtil;
import org.ruoyi.workflow.workflow.WfComponentNameEnum;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.ruoyi.workflow.cosntant.RedisKeyConstant.WORKFLOW_COMPONENTS;
import static org.ruoyi.workflow.cosntant.RedisKeyConstant.WORKFLOW_COMPONENT_START_KEY;
import static org.ruoyi.workflow.enums.ErrorEnum.C_WF_COMPONENT_DELETED_FAIL_BY_USED;

@Slf4j
@Service
public class WorkflowComponentService extends ServiceImpl<WorkflowComponentMapper, WorkflowComponent> {

    @Lazy
    @Resource
    private WorkflowComponentService self;

    @CacheEvict(cacheNames = {WORKFLOW_COMPONENTS, WORKFLOW_COMPONENT_START_KEY})
    public WorkflowComponent addOrUpdate(WfComponentReq req) {
        WorkflowComponent wfComponent;
        if (StringUtils.isNotBlank(req.getUuid())) {
            wfComponent = PrivilegeUtil.checkAndGetByUuid(req.getUuid(), this.query(), ErrorEnum.A_WF_COMPONENT_NOT_FOUND);

            WorkflowComponent update = new WorkflowComponent();
            BeanUtils.copyProperties(req, update, "id", "uuid");
            update.setId(wfComponent.getId());
            update.setName(req.getName());
            update.setTitle(req.getTitle());
            update.setRemark(req.getRemark());
            update.setIsEnable(req.getIsEnable());
            update.setDisplayOrder(req.getDisplayOrder());
            this.baseMapper.updateById(update);

            return update;
        } else {
            wfComponent = new WorkflowComponent();
            BeanUtils.copyProperties(req, wfComponent, "id", "uuid");
            wfComponent.setUuid(UuidUtil.createShort());
            this.baseMapper.insert(wfComponent);

            return wfComponent;
        }
    }

    @CacheEvict(cacheNames = {WORKFLOW_COMPONENTS, WORKFLOW_COMPONENT_START_KEY})
    public void enable(String uuid, Boolean isEnable) {
        WorkflowComponent wfComponent = PrivilegeUtil.checkAndGetByUuid(uuid, this.query(), ErrorEnum.A_WF_COMPONENT_NOT_FOUND);
        WorkflowComponent update = new WorkflowComponent();
        update.setIsEnable(isEnable);
        update.setId(wfComponent.getId());
        this.baseMapper.updateById(update);
    }

    @CacheEvict(cacheNames = {WORKFLOW_COMPONENTS, WORKFLOW_COMPONENT_START_KEY})
    public void deleteByUuid(String uuid) {
        WorkflowComponent component = PrivilegeUtil.checkAndGetByUuid(uuid, this.query(), ErrorEnum.A_WF_COMPONENT_NOT_FOUND);
        Integer refNodeCount = baseMapper.countRefNodes(uuid);
        if (refNodeCount != null && refNodeCount > 0) {
            throw new BaseException(C_WF_COMPONENT_DELETED_FAIL_BY_USED.getInfo());
        }
        boolean updated = ChainWrappers.lambdaUpdateChain(baseMapper)
                .eq(WorkflowComponent::getId, component.getId())
                .set(WorkflowComponent::getIsDeleted, true)
                .set(WorkflowComponent::getIsEnable, false)
                .update();
        if (!updated) {
            throw new BaseException(ErrorEnum.A_WF_COMPONENT_NOT_FOUND.getInfo());
        }
    }

    public Page<WorkflowComponent> search(WfComponentSearchReq searchReq, Integer currentPage, Integer pageSize) {
        LambdaQueryWrapper<WorkflowComponent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowComponent::getIsDeleted, false);
        wrapper.eq(null != searchReq.getIsEnable(), WorkflowComponent::getIsEnable, searchReq.getIsEnable());
        wrapper.like(StringUtils.isNotBlank(searchReq.getTitle()), WorkflowComponent::getTitle, searchReq.getTitle());
        wrapper.orderByAsc(List.of(WorkflowComponent::getDisplayOrder, WorkflowComponent::getId));
        return baseMapper.selectPage(new Page<>(currentPage, pageSize), wrapper);
    }

    @Cacheable(cacheNames = WORKFLOW_COMPONENTS)
    public List<WorkflowComponent> getAllEnable() {
        return ChainWrappers.lambdaQueryChain(baseMapper)
                .eq(WorkflowComponent::getIsEnable, true)
                .eq(WorkflowComponent::getIsDeleted, false)
                .orderByAsc(List.of(WorkflowComponent::getDisplayOrder, WorkflowComponent::getId))
                .list();
    }

    @Cacheable(cacheNames = WORKFLOW_COMPONENT_START_KEY)
    public WorkflowComponent getStartComponent() {
        List<WorkflowComponent> components = self.getAllEnable();
        return components.stream()
                .filter(component -> WfComponentNameEnum.START.getName().equals(component.getName()))
                .findFirst()
                .orElseThrow(() -> new BaseException(ErrorEnum.B_WF_NODE_DEFINITION_NOT_FOUND.getInfo()));
    }

    public WorkflowComponent getComponent(Long id) {
        List<WorkflowComponent> components = self.getAllEnable();
        return components.stream()
                .filter(component -> component.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new BaseException(ErrorEnum.B_WF_NODE_DEFINITION_NOT_FOUND.getInfo()));
    }
}
