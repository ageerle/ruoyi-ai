
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

| Module | Current Capabilities |
|:---:|---|
| **Model Management** | Multi-model integration (OpenAI/DeepSeek/Tongyi/Zhipu), multi-modal understanding, Coze/DIFY/FastGPT platform integration |
| **Knowledge Base** | Local RAG + Vector DB (Milvus/Weaviate/Qdrant) + Document parsing |
| **Tool Management** | MCP protocol integration, Skills capability + Extensible tool ecosystem |
| **Workflow Orchestration** | Visual workflow designer, drag-and-drop node orchestration, SSE streaming execution, currently supports model calls, email sending, manual review nodes |
| **Multi-Agent** | Agent framework based on Langchain4j, Supervisor mode orchestration, supports multiple decision models |

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
| Project Name           | GitHub Repository                                             | Gitee Repository |
|----------------|-------------------------------------------------------|------------------------------------------------------|
| element-plus-x | [element-plus-x](https://github.com/element-plus-x/Element-Plus-X)       | [element-plus-x](https://gitee.com/he-jiayue/element-plus-x)       |

## 🛠️ Technical Architecture

### Core Framework
- **Backend**: Spring Boot 4.0 + Spring AI 2.0 + Langchain4j
- **Data Storage**: MySQL 8.0 + Redis + Vector Databases (Milvus/Weaviate/Qdrant)
- **Frontend**: Vue 3 + Vben Admin + element-plus-x
- **Security**: Sa-Token + JWT dual-layer security


- **Document Processing**: PDF, Word, Excel parsing, intelligent image analysis
- **Real-time Communication**: WebSocket real-time communication, SSE streaming response
- **System Monitoring**: Comprehensive logging system, performance monitoring, service health checks

## 🐳 Docker Deployment

This project provides two Docker deployment methods:

### Method 1: One-click Start All Services (Recommended)

Use `docker-compose-all.yaml` to start all services at once (including backend, admin panel, user frontend, and dependencies):

```bash
# Clone the repository
git clone https://github.com/ageerle/ruoyi-ai.git
cd ruoyi-ai

# Start all services (pull pre-built images from registry)
docker-compose -f docker-compose-all.yaml up -d

# Check service status
docker-compose -f docker-compose-all.yaml ps

# Access services
# Admin Panel: http://localhost:25666 (admin / admin123)
# User Frontend: http://localhost:25137
# Backend API: http://localhost:26039
```

### Method 2: Step-by-step Deployment (Source Build)

If you need to build backend services from source, follow these steps:

#### Step 1: Deploy Backend Service

```bash
# Enter backend project directory
cd ruoyi-ai

# Start backend service (build from source)
docker-compose up -d --build

# Wait for backend service to start
docker-compose logs -f backend
```

#### Step 2: Deploy Admin Panel

```bash
# Enter admin panel project directory
cd ruoyi-admin

# Build and start admin panel
docker-compose up -d --build

# Access admin panel
# URL: http://localhost:5666
```

#### Step 3: Deploy User Frontend (Optional)

```bash
# Enter user frontend project directory
cd ruoyi-web

# Build and start user frontend
docker-compose up -d --build

# Access user frontend
# URL: http://localhost:5137
```

### Service Ports

| Service | One-click Port | Step-by-step Port | Description |
|------|-------------|-------------|------|
| Admin Panel | 25666 | 5666 | Admin backend access |
| User Frontend | 25137 | 5137 | User frontend access |
| Backend Service | 26039 | 6039 | Backend API service |
| MySQL | 23306 | 23306 | Database service |
| Redis | 26379 | 6379 | Cache service |
| Weaviate | 28080 | 28080 | Vector database |
| MinIO API | 29000 | 9000 | Object storage API |
| MinIO Console | 29090 | 9090 | Object storage console |

### Image Registry

All images are hosted on Alibaba Cloud Container Registry:

```
crpi-31mraxd99y2gqdgr.cn-beijing.personal.cr.aliyuncs.com/ruoyi_ai
```

Available images:
- `mysql:v3` - MySQL database (includes initialization SQL)
- `redis:6.2` - Redis cache
- `weaviate:1.30.0` - Vector database
- `minio:latest` - Object storage
- `ruoyi-ai-backend:latest` - Backend service
- `ruoyi-ai-admin:latest` - Admin frontend
- `ruoyi-ai-web:latest` - User frontend

### Common Commands

```bash
# Stop all services
docker-compose -f docker-compose-all.yaml down

# View service logs
docker-compose -f docker-compose-all.yaml logs -f [service-name]

# Restart a service
docker-compose -f docker-compose-all.yaml restart [service-name]
```

## 📚 Documentation

Want to learn more about installation, deployment, configuration, and secondary development?

**👉 [Complete Documentation](https://doc.pandarobot.chat)**

Experiencing issues with knowledge base or RAG responses?

**👉 [RAG Troubleshooting Guide](docs/troubleshooting/rag-failures.md)**

---

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


## 💬 Community Chat

<div align="center">

**[📱 Join Telegram Group](
https://t.me/+LqooQAc5HxRmYmE1)**

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
