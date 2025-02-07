# 搭建SnailJob任务调度中心(2.2.0新功能)
- - -

### 视频介绍

[Snail job任务调度中心：轻松掌握任务管理、重试机制和任务编排](https://www.bilibili.com/video/BV19i421m7GL/)

### 配置调度中心客户端
> 修改主服务配置文件
>

![输入图片说明](https://foruda.gitee.com/images/1716175076777941469/db565dc1_1766278.png "屏幕截图")

* `enabled` 可启用或关闭客户端注册
* `server.server-name` 为调度中心服务名(自动从Nacos获取服务 支持动态扩容调度中心)
* `server.address` 为调度中心地址(服务名优先 ip垫底)
* `server.port` 为调度中心通信端口
* `token` 为组通信校验token(可在调度中心组配置更换)
* `group-name` 为执行器组
* `namespace` 作用域(不同作用域相互隔离请勿填错)

### 启用调度中心
**需执行 ruoyi-job.sql 默认账号密码 `admin` `admin` 账号在数据库里 可以在页面修改密码**
<br>

![输入图片说明](https://foruda.gitee.com/images/1688634898607827011/8853b387_1766278.png "屏幕截图")

> 在 `ruoyi-visual -> ruoyi-snailjob-server` 模块启动
>
![输入图片说明](https://foruda.gitee.com/images/1716175119324078438/ca667a0c_1766278.png "屏幕截图")

> 需修改配置文件数据库连接地址(**注意: 此处为ruoyi-snailjob-server服务的配置文件 支持多种不同数据库**)
>
![输入图片说明](https://foruda.gitee.com/images/1688013663152608235/6c5d6a9c_1766278.png "屏幕截图")

### 快速入门

[Snailjob快速入门 基本使用介绍](https://juejin.cn/post/7412955032092442675)