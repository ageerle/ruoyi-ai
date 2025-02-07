# 网关路由与放行
- - -

## 新增路由
`ruoyi-gateway.yml` 配置文件 增加 `routers` 配置<br>
**注意: 路径格式为 `/服务路径/controller路径/接口方法路径` `*代表任意一级 **代表任意所有级`**<br>
下图代表 `resource/**` 将所有 `resource开头的路径` 都路由到 `ruoyi-resource` 服务<br>
例如: `/resource/sms/code` `resource路由到ruoyi-resource服务` `sms路由到对应的contrller` `code 路由到对应的接口`<br>
![输入图片说明](https://foruda.gitee.com/images/1669623462957266512/c282932b_1766278.png "屏幕截图")<br>
![输入图片说明](https://foruda.gitee.com/images/1669623527799049459/201a52db_1766278.png "屏幕截图")

## 放行使用方式
nacos 中 `ruoyi-gateway.yml` 白名单放行<br>
**注意: 放行路径格式为 `/服务路径/controller路径/接口方法路径` `*代表任意一级 **代表任意所有级`**<br>
示例: `/resource/sms/code` 代表 `ruoyi-resource服务 sms的controller code接口`<br>
![输入图片说明](https://foruda.gitee.com/images/1660622672461635175/屏幕截图.png "屏幕截图.png")

## 注意事项

接口放行后不需要token即可访问<br>
但是没有token也就无法获取用户信息与鉴权

### 解决方案
删除接口上的鉴权注解<br>
删除接口内获取用户信息功能<br>
删除数据库实体类 自动注入 `createBy` `updateBy` 因为会获取用户数据