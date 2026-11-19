package org.ruoyi.chat.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.chat.domain.Agent;
import org.ruoyi.chat.domain.vo.agent.AgentVo;
/**
 * 智能体信息 Mapper
 *
 * @author ruoyi team
 */
@Mapper
public interface AgentMapper extends BaseMapperPlus<Agent, AgentVo> {
}
