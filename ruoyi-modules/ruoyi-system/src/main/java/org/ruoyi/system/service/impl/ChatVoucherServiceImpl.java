package org.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.system.domain.SysUser;
import org.ruoyi.system.domain.bo.SysUserBo;
import org.ruoyi.system.domain.vo.SysUserVo;
import org.ruoyi.system.mapper.SysUserMapper;
import org.ruoyi.system.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.ruoyi.system.domain.bo.ChatVoucherBo;
import org.ruoyi.system.domain.vo.ChatVoucherVo;
import org.ruoyi.system.domain.ChatVoucher;
import org.ruoyi.system.mapper.ChatVoucherMapper;
import org.ruoyi.system.service.IChatVoucherService;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * 用户兑换记录Service业务层处理
 *
 * @author Lion Li
 * @date 2024-05-03
 */
@RequiredArgsConstructor
@Service
public class ChatVoucherServiceImpl implements IChatVoucherService {

    private final ChatVoucherMapper baseMapper;

    private final ISysUserService sysUserService;

    private final SysUserMapper sysUserMapper;

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
        if(CollectionUtil.isEmpty(result.getRecords())){
            return TableDataInfo.build(result);
        }
        // 获取所有userId
        List<Long> userIds = result.getRecords().stream()
            .map(ChatVoucherVo::getUserId)
            .collect(Collectors.toList());
        // 一次性查询所有userName
        Map<Long, String> userIdToUserNameMap = getUserNamesByUserIds(userIds);
        // 设置userName
        result.getRecords().forEach(chatVoucherVo -> {
            chatVoucherVo.setUserName(userIdToUserNameMap.get(chatVoucherVo.getUserId()));
        });
        return TableDataInfo.build(result);
    }

    private Map<Long, String> getUserNamesByUserIds(List<Long> userIds) {
        // 实现批量查询userName的逻辑，例如通过sysUserMapper查询sys_user表
        List<SysUser> sysUsers = sysUserMapper.selectBatchIds(userIds);
        return sysUsers.stream()
            .collect(Collectors.toMap(SysUser::getUserId, SysUser::getUserName));
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
        lqw.eq(bo.getUserId() != null, ChatVoucher::getUserId, bo.getUserId());
        lqw.eq(StringUtils.isNotBlank(bo.getCode()), ChatVoucher::getCode, bo.getCode());
        lqw.eq(bo.getAmount() != null, ChatVoucher::getAmount, bo.getAmount());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), ChatVoucher::getStatus, bo.getStatus());
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

    /**
     * 兑换卡密
     *
     * @param bo 卡密信息
     */
    @Override
    public Boolean redeem(ChatVoucherBo bo) {
        LambdaQueryWrapper<ChatVoucher> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getCode()), ChatVoucher::getCode, bo.getCode());
        ChatVoucherVo chatVoucherVo = baseMapper.selectVoOne(lqw);
        if(chatVoucherVo != null){
            // 如果卡密已经兑换
            if("2".equals(chatVoucherVo.getStatus())){
                return false;
            }
            SysUserVo sysUserVo = sysUserService.selectUserById(LoginHelper.getLoginUser().getUserId());
            // 更新卡密记录
            chatVoucherVo.setUserId(LoginHelper.getLoginUser().getUserId());
            chatVoucherVo.setStatus("2");
            chatVoucherVo.setBalanceBefore(sysUserVo.getUserBalance());
            chatVoucherVo.setBalanceAfter(sysUserVo.getUserBalance()+chatVoucherVo.getAmount());
            // 添加用户余额
            sysUserVo.setUserBalance(sysUserVo.getUserBalance() + chatVoucherVo.getAmount());
            SysUserBo user = new SysUserBo();
            BeanUtil.copyProperties(sysUserVo,user);
            sysUserService.updateUser(user);

            ChatVoucher update = MapstructUtils.convert(chatVoucherVo, ChatVoucher.class);
            baseMapper.updateById(update);
        }else {
            return false;
        }
        return true;
    }
}
