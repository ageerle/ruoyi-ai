package org.ruoyi.system.service.impl;

import org.ruoyi.common.core.utils.MapstructUtils;
    import org.ruoyi.core.page.TableDataInfo;
    import org.ruoyi.core.page.PageQuery;
    import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.ruoyi.system.domain.bo.SysDemoBo;
import org.ruoyi.system.domain.vo.SysDemoVo;
import org.ruoyi.system.domain.SysDemo;
import org.ruoyi.system.mapper.SysDemoMapper;
import org.ruoyi.system.service.SysDemoService;
import org.ruoyi.common.core.utils.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * dome管理Service业务层处理
 *
 * @author ageerle
 * @date Sat Aug 09 21:38:09 CST 2025
 */
@RequiredArgsConstructor
@Service
public class SysDemoServiceImpl implements SysDemoService {

    private final SysDemoMapper baseMapper;

    /**
     * 查询dome管理
     */
    @Override
    public SysDemoVo queryById(Integer id) {
        return baseMapper.selectVoById(id);
    }

        /**
         * 查询dome管理列表
         */
        @Override
        public TableDataInfo<SysDemoVo> queryPageList(SysDemoBo bo, PageQuery pageQuery) {
            LambdaQueryWrapper<SysDemo> lqw = buildQueryWrapper(bo);
            Page<SysDemoVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
            return TableDataInfo.build(result);
        }

    /**
     * 查询dome管理列表
     */
    @Override
    public List<SysDemoVo> queryList(SysDemoBo bo) {
        LambdaQueryWrapper<SysDemo> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<SysDemo> buildQueryWrapper(SysDemoBo bo) {
        LambdaQueryWrapper<SysDemo> lqw = Wrappers.lambdaQuery();
                    lqw.eq(StringUtils.isNotBlank(bo.getSysCode()), SysDemo::getSysCode, bo.getSysCode());
                    lqw.eq(StringUtils.isNotBlank(bo.getSysName()), SysDemo::getSysName, bo.getSysName());
                    lqw.eq(bo.getSysStatus() != null, SysDemo::getSysStatus, bo.getSysStatus());
                    lqw.eq(bo.getCreateDept() != null, SysDemo::getCreateDept, bo.getCreateDept());
                    lqw.eq(bo.getCreateBy() != null, SysDemo::getCreateBy, bo.getCreateBy());
                    lqw.eq(bo.getCreateTime() != null, SysDemo::getCreateTime, bo.getCreateTime());
                    lqw.eq(bo.getUpdateBy() != null, SysDemo::getUpdateBy, bo.getUpdateBy());
                    lqw.eq(bo.getUpdateTime() != null, SysDemo::getUpdateTime, bo.getUpdateTime());
                    lqw.eq(StringUtils.isNotBlank(bo.getRemark()), SysDemo::getRemark, bo.getRemark());
        return lqw;
    }

    /**
     * 新增dome管理
     */
    @Override
    public Boolean insertByBo(SysDemoBo bo) {
        SysDemo add = MapstructUtils.convert(bo, SysDemo. class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改dome管理
     */
    @Override
    public Boolean updateByBo(SysDemoBo bo) {
        SysDemo update = MapstructUtils.convert(bo, SysDemo. class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SysDemo entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除dome管理
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Integer> ids, Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
