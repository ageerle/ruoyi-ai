# 关于数据权限
- - -
* 参考 demo 模块用法(需导入 test.sql 文件)

### 新版数据权限功能:
1.支持自动注入 sql 数据过滤<br>
2.查询、更新、删除 限制<br>
3.支持自定义数据字段过滤<br>
4.模板支持 spel 语法实现动态 Bean 处理<br>
5.支持与菜单权限标识符联合使用(2.2.X新功能)

### 数据权限相关代码

| 类                             | 说明              | 功能                                     |
|-------------------------------|-----------------|----------------------------------------|
| DataScopeType                 | 数据权限模板定义        | 用于定义数据权限模板                             |
| DataPermission                | 数据权限组注解         | 用于标注开启数据权限 (默认过滤部门权限)                  |
| DataColumn                    | 具体的数据权限字段标注     | 用于替换数据权限模板内的 key 变量                    |
| PlusDataPermissionInterceptor | 数据权限 sql 拦截器    | 用于拦截所有 sql 检查是否标注了 `DataPermission` 注解 |
| PlusDataPermissionHandler     | 数据权限处理器         | 用于处理被拦截到的 sql 为其添加数据权限过滤条件             |
| DataPermissionHelper          | 数据权限助手          | 操作数据权限上下文变量                            |
| SysDataScopeService           | 自定义 Bean 处理数据权限 | 用于自定义扩展                                |

## 忽略数据权限

1.如果需要指定单独 SQL 不开启过滤，可在对应的 Mapper 接口添加如下忽略注解：
```
@InterceptorIgnore(dataPermission = "true")
```

2.如果需要在业务层忽略数据权限，可调用以下方法：
```
# 无返回值
DataPermissionHelper.ignore(() -> { 业务代码 });
# 有返回值
Class result = DataPermissionHelper.ignore(() -> { return 业务代码 });
```

### 使用方式 `参考demo模块`
数据权限体系 `用户 -> 多角色 => 角色 -> 单数据权限`
> 例子: 用户A 拥有两个角色<br>
> 角色A 部门经理 可查看 本部门及以下部门的数据<br>
> 角色B 兼职开发 可查看 仅自己的数据

> 创建角色 test1 为 本部门及以下

![输入图片说明](https://foruda.gitee.com/images/1678978669666831574/b51ed0a3_1766278.png "屏幕截图")

> 创建角色 test2 为 仅本人

![输入图片说明](https://foruda.gitee.com/images/1678978674159035056/69cf32ad_1766278.png "屏幕截图")

> 将其分配给用户 test

![输入图片说明](https://foruda.gitee.com/images/1678978680492570269/a47b6afc_1766278.png "屏幕截图")

### 编写列表查询(注意: 数据权限注解只能在 Mapper 层使用)

> 标注数据权限注解 `dept_id` 为过滤部门字段 `user_id` 为过滤创建用户

![输入图片说明](https://foruda.gitee.com/images/1678978687179608427/d6b83c30_1766278.png "屏幕截图")

### 重点注意: 如下情况不生效

> 有自定义实现方法 最终执行的mapper不是这个方法 所以无法生效
>
> 解决方案: 一直往下点 找到最终的执行mapper重写即可

![输入图片说明](https://foruda.gitee.com/images/1678978692558777291/78b0a3dd_1766278.png "屏幕截图")

### 编写数据权限模板

![输入图片说明](https://foruda.gitee.com/images/1678978697141183499/cfc1cb6a_1766278.png "屏幕截图")

1.`code` 为关联角色的数据权限 `code`<br>
2.`sqlTemplate` 为 sql 模板<br>
`#{#deptName}` 为模板变量 对应权限注解的 `key`<br>
`#{@sdss}` 为模板 Bean 调用 调用其 Bean 的处理方法<br>
3.`elseSql` 为兜底 sql 处理当前角色与标注的注解 无对应的情况<br>
例如 数据权限为仅本人 且 方法并未标注具体过滤注解 则 填充 `1 = 0` 使条件不满足 不允许查看<br>
更详细用法可以参考 `DataScopeType` 注释

### 测试代码

> 使用 `管理员` 用户优先测试

![输入图片说明](https://foruda.gitee.com/images/1678978703250082481/e93a68a5_1766278.png "屏幕截图")

> 使用 `test` 用户测试

![输入图片说明](https://foruda.gitee.com/images/1678978710644676604/d7f80487_1766278.png "屏幕截图")

> 使用 `test` 删除一条不属于自己的数据
> sql执行为不满足条件 不允许删除

![输入图片说明](https://foruda.gitee.com/images/1678978715711122947/441d61f7_1766278.png "屏幕截图")

![输入图片说明](https://foruda.gitee.com/images/1678978720298532619/a35b1147_1766278.png "屏幕截图")


> 使用 `test` 修改与删除同理<br>
> 具体实现为 更新和删除方法 标注数据权限注解

![输入图片说明](https://foruda.gitee.com/images/1678978725329242504/a70491a1_1766278.png "屏幕截图")

### 自定义SQL模板

> 1.首先在角色管理 数据权限下拉框 添加自定义模板<br>
> 为什么不放置到系统字典问题: 因数据权限与模板绑定 不应随意改动 最好事先定义好

![输入图片说明](https://foruda.gitee.com/images/1678978730563169865/3459ee17_1766278.png "屏幕截图")

> 2.代码 `DataScopeType` 自定义一个SQL模板

![输入图片说明](https://foruda.gitee.com/images/1678978735588305505/3f030c67_1766278.png "屏幕截图")

> 3.标注权限注解

![输入图片说明](https://foruda.gitee.com/images/1678978742259837391/eabe5caa_1766278.png "屏幕截图")

> 4.设置数据权限变量

![输入图片说明](https://foruda.gitee.com/images/1678978746778429543/e211201f_1766278.png "屏幕截图")

> 5.测试

![输入图片说明](https://foruda.gitee.com/images/1678978751875467640/7d210cf4_1766278.png "屏幕截图")

### mybatis-plus 原生方法 增加数据权限过滤

> 首先查看需要重写的方法源码 重点`方法源码` `方法源码` `方法源码`<br>
> 例如重写 `selectPage` 方法<br>

![输入图片说明](https://foruda.gitee.com/images/1678978757955000897/8315695c_1766278.png "屏幕截图")

> 复制源码到自己的 `Mapper` 并增加数据权限注解 注意左边出现重写图标 即为重写成功<br>

![输入图片说明](https://foruda.gitee.com/images/1678978763224011694/bbea25a1_1766278.png "屏幕截图")

### 支持类标注

> 获取规则 `方法 > 类` 注意: 类标注后 所有方法(包括父类方法) 都会进行数据权限过滤

![输入图片说明](https://foruda.gitee.com/images/1678978767336534896/fb13ee99_1766278.png "屏幕截图")
