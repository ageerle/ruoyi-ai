# 修改应用路径
- - -
## 修改后端路径

更改 `application.yml` 的 `server.servlet.context-path` 即可更改后端容器路径

![输入图片说明](https://foruda.gitee.com/images/1724316662536650741/41d534b1_1766278.png "屏幕截图")

与之对应前端 `dev`环境 需更改 `vite.config.ts` 的代理路径

![输入图片说明](https://foruda.gitee.com/images/1724316844091667249/9b0badc5_1766278.png "屏幕截图")

`prod` 生产环境需修改 `nginx.conf` 后端代理路径

![输入图片说明](https://foruda.gitee.com/images/1661823876773225117/f1f912a9_1766278.png "屏幕截图")

## 修改前端路径
### 注意: 3.4.0 提供便捷更改方式
直接修改对应环境的 `.env.环境` 文件内的 `VITE_APP_CONTEXT_PATH` 应用访问路径即可

![输入图片说明](https://foruda.gitee.com/images/1661824572484410642/14265f05_1766278.png "屏幕截图") <br>
![输入图片说明](https://foruda.gitee.com/images/1724317049535973756/0a2cc43b_1766278.png "屏幕截图")

生产环境 `nginx.conf` 与之对应修改即可 <br>
**注意: 文件真实目录为 `/usr/share/nginx/html/admin/index.html` 此功能一般为多项目部署需要 故会增加一层目录 如不需要可以自行修改** <br>
![输入图片说明](https://foruda.gitee.com/images/1678976662194341301/2720b7e9_1766278.png "屏幕截图")
