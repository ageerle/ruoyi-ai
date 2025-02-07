package org.ruoyi.system.service;

import org.ruoyi.system.domain.vo.ChatVisitorUsageVo;
import org.ruoyi.system.domain.bo.ChatVisitorUsageBo;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 访客管理Service接口
 *
 * @author Lion Li
 * @date 2024-07-14
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
