package org.ruoyi.service.impl;

import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.ruoyi.domain.vo.KnowledgeAttachVo;
import org.springframework.stereotype.Service;
import org.ruoyi.domain.bo.KnowledgeAttachBo;

import org.ruoyi.domain.KnowledgeAttach;
import org.ruoyi.mapper.KnowledgeAttachMapper;
import org.ruoyi.service.IKnowledgeAttachService;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 知识库附件Service业务层处理
 *
 * @author ageerle
 * @date 2025-04-08
 */
@RequiredArgsConstructor
@Service
public class KnowledgeAttachServiceImpl implements IKnowledgeAttachService {

    private final KnowledgeAttachMapper baseMapper;

    /**
     * 查询知识库附件
     */
    @Override
    public KnowledgeAttachVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询知识库附件列表
     */
    @Override
    public TableDataInfo<KnowledgeAttachVo> queryPageList(KnowledgeAttachBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<KnowledgeAttach> lqw = buildQueryWrapper(bo);
        Page<KnowledgeAttachVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询知识库附件列表
     */
    @Override
    public List<KnowledgeAttachVo> queryList(KnowledgeAttachBo bo) {
        LambdaQueryWrapper<KnowledgeAttach> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<KnowledgeAttach> buildQueryWrapper(KnowledgeAttachBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<KnowledgeAttach> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getKid()), KnowledgeAttach::getKid, bo.getKid());
        lqw.eq(StringUtils.isNotBlank(bo.getDocId()), KnowledgeAttach::getDocId, bo.getDocId());
        lqw.like(StringUtils.isNotBlank(bo.getDocName()), KnowledgeAttach::getDocName, bo.getDocName());
        lqw.eq(StringUtils.isNotBlank(bo.getDocType()), KnowledgeAttach::getDocType, bo.getDocType());
        lqw.eq(StringUtils.isNotBlank(bo.getContent()), KnowledgeAttach::getContent, bo.getContent());
        return lqw;
    }

    /**
     * 新增知识库附件
     */
    @Override
    public Boolean insertByBo(KnowledgeAttachBo bo) {
        KnowledgeAttach add = MapstructUtils.convert(bo, KnowledgeAttach.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改知识库附件
     */
    @Override
    public Boolean updateByBo(KnowledgeAttachBo bo) {
        KnowledgeAttach update = MapstructUtils.convert(bo, KnowledgeAttach.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(KnowledgeAttach entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除知识库附件
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
