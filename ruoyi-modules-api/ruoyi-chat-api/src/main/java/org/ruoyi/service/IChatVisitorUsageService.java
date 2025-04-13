package org.ruoyi.service;

import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.domain.bo.ChatVisitorUsageBo;
import org.ruoyi.domain.vo.ChatVisitorUsageVo;

import java.util.Collection;
import java.util.List;

/**
 * 访客管理Service接口
 *
 * @author ageerle
 * @date 2025-04-08
 */
public interface IChatVisitorUsageService {

    /**
     * 查询访客管理
     */
    ChatVisitorUsageVo queryById(Long id);

    /**
     * 查询访客管理列表
     */
    TableDataInfo<ChatVisitorUsageVo> queryPageList(ChatVisitorUsageBo bo, PageQuery pageQuery);

    /**
     * 查询访客管理列表
     */
    List<ChatVisitorUsageVo> queryList(ChatVisitorUsageBo bo);

    /**
     * 新增访客管理
     */
    Boolean insertByBo(ChatVisitorUsageBo bo);

    /**
     * 修改访客管理
     */
    Boolean updateByBo(ChatVisitorUsageBo bo);

    /**
     * 校验并批量删除访客管理信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
