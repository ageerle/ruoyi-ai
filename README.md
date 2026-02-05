# RuoYi AI

<div align="center">

[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![MIT License][license-shield]][license-url]


<p align="center">
  <a href="https://trendshift.io/repositories/13209">
    <img src="https://trendshift.io/api/badge/repositories/13209" alt="GitHub Trending">
  </a>
</p>

<img src="docs/image/logo.png" alt="RuoYi AI Logo" width="120" height="120">

## 功能建议&bug提交：【腾讯文档】
https://docs.qq.com/sheet/DR3hoR3FVVkpJcnVm

### 企业级AI助手平台

*开箱即用的全栈AI平台，集成Coze、DIFY等主流AI平台，提供先进的RAG技术、知识图谱、数字人和AI流程编排能力*

**[English](README_EN.md)** | **[📖 使用文档](https://doc.pandarobot.chat)** |
**[🚀 在线体验](https://web.pandarobot.chat)** | **[🐛 问题反馈](https://github.com/ageerle/ruoyi-ai/issues)** | **[💡 功能建议](https://github.com/ageerle/ruoyi-ai/issues)**

</div>




## ✨ 核心亮点

### 智能AI引擎
- **多模型接入**：支持 OpenAI、DeepSeek、通义千问、智谱AI 等主流厂商的模型
- **多模态理解**：支持文本、图片、文档等多种格式的智能处理
- **AI平台集成**：集成了 **扣子(Coze)**、**DIFY**、**FastGPT** 等主流AI应用平台
- **MCP能力集成**：基于模型上下文协议，打造可扩展的AI工具生态系统
- **AI编程助手**：内置智能代码分析和项目脚手架生成能力

### 本地化RAG方案
- **私有知识库**：基于 Langchain4j 框架 + BGE-large-zh-v1.5 中文向量模型实现本地私有知识库
- **多种向量库**：支持 Milvus、Weaviate、Qdrant 等主流向量数据库
- **数据安全可控**：支持完全本地部署，保护企业数据隐私
- **灵活模型部署**：兼容 Ollama、vLLM 等本地推理框架

### AI创作工具
- **AI 绘画创作**： 集成 MidJourney、GPT-4o-image
- **智能PPT生成**：一键将文本内容转换为精美演示文稿

### 知识图谱与智能编排
- **知识图谱构建**：自动从文档和对话中提取实体关系，构建可视化知识网络
- **AI 流程编排**：可视化工作流设计器，支持复杂AI任务的编排和自动化执行
- **数字人交互**：集成数字人形象，提供更自然的人机交互体验

## 🚀 快速体验

### 在线演示

- **用户端体验**：[web.pandarobot.chat](https://web.pandarobot.chat) (账号：admin 密码：admin123)
- **管理后台**：[admin.pandarobot.chat](https://admin.pandarobot.chat) (账号：admin 密码：admin123)

### 项目源码

| 项目模块     | GitHub 仓库                                             | Gitee 仓库                                             | GitCode 仓库                                             |
|----------|-------------------------------------------------------|------------------------------------------------------|--------------------------------------------------------|
| 🔧 后端服务  | [ruoyi-ai](https://github.com/ageerle/ruoyi-ai)       | [ruoyi-ai](https://gitee.com/ageerle/ruoyi-ai)       | [ruoyi-ai](https://gitcode.com/ageerle/ruoyi-ai)       |
| 🎨 用户前端  | [ruoyi-web](https://github.com/ageerle/ruoyi-web)     | [ruoyi-web](https://gitee.com/ageerle/ruoyi-web)     | [ruoyi-web](https://gitcode.com/ageerle/ruoyi-web)     |
| 🛠️ 管理后台 | [ruoyi-admin](https://github.com/ageerle/ruoyi-admin) | [ruoyi-admin](https://gitee.com/ageerle/ruoyi-admin) | [ruoyi-admin](https://gitcode.com/ageerle/ruoyi-admin) |

## 🛠️ 技术架构

### 核心框架
- **后端架构**：Spring Boot 3.5 + Langchain4j
- **数据存储**：MySQL 8.0 + Redis + 向量数据库（Milvus/Weaviate/Qdrant）
- **前端技术**：Vue 3 + Vben Admin + Element UI
- **安全认证**：Sa-Token + JWT 双重保障

### 系统组件
- **文档处理**：PDF、Word、Excel 解析，图像智能分析
- **实时通信**：WebSocket 实时通信，SSE 流式响应
- **系统监控**：完善的日志体系、性能监控、服务健康检查

## 📚 使用文档

想要深入了解安装部署、功能配置和二次开发？

**👉 [完整使用文档](https://doc.pandarobot.chat)**

## 🤝 参与贡献

我们热烈欢迎社区贡献！无论您是资深开发者还是初学者，都可以为项目贡献力量 💪

### 贡献方式

1. **Fork** 项目到您的账户
2. **创建分支** (`git checkout -b feature/新功能名称`)
3. **提交代码** (`git commit -m '添加某某功能'`)
4. **推送分支** (`git push origin feature/新功能名称`)
5. **发起 Pull Request**

> 💡 **小贴士**：建议将 PR 提交到 GitHub，我们会自动同步到其他代码托管平台

## 📄 开源协议

本项目采用 **MIT 开源协议**，详情请查看 [LICENSE](LICENSE) 文件。

## 🙏 特别鸣谢

感谢以下优秀的开源项目为本项目提供支持：
- [Spring AI Alibaba Copilot](https://github.com/spring-ai-alibaba/copilot) - 基于spring-ai-alibaba
  的智能编码助手
- [Langchain4j](https://github.com/langchain4j/langchain4j) - 强大的 Java LLM 开发框架
- [RuoYi-Vue-Plus](https://gitee.com/dromara/RuoYi-Vue-Plus) - 成熟的企业级快速开发框架
- [Vben Admin](https://github.com/vbenjs/vue-vben-admin) - 现代化的 Vue 后台管理模板

## 🌐 生态伙伴

- [PPIO 派欧云](https://ppinfra.com/user/register?invited_by=P8QTUY&utm_source=github_ruoyi-ai) - 提供高性价比的 GPU
  算力和模型 API 服务
- [优云智算](https://www.compshare.cn/?ytag=GPU_YY-gh_ruoyi) - 万卡RTX40系GPU+海内外主流模型API服务，秒级响应，按量计费，新客免费用。

## 优秀开源项目及社区推荐
- [imaiwork](https://gitee.com/tsinghua-open/imaiwork) - AI手机开源版，AI获客手机项目，基于无障碍模式，RPA，比豆包AI手机更强大。

## 💬 社区交流

<div align="center">

<table>
<tr>
<td align="center">
<img src="docs/image/wx.png" alt="微信二维码" width="200" height="200"><br>
<strong>扫码添加作者微信</strong><br>
<em>邀请进群学习</em>
</td>
<td align="center">
<img src="docs/image/qq.png" alt="QQ群二维码" width="200" height="200"><br>
<strong>QQ技术交流群</strong><br>
<em>技术讨论</em>
</td>

</tr>
</table>

</div>

---

<div align="center">

**[⭐ 点个Star支持一下](https://github.com/ageerle/ruoyi-ai)** • **[ Fork 开始贡献](https://github.com/ageerle/ruoyi-ai/fork)** • **[📚 English](README_EN.md)** • **[📖 查看完整文档](https://doc.pandarobot.chat)**

*用 ❤️ 打造，由 RuoYi AI 开源社区维护*

</div>

<!-- Badge Links -->

[contributors-shield]: https://img.shields.io/github/contributors/ageerle/ruoyi-ai.svg?style=flat-square

[contributors-url]: https://github.com/ageerle/ruoyi-ai/graphs/contributors

[forks-shield]: https://img.shields.io/github/forks/ageerle/ruoyi-ai.svg?style=flat-square

[forks-url]: https://github.com/ageerle/ruoyi-ai/network/members

[stars-shield]: https://img.shields.io/github/stars/ageerle/ruoyi-ai.svg?style=flat-square

[stars-url]: https://github.com/ageerle/ruoyi-ai/stargazers

[issues-shield]: https://img.shields.io/github/issues/ageerle/ruoyi-ai.svg?style=flat-square

[issues-url]: https://github.com/ageerle/ruoyi-ai/issues

[license-shield]: https://img.shields.io/github/license/ageerle/ruoyi-ai.svg?style=flat-square

[license-url]: https://github.com/ageerle/ruoyi-ai/blob/main/LICENSE
