# 如何指定dubbo注册ip
- - -
## 重点说明
以下方法指定IP必须是本地有网卡的自己可以访问的IP 不可以随意乱写<br>
(云服务器公网IP是没有网卡的)

## 在`nacos`指定协议IP地址(全局生效)
```yml
dubbo:
  protocol:
    # 指定dubbo协议注册ip
    host: 192.168.0.100
```

## docker指定dubbo环境变量(单服务生效)

![输入图片说明](https://foruda.gitee.com/images/1678981332028792584/7eeef9c5_1766278.png "屏幕截图")

