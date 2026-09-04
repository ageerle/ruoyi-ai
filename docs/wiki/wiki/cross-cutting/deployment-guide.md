---
topic: cross-cutting/deployment-guide
title: 部署指南
updated: 2026-09-04
raw:
  - raw/project-skeleton/application-yml.md
  - raw/docker-source/docker-compose-all.md
  - raw/docker-source/docker-compose.md
  - raw/docker-source/dockerfile-admin.md
  - raw/extend-source/monitor-admin-application.md
  - raw/extend-source/snailjob-server-application.md
---

# 部署指南

RuoYi-AI 提供 3 种部署方式：**Docker Compose 一键启动**（推荐）、**分模块源码构建**、**传统外部 Tomcat war 包**。

参见：[readme-md.md § Docker Deployment](../raw/project-skeleton/readme-md.md)、[pom-xml.md](../raw/project-skeleton/pom-xml.md)。

## 方式一：一键启动（推荐）

适合开发 / 测试 / 小规模生产。`docker-compose-all.yaml` 包含 backend + admin + web + 全部依赖。

参见：[docker-compose-all](../raw/docker-source/docker-compose-all)。

```bash
# Requirements: Docker Engine + Docker Compose V2

# 克隆特定版本
git clone --depth 1 --branch v3.1.0 https://github.com/ageerle/ruoyi-ai.git
cd ruoyi-ai

# 准备 .env（默认账号密码）
cp docs/docker/ruoyi-ai/.env.example docs/docker/ruoyi-ai/.env
sed -i 's/^RUIYI_VERSION=.*/RUIYI_VERSION=v3.1.0/' docs/docker/ruoyi-ai/.env

# 拉镜像 + 启动
docker compose --env-file docs/docker/ruoyi-ai/.env \
  -f docs/docker/ruoyi-ai/docker-compose-all.yaml pull
docker compose --env-file docs/docker/ruoyi-ai/.env \
  -f docs/docker/ruoyi-ai/docker-compose-all.yaml up -d

# 检查
docker compose --env-file docs/docker/ruoyi-ai/.env \
  -f docs/docker/ruoyi-ai/docker-compose-all.yaml ps
```

### 默认端口

| 服务 | 端口 | 用途 |
|---|---|---|
| Admin Panel | `25666` | 管理后台 |
| User Frontend | `25137` | 用户端 |
| Backend API | `26039` | 后端 API |
| MySQL | `23306` | 数据库 |
| Redis | `26379` | 缓存 |
| Weaviate | `28080` | 向量库 |
| MinIO API | `29000` | 对象存储 |
| MinIO Console | `29090` | 管理控制台 |

### 升级

```bash
sed -i 's/^RUIYI_VERSION=.*/RUIYI_VERSION=v3.2.0/' docs/docker/ruoyi-ai/.env
docker compose --env-file docs/docker/ruoyi-ai/.env \
  -f docs/docker/ruoyi-ai/docker-compose-all.yaml pull
docker compose --env-file docs/docker/ruoyi-ai/.env \
  -f docs/docker/ruoyi-ai/docker-compose-all.yaml up -d
```

**警告**：`docker compose down -v` 会**删除数据卷**——不要随便加 `-v`。

参见：[docker-compose](../raw/docker-source/docker-compose)。

## 方式二：分模块源码构建

适合需要自定义后端或参与开发的场景。

### Step 1：启动后端

```bash
cd ruoyi-ai
docker compose up -d --build
docker compose logs -f backend
```

参见：[dockerfile-admin](../raw/docker-source/dockerfile-admin)。

### Step 2：启动 Admin Panel

```bash
cd ruoyi-admin
docker compose up -d --build
# 访问 http://localhost:5666
```

### Step 3：启动 User Frontend

```bash
cd ruoyi-web
docker compose up -d --build
# 访问 http://localhost:5137
```

## 方式三：外部 Tomcat war 包

参见：[pom-xml.md § maven-war-plugin](../raw/project-skeleton/pom-xml.md) + [RuoYiAIServletInitializer](../raw/admin-source/ruoyi-ai-servlet-initializer)。

```bash
mvn clean package -DskipTests
# 产物：ruoyi-admin/target/ruoyi-admin.war
# 部署到外部 Tomcat 的 webapps/
```

## 三种 Profile 切换

参见 [application-yml.md § spring.profiles.active](../raw/project-skeleton/application-yml.md)。

```bash
# dev（默认）
java -jar ruoyi-admin.jar --spring.profiles.active=dev

# prod
java -jar ruoyi-admin.jar --spring.profiles.active=prod
```

| Profile | `demo.enabled` | 日志级别 | 用途 |
|---|---|---|---|
| `dev` | `true`（默认） | info | 开发 |
| `prod` | `false` | warn | 生产 |
| `local` | `true` | info | 本地 |

参见：[claude-md.md § Build & Run](../raw/project-skeleton/claude-md.md)。

## 多 Profile 配置差异

| 开关 | dev | prod |
|---|---|---|
| `demo.enabled` | true（拦截写操作） | false |
| `logging.level.org.ruoyi` | info | info |
| `logging.level.org.springframework` | warn | warn |
| `monitor.username/password` | ruoyi/123456 | ruoyi/123456（**生产必改**） |

## 配套独立服务

### ruoyi-monitor-admin（Spring Boot Admin）

参见：[monitor-admin-application](../raw/extend-source/monitor-admin-application)。

独立的 Spring Boot Admin 服务，监控后端 + admin 的健康 / 日志 / metrics。

### ruoyi-snailjob-server（分布式调度）

参见：[snailjob-server-application](../raw/extend-source/snailjob-server-application)。

独立的 SnailJob server，托管分布式定时任务（与 `ruoyi-common-job` 客户端配合）。

**重要**：这两个 `ruoyi-extend/*` 是**独立 Spring Boot 应用**，与 `ruoyi-admin` 分开部署。

## 环境变量 / 密钥管理

参见 [application-yml.md](../raw/project-skeleton/application-yml.md) 的 `${XXX:default}` 模式：

```yaml
chat:
  default-model: deepseek-v4-flash
workflow:
  web-search:
    zhipu:
      api-key: ${ZAI_API_KEY:}
short-drama:
  composition:
    ffmpeg-path: ${FFMPEG_PATH:ffmpeg}
```

**生产部署必须通过环境变量覆盖**：

- `ZAI_API_KEY` ——智智 API key
- `FFMPEG_PATH` ——ffmpeg 路径
- `SPRING_PROFILES_ACTIVE` ——profile
- MySQL / Redis / MinIO 密码

**禁止**把密钥写到 `application-prod.yml` 字面量里（参见 [cross-cutting/security.md § 密钥管理](../cross-cutting/) 的 hook 拦截规则）。

## 故障排查 Checklist

| 现象 | 排查点 |
|---|---|
| 启动失败端口被占 | 检查 `killPortProcess(6039)` 是否执行；或 `-Dserver.port=xxxx` 换端口 |
| `demo.enabled=true` 拦截写操作 | dev 环境默认；生产应设 `false` |
| 向量库连接失败 | 检查 `vector-store.type` 与 docker compose 内服务类型一致 |
| 多租户串数据 | 检查新表是否在 `tenant.excludes` 登记 |
| 异步任务报错 | 大概率是 `TenantHelper` 没在异步线程 set |
| JWT 鉴权失败 | 检查 `sa-token.jwt-secret-key` 生产环境是否替换 |
| Spring Boot Admin 不显示应用 | 检查 `spring-boot-admin-client` 配置和 server 地址 |
| SnailJob 任务不执行 | 检查 client / server 之间的 RPC 连接和 namespace |