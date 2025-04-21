package org.ruoyi.service;


import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.domain.bo.ChatPackagePlanBo;
import org.ruoyi.domain.vo.ChatPackagePlanVo;

import java.util.Collection;
import java.util.List;

/**
 * 套餐管理Service接口
 *
 * @author ageerle
 * @date 2025-04-08
 */
public interface IChatPackagePlanService {

    /**
     * 查询套餐管理
     */
    ChatPackagePlanVo queryById(Long id);

    /**
     * 查询套餐管理列表
     */
    TableDataInfo<ChatPackagePlanVo> queryPageList(ChatPackagePlanBo bo, PageQuery pageQuery);

    /**
     * 查询套餐管理列表
     */
    List<ChatPackagePlanVo> queryList(ChatPackagePlanBo bo);

    /**
     * 新增套餐管理
     */
    Boolean insertByBo(ChatPackagePlanBo bo);

    /**
     * 修改套餐管理
     */
    Boolean updateByBo(ChatPackagePlanBo bo);

    /**
     * 校验并批量删除套餐管理信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
