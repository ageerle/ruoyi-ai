# 如何对接国产数据库

> 1. 框架采用 mybatis-plus 几乎支持大部分市面上的数据库且框架内几乎没有sql语句存在
<br>
所以不用担心兼容性问题(顶多就是有一些关键字什么的 对接很简单)
<br>
> 2. 国产数据库大多都兼容主流三大数据库 mysql oracle postgresql
<br>
例如 达梦兼容oracle 人大金仓兼容mysql oceanbase兼容mysql 等等

# 对接方式

### 这里用 `达梦` 数据库为例

1.首先增加 jdbc依赖包 `vue版本在ruoyi-admin模块下` `cloud版本在ruoyi-common-mybatis模块下`

![输入图片说明](https://foruda.gitee.com/images/1723288594335994875/216ae8e7_1766278.png "屏幕截图")

2.在配置文件yml内配置数据库连接

![输入图片说明](https://foruda.gitee.com/images/1723288760519808620/3db91ba5_1766278.png "屏幕截图")

3.sql脚本使用框架内自带的sql文件根据兼容的数据库模式 例如 达梦用oracle的sql脚本

![输入图片说明](https://foruda.gitee.com/images/1723289018873298537/4d95c892_1766278.png "屏幕截图")

4.在代码生成器内 增加对应的数据库生成器依赖 代码生成器使用 anyline 支持几百种数据库只需要增加对应的依赖即可

![输入图片说明](https://foruda.gitee.com/images/1723288974693848785/3e8fc61f_1766278.png "屏幕截图")

这样基本就完成了所有需要做的事可以尝试启动项目了

5.如果项目启或者运行动过程中有sql报错 不要慌基本上都是一些关键字引起的
<br>
例如 达梦内的`domain`就是关键字 在我们的`SysOssConfig`表内使用`domain`进行自定义的域名存储
<br>
我们只需要在`SysOssConfig`实体类的`domain`属性增加一个注解即可解决此问题
<br>
**注意: 各种数据库处理关键字的标识符不一样注意替换**

![输入图片说明](https://foruda.gitee.com/images/1723289232470339283/480d5172_1766278.png "屏幕截图")
