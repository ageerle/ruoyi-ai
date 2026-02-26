package org.ruoyi.common.chat.service.chat;

import org.ruoyi.common.chat.domain.bo.chat.ChatModelBo;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * 模型管理Service接口
 *
 * @author ageerle
 * @date 2025-12-14
 */
public interface IChatModelService {

    /**
     * 查询模型管理
     *
     * @param id 主键
     * @return 模型管理
     */
    ChatModelVo queryById(Long id);

    /**
     * 根据模型名称查询模型
     *
     * @param modelName 模型名称
     * @return 模型管理
     */
    ChatModelVo selectModelByName(String modelName);

    /**
     * 分页查询模型管理列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 模型管理分页列表
     */
    TableDataInfo<ChatModelVo> queryPageList(ChatModelBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的模型管理列表
     *
     * @param bo 查询条件
     * @return 模型管理列表
     */
    List<ChatModelVo> queryList(ChatModelBo bo);

    /**
     * 新增模型管理
     *
     * @param bo 模型管理
     * @return 是否新增成功
     */
    Boolean insertByBo(ChatModelBo bo);

    /**
     * 修改模型管理
     *
     * @param bo 模型管理
     * @return 是否修改成功
     */
    Boolean updateByBo(ChatModelBo bo);

    /**
     * 校验并批量删除模型管理信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
