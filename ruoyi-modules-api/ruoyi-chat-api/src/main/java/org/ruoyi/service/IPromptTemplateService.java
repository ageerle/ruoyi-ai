package org.ruoyi.service;


import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.domain.bo.PromptTemplateBo;
import org.ruoyi.domain.vo.PromptTemplateVo;

import java.util.Collection;
import java.util.List;

/**
 * 提示词模板Service接口
 *
 * @author evo
 * @date 2025-06-12
 */
public interface IPromptTemplateService {

    /**
     * 查询提示词模板
     */
    PromptTemplateVo queryById(Long id);

    /**
     * 查询提示词模板列表
     */
    TableDataInfo<PromptTemplateVo> queryPageList(PromptTemplateBo bo, PageQuery pageQuery);

    /**
     * 查询提示词模板列表
     */
    List<PromptTemplateVo> queryList(PromptTemplateBo bo);

    /**
     * 新增提示词模板
     */
    Boolean insertByBo(PromptTemplateBo bo);

    /**
     * 修改提示词模板
     */
    Boolean updateByBo(PromptTemplateBo bo);

    /**
     * 校验并批量删除提示词模板信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 根据分类查询提示词模板
     *
     * @param category 分类
     */
    PromptTemplateVo queryByCategory(String category);
}
