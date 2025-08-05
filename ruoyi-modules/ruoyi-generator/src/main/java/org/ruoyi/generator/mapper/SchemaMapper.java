package org.ruoyi.generator.mapper;


import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.core.mapper.BaseMapperPlus;
import org.ruoyi.generator.domain.Schema;
import org.ruoyi.generator.domain.vo.SchemaVo;

/**
 * 数据模型Mapper接口
 *
 * @author ruoyi
 * @date 2024-01-01
 */
@Mapper
public interface SchemaMapper extends BaseMapperPlus<Schema, SchemaVo> {

}