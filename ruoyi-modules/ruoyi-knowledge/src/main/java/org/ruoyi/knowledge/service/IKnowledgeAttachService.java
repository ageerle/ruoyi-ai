package org.ruoyi.knowledge.service;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.knowledge.domain.bo.KnowledgeAttachBo;
import org.ruoyi.knowledge.domain.vo.KnowledgeAttachVo;

import java.util.Collection;
import java.util.List;

/**
 * 知识库附件Service接口
 *
 * @author Lion Li
 * @date 2024-10-21
 */
public interface IKnowledgeAttachService {

    /**
     * 查询知识库附件
     */
    KnowledgeAttachVo queryById(Long id);

    /**
     * 查询知识库附件列表
     */
    TableDataInfo<KnowledgeAttachVo> queryPageList(KnowledgeAttachBo bo, PageQuery pageQuery);

    /**
     * 查询知识库附件列表
     */
    List<KnowledgeAttachVo> queryList(KnowledgeAttachBo bo);

    /**
     * 新增知识库附件
     */
    Boolean insertByBo(KnowledgeAttachBo bo);

    /**
     * 修改知识库附件
     */
    Boolean updateByBo(KnowledgeAttachBo bo);

    /**
     * 校验并批量删除知识库附件信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);


    /**
     * 删除知识附件
     *
     * @return
     */
    void removeKnowledgeAttach(String kid);
}
