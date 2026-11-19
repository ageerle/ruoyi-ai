package org.ruoyi.chat.service.chat.impl;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import org.ruoyi.common.chat.domain.bo.chat.ChatMessageBo;
import org.ruoyi.common.chat.domain.vo.chat.ChatMessageVo;
import org.ruoyi.common.chat.entity.chat.ChatMessage;
import org.ruoyi.common.chat.enums.RoleType;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.chat.service.chat.IChatMessageService;
import org.ruoyi.chat.service.chat.ChatImageService;
import org.ruoyi.chat.mapper.ChatMessageImageStore;
import org.ruoyi.chat.mapper.ChatMessageImageStore.StoredImage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ruoyi.chat.mapper.ChatMessageMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

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
    private final ChatMessageImageStore images;

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
        Set<Long> imageMessageIds = images.findMessageIds(result.getRecords().stream()
            .filter(message -> RoleType.USER.getName().equals(message.getRole()))
            .map(ChatMessageVo::getId)
            .toList());
        result.getRecords().forEach(message -> message.setHasImage(imageMessageIds.contains(message.getId())));
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
        lqw.eq(bo.getTotalTokens() != null, ChatMessage::getTotalTokens, bo.getTotalTokens());
        lqw.like(StringUtils.isNotBlank(bo.getModelName()), ChatMessage::getModelName, bo.getModelName());
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
    public List<dev.langchain4j.data.message.ChatMessage> getMessagesBySessionId(Long sessionId) {
        if (sessionId == null) {
            return new java.util.ArrayList<>();
        }

        List<dev.langchain4j.data.message.ChatMessage> chatMessageList = new ArrayList<>();
        ChatMessageBo bo = new ChatMessageBo();
        bo.setSessionId(sessionId);
        List<ChatMessageVo> voList = queryList(bo);

        Map<Long, String> recognizedText = images.findRecognizedText(voList.stream()
            .filter(message -> RoleType.USER.getName().equals(message.getRole()))
            .map(ChatMessageVo::getId)
            .toList());
        for (ChatMessageVo chatMessageVo : voList) {
            switch (chatMessageVo.getRole()) {
                case "user" -> {
                    String input = ChatImageService.modelInput(chatMessageVo.getContent(),
                        recognizedText.get(chatMessageVo.getId()));
                    if (StringUtils.isNotBlank(input)) {
                        chatMessageList.add(UserMessage.from(input));
                    } else if (recognizedText.containsKey(chatMessageVo.getId())) {
                        chatMessageList.add(UserMessage.from("[用户发送了一张图片]"));
                    }
                }
                case "assistant" -> chatMessageList.add(AiMessage.from(chatMessageVo.getContent()));
            }
        }
        return chatMessageList;
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

    /**
     * 保存聊天消息
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @param content   消息内容
     * @param role      角色类型
     * @param modelName 模型名称
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveUserImageMessage(Long userId, Long sessionId, String content, String modelName,
                                     StoredImage image) {
        ChatMessageBo message = new ChatMessageBo();
        message.setUserId(userId);
        message.setSessionId(sessionId);
        message.setContent(content);
        message.setRole(RoleType.USER.getName());
        message.setModelName(modelName);
        if (!insertByBo(message)) {
            throw new IllegalStateException("Could not save the user image message");
        }
        images.insert(message.getId(), image.mimeType(), image.data());
        return message.getId();
    }

    @Override
    public Optional<StoredImage> findOwnedImage(Long userId, Long messageId) {
        if (userId == null || messageId == null) {
            return Optional.empty();
        }
        ChatMessage message = baseMapper.selectById(messageId);
        if (message == null || !userId.equals(message.getUserId())
            || !RoleType.USER.getName().equals(message.getRole())) {
            return Optional.empty();
        }
        return images.find(messageId);
    }

    @Override
    public void saveChatMessage(Long userId, Long sessionId, String content, String role, String modelName) {
        try {
            if (userId == null) {
                log.warn("缺少用户ID，无法保存消息");
                return;
            }

            ChatMessageBo messageBo = new ChatMessageBo();
            messageBo.setUserId(userId);
            messageBo.setSessionId(sessionId);
            messageBo.setContent(content);
            messageBo.setRole(role);
            messageBo.setModelName(modelName);

            insertByBo(messageBo);
            log.debug("保存聊天消息成功，角色: {}, 会话: {}", role, sessionId);
        } catch (Exception e) {
            log.error("保存聊天消息时出错: {}", e.getMessage(), e);
        }
    }
}
