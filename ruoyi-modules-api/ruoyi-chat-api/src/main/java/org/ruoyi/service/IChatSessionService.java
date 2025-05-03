package org.ruoyi.service;

import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.domain.bo.ChatSessionBo;
import org.ruoyi.domain.vo.ChatSessionVo;

import java.util.Collection;
import java.util.List;

/**
 * 会话管理Service接口
 *
 * @author ageerle
 * @date 2025-05-03
 */
public interface IChatSessionService {

    /**
     * 查询会话管理
     */
    ChatSessionVo queryById(Long id);

    /**
     * 查询会话管理列表
     */
    TableDataInfo<ChatSessionVo> queryPageList(ChatSessionBo bo, PageQuery pageQuery);

    /**
     * 查询会话管理列表
     */
    List<ChatSessionVo> queryList(ChatSessionBo bo);

    /**
     * 新增会话管理
     */
    Boolean insertByBo(ChatSessionBo bo);

    /**
     * 修改会话管理
     */
    Boolean updateByBo(ChatSessionBo bo);

    /**
     * 校验并批量删除会话管理信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
