package org.ruoyi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.domain.PromptTemplate;
import org.ruoyi.domain.bo.PromptTemplateBo;
import org.ruoyi.domain.vo.PromptTemplateVo;
import org.ruoyi.mapper.PromptTemplateMapper;
import org.ruoyi.service.IPromptTemplateService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 提示词模板Service业务层处理
 *
 * @author evo
 * @date 2025-06-12
 */
@Service
@RequiredArgsConstructor
public class PromptTemplateServiceImpl implements IPromptTemplateService {

    private final PromptTemplateMapper baseMapper;

    /**
     * 查询提示词模板
     */
    @Override
    public PromptTemplateVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询提示词模板列表
     */
    @Override
    public TableDataInfo<PromptTemplateVo> queryPageList(PromptTemplateBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PromptTemplate> lqw = buildQueryWrapper(bo);
        Page<PromptTemplateVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询提示词模板列表
     */
    @Override
    public List<PromptTemplateVo> queryList(PromptTemplateBo bo) {
        LambdaQueryWrapper<PromptTemplate> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<PromptTemplate> buildQueryWrapper(PromptTemplateBo bo) {
        LambdaQueryWrapper<PromptTemplate> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTemplateName()),
                PromptTemplate::getTemplateName, bo.getTemplateName());
        lqw.like(StringUtils.isNotBlank(bo.getTemplateContent()),
                PromptTemplate::getTemplateContent, bo.getTemplateContent());
        lqw.eq(StringUtils.isNotBlank(bo.getCategory()),
                PromptTemplate::getCategory, bo.getCategory());
        return lqw;
    }

    /**
     * 新增提示词模板
     */
    @Override
    public Boolean insertByBo(PromptTemplateBo bo) {
        PromptTemplate add = MapstructUtils.convert(bo, PromptTemplate.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改提示词模板
     */
    @Override
    public Boolean updateByBo(PromptTemplateBo bo) {
        PromptTemplate update = MapstructUtils.convert(bo, PromptTemplate.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(PromptTemplate entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除提示词模板
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    public PromptTemplateVo queryByCategory(String category) {
        LambdaQueryWrapper<PromptTemplate> queryWrapper = Wrappers.lambdaQuery(PromptTemplate.class);
        queryWrapper.eq(PromptTemplate::getCategory, category);
        queryWrapper.orderByDesc(PromptTemplate::getUpdateTime);
        queryWrapper.last("limit 1");
        return baseMapper.selectVoOne(queryWrapper);
    }
}