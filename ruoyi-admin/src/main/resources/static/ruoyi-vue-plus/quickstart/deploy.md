# 应用部署
- - -
## 版本 >= 4.3.0

### 请优先阅读 [idea环境配置](/ruoyi-vue-plus/quickstart/idea_environment.md)

## 手动部署

在服务器安装 `mysql` `redis` `nginx` `minio`

将项目内 `script/docker/nginx/nginx.conf` 配置文件 复制到 `nginx` 配置内<br>
将项目内 `script/docker/redis/redis.conf` 配置文件 复制到 `redis` 配置内

并修改相关参数如 `前端页面存放位置` `后端Ip地址` 等使其生效

jar包部署后端服务 打包命令如下

3.2.0及以上
```mvn
mvn clean package -D maven.test.skip=true -P prod
```
服务器需创建临时文件存储目录与配置文件对应(无此目录上传文件会报错)

![输入图片说明](https://foruda.gitee.com/images/1659951373949149804/屏幕截图.png "屏幕截图.png")

前端参考下方前端部署章节

## 部署视频

[RuoYi-Vue-Plus 5.0 生产环境搭建部署](https://www.bilibili.com/video/BV1mL411e7ha/)

## docker 后端部署

### 请优先阅读 [idea环境配置](/ruoyi-vue-plus/quickstart/idea_environment.md)

**重点: 一知半解的必看**
> [docker安装](https://lionli.blog.csdn.net/article/details/83153029)<br>
> [docker-compose安装](https://lionli.blog.csdn.net/article/details/111220320)<br>
> [docker网络模式讲解](https://lionli.blog.csdn.net/article/details/109603785)<br>
> [docker 开启端口 2375 供外部程序访问](https://lionli.blog.csdn.net/article/details/92627962)

### 将配置使用FTP上传到根目录
idea拖拽文件到远程目录即可上传

![输入图片说明](https://foruda.gitee.com/images/1662109450908169859/eaac9299_1766278.png "屏幕截图")

### 给docker分配文件夹权限
**重点注意: 一定要确保目录 `/docker` 及其所有子目录 具有写权限 如果后续出现权限异常问题 重新执行一遍分配权限**

![输入图片说明](https://foruda.gitee.com/images/1662109847279259882/3a2202c1_1766278.png "屏幕截图")
```shell
chmod -R 777 /docker
```
### 构建应用镜像

**1.需要先使用maven打包成jar包**

![输入图片说明](https://foruda.gitee.com/images/1662110477410977621/c6931c42_1766278.png "屏幕截图")

**2.执行构建**
> 项目初始化后会自动生成构建镜像的运行配置<br>
> 配置好docker连接之后 运行如下即可构建对应的应用镜像

**重点注意: idea2024及以上版本要求必须在本地安装docker才可以执行如下操作**

![输入图片说明](https://foruda.gitee.com/images/1662110192257483752/0f754b47_1766278.png "屏幕截图")

![输入图片说明](https://foruda.gitee.com/images/1662120004773449909/9fdef59c_1766278.png "屏幕截图")

**3.结构讲解**
右键编辑 即可看到内部配置

![输入图片说明](https://foruda.gitee.com/images/1662458355500139498/eaa26036_1766278.png "屏幕截图")

![输入图片说明](https://foruda.gitee.com/images/1662458446794722159/32c086a7_1766278.png "屏幕截图")


### 创建基础服务

```shell
docker-compose up -d mysql nginx-web redis minio
```

### 创建业务服务(需要先构建服务镜像)

4.X
```shell
docker-compose up -d ruoyi-monitor-admin ruoyi-xxl-job-admin ruoyi-server1 ruoyi-server2
```

5.X
```shell
docker-compose up -d ruoyi-monitor-admin ruoyi-snailjob-server ruoyi-server1 ruoyi-server2
```

### docker其他操作(idea的docker插件 推荐使用)
![输入图片说明](https://foruda.gitee.com/images/1662458271941863770/cd180a04_1766278.png "屏幕截图")

## 前端部署

执行打包命令
```shell
# 打包正式环境
npm run build:prod
```
打包后生成打包文件在 `ruoyi-ui/dist` 目录
将 `dist` 目录下文件(不包含 `dist` 目录) 上传到部署服务器 `docker/nginx/html` 目录下(手动部署放入自己配置的路径即可)

![输入图片说明](https://foruda.gitee.com/images/1662110914769648699/07f344c4_1766278.png "屏幕截图")

重启 `nginx` 服务即可


### 如需更改后端代理路径或者后端ip地址的话往下看

更改`nginx.conf`配置文件代理路径(注意: /开头/结尾)

![输入图片说明](https://foruda.gitee.com/images/1660185698211067202/屏幕截图.png "屏幕截图.png")

更改前端`.env.环境` 文件内的 `VITE_APP_BASE_API`

![输入图片说明](https://foruda.gitee.com/images/1724318035232137124/5d035a09_1766278.png "屏幕截图")

更改`nginx.conf`配置文件后端ip地址

![输入图片说明](https://foruda.gitee.com/images/1660185711265558730/屏幕截图.png "屏幕截图.png")
