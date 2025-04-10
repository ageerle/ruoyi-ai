package org.ruoyi.system.service;


import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.system.domain.bo.ChatConfigBo;
import org.ruoyi.system.domain.vo.ChatConfigVo;

import java.util.Collection;
import java.util.List;

/**
 * 配置信息Service接口
 *
 * @author ageerle
 * @date 2025-04-08
 */
public interface IChatConfigService {

    /**
     * 查询配置信息
     */
    ChatConfigVo queryById(Long id);

    /**
     * 查询配置信息列表
     */
    TableDataInfo<ChatConfigVo> queryPageList(ChatConfigBo bo, PageQuery pageQuery);

    /**
     * 查询配置信息列表
     */
    List<ChatConfigVo> queryList(ChatConfigBo bo);

    /**
     * 新增配置信息
     */
    Boolean insertByBo(ChatConfigBo bo);

    /**
     * 修改配置信息
     */
    Boolean updateByBo(ChatConfigBo bo);

    /**
     * 校验并批量删除配置信息信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
