# ELK搭建
- - -
# 环境搭建

项目内置 `ELK` 的 `docker-compose` 编排 可查看 `/docker/docker-compose.yml` 文件下方扩展编排

**注意: `/docker/elk/elasticsearch/` 目录下所有文件夹 均需要写权限**

`chmod 777 /docker/elk/elasticsearch/data`<br>
`chmod 777 /docker/elk/elasticsearch/logs`<br>
`chmod 777 /docker/elk/elasticsearch/plugins`<br>
**注意: es插件需要解压后放入 `plugins` 目录**

# 运行命令

```shell
docker-compose up -d elasticsearch kibana logstash
```

# 参考文章
[docker-compose 搭建 ELK 7.X 并整合 SpringBoot](https://lionli.blog.csdn.net/article/details/125743132)

# 项目内配置

服务引入依赖项

```xml
<!-- ELK 日志收集 -->
<dependency>
    <groupId>com.ruoyi</groupId>
    <artifactId>ruoyi-common-logstash</artifactId>
</dependency>
```

更改主 `pom` 文件 `logstash.address` 地址<br>

![输入图片说明](https://foruda.gitee.com/images/1678981534923588112/ba6cb5b7_1766278.png "屏幕截图")
