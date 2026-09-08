package org.ruoyi.ipd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.ipd.domain.SopTemplate;

/**
 * SOP 模板主表 Mapper（P1-3.3）
 * <p>复用 MyBatis-Plus {@link BaseMapper} 提供默认 CRUD；
 * 复杂查询（按 templateCode 查当前 PUBLISHED）走 Service 层 LambdaQueryWrapper 组装。
 */
@Mapper
public interface SopTemplateMapper extends BaseMapper<SopTemplate> {
}