# ParseException SQL解析异常
- - -
## 异常内容

`net.sf.jsqlparser.parser.ParseException: Encountered unexpected token:`

![输入图片说明](https://foruda.gitee.com/images/1678981169309778625/a17ff852_1766278.png "屏幕截图")

此异常为 SQL 解析异常, 应检查 SQL 语句内是否包含 SQL 关键字

异常通常都会提供坐标

![输入图片说明](https://foruda.gitee.com/images/1678981173813116217/a6f9ee32_1766278.png "屏幕截图")

检查报错 SQL 相关坐标位置

![输入图片说明](https://foruda.gitee.com/images/1678981179153564043/bf4912b4_1766278.png "屏幕截图")

## 异常由来
由 Mybatis-Plus 拦截器进行 SQL 解析导致<br>
常见拦截器导致问题 `TenantLineInnerInterceptor` `DataPermissionInterceptor`

## 解决方案

> 将关键字增加标识符区别开

1.实体类字段处理(以下仅限于mysql 其他数据库方法各不相同)

![输入图片说明](https://foruda.gitee.com/images/1678981183515542682/fccd85ad_1766278.png "屏幕截图")

2.自定义 SQL 或 XML 处理

![输入图片说明](https://foruda.gitee.com/images/1678981187926917963/38437edb_1766278.png "屏幕截图")

3.Mapper排除
> 查看具体使用了哪些拦截器导致问题 使用忽略注解依次进行排除即可

![输入图片说明](https://foruda.gitee.com/images/1678981192902044584/fb1c41eb_1766278.png "屏幕截图")


