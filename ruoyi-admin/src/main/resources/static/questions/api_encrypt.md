# 关于请求响应参数解密
---
## 1：前端加密请求

![输入图片说明](https://foruda.gitee.com/images/1717033672316716771/8e30a2f1_4959041.png "屏幕截图")

通过控制台获取加密结果：

![输入图片说明](https://foruda.gitee.com/images/1717033792384655437/900a0e0d_4959041.png "屏幕截图")

![输入图片说明](https://foruda.gitee.com/images/1717033896868612970/55581f0a_4959041.png "屏幕截图")


加密密钥：

```
PAg/fZzpV/cz0T1fMUJMJo/LEZvwVLb4bZgtCHkbB6FQAJWlLm/RLKtQ5fOo1blMjAkY+9ryWhsAfCqoMPTU4w==
```

请求参数加密结果：

```
F+Qxq6PzShcudDsUZHhp50lA67eBeTe63x5uGbdm/HJGgcDmjKncUk5VQm0evD8pz1sbmCbmmSl3X1D07K/qgHvP1YhjYSRBJf/M0GTfMkfOZqIkOtvfE5Z6fSFd8RYf6ji/qYxAmCiRmP/uADyJUAoBY1gMi5+zuvyHH3In/FyoFeD0rmJWvO4o4fn3n5GElHMWbP0O/HWPfgHFfg1F7bZQPuf4zAuDKQIqUG3jJTem3O97kAbTWw6lSSuYi1/8tV4cE9rq8SMSjx36/ZLSog==
```

### 解密步骤

1. 使用配置文件私钥对加密密钥解密

```java
// 参数说明：
// requestKey：即请求标头加密密钥 
// privateKey：application.yml 配置文件私钥
String decryptByRsa = EncryptUtils.decryptByRsa(requestKey, privateKey);
```

2. 对步骤一结果进行 Base64 解密，得到 AES 加密密钥

```java
String aesPassword = EncryptUtils.decryptByBase64(decryptByRsa);
```

3. 使用步骤二得到的密钥，对请求参数进行解密

```java
String decryptBody = EncryptUtils.decryptByAes(requestBody, aesPassword);
```

得到解密请求参数（已格式化）：

```json
{
    "tenantId": "000000",
    "username": "admin",
    "password": "admin123",
    "rememberMe": false,
    "uuid": "a39962b22c874f60872ef5db1cd811f5",
    "code": "5",
    "clientId": "e5cd7e4891bf95d1d19206ce24a7b32e",
    "grantType": "password"
}
```

|参数名|说明|
|---|---|
|tenantId| 租户id |
|username| 用户名 |
|password| 密码 |
|rememberMe| 记住密码 |
|uuid| - |
|code| 验证码结果 |
|clientId| 客户端id（表 sys_client） |
|grantType| 授权类型（表 sys_client） |

## 2：后端加密响应

对请求使用了注解 `@ApiEncrypt(response = true)`

![输入图片说明](https://foruda.gitee.com/images/1717035066844744866/2286b394_4959041.png "屏幕截图")

通过控制台获取加密结果：

![输入图片说明](https://foruda.gitee.com/images/1717035156784270596/156f2aa7_4959041.png "屏幕截图")

![输入图片说明](https://foruda.gitee.com/images/1717035193189175688/214631e5_4959041.png "屏幕截图")

加密密钥：

```
MXnKYnXcXeFYWKZg8utuhDtbz54cPDcov11E1KT5l19/vMt37d4NhzzwBWnqug72SOgOK5URGaWPJSs9VdaP0Q==
```

响应参数加密结果：

```
70 O63EMmwvbAyWPqDDmVOGTy+BOQnIVgKInMFNRtp8Zwzs8DEL20VgL2IslYrL8bc1u7lPhYNU/6 Q3iTYebm4EokwiG+styaT+LO3M9bUimggoAGpBTW8gCRF/34 kJaOITSRqYqYcXIJKn73+Gqn7jevyKUHyRXog/3 q/PlBdmUjNiB4gtxlOO/Vm+4 o+0 W4jcEe0xwwzV91+Ze3S6Eu/1 XN21g0iOsYT34emv/vhd9Hy3p5LfJlAHvn96x/c3MQBQUU32uM3Vkk3o6IpVHjJljE64gnGximSwB9vrmMA21xX+fq9HYioumknmDDbaY/JAKh32CDgn5M5hdaIklf08sU38r1IyvipySzrHX+ci9GmOZhP2ttCtoZ7SGvFFbNEuyojssxwxXEmJHAsG/OhIAeRXMUr3+dzDJ++XvvMuMgNJR0BMldNydFAjNOQEszgcVM1QEGwxfW5rElW8VxQaaqPyDATX+y2JrK1vdKxxdI/hF5dGpQMdU4FAEhHIftoIbD/FH4XcWJamZjJpbVtZvTkFYpbhiU7sz9MICSuKwaoSFJ8JGANc0bDdVoWpA8sXi7a27IM0pDzk9gD/FADcFGHXxPYUhENkXiUcnmg5LSdigiY4J6HrqEJdH6zNSwoGubcsXhiPdlB3V0DqcLAHFt+GYj5lcxZeqUAmixGVGCV7gSBWNiyo9/NnXcynA/EIlV3OZIvgzjWxiKzcVJ1HOKoXGEcg3Q54QNh5pCqEa7AtqVkKO7/Ffgg8nSEeCdJPzTV7zmr3n94Hn671OL8A==
```

### 解密步骤

1. 使用前端配置文件私钥对加密密钥解密

```java
// 参数说明：
// responseKey：即响应标头加密密钥 
// privateKey：前端 .env.development | .env.production 配置文件私钥，注意和后端私钥区分
String decryptByRsa = EncryptUtils.decryptByRsa(responseKey, privateKey);
```

2. 对步骤一结果进行 Base64 解密，得到 AES 加密密钥

```java
String aesPassword = EncryptUtils.decryptByBase64(decryptByRsa);
```

3. 使用步骤二得到的密钥，对响应参数进行解密

```java
String decryptBody = EncryptUtils.decryptByAes(responseBody, aesPassword);
```

得到解密请求参数（已格式化）：

```json
{
    "code": 200,
    "msg": "操作成功",
    "data": {
        "scope": null,
        "openid": null,
        "access_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJsb2dpblR5cGUiOiJsb2dpbiIsImxvZ2luSWQiOiJzeXNfdXNlcjoxIiwicm5TdHIiOiJjOVNWU1hRRVY4QVhFRkt4b2FrbndSSWxPczd4ajdRZCIsImNsaWVudGlkIjoiZTVjZDdlNDg5MWJmOTVkMWQxOTIwNmNlMjRhN2IzMmUiLCJ0ZW5hbnRJZCI6IjAwMDAwMCIsInVzZXJJZCI6MSwidXNlck5hbWUiOiJhZG1pbiIsImRlcHRJZCI6MTAzLCJkZXB0TmFtZSI6IueglOWPkemDqOmXqCJ9.YuaXPu6eTzJVkLyQC3ekzmPS_jXp50ykaIB2nWy11qM",
        "refresh_token": null,
        "expire_in": 604799,
        "refresh_expire_in": null,
        "client_id": "e5cd7e4891bf95d1d19206ce24a7b32e"
    }
}
```

|参数名|说明|
|---|---|
|scope| 令牌权限 |
|openid| 用户 openid |
|access_token| 授权令牌 |
|refresh_token| 刷新令牌 |
|expire_in| 授权令牌 access_token 的有效期 |
|refresh_expire_in| 刷新令牌 refresh_token 的有效期 |
|clientId| 客户端id（表 sys_client） |