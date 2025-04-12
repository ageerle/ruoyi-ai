# ES搜索引擎
- - -
## 环境搭建(如果已经搭建了ELK则跳过)

项目内置 `ELK` 的 `docker-compose` 编排 可查看 `/docker/docker-compose.yml` 文件下方扩展编排

**注意: `/docker/elk/elasticsearch/` 目录下所有文件夹 均需要写权限**

`chmod 777 /docker/elk/elasticsearch/data`<br>
`chmod 777 /docker/elk/elasticsearch/logs`<br>
`chmod 777 /docker/elk/elasticsearch/plugins`<br>
**注意: es插件需要解压后放入 `plugins` 目录**

## 运行命令

```shell
docker-compose up -d elasticsearch
```

## Easy-ES 文档
[Easy-ES 文档](https://www.easy-es.cn/)

## 用法

基本配置和用法可参考 `ruoyi-demo` 模块 更多高级用法请参考 Easy-ES 文档<br>
![输入图片说明](https://foruda.gitee.com/images/1660030085169129908/屏幕截图.png "屏幕截图.png")
