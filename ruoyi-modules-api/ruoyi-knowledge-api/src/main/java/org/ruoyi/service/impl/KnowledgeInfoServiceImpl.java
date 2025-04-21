package org.ruoyi.service.impl;

import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.ruoyi.domain.vo.KnowledgeInfoVo;
import org.springframework.stereotype.Service;
import org.ruoyi.domain.bo.KnowledgeInfoBo;
import org.ruoyi.domain.KnowledgeInfo;
import org.ruoyi.mapper.KnowledgeInfoMapper;
import org.ruoyi.service.IKnowledgeInfoService;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 知识库Service业务层处理
 *
 * @author ageerle
 * @date 2025-04-08
 */
@RequiredArgsConstructor
@Service
public class KnowledgeInfoServiceImpl implements IKnowledgeInfoService {

    private final KnowledgeInfoMapper baseMapper;

    /**
     * 查询知识库
     */
    @Override
    public KnowledgeInfoVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询知识库列表
     */
    @Override
    public TableDataInfo<KnowledgeInfoVo> queryPageList(KnowledgeInfoBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<KnowledgeInfo> lqw = buildQueryWrapper(bo);
        Page<KnowledgeInfoVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询知识库列表
     */
    @Override
    public List<KnowledgeInfoVo> queryList(KnowledgeInfoBo bo) {
        LambdaQueryWrapper<KnowledgeInfo> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<KnowledgeInfo> buildQueryWrapper(KnowledgeInfoBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<KnowledgeInfo> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getKid()), KnowledgeInfo::getKid, bo.getKid());
        lqw.eq(bo.getUid() != null, KnowledgeInfo::getUid, bo.getUid());
        lqw.like(StringUtils.isNotBlank(bo.getKname()), KnowledgeInfo::getKname, bo.getKname());
        lqw.eq(bo.getShare() != null, KnowledgeInfo::getShare, bo.getShare());
        lqw.eq(StringUtils.isNotBlank(bo.getDescription()), KnowledgeInfo::getDescription, bo.getDescription());
        lqw.eq(StringUtils.isNotBlank(bo.getKnowledgeSeparator()), KnowledgeInfo::getKnowledgeSeparator, bo.getKnowledgeSeparator());
        lqw.eq(StringUtils.isNotBlank(bo.getQuestionSeparator()), KnowledgeInfo::getQuestionSeparator, bo.getQuestionSeparator());
        lqw.eq(bo.getOverlapChar() != null, KnowledgeInfo::getOverlapChar, bo.getOverlapChar());
        lqw.eq(bo.getRetrieveLimit() != null, KnowledgeInfo::getRetrieveLimit, bo.getRetrieveLimit());
        lqw.eq(bo.getTextBlockSize() != null, KnowledgeInfo::getTextBlockSize, bo.getTextBlockSize());
        lqw.eq(StringUtils.isNotBlank(bo.getVector()), KnowledgeInfo::getVector, bo.getVector());
        lqw.eq(StringUtils.isNotBlank(bo.getVectorModel()), KnowledgeInfo::getVectorModel, bo.getVectorModel());
        return lqw;
    }

    /**
     * 新增知识库
     */
    @Override
    public Boolean insertByBo(KnowledgeInfoBo bo) {
        KnowledgeInfo add = MapstructUtils.convert(bo, KnowledgeInfo.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改知识库
     */
    @Override
    public Boolean updateByBo(KnowledgeInfoBo bo) {
        KnowledgeInfo update = MapstructUtils.convert(bo, KnowledgeInfo.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(KnowledgeInfo entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除知识库
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
