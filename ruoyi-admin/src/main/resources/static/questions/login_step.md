# 关于登录调试步骤

## 1：关闭 api 接口加密

1. 修改后端配置文件 `application.yml`

![输入图片说明](https://foruda.gitee.com/images/1717037518256330645/c5a9f0fc_4959041.png "屏幕截图")

2. 修改前端配置文件 `.env.development` | `.env.production`

![输入图片说明](https://foruda.gitee.com/images/1717037555118359683/0e73a369_4959041.png "屏幕截图")

## 2：登录参数

![输入图片说明](https://foruda.gitee.com/images/1717038201634120005/e02882d3_4959041.png "屏幕截图")

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

## 3：使用接口文档调试

### 3.1：使用接口文档请求

1. 配置接口文档（[参考文档](/ruoyi-vue-plus/framework/association/doc)）
2. 请求接口 `http://localhost:8080/auth/login`

![输入图片说明](https://foruda.gitee.com/images/1717039200581756307/97efbc9c_4959041.png "屏幕截图")

### 3.2：使用 idea 请求

![输入图片说明](https://foruda.gitee.com/images/1717039459944753490/040d2b9d_4959041.png "屏幕截图")

![输入图片说明](https://foruda.gitee.com/images/1717039534863944601/df91df67_4959041.png "屏幕截图")

![输入图片说明](https://foruda.gitee.com/images/1717039598067298052/cc9fe61b_4959041.png "屏幕截图")

### 3.3：获取验证码以及 uuid

!> 验证码以及 uuid 获取方式： Redis | 控制台

方式一、Redis：

![输入图片说明](https://foruda.gitee.com/images/1717040260329977942/42f7ed62_4959041.png "屏幕截图")

> **如果没有验证码相关 key，说明已经过期被清理了，去前端页面刷新一下即可。**

方式二、控制台：

![输入图片说明](https://foruda.gitee.com/images/1717040428227070908/1ef7562a_4959041.png "屏幕截图")

### 3.4：关闭验证码

如果嫌验证码太麻烦，可以关闭，修改后端配置文件 `application.yml`

![输入图片说明](https://foruda.gitee.com/images/1717040533266608114/054fd984_4959041.png "屏幕截图")

![输入图片说明](https://foruda.gitee.com/images/1717040745251872562/374267e8_4959041.png "屏幕截图")

请求参数：

![输入图片说明](https://foruda.gitee.com/images/1717040762860943102/81c9b44a_4959041.png "屏幕截图")