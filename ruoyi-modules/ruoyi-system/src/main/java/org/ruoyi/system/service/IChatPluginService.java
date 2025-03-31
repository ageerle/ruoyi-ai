package org.ruoyi.system.service;

import org.ruoyi.system.domain.ChatPlugin;
import org.ruoyi.system.domain.vo.ChatPluginVo;
import org.ruoyi.system.domain.bo.ChatPluginBo;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 插件管理Service接口
 *
 * @author ageerle
 * @date 2025-03-30
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
