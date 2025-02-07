# 如何使用druid连接池
- - -
## 为何移除druid

性能低下 bug频发 内含fastjson问题众多 监控不支持集群(鸡肋) 不支持一些高版本数据库 社区活跃度冰点

### 性能对比图
![输入图片说明](https://foruda.gitee.com/images/1667888745256002635/1bbd3481_1766278.png "屏幕截图")
### 包大小对比图
![输入图片说明](https://foruda.gitee.com/images/1667888760611300040/87af8d82_1766278.png "屏幕截图")
![输入图片说明](https://foruda.gitee.com/images/1667888766932068690/7b379298_1766278.png "屏幕截图")

## 为何使用hikari(中文: 光)

spring默认自带 代码量少结构简单 稳定可靠 性能突出(自行百度一堆测评)

## 参考提交记录反向操作即可

https://gitee.com/dromara/RuoYi-Vue-Plus/commit/1f42bd3d22c104aaa2d780c20a555b5e467858bf <br>
https://gitee.com/dromara/RuoYi-Vue-Plus/commit/a63abbf268e4c0a60344f63b5cba828a1347e178