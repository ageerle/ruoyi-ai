package org.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.system.domain.ChatMessage;
import org.ruoyi.system.domain.SysUser;
import org.ruoyi.system.domain.bo.ChatMessageBo;
import org.ruoyi.system.domain.vo.ChatMessageVo;
import org.ruoyi.system.domain.vo.SysUserVo;
import org.ruoyi.system.mapper.ChatMessageMapper;
import org.ruoyi.system.mapper.SysUserMapper;
import org.ruoyi.system.service.IChatMessageService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聊天消息Service业务层处理
 *
 * @author Lion Li
 * @date 2023-11-26
 */
@RequiredArgsConstructor
@Service
public class ChatMessageServiceImpl implements IChatMessageService {

    private final ChatMessageMapper baseMapper;

    private final SysUserMapper sysUserMapper;
    /**
     * 查询聊天消息
     */
    @Override
    public ChatMessageVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询聊天消息列表
     */
    @Override
    public TableDataInfo<ChatMessageVo> queryPageList(ChatMessageBo bo, PageQuery pageQuery) {
        // 根据用户名称查询用户
        if(StringUtils.isNotEmpty(bo.getUserName())){
            SysUserVo sysUserVo = sysUserMapper.selectUserByUserName(bo.getUserName());
            bo.setUserId(sysUserVo.getUserId());
        }

        LambdaQueryWrapper<ChatMessage> lqw = buildQueryWrapper(bo);
        Page<ChatMessageVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        if(CollectionUtil.isEmpty(result.getRecords())){
            return TableDataInfo.build(result);
        }
        List<Long> userIds = result.getRecords().stream()
            .map(ChatMessageVo::getUserId)
            .collect(Collectors.toList());
        // 一次性查询所有userName
        Map<Long, String> userIdToUserNameMap = getUserNamesByUserIds(userIds);
        // 设置userName
        result.getRecords().forEach(chatMessageVo -> {
            chatMessageVo.setUserName(userIdToUserNameMap.get(chatMessageVo.getUserId()));
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
     * 查询聊天消息列表
     */
    @Override
    public List<ChatMessageVo> queryList(ChatMessageBo bo) {
        LambdaQueryWrapper<ChatMessage> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<ChatMessage> buildQueryWrapper(ChatMessageBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<ChatMessage> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getUserId() != null, ChatMessage::getUserId, bo.getUserId());
        lqw.eq(StringUtils.isNotBlank(bo.getContent()), ChatMessage::getContent, bo.getContent());
        lqw.eq(bo.getDeductCost() != null, ChatMessage::getDeductCost, bo.getDeductCost());
        lqw.eq(bo.getTotalTokens() != null, ChatMessage::getTotalTokens, bo.getTotalTokens());
        lqw.like(StringUtils.isNotBlank(bo.getModelName()), ChatMessage::getModelName, bo.getModelName());
        return lqw;
    }

    /**
     * 新增聊天消息
     */
    @Override
    public Boolean insertByBo(ChatMessageBo bo) {
        ChatMessage add = MapstructUtils.convert(bo, ChatMessage.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改聊天消息
     */
    @Override
    public Boolean updateByBo(ChatMessageBo bo) {
        ChatMessage update = MapstructUtils.convert(bo, ChatMessage.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(ChatMessage entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除聊天消息
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
