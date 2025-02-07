package org.ruoyi.system.service;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.system.domain.bo.WxRobRelationBo;
import org.ruoyi.system.domain.vo.WxRobRelationVo;

import java.util.Collection;
import java.util.List;

/**
 * 【请填写功能名称】Service接口
 *
 * @author Lion Li
 * @date 2024-05-01
 */
public interface IWxRobRelationService {

    /**
     * 查询【请填写功能名称】
     */
    WxRobRelationVo queryById(Long id);

    /**
     * 查询【请填写功能名称】列表
     */
    TableDataInfo<WxRobRelationVo> queryPageList(WxRobRelationBo bo, PageQuery pageQuery);

    /**
     * 查询【请填写功能名称】列表
     */
    List<WxRobRelationVo> queryList(WxRobRelationBo bo);

    /**
     * 新增【请填写功能名称】
     */
    Boolean insertByBo(WxRobRelationBo bo);

    /**
     * 修改【请填写功能名称】
     */
    Boolean updateByBo(WxRobRelationBo bo);

    /**
     * 校验并批量删除【请填写功能名称】信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
