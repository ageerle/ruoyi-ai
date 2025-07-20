package org.ruoyi.service;

import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.domain.bo.KnowledgeRoleGroupBo;
import org.ruoyi.domain.vo.KnowledgeRoleGroupVo;

import java.util.Collection;
import java.util.List;

/**
 * 知识库角色组Service接口
 *
 * @author ageerle
 * @date 2025-07-19
 */
public interface IKnowledgeRoleGroupService {

    /**
     * 查询知识库角色组
     */
    KnowledgeRoleGroupVo queryById(Long id);

    /**
     * 查询知识库角色组列表
     */
    TableDataInfo<KnowledgeRoleGroupVo> queryPageList(KnowledgeRoleGroupBo bo, PageQuery pageQuery);

    /**
     * 查询知识库角色组列表
     */
    List<KnowledgeRoleGroupVo> queryList(KnowledgeRoleGroupBo bo);

    /**
     * 新增知识库角色组
     */
    Boolean insertByBo(KnowledgeRoleGroupBo bo);

    /**
     * 修改知识库角色组
     */
    Boolean updateByBo(KnowledgeRoleGroupBo bo);

    /**
     * 校验并批量删除知识库角色组信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
