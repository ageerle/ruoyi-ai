# 搭建PowerJob任务调度中心(2.X分支已废弃)
- - -
### 废弃原因

接到大量投诉 使用困难 用法诡异 各种问题等

### 配置调度中心客户端
> 查看ruoyi-job配置文件(默认情况下无需做任何更改)
>
![输入图片说明](https://foruda.gitee.com/images/1688013407489024239/9b619e0d_1766278.png "屏幕截图")

* `enabled` 可启用或关闭客户端注册
* `server-address` 为调度中心地址
* `server-name` 为调度中心服务名
* `app-name` 为执行器组账户名(需在调度中心注册方可登录查看)

### 启用调度中心
**需执行 ry-job.sql 默认账号密码 `ruoyi-worker` `123456` 账号在数据库里 可以在页面修改密码**
<br>

![输入图片说明](https://foruda.gitee.com/images/1688634898607827011/8853b387_1766278.png "屏幕截图")

> 在 `ruoyi-visual -> ruoyi-powerjob-server` 启动
>
![输入图片说明](https://foruda.gitee.com/images/1688013606234848334/cf2028cd_1766278.png "屏幕截图")

> 需修改配置文件数据库连接地址(**注意: 此处为ruoyi-powerjob-server服务的配置文件**)
>
![输入图片说明](https://foruda.gitee.com/images/1688013663152608235/6c5d6a9c_1766278.png "屏幕截图")

> 也可配置邮件发送 钉钉推送 和 mongodb存储
>
![输入图片说明](https://foruda.gitee.com/images/1687335842722317559/f875c07a_1766278.png "屏幕截图")
