# 对接 TOPIAM 单点登录
- - -

# 安装 TOPIAM 应用服务

参考 TOPIAM 官方文档安装 [TOPIAM安装部署](https://eiam.topiam.cn/docs/deployment/)

# 配置 OIDC 应用

在 `登录 Redirect URI` 中填写 `http://localhost:80/oauth/callback?source=topiam`

# 配置后端服务

找到框架 `application-环境.yml` 配置文件

修改 `topiam` 对应的 `client-id` 与 `client-secret`

```yaml
justauth:
  # 前端外网访问地址
  address: http://localhost:80
  type:
    topiam:
      # topiam 服务器地址，可在【应用配置信息】中找到
      server-url: http://127.0.0.1:1989/api/v1/authorize/y0q************spq***********8ol
      client-id: 449c4*********937************759
      client-secret: ac7***********1e0************28d
      redirect-uri: ${justauth.address}/social-callback?source=topiam
      scopes: [openid]
```