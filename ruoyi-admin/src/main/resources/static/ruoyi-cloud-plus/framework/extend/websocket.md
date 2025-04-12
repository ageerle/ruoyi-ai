# WebSocket功能
- - -

## 框架版本 >= 2.1.0

## 配置说明

配置在 `ruoyi-resource` 目录下

![输入图片说明](https://foruda.gitee.com/images/1688356273985385949/5e4d1de8_1766278.png "屏幕截图")

* enabled 是否开启此功能
* path 应用路径
* allowedOrigins 设置访问源地址

**重点: 如关闭ws功能需连同前端ws开关一同关闭 不然前端启动会报错**

![输入图片说明](https://foruda.gitee.com/images/1700644877512019497/052d2f46_1766278.png "屏幕截图")

## 使用方法

前端连接方式: `ws://后端ip:端口/resource/websocket?clientid=import.meta.env.VITE_APP_CLIENT_ID&Authorization=Bearer eyJ0eXAiO......`

**由于js不支持请求头传输故而采用参数传输 如支持请求头传输建议使用请求头传输**

传输方式:
```js
headers: {
    Authorization: "Bearer " + getToken(),
    clientid: import.meta.env.VITE_APP_CLIENT_ID
}
```

其中 `Authorization` 为请求token需要登录后获取 连接成功之后 与框架内其他获取登录用户方式一致

`WebSocketUtils.sendMessage` 推送单机消息(特殊需求使用)<br>
`WebSocketUtils.subscribeMessage` 订阅分布式消息(框架初始化已订阅)<br>
`WebSocketUtils.publishMessage` 发布分布式消息(推荐使用 所有集群内寻找到接收人)<br>
`WebSocketUtils.publishAll` 群发消息给所有连接人<br>