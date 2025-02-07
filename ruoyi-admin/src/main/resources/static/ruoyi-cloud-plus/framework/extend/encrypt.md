# 数据加解密
- - -
## 框架版本 >= 1.6.0

## 引入依赖

```xml
<dependency>
    <groupId>com.ruoyi</groupId>
    <artifactId>ruoyi-common-encrypt</artifactId>
</dependency>
```

## 功能说明

数据库 数据存储加密 查询解密功能<br>
支持加密算法: `BASE64` `AES` `RSA` `SM2` `SM4`

## 注解 `@EncryptField`

![输入图片说明](https://foruda.gitee.com/images/1675577493013639395/cd920f15_1766278.png "屏幕截图")

## 用法说明

**详细用法可参考案例 TestEncryptController 测试数据库加解密功能**

全局默认加密配置(如果注解不配置则使用全局配置)

![输入图片说明](https://foruda.gitee.com/images/1675577674063566357/dee94786_1766278.png "屏幕截图")

注解可自定义算法与配置

![输入图片说明](https://foruda.gitee.com/images/1675577725117970708/7ee7a833_1766278.png "屏幕截图")

## 密钥生成说明

![输入图片说明](https://foruda.gitee.com/images/1675577852271308699/9b30258e_1766278.png "屏幕截图")

