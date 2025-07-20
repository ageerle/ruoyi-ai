package org.ruoyi.service;

import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.domain.bo.KnowledgeRoleRelationBo;
import org.ruoyi.domain.vo.KnowledgeRoleRelationVo;

import java.util.Collection;
import java.util.List;

/**
 * 知识库角色与知识库关联Service接口
 *
 * @author ageerle
 * @date 2025-07-19
 */
public interface IKnowledgeRoleRelationService {

    /**
     * 查询知识库角色与知识库关联
     */
    KnowledgeRoleRelationVo queryById(Long id);

    /**
     * 查询知识库角色与知识库关联列表
     */
    TableDataInfo<KnowledgeRoleRelationVo> queryPageList(KnowledgeRoleRelationBo bo, PageQuery pageQuery);

    /**
     * 查询知识库角色与知识库关联列表
     */
    List<KnowledgeRoleRelationVo> queryList(KnowledgeRoleRelationBo bo);

    /**
     * 新增知识库角色与知识库关联
     */
    Boolean insertByBo(KnowledgeRoleRelationBo bo);

    /**
     * 修改知识库角色与知识库关联
     */
    Boolean updateByBo(KnowledgeRoleRelationBo bo);

    /**
     * 校验并批量删除知识库角色与知识库关联信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
