package org.ruoyi.ipd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.ipd.domain.RequirementChange;

/**
 * P2-6.1 需求变更单 mapper（requirement_changes，BR-GATE-07）。
 *
 * <p>承载双签否决对象：AC-REQ-08 需求转需求变更单 ⇒ 可生成，走双签否决。
 */
@Mapper
public interface RequirementChangeMapper extends BaseMapper<RequirementChange> {
}
