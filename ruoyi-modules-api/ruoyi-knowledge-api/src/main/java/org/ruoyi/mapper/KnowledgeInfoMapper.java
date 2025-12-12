package org.ruoyi.mapper;


import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ruoyi.core.mapper.BaseMapperPlus;
import org.ruoyi.domain.KnowledgeInfo;
import org.ruoyi.domain.vo.KnowledgeInfoVo;

/**
 * 知识库Mapper接口
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Mapper
public interface KnowledgeInfoMapper extends BaseMapperPlus<KnowledgeInfo, KnowledgeInfoVo> {

    /**
     * 根据kid查询知识库
     *
     * @param kid 知识库id
     * @return KnowledgeInfo
     */
    KnowledgeInfo selectByKid(@Param("kid") String kid);
}
