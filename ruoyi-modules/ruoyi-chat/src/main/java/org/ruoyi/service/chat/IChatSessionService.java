package org.ruoyi.service.chat;

import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.domain.bo.chat.ChatSessionBo;
import org.ruoyi.domain.vo.chat.ChatSessionVo;

import java.util.Collection;
import java.util.List;

/**
 * 会话管理Service接口
 *
 * @author ageerle
 * @date 2025-12-30
 */
public interface IChatSessionService {

    /**
     * 查询会话管理
     *
     * @param id 主键
     * @return 会话管理
     */
    ChatSessionVo queryById(Long id);

    /**
     * 分页查询会话管理列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 会话管理分页列表
     */
    TableDataInfo<ChatSessionVo> queryPageList(ChatSessionBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的会话管理列表
     *
     * @param bo 查询条件
     * @return 会话管理列表
     */
    List<ChatSessionVo> queryList(ChatSessionBo bo);

    /**
     * 新增会话管理
     *
     * @param bo 会话管理
     * @return 是否新增成功
     */
    Boolean insertByBo(ChatSessionBo bo);

    /**
     * 修改会话管理
     *
     * @param bo 会话管理
     * @return 是否修改成功
     */
    Boolean updateByBo(ChatSessionBo bo);

    /**
     * 校验并批量删除会话管理信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
