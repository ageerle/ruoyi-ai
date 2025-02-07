package org.ruoyi.system.service;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.system.domain.bo.WxRobConfigBo;
import org.ruoyi.system.domain.vo.WxRobConfigVo;

import java.util.Collection;
import java.util.List;

/**
 * 机器人Service接口
 *
 * @author Lion Li
 * @date 2024-05-01
 */
public interface IWxRobConfigService {

    /**
     * 查询机器人
     */
    WxRobConfigVo queryById(Long id);

    /**
     * 查询机器人列表
     */
    TableDataInfo<WxRobConfigVo> queryPageList(WxRobConfigBo bo, PageQuery pageQuery);

    /**
     * 查询当前用户绑定的机器人信息
     */
    List<WxRobConfigVo> queryList(WxRobConfigBo bo);

    /**
     * 新增机器人
     */
    Boolean insertByBo(WxRobConfigBo bo);

    /**
     * 修改机器人
     */
    Boolean updateByBo(WxRobConfigBo bo);

    /**
     * 校验并批量删除机器人信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
