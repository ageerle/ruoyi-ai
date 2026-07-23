package org.ruoyi.mapper.agent;

import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.domain.entity.agent.Agent;
import org.ruoyi.domain.vo.agent.AgentVo;
/**
 * 智能体信息 Mapper
 *
 * @author ruoyi team
 */
@Mapper
public interface AgentMapper extends BaseMapperPlus<Agent, AgentVo> {
}
