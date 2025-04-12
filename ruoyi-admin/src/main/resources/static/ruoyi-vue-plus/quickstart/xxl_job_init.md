# 搭建Xxl-Job任务调度中心(5.X分支已废弃)
- - -
### 废弃原因

长时间不维护 社区冰点 不支持jdk17 不支持boot3 不支持其他数据库等

### 配置调度中心客户端
> 修改主服务配置文件
>
![输入图片说明](https://foruda.gitee.com/images/1678941760168414366/b81e023b_1766278.png "屏幕截图")

* `enabled` 可启用或关闭客户端注册
* `admin-addresses` 为调度中心地址
* `access-token` 为调度中心交互鉴权token
* `executor` 为执行器配置 一个客户端为一个执行器 可配置执行器集群 使用分片任务处理

### 启用调度中心
**默认账号密码 `admin` `123456` 账号在数据库里 可以在页面修改密码**

> 在 `扩展项目 -> xxl-job-admin模块` 启动
>
![输入图片说明](https://foruda.gitee.com/images/1678976353500205883/058fef13_1766278.png "屏幕截图")

> 需修改配置文件数据库连接地址(**注意: 此处为xxl-job-admin服务的配置文件**)
>
![输入图片说明](https://foruda.gitee.com/images/1678941813423551656/04c32a5b_1766278.png "屏幕截图")

> 也可配置邮件发送
>
![输入图片说明](https://foruda.gitee.com/images/1678941825447455298/1baa5e43_1766278.png "屏幕截图")

### 前端修改任务调度中心访问路径
`dev`环境 默认使用 `.env.development` 配置文件内地址

![输入图片说明](https://foruda.gitee.com/images/1678976378255854583/8cdbf4e3_1766278.png "屏幕截图")

`prod`环境 使用 `.env.production` 本机路由

![输入图片说明](https://foruda.gitee.com/images/1678976382819019066/96288331_1766278.png "屏幕截图")

故而 `prod` 环境只需更改 `nginx` 反向代理路径即可

![输入图片说明](https://foruda.gitee.com/images/1678976386764602366/55894f85_1766278.png "屏幕截图")
