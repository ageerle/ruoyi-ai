# 翻译功能
- - -
## 框架版本 >= 1.6.0

## 引入依赖包

```xml
<dependency>
    <groupId>com.ruoyi</groupId>
    <artifactId>ruoyi-common-translation</artifactId>
</dependency>
```

## 注解

![输入图片说明](https://foruda.gitee.com/images/1675575648043199227/d04b3e21_1766278.png "屏幕截图")

`@Translation` 翻译注解 用于实体类字段上<br>
`@TranslationType` 翻译类别注解 用于实现类上标注与 `@Translation` 注解相同的 `type` 类型 实现翻译功能


## 用法说明

默认提供功能 `用户id转账号(用户名)` `部门id转名称` `字典type转label` `ossId转url`

![输入图片说明](https://foruda.gitee.com/images/1675575977860232549/143b74f8_1766278.png "屏幕截图")

用户名翻译(映射翻译) 根据另一个映射字段 翻译保存到此字段

![输入图片说明](https://foruda.gitee.com/images/1675576044011477847/13eb9f57_1766278.png "屏幕截图")

ossUrl翻译(直接翻译) 直接根据此字段值翻译后替换此字段值

![输入图片说明](https://foruda.gitee.com/images/1675576265894720924/70792f66_1766278.png "屏幕截图")

字典翻译(其他扩展条件翻译) 根据`other`条件 自行定义如何使用 例如字典翻译`other`条件就是字典的唯一值

![输入图片说明](https://foruda.gitee.com/images/1675576391012282823/f95c5d78_1766278.png "屏幕截图")

## 自定义扩展

实现接口 `TranslationInterface` 标注注解 `@TranslationType` 可参考框架默认实现

![输入图片说明](https://foruda.gitee.com/images/1676735454308997001/cfcf3590_1766278.png "屏幕截图")
