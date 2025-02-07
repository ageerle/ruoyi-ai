# 第三方授权功能
- - -
## 版本 >= 5.X

## 前置说明
1. 该功能基于 `JustAuth` 实现，支持多家平台实现第三方授权登录。
2. 以 `Gitee` 授权登录为例进行本功能的使用说明。
3. 其他第三方授权配置信息获取方式可参考 `JustAuth` [官方文档](https://www.justauth.cn/guide/)。<br>

   ![输入图片说明](https://foruda.gitee.com/images/1690937097426867003/91d80587_4959041.png "屏幕截图")

## 第三方授权配置

### 申请三方应用(以gitee为例)

![输入图片说明](https://foruda.gitee.com/images/1700641775779304627/1cf1b56f_1766278.png "屏幕截图")

### 更改后端配置 `application-dev.yml`

![输入图片说明](https://foruda.gitee.com/images/1690936741844431943/580f8998_4959041.png "屏幕截图")

**注：内网地址无法回调，请使用外网可以访问的地址。**

![输入图片说明](https://foruda.gitee.com/images/1690940457570856867/ce22df18_4959041.png "屏幕截图")

### 更改前端配置 `login.vue`

![输入图片说明](https://foruda.gitee.com/images/1690937306197173754/5c1ece29_4959041.png "屏幕截图")

## 授权登录（未绑定第三方平台）

### 步骤一：个人中心授权第三方应用

![输入图片说明](https://foruda.gitee.com/images/1690938449386201097/ea375106_4959041.png "屏幕截图")

### 步骤二：同意授权

![输入图片说明](https://foruda.gitee.com/images/1690938522418523183/81b327bf_4959041.png "屏幕截图")

顶部出现授权成功，并跳转到系统首页。<br>

![输入图片说明](https://foruda.gitee.com/images/1690938559178527841/563168e4_4959041.png "屏幕截图")<br>

![输入图片说明](https://foruda.gitee.com/images/1690938636375977741/8ceb77cf_4959041.png "屏幕截图")

查看第三方应用可看到授权成功的个人信息。<br>

![输入图片说明](https://foruda.gitee.com/images/1690938725512311321/5532a2a9_4959041.png "屏幕截图")

## 授权登录（已绑定第三方平台）

### 步骤一：点击登录页面图标

![输入图片说明](https://foruda.gitee.com/images/1690938908352243992/fd044381_4959041.png "屏幕截图")

### 步骤二：同意授权

![输入图片说明](https://foruda.gitee.com/images/1690938522418523183/81b327bf_4959041.png "屏幕截图")

## 解除授权绑定

### 步骤一：个人中心点击解绑第三方应用

![输入图片说明](https://foruda.gitee.com/images/1690939087877969002/4ef324e7_4959041.png "屏幕截图")

### 步骤二：点击确定完成解绑

![输入图片说明](https://foruda.gitee.com/images/1690939108017661775/7236088d_4959041.png "屏幕截图")
