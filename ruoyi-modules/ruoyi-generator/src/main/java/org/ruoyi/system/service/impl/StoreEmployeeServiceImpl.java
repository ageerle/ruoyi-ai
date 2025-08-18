package org.ruoyi.system.service.impl;

import org.ruoyi.common.core.utils.MapstructUtils;
    import org.ruoyi.core.page.TableDataInfo;
    import org.ruoyi.core.page.PageQuery;
    import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.ruoyi.system.domain.bo.StoreEmployeeBo;
import org.ruoyi.system.domain.vo.StoreEmployeeVo;
import org.ruoyi.system.domain.StoreEmployee;
import org.ruoyi.system.mapper.StoreEmployeeMapper;
import org.ruoyi.system.service.StoreEmployeeService;
import org.ruoyi.common.core.utils.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 员工分配Service业务层处理
 *
 * @author ageerle
 * @date Mon Aug 18 21:33:27 CST 2025
 */
@RequiredArgsConstructor
@Service
public class StoreEmployeeServiceImpl implements StoreEmployeeService {

    private final StoreEmployeeMapper baseMapper;

    /**
     * 查询员工分配
     */
    @Override
    public StoreEmployeeVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

        /**
         * 查询员工分配列表
         */
        @Override
        public TableDataInfo<StoreEmployeeVo> queryPageList(StoreEmployeeBo bo, PageQuery pageQuery) {
            LambdaQueryWrapper<StoreEmployee> lqw = buildQueryWrapper(bo);
            Page<StoreEmployeeVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
            return TableDataInfo.build(result);
        }

    /**
     * 查询员工分配列表
     */
    @Override
    public List<StoreEmployeeVo> queryList(StoreEmployeeBo bo) {
        LambdaQueryWrapper<StoreEmployee> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<StoreEmployee> buildQueryWrapper(StoreEmployeeBo bo) {
        LambdaQueryWrapper<StoreEmployee> lqw = Wrappers.lambdaQuery();
                    lqw.eq(bo.getStoreId() != null, StoreEmployee::getStoreId, bo.getStoreId());
                    lqw.eq(bo.getUserId() != null, StoreEmployee::getUserId, bo.getUserId());
        return lqw;
    }

    /**
     * 新增员工分配
     */
    @Override
    public Boolean insertByBo(StoreEmployeeBo bo) {
        StoreEmployee add = MapstructUtils.convert(bo, StoreEmployee. class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改员工分配
     */
    @Override
    public Boolean updateByBo(StoreEmployeeBo bo) {
        StoreEmployee update = MapstructUtils.convert(bo, StoreEmployee. class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(StoreEmployee entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除员工分配
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
