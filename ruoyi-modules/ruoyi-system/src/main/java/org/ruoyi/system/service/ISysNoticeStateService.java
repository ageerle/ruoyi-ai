package org.ruoyi.system.service;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.system.domain.bo.SysNoticeStateBo;
import org.ruoyi.system.domain.vo.SysNoticeStateVo;

import java.util.Collection;
import java.util.List;

/**
 * 用户阅读状态Service接口
 *
 * @author Lion Li
 * @date 2024-05-11
 */
public interface ISysNoticeStateService {

    /**
     * 查询用户阅读状态
     */
    SysNoticeStateVo queryById(Long id);

    /**
     * 查询用户阅读状态列表
     */
    TableDataInfo<SysNoticeStateVo> queryPageList(SysNoticeStateBo bo, PageQuery pageQuery);

    /**
     * 查询用户阅读状态列表
     */
    List<SysNoticeStateVo> queryList(SysNoticeStateBo bo);

    /**
     * 新增用户阅读状态
     */
    Boolean insertByBo(SysNoticeStateBo bo);

    /**
     * 修改用户阅读状态
     */
    Boolean updateByBo(SysNoticeStateBo bo);

    /**
     * 校验并批量删除用户阅读状态信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
