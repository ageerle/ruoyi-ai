# 搭建PowerJob任务调度中心(5.X分支已废弃)
- - -
### 废弃原因

接到大量投诉 使用困难 用法诡异 各种问题等

### 配置调度中心客户端
> 修改主服务配置文件
>

![输入图片说明](https://foruda.gitee.com/images/1687656939847353725/951c1af7_1766278.png "屏幕截图")

![输入图片说明](https://foruda.gitee.com/images/1687335574708412835/41d6c9d7_1766278.png "屏幕截图")

* `enabled` 可启用或关闭客户端注册
* `server-address` 为调度中心地址
* `app-name` 为执行器组账户名(需在调度中心注册方可登录查看)

### 启用调度中心
**需执行 powerjob.sql 默认账号密码 `ruoyi-worker` `123456` 账号在数据库里 可以在页面修改密码**
<br>

![输入图片说明](https://foruda.gitee.com/images/1688634469876143273/c89455c0_1766278.png "屏幕截图")

> 在 `扩展项目 -> powerjob-server模块` 启动
>
![输入图片说明](https://foruda.gitee.com/images/1687335752250147336/17abe410_1766278.png "屏幕截图")

> 需修改配置文件数据库连接地址(**注意: 此处为ruoyi-powerjob-server服务的配置文件**)
>
![输入图片说明](https://foruda.gitee.com/images/1687335802095066722/569d92be_1766278.png "屏幕截图")

> 也可配置邮件发送 钉钉推送 和 mongodb存储
>
![输入图片说明](https://foruda.gitee.com/images/1687335842722317559/f875c07a_1766278.png "屏幕截图")

### 前端修改任务调度中心访问路径
`dev`环境 默认使用 `.env.development` 配置文件内地址

![输入图片说明](https://foruda.gitee.com/images/1687335909698376722/7efa7539_1766278.png "屏幕截图")

`prod`环境 使用 `.env.production` 本机路由

![输入图片说明](https://foruda.gitee.com/images/1687335937599399056/dd769ef5_1766278.png "屏幕截图")

故而 `prod` 环境只需更改 `nginx` 反向代理路径即可

![输入图片说明](https://foruda.gitee.com/images/1687335979933648639/6a43b749_1766278.png "屏幕截图")