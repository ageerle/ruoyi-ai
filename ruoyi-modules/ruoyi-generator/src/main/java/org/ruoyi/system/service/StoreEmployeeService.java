package org.ruoyi.system.service;

import org.ruoyi.system.domain.vo.StoreEmployeeVo;
import org.ruoyi.system.domain.bo.StoreEmployeeBo;
    import org.ruoyi.core.page.TableDataInfo;
    import org.ruoyi.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 员工分配Service接口
 *
 * @author ageerle
 * @date Mon Aug 18 21:33:27 CST 2025
 */
public interface StoreEmployeeService {

    /**
     * 查询员工分配
     */
        StoreEmployeeVo queryById(Long id);

        /**
         * 查询员工分配列表
         */
        TableDataInfo<StoreEmployeeVo> queryPageList(StoreEmployeeBo bo, PageQuery pageQuery);

    /**
     * 查询员工分配列表
     */
    List<StoreEmployeeVo> queryList(StoreEmployeeBo bo);

    /**
     * 新增员工分配
     */
    Boolean insertByBo(StoreEmployeeBo bo);

    /**
     * 修改员工分配
     */
    Boolean updateByBo(StoreEmployeeBo bo);

    /**
     * 校验并批量删除员工分配信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
