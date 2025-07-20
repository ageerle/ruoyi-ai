package org.ruoyi.service;

import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.domain.bo.KnowledgeRoleBo;
import org.ruoyi.domain.vo.KnowledgeRoleVo;

import java.util.Collection;
import java.util.List;

/**
 * 知识库角色Service接口
 *
 * @author ageerle
 * @date 2025-07-19
 */
public interface IKnowledgeRoleService {

    /**
     * 查询知识库角色
     */
    KnowledgeRoleVo queryById(Long id);

    /**
     * 查询知识库角色列表
     */
    TableDataInfo<KnowledgeRoleVo> queryPageList(KnowledgeRoleBo bo, PageQuery pageQuery);

    /**
     * 查询知识库角色列表
     */
    List<KnowledgeRoleVo> queryList(KnowledgeRoleBo bo);

    /**
     * 新增知识库角色
     */
    Boolean insertByBo(KnowledgeRoleBo bo);

    /**
     * 修改知识库角色
     */
    Boolean updateByBo(KnowledgeRoleBo bo);

    /**
     * 校验并批量删除知识库角色信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
