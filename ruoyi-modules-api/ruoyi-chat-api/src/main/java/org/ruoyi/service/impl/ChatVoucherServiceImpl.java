package org.ruoyi.service.impl;

import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.ruoyi.domain.ChatVoucher;
import org.ruoyi.domain.bo.ChatVoucherBo;
import org.ruoyi.domain.vo.ChatVoucherVo;
import org.ruoyi.mapper.ChatVoucherMapper;
import org.ruoyi.service.IChatVoucherService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 用户兑换记录Service业务层处理
 *
 * @author ageerle
 * @date 2025-04-08
 */
@RequiredArgsConstructor
@Service
public class ChatVoucherServiceImpl implements IChatVoucherService {

    private final ChatVoucherMapper baseMapper;

    /**
     * 查询用户兑换记录
     */
    @Override
    public ChatVoucherVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询用户兑换记录列表
     */
    @Override
    public TableDataInfo<ChatVoucherVo> queryPageList(ChatVoucherBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ChatVoucher> lqw = buildQueryWrapper(bo);
        Page<ChatVoucherVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询用户兑换记录列表
     */
    @Override
    public List<ChatVoucherVo> queryList(ChatVoucherBo bo) {
        LambdaQueryWrapper<ChatVoucher> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<ChatVoucher> buildQueryWrapper(ChatVoucherBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<ChatVoucher> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getCode()), ChatVoucher::getCode, bo.getCode());
        lqw.eq(bo.getAmount() != null, ChatVoucher::getAmount, bo.getAmount());
        lqw.eq(bo.getUserId() != null, ChatVoucher::getUserId, bo.getUserId());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), ChatVoucher::getStatus, bo.getStatus());
        lqw.eq(bo.getBalanceBefore() != null, ChatVoucher::getBalanceBefore, bo.getBalanceBefore());
        lqw.eq(bo.getBalanceAfter() != null, ChatVoucher::getBalanceAfter, bo.getBalanceAfter());
        return lqw;
    }

    /**
     * 新增用户兑换记录
     */
    @Override
    public Boolean insertByBo(ChatVoucherBo bo) {
        ChatVoucher add = MapstructUtils.convert(bo, ChatVoucher.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改用户兑换记录
     */
    @Override
    public Boolean updateByBo(ChatVoucherBo bo) {
        ChatVoucher update = MapstructUtils.convert(bo, ChatVoucher.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(ChatVoucher entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除用户兑换记录
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
