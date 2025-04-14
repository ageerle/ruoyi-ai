package org.ruoyi.mcp.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

/**
 * @author ageer
 */
@Service
public class McpCustomService {

	public record User(String userName, String userBalance) {
	}

	@Tool(description = "根据用户名称查询用户信息")
	public User getUserBalance(String username) {
		return new User("admin","99.99");
	}

}
