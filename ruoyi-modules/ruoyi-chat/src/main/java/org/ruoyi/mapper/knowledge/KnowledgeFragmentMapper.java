package org.ruoyi.mapper.knowledge;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ruoyi.domain.entity.knowledge.KnowledgeFragment;
import org.ruoyi.domain.vo.knowledge.DocFragmentCountVo;
import org.ruoyi.domain.vo.knowledge.KnowledgeFragmentVo;
import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

/**
 * 知识片段Mapper接口
 *
 * @author ageerle
 * @date 2025-12-17
 */
@Mapper
public interface KnowledgeFragmentMapper extends BaseMapperPlus<KnowledgeFragment, KnowledgeFragmentVo> {

    /**
     * 批量统计各文档的分块数（强类型接收，避免 Map key 大小写问题）
     *
     * @param docIds 文档 ID 列表
     * @return 每个 docId 对应的分块数列表
     */
    @Select("<script>" +
            "SELECT doc_id AS docId, COUNT(*) AS fragmentCount " +
            "FROM knowledge_fragment " +
            "WHERE doc_id IN " +
            "<foreach collection='docIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "GROUP BY doc_id" +
            "</script>")
    List<DocFragmentCountVo> selectFragmentCountByDocIds(@Param("docIds") List<String> docIds);
}
