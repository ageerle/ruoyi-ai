# 关于OSS模块使用
- - -
## 重点注意事项

`桶/存储区域` 系统会根据配置自行创建分配权限<br>
~~如手动配置需要设置 `公有读` 权限 否则文件无法访问~~(`aliyun` 还需开通跨域配置)<br>
1.4.0 版本支持配置`公有/私有`权限(`aliyun` 还需开通跨域配置)<br>
访问站点 后严禁携带其他 `url` 例如: `/`, `/ruoyi` 等<br>
**阿里云与腾讯云SDK访问站点中不能包含桶名 系统会自动处理** <br>
**minio 站点不允许使用 localhost 请使用 127.0.0.1** <br>
**访问站点与自定义域名 都不要包含 `http` `https` 前缀 设置`https`请使用选项处理**

## 代码使用

> 参考 `SysOssService.upload` 用法 <br>
> 使用 `OssFactory.instance()` 获取当前启用的 `OssClient` 实例<br>
> 进行功能调用 获取返回值后 存储到对应的业务表

![输入图片说明](https://foruda.gitee.com/images/1678978345529639839/d350ec0b_1766278.png "屏幕截图")


## 功能配置

### 配置OSS

> 进入 `系统管理 -> 文件管理 -> 配置管理` 填写对应的OSS服务相关配置<br>

![输入图片说明](https://foruda.gitee.com/images/1678978349820700551/1f91a237_1766278.png "屏幕截图")<br>
![输入图片说明](https://foruda.gitee.com/images/1678978354387669856/3a91a3a9_1766278.png "屏幕截图")<br
![输入图片说明](https://foruda.gitee.com/images/1678978358019307086/0c2523e4_1766278.png "屏幕截图")

<font size="6">**重点说明**</font>

> 云厂商只需修改 `访问站点`对应的域 切勿乱改(云厂商强烈建议绑定自定义域名使用 七牛云必须绑定[官方规定])<br>

![输入图片说明](https://foruda.gitee.com/images/1678978362358100362/5c2c4d20_1766278.png "屏幕截图")

> 七牛云 访问站点<br>


![输入图片说明](https://foruda.gitee.com/images/1678978366254745764/e93a65ff_1766278.png "屏幕截图")<br>
![输入图片说明](https://foruda.gitee.com/images/1678978369853348732/79e8950e_1766278.png "屏幕截图")

> 阿里云 访问站点

![输入图片说明](https://foruda.gitee.com/images/1678978373981462025/56a70398_1766278.png "屏幕截图")

> 腾讯云 访问站点

![输入图片说明](https://foruda.gitee.com/images/1678978378697093134/785517f3_1766278.png "屏幕截图")

### MinIO 使用 https访问站点

**注意：S3 API 签名计算算法不支持托管 MinIO Server API 的代理方案**

[ minio https 配置方式](https://blog.csdn.net/Michelle_Zhong/article/details/126484358)

### 切换OSS

> 再配置列表点击 `状态` 按钮开启即可(注意: 只能开启一个OSS默认配置)<br>
> 手动使用 `OssFactory.instance("configKey")` <br>

![输入图片说明](https://foruda.gitee.com/images/1678978383700118702/7f3fa0c5_1766278.png "屏幕截图")

### 扩展分类

> 如有文件分类 建议创建多个 oss配置 进行切换存储<br>

例如: 创建一个 图片存储的 oss配置<br>
指定唯一的 `configKey` 与 `前缀目录` 或 直接使用独立的`桶`<br>
独立桶的特点 可以自定义访问权限<br>
例如: 创建一个私有文件存储桶 不对外开放<br>

![输入图片说明](https://foruda.gitee.com/images/1678978389139754119/140be1df_1766278.png "屏幕截图")

> 指定需要使用的配置<br>
> 使用 `OssFactory.instance("image")` 获取的 `OssClient` 会加载上图的配置 从而达到上传不同的目录或桶


![输入图片说明](https://foruda.gitee.com/images/1678978397550123641/1b536881_1766278.png "屏幕截图")


### 上传图片或文件

> 进入 `系统管理 -> 文件管理` 点击 `上传文件` 或 `上传图片` 根据选项选择即可 会对应上传到配置开启的OSS内<br>

![输入图片说明](https://foruda.gitee.com/images/1678978401028132972/445d058e_1766278.png "屏幕截图")<br>
![输入图片说明](https://foruda.gitee.com/images/1678978404388284503/5459da29_1766278.png "屏幕截图")<br>
![输入图片说明](https://foruda.gitee.com/images/1678978408761764835/c81651fc_1766278.png "屏幕截图")<br>
![输入图片说明](https://foruda.gitee.com/images/1678978412748494539/7bae621f_1766278.png "屏幕截图")

### 列表展示

> 默认展示图片(可预览) 文件会展示路径<br>

![输入图片说明](https://foruda.gitee.com/images/1678978416327601385/af1ecb3b_1766278.png "屏幕截图")<br>
![输入图片说明](https://foruda.gitee.com/images/1678978422249633007/19d68eaa_1766278.png "屏幕截图")

> 可以点击 `预览禁用启用` 按钮对是否展示进行更改

![输入图片说明](https://foruda.gitee.com/images/1678978426017014926/4f7fa3f3_1766278.png "屏幕截图")

> 点击禁用后 图片会变成路径展示

![输入图片说明](https://foruda.gitee.com/images/1678978429692592556/0231d778_1766278.png "屏幕截图")

> 也可再 `参数设置` 更改预览状态 将 `OSS预览列表资源` 改为 `false` 即可关闭预览

![输入图片说明](https://foruda.gitee.com/images/1678978433769403801/7d480e76_1766278.png "屏幕截图")

### 删除功能

> 点击列表上方或后方 `删除` 按钮 会根据OSS服务商类型 调用对应的删除(注意: 需确保对应的服务商配置正确)<br>
> 可勾选多服务商类型的文件进行删除 系统会自动判断

![输入图片说明](https://foruda.gitee.com/images/1678978438265941745/f32edc72_1766278.png "屏幕截图")
![输入图片说明](https://foruda.gitee.com/images/1678978441938542080/43ed7c3d_1766278.png "屏幕截图")

### 下载功能

> 点击列表后方对应资源的 `下载` 按钮 根据需求填写文件名 点击确认即可完成下载

![输入图片说明](https://foruda.gitee.com/images/1678978448927336261/409af888_1766278.png "屏幕截图")
![输入图片说明](https://foruda.gitee.com/images/1678978452761792483/ed0a4a72_1766278.png "屏幕截图")
