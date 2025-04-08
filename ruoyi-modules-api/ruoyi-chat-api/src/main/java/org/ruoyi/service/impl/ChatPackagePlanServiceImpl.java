package org.ruoyi.service.impl;

import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.ruoyi.domain.ChatPackagePlan;
import org.ruoyi.domain.bo.ChatPackagePlanBo;
import org.ruoyi.domain.vo.ChatPackagePlanVo;
import org.ruoyi.mapper.ChatPackagePlanMapper;
import org.ruoyi.service.IChatPackagePlanService;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 套餐管理Service业务层处理
 *
 * @author ageerle
 * @date 2025-04-08
 */
@RequiredArgsConstructor
@Service
public class ChatPackagePlanServiceImpl implements IChatPackagePlanService {

    private final ChatPackagePlanMapper baseMapper;

    /**
     * 查询套餐管理
     */
    @Override
    public ChatPackagePlanVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询套餐管理列表
     */
    @Override
    public TableDataInfo<ChatPackagePlanVo> queryPageList(ChatPackagePlanBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ChatPackagePlan> lqw = buildQueryWrapper(bo);
        Page<ChatPackagePlanVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询套餐管理列表
     */
    @Override
    public List<ChatPackagePlanVo> queryList(ChatPackagePlanBo bo) {
        LambdaQueryWrapper<ChatPackagePlan> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<ChatPackagePlan> buildQueryWrapper(ChatPackagePlanBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<ChatPackagePlan> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getName()), ChatPackagePlan::getName, bo.getName());
        lqw.eq(bo.getPrice() != null, ChatPackagePlan::getPrice, bo.getPrice());
        lqw.eq(bo.getDuration() != null, ChatPackagePlan::getDuration, bo.getDuration());
        lqw.eq(StringUtils.isNotBlank(bo.getPlanDetail()), ChatPackagePlan::getPlanDetail, bo.getPlanDetail());
        return lqw;
    }

    /**
     * 新增套餐管理
     */
    @Override
    public Boolean insertByBo(ChatPackagePlanBo bo) {
        ChatPackagePlan add = MapstructUtils.convert(bo, ChatPackagePlan.class);
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
    public Boolean updateByBo(ChatPackagePlanBo bo) {
        ChatPackagePlan update = MapstructUtils.convert(bo, ChatPackagePlan.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(ChatPackagePlan entity){
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
