package org.ruoyi.system.service.impl;

import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.ruoyi.system.domain.bo.SysUserModelBo;
import org.ruoyi.system.domain.vo.SysUserModelVo;
import org.ruoyi.system.domain.SysUserModel;
import org.ruoyi.system.mapper.SysUserModelMapper;
import org.ruoyi.system.service.ISysUserModelService;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 【请填写功能名称】Service业务层处理
 *
 * @author Lion Li
 * @date 2024-08-03
 */
@RequiredArgsConstructor
@Service
public class SysUserModelServiceImpl implements ISysUserModelService {

    private final SysUserModelMapper baseMapper;

    /**
     * 查询【请填写功能名称】
     */
    @Override
    public SysUserModelVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询【请填写功能名称】列表
     */
    @Override
    public TableDataInfo<SysUserModelVo> queryPageList(SysUserModelBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysUserModel> lqw = buildQueryWrapper(bo);
        Page<SysUserModelVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询【请填写功能名称】列表
     */
    @Override
    public List<SysUserModelVo> queryList(SysUserModelBo bo) {
        LambdaQueryWrapper<SysUserModel> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<SysUserModel> buildQueryWrapper(SysUserModelBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<SysUserModel> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getMid() != null, SysUserModel::getMid, bo.getMid());
        lqw.eq(bo.getGid() != null, SysUserModel::getGid, bo.getGid());
        return lqw;
    }

    /**
     * 新增【请填写功能名称】
     */
    @Override
    public Boolean insertByBo(SysUserModelBo bo) {
        SysUserModel add = MapstructUtils.convert(bo, SysUserModel.class);
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
    public Boolean updateByBo(SysUserModelBo bo) {
        SysUserModel update = MapstructUtils.convert(bo, SysUserModel.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SysUserModel entity){
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
