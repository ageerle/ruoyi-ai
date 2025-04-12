package org.ruoyi.mcp.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author ageer
 */
@Service
public class McpCustomService {


	public record Book(List<String> isbn, String title, List<String> authorName) {
	}

	@Tool(description = "Get list of Books by title")
	public List<Book> getBooks(String title) {
		// 这里模拟查询DB操作
		return List.of(new Book(List.of("ISBN-888"), "SpringAI教程", List.of("熊猫助手写的书")));
	}

}
