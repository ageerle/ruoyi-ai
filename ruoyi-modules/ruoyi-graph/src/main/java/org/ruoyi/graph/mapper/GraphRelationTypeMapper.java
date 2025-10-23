package org.ruoyi.graph.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.ruoyi.graph.domain.GraphRelationType;

import java.util.List;

/**
 * 图谱关系类型Mapper接口
 *
 * @author ruoyi
 * @date 2025-09-30
 */
public interface GraphRelationTypeMapper extends BaseMapper<GraphRelationType> {

    /**
     * 根据关系编码查询
     *
     * @param relationCode 关系编码
     * @return 关系类型
     */
    GraphRelationType selectByRelationCode(String relationCode);

    /**
     * 查询所有启用的关系类型
     *
     * @return 关系类型列表
     */
    List<GraphRelationType> selectEnabledTypes();

    /**
     * 批量查询关系类型
     *
     * @param relationCodes 关系编码列表
     * @return 关系类型列表
     */
    List<GraphRelationType> selectByRelationCodes(List<String> relationCodes);
}
