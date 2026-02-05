package org.ruoyi.service.chat.impl;

import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.domain.dto.ChatMessageDTO;
import org.ruoyi.service.chat.IChatMessageService;
import org.springframework.stereotype.Service;
import org.ruoyi.domain.bo.chat.ChatMessageBo;
import org.ruoyi.domain.vo.chat.ChatMessageVo;
import org.ruoyi.domain.entity.chat.ChatMessage;
import org.ruoyi.mapper.chat.ChatMessageMapper;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 聊天消息Service业务层处理
 *
 * @author ageerle
 * @date 2025-12-14
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ChatMessageServiceImpl implements IChatMessageService {

    private final ChatMessageMapper baseMapper;

    /**
     * 查询聊天消息
     *
     * @param id 主键
     * @return 聊天消息
     */
    @Override
    public ChatMessageVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询聊天消息列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 聊天消息分页列表
     */
    @Override
    public TableDataInfo<ChatMessageVo> queryPageList(ChatMessageBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ChatMessage> lqw = buildQueryWrapper(bo);
        Page<ChatMessageVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的聊天消息列表
     *
     * @param bo 查询条件
     * @return 聊天消息列表
     */
    @Override
    public List<ChatMessageVo> queryList(ChatMessageBo bo) {
        LambdaQueryWrapper<ChatMessage> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<ChatMessage> buildQueryWrapper(ChatMessageBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<ChatMessage> lqw = Wrappers.lambdaQuery();
        lqw.orderByAsc(ChatMessage::getId);
        lqw.eq(bo.getSessionId() != null, ChatMessage::getSessionId, bo.getSessionId());
        lqw.eq(bo.getUserId() != null, ChatMessage::getUserId, bo.getUserId());
        lqw.eq(StringUtils.isNotBlank(bo.getContent()), ChatMessage::getContent, bo.getContent());
        lqw.eq(StringUtils.isNotBlank(bo.getRole()), ChatMessage::getRole, bo.getRole());
        lqw.eq(bo.getDeductCost() != null, ChatMessage::getDeductCost, bo.getDeductCost());
        lqw.eq(bo.getTotalTokens() != null, ChatMessage::getTotalTokens, bo.getTotalTokens());
        lqw.like(StringUtils.isNotBlank(bo.getModelName()), ChatMessage::getModelName, bo.getModelName());
        lqw.eq(StringUtils.isNotBlank(bo.getBillingType()), ChatMessage::getBillingType, bo.getBillingType());
        return lqw;
    }

    /**
     * 新增聊天消息
     *
     * @param bo 聊天消息
     * @return 是否新增成功
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
     *
     * @param bo 聊天消息
     * @return 是否修改成功
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
     * 校验并批量删除聊天消息信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    /**
     * 根据会话ID获取所有消息
     * 用于长期记忆功能
     *
     * @param sessionId 会话ID
     * @return 消息DTO列表
     */
    @Override
    public List<org.ruoyi.domain.dto.ChatMessageDTO> getMessagesBySessionId(Long sessionId) {
        if (sessionId == null) {
            return new java.util.ArrayList<>();
        }

        ChatMessageBo bo = new ChatMessageBo();
        bo.setSessionId(sessionId);
        List<ChatMessageVo> voList = queryList(bo);

        return voList.stream()
                .map(vo -> {
                    ChatMessageDTO dto = new ChatMessageDTO();
                    dto.setRole(vo.getRole());
                    dto.setContent(vo.getContent());
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 根据会话ID删除所有消息
     * 用于清理会话历史
     *
     * @param sessionId 会话ID
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteBySessionId(Long sessionId) {
        if (sessionId == null) {
            return false;
        }

        LambdaQueryWrapper<ChatMessage> lqw = Wrappers.lambdaQuery();
        lqw.eq(ChatMessage::getSessionId, sessionId);
        return baseMapper.delete(lqw) > 0;
    }
}
