package org.ruoyi.system.service;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.system.domain.bo.SysModelBo;
import org.ruoyi.system.domain.vo.SysModelVo;

import java.util.Collection;
import java.util.List;

/**
 * 系统模型Service接口
 *
 * @author Lion Li
 * @date 2024-04-04
 */
public interface ISysModelService {

    /**
     * 查询系统模型
     */
    SysModelVo queryById(Long id);

    /**
     * 查询系统模型列表
     */
    TableDataInfo<SysModelVo> queryPageList(SysModelBo bo, PageQuery pageQuery);

    /**
     * 查询系统模型列表
     */
    List<SysModelVo> queryList(SysModelBo bo);

    /**
     * 新增系统模型
     */
    Boolean insertByBo(SysModelBo bo);

    /**
     * 修改系统模型
     */
    Boolean updateByBo(SysModelBo bo);

    /**
     * 校验并批量删除系统模型信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
