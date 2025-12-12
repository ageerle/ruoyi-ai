## RuoYi-AI 后端部署教程（Docker 部署版）

### 一、前置条件

在部署前，请确保系统已满足以下条件：

#### ✅ 系统环境要求

- 操作系统：Linux / MacOS（推荐 Linux 服务器）
- CPU：4 核以上
- 内存：≥ 4GB
- 磁盘空间：≥ 10GB（建议 20GB+）

#### ✅ 已安装软件

- **Docker**
- **Docker Compose**

验证命令是否可用：

```
docker -v
docker compose version
```

若无输出或提示“command not found”，请先安装 Docker 及 Compose。

------

### 二、目录结构配置

#### 1️⃣ 创建部署目录

在目标服务器执行以下命令：

```
# 第一级目录
mkdir /ruoyi-ai
cd /ruoyi-ai

# 第二级目录
mkdir deploy
cd deploy

# 第三级目录
mkdir data mysql-init

# 第四级目录
mkdir logs minio minio-config mysql redis weaviate
```

> 💡 `data` 目录用于挂载容器运行期间生成的数据文件。

最终目录结构示例：

```
/ruoyi-ai
 └── deploy
     ├── data/
     ├── mysql-init/
     ├── logs/
     ├── minio/
     ├── minio-config/
     ├── mysql/
     ├── redis/
     ├── weaviate/
```

------

### 三、上传配置文件

将以下配置文件上传到 `/ruoyi-ai/deploy` 目录：

- `docker-compose.yaml`
- `.env`
- `ruoyi-ai.sql`
- `Dockerfile`

> 📂 这些文件在项目目录 `/script/deploy/deploy` 下。
> 上传后请检查文件路径是否与上方目录结构一致。

------

### 四、构建 Jar 包

1. 打开 IDEA 或其他构建工具
2. 选择 **Maven 构建配置**，勾选 `prod` 环境，取消 `dev` 环境
3. 点击 `package` 进行打包
4. **注意：** 在构建前请将 `application-prod.yml` 拖入
   `ruoyi-admin/src/main/resources` 目录中

构建完成后会在：

```
ruoyi-admin/target/ruoyi-admin.jar
```

生成打包文件。

------

### 五、上传 Jar 包至服务器

将生成的 `ruoyi-admin.jar` 上传到服务器 `/ruoyi-ai/deploy` 目录下。
确保与 `Dockerfile` 同目录。

------

### 六、构建 Docker 镜像

`Dockerfile` 内容如下：

```
FROM openjdk:17-jdk

RUN mkdir -p /ruoyi/server/logs \
    /ruoyi/server/temp

WORKDIR /ruoyi/server
COPY ruoyi-admin.jar ruoyi-admin.jar

ENTRYPOINT ["java","-jar","ruoyi-admin.jar"]
```

在 `/ruoyi-ai/deploy` 目录执行以下命令：

```
# 构建镜像
docker build -t ruoyi-ai-backend:v20251013 .

# 查看镜像是否构建成功
docker image ls
```

然后在 `docker-compose.yaml` 文件中，将对应服务的镜像名修改为：

```
image: ruoyi-ai-backend:v20251013
```

------

### 七、启动容器服务

在启动前请确认：

- `.env` 中端口号、数据库密码、环境变量已正确配置
- `docker-compose.yaml` 中 MySQL 的端口已开放（用于导入数据）

如示例：

```
ports:
  - "3306:3306"
```

#### 启动命令：

```
cd /ruoyi-ai/deploy
docker compose up -d
```

#### 查看运行状态：

```
docker compose ps
```

#### 查看日志：

```
docker logs -f <容器名称>
```

> ⚠️ 初次启动时可仅运行 `ruoyi-admin`（后端）模块，将前端 `ruoyi-web` 服务暂时注释，确认后端服务正常后再启用前端容器。

------

### 八、数据库初始化

启动 MySQL 容器后，执行以下操作：

```
docker exec -it <mysql_container_name> bash
mysql -uroot -p
source /docker-entrypoint-initdb.d/ruoyi-ai.sql;
```

或手动在客户端中导入 `/ruoyi-ai/deploy/ruoyi-ai.sql` 文件。

------

### 九、常用 Docker 命令

| 功能        | 命令                                |
|-----------|-----------------------------------|
| 查看容器状态    | `docker ps -a`                    |
| 查看日志      | `docker logs -f <容器名>`            |
| 停止服务      | `docker compose down`             |
| 重启服务      | `docker compose restart`          |
| 重新构建镜像    | `docker compose build --no-cache` |
| 清理无用镜像/容器 | `docker system prune -a`          |

------

### 🔍 十、部署验证

1. 检查容器是否全部启动成功：

   ```
   docker compose ps
   ```

2. 访问后端接口：

   ```
   http://<服务器IP>:<后端端口>
   ```

3. 检查日志输出无异常。