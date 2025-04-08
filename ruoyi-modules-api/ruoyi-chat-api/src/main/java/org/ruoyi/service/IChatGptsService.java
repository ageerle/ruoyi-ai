package org.ruoyi.service;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.domain.bo.ChatGptsBo;
import org.ruoyi.domain.vo.ChatGptsVo;

import java.util.Collection;
import java.util.List;

/**
 * gpts管理Service接口
 *
 * @author Lion Li
 * @date 2024-07-09
 */
public interface IChatGptsService {

    /**
     * 查询gpts管理
     */
    ChatGptsVo queryById(Long id);

    /**
     * 查询gpts管理列表
     */
    TableDataInfo<ChatGptsVo> queryPageList(ChatGptsBo bo, PageQuery pageQuery);

    /**
     * 查询gpts管理列表
     */
    List<ChatGptsVo> queryList(ChatGptsBo bo);

    /**
     * 新增gpts管理
     */
    Boolean insertByBo(ChatGptsBo bo);

    /**
     * 修改gpts管理
     */
    Boolean updateByBo(ChatGptsBo bo);

    /**
     * 校验并批量删除gpts管理信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
