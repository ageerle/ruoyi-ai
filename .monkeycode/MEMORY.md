# 用户指令记忆

本文件记录了用户的指令、偏好和教导，用于在未来的交互中提供参考。

## 格式

### 用户指令条目
用户指令条目应遵循以下格式：

[用户指令摘要]
- Date: [YYYY-MM-DD]
- Context: [提及的场景或时间]
- Instructions:
  - [用户教导或指示的内容，逐行描述]

### 项目知识条目
Agent 在任务执行过程中发现的条目应遵循以下格式：

[项目知识摘要]
- Date: [YYYY-MM-DD]
- Context: Agent 在执行 [具体任务描述] 时发现
- Category: [代码结构|代码模式|代码生成|构建方法|测试方法|依赖关系|环境配置]
- Instructions:
  - [具体的知识点，逐行描述]

## 去重策略
- 添加新条目前，检查是否存在相似或相同的指令
- 若发现重复，跳过新条目或与已有条目合并
- 合并时，更新上下文或日期信息
- 这有助于避免冗余条目，保持记忆文件整洁

## 条目

[项目技术栈偏好：LangChain4j]
- Date: 2026-05-03
- Context: 用户在讨论“集成 AI 编程能力”方案时说明
- Category: 依赖关系
- Instructions:
  - 项目基于 LangChain4j 设计仓库级理解与自动化动作能力

[需求澄清：构建通用 Coding Agent]
- Date: 2026-05-03
- Context: 用户纠正方案范围时说明
- Category: 代码结构
- Instructions:
  - 目标是基于 LangChain4j 构建通用 coding agent，而非仅项目内问答助手
  - Agent 需要可操作文件、调用工具、跨前后端完成任务（如新建前端页面并对接现有后端）

[内置工具自动注册机制]
- Date: 2026-05-03
- Context: Agent 在执行 coding agent 工具扩展时发现
- Category: 代码结构
- Instructions:
  - ruoyi-chat 模块通过 BuiltinToolProvider + @Component 自动发现并注册内置工具
  - 新增工具无需手工改注册表，BuiltinToolRegistry 会在启动时扫描并创建可供 Agent 调用的实例

[新增能力方向：AI 报表 Agent]
- Date: 2026-05-03
- Context: 用户提出新的产品化需求
- Category: 代码模式
- Instructions:
  - 用户希望通过自然语言生成报表，包含数据库查询和 HTML 报表生成
  - 用户希望在报表页面内通过提示词继续动态编辑页面
