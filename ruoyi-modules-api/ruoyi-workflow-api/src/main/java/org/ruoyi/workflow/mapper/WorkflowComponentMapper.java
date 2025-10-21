package org.ruoyi.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ruoyi.workflow.entity.WorkflowComponent;

@Mapper
public interface WorkflowComponentMapper extends BaseMapper<WorkflowComponent> {
    Integer countRefNodes(@Param("uuid") String uuid);
}
