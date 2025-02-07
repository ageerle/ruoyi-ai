package org.ruoyi.common.core.service;

/**
 * 通用 部门服务
 *
 * @author Lion Li
 */
public interface DeptService {

    /**
     * 通过部门ID查询部门名称
     *
     * @param deptIds 部门ID串逗号分隔
     * @return 部门名称串逗号分隔
     */
    String selectDeptNameByIds(String deptIds);

}
