package org.ruoyi.service;


import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.domain.bo.ChatUsageTokenBo;
import org.ruoyi.domain.vo.ChatUsageTokenVo;

import java.util.Collection;
import java.util.List;

/**
 * 用户token使用详情Service接口
 *
 * @author ageerle
 * @date 2025-04-08
 */
public interface IChatUsageTokenService {

    /**
     * 查询用户token使用详情
     */
    ChatUsageTokenVo queryById(Long id);

    /**
     * 查询用户token使用详情列表
     */
    TableDataInfo<ChatUsageTokenVo> queryPageList(ChatUsageTokenBo bo, PageQuery pageQuery);

    /**
     * 查询用户token使用详情列表
     */
    List<ChatUsageTokenVo> queryList(ChatUsageTokenBo bo);

    /**
     * 新增用户token使用详情
     */
    Boolean insertByBo(ChatUsageTokenBo bo);

    /**
     * 修改用户token使用详情
     */
    Boolean updateByBo(ChatUsageTokenBo bo);

    /**
     * 校验并批量删除用户token使用详情信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
