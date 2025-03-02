package org.ruoyi.knowledge.service;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.knowledge.domain.bo.KnowledgeFragmentBo;
import org.ruoyi.knowledge.domain.vo.KnowledgeFragmentVo;

import java.util.Collection;
import java.util.List;

/**
 * 知识片段Service接口
 *
 * @author Lion Li
 * @date 2024-10-21
 */
public interface IKnowledgeFragmentService {

    /**
     * 查询知识片段
     */
    KnowledgeFragmentVo queryById(Long id);

    /**
     * 查询知识片段列表
     */
    TableDataInfo<KnowledgeFragmentVo> queryPageList(KnowledgeFragmentBo bo, PageQuery pageQuery);

    /**
     * 查询知识片段列表
     */
    List<KnowledgeFragmentVo> queryList(KnowledgeFragmentBo bo);

    /**
     * 新增知识片段
     */
    Boolean insertByBo(KnowledgeFragmentBo bo);

    /**
     * 修改知识片段
     */
    Boolean updateByBo(KnowledgeFragmentBo bo);

    /**
     * 校验并批量删除知识片段信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
