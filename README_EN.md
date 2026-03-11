
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

### Enterprise-Grade AI Assistant Platform

*An out-of-the-box full-stack AI platform supporting multi-agent collaboration, Supervisor mode orchestration, and multiple decision models, with advanced RAG technology and visual workflow orchestration capabilities*

**[中文](README.md)** | **[📖 Documentation](https://doc.pandarobot.chat)** |
**[🚀 Live Demo](https://web.pandarobot.chat)** | **[🐛 Report Issues](https://github.com/ageerle/ruoyi-ai/issues)** | **[💡 Feature Requests](https://github.com/ageerle/ruoyi-ai/issues)**

</div>




## ✨ Core Features

| Module | Current Capabilities | Extension Direction |
|:---:|---|---|
| **Model Management** | Multi-model integration (OpenAI/DeepSeek/Tongyi/Zhipu), multi-modal understanding, Coze/DIFY/FastGPT platform integration | Auto mode, fault tolerance |
| **Knowledge Base** | Local RAG + Vector DB (Milvus/Weaviate) + Knowledge Graph + Document parsing + Reranking | Audio/video parsing, knowledge source |
| **Tool Management** | MCP protocol integration, Skills capability + Extensible tool ecosystem | Tool plugin marketplace, toolAgent auto-loading |
| **Workflow Orchestration** | Visual workflow designer, drag-and-drop node orchestration, SSE streaming execution, currently supports model (with RAG) calls, email sending, manual review nodes | More node types |
| **Multi-Agent** | Agent framework based on Langchain4j, Supervisor mode orchestration, supports multiple decision models | Configurable agents |
| **AI Coding** | Intelligent code analysis, project scaffolding generation, Copilot assistant | Code generation optimization |

## 🚀 Quick Start

### Live Demo

|   Platform   | URL | Account |
|:------:|---|---|
|  User Frontend   | [web.pandarobot.chat](https://web.pandarobot.chat) | admin / admin123 |
| Admin Panel | [admin.pandarobot.chat](https://admin.pandarobot.chat) | admin / admin123 |

### Project Repositories

| Module     | GitHub Repository                                             | Gitee Repository                                             | GitCode Repository                                             |
|----------|-------------------------------------------------------|------------------------------------------------------|--------------------------------------------------------|
| 🔧 Backend  | [ruoyi-ai](https://github.com/ageerle/ruoyi-ai)       | [ruoyi-ai](https://gitee.com/ageerle/ruoyi-ai)       | [ruoyi-ai](https://gitcode.com/ageerle/ruoyi-ai)       |
| 🎨 User Frontend  | [ruoyi-web](https://github.com/ageerle/ruoyi-web)     | [ruoyi-web](https://gitee.com/ageerle/ruoyi-web)     | [ruoyi-web](https://gitcode.com/ageerle/ruoyi-web)     |
| 🛠️ Admin Panel | [ruoyi-admin](https://github.com/ageerle/ruoyi-admin) | [ruoyi-admin](https://gitee.com/ageerle/ruoyi-admin) | [ruoyi-admin](https://gitcode.com/ageerle/ruoyi-admin) |

### Partner Projects
| Project Name           | GitHub Repository                                             | Gitee Repository
|----------------|-------------------------------------------------------|------------------------------------------------------|
| element-plus-x | [element-plus-x](https://github.com/element-plus-x/Element-Plus-X)       | [element-plus-x](https://gitee.com/he-jiayue/element-plus-x)       |

## 🛠️ Technical Architecture

### Core Framework
- **Backend**: Spring Boot 4.0 + Spring AI 2.0 + Langchain4j
- **Data Storage**: MySQL 8.0 + Redis + Vector Databases (Milvus/Weaviate)
- **Frontend**: Vue 3 + Vben Admin + element-plus-x
- **Security**: Sa-Token + JWT dual-layer security


## 📚 Documentation

Want to learn more about installation, deployment, configuration, and secondary development?

**👉 [Complete Documentation](https://doc.pandarobot.chat)**

## 🤝 Contributing

We warmly welcome community contributions! Whether you are a seasoned developer or just getting started, you can contribute to the project 💪

### How to Contribute

1. **Fork** the project to your account
2. **Create a branch** (`git checkout -b feature/new-feature-name`)
3. **Commit your changes** (`git commit -m 'Add new feature'`)
4. **Push to the branch** (`git push origin feature/new-feature-name`)
5. **Create a Pull Request**

> 💡 **Tip**: We recommend submitting PRs to GitHub, we will automatically sync to other code hosting platforms

## 📄 License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

Thanks to the following excellent open-source projects for their support:
- [Spring AI Alibaba Copilot](https://github.com/spring-ai-alibaba/copilot) - Intelligent coding assistant based on spring-ai-alibaba
- [Langchain4j](https://github.com/langchain4j/langchain4j) - Powerful Java LLM development framework
- [RuoYi-Vue-Plus](https://gitee.com/dromara/RuoYi-Vue-Plus) - Mature enterprise-level rapid development framework
- [Vben Admin](https://github.com/vbenjs/vue-vben-admin) - Modern Vue admin template

## 🌐 Ecosystem Partners

- [PPIO Cloud](https://ppinfra.com/user/register?invited_by=P8QTUY&utm_source=github_ruoyi-ai) - Provides cost-effective GPU computing and model API services
- [Youyun Intelligent Computing](https://www.compshare.cn/?ytag=GPU_YY-gh_ruoyi) - Thousands of RTX40 series GPUs + mainstream models API services, second-level response, pay-per-use, free for new customers.

## Outstanding Open-Source Projects and Community Recommendations
- [imaiwork](https://gitee.com/tsinghua-open/imaiwork) - Open-source AI phone, AI customer acquisition phone project, based on accessibility mode and RPA, more powerful than Doubao AI phone.

## 💬 Community Chat

<div align="center">

**[📱 Join Telegram Group](https://t.me/+LqooQAc5HxRmYmE1)**

</div>

---

<div align="center">

**[⭐ Star to Support](https://github.com/ageerle/ruoyi-ai)** • **[Fork to Contribute](https://github.com/ageerle/ruoyi-ai/fork)** • **[📚 中文](README.md)** • **[📖 Complete Documentation](https://doc.pandarobot.chat)**

*Built with ❤️, maintained by the RuoYi AI open-source community*

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
