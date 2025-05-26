package org.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.SysNoticeState;
import org.ruoyi.system.domain.bo.SysNoticeStateBo;
import org.ruoyi.system.domain.vo.SysNoticeStateVo;
import org.ruoyi.system.mapper.SysNoticeStateMapper;
import org.ruoyi.system.service.ISysNoticeStateService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 用户阅读状态Service业务层处理
 *
 * @author Lion Li
 * @date 2024-05-11
 */
@RequiredArgsConstructor
@Service
public class SysNoticeStateServiceImpl implements ISysNoticeStateService {

    private final SysNoticeStateMapper baseMapper;

    /**
     * 查询用户阅读状态
     */
    @Override
    public SysNoticeStateVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询用户阅读状态列表
     */
    @Override
    public TableDataInfo<SysNoticeStateVo> queryPageList(SysNoticeStateBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysNoticeState> lqw = buildQueryWrapper(bo);
        Page<SysNoticeStateVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询用户阅读状态列表
     */
    @Override
    public List<SysNoticeStateVo> queryList(SysNoticeStateBo bo) {
        LambdaQueryWrapper<SysNoticeState> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<SysNoticeState> buildQueryWrapper(SysNoticeStateBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<SysNoticeState> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getUserId() != null, SysNoticeState::getUserId, bo.getUserId());
        lqw.eq(bo.getNoticeId() != null, SysNoticeState::getNoticeId, bo.getNoticeId());
        lqw.eq(StringUtils.isNotBlank(bo.getReadStatus()), SysNoticeState::getReadStatus, bo.getReadStatus());
        return lqw;
    }

    /**
     * 新增用户阅读状态
     */
    @Override
    public Boolean insertByBo(SysNoticeStateBo bo) {
        SysNoticeState add = MapstructUtils.convert(bo, SysNoticeState.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改用户阅读状态
     */
    @Override
    public Boolean updateByBo(SysNoticeStateBo bo) {
        LambdaQueryWrapper<SysNoticeState> lqw = buildQueryWrapper(bo);
        SysNoticeState sysNoticeState = baseMapper.selectOne(lqw);
        sysNoticeState.setReadStatus("1");
        return baseMapper.updateById(sysNoticeState) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SysNoticeState entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除用户阅读状态
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
