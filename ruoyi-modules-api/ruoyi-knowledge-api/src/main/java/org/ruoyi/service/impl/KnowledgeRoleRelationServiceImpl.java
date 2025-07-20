package org.ruoyi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.domain.KnowledgeRoleRelation;
import org.ruoyi.domain.bo.KnowledgeRoleRelationBo;
import org.ruoyi.domain.vo.KnowledgeRoleRelationVo;
import org.ruoyi.mapper.KnowledgeRoleRelationMapper;
import org.ruoyi.service.IKnowledgeRoleRelationService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 知识库角色与知识库关联Service业务层处理
 *
 * @author ageerle
 * @date 2025-07-19
 */
@RequiredArgsConstructor
@Service
public class KnowledgeRoleRelationServiceImpl implements IKnowledgeRoleRelationService {

    private final KnowledgeRoleRelationMapper baseMapper;
    private final KnowledgeRoleRelationMapper knowledgeRoleRelationMapper;

    /**
     * 查询知识库角色与知识库关联
     */
    @Override
    public KnowledgeRoleRelationVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询知识库角色与知识库关联列表
     */
    @Override
    public TableDataInfo<KnowledgeRoleRelationVo> queryPageList(KnowledgeRoleRelationBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<KnowledgeRoleRelation> lqw = buildQueryWrapper(bo);
        Page<KnowledgeRoleRelationVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询知识库角色与知识库关联列表
     */
    @Override
    public List<KnowledgeRoleRelationVo> queryList(KnowledgeRoleRelationBo bo) {
        LambdaQueryWrapper<KnowledgeRoleRelation> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<KnowledgeRoleRelation> buildQueryWrapper(KnowledgeRoleRelationBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<KnowledgeRoleRelation> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getKnowledgeRoleId() != null, KnowledgeRoleRelation::getKnowledgeRoleId, bo.getKnowledgeRoleId());
        lqw.eq(bo.getKnowledgeId() != null, KnowledgeRoleRelation::getKnowledgeId, bo.getKnowledgeId());
        return lqw;
    }

    /**
     * 新增知识库角色与知识库关联
     */
    @Override
    public Boolean insertByBo(KnowledgeRoleRelationBo bo) {
        KnowledgeRoleRelation add = MapstructUtils.convert(bo, KnowledgeRoleRelation.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改知识库角色与知识库关联
     */
    @Override
    public Boolean updateByBo(KnowledgeRoleRelationBo bo) {
        KnowledgeRoleRelation update = MapstructUtils.convert(bo, KnowledgeRoleRelation.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(KnowledgeRoleRelation entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除知识库角色与知识库关联
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    public void saveRoleKnowledgeRelations(Long knowledgeRoleId, List<Long> knowledgeIds) {
        if (knowledgeRoleId == null) {
            throw new IllegalArgumentException("knowledgeRoleId不能为空");
        }

        // 1. 删除旧的关联记录
        knowledgeRoleRelationMapper.deleteByRoleId(knowledgeRoleId);

        // 2. 插入新的关联记录
        if (CollectionUtils.isNotEmpty(knowledgeIds)) {
            List<KnowledgeRoleRelation> insertList = new ArrayList<>();
            knowledgeIds.forEach(knowledgeId -> {
                KnowledgeRoleRelation knowledgeRoleRelation = new KnowledgeRoleRelation();
                knowledgeRoleRelation.setKnowledgeId(knowledgeId);
                knowledgeRoleRelation.setKnowledgeRoleId(knowledgeRoleId);
                insertList.add(knowledgeRoleRelation);
            });
            knowledgeRoleRelationMapper.insertBatch(insertList);
        }
    }
}
