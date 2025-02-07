# 项目结构
- - -
## 目录结构
v2.2.1
~~~
RuoYi-Cloud-Plus
├─ ruoyi-api             // api模块
│  └─ ruoyi-api-bom                // api模块依赖管理
│  └─ ruoyi-api-resource           // 资源api模块
│  └─ ruoyi-api-system             // 系统api模块
│  └─ ruoyi-api-workflow           // 工作流api模块
├─ ruoyi-auth            // 认证服务 [9210]
├─ ruoyi-common          // 通用模块
│  └─ ruoyi-common-alibaba-bom     // alibaba 依赖管理
│  └─ ruoyi-common-bom             // common 依赖管理
│  └─ ruoyi-common-bus             // 消息总线模块
│  └─ ruoyi-common-core            // 核心功能模块
│  └─ ruoyi-common-dict            // 字典集成模块
│  └─ ruoyi-common-doc             // 文档集成模块
│  └─ ruoyi-common-dubbo           // dubbo集成模块
│  └─ ruoyi-common-elasticsearch   // ES集成模块
│  └─ ruoyi-common-encrypt         // 数据加解密模块
│  └─ ruoyi-common-excel           // excel集成模块
│  └─ ruoyi-common-idempotent      // 幂等功能模块
│  └─ ruoyi-common-job             // job定时任务集成模块
│  └─ ruoyi-common-json            // json集成模块
│  └─ ruoyi-common-loadbalancer    // 团队负载均衡集成模块
│  └─ ruoyi-common-log             // 日志集成模块
│  └─ ruoyi-common-logstash        // elk日志集成模块
│  └─ ruoyi-common-mail            // 邮件集成模块
│  └─ ruoyi-common-mybatis         // mybatis数据库相关集成模块
│  └─ ruoyi-common-oss             // oss相关集成模块
│  └─ ruoyi-common-prometheus      // prometheus监控
│  └─ ruoyi-common-redis           // redis集成模块
│  └─ ruoyi-common-satoken         // satoken集成模块
│  └─ ruoyi-common-seata           // seata分布式事务集成模块
│  └─ ruoyi-common-security        // 框架权限鉴权集成模块
│  └─ ruoyi-common-sensitive       // 脱敏功能模块
│  └─ ruoyi-common-sentinel        // sentinel集成模块
│  └─ ruoyi-common-skylog          // skywalking日志收集模块
│  └─ ruoyi-common-sms             // 短信集成模块
│  └─ ruoyi-common-social          // 社交三方功能模块
│  └─ ruoyi-common-sse             // sse流推送模块
│  └─ ruoyi-common-tenant          // 租户功能模块
│  └─ ruoyi-common-translation     // 通用翻译功能
│  └─ ruoyi-common-web             // web服务集成模块
│  └─ ruoyi-common-websocket       // websocket服务集成模块
├─ ruoyi-example        // 例子模块
│  └─ ruoyi-demo        // 演示模块 [9401]
│  └─ ruoyi-test-mq     // mq演示模块 [9402]
├─ ruoyi-gateway        // 网关模块 [8080]
├─ ruoyi-modules        // 功能模块
│  └─ ruoyi-gen                    // 代码生成模块 [9202]
│  └─ ruoyi-job                    // 任务调度模块 [9203,9901]
│  └─ ruoyi-resource               // 资源模块 [9204]
│  └─ ruoyi-system                 // 系统模块 [9201]
│  └─ ruoyi-workflow               // 工作流模块 [9205]
├─ ruoyi-visual         // 可视化模块
│  └─ ruoyi-monitor                // 服务监控模块 [9100]
│  └─ ruoyi-nacos                  // nacos服务模块 [8848,9848,9849]
│  └─ ruoyi-seata-server           // seata服务模块 [7091,8091]
│  └─ ruoyi-sentinel-dashboard     // sentinel控制台模块 [8718]
│  └─ ruoyi-snailjob-server        // 任务调度控制台模块 [8800,17888]
├─ plus-ui              // 前端框架 [80]
├─ config/nacos         // nacos配置文件(需复制到nacos配置中心使用)
│  └─ sentinel-ruoyi-gateway.json  // sentinel对接gateway限流配置文件
│  └─ seata-server.properties      // seata服务配置文件
│  └─ application-common.yml              // 所有应用主共享配置文件
│  └─ datasource.yml               // 所有应用共享数据源配置文件
│  └─ ruoyi-auth.yml               // auth 模块配置文件
│  └─ ruoyi-gateway.yml            // gateway 模块配置文件
│  └─ ruoyi-gen.yml                // gen 模块配置文件
│  └─ ruoyi-job.yml                // job 模块配置文件
│  └─ ruoyi-monitor.yml            // monitor 模块配置文件
│  └─ ruoyi-resource.yml           // resource 模块配置文件
│  └─ ruoyi-sentinel-dashboard.yml // sentinel 控制台 模块配置文件
│  └─ ruoyi-snailjob-server.yml    // snailjob 控制台 模块配置文件
│  └─ ruoyi-system.yml             // systen 模块配置文件
│  └─ ruoyi-workflow.yml           // workflow 模块配置文件
├─ config/grafana       // grafana配置文件(需复制到grafana使用)
│  └─ Nacos.json                        // Nacos监控页面
│  └─ SLS JVM监控大盘.json               // JVM监控页面
│  └─ Spring Boot 2.1 Statistics.json   // SpringBoot监控页面
├─ sql                  // sql脚本
├─ docker               // docker 配置脚本
├─ .run                 // 执行脚本文件
├─ .editorconfig        // 编辑器编码格式配置
├─ LICENSE              // 开源协议
├─ pom.xml              // 公共依赖
├─ README.md            // 框架说明文件
~~~