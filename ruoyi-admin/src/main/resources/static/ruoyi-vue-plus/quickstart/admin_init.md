# 搭建Admin监控
- - -
### 配置监控客户端

> 修改主服务配置文件

![输入图片说明](https://foruda.gitee.com/images/1678941504260707700/68ab99e5_1766278.png "屏幕截图")

* `enabled` 可启用或关闭客户端注册
* `url` 为监控中心地址
* `username 与 password` 为监控中心的账号密码

### 启用监控中心
在 `扩展项目 -> 监控模块` 启动

![输入图片说明](https://foruda.gitee.com/images/1678976327174539378/df97e36e_1766278.png "屏幕截图")

在监控模块对应的 `yml` 配置文件 可设置登录的账号密码与访问路径

![输入图片说明](https://foruda.gitee.com/images/1678941572583282843/28117457_1766278.png "屏幕截图")

### 前端修改admin监控访问路径
`dev`环境 默认使用 `.env.development` 配置文件内地址

![输入图片说明](https://foruda.gitee.com/images/1678941607472644388/460e8eea_1766278.png "屏幕截图")

`prod`环境 使用 `.env.production` 本机路由

![输入图片说明](https://foruda.gitee.com/images/1678941644784144830/6293ab1c_1766278.png "屏幕截图")
故而 `prod` 环境只需更改 `nginx` 反向代理路径即可

![输入图片说明](https://foruda.gitee.com/images/1678981483900657668/31fd1aad_1766278.png "屏幕截图")
