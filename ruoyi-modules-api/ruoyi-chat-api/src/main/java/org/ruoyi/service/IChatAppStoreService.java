package org.ruoyi.service;


import org.ruoyi.domain.bo.ChatAppStoreBo;
import org.ruoyi.domain.vo.ChatAppStoreVo;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 应用商店Service接口
 *
 * @author ageerle
 * @date 2025-04-08
 */
public interface IChatAppStoreService {

    /**
     * 查询应用商店
     */
    ChatAppStoreVo queryById(Long id);

    /**
     * 查询应用商店列表
     */
    TableDataInfo<ChatAppStoreVo> queryPageList(ChatAppStoreBo bo, PageQuery pageQuery);

    /**
     * 查询应用商店列表
     */
    List<ChatAppStoreVo> queryList(ChatAppStoreBo bo);

    /**
     * 新增应用商店
     */
    Boolean insertByBo(ChatAppStoreBo bo);

    /**
     * 修改应用商店
     */
    Boolean updateByBo(ChatAppStoreBo bo);

    /**
     * 校验并批量删除应用商店信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
