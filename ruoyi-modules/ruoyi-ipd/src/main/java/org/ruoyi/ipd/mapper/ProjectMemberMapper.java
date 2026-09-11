package org.ruoyi.ipd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.ipd.domain.ProjectMember;

/**
 * P4-1.1 双 PM 路由只读查询（BR-REQ-04）：产品 1:1 项目，取该项目在职 MARKET_PM / RD_PM。
 */
@Mapper
public interface ProjectMemberMapper extends BaseMapper<ProjectMember> {
}
