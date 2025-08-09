package org.ruoyi.system.service;

import org.ruoyi.system.domain.vo.SysDemoVo;
import org.ruoyi.system.domain.bo.SysDemoBo;
    import org.ruoyi.core.page.TableDataInfo;
    import org.ruoyi.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * dome管理Service接口
 *
 * @author ageerle
 * @date Sat Aug 09 21:38:09 CST 2025
 */
public interface SysDemoService {

    /**
     * 查询dome管理
     */
        SysDemoVo queryById(Integer id);

        /**
         * 查询dome管理列表
         */
        TableDataInfo<SysDemoVo> queryPageList(SysDemoBo bo, PageQuery pageQuery);

    /**
     * 查询dome管理列表
     */
    List<SysDemoVo> queryList(SysDemoBo bo);

    /**
     * 新增dome管理
     */
    Boolean insertByBo(SysDemoBo bo);

    /**
     * 修改dome管理
     */
    Boolean updateByBo(SysDemoBo bo);

    /**
     * 校验并批量删除dome管理信息
     */
    Boolean deleteWithValidByIds(Collection<Integer> ids, Boolean isValid);
}
