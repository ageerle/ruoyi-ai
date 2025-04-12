# 系统用户相关
- - -

> 框架采用sa-token控制权限 并对sa-token的api做了一定的业务封装<br>

## 用户登录

> 参考自带多种登录实现 不限制用户数据来源 只需要构建 LoginUser 即可完成登录<br>
> 例如: `同表不同类型` `不同表` `同表+扩展表`<br>

![输入图片说明](https://foruda.gitee.com/images/1699590555824776931/63d493fc_1766278.png "屏幕截图")

## 获取用户信息

> 完成登录后会生成登录token返回给前端 前端需要再请求头携带token 后端方可获取到对应的用户信息

请求头传递格式: `Authorization: Bearer token`

后端获取用户信息: 
```java
LoginUser user = LoginHelper.getLoginUser();
```

## 获取用户信息(基于token)
```java
LoginUser user = LoginHelper.getLoginUser(token);
```

## 获取登录用户id
```java
Long userId = LoginHelper.getUserId();
```

## 获取登录用户账户名
```java
String username = LoginHelper.getUsername();
```

## 获取登录用户所属租户id
```java
String tenantId = LoginHelper.getTenantId();
```

## 获取登录用户所属部门id
```java
Long deptId = LoginHelper.getDeptId();
```

## 获取登录用户类型
```java
UserType userType = LoginHelper.getUserType();
```

## 获取登录用户其他扩展属性
```java
Object obj = LoginHelper.getExtra(key);
```

## 设置登录用户其他扩展属性

参考登录设置 `clientId` 属性

![输入图片说明](https://foruda.gitee.com/images/1699591164562734430/42730add_1766278.png "屏幕截图")

## 判断用户是否为超级管理员

```java
// 判断当前登录用户
boolean b = LoginHelper.isSuperAdmin();
// 判断用户基于id
boolean b = LoginHelper.isSuperAdmin(userId);
```

## 判断用户是否为租户管理员

```java
// 判断当前登录用户
boolean b = LoginHelper.isTenantAdmin();
// 判断用户基于角色组
boolean b = LoginHelper.isSuperAdmin(rolePermission);
```

## 其他更多操作
[Sa-Token 官方文档 - 登录认证](https://sa-token.cc/doc.html#/use/login-auth)

