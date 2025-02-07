# SkyWalking搭建与集成
- - -
## 服务搭建
参考文章: [SpringBoot 整合 SkyWalking 8.X (包含 Logback 日志采集)](https://lionli.blog.csdn.net/article/details/127656534)

框架已经包含了 docker-compose 编排 执行如下命令启动容器即可

```shell
docker-compose up -d elasticsearch sky-oap sky-ui
```

### 本地开发使用
参考上方文章

### docker部署使用
上传探针到服务器 `/docker/skywalking/agent` 目录<br>
**不要使用网上下载的 请使用框架自带的 内含一些官网没有的插件**<br>
![输入图片说明](https://foruda.gitee.com/images/1667453098143152651/f1b4f492_1766278.png "屏幕截图")

在对应服务的`dockerfile`内 打开 `skywalking` 相关参数注释<br>
![输入图片说明](https://foruda.gitee.com/images/1667452514896786032/f4322fb9_1766278.png "屏幕截图")

服务编排增加探针路径映射<br>
![输入图片说明](https://foruda.gitee.com/images/1667453276389844864/7e139aa9_1766278.png "屏幕截图")


### 对接日志推送(不推荐 建议使用ELK收集日志)

框架已经封装好了对应的依赖和配置 在服务内添加如下依赖

```xml
<!-- skywalking 日志收集 -->
<dependency>
    <groupId>com.ruoyi</groupId>
    <artifactId>ruoyi-common-skylog</artifactId>
</dependency>
```

在 `logback.xml` 日志配置文件内引入 `skylog` 配置文件

![输入图片说明](https://foruda.gitee.com/images/1667452697748002725/a18212cd_1766278.png "屏幕截图")