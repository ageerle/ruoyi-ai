package org.ruoyi.system.service;

import org.ruoyi.system.domain.vo.SysPackagePlanVo;
import org.ruoyi.system.domain.bo.SysPackagePlanBo;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 套餐管理Service接口
 *
 * @author Lion Li
 * @date 2024-05-05
 */
public interface ISysPackagePlanService {

    /**
     * 查询套餐管理
     */
    SysPackagePlanVo queryById(Long id);

    /**
     * 查询套餐管理列表
     */
    TableDataInfo<SysPackagePlanVo> queryPageList(SysPackagePlanBo bo, PageQuery pageQuery);

    /**
     * 查询套餐管理列表
     */
    List<SysPackagePlanVo> queryList(SysPackagePlanBo bo);

    /**
     * 新增套餐管理
     */
    Boolean insertByBo(SysPackagePlanBo bo);

    /**
     * 修改套餐管理
     */
    Boolean updateByBo(SysPackagePlanBo bo);

    /**
     * 校验并批量删除套餐管理信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
