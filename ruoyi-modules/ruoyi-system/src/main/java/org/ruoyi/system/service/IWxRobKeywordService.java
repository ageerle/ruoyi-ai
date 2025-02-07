package org.ruoyi.system.service;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.system.domain.bo.WxRobKeywordBo;
import org.ruoyi.system.domain.vo.WxRobKeywordVo;

import java.util.Collection;
import java.util.List;

/**
 * 【请填写功能名称】Service接口
 *
 * @author Lion Li
 * @date 2024-05-01
 */
public interface IWxRobKeywordService {

    /**
     * 查询【请填写功能名称】
     */
    WxRobKeywordVo queryById(Long id);

    /**
     * 查询【请填写功能名称】列表
     */
    TableDataInfo<WxRobKeywordVo> queryPageList(WxRobKeywordBo bo, PageQuery pageQuery);

    /**
     * 查询【请填写功能名称】列表
     */
    List<WxRobKeywordVo> queryList(WxRobKeywordBo bo);

    /**
     * 新增【请填写功能名称】
     */
    Boolean insertByBo(WxRobKeywordBo bo);

    /**
     * 修改【请填写功能名称】
     */
    Boolean updateByBo(WxRobKeywordBo bo);

    /**
     * 校验并批量删除【请填写功能名称】信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
