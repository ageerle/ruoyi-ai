package org.ruoyi.service;


import org.ruoyi.domain.bo.KnowledgeInfoBo;
import org.ruoyi.domain.vo.KnowledgeInfoVo;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 知识库Service接口
 *
 * @author ageerle
 * @date 2025-04-08
 */
public interface IKnowledgeInfoService {

    /**
     * 查询知识库
     */
    KnowledgeInfoVo queryById(Long id);

    /**
     * 查询知识库列表
     */
    TableDataInfo<KnowledgeInfoVo> queryPageList(KnowledgeInfoBo bo, PageQuery pageQuery);

    /**
     * 查询知识库列表
     */
    List<KnowledgeInfoVo> queryList(KnowledgeInfoBo bo);

    /**
     * 新增知识库
     */
    Boolean insertByBo(KnowledgeInfoBo bo);

    /**
     * 修改知识库
     */
    Boolean updateByBo(KnowledgeInfoBo bo);

    /**
     * 校验并批量删除知识库信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
