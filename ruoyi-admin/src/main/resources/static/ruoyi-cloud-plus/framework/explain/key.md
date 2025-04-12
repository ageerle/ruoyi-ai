# 主键使用说明
- - -
## 关于如何使用分布式id或雪花id

参考 `MybatisPlusConfig` 如需自定义 修改 `Bean` 实现即可

![输入图片说明](https://foruda.gitee.com/images/1678979401707903546/e25f6c06_1766278.png "屏幕截图")

框架默认集成 雪花ID 只需全局更改 主键类型即可

![输入图片说明](https://foruda.gitee.com/images/1678979411517764918/1470df04_1766278.png "屏幕截图")

如单表使用 可单独配置注解

![输入图片说明](https://foruda.gitee.com/images/1678979416033986923/2a4c3736_1766278.png "屏幕截图")

### 重点说明
* 由于雪花id位数过长 `Long` 类型在前端会失真
* 框架已配置序列化方案 超越 `JS` 最大值自动转字符串 参考 `BigNumberSerializer` 类