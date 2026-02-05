package org.ruoyi.service.chat;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.domain.bo.chat.ChatConfigBo;
import org.ruoyi.domain.vo.chat.ChatConfigVo;

import java.util.Collection;
import java.util.List;

/**
 * 配置信息Service接口
 *
 * @author ageerle
 * @date 2025-12-14
 */
public interface IChatConfigService {

    /**
     * 查询配置信息
     *
     * @param id 主键
     * @return 配置信息
     */
    ChatConfigVo queryById(Long id);

    /**
     * 分页查询配置信息列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 配置信息分页列表
     */
    TableDataInfo<ChatConfigVo> queryPageList(ChatConfigBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的配置信息列表
     *
     * @param bo 查询条件
     * @return 配置信息列表
     */
    List<ChatConfigVo> queryList(ChatConfigBo bo);

    /**
     * 新增配置信息
     *
     * @param bo 配置信息
     * @return 是否新增成功
     */
    Boolean insertByBo(ChatConfigBo bo);

    /**
     * 修改配置信息
     *
     * @param bo 配置信息
     * @return 是否修改成功
     */
    Boolean updateByBo(ChatConfigBo bo);

    /**
     * 校验并批量删除配置信息信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
