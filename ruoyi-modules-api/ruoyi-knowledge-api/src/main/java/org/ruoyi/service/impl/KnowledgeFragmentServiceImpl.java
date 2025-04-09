package org.ruoyi.service.impl;

import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.ruoyi.domain.vo.KnowledgeFragmentVo;
import org.springframework.stereotype.Service;
import org.ruoyi.domain.bo.KnowledgeFragmentBo;
import org.ruoyi.domain.KnowledgeFragment;
import org.ruoyi.mapper.KnowledgeFragmentMapper;
import org.ruoyi.service.IKnowledgeFragmentService;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 知识片段Service业务层处理
 *
 * @author ageerle
 * @date 2025-04-08
 */
@RequiredArgsConstructor
@Service
public class KnowledgeFragmentServiceImpl implements IKnowledgeFragmentService {

    private final KnowledgeFragmentMapper baseMapper;

    /**
     * 查询知识片段
     */
    @Override
    public KnowledgeFragmentVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询知识片段列表
     */
    @Override
    public TableDataInfo<KnowledgeFragmentVo> queryPageList(KnowledgeFragmentBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<KnowledgeFragment> lqw = buildQueryWrapper(bo);
        Page<KnowledgeFragmentVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询知识片段列表
     */
    @Override
    public List<KnowledgeFragmentVo> queryList(KnowledgeFragmentBo bo) {
        LambdaQueryWrapper<KnowledgeFragment> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<KnowledgeFragment> buildQueryWrapper(KnowledgeFragmentBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<KnowledgeFragment> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getKid()), KnowledgeFragment::getKid, bo.getKid());
        lqw.eq(StringUtils.isNotBlank(bo.getDocId()), KnowledgeFragment::getDocId, bo.getDocId());
        lqw.eq(StringUtils.isNotBlank(bo.getFid()), KnowledgeFragment::getFid, bo.getFid());
        lqw.eq(bo.getIdx() != null, KnowledgeFragment::getIdx, bo.getIdx());
        lqw.eq(StringUtils.isNotBlank(bo.getContent()), KnowledgeFragment::getContent, bo.getContent());
        return lqw;
    }

    /**
     * 新增知识片段
     */
    @Override
    public Boolean insertByBo(KnowledgeFragmentBo bo) {
        KnowledgeFragment add = MapstructUtils.convert(bo, KnowledgeFragment.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改知识片段
     */
    @Override
    public Boolean updateByBo(KnowledgeFragmentBo bo) {
        KnowledgeFragment update = MapstructUtils.convert(bo, KnowledgeFragment.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(KnowledgeFragment entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除知识片段
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
