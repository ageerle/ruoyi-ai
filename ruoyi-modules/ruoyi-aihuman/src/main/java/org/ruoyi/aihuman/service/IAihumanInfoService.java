package org.ruoyi.aihuman.service;

import org.ruoyi.aihuman.domain.AihumanInfo;
import org.ruoyi.aihuman.domain.vo.AihumanInfoVo;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * AI人类交互信息Service接口
 *
 * @author QingYunAI
 */
public interface IAihumanInfoService {

    /**
     * 查询AI人类交互信息
     */
    AihumanInfoVo queryById(Long id);

    /**
     * 查询AI人类交互信息列表
     */
    TableDataInfo<AihumanInfoVo> queryPageList(AihumanInfo record, PageQuery pageQuery);

    /**
     * 查询AI人类交互信息列表
     */
    List<AihumanInfoVo> queryList(AihumanInfo record);

    /**
     * 新增AI人类交互信息
     */
    int insert(AihumanInfo record);

    /**
     * 修改AI人类交互信息
     */
    int update(AihumanInfo record);

    /**
     * 批量删除AI人类交互信息
     */
    int deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}