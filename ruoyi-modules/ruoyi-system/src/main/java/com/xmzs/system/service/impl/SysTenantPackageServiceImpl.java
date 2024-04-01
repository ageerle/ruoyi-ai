package com.xmzs.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xmzs.common.core.constant.TenantConstants;
import com.xmzs.common.core.exception.ServiceException;
import com.xmzs.common.core.utils.MapstructUtils;
import com.xmzs.common.core.utils.StringUtils;
import com.xmzs.common.mybatis.core.page.PageQuery;
import com.xmzs.common.mybatis.core.page.TableDataInfo;
import com.xmzs.system.domain.SysTenant;
import com.xmzs.system.domain.SysTenantPackage;
import com.xmzs.system.domain.bo.SysTenantPackageBo;
import com.xmzs.system.domain.vo.SysTenantPackageVo;
import com.xmzs.system.mapper.SysTenantMapper;
import com.xmzs.system.mapper.SysTenantPackageMapper;
import com.xmzs.system.service.ISysTenantPackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 租户套餐Service业务层处理
 *
 * @author Michelle.Chung
 */
@RequiredArgsConstructor
@Service
public class SysTenantPackageServiceImpl implements ISysTenantPackageService {

    private final SysTenantPackageMapper baseMapper;
    private final SysTenantMapper tenantMapper;

    /**
     * 查询租户套餐
     */
    @Override
    public SysTenantPackageVo queryById(Long packageId){
        return baseMapper.selectVoById(packageId);
    }

    /**
     * 查询租户套餐列表
     */
    @Override
    public TableDataInfo<SysTenantPackageVo> queryPageList(SysTenantPackageBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysTenantPackage> lqw = buildQueryWrapper(bo);
        Page<SysTenantPackageVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<SysTenantPackageVo> selectList() {
        return baseMapper.selectVoList(new LambdaQueryWrapper<SysTenantPackage>()
                .eq(SysTenantPackage::getStatus, TenantConstants.NORMAL));
    }

    /**
     * 查询租户套餐列表
     */
    @Override
    public List<SysTenantPackageVo> queryList(SysTenantPackageBo bo) {
        LambdaQueryWrapper<SysTenantPackage> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<SysTenantPackage> buildQueryWrapper(SysTenantPackageBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<SysTenantPackage> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getPackageName()), SysTenantPackage::getPackageName, bo.getPackageName());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), SysTenantPackage::getStatus, bo.getStatus());
        return lqw;
    }

    /**
     * 新增租户套餐
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(SysTenantPackageBo bo) {
        SysTenantPackage add = MapstructUtils.convert(bo, SysTenantPackage.class);
        // 保存菜单id
        List<Long> menuIds = Arrays.asList(bo.getMenuIds());
        if (CollUtil.isNotEmpty(menuIds)) {
            add.setMenuIds(StringUtils.join(menuIds, ", "));
        } else {
            add.setMenuIds("");
        }
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setPackageId(add.getPackageId());
        }
        return flag;
    }

    /**
     * 修改租户套餐
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(SysTenantPackageBo bo) {
        SysTenantPackage update = MapstructUtils.convert(bo, SysTenantPackage.class);
        // 保存菜单id
        List<Long> menuIds = Arrays.asList(bo.getMenuIds());
        if (CollUtil.isNotEmpty(menuIds)) {
            update.setMenuIds(StringUtils.join(menuIds, ", "));
        } else {
            update.setMenuIds("");
        }
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 修改套餐状态
     *
     * @param bo 套餐信息
     * @return 结果
     */
    @Override
    public int updatePackageStatus(SysTenantPackageBo bo) {
        SysTenantPackage tenantPackage = MapstructUtils.convert(bo, SysTenantPackage.class);
        return baseMapper.updateById(tenantPackage);
    }

    /**
     * 批量删除租户套餐
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            boolean exists = tenantMapper.exists(new LambdaQueryWrapper<SysTenant>().in(SysTenant::getPackageId, ids));
            if (exists) {
                throw new ServiceException("租户套餐已被使用");
            }
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
