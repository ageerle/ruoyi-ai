package org.ruoyi.system.service.impl;

import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.ruoyi.system.mapper.SysUserGroupMapper;

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
public class SysUserGroupServiceImpl implements ISysUserGroupService {

    private final SysUserGroupMapper baseMapper;

    /**
     * 查询【请填写功能名称】
     */
    @Override
    public SysUserGroupVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询【请填写功能名称】列表
     */
    @Override
    public TableDataInfo<SysUserGroupVo> queryPageList(SysUserGroupBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysUserGroup> lqw = buildQueryWrapper(bo);
        Page<SysUserGroupVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询【请填写功能名称】列表
     */
    @Override
    public List<SysUserGroupVo> queryList(SysUserGroupBo bo) {
        LambdaQueryWrapper<SysUserGroup> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<SysUserGroup> buildQueryWrapper(SysUserGroupBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<SysUserGroup> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getGroupName()), SysUserGroup::getGroupName, bo.getGroupName());
        lqw.eq(StringUtils.isNotBlank(bo.getUpdateIp()), SysUserGroup::getUpdateIp, bo.getUpdateIp());
        return lqw;
    }

    /**
     * 新增【请填写功能名称】
     */
    @Override
    public Boolean insertByBo(SysUserGroupBo bo) {
        SysUserGroup add = MapstructUtils.convert(bo, SysUserGroup.class);
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
    public Boolean updateByBo(SysUserGroupBo bo) {
        SysUserGroup update = MapstructUtils.convert(bo, SysUserGroup.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SysUserGroup entity){
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
