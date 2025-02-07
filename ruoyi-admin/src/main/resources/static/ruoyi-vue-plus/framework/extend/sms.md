# 短信模块
- - -

# 配置功能

### 版本: >= v5.1.0

已完成 sms4j 项目整合 文档地址: https://sms4j.com/doc3

配置方式 具体厂商配置扩展 可以查看sms4j文档

![输入图片说明](https://foruda.gitee.com/images/1705573035997239848/2ca8512d_1766278.png "屏幕截图")

使用方式 参考文档各种写法 下方为 demo 模块提供示例

![输入图片说明](https://foruda.gitee.com/images/1705573001447394180/2bd726d0_1766278.png "屏幕截图")

### 版本: v4.2.0 提供短信模块

短信模块采用SPI加载<br>
使用哪家的短信 引入哪家的依赖 即可动态加载<br>
目前支持: `阿里云` `腾讯云` 欢迎扩展PR其他

> 参考 `ruoyi-demo` pom文件写法

![输入图片说明](https://foruda.gitee.com/images/1678979157797419426/cc9b7444_1766278.png "屏幕截图")

> 修改配置文件

![输入图片说明](https://foruda.gitee.com/images/1678979163029635375/e5fd6e20_1766278.png "屏幕截图")

* `enabled` 为短信功能开关
* `endpoint` 为域名 各厂家域名固定 按照文档配置即可
* `accessKeyId` 密钥id
* `accessKeySecret` 密钥密匙
* `signName` 签名
* `sdkAppId` 应用id 腾讯专用

## 功能使用

参考 `demo` 模块 `SmsController` 短信演示案例<br>
功能采用 `模板模式` 动态加载对应厂家的工具模板<br>
引入 `SmsTemplate` 即可使用

![输入图片说明](https://foruda.gitee.com/images/1678979168699323982/e9301e84_1766278.png "屏幕截图")

## 重点须知

由于各厂家参数解析不一致 请遵守以下规则

![输入图片说明](https://foruda.gitee.com/images/1678979172581090456/ac1f10e8_1766278.png "屏幕截图")
