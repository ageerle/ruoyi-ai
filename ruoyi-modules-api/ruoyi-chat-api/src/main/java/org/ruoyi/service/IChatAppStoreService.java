package org.ruoyi.service;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.domain.bo.ChatAppStoreBo;
import org.ruoyi.domain.vo.ChatAppStoreVo;


import java.util.Collection;
import java.util.List;

/**
 * 应用市场Service接口
 *
 * @author Lion Li
 * @date 2024-03-19
 */
public interface IChatAppStoreService {

    /**
     * 查询应用市场
     */
    ChatAppStoreVo queryById(Long id);

    /**
     * 查询应用市场列表
     */
    TableDataInfo<ChatAppStoreVo> queryPageList(ChatAppStoreBo bo, PageQuery pageQuery);

    /**
     * 查询应用市场列表
     */
    List<ChatAppStoreVo> queryList(ChatAppStoreBo bo);


    /**
     * 修改应用市场
     */
    Boolean updateByBo(ChatAppStoreBo bo);

    /**
     * 校验并批量删除应用市场信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);


}
