package org.ruoyi.graph.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.ruoyi.graph.domain.GraphEntityType;

import java.util.List;

/**
 * 图谱实体类型Mapper接口
 *
 * @author ruoyi
 * @date 2025-09-30
 */
public interface GraphEntityTypeMapper extends BaseMapper<GraphEntityType> {

    /**
     * 根据类型编码查询
     *
     * @param typeCode 类型编码
     * @return 实体类型
     */
    GraphEntityType selectByTypeCode(String typeCode);

    /**
     * 查询所有启用的实体类型
     *
     * @return 实体类型列表
     */
    List<GraphEntityType> selectEnabledTypes();

    /**
     * 批量查询实体类型
     *
     * @param typeCodes 类型编码列表
     * @return 实体类型列表
     */
    List<GraphEntityType> selectByTypeCodes(List<String> typeCodes);
}
