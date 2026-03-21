package org.ruoyi.service.chat;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.domain.bo.chat.ChatProviderBo;
import org.ruoyi.domain.vo.chat.ChatProviderVo;

import java.util.Collection;
import java.util.List;

/**
 * 厂商管理Service接口
 *
 * @author ageerle
 * @date 2025-12-14
 */
public interface IChatProviderService {

    /**
     * 查询厂商管理
     *
     * @param id 主键
     * @return 厂商管理
     */
    ChatProviderVo queryById(Long id);

    /**
     * 分页查询厂商管理列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 厂商管理分页列表
     */
    TableDataInfo<ChatProviderVo> queryPageList(ChatProviderBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的厂商管理列表
     *
     * @param bo 查询条件
     * @return 厂商管理列表
     */
    List<ChatProviderVo> queryList(ChatProviderBo bo);

    /**
     * 新增厂商管理
     *
     * @param bo 厂商管理
     * @return 是否新增成功
     */
    Boolean insertByBo(ChatProviderBo bo);

    /**
     * 修改厂商管理
     *
     * @param bo 厂商管理
     * @return 是否修改成功
     */
    Boolean updateByBo(ChatProviderBo bo);

    /**
     * 校验并批量删除厂商管理信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
