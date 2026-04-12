package org.ruoyi.mapper.knowledge;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ruoyi.domain.entity.knowledge.KnowledgeAttach;
import org.ruoyi.domain.vo.knowledge.KnowledgeAttachVo;
import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 知识库附件Mapper接口
 *
 * @author ageerle
 * @date 2025-12-17
 */
@Mapper
public interface KnowledgeAttachMapper extends BaseMapperPlus<KnowledgeAttach, KnowledgeAttachVo> {

    /**
     * 统计指定知识库下的文档数量
     */
    @Select("SELECT COUNT(*) FROM knowledge_attach WHERE knowledge_id = #{knowledgeId}")
    int countByKnowledgeId(@Param("knowledgeId") Long knowledgeId);
}
