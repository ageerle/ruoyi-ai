package org.ruoyi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.domain.KnowledgeRole;
import org.ruoyi.domain.KnowledgeRoleGroup;
import org.ruoyi.domain.bo.KnowledgeRoleGroupBo;
import org.ruoyi.domain.vo.KnowledgeRoleGroupVo;
import org.ruoyi.mapper.KnowledgeRoleGroupMapper;
import org.ruoyi.mapper.KnowledgeRoleMapper;
import org.ruoyi.service.IKnowledgeRoleGroupService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识库角色组Service业务层处理
 *
 * @author ageerle
 * @date 2025-07-19
 */
@RequiredArgsConstructor
@Service
public class KnowledgeRoleGroupServiceImpl implements IKnowledgeRoleGroupService {

    private final KnowledgeRoleGroupMapper baseMapper;
    private final KnowledgeRoleMapper knowledgeRoleMapper;
    private final KnowledgeRoleServiceImpl knowledgeRoleServiceImpl;

    /**
     * 查询知识库角色组
     */
    @Override
    public KnowledgeRoleGroupVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询知识库角色组列表
     */
    @Override
    public TableDataInfo<KnowledgeRoleGroupVo> queryPageList(KnowledgeRoleGroupBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<KnowledgeRoleGroup> lqw = buildQueryWrapper(bo);
        Page<KnowledgeRoleGroupVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询知识库角色组列表
     */
    @Override
    public List<KnowledgeRoleGroupVo> queryList(KnowledgeRoleGroupBo bo) {
        LambdaQueryWrapper<KnowledgeRoleGroup> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<KnowledgeRoleGroup> buildQueryWrapper(KnowledgeRoleGroupBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<KnowledgeRoleGroup> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getName()), KnowledgeRoleGroup::getName, bo.getName());
        return lqw;
    }

    /**
     * 新增知识库角色组
     */
    @Override
    public Boolean insertByBo(KnowledgeRoleGroupBo bo) {
        KnowledgeRoleGroup add = MapstructUtils.convert(bo, KnowledgeRoleGroup.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改知识库角色组
     */
    @Override
    public Boolean updateByBo(KnowledgeRoleGroupBo bo) {
        KnowledgeRoleGroup update = MapstructUtils.convert(bo, KnowledgeRoleGroup.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(KnowledgeRoleGroup entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除知识库角色组
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }

        // 查询role表
        LambdaQueryWrapper<KnowledgeRole> lqwState = Wrappers.lambdaQuery();
        lqwState.in(KnowledgeRole::getGroupId, ids);
        List<KnowledgeRole> knowledgeRoles = knowledgeRoleMapper.selectList();
        knowledgeRoleServiceImpl.deleteWithValidByIds(knowledgeRoles.stream().map(KnowledgeRole::getId).collect(Collectors.toList()), true);

        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
