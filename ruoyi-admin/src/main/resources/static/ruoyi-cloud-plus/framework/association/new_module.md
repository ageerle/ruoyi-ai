# 创建新服务
- - -
### 最简单的方式
> 找个配置好的 例如 `ruoyi-system` 直接copy一份

> 将 `pom` 名称改掉<br>

![输入图片说明](https://foruda.gitee.com/images/1678980168782983123/c717e9ba_1766278.png "屏幕截图")

> 服务启动类 名称改掉<br>

![输入图片说明](https://foruda.gitee.com/images/1678980179829877203/f89d5c18_1766278.png "屏幕截图")

> `application.yml` 配置服务应用名 改掉<br>

![输入图片说明](https://foruda.gitee.com/images/1678980184047648028/e4c6c6cc_1766278.png "屏幕截图")

> `nacos` 新建一份新的 对应新模块名称的 配置文件<br>
![输入图片说明](https://foruda.gitee.com/images/1678980188806372269/cfd9731a_1766278.png "屏幕截图")

更改 `nacos` 上的 `ruoyi-gateway.yml` 增加新服务路由<br>
新服务访问路径 `网关ip:端口/服务路径/controller路径/接口路径`<br>
例子: `http://localhost:8080/system/user/list` <br>

![输入图片说明](https://foruda.gitee.com/images/1666861595048863422/9e9755b3_1766278.png "屏幕截图")

![输入图片说明](https://foruda.gitee.com/images/1666861629037264535/bdfd5484_1766278.png "屏幕截图")

### 注意事项
如果是两个不同包名的模块 需要修改如下配置

![输入图片说明](https://foruda.gitee.com/images/1719813861680271619/82435586_1766278.png "屏幕截图")

![输入图片说明](https://foruda.gitee.com/images/1692006501957936219/059f8526_1766278.png "屏幕截图")

如果新服务需要使用 `seata` 分布式事务<br>
需要在 `nacos` 上的 `seata-server.properties` 文件内增加服务组

![输入图片说明](https://foruda.gitee.com/images/1692006825427360840/5b9e410c_1766278.png "屏幕截图")