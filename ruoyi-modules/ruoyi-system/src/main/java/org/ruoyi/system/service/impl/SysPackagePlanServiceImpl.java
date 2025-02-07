package org.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.system.domain.SysPackagePlan;
import org.ruoyi.system.domain.bo.SysPackagePlanBo;
import org.ruoyi.system.domain.vo.SysPackagePlanVo;
import org.ruoyi.system.mapper.SysPackagePlanMapper;
import org.ruoyi.system.service.ISysPackagePlanService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 套餐管理Service业务层处理
 *
 * @author Lion Li
 * @date 2024-05-05
 */
@RequiredArgsConstructor
@Service
public class SysPackagePlanServiceImpl implements ISysPackagePlanService {

    private final SysPackagePlanMapper baseMapper;

    /**
     * 查询套餐管理
     */
    @Override
    public SysPackagePlanVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询套餐管理列表
     */
    @Override
    public TableDataInfo<SysPackagePlanVo> queryPageList(SysPackagePlanBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysPackagePlan> lqw = buildQueryWrapper(bo);
        Page<SysPackagePlanVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询套餐管理列表
     */
    @Override
    public List<SysPackagePlanVo> queryList(SysPackagePlanBo bo) {
        LambdaQueryWrapper<SysPackagePlan> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<SysPackagePlan> buildQueryWrapper(SysPackagePlanBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<SysPackagePlan> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getName()), SysPackagePlan::getName, bo.getName());
        lqw.eq(bo.getPrice() != null, SysPackagePlan::getPrice, bo.getPrice());
        lqw.eq(bo.getDuration() != null, SysPackagePlan::getDuration, bo.getDuration());
        lqw.eq(StringUtils.isNotBlank(bo.getPlanDetail()), SysPackagePlan::getPlanDetail, bo.getPlanDetail());
        return lqw;
    }

    /**
     * 新增套餐管理
     */
    @Override
    public Boolean insertByBo(SysPackagePlanBo bo) {
        SysPackagePlan add = MapstructUtils.convert(bo, SysPackagePlan.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改套餐管理
     */
    @Override
    public Boolean updateByBo(SysPackagePlanBo bo) {
        SysPackagePlan update = MapstructUtils.convert(bo, SysPackagePlan.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SysPackagePlan entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除套餐管理
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
