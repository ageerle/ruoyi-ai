package org.ruoyi.mcp.service;

import lombok.RequiredArgsConstructor;
import org.ruoyi.system.domain.vo.SysUserVo;
import org.ruoyi.system.mapper.SysUserMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

/**
 * @author ageer
 */
@Service
@RequiredArgsConstructor
public class McpCustomService {

	private final SysUserMapper userMapper;

	public record User(String userName, Double userBalance) {
	}

	@Tool(description = "根据用户名称查询用户信息")
	public User getUserBalance(String username) {
		SysUserVo sysUserVo = userMapper.selectUserByUserName(username);
		return new User(sysUserVo.getUserName(),sysUserVo.getUserBalance());
	}

}
