package org.ruoyi.ipd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.ipd.domain.SopTemplateInstance;

/**
 * SOP 模板实例快照 Mapper（P1-3.3）
 * <p>复用 MyBatis-Plus {@link BaseMapper} 提供默认 CRUD；
 * 复杂查询（按 projectId 查 ACTIVE 实例、按 templateId 查 ACTIVE 实例）
 * 走 Service 层 LambdaQueryWrapper 组装。
 */
@Mapper
public interface SopTemplateInstanceMapper extends BaseMapper<SopTemplateInstance> {
}