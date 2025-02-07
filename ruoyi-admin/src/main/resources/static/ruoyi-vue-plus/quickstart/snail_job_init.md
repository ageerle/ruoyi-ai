# 搭建SnailJob任务调度中心(5.2.0新功能)
- - -

### 视频介绍

[Snail job任务调度中心：轻松掌握任务管理、重试机制和任务编排](https://www.bilibili.com/video/BV19i421m7GL/)

### 配置调度中心客户端
> 修改主服务配置文件
>

![输入图片说明](https://foruda.gitee.com/images/1687656939847353725/951c1af7_1766278.png "屏幕截图")

![输入图片说明](https://foruda.gitee.com/images/1716174758437043952/de28db71_1766278.png "屏幕截图")

* `enabled` 可启用或关闭客户端注册
* `server.address` 为调度中心地址
* `server.port` 为调度中心通信端口
* `token` 为组通信校验token(可在调度中心组配置更换)
* `group-name` 为执行器组
* `namespace` 作用域(不同作用域相互隔离请勿填错)

### 启用调度中心
**需执行 snail_job.sql 默认账号密码 `admin` `admin` 账号在数据库里 可以在页面修改密码**
<br>

![输入图片说明](https://foruda.gitee.com/images/1714355875395308961/adc21668_1766278.png "屏幕截图")

> 在 `ruoyi-extend -> ruoyi-snailjob-server` 模块启动
>
![输入图片说明](https://foruda.gitee.com/images/1716174842485474283/78cec86d_1766278.png "屏幕截图")

> 需修改配置文件数据库连接地址(**注意: 此处为ruoyi-snailjob-server服务的配置文件 支持多种不同数据库**)
>
![输入图片说明](https://foruda.gitee.com/images/1714356048711590477/13289085_1766278.png "屏幕截图")

### 快速入门

[Snailjob快速入门 基本使用介绍](https://juejin.cn/post/7412955032092442675)

### 前端修改任务调度中心访问路径
`dev`环境 默认使用 `.env.development` 配置文件内地址

![输入图片说明](https://foruda.gitee.com/images/1716174933143893408/58d47bbc_1766278.png "屏幕截图")

`prod`环境 使用 `.env.production` 本机路由

![输入图片说明](https://foruda.gitee.com/images/1716174973454805690/0d6f20fb_1766278.png "屏幕截图")

故而 `prod` 环境只需更改 `nginx` 反向代理路径即可

![输入图片说明](https://foruda.gitee.com/images/1716174998979181179/2f9e4e4a_1766278.png "屏幕截图")