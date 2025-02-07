package org.ruoyi.system.service;

import org.ruoyi.system.domain.vo.SysUserModelVo;
import org.ruoyi.system.domain.bo.SysUserModelBo;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 【请填写功能名称】Service接口
 *
 * @author Lion Li
 * @date 2024-08-03
 */
public interface ISysUserModelService {

    /**
     * 查询【请填写功能名称】
     */
    SysUserModelVo queryById(Long id);

    /**
     * 查询【请填写功能名称】列表
     */
    TableDataInfo<SysUserModelVo> queryPageList(SysUserModelBo bo, PageQuery pageQuery);

    /**
     * 查询【请填写功能名称】列表
     */
    List<SysUserModelVo> queryList(SysUserModelBo bo);

    /**
     * 新增【请填写功能名称】
     */
    Boolean insertByBo(SysUserModelBo bo);

    /**
     * 修改【请填写功能名称】
     */
    Boolean updateByBo(SysUserModelBo bo);

    /**
     * 校验并批量删除【请填写功能名称】信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
