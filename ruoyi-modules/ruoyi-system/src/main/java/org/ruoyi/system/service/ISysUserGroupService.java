package org.ruoyi.system.service;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.system.domain.bo.SysUserGroupBo;
import org.ruoyi.system.domain.vo.SysUserGroupVo;

import java.util.Collection;
import java.util.List;

/**
 * 【请填写功能名称】Service接口
 *
 * @author Lion Li
 * @date 2024-08-03
 */
public interface ISysUserGroupService {

    /**
     * 查询【请填写功能名称】
     */
    SysUserGroupVo queryById(Long id);

    /**
     * 查询【请填写功能名称】列表
     */
    TableDataInfo<SysUserGroupVo> queryPageList(SysUserGroupBo bo, PageQuery pageQuery);

    /**
     * 查询【请填写功能名称】列表
     */
    List<SysUserGroupVo> queryList(SysUserGroupBo bo);

    /**
     * 新增【请填写功能名称】
     */
    Boolean insertByBo(SysUserGroupBo bo);

    /**
     * 修改【请填写功能名称】
     */
    Boolean updateByBo(SysUserGroupBo bo);

    /**
     * 校验并批量删除【请填写功能名称】信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
