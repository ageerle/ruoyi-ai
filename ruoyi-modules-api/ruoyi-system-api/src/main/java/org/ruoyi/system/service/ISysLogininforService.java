package org.ruoyi.system.service;

import org.ruoyi.common.log.event.LogininforEvent;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.bo.SysLogininforBo;
import org.ruoyi.system.domain.vo.SysLogininforVo;

import java.util.List;

/**
 * 系统访问日志情况信息 服务层
 *
 * @author Lion Li
 */
public interface ISysLogininforService {


    TableDataInfo<SysLogininforVo> selectPageLogininforList(SysLogininforBo logininfor, PageQuery pageQuery);

    /**
     * 新增系统登录日志
     *
     * @param bo 访问日志对象
     */
    void insertLogininfor(SysLogininforBo bo);

    /**
     * 查询系统登录日志集合
     *
     * @param logininfor 访问日志对象
     * @return 登录记录集合
     */
    List<SysLogininforVo> selectLogininforList(SysLogininforBo logininfor);

    /**
     * 批量删除系统登录日志
     *
     * @param infoIds 需要删除的登录日志ID
     * @return 结果
     */
    int deleteLogininforByIds(Long[] infoIds);

    /**
     * 清空系统登录日志
     */
    void cleanLogininfor();

  void recordLogininfor(LogininforEvent logininforEvent);
}
