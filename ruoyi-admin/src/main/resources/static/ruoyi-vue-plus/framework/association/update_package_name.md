# 关于修改包名
- - -

**注意: 老包名为 com.ruoyi**

## 1.随便找个地方新建 org.dromara 包
![输入图片说明](https://foruda.gitee.com/images/1708491220807198688/b95c0c34_1766278.png "屏幕截图")

## 2.在包上右键选择 refactor -> rename 选择 All Directories
![输入图片说明](https://foruda.gitee.com/images/1683276891079076405/79808b22_1766278.png "屏幕截图")
![输入图片说明](https://foruda.gitee.com/images/1708491697128844860/1e87ad39_1766278.png "屏幕截图")

**因为dromara组织下有很多依赖导致idea无法识别完整包名**
<br>
![输入图片说明](https://foruda.gitee.com/images/1708490576909691001/692e5b37_1766278.png "屏幕截图")

**需要先将dromara修改为 例如: ruoyi 然后重复上述步骤 这样就可以整包修改了**
<br>
![输入图片说明](https://foruda.gitee.com/images/1708490906933084793/ff104cd7_1766278.png "屏幕截图")

## 3.使用IDEA全局替换 org.dromara 替换为 com.xxx

![输入图片说明](https://foruda.gitee.com/images/1708491055347995519/dedda0d1_1766278.png "屏幕截图")

**注意: 由于dromara组织下项目很多 非本框架的依赖模块 请勿修改 例如上图中的 org.dromara.sms4j**

## 4.如有需要 将所有模块名逐一修改即可

## 5.修改完成后需查看所有common包下模块spi文件是否修改正确

**老版本idea或者未按照教程修改包名可能导致文件丢包问题**

![输入图片说明](https://foruda.gitee.com/images/1708491365841192006/8bc337c2_1766278.png "屏幕截图")
