# 多团队开发
- - -
## 功能介绍

> 多人员/团队开发往往会出现 调试程序 被负载均衡到别人那里 自己抓不到请求等问题<br>
> 正确团队开发模式 `测试机一台` 公共服务都放到测试机上<br>
> 本地开发人员 需启动 `ruoyi-gateway` 与 其他 调试的业务模块<br>
> 将所有服务都统一指向同一个 Nacos 服务<br>
> 前端连接本机 `ruoyi-gateway` 网关调试程序<br>

框架提供了 `ruoyi-common-loadbalancer` 多团队 负载均衡模块 可以将网关的请求锁定到与网关相同的IP服务

需要在 `ruoyi-gateway` `ruoyi-auth` `ruoyi-modules` 引入 `ruoyi-common-loadbalancer` 模块

![输入图片说明](https://foruda.gitee.com/images/1678980590168990366/afa2fdf6_1766278.png "屏幕截图")

启动前端访问本机 `ruoyi-gateway` 网关在请求转发 和 `dubbo` 进行 RPC 调用时<br>
会获取与本机IP地址相同的服务优先调用(如未找到 会随机返回)

# 重点说明

请检查本机是否有虚机网卡IP 如有多网卡获取IP地址会不准确

可使用如下代码检查本机IP是否正常
```java
InetAddress.getLocalHost().getHostAddress()
```