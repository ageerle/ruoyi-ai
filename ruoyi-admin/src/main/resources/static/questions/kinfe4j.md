# 对接前声明

经常有小伙伴希望可以对接 knife4j

那么这里将介绍如何使用 框架生成的 openapi 对接 knife4j

# 如何对接

**重点声明: 本框架生成标准openapi结构 如对接后遇到不好用等问题 皆与本框架无关**

knife4j 本身提供了独立的文档中间件 可以零成本的介入 openapi

文档地址: https://doc.xiaominfo.com/docs/middleware-sources

**注意: 此组件应单独搞一个boot项目 不要往框架里做任何代码上的更改**

使用文档提供的 Cloud 模式 对接咱们框架的 openapi 地址即可完成对接

![输入图片说明](https://foruda.gitee.com/images/1685953873117929554/22dce56e_1766278.png "屏幕截图")

vue版本对接配置如下: 

```yml
knife4j:
  enable-aggregation: true
  cloud:
    enable: true
    routes:
      - name: 演示模块
        uri: localhost:8080
        location: /v3/api-docs/1.演示模块
      - name: 系统模块
        uri: localhost:8080
        location: /v3/api-docs/2.系统模块
      - name: 代码生成模块
        uri: localhost:8080
        location: /v3/api-docs/3.代码生成模块
```

cloud版本对接配置如下: 

```yml
knife4j:
  enable-aggregation: true
  cloud:
    enable: true
    routes:
      - name: 演示模块
        uri: localhost:8080
        location: /demo/v3/api-docs
      - name: 认证服务
        uri: localhost:8080
        location: /auth/v3/api-docs
      - name: 资源服务
        uri: localhost:8080
        location: /resource/v3/api-docs
      - name: 系统服务
        uri: localhost:8080
        location: /system/v3/api-docs
      - name: 监控服务
        uri: localhost:8080
        location: /monitor/v3/api-docs
      - name: 代码生成服务
        uri: localhost:8080
        location: /gen/v3/api-docs
```