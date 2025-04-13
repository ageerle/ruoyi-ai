package org.ruoyi.service;


import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.domain.bo.ChatPluginBo;
import org.ruoyi.domain.vo.ChatPluginVo;

import java.util.Collection;
import java.util.List;

/**
 * 插件管理Service接口
 *
 * @author ageerle
 * @date 2025-04-08
 */
public interface IChatPluginService {

    /**
     * 查询插件管理
     */
    ChatPluginVo queryById(Long id);

    /**
     * 查询插件管理列表
     */
    TableDataInfo<ChatPluginVo> queryPageList(ChatPluginBo bo, PageQuery pageQuery);

    /**
     * 查询插件管理列表
     */
    List<ChatPluginVo> queryList(ChatPluginBo bo);

    /**
     * 新增插件管理
     */
    Boolean insertByBo(ChatPluginBo bo);

    /**
     * 修改插件管理
     */
    Boolean updateByBo(ChatPluginBo bo);

    /**
     * 校验并批量删除插件管理信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
