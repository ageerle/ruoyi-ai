package org.ruoyi.service;


import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.domain.bo.ChatGptsBo;
import org.ruoyi.domain.vo.ChatGptsVo;

import java.util.Collection;
import java.util.List;

/**
 * 应用管理Service接口
 *
 * @author ageerle
 * @date 2025-04-08
 */
public interface IChatGptsService {

    /**
     * 查询应用管理
     */
    ChatGptsVo queryById(Long id);

    /**
     * 查询应用管理列表
     */
    TableDataInfo<ChatGptsVo> queryPageList(ChatGptsBo bo, PageQuery pageQuery);

    /**
     * 查询应用管理列表
     */
    List<ChatGptsVo> queryList(ChatGptsBo bo);

    /**
     * 新增应用管理
     */
    Boolean insertByBo(ChatGptsBo bo);

    /**
     * 修改应用管理
     */
    Boolean updateByBo(ChatGptsBo bo);

    /**
     * 校验并批量删除应用管理信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
