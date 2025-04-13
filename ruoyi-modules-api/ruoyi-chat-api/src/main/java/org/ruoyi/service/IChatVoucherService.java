package org.ruoyi.service;


import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.domain.bo.ChatVoucherBo;
import org.ruoyi.domain.vo.ChatVoucherVo;

import java.util.Collection;
import java.util.List;

/**
 * 用户兑换记录Service接口
 *
 * @author ageerle
 * @date 2025-04-08
 */
public interface IChatVoucherService {

    /**
     * 查询用户兑换记录
     */
    ChatVoucherVo queryById(Long id);

    /**
     * 查询用户兑换记录列表
     */
    TableDataInfo<ChatVoucherVo> queryPageList(ChatVoucherBo bo, PageQuery pageQuery);

    /**
     * 查询用户兑换记录列表
     */
    List<ChatVoucherVo> queryList(ChatVoucherBo bo);

    /**
     * 新增用户兑换记录
     */
    Boolean insertByBo(ChatVoucherBo bo);

    /**
     * 修改用户兑换记录
     */
    Boolean updateByBo(ChatVoucherBo bo);

    /**
     * 校验并批量删除用户兑换记录信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
