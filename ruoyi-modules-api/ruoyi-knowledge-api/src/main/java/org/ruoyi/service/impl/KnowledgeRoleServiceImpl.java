package org.ruoyi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.domain.KnowledgeRole;
import org.ruoyi.domain.KnowledgeRoleGroup;
import org.ruoyi.domain.KnowledgeRoleRelation;
import org.ruoyi.domain.bo.KnowledgeRoleBo;
import org.ruoyi.domain.vo.KnowledgeRoleVo;
import org.ruoyi.mapper.KnowledgeRoleGroupMapper;
import org.ruoyi.mapper.KnowledgeRoleMapper;
import org.ruoyi.mapper.KnowledgeRoleRelationMapper;
import org.ruoyi.service.IKnowledgeRoleService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库角色Service业务层处理
 *
 * @author ageerle
 * @date 2025-07-19
 */
@RequiredArgsConstructor
@Service
public class KnowledgeRoleServiceImpl implements IKnowledgeRoleService {

    private final KnowledgeRoleMapper baseMapper;
    private final KnowledgeRoleGroupMapper knowledgeRoleGroupMapper;
    private final KnowledgeRoleRelationMapper knowledgeRoleRelationMapper;
    private final KnowledgeRoleRelationServiceImpl knowledgeRoleRelationServiceImpl;

    /**
     * 查询知识库角色
     */
    @Override
    public KnowledgeRoleVo queryById(Long id) {
        KnowledgeRoleVo vo = baseMapper.selectVoById(id);
        fillKnowledgeIds(vo);
        fillKnowledgeRoleGroupName(vo);
        return vo;
    }

    /**
     * 查询知识库角色列表
     */
    @Override
    public TableDataInfo<KnowledgeRoleVo> queryPageList(KnowledgeRoleBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<KnowledgeRole> lqw = buildQueryWrapper(bo);
        Page<KnowledgeRoleVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);

        fillKnowledgeIds(result.getRecords());
        fillKnowledgeRoleGroupName(result.getRecords());
        return TableDataInfo.build(result);
    }

    /**
     * 查询知识库角色列表
     */
    @Override
    public List<KnowledgeRoleVo> queryList(KnowledgeRoleBo bo) {
        LambdaQueryWrapper<KnowledgeRole> lqw = buildQueryWrapper(bo);
        List<KnowledgeRoleVo> knowledgeRoleVos = baseMapper.selectVoList(lqw);
        fillKnowledgeIds(knowledgeRoleVos);
        fillKnowledgeRoleGroupName(knowledgeRoleVos);
        return knowledgeRoleVos;
    }

    private LambdaQueryWrapper<KnowledgeRole> buildQueryWrapper(KnowledgeRoleBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<KnowledgeRole> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getGroupId() != null, KnowledgeRole::getGroupId, bo.getGroupId());
        return lqw;
    }

    /**
     * 新增知识库角色
     */
    @Override
    public Boolean insertByBo(KnowledgeRoleBo bo) {
        KnowledgeRole add = MapstructUtils.convert(bo, KnowledgeRole.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());

            // 生成relation数据
            knowledgeRoleRelationServiceImpl.saveRoleKnowledgeRelations(bo.getId(), bo.getKnowledgeIds());
        }
        return flag;
    }

    /**
     * 修改知识库角色
     */
    @Override
    public Boolean updateByBo(KnowledgeRoleBo bo) {
        KnowledgeRole update = MapstructUtils.convert(bo, KnowledgeRole.class);
        validEntityBeforeSave(update);
        int count = baseMapper.updateById(update);
        if (count > 0) {
            // 生成relation数据
            knowledgeRoleRelationServiceImpl.saveRoleKnowledgeRelations(bo.getId(), bo.getKnowledgeIds());
        }

        return count > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(KnowledgeRole entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除知识库角色
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        int count = baseMapper.deleteBatchIds(ids);
        if (count > 0) {
            knowledgeRoleRelationMapper.deleteByRoleIds(ids.stream().toList());
        }

        return count > 0;
    }

    /**
     * 填充 VO 的 knowledgeIds 字段（从中间表查询）
     */
    private void fillKnowledgeIds(KnowledgeRoleVo vo) {
        if (vo == null) return;
        List<Long> knowledgeIds = knowledgeRoleRelationMapper.selectKnowledgeIdsByRoleId(vo.getId());
        vo.setKnowledgeIds(knowledgeIds);
    }

    private void fillKnowledgeIds(List<KnowledgeRoleVo> list) {
        if (CollectionUtils.isEmpty(list)) return;

        // 1. 提取所有 roleId
        Set<Long> roleIds = list.stream()
                .map(KnowledgeRoleVo::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (roleIds.isEmpty()) return;

        // 2. 一次性查询所有 roleId 对应的 knowledgeId 列表
        List<KnowledgeRoleRelation> relations = knowledgeRoleRelationMapper.selectList(
                new LambdaQueryWrapper<KnowledgeRoleRelation>()
                        .in(KnowledgeRoleRelation::getKnowledgeRoleId, roleIds)
        );

        // 3. 转为 Map<roleId, List<knowledgeId>>
        Map<Long, List<Long>> roleIdToKnowledgeIds = relations.stream()
                .collect(Collectors.groupingBy(
                        KnowledgeRoleRelation::getKnowledgeRoleId,
                        Collectors.mapping(KnowledgeRoleRelation::getKnowledgeId, Collectors.toList())
                ));

        // 4. 回填到 VO 中
        for (KnowledgeRoleVo vo : list) {
            vo.setKnowledgeIds(roleIdToKnowledgeIds.getOrDefault(vo.getId(), Collections.emptyList()));
        }
    }

    /**
     * 填充 VO 的 knowledgeRoleGroupName 字段
     */
    private void fillKnowledgeRoleGroupName(KnowledgeRoleVo vo) {
        if (vo == null || vo.getGroupId() == null) return;

        KnowledgeRoleGroup group = knowledgeRoleGroupMapper.selectById(vo.getGroupId());
        if (group != null) {
            vo.setGroupName(group.getName());
        }
    }

    private void fillKnowledgeRoleGroupName(List<KnowledgeRoleVo> list) {
        if (CollectionUtils.isEmpty(list)) return;

        // 1. 提取所有 groupId（去重）
        Set<Long> groupIds = list.stream()
                .map(KnowledgeRoleVo::getGroupId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (groupIds.isEmpty()) return;

        // 2. 一次性查出所有角色组信息
        List<KnowledgeRoleGroup> groupList = knowledgeRoleGroupMapper.selectBatchIds(groupIds);

        // 3. 转为 Map<id, name>
        Map<Long, String> groupNameMap = groupList.stream()
                .collect(Collectors.toMap(KnowledgeRoleGroup::getId, KnowledgeRoleGroup::getName));

        // 4. 回填到每个 VO
        for (KnowledgeRoleVo vo : list) {
            vo.setGroupName(groupNameMap.get(vo.getGroupId()));
        }
    }
}
