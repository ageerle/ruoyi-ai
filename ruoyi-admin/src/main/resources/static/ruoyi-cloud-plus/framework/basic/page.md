# 分页功能
- - -

## 重点说明

> 项目使用 `mybatis-plus` 分页插件 实现分页功能 大致用法与 MP 一致 [MP分页文档](https://baomidou.com/pages/97710a/) <br>
> 项目已配置分页合理化 页数溢出 例如: 一共5页 查了第6页 默认返回第一页 <br>

![输入图片说明](https://foruda.gitee.com/images/1678977804058241635/b5cb362d_1766278.png "屏幕截图")

## 代码用法

> `Controller` 使用 `PageQuery` 接收分页参数 具体参数参考 `PageQuery`

![输入图片说明](https://foruda.gitee.com/images/1678977844048821356/1f994221_1766278.png "屏幕截图")

> 构建 `Mybatis-Plus` 分页对象 <br>
> 使用 `PageQuery#build()` 方法 可快速(基于当前对象数据)构建 `MP` 分页对象

![输入图片说明](https://foruda.gitee.com/images/1678977862816976499/b82c1638_1766278.png "屏幕截图")<br>
![输入图片说明](https://foruda.gitee.com/images/1678977876194578744/eaa7b854_1766278.png "屏幕截图")<br>

具体用法与 `MP` 一致

> 自定义 `SQL` 方法分页 <br>
> 只需在 `Mapper` 方法第一个参数和返回值 重点: 第一个参数 标注分页对象

![输入图片说明](https://foruda.gitee.com/images/1678977898181729571/6e102731_1766278.png "屏幕截图")<br>
![输入图片说明](https://foruda.gitee.com/images/1678977906788451483/70979292_1766278.png "屏幕截图")
