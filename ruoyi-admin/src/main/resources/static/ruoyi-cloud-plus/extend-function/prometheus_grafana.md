# Prometheus+Grafana搭建
- - -
## 基础搭建

参考文章: https://lionli.blog.csdn.net/article/details/127959009

## 框架内扩展

框架已经包含了 docker-compose 编排 执行如下命令启动容器即可

```shell
docker-compose up -d prometheus grafana
```

## 应用配置

各个服务引入 `ruoyi-common-prometheus` 模块

![输入图片说明](https://foruda.gitee.com/images/1668998415863943539/413dc560_1766278.png "屏幕截图")

修改 `prometheus.yml` 配置采集数据源

![输入图片说明](https://foruda.gitee.com/images/1668998433756761442/bf31c212_1766278.png "屏幕截图")

修改 `Nacos` 地址 与 `SpringBoot-Admin` 监控地址 用于数据采集<br>
如都为本地应用则无需更改

![输入图片说明](https://foruda.gitee.com/images/1668998317973042740/2d3590ec_1766278.png "屏幕截图")

## 导入框架特制模板
**注意: 此处数据源名称必须与图片保持一致 不然会和模板对应不上导致无法读取数据**<br>
![输入图片说明](https://foruda.gitee.com/images/1669866309495145064/1de987ce_1766278.png "屏幕截图")

> 找到框架内的特制模板json文件 在grafana点击上传json文件 导入模板<br>

![输入图片说明](https://foruda.gitee.com/images/1668998149634542527/f0881c8e_1766278.png "屏幕截图")<br>
![输入图片说明](https://foruda.gitee.com/images/1668998179391197847/b1d3a630_1766278.png "屏幕截图")

## 选择查看监控

点击右侧菜单浏览 选择想要查看的监控即可

![输入图片说明](https://foruda.gitee.com/images/1668998515814170229/817ac8b0_1766278.png "屏幕截图")<br>
![输入图片说明](https://foruda.gitee.com/images/1668998567335384306/acdf2833_1766278.png "屏幕截图")<br>
![输入图片说明](https://foruda.gitee.com/images/1668998616894681785/ac27538b_1766278.png "屏幕截图")