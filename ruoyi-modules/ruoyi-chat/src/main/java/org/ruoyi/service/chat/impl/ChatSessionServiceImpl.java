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
import org.ruoyi.domain.bo.chat.ChatSessionBo;
import org.ruoyi.domain.entity.chat.ChatSession;
import org.ruoyi.mapper.chat.ChatSessionMapper;
import org.ruoyi.service.chat.IChatSessionService;
import org.springframework.stereotype.Service;
import org.ruoyi.domain.vo.chat.ChatSessionVo;
import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 会话管理Service业务层处理
 *
 * @author ageerle
 * @date 2025-12-30
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ChatSessionServiceImpl implements IChatSessionService {

    private final ChatSessionMapper baseMapper;

    /**
     * 查询会话管理
     *
     * @param id 主键
     * @return 会话管理
     */
    @Override
    public ChatSessionVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询会话管理列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 会话管理分页列表
     */
    @Override
    public TableDataInfo<ChatSessionVo> queryPageList(ChatSessionBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ChatSession> lqw = buildQueryWrapper(bo);
        Page<ChatSessionVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的会话管理列表
     *
     * @param bo 查询条件
     * @return 会话管理列表
     */
    @Override
    public List<ChatSessionVo> queryList(ChatSessionBo bo) {
        LambdaQueryWrapper<ChatSession> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<ChatSession> buildQueryWrapper(ChatSessionBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<ChatSession> lqw = Wrappers.lambdaQuery();
        lqw.orderByAsc(ChatSession::getId);
        lqw.eq(bo.getUserId() != null, ChatSession::getUserId, bo.getUserId());
        lqw.eq(StringUtils.isNotBlank(bo.getSessionTitle()), ChatSession::getSessionTitle, bo.getSessionTitle());
        lqw.eq(StringUtils.isNotBlank(bo.getSessionContent()), ChatSession::getSessionContent, bo.getSessionContent());
        lqw.eq(StringUtils.isNotBlank(bo.getConversationId()), ChatSession::getConversationId, bo.getConversationId());
        return lqw;
    }

    /**
     * 新增会话管理
     *
     * @param bo 会话管理
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(ChatSessionBo bo) {
        ChatSession add = MapstructUtils.convert(bo, ChatSession.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改会话管理
     *
     * @param bo 会话管理
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(ChatSessionBo bo) {
        ChatSession update = MapstructUtils.convert(bo, ChatSession.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(ChatSession entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除会话管理信息
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
}
