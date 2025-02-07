# idea环境配置
- - -
## 配置项目编码
![输入图片说明](https://foruda.gitee.com/images/1662107706295343419/e27065a9_1766278.png "屏幕截图")

## 配置运行看板
![输入图片说明](https://foruda.gitee.com/images/1662108673306567278/8af97b47_1766278.png "屏幕截图")
### 配置spring与docker看板
![输入图片说明](https://foruda.gitee.com/images/1662111392476935892/6b6760fb_1766278.png "屏幕截图")
![输入图片说明](https://foruda.gitee.com/images/1662108865191892425/3c045999_1766278.png "屏幕截图")
![输入图片说明](https://foruda.gitee.com/images/1662108877322329668/ddb6d93d_1766278.png "屏幕截图")
![输入图片说明](https://foruda.gitee.com/images/1662108894122798039/6a53a38c_1766278.png "屏幕截图")

## 配置服务器SSH连接
进入 `Settings -> Tools -> SSH Configurations` 点击加号创建SSH连接配置<br>
填写 服务器IP 用户名 密码 端口号 点击 Test Connection 测试连接<br>
![输入图片说明](https://foruda.gitee.com/images/1662107776533098115/bd78467b_1766278.png "屏幕截图")
使用Terminal 工具 点击箭头找到上方创建的SSH连接配置<br>
选择即可进入SSH连接界面 在这里可以对服务器进行命令操作<br>
![输入图片说明](https://foruda.gitee.com/images/1662108010120640495/c70f9f9a_1766278.png "屏幕截图")

## 配置服务器FTP连接
进入 `Settings -> Build-> Deployment` 点击加号 选择SFTP 创建 FTP 连接配置<br>
选择之前创建好的SSH配置 点击 Test Connection 测试连接<br>
![输入图片说明](https://foruda.gitee.com/images/1662107899553257979/e2eeb7fd_1766278.png "屏幕截图")
在IDEA上方工具栏 找到 `Tools -> Deployment -> Browse Remote Host` 打开远程界面<br>
点击箭头找到我们上方配置的SFTP连接配置 即可连接到服务器的文件目录<br>
![输入图片说明](https://foruda.gitee.com/images/1662107974682787233/b8a601fd_1766278.png "屏幕截图")

## 配置Docker连接
### 可操作远程docker与构建上传docker镜像(代替原来maven docker插件)
tcp连接需要开放服务器2375端口<br>
ssh需要使用上方的SSH连接配置<br>
建议使用SSH连接<br>
![输入图片说明](https://foruda.gitee.com/images/1662108188005932060/75872bf8_1766278.png "屏幕截图")
配置好之后 在运行窗口会多出一个Docker图标 双击即可连接远程docker<br>
可以查看容器实时日志 启动 重启 停止 等操作<br>
![输入图片说明](https://foruda.gitee.com/images/1662108250902891875/b82d022b_1766278.png "屏幕截图")