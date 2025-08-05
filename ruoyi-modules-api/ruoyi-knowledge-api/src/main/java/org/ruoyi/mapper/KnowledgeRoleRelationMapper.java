package org.ruoyi.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ruoyi.core.mapper.BaseMapperPlus;
import org.ruoyi.domain.KnowledgeRoleRelation;
import org.ruoyi.domain.vo.KnowledgeRoleRelationVo;

import java.util.List;

/**
 * 知识库角色与知识库关联Mapper接口
 *
 * @author ageerle
 * @date 2025-07-19
 */
@Mapper
public interface KnowledgeRoleRelationMapper extends BaseMapperPlus<KnowledgeRoleRelation, KnowledgeRoleRelationVo> {
    @Select("SELECT knowledge_id FROM knowledge_role_relation WHERE knowledge_role_id = #{knowledgeRoleId}")
    List<Long> selectKnowledgeIdsByRoleId(@Param("knowledgeRoleId") Long knowledgeRoleId);

    /**
     * 根据 roleId 删除所有关联
     */
    @Delete("DELETE FROM knowledge_role_relation WHERE knowledge_role_id = #{knowledgeRoleId}")
    void deleteByRoleId(@Param("knowledgeRoleId") Long knowledgeRoleId);

    @Delete({
            "<script>",
            "DELETE FROM knowledge_role_relation",
            "WHERE knowledge_role_id IN",
            "<foreach collection='knowledgeRoleIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    void deleteByRoleIds(@Param("knowledgeRoleIds") List<Long> knowledgeRoleIds);
}
