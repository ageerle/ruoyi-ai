package org.ruoyi.service;


import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.domain.ChatModel;
import org.ruoyi.domain.bo.ChatModelBo;
import org.ruoyi.domain.vo.ChatModelVo;

import java.util.Collection;
import java.util.List;

/**
 * 聊天模型Service接口
 *
 * @author ageerle
 * @date 2025-04-08
 */
public interface IChatModelService {

    /**
     * 查询聊天模型
     */
    ChatModelVo queryById(Long id);

    /**
     * 查询聊天模型列表
     */
    TableDataInfo<ChatModelVo> queryPageList(ChatModelBo bo, PageQuery pageQuery);

    /**
     * 查询聊天模型列表
     */
    List<ChatModelVo> queryList(ChatModelBo bo);

    /**
     * 新增聊天模型
     */
    Boolean insertByBo(ChatModelBo bo);

    /**
     * 修改聊天模型
     */
    Boolean updateByBo(ChatModelBo bo);

    /**
     * 校验并批量删除聊天模型信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);


    /**
     * 通过模型名称获取模型信息
     */
    ChatModelVo selectModelByName(String modelName);
    /**
     * 通过模型分类获取模型信息
     */
    ChatModelVo selectModelByCategory(String image);
    /**
     * 获取ppt模型信息
     */
    ChatModel getPPT();

}
