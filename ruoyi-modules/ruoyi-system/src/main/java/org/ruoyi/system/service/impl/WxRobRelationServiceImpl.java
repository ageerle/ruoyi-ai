package org.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.system.domain.WxRobRelation;
import org.ruoyi.system.domain.bo.WxRobRelationBo;
import org.ruoyi.system.domain.vo.WxRobRelationVo;
import org.ruoyi.system.mapper.WxRobRelationMapper;
import org.ruoyi.system.service.IWxRobRelationService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 【请填写功能名称】Service业务层处理
 *
 * @author Lion Li
 * @date 2024-05-01
 */
@RequiredArgsConstructor
@Service
public class WxRobRelationServiceImpl implements IWxRobRelationService {

    private final WxRobRelationMapper baseMapper;

    /**
     * 查询【请填写功能名称】
     */
    @Override
    public WxRobRelationVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询【请填写功能名称】列表
     */
    @Override
    public TableDataInfo<WxRobRelationVo> queryPageList(WxRobRelationBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<WxRobRelation> lqw = buildQueryWrapper(bo);
        Page<WxRobRelationVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询【请填写功能名称】列表
     */
    @Override
    public List<WxRobRelationVo> queryList(WxRobRelationBo bo) {
        LambdaQueryWrapper<WxRobRelation> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<WxRobRelation> buildQueryWrapper(WxRobRelationBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<WxRobRelation> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getOutKey()), WxRobRelation::getOutKey, bo.getOutKey());
        lqw.eq(StringUtils.isNotBlank(bo.getUniqueKey()), WxRobRelation::getUniqueKey, bo.getUniqueKey());
        lqw.like(StringUtils.isNotBlank(bo.getNickName()), WxRobRelation::getNickName, bo.getNickName());
        lqw.eq(bo.getToGroup() != null, WxRobRelation::getToGroup, bo.getToGroup());
        lqw.eq(bo.getEnable() != null, WxRobRelation::getEnable, bo.getEnable());
        lqw.eq(StringUtils.isNotBlank(bo.getWhiteList()), WxRobRelation::getWhiteList, bo.getWhiteList());
        return lqw;
    }

    /**
     * 新增【请填写功能名称】
     */
    @Override
    public Boolean insertByBo(WxRobRelationBo bo) {
        WxRobRelation add = MapstructUtils.convert(bo, WxRobRelation.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改【请填写功能名称】
     */
    @Override
    public Boolean updateByBo(WxRobRelationBo bo) {
        WxRobRelation update = MapstructUtils.convert(bo, WxRobRelation.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(WxRobRelation entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除【请填写功能名称】
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
